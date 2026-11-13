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
import java.util.Map;

import javax.sql.DataSource;

import org.cibseven.modeler.config.ModelerPersistence;
import org.cibseven.modeler.model.FolderEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The setup of the run distribution with the direct provider (CIB7-2712): the webclient's unit and
 * the engine share one data source, and the engine runs under a second {@link JpaTransactionManager},
 * Spring Boot's {@code transactionManager}. The engine is called inside webclient transactions,
 * for instance by the modeler's access check.
 */
class CibsevenTransactionManagerTest {

	private EmbeddedDatabase dataSource;
	private LocalContainerEntityManagerFactoryBean webclientUnit;
	private LocalContainerEntityManagerFactoryBean engineUnit;
	private PlatformTransactionManager engineTransactions;

	@BeforeEach
	void setUp() {
		dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
		webclientUnit = unit(dataSource, ModelerPersistence.ENTITY_PACKAGE);
		// Stands in for Boot's unit, which maps no webclient entities
		engineUnit = unit(dataSource, ModelerPersistence.REPOSITORY_PACKAGE);
		engineTransactions = new JpaTransactionManager(engineUnit.getObject());
	}

	@AfterEach
	void tearDown() {
		webclientUnit.destroy();
		engineUnit.destroy();
		dataSource.shutdown();
	}

	@Test
	void theEngineRunsInsideAWebclientTransaction() {
		TransactionTemplate webclient = new TransactionTemplate(new CibsevenTransactionManager(webclientUnit.getObject()));

		Object engineResult = webclient.execute(status -> new TransactionTemplate(engineTransactions)
			.execute(inner -> selectOne(engineUnit.getObject())));

		assertThat(engineResult).isEqualTo(1);
	}

	/** The cause of CIB7-2712, kept so the test above cannot pass for another reason. */
	@Test
	void aPlainJpaTransactionManagerMakesTheEngineFail() {
		TransactionTemplate webclient = new TransactionTemplate(new JpaTransactionManager(webclientUnit.getObject()));

		assertThrows(IllegalTransactionStateException.class, () -> webclient.execute(status ->
			new TransactionTemplate(engineTransactions).execute(inner -> selectOne(engineUnit.getObject()))));
	}

	@Test
	void theWebclientRunsInsideAnEngineTransaction() {
		TransactionTemplate webclient = new TransactionTemplate(new CibsevenTransactionManager(webclientUnit.getObject()));

		Object webclientResult = new TransactionTemplate(engineTransactions)
			.execute(status -> webclient.execute(inner -> selectOne(webclientUnit.getObject())));

		assertThat(webclientResult).isEqualTo(1);
	}

	@Test
	void theWebclientsOwnWorkIsCommittedOrRolledBackAsAWhole() {
		TransactionTemplate webclient = new TransactionTemplate(new CibsevenTransactionManager(webclientUnit.getObject()));

		webclient.executeWithoutResult(status -> {
			persistFolder("kept");
			new TransactionTemplate(engineTransactions).execute(inner -> selectOne(engineUnit.getObject()));
		});
		webclient.executeWithoutResult(status -> {
			persistFolder("dropped");
			new TransactionTemplate(engineTransactions).execute(inner -> selectOne(engineUnit.getObject()));
			status.setRollbackOnly();
		});

		List<String> names = webclient.execute(status -> webclientEntityManager()
			.createQuery("select f.name from FolderEntity f", String.class).getResultList());
		assertThat(names).containsExactly("kept");
	}

	private void persistFolder(String name) {
		FolderEntity folder = new FolderEntity();
		folder.setName(name);
		webclientEntityManager().persist(folder);
	}

	private EntityManager webclientEntityManager() {
		return EntityManagerFactoryUtils.getTransactionalEntityManager(webclientUnit.getObject());
	}

	private static Object selectOne(EntityManagerFactory unit) {
		return EntityManagerFactoryUtils.getTransactionalEntityManager(unit).createNativeQuery("select 1").getSingleResult();
	}

	private static LocalContainerEntityManagerFactoryBean unit(DataSource dataSource, String packageToScan) {
		LocalContainerEntityManagerFactoryBean unit = new LocalContainerEntityManagerFactoryBean();
		unit.setDataSource(dataSource);
		unit.setPackagesToScan(packageToScan);
		unit.setPersistenceUnitName(packageToScan);
		unit.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		unit.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
		unit.afterPropertiesSet();
		return unit;
	}
}
