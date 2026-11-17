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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.CustomRestTemplate;
import org.cibseven.webapp.rest.RestTemplateConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * The per-call RestTemplate of {@link SevenProviderBase} against a mock engine. It lives here rather
 * than in cibseven-interfaces' SevenProviderBaseTest because {@link MockEngineRest} and MockWebServer
 * are test dependencies of this module only.
 */
public class SevenProviderBaseHttpClientTest {

	private static class TestProvider extends SevenProviderBase {
	}

	/**
	 * Engine GETs used to run on a plain HttpURLConnection: no pooling, no configured timeouts, and an
	 * infinite read timeout (CIB7-2206). The User-Agent is a default header of the configured HTTP
	 * client, so it only arrives when the request went through that client.
	 */
	@Test
	void doGet_runsOnTheConfiguredHttpClientAndItsInterceptorsOnce() throws Exception {
		CustomRestTemplate template = CustomRestTemplate.create(new RestTemplateConfiguration());
		template.setInterceptors(List.of((request, body, execution) -> {
			request.getHeaders().add("X-Intercepted", "yes");
			return execution.execute(request, body);
		}));
		try (MockEngineRest engine = new MockEngineRest()) {
			TestProvider provider = engine.wire(new TestProvider());
			ReflectionTestUtils.setField(provider, "customRestTemplate", template);
			CIBUser user = MockEngineRest.user();
			engine.enqueueJson("{}");

			provider.doGet(provider.getEngineRestUrl(user) + "/version", String.class, user, false);

			RecordedRequest request = engine.take();
			assertThat(request.getHeader("User-Agent")).isEqualTo("CibSeven-CustomRestTemplate");
			assertThat(request.getHeaders().values("X-Intercepted")).containsExactly("yes");
		} finally {
			template.destroy();
		}
	}

	/**
	 * Since engine GETs run on the configured HTTP client (CIB7-2206), they share its connection pool
	 * and its limits: once {@code maxConnPerRoute} connections to the engine are in use, the next GET
	 * waits up to {@code connectionRequestTimeout} for one to free up and then fails without reaching
	 * the engine. On the former plain HttpURLConnection it opened a new connection instead, with no limit.
	 */
	@Test
	void doGet_waitsForAPooledConnection_andTimesOutWhenMaxConnPerRouteIsInUse() throws Exception {
		CustomRestTemplate template = CustomRestTemplate.create(poolConfig(1));
		try (MockEngineRest engine = new MockEngineRest()) {
			TestProvider provider = engine.wire(new TestProvider());
			ReflectionTestUtils.setField(provider, "customRestTemplate", template);
			CIBUser user = MockEngineRest.user();
			String url = provider.getEngineRestUrl(user) + "/version";
			engine.enqueueJsonAfter("{\"version\":\"slow\"}", 3, TimeUnit.SECONDS);

			CompletableFuture<ResponseEntity<String>> slow =
				CompletableFuture.supplyAsync(() -> provider.doGet(url, String.class, user, false));
			engine.take(); // the slow GET now holds the only pooled connection

			long start = System.nanoTime();
			assertThatThrownBy(() -> provider.doGet(url, String.class, user, false))
				.isInstanceOf(ResourceAccessException.class)
				.hasRootCauseInstanceOf(ConnectionRequestTimeoutException.class);
			assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start))
				.as("waited for connectionRequestTimeout, not for the slow response")
				.isBetween(900L, 2_900L);

			assertThat(slow.get(10, TimeUnit.SECONDS).getBody()).isEqualTo("{\"version\":\"slow\"}");
			assertThat(engine.requestCount()).as("the waiting GET never reached the engine").isEqualTo(1);
		} finally {
			template.destroy();
		}
	}

	/** With room in the pool, a second GET runs while the first one still waits for its answer. */
	@Test
	void doGet_runsInParallelUpToMaxConnPerRoute() throws Exception {
		CustomRestTemplate template = CustomRestTemplate.create(poolConfig(2));
		try (MockEngineRest engine = new MockEngineRest()) {
			TestProvider provider = engine.wire(new TestProvider());
			ReflectionTestUtils.setField(provider, "customRestTemplate", template);
			CIBUser user = MockEngineRest.user();
			String url = provider.getEngineRestUrl(user) + "/version";
			engine.enqueueJsonAfter("{\"version\":\"slow\"}", 3, TimeUnit.SECONDS);
			engine.enqueueJson("{\"version\":\"fast\"}");

			CompletableFuture<ResponseEntity<String>> slow =
				CompletableFuture.supplyAsync(() -> provider.doGet(url, String.class, user, false));
			engine.take();

			assertThat(provider.doGet(url, String.class, user, false).getBody()).isEqualTo("{\"version\":\"fast\"}");
			assertThat(slow).as("the first GET is still in flight").isNotDone();
			assertThat(slow.get(10, TimeUnit.SECONDS).getBody()).isEqualTo("{\"version\":\"slow\"}");
		} finally {
			template.destroy();
		}
	}

	private static RestTemplateConfiguration poolConfig(int maxConnPerRoute) {
		RestTemplateConfiguration config = new RestTemplateConfiguration();
		config.setMaxConnPerRoute(maxConnPerRoute);
		config.setMaxConnTotal(maxConnPerRoute);
		config.setConnectionRequestTimeout(1);
		return config;
	}
}
