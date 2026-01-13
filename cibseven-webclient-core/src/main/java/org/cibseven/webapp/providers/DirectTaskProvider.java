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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.bpm.engine.EntityTypes;
import org.cibseven.bpm.engine.FormService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.exception.NotFoundException;
import org.cibseven.bpm.engine.exception.NullValueException;
import org.cibseven.bpm.engine.form.CamundaFormRef;
import org.cibseven.bpm.engine.form.FormData;
import org.cibseven.bpm.engine.history.HistoricTaskInstance;
import org.cibseven.bpm.engine.history.HistoricTaskInstanceQuery;
import org.cibseven.bpm.engine.identity.Group;
import org.cibseven.bpm.engine.identity.GroupQuery;
import org.cibseven.bpm.engine.impl.form.validator.FormFieldValidationException;
import org.cibseven.bpm.engine.impl.identity.Authentication;
import org.cibseven.bpm.engine.impl.util.IoUtil;
import org.cibseven.bpm.engine.query.Query;
import org.cibseven.bpm.engine.rest.dto.AbstractQueryDto;
import org.cibseven.bpm.engine.rest.dto.VariableValueDto;
import org.cibseven.bpm.engine.rest.dto.converter.DelegationStateConverter;
import org.cibseven.bpm.engine.rest.dto.converter.StringListConverter;
import org.cibseven.bpm.engine.rest.dto.history.HistoricTaskInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricTaskInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.StartProcessInstanceDto;
import org.cibseven.bpm.engine.rest.dto.task.CompleteTaskDto;
import org.cibseven.bpm.engine.rest.dto.task.FormDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskBpmnErrorDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskCountByCandidateGroupResultDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskQueryDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskWithAttachmentAndCommentDto;
import org.cibseven.bpm.engine.rest.exception.RestException;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.rest.util.ApplicationContextPathUtil;
import org.cibseven.bpm.engine.rest.util.EncodingUtil;
import org.cibseven.bpm.engine.rest.util.QueryUtil;
import org.cibseven.bpm.engine.task.DelegationState;
import org.cibseven.bpm.engine.task.TaskCountByCandidateGroupResult;
import org.cibseven.bpm.engine.task.TaskQuery;
import org.cibseven.bpm.engine.variable.VariableMap;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SubmitDeniedException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.CamundaForm;
import org.cibseven.webapp.rest.model.CandidateGroupTaskCount;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskForm;
import org.cibseven.webapp.rest.model.TaskHistory;
import org.cibseven.webapp.rest.model.Variable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DirectTaskProvider implements ITaskProvider {

	DirectProviderUtil directProviderUtil;
	public DirectTaskProvider(DirectProviderUtil directProviderUtil){
		this.directProviderUtil = directProviderUtil;
	}

	@Override
	public Collection<Task> findTasks(String filter, CIBUser user) {
		Map<String, Object> filters = new HashMap<>();
		String[] splitFilter = filter.split("&");
		for (String params : splitFilter) {
			String[] splitValue = params.split("=");
			if (splitValue.length > 1)
				filters.put(splitValue[0], URLDecoder.decode(splitValue[1], Charset.forName("UTF-8")));
		}
		return convertTasks(queryTasks(filters, user), user);
	}

	@Override
	public Integer findTasksCount(@RequestBody Map<String, Object> filters, CIBUser user) {
		return queryTasks(filters, user).size();
	}

	@Override
	public Collection<Task> findTasksByProcessInstance(String processInstanceId, CIBUser user) {
		TaskQuery taskQuery = directProviderUtil.getProcessEngine(user).getTaskService().createTaskQuery().processInstanceId(processInstanceId);
		List<org.cibseven.bpm.engine.task.Task> resultList = taskQuery.initializeFormKeys().list();
		return convertTasks(resultList, user);
	}

	@Override
	public Collection<Task> findTasksByProcessInstanceAsignee(Optional<String> processInstanceId,
			Optional<String> createdAfter, CIBUser user) {
		TaskQueryDto dto = new TaskQueryDto();
		if (createdAfter.isPresent()) {
			dto.setCreatedAfter(directProviderUtil.getObjectMapper(user).convertValue(createdAfter.get(), Date.class));
		}
		dto.setAssignee(user.getId());
		if (processInstanceId.isPresent())
			dto.setProcessInstanceId(processInstanceId.get());
		TaskQuery taskQuery = dto.toQuery(directProviderUtil.getProcessEngine(user));
		List<org.cibseven.bpm.engine.task.Task> resultList = taskQuery.initializeFormKeys().list();
		return convertTasks(resultList, user);

	}

	@Override
	public Task findTaskById(String id, CIBUser user) {
		org.cibseven.bpm.engine.task.Task result = directProviderUtil.getProcessEngine(user).getTaskService().createTaskQuery().taskId(id).initializeFormKeys()
				.singleResult();
		if (result == null)
			throw new NoObjectFoundException(null);
		return directProviderUtil.getObjectMapper(user).convertValue(TaskDto.fromEntity(result), Task.class);
	}

	@Override
	public void update(Task task, CIBUser user) {
		org.cibseven.bpm.engine.task.Task foundTask = directProviderUtil.getProcessEngine(user).getTaskService().createTaskQuery().taskId(task.getId()).initializeFormKeys()
				.singleResult();

		if (foundTask == null) {
			throw new NoObjectFoundException(new SystemException("No matching task with id " + task.getId()));
		}

		foundTask.setName(task.getName());
		foundTask.setDescription(task.getDescription());
		foundTask.setPriority((int) task.getPriority());
		foundTask.setAssignee(task.getAssignee());
		foundTask.setOwner(task.getOwner());

		DelegationState state = null;
		if (task.getDelegationState() != null) {
			DelegationStateConverter converter = new DelegationStateConverter();
			state = converter.convertQueryParameterToType(task.getDelegationState());
		}
		foundTask.setDelegationState(state);

		foundTask.setDueDate(directProviderUtil.getObjectMapper(user).convertValue(task.getDue(), Date.class));
		foundTask.setFollowUpDate(directProviderUtil.getObjectMapper(user).convertValue(task.getFollowUp(), Date.class));
		foundTask.setParentTaskId(task.getParentTaskId());
		foundTask.setCaseInstanceId(task.getCaseInstanceId());
		foundTask.setTenantId(task.getTenantId());

		directProviderUtil.getProcessEngine(user).getTaskService().saveTask(foundTask);
	}

	@Override
	public void setAssignee(String taskId, String assignee, CIBUser user) {
		org.cibseven.bpm.engine.task.Task foundTask = getTaskById(taskId, user);
		foundTask.setAssignee(assignee);
		directProviderUtil.getProcessEngine(user).getTaskService().saveTask(foundTask);
	}

	@Override
	public void submit(String taskId, CIBUser user) {
		VariableMap variables = null;
		directProviderUtil.getProcessEngine(user).getFormService().submitTaskForm(taskId, variables);
	}

	@Override
	public void submit(Task task, List<Variable> formResult, CIBUser user) {
		Map<String, VariableValueDto> variables = new HashMap<>();
		for (Variable variable : formResult) {
			VariableValueDto variableValueDto = directProviderUtil.convertValue(variable, VariableValueDto.class, user);
			variableValueDto.setType(variable.getType());
			variableValueDto.setValue(variable.getValue());
			if (variable.getValueInfo() != null)
				variableValueDto.setValueInfo(new HashMap<>(variable.getValueInfo()));
			variables.put(variable.getName(), variableValueDto);
		}
		CompleteTaskDto completeTaskDto = new CompleteTaskDto();
		completeTaskDto.setVariables(variables);

		try {
			VariableMap variablesMap = VariableValueDto.toMap(completeTaskDto.getVariables(), directProviderUtil.getProcessEngine(user), directProviderUtil.getObjectMapper(user));
			directProviderUtil.getProcessEngine(user).getFormService().submitTaskForm(task.getId(), variablesMap);
		} catch (AuthorizationException e) {
			throw e;
		} catch (ProcessEngineException e) {
			String errorMessage = String.format("Cannot submit task form %s: %s", task.getId(), e.getMessage());
			throw new SubmitDeniedException(new SystemException(errorMessage, e));
		}
	}

	@Override
	public Object formReference(String taskId, CIBUser user) {
		List<String> formVariables = null;
		String variableNames = "formReference";
		if (variableNames != null) {
			StringListConverter stringListConverter = new StringListConverter();
			formVariables = stringListConverter.convertQueryParameterToType(variableNames);
		}
		boolean deserializeValues = true;
		VariableMap startFormVariables = directProviderUtil.getProcessEngine(user).getFormService().getTaskFormVariables(taskId, formVariables, deserializeValues);
		Set<String> keys = startFormVariables.keySet();
		if (keys.isEmpty())
			return new String("empty-task");
		else {
			return VariableValueDto.fromMap(startFormVariables);
		}
	}

	@Override
	public Object form(String taskId, CIBUser user) {
		org.cibseven.bpm.engine.task.Task task = getTaskById(taskId, user);
		FormData formData;
		try {
			formData = directProviderUtil.getProcessEngine(user).getFormService().getTaskFormData(taskId);
		} catch (AuthorizationException e) {
			throw e;
		} catch (ProcessEngineException e) {
			throw new SystemException("Cannot get form for task " + taskId, e);
		}

		FormDto dto = FormDto.fromFormData(formData);
		if (dto.getKey() == null || dto.getKey().isEmpty()) {
			if (formData != null && formData.getFormFields() != null && !formData.getFormFields().isEmpty()) {
				dto.setKey("embedded:engine://engine/:engine/task/" + taskId + "/rendered-form");
			}
		}
		if (dto.getKey() == null || dto.getKey().isEmpty()) {
			return "empty-task";
		}

		directProviderUtil.runWithoutAuthorization(() -> {
			String processDefinitionId = task.getProcessDefinitionId();
			String caseDefinitionId = task.getCaseDefinitionId();
			if (processDefinitionId != null) {
				dto.setContextPath(
						ApplicationContextPathUtil.getApplicationPathByProcessDefinitionId(directProviderUtil.getProcessEngine(user), processDefinitionId));

			} else if (caseDefinitionId != null) {
				dto.setContextPath(
						ApplicationContextPathUtil.getApplicationPathByCaseDefinitionId(directProviderUtil.getProcessEngine(user), caseDefinitionId));
			}
			return null;
		}, user);

		TaskForm taskForm = new TaskForm();
		CamundaFormRef camundaFormRef = dto.getCamundaFormRef();
		if (camundaFormRef != null)
			taskForm.setCamundaFormRef(new CamundaForm(camundaFormRef.getKey(), camundaFormRef.getBinding(),
					Integer.toString(camundaFormRef.getVersion())));
		taskForm.setContextPath(dto.getContextPath());
		taskForm.setKey(dto.getKey());
		return taskForm;

	}

	@Override
	public Collection<Task> findTasksByFilter(TaskFiltering filters, String filterId, CIBUser user, Integer firstResult,
			Integer maxResults) {
		List<?> entities = executeFilterList(filters, filterId, user, firstResult, maxResults);

		if (entities != null && !entities.isEmpty()) {
			List<Task> list = convertToDtoList(entities, user);
			return list;
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public Integer findTasksCountByFilter(String filterId, CIBUser user, TaskFiltering filters) {
		List<?> entities = executeFilterList(filters, filterId, user, null, null);
		return entities.size();
	}

	@Override
	public Collection<TaskHistory> findTasksByProcessInstanceHistory(String processInstanceId, CIBUser user) {
		List<TaskHistory> taskHistoryList = new ArrayList<>();
		List<HistoricTaskInstance> results = directProviderUtil.getProcessEngine(user).getHistoryService().createHistoricTaskInstanceQuery()
				.processInstanceId(processInstanceId).unlimitedList();
		for (HistoricTaskInstance result : results) {
			taskHistoryList.add(directProviderUtil.convertValue(HistoricTaskInstanceDto.fromHistoricTaskInstance(result), TaskHistory.class, user));
		}
		return taskHistoryList;
	}

	@Override
	public Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId,
			CIBUser user) {
		List<TaskHistory> taskHistoryList = new ArrayList<>();
		List<HistoricTaskInstance> results = directProviderUtil.getProcessEngine(user).getHistoryService().createHistoricTaskInstanceQuery()
				.taskDefinitionKey(taskDefinitionKey).processInstanceId(processInstanceId).unlimitedList();
		for (HistoricTaskInstance result : results) {
			taskHistoryList.add(directProviderUtil.convertValue(HistoricTaskInstanceDto.fromHistoricTaskInstance(result), TaskHistory.class, user));
		}
		return taskHistoryList;
	}

	@Override
	public Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException {
		TaskQueryDto queryDto = directProviderUtil.getObjectMapper(user).convertValue(data, TaskQueryDto.class);
		queryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		TaskQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));

		query.initializeFormKeys();
		List<org.cibseven.bpm.engine.task.Task> matchingTasks = QueryUtil.list(query, null, null);

		List<TaskDto> tasks = new ArrayList<>();
		if (Boolean.TRUE.equals(queryDto.getWithCommentAttachmentInfo())) {
			tasks = matchingTasks.stream().map(TaskWithAttachmentAndCommentDto::fromEntity).collect(Collectors.toList());
		} else {
			tasks = matchingTasks.stream().map(TaskDto::fromEntity).collect(Collectors.toList());
		}
		List<Task> resultTasks = new ArrayList<>();
		for (TaskDto matchingTask : tasks) {
			resultTasks.add(directProviderUtil.convertValue(matchingTask, Task.class, user));
		}
		return resultTasks;
	}

	@Override
	public Collection<IdentityLink> findIdentityLink(String taskId, Optional<String> type, CIBUser user) {
		List<org.cibseven.bpm.engine.task.IdentityLink> identityLinks = directProviderUtil.getProcessEngine(user).getTaskService().getIdentityLinksForTask(taskId);

		Collection<IdentityLink> result = new ArrayList<>();
		for (org.cibseven.bpm.engine.task.IdentityLink link : identityLinks) {
			if (type.isEmpty() || type.get().equals(link.getType())) {
				result.add(new IdentityLink(link.getUserId(), link.getGroupId(), link.getType()));
			}
		}
		return result;
	}

	@Override
	public void createIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
		String userId = (String) data.get("userId");
		String groupId = (String) data.get("groupId");
		if (userId != null && groupId != null) {
			throw new SystemException("Identity Link requires userId or groupId, but not both.");
		}

		if (userId == null && groupId == null) {
			throw new SystemException("Identity Link requires userId or groupId.");
		}

		String type = (String) data.get("type");
		if (userId != null) {
			directProviderUtil.getProcessEngine(user).getTaskService().addUserIdentityLink(taskId, userId, type);
		} else if (groupId != null) {
			directProviderUtil.getProcessEngine(user).getTaskService().addGroupIdentityLink(taskId, groupId, type);
		}
	}

	@Override
	public void deleteIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
		String userId = (String) data.get("userId");
		String groupId = (String) data.get("groupId");
		if (userId != null && groupId != null) {
			throw new SystemException("Identity Link requires userId or groupId, but not both.");
		}

		if (userId == null && groupId == null) {
			throw new SystemException("Identity Link requires userId or groupId.");
		}

		String type = (String) data.get("type");
		if (userId != null) {
			directProviderUtil.getProcessEngine(user).getTaskService().deleteUserIdentityLink(taskId, userId, type);
		} else if (groupId != null) {
			directProviderUtil.getProcessEngine(user).getTaskService().deleteGroupIdentityLink(taskId, groupId, type);
		}

	}

	@Override
	public void handleBpmnError(String taskId, Map<String, Object> data, CIBUser user) throws SystemException {
		TaskBpmnErrorDto dto = directProviderUtil.getObjectMapper(user).convertValue(data, TaskBpmnErrorDto.class);
		try {
			directProviderUtil.getProcessEngine(user).getTaskService().handleBpmnError(taskId, dto.getErrorCode(), dto.getErrorMessage(),
					VariableValueDto.toMap(dto.getVariables(), directProviderUtil.getProcessEngine(user), directProviderUtil.getObjectMapper(user)));
		} catch (NotFoundException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	public Collection<TaskHistory> findTasksByTaskIdHistory(String taskId, CIBUser user) {
		List<TaskHistory> taskHistoryList = new ArrayList<>();
		List<HistoricTaskInstance> results = directProviderUtil.getProcessEngine(user).getHistoryService().createHistoricTaskInstanceQuery().taskId(taskId).unlimitedList();
		for (HistoricTaskInstance result : results) {
			taskHistoryList.add(directProviderUtil.convertValue(HistoricTaskInstanceDto.fromHistoricTaskInstance(result), TaskHistory.class, user));
		}
		return taskHistoryList;
	}

	@Override
	public ResponseEntity<byte[]> getDeployedForm(String taskId, CIBUser user) {
		InputStream form = directProviderUtil.getProcessEngine(user).getFormService().getDeployedTaskForm(taskId);
		if (form != null) {
			try {
				byte[] bytes = IOUtils.toByteArray(form);
				ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(bytes, HttpStatusCode.valueOf(200));
				return responseEntity;
			} catch (IOException e) {
				throw new SystemException(e.getMessage());
			} finally {
				IoUtil.closeSilently(form);
			}
		}
		return new ResponseEntity<byte[]>(HttpStatusCode.valueOf(422));
	}

	@Override
	public Integer findHistoryTasksCount(Map<String, Object> filters, CIBUser user) {
		HistoricTaskInstanceQueryDto queryDto = directProviderUtil.getObjectMapper(user).convertValue(filters, HistoricTaskInstanceQueryDto.class);
		queryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		HistoricTaskInstanceQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));

		long count = query.count();
		return (int) count;
	}

	@Override
	public Collection<CandidateGroupTaskCount> getTaskCountByCandidateGroup(CIBUser user) {
		TaskCountByCandidateGroupResultDto reportDto = new TaskCountByCandidateGroupResultDto();
		List<TaskCountByCandidateGroupResult> results = reportDto.executeTaskCountByCandidateGroupReport(directProviderUtil.getProcessEngine(user));
		Collection<CandidateGroupTaskCount> resultTaskCount = new ArrayList<>();
		for (TaskCountByCandidateGroupResult result : results) {
			resultTaskCount.add(directProviderUtil.convertValue(TaskCountByCandidateGroupResultDto.fromTaskCountByCandidateGroupResultDto(result),
					CandidateGroupTaskCount.class, user));
		}
		return resultTaskCount;
	}

	private Collection<Task> convertTasks(Collection<org.cibseven.bpm.engine.task.Task> engineTasks, CIBUser user) {
		List<Task> resultList = new ArrayList<>();
		for (org.cibseven.bpm.engine.task.Task engineTask : engineTasks)
			resultList.add(directProviderUtil.getObjectMapper(user).convertValue(TaskDto.fromEntity(engineTask), Task.class));
		return resultList;
	}

	private List<org.cibseven.bpm.engine.task.Task> queryTasks(Map<String, Object> filters, CIBUser user) {
		ObjectMapper localObjectMapper = new ObjectMapper();
		JacksonConfigurator.configureObjectMapper(localObjectMapper);
		localObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		TaskQueryDto dto = localObjectMapper.convertValue(filters, TaskQueryDto.class);
		TaskQuery taskQuery = dto.toQuery(directProviderUtil.getProcessEngine(user));
		List<org.cibseven.bpm.engine.task.Task> taskList = taskQuery.taskInvolvedUser(user.getUserID()).list();
		return taskList;
	}

	protected org.cibseven.bpm.engine.task.Task getTaskById(String taskId, CIBUser user) {
		org.cibseven.bpm.engine.task.Task foundTask = directProviderUtil.getProcessEngine(user).getTaskService().createTaskQuery().taskId(taskId).initializeFormKeys()
				.singleResult();
		if (foundTask == null) {
			throw new NoObjectFoundException(new SystemException("No matching task with id " + taskId));
		}
		return foundTask;
	}

	private List<?> executeFilterList(TaskFiltering filters, String filterId, CIBUser user, Integer firstResult,
			Integer maxResults) {
		// authentication is required to access the current user while executing the
		// query
		GroupQuery groupQuery = directProviderUtil.getProcessEngine(user).getIdentityService().createGroupQuery();
		List<Group> userGroups = groupQuery.groupMember(user.getId()).orderByGroupName().asc().unlimitedList();
		List<String> groupNames = new ArrayList<>();
		for (Group userGroup : userGroups)
			groupNames.add(userGroup.getId());

		Authentication authentication = new Authentication(user.getId(), groupNames);
		directProviderUtil.getProcessEngine(user).getIdentityService().setAuthentication(authentication);

		String extendingQuery;
		try {
			extendingQuery = filters.json();
		} catch (JsonProcessingException e) {
			throw new SystemException("Failed json conversion", e);
		}
		List<?> entities = executeFilterList(extendingQuery, filterId, firstResult, maxResults, user);
		return entities;
	}

	private List<?> executeFilterList(String extendingQueryString, String filterId, Integer firstResult, Integer maxResults, CIBUser user) {
		Query<?, ?> extendingQuery = convertQuery(extendingQueryString, filterId, user);
		try {
			if (firstResult != null || maxResults != null) {
				if (firstResult == null) {
					firstResult = 0;
				}
				if (maxResults == null) {
					maxResults = Integer.MAX_VALUE;
				}
				return directProviderUtil.getProcessEngine(user).getFilterService().listPage(filterId, extendingQuery, firstResult, maxResults);
			} else {
				return directProviderUtil.getProcessEngine(user).getFilterService().list(filterId, extendingQuery);
			}
		} catch (NullValueException e) {
			throw new SystemException("Filter not found", e);
		}
	}

	private Query<?, ?> convertQuery(String queryString, String filterId, CIBUser user) {
		if (isEmptyJson(queryString)) {
			return null;
		} else {
			ProcessEngine processEngine = directProviderUtil.getProcessEngine(user);
			String resourceType = directProviderUtil.getProcessEngine(user).getFilterService().getFilter(filterId).getResourceType();

			AbstractQueryDto<?> queryDto = getQueryDtoForQuery(queryString, resourceType, user);
			queryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
			return queryDto.toQuery(processEngine);
		}
	}

	private AbstractQueryDto<?> getQueryDtoForQuery(String queryString, String resourceType, CIBUser user) {
		try {
			if (EntityTypes.TASK.equals(resourceType)) {
				return directProviderUtil.getObjectMapper(user).readValue(queryString, TaskQueryDto.class);
			} else {
				throw new SystemException(
						"Queries for resource type '" + resourceType + "' are currently not supported by filters.");
			}
		} catch (IOException e) {
			throw new SystemException("Invalid query for resource type '" + resourceType + "'", e);
		}
	}

	private List<Task> convertToDtoList(List<?> entities, CIBUser user) {
		List<Task> dtoList = new ArrayList<>();
		for (Object entity : entities) {
			dtoList.add(convertToDto(entity, user));
		}
		return dtoList;
	}

	private Task convertToDto(Object entity, CIBUser user) {
		if (entity instanceof org.cibseven.bpm.engine.task.Task) {
			return directProviderUtil.convertValue(TaskDto.fromEntity((org.cibseven.bpm.engine.task.Task) entity), Task.class, user);
		} else {
			throw new SystemException(
					"Entities of class '" + entity.getClass().getCanonicalName() + "' are currently not supported by filters.");
		}
	}

	private boolean isEmptyJson(String jsonString) {
		final Pattern EMPTY_JSON_BODY = Pattern.compile("\\s*\\{\\s*\\}\\s*");
		return jsonString == null || jsonString.trim().isEmpty() || EMPTY_JSON_BODY.matcher(jsonString).matches();
	}

	@Override
	public void submit(String taskId, String formResult, CIBUser user) {
    ProcessEngine engine = directProviderUtil.getProcessEngine(user);
		FormService formService = engine.getFormService();
		ObjectMapper objectMapper = directProviderUtil.getObjectMapper(user);
		CompleteTaskDto dto;
		try {
			dto = objectMapper.readValue(formResult, CompleteTaskDto.class);
		} catch (JsonProcessingException e) {
			throw new SystemException(e.getMessage(), e);
		} 

		try {
			VariableMap variables = VariableValueDto.toMap(dto.getVariables(), engine, objectMapper);
			if (dto.isWithVariablesInReturn()) {
				formService.submitTaskFormWithVariablesInReturn(taskId, variables, false);
				} else {
					formService.submitTaskForm(taskId, variables);
				}

		} catch (RestException|ProcessEngineException e) {
			String errorMessage = String.format("Cannot submit task form %s: %s", taskId, e.getMessage());
			throw new SystemException(errorMessage, e);
		}
		
	}

	@Override
	public ResponseEntity<String> getRenderedForm(String taskId, Map<String, Object> params, CIBUser user) {
    FormService formService = directProviderUtil.getProcessEngine(user).getFormService();

		Object renderedTaskForm = formService.getRenderedTaskForm(taskId);
		if(renderedTaskForm != null) {
			String content = renderedTaskForm.toString();
			InputStream stream = new ByteArrayInputStream(content.getBytes(EncodingUtil.DEFAULT_ENCODING));
			try {
				return new ResponseEntity<String>(IOUtils.toString(stream, Charset.defaultCharset()), HttpStatusCode.valueOf(200));
			} catch (IOException e) {
				throw new SystemException(e.getMessage(), e);
			}
		}

		throw new SystemException("No matching rendered form for task with the id " + taskId + " found.");
	}
}
