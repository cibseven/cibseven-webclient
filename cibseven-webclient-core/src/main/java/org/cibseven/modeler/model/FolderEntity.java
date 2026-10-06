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
 * path a model is reached under. Only models kept in the database have one: a repository or a
 * directory brings its own tree.
 *
 * <p>A folder without a parent is at the top level; models may live in any folder.</p>
 */
@Setter @Getter @RequiredArgsConstructor
@Entity
@Table(
	name = "MOD_FOLDERS",
	uniqueConstraints = @UniqueConstraint(name = "MOD_UK_FOLDERS_PARENT_NAME", columnNames = { "parent_id", "name" }),
	indexes = @Index(name = "MOD_IDX_FOLDERS_PARENT", columnList = "parent_id")
)
public class FolderEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(length = 36)
	private String id;

	/** Null in a folder at the top level. */
	@Column(name = "parent_id", length = 36)
	private String parentId;

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
