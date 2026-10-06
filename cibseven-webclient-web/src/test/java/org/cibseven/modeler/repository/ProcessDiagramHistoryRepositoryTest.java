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
import org.cibseven.modeler.model.ModRevInfo;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.model.ProcessDiagramSnapshotEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The diagram history is written into tables the installation already has, so the mapping is
 * exercised against their shipped definition rather than against a schema generated from the
 * entities - a wrong column name has to fail here.
 */
class ProcessDiagramHistoryRepositoryTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:h2:mem:history;DB_CLOSE_DELAY=-1",
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
			ProcessDiagramSnapshotRepository snapshots = context.getBean(ProcessDiagramSnapshotRepository.class);

			long revision = nextRevision(context);
			snapshots.save(ProcessDiagramSnapshotEntity.added(diagram("d1", 1, "one"), revision));

			List<ProcessDiagramSnapshotEntity> history = snapshots.findHistory("d1", PageRequest.of(0, 10));

			assertThat(history).singleElement().satisfies(snapshot -> {
				assertThat(snapshot.getRev()).isEqualTo(revision);
				assertThat(snapshot.getRevtype()).isEqualTo(ProcessDiagramSnapshotEntity.REVTYPE_ADD);
				assertThat(snapshot.getName()).isEqualTo("one");
				assertThat(snapshot.getProcesskey()).isEqualTo("key-d1");
				assertThat(snapshot.getVersion()).isEqualTo(1);
				assertThat(snapshot.getDiagramMod()).isTrue();
				assertThat(new String(snapshot.getDiagram(), StandardCharsets.UTF_8)).isEqualTo("one");
			});
		});
	}

	/** A save that only touched the metadata is no version to go back to, so it stays out. */
	@Test
	void reportsOnlyTheSavesThatChangedTheDiagram() {
		runner.run(context -> {
			ProcessDiagramSnapshotRepository snapshots = context.getBean(ProcessDiagramSnapshotRepository.class);

			snapshots.save(ProcessDiagramSnapshotEntity.added(diagram("d2", 1, "first"), nextRevision(context)));
			snapshots.save(ProcessDiagramSnapshotEntity.modified(diagram("d2", 1, "first"), nextRevision(context), false));
			snapshots.save(ProcessDiagramSnapshotEntity.modified(diagram("d2", 2, "second"), nextRevision(context), true));

			assertThat(snapshots.findHistory("d2", PageRequest.of(0, 10)))
				.extracting(ProcessDiagramSnapshotEntity::getVersion)
				.containsExactly(2, 1);
		});
	}

	/** Newest first, and never more than the endpoint asked for. */
	@Test
	void reportsTheNewestFirstAndHonoursTheLimit() {
		runner.run(context -> {
			ProcessDiagramSnapshotRepository snapshots = context.getBean(ProcessDiagramSnapshotRepository.class);

			for (int version = 1; version <= 4; version++) {
				snapshots.save(ProcessDiagramSnapshotEntity.modified(
					diagram("d3", version, "v" + version), nextRevision(context), true));
			}

			assertThat(snapshots.findHistory("d3", PageRequest.of(0, 2)))
				.extracting(ProcessDiagramSnapshotEntity::getVersion)
				.containsExactly(4, 3);
		});
	}

	@Test
	void keepsTheHistoriesOfDifferentDiagramsApart() {
		runner.run(context -> {
			ProcessDiagramSnapshotRepository snapshots = context.getBean(ProcessDiagramSnapshotRepository.class);

			snapshots.save(ProcessDiagramSnapshotEntity.added(diagram("d4", 1, "mine"), nextRevision(context)));
			snapshots.save(ProcessDiagramSnapshotEntity.added(diagram("d5", 1, "yours"), nextRevision(context)));

			assertThat(snapshots.findHistory("d4", PageRequest.of(0, 10)))
				.extracting(ProcessDiagramSnapshotEntity::getId).containsExactly("d4");
		});
	}

	/** What the history endpoint hands out has to be the diagram as it was. */
	@Test
	void turnsASnapshotBackIntoADiagram() {
		ProcessDiagramEntity restored = ProcessDiagramSnapshotEntity
			.modified(diagram("d6", 7, "seven"), 42L, true).toDiagram();

		assertThat(restored.getId()).isEqualTo("d6");
		assertThat(restored.getVersion()).isEqualTo(7);
		assertThat(restored.getProcesskey()).isEqualTo("key-d6");
		assertThat(new String(restored.getDiagram(), StandardCharsets.UTF_8)).isEqualTo("seven");
	}

	/** A revision is a row of its own, and its number is the database's. */
	private static long nextRevision(org.springframework.context.ApplicationContext context) {
		ModRevInfo revision = new ModRevInfo();
		revision.setRevtstmp(System.currentTimeMillis());
		return context.getBean(ModRevInfoRepository.class).save(revision).getRev();
	}

	private static ProcessDiagramEntity diagram(String id, int version, String content) {
		ProcessDiagramEntity diagram = new ProcessDiagramEntity();
		diagram.setId(id);
		diagram.setName(content);
		diagram.setProcesskey("key-" + id);
		diagram.setDescription("a diagram");
		diagram.setType("bpmn-c7");
		diagram.setVersion(version);
		diagram.setActive(true);
		diagram.setCreated(Timestamp.valueOf(LocalDateTime.now()));
		diagram.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		diagram.setUpdatedBy("tester");
		diagram.setDiagram(content.getBytes(StandardCharsets.UTF_8));
		return diagram;
	}
}
