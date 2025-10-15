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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.Data;
import org.cibseven.webapp.NamedByteArrayDataSource;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.ExpressionEvaluationException;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnexpectedTypeException;
import org.cibseven.webapp.exception.UnsupportedTypeException;
import org.cibseven.webapp.rest.model.ActivityInstance;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.Batch;
import org.cibseven.webapp.rest.model.CandidateGroupTaskCount;
import org.cibseven.webapp.rest.model.Decision;
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.rest.model.DeploymentResource;
import org.cibseven.webapp.rest.model.EventSubscription;
import org.cibseven.webapp.rest.model.ExternalTask;
import org.cibseven.webapp.rest.model.Filter;
import org.cibseven.webapp.rest.model.HistoricDecisionInstance;
import org.cibseven.webapp.rest.model.HistoryBatch;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Incident;
import org.cibseven.webapp.rest.model.Job;
import org.cibseven.webapp.rest.model.JobDefinition;
import org.cibseven.webapp.rest.model.Message;
import org.cibseven.webapp.rest.model.Metric;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.ProcessInstance;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.ProcessStatistics;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.rest.model.StartForm;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskHistory;
import org.cibseven.webapp.rest.model.Tenant;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.cibseven.webapp.rest.model.VariableInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;

public class SevenProvider extends SevenProviderBase implements BpmProvider {
		@Autowired private IDeploymentProvider deploymentProvider;
    @Autowired private IVariableProvider variableProvider;
    @Autowired private IVariableInstanceProvider variableInstanceProvider;
    @Autowired private IHistoricVariableInstanceProvider historicVariableInstanceProvider;
    @Autowired private ITaskProvider taskProvider;
    @Autowired private IProcessProvider processProvider;
    @Autowired private IActivityProvider activityProvider;
    @Autowired private IFilterProvider filterProvider;
    @Autowired private IUtilsProvider utilsProvider;
    @Autowired private IIncidentProvider incidentProvider;
    @Autowired private IJobDefinitionProvider jobDefinitionProvider;
    @Autowired private IUserProvider userProvider;
    @Autowired private IDecisionProvider decisionProvider;
    @Autowired private IJobProvider jobProvider;
    @Autowired private IBatchProvider batchProvider;
    @Autowired private ISystemProvider systemProvider;
    @Autowired private ITenantProvider tenantProvider;
    @Autowired private IExternalTaskProvider externalTaskProvider;
    
    
    //TODO: forward new direct calls while implementation of direct access is ongoing
    @Autowired private SevenDirectProvider sevenDirectProvider;
    //enable switching to new interface in debugger for in-progress functions
    boolean useDirectInterface = false;
    //enable switching to old interface in debugger for finished functions
    boolean useRestInterface = false;
    /*
	
	████████  █████  ███████ ██   ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	   ██    ██   ██ ██      ██  ██      ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	   ██    ███████ ███████ █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	   ██    ██   ██      ██ ██  ██      ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	   ██    ██   ██ ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                                                                                            
     */
    
	@Override
	public Collection<Task> findTasks(String filter, CIBUser user) {
    if (useDirectInterface)
        return sevenDirectProvider.findTasks(filter, user);
    else
		    return taskProvider.findTasks(filter, user);
	}

	@Override
	public Integer findTasksCount(@RequestBody Map<String, Object> filters, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findTasksCount(filters, user);
    else
		  return taskProvider.findTasksCount(filters, user);
	}
	
	@Override
	public Collection<Task> findTasksByProcessInstance(String processInstanceId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findTasksByProcessInstance(processInstanceId, user);
    else
		  return taskProvider.findTasksByProcessInstance(processInstanceId, user);
	}
	
	@Override
	public Collection<Task> findTasksByProcessInstanceAsignee(Optional<String> processInstanceId, Optional<String> createdAfter, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findTasksByProcessInstanceAsignee(processInstanceId, createdAfter, user);
    else
		  return taskProvider.findTasksByProcessInstanceAsignee(processInstanceId, createdAfter, user);
	}
	
	@Override
	public Task findTaskById(String id, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findTaskById(id, user);
    else
		return taskProvider.findTaskById(id, user);
	}
	

	@Override
	public void update(Task task, CIBUser user) {
		if (useDirectInterface)
		  sevenDirectProvider.update(task, user);
		else
	     taskProvider.update(task, user);
	}
	
	@Override
	public void setAssignee(String taskId, String assignee, CIBUser user) {
    if (!useRestInterface)
      sevenDirectProvider.setAssignee(taskId, assignee, user);
    else
		  taskProvider.setAssignee(taskId, assignee, user);
	}
	
	@Override
	public void submit(String taskId, CIBUser user) {
    if (useDirectInterface)
	    sevenDirectProvider.submit(taskId, user);
	  else
		  taskProvider.submit(taskId, user);
	}
	
	@Override
	public void submit(Task task, List<Variable> formResult, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.submit(task, formResult, user);
    else
		  taskProvider.submit(task, formResult, user);;
	}

	@Override
	public Object formReference(String taskId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.formReference(taskId, user);
    else
      return taskProvider.formReference(taskId, user);
	}
	
	@Override
	public Object form(String taskId, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.form(taskId, user);
    else
      return taskProvider.form(taskId, user);
	}
	
	@Override
	public Collection<Task> findTasksByFilter(TaskFiltering filters, String filterId, CIBUser user, Integer firstResult, Integer maxResults) {
    if (useDirectInterface)
      return sevenDirectProvider.findTasksByFilter(filters, filterId, user, firstResult, maxResults);
    else
      return taskProvider.findTasksByFilter(filters, filterId, user, firstResult, maxResults);
	}
	
	@Override
	public Integer findTasksCountByFilter(String filterId, CIBUser user, TaskFiltering filters) {
    if (useDirectInterface)
      return sevenDirectProvider.findTasksCountByFilter(filterId, user, filters);
    else
      return taskProvider.findTasksCountByFilter(filterId, user, filters);
	}
	
	@Override
	public Collection<TaskHistory> findTasksByProcessInstanceHistory(String processInstanceId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findTasksByProcessInstanceHistory(processInstanceId, user);
    else
		  return taskProvider.findTasksByProcessInstanceHistory(processInstanceId, user);
	}
	
	@Override
	public Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findTasksByDefinitionKeyHistory(taskDefinitionKey, processInstanceId, user);
    else
      return taskProvider.findTasksByDefinitionKeyHistory(taskDefinitionKey, processInstanceId, user);
	}
	
	@Override
	public Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException {
    if (useDirectInterface)
      return sevenDirectProvider.findTasksPost(data, user);
    else
      return taskProvider.findTasksPost(data, user);
	}
	
	@Override
	public Collection<IdentityLink> findIdentityLink(String taskId, Optional<String> type, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findIdentityLink(taskId, type, user);
    else
      return taskProvider.findIdentityLink(taskId, type, user);
	}

	@Override
	public void createIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.createIdentityLink(taskId, data, user);
    else
		  taskProvider.createIdentityLink(taskId, data, user);
	}
	
	@Override
	public void deleteIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.deleteIdentityLink(taskId, data, user);
    else
      taskProvider.deleteIdentityLink(taskId, data, user);
	}
	
	@Override
	public void handleBpmnError(String taskId, Map<String, Object> data, CIBUser user) throws SystemException {
    if (useDirectInterface)
      sevenDirectProvider.handleBpmnError(taskId, data, user);
    else
      taskProvider.handleBpmnError(taskId, data, user);
	}

	@Override
	public Collection<TaskHistory> findTasksByTaskIdHistory(String taskId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findTasksByTaskIdHistory(taskId, user);
    else
      return taskProvider.findTasksByTaskIdHistory(taskId, user);
	}	
	
	@Override
	public ResponseEntity<byte[]> getDeployedForm(String taskId, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.getDeployedForm(taskId, user);
    else
	    return taskProvider.getDeployedForm(taskId, user);
	}
	
	@Override
	public Integer findHistoryTasksCount(Map<String, Object> filters, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findHistoryTasksCount(filters, user);
    else
      return taskProvider.findHistoryTasksCount(filters, user);
	}

	@Override
	public Collection<CandidateGroupTaskCount> getTaskCountByCandidateGroup(CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.getTaskCountByCandidateGroup(user);
    else
      return taskProvider.getTaskCountByCandidateGroup(user);
	}
	
	/* 
	
	██████  ██████   ██████   ██████ ███████ ███████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██   ██ ██   ██ ██    ██ ██      ██      ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██████  ██████  ██    ██ ██      █████   ███████ ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██      ██   ██ ██    ██ ██      ██           ██      ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██      ██   ██  ██████   ██████ ███████ ███████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                          
	 */
	
	@Override
	public Collection<Process> findProcesses(CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findProcesses(user);
    else
		  return processProvider.findProcesses(user);
	}
	
	@Override
	public Collection<Process> findProcessesWithInfo(CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findProcessesWithInfo(user);
    else
		  return processProvider.findProcessesWithInfo(user);
	}
	
	@Override
	public Collection<Process> findProcessesWithFilters(String filters, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findProcessesWithFilters(filters, user);
    else
      return processProvider.findProcessesWithFilters(filters, user);
	}	
	
	@Override
	public Process findProcessByDefinitionKey(String key, String tenantId, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findProcessByDefinitionKey(key, tenantId, user);
    else
		  return processProvider.findProcessByDefinitionKey(key, tenantId, user);
	}
	
	@Override
	public Collection<Process> findProcessVersionsByDefinitionKey(String key, String tenantId, Optional<Boolean> lazyLoad, CIBUser user) {
    //TODO: not implemented
	  if (useDirectInterface)
      return sevenDirectProvider.findProcessVersionsByDefinitionKey(key, tenantId, lazyLoad, user);
    else
      return processProvider.findProcessVersionsByDefinitionKey(key, tenantId, lazyLoad, user);
	}

	@Override
	public Process findProcessById(String id, Optional<Boolean> extraInfo, CIBUser user) throws SystemException {
    if (!useRestInterface)
      return sevenDirectProvider.findProcessById(id, extraInfo, user);
    else
      return processProvider.findProcessById(id, extraInfo, user);
	}
		
	@Override
	public Collection<ProcessInstance> findProcessesInstances(String key, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findProcessesInstances(key, user);
    else
      return processProvider.findProcessesInstances(key, user);
	}
	
	@Override
	public ProcessDiagram fetchDiagram(String id, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.fetchDiagram(id, user);
    else
		  return processProvider.fetchDiagram(id, user);
	}
	
	@Override
	public StartForm fetchStartForm(String processDefinitionId, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.fetchStartForm(processDefinitionId, user);
    else
      return processProvider.fetchStartForm(processDefinitionId, user);
	}
	
	@Override
	public Data downloadBpmn(String id, String fileName, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.downloadBpmn(id, fileName, user);
    else
      return processProvider.downloadBpmn(id, fileName, user);
	}
	
	@Override
	public void suspendProcessInstance(String processInstanceId, Boolean suspend, CIBUser user) {
    if (!useRestInterface)
      sevenDirectProvider.suspendProcessInstance(processInstanceId, suspend, user);
    else
		  processProvider.suspendProcessInstance(processInstanceId, suspend, user);
	}
	
	@Override
	public void deleteProcessInstance(String processInstanceId, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.deleteProcessInstance(processInstanceId, user);
    else
      processProvider.deleteProcessInstance(processInstanceId, user);
	}
	
	@Override
	public void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.suspendProcessDefinition(processDefinitionId, suspend, includeProcessInstances, executionDate, user);
    else
      processProvider.suspendProcessDefinition(processDefinitionId, suspend, includeProcessInstances, executionDate, user);
	}
	
	@Override
	public ProcessStart startProcess(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
    if (!useRestInterface)
      return sevenDirectProvider.startProcess(processDefinitionKey, tenantId, data, user);
    else
		  return processProvider.startProcess(processDefinitionKey, tenantId, data, user);
	}
	
	@Override
	public ProcessStart submitForm(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
    if (!useRestInterface)
      return sevenDirectProvider.submitForm(processDefinitionKey, tenantId, data, user);
    else
      return processProvider.submitForm(processDefinitionKey, tenantId, data, user);
	}
	
	@Override
	public Collection<ProcessStatistics> findProcessStatistics(String processId, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
    if (!useRestInterface)
      return sevenDirectProvider.findProcessStatistics(processId, user);
    else
		  return processProvider.findProcessStatistics(processId, user);
	}

  @Override
  public Collection<ProcessStatistics> getProcessStatistics(Map<String, Object> queryParams, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.getProcessStatistics(queryParams, user);
    else
     return processProvider.getProcessStatistics(queryParams, user);
  }
	
	@Override
	public Collection<HistoryProcessInstance> findProcessesInstancesHistory(Map<String, Object> filters,
			Optional<Integer> firstResult, Optional<Integer> maxResults, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findProcessesInstancesHistory(filters, firstResult, maxResults, user);
    else
		  return processProvider.findProcessesInstancesHistory(filters, firstResult, maxResults, user);
	}
	
	@Override
	public Collection<HistoryProcessInstance> findProcessesInstancesHistory(String key, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, CIBUser user) {
	  if (useDirectInterface)
      return sevenDirectProvider.findProcessesInstancesHistory(key, active, firstResult, maxResults, user);
    else
      return processProvider.findProcessesInstancesHistory(key, active, firstResult, maxResults, user);
	}
	
	@Override
	public Collection<HistoryProcessInstance> findProcessesInstancesHistoryById(String id, Optional<String> activityId, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, String text, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findProcessesInstancesHistoryById(id, activityId, active, firstResult, maxResults, text, user);
    else
      return processProvider.findProcessesInstancesHistoryById(id, activityId, active, firstResult, maxResults, text, user);
	}
	
	@Override
	public Long countProcessesInstancesHistory(Map<String, Object> filters, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.countProcessesInstancesHistory(filters, user);
    else
      return processProvider.countProcessesInstancesHistory(filters, user);
	}
	
	@Override
	public ProcessInstance findProcessInstance(String processInstanceId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findProcessInstance(processInstanceId, user);
    else
		  return processProvider.findProcessInstance(processInstanceId, user);
	}

	@Override
	public Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, boolean deserializeValue, CIBUser user) throws SystemException  {
    if (useDirectInterface)
      return sevenDirectProvider.fetchProcessInstanceVariable(processInstanceId, variableName, deserializeValue, user);
    else
      return processProvider.fetchProcessInstanceVariable(processInstanceId, variableName, deserializeValue, user);
	}
	
	@Override
	public HistoryProcessInstance findHistoryProcessInstanceHistory(String processInstanceId, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findHistoryProcessInstanceHistory(processInstanceId, user);
    else
		  return processProvider.findHistoryProcessInstanceHistory(processInstanceId, user);
	}
	
	@Override
	public Collection<Process> findCalledProcessDefinitions(String processDefinitionId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findCalledProcessDefinitions(processDefinitionId, user);
    else
		  return processProvider.findCalledProcessDefinitions(processDefinitionId, user);
	}
	
	@Override
	public ResponseEntity<byte[]> getDeployedStartForm(String processDefinitionId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.getDeployedStartForm(processDefinitionId, user);
    else
      return processProvider.getDeployedStartForm(processDefinitionId, user);
	}

	@Override
	public void updateHistoryTimeToLive(String id, Map<String, Object> data, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.updateHistoryTimeToLive(id, data, user);
    else
      processProvider.updateHistoryTimeToLive(id, data, user);
	}

	@Override
	public void deleteProcessInstanceFromHistory(String id, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.deleteProcessInstanceFromHistory(id, user);
    else
      processProvider.deleteProcessInstanceFromHistory(id, user);
	}
	
	@Override
	public void deleteProcessDefinition(String id, Optional<Boolean> cascade, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.deleteProcessDefinition(id, cascade, user);
    else
      processProvider.deleteProcessDefinition(id, cascade, user);
	}
	
	@Override
	public Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user)
			throws SystemException {
    if (!useRestInterface)
      return sevenDirectProvider.findCurrentProcessesInstances(data, user);
    else
		  return processProvider.findCurrentProcessesInstances(data, user);
	}

	@Override
	public Object fetchHistoricActivityStatistics(String id, Map<String, Object> params, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.fetchHistoricActivityStatistics(id, params, user);
    else
      return processProvider.fetchHistoricActivityStatistics(id, params, user);
	}
	
	/*		
	
	███████ ██ ██      ████████ ███████ ██████      ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██      ██ ██         ██    ██      ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	█████   ██ ██         ██    █████   ██████      ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██      ██ ██         ██    ██      ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██      ██ ███████    ██    ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                                                                            
	 */
	
	@Override
	public Collection<Filter> findFilters(CIBUser user) {
	  if (!useRestInterface)
	    return sevenDirectProvider.findFilters(user);
	  else
		  return filterProvider.findFilters(user);
	}

	@Override
	public  Filter createFilter(Filter filter, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.createFilter(filter, user);
    else
		  return filterProvider.createFilter(filter, user);
	}

	@Override
	public void updateFilter(Filter filter, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.updateFilter(filter, user);
    else
      filterProvider.updateFilter(filter, user);
	}

	@Override
	public void deleteFilter(String filterId, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.deleteFilter(filterId, user);
    else
      filterProvider.deleteFilter(filterId, user);
	}
	
	/*
	
	██████  ███████ ██████  ██       ██████  ██    ██ ███    ███ ███████ ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██   ██ ██      ██   ██ ██      ██    ██  ██  ██  ████  ████ ██      ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██   ██ █████   ██████  ██      ██    ██   ████   ██ ████ ██ █████   ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██   ██ ██      ██      ██      ██    ██    ██    ██  ██  ██ ██      ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██████  ███████ ██      ███████  ██████     ██    ██      ██ ███████ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                                                                                                                                                                                                         
	 */
	
	@Override
	public Deployment deployBpmn(MultiValueMap<String, Object> data, MultiValueMap<String, MultipartFile> file, CIBUser user) throws SystemException {
    if (useDirectInterface)
      return sevenDirectProvider.deployBpmn(data, file, user);
    else
      return deploymentProvider.deployBpmn(data, file, user);
		
	}

	@Override
	public Long countDeployments(CIBUser user, String nameLike) {
    if (!useRestInterface)
      return sevenDirectProvider.countDeployments(user, nameLike);
    else
      return deploymentProvider.countDeployments(user, nameLike);
	}

	@Override
	public Collection<Deployment> findDeployments(CIBUser user, String nameLike, int firstResult, int maxResults, String sortBy, String sortOrder) {
    if (!useRestInterface)
      return sevenDirectProvider.findDeployments(user, nameLike, firstResult, maxResults, sortBy, sortOrder);
    else
      return deploymentProvider.findDeployments(user, nameLike, firstResult, maxResults, sortBy, sortOrder);
	}
	
	@Override
	public Deployment findDeployment(String deploymentId, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findDeployment(deploymentId, user);
    else
      return deploymentProvider.findDeployment(deploymentId, user);
	}

	@Override
	public Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findDeploymentResources(deploymentId, user);
    else
      return deploymentProvider.findDeploymentResources(deploymentId, user);
	}

	@Override
	public Data fetchDataFromDeploymentResource(HttpServletRequest rq, String deploymentId, String resourceId, String fileName) {
    if (useDirectInterface)
      return sevenDirectProvider.fetchDataFromDeploymentResource(rq, deploymentId, resourceId, fileName);
    else
      return deploymentProvider.fetchDataFromDeploymentResource(rq, deploymentId, resourceId, fileName);
	}
	
	@Override
	public void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException {
    if (!useRestInterface)
      sevenDirectProvider.deleteDeployment(deploymentId, cascade, user);
    else
      deploymentProvider.deleteDeployment(deploymentId, cascade, user);
	}
		
	/*
	
	 █████   ██████ ████████ ██ ██    ██ ██ ████████ ██    ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██   ██ ██         ██    ██ ██    ██ ██    ██     ██  ██      ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	███████ ██         ██    ██ ██    ██ ██    ██      ████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██   ██ ██         ██    ██  ██  ██  ██    ██       ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██   ██  ██████    ██    ██   ████   ██    ██       ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                           
	 */
	
	@Override
	public ActivityInstance findActivityInstance(String processInstanceId, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findActivityInstance(processInstanceId, user);
    else
		  return activityProvider.findActivityInstance(processInstanceId, user);
	}
	
	@Override
	public List<ActivityInstanceHistory> findActivitiesInstancesHistory(Map<String, Object> queryParams, CIBUser user) {
	  if (useDirectInterface)
      return sevenDirectProvider.findActivitiesInstancesHistory(queryParams, user);
    else
      return activityProvider.findActivitiesInstancesHistory(queryParams, user);
	}
	
	@Override
	public List<ActivityInstanceHistory> findActivitiesInstancesHistory(String processInstanceId, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findActivitiesInstancesHistory(processInstanceId, user);
    else
      return activityProvider.findActivitiesInstancesHistory(processInstanceId, user);
	}
	
	@Override
	public ActivityInstance findActivityInstances(String processInstanceId, CIBUser user) throws SystemException {
    if (useDirectInterface)
      return sevenDirectProvider.findActivityInstances(processInstanceId, user);
    else
      return activityProvider.findActivityInstances(processInstanceId, user);
	}
	
	@Override
	public List<ActivityInstanceHistory> findActivityInstanceHistory(String processInstanceId, CIBUser user) throws SystemException {
    if (useDirectInterface)
      return sevenDirectProvider.findActivityInstanceHistory(processInstanceId, user);
    else
      return activityProvider.findActivityInstanceHistory(processInstanceId, user);
	}	

	@Override
	public void deleteVariableByExecutionId(String executionId, String variableName, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.deleteVariableByExecutionId(executionId, variableName, user);
    else
      activityProvider.deleteVariableByExecutionId(executionId, variableName, user);
	}

	@Override
	public void deleteVariableHistoryInstance(String id, CIBUser user) {
	  if (!useRestInterface)
	    sevenDirectProvider.deleteVariableHistoryInstance(id, user);
	  else
	    activityProvider.deleteVariableHistoryInstance(id, user);
	}	
	
	/*
	
	██    ██ ████████ ██ ██      ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██    ██    ██    ██ ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██    ██    ██    ██ ██      ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██    ██    ██    ██ ██           ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	 ██████     ██    ██ ███████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                                                                                            
	 */
	
	@Override
	public Collection<Message> correlateMessage(Map<String, Object> data, CIBUser user) throws SystemException {
    if (useDirectInterface)
      return sevenDirectProvider.correlateMessage(data, user);
    else
      return utilsProvider.correlateMessage(data, user);
	}
	
	@Override
	public String findStacktrace(String jobId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findStacktrace(jobId, user);
    else
      return utilsProvider.findStacktrace(jobId, user);
	}
	
	@Override
	public String findExternalTaskErrorDetails(String externalTaskId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findExternalTaskErrorDetails(externalTaskId, user);
    else
	    return incidentProvider.findExternalTaskErrorDetails(externalTaskId, user);
	}
	
	@Override
	public String findHistoricExternalTaskErrorDetails(String externalTaskId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findHistoricExternalTaskErrorDetails(externalTaskId, user);
    else
		  return incidentProvider.findHistoricExternalTaskErrorDetails(externalTaskId, user);
	}
	
	@Override
	public Collection<Incident> findHistoricIncidents(Map<String, Object> params, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findHistoricIncidents(params, user);
    else
		  return incidentProvider.findHistoricIncidents(params, user);
	}
	
	@Override
	public String findHistoricStacktraceByJobId(String jobId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findHistoricStacktraceByJobId(jobId, user);
    else
      return incidentProvider.findHistoricStacktraceByJobId(jobId, user);
	}
	
	@Override
	public void retryJobById(String jobId, Map<String, Object> data, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.retryJobById(jobId, data, user);
    else
      utilsProvider.retryJobById(jobId, data, user);
	}

	@Override
	public void retryExternalTask(String externalTaskId, Map<String, Object> data, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.retryExternalTask(externalTaskId, data, user);
    else
      incidentProvider.retryExternalTask(externalTaskId, data, user);
	}

	@Override
	public Collection<EventSubscription> getEventSubscriptions(Optional<String> processInstanceId,
			Optional<String> eventType, Optional<String> eventName, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.getEventSubscriptions(processInstanceId, eventType, eventName, user);
    else
      return utilsProvider.getEventSubscriptions(processInstanceId, eventType, eventName, user);
	}	
	
	/*

	██    ██ ███████ ███████ ██████      ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██    ██ ██      ██      ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██    ██ ███████ █████   ██████      ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██    ██      ██ ██      ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	 ██████  ███████ ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                  
	*/
	
	@Override
	public Authorizations getUserAuthorization(String userId, CIBUser user) {
	  if (!useRestInterface)
      return sevenDirectProvider.getUserAuthorization(userId, user);
    else
		  return userProvider.getUserAuthorization(userId, user);
	}
	
	public Collection<SevenUser> fetchUsers(CIBUser user) throws SystemException {
    if (useDirectInterface)
      return sevenDirectProvider.fetchUsers(user);
    else
	    return userProvider.fetchUsers(user);
	}
	
	public SevenVerifyUser verifyUser(String username, String password, CIBUser user) throws SystemException {
    if (!useRestInterface)
      return sevenDirectProvider.verifyUser(username, password, user);
    else
		  return userProvider.verifyUser(username, password, user);
	}
	
	@Override
	public Collection<User> findUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike, Optional<String> lastName, Optional<String> lastNameLike,
			Optional<String> email, Optional<String> emailLike, Optional<String> memberOfGroup, Optional<String> memberOfTenant, Optional<String> idIn, 
			Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder, CIBUser user) {
    if (!useRestInterface)
      return sevenDirectProvider.findUsers(id, firstName, firstNameLike, lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder, user);
    else
	    return userProvider.findUsers(id, firstName, firstNameLike, lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder, user);
	}
	
	@Override
	public void createUser(NewUser user, CIBUser flowUser) throws InvalidUserIdException {
    if (!useRestInterface)
      sevenDirectProvider.createUser(user, flowUser);
    else
		  userProvider.createUser(user, flowUser);
	}
	
	@Override
	public void updateUserProfile(String userId, User user, CIBUser flowUser) {
    if (useDirectInterface)
      sevenDirectProvider.updateUserProfile(userId, user, flowUser);
    else
		  userProvider.updateUserProfile(userId, user, flowUser);
	}
	
	@Override
	public void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.updateUserCredentials(userId, data, user);
    else
      userProvider.updateUserCredentials(userId, data, user);
	}
	
	@Override
	public void addMemberToGroup(String groupId, String userId, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.addMemberToGroup(groupId, userId, user);
    else
		  userProvider.addMemberToGroup(groupId, userId, user);
	}
	
	@Override
	public void deleteMemberFromGroup(String groupId, String userId, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.deleteMemberFromGroup(groupId, userId, user);
    else
      userProvider.deleteMemberFromGroup(groupId, userId, user);
	}

	@Override
	public void deleteUser(String userId, CIBUser user) {
    if (!useRestInterface)
      sevenDirectProvider.deleteUser(userId, user);
    else
	    userProvider.deleteUser(userId, user);
	}
	
	@Override
	public SevenUser getUserProfile(String userId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.getUserProfile(userId, user);
    else
		  return userProvider.getUserProfile(userId, user);
	}

	@Override
	public Collection<UserGroup> findGroups(Optional<String> id, Optional<String> name, Optional<String> nameLike, Optional<String> type,
			Optional<String> member, Optional<String> memberOfTenant, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
			Optional<String> maxResults, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findGroups(id, name, nameLike, type, member, memberOfTenant, sortBy, sortOrder, firstResult, maxResults, user);
    else
      return userProvider.findGroups(id, name, nameLike, type, member, memberOfTenant, sortBy, sortOrder, firstResult, maxResults, user);
	}

	@Override
	public void createGroup(UserGroup group, CIBUser user) {
    if (!useRestInterface)
      sevenDirectProvider.createGroup(group, user);
    else
	    userProvider.createGroup(group, user);
	}

	@Override
	public void updateGroup(String groupId, UserGroup group, CIBUser user) {
    if (!useRestInterface)
      sevenDirectProvider.updateGroup(groupId, group, user);
    else
      userProvider.updateGroup(groupId, group, user);
	}

	@Override
	public void deleteGroup(String groupId, CIBUser user) {
    if (!useRestInterface)
      sevenDirectProvider.deleteGroup(groupId, user);
    else
      userProvider.deleteGroup(groupId, user);
	}

	@Override
	public Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type, Optional<String> userIdIn, Optional<String> groupIdIn,
			Optional<String> resourceType, Optional<String> resourceId, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
			Optional<String> maxResults, CIBUser user) {
	  if (useDirectInterface)
      return sevenDirectProvider.findAuthorization(id, type, userIdIn, groupIdIn, resourceType, resourceId, sortBy, sortOrder, firstResult, maxResults, user);
    else
      return userProvider.findAuthorization(id, type, userIdIn, groupIdIn, resourceType, resourceId, sortBy, sortOrder, firstResult, maxResults, user);
	}

	@Override
	public ResponseEntity<Authorization> createAuthorization(Authorization authorization, CIBUser user) {
	  if (!useRestInterface)
	    return sevenDirectProvider.createAuthorization(authorization, user);
	  else
	    return userProvider.createAuthorization(authorization, user);
	}

	@Override
	public void updateAuthorization(String authorizationId, Map<String, Object> data, CIBUser user) {
    if (!useRestInterface)
      sevenDirectProvider.updateAuthorization(authorizationId, data, user);
    else
      userProvider.updateAuthorization(authorizationId, data, user);
	}

	@Override
	public void deleteAuthorization(String authorizationId, CIBUser user) {
    if (!useRestInterface)
      sevenDirectProvider.deleteAuthorization(authorizationId, user);
    else
      userProvider.deleteAuthorization(authorizationId, user);
	}

	/*
	
	██ ███    ██  ██████ ██ ██████  ███████ ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██ ████   ██ ██      ██ ██   ██ ██      ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██ ██ ██  ██ ██      ██ ██   ██ █████   ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██ ██  ██ ██ ██      ██ ██   ██ ██      ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██ ██   ████  ██████ ██ ██████  ███████ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                            
	 */
	
	@Override
	public Long countIncident(Map<String, Object> params, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.countIncident(params, user);
    else
      return incidentProvider.countIncident(params, user);
	}

	@Override
	public Collection<Incident> findIncident(Map<String, Object> params, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findIncident(params, user);
    else
      return incidentProvider.findIncident(params, user);
	}
	
	@Override
	public List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findIncidentByInstanceId(processInstanceId, user);
    else
		  return incidentProvider.findIncidentByInstanceId(processInstanceId, user);
	}

	@Override
	public Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user) {
	  if (useDirectInterface)
      return sevenDirectProvider.fetchIncidents(processDefinitionKey, user);
    else
  		return incidentProvider.fetchIncidents(processDefinitionKey, user);
	}
	
	@Override
	public Collection<Incident> fetchIncidentsByInstanceAndActivityId(String processDefinitionKey, String activityId, CIBUser user) {
		if (useDirectInterface)
		  return sevenDirectProvider.fetchIncidentsByInstanceAndActivityId(processDefinitionKey, activityId, user);
		else
	    return incidentProvider.fetchIncidentsByInstanceAndActivityId(processDefinitionKey, activityId, user);
	}
	
	@Override
	public void setIncidentAnnotation(String incidentId, Map<String, Object> data, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.setIncidentAnnotation(incidentId, data, user);
    else
      incidentProvider.setIncidentAnnotation(incidentId, data, user);
	}

	/*
	
	██    ██  █████  ██████  ██  █████  ██████  ██      ███████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██    ██ ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██    ██ ███████ ██████  ██ ███████ ██████  ██      █████   ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	 ██  ██  ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██           ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	  ████   ██   ██ ██   ██ ██ ██   ██ ██████  ███████ ███████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                                     
	*/
	
	@Override
	public void modifyVariableByExecutionId(String executionId, Map<String, Object> data, CIBUser user) throws SystemException {
//    if (useDirectInterface)
//      sevenDirectProvider.modifyVariableByExecutionId(executionId, data, user);
//    else
		  variableProvider.modifyVariableByExecutionId(executionId, data, user);
	}
	
	@Override
	public void modifyVariableDataByExecutionId(String executionId, String variableName, MultipartFile data, String valueType, CIBUser user) throws SystemException {
  if (useDirectInterface)
    sevenDirectProvider.modifyVariableDataByExecutionId(executionId, variableName, data, valueType, user);
  else
		variableProvider.modifyVariableDataByExecutionId(executionId, variableName, data, valueType, user);
	}
	
	@Override
	public Collection<Variable> fetchProcessInstanceVariables(String processInstanceId, Map<String, Object> data, CIBUser user) throws SystemException {
    if (useDirectInterface)
      return sevenDirectProvider.fetchProcessInstanceVariables(processInstanceId, data, user);
    else
		  return variableProvider.fetchProcessInstanceVariables(processInstanceId, data, user);
	}
	
	@Override
	public ResponseEntity<byte[]> fetchVariableDataByExecutionId(String executionId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException  {
    if (useDirectInterface)
      return sevenDirectProvider.fetchVariableDataByExecutionId(executionId, variableName, user);
    else
      return variableProvider.fetchVariableDataByExecutionId(executionId, variableName, user);
	}	
	
	@Override
	public Collection<VariableHistory> fetchProcessInstanceVariablesHistory(String processInstanceId, Map<String, Object> data, CIBUser user) throws SystemException {
    if (!useRestInterface)
      return sevenDirectProvider.fetchProcessInstanceVariablesHistory(processInstanceId, data, user);
    else
		  return variableProvider.fetchProcessInstanceVariablesHistory(processInstanceId, data, user);
	}
	
	@Override
	public Collection<VariableHistory> fetchActivityVariablesHistory(String activityInstanceId, CIBUser user) {
		return variableProvider.fetchActivityVariablesHistory(activityInstanceId, user);
	}
	
	@Override
	public Collection<VariableHistory> fetchActivityVariables(String activityInstanceId, CIBUser user) {
		return variableProvider.fetchActivityVariables(activityInstanceId, user);
	}
	
	@Override
	public ResponseEntity<byte[]> fetchHistoryVariableDataById(String id, CIBUser user) throws NoObjectFoundException, SystemException  {
	  if (useDirectInterface)
	    return sevenDirectProvider.fetchHistoryVariableDataById(id, user);
	  else
	    return variableProvider.fetchHistoryVariableDataById(id, user);
	}
	
	@Override
	public Variable fetchVariable(String taskId, String variableName, 
			boolean deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException {		
		return variableProvider.fetchVariable(taskId, variableName, deserializeValue, user);
	}
	
	@Override
	public void deleteVariable(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException {		
		variableProvider.deleteVariable(taskId, variableName, user);
	}
	
	@Override
	public Map<String, Variable> fetchFormVariables(String taskId, boolean deserializeValues, CIBUser user) throws NoObjectFoundException, SystemException {
		return variableProvider.fetchFormVariables(taskId, deserializeValues, user);
	}
	
	@Override
	public Map<String, Variable> fetchFormVariables(List<String> variableListName, String taskId, CIBUser user) throws NoObjectFoundException, SystemException {
		return variableProvider.fetchFormVariables(variableListName, taskId, user);
	}
	
	@Override
	public Map<String, Variable> fetchProcessFormVariables(String key, CIBUser user) throws NoObjectFoundException, SystemException {
		return variableProvider.fetchProcessFormVariables(key, user);
	}
	
	@Override
	public NamedByteArrayDataSource fetchVariableFileData(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, UnexpectedTypeException, SystemException {		
		return variableProvider.fetchVariableFileData(taskId, variableName, user);
	}
	
	@Override
	public void uploadVariableFileData(String taskId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException {
		variableProvider.uploadVariableFileData(taskId, variableName, data, valueType, user);
	}
	
	@Override
	public ResponseEntity<byte[]> fetchProcessInstanceVariableData(String processInstanceId, String variableName,
			CIBUser user) throws NoObjectFoundException, SystemException {
		return variableProvider.fetchProcessInstanceVariableData(processInstanceId, variableName, user);
	}
	
	@Override
	public void uploadProcessInstanceVariableFileData(String processInstanceId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException {
		variableProvider.uploadProcessInstanceVariableFileData(processInstanceId, variableName, data, valueType, user);
	}
	
	@Override
	public ProcessStart submitStartFormVariables(String processDefinitionId, List<Variable> formResult, CIBUser user) throws SystemException {
		return variableProvider.submitStartFormVariables(processDefinitionId, formResult, user);
	}
	
	@Override
	public Variable fetchVariableByProcessInstanceId(String processInstanceId, String variableName, CIBUser user) throws SystemException {
		return variableProvider.fetchVariableByProcessInstanceId(processInstanceId, variableName, user);
	}

	@Override
	public void saveVariableInProcessInstanceId(String processInstanceId, List<Variable> variables, CIBUser user) throws SystemException {
		variableProvider.saveVariableInProcessInstanceId(processInstanceId, variables, user);
	}
	
	@Override
	public void submitVariables(String processInstanceId, List<Variable> formResult, CIBUser user, String processDefinitionId) throws SystemException {
		variableProvider.submitVariables(processInstanceId, formResult, user, processDefinitionId);
	}
	
	@Override
	public Map<String, Variable> fetchProcessFormVariablesById(String id, CIBUser user) throws SystemException {
		return variableProvider.fetchProcessFormVariablesById(id, user);
	}
	
	@Override
	public void putLocalExecutionVariable(String executionId, String varName, Map<String, Object> data, CIBUser user) {
		variableProvider.putLocalExecutionVariable(executionId, varName, data, user);
	}

	@Override
	public Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(String processDefinitionId, Map<String, Object> params, CIBUser user) {
		return activityProvider.findActivitiesProcessDefinitionHistory(processDefinitionId, params, user);
	}
	
	/*
	
	██████  ███████  ██████ ██ ███████ ██  ██████  ███    ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██   ██ ██      ██      ██ ██      ██ ██    ██ ████   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██   ██ █████   ██      ██ ███████ ██ ██    ██ ██ ██  ██     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██   ██ ██      ██      ██      ██ ██ ██    ██ ██  ██ ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██████  ███████  ██████ ██ ███████ ██  ██████  ██   ████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                                                                                                                              
	*/
	
	@Override
	public Collection<Decision> getDecisionDefinitionList(Map<String, Object> queryParams, CIBUser user) {
		return decisionProvider.getDecisionDefinitionList(queryParams, user);
	}
	
	
	@Override
	public Long getDecisionDefinitionListCount(Map<String, Object> queryParams, CIBUser user) {
		return decisionProvider.getDecisionDefinitionListCount(queryParams, user);
	}
	
	@Override
	public Decision getDecisionDefinitionByKey(String key, CIBUser user) {
		return decisionProvider.getDecisionDefinitionByKey(key, user);
	}
	
	@Override
	public Object getDiagramByKey(String key, CIBUser user) {
		return decisionProvider.getDiagramByKey(key, user);
	}

	@Override
	public Object evaluateDecisionDefinitionByKey(Map<String, Object> data, String key, CIBUser user) {
		return decisionProvider.evaluateDecisionDefinitionByKey(data, key, user);
	}
	
	@Override
	public void updateHistoryTTLByKey(Map<String, Object> data, String key, CIBUser user) {
		decisionProvider.updateHistoryTTLByKey(data, key, user);
	}
	
	@Override
	public Decision getDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user) {
		return decisionProvider.getDecisionDefinitionByKeyAndTenant(key, tenant, user);
	}
	
	@Override
	public Object getDiagramByKeyAndTenant(String key, String tenant, CIBUser user) {
		return decisionProvider.getDiagramByKeyAndTenant(key, tenant, user);
	}
	
	@Override
	public Object evaluateDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user) {
		return decisionProvider.evaluateDecisionDefinitionByKeyAndTenant(key, tenant, user);
	}
	
	@Override
	public Object updateHistoryTTLByKeyAndTenant(String key, String tenant, CIBUser user) {
		return decisionProvider.updateHistoryTTLByKeyAndTenant(key, tenant, user);
	}
	
	@Override
	public Object getXmlByKey(String key, CIBUser user) {
		return decisionProvider.getXmlByKey(key, user);
	}
	
	@Override
	public Object getXmlByKeyAndTenant(String key, String tenant, CIBUser user) {
		return decisionProvider.getXmlByKeyAndTenant(key, tenant, user);
	}
	
	@Override
	public Decision getDecisionDefinitionById(String id, Optional<Boolean> extraInfo, CIBUser user) {
		return decisionProvider.getDecisionDefinitionById(id, extraInfo, user);
	}
	
	@Override
	public Object getDiagramById(String id, CIBUser user) {
		return decisionProvider.getDiagramById(id, user);
	}
	
	@Override
	public Object evaluateDecisionDefinitionById(String id, CIBUser user) {
		return decisionProvider.evaluateDecisionDefinitionById(id, user);
	}
	
	@Override
	public void updateHistoryTTLById(String id, Map<String, Object> data, CIBUser user) {
		decisionProvider.updateHistoryTTLById(id, data, user);
	}
	
	@Override
	public Object getXmlById(String id, CIBUser user) {
		return decisionProvider.getXmlById(id, user);
	}

	@Override
	public Collection<Decision> getDecisionVersionsByKey(String key, Optional<Boolean> lazyLoad, CIBUser user) {
		return decisionProvider.getDecisionVersionsByKey(key, lazyLoad, user);
	}
	
	@Override
	public Collection<HistoricDecisionInstance> getHistoricDecisionInstances(Map<String, Object> queryParams, CIBUser user){
		return decisionProvider.getHistoricDecisionInstances(queryParams, user);
	}
	
	@Override
	public Long getHistoricDecisionInstanceCount(Map<String, Object> queryParams, CIBUser user){
		return decisionProvider.getHistoricDecisionInstanceCount(queryParams, user);
	}
	
	@Override
	public HistoricDecisionInstance getHistoricDecisionInstanceById(String id, Map<String, Object> queryParams, CIBUser user){
		return decisionProvider.getHistoricDecisionInstanceById(id, queryParams, user);
	}
	
	@Override
	public Object deleteHistoricDecisionInstances(Map<String, Object> data, CIBUser user){
		return decisionProvider.deleteHistoricDecisionInstances(data, user);
	}
	
	@Override
	public Object setHistoricDecisionInstanceRemovalTime(Map<String, Object> data, CIBUser user){
		return decisionProvider.setHistoricDecisionInstanceRemovalTime(data, user);
	}
	
	/*
	
	     ██  ██████  ██████      ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	     ██ ██    ██ ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	     ██ ██    ██ ██████      ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██   ██ ██    ██ ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	 █████   ██████  ██████      ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                          
	*/
	
	@Override
	public Collection<JobDefinition> findJobDefinitions(String params, CIBUser user) {
	if (useDirectInterface)
    return sevenDirectProvider.findJobDefinitions(params, user);
  else
		return jobDefinitionProvider.findJobDefinitions(params, user);
	}
	
	@Override
	public void suspendJobDefinition(String jobDefinitionId, String params, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.suspendJobDefinition(jobDefinitionId, params, user);
    else
      jobDefinitionProvider.suspendJobDefinition(jobDefinitionId, params, user);
	}
	
	@Override
	public void overrideJobDefinitionPriority(String jobDefinitionId, String params, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.overrideJobDefinitionPriority(jobDefinitionId, params, user);
    else
      jobDefinitionProvider.overrideJobDefinitionPriority(jobDefinitionId, params, user);
	}

	@Override
	public void retryJobDefinitionById(String id, Map<String, Object> params, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.retryJobDefinitionById(id, params, user);
    else
      jobDefinitionProvider.retryJobDefinitionById(id, params, user);
	}
	
	@Override
	public Collection<Job> getJobs(Map<String, Object> params, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.getJobs(params, user);
    else
      return jobProvider.getJobs(params, user);
	}

	@Override
	public void setSuspended(String id, Map<String, Object> params, CIBUser user) {
	  if (useDirectInterface)
	    sevenDirectProvider.setSuspended(id, params, user);
	  else
		  jobProvider.setSuspended(id, params, user);
	}

	@Override
	public void deleteJob(String id, CIBUser user) {
    if (useDirectInterface)
      sevenDirectProvider.deleteJob(id, user);
    else
      jobProvider.deleteJob(id, user);
	}

	@Override
	public JobDefinition findJobDefinition(String id, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.findJobDefinition(id, user);
    else
      return jobDefinitionProvider.findJobDefinition(id, user);
	}	

	@Override
	public Collection<Object> getHistoryJobLog(Map<String, Object> params, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.getHistoryJobLog(params, user);
    else
      return jobProvider.getHistoryJobLog(params, user);
	}
	
	@Override
	public String getHistoryJobLogStacktrace(String id, CIBUser user) {
    if (useDirectInterface)
      return sevenDirectProvider.getHistoryJobLogStacktrace(id, user);
    else
      return jobProvider.getHistoryJobLogStacktrace(id, user);
	}

	/*

	██████   █████  ████████  ██████ ██   ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██   ██ ██   ██    ██    ██      ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██████  ███████    ██    ██      ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██   ██ ██   ██    ██    ██      ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██████  ██   ██    ██     ██████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                                              
	*/
	
	@Override
	public Collection<Batch> getBatches(Map<String, Object> params, CIBUser user) {
		return batchProvider.getBatches(params, user);
    }

	@Override
	public Collection<Batch> getBatchStatistics(Map<String, Object> params, CIBUser user) {
		return batchProvider.getBatchStatistics(params, user);
	}

	@Override
	public void deleteBatch(String id, Map<String, Object> params, CIBUser user) {
		batchProvider.deleteBatch(id, params, user);		
	}
	
	@Override
	public void setBatchSuspensionState(String id, Map<String, Object> params, CIBUser user) {
		batchProvider.setBatchSuspensionState(id, params, user);		
	}
	
	@Override
	public Collection<HistoryBatch> getHistoricBatches(Map<String, Object> params, CIBUser user) {
		return batchProvider.getHistoricBatches(params, user);
    }
	
	@Override
	public Long getHistoricBatchCount(Map<String, Object> queryParams, CIBUser user) {
		return batchProvider.getHistoricBatchCount(queryParams, user);
    }
    
	@Override
	public HistoryBatch getHistoricBatchById(String id, CIBUser user) {
		return batchProvider.getHistoricBatchById(id, user);
    }
	
	@Override
	public void deleteHistoricBatch(String id, CIBUser user) {
		batchProvider.deleteHistoricBatch(id, user);
    }
	
	@Override
	public Object setRemovalTime(Map<String, Object> payload) {
		return batchProvider.setRemovalTime(payload);
    }
    
	@Override
	public Object getCleanableBatchReport(Map<String, Object> queryParams) {
		return batchProvider.getCleanableBatchReport(queryParams);
    }
    
	@Override
	public Object getCleanableBatchReportCount() {
		return batchProvider.getCleanableBatchReportCount();
    }
	
	/*

	███████ ██    ██ ███████ ████████ ███████ ███    ███     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██       ██  ██  ██         ██    ██      ████  ████     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	███████   ████   ███████    ██    █████   ██ ████ ██     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	     ██    ██         ██    ██    ██      ██  ██  ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	███████    ██    ███████    ██    ███████ ██      ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

	*/                                                                                                                      

	@Override
	public JsonNode getTelemetryData(CIBUser user) {
		return systemProvider.getTelemetryData(user);
	}
	
	@Override
	public Collection<Metric> getMetrics(Map<String, Object> queryParams, CIBUser user) {
		return systemProvider.getMetrics(queryParams, user);
	}

	/*
		  
	████████ ███████ ███    ██  █████  ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	   ██    ██      ████   ██ ██   ██ ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	   ██    █████   ██ ██  ██ ███████ ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	   ██    ██      ██  ██ ██ ██   ██ ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	   ██    ███████ ██   ████ ██   ██ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                       
	*/
	
	@Override
	public Collection<Tenant> fetchTenants(Map<String, Object> queryParams, CIBUser user) {
		return tenantProvider.fetchTenants(queryParams, user);
	}

	@Override
	public Tenant fetchTenant(String tenantId, CIBUser user) {
		return tenantProvider.fetchTenant(tenantId, user);
	}

	@Override
	public void createTenant(Tenant tenant, CIBUser user) {
		tenantProvider.createTenant(tenant, user);
	}

	@Override
	public void updateTenant(Tenant tenant, CIBUser user) {
		tenantProvider.updateTenant(tenant, user);
	}
	@Override
	public void deleteTenant(String tenantId, CIBUser user) {
		tenantProvider.deleteTenant(tenantId, user);
	}

	@Override
	public void addMemberToTenant(String tenantId, String userId, CIBUser user) {
		tenantProvider.addMemberToTenant(tenantId, userId, user);
	}

	@Override
	public void deleteMemberFromTenant(String tenantId, String userId, CIBUser user) {
		tenantProvider.deleteMemberFromTenant(tenantId, userId, user);
	}

	@Override
	public void addGroupToTenant(String tenantId, String groupId, CIBUser user) {
		tenantProvider.addGroupToTenant(tenantId, groupId, user);
	}
	
	@Override
	public void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user) {
		tenantProvider.deleteGroupFromTenant(tenantId, groupId, user);
	}

	/*
		
  ██    ██  █████  ██████  ██  █████  ██████  ██      ███████     ██ ███    ██ ███████ ████████  █████  ███    ██  ██████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
  ██    ██ ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ████   ██ ██         ██    ██   ██ ████   ██ ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
  ██    ██ ███████ ██████  ██ ███████ ██████  ██      █████       ██ ██ ██  ██ ███████    ██    ███████ ██ ██  ██ ██      █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
   ██  ██  ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ██  ██ ██      ██    ██    ██   ██ ██  ██ ██ ██      ██          ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
    ████   ██   ██ ██   ██ ██ ██   ██ ██████  ███████ ███████     ██ ██   ████ ███████    ██    ██   ██ ██   ████  ██████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                                                                                                 
	*/

	@Override
	public VariableInstance getVariableInstance(String id, boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException {
    if (!useRestInterface)
      return sevenDirectProvider.getVariableInstance(id, deserializeValue, user);
    else
      return variableInstanceProvider.getVariableInstance(id, deserializeValue, user);
	}

	/*
	
	██   ██ ██ ███████ ████████  ██████  ██████  ██  ██████     ██    ██  █████  ██████  ██  █████  ██████  ██      ███████     ██ ███    ██ ███████ ████████  █████  ███    ██  ██████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██   ██ ██ ██         ██    ██    ██ ██   ██ ██ ██          ██    ██ ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ████   ██ ██         ██    ██   ██ ████   ██ ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	███████ ██ ███████    ██    ██    ██ ██████  ██ ██          ██    ██ ███████ ██████  ██ ███████ ██████  ██      █████       ██ ██ ██  ██ ███████    ██    ███████ ██ ██  ██ ██      █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██   ██ ██      ██    ██    ██    ██ ██   ██ ██ ██           ██  ██  ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ██  ██ ██      ██    ██    ██   ██ ██  ██ ██ ██      ██          ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██   ██ ██ ███████    ██     ██████  ██   ██ ██  ██████       ████   ██   ██ ██   ██ ██ ██   ██ ██████  ███████ ███████     ██ ██   ████ ███████    ██    ██   ██ ██   ████  ██████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                                                                                                                             
	 */
	@Override
	public VariableHistory getHistoricVariableInstance(String id, boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException {
    if (!useRestInterface)
      return sevenDirectProvider.getHistoricVariableInstance(id, deserializeValue, user);
    else
      return historicVariableInstanceProvider.getHistoricVariableInstance(id, deserializeValue, user);
	}
	
	/*

	███████ ██   ██ ████████ ███████ ██████  ███    ██  █████  ██           ████████  █████  ███████ ██   ██     ██       ██████   ██████  
	██       ██ ██     ██    ██      ██   ██ ████   ██ ██   ██ ██              ██    ██   ██ ██      ██  ██      ██      ██    ██ ██       
	█████     ███      ██    █████   ██████  ██ ██  ██ ███████ ██              ██    ███████ ███████ █████       ██      ██    ██ ██   ███ 
	██       ██ ██     ██    ██      ██   ██ ██  ██ ██ ██   ██ ██              ██    ██   ██      ██ ██  ██      ██      ██    ██ ██    ██ 
	███████ ██   ██    ██    ███████ ██   ██ ██   ████ ██   ██ ███████         ██    ██   ██ ███████ ██   ██     ███████  ██████   ██████  
                                                                                                                                              
	*/

	@Override
	public Collection<ExternalTask> getExternalTasks(Map<String, Object> queryParams, CIBUser user) throws SystemException {
    if (!useRestInterface)
      return sevenDirectProvider.getExternalTasks(queryParams, user);
    else
      return externalTaskProvider.getExternalTasks(queryParams, user);
	}

	/*

	██████  ███████ ██████  ██       ██████  ██    ██ ███    ███ ███████ ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██   ██ ██      ██   ██ ██      ██    ██  ██  ██  ████  ████ ██      ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██   ██ █████   ██████  ██      ██    ██   ████   ██ ████ ██ █████   ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██   ██ ██      ██      ██      ██    ██    ██    ██  ██  ██ ██      ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██████  ███████ ██      ███████  ██████     ██    ██      ██ ███████ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
                                                                                                                                                         
	 */

	@Override
	public Deployment createDeployment(MultiValueMap<String, Object> data, MultipartFile[] files, CIBUser user) throws SystemException {
		if (useDirectInterface)
		  return sevenDirectProvider.createDeployment(data, files, user);
		else
		  return deploymentProvider.createDeployment(data, files, user);
	}

}
