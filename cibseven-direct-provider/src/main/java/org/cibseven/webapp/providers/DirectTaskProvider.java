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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.cibseven.bpm.engine.rest.dto.CountResultDto;
import org.cibseven.bpm.engine.rest.dto.VariableValueDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricTaskInstanceDto;
import org.cibseven.bpm.engine.rest.dto.task.CompleteTaskDto;
import org.cibseven.bpm.engine.rest.dto.task.FormDto;
import org.cibseven.bpm.engine.rest.dto.task.IdentityLinkDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskBpmnErrorDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskQueryDto;
import org.cibseven.bpm.engine.rest.dto.task.UserIdDto;
import org.cibseven.bpm.engine.rest.exception.RestException;
import org.cibseven.bpm.engine.rest.impl.FilterRestServiceImpl;
import org.cibseven.bpm.engine.rest.impl.TaskRestServiceImpl;
import org.cibseven.bpm.engine.rest.impl.history.HistoricTaskInstanceRestServiceImpl;
import org.cibseven.bpm.engine.rest.sub.runtime.FilterResource;
import org.cibseven.bpm.engine.rest.sub.task.TaskReportResource;
import org.cibseven.bpm.engine.rest.sub.task.TaskResource;
import org.cibseven.bpm.engine.variable.VariableMap;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.CandidateGroupTaskCount;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskHistory;
import org.cibseven.webapp.rest.model.Variable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DirectTaskProvider extends SevenProviderBase implements ITaskProvider {

	DirectProviderUtil directProviderUtil;
	public DirectTaskProvider(DirectProviderUtil directProviderUtil){
		this.directProviderUtil = directProviderUtil;
	}

	@Override
	public Collection<Task> findTasks(String filter, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));

		Integer firstResult = null;
		Integer maxResults = null;
		MultivaluedMap<String, String> filters = new MultivaluedHashMap<>();
		String[] splitFilter = filter.split("&");
		for (String params : splitFilter) {
			String[] splitValue = params.split("=");
			if (splitValue.length > 1) {
				if (splitValue[0].equals("firstResult")) {
					firstResult = Integer.parseInt(splitValue[1]);
				} else if (splitValue[0].equals("maxResults")) {
					maxResults = Integer.parseInt(splitValue[1]);
				} else {
					filters.put(splitValue[0], Arrays.asList(URLDecoder.decode(splitValue[1], Charset.forName("UTF-8"))));
				}
			}
		}
		
		UriInfo uriInfo = new DirectUriInfo(filters);
		DirectRequest request = new DirectRequest();
		Object tasks = taskRestServiceImpl.getTasks(request, uriInfo, firstResult, maxResults);
		return convertTasksObjectToCollection(tasks, user);
	}

	private Collection<Task> convertTasksObjectToCollection(Object tasks, CIBUser user) {
		List<Task> resultList = new ArrayList<>();
		if (tasks instanceof List<?>) {
		    List<?> taskList = (List<?>) tasks;
		    if (taskList.isEmpty() || taskList.get(0) instanceof TaskDto) {
		        for (Object obj : taskList) {
		            resultList.add(directProviderUtil.getObjectMapper(user).convertValue(obj, Task.class));
		        }
		    } else {
		        throw new IllegalStateException("Expected List<TaskDto> but got a List of " + taskList.get(0).getClass());
		    }
		} else {
		    throw new IllegalStateException("Expected List<TaskDto> but got " + tasks.getClass());
		}
		return resultList;
	}
	
	private Task convertTaskObjectToTask(Object task, CIBUser user) {
    if (task == null)
    	return null;
    if (task != null && task instanceof TaskDto) {
          return directProviderUtil.getObjectMapper(user).convertValue(task, Task.class);
    } else {
      throw new IllegalStateException("Expected TaskDto but got  " + task.getClass());
    }
	}
	
	@Override
	public Integer findTasksCount(@RequestBody Map<String, Object> filters, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		UriInfo uriInfo = new DirectUriInfo(filters);
		CountResultDto countResultDto = taskRestServiceImpl.getTasksCount(uriInfo);
		return (int)countResultDto.getCount();
	}

	@Override
	public Collection<Task> findTasksByProcessInstance(String processInstanceId, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
		queryParameters.add("processInstanceId", processInstanceId);
		UriInfo uriInfo = new DirectUriInfo(queryParameters);
		DirectRequest request = new DirectRequest();
		
		Object tasks = taskRestServiceImpl.getTasks(request, uriInfo, null, null);
		return convertTasksObjectToCollection(tasks, user);
	}

	@Override
	public Collection<Task> findTasksByProcessInstanceAsignee(Optional<String> processInstanceId,
			Optional<String> createdAfter, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
		if (processInstanceId.isPresent())
			queryParameters.add("processInstanceId", processInstanceId.get());
		
		queryParameters.add("assignee=", user.getId());
		if (createdAfter.isPresent())
			queryParameters.add("createdAfter=", createdAfter.get());
		
		UriInfo uriInfo = new DirectUriInfo(queryParameters);
		DirectRequest request = new DirectRequest();

		Object tasks = taskRestServiceImpl.getTasks(request, uriInfo, null, null);
		return convertTasksObjectToCollection(tasks, user);
	}

	@Override
	public Task findTaskById(String id, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(id, false, false, false);
		DirectRequest request = new DirectRequest();
		Object task = taskResource.getTask(request);
		return convertTaskObjectToTask(task, user);
	}

	@Override
	public void update(Task task, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(task.getId(), false, false, false);
		TaskDto restTask = directProviderUtil.getObjectMapper(user).convertValue(task, TaskDto.class);
		taskResource.updateTask(restTask);
	}

	@Override
	public void setAssignee(String taskId, String assignee, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);

		if (assignee == null)
			taskResource.unclaim();
		else {
			UserIdDto dto = new UserIdDto();
			dto.setUserId(assignee);
			taskResource.setAssignee(dto);
		}
	}

	@Override
	public void submit(String taskId, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);
		taskResource.submit(new CompleteTaskDto());
		VariableMap variables = null;
		directProviderUtil.getProcessEngine(user).getFormService().submitTaskForm(taskId, variables);
	}

	@Override
	public void submit(Task task, List<Variable> formResult, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(task.getId(), false, false, false);
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
			taskResource.submit(completeTaskDto);
		} catch (RestException e) {
			throw wrapException(e, user);
		}
	}

	@Override
	public Object formReference(String taskId, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);
		Map<String, VariableValueDto> variableDtos = taskResource.getFormVariables("formReference", true);
		return variableDtos;
	}

	@Override
	public Object form(String taskId, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);
		FormDto formData = taskResource.getForm();
		return formData;
	}

	@Override
	public Collection<Task> findTasksByFilter(TaskFiltering filters, String filterId, CIBUser user, Integer firstResult,
			Integer maxResults) {
		FilterRestServiceImpl filterRestServiceImpl = new FilterRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		FilterResource filterResource = filterRestServiceImpl.getFilter(filterId);
		DirectRequest request = new DirectRequest();

		Object tasks = filterResource.executeList(request, firstResult, maxResults);
		return convertTasksObjectToCollection(tasks, user);
	}

	@Override
	public Integer findTasksCountByFilter(String filterId, CIBUser user, TaskFiltering filters) {
		FilterRestServiceImpl filterRestServiceImpl = new FilterRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		FilterResource filterResource = filterRestServiceImpl.getFilter(filterId);
		try {
			CountResultDto dto = filterResource.queryCount(filters.json());
			if (dto == null)
				throw new NullPointerException();
			return (int)dto.getCount();
		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in findTasksCountByFilter(...):", se);
			throw se;
		}
	}

	@Override
	public Collection<TaskHistory> findTasksByProcessInstanceHistory(String processInstanceId, CIBUser user) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("processInstanceId", processInstanceId);
		filters.put("sortBy", "startTime");
		filters.put("sortOrder", "desc");
		return findTasksByFilters(filters, user);
	}

	@Override
	public Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId,
			CIBUser user) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("processInstanceId", processInstanceId);
		filters.put("taskDefinitionKey", taskDefinitionKey);
		return findTasksByFilters(filters, user);
	}

	@Override
	public Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));

		Integer firstResult = null;
		Integer maxResults = null;
		for (Entry<String, Object> entry : data.entrySet()) {
			if (entry.getKey().equals("firstResult"))
				firstResult = Integer.parseInt((String) data.get("firstResult"));
			else if (entry.getKey().equals("maxResults"))
				maxResults = Integer.parseInt((String) data.get("maxResults"));
		}

		TaskQueryDto queryDto = directProviderUtil.getObjectMapper(user).convertValue(data, TaskQueryDto.class);
		List<TaskDto> tasks = taskRestServiceImpl.queryTasks(queryDto, firstResult, maxResults);		

		List<Task> resultTasks = new ArrayList<>();
		for (TaskDto matchingTask : tasks) {
			resultTasks.add(directProviderUtil.convertValue(matchingTask, Task.class, user));
		}
		return resultTasks;
	}

	@Override
	public Collection<IdentityLink> findIdentityLink(String taskId, Optional<String> type, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);
	  List<IdentityLinkDto> links = taskResource.getIdentityLinks(type.orElse(null));
		Collection<IdentityLink> result = new ArrayList<>();
		for (IdentityLinkDto link : links) {
			result.add(directProviderUtil.convertValue(link, IdentityLink.class, user));
		}
		return result;
	}

	@Override
	public void createIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);
		taskResource.addIdentityLink(directProviderUtil.getObjectMapper(user).convertValue(data, IdentityLinkDto.class));
	}

	@Override
	public void deleteIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);
		taskResource.deleteIdentityLink(directProviderUtil.getObjectMapper(user).convertValue(data, IdentityLinkDto.class));
	}

	@Override
	public void handleBpmnError(String taskId, Map<String, Object> data, CIBUser user) throws SystemException {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);
		taskResource.handleBpmnError(directProviderUtil.getObjectMapper(user).convertValue(data, TaskBpmnErrorDto.class));
	}

	@Override
	public Collection<TaskHistory> findTasksByTaskIdHistory(String taskId, CIBUser user) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("taskId", taskId);
		return findTasksByFilters(filters, user);
	}

	private Collection<TaskHistory> findTasksByFilters(Map<String, Object> filters, CIBUser user) {
		UriInfo uriInfo = new DirectUriInfo(filters);
		HistoricTaskInstanceRestServiceImpl historicTaskInstanceRestServiceImpl = new HistoricTaskInstanceRestServiceImpl(
				directProviderUtil.getObjectMapper(user), directProviderUtil.getProcessEngine(user));
		List<HistoricTaskInstanceDto> instances = historicTaskInstanceRestServiceImpl.getHistoricTaskInstances(uriInfo, null, null);
		
		List<TaskHistory> taskHistoryList = new ArrayList<>();
		for (HistoricTaskInstanceDto instance : instances) {
			taskHistoryList.add(directProviderUtil.convertValue(instance, TaskHistory.class, user));
		}
		return taskHistoryList;
	}

	@Override
	public ResponseEntity<byte[]> getDeployedForm(String taskId, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);
	  try {
		Response response = taskResource.getDeployedForm();
	  return new ResponseEntity<byte[]>(IOUtils.toByteArray((InputStream) response.getEntity()), HttpStatusCode.valueOf(response.getStatus()));
	  } catch (RestException e) {
	  	throw wrapException(e, user);
	  } catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	public Integer findHistoryTasksCount(Map<String, Object> filters, CIBUser user) {
		UriInfo uriInfo = new DirectUriInfo(filters);
		HistoricTaskInstanceRestServiceImpl historicTaskInstanceRestServiceImpl = new HistoricTaskInstanceRestServiceImpl(
				directProviderUtil.getObjectMapper(user), directProviderUtil.getProcessEngine(user));
		CountResultDto dto = historicTaskInstanceRestServiceImpl.getHistoricTaskInstancesCount(uriInfo);
		return (int)dto.getCount();
	}

	@Override
	public Collection<CandidateGroupTaskCount> getTaskCountByCandidateGroup(CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
	  TaskReportResource taskReportResource = taskRestServiceImpl.getTaskReportResource();
	  DirectRequest request = new DirectRequest();
	  Response response = taskReportResource.getTaskCountByCandidateGroupReport(request);
	  Object entity = response.getEntity();
	  if (entity != null && entity instanceof List<?>) {
	  	List<?> entityList = (List<?>) entity;
	  	if (!entityList.isEmpty() && entityList.get(0) instanceof CandidateGroupTaskCount) {
	  		return entityList.stream().filter(e -> e instanceof CandidateGroupTaskCount).map(e -> (CandidateGroupTaskCount) e).collect(Collectors.toList());
	  	} else {
	  		throw new IllegalStateException("Expected List<CandidateGroupTaskCount> but got a List of " + entityList.get(0).getClass());
	  	}
	  } else {
	  	throw new IllegalStateException("Expected List<CandidateGroupTaskCount> but got " + (entity != null ? entity.getClass() : "null"));
	  }
	}

	@Override
	public void submit(String taskId, String formResult, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);
		ObjectMapper objectMapper = directProviderUtil.getObjectMapper(user);
		CompleteTaskDto dto;
		try {
			dto = objectMapper.readValue(formResult, CompleteTaskDto.class);
		} catch (JsonProcessingException e) {
			throw new SystemException(e.getMessage(), e);
		} 
	  try {
	  	taskResource.submit(dto);
		} catch (RestException e) {
	  	throw wrapException(e, user);
	  }
	}

	@Override
	public ResponseEntity<String> getRenderedForm(String taskId, Map<String, Object> params, CIBUser user) {
		TaskRestServiceImpl taskRestServiceImpl = new TaskRestServiceImpl(directProviderUtil.getEngineName(user), directProviderUtil.getObjectMapper(user));
		TaskResource taskResource = taskRestServiceImpl.getTask(taskId, false, false, false);
		Response response = taskResource.getRenderedForm();
		Object entity = response.getEntity();
		if (entity != null) {
			try {
				return new ResponseEntity<String>(IOUtils.toString((InputStream) entity, Charset.defaultCharset()), HttpStatusCode.valueOf(response.getStatus()));
			} catch (IOException e) {
				throw new SystemException(e.getMessage(), e);
			}
		} else {
			throw new SystemException("No matching rendered form for task with the id " + taskId + " found.");
		}
	}
}