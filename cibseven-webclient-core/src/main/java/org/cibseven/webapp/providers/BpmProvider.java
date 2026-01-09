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
import org.cibseven.webapp.rest.model.Engine;
import org.cibseven.webapp.rest.model.EventSubscription;
import org.cibseven.webapp.rest.model.ExternalTask;
import org.cibseven.webapp.rest.model.Filter;
import org.cibseven.webapp.rest.model.HistoricDecisionInstance;
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
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;

public interface BpmProvider {

	IDeploymentProvider getDeploymentProvider();
  IVariableProvider getVariableProvider();
  IVariableInstanceProvider getVariableInstanceProvider();
  IHistoricVariableInstanceProvider getHistoricVariableInstanceProvider();
  ITaskProvider getTaskProvider();
  IProcessProvider getProcessProvider();
  IActivityProvider getActivityProvider();
  IFilterProvider getFilterProvider();
  IUtilsProvider getUtilsProvider();
  IIncidentProvider getIncidentProvider();
  IJobDefinitionProvider getJobDefinitionProvider();
  IUserProvider getUserProvider();
  IDecisionProvider getDecisionProvider();
  IJobProvider getJobProvider();
  IBatchProvider getBatchProvider();
  ISystemProvider getSystemProvider();
  ITenantProvider getTenantProvider();
  IExternalTaskProvider getExternalTaskProvider();
  IEngineProvider getEngineProvider();

  /*

████████  █████  ███████ ██   ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
   ██    ██   ██ ██      ██  ██      ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
   ██    ███████ ███████ █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
   ██    ██   ██      ██ ██  ██      ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
   ██    ██   ██ ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

   */

	default Integer findTasksCount(Map<String, Object> filters, CIBUser user) throws SystemException {
		return getTaskProvider().findTasksCount(filters, user);
	}

	/**
   * Search tasks which belongs to a specific process instance.
   * @param processInstanceId filter by process instance id.
   * @param user the user performing the search
   * @return Fetched tasks.
   * @throws SystemException in case of an error.
   */
	default Collection<Task> findTasksByProcessInstance(String processInstanceId, CIBUser user) throws SystemException {
		return getTaskProvider().findTasksByProcessInstance(processInstanceId, user);
	}

	/**
   * Search tasks which belongs to a specific process instance and a user.
   * @param processInstanceId filter by process instance id.
   * @param createdAfter filter by creation date.
   * @param user the user performing the search
   * @return Fetched tasks.
   * @throws SystemException in case of an error.
   */
	default Collection<Task> findTasksByProcessInstanceAsignee(Optional<String> processInstanceId,
			Optional<String> createdAfter, CIBUser user) throws SystemException {
		return getTaskProvider().findTasksByProcessInstanceAsignee(processInstanceId, createdAfter, user);
	}

	/**
   * Search task with a specific Id.
   * @param taskId filter by task id.
   * @param user the user performing the search
   * @return Fetched task.
   * @throws NoObjectFoundException when the task searched for could not be found.
   * @throws SystemException in case of any other error.
   */
	default Task findTaskById(String taskId, CIBUser user) throws SystemException {
		return getTaskProvider().findTaskById(taskId, user);
	}

	/**
   * Update task.
   * @param task to be updated with the desired values already modified.
   * @param user the user performing the update
   * @throws SystemException in case of an error.
   */
	default void update(Task task, CIBUser user) throws SystemException {
		getTaskProvider().update(task, user);
	}

	/**
   * Set assignee to an specific task.
   * @param taskId filter by task id.
   * @param assignee to be set as assignee.
   * @param user the user performing the update
   * @throws SystemException in case of an error.
   */
	default void setAssignee(String taskId, String assignee, CIBUser user) throws SystemException {
		getTaskProvider().setAssignee(taskId, assignee, user);
	}

	/**
	 * Submit task without saving any variables, because that is done by the
	 * ui-element-template (in ours).
	 *
	 * @param taskId the ID of the task to be submitted.
	 * @param user the user performing the submission.
	 * @throws SubmitDeniedException when trying to submit a non-existing task.
	 * @throws SystemException in case of any other error.
	 */
	default void submit(String taskId, CIBUser user) throws SystemException, SubmitDeniedException {
		getTaskProvider().submit(taskId, user);
	}

	/**
	 * Submit task without saving any variables, because that is done by the ui-element-template (in ours).
     * @param taskId the ID of the task to be submitted.
     * @param user the user performing the submission.
     * @throws SubmitDeniedException when trying to submit a non-existing task.
     * @throws SystemException in case of any other error.
	 */
	default void submit(Task task, List<Variable> formResult, CIBUser user) throws SystemException, SubmitDeniedException {
		getTaskProvider().submit(task, formResult, user);
	}

	/**
	 * Fetch form-reference variable from task.
	 * @param taskId filter by task id.
	 * @param user the user performing the search
	 * @return form-reference
     * @throws NoObjectFoundException when the searched task could not be found.
     * @throws SystemException in case of any other error.
	 */
	default Object formReference(String taskId, CIBUser user) throws SystemException {
		return getTaskProvider().formReference(taskId, user);
	}

	/**
	 * Retrieves the form configuration data associated with a specific task.
	 *
	 * @param taskId
	 *          filter by task id.
	 * @param user
	 *          the user performing the search
	 * @return TaskForm object containing key, camundaFormRef, and contextPath
	 * @throws NoObjectFoundException
	 *           when the searched task could not be found.
	 * @throws SystemException
	 *           in case of any other error.
	 */
	default Object form(String taskId, CIBUser user) throws SystemException {
		return getTaskProvider().form(taskId, user);
	}

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
	default Collection<Task> findTasksByFilter(TaskFiltering filters, String filterId, CIBUser user, Integer firstResult, Integer maxResults) throws SystemException {
		return getTaskProvider().findTasksByFilter(filters, filterId, user, firstResult, maxResults);
	}

	/**
	* Find Tasks count by filter
	* @param filterId to filter task.
	* @param filters list of properties which will be use to filter tasks.
	* @param user since this call is secured we need the user to authenticate.
	* @return Collection of Tasks fetched in the search.
	* @throws SystemException in case of an error.
	*/
	default Integer findTasksCountByFilter(String filterId, CIBUser user, TaskFiltering filters) throws SystemException {
		return getTaskProvider().findTasksCountByFilter(filterId, user, filters);
	}

	/**
	 * Search tasks which belongs to a specific process instance.
	 * The tasks found belongs to the history, they have other attributes and finished tasks
	 * are also fetched.
	 * @param processInstanceId filter by process instance id.
	 * @param user the user performing the search
	 * @return Fetched tasks.
	   * @throws SystemException in case of an error.
	 */
	default Collection<TaskHistory> findTasksByProcessInstanceHistory(String processInstanceId, CIBUser user) throws SystemException {
		return getTaskProvider().findTasksByProcessInstanceHistory(processInstanceId, user);
	}

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
	default Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId, CIBUser user) throws SystemException {
		return getTaskProvider().findTasksByDefinitionKeyHistory(taskDefinitionKey, processInstanceId, user);
	}

	/**
	* Required by OFDKA
	* Queries for tasks that fulfill a given filter. This method is slightly more powerful than the Get Tasks method because it allows
	* filtering by multiple process or task variables of types String, Number or Boolean.
	* @param data variables to apply search.
	* @param user the user performing the search.
	* @return Collection tasks fetched in the search.
	* @throws SystemException in case of an error.
	*/
	default Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException {
		return getTaskProvider().findTasksPost(data, user);
	}

	/**
	 *  Identity links, e.g. to get the candidates user or groups of a task.
	 *
	 * @param taskId the ID of the task.
	 * @param type Filter by the type of links to include. e.g. "candidate".
	 * @param user the user performing the query.
	 * @return Collection of Identity Links.
	 */
	default Collection<IdentityLink> findIdentityLink(String taskId, Optional<String> type, CIBUser user) {
		return getTaskProvider().findIdentityLink(taskId, type, user);
	}

	/**
	 *  Create identity links, e.g., to set the candidates user or groups of a task.
	 *
	 * @param taskId the ID of the task.
	 * @param data a map containing the type of the identity link and group or user ID.
	 * @param user the user performing the operation.
	 * @throws SystemException in case of any other error.
	 */
	default void createIdentityLink(String taskId, Map<String, Object> data, CIBUser user) throws SystemException {
		getTaskProvider().createIdentityLink(taskId, data, user);
	}

	/**
	 *  Delete identity links, e.g., to remove the candidates user or groups of a task.
	 *
	 * @param taskId the ID of the task.
	 * @param type a map containing the type of the identity link to be removed.
	 * @param user the user performing the operation.
	 * @throws SystemException in case of any other error.
	 */
	default void deleteIdentityLink(String taskId, Map<String, Object> data, CIBUser user) throws SystemException {
		getTaskProvider().deleteIdentityLink(taskId, data, user);
	}

	/**
	 * Reports a business error in the context of a running task by id. The error code must be specified to identify the BPMN error handler.
	 * @param taskId filter by task id.
	 * @param data variables for the BPMN error reporting.
	 * @param user the user performing the operation.
	 * @throws SystemException in case of any other error.
	 */
	default void handleBpmnError(String taskId, Map<String, Object> data, CIBUser user) throws SystemException {
		getTaskProvider().handleBpmnError(taskId, data, user);
	}

	default Collection<TaskHistory> findTasksByTaskIdHistory(String taskId, CIBUser user) throws SystemException {
		return getTaskProvider().findTasksByTaskIdHistory(taskId, user);
	}

	default ResponseEntity<byte[]> getDeployedForm(String taskId, CIBUser user) throws SystemException {
		return getTaskProvider().getDeployedForm(taskId, user);
	}

	default Integer findHistoryTasksCount(Map<String, Object> filters, CIBUser user) throws SystemException {
		return getTaskProvider().findHistoryTasksCount(filters, user);
	}

	default Collection<CandidateGroupTaskCount> getTaskCountByCandidateGroup(CIBUser user)  {
		return getTaskProvider().getTaskCountByCandidateGroup(user);
	}

	/**
	 * Search tasks, which contains specified filter.
	 *
	 * @param filter applied in the search
	 * @param user the user performing the search
	 * @return Collection tasks fetched in the search.
	 * @throws SystemException in case of an error.
	 */
	default Collection<Task> findTasks(String filter, CIBUser user) throws SystemException {
		return getTaskProvider().findTasks(filter, user);
	}

/*

███████ ██ ██      ████████ ███████ ██████      ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██      ██ ██         ██    ██      ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
█████   ██ ██         ██    █████   ██████      ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
██      ██ ██         ██    ██      ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
██      ██ ███████    ██    ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

 */

	/**
   * Search filters.
   * @param user the user performing the query.
   * @return Collection of Filters fetched in the search.
   * @throws SystemException in case of an error.
   */
	default Collection<Filter> findFilters(CIBUser user) throws SystemException {
		return getFilterProvider().findFilters(user);
	}

	/**
   * Create filter.
   * @param filter to be created.
   * @param user the user performing the creation.
   * @throws SystemException in case of an error.
   */
	default  Filter createFilter(Filter filter, CIBUser user) throws SystemException {
		return getFilterProvider().createFilter(filter, user);
	}

	/**
   * Update filter.
   * @param filter to be updated.
   * @param user the user performing the update.
   * @throws NoObjectFoundException when the filter to be changed could not be found.
   * @throws SystemException in case of any other error.
   */
	default void updateFilter(Filter filter, CIBUser user) throws SystemException, NoObjectFoundException {
		getFilterProvider().updateFilter(filter, user);
	}

	/**
   * Delete filter.
   * @param filterId the ID of the filter to be deleted.
   * @param user the user performing the deletion.
   * @throws SystemException in case of an error.
   */
	default void deleteFilter(String filterId, CIBUser user) throws SystemException {
		getFilterProvider().deleteFilter(filterId, user);
	}

/*

██████  ███████ ██████  ██       ██████  ██    ██ ███    ███ ███████ ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██   ██ ██      ██   ██ ██      ██    ██  ██  ██  ████  ████ ██      ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
██   ██ █████   ██████  ██      ██    ██   ████   ██ ████ ██ █████   ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
██   ██ ██      ██      ██      ██    ██    ██    ██  ██  ██ ██      ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
██████  ███████ ██      ███████  ██████     ██    ██      ██ ███████ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

 */

	/**
   * Deploy process-bpmn.
 * @param data metadata of the diagram to be deployed (deployment-name, deployment-source, deploy-changed-only).
 * @param file of the diagram to be deployed.
 * @param user the user performing the deployment.
 * @return Deployment information.
   * @throws SystemException in case of any other error.
   */
	default Deployment deployBpmn(MultiValueMap<String, Object> data, MultiValueMap<String, MultipartFile> file, CIBUser user) throws SystemException {
		return getDeploymentProvider().deployBpmn(data, file, user);

	}

	/**
	 * Retrieves number of all deployments with provided query.
	 * @param user the user performing the search.
	 * @return Fetched deployments.
	 * @throws SystemException in case of any other error.
	 */
	default Long countDeployments(CIBUser user, String nameLike) throws SystemException {
		return getDeploymentProvider().countDeployments(user, nameLike);
	}

	/**
	 * Retrieves all deployments matched with provided query.
	 * @param user the user performing the search.
	 * @return Fetched deployments.
	 * @throws SystemException in case of any other error.
	 */
	default Collection<Deployment> findDeployments(CIBUser user, String nameLike, int firstResult, int maxResults, String sortBy, String sortOrder) throws SystemException {
		return getDeploymentProvider().findDeployments(user, nameLike, firstResult, maxResults, sortBy, sortOrder);
	}

	/**
	 * Retrieves all deployment resources of a given deployment.
	 * @param deploymentId the ID of the deployment.
	 * @param user the user performing the query.
	 * @return Fetched deployment resources.
     * @throws SystemException in case of any other error.
	 */
	default Deployment findDeployment(String deploymentId, CIBUser user) throws SystemException {
		return getDeploymentProvider().findDeployment(deploymentId, user);
	}

	/**
     * Search deployment with a specific Id.
     * @param deploymentId the ID of the deployment.
     * @return Fetched deployment.
     * @throws SystemException in case of any other error.
     */
	default Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user) throws SystemException {
		return getDeploymentProvider().findDeploymentResources(deploymentId, user);
	}

	/**
	 * Retrieves the binary content of a deployment resource for the given deployment by id.
	 * @param rq the HTTP request.
	 * @param deploymentId the ID of the deployment.
	 * @param resourceId the ID of the resource.
	 * @param fileName the name of the file.
	 * @param user the authenticated user.
	 * @return resource data.
     * @throws SystemException in case of any other error.
	 */
	default Data fetchDataFromDeploymentResource(HttpServletRequest rq, String deploymentId, String resourceId, String fileName, CIBUser user) throws SystemException {
		return getDeploymentProvider().fetchDataFromDeploymentResource(rq, deploymentId, resourceId, fileName, user);
	}

	/**
	 * Delete deployment by an Id.
	 * @param deploymentId the ID of the deployment.
	 * @param cascade whether to cascade the deletion.
	 * @param user the user performing the deletion.
     * @throws SystemException in case of any other error.
	 */
	default void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException {
		getDeploymentProvider().deleteDeployment(deploymentId, cascade, user);
	}

	/**
	 * Creates a new deployment using the Camunda REST API.
	 *
	 * @param data the deployment parameters (deployment-name, deployment-source, tenant-id, etc.)
	 * @param files the files to deploy (DMN, BPMN, etc.)
	 * @param user the user creating the deployment
	 * @return the created deployment
	 * @throws SystemException in case of an error
	 */
	default Deployment createDeployment(MultiValueMap<String, Object> data, MultipartFile[] files, CIBUser user) throws SystemException {
		return getDeploymentProvider().createDeployment(data, files, user);
	}

	/**
	 * Redeploy an existing deployment.
	 * For every contained decision or process definition a new version will be created.
	 *
	 * @param id the ID of the deployment to redeploy
	 * @param data the redeployment parameters (tenantId, source, resourceIds, resourceNames)
	 * @param user the user performing the redeployment
	 * @return the newly created deployment
	 * @throws SystemException in case of an error
	 */
	default Deployment redeployDeployment(String id, Map<String, Object> data, CIBUser user) throws SystemException {
		return getDeploymentProvider().redeployDeployment(id, data, user);
	}

/*

 █████   ██████ ████████ ██ ██    ██ ██ ████████ ██    ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██   ██ ██         ██    ██ ██    ██ ██    ██     ██  ██      ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
███████ ██         ██    ██ ██    ██ ██    ██      ████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
██   ██ ██         ██    ██  ██  ██  ██    ██       ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
██   ██  ██████    ██    ██   ████   ██    ██       ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

 */

	/**
   * Search activity that belong to a process instance.
   * @param processInstanceId filter by process instance id.
   * @param user the user performing the search
   * @return Fetched activity.
   * @throws NoObjectFoundException when the searched process instance could not be found.
   * @throws SystemException in case of any other error.
   */
	default ActivityInstance findActivityInstance(String processInstanceId, CIBUser user) throws SystemException, NoObjectFoundException {
		return getActivityProvider().findActivityInstance(processInstanceId, user);
	}

	/**
	   * Queries for historic activity instances that fulfill the given parameters.
	 * The activities found belong to the history.
	   * @param queryParams a map of parameters to filter the query.
	   * @param user the user performing the query.
	   * @return Fetched Historic Activity Instances.
	   * @throws InvalidAttributeValueException when the tenant of a task could not be changed or when the delegation state of a task should be changed to an invalid value.
	   * @throws SystemException in case of any other error.
	   */
	default List<ActivityInstanceHistory> findActivitiesInstancesHistory(Map<String, Object> queryParams, CIBUser user) throws SystemException, InvalidAttributeValueException {
		return getActivityProvider().findActivitiesInstancesHistory(queryParams, user);
	}

	/**
	   * Search activities instances that belong to a process instance. The activities found belongs
	   * to the history, they have other attributes and activities from finished processes are also fetched.
	   * @param processInstanceId filter by process instance id.
	   * @param user the user performing the search
	   * @return Fetched Activity Instance.
	   * @throws InvalidAttributeValueException when the tenant of a task could not be changed or when the delegation state of a task should be changed to an invalid value.
	   * @throws SystemException in case of any other error.
	   */
	default List<ActivityInstanceHistory> findActivitiesInstancesHistory(String processInstanceId, CIBUser user) throws SystemException, InvalidAttributeValueException {
		return getActivityProvider().findActivitiesInstancesHistory(processInstanceId, user);
	}

	/*UI Element templates methods migrated*/

	default ActivityInstance findActivityInstances(String processInstanceId, CIBUser user) throws SystemException {
		return getActivityProvider().findActivityInstances(processInstanceId, user);
	}

	default List<ActivityInstanceHistory> findActivityInstanceHistory(String processInstanceId, CIBUser user) throws SystemException {
		return getActivityProvider().findActivityInstanceHistory(processInstanceId, user);
	}

	default void deleteVariableByExecutionId(String executionId, String variableName, CIBUser user) {
		getActivityProvider().deleteVariableByExecutionId(executionId, variableName, user);
	}

	default void deleteVariableHistoryInstance(String id, CIBUser user) {
		getActivityProvider().deleteVariableHistoryInstance(id, user);
	}

	default Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(String processDefinitionId, Map<String, Object> params, CIBUser user) {
		return getActivityProvider().findActivitiesProcessDefinitionHistory(processDefinitionId, params, user);
	}

/*

██    ██ ███████ ███████ ██████      ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██    ██ ██      ██      ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
██    ██ ███████ █████   ██████      ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
██    ██      ██ ██      ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
 ██████  ███████ ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

*/

	/**
	 * Get authorizations, filtered by userId and groups in which user belongs.
	 * @param userId filter user identification (username).
	 * @param user the user performing the search
	 * @return Fetched bpmn
     * @throws SystemException in case of an error.
	 */
	default Authorizations getUserAuthorization(String userId, CIBUser user) throws SystemException {
		return getUserProvider().getUserAuthorization(userId, user);
	}

	default Collection<SevenUser> fetchUsers(CIBUser user) throws SystemException {
		return getUserProvider().fetchUsers(user);
	}

	/**
	*
	* @param username login user
	* @param password login password
	* @param user the calling user
	* @return verification
	* @throws SystemException
	*/
	default SevenVerifyUser verifyUser(String username, String password, CIBUser user) throws SystemException {
		return getUserProvider().verifyUser(username, password, user);
	}

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
	default Collection<User> findUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike, Optional<String> lastName, Optional<String> lastNameLike,
			Optional<String> email, Optional<String> emailLike, Optional<String> memberOfGroup, Optional<String> memberOfTenant, Optional<String> idIn, 
			Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder, CIBUser user) {
		return getUserProvider().findUsers(id, firstName, firstNameLike, lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder, user);
	}

	/**
	 * Get the count of users in the system with optional filters.
	 * 
	 * @param filters the filters to apply (e.g., memberOfGroup). Can be null or empty for no filtering.
	 * @param user the user performing the operation.
	 * @return the count of users matching the filters.
	 */
	default long countUsers(Map<String, Object> filters, CIBUser user) {
		return getUserProvider().countUsers(filters, user);
	}

	/**
	 * Create a new user.
	 *
	 * @param user the new user to be created.
	 * @param flowUser the user performing the creation.
	 * @throws InvalidUserIdException when the user ID is invalid.
	 */
	default void createUser(NewUser user, CIBUser flowUser) throws InvalidUserIdException {
		getUserProvider().createUser(user, flowUser);
	}

	/**
	 * Updates a user’s profile.
	 *
	 * @param userId the ID of the user to be updated.
	 * @param user the user to Update.
	 * @param flowUser the user performing the update.
	 */
	default void updateUserProfile(String userId, User user, CIBUser flowUser) {
		getUserProvider().updateUserProfile(userId, user, flowUser);
	}

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
	default void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user) {
		getUserProvider().updateUserCredentials(userId, data, user);
	}

	/**
	 * Get groups by id, ....
	 *
	 * @param id // Filter by the id of the group.
	 * @param name // Filter by the name of the group.
	 * @param nameLike // Filter by the name that the parameter is a substring of.
	 * @param type // Filter by the type of the group.
	 * @param member // Only retrieve groups which the given user id is a member of.
	 * @param memberOfTenant // Only retrieve groups which are members of the given tenant.
	 * @param sortBy // Sort the results lexicographically by a given criterion. Valid values are id, name and type. Must be used in conjunction with the sortOrder parameter.
	 * @param sortOrder // Sort the results in a given order. Values may be asc for ascending order or desc for descending order. Must be used in conjunction with the sortBy parameter.
	 * @param firstResult // Pagination of results. Specifies the index of the first result to return.
	 * @param maxResults // Pagination of results. Specifies the maximum number of results to return. Will return less results if there are no more results left.
	 *
	 * @param user the user performing the search.
	 * @return Collection of User Groups.
	 */
	default Collection<UserGroup> findGroups(Optional<String> id, Optional<String> name, Optional<String> nameLike, Optional<String> type,
			Optional<String> member, Optional<String> memberOfTenant, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
			Optional<String> maxResults, CIBUser user) {
		return getUserProvider().findGroups(id, name, nameLike, type, member, memberOfTenant, sortBy, sortOrder, firstResult, maxResults, user);
	}

	/**
	 * Create a group.
	 *
	 * @param group the group to be created.
	 * @param user the user performing the creation.
	 */
	default void createGroup(UserGroup group, CIBUser user) {
		getUserProvider().createGroup(group, user);
	}

	/**
	 * Updates a group.
	 *
	 * @param groupId the ID of the group to be updated.
	 * @param group the group to be updated.
	 * @param user the user performing the update.
	 */
	default void updateGroup(String groupId, UserGroup group, CIBUser user) {
		getUserProvider().updateGroup(groupId, group, user);
	}

	/**
	 * Deletes a group by id.
	 *
	 * @param groupId the ID of the group to be deleted.
	 * @param user the user performing the deletion.
	 */
	default void deleteGroup(String groupId, CIBUser user) {
		getUserProvider().deleteGroup(groupId, user);
	}

	/**
	 * Get Authorization by id, ....
	 *
	 * @param id // Filter by the id.
	 * @param type //  	Filter by authorization type. (0=global, 1=grant, 2=revoke). See the User Guide for more information about authorization types.
	 * @param userIdIn //  	Filter by a comma-separated list of userIds.
	 * @param groupIdIn //  	Filter by a comma-separated list of groupIds.
	 * @param resourceType //  	Filter by an integer representation of the resource type. See the User Guide for a list of integer representations of resource types.
	 * @param resourceId //  	Filter by resource id.	 * @param sortBy, // Sort the results lexicographically by a given criterion. Valid values are id, name and type. Must be used in conjunction with the sortOrder parameter.
	 * @param sortBy // Sort the results lexicographically by a given criterion. Valid values are id, name and type. Must be used in conjunction with the sortOrder parameter.
	 * @param sortOrder // Sort the results in a given order. Values may be asc for ascending order or desc for descending order. Must be used in conjunction with the sortBy parameter.
	 * @param firstResult // Pagination of results. Specifies the index of the first result to return.
	 * @param maxResults // Pagination of results. Specifies the maximum number of results to return. Will return less results if there are no more results left.
	 * @param user the user performing the search.
	 * @return Collection of Authorizations.
	 */
	default Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type, Optional<String> userIdIn, Optional<String> groupIdIn,
			Optional<String> resourceType, Optional<String> resourceId, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
			Optional<String> maxResults, CIBUser user) {
		return getUserProvider().findAuthorization(id, type, userIdIn, groupIdIn, resourceType, resourceId, sortBy, sortOrder, firstResult, maxResults, user);
	}

	/**
	 * Create an authorization.
	 *
	 * @param authorization the authorization to be created.
	 * @param user the user performing the creation.
	 * @return ResponseEntity containing the created authorization.
	 */
	default ResponseEntity<Authorization> createAuthorization(Authorization authorization, CIBUser user) {
		return getUserProvider().createAuthorization(authorization, user);
	}

	/**
	 * Update an authorization by id.
	 *
	 * @param authorizationId the ID of the authorization to be updated.
	 * @param data the data to update.
	 * @param user the user performing the update.
	 */
	default void updateAuthorization(String authorizationId, Map<String, Object> data, CIBUser user) {
		getUserProvider().updateAuthorization(authorizationId, data, user);
	}

	/**
	 * Deletes an authorization by id.
	 *
	 * @param authorizationId the ID of the authorization to be deleted.
	 * @param user the user performing the deletion.
	 */
	default void deleteAuthorization(String authorizationId, CIBUser user) {
		getUserProvider().deleteAuthorization(authorizationId, user);
	}

/*

██    ██  █████  ██████  ██  █████  ██████  ██      ███████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██    ██ ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
██    ██ ███████ ██████  ██ ███████ ██████  ██      █████   ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
 ██  ██  ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██           ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ████   ██   ██ ██   ██ ██ ██   ██ ██████  ███████ ███████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

*/

/**
 * Modify a variable in the Process Instance.
 * @param executionId Id of the execution.
 * @param data to be updated.
 * @param user User who is modifying the variable.
	* @throws SystemException in case of any other error.
 */
	default void modifyVariableByExecutionId(String executionId, Map<String, Object> data, CIBUser user) throws SystemException {
		getVariableProvider().modifyVariableByExecutionId(executionId, data, user);
	}

	/**
	 * Modify a variable data in the Process Instance.
	 * @param executionId the ID of the execution.
	 * @param variableName the name of the variable.
	 * @param data the file containing the data to be updated.
	 * @param valueType the type of the variable. Enum with the possible values: "File", "Bytes".
	 * @param user the user modifying the variable.
     * @throws SystemException in case of any other error.
	 */
	default void modifyVariableDataByExecutionId(String executionId, String variableName, MultipartFile data, String valueType, CIBUser user) throws SystemException {
		getVariableProvider().modifyVariableDataByExecutionId(executionId, variableName, data, valueType, user);
	}

	 /**
	 * Fetch a variables from a process instance.
	 * @param processInstanceId Id of the instance.
	 * @param data a map of parameters to filter the query.
	 * @param user User who is fetching the variables.
	 * @return Data.
    * @throws SystemException in case of any other error.
	 */ 
	default Collection<Variable> fetchProcessInstanceVariables(String processInstanceId, Map<String, Object> data, CIBUser user) throws NoObjectFoundException, SystemException {
		return getVariableProvider().fetchProcessInstanceVariables(processInstanceId, data, user);
	}

	 /**
	 * Fetch a variable data in the Process Instance.
	 * @param executionId Id of the execution.
	 * @param variableName Name of the variable.
	 * @param user User who is fetching the variable.
	 * @return Data.
    * @throws SystemException in case of any other error.
	 */ 
		default ResponseEntity<byte[]> fetchVariableDataByExecutionId(String executionId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException  {
			return getVariableProvider().fetchVariableDataByExecutionId(executionId, variableName, user);
		}

	 /**
	 * Fetch a variable data in from the process history.
	 * @param id Id of the variable.
	 * @param user User who is modifying the variable.
	 * @return Data.
    * @throws SystemException in case of any other error.
	 */ 
		default ResponseEntity<byte[]> fetchHistoryVariableDataById(String id, CIBUser user) throws NoObjectFoundException, SystemException  {
			return getVariableProvider().fetchHistoryVariableDataById(id, user);
		}

	/**
	 * Fetch variables from a specific process instance.
	 * The variables found belong to the history, they have other attributes, and variables from finished process instances are also fetched.
     * @param processInstanceId filter by process instance id.
	 * @param data a map of parameters to filter the query.
	 * @param user the user performing the search
     * @return Fetched variables.
     * @throws SystemException in case of an error.
     */
	default Collection<VariableHistory> fetchProcessInstanceVariablesHistory(String processInstanceId, Map<String, Object> data, CIBUser user) throws SystemException {
		return getVariableProvider().fetchProcessInstanceVariablesHistory(processInstanceId, data, user);
	}

	/**
	 * Fetch variables from a specific activity.
	 * The variables found belongs to the history, they have other attributes 
	 * and variables from finished activities are also fetched.
     * @param activityInstanceId filter by activity instance id.
     * @param user the user performing the search
     * @return Fetched variables.
     * @throws SystemException in case of an error.
     */
	default Collection<VariableHistory> fetchActivityVariablesHistory(String activityInstanceId, CIBUser user) throws SystemException {
	return getVariableProvider().fetchActivityVariablesHistory(activityInstanceId, user);
}

	/**
	 * Fetch variables from a specific activity.
     * @param activityInstanceId filter by activity instance id.
     * @param user the user performing the search
     * @return Fetched variables.
     * @throws SystemException in case of an error.
     */
	default Collection<VariableHistory> fetchActivityVariables(String activityInstanceId, CIBUser user) throws SystemException {
		return getVariableProvider().fetchActivityVariables(activityInstanceId, user);
	}

	default Variable fetchVariable(String taskId, String variableName, 
			boolean deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException {
		return getVariableProvider().fetchVariable(taskId, variableName, deserializeValue, user);
	}

	default void deleteVariable(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException {
		getVariableProvider().deleteVariable(taskId, variableName, user);
	}

	default Map<String, Variable> fetchFormVariables(String taskId, boolean deserializeValues, CIBUser user) throws NoObjectFoundException, SystemException {
		return getVariableProvider().fetchFormVariables(taskId, deserializeValues, user);
	}

	default Map<String, Variable> fetchFormVariables(List<String> variableListName, String taskId, CIBUser user) throws NoObjectFoundException, SystemException {
		return getVariableProvider().fetchFormVariables(variableListName, taskId, user);
	}

	default Map<String, Variable> fetchProcessFormVariables(String key, CIBUser user) throws NoObjectFoundException, SystemException {
		return getVariableProvider().fetchProcessFormVariables(key, user);
	}

	default NamedByteArrayDataSource fetchVariableFileData(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, UnexpectedTypeException, SystemException {
		return getVariableProvider().fetchVariableFileData(taskId, variableName, user);
	}

	default void uploadVariableFileData(String taskId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException {
		getVariableProvider().uploadVariableFileData(taskId, variableName, data, valueType, user);
	}

	default ResponseEntity<byte[]> fetchProcessInstanceVariableData(String processInstanceId, String variableName,
			CIBUser user) throws NoObjectFoundException, SystemException {
		return getVariableProvider().fetchProcessInstanceVariableData(processInstanceId, variableName, user);
	}

	default void uploadProcessInstanceVariableFileData(String processInstanceId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException {
		getVariableProvider().uploadProcessInstanceVariableFileData(processInstanceId, variableName, data, valueType, user);
	}

	default ProcessStart submitStartFormVariables(String processDefinitionId, List<Variable> formResult, CIBUser user) throws SystemException {
		return getVariableProvider().submitStartFormVariables(processDefinitionId, formResult, user);
	}

	default Variable fetchVariableByProcessInstanceId(String processInstanceId, String variableName, CIBUser user) throws SystemException {
		return getVariableProvider().fetchVariableByProcessInstanceId(processInstanceId, variableName, user);
	}

	default void saveVariableInProcessInstanceId(String processInstanceId, List<Variable> variables, CIBUser user) throws SystemException {
		getVariableProvider().saveVariableInProcessInstanceId(processInstanceId, variables, user);
	}

	default void submitVariables(String processInstanceId, List<Variable> formResult, CIBUser user, String processDefinitionId) throws SystemException {
		getVariableProvider().submitVariables(processInstanceId, formResult, user, processDefinitionId);
	}

	default Map<String, Variable> fetchProcessFormVariablesById(String id, CIBUser user) throws SystemException {
		return getVariableProvider().fetchProcessFormVariablesById(id, user);
	}

	default void putLocalExecutionVariable(String executionId, String varName, Map<String, Object> data, CIBUser user) {
		getVariableProvider().putLocalExecutionVariable(executionId, varName, data, user);
	}

/*

██████  ███████  ██████ ██ ███████ ██  ██████  ███    ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██   ██ ██      ██      ██ ██      ██ ██    ██ ████   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
██   ██ █████   ██      ██ ███████ ██ ██    ██ ██ ██  ██     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
██   ██ ██      ██      ██      ██ ██ ██    ██ ██  ██ ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
██████  ███████  ██████ ██ ███████ ██  ██████  ██   ████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

*/

	default Collection<Decision> getDecisionDefinitionList(Map<String, Object> queryParams, CIBUser user) {
		return getDecisionProvider().getDecisionDefinitionList(queryParams, user);
	}

	default Long getDecisionDefinitionListCount(Map<String, Object> queryParams, CIBUser user) {
		return getDecisionProvider().getDecisionDefinitionListCount(queryParams, user);
	}

	default Decision getDecisionDefinitionByKey(String key, CIBUser user) {
		return getDecisionProvider().getDecisionDefinitionByKey(key, user);
	}

	default Object getDiagramByKey(String key, CIBUser user) {
		return getDecisionProvider().getDiagramByKey(key, user);
	}

	default Object evaluateDecisionDefinitionByKey(Map<String, Object> data, String key, CIBUser user) {
		return getDecisionProvider().evaluateDecisionDefinitionByKey(data, key, user);
	}

	default void updateHistoryTTLByKey(Map<String, Object> data, String key, CIBUser user) {
		getDecisionProvider().updateHistoryTTLByKey(data, key, user);
	}

	default Object getDiagramByKeyAndTenant(String key, String tenant, CIBUser user) {
		return getDecisionProvider().getDiagramByKeyAndTenant(key, tenant, user);
	}

	default Object evaluateDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user) {
		//TODO: not implemented in DecisionProvider
		//interface should contain parameters like evaluateDecisionDefinitionByKey 
		return getDecisionProvider().evaluateDecisionDefinitionByKeyAndTenant(key, tenant, user);
	}

	default Object updateHistoryTTLByKeyAndTenant(String key, String tenant, CIBUser user) {
		//TODO: not implemented in DecisionProvider
		//interface should contain parameters like HistoryTTLByKey 
		return getDecisionProvider().updateHistoryTTLByKeyAndTenant(key, tenant, user);
	}

	default Object getXmlByKey(String key, CIBUser user) {
		return getDecisionProvider().getXmlByKey(key, user);
	}

	default Object getXmlByKeyAndTenant(String key, String tenant, CIBUser user) {
		return getDecisionProvider().getXmlByKeyAndTenant(key, tenant, user);
	}

	default Decision getDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user) {
		return getDecisionProvider().getDecisionDefinitionByKeyAndTenant(key, tenant, user);
	}

	default Decision getDecisionDefinitionById(String id, Optional<Boolean> extraInfo, CIBUser user) {
		return getDecisionProvider().getDecisionDefinitionById(id, extraInfo, user);
	}

	default Object getDiagramById(String id, CIBUser user) {
		return getDecisionProvider().getDiagramById(id, user);
	}

	default Object evaluateDecisionDefinitionById(String id, CIBUser user) {
		//TODO: not implemented in DecisionProvider
		return getDecisionProvider().evaluateDecisionDefinitionById(id, user);
	}

	default void updateHistoryTTLById(String id, Map<String, Object> data, CIBUser user) {
		getDecisionProvider().updateHistoryTTLById(id, data, user);
	}

	default Object getXmlById(String id, CIBUser user) throws SystemException {
		return getDecisionProvider().getXmlById(id, user);
	}

	default Collection<Decision> getDecisionVersionsByKey(String key, Optional<Boolean> lazyLoad, CIBUser user) {
		return getDecisionProvider().getDecisionVersionsByKey(key, lazyLoad, user);
	}

	default Collection<HistoricDecisionInstance> getHistoricDecisionInstances(Map<String, Object> queryParams, CIBUser user) {
		return getDecisionProvider().getHistoricDecisionInstances(queryParams, user);
	}

	default Long getHistoricDecisionInstanceCount(Map<String, Object> queryParams, CIBUser user) {
		return getDecisionProvider().getHistoricDecisionInstanceCount(queryParams, user);
	}

	default HistoricDecisionInstance getHistoricDecisionInstanceById(String id, Map<String, Object> queryParams, CIBUser user) {
		return getDecisionProvider().getHistoricDecisionInstanceById(id, queryParams, user);
	}

	default Object deleteHistoricDecisionInstances(Map<String, Object> data, CIBUser user) {
		return getDecisionProvider().deleteHistoricDecisionInstances(data, user);
	}

	default Object setHistoricDecisionInstanceRemovalTime(Map<String, Object> data, CIBUser user) {
		return getDecisionProvider().setHistoricDecisionInstanceRemovalTime(data, user);
	}

/*

     ██  ██████  ██████      ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
     ██ ██    ██ ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
     ██ ██    ██ ██████      ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
██   ██ ██    ██ ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
 █████   ██████  ██████      ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

*/

	default Collection<JobDefinition> findJobDefinitions(String params, CIBUser user) {
		return getJobDefinitionProvider().findJobDefinitions(params, user);
	}

	default void suspendJobDefinition(String jobDefinitionId, String params, CIBUser user) {
		getJobDefinitionProvider().suspendJobDefinition(jobDefinitionId, params, user);
	}

	default void overrideJobDefinitionPriority(String jobDefinitionId, String params, CIBUser user) {
		getJobDefinitionProvider().overrideJobDefinitionPriority(jobDefinitionId, params, user);
	}

	default void retryJobDefinitionById(String id, Map<String, Object> params, CIBUser user) {
		getJobDefinitionProvider().retryJobDefinitionById(id, params, user);
	}

	default Collection<Job> getJobs(Map<String, Object> params, CIBUser user) {
		return getJobProvider().getJobs(params, user);
	}

	default void setSuspended(String id, Map<String, Object> params, CIBUser user) {
		getJobProvider().setSuspended(id, params, user);
	}

	default void deleteJob(String id, CIBUser user) {
		getJobProvider().deleteJob(id, user);
	}

	default JobDefinition findJobDefinition(String id, CIBUser user) {
		return getJobDefinitionProvider().findJobDefinition(id, user);
	}

	default Collection<Object> getHistoryJobLog(Map<String, Object> params, CIBUser user) {
		return getJobProvider().getHistoryJobLog(params, user);
	}

	default String getHistoryJobLogStacktrace(String id, CIBUser user) {
		return getJobProvider().getHistoryJobLogStacktrace(id, user);
	}

	default void changeDueDate(String id, Map<String, Object> data, CIBUser user) {
		getJobProvider().changeDueDate(id, data, user);
	}

	default void recalculateDueDate(String id, Map<String, Object> params, CIBUser user) {
		getJobProvider().recalculateDueDate(id, params, user);
	}

/*

██████   █████  ████████  ██████ ██   ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██   ██ ██   ██    ██    ██      ██   ██     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
██████  ███████    ██    ██      ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
██   ██ ██   ██    ██    ██      ██   ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
██████  ██   ██    ██     ██████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

*/

	default Collection<Batch> getBatches(Map<String, Object> params, CIBUser user) {
		return getBatchProvider().getBatches(params, user);
	}

	default Collection<Batch> getBatchStatistics(Map<String, Object> params, CIBUser user) {
		return getBatchProvider().getBatchStatistics(params, user);
	}

	default void deleteBatch(String id, Map<String, Object> params, CIBUser user) {
		getBatchProvider().deleteBatch(id, params, user);
	}

	default void setBatchSuspensionState(String id, Map<String, Object> params, CIBUser user) {
		getBatchProvider().setBatchSuspensionState(id, params, user);
	}

	default Collection<HistoryBatch> getHistoricBatches(Map<String, Object> params, CIBUser user) {
		return getBatchProvider().getHistoricBatches(params, user);
	}

	default Long getHistoricBatchCount(Map<String, Object> queryParams, CIBUser user) {
		return getBatchProvider().getHistoricBatchCount(queryParams, user);
	}

	default HistoryBatch getHistoricBatchById(String id, CIBUser user) {
		return getBatchProvider().getHistoricBatchById(id, user);
	}

	default void deleteHistoricBatch(String id, CIBUser user) {
		getBatchProvider().deleteHistoricBatch(id, user);
	}

	default Object setRemovalTime(Map<String, Object> payload, CIBUser user) {
		return getBatchProvider().setRemovalTime(payload, user);
	  }

	default Object getCleanableBatchReport(Map<String, Object> queryParams, CIBUser user) {
		return getBatchProvider().getCleanableBatchReport(queryParams, user);
	  }

	default Object getCleanableBatchReportCount(CIBUser user) {
		return getBatchProvider().getCleanableBatchReportCount(user);
	  }

/*

███████ ██    ██ ███████ ████████ ███████ ███    ███     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██       ██  ██  ██         ██    ██      ████  ████     ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
███████   ████   ███████    ██    █████   ██ ████ ██     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
     ██    ██         ██    ██    ██      ██  ██  ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
███████    ██    ███████    ██    ███████ ██      ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

*/

	default JsonNode getTelemetryData(CIBUser user) {
		return getSystemProvider().getTelemetryData(user);
	}

	default Collection<Metric> getMetrics(Map<String, Object> queryParams, CIBUser user) {
		return getSystemProvider().getMetrics(queryParams, user);
	}

/*

██    ██  █████  ██████  ██  █████  ██████  ██      ███████     ██ ███    ██ ███████ ████████  █████  ███    ██  ██████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██    ██ ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ████   ██ ██         ██    ██   ██ ████   ██ ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
██    ██ ███████ ██████  ██ ███████ ██████  ██      █████       ██ ██ ██  ██ ███████    ██    ███████ ██ ██  ██ ██      █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
 ██  ██  ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ██  ██ ██      ██    ██    ██   ██ ██  ██ ██ ██      ██          ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
  ████   ██   ██ ██   ██ ██ ██   ██ ██████  ███████ ███████     ██ ██   ████ ███████    ██    ██   ██ ██   ████  ██████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

*/

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
	default VariableInstance getVariableInstance(String id, boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException {
		return getVariableInstanceProvider().getVariableInstance(id, deserializeValue, user);
	}

/*

██   ██ ██ ███████ ████████  ██████  ██████  ██  ██████     ██    ██  █████  ██████  ██  █████  ██████  ██      ███████     ██ ███    ██ ███████ ████████  █████  ███    ██  ██████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██   ██ ██ ██         ██    ██    ██ ██   ██ ██ ██          ██    ██ ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ████   ██ ██         ██    ██   ██ ████   ██ ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
███████ ██ ███████    ██    ██    ██ ██████  ██ ██          ██    ██ ███████ ██████  ██ ███████ ██████  ██      █████       ██ ██ ██  ██ ███████    ██    ███████ ██ ██  ██ ██      █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
██   ██ ██      ██    ██    ██    ██ ██   ██ ██ ██           ██  ██  ██   ██ ██   ██ ██ ██   ██ ██   ██ ██      ██          ██ ██  ██ ██      ██    ██    ██   ██ ██  ██ ██ ██      ██          ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
██   ██ ██ ███████    ██     ██████  ██   ██ ██  ██████       ████   ██   ██ ██   ██ ██ ██   ██ ██████  ███████ ███████     ██ ██   ████ ███████    ██    ██   ██ ██   ████  ██████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

 */

	/**
	 * Retrieves a historic variable instance by its ID.
	 * @param id The ID of the historic variable instance
	 * @param deserializeValue Whether to deserialize the variable value or not
	 * @param user the user performing the search
	 * @return Historic variable instance details
	 * @throws SystemException in case of an error
	 * @throws NoObjectFoundException when the historic variable instance could not be found
	 */
	default VariableHistory getHistoricVariableInstance(String id, boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException {
		return getHistoricVariableInstanceProvider().getHistoricVariableInstance(id, deserializeValue, user);
	}

/*

███████ ██   ██ ████████ ███████ ██████  ███    ██  █████  ██           ████████  █████  ███████ ██   ██     ██       ██████   ██████  
██       ██ ██     ██    ██      ██   ██ ████   ██ ██   ██ ██              ██    ██   ██ ██      ██  ██      ██      ██    ██ ██       
█████     ███      ██    █████   ██████  ██ ██  ██ ███████ ██              ██    ███████ ███████ █████       ██      ██    ██ ██   ███ 
██       ██ ██     ██    ██      ██   ██ ██  ██ ██ ██   ██ ██              ██    ██   ██      ██ ██  ██      ██      ██    ██ ██    ██ 
███████ ██   ██    ██    ███████ ██   ██ ██   ████ ██   ██ ███████         ██    ██   ██ ███████ ██   ██     ███████  ██████   ██████  

*/

	/**
	 * Get external tasks based on query parameters
	 *
	 * @param queryParams Query parameters for filtering external tasks
	 * @param user the user performing the operation
	 * @return Collection of external tasks
	 * @throws SystemException in case of an error
	 */
	default Collection<ExternalTask> getExternalTasks(Map<String, Object> queryParams, CIBUser user) throws SystemException {
		return getExternalTaskProvider().getExternalTasks(queryParams, user);
	}

	/*

	██████  ██████   ██████   ██████ ███████ ███████ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██   ██ ██   ██ ██    ██ ██      ██      ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██████  ██████  ██    ██ ██      █████   ███████ ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██      ██   ██ ██    ██ ██      ██           ██      ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██      ██   ██  ██████   ██████ ███████ ███████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

	 */

	/**
	 * Search processes.
     * @param user the user performing the search
     * @return Fetched processes.
     * @throws InvalidAttributeValueException when searching for processes with at least one invalid parameter value.
     * @throws SystemException in case of any other error.
     */
	default Collection<Process> findProcesses(CIBUser user) throws SystemException {
		return getProcessProvider().findProcesses(user);
	}

	/**
	 * Search processes with number of process instances and incidents.
     * @param user the user performing the search
     * @return Fetched processes.
     * @throws InvalidAttributeValueException when searching for processes with at least one invalid parameter value.
     * @throws SystemException in case of any other error.
     */
	default Collection<Process> findProcessesWithInfo(CIBUser user) throws SystemException {
		return getProcessProvider().findProcessesWithInfo(user);
	}

	/**
	 * Search processes.
	 * @param filters filters to be applied.
	 * @param user the user performing the search
     * @return Fetched processes.
     * @throws InvalidAttributeValueException when searching for processes with at least one invalid parameter value.
     * @throws SystemException in case of any other error.
     */
	default Collection<Process> findProcessesWithFilters(String filters, CIBUser user) throws SystemException {
		return getProcessProvider().findProcessesWithFilters(filters, user);
	}

	/**
     * Search process with a specific Key.
     * @param processKey filter by process definition key.
	 * @param tenantId 
     * @param user since this call is secured we need the user to authenticate.
     * @return Fetched process.
     * @throws SystemException in case of an error.
     */
	default Process findProcessByDefinitionKey(String key, String tenantId, CIBUser user) throws SystemException {
		return getProcessProvider().findProcessByDefinitionKey(key, tenantId, user);
	}

	/**
   * Search processes (diferents versions) with a specific Key.
   * @param processKey filter by process definition key.
 * @param tenantId 
   * @param lazyLoad parameter to decide if load all the data or the minimum necessary.
   * @param user since this call is secured we need the user to authenticate.
   * @return Fetched process.
   * @throws SystemException in case of an error.
   */
	default Collection<Process> findProcessVersionsByDefinitionKey(String key, String tenantId, Optional<Boolean> lazyLoad, CIBUser user) throws SystemException {
		return getProcessProvider().findProcessVersionsByDefinitionKey(key, tenantId, lazyLoad, user);
	}

	/**
   * Search process with a specific Id.
   * @param id filter by process definition id.
   * @param extraInfo parameter to specify if more data will be loaded.
   * @param user the user performing the query.
   * @return Fetched process.
   * @throws SystemException in case of an error.
   */
	default Process findProcessById(String id, Optional<Boolean> extraInfo, CIBUser user) throws SystemException {
		return getProcessProvider().findProcessById(id, extraInfo, user);
	}

	/**
	 * Search processes instances with a specific process key.
	 * @param key the process key to filter by.
	 * @param user the user performing the search
	   * @return Fetched processes instances.
	   * @throws SystemException in case of an error.
	   */
	default Collection<ProcessInstance> findProcessesInstances(String key, CIBUser user) throws SystemException {
		return getProcessProvider().findProcessesInstances(key, user);
	}

	/**
	 * Fetch process diagram, a xml that contains the specification to render the diagram.
	 * @param processDefinitionId filter by process definition id.
	 * @param user the user performing the search
	 * @return process diagram xml that contains diagram to be render.
     * @throws NoObjectFoundException when the process definition searched for could not be found.
     * @throws SystemException in case of any other error.
	 */
	default ProcessDiagram fetchDiagram(String processDefinitionId, CIBUser user) throws SystemException {
		return getProcessProvider().fetchDiagram(processDefinitionId, user);
	}

	/**
	 * Fetch start-form to start a process
	 * @param processDefinitionId of the process to be started.
	 * @param user the user performing the search
	 * @return Startform variables and formReference.
     * @throws NoObjectFoundException when trying to find start form data of a non-existing process definition.
     * @throws SystemException in case of any other error.
	 */
	default StartForm fetchStartForm(String processDefinitionId, CIBUser user) throws SystemException {
		return getProcessProvider().fetchStartForm(processDefinitionId, user);
	}

	/**
	 * Download bpmn from a process definition id.
	 * @param processDefinitionId filter by process definition id.
	 * @param fileName name of the file content the bpmn.
	 * @param user the user performing the download
	 * @return Fetched bpmn
     * @throws SystemException in case of an error.
	 */
	default Data downloadBpmn(String processDefinitionId, String fileName, CIBUser user) throws SystemException {
		return getProcessProvider().downloadBpmn(processDefinitionId, fileName, user);
	}

	/**
   * Activate/Suspend process instance by ID.
   * @param processInstanceId instance id to be suspended or activated.
   * @param suspend if true, the process instance will be activated if false process will be suspended.
   * @param user the user performing the operation.
   * @throws SystemException in case of other error.
   */
	default void suspendProcessInstance(String processInstanceId, Boolean suspend, CIBUser user) throws SystemException {
		getProcessProvider().suspendProcessInstance(processInstanceId, suspend, user);
	}

	/**
   * Delete process instance by ID.
   * @param processInstanceId instance id to be deleted.
   * @param user the user performing the deletion.
   * @throws NoObjectFoundException when the filter to be changed could not be found.
   * @throws SystemException in case of any other error.
   */
	default void deleteProcessInstance(String processInstanceId, CIBUser user) throws SystemException {
		getProcessProvider().deleteProcessInstance(processInstanceId, user);
	}

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
	default void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user) throws SystemException {
		getProcessProvider().suspendProcessDefinition(processDefinitionId, suspend, includeProcessInstances, executionDate, user);
	}

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
	default ProcessStart startProcess(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		return getProcessProvider().startProcess(processDefinitionKey, tenantId, data, user);
	}

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
	default ProcessStart submitForm(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		return getProcessProvider().submitForm(processDefinitionKey, tenantId, data, user);
	}

	/**
	 * Search statistics from a process.
	 * @param processId filter by process id.
	 * @param user the user performing the search
	 * @return Fetched processes instances.
	 * @throws SystemException in case of an error.
	 */
	default Collection<ProcessStatistics> findProcessStatistics(String processId, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		return getProcessProvider().findProcessStatistics(processId, user);
	}

  /**
   * Search statistics for all processes.
   * @param queryParams query parameters to filter the search
   * @param user the user performing the search
   * @return Fetched processes instances.
   * @throws SystemException in case of an error.
   */
	default Collection<ProcessStatistics> getProcessStatistics(Map<String, Object> queryParams, CIBUser user) throws SystemException {
		return getProcessProvider().getProcessStatistics(queryParams, user);
	}

	/**
	 * Queries for historic process instances that fulfill the given parameters.
	 * @param filters is a map of parameters to filter query. Parameters firstResult and maxResults are used for pagination.
	 * @param user the user performing the query.
	   * @return Fetched processes instances.
	   * @throws SystemException in case of an error.
	   */
	default Collection<HistoryProcessInstance> findProcessesInstancesHistory(Map<String, Object> filters,
			Optional<Integer> firstResult, Optional<Integer> maxResults, CIBUser user) throws SystemException {
		return getProcessProvider().findProcessesInstancesHistory(filters, firstResult, maxResults, user);
	}

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
	default Collection<HistoryProcessInstance> findProcessesInstancesHistory(String key, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, CIBUser user) throws SystemException {
		return getProcessProvider().findProcessesInstancesHistory(key, active, firstResult, maxResults, user);
	}

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
	default Collection<HistoryProcessInstance> findProcessesInstancesHistoryById(String id, Optional<String> activityId, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, String text, CIBUser user) throws SystemException {
		return getProcessProvider().findProcessesInstancesHistoryById(id, activityId, active, firstResult, maxResults, text, user);
	}

	default Long countProcessesInstancesHistory(Map<String, Object> filters, CIBUser user) {
		return getProcessProvider().countProcessesInstancesHistory(filters, user);
	}

	/**
	 * Required by OFDKA
	 * Search process instance with a specific process instance id.
	 * @param processInstanceId filter by process instance id.
	 * @param user the user performing the search.
	 * @return Fetched process instance.
	 * @throws NoObjectFoundException when the process instance searched for could not be found.
	 * @throws SystemException in case of any other error.
	 */
	default ProcessInstance findProcessInstance(String processInstanceId, CIBUser user) throws SystemException {
		return getProcessProvider().findProcessInstance(processInstanceId, user);
	}

	/**
	 * Required by OFDKA
	 * Retrieves a variable of a given process instance by id.
	 * @param processInstanceId filter by process instance id.
	 * @param variableName variable name.
	 * @param deserializeValue whether to deserialize the variable value. Default: true.
	 * @param user the user performing the search.
	 * @return Fetched variables.
	 * @throws SystemException in case of an error.
	 */
	default Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, boolean deserializeValue, CIBUser user) throws SystemException  {
		return getProcessProvider().fetchProcessInstanceVariable(processInstanceId, variableName, deserializeValue, user);
	}

	/**
   * Search process instance with a specific process instance id.
   * @param processInstanceId filter by process instance id.
   * @param user the user performing the search
   * @return Fetched process instance.
   * @throws NoObjectFoundException when the process instance searched for could not be found.
   * @throws SystemException in case of any other error.
   */
	default HistoryProcessInstance findHistoryProcessInstanceHistory(String processInstanceId, CIBUser user) throws SystemException {
		return getProcessProvider().findHistoryProcessInstanceHistory(processInstanceId, user);
	}

	default Collection<Process> findCalledProcessDefinitions(String processDefinitionId, CIBUser user) throws SystemException {
		return getProcessProvider().findCalledProcessDefinitions(processDefinitionId, user);
	}

	default ResponseEntity<byte[]> getDeployedStartForm(String processDefinitionId, CIBUser user) throws SystemException {
		return getProcessProvider().getDeployedStartForm(processDefinitionId, user);
	}

	default void updateHistoryTimeToLive(String id, Map<String, Object> data, CIBUser user) throws SystemException {
		getProcessProvider().updateHistoryTimeToLive(id, data, user);
	}

	default void deleteProcessInstanceFromHistory(String id, CIBUser user) throws SystemException {
		getProcessProvider().deleteProcessInstanceFromHistory(id, user);
	}

	default void deleteProcessDefinition(String id, Optional<Boolean> cascade, CIBUser user) throws SystemException {
		getProcessProvider().deleteProcessDefinition(id, cascade, user);
	}

	/**
	 * Search processes instances by filter.
	 * @param data a map of parameters to filter the query.
	 * @param user the user performing the query.
     * @return Fetched processes instances.
     * @throws SystemException in case of an error.
     */
	default Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user)
			throws SystemException {
		return getProcessProvider().findCurrentProcessesInstances(data, user);
	}

	/**
	 * Fetch historic activity statistics for a given process definition ID.
	 *
	 * @param id the ID of the process definition
	 * @param params query parameters to filter statistics (e.g., canceled, finished, incidents)
	 * @param user the user performing the operation
	 * @return a list or map containing the historic activity statistics
	 * @throws SystemException in case of an error
	 */
	default Object fetchHistoricActivityStatistics(String id, Map<String, Object> params, CIBUser user) throws SystemException {
		return getProcessProvider().fetchHistoricActivityStatistics(id, params, user);
	}

	/**
     * Fetch incidents for an specific process.
	 * @param processDefinitionKey of the process to fetch incidents.
	 * @param user the user performing the search.
	 * @throws UnsupportedTypeException when a process instance cannot be created because of an unsupported value type or an invalid expression used in the process definition.
     * @throws SystemException in case of any other error.
     */
		default Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user) throws SystemException {
			return getIncidentProvider().fetchIncidents(processDefinitionKey, user);
		}

	 /*

	 ██    ██ ████████ ██ ██      ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	 ██    ██    ██    ██ ██      ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	 ██    ██    ██    ██ ██      ███████     ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	 ██    ██    ██    ██ ██           ██     ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	  ██████     ██    ██ ███████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

	  */

		/**
		 * Correlates a message to the process engine to either trigger a message start event or an intermediate message catching event.
		 * @param data variables to start process.
		 * @param user the user performing the correlation.
		 * @return Collection of correlated messages.
	     * @throws SystemException in case of any other error.
		 */
	 default Collection<Message> correlateMessage(Map<String, Object> data, CIBUser user) throws SystemException {
	 	return getUtilsProvider().correlateMessage(data, user);
	 }

	/**
	 * Add user to a group.
	 *
	 * @param groupId the ID of the group.
	 * @param userId the ID of the user to be added.
	 * @param user the user performing the operation.
	 */
	default void addMemberToGroup(String groupId, String userId, CIBUser user) throws SystemException {
		getUserProvider().addMemberToGroup(groupId, userId, user);
	}

	/**
	 * Delete user from a group.
	 *
	 * @param groupId the ID of the group.
	 * @param userId the ID of the user to be removed.
	 * @param user the user performing the operation.
	 */
	default void deleteMemberFromGroup(String groupId, String userId, CIBUser user) throws SystemException {
		getUserProvider().deleteMemberFromGroup(groupId, userId, user);
	}

	/**
	 * Deletes a user by id.
	 *
	 * @param userId the ID of the user to be deleted.
	 * @param user the user performing the deletion.
	 */
	default void deleteUser(String userId, CIBUser user) throws SystemException {
		getUserProvider().deleteUser(userId, user);
	}

	/**
	 * Get user by id.
	 *
	 * @param userId the ID of the user to be fetched.
	 * @param user the user performing the search.
	 * @return SevenUser object containing user profile information.
	 */
	default SevenUser getUserProfile(String userId, CIBUser user) throws SystemException {
		return getUserProvider().getUserProfile(userId, user);
	}

	default void retryJobById(String jobId, Map<String, Object> data, CIBUser user) throws SystemException {
		getUtilsProvider().retryJobById(jobId, data, user);
	}

	default String findStacktrace(String jobId, CIBUser user) {
		return getUtilsProvider().findStacktrace(jobId, user);
	}

	/*

	██ ███    ██  ██████ ██ ██████  ███████ ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	██ ████   ██ ██      ██ ██   ██ ██      ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	██ ██ ██  ██ ██      ██ ██   ██ █████   ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	██ ██  ██ ██ ██      ██ ██   ██ ██      ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	██ ██   ████  ██████ ██ ██████  ███████ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 

*/

	default Long countIncident(Map<String, Object> params, CIBUser user) throws SystemException {
		return getIncidentProvider().countIncident(params, user);
	}

	default Long countHistoricIncident(Map<String, Object> params, CIBUser user) throws SystemException {
		return getIncidentProvider().countHistoricIncident(params, user);
	}

	default Collection<Incident> findIncident(Map<String, Object> params, CIBUser user) throws SystemException {
		return getIncidentProvider().findIncident(params, user);
	}

	default List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user) throws SystemException {
		return getIncidentProvider().findIncidentByInstanceId(processInstanceId, user);
	}

	default Collection<Incident> fetchIncidentsByInstanceAndActivityId(String processDefinitionKey, String activityId, CIBUser user) throws SystemException {
		return getIncidentProvider().fetchIncidentsByInstanceAndActivityId(processDefinitionKey, activityId, user);
	}

	default void setIncidentAnnotation(String incidentId, Map<String, Object> data, CIBUser user) {
		getIncidentProvider().setIncidentAnnotation(incidentId, data, user);
	}

	default String findExternalTaskErrorDetails(String externalTaskId, CIBUser user) throws SystemException {
		return getIncidentProvider().findExternalTaskErrorDetails(externalTaskId, user);
	}

	default String findHistoricExternalTaskErrorDetails(String externalTaskId, CIBUser user) throws SystemException {
		return getIncidentProvider().findHistoricExternalTaskErrorDetails(externalTaskId, user);
	}

	default void retryExternalTask(String externalTaskId, Map<String, Object> data, CIBUser user) {
		getIncidentProvider().retryExternalTask(externalTaskId, data, user);
	}

	default Collection<Incident> findHistoricIncidents(Map<String, Object> params, CIBUser user) throws SystemException {
		return getIncidentProvider().findHistoricIncidents(params, user);
	}

	default String findHistoricStacktraceByJobId(String jobId, CIBUser user) throws SystemException {
		return getIncidentProvider().findHistoricStacktraceByJobId(jobId, user);
	}

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
	default Collection<EventSubscription> getEventSubscriptions(Optional<String> processInstanceId,
			Optional<String> eventType, Optional<String> eventName, CIBUser user) throws SystemException {
		return getUtilsProvider().getEventSubscriptions(processInstanceId, eventType, eventName, user);
	}

	/*

	████████ ███████ ███    ██  █████  ███    ██ ████████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	   ██    ██      ████   ██ ██   ██ ████   ██    ██        ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	   ██    █████   ██ ██  ██ ███████ ██ ██  ██    ██        ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	   ██    ██      ██  ██ ██ ██   ██ ██  ██ ██    ██        ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	   ██    ███████ ██   ████ ██   ██ ██   ████    ██        ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	*/

	default Collection<Tenant> fetchTenants(Map<String, Object> queryParams, CIBUser user) throws SystemException {
		return getTenantProvider().fetchTenants(queryParams, user);
	}

	default Tenant fetchTenant(String tenantId, CIBUser user) throws SystemException {
		return getTenantProvider().fetchTenant(tenantId, user);
	}

	default void createTenant(Tenant tenant, CIBUser user) throws SystemException {
		getTenantProvider().createTenant(tenant, user);
	}

	default void updateTenant(Tenant tenant, CIBUser user) throws SystemException {
		getTenantProvider().updateTenant(tenant, user);
	}

	default void deleteTenant(String tenantId, CIBUser user) throws SystemException {
		getTenantProvider().deleteTenant(tenantId, user);
	}

	default void addMemberToTenant(String tenantId, String userId, CIBUser user) throws SystemException {
		getTenantProvider().addMemberToTenant(tenantId, userId, user);
	}

	default void deleteMemberFromTenant(String tenantId, String userId, CIBUser user) throws SystemException {
		getTenantProvider().deleteMemberFromTenant(tenantId, userId, user);
	}

	default void addGroupToTenant(String tenantId, String groupId, CIBUser user) throws SystemException {
		getTenantProvider().addGroupToTenant(tenantId, groupId, user);
	}

	default void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user) throws SystemException {
		getTenantProvider().deleteGroupFromTenant(tenantId, groupId, user);
	}

/*

███████ ███    ██  ██████  ██ ███    ██ ███████     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
██      ████   ██ ██       ██ ████   ██ ██          ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
█████   ██ ██  ██ ██   ███ ██ ██ ██  ██ █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
██      ██  ██ ██ ██    ██ ██ ██  ██ ██ ██          ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
███████ ██   ████  ██████  ██ ██   ████ ███████     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
 */

	/**
	 * Get the names of all process engines available on the engine.
	 *
	 * @return a collection of engine objects containing name information
	 * @throws SystemException in case of an error
	 */

	default Collection<Engine> getProcessEngineNames() throws SystemException {
		return getEngineProvider().getProcessEngineNames();
	}

	/**
	 * Determine whether an initial user needs to be created
	 *
	 * @return true if admin group is available and write access is set
	 * @throws SystemException in case of an error
	 */
	default Boolean requiresSetup(String engine) {
		return getEngineProvider().requiresSetup(engine);
	}

	/**
	 * Creates a new initial user assigned to the also created admin group.
	 *
	 * @param user the new user to be created.
	 * @throws InvalidUserIdException when the user ID is invalid.
	 */
	default void createSetupUser(NewUser user, String engine) throws InvalidUserIdException {
		getEngineProvider().createSetupUser(user, engine);
	}

}
