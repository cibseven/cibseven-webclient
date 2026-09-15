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
package org.cibseven.webapp.auth;

import java.util.Map;

import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Wiring, rather than behaviour: the checker is injected into the modeler controllers and into
 * the chat STOMP interceptor, so it has to be a bean of a package the webclient actually scans.
 * It lives in {@code org.cibseven.webapp.auth} because that package is scanned unconditionally by
 * {@code SevenWebclientContext} in every packaging — the modeler packages are scanned only when
 * the modeler is enabled, and the chat can be enabled without it.
 */
class ModelerAccessCheckerWiringTest {

	private static final String SCANNED_PACKAGE = "org.cibseven.webapp.auth";

	@Test
	void checkerIsAComponentOfTheScannedPackage() {
		assertEquals(SCANNED_PACKAGE, AuthorizationChecker.class.getPackageName(),
			"must stay in the package scanned by SevenWebclientContext");
		assertEquals(SCANNED_PACKAGE, ModelerAccessChecker.class.getPackageName(),
			"must stay in the package scanned by SevenWebclientContext");

		try (AnnotationConfigApplicationContext context = context(Map.of())) {
			assertNotNull(context.getBean(AuthorizationChecker.class));
			assertNotNull(context.getBean(ModelerAccessChecker.class));
		}
	}

	private AnnotationConfigApplicationContext context(Map<String, Object> properties) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources()
			.addFirst(new MapPropertySource("test", properties));
		context.register(TestConfiguration.class);
		context.refresh();
		return context;
	}

	@Configuration
	@ComponentScan(basePackages = SCANNED_PACKAGE, useDefaultFilters = false,
		includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
			classes = { AuthorizationChecker.class, ModelerAccessChecker.class }))
	static class TestConfiguration {

		@Bean
		static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		BpmProvider bpmProvider() {
			return mock(BpmProvider.class);
		}
	}
}
