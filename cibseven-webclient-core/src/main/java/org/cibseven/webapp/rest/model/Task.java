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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
	String assignee;
	String caseDefinitionId;
	String caseExecutionId;
	String caseInstanceId;
	String delegationState;
	String description;
	String executionId;
	String formKey;
	String id;
	String name;
	String owner;
	String parentTaskId;
	long priority;
	String suspended;
	String tenantId;
	CamundaForm camundaFormRef;
	
	@JsonProperty("created") @JsonAlias({"creationDate"}) String created;
	@JsonProperty("due") @JsonAlias({"dueDate"}) String due;
	@JsonProperty("followUp") @JsonAlias({"followUpDate"}) String followUp;
	@JsonProperty("taskDefinitionKey") @JsonAlias({"taskDefinitionId"}) String taskDefinitionKey;
	@JsonProperty("processDefinitionId") @JsonAlias({"processDefinitionKey"}) String processDefinitionId;
	@JsonProperty("processInstanceId") @JsonAlias({"processInstanceKey"}) String processInstanceId;
	
	public String json() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}
}
