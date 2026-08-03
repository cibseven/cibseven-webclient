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

import org.cibseven.modeler.config.ModelerJpa;
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
 * The modeler runs on its own persistence unit, named after {@link ModelerJpa}, and must never
 * attach itself to the host application's beans — in an embedding application those are the ones
 * called {@code entityManagerFactory} and {@code transactionManager}, and they may well point at a
 * different database (CIB7-1776).
 *
 * <p>The transaction manager assertions go through {@code BeanFactoryAnnotationUtils.qualifiedBeanOfType},
 * which is the lookup {@code TransactionAspectSupport.determineQualifiedTransactionManager} performs
 * for a {@code @Transactional(ModelerJpa.TRANSACTION_MANAGER)} qualifier.</p>
 */
class AuditDiagramFactoryResolutionTest {

	/** An embedding application: it owns the default names, the modeler owns its own. */
	private final ApplicationContextRunner embedded =
			new ApplicationContextRunner().withUserConfiguration(HostAndModelerUnitsConfig.class);

	/** A host that named its beans differently, so no bean is called {@code entityManagerFactory}. */
	private final ApplicationContextRunner noDefaultNames =
			new ApplicationContextRunner().withUserConfiguration(ModelerUnitOnlyConfig.class);

	@Test
	void auditDiagramResolvesTheModelersFactoryNotTheHosts() {
		embedded.run(context -> {
			assertThat(context).hasNotFailed();
			EntityManager entityManager = entityManagerOf(context.getBean(AuditDiagram.class));

			assertThat(entityManager.getEntityManagerFactory())
					.isSameAs(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class))
					.isNotSameAs(context.getBean("entityManagerFactory", EntityManagerFactory.class));
		});
	}

	@Test
	void modelerEntitiesLiveInTheModelersUnitOnly() {
		embedded.run(context -> {
			assertThat(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class)
					.getMetamodel().getEntities())
					.anyMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()));
			assertThat(context.getBean("entityManagerFactory", EntityManagerFactory.class)
					.getMetamodel().getEntities())
					.noneMatch(entity -> ProcessDiagramEntity.class.equals(entity.getJavaType()));
		});
	}

	@Test
	void transactionalQualifierResolvesToTheModelersTransactionManager() {
		embedded.run(context -> {
			TransactionManager resolved = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
					context.getBeanFactory(), TransactionManager.class, ModelerJpa.TRANSACTION_MANAGER);

			assertThat(((JpaTransactionManager) resolved).getEntityManagerFactory())
					.isSameAs(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class))
					.isNotSameAs(context.getBean("entityManagerFactory", EntityManagerFactory.class));
		});
	}

	/** The host's own beans keep working: the modeler claims nothing of theirs. */
	@Test
	void hostBeansAreUntouched() {
		embedded.run(context -> {
			assertThat(context.getBeanNamesForType(EntityManagerFactory.class))
					.containsExactlyInAnyOrder("entityManagerFactory", ModelerJpa.ENTITY_MANAGER_FACTORY);
			assertThat(context.getBeanNamesForType(PlatformTransactionManager.class))
					.containsExactlyInAnyOrder("transactionManager", ModelerJpa.TRANSACTION_MANAGER);
		});
	}

	/** A host with no bean called {@code entityManagerFactory} at all must still work. */
	@Test
	void worksWithoutAnyDefaultlyNamedBeans() {
		noDefaultNames.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(entityManagerOf(context.getBean(AuditDiagram.class)).getEntityManagerFactory())
					.isSameAs(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class));

			TransactionManager resolved = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
					context.getBeanFactory(), TransactionManager.class, ModelerJpa.TRANSACTION_MANAGER);
			assertThat(((JpaTransactionManager) resolved).getEntityManagerFactory())
					.isSameAs(context.getBean(ModelerJpa.ENTITY_MANAGER_FACTORY, EntityManagerFactory.class));
		});
	}

	private static EntityManager entityManagerOf(AuditDiagram auditDiagram) {
		return (EntityManager) ReflectionTestUtils.getField(auditDiagram, "entityManager");
	}

	@Configuration(proxyBeanMethods = false)
	@Import(AuditDiagram.class)
	static class HostAndModelerUnitsConfig {

		@Bean
		DataSource modelerDataSource() {
			return embeddedDatabase();
		}

		@Bean
		DataSource appDataSource() {
			return embeddedDatabase();
		}

		@Bean(ModelerJpa.ENTITY_MANAGER_FACTORY)
		LocalContainerEntityManagerFactoryBean modelerEntityManagerFactory(
				@Qualifier("modelerDataSource") DataSource dataSource) {
			return factoryFor(dataSource, ModelerJpa.ENTITY_PACKAGE);
		}

		/** Stands in for the host application's factory; deliberately has no modeler entities. */
		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactory(
				@Qualifier("appDataSource") DataSource dataSource) {
			return factoryFor(dataSource, ModelerJpa.REPOSITORY_PACKAGE);
		}

		@Bean(ModelerJpa.TRANSACTION_MANAGER)
		PlatformTransactionManager modelerTransactionManager(
				@Qualifier(ModelerJpa.ENTITY_MANAGER_FACTORY) EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}

		@Bean
		PlatformTransactionManager transactionManager(
				@Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import(AuditDiagram.class)
	static class ModelerUnitOnlyConfig {

		@Bean
		DataSource dataSource() {
			return embeddedDatabase();
		}

		@Bean(ModelerJpa.ENTITY_MANAGER_FACTORY)
		LocalContainerEntityManagerFactoryBean modelerEntityManagerFactory(DataSource dataSource) {
			return factoryFor(dataSource, ModelerJpa.ENTITY_PACKAGE);
		}

		@Bean(ModelerJpa.TRANSACTION_MANAGER)
		PlatformTransactionManager modelerTransactionManager(
				@Qualifier(ModelerJpa.ENTITY_MANAGER_FACTORY) EntityManagerFactory entityManagerFactory) {
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
