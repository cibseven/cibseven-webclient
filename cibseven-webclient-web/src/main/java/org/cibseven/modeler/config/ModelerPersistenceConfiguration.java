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

import org.cibseven.persistence.CibsevenEntityPackages;
import org.cibseven.persistence.CibsevenJpa;
import org.cibseven.persistence.CibsevenPersistenceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * The modeler's share of the webclient's persistence unit: its entities go into the unit, and its
 * repositories are bound to that unit's entity manager and transaction manager.
 *
 * <p>The unit itself belongs to the webclient rather than to the modeler, see
 * {@link CibsevenPersistenceConfiguration}, and every feature that stores data joins it the same
 * way this one does.</p>
 */
@Configuration(proxyBeanMethods = false)
@Import(CibsevenPersistenceConfiguration.class)
@EnableJpaRepositories(
	basePackages = ModelerPersistence.REPOSITORY_PACKAGE,
	entityManagerFactoryRef = CibsevenJpa.ENTITY_MANAGER_FACTORY,
	transactionManagerRef = CibsevenJpa.TRANSACTION_MANAGER)
public class ModelerPersistenceConfiguration {

	@Bean
	CibsevenEntityPackages modelerEntityPackages() {
		return () -> List.of(ModelerPersistence.ENTITY_PACKAGE);
	}
}
