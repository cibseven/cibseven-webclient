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

import java.util.List;

import javax.sql.DataSource;

import org.cibseven.modeler.config.contributed.ContributedEntity;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.util.ElementTemplateLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The modeler is embedded into applications that bring their own JPA setup, so its persistence unit
 * must stand on its own: distinct bean names, nothing {@code @Primary}, and no modeler entities in
 * the host's unit (CIB7-1776).
 *
 * <p>The transaction manager assertions go through {@code BeanFactoryAnnotationUtils.qualifiedBeanOfType},
 * which is the lookup {@code TransactionAspectSupport.determineQualifiedTransactionManager} performs for
 * a {@code @Transactional(ModelerJpa.TRANSACTION_MANAGER)} qualifier.</p>
 */
class ModelerPersistenceConfigurationTest {

	/** A standalone webclient: Spring Boot auto-configures the application's JPA beans. */
	private final ApplicationContextRunner standalone = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:standalone;DB_CLOSE_DELAY=-1",
			"spring.jpa.hibernate.ddl-auto=create-drop")
		.withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations
			.of(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
		.withUserConfiguration(ModelerPersistenceConfiguration.class);

	/** An embedding application that named its own JPA beans and left no default names behind. */
	private final ApplicationContextRunner embedded = standalone
		.withUserConfiguration(HostWithOwnPersistenceUnit.class);

	@Test
	void modelerOwnsItsFactoryAndTransactionManagerUnderItsOwnNames() {
		standalone.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasBean(ModelerJpa.ENTITY_MANAGER_FACTORY);
			assertThat(context).hasBean(ModelerJpa.TRANSACTION_MANAGER);
		});
	}

	@Test
	void autoConfiguredBeansOfTheApplicationAreLeftAlone() {
		standalone.run(context -> {
			// Boot's own beans still exist: the modeler adds a unit, it does not replace one.
			assertThat(context.getBeanNamesForType(EntityManagerFactory.class))
				.contains("entityManagerFactory", ModelerJpa.ENTITY_MANAGER_FACTORY);
			assertThat(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class))
				.isNotSameAs(context.getBean("entityManagerFactory", EntityManagerFactory.class));
		});
	}

	@Test
	void neitherModelerBeanIsPrimary() {
		standalone.run(context -> {
			assertThat(context.getBeanFactory().getBeanDefinition(ModelerJpa.ENTITY_MANAGER_FACTORY).isPrimary())
				.isFalse();
			assertThat(context.getBeanFactory().getBeanDefinition(ModelerJpa.TRANSACTION_MANAGER).isPrimary())
				.isFalse();
		});
	}

	@Test
	void modelerEntitiesStayOutOfTheApplicationsUnit() {
		standalone.run(context -> {
			assertThat(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class)
				.getMetamodel().getEntities())
				.anyMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()));
			assertThat(context.getBean("entityManagerFactory", EntityManagerFactory.class)
				.getMetamodel().getEntities())
				.noneMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()));
		});
	}

	@Test
	void transactionalQualifierResolvesToTheModelersOwnManager() {
		standalone.run(context -> {
			TransactionManager resolved = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
				context.getBeanFactory(), TransactionManager.class, ModelerJpa.TRANSACTION_MANAGER);

			assertThat(((JpaTransactionManager) resolved).getEntityManagerFactory())
				.isSameAs(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class));
		});
	}

	/**
	 * The case the previous fix could not survive: the host application named its beans
	 * {@code appEntityManagerFactory} / {@code appTransactionManager}, so nothing is called
	 * {@code entityManagerFactory} and resolving that name would fail.
	 */
	@Test
	void worksWhenTheHostUsesNoDefaultBeanNames() {
		embedded.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBeanNamesForType(EntityManagerFactory.class))
				.containsExactlyInAnyOrder("appEntityManagerFactory", ModelerJpa.ENTITY_MANAGER_FACTORY);
			assertThat(context.getBeanNamesForType(PlatformTransactionManager.class))
				.containsExactlyInAnyOrder("appTransactionManager", ModelerJpa.TRANSACTION_MANAGER);

			TransactionManager resolved = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
				context.getBeanFactory(), TransactionManager.class, ModelerJpa.TRANSACTION_MANAGER);
			assertThat(((JpaTransactionManager) resolved).getEntityManagerFactory())
				.isSameAs(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class));
		});
	}

	/**
	 * How the enterprise chat joins the unit: it contributes its entity package instead of opening a
	 * second persistence unit with a second transaction manager.
	 */
	@Test
	void contributedEntityPackagesJoinTheModelersUnit() {
		standalone.withUserConfiguration(FeatureContributingEntities.class).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class)
				.getMetamodel().getEntities())
				.anyMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()))
				.anyMatch(entity -> ContributedEntity.class.equals(entity.getJavaType()));
		});
	}

	@Test
	void aDedicatedDataSourceIsUsedWhenTheApplicationProvidesOne() {
		standalone.withUserConfiguration(HostWithDedicatedModelerDataSource.class).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class)
				.getProperties().get("jakarta.persistence.nonJtaDataSource"))
				.isSameAs(context.getBean(ModelerJpa.DATA_SOURCE, DataSource.class));
		});
	}

	/**
	 * Startup work must run on the modeler's transaction manager too. An unqualified
	 * {@code TransactionTemplate} is built on the primary manager, which in an embedding application
	 * is the host's: a JDBC transaction there binds a connection to the thread and the modeler's JPA
	 * work then fails with "Pre-bound JDBC Connection found".
	 */
	@Test
	void theElementTemplateLoaderRunsOnTheModelersTransactionManager() {
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
					.isSameAs(context.getBean(ModelerJpa.TRANSACTION_MANAGER, PlatformTransactionManager.class));
			});
	}

	/**
	 * A host whose primary transaction manager is JDBC-based on the shared data source — the shape of
	 * an application embedding the modeler next to the process engine.
	 */
	@Configuration(proxyBeanMethods = false)
	static class HostDrivingItsOwnDataSourceTransactions {

		@Bean
		@Primary
		PlatformTransactionManager hostTransactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class HostWithOwnPersistenceUnit {

		@Bean
		org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean appEntityManagerFactory(
				org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder builder, DataSource dataSource) {
			// Deliberately scans a package without entities: the host's unit knows nothing of the modeler.
			return builder.dataSource(dataSource).packages("org.cibseven.modeler.config")
				.persistenceUnit("app").build();
		}

		@Bean
		PlatformTransactionManager appTransactionManager(
				@Qualifier("appEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class FeatureContributingEntities {

		@Bean
		ModelerEntityPackages contributedPackages() {
			return () -> List.of(ContributedEntity.class.getPackageName());
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class HostWithDedicatedModelerDataSource {

		@Bean(ModelerJpa.DATA_SOURCE)
		DataSource modelerDataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.generateUniqueName(true).build();
		}
	}
}
