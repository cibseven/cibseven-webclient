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

import org.cibseven.modeler.config.ModelerPersistenceConfiguration;
import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The listing behind the folder tree. Diagrams and forms come from the two halves of a union, so
 * each half has to filter on its own: a folder that filtered only one of them would show the
 * forms of the whole installation in every folder.
 */
class UnifiedDiagramFolderQueryTest {

	private static final String GENERAL = "00000000-0000-0000-0000-0000000000d1";
	private static final PageRequest FIRST_PAGE = PageRequest.of(0, 10);

	/** Each test gets a database of its own, as they move models around. */
	private ApplicationContextRunner runner(String database) {
		return new ApplicationContextRunner()
			.withPropertyValues("spring.datasource.url=jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.sql.init.mode=always",
				"spring.sql.init.schema-locations=classpath:modeler-folders-before.sql,classpath:modeler-folders-migration.sql",
				"spring.jpa.hibernate.ddl-auto=none")
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class, SqlInitializationAutoConfiguration.class))
			.withUserConfiguration(ModelerPersistenceConfiguration.class);
	}

	@Test
	void listsEveryFolderWhenNoneIsAskedFor() {
		runner("unifiedall").run(context -> {
			ProcessDiagramRepository diagrams = context.getBean(ProcessDiagramRepository.class);

			// Two diagrams and one form, the whole fixture
			assertThat(diagrams.findAllUnified(null, null, null, FIRST_PAGE)).hasSize(3);
		});
	}

	@Test
	void listsOnlyTheDiagramsOfTheFolderAskedFor() {
		runner("unifieddiagram").run(context -> {
			ProcessDiagramRepository diagrams = context.getBean(ProcessDiagramRepository.class);
			String archive = archiveFolder(context.getBean(FolderRepository.class));

			ProcessDiagramEntity moved = diagrams.findById("diagram-2").orElseThrow();
			moved.setFolderId(archive);
			diagrams.save(moved);

			assertThat(diagrams.findAllUnified(null, null, archive, FIRST_PAGE))
				.singleElement()
				.satisfies(only -> assertThat(only.getId()).isEqualTo("diagram-2"));
			assertThat(diagrams.findAllUnified(null, null, GENERAL, FIRST_PAGE)).hasSize(2);
		});
	}

	@Test
	void listsOnlyTheFormsOfTheFolderAskedFor() {
		runner("unifiedform").run(context -> {
			ProcessDiagramRepository diagrams = context.getBean(ProcessDiagramRepository.class);
			FormRepository forms = context.getBean(FormRepository.class);
			String archive = archiveFolder(context.getBean(FolderRepository.class));

			FormEntity moved = forms.findById("form-1").orElseThrow();
			moved.setFolderId(archive);
			forms.save(moved);

			assertThat(diagrams.findAllUnified(null, null, archive, FIRST_PAGE))
				.singleElement()
				.satisfies(only -> assertThat(only.getType()).isEqualTo("form"));
			assertThat(diagrams.findAllUnified(null, null, GENERAL, FIRST_PAGE))
				.hasSize(2)
				.noneMatch(model -> "form".equals(model.getType()));
		});
	}

	/** Searching is not browsing: a keyword looks through the whole tree, not one folder. */
	@Test
	void keepsSearchingAcrossFoldersWhenNoFolderIsGiven() {
		runner("unifiedsearch").run(context -> {
			ProcessDiagramRepository diagrams = context.getBean(ProcessDiagramRepository.class);
			String archive = archiveFolder(context.getBean(FolderRepository.class));

			ProcessDiagramEntity moved = diagrams.findById("diagram-1").orElseThrow();
			moved.setFolderId(archive);
			diagrams.save(moved);

			assertThat(diagrams.findAllUnified("%invoice%", null, null, FIRST_PAGE))
				.extracting(model -> model.getId())
				.contains("diagram-1");
		});
	}

	private String archiveFolder(FolderRepository folders) {
		FolderEntity archive = new FolderEntity();
		archive.setName("Archive");
		return folders.save(archive).getId();
	}
}
