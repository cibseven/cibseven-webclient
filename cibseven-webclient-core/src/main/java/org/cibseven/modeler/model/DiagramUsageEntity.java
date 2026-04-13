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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@SuppressWarnings("deprecation")
@Setter
@Getter
@RequiredArgsConstructor
@Entity
@Table(name = "diagram_usage")
public class DiagramUsageEntity {

	@Id
	@GeneratedValue(generator = "uuid") @GenericGenerator(name = "uuid", strategy = "uuid2")
	@Column(length = 36)
	private String id;

	@Column(name = "user_id", nullable = false, length = 100)
	private String userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "diagram_id", nullable = false)
	@JsonIgnore
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ProcessDiagramEntity diagram;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id", nullable = false)
	@JsonIgnore
	@OnDelete(action = OnDeleteAction.CASCADE)
	private UserSessionEntity userSession;

	@Column(name = "opened_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private Timestamp openedAt;

	@Column(name = "closed_at")
	private Timestamp closedAt;

	@JsonProperty("sessionId")
	public String getSessionId() {
		if (userSession != null) return userSession.getId();
		return null;
	}
}
