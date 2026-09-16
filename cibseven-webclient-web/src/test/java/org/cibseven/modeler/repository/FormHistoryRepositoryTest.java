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

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.cibseven.modeler.config.ModelerPersistenceConfiguration;
import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.model.FormSnapshotEntity;
import org.cibseven.modeler.model.ModRevInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The form history is written into tables the installation already has, so the mapping is exercised
 * against their shipped definition rather than against a schema generated from the entities - a
 * wrong column name has to fail here.
 */
class FormHistoryRepositoryTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:formhistory;DB_CLOSE_DELAY=-1",
			"spring.datasource.driver-class-name=org.h2.Driver",
			"spring.sql.init.mode=always",
			"spring.sql.init.schema-locations=classpath:modeler-history-schema.sql",
			"spring.jpa.hibernate.ddl-auto=none")
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class, SqlInitializationAutoConfiguration.class))
		.withUserConfiguration(ModelerPersistenceConfiguration.class);

	@Test
	void writesAndReadsASnapshotThroughTheShippedColumns() {
		runner.run(context -> {
			assertThat(context).hasNotFailed();
			FormSnapshotRepository snapshots = context.getBean(FormSnapshotRepository.class);

			long revision = nextRevision(context);
			snapshots.save(FormSnapshotEntity.added(form("f1", 1, "one"), revision));

			List<FormSnapshotEntity> history = snapshots.findHistory("f1", PageRequest.of(0, 10));

			assertThat(history).singleElement().satisfies(snapshot -> {
				assertThat(snapshot.getRev()).isEqualTo(revision);
				assertThat(snapshot.getRevtype()).isEqualTo(FormSnapshotEntity.REVTYPE_ADD);
				assertThat(snapshot.getFormId()).isEqualTo("form-f1");
				assertThat(snapshot.getDescription()).isEqualTo("a form");
				assertThat(snapshot.getVersion()).isEqualTo(1);
				assertThat(snapshot.getSchemaMod()).isTrue();
				assertThat(snapshot.getUpdatedBy()).isEqualTo("tester");
				assertThat(new String(snapshot.getFormSchema(), StandardCharsets.UTF_8)).isEqualTo("one");
			});
		});
	}

	/** A save that only touched the metadata is no version to go back to, so it stays out. */
	@Test
	void reportsOnlyTheSavesThatChangedTheSchema() {
		runner.run(context -> {
			FormSnapshotRepository snapshots = context.getBean(FormSnapshotRepository.class);

			snapshots.save(FormSnapshotEntity.added(form("f2", 1, "first"), nextRevision(context)));
			snapshots.save(FormSnapshotEntity.modified(form("f2", 1, "first"), nextRevision(context), false));
			snapshots.save(FormSnapshotEntity.modified(form("f2", 2, "second"), nextRevision(context), true));

			assertThat(snapshots.findHistory("f2", PageRequest.of(0, 10)))
				.extracting(FormSnapshotEntity::getVersion)
				.containsExactly(2, 1);
		});
	}

	/** Newest first, and never more than the endpoint asked for. */
	@Test
	void reportsTheNewestFirstAndHonoursTheLimit() {
		runner.run(context -> {
			FormSnapshotRepository snapshots = context.getBean(FormSnapshotRepository.class);

			for (int version = 1; version <= 4; version++) {
				snapshots.save(FormSnapshotEntity.modified(
					form("f3", version, "v" + version), nextRevision(context), true));
			}

			assertThat(snapshots.findHistory("f3", PageRequest.of(0, 2)))
				.extracting(FormSnapshotEntity::getVersion)
				.containsExactly(4, 3);
		});
	}

	@Test
	void keepsTheHistoriesOfDifferentFormsApart() {
		runner.run(context -> {
			FormSnapshotRepository snapshots = context.getBean(FormSnapshotRepository.class);

			snapshots.save(FormSnapshotEntity.added(form("f4", 1, "mine"), nextRevision(context)));
			snapshots.save(FormSnapshotEntity.added(form("f5", 1, "yours"), nextRevision(context)));

			assertThat(snapshots.findHistory("f4", PageRequest.of(0, 10)))
				.extracting(FormSnapshotEntity::getId).containsExactly("f4");
		});
	}

	/** Snapshots of a form and of a diagram share MOD_REVINFO but not their history. */
	@Test
	void keepsFormHistoryOutOfTheDiagramHistory() {
		runner.run(context -> {
			FormSnapshotRepository snapshots = context.getBean(FormSnapshotRepository.class);
			ProcessDiagramSnapshotRepository diagrams = context.getBean(ProcessDiagramSnapshotRepository.class);

			snapshots.save(FormSnapshotEntity.added(form("shared-id", 1, "a form"), nextRevision(context)));

			assertThat(diagrams.findHistory("shared-id", PageRequest.of(0, 10))).isEmpty();
			assertThat(snapshots.findHistory("shared-id", PageRequest.of(0, 10))).hasSize(1);
		});
	}

	/**
	 * A form saved for years would otherwise carry every schema it ever had, so the cleanup
	 * job keeps the newest versions and drops the rest.
	 */
	@Test
	void keepsOnlyTheNewestVersionsOfAForm() {
		runner.run(context -> {
			FormSnapshotRepository snapshots = context.getBean(FormSnapshotRepository.class);
			for (int version = 1; version <= 5; version++) {
				snapshots.save(FormSnapshotEntity.modified(form("f7", version, "v" + version), nextRevision(context), true));
			}

			snapshots.deleteOldRecords(2);

			assertThat(snapshots.findHistory("f7", PageRequest.of(0, 10)))
				.extracting(FormSnapshotEntity::getVersion)
				.containsExactly(5, 4);
		});
	}

	/** Cleaning one form's history must not touch another's. */
	@Test
	void keepsTheNewestVersionsOfEveryFormSeparately() {
		runner.run(context -> {
			FormSnapshotRepository snapshots = context.getBean(FormSnapshotRepository.class);
			for (int version = 1; version <= 3; version++) {
				snapshots.save(FormSnapshotEntity.modified(form("f8", version, "a" + version), nextRevision(context), true));
				snapshots.save(FormSnapshotEntity.modified(form("f9", version, "b" + version), nextRevision(context), true));
			}

			snapshots.deleteOldRecords(1);

			assertThat(snapshots.findHistory("f8", PageRequest.of(0, 10)))
				.extracting(FormSnapshotEntity::getVersion).containsExactly(3);
			assertThat(snapshots.findHistory("f9", PageRequest.of(0, 10)))
				.extracting(FormSnapshotEntity::getVersion).containsExactly(3);
		});
	}

	/** What the history endpoint hands out has to be the form as it was. */
	@Test
	void turnsASnapshotBackIntoAForm() {
		FormEntity restored = FormSnapshotEntity
			.modified(form("f6", 7, "seven"), 42L, true).toForm();

		assertThat(restored.getId()).isEqualTo("f6");
		assertThat(restored.getVersion()).isEqualTo(7);
		assertThat(restored.getFormId()).isEqualTo("form-f6");
		assertThat(new String(restored.getFormSchema(), StandardCharsets.UTF_8)).isEqualTo("seven");
	}

	/** A revision is a row of its own, and its number is the database's. */
	private static long nextRevision(org.springframework.context.ApplicationContext context) {
		ModRevInfo revision = new ModRevInfo();
		revision.setRevtstmp(System.currentTimeMillis());
		return context.getBean(ModRevInfoRepository.class).save(revision).getRev();
	}

	private static FormEntity form(String id, int version, String schema) {
		FormEntity form = new FormEntity();
		form.setId(id);
		form.setFormId("form-" + id);
		form.setDescription("a form");
		form.setVersion(version);
		form.setActive(true);
		form.setCreated(Timestamp.valueOf(LocalDateTime.now()));
		form.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		form.setUpdatedBy("tester");
		form.setFormSchema(schema.getBytes(StandardCharsets.UTF_8));
		return form;
	}
}
