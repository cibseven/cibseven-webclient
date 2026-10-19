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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;

public class SevenWebclientPropertiesTest {

	private static SevenWebclientProperties bind(StandardEnvironment environment) {
		return Binder.get(environment).bindOrCreate("cibseven.webclient", SevenWebclientProperties.class);
	}

	@Test
	public void defaultsMatchFormerlyShippedApplicationYaml() throws IOException {
		StandardEnvironment environment = new StandardEnvironment();
		new YamlPropertySourceLoader()
			.load("legacy", new ClassPathResource("config/legacy-webclient-defaults.yaml"))
			.forEach(environment.getPropertySources()::addFirst);

		assertEquals(new SevenWebclientProperties(), bind(environment));
	}

	@Test
	public void ssoPropertiesRequiredByTheProvidersHaveNoDefault() {
		SevenWebclientProperties.Sso sso = new SevenWebclientProperties().getSso();
		assertNull(sso.getClientId());
		assertNull(sso.getEndpoints().getToken());
		assertNull(sso.getEndpoints().getJwks());
	}

	static Stream<Arguments> propertyNames() {
		Function<SevenWebclientProperties, Object> engineRestUrl = p -> p.getEngineRest().getUrl();
		return Stream.of(
			Arguments.of("cibseven.webclient.engineRest.url", "http://engine", engineRestUrl),
			Arguments.of("cibseven.webclient.engine-rest.url", "http://engine", engineRestUrl),
			Arguments.of("cibseven.webclient.support-dialog", "{}",
				(Function<SevenWebclientProperties, Object>) SevenWebclientProperties::getSupportDialog),
			Arguments.of("cibseven.webclient.deprecated.authorization.enabled", true,
				(Function<SevenWebclientProperties, Object>) p -> p.getDeprecated().getAuthorization().isEnabled()),
			Arguments.of("cibseven.webclient.legacy.authorization.enabled", true,
				(Function<SevenWebclientProperties, Object>) p -> p.getLegacy().getAuthorization().isEnabled()),
			Arguments.of("cibseven.webclient.plugins.enabled", true,
				(Function<SevenWebclientProperties, Object>) p -> p.getPlugins().isEnabled()),
			Arguments.of("cibseven.webclient.custom.spring.jackson.parser.max-size", 42,
				(Function<SevenWebclientProperties, Object>) p -> p.getCustom().getSpring().getJackson().getParser().getMaxSize()));
	}

	@ParameterizedTest
	@MethodSource("propertyNames")
	public void bindsPropertyName(String name, Object value, Function<SevenWebclientProperties, Object> accessor) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(name, value.toString())));

		assertEquals(value, accessor.apply(bind(environment)));
	}

	@Test
	public void bindsEnvironmentVariable() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
			StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
			Map.of("CIBSEVEN_WEBCLIENT_ENGINEREST_URL", "http://engine")));

		assertEquals("http://engine", bind(environment).getEngineRest().getUrl());
	}
}
