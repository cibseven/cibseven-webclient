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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true) @JsonInclude(Include.NON_NULL)
public class FilterCriterias {
	private String processInstanceId;
	private String processInstanceBusinessKey;
	private String processInstanceBusinessKeyExpression;
	private String[] processInstanceBusinessKeyIn;
	private String processInstanceBusinessKeyLike;
	private String processInstanceBusinessKeyLikeExpression;
	private String processDefinitionId;
	private String processDefinitionKey;
	private String[] processDefinitionKeyIn;
	private String processDefinitionName;
	private String processDefinitionNameLike;
	private String executionId;
	private String caseInstanceId;
	private String caseInstanceBusinessKey;
	private String caseInstanceBusinessKeyLike;
	private String caseDefinitionId;
	private String caseDefinitionKey;
	private String caseDefinitionName;
	private String caseDefinitionNameLike;
	private String caseExecutionId;
	private String[] activityInstanceIdIn;
	private String tenantIdIn;
	private String withoutTenantId;
	private String assignee;
	private String assigneeExpression;
	private String assigneeLike;
	private String assigneeLikeExpression;
	private String[] assigneeIn;
	private String owner;
	private String ownerExpression;
	private String candidateGroup;
	private String candidateGroupExpression;
	private String withCandidateGroups;
	private String withoutCandidateGroups;
	private String withCandidateUsers;
	private String withoutCandidateUsers;
	private String candidateUser;
	private String candidateUserExpression;
	private String includeAssignedTasks;
	private String involvedUser;
	private String involvedUserExpression;
	private boolean assigned;
	private boolean unassigned;
	private String taskDefinitionKey;
	private String[] taskDefinitionKeyIn;
	private String taskDefinitionKeyLike;
	private String name;
	private String nameNotEqual;
	private String nameLike;
	private String nameNotLike;
	private String description;
	private String descriptionLike;
	private String priority;
	private String maxPriority;
	private String minPriority;
	private String dueDate;
	private String dueDateExpression;
	private String dueAfter;
	private String dueAfterExpression;
	private String dueBefore;
	private String dueBeforeExpression;
	private String followUpDate;
	private String followUpDateExpression;
	private String followUpAfter;
	private String followUpAfterExpression;
	private String followUpBefore;
	private String followUpBeforeExpression;
	private String followUpBeforeOrNotExistent;
	private String followUpBeforeOrNotExistentExpression;
	private String createdOn;
	private String createdOnExpression;
	private String createdAfter;
	private String createdAfterExpression;
	private String createdBefore;
	private String createdBeforeExpression;
	private String delegationState;
	private String[] candidateGroups;
	private String candidateGroupsExpression;
	private boolean active;
	private boolean suspended;
	private ArrayNode taskVariables;
	private ArrayNode processVariables;
	private ArrayNode caseInstanceVariables;
	private boolean variableNamesIgnoreCase;
	private boolean variableValuesIgnoreCase;
	private String parentTaskId;
	private ArrayNode orQueries;
	private ArrayNode sorting;
}
