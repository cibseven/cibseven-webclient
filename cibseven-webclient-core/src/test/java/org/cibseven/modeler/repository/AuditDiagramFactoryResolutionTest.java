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
package org.cibseven.modeler.repository;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The modeler resolves its entity manager factory and transaction manager by bean name, so it has
 * to work both in a standalone deployment, where Spring Boot auto-configures a single one of each,
 * and in an application that embeds the webclient next to its own JPA setup, where there are two of
 * each and neither is marked {@code @Primary}.
 *
 * <p>The transaction manager assertions go through {@code BeanFactoryAnnotationUtils.qualifiedBeanOfType},
 * which is the lookup {@code TransactionAspectSupport.determineQualifiedTransactionManager} performs
 * for a {@code @Transactional("transactionManager")} qualifier.
 */
class AuditDiagramFactoryResolutionTest {

	private final ApplicationContextRunner twoUnits =
			new ApplicationContextRunner().withUserConfiguration(TwoPersistenceUnitsConfig.class);

	private final ApplicationContextRunner singleUnit =
			new ApplicationContextRunner().withUserConfiguration(SinglePersistenceUnitConfig.class);

	// --- Embedded in a host application: two factories, two transaction managers, no primary ---

	@Test
	void startsWithTwoFactoriesAndNoPrimary() {
		twoUnits.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBeanNamesForType(EntityManagerFactory.class))
					.containsExactlyInAnyOrder("entityManagerFactory", "appEntityManagerFactory");
			assertThat(context.getBeanNamesForType(PlatformTransactionManager.class))
					.containsExactlyInAnyOrder("transactionManager", "appTransactionManager");
			assertThat(context).hasSingleBean(AuditDiagram.class);
		});
	}

	@Test
	void auditDiagramResolvesTheFactoryNamedEntityManagerFactory() {
		twoUnits.run(context -> {
			EntityManager entityManager = entityManagerOf(context.getBean(AuditDiagram.class));

			assertThat(entityManager.getEntityManagerFactory())
					.isSameAs(context.getBean("entityManagerFactory", EntityManagerFactory.class));

			// The modeler entities are reachable through that factory and not through the host's,
			// so the assertion above cannot hold by accident.
			assertThat(entityManager.getMetamodel().getEntities())
					.anyMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()));
			assertThat(context.getBean("appEntityManagerFactory", EntityManagerFactory.class)
					.getMetamodel().getEntities())
					.noneMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()));
		});
	}

	@Test
	void transactionalQualifierResolvesToTheModelerTransactionManager() {
		twoUnits.run(context -> {
			TransactionManager resolved = BeanFactoryAnnotationUtils
					.qualifiedBeanOfType(context.getBeanFactory(), TransactionManager.class, "transactionManager");

			assertThat(resolved).isSameAs(context.getBean("transactionManager", PlatformTransactionManager.class));
			// It transacts against the modeler datasource, not the host's.
			assertThat(((JpaTransactionManager) resolved).getEntityManagerFactory())
					.isSameAs(context.getBean("entityManagerFactory", EntityManagerFactory.class))
					.isNotSameAs(context.getBean("appEntityManagerFactory", EntityManagerFactory.class));
		});
	}

	// --- Standalone deployment: a single factory and transaction manager, as Boot configures them ---

	@Test
	void standaloneDeploymentResolvesTheSingleFactoryAndTransactionManager() {
		singleUnit.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBeanNamesForType(EntityManagerFactory.class))
					.containsExactly("entityManagerFactory");

			EntityManagerFactory factory = context.getBean("entityManagerFactory", EntityManagerFactory.class);
			assertThat(entityManagerOf(context.getBean(AuditDiagram.class)).getEntityManagerFactory())
					.isSameAs(factory);

			TransactionManager resolved = BeanFactoryAnnotationUtils
					.qualifiedBeanOfType(context.getBeanFactory(), TransactionManager.class, "transactionManager");
			assertThat(((JpaTransactionManager) resolved).getEntityManagerFactory()).isSameAs(factory);
		});
	}

	private static EntityManager entityManagerOf(AuditDiagram auditDiagram) {
		return (EntityManager) ReflectionTestUtils.getField(auditDiagram, "entityManager");
	}

	@Configuration(proxyBeanMethods = false)
	@Import(AuditDiagram.class)
	static class TwoPersistenceUnitsConfig {

		@Bean
		DataSource modelerDataSource() {
			return embeddedDatabase();
		}

		@Bean
		DataSource appDataSource() {
			return embeddedDatabase();
		}

		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactory(
				@Qualifier("modelerDataSource") DataSource dataSource) {
			return factoryFor(dataSource, "org.cibseven.modeler.model");
		}

		/** Stands in for the host application's own factory; deliberately has no modeler entities. */
		@Bean
		LocalContainerEntityManagerFactoryBean appEntityManagerFactory(
				@Qualifier("appDataSource") DataSource dataSource) {
			return factoryFor(dataSource, "org.cibseven.modeler.repository");
		}

		@Bean
		PlatformTransactionManager transactionManager(
				@Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}

		@Bean
		PlatformTransactionManager appTransactionManager(
				@Qualifier("appEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import(AuditDiagram.class)
	static class SinglePersistenceUnitConfig {

		@Bean
		DataSource dataSource() {
			return embeddedDatabase();
		}

		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
			return factoryFor(dataSource, "org.cibseven.modeler.model");
		}

		@Bean
		PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}
	}

	private static DataSource embeddedDatabase() {
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
	}

	private static LocalContainerEntityManagerFactoryBean factoryFor(DataSource dataSource, String packageToScan) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("hibernate.hbm2ddl.auto", "create-drop");
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setDataSource(dataSource);
		factory.setPackagesToScan(packageToScan);
		factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factory.setJpaPropertyMap(properties);
		return factory;
	}
}
