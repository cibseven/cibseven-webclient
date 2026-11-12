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
package org.cibseven.modeler.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cibseven.modeler.config.ModelerEnvironmentPostProcessor.DATA_SOURCE_AUTO_CONFIGURATION;
import static org.cibseven.modeler.config.ModelerEnvironmentPostProcessor.JNDI_DATA_SOURCE_AUTO_CONFIGURATION;

import java.util.List;

import javax.sql.DataSource;

import org.cibseven.webapp.SevenWebclientApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JndiDataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ClassUtils;

class ModelerEnvironmentPostProcessorTest {

	private static final String WILDFLY_JNDI_NAME = "spring.datasource.jndi-name=java:jboss/datasources/ProcessEngine";

	private final ModelerEnvironmentPostProcessor postProcessor = new ModelerEnvironmentPostProcessor();

	private static SpringApplication standalone() {
		SpringApplication application = new SpringApplication();
		application.setMainApplicationClass(SevenWebclientApplication.class);
		return application;
	}

	private List<String> excludedAfterPostProcessing(MockEnvironment environment, SpringApplication application) {
		postProcessor.postProcessEnvironment(environment, application);
		return Binder.get(environment)
			.bind("spring.autoconfigure.exclude", Bindable.listOf(String.class))
			.orElse(List.of());
	}

	@Test
	void excludesBothDataSourcesWhenTheStandaloneModelerIsDisabled() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("spring.datasource.jndi-name", "java:jboss/datasources/ProcessEngine");

		assertThat(excludedAfterPostProcessing(environment, standalone()))
			.containsExactly(DATA_SOURCE_AUTO_CONFIGURATION, JNDI_DATA_SOURCE_AUTO_CONFIGURATION);
	}

	@Test
	void excludesNothingWhenTheModelerIsEnabled() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("cibseven.webclient.modeler.enabled", "true");

		assertThat(excludedAfterPostProcessing(environment, standalone())).isEmpty();
	}

	@Test
	void leavesAnEmbeddingApplicationsDataSourcesAlone() {
		SpringApplication host = new SpringApplication();
		host.setMainApplicationClass(ModelerEnvironmentPostProcessorTest.class);

		assertThat(excludedAfterPostProcessing(new MockEnvironment(), host)).isEmpty();
	}

	@Test
	void keepsTheExclusionsTheApplicationConfigures() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("spring.autoconfigure.exclude", "com.example.First," + DATA_SOURCE_AUTO_CONFIGURATION);

		assertThat(excludedAfterPostProcessing(environment, standalone())).containsExactly(
			"com.example.First", DATA_SOURCE_AUTO_CONFIGURATION, JNDI_DATA_SOURCE_AUTO_CONFIGURATION);
	}

	@Test
	void keepsExclusionsConfiguredAsAnIndexedList() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("spring.autoconfigure.exclude[0]", "com.example.First")
			.withProperty("spring.autoconfigure.exclude[1]", "com.example.Second");

		assertThat(excludedAfterPostProcessing(environment, standalone())).containsExactly(
			"com.example.First", "com.example.Second", DATA_SOURCE_AUTO_CONFIGURATION, JNDI_DATA_SOURCE_AUTO_CONFIGURATION);
	}

	@Test
	void namesAutoConfigurationsThatExist() {
		ClassLoader classLoader = getClass().getClassLoader();
		assertThat(ClassUtils.isPresent(DATA_SOURCE_AUTO_CONFIGURATION, classLoader)).isTrue();
		assertThat(ClassUtils.isPresent(JNDI_DATA_SOURCE_AUTO_CONFIGURATION, classLoader)).isTrue();
	}

	/** Startups below run the post-processor through its registration in META-INF/spring.factories */
	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ JndiDataSourceAutoConfiguration.class, DataSourceAutoConfiguration.class })
	static class DataSourceOnly {
	}

	private static SpringApplicationBuilder application(String... properties) {
		return new SpringApplicationBuilder(DataSourceOnly.class)
			.web(WebApplicationType.NONE)
			.properties(properties);
	}

	@Test
	void startsTheWildFlyWarWithoutItsJndiDataSourceWhenTheModelerIsDisabled() {
		try (ConfigurableApplicationContext context = application(WILDFLY_JNDI_NAME, "cibseven.webclient.modeler.enabled=false")
				.main(SevenWebclientApplication.class)
				.run()) {
			assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
		}
	}

	@Test
	void looksUpTheJndiDataSourceWhenTheModelerIsEnabled() {
		SpringApplicationBuilder war = application(WILDFLY_JNDI_NAME, "cibseven.webclient.modeler.enabled=true")
			.main(SevenWebclientApplication.class);

		assertThatThrownBy(() -> war.run().close()).hasStackTraceContaining("java:jboss/datasources/ProcessEngine");
	}

	@Test
	void keepsTheDataSourceOfAnEmbeddingApplication() {
		try (ConfigurableApplicationContext context = application("spring.datasource.url=jdbc:h2:mem:host").run()) {
			assertThat(context.getBeanNamesForType(DataSource.class)).hasSize(1);
		}
	}
}
