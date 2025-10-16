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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;

import org.apache.commons.io.IOUtils;
import org.cibseven.bpm.dmn.engine.DmnDecisionResult;
import org.cibseven.bpm.dmn.engine.DmnDecisionResultEntries;
import org.cibseven.bpm.dmn.engine.DmnEngineException;
import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.bpm.engine.AuthorizationService;
import org.cibseven.bpm.engine.BadUserRequestException;
import org.cibseven.bpm.engine.CaseService;
import org.cibseven.bpm.engine.DecisionService;
import org.cibseven.bpm.engine.EntityTypes;
import org.cibseven.bpm.engine.ExternalTaskService;
import org.cibseven.bpm.engine.FilterService;
import org.cibseven.bpm.engine.FormService;
import org.cibseven.bpm.engine.HistoryService;
import org.cibseven.bpm.engine.IdentityService;
import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.MismatchingMessageCorrelationException;
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
import org.cibseven.bpm.engine.batch.BatchQuery;
import org.cibseven.bpm.engine.batch.BatchStatistics;
import org.cibseven.bpm.engine.batch.BatchStatisticsQuery;
import org.cibseven.bpm.engine.batch.history.HistoricBatch;
import org.cibseven.bpm.engine.batch.history.HistoricBatchQuery;
import org.cibseven.bpm.engine.exception.NotFoundException;
import org.cibseven.bpm.engine.exception.NotValidException;
import org.cibseven.bpm.engine.exception.NullValueException;
import org.cibseven.bpm.engine.externaltask.ExternalTaskQuery;
import org.cibseven.bpm.engine.filter.FilterQuery;
import org.cibseven.bpm.engine.form.CamundaFormRef;
import org.cibseven.bpm.engine.form.FormData;
import org.cibseven.bpm.engine.form.StartFormData;
import org.cibseven.bpm.engine.history.CleanableHistoricBatchReport;
import org.cibseven.bpm.engine.history.CleanableHistoricBatchReportResult;
import org.cibseven.bpm.engine.history.HistoricActivityInstance;
import org.cibseven.bpm.engine.history.HistoricActivityInstanceQuery;
import org.cibseven.bpm.engine.history.HistoricActivityStatistics;
import org.cibseven.bpm.engine.history.HistoricActivityStatisticsQuery;
import org.cibseven.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.cibseven.bpm.engine.history.HistoricIncident;
import org.cibseven.bpm.engine.history.HistoricIncidentQuery;
import org.cibseven.bpm.engine.history.HistoricJobLog;
import org.cibseven.bpm.engine.history.HistoricJobLogQuery;
import org.cibseven.bpm.engine.history.HistoricProcessInstance;
import org.cibseven.bpm.engine.history.HistoricProcessInstanceQuery;
import org.cibseven.bpm.engine.history.HistoricTaskInstance;
import org.cibseven.bpm.engine.history.HistoricTaskInstanceQuery;
import org.cibseven.bpm.engine.history.HistoricVariableInstance;
import org.cibseven.bpm.engine.history.HistoricVariableInstanceQuery;
import org.cibseven.bpm.engine.history.SetRemovalTimeSelectModeForHistoricBatchesBuilder;
import org.cibseven.bpm.engine.history.SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder;
import org.cibseven.bpm.engine.identity.Group;
import org.cibseven.bpm.engine.identity.GroupQuery;
import org.cibseven.bpm.engine.identity.TenantQuery;
import org.cibseven.bpm.engine.identity.UserQuery;
import org.cibseven.bpm.engine.impl.RuntimeServiceImpl;
import org.cibseven.bpm.engine.impl.calendar.DateTimeUtil;
import org.cibseven.bpm.engine.impl.form.validator.FormFieldValidationException;
import org.cibseven.bpm.engine.impl.identity.Authentication;
import org.cibseven.bpm.engine.impl.util.IoUtil;
import org.cibseven.bpm.engine.impl.util.PermissionConverter;
import org.cibseven.bpm.engine.management.ActivityStatistics;
import org.cibseven.bpm.engine.management.ActivityStatisticsQuery;
import org.cibseven.bpm.engine.management.IncidentStatistics;
import org.cibseven.bpm.engine.management.JobDefinitionQuery;
import org.cibseven.bpm.engine.management.Metrics;
import org.cibseven.bpm.engine.management.MetricsQuery;
import org.cibseven.bpm.engine.management.ProcessDefinitionStatistics;
import org.cibseven.bpm.engine.management.ProcessDefinitionStatisticsQuery;
import org.cibseven.bpm.engine.management.SetJobRetriesBuilder;
import org.cibseven.bpm.engine.query.Query;
import org.cibseven.bpm.engine.repository.DecisionDefinition;
import org.cibseven.bpm.engine.repository.DecisionDefinitionQuery;
import org.cibseven.bpm.engine.repository.DeploymentBuilder;
import org.cibseven.bpm.engine.repository.DeploymentQuery;
import org.cibseven.bpm.engine.repository.DeploymentWithDefinitions;
import org.cibseven.bpm.engine.repository.ProcessDefinition;
import org.cibseven.bpm.engine.repository.ProcessDefinitionQuery;
import org.cibseven.bpm.engine.repository.Resource;
import org.cibseven.bpm.engine.rest.dto.AbstractQueryDto;
import org.cibseven.bpm.engine.rest.dto.AnnotationDto;
import org.cibseven.bpm.engine.rest.dto.CountResultDto;
import org.cibseven.bpm.engine.rest.dto.HistoryTimeToLiveDto;
import org.cibseven.bpm.engine.rest.dto.PatchVariablesDto;
import org.cibseven.bpm.engine.rest.dto.StatisticsResultDto;
import org.cibseven.bpm.engine.rest.dto.VariableValueDto;
import org.cibseven.bpm.engine.rest.dto.authorization.AuthorizationDto;
import org.cibseven.bpm.engine.rest.dto.authorization.AuthorizationQueryDto;
import org.cibseven.bpm.engine.rest.dto.batch.BatchDto;
import org.cibseven.bpm.engine.rest.dto.batch.BatchQueryDto;
import org.cibseven.bpm.engine.rest.dto.batch.BatchStatisticsDto;
import org.cibseven.bpm.engine.rest.dto.batch.BatchStatisticsQueryDto;
import org.cibseven.bpm.engine.rest.dto.converter.DateConverter;
import org.cibseven.bpm.engine.rest.dto.converter.DelegationStateConverter;
import org.cibseven.bpm.engine.rest.dto.converter.StringListConverter;
import org.cibseven.bpm.engine.rest.dto.dmn.EvaluateDecisionDto;
import org.cibseven.bpm.engine.rest.dto.externaltask.ExternalTaskDto;
import org.cibseven.bpm.engine.rest.dto.externaltask.ExternalTaskQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.DeleteHistoricProcessInstancesDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricActivityInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricActivityInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricActivityStatisticsDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricDecisionInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricDecisionInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricIncidentDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricIncidentQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricJobLogDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricJobLogQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricProcessInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricProcessInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricTaskInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricTaskInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricVariableInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricVariableInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.CleanableHistoricBatchReportDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.CleanableHistoricBatchReportResultDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.DeleteHistoricDecisionInstancesDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.HistoricBatchDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.HistoricBatchQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.removaltime.SetRemovalTimeToHistoricBatchesDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.removaltime.SetRemovalTimeToHistoricDecisionInstancesDto;
import org.cibseven.bpm.engine.rest.dto.identity.GroupQueryDto;
import org.cibseven.bpm.engine.rest.dto.identity.TenantDto;
import org.cibseven.bpm.engine.rest.dto.identity.TenantQueryDto;
import org.cibseven.bpm.engine.rest.dto.identity.UserQueryDto;
import org.cibseven.bpm.engine.rest.dto.management.JobDefinitionDto;
import org.cibseven.bpm.engine.rest.dto.management.JobDefinitionQueryDto;
import org.cibseven.bpm.engine.rest.dto.management.JobDefinitionSuspensionStateDto;
import org.cibseven.bpm.engine.rest.dto.message.CorrelationMessageDto;
import org.cibseven.bpm.engine.rest.dto.message.MessageCorrelationResultDto;
import org.cibseven.bpm.engine.rest.dto.message.MessageCorrelationResultWithVariableDto;
import org.cibseven.bpm.engine.rest.dto.repository.ActivityStatisticsResultDto;
import org.cibseven.bpm.engine.rest.dto.repository.CalledProcessDefinitionDto;
import org.cibseven.bpm.engine.rest.dto.repository.DecisionDefinitionDiagramDto;
import org.cibseven.bpm.engine.rest.dto.repository.DecisionDefinitionDto;
import org.cibseven.bpm.engine.rest.dto.repository.DecisionDefinitionQueryDto;
import org.cibseven.bpm.engine.rest.dto.repository.DeploymentDto;
import org.cibseven.bpm.engine.rest.dto.repository.DeploymentQueryDto;
import org.cibseven.bpm.engine.rest.dto.repository.DeploymentResourceDto;
import org.cibseven.bpm.engine.rest.dto.repository.DeploymentWithDefinitionsDto;
import org.cibseven.bpm.engine.rest.dto.repository.ProcessDefinitionDiagramDto;
import org.cibseven.bpm.engine.rest.dto.repository.ProcessDefinitionDto;
import org.cibseven.bpm.engine.rest.dto.repository.ProcessDefinitionQueryDto;
import org.cibseven.bpm.engine.rest.dto.repository.ProcessDefinitionStatisticsResultDto;
import org.cibseven.bpm.engine.rest.dto.repository.ProcessDefinitionSuspensionStateDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ActivityInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.EventSubscriptionDto;
import org.cibseven.bpm.engine.rest.dto.runtime.EventSubscriptionQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.FilterDto;
import org.cibseven.bpm.engine.rest.dto.runtime.FilterQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.IncidentDto;
import org.cibseven.bpm.engine.rest.dto.runtime.IncidentQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ProcessInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ProcessInstanceSuspensionStateDto;
import org.cibseven.bpm.engine.rest.dto.runtime.ProcessInstanceWithVariablesDto;
import org.cibseven.bpm.engine.rest.dto.runtime.RetriesDto;
import org.cibseven.bpm.engine.rest.dto.runtime.StartProcessInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceQueryDto;
import org.cibseven.bpm.engine.rest.dto.runtime.batch.DeleteProcessInstancesDto;
import org.cibseven.bpm.engine.rest.dto.runtime.modification.ProcessInstanceModificationInstructionDto;
import org.cibseven.bpm.engine.rest.dto.task.CompleteTaskDto;
import org.cibseven.bpm.engine.rest.dto.task.FormDto;
import org.cibseven.bpm.engine.rest.dto.task.GroupDto;
import org.cibseven.bpm.engine.rest.dto.task.GroupInfoDto;
import org.cibseven.bpm.engine.rest.dto.task.IdentityLinkDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskBpmnErrorDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskCountByCandidateGroupResultDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskQueryDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskWithAttachmentAndCommentDto;
import org.cibseven.bpm.engine.rest.dto.task.UserDto;
import org.cibseven.bpm.engine.rest.dto.telemetry.TelemetryDataDto;
import org.cibseven.bpm.engine.rest.exception.InvalidRequestException;
import org.cibseven.bpm.engine.rest.exception.RestException;
import org.cibseven.bpm.engine.rest.impl.history.HistoricActivityStatisticsQueryDto;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.rest.mapper.MultipartFormData;
import org.cibseven.bpm.engine.rest.mapper.MultipartFormData.FormPart;
import org.cibseven.bpm.engine.rest.sub.impl.VariableResponseProvider;
import org.cibseven.bpm.engine.rest.sub.repository.impl.ProcessDefinitionResourceImpl;
import org.cibseven.bpm.engine.rest.util.ApplicationContextPathUtil;
import org.cibseven.bpm.engine.rest.util.ContentTypeUtil;
import org.cibseven.bpm.engine.rest.util.QueryUtil;
import org.cibseven.bpm.engine.rest.util.URLEncodingUtil;
import org.cibseven.bpm.engine.runtime.DeserializationTypeValidator;
import org.cibseven.bpm.engine.runtime.EventSubscriptionQuery;
import org.cibseven.bpm.engine.runtime.IncidentQuery;
import org.cibseven.bpm.engine.runtime.MessageCorrelationBuilder;
import org.cibseven.bpm.engine.runtime.MessageCorrelationResult;
import org.cibseven.bpm.engine.runtime.MessageCorrelationResultWithVariables;
import org.cibseven.bpm.engine.runtime.ProcessInstanceQuery;
import org.cibseven.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.cibseven.bpm.engine.runtime.ProcessInstantiationBuilder;
import org.cibseven.bpm.engine.runtime.VariableInstanceQuery;
import org.cibseven.bpm.engine.task.DelegationState;
import org.cibseven.bpm.engine.task.TaskCountByCandidateGroupResult;
import org.cibseven.bpm.engine.task.TaskQuery;
import org.cibseven.bpm.engine.telemetry.TelemetryData;
import org.cibseven.bpm.engine.variable.VariableMap;
import org.cibseven.bpm.engine.variable.Variables;
import org.cibseven.bpm.engine.variable.impl.type.AbstractValueTypeImpl;
import org.cibseven.bpm.engine.variable.type.FileValueType;
import org.cibseven.bpm.engine.variable.type.ValueType;
import org.cibseven.bpm.engine.variable.value.BytesValue;
import org.cibseven.bpm.engine.variable.value.FileValue;
import org.cibseven.bpm.engine.variable.value.PrimitiveValue;
import org.cibseven.bpm.engine.variable.value.SerializableValue;
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
import org.cibseven.webapp.rest.model.Batch;
import org.cibseven.webapp.rest.model.CamundaForm;
import org.cibseven.webapp.rest.model.CandidateGroupTaskCount;
import org.cibseven.webapp.rest.model.Decision;
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.rest.model.DeploymentResource;
import org.cibseven.webapp.rest.model.EventSubscription;
import org.cibseven.webapp.rest.model.ExternalTask;
import org.cibseven.webapp.rest.model.Filter;
import org.cibseven.webapp.rest.model.FilterCriterias;
import org.cibseven.webapp.rest.model.FilterProperties;
import org.cibseven.webapp.rest.model.HistoricDecisionInstance;
import org.cibseven.webapp.rest.model.HistoryBatch;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Incident;
import org.cibseven.webapp.rest.model.IncidentInfo;
import org.cibseven.webapp.rest.model.Job;
import org.cibseven.webapp.rest.model.JobDefinition;
import org.cibseven.webapp.rest.model.Message;
import org.cibseven.webapp.rest.model.Metric;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskForm;
import org.cibseven.webapp.rest.model.TaskHistory;
import org.cibseven.webapp.rest.model.Tenant;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.cibseven.webapp.rest.model.VariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import camundajar.impl.scala.Array;
import jakarta.activation.DataSource;
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
  private FormService formService;
  private TaskService taskService;
  private IdentityService identityService;
  private AuthorizationService authorizationService;
  private RuntimeService runtimeService;
  private ManagementService managementService;
  private RepositoryService repositoryService;
  private HistoryService historyService;
  private FilterService filterService;
  private ExternalTaskService externalTaskService;
  private DecisionService decisionService;
  //private CaseService caseService;
  private ProcessEngineConfiguration processEngineConfiguration;
  private ProcessEngine processEngine;
  
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
  
  public SevenDirectProvider(String engineName) {
    if (engineName == null)
      processEngine = org.cibseven.bpm.BpmPlatform.getDefaultProcessEngine();
    else
      processEngine = org.cibseven.bpm.BpmPlatform.getProcessEngineService().getProcessEngine(engineName);
    formService = processEngine.getFormService();
    taskService = processEngine.getTaskService();
    identityService = processEngine.getIdentityService();
    authorizationService = processEngine.getAuthorizationService();
    runtimeService = processEngine.getRuntimeService();
    managementService = processEngine.getManagementService();
    repositoryService = processEngine.getRepositoryService();
    historyService = processEngine.getHistoryService();
    filterService = processEngine.getFilterService();
    externalTaskService = processEngine.getExternalTaskService();
    decisionService = processEngine.getDecisionService();
    //caseService = processEngine.getCaseService();
    processEngineConfiguration = processEngine.getProcessEngineConfiguration();
    objectMapper = new ObjectMapper();
    JacksonConfigurator.configureObjectMapper(objectMapper);
    
  }

  /*
  
    ████████  █████  ███████ ██   ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
       ██    ██   ██ ██      ██  ██      ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
       ██    ███████ ███████ █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
       ██    ██   ██      ██ ██  ██      ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
       ██    ██   ██ ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                                            
   */
  
  //@Override
  public Collection<Task> findTasks(String filter, CIBUser user) {
    //TODO: not tested 
    Map<String, Object> filters = new HashMap<>();
      String[] splitFilter = filter.split("&");
      for (String params : splitFilter) {
        String[] splitValue = params.split("=");
        if (splitValue.length > 1)
          filters.put(splitValue[0], URLDecoder.decode(splitValue[1], Charset.forName("UTF-8")));
      }
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
    //TODO: not tested
    TaskQueryDto dto = new TaskQueryDto();
    if (createdAfter.isPresent()) {
      dto.setCreatedAfter(objectMapper.convertValue(createdAfter.get(), Date.class));
    }
    dto.setAssignee(user.getId());
    if (processInstanceId.isPresent())
      dto.setProcessInstanceId(processInstanceId.get());
    TaskQuery taskQuery = dto.toQuery(processEngine);
    List<org.cibseven.bpm.engine.task.Task> resultList = taskQuery.initializeFormKeys().list();
    return convertTasks(resultList);

  }
  
  //@Override
  public Task findTaskById(String id, CIBUser user) {
    org.cibseven.bpm.engine.task.Task result = taskService.createTaskQuery().taskId(id).initializeFormKeys().singleResult();
    return objectMapper.convertValue(TaskDto.fromEntity(result), Task.class);
  }

  //@Override
  public void update(Task task, CIBUser user) {
    //TODO: not completely tested
    org.cibseven.bpm.engine.task.Task foundTask = taskService.createTaskQuery().taskId(task.getId()).initializeFormKeys().singleResult();

    if (foundTask == null) {
      throw new SystemException("No matching task with id " + task.getId());
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

    foundTask.setDueDate(objectMapper.convertValue(task.getDue(), Date.class));
    foundTask.setFollowUpDate(objectMapper.convertValue(task.getFollowUp(), Date.class));
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
    //TODO: not tested
    Map<String, VariableValueDto> variables = new HashMap<>();
    for (Variable variable : formResult) {
      VariableValueDto variableValueDto = convertValue(variable, VariableValueDto.class);
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
      formService.submitTaskForm(task.getId(), variablesMap);

    } catch (AuthorizationException e) {
      throw e;

    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot submit task form %s: %s", task.getId(), e.getMessage());
      throw new SystemException(errorMessage, e);
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
      throw new SystemException("Failed json conversion", e);
    }
    List<?> entities = executeFilterList(extendingQuery, filterId, firstResult, maxResults, objectMapper);
    return entities;
  }

  //@Override
  public Collection<Task> findTasksByFilter(TaskFiltering filters, String filterId, CIBUser user, 
      Integer firstResult, Integer maxResults) {

    List<?> entities = executeFilterList(filters, filterId, user, firstResult, maxResults);

    if (entities != null && !entities.isEmpty()) {
      //TODO: currently list of TaskDto
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
      return convertValue(TaskDto.fromEntity((org.cibseven.bpm.engine.task.Task) entity), Task.class);
    }
    else {
      throw new SystemException("Entities of class '" + entity.getClass().getCanonicalName() + "' are currently not supported by filters."); 
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
      throw new SystemException("Filter not found",e);
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
        throw new SystemException("Queries for resource type '" + resourceType + "' are currently not supported by filters.");
      }
    } catch (IOException e) {
        throw new SystemException("Invalid query for resource type '" + resourceType + "'", e);
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
    //TODO: not tested
    List<TaskHistory> taskHistoryList = new ArrayList<>();
    List<HistoricTaskInstance> results = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).unlimitedList();
    for (HistoricTaskInstance result : results) {
      taskHistoryList.add(convertValue(HistoricTaskInstanceDto.fromHistoricTaskInstance(result), TaskHistory.class));
    }
    return taskHistoryList;
  }
  
  //@Override
  public Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId, CIBUser user) {
    //TODO: not tested
    List<TaskHistory> taskHistoryList = new ArrayList<>();
    List<HistoricTaskInstance> results = historyService.createHistoricTaskInstanceQuery().taskDefinitionKey(taskDefinitionKey).unlimitedList();
    for (HistoricTaskInstance result : results) {
      taskHistoryList.add(convertValue(HistoricTaskInstanceDto.fromHistoricTaskInstance(result), TaskHistory.class));
    }
    return taskHistoryList;
  }
  
  //@Override
  public Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException {
    TaskQueryDto queryDto = objectMapper.convertValue(data, TaskQueryDto.class); 
    queryDto.setObjectMapper(objectMapper);
    TaskQuery query = queryDto.toQuery(processEngine);

    query.initializeFormKeys();
    List<org.cibseven.bpm.engine.task.Task> matchingTasks = QueryUtil.list(query, null, null);

    List<TaskDto> tasks = new ArrayList<TaskDto>();
    if (Boolean.TRUE.equals(queryDto.getWithCommentAttachmentInfo())) {
      tasks = matchingTasks.stream().map(TaskWithAttachmentAndCommentDto::fromEntity).collect(Collectors.toList());
    }
    else {
      tasks = matchingTasks.stream().map(TaskDto::fromEntity).collect(Collectors.toList());
    }
    List<Task> resultTasks = new ArrayList<>();
    for ( org.cibseven.bpm.engine.task.Task matchingTask : matchingTasks) {
      resultTasks.add(objectMapper.convertValue(matchingTask, Task.class));
    }
    return resultTasks;
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
      throw new SystemException("Identity Link requires userId or groupId, but not both.");
    }
      
    if (userId == null && groupId == null) {
      throw new SystemException("Identity Link requires userId or groupId.");
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
      throw new SystemException("Identity Link requires userId or groupId, but not both.");
    }
      
    if (userId == null && groupId == null) {
      throw new SystemException("Identity Link requires userId or groupId.");
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
    //TODO: not tested
    TaskBpmnErrorDto dto = objectMapper.convertValue(data, TaskBpmnErrorDto.class); 
    try {
      taskService.handleBpmnError(taskId, dto.getErrorCode(), dto.getErrorMessage(),
          VariableValueDto.toMap(dto.getVariables(), processEngine, objectMapper));
    } catch (NotFoundException e) {
      throw new SystemException(e.getMessage(), e);
    } catch (BadUserRequestException e) {
      throw new SystemException(e.getMessage());
    }
  }

  //@Override
  public Collection<TaskHistory> findTasksByTaskIdHistory(String taskId, CIBUser user) {
    //TODO: not tested
    List<TaskHistory> taskHistoryList = new ArrayList<>();
    List<HistoricTaskInstance> results = historyService.createHistoricTaskInstanceQuery().taskId(taskId).unlimitedList();
    for (HistoricTaskInstance result : results) {
      taskHistoryList.add(convertValue(HistoricTaskInstanceDto.fromHistoricTaskInstance(result), TaskHistory.class));
    }
    return taskHistoryList;
  } 
  
  //@Override
  public ResponseEntity<byte[]> getDeployedForm(String taskId, CIBUser user) {
    InputStream form = formService.getDeployedTaskForm(taskId);
    try {
      byte[] bytes = IOUtils.toByteArray(form);
      ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(bytes, HttpStatusCode.valueOf(200));
      return responseEntity;
    } catch (IOException e) {
      //TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new ResponseEntity<byte[]>(HttpStatusCode.valueOf(422));
  }
  
  //@Override
  public Integer findHistoryTasksCount(Map<String, Object> filters, CIBUser user) {
    HistoricTaskInstanceQueryDto queryDto = objectMapper.convertValue(filters, HistoricTaskInstanceQueryDto.class);
    queryDto.setObjectMapper(objectMapper);
    HistoricTaskInstanceQuery query = queryDto.toQuery(processEngine);

    long count = query.count();
    return (int) count;
  }

  //@Override
  public Collection<CandidateGroupTaskCount> getTaskCountByCandidateGroup(CIBUser user) {
    TaskCountByCandidateGroupResultDto reportDto = new TaskCountByCandidateGroupResultDto();
    List<TaskCountByCandidateGroupResult> results =  reportDto.executeTaskCountByCandidateGroupReport(processEngine);
    Collection<CandidateGroupTaskCount> resultTaskCount = new ArrayList<>();
    for (TaskCountByCandidateGroupResult result : results) {
      resultTaskCount.add(convertValue(
        TaskCountByCandidateGroupResultDto.fromTaskCountByCandidateGroupResultDto(result),
        CandidateGroupTaskCount.class));
    }
    return resultTaskCount;
  }
  
  private Collection<Task> convertTasks(Collection<org.cibseven.bpm.engine.task.Task> engineTasks) {
    List<Task> resultList = new ArrayList<>();
    for( org.cibseven.bpm.engine.task.Task engineTask : engineTasks )
       resultList.add(objectMapper.convertValue(engineTask, Task.class));
    return resultList;
  }
  
  private List<org.cibseven.bpm.engine.task.Task> queryTasks(Map<String, Object> filters, CIBUser user) {
    ObjectMapper localObjectMapper = new ObjectMapper();
    JacksonConfigurator.configureObjectMapper(localObjectMapper);
    //TODO: 'unfinished' is requested but not supported by the TaskQuery -> create Ticket!
    localObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    TaskQueryDto dto = localObjectMapper.convertValue(filters, TaskQueryDto.class);
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
    ProcessDefinitionQueryDto queryDto = new ProcessDefinitionQueryDto(objectMapper, queryParameters);

    ProcessDefinitionQuery query = queryDto.toQuery(processEngine);
    List<ProcessDefinition> matchingDefinitions = QueryUtil.list(query, null, null);

    List<Process> processes = new ArrayList<>(); 
    for (ProcessDefinition definition : matchingDefinitions) {
      ProcessDefinitionDto def = ProcessDefinitionDto.fromProcessDefinition(definition);
      processes.add(convertValue(def, Process.class));
    }
    return processes;
  }
  
  // @Override
  public Collection<Process> findProcessesWithInfo(CIBUser user) {
    Map<String, Object> queryParams = new HashMap<>();
    queryParams.put("failedJobs", true);
    queryParams.put("incidents", true);
    Collection<ProcessStatistics> statisticsCollection = getProcessStatistics(queryParams, user);
    // TODO: not tested
    // Group by key and tenant ID to consolidate different versions
    List<ProcessStatistics> groupedStatistics = processProvider
        .groupProcessStatisticsByKeyAndTenant(statisticsCollection);
    // Build Process objects directly from grouped ProcessStatistics
    return groupedStatistics.stream().map(stats -> {
      Process process = convertValue(stats.getDefinition(), Process.class);

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
  
  public Collection<ProcessStatistics> getProcessStatistics(Map<String, Object> queryParams, CIBUser user) {

    Boolean includeIncidents = (Boolean)queryParams.get("incidents");
    String includeIncidentsForType = (String)queryParams.get("incidentsForType");
    Boolean includeRootIncidents = (Boolean)queryParams.get("rootIncidents");
    Boolean includeFailedJobs = (Boolean)queryParams.get("failedJobs");
    if (includeIncidents != null && includeIncidentsForType != null) {
      throw new SystemException("Only one of the query parameter includeIncidents or includeIncidentsForType can be set.");
    }

    if (includeIncidents != null && includeRootIncidents != null) {
      throw new SystemException("Only one of the query parameter includeIncidents or includeRootIncidents can be set.");
    }

    if (includeRootIncidents != null && includeIncidentsForType != null) {
      throw new SystemException("Only one of the query parameter includeRootIncidents or includeIncidentsForType can be set.");
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
      processStatistics.add(objectMapper.convertValue(ProcessDefinitionStatisticsResultDto.fromProcessDefinitionStatistics(queryResult), ProcessStatistics.class));
    }
    return processStatistics;
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
    
    ProcessDefinitionQueryDto queryDto = objectMapper.convertValue(filterMap, ProcessDefinitionQueryDto.class);

    List<Process> processes = new ArrayList<>();
    ProcessDefinitionQuery query = queryDto.toQuery(processEngine);
    List<ProcessDefinition> matchingDefinitions = QueryUtil.list(query, null, null);

    for (ProcessDefinition definition : matchingDefinitions) {
      ProcessDefinitionDto def = ProcessDefinitionDto.fromProcessDefinition(definition);
      processes.add(convertValue(def, Process.class));
    }
    for(Process process : processes) {
      ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
      processInstanceQueryDto.setProcessDefinitionId(process.getId());
       process.setRunningInstances(queryProcessInstancesCount(processInstanceQueryDto));
    }
    return processes;
  } 
  
  private Long queryProcessInstancesCount(ProcessInstanceQueryDto queryDto) {
    queryDto.setObjectMapper(objectMapper);
    ProcessInstanceQuery query = queryDto.toQuery(processEngine);
    return query.count();
  }
  
//@Override
  public Process findProcessByDefinitionKey(String key, String tenantId, CIBUser user) {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey(key)
        .latestVersion();
    if (tenantId !=null)
      query.tenantIdIn(new String[] {tenantId});
    else
      query.withoutTenantId();
    ProcessDefinition instance = query.singleResult();
    if (instance == null) {
      if (tenantId !=null)
        throw new SystemException("Process instance " + key + " not found with tenantId " + tenantId );
      else
        throw new SystemException("Process instance not found: " + key);
    }
    Process process = convertValue(ProcessDefinitionDto.fromProcessDefinition(instance), Process.class);
    return process;
  }
  
  //@Override
  public Collection<Process> findProcessVersionsByDefinitionKey(String key, String tenantId, Optional<Boolean> lazyLoad, CIBUser user) {
    ProcessDefinitionQueryDto queryDto = new ProcessDefinitionQueryDto();
    queryDto.setKey(key);
    if (tenantId != null)
        queryDto.setTenantIdIn(Arrays.asList(tenantId));
    else
      queryDto.setWithoutTenantId(true);
    ProcessDefinitionQuery query = queryDto.toQuery(processEngine);
    List<ProcessDefinition> definitions = QueryUtil.list(query, null, null);
    List<Process> processes = new ArrayList<>(); 
    for (ProcessDefinition definition : definitions) {
      ProcessDefinitionDto def = ProcessDefinitionDto.fromProcessDefinition(definition);
      processes.add(convertValue(def, Process.class));
    }

    if (!lazyLoad.isPresent() || (lazyLoad.isPresent() && !lazyLoad.get())) {
      for(Process process : processes) {
        HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto = new HistoricProcessInstanceQueryDto();
        historicProcessInstanceQueryDto.setProcessDefinitionId(process.getId());
        historicProcessInstanceQueryDto.setObjectMapper(objectMapper);
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historicProcessInstanceQueryDto.toQuery(processEngine);
        List<HistoricProcessInstance> matchingHistoricProcessInstances = historicProcessInstanceQuery.unlimitedList(); 

        if (matchingHistoricProcessInstances.isEmpty())
          throw new NullPointerException();
        process.setAllInstances(matchingHistoricProcessInstances.size());
        
        historicProcessInstanceQueryDto.setUnfinished(true);
        historicProcessInstanceQuery = historicProcessInstanceQueryDto.toQuery(processEngine);
        matchingHistoricProcessInstances = historicProcessInstanceQuery.unlimitedList(); 
        
        if (matchingHistoricProcessInstances.isEmpty())
          throw new NullPointerException();
        process.setRunningInstances(matchingHistoricProcessInstances.size());
        
        historicProcessInstanceQueryDto.setUnfinished(false);
        historicProcessInstanceQueryDto.setCompleted(true);
        historicProcessInstanceQuery = historicProcessInstanceQueryDto.toQuery(processEngine);
        matchingHistoricProcessInstances = historicProcessInstanceQuery.unlimitedList(); 
        
        if (matchingHistoricProcessInstances.isEmpty())
          throw new NullPointerException();
        process.setCompletedInstances(matchingHistoricProcessInstances.size());
      }
    }
    return processes;
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
    Process process = convertValue(definitionDto, Process.class);
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
    //TODO: not tested
    List<ProcessInstance> result = new ArrayList<>();
    List<org.cibseven.bpm.engine.runtime.ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey(key)
        .list();

    for (org.cibseven.bpm.engine.runtime.ProcessInstance instance : instances) {
        ProcessInstanceDto backendDto = ProcessInstanceDto.fromProcessInstance(instance);
        ProcessInstance webClientDto = convertValue(backendDto, ProcessInstance.class);
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
      return convertValue(ProcessDefinitionDiagramDto.create(id, new String(processModel, "UTF-8")), ProcessDiagram.class);
    } catch (AuthorizationException e) {
      throw e;
    } catch (NotFoundException e) {
      //TODO: check exception type
      throw new SystemException( "No matching definition with id " + id, e);
    } catch (UnsupportedEncodingException e) {
      //TODO: check exception type
      throw new SystemException(e.getMessage(), e);
    } finally {
      IoUtil.closeSilently(processModelIn);
    }
  }
  
  //@Override
  public StartForm fetchStartForm(String processDefinitionId, CIBUser user) {
    final StartFormData formData;
    try {
      formData = formService.getStartFormData(processDefinitionId);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new SystemException("Cannot get start form data for process definition " + processDefinitionId, e);
    }
    FormDto dto = FormDto.fromFormData(formData);
    if((dto.getKey() == null || dto.getKey().isEmpty()) && dto.getCamundaFormRef() == null) {
      if(formData != null && formData.getFormFields() != null && !formData.getFormFields().isEmpty()) {
        dto.setKey("embedded:engine://engine/:engine/process-definition/"+processDefinitionId+"/rendered-form");
      }
    }
    dto.setContextPath(ApplicationContextPathUtil.getApplicationPathByProcessDefinitionId(processEngine, processDefinitionId));
    return convertValue(dto, StartForm.class);
  }
  
  //@Override
  public Data downloadBpmn(String id, String fileName, CIBUser user) {
    ProcessDiagram diagram = fetchDiagram(id, user);
    ByteArrayResource resource = new ByteArrayResource(diagram.getBpmn20Xml().getBytes());
    return new Data(fileName, "application/bpmn+xml", resource, resource.contentLength());
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
    try {
      runtimeService.deleteProcessInstance(processInstanceId, null);
    } catch (BadUserRequestException e) {
      throw new SystemException(e.getMessage());
    }
  }
  
  //@Override
  public void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user) {
    ProcessDefinitionSuspensionStateDto dto = new ProcessDefinitionSuspensionStateDto();
    dto.setProcessDefinitionId(processDefinitionId);
    dto.setSuspended(suspend);
    dto.setIncludeProcessInstances(includeProcessInstances);
    //TODO: date conversion required?
    if (executionDate != null)
      dto.setExecutionDate(executionDate);
    try {
      dto.updateSuspensionState(processEngine);

    } catch (IllegalArgumentException e) {
      String message = String.format("Could not update the suspension state of Process Definitions due to: %s", e.getMessage()) ;
      throw new SystemException(message, e);
    }
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
      
      throw new SystemException(errorMessage);

    } else {
      // start the process
      ProcessInstanceWithVariables processInstanceWithVariables = null;
      // the simple case contains the _locale variable, only
      StartProcessInstanceDto startProcessInstanceDto = objectMapper.convertValue(data,  StartProcessInstanceDto.class);
      try {
        processInstanceWithVariables = startProcessInstanceAtActivities(startProcessInstanceDto,
            processDefinition.getId());
      } catch (AuthorizationException e) {
        throw e;

      } catch (ProcessEngineException e) {
        String errorMessage = String.format("Cannot instantiate process definition %s: %s", processDefinition.getId(),
            e.getMessage());
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
        //TODO Auto-generated catch block
        e.printStackTrace();
      }

      ProcessDefinition definition;
      try {
        definition = repositoryService.getProcessDefinition(processDefinition.getId());
      } catch (ProcessEngineException e) {
        throw new SystemException("No matching definition with id " + processDefinition.getId(), e);
      }

      return createProcessDefinitionQuery(definition);
    }

  }
  
  //TODO: rename, move to util class
  private ProcessInstanceWithVariables startProcessInstanceAtActivities(StartProcessInstanceDto dto, String processDefinitionKey) {
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
  public ProcessStart submitForm(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) 
    throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
    ProcessDefinitionQuery query = processEngine
        .getRepositoryService()
        .createProcessDefinitionQuery()
        .processDefinitionKey(processDefinitionKey);
    if (tenantId != null)
      query.tenantIdIn(tenantId);
    else
      query.withoutTenantId();
    ProcessDefinition processDefinition = query.latestVersion()
        .singleResult();

    if (processDefinition == null) {
      String errorMessage = String.format("No matching process definition with key: %s and tenant-id: %s", processDefinitionKey, tenantId);
      throw new SystemException(errorMessage);
    } else {
      StartProcessInstanceDto parameters = objectMapper.convertValue(data,  StartProcessInstanceDto.class);
      org.cibseven.bpm.engine.runtime.ProcessInstance instance = null;
      try {
        Map<String, Object> variables = VariableValueDto.toMap(parameters.getVariables(), processEngine, objectMapper);
        String businessKey = parameters.getBusinessKey();
        if (businessKey != null) {
          instance = formService.submitStartForm(processDefinition.getId(), businessKey, variables);
        } else {
          instance = formService.submitStartForm(processDefinition.getId(), variables);
        }
  
      } catch (AuthorizationException e) {
        throw e;
  
      } catch (FormFieldValidationException e) {
        String errorMessage = String.format("Cannot instantiate process definition %s: %s", processDefinition.getId(), e.getMessage());
        throw new SystemException(errorMessage, e);
  
      } catch (ProcessEngineException e) {
        String errorMessage = String.format("Cannot instantiate process definition %s: %s", processDefinition.getId(), e.getMessage());
        throw new SystemException(errorMessage, e);
  
      }
  
      ProcessInstanceDto result = ProcessInstanceDto.fromProcessInstance(instance);
  
      //TODO: getEngineRestUrl() will be a base class method after completion
      UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://localhost:8080");//getEngineRestUrl());
      URI uri = builder
        .path("/")//relativeRootResourcePath)
        .path("/process-instance")//ProcessInstanceRestService.PATH)
        .path(instance.getId())
        .build().toUri();
  
      result.addReflexiveLink(uri, HttpMethod.GET, "self");
  
      return objectMapper.convertValue(result, ProcessStart.class);
      
    }
  }
  
  //@Override
  public Collection<ProcessStatistics> findProcessStatistics(String processId, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
    ActivityStatisticsQuery query = managementService.createActivityStatisticsQuery(processId);
    List<ActivityStatistics> queryResults = query.unlimitedList();

    Collection<ProcessStatistics> processStatistics = new ArrayList<>();
    for (ActivityStatistics queryResult : queryResults) {
      StatisticsResultDto dto = ActivityStatisticsResultDto.fromActivityStatistics(queryResult);
      processStatistics.add(objectMapper.convertValue(dto, ProcessStatistics.class));
    }
    return processStatistics;
  }

  //@Override
  public Collection<HistoryProcessInstance> findProcessesInstancesHistory(Map<String, Object> filters,
      Optional<Integer> firstResult, Optional<Integer> maxResults, CIBUser user) {
    Boolean fetchIncidents = (Boolean) filters.get("fetchIncidents");
    if (fetchIncidents != null) {
      filters.remove("fetchIncidents");
    }
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
      historicProcessInstanceResults.add(convertValue(resultHistoricProcessInstanceDto, HistoryProcessInstance.class));
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
    //TODO: not tested
    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto = new HistoricProcessInstanceQueryDto();
    //historicProcessInstanceQueryDto.setProcessDefinitionId(id);
    historicProcessInstanceQueryDto.setProcessDefinitionKey(key);
    if (active.isPresent())
      historicProcessInstanceQueryDto.setActive(active.get());
    return queryHistoryProcessInstances(historicProcessInstanceQueryDto, firstResult, maxResults);
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
    return queryHistoryProcessInstances(historicProcessInstanceQueryDto, firstResult, maxResults);
  }
  
  private Collection<HistoryProcessInstance> queryHistoryProcessInstances(
      HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto, Integer firstResult, Integer maxResults){
    historicProcessInstanceQueryDto.setObjectMapper(objectMapper);
    
    HistoricProcessInstanceQuery query = historicProcessInstanceQueryDto.toQuery(processEngine);
    List<HistoricProcessInstance> matchingHistoricProcessInstances = QueryUtil.list(query, firstResult, maxResults);

    List<HistoryProcessInstance> HistoryProcessInstanceResults = new ArrayList<HistoryProcessInstance>();
    for (HistoricProcessInstance historicProcessInstance : matchingHistoricProcessInstances) {
      HistoricProcessInstanceDto resultHistoricProcessInstanceDto = HistoricProcessInstanceDto.fromHistoricProcessInstance(historicProcessInstance);
      HistoryProcessInstanceResults.add(convertValue(resultHistoricProcessInstanceDto, HistoryProcessInstance.class));
    }
    return HistoryProcessInstanceResults;
  }
  
  //@Override
  public Long countProcessesInstancesHistory(Map<String, Object> filters, CIBUser user) {
    HistoricProcessInstanceQueryDto historicProcessInstanceQueryDto = objectMapper.convertValue(filters, HistoricProcessInstanceQueryDto.class);  
    
    historicProcessInstanceQueryDto.setObjectMapper(objectMapper);
    HistoricProcessInstanceQuery query = historicProcessInstanceQueryDto.toQuery(processEngine);

    long count = query.count();
    return count;
  }
  
  //@Override
  public ProcessInstance findProcessInstance(String processInstanceId, CIBUser user) {
    //TODO: not tested
    org.cibseven.bpm.engine.runtime.ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
    if (instance == null) {
      throw new SystemException("Process instance with id " + processInstanceId + " does not exist");
    }

    ProcessInstanceDto result = ProcessInstanceDto.fromProcessInstance(instance);
    return convertValue(result, ProcessInstance.class);
  }

  //@Override
  public Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, boolean deserializeValue, CIBUser user) throws SystemException  {
    //TODO: not tested
    VariableInstanceQueryDto queryDto = new VariableInstanceQueryDto();
    queryDto.setProcessInstanceIdIn(new String[] {processInstanceId});
    queryDto.setVariableName(variableName);
    queryDto.setObjectMapper(objectMapper);

    List<Variable> variablesDeserialized = queryVariableInstances(queryDto, null, null, true);
    List<Variable> variablesSerialized = queryVariableInstances(queryDto, null, null, false);
    Variable variableDeserialized = variablesDeserialized.size() == 1 ? variablesDeserialized.get(0) : null;
    Variable variableSerialized = variablesSerialized.size() == 1 ? variablesSerialized.get(0) : null;
    
    if (deserializeValue) {
      variableDeserialized.setValueSerialized(variableSerialized.getValue());
      variableDeserialized.setValueDeserialized(variableDeserialized.getValue());
      return variableDeserialized;
    }
    else {
      variableSerialized.setValueSerialized(variableSerialized.getValue());
      variableSerialized.setValueDeserialized(variableDeserialized.getValue());
      return variableSerialized;
    }

  }
  
  //@Override
  public HistoryProcessInstance findHistoryProcessInstanceHistory(String processInstanceId, CIBUser user) {
    HistoricProcessInstance instance = 
        historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
    if (instance == null) {
      throw new SystemException("Historic process instance with id " + processInstanceId + " does not exist");
    }

    HistoryProcessInstance historyProcessInstance = convertValue(
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
      throw new SystemException(e.getMessage());
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
    //TODO: not tested
    try {
      InputStream deployedStartForm = formService.getDeployedStartForm(processDefinitionId);
      byte[] bytes = IOUtils.toByteArray(deployedStartForm);
      return new ResponseEntity<byte[]>(bytes, HttpStatusCode.valueOf(200));
    } catch (NotFoundException e) {
      throw new SystemException(e.getMessage());
    } catch (NullValueException e) {
      throw new SystemException(e.getMessage());
    } catch (AuthorizationException e) {
      throw new SystemException(e.getMessage());
    } catch (BadUserRequestException e) {
      throw new SystemException(e.getMessage());
    } catch (IOException e) {
      throw new SystemException(e.getMessage());
    }
  }

  //@Override
  public void updateHistoryTimeToLive(String id, Map<String, Object> data, CIBUser user) {
    //TODO: not tested
    HistoryTimeToLiveDto historyTimeToLiveDto = objectMapper.convertValue(data, HistoryTimeToLiveDto.class);
    repositoryService.updateProcessDefinitionHistoryTimeToLive(id, historyTimeToLiveDto.getHistoryTimeToLive());
  }

  //@Override
  public void deleteProcessInstanceFromHistory(String id, CIBUser user) {
    try {
      historyService.deleteHistoricProcessInstance(id);
    } catch (BadUserRequestException e) {
      throw new SystemException(e.getMessage());
    }
  }
  
  //@Override
  public void deleteProcessDefinition(String id, Optional<Boolean> cascade, CIBUser user) {
    //TODO: not tested
    boolean cascadeVal = cascade.orElse(true);
    try {
      repositoryService.deleteProcessDefinition(id, cascadeVal);
    } catch (NotFoundException nfe) {
      throw new SystemException(nfe.getMessage(), nfe);
    }
  }
  
  //@Override
  public Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user)
      throws SystemException {

    ProcessInstanceQueryDto queryDto = objectMapper.convertValue(data, ProcessInstanceQueryDto.class);
    queryDto.setObjectMapper(objectMapper);
    ProcessInstanceQuery query = queryDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.runtime.ProcessInstance> matchingInstances = QueryUtil.list(query, null, null);

    List<ProcessInstance> instanceResults = new ArrayList<>();
    for (org.cibseven.bpm.engine.runtime.ProcessInstance instance : matchingInstances) {
      ProcessInstanceDto resultInstance = ProcessInstanceDto.fromProcessInstance(instance);
      instanceResults.add(convertValue(resultInstance, ProcessInstance.class));
    }
    return instanceResults;
  }

  //@Override
  public Object fetchHistoricActivityStatistics(String id, Map<String, Object> params, CIBUser user) {
    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
    for (String key: params.keySet()) {
      queryParams.put(key, Arrays.asList((String)params.get(key)));
    }
    
    HistoricActivityStatisticsQueryDto historicActivityStatisticsQueryDto = new HistoricActivityStatisticsQueryDto(objectMapper, id, queryParams);  
    HistoricActivityStatisticsQuery query = historicActivityStatisticsQueryDto.toQuery(processEngine);
    List<HistoricActivityStatisticsDto> result = new ArrayList<>();
    List<HistoricActivityStatistics> statistics = query.unlimitedList();
    for (HistoricActivityStatistics currentStatistics : statistics) {
      result.add(HistoricActivityStatisticsDto.fromHistoricActivityStatistics(currentStatistics));
    }
    return result;
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
    for (org.cibseven.bpm.engine.filter.Filter filter : matchingFilters) {
      FilterDto filterDto = FilterDto.fromFilter(filter);
      //TODO: itemCount not used?
      // if (itemCount != null && itemCount) {
      // dto.setItemCount(filterService.count(filter.getId()));
      // }
      filters.add(convertValue(filterDto, Filter.class));
    }
    return filters;
  }

  //@Override
  public  Filter createFilter(Filter filter, CIBUser user) {
  //TODO: untested
    FilterDto filterDto = convertValue(filter, FilterDto.class); 
    String resourceType = filterDto.getResourceType();

    org.cibseven.bpm.engine.filter.Filter engineFilter;
    if (EntityTypes.TASK.equals(resourceType)) {
      engineFilter = filterService.newTaskFilter();
    }
    else {
      throw new SystemException("Unable to create filter with invalid resource type '" + resourceType + "'");
    }

    try {
      filterDto.updateFilter(engineFilter, processEngine);
    }
    catch (NotValidException e) {
      throw new SystemException("Unable to create filter with invalid content", e);
    }

    filterService.saveFilter(engineFilter);

    Filter resultFilter = convertValue(FilterDto.fromFilter(engineFilter), Filter.class);
    return resultFilter;
  }
  //@Override
  public void updateFilter(Filter filter, CIBUser user) {
  //TODO: untested
    FilterDto filterDto = convertValue(filter, FilterDto.class); 
    org.cibseven.bpm.engine.filter.Filter dbFilter = filterService.getFilter(filter.getId());

    if (dbFilter == null) {
      throw new SystemException("Requested filter not found: " + filter.getId());
    }

    try {
      filterDto.updateFilter(dbFilter, processEngine);
    }
    catch (NotValidException e) {
      throw new SystemException("Unable to update filter with invalid content", e);
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
      throw new SystemException("Requested filter not found: " + filterId);
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
    file.forEach((key, value) -> { 
      try {
        data.add(key, value.get(0).getResource());
      } catch (Exception e) {
        throw new SystemException(e);
      }
    });
    DeploymentBuilder deploymentBuilder = extractDeploymentInformation(convertValue(data, MultipartFormData.class));

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
      return convertValue(deploymentDto, Deployment.class);

    } else {
      throw new SystemException("No deployment resources contained in the form upload.");
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
          throw new SystemException("No file name found in the deployment resource described by form parameter '" + fileName + "'.");
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
      deployments.add(convertValue(def, Deployment.class));
    }
    return deployments;
  }
  
  //@Override
  public Deployment findDeployment(String deploymentId, CIBUser user) {
    org.cibseven.bpm.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

    if (deployment == null) {
      throw new SystemException("Deployment with id '" + deploymentId + "' does not exist");
    }

    return convertValue(DeploymentDto.fromDeployment(deployment), Deployment.class);
  }

  //@Override
  public Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user) {
    
    List<Resource> resources = repositoryService.getDeploymentResources(deploymentId);

    List<DeploymentResource> deploymentResources = new ArrayList<DeploymentResource>();
    for (Resource resource : resources) {
      deploymentResources.add(convertValue(DeploymentResourceDto.fromResources(resource), DeploymentResource.class));
    }

    if (!deploymentResources.isEmpty()) {
      return deploymentResources;
    }
    else {
      throw new SystemException("Deployment resources for deployment id '" + deploymentId + "' do not exist.");
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
        throw new SystemException("Deployment resource '" + resourceId + "' for deployment id '" + deploymentId + "'could not be read.");
      }
    } else {
      throw new SystemException("Deployment resource '" + resourceId + "' for deployment id '" + deploymentId + "' does not exist.");
    }
  }
  
  private DeploymentResourceDto getDeploymentResource(String resourceId, String deploymentId) {
    List<DeploymentResourceDto> deploymentResources = getDeploymentResources(deploymentId);
    for (DeploymentResourceDto deploymentResource : deploymentResources) {
      if (deploymentResource.getId().equals(resourceId)) {
        return deploymentResource;
      }
    }
  
    throw new SystemException("Deployment resource with resource id '" + resourceId + "' for deployment id '" + deploymentId + "' does not exist.");
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
      throw new SystemException("Deployment resources for deployment id '" + deploymentId + "' do not exist.");
    }
  }
  
  //@Override
  public void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException {
    org.cibseven.bpm.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
    if (deployment == null) {
      throw new SystemException("Deployment with id '" + deploymentId + "' do not exist");
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
     throw new SystemException(e.getMessage(), e);
   }

   if (activityInstance == null) {
     throw new SystemException("Process instance with id " + processInstanceId + " does not exist");
   }

   ActivityInstanceDto result = ActivityInstanceDto.fromActivityInstance(activityInstance);
   return convertValue(result, ActivityInstance.class);
 }
 
 //@Override
 public List<ActivityInstanceHistory> findActivitiesInstancesHistory(Map<String, Object> queryParams, CIBUser user) {
   //TODO: not tested
   HistoricActivityInstanceQueryDto queryHistoricActivityInstanceDto = objectMapper.convertValue(queryParams, HistoricActivityInstanceQueryDto.class);
   return queryHistoricActivityInstance(queryHistoricActivityInstanceDto);

 }
 
 //@Override
 public List<ActivityInstanceHistory> findActivitiesInstancesHistory(String processInstanceId, CIBUser user) {
   HistoricActivityInstanceQueryDto queryHistoricActivityInstanceDto = new HistoricActivityInstanceQueryDto();
   queryHistoricActivityInstanceDto.setProcessInstanceId(processInstanceId);
   return queryHistoricActivityInstance(queryHistoricActivityInstanceDto);
 }
 
 private List<ActivityInstanceHistory> queryHistoricActivityInstance(HistoricActivityInstanceQueryDto queryHistoricActivityInstanceDto) {
   queryHistoricActivityInstanceDto.setObjectMapper(objectMapper);
   HistoricActivityInstanceQuery query = queryHistoricActivityInstanceDto.toQuery(processEngine);
   List<HistoricActivityInstance> matchingHistoricActivityInstances = QueryUtil.list(query, null, null);

   List<ActivityInstanceHistory> historicActivityInstanceResults = new ArrayList<>();
   for (HistoricActivityInstance historicActivityInstance : matchingHistoricActivityInstances) {
     HistoricActivityInstanceDto resultHistoricActivityInstance = new HistoricActivityInstanceDto();
     HistoricActivityInstanceDto.fromHistoricActivityInstance(resultHistoricActivityInstance, historicActivityInstance);
     historicActivityInstanceResults
         .add(convertValue(resultHistoricActivityInstance, ActivityInstanceHistory.class));
   }
   return historicActivityInstanceResults;
 }
 
 //@Override
 public ActivityInstance findActivityInstances(String processInstanceId, CIBUser user) throws SystemException {

   org.cibseven.bpm.engine.runtime.ActivityInstance activityInstance = null;

   try {
     activityInstance = runtimeService.getActivityInstance(processInstanceId);
   } catch (AuthorizationException e) {
     throw e;
   } catch (ProcessEngineException e) {
     throw new SystemException(e.getMessage(), e);
   }

   if (activityInstance == null) {
     throw new SystemException("Process instance with id " + processInstanceId + " does not exist");
   }

   ActivityInstanceDto result = ActivityInstanceDto.fromActivityInstance(activityInstance);
   return convertValue(result, ActivityInstance.class);
 }
 
 //@Override
 public List<ActivityInstanceHistory> findActivityInstanceHistory(String processInstanceId, CIBUser user) throws SystemException {

   HistoricActivityInstanceQueryDto queryHistoricActivityInstanceDto = new HistoricActivityInstanceQueryDto();
   queryHistoricActivityInstanceDto.setProcessInstanceId(processInstanceId);
   return queryHistoricActivityInstance(queryHistoricActivityInstanceDto);
 } 

 //@Override
 public void deleteVariableByExecutionId(String executionId, String variableName, CIBUser user) {
   //TODO: not tested
   try {
     runtimeService.removeVariableLocal(executionId, variableName);
   } catch (AuthorizationException e) {
     throw e;
   } catch (ProcessEngineException e) {
     String errorMessage = String.format("Cannot delete %s variable %s: %s", executionId, variableName, e.getMessage());
     throw new SystemException(errorMessage, e);
   }

 }

 //@Override
 public void deleteVariableHistoryInstance(String id, CIBUser user) {
   try {
     historyService.deleteHistoricVariableInstance(id);
   } catch (NotFoundException nfe) { // rewrite status code from bad request (400) to not found (404)
     throw new SystemException(nfe.getMessage(), nfe);
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
    // TODO: not tested
    CorrelationMessageDto messageDto = objectMapper.convertValue(data, CorrelationMessageDto.class);
    if (messageDto.getMessageName() == null) {
      throw new SystemException("No message name supplied");
    }
    if (messageDto.getTenantId() != null && messageDto.isWithoutTenantId()) {
      throw new SystemException("Parameter 'tenantId' cannot be used together with parameter 'withoutTenantId'.");
    }
    boolean variablesInResultEnabled = messageDto.isVariablesInResultEnabled();
    if (!messageDto.isResultEnabled() && variablesInResultEnabled) {
      throw new SystemException(
          "Parameter 'variablesInResultEnabled' cannot be used without 'resultEnabled' set to true.");
    }

    List<MessageCorrelationResultDto> resultDtos = new ArrayList<>();
    try {
      MessageCorrelationBuilder correlation = createMessageCorrelationBuilder(messageDto);
      if (!variablesInResultEnabled) {
        resultDtos.addAll(correlate(messageDto, correlation));
      } else {
        resultDtos.addAll(correlateWithVariablesEnabled(messageDto, correlation));
      }
    } catch (RestException e) {
      String errorMessage = String.format("Cannot deliver message: %s", e.getMessage());
      throw new SystemException(errorMessage, e);

    } catch (MismatchingMessageCorrelationException e) {
      throw new SystemException(e);
    }
    List<Message> messageList = new ArrayList<>();
    if (messageDto.isResultEnabled()) {
      for (MessageCorrelationResultDto resultDto : resultDtos) {
        messageList.add(convertValue(resultDto, Message.class));
      }
    }
    return messageList;
  }

  private List<MessageCorrelationResultDto> correlate(CorrelationMessageDto messageDto, MessageCorrelationBuilder correlation) {
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

  private List<MessageCorrelationResultWithVariableDto> correlateWithVariablesEnabled(CorrelationMessageDto messageDto, MessageCorrelationBuilder correlation) {
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

  private MessageCorrelationBuilder createMessageCorrelationBuilder(CorrelationMessageDto messageDto) {
    RuntimeService runtimeService = processEngine.getRuntimeService();

    Map<String, Object> correlationKeys = VariableValueDto.toMap(messageDto.getCorrelationKeys(), processEngine, objectMapper);
    Map<String, Object> localCorrelationKeys = VariableValueDto.toMap(messageDto.getLocalCorrelationKeys(), processEngine, objectMapper);
    Map<String, Object> processVariables = VariableValueDto.toMap(messageDto.getProcessVariables(), processEngine, objectMapper);
    Map<String, Object> processVariablesLocal = VariableValueDto.toMap(messageDto.getProcessVariablesLocal(), processEngine, objectMapper);
    Map<String, Object> processVariablesToTriggeredScope = VariableValueDto.toMap(messageDto.getProcessVariablesToTriggeredScope(), processEngine, objectMapper);

    MessageCorrelationBuilder builder = runtimeService
        .createMessageCorrelation(messageDto.getMessageName());

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
      for (java.util.Map.Entry<String, Object> correlationKey  : correlationKeys.entrySet()) {
        String name = correlationKey.getKey();
        Object value = correlationKey.getValue();
        builder.processInstanceVariableEquals(name, value);
      }
    }

    if (localCorrelationKeys != null && !localCorrelationKeys.isEmpty()) {
      for (java.util.Map.Entry<String, Object> correlationKey  : localCorrelationKeys.entrySet()) {
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
  //@Override
  public String findStacktrace(String jobId, CIBUser user) {
    //TODO: not tested
    try {
      String stacktrace = managementService.getJobExceptionStacktrace(jobId);
      return stacktrace;
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new SystemException(e.getMessage());
    }
  }
  
  //@Override
  public String findExternalTaskErrorDetails(String externalTaskId, CIBUser user) {
  //untested
    try {
      return externalTaskService.getExternalTaskErrorDetails(externalTaskId);
    } catch (NotFoundException e) {
      throw new SystemException("External task with id " + externalTaskId + " does not exist", e);
    }
  }
  
  //@Override
  public String findHistoricExternalTaskErrorDetails(String externalTaskId, CIBUser user) {
    try {
      return historyService.getHistoricExternalTaskLogErrorDetails(externalTaskId);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new SystemException(e.getMessage());
    }
  }
  
  //@Override
  public Collection<Incident> findHistoricIncidents(Map<String, Object> params, CIBUser user) {
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
      Incident incident = convertValue(incidentDto, Incident.class);
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
   //TODO: not tested
    try {
      String stacktrace = historyService.getHistoricJobLogExceptionStacktrace(jobId);
      return stacktrace;
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new SystemException(e.getMessage());
    }
  }
  
  //@Override
  public void retryJobById(String jobId, Map<String, Object> data, CIBUser user) {
   //TODO: not tested
    RetriesDto dto = objectMapper.convertValue(data, RetriesDto.class);
    try {
      SetJobRetriesBuilder builder = managementService
          .setJobRetries(dto.getRetries())
          .jobId(jobId);
      if (dto.isDueDateSet()) {
        builder.dueDate(dto.getDueDate());
      }
      builder.execute();
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new SystemException(e.getMessage());
    }
}

  //@Override
  public void retryExternalTask(String externalTaskId, Map<String, Object> data, CIBUser user) {
   //TODO: not tested
    RetriesDto dto = objectMapper.convertValue(data, RetriesDto.class);
    Integer retries = dto.getRetries();

    if (retries == null) {
      throw new SystemException("The number of retries cannot be null.");
    }

    try {
      externalTaskService.setRetries(externalTaskId, retries);
    } catch (NotFoundException e) {
      throw new SystemException("External task with id " + externalTaskId + " does not exist", e);
    }
  }

  //@Override
  public Collection<EventSubscription> getEventSubscriptions(Optional<String> processInstanceId,
      Optional<String> eventType, Optional<String> eventName, CIBUser user) {
   //TODO: not tested
    EventSubscriptionQueryDto queryDto = new EventSubscriptionQueryDto();
    queryDto.setObjectMapper(objectMapper);
    if (processInstanceId.isPresent())
      queryDto.setProcessInstanceId(processInstanceId.get());
    if (eventType.isPresent())
      queryDto.setEventType(eventType.get());
    if (eventName.isPresent())
      queryDto.setEventName(eventName.get());
    EventSubscriptionQuery query = queryDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.runtime.EventSubscription> matchingEventSubscriptions = QueryUtil.list(query, null, null);

    List<EventSubscription> eventSubscriptionResults = new ArrayList<>();
    for (org.cibseven.bpm.engine.runtime.EventSubscription eventSubscription : matchingEventSubscriptions) {
      EventSubscriptionDto resultEventSubscription = EventSubscriptionDto.fromEventSubscription(eventSubscription);
      eventSubscriptionResults.add(convertValue(resultEventSubscription, EventSubscription.class));
    }
    return eventSubscriptionResults;
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
    queryDto.setObjectMapper(objectMapper);
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
    groupIdQueryDto.setObjectMapper(objectMapper);
    AuthorizationQuery groupIdQuery = groupIdQueryDto.toQuery(processEngine);
    //expected: 51 authorizations with id, type, userid, groupId, resourceType, resourceId
    List<org.cibseven.bpm.engine.authorization.Authorization> groupIdResultList = QueryUtil.list(groupIdQuery, null, null);
    Collection<Authorization> groupsAuthorizations = createAuthorizationCollection(groupIdResultList);

    AuthorizationQueryDto globalIdQueryDto = new AuthorizationQueryDto();
    globalIdQueryDto.setType(0);
    globalIdQueryDto.setObjectMapper(objectMapper);
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
    queryDto.setObjectMapper(objectMapper);
    UserQuery query = queryDto.toQuery(processEngine);
    query.userId(user.getId());
    List<org.cibseven.bpm.engine.identity.User> resultList = QueryUtil.list(query, null, null);

    Collection<SevenUser> userCollection = createSevenUsers(resultList); 
    return userCollection;
  }
  
  public SevenVerifyUser verifyUser(String username, String password, CIBUser user) throws SystemException {
    if ((username == null || username.isBlank())|| (password == null || password.isBlank()))
      throw new SystemException("Username and password are required");
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
    queryDto.setObjectMapper(objectMapper);
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
      throw new SystemException("Identity service implementation is read-only.");
    }

    org.cibseven.bpm.engine.identity.User dbUser = findUserObject(user.getId());
    if(dbUser == null) {
      throw new SystemException("User with id " + user.getId() + " does not exist");
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
      throw new SystemException("Exception while performing user query: " + e.getMessage());
    }
    return dbUser;
  }

  private Group findGroupObject(String groupId) {
    try {
      return identityService.createGroupQuery()
          .groupId(groupId)
          .singleResult();
    } catch(ProcessEngineException e) {
      throw new SystemException("Exception while performing group query: "+ e.getMessage());
    }
  }
  
  //TODO: not tested, UI seems to have no function to change password without sending mails before
  //@Override
  public void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user) {
    if(identityService.isReadOnly()) {
      throw new SystemException("Identity service implementation is read-only.");
    }
    Authentication currentAuthentication = identityService.getCurrentAuthentication();
    if(currentAuthentication != null && currentAuthentication.getUserId() != null) {
      if(!identityService.checkPassword(currentAuthentication.getUserId(), (String)data.get("authenticatedUserPassword"))) {
        throw new SystemException("The given authenticated user password is not valid.");
      }
    }

    org.cibseven.bpm.engine.identity.User dbUser = findUserObject(userId);
    if(dbUser == null) {
      throw new SystemException("User with id " + user.getId() + " does not exist");
    }

    dbUser.setPassword((String)data.get("password"));
    identityService.saveUser(dbUser);
  }
  
  //@Override
  public void addMemberToGroup(String groupId, String userId, CIBUser user) {
    if(identityService.isReadOnly()) {
      throw new SystemException("Identity service implementation is read-only.");
    }
    identityService.createMembership(userId, groupId);
  }
  
  //@Override
  public void deleteMemberFromGroup(String groupId, String userId, CIBUser user) {
    if(identityService.isReadOnly()) {
      throw new SystemException("Identity service implementation is read-only.");
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
    queryDto.setObjectMapper(objectMapper);
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
      throw new SystemException("Identity service implementation is read-only.");
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
      throw new SystemException("Identity service implementation is read-only.");
    }

    Group dbGroup = findGroupObject(groupId);
    if(dbGroup == null) {
      throw new SystemException("Group with id " + groupId + " does not exist");
    }

    dbGroup.setId(group.getId());
    dbGroup.setName(group.getName());
    dbGroup.setType(group.getType());

    identityService.saveGroup(dbGroup);
  }

  //@Override
  public void deleteGroup(String groupId, CIBUser user) {
    if(identityService.isReadOnly()) {
      throw new SystemException("Identity service implementation is read-only.");
    }
    identityService.deleteGroup(groupId);
  }

  //@Override
  public Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type, Optional<String> userIdIn, Optional<String> groupIdIn,
      Optional<String> resourceType, Optional<String> resourceId, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
      Optional<String> maxResults, CIBUser user) {
    //TODO: not tested
    AuthorizationQueryDto queryDto = new AuthorizationQueryDto();
    queryDto.setObjectMapper(objectMapper);
    if (id.isPresent())
      queryDto.setId(id.get());
    if (type.isPresent())
      queryDto.setType(Integer.parseInt(type.get()));
    if (userIdIn.isPresent())
      queryDto.setUserIdIn(new String[]{userIdIn.get()});
    if (groupIdIn.isPresent())
      queryDto.setGroupIdIn(new String[]{groupIdIn.get()});
    if (resourceType.isPresent())
      queryDto.setResourceType(Integer.parseInt(resourceType.get()));
    if (resourceId.isPresent())
      queryDto.setResourceId(resourceId.get());
    if (sortOrder.isPresent())
      queryDto.setSortOrder(sortOrder.get());
    if (sortOrder.isPresent())
      queryDto.setSortOrder(sortOrder.get());
    Integer firstResultParam = null;
    if (firstResult.isPresent())
      firstResultParam = Integer.parseInt(firstResult.get());
    Integer maxResultsParam = null;
    if (maxResults.isPresent())
      maxResultsParam = Integer.parseInt(maxResults.get());
    return queryAuthorizations(queryDto, firstResultParam, maxResultsParam);
  }

  private List<Authorization> queryAuthorizations(AuthorizationQueryDto queryDto, Integer firstResult, Integer maxResults) {
    //TODO: not tested
    queryDto.setObjectMapper(objectMapper);
    AuthorizationQuery query = queryDto.toQuery(processEngine);
  
    List<org.cibseven.bpm.engine.authorization.Authorization> resultList = QueryUtil.list(query, firstResult, maxResults);
    List<AuthorizationDto> authorizationDtoList = AuthorizationDto.fromAuthorizationList(resultList, processEngine.getProcessEngineConfiguration());
    List<Authorization> authorizationList = new ArrayList<>();
    for (AuthorizationDto authorizationDto : authorizationDtoList) {
      authorizationList.add(convertValue(authorizationDto, Authorization.class));
    }
    return authorizationList;
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
      throw new SystemException("Authorization with id " + authorizationId + " does not exist.");
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
      throw new SystemException("No matching task with id " + taskId);
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
    //TODO: not tested
    IncidentQueryDto queryDto = objectMapper.convertValue(params, IncidentQueryDto.class);
    IncidentQuery query = queryDto.toQuery(processEngine);
    return query.count();
  }

  //@Override
  public Collection<Incident> findIncident(Map<String, Object> params, CIBUser user) {
    //TODO: not tested
    IncidentQueryDto queryDto = objectMapper.convertValue(params, IncidentQueryDto.class);
    IncidentQuery query = queryDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.runtime.Incident> queryResult = QueryUtil.list(query, null, null);

    List<Incident> incidents = new ArrayList<>();
    for (org.cibseven.bpm.engine.runtime.Incident incident : queryResult) {
      IncidentDto dto = IncidentDto.fromIncident(incident);
      incidents.add(convertValue(dto, Incident.class));
    }

    for (Incident incident : incidents) {
      if (incident.getId() != null && incident.getRootCauseIncidentId() != null 
          && !incident.getId().equals(incident.getRootCauseIncidentId())) {
        try {
          // Fetch the root cause incident
          Incident rootCauseIncident = fetchIncidentById(incident.getRootCauseIncidentId());
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
          log.warn("Failed to enrich incident with ID: {} and root cause ID: {}", 
            incident.getId(), 
            incident.getRootCauseIncidentId(), 
            e);
        }
      }
    }
    return incidents;
  }
    
  private Incident fetchIncidentById(String incidentId) {
    org.cibseven.bpm.engine.runtime.Incident incident = runtimeService.createIncidentQuery().incidentId(incidentId).singleResult();
    if (incident == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "No matching incident with id " + incidentId);
    }
    return convertValue(IncidentDto.fromIncident(incident), Incident.class);

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
      result.add(convertValue(dto, Incident.class));
    }

    return result;
  
  }
  
  //@Override
  public void setIncidentAnnotation(String incidentId, Map<String, Object> data, CIBUser user) {
    //TODO: not tested
    AnnotationDto annotationDto = objectMapper.convertValue(data, AnnotationDto.class);
    runtimeService.setAnnotationForIncidentById(incidentId, annotationDto.getAnnotation());
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
      VariableInstance variableInstance = convertValue(variableEngineInstance, VariableInstance.class);
        // return transformToDto(variableInstance);
        return variableInstance;
    } else {
      throw new SystemException("Variable with Id '" + id + "' does not exist.");
    }
  }
  

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
      VariableHistory result = convertValue(HistoricVariableInstanceDto.fromHistoricVariableInstance(variableInstance), VariableHistory.class);
      return result;
    } else {
      throw new SystemException(" historic variable with Id '" + id + "' does not exist.");
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
    //TODO: in progress
    ExternalTaskQueryDto queryDto = objectMapper.convertValue(queryParams, ExternalTaskQueryDto.class);
    queryDto.setObjectMapper(objectMapper);
    ExternalTaskQuery query = queryDto.toQuery(processEngine);
    List<org.cibseven.bpm.engine.externaltask.ExternalTask> matchingTasks = QueryUtil.list(query, null, null);

    List<ExternalTask> taskResults = new ArrayList<>();
    for (org.cibseven.bpm.engine.externaltask.ExternalTask task : matchingTasks) {
      ExternalTaskDto resultInstance = ExternalTaskDto.fromExternalTask(task);
      taskResults.add(convertValue(resultInstance, ExternalTask.class));
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
    //TODO: not tested
    PatchVariablesDto patch = objectMapper.convertValue(data, PatchVariablesDto.class);  
    VariableMap variableModifications = null;
    try {
      variableModifications = VariableValueDto.toMap(patch.getModifications(), processEngine, objectMapper);

    } catch (RestException e) {
      String errorMessage = String.format("Cannot modify variables for %s: %s", "modifyVariableByExecutionId", e.getMessage());
      throw new SystemException(errorMessage, e);
    }

    List<String> variableDeletions = patch.getDeletions();
    try {
      ((RuntimeServiceImpl) runtimeService).updateVariables(executionId, variableModifications, variableDeletions);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot modify variables for %s %s: %s", "modifyVariableByExecutionId", executionId, e.getMessage());
      throw new SystemException(errorMessage, e);
    }
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
        VariableValueDto valueDto = createVariableValueDto(valueType, data);
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
      throw new SystemException("Could not deserialize JSON object: "+e.getMessage());
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
          throw new SystemException("The following classes are not whitelisted for deserialization: " + invalidTypes);
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

    VariableInstanceQueryDto queryDto = objectMapper.convertValue(data, VariableInstanceQueryDto.class);
    
    queryDto.setObjectMapper(objectMapper);
    
    List<Variable> variablesDeserialized = queryVariableInstances(queryDto, null, null, true);
    if ( variablesDeserialized.isEmpty())
      return Collections.emptyList();
    List<Variable> variablesSerialized = queryVariableInstances(queryDto, null, null, false);
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
      VariableInstanceQueryDto queryDto, Integer firstResult, Integer maxResults, boolean deserializeObjectValues) {  
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
      VariableHistory resultInstance = convertValue(resultInstanceDto, VariableHistory.class);
      instanceResults.add(resultInstance);
    }
    return instanceResults;
  }
  

  
  // @Override
  public ResponseEntity<byte[]> fetchVariableDataByExecutionId(String executionId, String variableName, CIBUser user)
      throws NoObjectFoundException, SystemException {
    //TODO: not tested
    TypedValue typedVariableValue = runtimeService.getVariableLocalTyped(executionId, variableName, false);
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
        //TODO exception type
        throw new SystemException(e.getMessage(), e);
      }
    } else {
      throw new SystemException(String.format("Value of variable with id %s is not a binary value.", id));
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

    ObjectMapper localObjectMapper = new ObjectMapper();
    JacksonConfigurator.configureObjectMapper(objectMapper);
    //TODO: data contains "variableValues=null" which is not member of HistoricVariableInstanceQueryDto
    //but there is "variableValue" -> create ticket
    localObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    HistoricVariableInstanceQueryDto queryDto = localObjectMapper.convertValue(data, HistoricVariableInstanceQueryDto.class);
    
    queryDto.setObjectMapper(localObjectMapper);
    
    List<VariableHistory> variablesDeserialized = queryHistoricVariableInstances(queryDto, localObjectMapper, null, null, true);
//[VariableHistory(id=2b38760a-a35a-11f0-afe5-4ce1734f67af, name=testBool, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b38760c-a35a-11f0-afe5-4ce1734f67af, name=datetime_fr4cbr, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b389d1e-a35a-11f0-afe5-4ce1734f67af, name=season, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b389d20-a35a-11f0-afe5-4ce1734f67af, name=guestCount, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null), VariableHistory(id=2b389d22-a35a-11f0-afe5-4ce1734f67af, name=number_double, processDefinitionKey=null, processDefinitionId=Process_CalcDish:3:b5fcf7f6-7e60-11f0-8785-4ce1734f67af, processInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, executionId=8a620626-a356-11f0-afe5-4ce1734f67af, activityInstanceId=8a620626-a356-11f0-afe5-4ce1734f67af, caseDefinitionKey=null, caseDefinitionId=null, caseInstanceId=null, caseExecutionId=null, taskId=null, errorMessage=null, tenantId=null, state=null, createTime=null, removalTime=null, rootProcessInstanceId=null)]    
    if ( variablesDeserialized.isEmpty())
      return Collections.emptyList();
    List<VariableHistory> variablesSerialized = queryHistoricVariableInstances(queryDto, localObjectMapper, null, null, false);
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
      VariableHistory resultInstance = convertValue(resultInstanceDto, VariableHistory.class);
      instanceResults.add(resultInstance);
    }
    return instanceResults;
  }
  
  //@Override
  public Collection<VariableHistory> fetchActivityVariablesHistory(String activityInstanceId, CIBUser user) {
    //TODO: not tested
    HistoricVariableInstanceQueryDto queryDto = new HistoricVariableInstanceQueryDto();
    queryDto.setObjectMapper(objectMapper);
    return queryHistoricVariableInstances(queryDto, null, null, true);
  }

  private List<VariableHistory> queryHistoricVariableInstances(HistoricVariableInstanceQueryDto queryDto,
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
      HistoricVariableInstanceDto resultHistoricVariableInstance = HistoricVariableInstanceDto.fromHistoricVariableInstance(historicVariableInstance);
      historicVariableInstanceDtoResults.add(convertValue(resultHistoricVariableInstance, VariableHistory.class));
    }
    return historicVariableInstanceDtoResults;
  }
  
  //@Override
  public Collection<VariableHistory> fetchActivityVariables(String activityInstanceId, CIBUser user) {
    //TODO: not tested
    //TODO: method returns VariableHistory without acessing history data: "/variable-instance" 
    VariableInstanceQueryDto queryDto = new VariableInstanceQueryDto();
    queryDto.setObjectMapper(objectMapper);
    queryDto.setActivityInstanceIdIn(new String[] {activityInstanceId});
    List<Variable> variableInstances = queryVariableInstances(queryDto, null, null, true);
    List<VariableHistory> historyVariables = new ArrayList<>();
    for(Variable variableInstance : variableInstances) {
      historyVariables.add(convertValue(variableInstance, VariableHistory.class));
    }
    return historyVariables;
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
      throw new SystemException("HistoryVariable with Id '" + id + "' does not exist.");
    }
  }
  
  //@Override
  public Variable fetchVariable(String taskId, String variableName, 
      boolean deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException {    
    //TODO: in progress
    Variable variableSerialized = fetchTaskVariableImpl(taskId, variableName, false, user);
    Variable variableDeserialized = fetchTaskVariableImpl(taskId, variableName, true, user);

    if (deserializeValue) {
      variableDeserialized.setValueSerialized(variableSerialized.getValue());
      variableDeserialized.setValueDeserialized(variableDeserialized.getValue());
      return variableDeserialized;
    }
    else {
      variableSerialized.setValueSerialized(variableSerialized.getValue());
      variableSerialized.setValueDeserialized(variableDeserialized.getValue());
      return variableSerialized;
    }
  }
  
  private Variable fetchTaskVariableImpl(String taskId, String variableName, 
      boolean deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException {    
    //TODO: not tested
    TypedValue value = getTypedValueForTaskVariable(taskId, variableName, deserializeValue);
    return convertValue(VariableValueDto.fromTypedValue(value), Variable.class);
  }

  private TypedValue getTypedValueForTaskVariable(String taskId, String variableName, boolean deserializeValue) {
    TypedValue value = null;
    try {
       value = taskService.getVariableTyped(taskId, variableName, deserializeValue);
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
  
  //@Override
  public void deleteVariable(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException {   
    //TODO: not tested
    try {
      taskService.removeVariable(taskId, variableName);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot delete %s variable %s: %s", "task", variableName, e.getMessage());
      throw new SystemException(errorMessage, e);
    }
  }
  
  //@Override
  public Map<String, Variable> fetchFormVariables(String taskId, boolean deserializeValues, CIBUser user) 
        throws NoObjectFoundException, SystemException {
    //TODO: not tested
    return fetchFormVariables(null, taskId, user);
  }
  
  //@Override
  public Map<String, Variable> fetchFormVariables(List<String> variableListName, String taskId, CIBUser user) 
       throws NoObjectFoundException, SystemException {
    //TODO: not tested
    VariableMap startFormVariables = formService.getTaskFormVariables(taskId, variableListName, true);
    Map<String, VariableValueDto> variableDtos = VariableValueDto.fromMap(startFormVariables);
    Map<String, Variable> variablesMap = new HashMap<>();
    for (Entry<String, VariableValueDto> e : variableDtos.entrySet()) {
      variablesMap.put(e.getKey(), convertValue(e.getValue(), Variable.class));
    }
    return variablesMap;
  }
  
  //@Override
  public Map<String, Variable> fetchProcessFormVariables(String key, CIBUser user) throws NoObjectFoundException, SystemException {
    //TODO: not tested
    List<String> formVariables = null;

//    if(variableNames != null) {
//      StringListConverter stringListConverter = new StringListConverter();
//      formVariables = stringListConverter.convertQueryParameterToType(variableNames);
//    }
    ProcessDefinition processDefinition = processEngine
        .getRepositoryService()
        .createProcessDefinitionQuery()
        .processDefinitionKey(key)
        .withoutTenantId()
        .latestVersion()
        .singleResult();

    if(processDefinition == null){
      String errorMessage = String.format("No matching process definition with key: %s and no tenant-id", key);
      throw new SystemException(errorMessage);

    }

    VariableMap startFormVariables = formService.getStartFormVariables(processDefinition.getId(), formVariables, true);
    Map<String, VariableValueDto> variableDtos = VariableValueDto.fromMap(startFormVariables);
    Map<String, Variable> variablesMap = new HashMap<>();
    for (Entry<String, VariableValueDto> e : variableDtos.entrySet()) {
      variablesMap.put(e.getKey(), convertValue(e.getValue(), Variable.class));
    }
    return variablesMap;
  }
  
  //@Override
  public NamedByteArrayDataSource fetchVariableFileData(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, UnexpectedTypeException, SystemException {   
    //TODO: not tested
    try {
      byte[] data = null;
      String filename = null;
      String mimeType = null;
        
      Variable variable = fetchVariable(taskId, variableName, true, user);
      String objectType = variable.getValueInfo().get("objectTypeName");
      if (objectType != null) {
        try {
          Class<?> clazz =  Class.forName(objectType);

          if (DataSource.class.isAssignableFrom(clazz)) {
            @SuppressWarnings("unchecked")
            DataSource ds = objectMapper.convertValue(variable.getValue(), (Class<? extends DataSource>) clazz);

            return new NamedByteArrayDataSource(ds.getName(), ds.getContentType(),
                IOUtils.toByteArray(ds.getInputStream()));
          }
        } catch (ClassNotFoundException e) {
          log.info("Class " + objectType + " could not be loaded!");
        }
      }

      filename = variable.getFilename();
      mimeType = variable.getMimeType();

      TypedValue typedVariableValue = getTypedValueForTaskVariable(taskId, variableName, true);
      //VariableValueDto dto = VariableValueDto.fromTypedValue(value);
      if (typedVariableValue instanceof BytesValue || ValueType.BYTES.equals(typedVariableValue.getType())) {
        data = (byte[]) typedVariableValue.getValue();
        if (data == null) {
          data = new byte[0];
        }
      } else if (ValueType.FILE.equals(typedVariableValue.getType())) {
        FileValue typedFileValue = (FileValue) typedVariableValue;
        try {
          data = typedFileValue.getValue() == null ? null : IOUtils.toByteArray(typedFileValue.getValue());
          // status code if bytes==null?
        } catch (IOException e) {
          throw new SystemException(e.getMessage(), e);
        }
      } else {
        throw new SystemException(String.format("Value of variable with id %s is not a binary value.", variableName));
      }
      
      return new NamedByteArrayDataSource(filename, mimeType, data);
    } catch (HttpStatusCodeException e) {
      //TODO: wrapException will be provided by the base class
      //throw wrapException(e, user);
      throw new SystemException(e.getMessage());
    } catch (IOException e) {
      throw new SystemException(e);
    }
  }
  
  //@Override
  public void uploadVariableFileData(String taskId, String variableName, MultipartFile data, String valueType, CIBUser user) 
    throws NoObjectFoundException, SystemException {
    //TODO: not tested
    try {
      setBinaryVariable(data, valueType, null, taskId, null, variableName);
    } catch (HttpStatusCodeException e) {
      //TODO: wrapException will be provided by the base class
      //throw wrapException(e, user);
      throw new SystemException(e.getMessage());
    } catch (IOException e) {
      throw new SystemException(e.getMessage());
    }
  }
  
  /*
    puts variable to different targets depending on taskId, processInstanceId, ...
  */
  private void setBinaryVariable(MultipartFile data, String valueType, String objectType, 
    String taskId, String processInstanceId, String variableName) throws IOException {
    if(objectType != null) {
      Object object = null;

      if(data.getContentType()!=null
          && data.getContentType().toLowerCase().contains(MediaType.APPLICATION_JSON.toString())) {

        byte[] bytes = IOUtils.toByteArray(data.getResource().getInputStream());
        object = deserializeJsonObject(objectType, bytes);

      } else {
        throw new SystemException("Unrecognized content type for serialized java type: "+ data.getContentType());
      }

      if(object != null) {
        if (taskId != null)
          taskService.setVariable(taskId, variableName, Variables.objectValue(object).create());
        else if (processInstanceId != null)
          runtimeService.setVariable(processInstanceId, variableName, Variables.objectValue(object).create());
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
        taskService.setVariable(taskId, variableName, typedValue);
        else if (processInstanceId != null)
          runtimeService.setVariable(taskId, variableName, typedValue);
      } catch (AuthorizationException e) {
        throw e;
      } catch (ProcessEngineException e) {
        String errorMessage = String.format("Cannot put %s variable %s: %s", "task", variableName, e.getMessage());
        throw new SystemException(errorMessage, e);
      }
    }
    
  }
  
  private VariableValueDto createVariableValueDto(String valueTypeName, MultipartFile data) throws IOException {
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
  //@Override
  public ResponseEntity<byte[]> fetchProcessInstanceVariableData(String processInstanceId, String variableName,
      CIBUser user) throws NoObjectFoundException, SystemException {
    //TODO: not tested
    Variable variable = fetchVariableByProcessInstanceId(processInstanceId, variableName, user);
    String objectType = variable.getValueInfo().get("objectTypeName");
    if (objectType != null) {
      try {
        Class<?> clazz =  Class.forName(objectType);

        if (DataSource.class.isAssignableFrom(clazz)) {
          final ObjectMapper mapper = new ObjectMapper();
          @SuppressWarnings("unchecked")
          DataSource ds = mapper.convertValue(variable.getValue(), (Class<? extends DataSource>) clazz);

          new ResponseEntity<>(IOUtils.toByteArray(ds.getInputStream()), HttpStatusCode.valueOf(200));
        }
      } catch (ClassNotFoundException e) {
        log.info("Class " + objectType + " could not be loaded!");
      } catch (IOException e) {
        throw new SystemException(e.getMessage(), e);
      }
    }
    TypedValue value = null;
    try {
      // value = getVariableEntity(variableName, false);
      value = runtimeService.getVariableTyped(processInstanceId, variableName, false);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot get %s variable %s: %s", "processInstance", variableName, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }

    if (value == null) {
      String errorMessage = String.format("%s variable with name %s does not exist", "processInstance", variableName);
      throw new InvalidRequestException(Status.NOT_FOUND, errorMessage);
    }
    if (value instanceof BytesValue || ValueType.BYTES.equals(value.getType())) {
      byte[] valueBytes = (byte[]) value.getValue();
      if (valueBytes == null) {
        valueBytes = new byte[0];
      }
      ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(valueBytes, HttpStatusCode.valueOf(200));
    } else if (ValueType.FILE.equals(value.getType())) {
      FileValue typedFileValue = (FileValue) value;
      try {
        byte[] bytes = typedFileValue.getValue() == null ? null : IOUtils.toByteArray(typedFileValue.getValue());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(typedFileValue.getMimeType() != null ? MediaType.valueOf(typedFileValue.getMimeType()) :  MediaType.APPLICATION_OCTET_STREAM);
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(bytes, headers, HttpStatusCode.valueOf(200));
        return responseEntity;
      } catch (IOException e) {
        throw new SystemException(e.getMessage(), e);
      }
    } else {
      throw new InvalidRequestException(Response.Status.BAD_REQUEST, String.format("Value of variable with id %s is not a binary value.", variableName));
    }
    return null;
  }
  
  //@Override
  public void uploadProcessInstanceVariableFileData(String processInstanceId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException {
    //TODO: not tested
    try {
      setBinaryVariable(data, valueType, null, null, processInstanceId, variableName);
    } catch (HttpStatusCodeException e) {
      //TODO: wrapException will be provided by the base class
      //throw wrapException(e, user);
      throw new SystemException(e.getMessage());
    } catch (IOException e) {
      throw new SystemException(e.getMessage());
    }
  }
  
  //@Override
  public ProcessStart submitStartFormVariables(String processDefinitionId, List<Variable> formResult, CIBUser user) 
    throws SystemException {
    //TODO: not tested
    
    Map<String, Object> variables = new HashMap<>();
    for (Variable variable : formResult) {
      VariableValueDto variableValueDto = convertValue(variable, VariableValueDto.class);
      variableValueDto.setType(variable.getType());
      variableValueDto.setValue(variable.getValue());
      if (variable.getValueInfo() != null)
        variableValueDto.setValueInfo(new HashMap<>(variable.getValueInfo()));
      variables.put(variable.getName(), variableValueDto);
    }

    //TODO: VariableProvider modifies the variables:
//  ObjectMapper mapper = new ObjectMapper();
//  ObjectNode variables = mapper.getNodeFactory().objectNode();
//  ObjectNode modifications = mapper.getNodeFactory().objectNode();
//  try {   
//    for (Variable variable: formResult) {
//      ObjectNode variablePost = mapper.getNodeFactory().objectNode();
//              String val = String.valueOf(variable.getValue());
//      if (variable.getValue() == null) {
//        variablePost.put("type", "Null");
//      }
//              else if (variable.getType().equals("Boolean")) {
//                  variablePost.put("value", Boolean.parseBoolean(val));
//              } else if (variable.getType().equals("Double")) {
//                  variablePost.put("value", Double.parseDouble(val));
//              } else if (variable.getType().equals("Integer")) {
//                  variablePost.put("value", Integer.parseInt(val));
//              }
//      else variablePost.put("value", val);
//
//      if(variable.getType().equals("file")) {
//
//        //https://helpdesk.cib.de/browse/BPM4CIB-434
//        int lastIndex = variable.getFilename().lastIndexOf(".rtf");
//        if ((lastIndex > 0) && ((lastIndex + 4) == variable.getFilename().length())) {
//          variable.getValueInfo().put("mimeType", "application/rtf");
//        }
//
//      }
//
//      if (variable.getType().equals("Object")) {
//        variablePost.set("valueInfo", mapper.valueToTree(variable.getValueInfo()));
//        variablePost.put("type", "Object");
//        try {
//          variablePost.put("value", mapper.writeValueAsString(variable.getValue()));
//        } catch (IOException e) {
//          SystemException se = new SystemException(e);
//          log.info("Exception in submitVariables(...):", se);
//          throw se;
//        }
//      }
//  
//      if (variable.getType().equals("File")) {
//        variablePost.set("valueInfo", mapper.valueToTree(variable.getValueInfo()));
//        variablePost.put("type", "File");
//      }
//
//      variables.set(variable.getName(), variablePost);
//    }
//
//    modifications.set("variables", variables);
    
    org.cibseven.bpm.engine.runtime.ProcessInstance instance = null;
    try {
      //TODO: businessKey not ued here
      //Map<String, Object> variables = VariableValueDto.toMap(startProcessInstanceDto.getVariables(), engine, objectMapper);
//      String businessKey = startProcessInstanceDto.getBusinessKey();
//      if (businessKey != null) {
//        instance = formService.submitStartForm(processDefinitionId, businessKey, variables);
//      } else {
        instance = formService.submitStartForm(processDefinitionId, variables);
//      }

    } catch (AuthorizationException e) {
      throw e;

    } catch (FormFieldValidationException e) {
      String errorMessage = String.format("Cannot instantiate process definition %s: %s", processDefinitionId, e.getMessage());
      throw new SystemException(errorMessage, e);

    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot instantiate process definition %s: %s", processDefinitionId, e.getMessage());
      throw new SystemException(errorMessage, e);

    } catch (RestException e) {
      String errorMessage = String.format("Cannot instantiate process definition %s: %s", processDefinitionId, e.getMessage());
      throw new SystemException(errorMessage, e);

    }

    ProcessInstanceDto processInstanceDto = ProcessInstanceDto.fromProcessInstance(instance);

    //TODO: implementation creates URL that is probably unused
//    URI uri = context.getBaseUriBuilder()
//      .path(rootResourcePath)
//      .path(ProcessInstanceRestService.PATH)
//      .path(instance.getId())
//      .build();
//
//    result.addReflexiveLink(uri, HttpMethod.GET, "self");
    ProcessStart result = convertValue(processInstanceDto, ProcessStart.class);
    return result;
    
  }
  
  //@Override
  public Variable fetchVariableByProcessInstanceId(String processInstanceId, String variableName, CIBUser user) throws SystemException {
    //TODO: to be implemented
    Variable variableSerialized = fetchVariableByProcessInstanceIdImpl(processInstanceId, variableName, false, user);
    Variable variableDeserialized = fetchVariableByProcessInstanceIdImpl(processInstanceId, variableName, true, user);

    variableDeserialized.setValueSerialized(variableSerialized.getValue());
    variableDeserialized.setValueDeserialized(variableDeserialized.getValue());
    return variableDeserialized;
  }

  private Variable fetchVariableByProcessInstanceIdImpl(String processInstanceId, String variableName, boolean deserializeValue, CIBUser user) 
      throws SystemException {
    //TODO: not tested
    TypedValue value = getTypedValueForProcessInstanceVariable(processInstanceId, variableName, deserializeValue);
    return convertValue(VariableValueDto.fromTypedValue(value), Variable.class);
  }
  
  private TypedValue getTypedValueForProcessInstanceVariable(String processInstanceId, String variableName, boolean deserializeValue) {
    try {
       return runtimeService.getVariableTyped(processInstanceId, variableName, deserializeValue);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot get %s variable %s: %s", "task", variableName, e.getMessage());
      throw new SystemException(errorMessage, e);
    }
  }
  
  //@Override
  public void saveVariableInProcessInstanceId(String processInstanceId, List<Variable> variables, CIBUser user) throws SystemException {
    //TODO: not tested
    List<String> deletions = new ArrayList<>();
    Map<String, VariableValueDto> modifications = new HashMap<>();
    for (Variable variable : variables) {
      VariableValueDto variableValueDto = convertValue(variable, VariableValueDto.class);
      variableValueDto.setType(variable.getType());
      variableValueDto.setValue(variable.getValue());
      if (variable.getValueInfo() != null)
        variableValueDto.setValueInfo(new HashMap<>(variable.getValueInfo()));
      modifications.put(variable.getName(), variableValueDto);
    }
    updateVariableEntities(processInstanceId, modifications, deletions);
  }

  //updates execution variables 
  private void updateVariableEntities(String processInstanceId, Map<String, VariableValueDto> modifications, List<String> deletions) {
    VariableMap variableModifications = null;
    try {
      variableModifications = VariableValueDto.toMap(modifications, processEngine, objectMapper);
    } catch (RestException e) {
      String errorMessage = String.format("Cannot modify variables for %s: %s", "processInstance", e.getMessage());
      throw new SystemException(errorMessage, e);
    }
    try {
      RuntimeServiceImpl runtimeServiceImpl = (RuntimeServiceImpl) runtimeService;
      runtimeServiceImpl.updateVariables(processInstanceId, variableModifications, deletions);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot modify variables for %s %s: %s", "processInstance", processInstanceId, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }
  }
  
  //@Override
  public void submitVariables(String processInstanceId, List<Variable> formResult, CIBUser user, String processDefinitionId) 
       throws SystemException {
    //TODO: not tested
    //TODO: VariableProvider ignores processDefinitionId and converts the variables: 
//  ObjectMapper mapper = new ObjectMapper();
//  ObjectNode variables = mapper.getNodeFactory().objectNode();
//  ObjectNode modifications = mapper.getNodeFactory().objectNode();
//
//  for (Variable variable: formResult) {
//    ObjectNode variablePost = mapper.getNodeFactory().objectNode();
//    String val = String.valueOf(variable.getValue());
//    if (variable.getValue() == null) {
//      variablePost.put("type", "Null");
//    }
//    else if (variable.getType().equals("Boolean")) {
//      variablePost.put("value", Boolean.parseBoolean(val));
//    } else if (variable.getType().equals("Double")) {
//        variablePost.put("value", Double.parseDouble(val));
//    } else if (variable.getType().equals("Integer")) {
//        variablePost.put("value", Integer.parseInt(val));
//    }
//    else variablePost.put("value", val);
//    //TODO Changing variables before saving should be done in the task classes
//
//    if (variable.getType().equals("file")) {
//
//      //https://helpdesk.cib.de/browse/BPM4CIB-434
//      int lastIndex = variable.getFilename().lastIndexOf(".rtf");
//      if ((lastIndex > 0) && ((lastIndex + 4) == variable.getFilename().length())) {
//        variable.getValueInfo().put("mimeType", "application/rtf");
//      }
//    }
//
//    if (variable.getType().equals("json")) {
//      variablePost.set("valueInfo", mapper.valueToTree(variable.getValueInfo()));
//      variablePost.put("type", "json");
//      try {
//        variablePost.put("value", mapper.writeValueAsString(variable.getValue()));
//      } catch (IOException e) {
//        SystemException se = new SystemException(e);
//        log.info("Exception in submitVariables(...):", se);
//        throw se;
//      }
//    }
//
//    if (variable.getType().equals("Object")) {
//      variablePost.set("valueInfo", mapper.valueToTree(variable.getValueInfo()));
//      variablePost.put("type", "Object");
//      try {
//        variablePost.put("value", mapper.writeValueAsString(variable.getValue()));
//      } catch (IOException e) {
//        SystemException se = new SystemException(e);
//        log.info("Exception in submitVariables(...):", se);
//        throw se;
//      }
//    }
//
//    variables.set(variable.getName(), variablePost);
//}
    
    List<String> deletions = new ArrayList<>();
    Map<String, VariableValueDto> modifications = new HashMap<>();
    for (Variable variable : formResult) {
      VariableValueDto variableValueDto = convertValue(variable, VariableValueDto.class);
      variableValueDto.setType(variable.getType());
      variableValueDto.setValue(variable.getValue());
      if (variable.getValueInfo() != null)
        variableValueDto.setValueInfo(new HashMap<>(variable.getValueInfo()));
      modifications.put(variable.getName(), variableValueDto);
    }
    
    updateVariableEntities(processInstanceId, modifications, deletions);
    
  }
  
  //@Override
  public Map<String, Variable> fetchProcessFormVariablesById(String id, CIBUser user) throws SystemException {
    //TODO: not tested
    VariableMap startFormVariables = formService.getStartFormVariables(id, null, true);
    Map<String, Variable> resultMap = new HashMap<>(); 
    Map<String, VariableValueDto> resultDtoMap = VariableValueDto.fromMap(startFormVariables);
    for (Entry<String, VariableValueDto> resultDtoEntry : resultDtoMap.entrySet()) {
      resultMap.put(resultDtoEntry.getKey(), convertValue(resultDtoEntry.getValue(), Variable.class));
    }
    return resultMap;
}
  
  //@Override
  public void putLocalExecutionVariable(String executionId, String varName, Map<String, Object> data, CIBUser user) {
    //TODO: not tested
    try {
      VariableValueDto variable = objectMapper.convertValue(data, VariableValueDto.class);
      TypedValue typedValue = variable.toTypedValue(processEngine, objectMapper);
      runtimeService.setVariable(executionId, varName, typedValue);

    } catch (RestException e) {
      throw new InvalidRequestException(e.getStatus(), e,
        String.format("Cannot put %s variable %s: %s", "execution", varName, e.getMessage()));
    } catch (BadUserRequestException e) {
      throw new SystemException(String.format("Cannot put %s variable %s: %s", "execution", varName, e.getMessage()), e);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new SystemException(String.format("Cannot put %s variable %s: %s", "execution", varName, e.getMessage()), e);
    }
  }

  //@Override
  public Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(String processDefinitionId, Map<String, Object> params, CIBUser user) {
    //TODO: not tested
    HistoricActivityInstanceQueryDto queryHistoricActivityInstanceDto = objectMapper.convertValue(params, HistoricActivityInstanceQueryDto.class);
    queryHistoricActivityInstanceDto.setProcessDefinitionId(processDefinitionId);
    return queryHistoricActivityInstance(queryHistoricActivityInstanceDto);
    
  }

  /*
  
  ██████  ███████  ██████ ██ ███████ ██  ██████  ███    ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██   ██ ██      ██      ██ ██      ██ ██    ██ ████   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██   ██ █████   ██      ██ ███████ ██ ██    ██ ██ ██  ██     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██   ██ ██      ██      ██      ██ ██ ██    ██ ██  ██ ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ██████  ███████  ██████ ██ ███████ ██  ██████  ██   ████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                                                                                
  */
  
  //@Override
  public Collection<Decision> getDecisionDefinitionList(Map<String, Object> queryParams, CIBUser user) {
     
    DecisionDefinitionQueryDto queryDto = objectMapper.convertValue(queryParams, DecisionDefinitionQueryDto.class);
    List<Decision> definitions = new ArrayList<>();
    DecisionDefinitionQuery query = queryDto.toQuery(processEngine);
    List<DecisionDefinition> matchingDefinitions = QueryUtil.list(query, null, null);
    for (DecisionDefinition definition : matchingDefinitions) {
      DecisionDefinitionDto def = DecisionDefinitionDto.fromDecisionDefinition(definition);
      definitions.add(convertValue(def, Decision.class));
    }
    return definitions;
  } 
  
  //@Override
  public Long getDecisionDefinitionListCount(Map<String, Object> queryParams, CIBUser user) {
    //TODO: not tested
    DecisionDefinitionQueryDto queryDto = objectMapper.convertValue(queryParams, DecisionDefinitionQueryDto.class);
    DecisionDefinitionQuery query = queryDto.toQuery(processEngine);
    List<DecisionDefinition> matchingDefinitions = QueryUtil.list(query, null, null);
    return Long.valueOf(matchingDefinitions.size());
  }
  
  //@Override
  public Decision getDecisionDefinitionByKey(String key, CIBUser user) {
    //TODO: not tested
    DecisionDefinition decisionDefinition = getDecisionDefinitionByKeyAndTenant(key, null);
    return convertValue(DecisionDefinitionDto.fromDecisionDefinition(decisionDefinition), Decision.class);
}
  
  private DecisionDefinition getDecisionDefinitionByKeyAndTenant(String key, String tenantId) {
    DecisionDefinitionQuery query = repositoryService
    .createDecisionDefinitionQuery()
    .decisionDefinitionKey(key);
    if (tenantId == null)
      query.withoutTenantId();
    else
      query.tenantIdIn(new String[] {tenantId});
    
    DecisionDefinition decisionDefinition = query
        .latestVersion()
        .singleResult();

    if (decisionDefinition == null) {
      String errorMessage = String.format("No matching decision definition with key: %s and no tenant-id", key);
      throw new SystemException(errorMessage);
    }
    return decisionDefinition;
  }

  //@Override
  public Object getDiagramByKey(String key, CIBUser user) {
    //TODO: not tested
    return getDiagramByKeyAndTenant(key, null, user);
  }

  //@Override
  public Object evaluateDecisionDefinitionByKey(Map<String, Object> data, String key, CIBUser user) {
    //TODO: not tested
    EvaluateDecisionDto parameters = objectMapper.convertValue(data, EvaluateDecisionDto.class);
    Map<String, Object> variables = VariableValueDto.toMap(parameters.getVariables(), processEngine, objectMapper);
    DecisionDefinition decisionDefinition = getDecisionDefinitionByKeyAndTenant(key, null);

    try {
      DmnDecisionResult decisionResult = decisionService
          .evaluateDecisionById(decisionDefinition.getId())
          .variables(variables)
          .evaluate();

      List<Map<String, VariableValueDto>> dto = new ArrayList<>();

      for (DmnDecisionResultEntries entries : decisionResult) {
        Map<String, VariableValueDto> resultEntriesDto = createResultEntriesDto(entries);
        dto.add(resultEntriesDto);
      }
      //TODO: probably wrong object type
      return dto;

    }
    catch (AuthorizationException e) {
      throw e;
    }
    catch (NotFoundException e) {
      String errorMessage = String.format("Cannot evaluate decision %s: %s", decisionDefinition.getId(), e.getMessage());
      throw new SystemException(errorMessage, e);
    }
    catch (NotValidException e) {
      String errorMessage = String.format("Cannot evaluate decision %s: %s", decisionDefinition.getId(), e.getMessage());
      throw new SystemException(errorMessage, e);
    }
    catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot evaluate decision %s: %s", decisionDefinition.getId(), e.getMessage());
      throw new SystemException(errorMessage, e);
    }
    catch (DmnEngineException e) {
      String errorMessage = String.format("Cannot evaluate decision %s: %s", decisionDefinition.getId(), e.getMessage());
      throw new SystemException(errorMessage, e);
    }
  }
  
  private Map<String, VariableValueDto> createResultEntriesDto(DmnDecisionResultEntries entries) {
    VariableMap variableMap = Variables.createVariables();

    for(String key : entries.keySet()) {
      TypedValue typedValue = entries.getEntryTyped(key);
      variableMap.putValueTyped(key, typedValue);
    }

    return VariableValueDto.fromMap(variableMap);
  }

  //@Override
  public void updateHistoryTTLByKey(Map<String, Object> data, String key, CIBUser user) {
    //TODO: not tested
    HistoryTimeToLiveDto historyTimeToLiveDto = objectMapper.convertValue(data,  HistoryTimeToLiveDto.class);
    DecisionDefinition decisionDefinition = getDecisionDefinitionByKeyAndTenant(key, null);
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinition.getId(), 
        historyTimeToLiveDto.getHistoryTimeToLive());
}
  
  //@Override
  public Decision getDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user) {
    //TODO: not tested
    DecisionDefinition decisionDefinition = getDecisionDefinitionByKeyAndTenant(key, tenant);
    DecisionDefinitionDto dto = DecisionDefinitionDto.fromDecisionDefinition(decisionDefinition); 
    return convertValue(dto, Decision.class);
  }
  
  //@Override
  public Object getDiagramByKeyAndTenant(String key, String tenant, CIBUser user) {
    //TODO: not tested
    DecisionDefinition decisionDefinition = getDecisionDefinitionByKeyAndTenant(key, tenant);
    return getDiagramByDecisionDefinition(decisionDefinition, user);
  }
  
  private Object getDiagramByDecisionDefinition(DecisionDefinition decisionDefinition, CIBUser user) {
    InputStream decisionDiagram = repositoryService.getDecisionDiagram(decisionDefinition.getId());
    if (decisionDiagram == null) {
      return Response.noContent().build();
    } else {
      //TODO: fetchDiagram creates xml 
//      byte[] processModel = IoUtil.readInputStream(processModelIn, "processModelBpmn20Xml");
//      return convertValue(ProcessDefinitionDiagramDto.create(id, new String(processModel, "UTF-8")), ProcessDiagram.class);
      //TODO: probably not the correct object type
      String fileName = decisionDefinition.getDiagramResourceName();
      return Response.ok(decisionDiagram).header("Content-Disposition",URLEncodingUtil.buildAttachmentValue(fileName))
          .type(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix(fileName)).build();
    }
  }

  //@Override
  public Object evaluateDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user) {
    //TODO: not implemented in DecisionProvider, parameter array not part of the interface as in evaluateDecisionDefinitionByKey() 
    return null;
  }
  
  //@Override
  public Object updateHistoryTTLByKeyAndTenant(String key, String tenant, CIBUser user) {
    //TODO: not implemented in DecisionProvider, parameter array not part of the interface as in updateHistoryTTLByKey() 
    return null;
  }
  
  //@Override
  public Object getXmlByKey(String key, CIBUser user) {
    //TODO: not tested
    return getXmlByKeyAndTenant(key, null, user);
  }
  
  //@Override
  public Object getXmlByKeyAndTenant(String key, String tenant, CIBUser user) {
    //TODO: not tested
    DecisionDefinition decisionDefinition = getDecisionDefinitionByKeyAndTenant(key, tenant);
    return getXmlByDefinitionId(decisionDefinition.getId());
  }
  
  private Object getXmlByDefinitionId(String definitionId) {
    InputStream decisionModelInputStream = null;
    try {
      decisionModelInputStream = repositoryService.getDecisionModel(definitionId);

      byte[] decisionModel = IoUtil.readInputStream(decisionModelInputStream, "decisionModelDmnXml");
      return DecisionDefinitionDiagramDto.create(definitionId, new String(decisionModel, "UTF-8"));

    } catch (ProcessEngineException|UnsupportedEncodingException e) {
      throw new SystemException(e.getMessage(), e);

    } finally {
      IoUtil.closeSilently(decisionModelInputStream);
    }
  }
  
  //@Override
  public Decision getDecisionDefinitionById(String id, Optional<Boolean> extraInfo, CIBUser user) {
    
    DecisionDefinition definition = getDecisionDefinitionId(id, user);
    Decision decision = convertValue(DecisionDefinitionDto.fromDecisionDefinition(definition), Decision.class);
    if (extraInfo.isPresent() && extraInfo.get()) {
      Map<String, Object> queryParams = new HashMap<>();
      queryParams.put("decisionDefinitionId", definition.getId());
      Long count = getHistoricDecisionInstanceCount(queryParams, user);
      decision.setAllInstances(count);
    }
    return decision;
  }
  
  //@Override
  public Object getDiagramById(String id, CIBUser user) {
    //TODO: not tested
    DecisionDefinition definition = getDecisionDefinitionId(id, user);
    return getDiagramByDecisionDefinition(definition, user);
  }
  
  private DecisionDefinition getDecisionDefinitionId(String id, CIBUser user) {
    //TODO: not tested
    DecisionDefinition definition = null;
    try {
      definition = repositoryService.getDecisionDefinition(id);
    } catch (ProcessEngineException e) {
      throw new SystemException(e.getMessage(), e);
    }
    return definition;
  }

  //@Override
  public Object evaluateDecisionDefinitionById(String id, CIBUser user) {
    //TODO: not implemented in DecisionProvider and parameters are missing like in evaluateDecisionDefinitionByKey() 
    DecisionDefinition definition = getDecisionDefinitionId(id, user);
    return null;
  }
  
  //@Override
  public void updateHistoryTTLById(String id, Map<String, Object> data, CIBUser user) {
    //TODO: not tested
    HistoryTimeToLiveDto historyTimeToLiveDto = objectMapper.convertValue(data,  HistoryTimeToLiveDto.class);
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(id, 
        historyTimeToLiveDto.getHistoryTimeToLive());
  }
  
  //@Override
  public Object getXmlById(String id, CIBUser user) {
    //TODO: not tested
    //should return like 
//    {id=dish:1:92f39b44-76b2-11f0-bd0d-4ce1734f67af, dmnXml=<?xml version="1.0" encoding="UTF-8"?>
//    <definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/" xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/" xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/" xmlns:modeler="http://camunda.org/schema/modeler/1.0" xmlns:camunda="http://camunda.org/schema/1.0/dmn" id="Dish" name="dish" namespace="http://camunda.org/schema/1.0/dmn" exporter="Camunda Modeler" exporterVersion="5.35.0" modeler:executionPlatform="Camunda Platform" modeler:executionPlatformVersion="7.23.0">
//      <decision id="dish" name="Last Dish ;-)" camunda:historyTimeToLive="P180D">
    //but returns:
// id=dish:1:92f39b44-76b2-11f0-bd0d-4ce1734f67af
// dmnXml: <?xml version="1.0" encoding="UTF-8"?>
//    <definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/" xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/" xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/" xmlns:modeler="http://camunda.org/schema/modeler/1.0" xmlns:camunda="http://camunda.org/schema/1.0/dmn" id="Dish" name="dish" namespace="http://camunda.org/schema/1.0/dmn" exporter="Camunda Modeler" exporterVersion="5.35.0" modeler:executionPlatform="Camunda Platform" modeler:executionPlatformVersion="7.23.0">
//    <decision id="dish" name="Last Dish ;-)" camunda:historyTimeToLive="P180D">
//      <decisionTable id="DecisionTable_18p9dax">
    
    
    return getXmlByDefinitionId(id);
  }

  //@Override
  public Collection<Decision> getDecisionVersionsByKey(String key, Optional<Boolean> lazyLoad, CIBUser user) {
    List<DecisionDefinition> decisionDefinitions = repositoryService
    .createDecisionDefinitionQuery()
    .decisionDefinitionKey(key)
    .withoutTenantId()
    .unlimitedList();;

    if (decisionDefinitions == null  || decisionDefinitions.isEmpty()) {
      String errorMessage = String.format("No matching decision definition with key: %s and no tenant-id", key);
      throw new SystemException(errorMessage);
    }
    List<Decision> decisions = new ArrayList<>();    
    for (DecisionDefinition decisionDefinition : decisionDefinitions) {
      DecisionDefinitionDto decisionDefinitionDto = DecisionDefinitionDto.fromDecisionDefinition(decisionDefinition); 
      decisions.add(convertValue(decisionDefinitionDto, Decision.class));
    }
    return decisions;
  }
  
  //@Override
  public Collection<HistoricDecisionInstance> getHistoricDecisionInstances(Map<String, Object> queryParams, CIBUser user){
    HistoricDecisionInstanceQueryDto queryHistoricDecisionInstanceDto = objectMapper.convertValue(queryParams, HistoricDecisionInstanceQueryDto.class);
    HistoricDecisionInstanceQuery query = queryHistoricDecisionInstanceDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.history.HistoricDecisionInstance> matchingHistoricDecisionInstances = QueryUtil.list(query, null, null);

    List<HistoricDecisionInstance> historicDecisionInstanceDtoResults = new ArrayList<>();
    for (org.cibseven.bpm.engine.history.HistoricDecisionInstance historicDecisionInstance : matchingHistoricDecisionInstances) {
      HistoricDecisionInstanceDto resultHistoricDecisionInstanceDto = HistoricDecisionInstanceDto.fromHistoricDecisionInstance(historicDecisionInstance);
      historicDecisionInstanceDtoResults.add(convertValue(resultHistoricDecisionInstanceDto, HistoricDecisionInstance.class));
    }
    return historicDecisionInstanceDtoResults;
  }
  
  //@Override
  public Long getHistoricDecisionInstanceCount(Map<String, Object> queryParams, CIBUser user){
    //TODO: not tested
    HistoricDecisionInstanceQueryDto queryHistoricDecisionInstanceDto = objectMapper.convertValue(queryParams, HistoricDecisionInstanceQueryDto.class);
    HistoricDecisionInstanceQuery query = queryHistoricDecisionInstanceDto.toQuery(processEngine);
    return query.count();
  }
  
  //@Override
  public HistoricDecisionInstance getHistoricDecisionInstanceById(String id, Map<String, Object> queryParams, CIBUser user){
    //TODO: not tested
    HistoricDecisionInstanceQueryDto historicDecisionInstanceQueryDto = objectMapper.convertValue(queryParams, HistoricDecisionInstanceQueryDto.class);
    HistoricDecisionInstanceQuery query = historicDecisionInstanceQueryDto.toQuery(processEngine);
    org.cibseven.bpm.engine.history.HistoricDecisionInstance instance = query.singleResult();

    if (instance == null) {
      throw new SystemException("Historic decision instance with id '" + id + "' does not exist");
    }

    return convertValue(HistoricDecisionInstanceDto.fromHistoricDecisionInstance(instance), HistoricDecisionInstance.class);
  }
  
  //@Override
  public Object deleteHistoricDecisionInstances(Map<String, Object> data, CIBUser user){
    //TODO: not tested
    DeleteHistoricDecisionInstancesDto dto = objectMapper.convertValue(data, DeleteHistoricDecisionInstancesDto.class);
    HistoricDecisionInstanceQuery decisionInstanceQuery = null;
    if (dto.getHistoricDecisionInstanceQuery() != null) {
      decisionInstanceQuery = dto.getHistoricDecisionInstanceQuery().toQuery(processEngine);
    }

    try {
      List<String> historicDecisionInstanceIds = dto.getHistoricDecisionInstanceIds();
      String deleteReason = dto.getDeleteReason();
      org.cibseven.bpm.engine.batch.Batch batch = processEngine.getHistoryService().deleteHistoricDecisionInstancesAsync(historicDecisionInstanceIds, decisionInstanceQuery, deleteReason);
      return BatchDto.fromBatch(batch);
    }
    catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }
  
  //@Override
  public Object setHistoricDecisionInstanceRemovalTime(Map<String, Object> data, CIBUser user){
    //TODO: not tested
    SetRemovalTimeToHistoricDecisionInstancesDto dto = objectMapper.convertValue(data, SetRemovalTimeToHistoricDecisionInstancesDto.class);
    HistoryService historyService = processEngine.getHistoryService();

    HistoricDecisionInstanceQuery historicDecisionInstanceQuery = null;

    if (dto.getHistoricDecisionInstanceQuery() != null) {
      historicDecisionInstanceQuery = dto.getHistoricDecisionInstanceQuery().toQuery(processEngine);

    }

    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builder =
      historyService.setRemovalTimeToHistoricDecisionInstances();

    if (dto.isCalculatedRemovalTime()) {
      builder.calculatedRemovalTime();

    }

    Date removalTime = dto.getAbsoluteRemovalTime();
    if (dto.getAbsoluteRemovalTime() != null) {
      builder.absoluteRemovalTime(removalTime);

    }

    if (dto.isClearedRemovalTime()) {
      builder.clearedRemovalTime();

    }

    builder.byIds(dto.getHistoricDecisionInstanceIds());
    builder.byQuery(historicDecisionInstanceQuery);

    if (dto.isHierarchical()) {
      builder.hierarchical();

    }

    org.cibseven.bpm.engine.batch.Batch batch = builder.executeAsync();
    return BatchDto.fromBatch(batch);
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
    JobDefinitionQueryDto queryDto;
    try {
      queryDto = objectMapper.readValue(params, JobDefinitionQueryDto.class);
    } catch (JsonProcessingException e) {
      throw new SystemException(e.getMessage());
    }
    queryDto.setObjectMapper(objectMapper);
    JobDefinitionQuery query = queryDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.management.JobDefinition> matchingJobDefinitions = QueryUtil.list(query, null, null);

    List<JobDefinition> jobDefinitionResults = new ArrayList<>();
    for (org.cibseven.bpm.engine.management.JobDefinition jobDefinition : matchingJobDefinitions) {
      JobDefinitionDto result = JobDefinitionDto.fromJobDefinition(jobDefinition);
      jobDefinitionResults.add(convertValue(result, JobDefinition.class));
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
    //TODO: in progress
    JobDefinitionSuspensionStateDto jobDefinitionSuspensionStateDto = objectMapper.convertValue(params,
        JobDefinitionSuspensionStateDto.class);
    jobDefinitionSuspensionStateDto.setProcessDefinitionId(id);
    if (jobDefinitionSuspensionStateDto.getJobDefinitionId() != null) {
      String message = "Either processDefinitionId or processDefinitionKey can be set to update the suspension state.";
      throw new SystemException(message);
    }

    try {
      jobDefinitionSuspensionStateDto.updateSuspensionState(processEngine);

    } catch (IllegalArgumentException e) {
      String message = String.format("Could not update the suspension state of Job Definitions due to: %s",
          e.getMessage());
      throw new SystemException(message, e);
    }
  }

  //@Override
  public void deleteJob(String id, CIBUser user) {
    //TODO: not tested
    try {
      managementService.deleteJob(id);
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new SystemException(e.getMessage());
    }
  }

  //@Override
  public JobDefinition findJobDefinition(String id, CIBUser user) {
    //TODO: not tested
    org.cibseven.bpm.engine.management.JobDefinition jobDefinition = managementService.createJobDefinitionQuery().jobDefinitionId(id).singleResult();
    if (jobDefinition == null) {
      throw new SystemException("Job Definition with id " + id + " does not exist");
    }
    return convertValue(JobDefinitionDto.fromJobDefinition(jobDefinition), JobDefinition.class);
  } 

  //@Override
  public Collection<Object> getHistoryJobLog(Map<String, Object> params, CIBUser user) {
    //TODO: not tested
    HistoricJobLogQueryDto queryDto = objectMapper.convertValue(params, HistoricJobLogQueryDto.class);
    queryDto.setObjectMapper(objectMapper);
    HistoricJobLogQuery query = queryDto.toQuery(processEngine);
    List<HistoricJobLog> matchingHistoricJobLogs = QueryUtil.list(query, null, null);

    List<Object> results = new ArrayList<>();
    for (HistoricJobLog historicJobLog : matchingHistoricJobLogs) {
      HistoricJobLogDto result = HistoricJobLogDto.fromHistoricJobLog(historicJobLog);
      results.add(result);
    }
    return results;
  }
  
  //@Override
  public String getHistoryJobLogStacktrace(String id, CIBUser user) {
    //TODO: not tested
    try {
      String stacktrace = historyService.getHistoricJobLogExceptionStacktrace(id);
      return stacktrace;
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new SystemException(e.getMessage());
    }
  }

/** 
* conversion and helper functions
*/
  private <T> T convertValue(Object fromValueDto, Class<T> toValueType)
        throws IllegalArgumentException {
      Map<String, Object> filterDtoMap = objectMapper
          .convertValue(fromValueDto, new TypeReference<Map<String, Object>>() {});
      return objectMapper.convertValue(filterDtoMap, toValueType);
  }
  
  /*

  ██████   █████  ████████  ██████ ██   ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██   ██ ██   ██    ██    ██      ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██████  ███████    ██    ██      ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
  ██   ██ ██   ██    ██    ██      ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ██████  ██   ██    ██     ██████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                                              
  */
  
  //@Override
  public Collection<Batch> getBatches(Map<String, Object> params, CIBUser user) {
    //TODO: not tested
    //TODO: no default Ctor in BatchStatisticsQueryDto - will not work! 
    BatchQueryDto queryDto = objectMapper.convertValue(params, BatchQueryDto.class);
    BatchQuery query = queryDto.toQuery(processEngine);

    List<org.cibseven.bpm.engine.batch.Batch> matchingBatches = QueryUtil.list(query, null, null);

    List<Batch> batchResults = new ArrayList<>();
    for (org.cibseven.bpm.engine.batch.Batch matchingBatch : matchingBatches) {
      batchResults.add(convertValue(BatchDto.fromBatch(matchingBatch), Batch.class));
    }
    batchResults.forEach(batch -> {
      String batchId = batch.getId();

        Map<String, Object> statParams = new HashMap<>();
        statParams.put("batchId", batchId);

        Collection<Batch> statList = getBatchStatistics(statParams, user);
        if (!statList.isEmpty()) {
          Batch stats = statList.iterator().next();
            batch.setCompletedJobs(stats.getCompletedJobs());
            batch.setRemainingJobs(stats.getRemainingJobs());
            batch.setFailedJobs(stats.getFailedJobs());
        }
      });
    
    return batchResults;
  }

  //@Override
  public Collection<Batch> getBatchStatistics(Map<String, Object> params, CIBUser user) {
    //TODO: not tested
    //TODO: no default Ctor in BatchStatisticsQueryDto - will not work! 
    BatchStatisticsQueryDto queryDto = objectMapper.convertValue(params, BatchStatisticsQueryDto.class);
    BatchStatisticsQuery query = queryDto.toQuery(processEngine);

    List<BatchStatistics> batchStatisticsList = QueryUtil.list(query, null, null);

    List<Batch> statisticsResults = new ArrayList<>();
    for (BatchStatistics batchStatistics : batchStatisticsList) {
      statisticsResults.add(convertValue(BatchStatisticsDto.fromBatchStatistics(batchStatistics), Batch.class));
    }

    return statisticsResults;
  }

  //@Override
  public void deleteBatch(String id, Map<String, Object> params, CIBUser user) {
    //TODO: in progress - not tested
    //is cascade in params?
    Boolean cascade = false;
    params.containsValue("cascade");
      cascade = params.get("cascade").equals("true");
    try {
      processEngine.getManagementService()
        .deleteBatch(id, cascade);
    }
    catch (BadUserRequestException e) {
      throw new SystemException("Unable to delete batch with id '" + id + "'", e);
    }
  }
  
  //@Override
  public void setBatchSuspensionState(String id, Map<String, Object> params, CIBUser user) {
    //TODO: not tested
    //is suspended in params?
    Boolean suspended = false;
    params.containsValue("suspended");
      suspended = params.get("suspended").equals("true");

    if (suspended) {
      try {
        processEngine.getManagementService().suspendBatchById(id);
      }
      catch (BadUserRequestException e) {
        throw new SystemException("Unable to suspend batch with id '" + id + "'", e);
      }
    }
    else {
      try {
        processEngine.getManagementService().activateBatchById(id);
      }
      catch (BadUserRequestException e) {
        throw new SystemException("Unable to activate batch with id '" + id + "'", e);
      }
    }
  }
  
  //@Override
  public Collection<HistoryBatch> getHistoricBatches(Map<String, Object> params, CIBUser user) {
    //TODO: not tested
    HistoricBatchQueryDto queryDto = objectMapper.convertValue(params, HistoricBatchQueryDto.class);
    HistoricBatchQuery query = queryDto.toQuery(processEngine);

    List<HistoricBatch> matchingBatches = QueryUtil.list(query, null, null);

    List<HistoryBatch> batchResults = new ArrayList<>();
    for (HistoricBatch matchingBatch : matchingBatches) {
      batchResults.add(convertValue(HistoricBatchDto.fromBatch(matchingBatch), HistoryBatch.class));
    }
    return batchResults;
  }
  
  //@Override
  public Long getHistoricBatchCount(Map<String, Object> queryParams, CIBUser user) {
    //TODO: not tested
    HistoricBatchQueryDto queryDto = objectMapper.convertValue(queryParams, HistoricBatchQueryDto.class);
    HistoricBatchQuery query = queryDto.toQuery(processEngine);
    return query.count();
  }
    
  //@Override
  public HistoryBatch getHistoricBatchById(String id, CIBUser user) {
    //TODO: not tested
    HistoricBatch batch = processEngine.getHistoryService()
      .createHistoricBatchQuery()
      .batchId(id)
      .singleResult();

    if (batch == null) {
      throw new SystemException("Historic batch with id '" + id + "' does not exist");
    }

    return convertValue(HistoricBatchDto.fromBatch(batch), HistoryBatch.class);
  }
  
  //@Override
  public void deleteHistoricBatch(String id, CIBUser user) {
    //TODO: not tested
    try {
      processEngine.getHistoryService()
        .deleteHistoricBatch(id);
    }
    catch (BadUserRequestException e) {
      throw new SystemException("Unable to delete historic batch with id '" + id + "'", e);
    }
  }
  
  //@Override
  public Object setRemovalTime(Map<String, Object> payload) {
    //TODO: not tested
    SetRemovalTimeToHistoricBatchesDto dto = objectMapper.convertValue(payload, SetRemovalTimeToHistoricBatchesDto.class); 
    HistoricBatchQuery historicBatchQuery = null;

    if (dto.getHistoricBatchQuery() != null) {
      historicBatchQuery = dto.getHistoricBatchQuery().toQuery(processEngine);
    }

    SetRemovalTimeSelectModeForHistoricBatchesBuilder builder = historyService.setRemovalTimeToHistoricBatches();

    if (dto.isCalculatedRemovalTime()) {
      builder.calculatedRemovalTime();
    }

    Date removalTime = dto.getAbsoluteRemovalTime();
    if (dto.getAbsoluteRemovalTime() != null) {
      builder.absoluteRemovalTime(removalTime);
    }

    if (dto.isClearedRemovalTime()) {
      builder.clearedRemovalTime();
    }

    builder.byIds(dto.getHistoricBatchIds());
    builder.byQuery(historicBatchQuery);

    org.cibseven.bpm.engine.batch.Batch batch = builder.executeAsync();
    return convertValue(BatchDto.fromBatch(batch), Batch.class);
  }
    
  //@Override
  public Object getCleanableBatchReport(Map<String, Object> queryParams) {
    //TODO: not tested
    MultivaluedMap<String, String> multiValueMap = new MultivaluedHashMap<>();
    for (String key: queryParams.keySet()) {
      multiValueMap.put(key, Arrays.asList((String)queryParams.get(key)));
    }
    CleanableHistoricBatchReportDto queryDto = new CleanableHistoricBatchReportDto(objectMapper, multiValueMap);
    CleanableHistoricBatchReport query = queryDto.toQuery(processEngine);

    List<CleanableHistoricBatchReportResult> reportResult = QueryUtil.list(query, null, null);

    return CleanableHistoricBatchReportResultDto.convert(reportResult);
  }
    
  //@Override
  public Object getCleanableBatchReportCount() {
    //TODO: not tested - why aren't there parameters?
    MultivaluedMap<String, String> multiValueMap = new MultivaluedHashMap<>();
    CleanableHistoricBatchReportDto queryDto = new CleanableHistoricBatchReportDto(objectMapper, multiValueMap);
    queryDto.setObjectMapper(objectMapper);
    CleanableHistoricBatchReport query = queryDto.toQuery(processEngine);
    return query.count();
  }
  
  /*

  ███████ ██    ██ ███████ ████████ ███████ ███    ███     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██       ██  ██  ██         ██    ██      ████  ████     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ███████   ████   ███████    ██    █████   ██ ████ ██     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
       ██    ██         ██    ██    ██      ██  ██  ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ███████    ██    ███████    ██    ███████ ██      ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

  */                                                                                                                      

  //@Override
  public JsonNode getTelemetryData(CIBUser user) {
    //TODO: not tested
    TelemetryData data = managementService.getTelemetryData();
    JsonNode node = objectMapper.valueToTree(TelemetryDataDto.fromEngineDto(data));
    return node;
  }
  
  //@Override
  public Collection<Metric> getMetrics(Map<String, Object> queryParams, CIBUser user) {
    //TODO: not tested
    Collection<Metric> metrics = new ArrayList<>();
    List<Map<String, Object>> queryData = new ArrayList<>();
    List<String> metricNames = Optional.ofNullable(queryParams.get("metrics"))
            .map(Object::toString)
            .filter(s -> !s.isEmpty())
            .map(s -> Arrays.asList(s.split(",")))
            .orElse(Arrays.asList("process-instances", "decision-instances", "task-users"));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    String currentDate = ZonedDateTime.now(ZoneId.systemDefault()).format(formatter);
    String groupBy = Optional.ofNullable(queryParams.get("groupBy"))
            .map(Object::toString)
            .orElse("month");
    String subsStartDate = queryParams.get("subscriptionStartDate").toString();
    ZonedDateTime subsStartDateParsed = ZonedDateTime.parse(subsStartDate, formatter);
    if (groupBy.equals("year")) {
      String prevDate = subsStartDateParsed.minusYears(1).format(formatter);
      for (String metric : metricNames) {
          queryData.add(createSumParamsMap(metric, subsStartDate, currentDate));
          queryData.add(createSumParamsMap(metric, prevDate, subsStartDate));
      }
    } else if (groupBy.equals("month")) {
      String startDate = queryParams.get("startDate").toString();
      ZonedDateTime startDateParsed = ZonedDateTime.parse(startDate, formatter);
      for (ZonedDateTime stDate = startDateParsed; !stDate.isAfter(subsStartDateParsed); stDate = stDate.plusMonths(1)) {
          ZonedDateTime startDayM = stDate.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);
          ZonedDateTime endDayM = stDate.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999_000_000);
          for (String metric : metricNames) {
             queryData.add(createSumParamsMap(metric, startDayM.format(formatter), endDayM.format(formatter)));
          }
      }
    }
    for (Map<String, Object> params : queryData) {
      Metric metricsData = new Metric();
      metricsData.setMetric(params.get("metric").toString());
      ZonedDateTime startDate = ZonedDateTime.parse(params.get("startDate").toString(), formatter);
      metricsData.setSubscriptionYear(startDate.getYear());
      if (groupBy.equals("month")) {
        metricsData.setSubscriptionMonth(startDate.getMonthValue());
      }
      int count = getSum(metricsData.getMetric(), params, user);
      metricsData.setSum(count);
      metrics.add(metricsData);
    }
    return metrics;
  }

  private Map<String, Object> createSumParamsMap(String metric, String startDate, String endDate) {
    Map<String, Object> params = new HashMap<>();
    params.put("metric", metric);
    params.put("startDate", startDate);
    params.put("endDate", endDate);
    return params;
  }
  
  private int getSum(String metricsName, Map<String, Object> queryParams, CIBUser user) {
    DateConverter dateConverter = new DateConverter();
    dateConverter.setObjectMapper(objectMapper);

    long result = 0;

    if (Metrics.UNIQUE_TASK_WORKERS.equals(metricsName) || Metrics.TASK_USERS.equals(metricsName)) {
      result = processEngine.getManagementService().getUniqueTaskWorkerCount(
          extractStartDate(queryParams, dateConverter),
          extractEndDate(queryParams, dateConverter));
    } else {
      MetricsQuery query = processEngine.getManagementService()
        .createMetricsQuery()
        .name(metricsName);

      applyQueryParams(queryParams, dateConverter, query);
      result = query.sum();
    }
    return (int)result;
  }
  
  private void applyQueryParams(Map<String, Object> queryParameters, DateConverter dateConverter, MetricsQuery query) {
    Date startDate = extractStartDate(queryParameters, dateConverter);
    Date endDate = extractEndDate(queryParameters, dateConverter);
    if (startDate != null) {
      query.startDate(startDate);
    }
    if (endDate != null) {
      query.endDate(endDate);
    }
  }

  private Date extractEndDate(Map<String, Object> queryParameters, DateConverter dateConverter) {
    if(queryParameters.containsKey("endDate")) {
      return dateConverter.convertQueryParameterToType((String)queryParameters.get("endDate"));
    }
    return null;
  }

  private Date extractStartDate(Map<String, Object> queryParameters, DateConverter dateConverter) {
    if(queryParameters.containsKey("startDate")) {
      return dateConverter.convertQueryParameterToType((String)queryParameters.get("startDate"));
    }
    return null;
  }

  /*
      
  ████████ ███████ ███    ██  █████  ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
     ██    ██      ████   ██ ██   ██ ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
     ██    █████   ██ ██  ██ ███████ ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
     ██    ██      ██  ██ ██ ██   ██ ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
     ██    ███████ ██   ████ ██   ██ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                       
  */
  
  //@Override
  public Collection<Tenant> fetchTenants(Map<String, Object> queryParams, CIBUser user) {
    //TODO: not tested
    TenantQueryDto queryDto = objectMapper.convertValue(queryParams, TenantQueryDto.class);

    TenantQuery query = queryDto.toQuery(processEngine);
    List<org.cibseven.bpm.engine.identity.Tenant> tenants = QueryUtil.list(query, null, null);
    List<Tenant> tenantList = new ArrayList<>();
    List<TenantDto> tennantDtoList = TenantDto.fromTenantList(tenants);
    for (TenantDto tennantDto : tennantDtoList) {
      tenantList.add(convertValue(tennantDto, Tenant.class));
    }
    return tenantList;
  }

  //@Override
  public Tenant fetchTenant(String tenantId, CIBUser user) {
    //TODO: not tested
    org.cibseven.bpm.engine.identity.Tenant tenant = findTenantObject(tenantId);    
    TenantDto dto = TenantDto.fromTenant(tenant);
    return convertValue(tenant, Tenant.class);
  }

  //@Override
  public void createTenant(Tenant tenant, CIBUser user) {
    //TODO: not tested
    ensureNotReadOnly();
    TenantDto tenantDto = convertValue(tenant, TenantDto.class);

    org.cibseven.bpm.engine.identity.Tenant newTenant = identityService.newTenant(tenantDto.getId());
    tenantDto.update(newTenant);

    identityService.saveTenant(newTenant);
}

  //@Override
  public void updateTenant(Tenant tenant, CIBUser user) {
    //TODO: not tested
      ensureNotReadOnly();
      TenantDto tenantDto = convertValue(tenant, TenantDto.class);
      org.cibseven.bpm.engine.identity.Tenant systemTenant = findTenantObject(tenant.getId());
      if(systemTenant == null) {
        throw new SystemException("Tenant with id " + tenant.getId() + " does not exist");
      }
      tenantDto.update(systemTenant);
      identityService.saveTenant(systemTenant);
  }
  
  private org.cibseven.bpm.engine.identity.Tenant findTenantObject(String tenantId) {
    try {
      return identityService.createTenantQuery()
          .tenantId(tenantId)
          .singleResult();

    } catch(ProcessEngineException e) {
      throw new SystemException("Exception while performing tenant query: " + e.getMessage());
    }
  }
  
  //@Override
  public void deleteTenant(String tenantId, CIBUser user) {
    //TODO: not tested
    ensureNotReadOnly();
    identityService.deleteTenant(tenantId);
  }

  //@Override
  public void addMemberToTenant(String tenantId, String userId, CIBUser user) {
    //TODO: not tested
    ensureNotReadOnly();
    identityService.createTenantUserMembership(tenantId, userId);
  }

  //@Override
  public void deleteMemberFromTenant(String tenantId, String userId, CIBUser user) {
    //TODO: not tested
    ensureNotReadOnly();
    identityService.deleteTenantUserMembership(tenantId, userId);
  }

  //@Override
  public void addGroupToTenant(String tenantId, String groupId, CIBUser user) {
    //TODO: not tested
    ensureNotReadOnly();
    identityService.createTenantGroupMembership(tenantId, groupId);
  }
  
  //@Override
  public void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user) {
    //TODO: not tested
    ensureNotReadOnly();
    identityService.deleteTenantGroupMembership(tenantId, groupId);
}

  private void ensureNotReadOnly() {
    if(identityService.isReadOnly()) {
      throw new InvalidRequestException(Status.FORBIDDEN, "Identity service implementation is read-only.");
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
  public Deployment createDeployment(MultiValueMap<String, Object> data, MultipartFile[] files, CIBUser user) throws SystemException {
    //TODO: in progress
    //TODO: add authorization
    MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>(data);
    for (int i = 0; i < files.length; i++) {
      MultipartFile file = files[i];
      // API expects files with parameter name "data", so we use indexed keys
      // result could be: ['data0', 'data1', 'data2', ...] or just single ['data']
      String key = files.length > 1 ? "data" + i : "data";
      formData.add(key, file.getResource());
    }
    DeploymentBuilder deploymentBuilder = extractDeploymentInformation(convertValue(formData, MultipartFormData.class));

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
      return convertValue(deploymentDto, Deployment.class);

    } else {
      throw new SystemException("No deployment resources contained in the form upload.");
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
}
