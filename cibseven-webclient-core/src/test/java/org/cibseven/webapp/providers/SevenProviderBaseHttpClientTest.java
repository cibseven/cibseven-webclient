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

import java.util.List;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.CustomRestTemplate;
import org.cibseven.webapp.rest.RestTemplateConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
}
