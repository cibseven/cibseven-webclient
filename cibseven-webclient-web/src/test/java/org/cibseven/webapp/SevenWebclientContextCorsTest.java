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
package org.cibseven.webapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * A cross-origin caller must be able to reach every verb the REST services expose. The element
 * template services answer PATCH, which a browser rejects with "Invalid CORS request" when the
 * method is missing from this registry.
 */
class SevenWebclientContextCorsTest {

	/** CorsRegistry keeps the registered configurations protected. */
	private static class ReadableCorsRegistry extends CorsRegistry {
		@Override
		public Map<String, CorsConfiguration> getCorsConfigurations() {
			return super.getCorsConfigurations();
		}
	}

	private CorsConfiguration configuration() {
		ReadableCorsRegistry registry = new ReadableCorsRegistry();
		new SevenWebclientContext().addCorsMappings(registry);

		return registry.getCorsConfigurations().get("/**");
	}

	@Test
	void allowsEveryVerbTheServicesExpose() {
		assertThat(configuration().getAllowedMethods())
			.containsExactlyInAnyOrder("GET", "POST", "DELETE", "PUT", "PATCH");
	}

	@Test
	void allowsPatchForTheElementTemplateServices() {
		assertThat(configuration().getAllowedMethods()).contains("PATCH");
	}
}
