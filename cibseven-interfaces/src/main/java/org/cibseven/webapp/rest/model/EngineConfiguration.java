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

	// Default time to live for historical data, in days, as reported by the engine.
	// -1 means to keep historical data indefinitely, 0 means to delete it immediately.
	// Boxed, not primitive: null means "unknown" (e.g. an engine-rest version that doesn't
	// report it). Valid when `enforceHistoryTimeToLive` is not null, elsewhere ignored.
	// Supported since engine version 3.0.0.
	private Integer historyTimeToLive;
	// Boxed, not primitive: null means "unknown" (e.g. an engine-rest version that doesn't
	// report it), which must stay distinguishable from a real, known false.
	// Supported since engine version 3.0.0.
	private Boolean enforceHistoryTimeToLive;

	public String getHistoryLevel() {
		return historyLevel != null ? historyLevel.toLowerCase() : DEFAULT_HISTORY_LEVEL;
	}

	// historyTimeToLive is only meaningful when enforceHistoryTimeToLive is known. A partial
	// report, where only the TTL came through, is therefore normalized to unknown (null).
	public Integer getHistoryTimeToLive() {
		return enforceHistoryTimeToLive != null ? historyTimeToLive : null;
	}
}
