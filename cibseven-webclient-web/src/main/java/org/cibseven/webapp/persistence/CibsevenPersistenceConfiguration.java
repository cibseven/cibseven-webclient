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
package org.cibseven.webapp.persistence;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

/**
 * Gives the webclient its own persistence unit, separate from whatever JPA setup the surrounding
 * application has.
 *
 * <p>Neither bean is {@code @Primary} and neither uses the default names {@code entityManagerFactory}
 * or {@code transactionManager}, so an embedding application keeps full control of its own beans and
 * the webclient cannot be attached to the host's persistence unit by accident. Webclient entities
 * stay out of the host's unit as well, because this factory scans them rather than {@code @EntityScan}
 * contributing them to the auto-configured factory.</p>
 *
 * <p>A feature that stores data imports this configuration, which is what brings the unit up: there
 * is no unit without a feature to fill it. It adds its entity packages through
 * {@link CibsevenEntityPackages} and points its own {@code @EnableJpaRepositories} at these beans
 * instead of opening a second unit.</p>
 *
 * <p>Both beans are declared {@code defaultCandidate = false}: they are reachable only by the names
 * above, never by type. That keeps them out of the host application's autowiring, and it stops them
 * from suppressing Spring Boot's own {@code entityManagerFactory} and {@code transactionManager},
 * whose {@code @ConditionalOnMissingBean} ignores beans that are not default candidates.</p>
 */
@Configuration(proxyBeanMethods = false)
public class CibsevenPersistenceConfiguration {

	/**
	 * @param cibsevenDataSource a data source dedicated to the webclient, if the application defines
	 *        one under {@link CibsevenJpa#DATA_SOURCE} or its former name
	 * @param dataSource the application's data source, used when there is no dedicated one
	 */
	@Bean(name = { CibsevenJpa.ENTITY_MANAGER_FACTORY, CibsevenJpa.LEGACY_ENTITY_MANAGER_FACTORY },
			defaultCandidate = false)
	LocalContainerEntityManagerFactoryBean cibsevenEntityManagerFactory(
			EntityManagerFactoryBuilder builder,
			@Qualifier(CibsevenJpa.DATA_SOURCE) ObjectProvider<DataSource> cibsevenDataSource,
			@Qualifier(CibsevenJpa.LEGACY_DATA_SOURCE) ObjectProvider<DataSource> modelerDataSource,
			ObjectProvider<DataSource> dataSource,
			ObjectProvider<CibsevenEntityPackages> contributedPackages) {
		Set<String> packages = new LinkedHashSet<>();
		contributedPackages.forEach(contributor -> packages.addAll(contributor.packages()));
		return builder
			.dataSource(cibsevenDataSource.getIfAvailable(
				() -> modelerDataSource.getIfAvailable(dataSource::getObject)))
			.packages(packages.toArray(String[]::new))
			.persistenceUnit(CibsevenJpa.PERSISTENCE_UNIT)
			.build();
	}

	@Bean(name = { CibsevenJpa.TRANSACTION_MANAGER, CibsevenJpa.LEGACY_TRANSACTION_MANAGER },
			defaultCandidate = false)
	PlatformTransactionManager cibsevenTransactionManager(
			@Qualifier(CibsevenJpa.ENTITY_MANAGER_FACTORY) EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}
