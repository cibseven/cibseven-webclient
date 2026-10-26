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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.cibseven.modeler.config.ModelerPersistence;
import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.model.ProcessDiagramReduce;
import org.cibseven.modeler.model.UnifiedDiagram;
import org.cibseven.modeler.provider.ProjectGuard;
import org.cibseven.persistence.CibsevenJpa;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The lists leave out the folders of hidden projects inside the query, so paging counts only what
 * the user sees. Run against a database, as the point is that the NOT IN lists are valid SQL.
 */
class HiddenFolderQueryTest {

	private final ApplicationContextRunner modeler = new ApplicationContextRunner().withUserConfiguration(Config.class);

	@Test
	void leavesTheModelsOfHiddenFoldersOutOfEveryList() {
		modeler.run(context -> {
			ProcessDiagramRepository diagrams = context.getBean(ProcessDiagramRepository.class);
			FormRepository forms = context.getBean(FormRepository.class);
			diagrams.save(diagram("invoice", "claims"));
			diagrams.save(diagram("order", "sales"));
			diagrams.save(diagram("legacy", null));
			forms.save(form("invoice-form", "claims"));
			forms.save(form("order-form", "sales"));

			Collection<String> hidden = List.of("claims");
			PageRequest page = PageRequest.of(0, 20);

			assertThat(diagrams.findAllUnified(null, null, null, hidden, page))
				.extracting(UnifiedDiagram::getProcesskey).containsExactlyInAnyOrder("order", "legacy", "order-form");
			assertThat(diagrams.findAllFiltered("", "", hidden, page))
				.extracting(ProcessDiagramReduce::getProcesskey).containsExactlyInAnyOrder("order", "legacy");
			assertThat(forms.findAllFiltered("", hidden, page))
				.extracting(FormEntity::getFormId).containsExactly("order-form");
		});
	}

	/** Nothing hidden is the stand-in, which matches no folder. */
	@Test
	void listsEverythingWhenNothingIsHidden() {
		modeler.run(context -> {
			ProcessDiagramRepository diagrams = context.getBean(ProcessDiagramRepository.class);
			FormRepository forms = context.getBean(FormRepository.class);
			diagrams.save(diagram("invoice", "claims"));
			forms.save(form("invoice-form", "claims"));
			Collection<String> none = ProjectGuard.notEmpty(List.of());

			assertThat(diagrams.findAllUnified(null, null, null, none, PageRequest.of(0, 20))).hasSize(2);
			assertThat(forms.findAllFiltered("", none, PageRequest.of(0, 20))).hasSize(1);
		});
	}

	private static ProcessDiagramEntity diagram(String key, String folderId) {
		ProcessDiagramEntity diagram = new ProcessDiagramEntity();
		diagram.setName(key);
		diagram.setProcesskey(key);
		diagram.setType("bpmn-c7");
		diagram.setActive(true);
		diagram.setDiagram("<bpmn/>".getBytes());
		diagram.setFolderId(folderId);
		return diagram;
	}

	private static FormEntity form(String formId, String folderId) {
		FormEntity form = new FormEntity();
		form.setFormId(formId);
		form.setActive(true);
		form.setFormSchema("{}".getBytes());
		form.setFolderId(folderId);
		return form;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableJpaRepositories(
		basePackages = ModelerPersistence.REPOSITORY_PACKAGE,
		entityManagerFactoryRef = CibsevenJpa.ENTITY_MANAGER_FACTORY,
		transactionManagerRef = CibsevenJpa.TRANSACTION_MANAGER)
	static class Config {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
		}

		@Bean(CibsevenJpa.ENTITY_MANAGER_FACTORY)
		LocalContainerEntityManagerFactoryBean modelerEntityManagerFactory(DataSource dataSource) {
			Map<String, Object> properties = new HashMap<>();
			properties.put("hibernate.hbm2ddl.auto", "create-drop");
			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setDataSource(dataSource);
			factory.setPackagesToScan(ModelerPersistence.ENTITY_PACKAGE);
			factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
			factory.setJpaPropertyMap(properties);
			return factory;
		}

		@Bean(CibsevenJpa.TRANSACTION_MANAGER)
		PlatformTransactionManager modelerTransactionManager(
				@Qualifier(CibsevenJpa.ENTITY_MANAGER_FACTORY) EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}
	}
}
