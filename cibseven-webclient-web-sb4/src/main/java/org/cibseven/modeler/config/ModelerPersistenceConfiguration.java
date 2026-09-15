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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

/**
 * Gives the modeler its own persistence unit, separate from whatever JPA setup the surrounding
 * application has.
 *
 * <p>Neither bean is {@code @Primary} and neither uses the default names {@code entityManagerFactory}
 * or {@code transactionManager}, so an embedding application keeps full control of its own beans and
 * the modeler cannot be attached to the host's persistence unit by accident. Modeler entities stay
 * out of the host's unit as well, because this factory scans them rather than {@code @EntityScan}
 * contributing them to the auto-configured factory.</p>
 *
 * <p>Features that keep their data next to the modeler's, such as the enterprise chat, add their
 * entity packages through {@link ModelerEntityPackages} and share this unit and its transaction
 * manager instead of opening a second one.</p>
 *
 * <p>Both beans are declared {@code defaultCandidate = false}: they are reachable only by the names
 * above, never by type. That keeps them out of the host application's autowiring, and it stops them
 * from suppressing Spring Boot's own {@code entityManagerFactory} and {@code transactionManager},
 * whose {@code @ConditionalOnMissingBean} ignores beans that are not default candidates.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
	basePackages = ModelerJpa.REPOSITORY_PACKAGE,
	entityManagerFactoryRef = ModelerJpa.ENTITY_MANAGER_FACTORY,
	transactionManagerRef = ModelerJpa.TRANSACTION_MANAGER)
public class ModelerPersistenceConfiguration {

	/**
	 * @param modelerDataSource a data source dedicated to the modeler, if the application defines
	 *        one under {@link ModelerJpa#DATA_SOURCE}
	 * @param dataSource the application's data source, used when there is no dedicated one
	 */
	@Bean(name = ModelerJpa.ENTITY_MANAGER_FACTORY, defaultCandidate = false)
	LocalContainerEntityManagerFactoryBean modelerEntityManagerFactory(
			EntityManagerFactoryBuilder builder,
			@Qualifier(ModelerJpa.DATA_SOURCE) ObjectProvider<DataSource> modelerDataSource,
			ObjectProvider<DataSource> dataSource,
			ObjectProvider<ModelerEntityPackages> contributedPackages) {
		Set<String> packages = new LinkedHashSet<>();
		packages.add(ModelerJpa.ENTITY_PACKAGE);
		contributedPackages.forEach(contributor -> packages.addAll(contributor.packages()));
		return builder
			.dataSource(modelerDataSource.getIfAvailable(dataSource::getObject))
			.packages(packages.toArray(String[]::new))
			.persistenceUnit(ModelerJpa.PERSISTENCE_UNIT)
			.build();
	}

	@Bean(name = ModelerJpa.TRANSACTION_MANAGER, defaultCandidate = false)
	PlatformTransactionManager modelerTransactionManager(
			@Qualifier(ModelerJpa.ENTITY_MANAGER_FACTORY) EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}
