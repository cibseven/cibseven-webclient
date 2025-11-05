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
package org.cibseven.webapp.providers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.CandidateGroupTaskCount;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.ProcessVariables;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskForm;
import org.cibseven.webapp.rest.model.TaskHistory;
import org.cibseven.webapp.rest.model.Variable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TaskProvider extends SevenProviderBase implements ITaskProvider {

	@Autowired private IVariableProvider variableProvider;

	@Override
	public Collection<Task> findTasks(String filter, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task?sortBy=created&sortOrder=desc" + (filter!=null ? filter : "");
		return Arrays.asList(((ResponseEntity<Task[]>) doGet(url, Task[].class, user, false)).getBody());
	}

	@Override
	public Integer findTasksCount(Map<String, Object> filters, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/count";
		JsonNode body =  ((ResponseEntity<JsonNode>) doPost(url, filters, JsonNode.class, user)).getBody();
		if (body == null) {
			throw new NullPointerException();
		}
		return body.get("count").asInt();
	}

	@Override
	public Collection<Task> findTasksByProcessInstance(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task?processInstanceId=" + processInstanceId;
		return Arrays.asList(((ResponseEntity<Task[]>) doGet(url, Task[].class, user, false)).getBody());		
	}

	@Override
	public Collection<Task> findTasksByProcessInstanceAsignee(Optional<String> processInstanceId, Optional<String> createdAfter, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task";
		try {
			url += "?assignee=" + URLEncoder.encode(user.getId(), StandardCharsets.UTF_8.toString());
			url += processInstanceId.isPresent() ? "&processInstanceId=" + processInstanceId.get() : "";
			if (createdAfter.isPresent()) {
				url += "&createdAfter=" + URLEncoder.encode(createdAfter.get(), StandardCharsets.UTF_8.toString());
			}
		} catch (UnsupportedEncodingException e) {
			throw new SystemException(e);
		}
		return Arrays.asList(((ResponseEntity<Task[]>) doGet(url, Task[].class, user, true)).getBody());
	}

	@Override
	public Task findTaskById(String id, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/" + id;
		return ((ResponseEntity<Task>) doGet(url, Task.class, user, false)).getBody();
	}

	@Override
	public void update(Task task, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/" + task.getId();
		String filteredTask = "{ ";
		if(task.getName() != null) filteredTask += "\"name\": \"" + task.getName() + "\" ";
		if(task.getDescription() != null) filteredTask += ", \"description\": \"" + task.getDescription() + "\"";
		//if(task.getPriority() != null) filteredTask += ", \"priority\": " + task.getPriority() + "";
		if(task.getAssignee() != null) filteredTask += ", \"assignee\": \"" + task.getAssignee() + "\""; 
		if(task.getOwner() != null) filteredTask += ", \"owner\": \"" + task.getOwner() + "\"";
		if(task.getDelegationState() != null) filteredTask += ", \"delegationState\": \"" + task.getDelegationState() + "\"";
		if(task.getDue() != null) filteredTask += ", \"due\": \"" + task.getDue() + "\"";
		if(task.getFollowUp() != null) filteredTask += ", \"followUp\": \"" + task.getFollowUp() + "\""; 
		if(task.getParentTaskId() != null) filteredTask += ", \"parentTaskId\": \"" + task.getParentTaskId() + "\""; 
		if(task.getCaseInstanceId() != null) filteredTask += ", \"caseInstanceId\": \"" + task.getCaseInstanceId() + "\""; 
		if(task.getTenantId() != null) filteredTask += ", \"tenantId\": \"" + task.getTenantId() + "\"";
		filteredTask += " }";
		doPut(url, filteredTask, user);
	}

	@Override
	public void setAssignee(String taskId, String assignee, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/" + taskId;
		String variables = "{}";

		if(!assignee.equals("null")) {
			url += "/assignee";
			variables = "{ \"userId\": \"" + assignee + "\" }";
		} else {
			url += "/unclaim";
		}

		doPost(url, variables, String.class, user);
	}

	@Override
	public void submit(String taskId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/" + taskId + "/submit-form";
		doPost(url, "{ \"variables\": {} }", String.class, user);
	}

	@Override
	public void submit(Task task, List<Variable> formResult, CIBUser user) {
		if (!formResult.isEmpty()) {
			variableProvider.submitVariables(task.getProcessInstanceId(), formResult, user, task.getProcessDefinitionId());
		}
		submit(task.getId(), user);
	}

	@Override
	public Object formReference(String taskId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/" + taskId + "/form-variables?variableNames=formReference";
		ProcessVariables body =  ((ResponseEntity<ProcessVariables>) doGet(url, ProcessVariables.class, user, false)).getBody();
		if (body == null) {
			throw new NullPointerException();
		}
		Variable formReference = body.getFormReference();
		if (formReference == null) return new String("empty-task"); 
		else return formReference.getValue();
	}
	
	@Override
	public Object form(String taskId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/" + taskId + "/form";
		TaskForm taskForm = ((ResponseEntity<TaskForm>) doGet(url, TaskForm.class, user, false)).getBody();
		if (taskForm == null || taskForm.getKey() == null) {
			return "empty-task";
		}
		return taskForm;
	}

	@Override
	public Collection<Task> findTasksByFilter(TaskFiltering filters, String filterId, CIBUser user, Integer firstResult, Integer maxResults) {
		String url = getEngineRestUrl(user) + "/filter/" + filterId + "/list?firstResult=" + firstResult + "&maxResults=" + maxResults;		
		try {
			return Arrays.asList(((ResponseEntity<Task[]>) doPost(url, filters.json(), Task[].class, user)).getBody());
		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in getTasksFiltered(...):", se);
			throw se;
		}
	}

	@Override
	public Integer findTasksCountByFilter(String filterId, CIBUser user, TaskFiltering filters) {
		String url = getEngineRestUrl(user) + "/filter/" + filterId + "/count";		
		try {
			JsonNode body =  ((ResponseEntity<JsonNode>) doPost(url, filters.json(), JsonNode.class, user)).getBody();
			if (body == null) {
				throw new NullPointerException();
			}
			return body.get("count").asInt();
		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in findTasksCountByFilter(...):", se);
			throw se;
		}
	}

	@Override
	public Collection<TaskHistory> findTasksByProcessInstanceHistory(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/history/task?processInstanceId=" + processInstanceId + "&sortBy=startTime&sortOrder=desc";
		return Arrays.asList(((ResponseEntity<TaskHistory[]>) doGet(url, TaskHistory[].class, user, false)).getBody());
	}

	@Override
	public Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/history/task?processInstanceId=" + processInstanceId + "&taskDefinitionKey=" + taskDefinitionKey;
		return Arrays.asList(((ResponseEntity<TaskHistory[]>) doGet(url, TaskHistory[].class, user, false)).getBody());				
	}

	@Override
	public Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException {
		String url = getEngineRestUrl(user) + "/task";
		return Arrays.asList(((ResponseEntity<Task[]>) doPost(url, data, Task[].class, user)).getBody());
	}

	@Override
	public Collection<IdentityLink> findIdentityLink(String taskId, Optional<String> type, CIBUser user) 
	{
		String url = getEngineRestUrl(user) + "/task/" + taskId	+ "/identity-links";

		String param = "";
		param += addQueryParameter(param, "type", type, true);

		url += param;

		return Arrays.asList(((ResponseEntity<IdentityLink[]>) doGet(url, IdentityLink[].class, user, false)).getBody());
	}

	@Override
	public void createIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/" + taskId	+ "/identity-links";
		doPost(url, data, null, user);
	}

	@Override
	public void deleteIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/" + taskId	+ "/identity-links/delete";
		doPost(url, data, null, user);
	}

	@Override
	public void handleBpmnError(String taskId, Map<String, Object> data, CIBUser user) throws SystemException {
		String url = getEngineRestUrl(user) + "/task/" + taskId + "/bpmnError";
		doPost(url, data, null, user);
	}

	@Override
	public Collection<TaskHistory> findTasksByTaskIdHistory(String taskId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/history/task?taskId=" + taskId;
		return Arrays.asList(((ResponseEntity<TaskHistory[]>) doGet(url, TaskHistory[].class, user, false)).getBody());
	}

	@Override
	public ResponseEntity<byte[]> getDeployedForm(String taskId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/" + taskId + "/deployed-form";
		return doGetWithHeader(url, byte[].class, user, true, MediaType.APPLICATION_OCTET_STREAM);
	}

	@Override
	public Integer findHistoryTasksCount(Map<String, Object> filters, CIBUser user) {
		String url = getEngineRestUrl(user) + "/history/task/count";
		JsonNode body = ((ResponseEntity<JsonNode>) doPost(url, filters, JsonNode.class, user)).getBody();
		if (body == null) {
			throw new NullPointerException();
		}
		return body.get("count").asInt();
	}

	@Override
	public Collection<CandidateGroupTaskCount> getTaskCountByCandidateGroup(CIBUser user) {
		String url = getEngineRestUrl(user) + "/task/report/candidate-group-count";
		return Arrays.asList(((ResponseEntity<CandidateGroupTaskCount[]>) doGet(url, CandidateGroupTaskCount[].class, user, false)).getBody());
	}
}
