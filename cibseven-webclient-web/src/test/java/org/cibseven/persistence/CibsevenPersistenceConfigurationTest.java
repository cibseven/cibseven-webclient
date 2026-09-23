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
package org.cibseven.persistence;

import java.util.List;

import javax.sql.DataSource;

import org.cibseven.modeler.config.ModelerPersistence;
import org.cibseven.modeler.config.contributed.ContributedEntity;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;

import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The persistence unit the webclient owns: one unit, several features in it, and none of it
 * attached to the beans an embedding application brought.
 */
class CibsevenPersistenceConfigurationTest {

	/** A standalone webclient: Spring Boot auto-configures the application's JPA beans. */
	private final ApplicationContextRunner standalone = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:standalone;DB_CLOSE_DELAY=-1",
			"spring.jpa.hibernate.ddl-auto=create-drop")
		.withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations
			.of(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
		.withUserConfiguration(CibsevenPersistenceConfiguration.class, ModelerLikeFeature.class);

	/** An embedding application that named its own JPA beans and left no default names behind. */
	private final ApplicationContextRunner embedded = standalone
		.withUserConfiguration(HostWithOwnPersistenceUnit.class);

	@Test
	void theUnitCarriesTheNameOfTheWebclient() {
		standalone.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasBean(CibsevenJpa.ENTITY_MANAGER_FACTORY);
			assertThat(context).hasBean(CibsevenJpa.TRANSACTION_MANAGER);
		});
	}

	@Test
	void autoConfiguredBeansOfTheApplicationAreLeftAlone() {
		standalone.run(context -> {
			// Boot's own beans still exist: the webclient adds a unit, it does not replace one.
			assertThat(context.getBeanNamesForType(EntityManagerFactory.class))
				.contains("entityManagerFactory", CibsevenJpa.ENTITY_MANAGER_FACTORY);
			assertThat(context.getBean(CibsevenJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class))
				.isNotSameAs(context.getBean("entityManagerFactory", EntityManagerFactory.class));
		});
	}

	@Test
	void neitherBeanIsPrimary() {
		standalone.run(context -> {
			assertThat(context.getBeanFactory().getBeanDefinition(CibsevenJpa.ENTITY_MANAGER_FACTORY).isPrimary())
				.isFalse();
			assertThat(context.getBeanFactory().getBeanDefinition(CibsevenJpa.TRANSACTION_MANAGER).isPrimary())
				.isFalse();
		});
	}

	@Test
	void theEntitiesOfAFeatureStayOutOfTheApplicationsUnit() {
		standalone.run(context -> {
			assertThat(context.getBean(CibsevenJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class)
				.getMetamodel().getEntities())
				.anyMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()));
			assertThat(context.getBean("entityManagerFactory", EntityManagerFactory.class)
				.getMetamodel().getEntities())
				.noneMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()));
		});
	}

	@Test
	void transactionalQualifierResolvesToTheWebclientsOwnManager() {
		standalone.run(context -> {
			TransactionManager resolved = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
				context.getBeanFactory(), TransactionManager.class, CibsevenJpa.TRANSACTION_MANAGER);

			assertThat(((JpaTransactionManager) resolved).getEntityManagerFactory())
				.isSameAs(context.getBean(CibsevenJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class));
		});
	}

	/**
	 * The case the beans are named for: the host application called its own
	 * {@code appEntityManagerFactory} / {@code appTransactionManager}, so nothing is called
	 * {@code entityManagerFactory} and resolving that name would fail.
	 */
	@Test
	void worksWhenTheHostUsesNoDefaultBeanNames() {
		embedded.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBeanNamesForType(EntityManagerFactory.class))
				.containsExactlyInAnyOrder("appEntityManagerFactory", CibsevenJpa.ENTITY_MANAGER_FACTORY);
			assertThat(context.getBeanNamesForType(PlatformTransactionManager.class))
				.containsExactlyInAnyOrder("appTransactionManager", CibsevenJpa.TRANSACTION_MANAGER);

			TransactionManager resolved = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
				context.getBeanFactory(), TransactionManager.class, CibsevenJpa.TRANSACTION_MANAGER);
			assertThat(((JpaTransactionManager) resolved).getEntityManagerFactory())
				.isSameAs(context.getBean(CibsevenJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class));
		});
	}

	/** Every feature that stores data joins this unit rather than opening one of its own. */
	@Test
	void featuresShareTheOneUnit() {
		standalone.withUserConfiguration(FeatureContributingEntities.class).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(CibsevenJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class)
				.getMetamodel().getEntities())
				.anyMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()))
				.anyMatch(entity -> ContributedEntity.class.equals(entity.getJavaType()));
		});
	}

	@Test
	void aDedicatedDataSourceIsUsedWhenTheApplicationProvidesOne() {
		standalone.withUserConfiguration(HostWithDedicatedDataSource.class).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(CibsevenJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class)
				.getProperties().get("jakarta.persistence.nonJtaDataSource"))
				.isSameAs(context.getBean(CibsevenJpa.DATA_SOURCE, DataSource.class))
				.isNotSameAs(context.getBean("applicationDataSource", DataSource.class));
		});
	}

	/** Stands in for the modeler, which contributes its entities like any other feature. */
	@Configuration(proxyBeanMethods = false)
	static class ModelerLikeFeature {

		@Bean
		CibsevenEntityPackages modelerEntityPackages() {
			return () -> List.of(ModelerPersistence.ENTITY_PACKAGE);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class HostWithOwnPersistenceUnit {

		@Bean
		org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean appEntityManagerFactory(
				org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder builder, DataSource dataSource) {
			// Deliberately scans a package without entities: the host's unit knows nothing of ours.
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
		CibsevenEntityPackages contributedPackages() {
			return () -> List.of(ContributedEntity.class.getPackageName());
		}
	}

	/** Two data sources, so the assertion can tell which one the unit took. */
	@Configuration(proxyBeanMethods = false)
	static class HostWithDedicatedDataSource {

		@Bean(CibsevenJpa.DATA_SOURCE)
		DataSource cibsevenDataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.generateUniqueName(true).build();
		}

		@Bean
		@Primary
		DataSource applicationDataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.generateUniqueName(true).build();
		}
	}

}
