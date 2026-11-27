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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

public class StandaloneDefaultsTest {

	private static StandardEnvironment withStandaloneDefaults(Map<String, Object> configured) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("configured", configured));
		environment.getPropertySources().addLast(new MapPropertySource("defaultProperties", StandaloneDefaults.properties()));
		return environment;
	}

	@Test
	void disablesOpenInViewLikeTheFormerlyShippedApplicationYaml() {
		assertEquals("false", withStandaloneDefaults(Map.of()).getProperty("spring.jpa.open-in-view"));
	}

	@Test
	void configuredValuesOverrideTheStandaloneDefaults() {
		StandardEnvironment environment = withStandaloneDefaults(Map.of(
			"spring.jpa.open-in-view", "true",
			"spring.servlet.multipart.max-file-size", "50MB"));

		assertEquals("true", environment.getProperty("spring.jpa.open-in-view"));
		assertEquals("50MB", environment.getProperty("spring.servlet.multipart.max-file-size"));
		assertEquals("1GB", environment.getProperty("spring.servlet.multipart.max-request-size"));
	}

	@Test
	void pointsSpringdocAtTheVersionOfTheShippedSwaggerUiWebjar() throws IOException {
		Properties pom = new Properties();
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(StandaloneDefaults.SWAGGER_UI_POM_PROPERTIES)) {
			pom.load(in);
		}

		assertEquals(pom.getProperty("version"), withStandaloneDefaults(Map.of()).getProperty("springdoc.swagger-ui.version"));
	}

	@Test
	void readsTheSwaggerUiVersionFromTheWebjarMetadata(@TempDir Path dir) throws IOException {
		Path pom = dir.resolve(StandaloneDefaults.SWAGGER_UI_POM_PROPERTIES);
		Files.createDirectories(pom.getParent());
		Files.writeString(pom, "version=9.9.9\ngroupId=org.webjars\nartifactId=swagger-ui\n");

		try (URLClassLoader loader = new URLClassLoader(new URL[] { dir.toUri().toURL() }, null)) {
			assertEquals("9.9.9", StandaloneDefaults.swaggerUiVersion(loader));
		}
	}

	@Test
	void leavesTheSwaggerUiVersionToSpringdocWithoutTheWebjar(@TempDir Path dir) throws IOException {
		try (URLClassLoader loader = new URLClassLoader(new URL[] { dir.toUri().toURL() }, null)) {
			assertNull(StandaloneDefaults.swaggerUiVersion(loader));
		}
	}
}
