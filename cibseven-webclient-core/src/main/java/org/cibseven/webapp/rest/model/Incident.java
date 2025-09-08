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

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class Incident {
	private String id;
	private String processDefinitionKey;
	private String processDefinitionId;
	private String processInstanceId;
	private String executionId;
	private String rootProcessInstanceId;
	private String createTime;
	private String endTime;
	private String removalTime;
	private String incidentTimestamp;
	private String incidentType;
	private String activityId;
	private String failedActivityId;
	private String causeIncidentId;
	private String rootCauseIncidentId;
	private String configuration;
	private String historyConfiguration;
	private String incidentMessage;
	private String tenantId;
	private String jobDefinitionId;
	private Boolean open;
	private Boolean deleted;
	private Boolean resolved;
	private String annotation;
	private String causeIncidentProcessInstanceId;
	private String causeIncidentProcessDefinitionId;
	private String causeIncidentActivityId;
	private String causeIncidentFailedActivityId;
	private String rootCauseIncidentProcessInstanceId;
	private String rootCauseIncidentProcessDefinitionId;
	private String rootCauseIncidentActivityId;
	private String rootCauseIncidentFailedActivityId;
	private String rootCauseIncidentConfiguration;
	private String rootCauseIncidentMessage;
}
