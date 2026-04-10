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
import java.util.List;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@SuppressWarnings("deprecation")
@Setter
@Getter
@RequiredArgsConstructor
@Entity
@Table(name = "user_sessions")
public class UserSessionEntity {

	@Id
	@GeneratedValue(generator = "uuid") @GenericGenerator(name = "uuid", strategy = "uuid2")
	@Column(length = 36)
	private String id;

	@Column(name = "user_id", nullable = false, length = 255)
	private String userId;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private Timestamp createdAt;

	@Column(name = "expires_at")
	private Timestamp expiresAt;

	@OneToMany(mappedBy = "userSession", fetch = FetchType.LAZY)
	private List<FormUsageEntity> formUsages;

	@OneToMany(mappedBy = "userSession", fetch = FetchType.LAZY)
	private List<DiagramUsageEntity> diagramUsages;
}
