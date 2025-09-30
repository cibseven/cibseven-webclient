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
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.cibseven.bpm.dmn.engine.DmnDecisionResultEntries;
import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.bpm.engine.EntityTypes;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.authorization.AuthorizationQuery;
import org.cibseven.bpm.engine.authorization.Permissions;
import org.cibseven.bpm.engine.exception.NullValueException;
import org.cibseven.bpm.engine.history.HistoricActivityInstance;
import org.cibseven.bpm.engine.history.HistoricActivityInstanceQuery;
import org.cibseven.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.cibseven.bpm.engine.history.HistoricIncident;
import org.cibseven.bpm.engine.history.HistoricIncidentQuery;
import org.cibseven.bpm.engine.history.HistoricProcessInstance;
import org.cibseven.bpm.engine.history.HistoricProcessInstanceQuery;
import org.cibseven.bpm.engine.history.HistoricVariableInstance;
import org.cibseven.bpm.engine.history.HistoricVariableInstanceQuery;
import org.cibseven.bpm.engine.identity.Group;
import org.cibseven.bpm.engine.identity.GroupQuery;
import org.cibseven.bpm.engine.identity.UserQuery;
import org.cibseven.bpm.engine.impl.RuntimeServiceImpl;
import org.cibseven.bpm.engine.impl.calendar.DateTimeUtil;
import org.cibseven.bpm.engine.impl.identity.Authentication;
import org.cibseven.bpm.engine.impl.util.IoUtil;
import org.cibseven.bpm.engine.impl.util.PermissionConverter;
import org.cibseven.bpm.engine.management.Metrics;
import org.cibseven.bpm.engine.management.MetricsQuery;
import org.cibseven.bpm.engine.query.Query;
import org.cibseven.bpm.engine.repository.DecisionDefinition;
import org.cibseven.bpm.engine.repository.DecisionDefinitionQuery;
import org.cibseven.bpm.engine.repository.DeploymentBuilder;
import org.cibseven.bpm.engine.repository.DeploymentWithDefinitions;
import org.cibseven.bpm.engine.repository.Resource;
import org.cibseven.bpm.engine.rest.dto.AbstractQueryDto;
import org.cibseven.bpm.engine.rest.dto.VariableValueDto;
import org.cibseven.bpm.engine.rest.dto.authorization.AuthorizationDto;
import org.cibseven.bpm.engine.rest.dto.authorization.AuthorizationQueryDto;
import org.cibseven.bpm.engine.rest.dto.converter.DateConverter;
import org.cibseven.bpm.engine.rest.dto.history.HistoricActivityInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricActivityInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricDecisionInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricIncidentDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricIncidentQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricProcessInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricProcessInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricVariableInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricVariableInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.identity.UserQueryDto;
import org.cibseven.bpm.engine.rest.dto.message.CorrelationMessageDto;
import org.cibseven.bpm.engine.rest.dto.message.MessageCorrelationResultDto;
import org.cibseven.bpm.engine.rest.dto.message.MessageCorrelationResultWithVariableDto;
import org.cibseven.bpm.engine.rest.dto.repository.CalledProcessDefinitionDto;
import org.cibseven.bpm.engine.rest.dto.repository.DecisionDefinitionDiagramDto;
import org.cibseven.bpm.engine.rest.dto.repository.DeploymentDto;
import org.cibseven.bpm.engine.rest.dto.repository.DeploymentResourceDto;
import org.cibseven.bpm.engine.rest.dto.repository.RedeploymentDto;
import org.cibseven.bpm.engine.rest.dto.runtime.IncidentDto;
import org.cibseven.bpm.engine.rest.dto.runtime.IncidentQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ProcessInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.StartProcessInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.modification.ProcessInstanceModificationInstructionDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskQueryDto;
import org.cibseven.bpm.engine.rest.exception.InvalidRequestException;
import org.cibseven.bpm.engine.rest.exception.RestException;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.rest.util.QueryUtil;
import org.cibseven.bpm.engine.runtime.DeserializationTypeValidator;
import org.cibseven.bpm.engine.runtime.IncidentQuery;
import org.cibseven.bpm.engine.runtime.MessageCorrelationBuilder;
import org.cibseven.bpm.engine.runtime.MessageCorrelationResult;
import org.cibseven.bpm.engine.runtime.MessageCorrelationResultWithVariables;
import org.cibseven.bpm.engine.runtime.ProcessInstanceQuery;
import org.cibseven.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.cibseven.bpm.engine.runtime.ProcessInstantiationBuilder;
import org.cibseven.bpm.engine.runtime.VariableInstanceQuery;
import org.cibseven.bpm.engine.task.TaskQuery;
import org.cibseven.bpm.engine.variable.VariableMap;
import org.cibseven.bpm.engine.variable.Variables;
import org.cibseven.bpm.engine.variable.impl.type.AbstractValueTypeImpl;
import org.cibseven.bpm.engine.variable.type.FileValueType;
import org.cibseven.bpm.engine.variable.type.ValueType;
import org.cibseven.bpm.engine.variable.value.BytesValue;
import org.cibseven.bpm.engine.variable.value.FileValue;
import org.cibseven.bpm.engine.variable.value.TypedValue;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.Incident;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.cibseven.webapp.rest.model.VariableInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;
import jakarta.ws.rs.core.Response.Status;

public class DirectProviderUtil {
	
	@Value("${cibseven.webclient.users.search.wildcard:}") String wildcard;
  //decides about ldap/adfs
	@Value("${cibseven.webclient.user.provider:org.cibseven.webapp.auth.SevenUserProvider}") String userProvider;

	public final static String DEPLOYMENT_NAME = "deployment-name";
	public final static String DEPLOYMENT_ACTIVATION_TIME = "deployment-activation-time";
	public final static String ENABLE_DUPLICATE_FILTERING = "enable-duplicate-filtering";
	public final static String DEPLOY_CHANGED_ONLY = "deploy-changed-only";
	public final static String DEPLOYMENT_SOURCE = "deployment-source";
	public final static String TENANT_ID = "tenant-id";

	protected static final Set<String> RESERVED_KEYWORDS = new HashSet<String>();

	static {
		RESERVED_KEYWORDS.add(DEPLOYMENT_NAME);
		RESERVED_KEYWORDS.add(DEPLOYMENT_ACTIVATION_TIME);
		RESERVED_KEYWORDS.add(ENABLE_DUPLICATE_FILTERING);
		RESERVED_KEYWORDS.add(DEPLOY_CHANGED_ONLY);
		RESERVED_KEYWORDS.add(DEPLOYMENT_SOURCE);
		RESERVED_KEYWORDS.add(TENANT_ID);
	}

	public static final String DEFAULT_BINARY_VALUE_TYPE = "Bytes";

	protected ProcessEngine processEngine;
	protected ObjectMapper objectMapper;	
	
	public DirectProviderUtil(ProcessEngine processEngine, ObjectMapper objectMapper) {
		this.processEngine = processEngine;
		this.objectMapper = objectMapper;
	}

	/**
	 * conversion and helper functions
	 */
	public <T> T convertValue(Object fromValueDto, Class<T> toValueType) throws IllegalArgumentException {
		Map<String, Object> filterDtoMap = objectMapper.convertValue(fromValueDto, new TypeReference<Map<String, Object>>() {
		});
		return objectMapper.convertValue(filterDtoMap, toValueType);
	}

	public List<Task> convertToDtoList(List<?> entities, ObjectMapper objectMapper) {
		List<Task> dtoList = new ArrayList<>();
		for (Object entity : entities) {
			dtoList.add(convertToDto(entity, objectMapper));
		}
		return dtoList;
	}
	
	public Task convertToDto(Object entity, ObjectMapper objectMapper) {
		if (entity instanceof org.cibseven.bpm.engine.task.Task) {
			return convertValue(TaskDto.fromEntity((org.cibseven.bpm.engine.task.Task) entity), Task.class);
		} else {
			throw new SystemException(
					"Entities of class '" + entity.getClass().getCanonicalName() + "' are currently not supported by filters.");
		}
	}
	
	public List<?> executeFilterList(String extendingQueryString, String filterId, Integer firstResult, Integer maxResults) {
		Query<?, ?> extendingQuery = convertQuery(extendingQueryString, filterId);
		try {
			if (firstResult != null || maxResults != null) {
				if (firstResult == null) {
					firstResult = 0;
				}
				if (maxResults == null) {
					maxResults = Integer.MAX_VALUE;
				}
				return processEngine.getFilterService().listPage(filterId, extendingQuery, firstResult, maxResults);
			} else {
				return processEngine.getFilterService().list(filterId, extendingQuery);
			}
		} catch (NullValueException e) {
			throw new SystemException("Filter not found", e);
		}
	}
	
	public List<?> executeFilterList(TaskFiltering filters, String filterId, CIBUser user, Integer firstResult,
			Integer maxResults) {
		// authentication is required to access the current user while executing the
		// query
		GroupQuery groupQuery = processEngine.getIdentityService().createGroupQuery();
		List<Group> userGroups = groupQuery.groupMember(user.getId()).orderByGroupName().asc().unlimitedList();
		List<String> groupNames = new ArrayList<>();
		for (Group userGroup : userGroups)
			groupNames.add(userGroup.getId());
	
		Authentication authentication = new Authentication(user.getId(), groupNames);
		processEngine.getIdentityService().setAuthentication(authentication);
	
		String extendingQuery;
		try {
			extendingQuery = filters.json();
		} catch (JsonProcessingException e) {
			throw new SystemException("Failed json conversion", e);
		}
		List<?> entities = executeFilterList(extendingQuery, filterId, firstResult, maxResults);
		return entities;
	}
	

	public Query<?, ?> convertQuery(String queryString, String filterId) {
		if (isEmptyJson(queryString)) {
			return null;
		} else {
			String resourceType = processEngine.getFilterService().getFilter(filterId).getResourceType();
	
			AbstractQueryDto<?> queryDto = getQueryDtoForQuery(queryString, resourceType);
			queryDto.setObjectMapper(objectMapper);
			return queryDto.toQuery(processEngine);
		}
	}
	
	public AbstractQueryDto<?> getQueryDtoForQuery(String queryString, String resourceType) {
		try {
			if (EntityTypes.TASK.equals(resourceType)) {
				return objectMapper.readValue(queryString, TaskQueryDto.class);
			} else {
				throw new SystemException(
						"Queries for resource type '" + resourceType + "' are currently not supported by filters.");
			}
		} catch (IOException e) {
			throw new SystemException("Invalid query for resource type '" + resourceType + "'", e);
		}
	}
	
	public boolean isEmptyJson(String jsonString) {
		final Pattern EMPTY_JSON_BODY = Pattern.compile("\\s*\\{\\s*\\}\\s*");
		return jsonString == null || jsonString.trim().isEmpty() || EMPTY_JSON_BODY.matcher(jsonString).matches();
	}
	
	public Collection<Task> convertTasks(Collection<org.cibseven.bpm.engine.task.Task> engineTasks) {
		List<Task> resultList = new ArrayList<>();
		for (org.cibseven.bpm.engine.task.Task engineTask : engineTasks)
			resultList.add(objectMapper.convertValue(TaskDto.fromEntity(engineTask), Task.class));
		return resultList;
	}
	
	public List<org.cibseven.bpm.engine.task.Task> queryTasks(Map<String, Object> filters, CIBUser user) {
		ObjectMapper localObjectMapper = new ObjectMapper();
		JacksonConfigurator.configureObjectMapper(localObjectMapper);
		// TODO: 'unfinished' is requested but not supported by the TaskQuery ->
		// create Ticket!
		localObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		TaskQueryDto dto = localObjectMapper.convertValue(filters, TaskQueryDto.class);
		TaskQuery taskQuery = dto.toQuery(processEngine);
		List<org.cibseven.bpm.engine.task.Task> taskList = taskQuery.taskInvolvedUser(user.getUserID()).list();
		return taskList;
	}

	public Boolean getBooleanValueFromObject(Object value) {
		return objectMapper.convertValue(value, Boolean.class);
	}

	public Long queryProcessInstancesCount(ProcessInstanceQueryDto queryDto) {
		queryDto.setObjectMapper(objectMapper);
		ProcessInstanceQuery query = queryDto.toQuery(processEngine);
		return query.count();
	}
	
	public ProcessInstanceWithVariables startProcessInstanceAtActivities(StartProcessInstanceDto dto,
			String processDefinitionKey) {
		Map<String, Object> processInstanceVariables = VariableValueDto.toMap(dto.getVariables(), processEngine,
				objectMapper);
		String businessKey = dto.getBusinessKey();
		String caseInstanceId = dto.getCaseInstanceId();
	
		ProcessInstantiationBuilder instantiationBuilder = processEngine.getRuntimeService().createProcessInstanceById(processDefinitionKey)
				.businessKey(businessKey).caseInstanceId(caseInstanceId).setVariables(processInstanceVariables);
	
		if (dto.getStartInstructions() != null && !dto.getStartInstructions().isEmpty()) {
			for (ProcessInstanceModificationInstructionDto instruction : dto.getStartInstructions()) {
				instruction.applyTo(instantiationBuilder, processEngine, objectMapper);
			}
		}
	
		return instantiationBuilder.executeWithVariablesInReturn(dto.isSkipCustomListeners(), dto.isSkipIoMappings());
	}
	
	public Collection<HistoryProcessInstance> queryHistoryProcessInstances(
			HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto, Integer firstResult, Integer maxResults) {
		historicProcessInstanceQueryDto.setObjectMapper(objectMapper);
	
		HistoricProcessInstanceQuery query = historicProcessInstanceQueryDto.toQuery(processEngine);
		List<HistoricProcessInstance> matchingHistoricProcessInstances = QueryUtil.list(query, firstResult, maxResults);
	
		List<HistoryProcessInstance> HistoryProcessInstanceResults = new ArrayList<HistoryProcessInstance>();
		for (HistoricProcessInstance historicProcessInstance : matchingHistoricProcessInstances) {
			HistoricProcessInstanceDto resultHistoricProcessInstanceDto = HistoricProcessInstanceDto
					.fromHistoricProcessInstance(historicProcessInstance);
			HistoryProcessInstanceResults.add(convertValue(resultHistoricProcessInstanceDto, HistoryProcessInstance.class));
		}
		return HistoryProcessInstanceResults;
	}
	
	public static Process convertToProcess(CalledProcessDefinitionDto dto) {
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> filterDtoMap = objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {
		});
		return objectMapper.convertValue(filterDtoMap, Process.class);
	}

	public DeploymentBuilder extractDeploymentInformation(MultipartFile[] files, MultiValueMap<String, Object> data) {
		DeploymentBuilder deploymentBuilder = processEngine.getRepositoryService().createDeployment();

		for (MultipartFile file : files) {
			String fileName = file.getOriginalFilename();
			if (fileName != null) {
				try {
					deploymentBuilder.addInputStream(fileName, new ByteArrayInputStream(file.getBytes()));
				} catch (IOException e) {
					throw new SystemException(e.getMessage(), e);
				}
			} else {
				throw new SystemException(
						"No file name found in the deployment resource described by form parameter '" + fileName + "'.");
			}
		}
		String deploymentName = getStringValue(DEPLOYMENT_NAME, data);
		if (deploymentName != null) {
			deploymentBuilder.name(deploymentName);
		}
	
		String deploymentActivationTime = getStringValue(DEPLOYMENT_ACTIVATION_TIME, data);
		if (deploymentActivationTime != null) {
			deploymentBuilder.activateProcessDefinitionsOn(DateTimeUtil.parseDate(deploymentActivationTime));
		}
	
		String deploymentSource = getStringValue(DEPLOYMENT_SOURCE, data);
		if (deploymentSource != null) {
			deploymentBuilder.source(deploymentSource);
		}
	
		String deploymentTenantId = getStringValue(TENANT_ID, data);
		if (deploymentTenantId != null) {
			deploymentBuilder.tenantId(deploymentTenantId);
		}
	
		extractDuplicateFilteringForDeployment(data, deploymentBuilder);
		return deploymentBuilder;
	}
	
	public String getStringValue(String key, MultiValueMap<String, Object> data) {
		if (data.containsKey(key)) {
			List<Object> entryData = data.get(key);
			if (!entryData.isEmpty() && entryData.get(0) instanceof String)
				return (String)entryData.get(0);
		}
		return null;
	}
	
	public void extractDuplicateFilteringForDeployment(MultiValueMap<String, Object> data, DeploymentBuilder deploymentBuilder) {
		boolean enableDuplicateFiltering = false;
		boolean deployChangedOnly = false;
	
		String enableDuplicateFilteringValue = getStringValue(ENABLE_DUPLICATE_FILTERING, data);
		if (enableDuplicateFilteringValue != null)
			enableDuplicateFiltering = Boolean.parseBoolean(enableDuplicateFilteringValue);
		
		String deployChangedOnlyValue = getStringValue(DEPLOY_CHANGED_ONLY, data);
		if (deployChangedOnlyValue != null)
			deployChangedOnly = Boolean.parseBoolean(deployChangedOnlyValue);
	
		// deployChangedOnly overrides the enableDuplicateFiltering setting
		if (deployChangedOnly) {
			deploymentBuilder.enableDuplicateFiltering(true);
		} else if (enableDuplicateFiltering) {
			deploymentBuilder.enableDuplicateFiltering(false);
		}
	}
	
	public DeploymentResourceDto getDeploymentResource(String resourceId, String deploymentId) {
		List<DeploymentResourceDto> deploymentResources = getDeploymentResources(deploymentId);
		for (DeploymentResourceDto deploymentResource : deploymentResources) {
			if (deploymentResource.getId().equals(resourceId)) {
				return deploymentResource;
			}
		}
	
		throw new SystemException("Deployment resource with resource id '" + resourceId + "' for deployment id '"
				+ deploymentId + "' does not exist.");
	}
	
	public List<DeploymentResourceDto> getDeploymentResources(String deploymentId) {
		List<Resource> resources = processEngine.getRepositoryService().getDeploymentResources(deploymentId);
		List<DeploymentResourceDto> deploymentResources = new ArrayList<DeploymentResourceDto>();
		for (Resource resource : resources) {
			deploymentResources.add(DeploymentResourceDto.fromResources(resource));
		}
	
		if (!deploymentResources.isEmpty()) {
			return deploymentResources;
		} else {
			throw new SystemException("Deployment resources for deployment id '" + deploymentId + "' do not exist.");
		}
	}
	
	public List<ActivityInstanceHistory> queryHistoricActivityInstance(
			HistoricActivityInstanceQueryDto queryHistoricActivityInstanceDto) {
		queryHistoricActivityInstanceDto.setObjectMapper(objectMapper);
		HistoricActivityInstanceQuery query = queryHistoricActivityInstanceDto.toQuery(processEngine);
		List<HistoricActivityInstance> matchingHistoricActivityInstances = QueryUtil.list(query, null, null);
	
		List<ActivityInstanceHistory> historicActivityInstanceResults = new ArrayList<>();
		for (HistoricActivityInstance historicActivityInstance : matchingHistoricActivityInstances) {
			HistoricActivityInstanceDto resultHistoricActivityInstance = new HistoricActivityInstanceDto();
			HistoricActivityInstanceDto.fromHistoricActivityInstance(resultHistoricActivityInstance, historicActivityInstance);
			historicActivityInstanceResults.add(convertValue(resultHistoricActivityInstance, ActivityInstanceHistory.class));
		}
		return historicActivityInstanceResults;
	}

	public List<MessageCorrelationResultDto> correlate(CorrelationMessageDto messageDto,
			MessageCorrelationBuilder correlation) {
		List<MessageCorrelationResultDto> resultDtos = new ArrayList<>();
		if (!messageDto.isAll()) {
			MessageCorrelationResult result = correlation.correlateWithResult();
			resultDtos.add(MessageCorrelationResultDto.fromMessageCorrelationResult(result));
		} else {
			List<MessageCorrelationResult> results = correlation.correlateAllWithResult();
			for (MessageCorrelationResult result : results) {
				resultDtos.add(MessageCorrelationResultDto.fromMessageCorrelationResult(result));
			}
		}
		return resultDtos;
	}
	
	public List<MessageCorrelationResultWithVariableDto> correlateWithVariablesEnabled(CorrelationMessageDto messageDto,
			MessageCorrelationBuilder correlation) {
		List<MessageCorrelationResultWithVariableDto> resultDtos = new ArrayList<>();
		if (!messageDto.isAll()) {
			MessageCorrelationResultWithVariables result = correlation.correlateWithResultAndVariables(false);
			resultDtos.add(MessageCorrelationResultWithVariableDto.fromMessageCorrelationResultWithVariables(result));
		} else {
			List<MessageCorrelationResultWithVariables> results = correlation.correlateAllWithResultAndVariables(false);
			for (MessageCorrelationResultWithVariables result : results) {
				resultDtos.add(MessageCorrelationResultWithVariableDto.fromMessageCorrelationResultWithVariables(result));
			}
		}
		return resultDtos;
	}
	
	public MessageCorrelationBuilder createMessageCorrelationBuilder(CorrelationMessageDto messageDto) {
		RuntimeService runtimeService = processEngine.getRuntimeService();
	
		Map<String, Object> correlationKeys = VariableValueDto.toMap(messageDto.getCorrelationKeys(), processEngine,
				objectMapper);
		Map<String, Object> localCorrelationKeys = VariableValueDto.toMap(messageDto.getLocalCorrelationKeys(), processEngine,
				objectMapper);
		Map<String, Object> processVariables = VariableValueDto.toMap(messageDto.getProcessVariables(), processEngine,
				objectMapper);
		Map<String, Object> processVariablesLocal = VariableValueDto.toMap(messageDto.getProcessVariablesLocal(),
				processEngine, objectMapper);
		Map<String, Object> processVariablesToTriggeredScope = VariableValueDto
				.toMap(messageDto.getProcessVariablesToTriggeredScope(), processEngine, objectMapper);
	
		MessageCorrelationBuilder builder = processEngine.getRuntimeService().createMessageCorrelation(messageDto.getMessageName());
	
		if (processVariables != null) {
			builder.setVariables(processVariables);
		}
		if (processVariablesLocal != null) {
			builder.setVariablesLocal(processVariablesLocal);
		}
		if (processVariablesToTriggeredScope != null) {
			builder.setVariablesToTriggeredScope(processVariablesToTriggeredScope);
		}
		if (messageDto.getBusinessKey() != null) {
			builder.processInstanceBusinessKey(messageDto.getBusinessKey());
		}
	
		if (correlationKeys != null && !correlationKeys.isEmpty()) {
			for (java.util.Map.Entry<String, Object> correlationKey : correlationKeys.entrySet()) {
				String name = correlationKey.getKey();
				Object value = correlationKey.getValue();
				builder.processInstanceVariableEquals(name, value);
			}
		}
	
		if (localCorrelationKeys != null && !localCorrelationKeys.isEmpty()) {
			for (java.util.Map.Entry<String, Object> correlationKey : localCorrelationKeys.entrySet()) {
				String name = correlationKey.getKey();
				Object value = correlationKey.getValue();
				builder.localVariableEquals(name, value);
			}
		}
	
		if (messageDto.getTenantId() != null) {
			builder.tenantId(messageDto.getTenantId());
	
		} else if (messageDto.isWithoutTenantId()) {
			builder.withoutTenantId();
		}
	
		String processInstanceId = messageDto.getProcessInstanceId();
		if (processInstanceId != null) {
			builder.processInstanceId(processInstanceId);
		}
	
		return builder;
	}
	
	public HistoricIncidentDto fetchHistoricIncidentById(String incidentId, CIBUser user, ObjectMapper objectMapper) {
		Map<String, Object> params = Map.of("incidentId", incidentId);
		HistoricIncidentQueryDto queryDto = objectMapper.convertValue(params, HistoricIncidentQueryDto.class);
		HistoricIncidentQuery query = queryDto.toQuery(processEngine);
	
		List<HistoricIncident> queryResult = QueryUtil.list(query, null, null);
	
		for (HistoricIncident historicIncident : queryResult) {
			HistoricIncidentDto dto = HistoricIncidentDto.fromHistoricIncident(historicIncident);
			return dto;
		}
		// Historic incident not found, return null
		return null;
	}
	
	public Collection<Authorization> createAuthorizationCollection(
			List<org.cibseven.bpm.engine.authorization.Authorization> userAuthorizationList) {
		Collection<Authorization> resultAuthorization = new ArrayList<>();
		for (org.cibseven.bpm.engine.authorization.Authorization userAuthorization : userAuthorizationList) {
			resultAuthorization.add(createAuthorization(userAuthorization));
		}
		return resultAuthorization;
	}
	
	public Authorization createAuthorization(org.cibseven.bpm.engine.authorization.Authorization userAuthorization) {
		Authorization newUserAuthorization = new Authorization();
		newUserAuthorization.setGroupId(userAuthorization.getGroupId());
		newUserAuthorization.setId(userAuthorization.getId());
		newUserAuthorization.setPermissions(PermissionConverter.getNamesForPermissions(userAuthorization,
				userAuthorization.getPermissions(Permissions.values())));
		newUserAuthorization.setResourceId(userAuthorization.getResourceId());
		newUserAuthorization.setResourceType(userAuthorization.getResourceType());
		newUserAuthorization.setType(userAuthorization.getAuthorizationType());
		newUserAuthorization.setUserId(userAuthorization.getUserId());
		return newUserAuthorization;
	}
	
	public Collection<SevenUser> fetchUsers(CIBUser user) throws SystemException {
		UserQueryDto queryDto = new UserQueryDto();
		queryDto.setObjectMapper(objectMapper);
		UserQuery query = queryDto.toQuery(processEngine);
		query.userId(user.getId());
		List<org.cibseven.bpm.engine.identity.User> resultList = QueryUtil.list(query, null, null);
	
		Collection<SevenUser> userCollection = createSevenUsers(resultList);
		return userCollection;
	}
	
	public Collection<User> getUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike,
			Optional<String> lastName, Optional<String> lastNameLike, Optional<String> email, Optional<String> emailLike,
			Optional<String> memberOfGroup, Optional<String> memberOfTenant, Optional<String> idIn,
			Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder,
			String wcard) {
		UserQueryDto queryDto = new UserQueryDto();
		queryDto.setObjectMapper(objectMapper);
		UserQuery query = queryDto.toQuery(processEngine);
		if (memberOfGroup.isPresent())
			query.memberOfGroup(memberOfGroup.get());
		if (memberOfTenant.isPresent())
			query.memberOfTenant(memberOfTenant.get());
		// TODO: there is a protected void UserQuery.applySortBy(UserQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine)
		if (sortBy.isPresent()) {
			String sortByValue = sortBy.get();
			switch (sortByValue) {
			case "userId":
				query.orderByUserId();
				break;
			case "firstName":
				query.orderByUserFirstName();
				break;
			case "lastName":
				query.orderByUserLastName();
				break;
			case "email":
				query.orderByUserEmail();
				break;
			default:
			}
		}
		if (email.isPresent())
			query.userEmail(email.get());
		if (emailLike.isPresent())
			query.userEmailLike(emailLike.get().replace("*", wcard));
		if (firstName.isPresent())
			query.userFirstName(firstName.get());
		if (firstNameLike.isPresent())
			query.userFirstNameLike(firstNameLike.get().replace("*", wcard));
		if (id.isPresent())
			query.userId(id.get());
		if (lastName.isPresent())
			query.userLastName(lastName.get());
		if (lastNameLike.isPresent())
			query.userLastNameLike(lastNameLike.get().replace("*", wcard));
		Integer first = firstResult.isPresent() ? Integer.parseInt(firstResult.get()) : null;
		Integer max = maxResults.isPresent() ? Integer.parseInt(maxResults.get()) : null;
		List<org.cibseven.bpm.engine.identity.User> resultList = QueryUtil.list(query, first, max);
	
		Collection<User> userCollection = createUsers(resultList);
		return userCollection;
	}
	
	public String getWildcard () {
		String wcard = "";
		if (!wildcard.equals("")) wcard = wildcard;
		else {
			if (userProvider.equals("org.cibseven.webapp.auth.LdapUserProvider") || userProvider.equals("org.cibseven.webapp.auth.AdfsUserProvider")) {
				wcard = "*";				
			} else wcard = "%";
		}
		return wcard;
	}

	public Collection<User> createUsers(List<org.cibseven.bpm.engine.identity.User> resultList) {
		Collection<User> users = new ArrayList<>();
		for (org.cibseven.bpm.engine.identity.User resultUser : resultList) {
			User user = new User();
			user.setEmail(resultUser.getEmail());
			user.setFirstName(resultUser.getFirstName());
			user.setId(resultUser.getId());
			user.setLastName(resultUser.getLastName());
			users.add(user);
		}
		return users;
	}
	
	public Collection<SevenUser> createSevenUsers(List<org.cibseven.bpm.engine.identity.User> resultList) {
		Collection<SevenUser> users = new ArrayList<>();
		for (org.cibseven.bpm.engine.identity.User resultUser : resultList) {
			users.add(createSevenUser(resultUser));
		}
		return users;
	}
	
	public SevenUser createSevenUser(org.cibseven.bpm.engine.identity.User engineUser) {
		SevenUser user = new SevenUser();
		user.setEmail(engineUser.getEmail());
		user.setFirstName(engineUser.getFirstName());
		user.setId(engineUser.getId());
		user.setLastName(engineUser.getPassword());
		return user;
	}
	
	public org.cibseven.bpm.engine.identity.User findUserObject(String id) {
		org.cibseven.bpm.engine.identity.User dbUser = null;
		try {
			List<org.cibseven.bpm.engine.identity.User> users = processEngine.getIdentityService().createUserQuery().userId(id).list();
	
			if (users.size() == 1) {
				dbUser = users.get(0);
			} else if (!users.isEmpty()) {
	
				dbUser = users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
	
				if (dbUser == null) {
					dbUser = users.get(0);
				}
			}
		} catch (ProcessEngineException e) {
			throw new SystemException("Exception while performing user query: " + e.getMessage());
		}
		return dbUser;
	}
	
	public Group findGroupObject(String groupId) {
		try {
			return processEngine.getIdentityService().createGroupQuery().groupId(groupId).singleResult();
		} catch (ProcessEngineException e) {
			throw new SystemException("Exception while performing group query: " + e.getMessage());
		}
	}
	
	public SevenUser getUserProfile(String userId) {
		List<org.cibseven.bpm.engine.identity.User> users = processEngine.getIdentityService().createUserQuery().userId(userId).list();
		org.cibseven.bpm.engine.identity.User identityUser = null;
		if (users.isEmpty()) {
			return null;
		} else if (users.size() == 1) {
			identityUser = users.get(0);
		} else {
			identityUser = users.stream().filter(u -> u.getId().equals(userId)).findFirst().orElse(null);
			if (identityUser == null) {
				identityUser = users.get(0);
			}
		}
		return createSevenUser(identityUser);
	}

	public Collection<UserGroup> createUserGroups(List<Group> resultList) {
		Collection<UserGroup> userGroups = new ArrayList<>();
		for (Group group : resultList) {
			userGroups.add(createUserGroup(group));
		}
		return userGroups;
	}
	
	public UserGroup createUserGroup(Group group) {
		UserGroup userGroup = new UserGroup();
		userGroup.setId(group.getId());
		userGroup.setName(group.getName());
		userGroup.setType(group.getType());
		return userGroup;
	}
	
	public List<Authorization> queryAuthorizations(AuthorizationQueryDto queryDto, Integer firstResult,
			Integer maxResults) {
		queryDto.setObjectMapper(objectMapper);
		AuthorizationQuery query = queryDto.toQuery(processEngine);
	
		List<org.cibseven.bpm.engine.authorization.Authorization> resultList = QueryUtil.list(query, firstResult, maxResults);
		List<AuthorizationDto> authorizationDtoList = AuthorizationDto.fromAuthorizationList(resultList,
				processEngine.getProcessEngineConfiguration());
		List<Authorization> authorizationList = new ArrayList<>();
		for (AuthorizationDto authorizationDto : authorizationDtoList) {
			authorizationList.add(convertValue(authorizationDto, Authorization.class));
		}
		return authorizationList;
	}
	
	public org.cibseven.bpm.engine.task.Task getTaskById(String taskId) {
		org.cibseven.bpm.engine.task.Task foundTask = processEngine.getTaskService().createTaskQuery().taskId(taskId).initializeFormKeys()
				.singleResult();
		// TODO: any use of 'withCommentAttachmentInfo()'
		if (foundTask == null) {
			// TODO: check exception type
			throw new SystemException("No matching task with id " + taskId);
		}
		return foundTask;
	}
	
	public <V extends Object> V runWithoutAuthorization(Supplier<V> action) {
		Authentication currentAuthentication = processEngine.getIdentityService().getCurrentAuthentication();
		try {
			processEngine.getIdentityService().clearAuthentication();
			return action.get();
		} catch (Exception e) {
			throw e;
		} finally {
			processEngine.getIdentityService().setAuthentication(currentAuthentication);
		}
	}

	public Incident fetchIncidentById(String incidentId) {
		org.cibseven.bpm.engine.runtime.Incident incident = processEngine.getRuntimeService().createIncidentQuery().incidentId(incidentId)
				.singleResult();
		if (incident == null) {
			throw new InvalidRequestException(Status.NOT_FOUND, "No matching incident with id " + incidentId);
		}
		return convertValue(IncidentDto.fromIncident(incident), Incident.class);
	
	}
	
	public List<Incident> fetchIncidents(String processDefinitionKey, String activityId, CIBUser user,
			String processInstanceId) {
		IncidentQueryDto queryDto = new IncidentQueryDto();
		queryDto.setActivityId(activityId);
		if (processDefinitionKey != null)
			queryDto.setProcessDefinitionKeyIn(new String[] { processDefinitionKey });
		queryDto.setProcessInstanceId(processInstanceId);
		;
		IncidentQuery query = queryDto.toQuery(processEngine);
	
		List<org.cibseven.bpm.engine.runtime.Incident> queryResult = QueryUtil.list(query, null, null);
	
		List<Incident> result = new ArrayList<>();
		for (org.cibseven.bpm.engine.runtime.Incident incident : queryResult) {
			IncidentDto dto = IncidentDto.fromIncident(incident);
			result.add(convertValue(dto, Incident.class));
		}
		return result;
	}
	
	public VariableInstance getVariableInstanceImpl(String id, boolean deserializeValue, CIBUser user)
			throws SystemException, NoObjectFoundException {
		VariableInstanceQuery variableInstanceQuery = processEngine.getRuntimeService().createVariableInstanceQuery().variableId(id);
		// do not fetch byte arrays
		variableInstanceQuery.disableBinaryFetching();
	
		if (!deserializeValue) {
			variableInstanceQuery.disableCustomObjectDeserialization();
		}
		org.cibseven.bpm.engine.runtime.VariableInstance variableEngineInstance = variableInstanceQuery.singleResult();
		if (variableEngineInstance != null) {
			VariableInstance variableInstance = convertValue(variableEngineInstance, VariableInstance.class);
			// return transformToDto(variableInstance);
			return variableInstance;
		} else {
			throw new SystemException("Variable with Id '" + id + "' does not exist.");
		}
	}

	public VariableHistory getHistoricVariableInstanceImpl(String id, boolean deserializeValue, CIBUser user) {
		HistoricVariableInstanceQuery query = processEngine.getHistoryService().createHistoricVariableInstanceQuery().variableId(id);
		if (!deserializeValue) {
			query.disableCustomObjectDeserialization();
		}
		HistoricVariableInstance variableInstance = query.singleResult();
		if (variableInstance != null) {
			VariableHistory result = convertValue(HistoricVariableInstanceDto.fromHistoricVariableInstance(variableInstance),
					VariableHistory.class);
			return result;
		} else {
			throw new SystemException(" historic variable with Id '" + id + "' does not exist.");
		}
	}

	public Object deserializeJsonObject(String className, byte[] data) {
		try {
			JavaType type = TypeFactory.defaultInstance().constructFromCanonical(className);
			validateType(type);
			return objectMapper.readValue(new String(data, Charset.forName("UTF-8")), type);
		} catch (Exception e) {
			throw new SystemException("Could not deserialize JSON object: " + e.getMessage());
		}
	}
	
	/**
	 * Validate the type with the help of the validator in the engine.<br>
	 * Note: when adjusting this method, please also consider adjusting the
	 * {@code JacksonJsonDataFormatMapper#validateType} in the Engine Spin Plugin
	 */
	public void validateType(JavaType type) {
		if (processEngine.getProcessEngineConfiguration().isDeserializationTypeValidationEnabled()) {
			DeserializationTypeValidator validator = processEngine.getProcessEngineConfiguration().getDeserializationTypeValidator();
			if (validator != null) {
				List<String> invalidTypes = new ArrayList<>();
				validateType(type, validator, invalidTypes);
				if (!invalidTypes.isEmpty()) {
					throw new SystemException("The following classes are not whitelisted for deserialization: " + invalidTypes);
				}
			}
		}
	}
	
	public void validateType(JavaType type, DeserializationTypeValidator validator, List<String> invalidTypes) {
		if (!type.isPrimitive()) {
			if (!type.isArrayType()) {
				validateTypeInternal(type, validator, invalidTypes);
			}
			if (type.isMapLikeType()) {
				validateType(type.getKeyType(), validator, invalidTypes);
			}
			if (type.isContainerType() || type.hasContentType()) {
				validateType(type.getContentType(), validator, invalidTypes);
			}
		}
	}
	
	public void validateTypeInternal(JavaType type, DeserializationTypeValidator validator, List<String> invalidTypes) {
		String className = type.getRawClass().getName();
		if (!validator.validate(className) && !invalidTypes.contains(className)) {
			invalidTypes.add(className);
		}
	}
	
	public List<Variable> queryVariableInstances(VariableInstanceQueryDto queryDto, Integer firstResult,
			Integer maxResults, boolean deserializeObjectValues) {
		VariableInstanceQuery query = queryDto.toQuery(processEngine);
	
		// disable binary fetching by default.
		query.disableBinaryFetching();
	
		// disable custom object fetching by default. Cannot be done to not break
		// existing API
		if (!deserializeObjectValues) {
			query.disableCustomObjectDeserialization();
		}
	
		List<org.cibseven.bpm.engine.runtime.VariableInstance> matchingInstances = QueryUtil.list(query, firstResult,
				maxResults);
	
		// List<VariableInstanceDto> instanceResults = new ArrayList<>();
		List<Variable> instanceResults = new ArrayList<>();
		for (org.cibseven.bpm.engine.runtime.VariableInstance instance : matchingInstances) {
			VariableInstanceDto resultInstanceDto = VariableInstanceDto.fromVariableInstance(instance);
			VariableHistory resultInstance = convertValue(resultInstanceDto, VariableHistory.class);
			instanceResults.add(resultInstance);
		}
		return instanceResults;
	}
	
	public ResponseEntity<byte[]> getResponseForTypedVariable(TypedValue typedVariableValue, String id) {
		if (typedVariableValue instanceof BytesValue || ValueType.BYTES.equals(typedVariableValue.getType())) {
			byte[] valueBytes = (byte[]) typedVariableValue.getValue();
			if (valueBytes == null) {
				valueBytes = new byte[0];
			}
			ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(valueBytes, HttpStatusCode.valueOf(200));
			return responseEntity;
		} else if (ValueType.FILE.equals(typedVariableValue.getType())) {
			FileValue typedFileValue = (FileValue) typedVariableValue;
			// TODO: is anybody interested in the type
			// String type = typedFileValue.getMimeType() != null ?
			// typedFileValue.getMimeType()
			// : MediaType.APPLICATION_OCTET_STREAM.toString();
			// if (typedFileValue.getEncoding() != null) {
			// type += "; charset=" + typedFileValue.getEncoding();
			// }
			try {
				byte[] bytes = typedFileValue.getValue() == null ? null : IOUtils.toByteArray(typedFileValue.getValue());
				// status code if bytes==null?
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(typedFileValue.getMimeType() != null ? MediaType.valueOf(typedFileValue.getMimeType())
						: MediaType.APPLICATION_OCTET_STREAM);
				ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(bytes, headers, HttpStatusCode.valueOf(200));
				return responseEntity;
			} catch (IOException e) {
				// TODO exception type
				throw new SystemException(e.getMessage(), e);
			}
		} else {
			throw new SystemException(String.format("Value of variable with id %s is not a binary value.", id));
		}
	}
	
	public List<VariableHistory> queryHistoricVariableInstances(HistoricVariableInstanceQueryDto queryDto,
			ObjectMapper objectMapper, Integer firstResult, Integer maxResults, boolean deserializeObjectValues) {
		// change to history query!!
		HistoricVariableInstanceQuery query = queryDto.toQuery(processEngine);
	
		// disable binary fetching by default.
		query.disableBinaryFetching();
	
		// disable custom object fetching by default. Cannot be done to not break
		// existing API
		if (!deserializeObjectValues) {
			query.disableCustomObjectDeserialization();
		}
	
		List<HistoricVariableInstance> matchingInstances = QueryUtil.list(query, firstResult, maxResults);
	
		// List<VariableInstanceDto> instanceResults = new ArrayList<>();
		List<VariableHistory> instanceResults = new ArrayList<>();
		for (HistoricVariableInstance instance : matchingInstances) {
			HistoricVariableInstanceDto resultInstanceDto = HistoricVariableInstanceDto.fromHistoricVariableInstance(instance);
			VariableHistory resultInstance = convertValue(resultInstanceDto, VariableHistory.class);
			instanceResults.add(resultInstance);
		}
		return instanceResults;
	}
	
	public List<VariableHistory> queryHistoricVariableInstances(HistoricVariableInstanceQueryDto queryDto,
			Integer firstResult, Integer maxResults, boolean deserializeObjectValues) {
		queryDto.setObjectMapper(objectMapper);
		HistoricVariableInstanceQuery query = queryDto.toQuery(processEngine);
		query.disableBinaryFetching();
	
		if (!deserializeObjectValues) {
			query.disableCustomObjectDeserialization();
		}
	
		List<HistoricVariableInstance> matchingHistoricVariableInstances = QueryUtil.list(query, firstResult, maxResults);
		List<VariableHistory> historicVariableInstanceDtoResults = new ArrayList<>();
		for (HistoricVariableInstance historicVariableInstance : matchingHistoricVariableInstances) {
			HistoricVariableInstanceDto resultHistoricVariableInstance = HistoricVariableInstanceDto
					.fromHistoricVariableInstance(historicVariableInstance);
			historicVariableInstanceDtoResults.add(convertValue(resultHistoricVariableInstance, VariableHistory.class));
		}
		return historicVariableInstanceDtoResults;
	}
	
	public Variable fetchTaskVariableImpl(String taskId, String variableName, boolean deserializeValue, CIBUser user)
			throws NoObjectFoundException, SystemException {
		TypedValue value = getTypedValueForTaskVariable(taskId, variableName, deserializeValue);
		return convertValue(VariableValueDto.fromTypedValue(value), Variable.class);
	}
	
	public TypedValue getTypedValueForTaskVariable(String taskId, String variableName, boolean deserializeValue) {
		TypedValue value = null;
		try {
			value = processEngine.getTaskService().getVariableTyped(taskId, variableName, deserializeValue);
		} catch (AuthorizationException e) {
			throw e;
		} catch (ProcessEngineException e) {
			String errorMessage = String.format("Cannot get %s variable %s: %s", "task", variableName, e.getMessage());
			throw new SystemException(errorMessage, e);
		}
	
		if (value == null) {
			String errorMessage = String.format("%s variable with name %s does not exist", "task", variableName);
			throw new SystemException(errorMessage);
		}
		return value;
	}
	
	/*
	 * puts variable to different targets depending on taskId, processInstanceId,
	 * ...
	 */
	public void setBinaryVariable(MultipartFile data, String valueType, String objectType, String taskId,
			String processInstanceId, String variableName) throws IOException {
		if (objectType != null) {
			Object object = null;
	
			if (data.getContentType() != null
					&& data.getContentType().toLowerCase().contains(MediaType.APPLICATION_JSON.toString())) {
	
				byte[] bytes = IOUtils.toByteArray(data.getResource().getInputStream());
				object = deserializeJsonObject(objectType, bytes);
	
			} else {
				throw new SystemException("Unrecognized content type for serialized java type: " + data.getContentType());
			}
	
			if (object != null) {
				if (taskId != null)
					processEngine.getTaskService().setVariable(taskId, variableName, Variables.objectValue(object).create());
				else if (processInstanceId != null)
					processEngine.getRuntimeService().setVariable(processInstanceId, variableName, Variables.objectValue(object).create());
			}
		} else {
	
			String valueTypeName = DEFAULT_BINARY_VALUE_TYPE;
			if (valueType != null) {
				if (valueType.isBlank()) {
					throw new InvalidRequestException(Status.BAD_REQUEST,
							"Form part with name 'valueType' must have a text/plain value");
				}
	
				valueTypeName = valueType;
			}
			VariableValueDto valueDto = createVariableValueDto(valueTypeName, data);
			try {
				TypedValue typedValue = valueDto.toTypedValue(processEngine, objectMapper);
				if (taskId != null)
					processEngine.getTaskService().setVariable(taskId, variableName, typedValue);
				else if (processInstanceId != null) 
					processEngine.getRuntimeService().setVariable(processInstanceId, variableName, typedValue);
			} catch (AuthorizationException e) {
				throw e;
			} catch (ProcessEngineException e) {
				String errorMessage = String.format("Cannot put %s variable %s: %s", "task", variableName, e.getMessage());
				throw new SystemException(errorMessage, e);
			}
		}
	
	}
	
	public VariableValueDto createVariableValueDto(String valueTypeName, MultipartFile data) throws IOException {
		VariableValueDto valueDto = new VariableValueDto();
		valueDto.setType(valueTypeName);
		valueDto.setValue(data.getBytes());
	
		String contentType = data.getContentType();
		if (contentType == null) {
			contentType = MediaType.APPLICATION_OCTET_STREAM.toString();
		}
	
		Map<String, Object> valueInfoMap = new HashMap<>();
		valueInfoMap.put(FileValueType.VALUE_INFO_FILE_NAME, data.getResource().getFilename());
		MimeType mimeType = null;
		try {
			mimeType = new MimeType(contentType);
		} catch (MimeTypeParseException e) {
			throw new RestException(Status.BAD_REQUEST, "Invalid mime type given");
		}
	
		valueInfoMap.put(FileValueType.VALUE_INFO_FILE_MIME_TYPE, mimeType.getBaseType());
	
		String encoding = mimeType.getParameter("encoding");
		if (encoding != null) {
			valueInfoMap.put(FileValueType.VALUE_INFO_FILE_ENCODING, encoding);
		}
	
		String transientString = mimeType.getParameter("transient");
		boolean isTransient = Boolean.parseBoolean(transientString);
		if (isTransient) {
			valueInfoMap.put(AbstractValueTypeImpl.VALUE_INFO_TRANSIENT, isTransient);
		}
		valueDto.setValueInfo(valueInfoMap);
		return valueDto;
	}
	
	public Variable fetchVariableByProcessInstanceIdImpl(String processInstanceId, String variableName,
			boolean deserializeValue, CIBUser user) throws SystemException {
		TypedValue value = getTypedValueForProcessInstanceVariable(processInstanceId, variableName, deserializeValue);
		return convertValue(VariableValueDto.fromTypedValue(value), Variable.class);
	}
	
	public TypedValue getTypedValueForProcessInstanceVariable(String processInstanceId, String variableName,
			boolean deserializeValue) {
		try {
			return processEngine.getRuntimeService().getVariableTyped(processInstanceId, variableName, deserializeValue);
		} catch (AuthorizationException e) {
			throw e;
		} catch (ProcessEngineException e) {
			String errorMessage = String.format("Cannot get %s variable %s: %s", "task", variableName, e.getMessage());
			throw new SystemException(errorMessage, e);
		}
	}
	
	// updates execution variables
	public void updateVariableEntities(String processInstanceId, Map<String, VariableValueDto> modifications,
			List<String> deletions) {
		VariableMap variableModifications = null;
		try {
			variableModifications = VariableValueDto.toMap(modifications, processEngine, objectMapper);
		} catch (RestException e) {
			String errorMessage = String.format("Cannot modify variables for %s: %s", "processInstance", e.getMessage());
			throw new SystemException(errorMessage, e);
		}
		try {
			RuntimeServiceImpl runtimeServiceImpl = (RuntimeServiceImpl) processEngine.getRuntimeService();
			runtimeServiceImpl.updateVariables(processInstanceId, variableModifications, deletions);
		} catch (AuthorizationException e) {
			throw e;
		} catch (ProcessEngineException e) {
			String errorMessage = String.format("Cannot modify variables for %s %s: %s", "processInstance", processInstanceId,
					e.getMessage());
			throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
		}
	}
	
	public DecisionDefinition getDecisionDefinitionByKeyAndTenant(String key, String tenantId) {
		DecisionDefinitionQuery query = processEngine.getRepositoryService().createDecisionDefinitionQuery().decisionDefinitionKey(key);
		if (tenantId == null)
			query.withoutTenantId();
		else
			query.tenantIdIn(new String[] { tenantId });
	
		DecisionDefinition decisionDefinition = query.latestVersion().singleResult();
	
		if (decisionDefinition == null) {
			String errorMessage = String.format("No matching decision definition with key: %s and no tenant-id", key);
			throw new SystemException(errorMessage);
		}
		return decisionDefinition;
	}
	
	public Map<String, VariableValueDto> createResultEntriesDto(DmnDecisionResultEntries entries) {
		VariableMap variableMap = Variables.createVariables();
	
		for (String key : entries.keySet()) {
			TypedValue typedValue = entries.getEntryTyped(key);
			variableMap.putValueTyped(key, typedValue);
		}
	
		return VariableValueDto.fromMap(variableMap);
	}
	
	public Object getDiagramByDecisionDefinition(DecisionDefinition decisionDefinition, CIBUser user) {
		InputStream decisionDiagram = processEngine.getRepositoryService().getDecisionDiagram(decisionDefinition.getId());
		if (decisionDiagram == null) {
			throw new SystemException("Diagram of decision " + decisionDefinition.getId() + " not found.");
		} else {
			// TODO: empty result object
			// DecisionProvider creates a Decision from the body 		
			// return ((ResponseEntity<Decision>) doGet(url, Decision.class, user, false)).getBody();
			// the backend puts an InputStream with filename into the response 
			// return Response.ok(decisionDiagram).header("Content-Disposition", URLEncodingUtil.buildAttachmentValue(fileName))
			// .type(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix(fileName)).build();
			//ByteArrayResource resource = new ByteArrayResource(diagram.getBpmn20Xml().getBytes());
			try {
				//TODO: define a useful return object for getDiagramByKey(AndTenant)
				byte[] byteContent = IOUtils.toByteArray(decisionDiagram);
				return DecisionDefinitionDiagramDto.create(decisionDefinition.getDiagramResourceName(), new String(byteContent, "UTF-8"));
			} catch (IOException e) {
				throw new SystemException(e.getMessage(), e);
			}
		}
	}
	
	public Object getXmlByDefinitionId(String definitionId) {
		InputStream decisionModelInputStream = null;
		try {
			decisionModelInputStream = processEngine.getRepositoryService().getDecisionModel(definitionId);
	
			byte[] decisionModel = IoUtil.readInputStream(decisionModelInputStream, "decisionModelDmnXml");
			return DecisionDefinitionDiagramDto.create(definitionId, new String(decisionModel, "UTF-8"));
	
		} catch (ProcessEngineException | UnsupportedEncodingException e) {
			throw new SystemException(e.getMessage(), e);
	
		} finally {
			IoUtil.closeSilently(decisionModelInputStream);
		}
	}
	
	public DecisionDefinition getDecisionDefinitionById(String id, CIBUser user) {

		DecisionDefinition definition = null;
		try {
			definition = processEngine.getRepositoryService().getDecisionDefinition(id);
		} catch (ProcessEngineException e) {
			throw new SystemException(e.getMessage(), e);
		}
		return definition;
	}
	
	public Long getHistoricDecisionInstanceCount(Map<String, Object> queryParams) {
		HistoricDecisionInstanceQueryDto queryHistoricDecisionInstanceDto = objectMapper.convertValue(queryParams,
				HistoricDecisionInstanceQueryDto.class);
		HistoricDecisionInstanceQuery query = queryHistoricDecisionInstanceDto.toQuery(processEngine);
		return query.count();
	}

	public Map<String, Object> createSumParamsMap(String metric, String startDate, String endDate) {
		Map<String, Object> params = new HashMap<>();
		params.put("metric", metric);
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		return params;
	}
	
	public int getSum(String metricsName, Map<String, Object> queryParams, CIBUser user) {
		DateConverter dateConverter = new DateConverter();
		dateConverter.setObjectMapper(objectMapper);
	
		long result = 0;
	
		if (Metrics.UNIQUE_TASK_WORKERS.equals(metricsName) || Metrics.TASK_USERS.equals(metricsName)) {
			result = processEngine.getManagementService().getUniqueTaskWorkerCount(extractStartDate(queryParams, dateConverter),
					extractEndDate(queryParams, dateConverter));
		} else {
			MetricsQuery query = processEngine.getManagementService().createMetricsQuery().name(metricsName);
	
			applyQueryParams(queryParams, dateConverter, query);
			result = query.sum();
		}
		return (int) result;
	}
	
	public void applyQueryParams(Map<String, Object> queryParameters, DateConverter dateConverter, MetricsQuery query) {
		Date startDate = extractStartDate(queryParameters, dateConverter);
		Date endDate = extractEndDate(queryParameters, dateConverter);
		if (startDate != null) {
			query.startDate(startDate);
		}
		if (endDate != null) {
			query.endDate(endDate);
		}
	}
	
	public Date extractEndDate(Map<String, Object> queryParameters, DateConverter dateConverter) {
		if (queryParameters.containsKey("endDate")) {
			return dateConverter.convertQueryParameterToType((String) queryParameters.get("endDate"));
		}
		return null;
	}
	
	public Date extractStartDate(Map<String, Object> queryParameters, DateConverter dateConverter) {
		if (queryParameters.containsKey("startDate")) {
			return dateConverter.convertQueryParameterToType((String) queryParameters.get("startDate"));
		}
		return null;
	}
	
	public org.cibseven.bpm.engine.identity.Tenant findTenantObject(String tenantId) {
		try {
			return processEngine.getIdentityService().createTenantQuery().tenantId(tenantId).singleResult();
	
		} catch (ProcessEngineException e) {
			throw new SystemException("Exception while performing tenant query: " + e.getMessage());
		}
	}
	
	public void ensureNotReadOnly() {
		if (processEngine.getIdentityService().isReadOnly()) {
			throw new InvalidRequestException(Status.FORBIDDEN, "Identity service implementation is read-only.");
		}
	}

  public DeploymentWithDefinitions tryToRedeploy(String deploymentId, RedeploymentDto redeployment) {
    DeploymentBuilder builder = processEngine.getRepositoryService().createDeployment();
    builder.nameFromDeployment(deploymentId);

    String tenantId = getDeployment(deploymentId).getTenantId();
    if (tenantId != null) {
      builder.tenantId(tenantId);
    }

    if (redeployment != null) {
      builder = addRedeploymentResources(deploymentId, builder, redeployment);
    } else {
      builder.addDeploymentResources(deploymentId);
    }

    return builder.deployWithResult();
  }

  public DeploymentDto getDeployment(String deploymentId) {
     org.cibseven.bpm.engine.repository.Deployment deployment = processEngine.getRepositoryService().createDeploymentQuery().deploymentId(deploymentId).singleResult();

    if (deployment == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Deployment with id '" + deploymentId + "' does not exist");
    }

    return DeploymentDto.fromDeployment(deployment);
  }

  public DeploymentBuilder addRedeploymentResources(String deploymentId, DeploymentBuilder builder, RedeploymentDto redeployment) {
    builder.source(redeployment.getSource());

    List<String> resourceIds = redeployment.getResourceIds();
    List<String> resourceNames = redeployment.getResourceNames();

    boolean isResourceIdListEmpty = resourceIds == null || resourceIds.isEmpty();
    boolean isResourceNameListEmpty = resourceNames == null || resourceNames.isEmpty();

    if (isResourceIdListEmpty && isResourceNameListEmpty) {
      builder.addDeploymentResources(deploymentId);

    } else {
      if (!isResourceIdListEmpty) {
        builder.addDeploymentResourcesById(deploymentId, resourceIds);
      }
      if (!isResourceNameListEmpty) {
        builder.addDeploymentResourcesByName(deploymentId, resourceNames);
      }
    }
    return builder;
  }

}
