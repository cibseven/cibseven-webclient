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
import org.cibseven.modeler.provider.FolderProvider.FolderChange;
import org.cibseven.webapp.exception.InvalidFolderException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A rename and a move arrive in one request. Applied one after the other they would each commit
 * on their own, so a move the tree refuses would leave the folder renamed and the caller looking
 * at an error for a change that partly happened.
 */
class FolderUpdateAtomicityTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:folderupdate;DB_CLOSE_DELAY=-1",
			"spring.datasource.driver-class-name=org.h2.Driver",
			"spring.sql.init.mode=always",
			"spring.sql.init.schema-locations=classpath:modeler-folders-before.sql,classpath:modeler-folders-migration.sql",
			"spring.jpa.hibernate.ddl-auto=none")
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class, SqlInitializationAutoConfiguration.class,
			TransactionAutoConfiguration.class))
		.withUserConfiguration(ModelerPersistenceConfiguration.class, FolderProvider.class);

	@Test
	void appliesARenameAndAMoveTogether() {
		runner.run(context -> {
			FolderProvider folders = context.getBean(FolderProvider.class);
			FolderEntity target = folders.create(null, "Archive", "tester");
			FolderEntity folder = folders.create(null, "Drafts", "tester");

			FolderEntity updated = folders.update(folder.getId(),
				new FolderChange(true, "Old drafts", true, target.getId()), "tester");

			assertThat(updated.getName()).isEqualTo("Old drafts");
			assertThat(updated.getParentId()).isEqualTo(target.getId());
		});
	}

	@Test
	void leavesTheNameAloneWhenTheMoveIsRefused() {
		runner.run(context -> {
			FolderProvider folders = context.getBean(FolderProvider.class);
			FolderEntity target = folders.create(null, "Invoicing", "tester");
			// The name the folder is renamed to is already taken where it is moved
			folders.create(target.getId(), "Billing", "tester");
			// Names of its own: both tests run against the same in-memory database
			FolderEntity folder = folders.create(null, "Sketches", "tester");

			assertThatThrownBy(() -> folders.update(folder.getId(),
					new FolderChange(true, "Billing", true, target.getId()), "tester"))
				.isInstanceOf(InvalidFolderException.class);

			FolderEntity unchanged = folders.find(folder.getId());
			assertThat(unchanged.getName()).isEqualTo("Sketches");
			assertThat(unchanged.getParentId()).isNull();
		});
	}
}
