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

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariableHistory extends Variable {
	String id;
	String name;
	String processDefinitionKey;
	String processDefinitionId;
	String processInstanceId;
	String executionId;
	String activityInstanceId;
	String caseDefinitionKey;
	String caseDefinitionId;
	String caseInstanceId;
	String caseExecutionId;
	String taskId;
	String errorMessage;
	String tenantId;
	String state;
	String createTime;
	String removalTime;
	String rootProcessInstanceId;
}
