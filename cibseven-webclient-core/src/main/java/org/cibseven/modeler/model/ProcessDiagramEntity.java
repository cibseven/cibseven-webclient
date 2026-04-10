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
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter @Getter @RequiredArgsConstructor
@Entity
@Table(name = "processes_diagrams")
@Audited
public class ProcessDiagramEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(length = 36)
	private String id;

	@Column(name = "name", length = 255)
	private String name;

	@NotBlank
	@Column(name = "processkey", unique = true, length = 100)
	private String processkey;

	@Column(name = "description", length = 150)
	private String description;

	@Column(name = "created")
	private Timestamp created;

	@Column(name = "updated")
	private Timestamp updated;

	@Column(name = "updated_by", length = 100)
	private String updatedBy;

	@NotNull
	@JdbcTypeCode(SqlTypes.BOOLEAN)
	@Column(name = "active", columnDefinition = "boolean default true")
	private Boolean active = true;

	@NotBlank
	@Column(name = "type")
	private String type = "bpmn-c7";

	@Column(name = "version", columnDefinition = "integer default 1")
	private int version;

	@JdbcTypeCode(SqlTypes.BLOB)
	@Column(name = "diagram")
	@Audited(withModifiedFlag = true)
	private byte[] diagram;

	@OneToMany(mappedBy = "diagram", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.EAGER)
	@NotAudited
	private List<DiagramUsageEntity> diagramUsages = new ArrayList<>();

}
