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
package org.cibseven.webapp.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.BaseService;
import org.cibseven.webapp.rest.HistoryProcessService;
import org.cibseven.webapp.rest.HopByHopHeadersAdvice;
import org.cibseven.webapp.rest.ProcessDefinitionService;
import org.cibseven.webapp.rest.ProcessService;
import org.cibseven.webapp.rest.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CIB7-2766: the webclient relayed engine-rest's Transfer-Encoding, Tomcat framed the response
 * itself as well, and strict proxies (nginx 1.21.1+) answered 502 on the duplicate header.
 * This drives the real controllers and REST providers through a real Tomcat, with engine-rest
 * streaming a large resource chunked, and reads the raw response a proxy would see.
 */
class EngineResponseHeadersIT {

	private static final byte[] FORM = largeForm();

	@TempDir
	Path tomcatBaseDir;

	private MockEngineRest engine;
	private Tomcat tomcat;
	private int port;

	private static byte[] largeForm() {
		// above engine-rest's 8 KB output buffer, which is when it switches to chunked
		StringBuilder form = new StringBuilder("<form role=\"form\">\n");
		for (int i = 0; i < 400; i++) {
			form.append("<input cam-variable-name=\"x").append(i).append("\" cam-variable-type=\"String\"/>\n");
		}
		return form.append("</form>\n").toString().getBytes(StandardCharsets.UTF_8);
	}

	static Stream<Arguments> engineResources() {
		return Stream.of(
			Arguments.of("deployed start form", "/services/v1/process/def-1/deployed-start-form", "application/xhtml+xml"),
			Arguments.of("deployed task form", "/services/v1/task/task-1/deployed-form", "application/xhtml+xml"),
			Arguments.of("rendered task form", "/services/v1/task/task-1/rendered-form", "text/html"),
			Arguments.of("rendered start form", "/services/v1/process-definition/def-1/rendered-form", "text/html"),
			Arguments.of("execution variable data", "/services/v1/process/execution/exec-1/localVariables/file/data", "application/octet-stream"),
			Arguments.of("historic variable data", "/services/v1/process-history/variable/var-1/data", "application/octet-stream"));
	}

	@BeforeEach
	void start() throws Exception {
		engine = new MockEngineRest();

		BpmProvider bpmProvider = mock(BpmProvider.class, CALLS_REAL_METHODS);
		doReturn(engine.wire(new ProcessProvider())).when(bpmProvider).getProcessProvider();
		doReturn(engine.wire(new TaskProvider())).when(bpmProvider).getTaskProvider();
		doReturn(engine.wire(new VariableProvider())).when(bpmProvider).getVariableProvider();

		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(MvcConfig.class, HopByHopHeadersAdvice.class);
		context.addBeanFactoryPostProcessor(beanFactory -> {
			beanFactory.registerSingleton("processService", withProvider(new ProcessService(), bpmProvider));
			beanFactory.registerSingleton("taskService", withProvider(new TaskService(), bpmProvider));
			beanFactory.registerSingleton("processDefinitionService", withProvider(new ProcessDefinitionService(), bpmProvider));
			beanFactory.registerSingleton("historyProcessService", withProvider(new HistoryProcessService(), bpmProvider));
			beanFactory.registerSingleton("passThroughController", new PassThroughController());
		});

		tomcat = new Tomcat();
		tomcat.setBaseDir(tomcatBaseDir.toString());
		tomcat.setPort(0);
		Context servletContext = tomcat.addContext("", null);
		Tomcat.addServlet(servletContext, "dispatcher", new DispatcherServlet(context)).setLoadOnStartup(1);
		servletContext.addServletMapping("/", "dispatcher");
		tomcat.start();
		port = tomcat.getConnector().getLocalPort();
	}

	@AfterEach
	void stop() throws LifecycleException, IOException {
		tomcat.stop();
		tomcat.destroy();
		engine.close();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("engineResources")
	void relaysAChunkedEngineResourceWithASingleFraming(String resource, String path, String contentType) throws Exception {
		engine.enqueueChunked(FORM, contentType);

		RawResponse response = get(path);

		assertThat(response.statusLine).contains(" 200");
		assertSingleFraming(response);
		assertThat(response.header("Content-Type")).startsWith(contentType);
		assertThat(response.body()).isEqualTo(FORM);
	}

	@Test
	void stripsConnectionHeadersFromAnyControllerResponse() throws Exception {
		RawResponse response = get("/pass-through");

		assertThat(response.statusLine).contains(" 200");
		assertSingleFraming(response);
		assertThat(response.body()).isEqualTo(FORM);
	}

	private static void assertSingleFraming(RawResponse response) {
		// RFC 9112 6.1/6.3: at most one Transfer-Encoding, never together with Content-Length
		assertThat(response.count("Transfer-Encoding")).isLessThanOrEqualTo(1);
		assertThat(response.count("Transfer-Encoding") + response.count("Content-Length")).isEqualTo(1);
		// the engine's keep-alive belongs to the engine connection
		assertThat(response.count("Keep-Alive")).isZero();
		assertThat(response.values("Connection")).noneMatch(value -> value.equalsIgnoreCase("keep-alive"));
	}

	private RawResponse get(String path) throws IOException {
		try (Socket socket = new Socket("localhost", port)) {
			socket.setSoTimeout(10_000);
			OutputStream out = socket.getOutputStream();
			out.write(("GET " + path + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
				.getBytes(StandardCharsets.US_ASCII));
			out.flush();
			return RawResponse.parse(socket.getInputStream().readAllBytes());
		}
	}

	private static <S extends BaseService> S withProvider(S service, BpmProvider bpmProvider) {
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
		return service;
	}

	@Configuration
	@EnableWebMvc
	static class MvcConfig implements WebMvcConfigurer {
		@Override
		public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
			resolvers.add(new HandlerMethodArgumentResolver() {
				@Override
				public boolean supportsParameter(MethodParameter parameter) {
					return CIBUser.class.isAssignableFrom(parameter.getParameterType());
				}

				@Override
				public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
						NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
					return MockEngineRest.user();
				}
			});
		}
	}

	/** A controller that forgets to filter what it got from another server. */
	@RestController
	public static class PassThroughController {
		@GetMapping("/pass-through")
		public ResponseEntity<byte[]> passThrough() {
			return ResponseEntity.ok()
				.header("Transfer-Encoding", "chunked")
				.header("Connection", "keep-alive")
				.header("Keep-Alive", "timeout=60")
				.header("Content-Type", "application/xhtml+xml")
				.body(FORM);
		}
	}

	/** The response exactly as it went over the wire, headers not merged or normalized. */
	private record RawResponse(String statusLine, List<String[]> headers, byte[] rawBody) {

		static RawResponse parse(byte[] raw) {
			int end = indexOf(raw, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
			String[] lines = new String(raw, 0, end, StandardCharsets.ISO_8859_1).split("\r\n");
			List<String[]> headers = new ArrayList<>();
			for (int i = 1; i < lines.length; i++) {
				int colon = lines[i].indexOf(':');
				headers.add(new String[] { lines[i].substring(0, colon).trim(), lines[i].substring(colon + 1).trim() });
			}
			return new RawResponse(lines[0], headers, Arrays.copyOfRange(raw, end + 4, raw.length));
		}

		List<String> values(String name) {
			return headers.stream().filter(h -> h[0].equalsIgnoreCase(name)).map(h -> h[1]).toList();
		}

		long count(String name) {
			return values(name).size();
		}

		String header(String name) {
			List<String> values = values(name);
			return values.isEmpty() ? "" : values.get(0);
		}

		byte[] body() throws IOException {
			return count("Transfer-Encoding") == 0 ? rawBody : dechunk(rawBody);
		}

		private static byte[] dechunk(byte[] chunked) throws IOException {
			ByteArrayOutputStream body = new ByteArrayOutputStream();
			int pos = 0;
			while (true) {
				int lineEnd = indexOf(Arrays.copyOfRange(chunked, pos, chunked.length), "\r\n".getBytes(StandardCharsets.US_ASCII)) + pos;
				String sizeLine = new String(chunked, pos, lineEnd - pos, StandardCharsets.US_ASCII);
				int size = Integer.parseInt(sizeLine.split(";")[0].trim(), 16);
				if (size == 0) {
					return body.toByteArray();
				}
				body.write(chunked, lineEnd + 2, size);
				pos = lineEnd + 2 + size + 2;
			}
		}

		private static int indexOf(byte[] data, byte[] pattern) {
			outer:
			for (int i = 0; i <= data.length - pattern.length; i++) {
				for (int j = 0; j < pattern.length; j++) {
					if (data[i + j] != pattern[j]) {
						continue outer;
					}
				}
				return i;
			}
			throw new IllegalStateException("malformed HTTP response: " + new String(data, StandardCharsets.ISO_8859_1));
		}
	}
}
