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
package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineConfiguration {
	private static final String DEFAULT_HISTORY_LEVEL = "full";

	private String engineName;
	private String historyLevel = DEFAULT_HISTORY_LEVEL;
	private boolean authorizationEnabled = true;
	private boolean enablePasswordPolicy;

	/**
	 * The engine-wide default history time to live, in days, as reported by the engine's
	 * {@code GET /engine/{name}/configuration} endpoint (or read directly from the engine
	 * configuration by the direct provider).
	 * <ul>
	 *   <li>{@code n >= 0}: the engine applies this TTL at deployment time to definitions whose
	 *   model does not set its own one ({@code 0}: history is removed by the next cleanup run).</li>
	 *   <li>{@code -1}: the engine has no default configured.</li>
	 *   <li>{@code null}: <b>unknown</b>, the engine does not report it (engines before 3.0.0, which
	 *   omit the property). It must not be mistaken for {@code -1}.</li>
	 * </ul>
	 * Boxed on purpose: a primitive with a fallback default would make an engine that does not
	 * report the value indistinguishable from one that really has that value.
	 *
	 * @since 3.0.0 (engine version)
	 */
	private Integer historyTimeToLive;

	/**
	 * Whether the engine rejects the deployment of a model resource (BPMN, DMN, CMMN) for which no
	 * history time to live can be resolved, and rejects setting a definition's TTL to {@code null}.
	 * <ul>
	 *   <li>{@code true} / {@code false}: the value reported by the engine.</li>
	 *   <li>{@code null}: <b>unknown</b>, the engine does not report it (engines before 3.0.0, which
	 *   omit the property). It must not be mistaken for {@code true} (the engine default), since
	 *   the engine may have enforcement switched off, nor for {@code false}.</li>
	 * </ul>
	 * Boxed on purpose, see {@link #historyTimeToLive}. Note that Lombok generates
	 * {@code getEnforceHistoryTimeToLive()} (not {@code isEnforceHistoryTimeToLive()}) for a
	 * {@link Boolean}.
	 *
	 * @since 3.0.0 (engine version)
	 */
	private Boolean enforceHistoryTimeToLive;

	public String getHistoryLevel() {
		return historyLevel != null ? historyLevel.toLowerCase() : DEFAULT_HISTORY_LEVEL;
	}
}
