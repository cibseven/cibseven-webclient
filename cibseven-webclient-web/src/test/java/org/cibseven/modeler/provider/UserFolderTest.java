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

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.cibseven.modeler.config.ModelerPersistenceConfiguration;
import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.repository.FolderRepository;
import org.cibseven.webapp.exception.InvalidFolderException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The folder every user gets at the top level. No key covers the top level, so two first visits
 * at once can store it twice; only a real schema shows that the second row does not break the
 * lookups that follow.
 */
class UserFolderTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:userfolder;DB_CLOSE_DELAY=-1",
			"spring.datasource.driver-class-name=org.h2.Driver",
			"spring.sql.init.mode=always",
			"spring.sql.init.schema-locations=classpath:modeler-folders-before.sql,classpath:modeler-folders-migration.sql",
			"spring.jpa.hibernate.ddl-auto=none")
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class, DataSourceInitializationAutoConfiguration.class,
			TransactionAutoConfiguration.class))
		.withUserConfiguration(ModelerPersistenceConfiguration.class, FolderProvider.class);

	@Test
	void createsTheUserFolderOnceAndFindsItAfterwards() {
		runner.run(context -> {
			FolderProvider folders = context.getBean(FolderProvider.class);

			FolderEntity first = folders.userFolder("anna");
			FolderEntity second = folders.userFolder("anna");

			assertThat(second.getId()).isEqualTo(first.getId());
			assertThat(second.isUserFolder()).isTrue();
		});
	}

	@Test
	void usesTheOldestWhenAUserFolderWasStoredTwice() {
		runner.run(context -> {
			FolderProvider folders = context.getBean(FolderProvider.class);
			FolderRepository repository = context.getBean(FolderRepository.class);
			LocalDateTime now = LocalDateTime.now();
			FolderEntity oldest = repository.save(userFolder("bert", now.minusMinutes(1)));
			repository.save(userFolder("bert", now));

			assertThat(folders.userFolder("bert").getId()).isEqualTo(oldest.getId());
			// The name is still taken, and reported as such rather than failing on the second row
			assertThatThrownBy(() -> folders.create(null, "bert", "someone-else"))
				.isInstanceOf(InvalidFolderException.class);
		});
	}

	private static FolderEntity userFolder(String user, LocalDateTime created) {
		FolderEntity folder = new FolderEntity();
		folder.setName(user);
		folder.setCreatedBy(user);
		folder.setCreated(Timestamp.valueOf(created));
		return folder;
	}
}
