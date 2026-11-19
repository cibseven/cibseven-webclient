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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Spring Boot and springdoc defaults of the standalone CIB seven webclient
 * application.
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

	/** Maven metadata of the swagger-ui webjar, which carries the version of the shipped jar. */
	static final String SWAGGER_UI_POM_PROPERTIES = "META-INF/maven/org.webjars/swagger-ui/pom.properties";

	public static Map<String, Object> properties() {
		Map<String, Object> properties = new HashMap<>(Map.ofEntries(
			Map.entry("spring.servlet.register-default-servlet", "true"),
			// Maximum size for each individual file upload
			Map.entry("spring.servlet.multipart.max-file-size", "10MB"),
			// Maximum size of the entire request, including all files and form data
			Map.entry("spring.servlet.multipart.max-request-size", "1GB"),
			// The webclient's repositories use their own persistence unit, which open-in-view does not bind
			Map.entry("spring.jpa.open-in-view", "false"),

			Map.entry("springdoc.swagger-ui.path", "/swagger-ui"),
			Map.entry("springdoc.swagger-ui.operationsSorter", "alpha"),
			Map.entry("springdoc.swagger-ui.tagsSorter", "alpha"),
			Map.entry("springdoc.swagger-ui.doc-expansion", "none")));

		// springdoc serves the swagger-ui webjar under the version it was built with (5.32.11 for
		// springdoc 3.1.0), and the Swagger UI answers 404 when the shipped webjar has another one.
		// The root pom pins a newer webjar for its patched DOMPurify, so the version is taken from
		// the jar itself instead of being kept in sync by hand.
		String swaggerUiVersion = swaggerUiVersion(StandaloneDefaults.class.getClassLoader());
		if (swaggerUiVersion != null) {
			properties.put("springdoc.swagger-ui.version", swaggerUiVersion);
		}
		return Map.copyOf(properties);
	}

	/**
	 * Returns the version of the swagger-ui webjar on the class path, or {@code null} when there is
	 * none or it carries no Maven metadata. springdoc then keeps its own default.
	 */
	static String swaggerUiVersion(ClassLoader classLoader) {
		try (InputStream in = classLoader.getResourceAsStream(SWAGGER_UI_POM_PROPERTIES)) {
			if (in == null) {
				return null;
			}
			Properties pom = new Properties();
			pom.load(in);
			String version = pom.getProperty("version");
			return version == null || version.isBlank() ? null : version.trim();
		} catch (IOException e) {
			return null;
		}
	}
}
