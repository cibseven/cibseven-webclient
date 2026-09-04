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
 * A form as it was at one revision. One row is written per save, so the history of a form is the
 * rows sharing its id, ordered by revision.
 *
 * <p>{@code schemaMod} says whether that save changed the schema itself rather than only its
 * metadata; the history the modeler shows is filtered on it.
 */
@Setter @Getter @NoArgsConstructor
@Entity
@Table(name = "MOD_FORMS_AUD")
@IdClass(FormSnapshotEntity.SnapshotId.class)
public class FormSnapshotEntity {

	/** A save is either the first one for a form or a later change to it. */
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

	@Column(name = "formid", length = 100)
	private String formId;

	@Column(name = "version")
	private Integer version;

	@JdbcTypeCode(SqlTypes.BOOLEAN)
	@Column(name = "schema_mod")
	private Boolean schemaMod;

	@JdbcTypeCode(SqlTypes.LONGVARBINARY)
	@Column(name = "form_schema")
	private byte[] formSchema;

	/** The snapshot of a form that has just been created. */
	public static FormSnapshotEntity added(FormEntity form, long revision) {
		return of(form, revision, REVTYPE_ADD, true);
	}

	/**
	 * The snapshot of a form that has just been saved.
	 *
	 * @param schemaChanged whether the save changed the schema itself and not only its metadata
	 */
	public static FormSnapshotEntity modified(FormEntity form, long revision, boolean schemaChanged) {
		return of(form, revision, REVTYPE_MOD, schemaChanged);
	}

	private static FormSnapshotEntity of(FormEntity form, long revision, short revtype,
			boolean schemaChanged) {
		FormSnapshotEntity snapshot = new FormSnapshotEntity();
		snapshot.setId(form.getId());
		snapshot.setRev(revision);
		snapshot.setRevtype(revtype);
		snapshot.setDescription(form.getDescription());
		snapshot.setCreated(form.getCreated());
		snapshot.setUpdated(form.getUpdated());
		snapshot.setUpdatedBy(form.getUpdatedBy());
		snapshot.setActive(form.getActive());
		snapshot.setFormId(form.getFormId());
		snapshot.setVersion(form.getVersion());
		snapshot.setSchemaMod(schemaChanged);
		snapshot.setFormSchema(form.getFormSchema());
		return snapshot;
	}

	/** Turns a snapshot back into the form it was taken from, for the history endpoint. */
	public FormEntity toForm() {
		FormEntity form = new FormEntity();
		form.setId(id);
		form.setDescription(description);
		form.setCreated(created);
		form.setUpdated(updated);
		form.setUpdatedBy(updatedBy);
		form.setActive(active);
		form.setFormId(formId);
		form.setVersion(version == null ? 0 : version);
		form.setFormSchema(formSchema);
		return form;
	}

	@Setter @Getter @NoArgsConstructor @EqualsAndHashCode
	public static class SnapshotId implements Serializable {
		private static final long serialVersionUID = 1L;
		private String id;
		private Long rev;
	}
}
