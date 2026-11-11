/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.cibseven.webapp.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

public class CustomRestTemplateTest {

	@Test
	void defaultConstructor_decodesStringsAsUtf8() {
		assertStringConvertersUseUtf8(new CustomRestTemplate());
	}

	@Test
	void requestFactoryConstructor_decodesStringsAsUtf8() {
		assertStringConvertersUseUtf8(new CustomRestTemplate(new SimpleClientHttpRequestFactory()));
	}

	@Test
	void customStringConverterAddedLater_isKeptAndTakesPrecedence() throws Exception {
		// a customer's own converter must neither be replaced nor switched to UTF-8
		StringHttpMessageConverter custom = new StringHttpMessageConverter(StandardCharsets.ISO_8859_1);
		CustomRestTemplate template = new CustomRestTemplate();
		template.addConverter(custom);
		template.initialize();
		try {
			assertThat(template.getMessageConverters().get(0)).isSameAs(custom);
			assertThat(custom.getDefaultCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
		} finally {
			// initialize() builds a pooled HttpClient; release it
			((DisposableBean) template.getRequestFactory()).destroy();
		}
	}

	@Test
	void plainRestTemplates_keepSpringsDefault() {
		// the change applies to this template's own converter, not to a shared or static default
		new CustomRestTemplate();

		StringHttpMessageConverter plain = new RestTemplate().getMessageConverters().stream()
			.filter(StringHttpMessageConverter.class::isInstance)
			.map(StringHttpMessageConverter.class::cast)
			.findFirst().orElseThrow();
		assertThat(plain.getDefaultCharset()).isEqualTo(StringHttpMessageConverter.DEFAULT_CHARSET);
	}

	@Test
	void connectionClosedByServer_isNotReused() throws Exception {
		// like Tomcat after maxKeepAliveRequests: "Connection: close" on the last response, then the socket is closed
		try (KeepAliveServer server = new KeepAliveServer(2)) {
			assertThat(postSequentially(new RestTemplateConfiguration(), server, 5)).containsExactly("1", "1", "2", "2", "3");
		}
	}

	@Test
	void keptAliveConnection_isReused() throws Exception {
		try (KeepAliveServer server = new KeepAliveServer(Integer.MAX_VALUE)) {
			assertThat(postSequentially(new RestTemplateConfiguration(), server, 5)).containsOnly("1");
		}
	}

	@Test
	void connectionReuseDisabled_opensNewConnectionPerRequest() throws Exception {
		RestTemplateConfiguration config = new RestTemplateConfiguration();
		config.setConnectionReuseEnabled(false);
		try (KeepAliveServer server = new KeepAliveServer(Integer.MAX_VALUE)) {
			assertThat(postSequentially(config, server, 3)).containsExactly("1", "2", "3");
		}
	}

	/**
	 * Sends {@code count} POST requests one after the other, 200 ms apart, and returns the response bodies:
	 * the number of the server connection that answered each request.
	 */
	private static List<String> postSequentially(RestTemplateConfiguration config, KeepAliveServer server, int count)
			throws Exception {
		CustomRestTemplate template = CustomRestTemplate.create(config);
		try {
			List<String> bodies = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				if (i > 0) {
					// stay well below HttpClient's 2 s stale check: a longer pause replaces the closed connection and hides the bug
					Thread.sleep(200);
				}
				bodies.add(template.postForObject(server.url(), "request " + i, String.class));
			}
			return bodies;
		} finally {
			template.destroy();
		}
	}

	/**
	 * Minimal HTTP/1.1 server that answers each request with the number of the connection it came in on and
	 * closes a connection after {@code requestsPerConnection} requests, with a {@code Connection: close}
	 * header on the last response.
	 */
	private static final class KeepAliveServer implements AutoCloseable {

		private final ServerSocket serverSocket;
		private final AtomicInteger connections = new AtomicInteger();
		private final int requestsPerConnection;

		KeepAliveServer(int requestsPerConnection) throws IOException {
			this.requestsPerConnection = requestsPerConnection;
			this.serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
			Thread acceptor = new Thread(this::acceptLoop, "keep-alive-test-server");
			acceptor.setDaemon(true);
			acceptor.start();
		}

		String url() {
			return "http://localhost:" + serverSocket.getLocalPort() + "/engine-rest/test";
		}

		private void acceptLoop() {
			while (!serverSocket.isClosed()) {
				try {
					Socket socket = serverSocket.accept();
					int connection = connections.incrementAndGet();
					Thread handler = new Thread(() -> serve(socket, connection), "keep-alive-test-connection-" + connection);
					handler.setDaemon(true);
					handler.start();
				} catch (IOException e) {
					return; // server closed
				}
			}
		}

		private void serve(Socket socket, int connection) {
			try (socket) {
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
				OutputStream out = socket.getOutputStream();
				for (int request = 1; request <= requestsPerConnection; request++) {
					if (!readRequest(in)) {
						return;
					}
					boolean last = request == requestsPerConnection;
					byte[] body = String.valueOf(connection).getBytes(StandardCharsets.ISO_8859_1);
					String head = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + body.length + "\r\n"
						+ (last ? "Connection: close\r\n" : "") + "\r\n";
					out.write(head.getBytes(StandardCharsets.ISO_8859_1));
					out.write(body);
					out.flush();
				}
			} catch (IOException e) {
				// client went away
			}
		}

		/** Reads one request including its body; returns false at the end of the stream. */
		private static boolean readRequest(BufferedReader in) throws IOException {
			String line = in.readLine();
			if (line == null) {
				return false;
			}
			int contentLength = 0;
			while ((line = in.readLine()) != null && !line.isEmpty()) {
				if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) {
					contentLength = Integer.parseInt(line.substring(15).trim());
				}
			}
			for (int i = 0; i < contentLength; i++) {
				in.read();
			}
			return true;
		}

		@Override
		public void close() throws IOException {
			serverSocket.close();
		}
	}

	private static void assertStringConvertersUseUtf8(CustomRestTemplate template) {
		// engine-rest sends text without a charset; the String converter's default decides the decoding
		List<StringHttpMessageConverter> converters = template.getMessageConverters().stream()
			.filter(StringHttpMessageConverter.class::isInstance)
			.map(StringHttpMessageConverter.class::cast)
			.toList();

		assertThat(converters).isNotEmpty();
		assertThat(converters).allSatisfy(converter ->
			assertThat(converter.getDefaultCharset()).isEqualTo(StandardCharsets.UTF_8));
	}
}
