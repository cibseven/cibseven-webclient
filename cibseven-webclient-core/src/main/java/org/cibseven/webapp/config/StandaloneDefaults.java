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
package org.cibseven.webapp.config;

import java.util.Map;

/**
 * Spring Boot, springdoc and actuator defaults of the standalone CIB seven webclient
 * application, shared by the Spring Boot 3 and 4 builds.
 *
 * <p>They are registered as default properties by {@code SevenWebclientApplication}, both
 * when started via {@code main} and when deployed as a WAR, so every other configuration
 * source overrides them. They are deliberately not applied when the webclient is embedded
 * into another application, which keeps its own framework settings. The webclient settings
 * themselves are defined by {@link SevenWebclientProperties}.</p>
 */
public final class StandaloneDefaults {

	private StandaloneDefaults() {
	}

	public static Map<String, Object> properties() {
		return Map.ofEntries(
			// Later entries override earlier ones
			Map.entry("spring.config.import", "optional:classpath:cibseven-webclient.properties,optional:classpath:cibseven-webclient.yaml"),

			Map.entry("spring.servlet.register-default-servlet", "true"),
			// Maximum size for each individual file upload
			Map.entry("spring.servlet.multipart.max-file-size", "10MB"),
			// Maximum size of the entire request, including all files and form data
			Map.entry("spring.servlet.multipart.max-request-size", "1GB"),

			Map.entry("springdoc.swagger-ui.path", "/swagger-ui"),
			Map.entry("springdoc.swagger-ui.operationsSorter", "alpha"),
			Map.entry("springdoc.swagger-ui.tagsSorter", "alpha"),
			Map.entry("springdoc.swagger-ui.doc-expansion", "none"),

			// Only expose the "/actuator/info" and "/actuator/health" endpoints
			Map.entry("management.endpoints.access.default", "none"),
			Map.entry("management.endpoints.web.exposure.include", "health,info"),
			Map.entry("management.endpoint.info.access", "read-only"),
			Map.entry("management.endpoint.health.access", "read-only"),
			Map.entry("management.endpoint.health.probes.enabled", "true"),
			Map.entry("management.info.build.enabled", "true"),
			Map.entry("management.info.git.enabled", "true"),
			Map.entry("management.info.git.mode", "full"),
			Map.entry("management.info.java.enabled", "true"));
	}
}
