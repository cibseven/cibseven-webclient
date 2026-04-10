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

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * JPA Entity representing an Element Template in the CIB seven Modeler system.
 * <p>
 * Element Templates define reusable BPMN process elements with predefined
 * configurations and properties. They provide a way to standardize and
 * streamline process modeling by offering pre-configured components that
 * users can easily add to their process diagrams.
 * </p>
 */
@SuppressWarnings("deprecation")
@Entity
@Table(name = "element_templates")
@Data
@NoArgsConstructor
public class ElementTemplate {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	@Column(name = "id", updatable = false, nullable = false)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private String id;

	@JdbcTypeCode(SqlTypes.BOOLEAN)
	@Column(name = "active")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Boolean active = true;

	@Column(name = "version")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer version = 1;

	@Column(name = "template_id", unique = true, nullable = false, length = 100)
	@JsonAlias("id")
	private String templateId;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@JdbcTypeCode(SqlTypes.CLOB)
	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "origin", nullable = false, updatable = false, length = 50)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private ElementTemplateOrigin origin;

	@JdbcTypeCode(SqlTypes.CLOB)
	@Column(name = "content")
	private String content;

	@Column(name = "created_at", updatable = false)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime updatedAt;

	@Column(name = "created_by", length = 100)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private String createdBy;

	@Column(name = "updated_by", length = 100)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private String updatedBy;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
