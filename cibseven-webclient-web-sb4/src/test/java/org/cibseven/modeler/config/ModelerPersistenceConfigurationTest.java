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

import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.repository.ProcessDiagramRepository;
import org.cibseven.persistence.CibsevenJpa;
import org.junit.jupiter.api.Test;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
}
