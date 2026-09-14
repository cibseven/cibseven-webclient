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
import org.cibseven.modeler.model.ModelSource;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The upgrade of an installation that has no folders yet: it runs the shipped statements against
 * the tables as they are today, so what the distributions will execute is exercised here, not a
 * schema generated from the entities.
 */
class FolderMigrationTest {

	private static final String GENERAL = "00000000-0000-0000-0000-0000000000d2";

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:foldermigration;DB_CLOSE_DELAY=-1",
			"spring.datasource.driver-class-name=org.h2.Driver",
			"spring.sql.init.mode=always",
			"spring.sql.init.schema-locations=classpath:modeler-folders-before.sql,classpath:modeler-folders-migration.sql",
			"spring.jpa.hibernate.ddl-auto=none")
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class, SqlInitializationAutoConfiguration.class))
		.withUserConfiguration(ModelerPersistenceConfiguration.class);

	@Test
	void putsTheModelsOfAFlatInstallationInOneFolder() {
		runner.run(context -> {
			assertThat(context).hasNotFailed();
			ProcessDiagramRepository diagrams = context.getBean(ProcessDiagramRepository.class);
			FormRepository forms = context.getBean(FormRepository.class);

			// Nothing to sort by hand: every model that was there is filed
			assertThat(diagrams.findAll()).hasSize(2).allMatch(d -> GENERAL.equals(d.getFolderId()));
			assertThat(forms.findAll()).hasSize(1).allMatch(f -> GENERAL.equals(f.getFolderId()));
		});
	}

	@Test
	void keepsTheIdentifiersTheEngineAndTheLinksResolve() {
		runner.run(context -> {
			ProcessDiagramRepository diagrams = context.getBean(ProcessDiagramRepository.class);

			ProcessDiagramEntity invoice = diagrams.findById("diagram-1").orElseThrow();

			assertThat(invoice.getProcesskey()).isEqualTo("invoice");
			assertThat(invoice.getVersion()).isEqualTo(1);
		});
	}

	@Test
	void createsTheFolderAtTheTopLevelOfTheDatabaseSource() {
		runner.run(context -> {
			FolderRepository folders = context.getBean(FolderRepository.class);

			FolderEntity general = folders
				.findBySourceAndParentIdIsNullAndName(ModelSource.DATABASE, "General").orElseThrow();

			assertThat(general.getId()).isEqualTo(GENERAL);
			assertThat(general.getParentId()).isNull();
			// Nothing above it: the source is not a folder of its own
			assertThat(folders.findAll()).hasSize(1);
		});
	}

	/** Running it twice is what an operator does after a failed upgrade, and what a retry does. */
	@Test
	void readsTheFoldersThroughTheShippedColumns() {
		runner.run(context -> {
			FolderRepository folders = context.getBean(FolderRepository.class);
			FormRepository forms = context.getBean(FormRepository.class);

			FormEntity form = forms.findById("form-1").orElseThrow();

			assertThat(folders.findById(form.getFolderId())).isPresent();
		});
	}
}
