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
package org.cibseven.modeler.model;

import java.io.Serializable;
import java.sql.Timestamp;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A diagram as it was at one revision. One row is written per save, so the history of a diagram is
 * the rows sharing its id, ordered by revision.
 *
 * <p>{@code diagramMod} says whether that save changed the diagram itself rather than only its
 * metadata; the history the modeler shows is filtered on it, and the retention job keys on it.
 */
@Setter @Getter @NoArgsConstructor
@Entity
@Table(name = "MOD_PROCESSES_DIAGRAMS_AUD")
@IdClass(ProcessDiagramSnapshotEntity.SnapshotId.class)
public class ProcessDiagramSnapshotEntity {

	/** A save is either the first one for a diagram or a later change to it. */
	public static final short REVTYPE_ADD = 0;
	public static final short REVTYPE_MOD = 1;

	@Id
	@Column(name = "id", length = 36)
	private String id;

	@Id
	@Column(name = "rev")
	private Long rev;

	@Column(name = "revtype")
	private Short revtype;

	@Column(name = "name", length = 255)
	private String name;

	@Column(name = "processkey", length = 100)
	private String processkey;

	@Column(name = "description", length = 150)
	private String description;

	@Column(name = "created")
	private Timestamp created;

	@Column(name = "updated")
	private Timestamp updated;

	@Column(name = "updated_by", length = 100)
	private String updatedBy;

	@JdbcTypeCode(SqlTypes.BOOLEAN)
	@Column(name = "active")
	private Boolean active;

	@Column(name = "type")
	private String type;

	@Column(name = "version")
	private Integer version;

	@JdbcTypeCode(SqlTypes.BOOLEAN)
	@Column(name = "diagram_mod")
	private Boolean diagramMod;

	@JdbcTypeCode(SqlTypes.LONGVARBINARY)
	@Column(name = "diagram")
	private byte[] diagram;

	/** The snapshot of a diagram that has just been created. */
	public static ProcessDiagramSnapshotEntity added(ProcessDiagramEntity diagram, long revision) {
		return of(diagram, revision, REVTYPE_ADD, true);
	}

	/**
	 * The snapshot of a diagram that has just been saved.
	 *
	 * @param diagramChanged whether the save changed the diagram itself and not only its metadata
	 */
	public static ProcessDiagramSnapshotEntity modified(ProcessDiagramEntity diagram, long revision,
			boolean diagramChanged) {
		return of(diagram, revision, REVTYPE_MOD, diagramChanged);
	}

	private static ProcessDiagramSnapshotEntity of(ProcessDiagramEntity diagram, long revision,
			short revtype, boolean diagramChanged) {
		ProcessDiagramSnapshotEntity snapshot = new ProcessDiagramSnapshotEntity();
		snapshot.setId(diagram.getId());
		snapshot.setRev(revision);
		snapshot.setRevtype(revtype);
		snapshot.setName(diagram.getName());
		snapshot.setProcesskey(diagram.getProcesskey());
		snapshot.setDescription(diagram.getDescription());
		snapshot.setCreated(diagram.getCreated());
		snapshot.setUpdated(diagram.getUpdated());
		snapshot.setUpdatedBy(diagram.getUpdatedBy());
		snapshot.setActive(diagram.getActive());
		snapshot.setType(diagram.getType());
		snapshot.setVersion(diagram.getVersion());
		snapshot.setDiagramMod(diagramChanged);
		snapshot.setDiagram(diagram.getDiagram());
		return snapshot;
	}

	/** Turns a snapshot back into the diagram it was taken from, for the history endpoint. */
	public ProcessDiagramEntity toDiagram() {
		ProcessDiagramEntity diagram = new ProcessDiagramEntity();
		diagram.setId(id);
		diagram.setName(name);
		diagram.setProcesskey(processkey);
		diagram.setDescription(description);
		diagram.setCreated(created);
		diagram.setUpdated(updated);
		diagram.setUpdatedBy(updatedBy);
		diagram.setActive(active);
		diagram.setType(type);
		diagram.setVersion(version == null ? 0 : version);
		diagram.setDiagram(this.diagram);
		return diagram;
	}

	@Setter @Getter @NoArgsConstructor @EqualsAndHashCode
	public static class SnapshotId implements Serializable {
		private static final long serialVersionUID = 1L;
		private String id;
		private Long rev;
	}
}
