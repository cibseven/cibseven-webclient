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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.rest.model.Decision;
import org.cibseven.webapp.Data;
import org.cibseven.webapp.NamedByteArrayDataSource;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.ExpressionEvaluationException;
import org.cibseven.webapp.exception.InvalidAttributeValueException;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SubmitDeniedException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnexpectedTypeException;
import org.cibseven.webapp.exception.UnsupportedTypeException;
import org.cibseven.webapp.rest.model.ActivityInstance;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.Batch;
import org.cibseven.webapp.rest.model.CandidateGroupTaskCount;
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.rest.model.DeploymentResource;
import org.cibseven.webapp.rest.model.EventSubscription;
import org.cibseven.webapp.rest.model.ExternalTask;
import org.cibseven.webapp.rest.model.Filter;
import org.cibseven.webapp.rest.model.HistoryBatch;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Incident;
import org.cibseven.webapp.rest.model.JobDefinition;
import org.cibseven.webapp.rest.model.Job;
import org.cibseven.webapp.rest.model.Message;
import org.cibseven.webapp.rest.model.Metric;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.ProcessInstance;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.ProcessStatistics;
import org.cibseven.webapp.rest.model.SevenUser;
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
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;

public interface BpmProvider {
	
	/**
     * Search tasks, which contains specified filter.
     * @param filter applied in the search
     * @param user the user performing the search
     * @return Collection tasks fetched in the search.
     * @throws SystemException in case of an error.
     */
	Collection<Task> findTasks(String filter, CIBUser user) throws SystemException;
	
	/**
     * Search tasks which belongs to a specific process instance.
     * @param processInstanceId filter by process instance id.
     * @param user the user performing the search
     * @return Fetched tasks.
     * @throws SystemException in case of an error.
     */	
	Collection<Task> findTasksByProcessInstance(String processInstanceId, CIBUser user) throws SystemException;
	
	/**
     * Search tasks which belongs to a specific process instance and a user.
     * @param processInstanceId filter by process instance id.
     * @param createdAfter filter by creation date.
     * @param user the user performing the search
     * @return Fetched tasks.
     * @throws SystemException in case of an error.
     */	
	Collection<Task> findTasksByProcessInstanceAsignee(Optional<String> processInstanceId, Optional<String> createdAfter, CIBUser user) throws SystemException;
	
	/**
	 * Search tasks which belongs to a specific process instance.
	 * The tasks found belongs to the history, they have other attributes and finished tasks
	 * are also fetched.
	 * @param processInstanceId filter by process instance id.
	 * @param user the user performing the search
	 * @return Fetched tasks.
     * @throws SystemException in case of an error.
	 */
	Collection<TaskHistory> findTasksByProcessInstanceHistory(String processInstanceId, CIBUser user) throws SystemException;

	/**
	 * Search tasks which belongs to a specific process instance and filtered by a definition key.
	 * The tasks found belongs to the history, they have other attributes and finished tasks
	 * are also fetched.
	 * @param processInstanceId filter by process instance id.
	 * @param taskDefinitionKey restrict to tasks that have the given key.
	 * @param user the user performing the search
	 * @return Fetched tasks.
     * @throws SystemException in case of an error.
	 */
	Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId, CIBUser user) throws SystemException;
	
	/**
     * Search task with a specific Id.
     * @param taskId filter by task id.
     * @param user the user performing the search
     * @return Fetched task.
     * @throws NoObjectFoundException when the task searched for could not be found.
     * @throws SystemException in case of any other error.
     */	
	Task findTaskById(String taskId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
     * Search activity that belong to a process instance.
     * @param processInstanceId filter by process instance id.
     * @param user the user performing the search
     * @return Fetched activity.
     * @throws NoObjectFoundException when the searched process instance could not be found.
     * @throws SystemException in case of any other error.
     */
	ActivityInstance findActivityInstance(String processInstanceId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
     * Queries for historic activity instances that fulfill the given parameters.
	 * The activities found belong to the history.
     * @param queryParams a map of parameters to filter the query.
     * @param user the user performing the query.
     * @return Fetched Historic Activity Instances.
     * @throws InvalidAttributeValueException when the tenant of a task could not be changed or when the delegation state of a task should be changed to an invalid value.
     * @throws SystemException in case of any other error.
     */	
	Collection<ActivityInstanceHistory> findActivitiesInstancesHistory(Map<String, Object> queryParams, CIBUser user) throws SystemException, InvalidAttributeValueException;

	/**
     * Search activities instances that belong to a process instance. The activities found belongs
     * to the history, they have other attributes and activities from finished processes are also fetched.
     * @param processInstanceId filter by process instance id.
     * @param user the user performing the search
     * @return Fetched Activity Instance.
     * @throws InvalidAttributeValueException when the tenant of a task could not be changed or when the delegation state of a task should be changed to an invalid value.
     * @throws SystemException in case of any other error.
     */	
	Collection<ActivityInstanceHistory> findActivitiesInstancesHistory(String processInstanceId, CIBUser user) throws SystemException, InvalidAttributeValueException;

	/**
	 * Fetch variables from a specific activity.
	 * The variables found belongs to the history, they have other attributes 
	 * and variables from finished activities are also fetched.
     * @param activityInstanceId filter by activity instance id.
     * @param user the user performing the search
     * @return Fetched variables.
     * @throws SystemException in case of an error.
     */
	Collection<VariableHistory> fetchActivityVariablesHistory(String activityInstanceId, CIBUser user) throws SystemException;
	
	/**
	 * Fetch variables from a specific activity.
     * @param activityInstanceId filter by activity instance id.
     * @param user the user performing the search
     * @return Fetched variables.
     * @throws SystemException in case of an error.
     */
	Collection<VariableHistory> fetchActivityVariables(String activityInstanceId, CIBUser user) throws SystemException;
	
	/**
     * Update task.
     * @param task to be updated with the desired values already modified.
     * @param user the user performing the update
     * @throws SystemException in case of an error.
     */
	void update(Task task, CIBUser user) throws SystemException;
	
	/**
     * Set assignee to an specific task.
     * @param taskId filter by task id.
     * @param assignee to be set as assignee.
     * @param user the user performing the update
     * @throws SystemException in case of an error.
     */	
	void setAssignee(String taskId, String assignee, CIBUser user) throws SystemException;

	/**
	 * Submit task without saving any variables, because that is done by the ui-element-template (in ours).
     * @param taskId the ID of the task to be submitted.
     * @param user the user performing the submission.
     * @throws SubmitDeniedException when trying to submit a non-existing task.
     * @throws SystemException in case of any other error.
	 */
	void submit(String taskId, CIBUser user) throws SystemException, SubmitDeniedException;
	
	/**
	 * Fetch form-reference variable from task.
	 * @param taskId filter by task id.
	 * @param user the user performing the search
	 * @return form-reference
     * @throws NoObjectFoundException when the searched task could not be found.
     * @throws SystemException in case of any other error.
	 */
	Object formReference(String taskId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
	 * Search processes.
     * @param user the user performing the search
     * @return Fetched processes.
     * @throws InvalidAttributeValueException when searching for processes with at least one invalid parameter value.
     * @throws SystemException in case of any other error.
     */
	Collection<Process> findProcesses(CIBUser user) throws SystemException, InvalidAttributeValueException;

	/**
	 * Search processes with number of process instances and incidents.
     * @param user the user performing the search
     * @return Fetched processes.
     * @throws InvalidAttributeValueException when searching for processes with at least one invalid parameter value.
     * @throws SystemException in case of any other error.
     */
	Collection<Process> findProcessesWithInfo(CIBUser user) throws SystemException, InvalidAttributeValueException;
	
	/**
	 * Search processes.
	 * @param filters filters to be applied.
	 * @param user the user performing the search
     * @return Fetched processes.
     * @throws InvalidAttributeValueException when searching for processes with at least one invalid parameter value.
     * @throws SystemException in case of any other error.
     */
	Collection<Process> findProcessesWithFilters(String filters, CIBUser user) throws SystemException, InvalidAttributeValueException;
	
	/**
     * Search process with a specific Key.
     * @param processKey filter by process definition key.
	 * @param tenantId 
     * @param user since this call is secured we need the user to authenticate.
     * @return Fetched process.
     * @throws SystemException in case of an error.
     */	
	Process findProcessByDefinitionKey(String processKey, String tenantId, CIBUser user) throws SystemException;
	
	/**
     * Search processes (diferents versions) with a specific Key.
     * @param processKey filter by process definition key.
	 * @param tenantId 
     * @param lazyLoad parameter to decide if load all the data or the minimum necessary.
     * @param user since this call is secured we need the user to authenticate.
     * @return Fetched process.
     * @throws SystemException in case of an error.
     */	
	Collection<Process> findProcessVersionsByDefinitionKey(String processKey, String tenantId, Optional<Boolean> lazyLoad, CIBUser user) throws SystemException;
	
	/**
     * Search process with a specific Id.
     * @param id filter by process definition id.
     * @param extraInfo parameter to specify if more data will be loaded.
     * @param user the user performing the query.
     * @return Fetched process.
     * @throws SystemException in case of an error.
     */	
	Process findProcessById(String id, Optional<Boolean> extraInfo, CIBUser user) throws SystemException;
	
	/**
	 * Queries for historic process instances that fulfill the given parameters.
	 * @param filters is a map of parameters to filter query. Parameters firstResult and maxResults are used for pagination.
	 * @param user the user performing the query.
     * @return Fetched processes instances.
     * @throws SystemException in case of an error.
     */
	Collection<HistoryProcessInstance> findProcessesInstancesHistory(Map<String, Object> filters, Optional<Integer> firstResult, Optional<Integer> maxResults, CIBUser user) throws SystemException;

	/**
	 * Search processes instances with a specific process key (in the history).
	 * @param key the process key to filter by.
	 * @param active true means that unfinished processes will be fetched, false means only finished processes will be fetched.
	 * @param firstResult index of the first result to return.
	 * @param maxResults maximum number of results to return.
	 * @param user the user performing the query.
     * @return Fetched process instances.
     * @throws SystemException in case of an error.
     */
	Collection<HistoryProcessInstance> findProcessesInstancesHistory(String key, Optional<Boolean> active, Integer firstResult, Integer maxResults, CIBUser user) throws SystemException;
	
	/**
	 * Search processes instances with a specific process key.
	 * @param key the process key to filter by.
	 * @param user the user performing the search
     * @return Fetched processes instances.
     * @throws SystemException in case of an error.
     */
	Collection<ProcessInstance> findProcessesInstances(String key, CIBUser user) throws SystemException;
	
	/**
	 * Search statistics from a process.
	 * @param id filter by process id.
	 * @param user the user performing the search
	 * @return Fetched processes instances.
	 * @throws SystemException in case of an error.
	 */
	Collection<ProcessStatistics> findProcessStatistics(String id, CIBUser user) throws SystemException;
  /**
   * Search statistics for all processes.
   * @param queryParams query parameters to filter the search
   * @param user the user performing the search
   * @return Fetched processes instances.
   * @throws SystemException in case of an error.
   */
	public Collection<ProcessStatistics> getProcessStatistics(Map<String, Object> queryParams, CIBUser user) throws SystemException;

	/**
	 * Search processes instances by filter.
	 * @param data a map of parameters to filter the query.
	 * @param user the user performing the query.
     * @return Fetched processes instances.
     * @throws SystemException in case of an error.
     */
	Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user) throws SystemException;
	
	/**
     * Search process instance with a specific process instance id.
     * @param processInstanceId filter by process instance id.
     * @param user the user performing the search
     * @return Fetched process instance.
     * @throws NoObjectFoundException when the process instance searched for could not be found.
     * @throws SystemException in case of any other error.
     */	
	HistoryProcessInstance findHistoryProcessInstanceHistory(String processInstanceId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
	 * FindTask by filter
	 * @param filters list of properties which will be use to filter tasks.
	 * @param filterId to filter task.
	 * @param firstResult index of the first result to return.
	 * @param maxResults maximum number of results to return.
	 * @param user since this call is secured we need the user to authenticate.
	 * @return Collection of Tasks fetched in the search.
	 * @throws SystemException in case of an error.
	 */
	Collection<Task> findTasksByFilter(TaskFiltering filters, String filterId, CIBUser user, Integer firstResult, Integer maxResults) throws SystemException;
	
	/**
	 * Find Tasks count by filter
	 * @param filterId to filter task.
	 * @param filters list of properties which will be use to filter tasks.
	 * @param user since this call is secured we need the user to authenticate.
	 * @return Collection of Tasks fetched in the search.
	 * @throws SystemException in case of an error.
	 */
	Integer findTasksCountByFilter(String filterId, CIBUser user, TaskFiltering filters) throws SystemException;
	
	/**
	 * Fetch process diagram, a xml that contains the specification to render the diagram.
	 * @param processDefinitionId filter by process definition id.
	 * @param user the user performing the search
	 * @return process diagram xml that contains diagram to be render.
     * @throws NoObjectFoundException when the process definition searched for could not be found.
     * @throws SystemException in case of any other error.
	 */
	ProcessDiagram fetchDiagram(String processDefinitionId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
	 * Fetch variables from a specific process instance.
	 * The variables found belong to the history, they have other attributes, and variables from finished process instances are also fetched.
     * @param processInstanceId filter by process instance id.
	 * @param data a map of parameters to filter the query.
	 * @param user the user performing the search
     * @return Fetched variables.
     * @throws SystemException in case of an error.
     */
	Collection<VariableHistory> fetchProcessInstanceVariablesHistory(String processInstanceId, Map<String, Object> data, CIBUser user) 
			throws SystemException;

	/**
	 * Fetch start-form to start a process
	 * @param processDefinitionId of the process to be started.
	 * @param user the user performing the search
	 * @return Startform variables and formReference.
     * @throws NoObjectFoundException when trying to find start form data of a non-existing process definition.
     * @throws SystemException in case of any other error.
	 */
	StartForm fetchStartForm(String processDefinitionId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
	 * Download bpmn from a process definition id.
	 * @param processDefinitionId filter by process definition id.
	 * @param fileName name of the file content the bpmn.
	 * @param user the user performing the download
	 * @return Fetched bpmn
     * @throws SystemException in case of an error.
	 */
	Data downloadBpmn(String processDefinitionId, String fileName, CIBUser user) throws SystemException;

	/**
	 * Get authorizations, filtered by userId and groups in which user belongs.
	 * @param userId filter user identification (username).
	 * @param user the user performing the search
	 * @return Fetched bpmn
     * @throws SystemException in case of an error.
	 */	
	Authorizations getUserAuthorization(String userId, CIBUser user) throws SystemException;
	
	/**
     * Search filters.
     * @param user the user performing the query.
     * @return Collection of Filters fetched in the search.
     * @throws SystemException in case of an error.
     */
	Collection<Filter> findFilters(CIBUser user) throws SystemException;
	
	/**
     * Create filter.
     * @param filter to be created.
     * @param user the user performing the creation.
     * @throws SystemException in case of an error.
     */
	Filter createFilter(Filter filter, CIBUser user) throws SystemException;
	
	/**
     * Update filter.
     * @param filter to be updated.
     * @param user the user performing the update.
     * @throws NoObjectFoundException when the filter to be changed could not be found.
     * @throws SystemException in case of any other error.
     */
	void updateFilter(Filter filter, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
     * Delete filter.
     * @param filterId the ID of the filter to be deleted.
     * @param user the user performing the deletion.
     * @throws SystemException in case of an error.
     */
	void deleteFilter(String filterId, CIBUser user) throws SystemException;
	
	/**
     * Activate/Suspend process instance by ID.
     * @param processInstanceId instance id to be suspended or activated.
     * @param suspend if true, the process instance will be activated if false process will be suspended.
     * @param user the user performing the operation.
     * @throws SystemException in case of other error.
     */
	void suspendProcessInstance(String processInstanceId, Boolean suspend, CIBUser user) throws SystemException;
	
	/**
     * Activate/Suspend process instance by ID.
     * @param processDefinitionId definition id to be suspended or activated.
     * @param suspend if true, the process will be activated if false process will be suspended.
     * @param includeProcessInstances indicates whether to activate or suspend also all process instances of the given process definition
     * @param executionDate The date on which the given process definition will be activated or suspended ej. 2013-01-23T14:42:45. yyyy-MM-dd'T'HH:mm:ss,
     *  If null, the suspension state of the given process definition is updated immediately.
     * @param user the user performing the operation.
     * @throws SystemException in case of other error.
	 * @throws UnsupportedTypeException when a process instance cannot be created because of an unsupported value type or an invalid expression used in the process definition.
	 * @throws NoObjectFoundException when the filter to be changed could not be found.
     */
	void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user) throws SystemException, UnsupportedTypeException, NoObjectFoundException;
	
	/**
     * Delete process instance by ID.
     * @param processInstanceId instance id to be deleted.
     * @param user the user performing the deletion.
     * @throws NoObjectFoundException when the filter to be changed could not be found.
     * @throws SystemException in case of any other error.
     */
	void deleteProcessInstance(String processInstanceId, CIBUser user) throws SystemException, NoObjectFoundException;

	/**
     * Fetch incidents for an specific process.
	 * @param processDefinitionKey of the process to fetch incidents.
	 * @param user the user performing the search.
	 * @throws UnsupportedTypeException when a process instance cannot be created because of an unsupported value type or an invalid expression used in the process definition.
     * @throws SystemException in case of any other error.
     */
	 Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user) throws SystemException, UnsupportedTypeException;

	/**
     * Deploy process-bpmn.
	 * @param data metadata of the diagram to be deployed (deployment-name, deployment-source, deploy-changed-only).
	 * @param file of the diagram to be deployed.
	 * @param user the user performing the deployment.
	 * @return Deployment information.
     * @throws SystemException in case of any other error.
     */
	 Deployment deployBpmn(MultiValueMap<String, Object> data, MultiValueMap<String, MultipartFile> file, CIBUser user) throws SystemException;
	
	/**
	 * Start process.
	 * @param processDefinitionKey of the process to be started.
	 * @param tenantId the tenant ID.
	 * @param data variables to start process.
	 * @param user the user starting the process.
	 * @return information about the process started.
     * @throws UnsupportedTypeException when a process instance cannot be created because of an unsupported value type or an invalid expression used in the process definition.
     * @throws ExpressionEvaluationException when .
     * @throws SystemException in case of any other error.
	 */
	 ProcessStart startProcess(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;

	/**
	 * Correlates a message to the process engine to either trigger a message start event or an intermediate message catching event.
	 * @param data variables to start process.
	 * @param user the user performing the correlation.
	 * @return Collection of correlated messages.
     * @throws SystemException in case of any other error.
	 */
	 Collection<Message> correlateMessage(Map<String, Object> data, CIBUser user) throws SystemException;
	 
	/**
	 * Submit form with variables.
	 * @param processDefinitionKey of the process to be started.
	 * @param tenantId the tenant ID.
	 * @param data variables to submit.
	 * @param user the user submitting the form.
	 * @return information about the process started.
     * @throws UnsupportedTypeException when a process instance cannot be created because of an unsupported value type or an invalid expression used in the process definition.
     * @throws ExpressionEvaluationException when .
     * @throws SystemException in case of any other error.
	 */
	 ProcessStart submitForm(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;

	/**
	 * Modify a variable in the Process Instance.
	 * @param executionId Id of the execution.
	 * @param data to be updated.
	 * @param user User who is modifying the variable.
     * @throws SystemException in case of any other error.
	 */
	 void modifyVariableByExecutionId(String executionId, Map<String, Object> data, CIBUser user) throws SystemException; 

	/**
	 * Modify a variable data in the Process Instance.
	 * @param executionId the ID of the execution.
	 * @param variableName the name of the variable.
	 * @param file the file containing the data to be updated.
	 * @param user the user modifying the variable.
     * @throws SystemException in case of any other error.
	 */ 
	 void modifyVariableDataByExecutionId(String executionId, String variableName, MultipartFile file, CIBUser user) throws SystemException;
	
	 /**
	 * Fetch a variables from a process instance.
	 * @param processInstanceId Id of the instance.
	 * @param data a map of parameters to filter the query.
	 * @param user User who is fetching the variables.
	 * @return Data.
     * @throws SystemException in case of any other error.
	 */ 
	 Collection<Variable> fetchProcessInstanceVariables(String processInstanceId, Map<String, Object> data, CIBUser user) 
			 throws NoObjectFoundException, SystemException;
	
	 /**
	 * Fetch a variable data in the Process Instance.
	 * @param executionId Id of the execution.
	 * @param variableName Name of the variable.
	 * @param user User who is fetching the variable.
	 * @return Data.
     * @throws SystemException in case of any other error.
	 */ 
	 ResponseEntity<byte[]> fetchVariableDataByExecutionId(String executionId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException;
	
	 /**
	 * Fetch a variable data in from the process history.
	 * @param id Id of the variable.
	 * @param user User who is modifying the variable.
	 * @return Data.
     * @throws SystemException in case of any other error.
	 */ 
	 ResponseEntity<byte[]> fetchHistoryVariableDataById(String id, CIBUser user) throws NoObjectFoundException, SystemException;

	/**
	 * Retrieves number of all deployments with provided query.
	 * @param user the user performing the search.
	 * @return Fetched deployments.
	 * @throws SystemException in case of any other error.
	 */
	Long countDeployments(CIBUser user, String nameLike) throws SystemException;

	/**
	 * Retrieves all deployments matched with provided query.
	 * @param user the user performing the search.
	 * @return Fetched deployments.
	 * @throws SystemException in case of any other error.
	 */
	Collection<Deployment> findDeployments(CIBUser user, String nameLike, int firstResult, int maxResults, String sortBy, String sortOrder) throws SystemException;

	/**
	 * Retrieves all deployment resources of a given deployment.
	 * @param deploymentId the ID of the deployment.
	 * @param user the user performing the query.
	 * @return Fetched deployment resources.
     * @throws SystemException in case of any other error.
	 */
	Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user) throws SystemException;

	/**
	 * Retrieves the binary content of a deployment resource for the given deployment by id.
	 * @param rq the HTTP request.
	 * @param deploymentId the ID of the deployment.
	 * @param resourceId the ID of the resource.
	 * @param fileName the name of the file.
	 * @return resource data.
     * @throws SystemException in case of any other error.
	 */
	Data fetchDataFromDeploymentResource(HttpServletRequest rq, String deploymentId, String resourceId, String fileName) throws SystemException;

	/**
	 * Delete deployment by an Id.
	 * @param deploymentId the ID of the deployment.
	 * @param cascade whether to cascade the deletion.
	 * @param user the user performing the deletion.
     * @throws SystemException in case of any other error.
	 */
	void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException;

	/**
	 *  Identity links, e.g. to get the candidates user or groups of a task.
	 *  
	 * @param taskId the ID of the task.
	 * @param type Filter by the type of links to include. e.g. "candidate".
	 * @param user the user performing the query.
	 * @return Collection of Identity Links.
	 */
	Collection<IdentityLink> findIdentityLink(String taskId, Optional<String> type, CIBUser user);
	
	/**
	 *  Create identity links, e.g., to set the candidates user or groups of a task.
	 *  
	 * @param taskId the ID of the task.
	 * @param type a map containing the type of the identity link and group or user ID.
	 * @param user the user performing the operation.
	 * @throws SystemException in case of any other error.
	 */
	void createIdentityLink(String taskId, Map<String, Object> type, CIBUser user) throws SystemException;
	
	/**
	 *  Delete identity links, e.g., to remove the candidates user or groups of a task.
	 *  
	 * @param taskId the ID of the task.
	 * @param type a map containing the type of the identity link to be removed.
	 * @param user the user performing the operation.
	 * @throws SystemException in case of any other error.
	 */
	void deleteIdentityLink(String taskId, Map<String, Object> type, CIBUser user) throws SystemException;
	
	/**
	 *  The following methods related to the Admin Section. 
	 *  They are all created but need first to check if those are used in webclient. If not we should remove them from here.
	 *  IMPORTANT: Methods related to users/groups need to check if they are allowed to be created or removed (LDAP or Camunda) 
	 *  then it only make sense to use them when SevenProvider is selected.
	 */
	
	/**
	 * Get users by id, ....
	 * 
	 * @param id, // Filter by the id of the user.
	 * @param firstName, // 	Filter by the firstname of the user.
	 * @param firstNameLike, // 	Filter by the firstname that the parameter is a substring of.
	 * @param lastName, // 	Filter by the lastname of the user.
	 * @param lastNameLike, // 	Filter by the lastname that the parameter is a substring of.
	 * @param email , //	Filter by the email of the user.
	 * @param emailLike, // 	Filter by the email that the parameter is a substring of.
	 * @param memberOfGroup, // 	Filter for users which are members of the given group.
	 * @param memberOfTenant , //	Filter for users which are members of the given tenant.			
	 * 
	 * @param user CIBSevenUser
	 * @return Collection of Users.
	 */
	Collection<User> findUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike, Optional<String> lastName,
			Optional<String> lastNameLike, Optional<String> email, Optional<String> emailLike, Optional<String> memberOfGroup, Optional<String> memberOfTenant, 
			Optional<String> idIn, Optional<String> firstResult, Optional<String> maxResult, 
			Optional<String> sortBy, Optional<String> sortOrder, CIBUser user);

	/**
	 * Create a new user.
	 * 
	 * @param user the new user to be created.
	 * @param flowUser the user performing the creation.
	 * @throws InvalidUserIdException when the user ID is invalid.
	 */
	void createUser(NewUser user, CIBUser flowUser) throws InvalidUserIdException;	
	
	/**
	 * Updates a user’s profile.
	 * 
	 * @param userId the ID of the user to be updated.
	 * @param user the user to Update.
	 * @param flowUser the user performing the update.
	 */
	void updateUserProfile(String userId, User user, CIBUser flowUser);	
	
	/**
	 * Add user to a group.
	 * 
	 * @param groupId the ID of the group.
	 * @param userId the ID of the user to be added.
	 * @param flowUser the user performing the operation.
	 */
	void addMemberToGroup(String groupId, String userId, CIBUser flowUser);
	
	/**
	 * Delete user from a group.
	 * 
	 * @param groupId the ID of the group.
	 * @param userId the ID of the user to be removed.
	 * @param flowUser the user performing the operation.
	 */	
	void deleteMemberFromGroup(String groupId, String userId, CIBUser flowUser);

	/**
	 * Updates a user’s credentials (password).
	 * 
	 * @param userId the ID of the user to be updated.
	 * @param data Request Body
	 * 		A JSON object with the following properties:
	 * 		Name 	Type 	Description
	 * 		password 	String 	The user's new password.
	 * 		authenticatedUserPassword 	String 	The password of the authenticated user who changes the password of the user (i.e., the user with passed id as path parameter).	 
	 * @param user the user performing the update.
	 */
	void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user);

	
	/**
	 * Deletes a user by id.
	 * 
	 * @param userId the ID of the user to be deleted.
	 * @param user the user performing the deletion.
	 */
	void deleteUser(String userId, CIBUser user);

	/**
	 * Get groups by id, ....
	 * 
	 * @param id, // Filter by the id of the group.
	 * @param name, // Filter by the name of the group.
	 * @param nameLike, // Filter by the name that the parameter is a substring of.
	 * @param type, // Filter by the type of the group.
	 * @param member, // Only retrieve groups which the given user id is a member of.
	 * @param memberOfTenant, // Only retrieve groups which are members of the given tenant.
	 * @param sortBy, // Sort the results lexicographically by a given criterion. Valid values are id, name and type. Must be used in conjunction with the sortOrder parameter.
	 * @param sortOrder, // Sort the results in a given order. Values may be asc for ascending order or desc for descending order. Must be used in conjunction with the sortBy parameter.
	 * @param firstResult, // Pagination of results. Specifies the index of the first result to return.
	 * @param maxResults, // Pagination of results. Specifies the maximum number of results to return. Will return less results if there are no more results left.			
	 * 
	 * @param user the user performing the search.
	 * @return Collection of User Groups.
	 */
	Collection<UserGroup> findGroups(Optional<String> id, Optional<String> name, Optional<String> nameLike, Optional<String> type, Optional<String> member,
			Optional<String> memberOfTenant, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult, Optional<String> maxResults,
			CIBUser user);

	/**
	 * Create a group.
	 * 
	 * @param group the group to be created.
	 * @param user the user performing the creation.
	 */
	void createGroup(UserGroup group, CIBUser user);

	/**
	 * Updates a group.
	 * 
	 * @param groupId the ID of the group to be updated.
	 * @param group the group to be updated.
	 * @param user the user performing the update.
	 */
	void updateGroup(String groupId, UserGroup group, CIBUser user);

	/**
	 * Deletes a group by id.
	 * 
	 * @param groupId the ID of the group to be deleted.
	 * @param user the user performing the deletion.
	 */
	void deleteGroup(String groupId, CIBUser user);

	/**
	 * Get Authorization by id, ....
	 * 
	 * @param id, // Filter by the id.
	 * @param type, //  	Filter by authorization type. (0=global, 1=grant, 2=revoke). See the User Guide for more information about authorization types.
	 * @param userIdIn, //  	Filter by a comma-separated list of userIds.
	 * @param groupIdIn, //  	Filter by a comma-separated list of groupIds.
	 * @param resourceType, //  	Filter by an integer representation of the resource type. See the User Guide for a list of integer representations of resource types.
	 * @param resourceId, //  	Filter by resource id.	 * @param sortBy, // Sort the results lexicographically by a given criterion. Valid values are id, name and type. Must be used in conjunction with the sortOrder parameter.
	 * @param sortBy, // Sort the results lexicographically by a given criterion. Valid values are id, name and type. Must be used in conjunction with the sortOrder parameter.
	 * @param sortOrder, // Sort the results in a given order. Values may be asc for ascending order or desc for descending order. Must be used in conjunction with the sortBy parameter.
	 * @param firstResult, // Pagination of results. Specifies the index of the first result to return.
	 * @param maxResults, // Pagination of results. Specifies the maximum number of results to return. Will return less results if there are no more results left.			
	 * @param user the user performing the search.
	 * @return Collection of Authorizations.
	 */
	Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type, Optional<String> userIdIn,Optional<String> groupIdIn, 
			Optional<String> resourceType, Optional<String> resourceId, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,Optional<String> maxResults, 
			CIBUser user);

	/**
	 * Create an authorization.
	 * 
	 * @param authorization the authorization to be created.
	 * @param user the user performing the creation.
	 * @return ResponseEntity containing the created authorization.
	 */
	ResponseEntity<Authorization> createAuthorization(Authorization authorization, CIBUser user);

	/**
	 * Update an authorization by id.
	 * 
	 * @param authorizationId the ID of the authorization to be updated.
	 * @param data the data to update.
	 * @param user the user performing the update.
	 */
	void updateAuthorization(String authorizationId, Map<String, Object> data, CIBUser user);

	/**
	 * Deletes an authorization by id.
	 * 
	 * @param authorizationId the ID of the authorization to be deleted.
	 * @param user the user performing the deletion.
	 */
	void deleteAuthorization(String authorizationId, CIBUser user);

	/**
	 * Queries for historic process instances that fulfill the given parameters.
	 * @param id the ID of the process instance.
	 * @param activityId optional activity ID to filter the query.
	 * @param active optional flag to filter active or inactive instances.
	 * @param firstResult index of the first result to return.
	 * @param maxResults maximum number of results to return.
	 * @param text additional text filter for the query.
	 * @param user the user performing the query.
     * @return Fetched process instances.
     * @throws SystemException in case of an error.
     */
	Collection<HistoryProcessInstance> findProcessesInstancesHistoryById(String id, Optional<String> activityId, Optional<Boolean> active, Integer firstResult, Integer maxResults, String text, CIBUser user) throws SystemException;
	
	Long countProcessesInstancesHistory(Map<String, Object> filters, CIBUser user);	
	
	/**
	 * Get user by id.
	 * 
	 * @param userId the ID of the user to be fetched.
	 * @param user the user performing the search.
	 * @return SevenUser object containing user profile information.
	 */
	SevenUser getUserProfile(String userId, CIBUser user);
	
	void submitVariables(String processInstanceId, List<Variable> variables, CIBUser user, String processDefinitionId) throws SystemException;

	Collection<Process> findCalledProcessDefinitions(String processDefinitionId, CIBUser user);

	/*UI Element templates methods migrated*/
	
	ActivityInstance findActivityInstances(String processInstanceId, CIBUser user) throws SystemException;

	List<ActivityInstanceHistory> findActivityInstanceHistory(String processInstanceId, CIBUser user)
			throws SystemException;

	Variable fetchVariable(String taskId, String variableName, Optional<Boolean> deserialize, CIBUser user)
			throws NoObjectFoundException, SystemException;

	void deleteVariable(String taskId, String variableName, CIBUser user)
			throws NoObjectFoundException, SystemException;

	Map<String, Variable> fetchFormVariables(String taskId, boolean deserializeValues, CIBUser user)
			throws NoObjectFoundException, SystemException;

	Map<String, Variable> fetchFormVariables(List<String> variableListName, String taskId, CIBUser user)
			throws NoObjectFoundException, SystemException;

	Map<String, Variable> fetchProcessFormVariables(String key, CIBUser user)
			throws NoObjectFoundException, SystemException;

	NamedByteArrayDataSource fetchVariableFileData(String taskId, String variableName, CIBUser user)
			throws NoObjectFoundException, UnexpectedTypeException, SystemException;

	ResponseEntity<byte[]> fetchProcessInstanceVariableData(String processInstanceId, String variableName,
			CIBUser user) throws NoObjectFoundException, SystemException;

	Variable fetchVariableByProcessInstanceId(
			String processInstanceId, String variableName, CIBUser user)
			throws SystemException;

	ProcessStart submitStartFormVariables(String processDefinitionId, List<Variable> formResult, CIBUser user)
			throws SystemException;

	void saveVariableInProcessInstanceId(String processInstanceId, List<Variable> variables, CIBUser user) throws SystemException;
			
	Map<String, Variable> fetchProcessFormVariablesById(String id, CIBUser user) throws SystemException;

	void retryJobById(String jobId, Map<String, Object> data, CIBUser user);
	
	String findExternalTaskErrorDetails(String externalTaskId, CIBUser user);
	
	void retryExternalTask(String externalTaskId, Map<String, Object> data, CIBUser user);
	
	void setIncidentAnnotation(String incidentId, Map<String, Object> data, CIBUser user);
	
	/**
	 * Submit task with saving variables.
	 * @param task the task to be submitted.
	 * @param formResult the variables to be saved.
	 * @param user the user performing the submission.
     * @throws SubmitDeniedException when trying to submit a non-existing task.
     * @throws SystemException in case of any other error.
	 */
	void submit(Task task, List<Variable> formResult, CIBUser user) throws SystemException, SubmitDeniedException;

	Long countIncident(Map<String, Object> params, CIBUser user);

	Collection<Incident> findIncident(Map<String, Object> params, CIBUser user);

	List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user);
	
	Collection<Incident> fetchIncidentsByInstanceAndActivityId(String processDefinitionKey, String activityId, CIBUser user);

	String findStacktrace(String jobId, CIBUser user);

	/**
	 * Required by OFDKA
	 * Queries for tasks that fulfill a given filter. This method is slightly more powerful than the Get Tasks method because it allows
	 * filtering by multiple process or task variables of types String, Number or Boolean.
	 * @param data variables to apply search.
	 * @param user the user performing the search.
	 * @return Collection tasks fetched in the search.
	 * @throws SystemException in case of an error.
	 */
	Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException;

	/**
	 * Required by OFDKA
	 * Search process instance with a specific process instance id.
	 * @param processInstanceId filter by process instance id.
	 * @param user the user performing the search.
	 * @return Fetched process instance.
	 * @throws NoObjectFoundException when the process instance searched for could not be found.
	 * @throws SystemException in case of any other error.
	 */
	ProcessInstance findProcessInstance(String processInstanceId, CIBUser user);

	/**
	 * Required by OFDKA
	 * Retrieves a variable of a given process instance by id.
	 * @param processInstanceId filter by process instance id.
	 * @param variableName variable name.
	 * @param deserializeValue whether to deserialize the variable value.
	 * @param user the user performing the search.
	 * @return Fetched variables.
	 * @throws SystemException in case of an error.
	 */
	Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, String deserializeValue,
			CIBUser user) throws SystemException;

	
	/**
	 * Required by OFDKA
	 * Queries for event subscriptions that fulfill given parameters. 
	 * The size of the result set can be retrieved by using the Get Event Subscriptions count method.
	 * @param processInstanceId filter by process instance id.
	 * @param eventType filter by event type.
	 * @param eventName filter by event name.
	 * @param user the user performing the search.
	 * @return Collection event subscriptions fetched in the search.
	 */
	Collection<EventSubscription> getEventSubscriptions(Optional<String> processInstanceId, Optional<String> eventType, 
			Optional<String> eventName, CIBUser user);
	
	
	Integer findTasksCount(Map<String, Object> filters, CIBUser user);
	
	/**
	 * Reports a business error in the context of a running task by id. The error code must be specified to identify the BPMN error handler.
	 * @param taskId filter by task id.
	 * @param data variables for the BPMN error reporting.
	 * @param user the user performing the operation.
	 * @throws SystemException in case of any other error.
	 */
	 void handleBpmnError(String taskId, Map<String, Object> data, CIBUser user) throws SystemException;

	Collection<TaskHistory> findTasksByTaskIdHistory(String taskId, CIBUser user);
	
	ResponseEntity<byte[]> getDeployedForm(String taskId, CIBUser user);
	
	ResponseEntity<byte[]> getDeployedStartForm(String processDefinitionId, CIBUser user);
	
	void updateHistoryTimeToLive(String id, Map<String, Object> data, CIBUser user);

	void deleteProcessInstanceFromHistory(String id, CIBUser user);
	
	void deleteProcessDefinition(String id, Optional<Boolean> cascade, CIBUser user);
	
	void deleteVariableByExecutionId(String executionId, String variableName, CIBUser user);

	void deleteVariableHistoryInstance(String id, CIBUser user);

	void putLocalExecutionVariable(String executionId, String varName, Map<String, Object> data, CIBUser user);

	Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(String processDefinitionId,
			Map<String, Object> params, CIBUser user);
	
	Collection<JobDefinition> findJobDefinitions(String params, CIBUser user);
	void suspendJobDefinition(String jobDefinitionId, String params, CIBUser user);
	void overrideJobDefinitionPriority(String jobDefinitionId, String params, CIBUser user);
	JobDefinition findJobDefinition(String id, CIBUser user);
	void retryJobDefinitionById(String id, Map<String, Object> params, CIBUser user);
	
	Collection<Decision> getDecisionDefinitionList(Map<String, Object> queryParams, CIBUser user);
	Object getDecisionDefinitionListCount(Map<String, Object> queryParams, CIBUser user);
	Decision getDecisionDefinitionByKey(String key, CIBUser user);
	Object getDiagramByKey(String key, CIBUser user);
	Object evaluateDecisionDefinitionByKey(Map<String, Object> data, String key, CIBUser user);
	void updateHistoryTTLByKey(Map<String, Object> data, String key, CIBUser user);

	Decision getDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user);
	Object getDiagramByKeyAndTenant(String key, String tenant, CIBUser user);
	Object evaluateDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user);
	Object updateHistoryTTLByKeyAndTenant(String key, String tenant, CIBUser user);
	Object getXmlByKey(String key, CIBUser user);
	Object getXmlByKeyAndTenant(String key, String tenant, CIBUser user);
	Decision getDecisionDefinitionById(String id, Optional<Boolean> extraInfo, CIBUser user);
	Object getDiagramById(String id, CIBUser user);
	Object evaluateDecisionDefinitionById(String id, CIBUser user);
	void updateHistoryTTLById(String id, Map<String, Object> data, CIBUser user);
	Object getXmlById(String id, CIBUser user);

	Collection<Decision> getDecisionVersionsByKey(String key, Optional<Boolean> lazyLoad, CIBUser user);
	
	Object getHistoricDecisionInstances(Map<String, Object> queryParams, CIBUser user);
	Object getHistoricDecisionInstanceCount(Map<String, Object> queryParams, CIBUser user);
	Object getHistoricDecisionInstanceById(String id, Map<String, Object> queryParams, CIBUser user);
	Object deleteHistoricDecisionInstances(Map<String, Object> body, CIBUser user);
	Object setHistoricDecisionInstanceRemovalTime(Map<String, Object> body, CIBUser user);
  
	Collection<Job> getJobs(Map<String, Object> params, CIBUser user);
	void setSuspended(String id, Map<String, Object> data, CIBUser user);
	void deleteJob(String id, CIBUser user);
	Collection<Object> getHistoryJobLog(Map<String, Object> params, CIBUser user);
	String getHistoryJobLogStacktrace(String id, CIBUser user);
	Integer findHistoryTasksCount(Map<String, Object> filters, CIBUser user);

	Collection<CandidateGroupTaskCount> getTaskCountByCandidateGroup(CIBUser user);
	
	Collection<Batch> getBatches(Map<String, Object> params, CIBUser user);
	Collection<Batch> getBatchStatistics(Map<String, Object> params, CIBUser user);
	void deleteBatch(String id, Map<String, Object> params, CIBUser user);
	void setBatchSuspensionState(String id, Map<String, Object> params, CIBUser user);
	Collection<HistoryBatch> getHistoricBatches(Map<String, Object> params, CIBUser user);
	Object getHistoricBatchCount(Map<String, Object> queryParams);
	HistoryBatch getHistoricBatchById(String id, CIBUser user);
	void deleteHistoricBatch(String id, CIBUser user);
	Object setRemovalTime(Map<String, Object> payload);
	Object getCleanableBatchReport(Map<String, Object> queryParams);
	Object getCleanableBatchReportCount();

	JsonNode getTelemetryData(CIBUser user);
	Collection<Metric> getMetrics(Map<String, Object> queryParams, CIBUser user);

	Collection<Tenant> fetchTenants(Map<String, Object> queryParams, CIBUser user);
	Tenant fetchTenant(String tenantId, CIBUser user);
	void createTenant(Tenant tenant, CIBUser user);
	void updateTenant(Tenant tenant, CIBUser user);
	void deleteTenant(String tenantId, CIBUser user);
	void addMemberToTenant(String tenantId, String userId, CIBUser user);
	void deleteMemberFromTenant(String tenantId, String userId, CIBUser user);
	void addGroupToTenant(String tenantId, String groupId, CIBUser user);
	void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user);

	// Variable Instance method
	/**
	 * Retrieves a variable instance by its ID.
	 * @param id The ID of the variable instance
	 * @param deserializeValue Whether to deserialize the variable value or not
	 * @param user the user performing the search
	 * @return Variable instance details
	 * @throws SystemException in case of an error
	 * @throws NoObjectFoundException when the variable instance could not be found
	 */
	VariableInstance getVariableInstance(String id, Boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException;

	/**
	 * Retrieves a historic variable instance by its ID.
	 * @param id The ID of the historic variable instance
	 * @param deserializeValue Whether to deserialize the variable value or not
	 * @param user the user performing the search
	 * @return Historic variable instance details
	 * @throws SystemException in case of an error
	 * @throws NoObjectFoundException when the historic variable instance could not be found
	 */
	VariableHistory getHistoricVariableInstance(String id, Boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException;

	/**
	 * Get external tasks based on query parameters
	 * 
	 * @param queryParams Query parameters for filtering external tasks
	 * @param user the user performing the operation
	 * @return Collection of external tasks
	 * @throws SystemException in case of an error
	 */
	Collection<ExternalTask> getExternalTasks(Map<String, Object> queryParams, CIBUser user) throws SystemException;

	/**
	 * Fetch historic activity statistics for a given process definition ID.
	 *
	 * @param id the ID of the process definition
	 * @param params query parameters to filter statistics (e.g., canceled, finished, incidents)
	 * @param user the user performing the operation
	 * @return a list or map containing the historic activity statistics
	 * @throws SystemException in case of an error
	 */
	Object fetchHistoricActivityStatistics(String id, Map<String, Object> params, CIBUser user);

}
