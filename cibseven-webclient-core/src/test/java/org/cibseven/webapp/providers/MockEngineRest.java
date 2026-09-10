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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.CustomRestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Stands in for the engine REST API when testing a {@link SevenProviderBase} subclass.
 * <p>
 * The providers in this package are thin adapters over HTTP: they build a URL, issue a request and
 * map the response. Testing them therefore means asserting the request that went out and the object
 * that came back, which is what this helper makes cheap. It wires a provider by reflection because
 * the providers have no constructor injection - their collaborators and {@code @Value} fields are
 * set by Spring in production, exactly as {@code InfoServiceTest} does it.
 * <p>
 * Prefer this over {@code @SpringBootTest}: it needs no application context, so a whole test class
 * runs in milliseconds. The {@code *IT} classes in this package use a context because they predate
 * this helper.
 */
final class MockEngineRest implements AutoCloseable {

	static final String ENGINE_REST_PATH = "/engine-rest";
	static final String AUTH_TOKEN = "Bearer test-token";

	private final MockWebServer server = new MockWebServer();

	MockEngineRest() throws IOException {
		// Fail fast rather than block: with the default dispatcher an unexpected extra request
		// (a provider that makes more round-trips than the test enqueued) parks forever and the
		// whole build hangs. failFast answers 404 instead, so the test fails in milliseconds and
		// names the provider that surprised us.
		QueueDispatcher dispatcher = new QueueDispatcher();
		dispatcher.setFailFast(true);
		server.setDispatcher(dispatcher);
		server.start();
	}

	/** Points {@code provider} at this server and fills in the fields Spring would inject. */
	<T extends SevenProviderBase> T wire(T provider) {
		@SuppressWarnings("unchecked")
		BaseUserProvider<?> userProvider = mock(BaseUserProvider.class);
		when(userProvider.getEngineRestToken(any(CIBUser.class))).thenReturn(AUTH_TOKEN);

		ReflectionTestUtils.setField(provider, "customRestTemplate", new CustomRestTemplate());
		ReflectionTestUtils.setField(provider, "baseUserProvider", userProvider);
		ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:" + server.getPort());
		ReflectionTestUtils.setField(provider, "engineRestPath", ENGINE_REST_PATH);
		ReflectionTestUtils.setField(provider, "jacksonParserMaxSize", 20_000_000);
		return provider;
	}

	/** A user with no engine set, so the provider falls back to the configured engine URL. */
	static CIBUser user() {
		CIBUser user = new CIBUser();
		user.setUserID("demo");
		user.setAuthToken(AUTH_TOKEN);
		return user;
	}

	void enqueueJson(String body) {
		server.enqueue(new MockResponse().setBody(body).addHeader("Content-Type", "application/json"));
	}

	void enqueueEmpty(int status) {
		server.enqueue(new MockResponse().setResponseCode(status));
	}

	void enqueueStatus(int status, String body) {
		server.enqueue(new MockResponse().setResponseCode(status).setBody(body)
			.addHeader("Content-Type", "application/json"));
	}

	RecordedRequest take() throws InterruptedException {
		RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
		if (request == null) {
			throw new AssertionError("expected another request to the engine, but none arrived");
		}
		return request;
	}

	/** The path of the next request, with the {@code /engine-rest} prefix stripped for readability. */
	String takePath() throws InterruptedException {
		String path = take().getPath();
		return path.startsWith(ENGINE_REST_PATH) ? path.substring(ENGINE_REST_PATH.length()) : path;
	}

	int requestCount() {
		return server.getRequestCount();
	}

	@Override
	public void close() throws IOException {
		server.shutdown();
	}
}
