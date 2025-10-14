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

import static org.cibseven.webapp.auth.SevenAuthorizationUtils.resourceType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;

import org.apache.commons.io.IOUtils;
import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.bpm.engine.AuthorizationService;
import org.cibseven.bpm.engine.CaseService;
import org.cibseven.bpm.engine.EntityTypes;
import org.cibseven.bpm.engine.ExternalTaskService;
import org.cibseven.bpm.engine.FilterService;
import org.cibseven.bpm.engine.FormService;
import org.cibseven.bpm.engine.HistoryService;
import org.cibseven.bpm.engine.IdentityService;
import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineConfiguration;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.RepositoryService;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDefinitionInfo;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.ProcessInstance;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.ProcessStatistics;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.rest.model.StartForm;
import org.cibseven.bpm.engine.TaskService;
import org.cibseven.bpm.engine.authorization.AuthorizationQuery;
import org.cibseven.bpm.engine.authorization.Permissions;
import org.cibseven.bpm.engine.exception.NotFoundException;
import org.cibseven.bpm.engine.exception.NotValidException;
import org.cibseven.bpm.engine.exception.NullValueException;
import org.cibseven.bpm.engine.externaltask.ExternalTaskQuery;
import org.cibseven.bpm.engine.filter.FilterQuery;
import org.cibseven.bpm.engine.form.CamundaFormRef;
import org.cibseven.bpm.engine.form.FormData;
import org.cibseven.bpm.engine.history.HistoricActivityInstance;
import org.cibseven.bpm.engine.history.HistoricActivityInstanceQuery;
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
import org.cibseven.bpm.engine.management.ActivityStatistics;
import org.cibseven.bpm.engine.management.ActivityStatisticsQuery;
import org.cibseven.bpm.engine.management.IncidentStatistics;
import org.cibseven.bpm.engine.management.JobDefinitionQuery;
import org.cibseven.bpm.engine.management.ProcessDefinitionStatistics;
import org.cibseven.bpm.engine.management.ProcessDefinitionStatisticsQuery;
import org.cibseven.bpm.engine.query.Query;
import org.cibseven.bpm.engine.repository.DeploymentBuilder;
import org.cibseven.bpm.engine.repository.DeploymentQuery;
import org.cibseven.bpm.engine.repository.DeploymentWithDefinitions;
import org.cibseven.bpm.engine.repository.ProcessDefinition;
import org.cibseven.bpm.engine.repository.ProcessDefinitionQuery;
import org.cibseven.bpm.engine.repository.Resource;
import org.cibseven.bpm.engine.rest.dto.AbstractQueryDto;
import org.cibseven.bpm.engine.rest.dto.CountResultDto;
import org.cibseven.bpm.engine.rest.dto.VariableValueDto;
import org.cibseven.bpm.engine.rest.dto.authorization.AuthorizationDto;
import org.cibseven.bpm.engine.rest.dto.authorization.AuthorizationQueryDto;
import org.cibseven.bpm.engine.rest.dto.converter.DelegationStateConverter;
import org.cibseven.bpm.engine.rest.dto.converter.StringListConverter;
import org.cibseven.bpm.engine.rest.dto.externaltask.ExternalTaskDto;
import org.cibseven.bpm.engine.rest.dto.externaltask.ExternalTaskQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricActivityInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricActivityInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricIncidentDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricIncidentQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricProcessInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricProcessInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricVariableInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricVariableInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.identity.GroupQueryDto;
import org.cibseven.bpm.engine.rest.dto.identity.UserQueryDto;
import org.cibseven.bpm.engine.rest.dto.management.JobDefinitionDto;
import org.cibseven.bpm.engine.rest.dto.management.JobDefinitionQueryDto;
import org.cibseven.bpm.engine.rest.dto.management.JobDefinitionSuspensionStateDto;
import org.cibseven.bpm.engine.rest.dto.repository.CalledProcessDefinitionDto;
import org.cibseven.bpm.engine.rest.dto.repository.DeploymentDto;
import org.cibseven.bpm.engine.rest.dto.repository.DeploymentQueryDto;
import org.cibseven.bpm.engine.rest.dto.repository.DeploymentResourceDto;
import org.cibseven.bpm.engine.rest.dto.repository.DeploymentWithDefinitionsDto;
import org.cibseven.bpm.engine.rest.dto.repository.ProcessDefinitionDiagramDto;
import org.cibseven.bpm.engine.rest.dto.repository.ProcessDefinitionDto;
import org.cibseven.bpm.engine.rest.dto.repository.ProcessDefinitionQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ActivityInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.FilterDto;
import org.cibseven.bpm.engine.rest.dto.runtime.FilterQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.IncidentDto;
import org.cibseven.bpm.engine.rest.dto.runtime.IncidentQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ProcessInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ProcessInstanceSuspensionStateDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ProcessInstanceWithVariablesDto;
import org.cibseven.bpm.engine.rest.dto.runtime.StartProcessInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.modification.ProcessInstanceModificationInstructionDto;
import org.cibseven.bpm.engine.rest.dto.task.CompleteTaskDto;
import org.cibseven.bpm.engine.rest.dto.task.FormDto;
import org.cibseven.bpm.engine.rest.dto.task.GroupDto;
import org.cibseven.bpm.engine.rest.dto.task.GroupInfoDto;
import org.cibseven.bpm.engine.rest.dto.task.IdentityLinkDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskQueryDto;
import org.cibseven.bpm.engine.rest.dto.task.UserDto;
import org.cibseven.bpm.engine.rest.exception.InvalidRequestException;
import org.cibseven.bpm.engine.rest.exception.RestException;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.rest.mapper.MultipartFormData;
import org.cibseven.bpm.engine.rest.mapper.MultipartFormData.FormPart;
import org.cibseven.bpm.engine.rest.sub.impl.VariableResponseProvider;
import org.cibseven.bpm.engine.rest.sub.repository.impl.ProcessDefinitionResourceImpl;
import org.cibseven.bpm.engine.rest.util.ApplicationContextPathUtil;
import org.cibseven.bpm.engine.rest.util.QueryUtil;
import org.cibseven.bpm.engine.runtime.DeserializationTypeValidator;
import org.cibseven.bpm.engine.runtime.EventSubscription;
import org.cibseven.bpm.engine.runtime.IncidentQuery;
import org.cibseven.bpm.engine.runtime.ProcessInstanceQuery;
import org.cibseven.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.cibseven.bpm.engine.runtime.ProcessInstantiationBuilder;
import org.cibseven.bpm.engine.runtime.VariableInstanceQuery;
import org.cibseven.bpm.engine.task.DelegationState;
import org.cibseven.bpm.engine.task.TaskQuery;
import org.cibseven.bpm.engine.variable.VariableMap;
import org.cibseven.bpm.engine.variable.Variables;
import org.cibseven.bpm.engine.variable.impl.type.AbstractValueTypeImpl;
import org.cibseven.bpm.engine.variable.type.FileValueType;
import org.cibseven.bpm.engine.variable.type.ValueType;
import org.cibseven.bpm.engine.variable.value.BytesValue;
import org.cibseven.bpm.engine.variable.value.FileValue;
import org.cibseven.bpm.engine.variable.value.PrimitiveValue;
import org.cibseven.bpm.engine.variable.value.SerializableValue;
import org.cibseven.bpm.model.bpmn.instance.Message;
import org.cibseven.bpm.engine.variable.value.TypedValue;
import org.cibseven.webapp.Data;
import org.cibseven.webapp.NamedByteArrayDataSource;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.ExpressionEvaluationException;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnexpectedTypeException;
import org.cibseven.webapp.exception.UnsupportedTypeException;
import org.cibseven.webapp.providers.utils.URLUtils;
import org.cibseven.webapp.rest.model.ActivityInstance;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.CamundaForm;
import org.cibseven.webapp.rest.model.CandidateGroupTaskCount;
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.rest.model.DeploymentResource;
import org.cibseven.webapp.rest.model.ExternalTask;
import org.cibseven.webapp.rest.model.Filter;
import org.cibseven.webapp.rest.model.FilterCriterias;
import org.cibseven.webapp.rest.model.FilterProperties;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Incident;
import org.cibseven.webapp.rest.model.IncidentInfo;
import org.cibseven.webapp.rest.model.Job;
import org.cibseven.webapp.rest.model.JobDefinition;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskForm;
import org.cibseven.webapp.rest.model.TaskHistory;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.cibseven.webapp.rest.model.VariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import camundajar.impl.scala.Array;
import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SevenDirectProvider /* extends SevenProviderBase implements BpmProvider*/{

  //TODO: used to call groupProcessStatisticsByKeyAndTenant which should be moved to some util class
  @Autowired private IProcessProvider processProvider;


  // direct access interface
  @Autowired private FormService formService;
  @Autowired private TaskService taskService;
  @Autowired private IdentityService identityService;
  @Autowired private AuthorizationService authorizationService;
  @Autowired private RuntimeService runtimeService;
  @Autowired private ManagementService managementService;
  @Autowired private RepositoryService repositoryService;
  @Autowired private HistoryService historyService;
  @Autowired private FilterService filterService;
  @Autowired private ExternalTaskService externalTaskService;
  @Autowired private CaseService caseService;
  @Autowired private ProcessEngineConfiguration processEngineConfiguration;
  @Autowired private ProcessEngine processEngine;
  
  private ObjectMapper objectMapper = null;
  
  //Required in deployment code
  protected static final Map<String, String> MEDIA_TYPE_MAPPING = new HashMap<String, String>();

  static {
    //TODO: rest code uses import javax.ws.rs.core.MediaType;
    MEDIA_TYPE_MAPPING.put("bpmn", MediaType.APPLICATION_XML.toString());
    MEDIA_TYPE_MAPPING.put("cmmn", MediaType.APPLICATION_XML.toString());
    MEDIA_TYPE_MAPPING.put("dmn", MediaType.APPLICATION_XML.toString());
    MEDIA_TYPE_MAPPING.put("json", MediaType.APPLICATION_JSON.toString());
    MEDIA_TYPE_MAPPING.put("xml", MediaType.APPLICATION_XML.toString());

    MEDIA_TYPE_MAPPING.put("gif", "image/gif");
    MEDIA_TYPE_MAPPING.put("jpeg", "image/jpeg");
    MEDIA_TYPE_MAPPING.put("jpe", "image/jpeg");
    MEDIA_TYPE_MAPPING.put("jpg", "image/jpeg");
    MEDIA_TYPE_MAPPING.put("png", "image/png");
    MEDIA_TYPE_MAPPING.put("svg", "image/svg+xml");
    MEDIA_TYPE_MAPPING.put("tiff", "image/tiff");
    MEDIA_TYPE_MAPPING.put("tif", "image/tiff");

    MEDIA_TYPE_MAPPING.put("groovy", "text/plain");
    MEDIA_TYPE_MAPPING.put("java", "text/plain");
    MEDIA_TYPE_MAPPING.put("js", "text/plain");
    MEDIA_TYPE_MAPPING.put("php", "text/plain");
    MEDIA_TYPE_MAPPING.put("py", "text/plain");
    MEDIA_TYPE_MAPPING.put("rb", "text/plain");

    MEDIA_TYPE_MAPPING.put("html", "text/html");
    MEDIA_TYPE_MAPPING.put("txt", "text/plain");
  }

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
  
  protected static final String DEFAULT_BINARY_VALUE_TYPE = "Bytes";

  /*
  
    ████████  █████  ███████ ██   ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
       ██    ██   ██ ██      ██  ██      ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
       ██    ███████ ███████ █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
       ██    ██   ██      ██ ██  ██      ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
       ██    ██   ██ ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                                            
   */
  
  //@Override
  public Collection<Task> findTasks(String filter, CIBUser user) {
      Map<String, Object> filters = new HashMap<>();
      //TODO: add filter to map!
      return convertTasks(queryTasks(filters, user));
  }

  //@Override
  public Integer findTasksCount(@RequestBody Map<String, Object> filters, CIBUser user) {
    return queryTasks(filters, user).size();
  }
  
  //@Override
  public Collection<Task> findTasksByProcessInstance(String processInstanceId, CIBUser user) {
    //TODO: not tested
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstanceId);
    List<org.cibseven.bpm.engine.task.Task> resultList = taskQuery.initializeFormKeys().list();
    return convertTasks(resultList);
  }
  
  //@Override
  public Collection<Task> findTasksByProcessInstanceAsignee(Optional<String> processInstanceId, Optional<String> createdAfter, CIBUser user) {
    //TODO: to be implemented
    TaskQuery taskQuery = taskService.createTaskQuery().taskAssignee(user.getId()).processInstanceId(processInstanceId.get());
    //TODO: create date from String
    if (createdAfter.isPresent()) {
      //taskQuery.taskCreatedAfter(createdAfter.get())
    }
//    //set assignee?
//    taskService.setAssignee(taskId, user.getId());
    List<org.cibseven.bpm.engine.task.Task> resultList = taskQuery.initializeFormKeys().list();
    return convertTasks(resultList);

  }
  
  //@Override
  public Task findTaskById(String id, CIBUser user) {
    org.cibseven.bpm.engine.task.Task result = taskService.createTaskQuery().taskId(id).initializeFormKeys().singleResult();
    return convertTask(result);
  }

  //@Override
  public void update(Task task, CIBUser user) {
    org.cibseven.bpm.engine.task.Task foundTask = taskService.createTaskQuery().taskId(task.getId()).initializeFormKeys().singleResult();

    if (foundTask == null) {
      //TODO: check exception type
      throw new IllegalArgumentException("No matching task with id " + task.getId());
    }

    foundTask.setName(task.getName());
    foundTask.setDescription(task.getDescription());
    foundTask.setPriority((int)task.getPriority());
    foundTask.setAssignee(task.getAssignee());
    foundTask.setOwner(task.getOwner());

    DelegationState state = null;
    if (task.getDelegationState() != null) {
      DelegationStateConverter converter = new DelegationStateConverter();
      state = converter.convertQueryParameterToType(task.getDelegationState());
    }
    foundTask.setDelegationState(state);

    //TODO: date conversion
    //foundTask.setDueDate(task.getDue());
    //foundTask.setFollowUpDate(task.getFollowUp());
    foundTask.setParentTaskId(task.getParentTaskId());
    foundTask.setCaseInstanceId(task.getCaseInstanceId());
    foundTask.setTenantId(task.getTenantId());    
    
    taskService.saveTask(foundTask);
  }
  
  //@Override
  public void setAssignee(String taskId, String assignee, CIBUser user) {
    org.cibseven.bpm.engine.task.Task foundTask = getTaskById(taskId);
    foundTask.setAssignee(assignee);
    taskService.saveTask(foundTask);
  }
  
  //@Override
  public void submit(String taskId, CIBUser user) {
    VariableMap variables = null; 
    formService.submitTaskForm(taskId, variables);
  }
  
  //@Override
  public void submit(Task task, List<Variable> formResult, CIBUser user) {
    //in progress
    ObjectMapper objectMapper = getObjectMapper();
    // VariableValueDto contains
//            protected String type;
//        protected Object value;
//        protected Map<String, Object> valueInfo;
//  formResult contains objects like Variable(name=season, type=String, value=Winter, 
//   valueSerialized=null, valueDeserialized=null, valueInfo=null)

// the name needs to put as key and the rest needs to be converted to VariableValueDto
// probably with     
//TODO: 'unfinished' is requested but not supported by the TaskQuery
//    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    
    Map<String, VariableValueDto> variables = new HashMap<>();
    //objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    for (Variable variable : formResult) {
      VariableValueDto variableValueDto = new VariableValueDto();
      variableValueDto.setType(variable.getType());
      variableValueDto.setValue(variable.getValue());
      if (variable.getValueInfo() != null)
        variableValueDto.setValueInfo(new HashMap<>(variable.getValueInfo()));
      variables.put(variable.getName(), variableValueDto);
    }
    CompleteTaskDto completeTaskDto = new CompleteTaskDto();
    completeTaskDto.setVariables(variables);
    
    try {
      VariableMap variablesMap = VariableValueDto.toMap(completeTaskDto.getVariables(), processEngine, objectMapper);
//      if (completeTaskDto.isWithVariablesInReturn()) {
//        VariableMap taskVariables = formService.submitTaskFormWithVariablesInReturn(task.getId(), variables, false);
//        //would be used to create return value
//        Map<String, VariableValueDto> variableValues = VariableValueDto.fromMap(taskVariables, true);
//      } else {
          formService.submitTaskForm(task.getId(), variablesMap);
//      }

    } catch (AuthorizationException e) {
      throw e;

    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot submit task form %s: %s", task.getId(), e.getMessage());
      //TODO: exception type
      throw new IllegalArgumentException(errorMessage, e);
    }
  }

//  @Override
  public Object formReference(String taskId, CIBUser user) {
    List<String> formVariables = null;
    String variableNames = "formReference";
    if(variableNames != null) {
      StringListConverter stringListConverter = new StringListConverter();
      formVariables = stringListConverter.convertQueryParameterToType(variableNames);
    }
    boolean deserializeValues = true;
    VariableMap startFormVariables = formService.getTaskFormVariables(taskId, formVariables, deserializeValues);
    Set<String> keys = startFormVariables.keySet();
    if (keys.isEmpty()) 
      return new String("empty-task"); 
    else {
      //TODO: result untested
      return VariableValueDto.fromMap(startFormVariables);    
    }
    
/*    
    String url = getEngineRestUrl() + "/task/" + taskId + "/form-variables?variableNames=formReference";
    ProcessVariables body =  ((ResponseEntity<ProcessVariables>) doGet(url, ProcessVariables.class, user, false)).getBody();
    if (body == null) {
      throw new NullPointerException();
    }
    Variable formReference = body.getFormReference();
    if (formReference == null) return new String("empty-task"); 
    else return formReference.getValue();

    final FormService formService = engine.getFormService();
    List<String> formVariables = null;

    if(variableNames != null) {
      StringListConverter stringListConverter = new StringListConverter();
      formVariables = stringListConverter.convertQueryParameterToType(variableNames);
    }

    VariableMap startFormVariables = formService.getTaskFormVariables(taskId, formVariables, deserializeValues);

    return VariableValueDto.fromMap(startFormVariables);


*/
  }
  
  //@Override
  public Object form(String taskId, CIBUser user) {
    org.cibseven.bpm.engine.task.Task task = getTaskById(taskId);
    FormData formData;
    try {
      formData = formService.getTaskFormData(taskId);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      // TODO: exception type
      throw new IllegalArgumentException("Cannot get form for task " + taskId, e);
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

    runWithoutAuthorization(() -> {
      String processDefinitionId = task.getProcessDefinitionId();
      String caseDefinitionId = task.getCaseDefinitionId();
      if (processDefinitionId != null) {
        dto.setContextPath(
            ApplicationContextPathUtil.getApplicationPathByProcessDefinitionId(processEngine, processDefinitionId));

      } else if (caseDefinitionId != null) {
        dto.setContextPath(
            ApplicationContextPathUtil.getApplicationPathByCaseDefinitionId(processEngine, caseDefinitionId));
      }
      return null;
    });

    TaskForm taskForm = new TaskForm();
    CamundaFormRef camundaFormRef = dto.getCamundaFormRef();
    if (camundaFormRef != null)
      taskForm.setCamundaFormRef(new CamundaForm(camundaFormRef.getKey(), camundaFormRef.getBinding(),
          Integer.toString(camundaFormRef.getVersion())));
    taskForm.setContextPath(dto.getContextPath());
    taskForm.setKey(dto.getKey());
    return taskForm;

  }
  private List<?> executeFilterList(TaskFiltering filters, String filterId, CIBUser user, 
      Integer firstResult, Integer maxResults) {
    //authentication is required to access the current user while executing the query
    GroupQuery groupQuery = identityService.createGroupQuery();
    List<Group> userGroups = groupQuery.groupMember(user.getId())
        .orderByGroupName()
        .asc()
        .unlimitedList();
    List<String> groupNames = new ArrayList<>();
    for (Group userGroup : userGroups)
      groupNames.add(userGroup.getId());
    
    Authentication authentication = new Authentication(user.getId(), groupNames);
    identityService.setAuthentication(authentication);
    
    String extendingQuery;
    try {
      extendingQuery = filters.json();
    } catch (JsonProcessingException e) {
      // TODO: exception type
      throw new IllegalArgumentException("Failed json conversion", e);
    }
    List<?> entities = executeFilterList(extendingQuery, filterId, firstResult, maxResults, getObjectMapper());
    return entities;
  }

  //@Override
  public Collection<Task> findTasksByFilter(TaskFiltering filters, String filterId, CIBUser user, 
      Integer firstResult, Integer maxResults) {
    //TODO: in progress
    List<?> entities = executeFilterList(filters, filterId, user, firstResult, maxResults);
//
    if (entities != null && !entities.isEmpty()) {
      //TODO: currently list of TaskDto
      ObjectMapper objectMapper = getObjectMapper();
      List<Task> list = convertToDtoList(entities, objectMapper);
      return list;
    }
    else {
      return Collections.emptyList();
    }
  }
  private List<Task> convertToDtoList(List<?> entities, ObjectMapper objectMapper) {
    List<Task> dtoList = new ArrayList<>();
    for (Object entity : entities) {
      dtoList.add(convertToDto(entity, objectMapper));
    }
    return dtoList;
  }
  private Task convertToDto(Object entity, ObjectMapper objectMapper) {
    if (entity instanceof org.cibseven.bpm.engine.task.Task) {
    //if (isEntityOfClass(entity, org.cibseven.bpm.engine.task.Task.class)) {
      return convertValue(objectMapper, TaskDto.fromEntity((org.cibseven.bpm.engine.task.Task) entity), Task.class);
    }
    else {
      //TODO: exception type
      throw new IllegalArgumentException("Entities of class '" + entity.getClass().getCanonicalName() + "' are currently not supported by filters."); 
    }
  }
  
  
  private List<?> executeFilterList(String extendingQueryString, String filterId, 
                    Integer firstResult, Integer maxResults, ObjectMapper objectMapper) {
    Query<?, ?> extendingQuery = convertQuery(extendingQueryString, filterId, objectMapper);
    try {
      if (firstResult != null || maxResults != null) {
        if (firstResult == null) {
          firstResult = 0;
        }
        if (maxResults == null) {
          maxResults = Integer.MAX_VALUE;
        }
        return filterService.listPage(filterId, extendingQuery, firstResult, maxResults);
      } else {
        return filterService.list(filterId, extendingQuery);
      }
    }
    catch (NullValueException e) {
      //TODO: exception type
      throw new IllegalArgumentException("Filter not found",e);
    }
  }
  private Query<?,?> convertQuery(String queryString, String filterId, ObjectMapper objectMapper) {
    if (isEmptyJson(queryString)) {
      return null;
    }
    else {
      String resourceType = filterService.getFilter(filterId).getResourceType();
      
      AbstractQueryDto<?> queryDto = getQueryDtoForQuery(queryString, resourceType, objectMapper);
      queryDto.setObjectMapper(objectMapper);
      return queryDto.toQuery(processEngine);
    }
  }
  private AbstractQueryDto<?> getQueryDtoForQuery(String queryString, String resourceType, ObjectMapper objectMapper) {
    try {
      if (EntityTypes.TASK.equals(resourceType)) {
        return objectMapper.readValue(queryString, TaskQueryDto.class);
      } else {
        //TODO: exception type
        throw new IllegalArgumentException("Queries for resource type '" + resourceType + "' are currently not supported by filters.");
      }
    } catch (IOException e) {
        //TODO: exception type
        throw new IllegalArgumentException("Invalid query for resource type '" + resourceType + "'", e);
    }
  }
  
  private boolean isEmptyJson(String jsonString) {
    final Pattern EMPTY_JSON_BODY = Pattern.compile("\\s*\\{\\s*\\}\\s*");
    return jsonString == null || jsonString.trim().isEmpty() || EMPTY_JSON_BODY.matcher(jsonString).matches();
  }

    
  //@Override
  public Integer findTasksCountByFilter(String filterId, CIBUser user, TaskFiltering filters) {
    List<?> entities = executeFilterList(filters, filterId, user, null, null);
    return entities.size();
  }
  
  //@Override
  public Collection<TaskHistory> findTasksByProcessInstanceHistory(String processInstanceId, CIBUser user) {
    //TODO: to be implemented
/*
    String url = getEngineRestUrl() + "/history/task?processInstanceId=" + processInstanceId + "&sortBy=startTime&sortOrder=desc";
    return Arrays.asList(((ResponseEntity<TaskHistory[]>) doGet(url, TaskHistory[].class, user, false)).getBody());
    
HistoricTaskInstanceRestServiceImpl
@Override
  public List<HistoricTaskInstanceDto> getHistoricTaskInstances(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    HistoricTaskInstanceQueryDto queryDto = new HistoricTaskInstanceQueryDto(objectMapper, uriInfo.getQueryParameters());
    return queryHistoricTaskInstances(queryDto, firstResult, maxResults);
  }

  @Override
  public List<HistoricTaskInstanceDto> queryHistoricTaskInstances(HistoricTaskInstanceQueryDto queryDto, Integer firstResult, Integer maxResults) {
    queryDto.setObjectMapper(objectMapper);
    HistoricTaskInstanceQuery query = queryDto.toQuery(processEngine);

    List<HistoricTaskInstance> match = QueryUtil.list(query, firstResult, maxResults);

    List<HistoricTaskInstanceDto> result = new ArrayList<HistoricTaskInstanceDto>();
    for (HistoricTaskInstance taskInstance : match) {
      HistoricTaskInstanceDto taskInstanceDto = HistoricTaskInstanceDto.fromHistoricTaskInstance(taskInstance);
      result.add(taskInstanceDto);
    }
    return result;
  }
    
*/
    return null;
  }
  
  //@Override
  public Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId, CIBUser user) {
    //TODO: to be implemented
/*
    String url = getEngineRestUrl() + "/history/task?processInstanceId=" + processInstanceId + "&taskDefinitionKey=" + taskDefinitionKey;
    return Arrays.asList(((ResponseEntity<TaskHistory[]>) doGet(url, TaskHistory[].class, user, false)).getBody());       

same as queryHistoricTaskInstances with add. taskDefinitionKey
*/
    return null;
  }
  
  //@Override
  public Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException {
    //TODO: to be implemented
/*
    String url = getEngineRestUrl() + "/task";
    return Arrays.asList(((ResponseEntity<Task[]>) doPost(url, data, Task[].class, user)).getBody());
data could be: 
{
 processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, 
 processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, 
 sortBy=created, 
 sortOrder=desc
 }
 
 TaskRestServiceImpl:
   public List<TaskDto> queryTasks(TaskQueryDto queryDto, Integer firstResult,
      Integer maxResults) {
    ProcessEngine engine = getProcessEngine();
    queryDto.setObjectMapper(getObjectMapper());
    TaskQuery query = queryDto.toQuery(engine);

    List<Task> matchingTasks = executeTaskQuery(firstResult, maxResults, query);

    List<TaskDto> tasks = new ArrayList<TaskDto>();
    if (Boolean.TRUE.equals(queryDto.getWithCommentAttachmentInfo())) {
      tasks = matchingTasks.stream().map(TaskWithAttachmentAndCommentDto::fromEntity).collect(Collectors.toList());
    }
    else {
      tasks = matchingTasks.stream().map(TaskDto::fromEntity).collect(Collectors.toList());
    }
    return tasks;
  }

result could be:
Task(assignee=null, 
caseDefinitionId=null, 
caseExecutionId=null, 
caseInstanceId=null, 
delegationState=null, 
description=null, 
executionId=8a620626-a356-11f0-afe5-4ce1734f67af, 
formKey=null, id=2fb6d245-a35a-11f0-afe5-4ce1734f67af, 
name=ShowDishSelection, 
owner=null, 
parentTaskId=null, 
priority=50, 
suspended=false, 
tenantId=null, 
camundaFormRef=CamundaForm(key=ShowDishSelection, binding=latest, version=null), 
created=2025-10-07T10:46:59.138+0200, due=null, followUp=null, 
taskDefinitionKey=Activity_0sy6qc5, 
processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af)
 * */    
    
    
    return null;
  }
  
  //@Override
  public Collection<IdentityLink> findIdentityLink(String taskId, Optional<String> type, CIBUser user) {

    List<org.cibseven.bpm.engine.task.IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);

    Collection<IdentityLink> result = new ArrayList<>();
    for (org.cibseven.bpm.engine.task.IdentityLink link : identityLinks) {
      if (type.isEmpty() || type.get().equals(link.getType())) {
        result.add(new IdentityLink(link.getUserId(), link.getGroupId(), link.getType()));
      }
    }
    return result;
  }

  //@Override
  public void createIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
    //TODO: untested
    String userId = (String)data.get("userId");
    String groupId = (String)data.get("groupId");
    if (userId != null && groupId != null) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity Link requires userId or groupId, but not both.");
    }
      
    if (userId == null && groupId == null) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity Link requires userId or groupId.");
    }

    String type = (String)data.get("type");
    if (userId != null) {
      taskService.addUserIdentityLink(taskId, userId, type);
    } else if (groupId != null) {
      taskService.addGroupIdentityLink(taskId, groupId, type);
    }
  }
  
  //@Override
  public void deleteIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
    //TODO: untested
    String userId = (String)data.get("userId");
    String groupId = (String)data.get("groupId");
    if (userId != null && groupId != null) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity Link requires userId or groupId, but not both.");
    }
      
    if (userId == null && groupId == null) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity Link requires userId or groupId.");
    }

    String type = (String)data.get("type");
    if (userId != null) {
      taskService.deleteUserIdentityLink(taskId, userId, type);
    } else if (groupId != null) {
      taskService.deleteGroupIdentityLink(taskId, groupId, type);
    }
    
  }
  
  //@Override
  public void handleBpmnError(String taskId, Map<String, Object> data, CIBUser user) throws SystemException {
    //TODO: to be implemented
  }

  //@Override
  public Collection<TaskHistory> findTasksByTaskIdHistory(String taskId, CIBUser user) {
    //TODO: to be implemented
    return null;
  } 
  
  //@Override
  public ResponseEntity<byte[]> getDeployedForm(String taskId, CIBUser user) {
    InputStream form = formService.getDeployedTaskForm(taskId);
    try {
      byte[] bytes = IOUtils.toByteArray(form);
      ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(bytes, HttpStatusCode.valueOf(200));
      return responseEntity;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new ResponseEntity<byte[]>(HttpStatusCode.valueOf(422));
  }
  
  //@Override
  public Integer findHistoryTasksCount(Map<String, Object> filters, CIBUser user) {
    //TODO: to be implemented
    return null;
  }

  //@Override
  public Collection<CandidateGroupTaskCount> getTaskCountByCandidateGroup(CIBUser user) {
    //TODO: to be implemented
    return null;
  }
  
  private Collection<Task> convertTasks(Collection<org.cibseven.bpm.engine.task.Task> engineTasks) {
    List<Task> resultList = new ArrayList<>();
    for( org.cibseven.bpm.engine.task.Task engineTask : engineTasks )
       resultList.add(convertTask(engineTask));
    return resultList;
  }
  
  //TODO: create Task-Ctor from org.cibseven.bpm.engine.task.Task   
  private Task convertTask(org.cibseven.bpm.engine.task.Task engineTask) {
    Task task = new Task(engineTask.getAssignee(), engineTask.getCaseDefinitionId(), engineTask.getCaseExecutionId(),
      engineTask.getCaseInstanceId(), 
      engineTask.getDelegationState() != null ? 
          engineTask.getDelegationState().toString() :
          null, 
      engineTask.getDescription(),
      engineTask.getExecutionId(), engineTask.getFormKey(), engineTask.getId(),
      engineTask.getName(), engineTask.getOwner(), engineTask.getParentTaskId(), 
      (long)engineTask.getPriority(), 
      engineTask.isSuspended() ? "true" : "false", 
       engineTask.getTenantId(),
       engineTask.getCamundaFormRef() != null ?  
        new CamundaForm(
            engineTask.getCamundaFormRef().getKey(), 
            engineTask.getCamundaFormRef().getBinding(), 
            engineTask.getCamundaFormRef().getVersion() != null ? 
              engineTask.getCamundaFormRef().getVersion().toString() : null) :
        null, 
       //TODO: time conversion required?
       engineTask.getCreateTime() != null ? engineTask.getCreateTime().toString() : null, 
       engineTask.getDueDate() != null ?engineTask.getDueDate().toString() : null, 
       engineTask.getFollowUpDate() != null ? engineTask.getFollowUpDate().toString() : null, 
       engineTask.getTaskDefinitionKey(), 
       engineTask.getProcessDefinitionId(), 
       engineTask.getProcessInstanceId()) ;
    
    return task;

  }
  
  private List<org.cibseven.bpm.engine.task.Task> queryTasks(Map<String, Object> filters, CIBUser user) {
    ObjectMapper objectMapper = getObjectMapper();
    //TODO: 'unfinished' is requested but not supported by the TaskQuery -> create Ticket!
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    TaskQueryDto dto = objectMapper.convertValue(filters, TaskQueryDto.class);
    TaskQuery taskQuery = dto.toQuery(processEngine);
    List<org.cibseven.bpm.engine.task.Task> taskList = 
        taskQuery.taskInvolvedUser(user.getUserID()).list();
    return taskList;
  }
  
/* 
  
  ██████  ██████   ██████   ██████ ███████ ███████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██   ██ ██   ██ ██    ██ ██      ██      ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██████  ██████  ██    ██ ██      █████   ███████ ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██      ██   ██ ██    ██ ██      ██           ██      ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ██      ██   ██  ██████   ██████ ███████ ███████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                            
   */
  
  //@Override
  public Collection<Process> findProcesses(CIBUser user) {
    MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
    // ProcessProvider adds:  "?latestVersion=true&sortBy=name&sortOrder=desc";"
    queryParameters.add("latestVersion", "true");
    queryParameters.add("sortBy", "name");
    queryParameters.add("sortOrder", "desc");
    ProcessDefinitionQueryDto queryDto = new ProcessDefinitionQueryDto(getObjectMapper(), queryParameters);
    List<ProcessDefinitionDto> definitions = new ArrayList<>();

    ProcessDefinitionQuery query = queryDto.toQuery(processEngine);
    List<ProcessDefinition> matchingDefinitions = QueryUtil.list(query, null, null);

    for (ProcessDefinition definition : matchingDefinitions) {
      ProcessDefinitionDto def = ProcessDefinitionDto.fromProcessDefinition(definition);
      definitions.add(def);
    }
    return convertProcesses(definitions);
  }
  
  // @Override
  public Collection<Process> findProcessesWithInfo(CIBUser user) {
    Map<String, Object> queryParams = new HashMap<>();
    queryParams.put("failedJobs", true);
    queryParams.put("incidents", true);
    Collection<ProcessStatistics> statisticsCollection = getProcessStatistics(queryParams, user);

    // Group by key and tenant ID to consolidate different versions
    List<ProcessStatistics> groupedStatistics = processProvider
        .groupProcessStatisticsByKeyAndTenant(statisticsCollection);
    // Build Process objects directly from grouped ProcessStatistics
    return groupedStatistics.stream().map(stats -> {
      Process process = new Process();
      ProcessDefinitionInfo definition = stats.getDefinition();

      // Copy fields from ProcessDefinitionInfo
      if (definition != null) {
        process.setId(definition.getId());
        process.setKey(definition.getKey());
        process.setCategory(definition.getCategory());
        process.setDescription(definition.getDescription());
        process.setName(definition.getName());
        process.setVersion(definition.getVersion() != null ? definition.getVersion().toString() : null);
        process.setResource(definition.getResource());
        process.setDeploymentId(definition.getDeploymentId());
        process.setDiagram(definition.getDiagram());
        process.setSuspended(definition.getSuspended() != null ? definition.getSuspended().toString() : null);
        process.setTenantId(definition.getTenantId());
        process.setVersionTag(definition.getVersionTag());
        process.setHistoryTimeToLive(
            definition.getHistoryTimeToLive() != null ? definition.getHistoryTimeToLive().toString() : null);
        process.setStartableInTasklist(definition.getStartableInTasklist());
      }

      // Set aggregated statistics data
      process.setRunningInstances(stats.getInstances());
      // Calculate total incidents from all incident types
      long totalIncidents = stats.getIncidents() != null
          ? stats.getIncidents().stream().mapToLong(incident -> incident.getIncidentCount()).sum()
          : 0L;
      process.setIncidents(totalIncidents);

      // Set default values for fields not available in statistics
      process.setAllInstances(stats.getInstances()); // Same as running
                                                     // instances for now
      process.setCompletedInstances(0L); // Would need separate call to get
                                         // completed instances

      return process;
    }).collect(Collectors.toList());

  }
  
  //@Override
  public Collection<Process> findProcessesWithFilters(String filters, CIBUser user) {

    Map<String, String> filterMap = new HashMap<>();
    String[] splitFilter = filters.split("&");
    for (String params : splitFilter) {
      String[] splitValue = params.split("=");
      if (splitValue.length > 1)
        filterMap.put(splitValue[0], URLDecoder.decode(splitValue[1], Charset.forName("UTF-8")));
    }
    
    ProcessDefinitionQueryDto queryDto = getObjectMapper().convertValue(filterMap, ProcessDefinitionQueryDto.class);

    List<Process> processes = new ArrayList<>();
    ProcessDefinitionQuery query = queryDto.toQuery(processEngine);
    List<ProcessDefinition> matchingDefinitions = QueryUtil.list(query, null, null);

    for (ProcessDefinition definition : matchingDefinitions) {
      ProcessDefinitionDto def = ProcessDefinitionDto.fromProcessDefinition(definition);
      processes.add(convertValue(getObjectMapper(), def, Process.class));
    }
    for(Process process : processes) {
      ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
      processInstanceQueryDto.setProcessDefinitionId(process.getId());
       process.setRunningInstances(queryProcessInstancesCount(processInstanceQueryDto));
    }
    return processes;
  } 
  
  private Long queryProcessInstancesCount(ProcessInstanceQueryDto queryDto) {
    queryDto.setObjectMapper(getObjectMapper());
    ProcessInstanceQuery query = queryDto.toQuery(processEngine);
    return query.count();
  }
  
//@Override
  public Process findProcessByDefinitionKey(String key, String tenantId, CIBUser user) {
    //TODO: not tested
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey(key);
    if (tenantId !=null)
      query.tenantIdIn(new String[] {tenantId});
    org.cibseven.bpm.engine.runtime.ProcessInstance instance = query.singleResult();
    if (instance == null) {
        throw new SystemException("Process instance not found: " + key);
    }
    Process process = convertValue(objectMapper, ProcessInstanceDto.fromProcessInstance(instance), Process.class);
    return process;
  }
  
  //@Override
  public Collection<Process> findProcessVersionsByDefinitionKey(String key, String tenantId, Optional<Boolean> lazyLoad, CIBUser user) {
    //TODO: to be implemented
    return null;
  }

  // @Override
  public Process findProcessById(String id, Optional<Boolean> extraInfo, CIBUser user) throws SystemException {
    ProcessDefinition definition;
    try {
      definition = repositoryService.getProcessDefinition(id);
    } catch (ProcessEngineException e) {
      throw new SystemException("No matching definition with id " + id, e);
    }

    ProcessDefinitionDto definitionDto = ProcessDefinitionDto.fromProcessDefinition(definition);
    Process process = convertValue(objectMapper, definitionDto, Process.class);
    if (extraInfo.isPresent() && extraInfo.get()) {
      Map<String, Object> filters = new HashMap<>();
      filters.put("processDefinitionId", id);
      Long count = countProcessesInstancesHistory(filters, user);
      process.setAllInstances(count);
      filters.clear();
      filters.put("processDefinitionId", process.getId());
      filters.put("unfinished", true);
      count = countProcessesInstancesHistory(filters, user);
      process.setRunningInstances(count);
      filters.clear();
      filters.put("processDefinitionId", process.getId());
      filters.put("completed", true);
      count = countProcessesInstancesHistory(filters, user);
      process.setCompletedInstances(count);
    }
    return process;
  }
    
  //@Override
  public Collection<ProcessInstance> findProcessesInstances(String key, CIBUser user) {
    //TODO: to be implemented
    //git copilot work
    
    List<ProcessInstance> result = new ArrayList<>();
    // Query the runtime service for process instances matching the given definition key
    List<org.cibseven.bpm.engine.runtime.ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey(key)
        .list();

    for (org.cibseven.bpm.engine.runtime.ProcessInstance camundaInstance : instances) {
        // Convert or map Camunda's ProcessInstance to your webclient's ProcessInstance type
        ProcessInstanceDto backendDto = ProcessInstanceDto.fromProcessInstance(camundaInstance);
        ProcessInstance webClientDto = convertValue(objectMapper, backendDto, ProcessInstance.class);
        result.add(webClientDto);
    }
    return result;
  }
  
  //@Override
  public ProcessDiagram fetchDiagram(String id, CIBUser user) {

    InputStream processModelIn = null;
    try {
      processModelIn = repositoryService.getProcessModel(id);
      byte[] processModel = IoUtil.readInputStream(processModelIn, "processModelBpmn20Xml");
      return convertValue(getObjectMapper(), ProcessDefinitionDiagramDto.create(id, new String(processModel, "UTF-8")), ProcessDiagram.class);
    } catch (AuthorizationException e) {
      throw e;
    } catch (NotFoundException e) {
      // TODO: check exception type
      throw new IllegalArgumentException( "No matching definition with id " + id, e);
    } catch (UnsupportedEncodingException e) {
      // TODO: check exception type
      throw new IllegalArgumentException(e.getMessage(), e);
    } finally {
      IoUtil.closeSilently(processModelIn);
    }
  }
  
  //@Override
  public StartForm fetchStartForm(String processDefinitionId, CIBUser user) {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public Data downloadBpmn(String id, String fileName, CIBUser user) {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public void suspendProcessInstance(String processInstanceId, Boolean suspend, CIBUser user) {
    ProcessInstanceSuspensionStateDto processInstanceSuspensionStateDto = new ProcessInstanceSuspensionStateDto();
    processInstanceSuspensionStateDto.setProcessInstanceIds(Arrays.asList(processInstanceId));
    processInstanceSuspensionStateDto.setSuspended(suspend);
    processInstanceSuspensionStateDto.updateSuspensionState(processEngine);
  }
  
  //@Override
  public void deleteProcessInstance(String processInstanceId, CIBUser user) {
    //TODO: to be implemented
  }
  
  //@Override
  public void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user) {
    //TODO: to be implemented
  }
  
  //@Override
  public ProcessStart startProcess(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user)
      throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
    ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey(processDefinitionKey);
    if (tenantId != null)
      processDefinitionQuery.tenantIdIn(tenantId);
    else
      processDefinitionQuery.withoutTenantId();
    ProcessDefinition processDefinition = processDefinitionQuery.latestVersion().singleResult();

    if (processDefinition == null) {
      String errorMessage = tenantId != null
          ? String.format("No matching process definition with key: %s and tenant-id: %s", processDefinitionKey,
              tenantId)
          : String.format("No matching process definition with key: %s and no tenant-id", processDefinitionKey);
      // TODO: check exception type
      throw new IllegalArgumentException(errorMessage);

    } else {
      // start the process
      ProcessInstanceWithVariables processInstanceWithVariables = null;
      // the simple case contains the _locale variable, only
      StartProcessInstanceDto startProcessInstanceDto = new StartProcessInstanceDto();
      // TODO: nothing to be set?
      // startProcessInstanceDto.setBusinessKey();
      // startProcessInstanceDto.setCaseInstanceId();
      // startProcessInstanceDto.setSkipCustomListeners();
      // startProcessInstanceDto.setSkipIoMappings();
      // startProcessInstanceDto.setStartInstructions();
      // startProcessInstanceDto.setVariables();
      // startProcessInstanceDto.setWithVariablesInReturn();
      try {
        processInstanceWithVariables = startProcessInstanceAtActivities(startProcessInstanceDto,
            processDefinition.getId());
      } catch (AuthorizationException e) {
        throw e;

      } catch (ProcessEngineException e) {
        String errorMessage = String.format("Cannot instantiate process definition %s: %s", processDefinition.getId(),
            e.getMessage());
        // TODO: exception type!
        throw new RuntimeException(errorMessage, e);
      }

      ProcessInstanceDto result;
      if (startProcessInstanceDto.isWithVariablesInReturn()) {
        result = ProcessInstanceWithVariablesDto.fromProcessInstance(processInstanceWithVariables);
      } else {
        result = ProcessInstanceDto.fromProcessInstance(processInstanceWithVariables);
      }

      // creates something like
      // http://localhost:8080/engine-rest/process-instance/804e094c-9f90-11f0-89f5-4ce1734f67af
      // getEngineRestUrl() will be a base class function once this instance is
      // ready
      String url = /* getEngineRestUrl() */ "http://localhost:8080/engine-rest" + "/process-instance/" + result.getId();
      try {
        URI uri = new URI(url);
        result.addReflexiveLink(uri, HttpMethod.GET, "self");
      } catch (URISyntaxException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      ProcessDefinition definition;
      try {
        definition = repositoryService.getProcessDefinition(processDefinition.getId());
      } catch (ProcessEngineException e) {
        throw new IllegalArgumentException("No matching definition with id " + processDefinition.getId(), e);
      }

      return createProcessDefinitionQuery(definition);
    }

  }
  
  //TODO: rename, move to util class
  private ProcessInstanceWithVariables startProcessInstanceAtActivities(StartProcessInstanceDto dto, String processDefinitionKey) {
    ObjectMapper objectMapper = getObjectMapper();
    Map<String, Object> processInstanceVariables = VariableValueDto.toMap(dto.getVariables(), processEngine, objectMapper);
    String businessKey = dto.getBusinessKey();
    String caseInstanceId = dto.getCaseInstanceId();

    ProcessInstantiationBuilder instantiationBuilder = runtimeService
        .createProcessInstanceById(processDefinitionKey)
        .businessKey(businessKey)
        .caseInstanceId(caseInstanceId)
        .setVariables(processInstanceVariables);

    if (dto.getStartInstructions() != null && !dto.getStartInstructions().isEmpty()) {
      for (ProcessInstanceModificationInstructionDto instruction : dto.getStartInstructions()) {
        instruction.applyTo(instantiationBuilder, processEngine, objectMapper);
      }
    }

    return instantiationBuilder.executeWithVariablesInReturn(dto.isSkipCustomListeners(), dto.isSkipIoMappings());
  }
  
  
  private ProcessStart createProcessDefinitionQuery(ProcessDefinition definition) {
    //TODO: Find missing parameters
    ProcessStart processStart = new ProcessStart();
//    processStart.setBusinessKey(definition);
//    processStart.setCaseInstanceId(definition);
    processStart.setDefinitionId(definition.getId());
//    processStart.setEnded(definition);
    processStart.setId(definition.getId());
//    processStart.setLinks(definition.);
    processStart.setSuspended(definition.isSuspended());
    processStart.setTenantId(definition.getTenantId());
    return processStart;
  }

  //@Override
  public ProcessStart submitForm(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public Collection<ProcessStatistics> findProcessStatistics(String processId, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
    //TODO: to be implemented
    /*
ProcessDefinitionRestServiceImpl creates resource to processId
  public ProcessDefinitionResource getProcessDefinitionById(
      String processDefinitionId) {
    return new ProcessDefinitionResourceImpl(getProcessEngine(), processDefinitionId, relativeRootResourcePath, getObjectMapper());
  }
   which creates the object with the following values
    this.engine = engine;
    this.processDefinitionId = processDefinitionId;
    this.rootResourcePath = rootResourcePath;
    this.objectMapper = objectMapper;

*/

/*

ProcessDefinitionResourceImpl  public List<StatisticsResultDto> getActivityStatistics(Boolean includeFailedJobs, Boolean includeIncidents, String includeIncidentsForType) {

    if (includeIncidents != null && includeIncidentsForType != null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Only one of the query parameter includeIncidents or includeIncidentsForType can be set.");
    }

    ManagementService mgmtService = engine.getManagementService();
    ActivityStatisticsQuery query = mgmtService.createActivityStatisticsQuery(processDefinitionId);

    if (includeFailedJobs != null && includeFailedJobs) {
      query.includeFailedJobs();
    }

    if (includeIncidents != null && includeIncidents) {
      query.includeIncidents();
    } else if (includeIncidentsForType != null) {
      query.includeIncidentsForType(includeIncidentsForType);
    }

    List<ActivityStatistics> queryResults = query.unlimitedList();

    List<StatisticsResultDto> results = new ArrayList<>();
    for (ActivityStatistics queryResult : queryResults) {
      StatisticsResultDto dto = ActivityStatisticsResultDto.fromActivityStatistics(queryResult);
      results.add(dto);
    }

    return results;*/
    
    ActivityStatisticsQuery query = managementService.createActivityStatisticsQuery(processId);

//    if (includeFailedJobs != null && includeFailedJobs) {
//      query.includeFailedJobs();
//    }

//    if (includeIncidents != null && includeIncidents) {
//      query.includeIncidents();
//    } else if (includeIncidentsForType != null) {
//      query.includeIncidentsForType(includeIncidentsForType);
//    }

    List<ActivityStatistics> queryResults = query.unlimitedList();

//    List<StatisticsResultDto> results = new ArrayList<>();
    Collection<ProcessStatistics> processStatistics = new ArrayList<>();
    for (ActivityStatistics queryResult : queryResults) {
      ProcessStatistics processStatistic = createProcessStatistics(queryResult);
      //StatisticsResultDto dto = ActivityStatisticsResultDto.fromActivityStatistics(queryResult);
      processStatistics.add(processStatistic);
    }
    return processStatistics;
  }


  private ProcessStatistics createProcessStatistics(ActivityStatistics activityStatistics) {
    ProcessStatistics result = new ProcessStatistics();
    result.setFailedJobs(activityStatistics.getFailedJobs());
    result.setId(activityStatistics.getId());
    List<IncidentInfo> incidents = new ArrayList<>();
    for (IncidentStatistics incidentStatistics : activityStatistics.getIncidentStatistics()) {
       IncidentInfo incidentInfo = new IncidentInfo();
       incidentInfo.setIncidentCount(Long.valueOf(incidentStatistics.getIncidentCount()));
       incidentInfo.setIncidentType(incidentStatistics.getIncidentType());
       incidents.add(incidentInfo);
    }
    result.setIncidents(incidents);
    result.setInstances(activityStatistics.getInstances());
    return result;
  }

  //@Override
  public Collection<ProcessStatistics> getProcessStatistics(Map<String, Object> queryParams, CIBUser user) {
    
    Boolean includeIncidents = (Boolean)queryParams.get("incidents");
    String includeIncidentsForType = (String)queryParams.get("incidentsForType");
    Boolean includeRootIncidents = (Boolean)queryParams.get("rootIncidents");
    Boolean includeFailedJobs = (Boolean)queryParams.get("failedJobs");
    if (includeIncidents != null && includeIncidentsForType != null) {
      throw new IllegalArgumentException("Only one of the query parameter includeIncidents or includeIncidentsForType can be set.");
    }

    if (includeIncidents != null && includeRootIncidents != null) {
      throw new IllegalArgumentException("Only one of the query parameter includeIncidents or includeRootIncidents can be set.");
    }

    if (includeRootIncidents != null && includeIncidentsForType != null) {
      throw new IllegalArgumentException("Only one of the query parameter includeRootIncidents or includeIncidentsForType can be set.");
    }
    
     ProcessDefinitionStatisticsQuery query = managementService.createProcessDefinitionStatisticsQuery();

    if (includeFailedJobs != null && includeFailedJobs) {
      query.includeFailedJobs();
    }

    if (includeIncidents != null && includeIncidents) {
      query.includeIncidents();
    } else if (includeIncidentsForType != null) {
      query.includeIncidentsForType(includeIncidentsForType);
    } else if (includeRootIncidents != null && includeRootIncidents) {
      query.includeRootIncidents();
    }

    List<ProcessDefinitionStatistics> queryResults = query.unlimitedList();

    Collection<ProcessStatistics> processStatistics = new ArrayList<>();
    for (ProcessDefinitionStatistics queryResult : queryResults) {
      ProcessStatistics processStatistic = createProcessStatistics(queryResult);
      processStatistics.add(processStatistic);
    }

    return processStatistics;

  }
  
  //TODO: move into ProcessStatistics as createFromProcessDefinitionStatistics() 
  private ProcessStatistics createProcessStatistics(ProcessDefinitionStatistics processDefinitionStatistics) {
    ProcessStatistics result = new ProcessStatistics();
    result.setDefinition(createDefinition(processDefinitionStatistics));
    result.setFailedJobs(processDefinitionStatistics.getFailedJobs());
    result.setId(processDefinitionStatistics.getId());
    List<IncidentInfo> incidents = new ArrayList<>();
    for (IncidentStatistics incidentStatistics : processDefinitionStatistics.getIncidentStatistics()) {
       IncidentInfo incidentInfo = new IncidentInfo();
       incidentInfo.setIncidentCount(Long.valueOf(incidentStatistics.getIncidentCount()));
       incidentInfo.setIncidentType(incidentStatistics.getIncidentType());
       incidents.add(incidentInfo);
    }
    result.setIncidents(incidents);
    result.setInstances(processDefinitionStatistics.getInstances());
    return result;
  }

  private ProcessDefinitionInfo createDefinition(ProcessDefinition definition) {
    ProcessDefinitionInfo processDefinitionInfo = new ProcessDefinitionInfo();
    processDefinitionInfo.setCategory(definition.getCategory());
    processDefinitionInfo.setDeploymentId(definition.getDeploymentId());
    processDefinitionInfo.setDescription(definition.getDescription());
    processDefinitionInfo.setDiagram(definition.getDiagramResourceName());
    processDefinitionInfo.setHistoryTimeToLive(definition.getHistoryTimeToLive());
    processDefinitionInfo.setId(definition.getId());
    processDefinitionInfo.setKey(definition.getKey());
    processDefinitionInfo.setName(definition.getName());
    processDefinitionInfo.setResource(definition.getResourceName());
    processDefinitionInfo.setStartableInTasklist(definition.isStartableInTasklist());
    processDefinitionInfo.setSuspended(definition.isSuspended());
    processDefinitionInfo.setTenantId(definition.getTenantId());
    processDefinitionInfo.setVersion(definition.getVersion());
    processDefinitionInfo.setVersionTag(definition.getVersionTag());
    return processDefinitionInfo;
  }

  //@Override
  public Collection<HistoryProcessInstance> findProcessesInstancesHistory(Map<String, Object> filters,
      Optional<Integer> firstResult, Optional<Integer> maxResults, CIBUser user) {
    Boolean fetchIncidents = (Boolean) filters.get("fetchIncidents");
    if (fetchIncidents != null) {
      filters.remove("fetchIncidents");
    }
    ObjectMapper objectMapper = getObjectMapper();
    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto =
        objectMapper.convertValue(filters, HistoricProcessInstanceQueryDto.class);
  
    historicProcessInstanceQueryDto.setObjectMapper(objectMapper);
    HistoricProcessInstanceQuery query = historicProcessInstanceQueryDto.toQuery(processEngine);

    List<HistoricProcessInstance> matchingHistoricProcessInstances = 
        QueryUtil.list(query, 
            firstResult.isPresent() ? firstResult.get() : null,
            maxResults.isPresent() ? maxResults.get() : null);

    List<HistoryProcessInstance> historicProcessInstanceResults = new ArrayList<HistoryProcessInstance>();
    for (HistoricProcessInstance historicProcessInstance : matchingHistoricProcessInstances) {
      HistoricProcessInstanceDto resultHistoricProcessInstanceDto = HistoricProcessInstanceDto.fromHistoricProcessInstance(historicProcessInstance);
      historicProcessInstanceResults.add(convertValue(objectMapper, resultHistoricProcessInstanceDto, HistoryProcessInstance.class));
    }
    // Check if caller wants incident handling
    if (fetchIncidents != null && fetchIncidents) {
      String processDefinitionId = (String) filters.get("processDefinitionId");
      if (processDefinitionId != null) {
        @SuppressWarnings("unchecked")
        List<String> activityIdIn = (List<String>) filters.get("activeActivityIdIn");

        // Handle case where no processes found with activity filter - fallback to incident-based search
        if ((historicProcessInstanceResults == null || historicProcessInstanceResults.isEmpty()) && activityIdIn != null && !activityIdIn.isEmpty()) {
          String activityId = activityIdIn.get(0);
Collection<Incident> incidents = fetchIncidentsByInstanceAndActivityId(processDefinitionId, activityId, user);

          if (incidents != null && !incidents.isEmpty()) {
            Map<String, List<Incident>> incidentsByProcessInstance = incidents.stream()
              .collect(Collectors.groupingBy(Incident::getProcessInstanceId));

            Set<String> processInstanceIds = incidentsByProcessInstance.keySet();

            // Create new query for process instances with incidents
            Map<String, Object> dataIdIn = new HashMap<>(filters);
            dataIdIn.put("processInstanceIdIn", processInstanceIds);
            dataIdIn.remove("activeActivityIdIn"); // Remove activity filter for fallback search

//TODO: find&implemented related call
            //could be something like
            historicProcessInstanceResults = 
                (List<HistoryProcessInstance>) findProcessesInstancesHistory(dataIdIn, Optional.ofNullable(null), Optional.ofNullable(null), user);
//historicProcessInstanceResults = Arrays.asList(
//    ((ResponseEntity<HistoryProcessInstance[]>) 
//        doPost(url, dataIdIn, HistoryProcessInstance[].class, user)).getBody()
//  );

            // Associate incidents with process instances
            historicProcessInstanceResults.forEach(p -> p.setIncidents(incidentsByProcessInstance.getOrDefault(p.getId(), Collections.emptyList())));
          }
        } else if (historicProcessInstanceResults != null) {
          // For regular queries, fetch incidents for all returned processes
          historicProcessInstanceResults.forEach(p -> {
            p.setIncidents(findIncidentByInstanceId(p.getId(), user));
          });
        }
      }
    }
    return historicProcessInstanceResults;

  }
  
  //@Override
  public Collection<HistoryProcessInstance> findProcessesInstancesHistory(String key, Optional<Boolean> active, 
      Integer firstResult, Integer maxResults, CIBUser user) {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public Collection<HistoryProcessInstance> findProcessesInstancesHistoryById(String id, Optional<String> activityId, Optional<Boolean> active, 
      Integer firstResult, Integer maxResults, String text, CIBUser user) {
    //TODO: not tested
  
    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto = new HistoricProcessInstanceQueryDto();
    historicProcessInstanceQueryDto.setProcessDefinitionId(id);
    if (activityId.isPresent())
      historicProcessInstanceQueryDto.setActivityIdIn(Arrays.asList(activityId.get()));
    if (active.isPresent())
      historicProcessInstanceQueryDto.setActive(active.get());
    historicProcessInstanceQueryDto.setObjectMapper(getObjectMapper());
    HistoricProcessInstanceQuery query = historicProcessInstanceQueryDto.toQuery(processEngine);
    List<HistoricProcessInstance> matchingHistoricProcessInstances = QueryUtil.list(query, firstResult, maxResults);

    List<HistoryProcessInstance> HistoryProcessInstanceResults = new ArrayList<HistoryProcessInstance>();
    for (HistoricProcessInstance historicProcessInstance : matchingHistoricProcessInstances) {
      HistoricProcessInstanceDto resultHistoricProcessInstanceDto = HistoricProcessInstanceDto.fromHistoricProcessInstance(historicProcessInstance);
      HistoryProcessInstanceResults.add(convertValue(getObjectMapper(), resultHistoricProcessInstanceDto, HistoryProcessInstance.class));
    }
    return HistoryProcessInstanceResults;

  }
  
  //@Override
  public Long countProcessesInstancesHistory(Map<String, Object> filters, CIBUser user) {
    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto = getObjectMapper().convertValue(filters, HistoricProcessInstanceQueryDto.class);  
    
    historicProcessInstanceQueryDto.setObjectMapper(getObjectMapper());
    HistoricProcessInstanceQuery query = historicProcessInstanceQueryDto.toQuery(processEngine);

    long count = query.count();
    return count;
  }
  
  //@Override
  public ProcessInstance findProcessInstance(String processInstanceId, CIBUser user) {
    //TODO: not tested
    org.cibseven.bpm.engine.runtime.ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

    if (instance == null) {
      //TODO: exception type
      throw new IllegalArgumentException("Process instance with id " + processInstanceId + " does not exist");
    }

    ProcessInstanceDto result = ProcessInstanceDto.fromProcessInstance(instance);
    return convertValue(getObjectMapper(), result, ProcessInstance.class);
  }

  //@Override
  public Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, boolean deserializeValue, CIBUser user) throws SystemException  {
    //TODO: to be implemented
/*
    
variablesprovider:    
    UriComponentsBuilder uriBuilder = UriComponentsBuilder
      .fromUriString(getEngineRestUrl())
      .path("/variable-instance");

    if (data != null) {
      data.forEach((key, value) -> {
        if (value != null) {
          uriBuilder.queryParam(key, value);
        }
      });
    }
    uriBuilder.queryParam("processInstanceIdIn", processInstanceId);

    final boolean deserializeValues = data != null
      && data.containsKey("deserializeValues")
      && (Boolean) data.get("deserializeValues");

    uriBuilder.replaceQueryParam("deserializeValues", "true");
    String urlDeserialized = uriBuilder.build().toUriString();
    Collection<Variable> variablesDeserialized = Arrays.asList(((ResponseEntity<VariableHistory[]>) doGet(urlDeserialized, VariableHistory[].class, user, false)).getBody());
    if (variablesDeserialized == null) {
      return Collections.emptyList();
    }

    uriBuilder.replaceQueryParam("deserializeValues", "false");
    String urlSerialized = uriBuilder.build().toUriString();
    Collection<Variable> variablesSerialized = Arrays.asList(((ResponseEntity<VariableHistory[]>) doGet(urlSerialized, VariableHistory[].class, user, false)).getBody());
    if (variablesSerialized == null) {
      return Collections.emptyList();
    }

    mergeVariablesValues(
      variablesDeserialized,
      variablesSerialized,
      deserializeValues);

    Collection<Variable> variables = (deserializeValues) ? variablesDeserialized : variablesSerialized;
    return variables;

*/
    return null;
  }
  
  //@Override
  public HistoryProcessInstance findHistoryProcessInstanceHistory(String processInstanceId, CIBUser user) {
    HistoricProcessInstance instance = 
        historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
    if (instance == null) {
      //TODO: exception type
      throw new IllegalArgumentException("Historic process instance with id " + processInstanceId + " does not exist");
    }

    HistoryProcessInstance historyProcessInstance = convertValue(getObjectMapper(), 
        HistoricProcessInstanceDto.fromHistoricProcessInstance(instance), 
        HistoryProcessInstance.class);;
    return historyProcessInstance;
  }
  
  //@Override
  public Collection<Process> findCalledProcessDefinitions(String processDefinitionId, CIBUser user) {
    try {
      List<Process> calledProcessDefinitionDtos = 
          processEngine.getRepositoryService().getStaticCalledProcessDefinitions(processDefinitionId).stream()
            .map(CalledProcessDefinitionDto::from)
            .map(SevenDirectProvider::convertToProcess)
            .collect(Collectors.toList());
      return calledProcessDefinitionDtos;
    } catch (NotFoundException e) {
      //TODO: exception type
      throw new IllegalArgumentException(e.getMessage());
    }
    
  }
  
  private static Process convertToProcess(CalledProcessDefinitionDto dto) {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> filterDtoMap = objectMapper
        .convertValue(dto, new TypeReference<Map<String, Object>>() {});
    return objectMapper.convertValue(filterDtoMap, Process.class);
  }
  
  //@Override
  public ResponseEntity<byte[]> getDeployedStartForm(String processDefinitionId, CIBUser user) {
    //TODO: to be implemented
    return null;
  }

  //@Override
  public void updateHistoryTimeToLive(String id, Map<String, Object> data, CIBUser user) {
    //TODO: to be implemented
  }

  //@Override
  public void deleteProcessInstanceFromHistory(String id, CIBUser user) {
    //TODO: to be implemented
  }
  
  //@Override
  public void deleteProcessDefinition(String id, Optional<Boolean> cascade, CIBUser user) {
    //TODO: to be implemented
  }
  
  //@Override
  public Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user)
      throws SystemException {

    ProcessInstanceQueryDto queryDto = objectMapper.convertValue(data, ProcessInstanceQueryDto.class);
    queryDto.setObjectMapper(getObjectMapper());
    ProcessInstanceQuery query = queryDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.runtime.ProcessInstance> matchingInstances = QueryUtil.list(query, null, null);

    List<ProcessInstance> instanceResults = new ArrayList<>();
    for (org.cibseven.bpm.engine.runtime.ProcessInstance instance : matchingInstances) {
      ProcessInstanceDto resultInstance = ProcessInstanceDto.fromProcessInstance(instance);
      instanceResults.add(convertValue(getObjectMapper(), resultInstance, ProcessInstance.class));
    }
    return instanceResults;
  }

  //@Override
  public Object fetchHistoricActivityStatistics(String id, Map<String, Object> params, CIBUser user) {
    //TODO: to be implemented
    return null;
  }
  

  private Process convertProcess(ProcessDefinitionDto definition) {
    Process process = new Process(
      definition.getCategory(),
      definition.getDeploymentId(),
      definition.getDescription(),
      definition.getDiagram(),
      //TODO: int to string?
      definition.getHistoryTimeToLive().toString(),
      definition.getId(),
      definition.getKey(),
      definition.getName(),
      definition.getResource(),
      definition.isStartableInTasklist(),
      //TODO: string values?
      definition.isSuspended() ? "TRUE" : "FALSE",
      definition.getTenantId(),
      // int to string?
      Integer.toString(definition.getVersion()),
      definition.getVersionTag(),
      null, //List<String> calledFromActivityIds;
      0, //long runningInstances;
      0, //long allInstances;
      0, //long completedInstances;
      0 //long incidents;
    );
    return process;
  }
  private Collection<Process> convertProcesses(List<ProcessDefinitionDto> definitions) {
    List<Process> processes = new ArrayList<>();
    for (ProcessDefinitionDto definition : definitions) {
      processes.add(convertProcess(definition));
    }
    return processes;
  }
  /*    
  
  ███████ ██ ██      ████████ ███████ ██████      ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██      ██ ██         ██    ██      ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  █████   ██ ██         ██    █████   ██████      ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██      ██ ██         ██    ██      ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ██      ██ ███████    ██    ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                              
   */
  
  //@Override
  public Collection<Filter> findFilters(CIBUser user) {
    FilterQueryDto filterQueryDto = new FilterQueryDto();
    filterQueryDto.setResourceType("Task");
    FilterQuery query = filterQueryDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.filter.Filter> matchingFilters = QueryUtil.list(query, null, null);

    List<Filter> filters = new ArrayList<>();
    ObjectMapper objectMapper = getObjectMapper();
    for (org.cibseven.bpm.engine.filter.Filter filter : matchingFilters) {
      FilterDto filterDto = FilterDto.fromFilter(filter);
      // TODO: itemCount not used?
      // if (itemCount != null && itemCount) {
      // dto.setItemCount(filterService.count(filter.getId()));
      // }
      filters.add(convertValue(objectMapper, filterDto, Filter.class));
    }
    return filters;
  }

  //@Override
  public  Filter createFilter(Filter filter, CIBUser user) {
  //TODO: untested
    ObjectMapper objectMapper = getObjectMapper();
    FilterDto filterDto = convertValue(objectMapper, filter, FilterDto.class); 
    String resourceType = filterDto.getResourceType();

    org.cibseven.bpm.engine.filter.Filter engineFilter;
    if (EntityTypes.TASK.equals(resourceType)) {
      engineFilter = filterService.newTaskFilter();
    }
    else {
      //TODO: exception type
      throw new IllegalArgumentException("Unable to create filter with invalid resource type '" + resourceType + "'");
    }

    try {
      filterDto.updateFilter(engineFilter, processEngine);
    }
    catch (NotValidException e) {
      //TODO: exception type
      throw new IllegalArgumentException("Unable to create filter with invalid content", e);
    }

    filterService.saveFilter(engineFilter);

    Filter resultFilter = convertValue(objectMapper, FilterDto.fromFilter(engineFilter), Filter.class);
    return resultFilter;
  }
  //@Override
  public void updateFilter(Filter filter, CIBUser user) {
  //TODO: untested
    ObjectMapper objectMapper = getObjectMapper();
    FilterDto filterDto = convertValue(objectMapper, filter, FilterDto.class); 
    org.cibseven.bpm.engine.filter.Filter dbFilter = filterService.getFilter(filter.getId());

    if (dbFilter == null) {
      //TODO: exception type
      throw new IllegalArgumentException("Requested filter not found: " + filter.getId());
    }

    try {
      filterDto.updateFilter(dbFilter, processEngine);
    }
    catch (NotValidException e) {
      //TODO: exception type
      throw new IllegalArgumentException("Unable to update filter with invalid content", e);
    }
    filterService.saveFilter(dbFilter);
  }

  //@Override
  public void deleteFilter(String filterId, CIBUser user) {
    //TODO: untested
    try {
      filterService.deleteFilter(filterId);
    }
    catch (NullValueException e) {
      //TODO: exception type
      throw new IllegalArgumentException("Requested filter not found: " + filterId);
    }
  }
  

  /*
  
  ██████  ███████ ██████  ██       ██████  ██    ██ ███    ███ ███████ ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██   ██ ██      ██   ██ ██      ██    ██  ██  ██  ████  ████ ██      ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██   ██ █████   ██████  ██      ██    ██   ████   ██ ████ ██ █████   ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██   ██ ██      ██      ██      ██    ██    ██    ██  ██  ██ ██      ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ██████  ███████ ██      ███████  ██████     ██    ██      ██ ███████ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                                                                                                                                                           
   */
  
  //@Override
  public Deployment deployBpmn(MultiValueMap<String, Object> data, MultiValueMap<String, MultipartFile> file, CIBUser user) throws SystemException {
    //TODO: not tested
    //MultipartFormData payload;
    //TODO: add authorization
    ObjectMapper objectMapper = getObjectMapper();
    file.forEach((key, value) -> { 
      try {
        data.add(key, value.get(0).getResource());
      } catch (Exception e) {
        throw new SystemException(e);
      }
    });
    DeploymentBuilder deploymentBuilder = extractDeploymentInformation(convertValue(objectMapper, data, MultipartFormData.class));

    if(!deploymentBuilder.getResourceNames().isEmpty()) {
      DeploymentWithDefinitions deployment = deploymentBuilder.deployWithResult();

      DeploymentWithDefinitionsDto deploymentDto = DeploymentWithDefinitionsDto.fromDeployment(deployment);


      //TODO: getEngineRestUrl() will be a base class method after completion
      UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://localhost:8080");//getEngineRestUrl());
      URI uri = builder
        .path("/")//relativeRootResourcePath)
        .path("/deployment")//DeploymentRestService.PATH)
        .path(deployment.getId())
        .build().toUri();

      // GET
      deploymentDto.addReflexiveLink(uri, HttpMethod.GET, "self");
      return convertValue(objectMapper, deploymentDto, Deployment.class);

    } else {
      //TODO: exception type 
      throw new IllegalArgumentException("No deployment resources contained in the form upload.");
    }
/*
    String url = getEngineRestUrl() + "/deployment/create";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    if (user != null) {
      headers.add(HttpHeaders.AUTHORIZATION, user.getAuthToken());
      headers.add(USER_ID_HEADER, user.getId());
    }
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

    file.forEach((key, value) -> { 
      try {
        data.add(key, value.get(0).getResource());
      } catch (Exception e) {
        throw new SystemException(e);
      }
    });

    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(data, headers);

    try {
      return customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, Deployment.class).getBody();
    } catch (HttpStatusCodeException e) {
      throw wrapException(e, user);
    }
 * */    
  }

  //TODO: change interface to use MultiValueMap<String, Object> instead of MultipartFormData
  private DeploymentBuilder extractDeploymentInformation(MultipartFormData payload) {
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();

    Set<String> partNames = payload.getPartNames();

    for (String name : partNames) {
      FormPart part = payload.getNamedPart(name);

      if (!RESERVED_KEYWORDS.contains(name)) {
        String fileName = part.getFileName();
        if (fileName != null) {
          deploymentBuilder.addInputStream(part.getFileName(), new ByteArrayInputStream(part.getBinaryContent()));
        } else {
          //TODO: exception type 
          throw new IllegalArgumentException("No file name found in the deployment resource described by form parameter '" + fileName + "'.");
        }
      }
    }

    FormPart deploymentName = payload.getNamedPart(DEPLOYMENT_NAME);
    if (deploymentName != null) {
      deploymentBuilder.name(deploymentName.getTextContent());
    }

    FormPart deploymentActivationTime = payload.getNamedPart(DEPLOYMENT_ACTIVATION_TIME);
    if (deploymentActivationTime != null && !deploymentActivationTime.getTextContent().isEmpty()) {
      deploymentBuilder.activateProcessDefinitionsOn(DateTimeUtil.parseDate(deploymentActivationTime.getTextContent()));
    }

    FormPart deploymentSource = payload.getNamedPart(DEPLOYMENT_SOURCE);
    if (deploymentSource != null) {
      deploymentBuilder.source(deploymentSource.getTextContent());
    }

    FormPart deploymentTenantId = payload.getNamedPart(TENANT_ID);
    if (deploymentTenantId != null) {
      deploymentBuilder.tenantId(deploymentTenantId.getTextContent());
    }

    extractDuplicateFilteringForDeployment(payload, deploymentBuilder);
    return deploymentBuilder;
  }

  private void extractDuplicateFilteringForDeployment(MultipartFormData payload, DeploymentBuilder deploymentBuilder) {
    boolean enableDuplicateFiltering = false;
    boolean deployChangedOnly = false;

    FormPart deploymentEnableDuplicateFiltering = payload.getNamedPart(ENABLE_DUPLICATE_FILTERING);
    if (deploymentEnableDuplicateFiltering != null) {
      enableDuplicateFiltering = Boolean.parseBoolean(deploymentEnableDuplicateFiltering.getTextContent());
    }

    FormPart deploymentDeployChangedOnly = payload.getNamedPart(DEPLOY_CHANGED_ONLY);
    if (deploymentDeployChangedOnly != null) {
      deployChangedOnly = Boolean.parseBoolean(deploymentDeployChangedOnly.getTextContent());
    }

    // deployChangedOnly overrides the enableDuplicateFiltering setting
    if (deployChangedOnly) {
      deploymentBuilder.enableDuplicateFiltering(true);
    } else if (enableDuplicateFiltering) {
      deploymentBuilder.enableDuplicateFiltering(false);
    }
  }


  //@Override
  public Long countDeployments(CIBUser user, String nameLike) {
    ObjectMapper objectMapper = getObjectMapper();
    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
    if (nameLike != null && !nameLike.isEmpty()) {
      queryParams.putSingle("nameLike", nameLike);
    }
    DeploymentQueryDto queryDto = new DeploymentQueryDto(objectMapper, queryParams);

    DeploymentQuery query = queryDto.toQuery(processEngine);

    return query.count();
  }

  //@Override
  public Collection<Deployment> findDeployments(CIBUser user, String nameLike, int firstResult, int maxResults, String sortBy, String sortOrder) {
    ObjectMapper objectMapper = getObjectMapper();
    JacksonConfigurator.configureObjectMapper(objectMapper);
    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
    queryParams.putSingle("sortBy", sortBy);
    queryParams.putSingle("sortOrder", sortOrder);
    if (nameLike != null && !nameLike.isEmpty()) {
      queryParams.putSingle("nameLike", nameLike);
    }
    
    DeploymentQueryDto queryDto = new DeploymentQueryDto(objectMapper, queryParams);
    DeploymentQuery query = queryDto.toQuery(processEngine);
    List<org.cibseven.bpm.engine.repository.Deployment> matchingDeployments = QueryUtil.list(query, firstResult, maxResults);
    List<Deployment> deployments = new ArrayList<>();
    for (org.cibseven.bpm.engine.repository.Deployment deployment : matchingDeployments) {
      DeploymentDto def = DeploymentDto.fromDeployment(deployment);
      deployments.add(convertValue(objectMapper, def, Deployment.class));
    }
    return deployments;
  }
  
  //@Override
  public Deployment findDeployment(String deploymentId, CIBUser user) {
    org.cibseven.bpm.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

    if (deployment == null) {
      //TODO: exception type 
      throw new IllegalArgumentException("Deployment with id '" + deploymentId + "' does not exist");
    }

    ObjectMapper objectMapper = getObjectMapper();
    return convertValue(objectMapper, DeploymentDto.fromDeployment(deployment), Deployment.class);
  }

  //@Override
  public Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user) {
    
    List<Resource> resources = repositoryService.getDeploymentResources(deploymentId);

    ObjectMapper objectMapper = getObjectMapper();
    List<DeploymentResource> deploymentResources = new ArrayList<DeploymentResource>();
    for (Resource resource : resources) {
      deploymentResources.add(convertValue(objectMapper, DeploymentResourceDto.fromResources(resource), DeploymentResource.class));
    }

    if (!deploymentResources.isEmpty()) {
      return deploymentResources;
    }
    else {
      //TODO: exception type 
      throw new IllegalArgumentException("Deployment resources for deployment id '" + deploymentId + "' do not exist.");
    }
  }

  //@Override
  public Data fetchDataFromDeploymentResource(HttpServletRequest rq, String deploymentId, String resourceId, String fileName) {
    //TODO: not tested
    InputStream resourceAsStream = repositoryService.getResourceAsStreamById(deploymentId, resourceId);
    if (resourceAsStream != null) {
      DeploymentResourceDto resource = getDeploymentResource(resourceId, deploymentId);
      String name = resource.getName();
      String filename = null;
      String mediaType = null;

      if (name != null) {
        name = name.replace("\\", "/");
        String[] filenameParts = name.split("/");
        if (filenameParts.length > 0) {
          int idx = filenameParts.length-1;
          filename = filenameParts[idx];
        }

        String[] extensionParts = name.split("\\.");
        if (extensionParts.length > 0) {
          int idx = extensionParts.length-1;
          String extension = extensionParts[idx];
          if (extension != null) {
            mediaType = MEDIA_TYPE_MAPPING.get(extension);
          }
        }
      }

      if (filename == null) {
        filename = "data";
      }

      if (mediaType == null) {
        mediaType = MediaType.APPLICATION_OCTET_STREAM.toString();
      }

      
      try {
        byte[] body = resourceAsStream.readAllBytes();
        if (body == null)
          throw new NullPointerException();
        InputStream targetStream = new ByteArrayInputStream(body);
        InputStreamSource iso = new InputStreamResource(targetStream);
        Data returnValue = new Data(fileName, mediaType, iso, body.length);
        return returnValue;
      } catch (IOException e) {
        //TODO: exception type 
        throw new IllegalArgumentException("Deployment resource '" + resourceId + "' for deployment id '" + deploymentId + "'could not be read.");
      }
    } else {
      //TODO: exception type 
      throw new IllegalArgumentException("Deployment resource '" + resourceId + "' for deployment id '" + deploymentId + "' does not exist.");
    }
  }
  
  private DeploymentResourceDto getDeploymentResource(String resourceId, String deploymentId) {
    List<DeploymentResourceDto> deploymentResources = getDeploymentResources(deploymentId);
    for (DeploymentResourceDto deploymentResource : deploymentResources) {
      if (deploymentResource.getId().equals(resourceId)) {
        return deploymentResource;
      }
    }
  
    //TODO: exception type 
    throw new IllegalArgumentException("Deployment resource with resource id '" + resourceId + "' for deployment id '" + deploymentId + "' does not exist.");
  }
  
  private List<DeploymentResourceDto> getDeploymentResources(String deploymentId) {
    List<Resource> resources = processEngine.getRepositoryService().getDeploymentResources(deploymentId);

    List<DeploymentResourceDto> deploymentResources = new ArrayList<DeploymentResourceDto>();
    for (Resource resource : resources) {
      deploymentResources.add(DeploymentResourceDto.fromResources(resource));
    }

    if (!deploymentResources.isEmpty()) {
      return deploymentResources;
    }
    else {
      //TODO: exception type 
      throw new IllegalArgumentException("Deployment resources for deployment id '" + deploymentId + "' do not exist.");
    }
  }
  
  //@Override
  public void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException {
    org.cibseven.bpm.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
    if (deployment == null) {
      //TODO: exception type 
      throw new IllegalArgumentException("Deployment with id '" + deploymentId + "' do not exist");
    }

    //TODO: properties unused
    boolean skipCustomListeners = false;
    boolean skipIoMappings = false;

    repositoryService.deleteDeployment(deploymentId, cascade, skipCustomListeners, skipIoMappings);
  }
    /*
  
  █████   ██████ ████████ ██ ██    ██ ██ ████████ ██    ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
 ██   ██ ██         ██    ██ ██    ██ ██    ██     ██  ██      ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
 ███████ ██         ██    ██ ██    ██ ██    ██      ████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
 ██   ██ ██         ██    ██  ██  ██  ██    ██       ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
 ██   ██  ██████    ██    ██   ████   ██    ██       ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                            
  */
 
  //@Override
 public ActivityInstance findActivityInstance(String processInstanceId, CIBUser user) {
  org.cibseven.bpm.engine.runtime.ActivityInstance activityInstance = null;
  try {
     activityInstance = runtimeService.getActivityInstance(processInstanceId);
   } catch (AuthorizationException e) {
     throw e;
   } catch (ProcessEngineException e) {
     //TODO: exception type 
     throw new IllegalArgumentException(e.getMessage(), e);
   }

   if (activityInstance == null) {
     //TODO: exception type 
     throw new IllegalArgumentException("Process instance with id " + processInstanceId + " does not exist");
   }

   ActivityInstanceDto result = ActivityInstanceDto.fromActivityInstance(activityInstance);
   return convertValue(getObjectMapper(), result, ActivityInstance.class);
 }
 
 //@Override
 public List<ActivityInstanceHistory> findActivitiesInstancesHistory(Map<String, Object> queryParams, CIBUser user) {
   //TODO: to be implemented
   return null;
 }
 
 //@Override
 public List<ActivityInstanceHistory> findActivitiesInstancesHistory(String processInstanceId, CIBUser user) {
   ObjectMapper objectMapper = getObjectMapper();
   HistoricActivityInstanceQueryDto queryHistoricActivityInstanceDto = new HistoricActivityInstanceQueryDto();
   queryHistoricActivityInstanceDto.setObjectMapper(objectMapper);
   queryHistoricActivityInstanceDto.setProcessInstanceId(processInstanceId);
   HistoricActivityInstanceQuery query = queryHistoricActivityInstanceDto.toQuery(processEngine);
   List<HistoricActivityInstance> matchingHistoricActivityInstances = QueryUtil.list(query, null, null);

   List<ActivityInstanceHistory> historicActivityInstanceResults = new ArrayList<>();
   for (HistoricActivityInstance historicActivityInstance : matchingHistoricActivityInstances) {
     HistoricActivityInstanceDto resultHistoricActivityInstance = new HistoricActivityInstanceDto();
     HistoricActivityInstanceDto.fromHistoricActivityInstance(resultHistoricActivityInstance, historicActivityInstance);
     historicActivityInstanceResults
         .add(convertValue(objectMapper, resultHistoricActivityInstance, ActivityInstanceHistory.class));
   }
   return historicActivityInstanceResults;
 }
 
 //@Override
 public ActivityInstance findActivityInstances(String processInstanceId, CIBUser user) throws SystemException {
   //TODO: to be implemented
   return null;
 }
 
 //@Override
 public List<ActivityInstanceHistory> findActivityInstanceHistory(String processInstanceId, CIBUser user) throws SystemException {
   //TODO: to be implemented
   return null;
 } 

 //@Override
 public void deleteVariableByExecutionId(String executionId, String variableName, CIBUser user) {
   // TODO: in progress
   try {
     runtimeService.removeVariableLocal(executionId, variableName);
   } catch (AuthorizationException e) {
     throw e;
   } catch (ProcessEngineException e) {
     String errorMessage = String.format("Cannot delete %s variable %s: %s", executionId, variableName, e.getMessage());
     // TODO: exception type
     throw new IllegalArgumentException(errorMessage, e);
   }

 }

 //@Override
 public void deleteVariableHistoryInstance(String id, CIBUser user) {
   try {
     historyService.deleteHistoricVariableInstance(id);
   } catch (NotFoundException nfe) { // rewrite status code from bad request (400) to not found (404)
     //TODO: exception type 
     throw new IllegalArgumentException(nfe.getMessage(), nfe);
   }
 } 
  /*
  
  ██    ██ ████████ ██ ██      ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██    ██    ██    ██ ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██    ██    ██    ██ ██      ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██    ██    ██    ██ ██           ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
   ██████     ██    ██ ███████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                                              
   */
  //@Override
  public Collection<Message> correlateMessage(Map<String, Object> data, CIBUser user) throws SystemException {
   //TODO: to be implemented
   return null;
  }
  
  //@Override
  public String findStacktrace(String jobId, CIBUser user) {
   //TODO: to be implemented
   return null;
  }
  
  //@Override
  public String findExternalTaskErrorDetails(String externalTaskId, CIBUser user) {
  //untested
    try {
      return externalTaskService.getExternalTaskErrorDetails(externalTaskId);
    } catch (NotFoundException e) {
      //TODO: exception type
      throw new IllegalArgumentException("External task with id " + externalTaskId + " does not exist", e);
    }
  }
  
  //@Override
  public String findHistoricExternalTaskErrorDetails(String externalTaskId, CIBUser user) {
    try {
      return historyService.getHistoricExternalTaskLogErrorDetails(externalTaskId);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      //TODO: exception type
      throw new IllegalArgumentException(e.getMessage());
    }
  }
  
  //@Override
  public Collection<Incident> findHistoricIncidents(Map<String, Object> params, CIBUser user) {
    ObjectMapper objectMapper = getObjectMapper();
    HistoricIncidentQueryDto queryDto = objectMapper.convertValue(params, HistoricIncidentQueryDto.class);
    HistoricIncidentQuery query = queryDto.toQuery(processEngine);

    List<HistoricIncident> queryResult = QueryUtil.list(query, null, null);

    List<HistoricIncidentDto> historicIncidentDtos = new ArrayList<HistoricIncidentDto>();
    for (HistoricIncident historicIncident : queryResult) {
      HistoricIncidentDto dto = HistoricIncidentDto.fromHistoricIncident(historicIncident);
      historicIncidentDtos.add(dto);
    }

    List<Incident> incidents = new ArrayList<>();
    //TODO: enrichment is not tested
    // Enrich historic incidents with root cause incident data (same enrichment algorithm as current incidents)
    for (HistoricIncidentDto incidentDto : historicIncidentDtos) {
      Incident incident = convertValue(objectMapper, incidentDto, Incident.class);
      if (incidentDto.getId() != null && incidentDto.getRootCauseIncidentId() != null 
          && !incidentDto.getId().equals(incidentDto.getRootCauseIncidentId())) {
        try {
          // For historic incidents, try to fetch the root cause from historic incidents first, then from current incidents
          HistoricIncidentDto rootCauseIncident = fetchHistoricIncidentById(incidentDto.getRootCauseIncidentId(), user, objectMapper);
          if (rootCauseIncident != null) {
            // Map root cause incident data to the specific fields
            incident.setCauseIncidentProcessInstanceId(rootCauseIncident.getProcessInstanceId());
            incident.setCauseIncidentProcessDefinitionId(rootCauseIncident.getProcessDefinitionId());
            incident.setCauseIncidentActivityId(rootCauseIncident.getActivityId());
            incident.setCauseIncidentFailedActivityId(rootCauseIncident.getFailedActivityId());
            incident.setRootCauseIncidentProcessInstanceId(rootCauseIncident.getProcessInstanceId());
            incident.setRootCauseIncidentProcessDefinitionId(rootCauseIncident.getProcessDefinitionId());
            incident.setRootCauseIncidentActivityId(rootCauseIncident.getActivityId());
            incident.setRootCauseIncidentFailedActivityId(rootCauseIncident.getFailedActivityId());
            incident.setRootCauseIncidentConfiguration(rootCauseIncident.getConfiguration());
            incident.setRootCauseIncidentMessage(rootCauseIncident.getIncidentMessage());
          }
        } catch (Exception e) {
          log.warn("Failed to enrich historic incident with ID: {} and root cause ID: {}", 
            incident.getId(), 
            incident.getRootCauseIncidentId(), 
            e);
        }
      }
      incidents.add(incident);
    }
    return incidents;
  }
  
  private HistoricIncidentDto fetchHistoricIncidentById(String incidentId, CIBUser user, ObjectMapper objectMapper) {
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
  
  //@Override
  public String findHistoricStacktraceByJobId(String jobId, CIBUser user) {
   //TODO: to be implemented
   return null;
  }
  
  //@Override
  public void retryJobById(String jobId, Map<String, Object> data, CIBUser user) {
   //TODO: to be implemented
  }

  //@Override
  public void retryExternalTask(String externalTaskId, Map<String, Object> data, CIBUser user) {
   //TODO: to be implemented
  }

  //@Override
  public Collection<EventSubscription> getEventSubscriptions(Optional<String> processInstanceId,
      Optional<String> eventType, Optional<String> eventName, CIBUser user) {
   //TODO: to be implemented
   return null;
  } 
  
  /*
   /*

  ██    ██ ███████ ███████ ██████      ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██    ██ ██      ██      ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██    ██ ███████ █████   ██████      ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██    ██      ██ ██      ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
   ██████  ███████ ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                    
  */
  
  //@Override
  public Authorizations getUserAuthorization(String userId, CIBUser user) {
    //TODO: eliminate rest classes
    AuthorizationQueryDto queryDto = new AuthorizationQueryDto();
    queryDto.setUserIdIn(new String[]{userId});
    queryDto.setObjectMapper(getObjectMapper());
    AuthorizationQuery userQuery = queryDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.authorization.Authorization> userAuthorizationList = QueryUtil.list(userQuery, null, null);
    GroupQuery groupQuery = identityService.createGroupQuery();
    List<Group> userGroups = groupQuery.groupMember(userId)
        .orderByGroupName()
        .asc()
        .unlimitedList();

    Set<UserDto> allGroupUsers = new HashSet<>();
    List<GroupDto> allGroups = new ArrayList<>();//TODO: not used, yet

    List<String> listGroups = new ArrayList<>();
    for (Group group : userGroups) {
      List<org.cibseven.bpm.engine.identity.User> groupUsers = identityService.createUserQuery()
          .memberOfGroup(group.getId())
          .unlimitedList();

      for (org.cibseven.bpm.engine.identity.User groupUser : groupUsers) {
        if (!user.getId().equals(userId)) {
          allGroupUsers.add(new UserDto(groupUser.getId(), groupUser.getFirstName(), groupUser.getLastName()));
        }
      }
      allGroups.add(new GroupDto(group.getId(), group.getName()));
      listGroups.add(group.getId());
    }

    AuthorizationQueryDto groupIdQueryDto = new AuthorizationQueryDto();
    groupIdQueryDto.setGroupIdIn(listGroups.toArray(new String[0]));
    groupIdQueryDto.setObjectMapper(getObjectMapper());
    AuthorizationQuery groupIdQuery = groupIdQueryDto.toQuery(processEngine);
    //expected: 51 authorizations with id, type, userid, groupId, resourceType, resourceId
    List<org.cibseven.bpm.engine.authorization.Authorization> groupIdResultList = QueryUtil.list(groupIdQuery, null, null);
    Collection<Authorization> groupsAuthorizations = createAuthorizationCollection(groupIdResultList);

    AuthorizationQueryDto globalIdQueryDto = new AuthorizationQueryDto();
    globalIdQueryDto.setType(0);
    globalIdQueryDto.setObjectMapper(getObjectMapper());
    AuthorizationQuery globalIdQuery = globalIdQueryDto.toQuery(processEngine);
    List<org.cibseven.bpm.engine.authorization.Authorization> globalIdResultList = QueryUtil.list(globalIdQuery, null, null);
    Collection<Authorization> globalAuthorizations = createAuthorizationCollection(globalIdResultList);
    
    
    Authorizations auths = new Authorizations();
    Collection<Authorization> userAuthorizations = createAuthorizationCollection(userAuthorizationList);
    userAuthorizations.addAll(groupsAuthorizations);
    userAuthorizations.addAll(globalAuthorizations);
    
    auths.setApplication(filterResources(userAuthorizations, resourceType(SevenResourceType.APPLICATION)));
    auths.setFilter(filterResources(userAuthorizations, resourceType(SevenResourceType.FILTER)));
    auths.setProcessDefinition(filterResources(userAuthorizations, resourceType(SevenResourceType.PROCESS_DEFINITION)));
    auths.setProcessInstance(filterResources(userAuthorizations, resourceType(SevenResourceType.PROCESS_INSTANCE)));
    auths.setTask(filterResources(userAuthorizations, resourceType(SevenResourceType.TASK)));
    auths.setAuthorization(filterResources(userAuthorizations, resourceType(SevenResourceType.AUTHORIZATION)));
    auths.setUser(filterResources(userAuthorizations, resourceType(SevenResourceType.USER)));
    auths.setGroup(filterResources(userAuthorizations, resourceType(SevenResourceType.GROUP)));
    auths.setDecisionDefinition(filterResources(userAuthorizations, resourceType(SevenResourceType.DECISION_DEFINITION)));
    auths.setDecisionRequirementsDefinition(filterResources(userAuthorizations, resourceType(SevenResourceType.DECISION_REQUIREMENTS_DEFINITION)));
    auths.setDeployment(filterResources(userAuthorizations, resourceType(SevenResourceType.DEPLOYMENT)));
    //auths.setCaseDefinition(filterResources(userAuthorizations, resourceType(SevenResourceType.CASE_DEFINITION)));
    //auths.setCaseInstance(filterResources(userAuthorizations, resourceType(SevenResourceType.CASE_INSTANCE)));
    //auths.setJobDefinition(filterResources(userAuthorizations, resourceType(SevenResourceType.JOB_DEFINITION)));
    auths.setBatch(filterResources(userAuthorizations, resourceType(SevenResourceType.BATCH)));
    auths.setGroupMembership(filterResources(userAuthorizations, resourceType(SevenResourceType.GROUP_MEMBERSHIP)));
    auths.setHistoricTask(filterResources(userAuthorizations, resourceType(SevenResourceType.HISTORIC_TASK)));
    auths.setHistoricProcessInstance(filterResources(userAuthorizations, resourceType(SevenResourceType.HISTORIC_PROCESS_INSTANCE)));
    auths.setTenant(filterResources(userAuthorizations, resourceType(SevenResourceType.TENANT)));
    auths.setTenantMembership(filterResources(userAuthorizations, resourceType(SevenResourceType.TENANT_MEMBERSHIP)));
    auths.setReport(filterResources(userAuthorizations, resourceType(SevenResourceType.REPORT)));
    auths.setDashboard(filterResources(userAuthorizations, resourceType(SevenResourceType.DASHBOARD)));
    auths.setUserOperationLogCategory(filterResources(userAuthorizations, resourceType(SevenResourceType.USER_OPERATION_LOG_CATEGORY)));
    auths.setSystem(filterResources(userAuthorizations, resourceType(SevenResourceType.SYSTEM)));
    //auths.setMessage(filterResources(userAuthorizations, resourceType(SevenResourceType.MESSAGE)));
    //auths.setEventSubscription(filterResources(userAuthorizations, resourceType(SevenResourceType.EVENT_SUBSCRIPTION)));
    
    return auths;
  }
  
  //TODO: this method will be obsolete once the base class is correctly set
  private Collection<Authorization> filterResources(Collection<Authorization> authorizations, int resourceType) {
    Set<Integer> resourceFilter = Arrays.asList(resourceType).stream().collect(Collectors.toSet());
    return authorizations.stream().filter(authorization -> resourceFilter.contains(authorization.getResourceType())).collect(Collectors.toList());
  }
  
  
  private Collection<Authorization> createAuthorizationCollection(List<org.cibseven.bpm.engine.authorization.Authorization> userAuthorizationList) {
    Collection<Authorization> resultAuthorization = new ArrayList<>();
    for (org.cibseven.bpm.engine.authorization.Authorization userAuthorization : userAuthorizationList) {
      resultAuthorization.add(createAuthorization(userAuthorization));
    }
    return resultAuthorization;
  }

  private Authorization createAuthorization(org.cibseven.bpm.engine.authorization.Authorization userAuthorization) {
    Authorization newUserAuthorization = new Authorization();
    newUserAuthorization.setGroupId(userAuthorization.getGroupId());
    newUserAuthorization.setId(userAuthorization.getId());
    newUserAuthorization.setPermissions(PermissionConverter.getNamesForPermissions( userAuthorization, userAuthorization.getPermissions(Permissions.values())));
    newUserAuthorization.setResourceId(userAuthorization.getResourceId());
    newUserAuthorization.setResourceType(userAuthorization.getResourceType());
    newUserAuthorization.setType(userAuthorization.getAuthorizationType());
    newUserAuthorization.setUserId(userAuthorization.getUserId());
    return newUserAuthorization;
  }
  
  public Collection<SevenUser> fetchUsers(CIBUser user) throws SystemException {
    UserQueryDto queryDto = new UserQueryDto();
    queryDto.setObjectMapper(getObjectMapper());
    UserQuery query = queryDto.toQuery(processEngine);
    query.userId(user.getId());
    List<org.cibseven.bpm.engine.identity.User> resultList = QueryUtil.list(query, null, null);

    Collection<SevenUser> userCollection = createSevenUsers(resultList); 
    return userCollection;
  }
  
  public SevenVerifyUser verifyUser(String username, String password, CIBUser user) throws SystemException {
    if ((username == null || username.isBlank())|| (password == null || password.isBlank()))
      //TODO: exception type
      throw new IllegalArgumentException("Username and password are required");
    SevenVerifyUser verifyUser = new SevenVerifyUser();
    boolean valid = identityService.checkPassword(username, password);
    verifyUser.setAuthenticated(valid);
    verifyUser.setAuthenticatedUser(username);
    return verifyUser;
  }
  
  //@Override
  public Collection<User> findUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike, Optional<String> lastName, Optional<String> lastNameLike,
      Optional<String> email, Optional<String> emailLike, Optional<String> memberOfGroup, Optional<String> memberOfTenant, Optional<String> idIn, 
      Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder, CIBUser user) {
    //TODO: does not work with ldap/adfsUserProvider
    //
    
    if (firstNameLike.isPresent()) { // javier, JAVIER, Javier
      Collection<User> lowerCaseResult = getUsers(id, firstName, 
          Optional.of(firstNameLike.get().toLowerCase()), lastName, lastNameLike, email, emailLike, memberOfGroup, 
          memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
      Collection<User> upperCaseResult = getUsers(id, firstName, 
          Optional.of(firstNameLike.get().toUpperCase()), lastName, lastNameLike, email, emailLike, memberOfGroup, 
          memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
      Collection<User> normalCaseResult = getUsers(id, firstName, Optional.of(firstNameLike.get().substring(0, 2).toUpperCase() + firstNameLike.get().substring(2).toLowerCase()), lastName, lastNameLike, email, emailLike, memberOfGroup, 
          memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
        
      Collection<User> res = new ArrayList<User>();
      res.addAll(lowerCaseResult);
      res.addAll(upperCaseResult);
      res.addAll(normalCaseResult);
      
      return res;
    }
    
    if (lastNameLike.isPresent()) { // javier, JAVIER, Javier
      Collection<User> lowerCaseResult = getUsers(id, firstName, 
          firstNameLike, lastName, Optional.of(lastNameLike.get().toLowerCase()), email, emailLike, memberOfGroup, 
          memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
      Collection<User> upperCaseResult = getUsers(id, firstName, 
          firstNameLike, lastName, Optional.of(lastNameLike.get().toLowerCase()), email, emailLike, memberOfGroup, 
          memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
      Collection<User> normalCaseResult = getUsers(id, firstName, firstNameLike, lastName, Optional.of(lastNameLike.get().substring(0, 2).toUpperCase() + lastNameLike.get().substring(2).toLowerCase()), email, emailLike, memberOfGroup, 
          memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
        
      Collection<User> res = new ArrayList<User>();
      res.addAll(lowerCaseResult);
      res.addAll(upperCaseResult);
      res.addAll(normalCaseResult);
      
      return res;
    }
    
    return getUsers(id, firstName, firstNameLike, lastName, lastNameLike, email, emailLike, memberOfGroup, 
        memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
  }
  
  private Collection<User> getUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike,
      Optional<String> lastName, Optional<String> lastNameLike, Optional<String> email, Optional<String> emailLike,
      Optional<String> memberOfGroup, Optional<String> memberOfTenant, Optional<String> idIn,
      Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder) {

    //TODO: Wildcard is always set to "%"
    final String wcard = "%";
    UserQueryDto queryDto = new UserQueryDto();
    queryDto.setObjectMapper(getObjectMapper());
    UserQuery query = queryDto.toQuery(processEngine);
    if (memberOfGroup.isPresent())
       query.memberOfGroup(memberOfGroup.get());
    if (memberOfTenant.isPresent())
      query.memberOfTenant(memberOfTenant.get());
    //TODO: there is protected void query.applySortBy(UserQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine)
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
    //query.userIdIn(null);
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

  private Collection<User> createUsers(List<org.cibseven.bpm.engine.identity.User> resultList) {
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

  private Collection<SevenUser> createSevenUsers(List<org.cibseven.bpm.engine.identity.User> resultList) {
    Collection<SevenUser> users = new ArrayList<>();
    for (org.cibseven.bpm.engine.identity.User resultUser : resultList) {
      users.add(createSevenUser(resultUser));
    }
    return users;
  }

  private SevenUser createSevenUser(org.cibseven.bpm.engine.identity.User engineUser) {
    SevenUser user = new SevenUser();
    user.setEmail(engineUser.getEmail());
    user.setFirstName(engineUser.getFirstName());
    user.setId(engineUser.getId());
    user.setLastName(engineUser.getPassword());
    return user;
  }

  //@Override
  public void createUser(NewUser user, CIBUser flowUser) throws InvalidUserIdException {
    User profile = user.getProfile();
    org.cibseven.bpm.engine.identity.User newUser = identityService.newUser(profile.getId());
    newUser.setId(profile.getId());
    newUser.setFirstName(profile.getFirstName());
    newUser.setLastName(profile.getLastName());
    newUser.setEmail(profile.getEmail());
    newUser.setPassword(user.getCredentials().getPassword());
    identityService.saveUser(newUser);
  }
  
  //@Override
  public void updateUserProfile(String userId, User user, CIBUser flowUser) {
    if(identityService.isReadOnly()) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity service implementation is read-only.");
    }

    org.cibseven.bpm.engine.identity.User dbUser = findUserObject(user.getId());
    if(dbUser == null) {
      //TODO: exception type
      throw new IllegalArgumentException("User with id " + user.getId() + " does not exist");
    }

    dbUser.setId(user.getId());
    dbUser.setFirstName(user.getFirstName());
    dbUser.setLastName(user.getLastName());
    dbUser.setEmail(user.getEmail());
    identityService.saveUser(dbUser);
  }
  
  private org.cibseven.bpm.engine.identity.User findUserObject(String id) {
    org.cibseven.bpm.engine.identity.User dbUser = null;
    try {
      List<org.cibseven.bpm.engine.identity.User> users = identityService.createUserQuery().userId(id).list();

      if (users.size() == 1) {
        dbUser = users.get(0);
      } else if (!users.isEmpty()) {

        dbUser = users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);

        if (dbUser == null) {
          dbUser = users.get(0);
        }
      }
    } catch (ProcessEngineException e) {
      // TODO: exception type
      throw new IllegalArgumentException("Exception while performing user query: " + e.getMessage());
    }
    return dbUser;
  }

  private Group findGroupObject(String groupId) {
    try {
      return identityService.createGroupQuery()
          .groupId(groupId)
          .singleResult();
    } catch(ProcessEngineException e) {
      //TODO: exception type
      throw new IllegalArgumentException("Exception while performing group query: "+ e.getMessage());
    }
  }
  
  //TODO: not tested, UI seems to have no function to change password without sending mails before
  //@Override
  public void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user) {
    if(identityService.isReadOnly()) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity service implementation is read-only.");
    }
    Authentication currentAuthentication = identityService.getCurrentAuthentication();
    if(currentAuthentication != null && currentAuthentication.getUserId() != null) {
      if(!identityService.checkPassword(currentAuthentication.getUserId(), (String)data.get("authenticatedUserPassword"))) {
        //TODO: exception type
        throw new IllegalArgumentException("The given authenticated user password is not valid.");
      }
    }

    org.cibseven.bpm.engine.identity.User dbUser = findUserObject(userId);
    if(dbUser == null) {
      //TODO: exception type
      throw new IllegalArgumentException("User with id " + user.getId() + " does not exist");
    }

    dbUser.setPassword((String)data.get("password"));
    identityService.saveUser(dbUser);
  }
  
  //@Override
  public void addMemberToGroup(String groupId, String userId, CIBUser user) {
    if(identityService.isReadOnly()) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity service implementation is read-only.");
    }
    identityService.createMembership(userId, groupId);
  }
  
  //@Override
  public void deleteMemberFromGroup(String groupId, String userId, CIBUser user) {
    if(identityService.isReadOnly()) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity service implementation is read-only.");
    }
    identityService.deleteMembership(userId, groupId);
  }

  //@Override
  public void deleteUser(String userId, CIBUser user) {
    identityService.deleteUser(userId);
  }
  
  //@Override
  public SevenUser getUserProfile(String userId, CIBUser user) {
    List<org.cibseven.bpm.engine.identity.User> users = identityService
        .createUserQuery()
        .userId(userId)
        .list();
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

  //@Override
  public Collection<UserGroup> findGroups(Optional<String> id, Optional<String> name, Optional<String> nameLike, Optional<String> type,
      Optional<String> member, Optional<String> memberOfTenant, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
      Optional<String> maxResults, CIBUser user) {
    //TODO: Wildcard is always set to "%"
    final String wcard = "%";
    GroupQueryDto queryDto = new GroupQueryDto();
    queryDto.setObjectMapper(getObjectMapper());
    //set parameters
    if (id.isPresent())
        queryDto.setId(id.get());
    if (name.isPresent())
      queryDto.setName(name.get());
    if (nameLike.isPresent())
      queryDto.setNameLike(nameLike.get().replace("*", wcard));;
    if (type.isPresent())
      queryDto.setType(type.get());
    if (member.isPresent())
      queryDto.setMember(member.get());
    if (memberOfTenant.isPresent())
      queryDto.setMemberOfTenant(memberOfTenant.get());
    if (sortBy.isPresent())//TODO: value unknown
      queryDto.setSortBy(sortBy.get());
    if (sortOrder.isPresent())//TODO: value unknown
      queryDto.setSortOrder( sortOrder.get().equals("asc")? "asc" : "desc");
    GroupQuery query = queryDto.toQuery(processEngine);

    Integer first = firstResult.isPresent() ? Integer.parseInt(firstResult.get()) : null;
    Integer max = maxResults.isPresent() ? Integer.parseInt(maxResults.get()) : null;
    List<Group> resultList = QueryUtil.list(query, first, max);

    Collection<UserGroup> userGroups = createUserGroups(resultList); 
    return userGroups;
  }

  private Collection<UserGroup> createUserGroups(List<Group> resultList) {
    Collection<UserGroup> userGroups = new ArrayList<>();
    for (Group group : resultList) {
      userGroups.add(createUserGroup(group));
    }
    return userGroups;
  }

  private UserGroup createUserGroup(Group group) {
    UserGroup userGroup = new UserGroup();
    userGroup.setId(group.getId());
    userGroup.setName(group.getName());
    userGroup.setType(group.getType());
    return userGroup;
  }

  //@Override
  public void createGroup(UserGroup group, CIBUser user) {
    if(identityService.isReadOnly()) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity service implementation is read-only.");
    }
    Group newGroup = identityService.newGroup(group.getId());
    newGroup.setId(group.getId()); 
    newGroup.setName(group.getName());
    newGroup.setType(group.getType());
    identityService.saveGroup(newGroup);
  }

  //@Override
  public void updateGroup(String groupId, UserGroup group, CIBUser user) {
    if(identityService.isReadOnly()) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity service implementation is read-only.");
    }

    Group dbGroup = findGroupObject(groupId);
    if(dbGroup == null) {
      //TODO: exception type
      throw new IllegalArgumentException("Group with id " + groupId + " does not exist");
    }

    dbGroup.setId(group.getId());
    dbGroup.setName(group.getName());
    dbGroup.setType(group.getType());

    identityService.saveGroup(dbGroup);
  }

  //@Override
  public void deleteGroup(String groupId, CIBUser user) {
    if(identityService.isReadOnly()) {
      //TODO: exception type
      throw new IllegalArgumentException("Identity service implementation is read-only.");
    }
    identityService.deleteGroup(groupId);
  }

  //@Override
  public Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type, Optional<String> userIdIn, Optional<String> groupIdIn,
      Optional<String> resourceType, Optional<String> resourceId, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
      Optional<String> maxResults, CIBUser user) {
    //TODO: to be implemented
    return null;
  }

  //@Override
  public ResponseEntity<Authorization> createAuthorization(Authorization authorization, CIBUser user) {
    
    //TODO: resulting permissions always set to ALL + selection permissions, the same happens in SevenProvider!
    // parameters contain ALL + selected values
    org.cibseven.bpm.engine.authorization.Authorization newAuthorization = authorizationService.createNewAuthorization(authorization.getType());
    //AuthorizationCreateDto authorizationCreateDto = new AuthorizationCreateDto();
    newAuthorization.setGroupId(authorization.getGroupId());
    newAuthorization.setUserId(authorization.getUserId());
    newAuthorization.setResourceType(authorization.getResourceType());
    newAuthorization.setResourceId(authorization.getResourceId());
    newAuthorization.setPermissions(PermissionConverter.getPermissionsForNames(authorization.getPermissions(), authorization.getResourceType(), processEngineConfiguration));

    newAuthorization = authorizationService.saveAuthorization(newAuthorization);

    Authorization resultAuthorization = new Authorization();
    resultAuthorization.setGroupId(newAuthorization.getGroupId());
    resultAuthorization.setId(newAuthorization.getId());
    resultAuthorization.setPermissions(PermissionConverter.getNamesForPermissions( newAuthorization, newAuthorization.getPermissions(Permissions.values())));
    resultAuthorization.setResourceId(newAuthorization.getResourceId());
    resultAuthorization.setResourceType(newAuthorization.getResourceType());
    resultAuthorization.setType(newAuthorization.getAuthorizationType());
    resultAuthorization.setUserId(newAuthorization.getUserId());
    return new ResponseEntity<Authorization>(resultAuthorization, HttpStatusCode.valueOf(200));
  }

  //@Override
  public void updateAuthorization(String authorizationId, Map<String, Object> data, CIBUser user) {
    org.cibseven.bpm.engine.authorization.Authorization dbAuthorization = authorizationService
        .createAuthorizationQuery().authorizationId(authorizationId).singleResult();

    if (dbAuthorization == null) {
      // TODO: exception type
      throw new IllegalArgumentException("Authorization with id " + authorizationId + " does not exist.");
    }
    AuthorizationDto authorizationDto = new AuthorizationDto();
    if (data.containsKey("groupId"))
      authorizationDto.setGroupId((String) data.get("groupId"));
    if (data.containsKey("permissions")) {
      List<String> permissionList = (List<String>) data.get("permissions");
      authorizationDto.setPermissions(permissionList.toArray(new String[0]));
    }
    if (data.containsKey("resourceId"))
      authorizationDto.setResourceId((String) data.get("resourceId"));
    if (data.containsKey("resourceType"))
      authorizationDto.setResourceType((Integer) data.get("resourceType"));
    if (data.containsKey("type"))
      authorizationDto.setType((Integer) data.get("type"));
    if (data.containsKey("userId"))
      authorizationDto.setUserId((String) data.get("userId"));
    AuthorizationDto.update(authorizationDto, dbAuthorization, processEngineConfiguration);
    // save
    authorizationService.saveAuthorization(dbAuthorization);
  }

  //@Override
  public void deleteAuthorization(String authorizationId, CIBUser user) {
    authorizationService.deleteAuthorization(authorizationId);
  }

  private org.cibseven.bpm.engine.task.Task getTaskById(String taskId) {
    org.cibseven.bpm.engine.task.Task foundTask = taskService.createTaskQuery().taskId(taskId).initializeFormKeys().singleResult();
    //TODO: any use of 'withCommentAttachmentInfo()'
    if (foundTask == null) {
      //TODO: check exception type
      throw new IllegalArgumentException("No matching task with id " + taskId);
    }
    return foundTask;
  }
  
  private <V extends Object> V runWithoutAuthorization(Supplier<V> action) {
    Authentication currentAuthentication = identityService.getCurrentAuthentication();
    try {
      identityService.clearAuthentication();
      return action.get();
    } catch (Exception e) {
      throw e;
    } finally {
      identityService.setAuthentication(currentAuthentication);
    }
  }
  
  /*
  
  ██ ███    ██  ██████ ██ ██████  ███████ ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██ ████   ██ ██      ██ ██   ██ ██      ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██ ██ ██  ██ ██      ██ ██   ██ █████   ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██ ██  ██ ██ ██      ██ ██   ██ ██      ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ██ ██   ████  ██████ ██ ██████  ███████ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                              
   */
  
  //@Override
  public Long countIncident(Map<String, Object> params, CIBUser user) {
    //TODO: to be implemented
    return null;
  }

  //@Override
  public Collection<Incident> findIncident(Map<String, Object> params, CIBUser user) {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user) {
    //TODO:  requires testing
    return fetchIncidents(null, null, user, processInstanceId);
  }

  //@Override
  public Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user) {
    //TODO:  requires testing
    return fetchIncidents(processDefinitionKey, null, user, null);
  }
  
  //@Override
  public Collection<Incident> fetchIncidentsByInstanceAndActivityId(String processDefinitionKey, 
    String activityId, CIBUser user) {
    //TODO: in progress
    return fetchIncidents(processDefinitionKey, activityId, user, null);
  }
  
  private List<Incident> fetchIncidents(String processDefinitionKey, 
      String activityId, CIBUser user, String processInstanceId) {
    ObjectMapper objectMapper = getObjectMapper();
    IncidentQueryDto queryDto = new IncidentQueryDto();
    queryDto.setActivityId(activityId);
    if (processDefinitionKey != null)
      queryDto.setProcessDefinitionKeyIn(new String[] {processDefinitionKey});
    queryDto.setProcessInstanceId(processInstanceId);;
    IncidentQuery query = queryDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.runtime.Incident> queryResult = QueryUtil.list(query, null, null);

    List<Incident> result = new ArrayList<>();
    for (org.cibseven.bpm.engine.runtime.Incident incident : queryResult) {
      IncidentDto dto = IncidentDto.fromIncident(incident);
      result.add(convertValue(objectMapper, dto, Incident.class));
    }

    return result;
  
  }
  
  //@Override
  public void setIncidentAnnotation(String incidentId, Map<String, Object> data, CIBUser user) {
    //TODO: to be implemented
  }

  /*
  
  ██    ██  █████  ██████  ██  █████  ██████  ██      ███████     ██ ███    ██ ███████ ████████  █████  ███    ██  ██████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██    ██ ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ████   ██ ██         ██    ██   ██ ████   ██ ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██    ██ ███████ ██████  ██ ███████ ██████  ██      █████       ██ ██ ██  ██ ███████    ██    ███████ ██ ██  ██ ██      █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
   ██  ██  ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ██  ██ ██      ██    ██    ██   ██ ██  ██ ██ ██      ██          ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
    ████   ██   ██ ██   ██ ██ ██   ██ ██████  ███████ ███████     ██ ██   ████ ███████    ██    ██   ██ ██   ████  ██████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                                                   
  */

  // @Override
  public VariableInstance getVariableInstance(String id, boolean deserializeValue, CIBUser user)
      throws SystemException, NoObjectFoundException {
    VariableInstance variableDeserialized = getVariableInstanceImpl(id, true, user);
    VariableInstance variableSerialized = getVariableInstanceImpl(id, false, user);
    if (variableDeserialized == null || variableSerialized == null)
      throw new SystemException("Variable not found: " + id);

    if (deserializeValue) {
      variableDeserialized.setValueSerialized(variableSerialized.getValue());
      variableDeserialized.setValueDeserialized(variableDeserialized.getValue());
      return variableDeserialized;
    } else {
      variableSerialized.setValueSerialized(variableSerialized.getValue());
      variableSerialized.setValueDeserialized(variableDeserialized.getValue());
      return variableSerialized;
    }
    //result: VariableInstance(id=21e2d1fd-72b7-11f0-b970-4ce1734f67af, name=amount, processDefinitionId=ReviewInvoice:1:1e944334-72b7-11f0-b970-4ce1734f67af, processInstanceId=21e283dc-72b7-11f0-b970-4ce1734f67af, executionId=21e283dc-72b7-11f0-b970-4ce1734f67af, caseInstanceId=null, caseExecutionId=null, taskId=null, batchId=null, activityInstanceId=21e283dc-72b7-11f0-b970-4ce1734f67af, tenantId=null, errorMessage=null, value=10.99, valueSerialized=10.99, valueDeserialized=10.99, type=Double, valueInfo={})
  }

  private VariableInstance getVariableInstanceImpl(String id, boolean deserializeValue, CIBUser user)
      throws SystemException, NoObjectFoundException {
    VariableInstanceQuery variableInstanceQuery = runtimeService.createVariableInstanceQuery().variableId(id);
    // do not fetch byte arrays
    variableInstanceQuery.disableBinaryFetching();

    if (!deserializeValue) {
      variableInstanceQuery.disableCustomObjectDeserialization();
    }
    org.cibseven.bpm.engine.runtime.VariableInstance variableEngineInstance = variableInstanceQuery.singleResult();
    if (variableEngineInstance != null) {
      VariableInstance variableInstance = convertValue(getObjectMapper(), variableEngineInstance, VariableInstance.class);
        // return transformToDto(variableInstance);
        return variableInstance;
    } else {
      // TODO: exception type
      throw new IllegalArgumentException("Variable with Id '" + id + "' does not exist.");
    }
  }
  
//  private static String toRestApiTypeName(String name) {
//    return name.substring(0, 1).toUpperCase() + name.substring(1);
//  }

  /*
  
  ██   ██ ██ ███████ ████████  ██████  ██████  ██  ██████     ██    ██  █████  ██████  ██  █████  ██████  ██      ███████     ██ ███    ██ ███████ ████████  █████  ███    ██  ██████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██   ██ ██ ██         ██    ██    ██ ██   ██ ██ ██          ██    ██ ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ████   ██ ██         ██    ██   ██ ████   ██ ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ███████ ██ ███████    ██    ██    ██ ██████  ██ ██          ██    ██ ███████ ██████  ██ ███████ ██████  ██      █████       ██ ██ ██  ██ ███████    ██    ███████ ██ ██  ██ ██      █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██   ██ ██      ██    ██    ██    ██ ██   ██ ██ ██           ██  ██  ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ██  ██ ██      ██    ██    ██   ██ ██  ██ ██ ██      ██          ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ██   ██ ██ ███████    ██     ██████  ██   ██ ██  ██████       ████   ██   ██ ██   ██ ██ ██   ██ ██████  ███████ ███████     ██ ██   ████ ███████    ██    ██   ██ ██   ████  ██████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                                                                                                             
   */
  //@Override
  public VariableHistory getHistoricVariableInstance(String id, boolean deserializeValue, CIBUser user)
      throws SystemException, NoObjectFoundException {
    VariableHistory variableSerialized = getHistoricVariableInstanceImpl(id, false, user);
    VariableHistory variableDeserialized = getHistoricVariableInstanceImpl(id, true, user);

    if (deserializeValue) {
      variableDeserialized.setValueSerialized(variableSerialized.getValue());
      variableDeserialized.setValueDeserialized(variableDeserialized.getValue());
      return variableDeserialized;
    } else {
      variableSerialized.setValueSerialized(variableSerialized.getValue());
      variableSerialized.setValueDeserialized(variableDeserialized.getValue());
      return variableSerialized;
    }
  }
  
  private VariableHistory getHistoricVariableInstanceImpl(String id, boolean deserializeValue, CIBUser user) {
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery().variableId(id);
    if (!deserializeValue) {
      query.disableCustomObjectDeserialization();
    }
    HistoricVariableInstance variableInstance = query.singleResult();
    if (variableInstance != null) {
      VariableHistory result = convertValue(getObjectMapper(),
          HistoricVariableInstanceDto.fromHistoricVariableInstance(variableInstance), VariableHistory.class);
      return result;
    } else {
      // TODO: exception type
      throw new IllegalArgumentException(" historic variable with Id '" + id + "' does not exist.");
    }
  }
  
  /*

  ███████ ██   ██ ████████ ███████ ██████  ███    ██  █████  ██           ████████  █████  ███████ ██   ██     ██       ██████   ██████  
  ██       ██ ██     ██    ██      ██   ██ ████   ██ ██   ██ ██              ██    ██   ██ ██      ██  ██      ██      ██    ██ ██       
  █████     ███      ██    █████   ██████  ██ ██  ██ ███████ ██              ██    ███████ ███████ █████       ██      ██    ██ ██   ███ 
  ██       ██ ██     ██    ██      ██   ██ ██  ██ ██ ██   ██ ██              ██    ██   ██      ██ ██  ██      ██      ██    ██ ██    ██ 
  ███████ ██   ██    ██    ███████ ██   ██ ██   ████ ██   ██ ███████         ██    ██   ██ ███████ ██   ██     ███████  ██████   ██████  
                                                                                                                                              
  */

  //@Override
  public Collection<ExternalTask> getExternalTasks(Map<String, Object> queryParams, CIBUser user)
      throws SystemException {
    // TODO: in progress
    ObjectMapper objectMapper = getObjectMapper();
    ExternalTaskQueryDto queryDto = objectMapper.convertValue(queryParams, ExternalTaskQueryDto.class);
    queryDto.setObjectMapper(objectMapper);
    ExternalTaskQuery query = queryDto.toQuery(processEngine);
    List<org.cibseven.bpm.engine.externaltask.ExternalTask> matchingTasks = QueryUtil.list(query, null, null);

    List<ExternalTask> taskResults = new ArrayList<>();
    for (org.cibseven.bpm.engine.externaltask.ExternalTask task : matchingTasks) {
      ExternalTaskDto resultInstance = ExternalTaskDto.fromExternalTask(task);
      taskResults.add(convertValue(objectMapper, resultInstance, ExternalTask.class));
    }
    return taskResults;
  }    

/*
  
  ██    ██  █████  ██████  ██  █████  ██████  ██      ███████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██    ██ ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██    ██ ███████ ██████  ██ ███████ ██████  ██      █████   ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
   ██  ██  ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██           ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
    ████   ██   ██ ██   ██ ██ ██   ██ ██████  ███████ ███████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                       
  */
  
  //@Override
  public void modifyVariableByExecutionId(String executionId, Map<String, Object> data, CIBUser user) throws SystemException {
    //TODO: to be implemented
/**
 * data contains modifications as HashMap which contains at least one value of type HashMap 
 * containing value, type [String] and valueInfo [HashMap]. 
 */
    
      ((RuntimeServiceImpl)runtimeService).updateVariablesLocal(executionId, data, null);
  }
  
  //@Override
  public void modifyVariableDataByExecutionId(String executionId, String variableName, MultipartFile data, 
      String valueType, CIBUser user) throws SystemException {
    //TODO: in progress - binary/file part is working correctly
    try {
      if (valueType.equalsIgnoreCase("File") || valueType.equalsIgnoreCase("Bytes")) {
        // Handle binary/file data
        String valueTypeName = DEFAULT_BINARY_VALUE_TYPE;
        valueTypeName = valueType;

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
        try {
          TypedValue typedValue = valueDto.toTypedValue(processEngine, objectMapper);//creates FileValueImpl
          runtimeService.setVariable(executionId, variableName, typedValue);
        } catch (AuthorizationException e) {
          throw e;
        } catch (ProcessEngineException e) {
          String errorMessage = String.format("Cannot put %s variable %s: %s", executionId, variableName, e.getMessage());
          throw new SystemException(errorMessage, e);
        }
      } else {
        // Handle JSON/serialized data
          Object object = null;

          if(data.getContentType() !=null
              && data.getContentType().toLowerCase().contains(MediaType.APPLICATION_JSON.toString())) {
            object = deserializeJsonObject(valueType, data.getBytes());

          } else {
            throw new SystemException("Unrecognized content type for serialized java type: " + data.getContentType());
          }

          if(object != null) {
            runtimeService.setVariable(executionId, variableName, Variables.objectValue(object).create());
          }
      }
    } catch (IOException e) { // from data.getBytes()
      throw new UnsupportedTypeException(e);
    }
  }

  private Object deserializeJsonObject(String className, byte[] data) {
    try {
      JavaType type = TypeFactory.defaultInstance().constructFromCanonical(className);
      validateType(type);
      return objectMapper.readValue(new String(data, Charset.forName("UTF-8")), type);
    } catch(Exception e) {
      throw new InvalidRequestException(Status.INTERNAL_SERVER_ERROR, "Could not deserialize JSON object: "+e.getMessage());
    }
  }

  /**
   * Validate the type with the help of the validator in the engine.<br>
   * Note: when adjusting this method, please also consider adjusting
   * the {@code JacksonJsonDataFormatMapper#validateType} in the Engine Spin Plugin
   */
  private void validateType(JavaType type) {
    if (processEngineConfiguration.isDeserializationTypeValidationEnabled()) {
      DeserializationTypeValidator validator = processEngineConfiguration.getDeserializationTypeValidator();
      if (validator != null) {
        List<String> invalidTypes = new ArrayList<>();
        validateType(type, validator, invalidTypes);
        if (!invalidTypes.isEmpty()) {
          throw new IllegalArgumentException("The following classes are not whitelisted for deserialization: " + invalidTypes);
        }
      }
    }
  }

  private void validateType(JavaType type, DeserializationTypeValidator validator, List<String> invalidTypes) {
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
  
  private void validateTypeInternal(JavaType type, DeserializationTypeValidator validator, List<String> invalidTypes) {
    String className = type.getRawClass().getName();
    if (!validator.validate(className) && !invalidTypes.contains(className)) {
      invalidTypes.add(className);
    }
  }

  //@Override
  public Collection<Variable> fetchProcessInstanceVariables(String processInstanceId, Map<String, Object> data, CIBUser user) throws SystemException {
    data.put("processInstanceIdIn", new String[]{processInstanceId});
    final boolean deserializeValues = data != null
        && data.containsKey("deserializeValues")
        && (Boolean) data.get("deserializeValues");
    if (data != null && data.containsKey("deserializeValues"))
      data.remove("deserializeValues"); 

    ObjectMapper objectMapper = getObjectMapper();
    VariableInstanceQueryDto queryDto = objectMapper.convertValue(data, VariableInstanceQueryDto.class);
    
    queryDto.setObjectMapper(objectMapper);
    
    List<Variable> variablesDeserialized = queryVariableInstances(queryDto, objectMapper, null, null, true);
    if ( variablesDeserialized.isEmpty())
      return Collections.emptyList();
    List<Variable> variablesSerialized = queryVariableInstances(queryDto, objectMapper, null, null, false);
    if ( variablesSerialized.isEmpty())
      return Collections.emptyList();
    
    
    VariableProvider.mergeVariablesValues(
        variablesDeserialized,
        variablesSerialized,
        deserializeValues);
      Collection<Variable> variables = (deserializeValues) ? variablesDeserialized : variablesSerialized;
      return variables;
  }
 
  private List<Variable> queryVariableInstances(
      VariableInstanceQueryDto queryDto, ObjectMapper objectMapper ,Integer firstResult, Integer maxResults, boolean deserializeObjectValues) {  
    VariableInstanceQuery query = queryDto.toQuery(processEngine);

    // disable binary fetching by default.
    query.disableBinaryFetching();

    // disable custom object fetching by default. Cannot be done to not break existing API
    if (!deserializeObjectValues) {
      query.disableCustomObjectDeserialization();
    }

    List<org.cibseven.bpm.engine.runtime.VariableInstance> matchingInstances = QueryUtil.list(query, firstResult, maxResults);

    //List<VariableInstanceDto> instanceResults = new ArrayList<>();
    List<Variable> instanceResults = new ArrayList<>();
    for (org.cibseven.bpm.engine.runtime.VariableInstance instance : matchingInstances) {
      VariableInstanceDto resultInstanceDto = VariableInstanceDto.fromVariableInstance(instance);
      VariableHistory resultInstance = convertValue(objectMapper, resultInstanceDto, VariableHistory.class);
      instanceResults.add(resultInstance);
    }
    return instanceResults;
  }
  

  
  // @Override
  public ResponseEntity<byte[]> fetchVariableDataByExecutionId(String executionId, String variableName, CIBUser user)
      throws NoObjectFoundException, SystemException {
    // TODO: in progress
    // R( /engine-rest/execution/79816b6b-a393-11f0-8830-4ce1734f67af/localVariables/invoiceDocument/data)
    TypedValue typedVariableValue = runtimeService.getVariableLocalTyped(executionId, variableName, false);
    // id: 79816b6b-a393-11f0-8830-4ce1734f67af

    return getResponseForTypedVariable(typedVariableValue, executionId);
  }

  private ResponseEntity<byte[]> getResponseForTypedVariable(TypedValue typedVariableValue, String id) {
    if (typedVariableValue instanceof BytesValue || ValueType.BYTES.equals(typedVariableValue.getType())) {
      byte[] valueBytes = (byte[]) typedVariableValue.getValue();
      if (valueBytes == null) {
        valueBytes = new byte[0];
      }
      ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(valueBytes, HttpStatusCode.valueOf(200));
      return responseEntity;
    } else if (ValueType.FILE.equals(typedVariableValue.getType())) {
      FileValue typedFileValue = (FileValue) typedVariableValue;
      //TODO: is anybody interested in the type
//      String type = typedFileValue.getMimeType() != null ? typedFileValue.getMimeType()
//          : MediaType.APPLICATION_OCTET_STREAM.toString();
//      if (typedFileValue.getEncoding() != null) {
//        type += "; charset=" + typedFileValue.getEncoding();
//      }
      try {
        byte[] bytes = typedFileValue.getValue() == null ? null : IOUtils.toByteArray(typedFileValue.getValue());
        // status code if bytes==null?
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(typedFileValue.getMimeType() != null ? MediaType.valueOf(typedFileValue.getMimeType()) :  MediaType.APPLICATION_OCTET_STREAM);
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(bytes, headers, HttpStatusCode.valueOf(200));
        return responseEntity;
      } catch (IOException e) {
        // TODO exception type
        throw new IllegalArgumentException(e.getMessage(), e);
      }
    } else {
      // TODO: exception type
      throw new IllegalArgumentException(String.format("Value of variable with id %s is not a binary value.", id));
    }
  }

  //@Override
  public Collection<VariableHistory> fetchProcessInstanceVariablesHistory(String processInstanceId, Map<String, Object> data, CIBUser user) throws SystemException {
    //TODO: requires further testing, escp. merging
    data.put("processInstanceIdIn", new String[]{processInstanceId});
    final boolean deserializeValues = data != null
        && data.containsKey("deserializeValues")
        && (Boolean) data.get("deserializeValues");
    if (data != null && data.containsKey("deserializeValues"))
      data.remove("deserializeValues"); 

    ObjectMapper objectMapper = getObjectMapper();
    //TODO: data contains "variableValues=null" which is not member of HistoricVariableInstanceQueryDto
    //but there is "variableValue" -> create ticket
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    HistoricVariableInstanceQueryDto queryDto = objectMapper.convertValue(data, HistoricVariableInstanceQueryDto.class);
    
    queryDto.setObjectMapper(objectMapper);
    
    List<VariableHistory> variablesDeserialized = queryHistoricVariableInstances(queryDto, objectMapper, null, null, true);
//[VariableHistory(id=2b38760a-a35a-11f0-afe5-4ce1734f67af, name=testBool, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b38760c-a35a-11f0-afe5-4ce1734f67af, name=datetime_fr4cbr, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b389d1e-a35a-11f0-afe5-4ce1734f67af, name=season, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b389d20-a35a-11f0-afe5-4ce1734f67af, name=guestCount, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b389d22-a35a-11f0-afe5-4ce1734f67af, name=number_double, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null)]    
    if ( variablesDeserialized.isEmpty())
      return Collections.emptyList();
    List<VariableHistory> variablesSerialized = queryHistoricVariableInstances(queryDto, objectMapper, null, null, false);
//[VariableHistory(id=2b38760a-a35a-11f0-afe5-4ce1734f67af, name=testBool, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b38760c-a35a-11f0-afe5-4ce1734f67af, name=datetime_fr4cbr, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b389d1e-a35a-11f0-afe5-4ce1734f67af, name=season, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b389d20-a35a-11f0-afe5-4ce1734f67af, name=guestCount, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b389d22-a35a-11f0-afe5-4ce1734f67af, name=number_double, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null)]
    if ( variablesSerialized.isEmpty())
      return Collections.emptyList();
    

    // Get list of variables and merge them
    final ArrayList<Variable> variablesDeserializedTyped = new ArrayList<>();
    if (variablesDeserialized.size() > 0) {
      variablesDeserializedTyped.addAll(variablesDeserialized);
    }

    final ArrayList<Variable> variablesSerializedTyped = new ArrayList<>();
    if (variablesSerialized.size() > 0) {
      variablesSerializedTyped.addAll(variablesSerialized);
    }

    VariableProvider.mergeVariablesValues(
      variablesDeserializedTyped,
      variablesSerializedTyped,
      deserializeValues);

    Collection<VariableHistory> variables = (deserializeValues) ? variablesDeserialized : variablesSerialized;
    return variables;
    
  }
 
  private List<VariableHistory> queryHistoricVariableInstances(
      HistoricVariableInstanceQueryDto queryDto, ObjectMapper objectMapper ,Integer firstResult, Integer maxResults, boolean deserializeObjectValues) {  
    //change to history query!!
    HistoricVariableInstanceQuery query = queryDto.toQuery(processEngine);

    // disable binary fetching by default.
    query.disableBinaryFetching();

    // disable custom object fetching by default. Cannot be done to not break existing API
    if (!deserializeObjectValues) {
      query.disableCustomObjectDeserialization();
    }

    List<HistoricVariableInstance> matchingInstances = QueryUtil.list(query, firstResult, maxResults);

    //List<VariableInstanceDto> instanceResults = new ArrayList<>();
    List<VariableHistory> instanceResults = new ArrayList<>();
    for ( HistoricVariableInstance instance : matchingInstances) {
      HistoricVariableInstanceDto resultInstanceDto = HistoricVariableInstanceDto.fromHistoricVariableInstance(instance);
      VariableHistory resultInstance = convertValue(objectMapper, resultInstanceDto, VariableHistory.class);
      //Variable resultInstance = createVariable(instance);
      instanceResults.add(resultInstance);
    }
    return instanceResults;
  }
  
  //@Override
  public Collection<VariableHistory> fetchActivityVariablesHistory(String activityInstanceId, CIBUser user) {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public Collection<VariableHistory> fetchActivityVariables(String activityInstanceId, CIBUser user) {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public ResponseEntity<byte[]> fetchHistoryVariableDataById(String id, CIBUser user) throws NoObjectFoundException, SystemException  {
    //TODO: needs more testing
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery().variableId(id);
    query.disableCustomObjectDeserialization();
    //HistoricVariableInstanceEntity?
    HistoricVariableInstance queryResult = query.singleResult();
    if (queryResult != null) {
      TypedValue typedValue = queryResult.getTypedValue();
      return getResponseForTypedVariable(typedValue, id);
    } else {
      throw new IllegalArgumentException("HistoryVariable with Id '" + id + "' does not exist.");
    }
  }
  
  //@Override
  public Variable fetchVariable(String taskId, String variableName, 
      boolean deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException {    
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public void deleteVariable(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException {   
    //TODO: to be implemented
  }
  
  //@Override
  public Map<String, Variable> fetchFormVariables(String taskId, boolean deserializeValues, CIBUser user) throws NoObjectFoundException, SystemException {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public Map<String, Variable> fetchFormVariables(List<String> variableListName, String taskId, CIBUser user) throws NoObjectFoundException, SystemException {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public Map<String, Variable> fetchProcessFormVariables(String key, CIBUser user) throws NoObjectFoundException, SystemException {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public NamedByteArrayDataSource fetchVariableFileData(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, UnexpectedTypeException, SystemException {   
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public void uploadVariableFileData(String taskId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException {
    //TODO: to be implemented
  }
  
  //@Override
  public ResponseEntity<byte[]> fetchProcessInstanceVariableData(String processInstanceId, String variableName,
      CIBUser user) throws NoObjectFoundException, SystemException {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public void uploadProcessInstanceVariableFileData(String processInstanceId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException {
    //TODO: to be implemented
  }
  
  //@Override
  public ProcessStart submitStartFormVariables(String processDefinitionId, List<Variable> formResult, CIBUser user) throws SystemException {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public Variable fetchVariableByProcessInstanceId(String processInstanceId, String variableName, CIBUser user) throws SystemException {
    //TODO: to be implemented
    return null;
  }

  //@Override
  public void saveVariableInProcessInstanceId(String processInstanceId, List<Variable> variables, CIBUser user) throws SystemException {
    //TODO: to be implemented
  }
  
  //@Override
  public void submitVariables(String processInstanceId, List<Variable> formResult, CIBUser user, String processDefinitionId) throws SystemException {
    //TODO: to be implemented
  }
  
  //@Override
  public Map<String, Variable> fetchProcessFormVariablesById(String id, CIBUser user) throws SystemException {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public void putLocalExecutionVariable(String executionId, String varName, Map<String, Object> data, CIBUser user) {
    //TODO: to be implemented
  }

  //@Override
  public Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(String processDefinitionId, Map<String, Object> params, CIBUser user) {
    //TODO: to be implemented
    return null;
  }
  
/*
  
       ██  ██████  ██████      ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
       ██ ██    ██ ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
       ██ ██    ██ ██████      ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██   ██ ██    ██ ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
   █████   ██████  ██████      ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                            
  */
  
  //@Override
  public Collection<JobDefinition> findJobDefinitions(String params, CIBUser user) {
    ObjectMapper objectMapper = getObjectMapper();
    JobDefinitionQueryDto queryDto;
    try {
      queryDto = objectMapper.readValue(params, JobDefinitionQueryDto.class);
    } catch (JsonProcessingException e) {
      // TODO: exception type
      throw new IllegalArgumentException(e.getMessage());
    }
    queryDto.setObjectMapper(objectMapper);
    JobDefinitionQuery query = queryDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.management.JobDefinition> matchingJobDefinitions = QueryUtil.list(query, null, null);

    List<JobDefinition> jobDefinitionResults = new ArrayList<>();
    for (org.cibseven.bpm.engine.management.JobDefinition jobDefinition : matchingJobDefinitions) {
      JobDefinitionDto result = JobDefinitionDto.fromJobDefinition(jobDefinition);
      jobDefinitionResults.add(convertValue(objectMapper, result, JobDefinition.class));
    }

    return jobDefinitionResults;
  }
  
  //@Override
  public void suspendJobDefinition(String jobDefinitionId, String params, CIBUser user) {
    //TODO: to be implemented
  }
  
  //@Override
  public void overrideJobDefinitionPriority(String jobDefinitionId, String params, CIBUser user) {
    //TODO: to be implemented
  }

  //@Override
  public void retryJobDefinitionById(String id, Map<String, Object> params, CIBUser user) {
    //TODO: to be implemented
  }
  
  //@Override
  public Collection<Job> getJobs(Map<String, Object> params, CIBUser user) {
    //TODO: to be implemented
    return null;
  }

  //@Override
  public void setSuspended(String id, Map<String, Object> params, CIBUser user) {
    // TODO: in progress
    ObjectMapper objectMapper = getObjectMapper();
    JobDefinitionSuspensionStateDto jobDefinitionSuspensionStateDto = objectMapper.convertValue(params,
        JobDefinitionSuspensionStateDto.class);
    jobDefinitionSuspensionStateDto.setProcessDefinitionId(id);
    if (jobDefinitionSuspensionStateDto.getJobDefinitionId() != null) {
      String message = "Either processDefinitionId or processDefinitionKey can be set to update the suspension state.";
      // TODO: exception type
      throw new IllegalArgumentException(message);
    }

    try {
      jobDefinitionSuspensionStateDto.updateSuspensionState(processEngine);

    } catch (IllegalArgumentException e) {
      String message = String.format("Could not update the suspension state of Job Definitions due to: %s",
          e.getMessage());
      // TODO: exception type
      throw new IllegalArgumentException(message, e);
    }
  }

  //@Override
  public void deleteJob(String id, CIBUser user) {
    //TODO: to be implemented
  }

  //@Override
  public JobDefinition findJobDefinition(String id, CIBUser user) {
    //TODO: to be implemented
    return null;
  } 

  //@Override
  public Collection<Object> getHistoryJobLog(Map<String, Object> params, CIBUser user) {
    //TODO: to be implemented
    return null;
  }
  
  //@Override
  public String getHistoryJobLogStacktrace(String id, CIBUser user) {
    //TODO: to be implemented
    return null;
  }

/** 
* conversion and helper functions
*/
  private <T> T convertValue(ObjectMapper objectMapper, Object fromValueDto, Class<T> toValueType)
        throws IllegalArgumentException {
      Map<String, Object> filterDtoMap = objectMapper
          .convertValue(fromValueDto, new TypeReference<Map<String, Object>>() {});
      return objectMapper.convertValue(filterDtoMap, toValueType);
  }
  
  /*

  ██████  ███████ ██████  ██       ██████  ██    ██ ███    ███ ███████ ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██   ██ ██      ██   ██ ██      ██    ██  ██  ██  ████  ████ ██      ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██   ██ █████   ██████  ██      ██    ██   ████   ██ ████ ██ █████   ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██   ██ ██      ██      ██      ██    ██    ██    ██  ██  ██ ██      ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ██████  ███████ ██      ███████  ██████     ██    ██      ██ ███████ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                         
   */

  //@Override
  public Deployment createDeployment(MultiValueMap<String, Object> data, MultipartFile[] files, CIBUser user) throws SystemException {
    //TODO: in progress
    //TODO: add authorization
    ObjectMapper objectMapper = getObjectMapper();
    MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>(data);
    for (int i = 0; i < files.length; i++) {
      MultipartFile file = files[i];
      // API expects files with parameter name "data", so we use indexed keys
      // result could be: ['data0', 'data1', 'data2', ...] or just single ['data']
      String key = files.length > 1 ? "data" + i : "data";
      formData.add(key, file.getResource());
    }
    DeploymentBuilder deploymentBuilder = extractDeploymentInformation(convertValue(objectMapper, formData, MultipartFormData.class));

    if(!deploymentBuilder.getResourceNames().isEmpty()) {
      DeploymentWithDefinitions deployment = deploymentBuilder.deployWithResult();

      DeploymentWithDefinitionsDto deploymentDto = DeploymentWithDefinitionsDto.fromDeployment(deployment);


      //TODO: getEngineRestUrl() will be a base class method after completion
      UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://localhost:8080");//getEngineRestUrl());
      URI uri = builder
        .path("/")//relativeRootResourcePath)
        .path("/deployment")//DeploymentRestService.PATH)
        .path(deployment.getId())
        .build().toUri();

      // GET
      deploymentDto.addReflexiveLink(uri, HttpMethod.GET, "self");
      return convertValue(objectMapper, deploymentDto, Deployment.class);

    } else {
      //TODO: exception type 
      throw new IllegalArgumentException("No deployment resources contained in the form upload.");
    }
/*
    String url = getEngineRestUrl() + "/deployment/create";
    // Prepare multipart form data - start with provided data
    MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>(data);
    // Add files to form data with indexed "data" keys
    for (int i = 0; i < files.length; i++) {
      MultipartFile file = files[i];
      // API expects files with parameter name "data", so we use indexed keys
      // result could be: ['data0', 'data1', 'data2', ...] or just single ['data']
      String key = files.length > 1 ? "data" + i : "data";
      formData.add(key, file.getResource());
    }
    // Use the base class method for multipart POST
    ResponseEntity<Deployment> response = doPostMultipart(url, formData, Deployment.class, user);
    return response.getBody();

//deploy bpmn:
    String url = getEngineRestUrl() + "/deployment/create";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    if (user != null) {
      headers.add(HttpHeaders.AUTHORIZATION, user.getAuthToken());
      headers.add(USER_ID_HEADER, user.getId());
    }
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

    file.forEach((key, value) -> { 
      try {
        data.add(key, value.get(0).getResource());
      } catch (Exception e) {
        throw new SystemException(e);
      }
    });

    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(data, headers);

    try {
      return customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, Deployment.class).getBody();
    } catch (HttpStatusCodeException e) {
      throw wrapException(e, user);
    }
 * */    
    
  }
  private ObjectMapper getObjectMapper() {
    if (objectMapper == null) 
    {
      objectMapper = new ObjectMapper();
      JacksonConfigurator.configureObjectMapper(objectMapper);
    }
    return objectMapper;
  }
}
