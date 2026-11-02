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

import java.util.Map;

import org.junit.jupiter.api.Test;
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
}
