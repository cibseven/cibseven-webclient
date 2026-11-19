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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Starts a Spring application, so the post-processor runs through its registration in
 * META-INF/spring.factories, after Spring Boot loaded the application's configuration.
 */
class WebclientConfigFilesEnvironmentPostProcessorTest {

	@Configuration(proxyBeanMethods = false)
	static class Empty {
	}

	/** Serves the webclient's configuration files from config-files/ of the test classpath */
	private static final DefaultResourceLoader RESOURCE_LOADER = new DefaultResourceLoader() {
		@Override
		public Resource getResource(String location) {
			return location.startsWith("classpath:cibseven-webclient.")
				? new ClassPathResource("config-files/" + location.substring("classpath:".length()))
				: super.getResource(location);
		}
	};

	@Test
	void appliesTheConfigurationFilesBelowTheApplicationsConfiguration() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Empty.class)
				.web(WebApplicationType.NONE)
				.resourceLoader(RESOURCE_LOADER)
				.properties("cibseven.webclient.url=from-defaults")
				.run("--cibseven.webclient.history-level=from-arguments")) {
			Environment environment = context.getEnvironment();
			assertThat(environment.getProperty("cibseven.webclient.theme")).isEqualTo("from-yaml");
			assertThat(environment.getProperty("cibseven.webclient.product-name-page-title")).isEqualTo("from-properties");
			assertThat(environment.getProperty("cibseven.webclient.history-level")).isEqualTo("from-arguments");
			assertThat(environment.getProperty("cibseven.webclient.url")).isEqualTo("from-defaults");
		}
	}
}
