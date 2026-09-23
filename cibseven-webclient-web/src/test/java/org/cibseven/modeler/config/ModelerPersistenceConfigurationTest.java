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

import javax.sql.DataSource;

import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.repository.ProcessDiagramRepository;
import org.cibseven.modeler.util.ElementTemplateLoader;
import org.cibseven.persistence.CibsevenJpa;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The modeler's share of the webclient's persistence unit. The unit itself — its names, the beans
 * it must not touch and the features that join it — is covered by the persistence configuration's
 * own test.
 */
class ModelerPersistenceConfigurationTest {

	private final ApplicationContextRunner standalone = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:modelershare;DB_CLOSE_DELAY=-1",
			"spring.jpa.hibernate.ddl-auto=create-drop")
		.withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations
			.of(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
		.withUserConfiguration(ModelerPersistenceConfiguration.class);

	@Test
	void theModelersEntitiesGoIntoTheWebclientsUnit() {
		standalone.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(CibsevenJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class)
				.getMetamodel().getEntities())
				.anyMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()));
		});
	}

	/** A repository bound to the host's unit would not find the modeler's entities at all. */
	@Test
	void theModelersRepositoriesRunOnThatUnit() {
		standalone.run(context -> {
			assertThat(context).hasSingleBean(ProcessDiagramRepository.class);
			assertThat(context.getBean(ProcessDiagramRepository.class).count()).isZero();
		});
	}

	/**
	 * Startup work must run on the webclient's transaction manager too. An unqualified
	 * {@code TransactionTemplate} is built on the primary manager, which in an embedding application
	 * is the host's: a JDBC transaction there binds a connection to the thread and the JPA work then
	 * fails with "Pre-bound JDBC Connection found".
	 */
	@Test
	void theElementTemplateLoaderRunsOnTheWebclientsTransactionManager() {
		standalone
			.withPropertyValues("cibseven.webclient.modeler.enabled=true")
			.withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations
				.of(TransactionAutoConfiguration.class))
			.withUserConfiguration(HostDrivingItsOwnDataSourceTransactions.class)
			.withBean(ElementTemplateProperties.class)
			.withBean(ObjectMapper.class)
			.withBean(ElementTemplateLoader.class)
			.run(context -> {
				assertThat(context).hasNotFailed();
				TransactionTemplate template = (TransactionTemplate) ReflectionTestUtils
					.getField(context.getBean(ElementTemplateLoader.class), "transactionTemplate");

				assertThat(template.getTransactionManager())
					.isSameAs(context.getBean(CibsevenJpa.TRANSACTION_MANAGER, PlatformTransactionManager.class));
			});
	}

	/**
	 * A host whose primary transaction manager is JDBC-based on the shared data source — the shape of
	 * an application embedding the webclient next to the process engine.
	 */
	@Configuration(proxyBeanMethods = false)
	static class HostDrivingItsOwnDataSourceTransactions {

		@Bean
		@Primary
		PlatformTransactionManager hostTransactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}
	}
}
