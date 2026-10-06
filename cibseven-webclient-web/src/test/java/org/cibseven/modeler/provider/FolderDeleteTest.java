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

import org.cibseven.modeler.config.ModelerPersistenceConfiguration;
import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.repository.FolderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deleting a folder that holds other folders. A folder row points at its parent, so the rows have
 * to go deepest first; taken in the order the subtree is walked, the database refuses the delete
 * with a referential integrity violation. Only a real schema shows that, which is why this lives
 * here rather than next to the provider, where the repository is a mock that enforces nothing.
 */
class FolderDeleteTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:folderdelete;DB_CLOSE_DELAY=-1",
			"spring.datasource.driver-class-name=org.h2.Driver",
			"spring.sql.init.mode=always",
			"spring.sql.init.schema-locations=classpath:modeler-folders-before.sql,classpath:modeler-folders-migration.sql",
			"spring.jpa.hibernate.ddl-auto=none")
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class, SqlInitializationAutoConfiguration.class,
			TransactionAutoConfiguration.class))
		.withUserConfiguration(ModelerPersistenceConfiguration.class, FolderProvider.class);

	@Test
	void removesAFolderWithTheFoldersBelowIt() {
		runner.run(context -> {
			FolderProvider folders = context.getBean(FolderProvider.class);
			FolderRepository repository = context.getBean(FolderRepository.class);

			FolderEntity root = folders.create(null, "Projects", "tester");
			FolderEntity child = folders.create(root.getId(), "Invoicing", "tester");
			FolderEntity grandchild = folders.create(child.getId(), "Drafts", "tester");
			folders.create(root.getId(), "Archive", "tester");

			FolderProvider.FolderContents removed = folders.delete(root.getId());

			assertThat(removed.folders()).isEqualTo(3);
			assertThat(repository.findById(root.getId())).isEmpty();
			assertThat(repository.findById(child.getId())).isEmpty();
			assertThat(repository.findById(grandchild.getId())).isEmpty();
		});
	}

	@Test
	void leavesTheFoldersBesideItAlone() {
		runner.run(context -> {
			FolderProvider folders = context.getBean(FolderProvider.class);
			FolderRepository repository = context.getBean(FolderRepository.class);

			FolderEntity removed = folders.create(null, "Going", "tester");
			folders.create(removed.getId(), "Below", "tester");
			FolderEntity kept = folders.create(null, "Staying", "tester");

			folders.delete(removed.getId());

			assertThat(repository.findById(kept.getId())).isPresent();
		});
	}
}
