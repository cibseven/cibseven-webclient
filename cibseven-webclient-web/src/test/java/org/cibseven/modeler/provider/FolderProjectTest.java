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
package org.cibseven.modeler.provider;

import javax.sql.DataSource;

import org.cibseven.modeler.config.ModelerPersistenceConfiguration;
import org.cibseven.modeler.repository.FolderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The project kept on every folder, which the lists filter on. The subtree is rewritten in one
 * statement past the entities in memory, so only a real database shows that it sticks.
 */
class FolderProjectTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:folderproject;DB_CLOSE_DELAY=-1",
			"spring.datasource.driver-class-name=org.h2.Driver",
			"spring.sql.init.mode=always",
			"spring.sql.init.schema-locations=classpath:modeler-folders-before.sql,classpath:modeler-folders-migration.sql",
			"spring.jpa.hibernate.ddl-auto=none")
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class, DataSourceInitializationAutoConfiguration.class,
			TransactionAutoConfiguration.class))
		.withUserConfiguration(ModelerPersistenceConfiguration.class, FolderProvider.class, OpenProjectAccess.class);

	@Test
	void keepsTheProjectOfEveryFolderThroughCreatingAndMoving() {
		runner.run(context -> {
			FolderProvider folders = context.getBean(FolderProvider.class);
			String claims = folders.create(null, "Claims", "anna").getId();
			String drafts = folders.create(claims, "Drafts", "anna").getId();
			String old = folders.create(drafts, "Old", "anna").getId();
			String archive = folders.create(null, "Archive", "anna").getId();

			assertThat(projectOf(context, claims)).isEqualTo(claims);
			assertThat(projectOf(context, old)).isEqualTo(claims);
			assertThat(folders.projectIds()).contains(claims, archive).doesNotContain(drafts, old);

			folders.move(drafts, archive, "anna");
			assertThat(projectOf(context, drafts)).isEqualTo(archive);
			assertThat(projectOf(context, old)).isEqualTo(archive);

			// Taken out to the top level, it is a project of its own with everything below it
			folders.move(drafts, null, "anna");
			assertThat(projectOf(context, drafts)).isEqualTo(drafts);
			assertThat(projectOf(context, old)).isEqualTo(drafts);
		});
	}

	/** Folders from before the project was kept on them, or with a stale one, are put right. */
	@Test
	void storesTheProjectOfFoldersThatHaveNone() {
		runner.run(context -> {
			FolderProvider folders = context.getBean(FolderProvider.class);
			String sales = folders.create(null, "Sales", "bert").getId();
			String leads = folders.create(sales, "Leads", "bert").getId();
			JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
			jdbc.update("UPDATE MOD_FOLDERS SET PROJECT_ID = NULL WHERE ID = ?", sales);
			jdbc.update("UPDATE MOD_FOLDERS SET PROJECT_ID = 'stale' WHERE ID = ?", leads);

			assertThat(folders.fillProjects()).isEqualTo(2);

			assertThat(projectOf(context, sales)).isEqualTo(sales);
			assertThat(projectOf(context, leads)).isEqualTo(sales);
			assertThat(folders.fillProjects()).isZero();
		});
	}

	private static String projectOf(ApplicationContext context, String folderId) {
		return context.getBean(FolderRepository.class).findById(folderId).orElseThrow().getProjectId();
	}
}
