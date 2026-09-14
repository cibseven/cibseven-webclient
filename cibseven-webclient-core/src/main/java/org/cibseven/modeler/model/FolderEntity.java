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

import java.sql.Timestamp;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * A folder of the modeler tree. Every model belongs to exactly one, so a folder is also the
 * path a model is reached under. Only the database source keeps its tree here.
 *
 * <p>A folder without a parent is one the UI shows as a project, at the top level of its source.
 * The source itself is not a folder: it is what the column says, and the UI offers it as a
 * choice rather than as something to open.</p>
 */
@Setter @Getter @RequiredArgsConstructor
@Entity
@Table(
	name = "MOD_FOLDERS",
	uniqueConstraints = @UniqueConstraint(name = "UK_MOD_FOLDERS_PARENT_NAME", columnNames = { "source", "parent_id", "name" }),
	indexes = @Index(name = "IDX_MOD_FOLDERS_PARENT", columnList = "parent_id")
)
public class FolderEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(length = 36)
	private String id;

	/** Null in a folder at the top level of its source. */
	@Column(name = "parent_id", length = 36)
	private String parentId;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 50)
	private ModelSource source = ModelSource.DATABASE;

	@NotBlank
	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Column(name = "created")
	private Timestamp created;

	@Column(name = "created_by", length = 100)
	private String createdBy;

	@Column(name = "updated")
	private Timestamp updated;

	@Column(name = "updated_by", length = 100)
	private String updatedBy;
}
