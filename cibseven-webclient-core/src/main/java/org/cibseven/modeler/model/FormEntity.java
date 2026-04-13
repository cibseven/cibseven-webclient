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

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@SuppressWarnings("deprecation")
@Setter
@Getter
@RequiredArgsConstructor
@Entity
@Table(name = "forms")
public class FormEntity {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	@Column(length = 36)
	private String id;

	@Column(name = "description", length = 150)
	private String description;

	@Column(name = "created")
	private Timestamp created;

	@Column(name = "updated")
	private Timestamp updated;

	@Column(name = "updated_by", length = 100)
	private String updatedBy;

	@JdbcTypeCode(SqlTypes.BOOLEAN)
	@Column(name = "active", nullable = false, columnDefinition = "boolean default true")
	private Boolean active = true;

	@JdbcTypeCode(SqlTypes.BLOB)
	@Column(name = "form_schema", nullable = false)
	private byte[] formSchema;

	@Column(name = "formid", unique = true, nullable = false, length = 100)
	private String formId;

	@Column(name = "version", columnDefinition = "integer default 1")
	private int version;
}
