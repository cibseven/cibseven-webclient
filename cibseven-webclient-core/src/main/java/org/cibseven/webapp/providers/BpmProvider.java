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
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.rest.model.DeploymentResource;
import org.cibseven.webapp.rest.model.EventSubscription;
import org.cibseven.webapp.rest.model.Filter;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Incident;
import org.cibseven.webapp.rest.model.JobDefinition;
import org.cibseven.webapp.rest.model.Job;
import org.cibseven.webapp.rest.model.Message;
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
import org.cibseven.webapp.rest.model.TaskCount;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskHistory;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

public interface BpmProvider {
	
	/**
     * Search tasks, which contains specified filter.
     * @param filter applied in the search
     * @return Collection tasks fetched in the search.
     * @throws SystemException in case of an error.
     */
	Collection<Task> findTasks(String filter, CIBUser user) throws SystemException;
	
	/**
     * Search tasks which belongs to a specific process instance.
     * @param processInstanceId filter by process instance id.
     * @return Fetched tasks.
     * @throws SystemException in case of an error.
     */	
	Collection<Task> findTasksByProcessInstance(String processInstanceId, CIBUser user) throws SystemException;
	
	/**
     * Search tasks which belongs to a specific process instance and a user.
     * @param processInstanceId filter by process instance id.
     * @param createdAfter filter by creation date.
     * @return Fetched tasks.
     * @throws SystemException in case of an error.
     */	
	Collection<Task> findTasksByProcessInstanceAsignee(Optional<String> processInstanceId, Optional<String> createdAfter, CIBUser user) throws SystemException;
	
	/**
	 * Search tasks which belongs to a specific process instance.
	 * The tasks found belongs to the history, they have other attributes and finished tasks
	 * are also fetched.
	 * @param processInstanceId filter by process instance id.
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
	 * @return Fetched tasks.
     * @throws SystemException in case of an error.
	 */
	Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId, CIBUser user) throws SystemException;
	
	/**
     * Search task with a specific Id.
     * @param taskId filter by task id.
     * @return Fetched task.
     * @throws NoObjectFoundException when the task searched for could not be found.
     * @throws SystemException in case of any other error.
     */	
	Task findTaskById(String taskId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
     * Search activity that belong to a process instance.
     * @param processInstanceId filter by process instance id.
     * @return Fetched activity.
     * @throws NoObjectFoundException when the searched process instance could not be found.
     * @throws SystemException in case of any other error.
     */
	ActivityInstance findActivityInstance(String processInstanceId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
     * Search activities instances that belong to a process instance. The activities found belongs
     * to the history, they have other attributes and activities from finished processes are also fetched.
     * @param processInstanceId filter by process instance id.
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
     * @return Fetched variables.
     * @throws SystemException in case of an error.
     */
	Collection<VariableHistory> fetchActivityVariablesHistory(String activityInstanceId, CIBUser user) throws SystemException;
	
	/**
	 * Fetch variables from a specific activity.
     * @param activityInstanceId filter by activity instance id.
     * @return Fetched variables.
     * @throws SystemException in case of an error.
     */
	Collection<VariableHistory> fetchActivityVariables(String activityInstanceId, CIBUser user) throws SystemException;
	
	/**
     * Update task.
     * @param task to be updated with the desired values already modified.
     * @throws SystemException in case of an error.
     */
	void update(Task task, CIBUser user) throws SystemException;
	
	/**
     * Set assignee to an specific task.
     * @param taskId filter by task id.
     * @param assignee to be set as assignee.
     * @throws SystemException in case of an error.
     */	
	void setAssignee(String taskId, String assignee, CIBUser user) throws SystemException;

	/**
	 * Submit task without saving any variables, because that is done by the ui-element-template (in ours).
     * @throws SubmitDeniedException when trying to submit a non-existing task.
     * @throws SystemException in case of any other error.
	 */
	void submit(String taskId, CIBUser user) throws SystemException, SubmitDeniedException;
	
	/**
	 * Fetch form-reference variable from task.
	 * @param taskId filter by task id.
	 * @return form-reference
     * @throws NoObjectFoundException when the searched task could not be found.
     * @throws SystemException in case of any other error.
	 */
	Object formReference(String taskId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
	 * Search processes.
     * @return Fetched processes.
     * @throws InvalidAttributeValueException when searching for processes with at least one invalid parameter value.
     * @throws SystemException in case of any other error.
     */
	Collection<Process> findProcesses(CIBUser user) throws SystemException, InvalidAttributeValueException;

	/**
	 * Search processes with number of process instances and incidents.
     * @return Fetched processes.
     * @throws InvalidAttributeValueException when searching for processes with at least one invalid parameter value.
     * @throws SystemException in case of any other error.
     */
	Collection<Process> findProcessesWithInfo(CIBUser user) throws SystemException, InvalidAttributeValueException;
	
	/**
	 * Search processes.
	 * @param filters filters to be applied.
     * @return Fetched processes.
     * @throws InvalidAttributeValueException when searching for processes with at least one invalid parameter value.
     * @throws SystemException in case of any other error.
     */
	Collection<Process> findProcessesWithFilters(String filters, CIBUser user) throws SystemException, InvalidAttributeValueException;
	
	/**
     * Search process with a specific Key.
     * @param processKey filter by process definition key.
     * @param user since this call is secured we need the user to authenticate.
     * @return Fetched process.
     * @throws SystemException in case of an error.
     */	
	Process findProcessByDefinitionKey(String processKey, CIBUser user) throws SystemException;
	
	/**
     * Search processes (diferents versions) with a specific Key.
     * @param processKey filter by process definition key.
     * @param lazyLoad parameter to decide if load all the data or the minimum necessary.
     * @param user since this call is secured we need the user to authenticate.
     * @return Fetched process.
     * @throws SystemException in case of an error.
     */	
	Collection<Process> findProcessVersionsByDefinitionKey(String processKey, Optional<Boolean> lazyLoad, CIBUser user) throws SystemException;
	
	/**
     * Search process with a specific Id.
     * @param processId filter by process definition id.
     * @param extraInfo parameter to specify if more data will be load.
     * @param user since this call is secured we need the user to authenticate.
     * @return Fetched process.
     * @throws SystemException in case of an error.
     */	
	Process findProcessById(String id, Optional<Boolean> extraInfo, CIBUser user) throws SystemException;
	
	/**
	 * Search processes instances with a specific process key (in the history).
	 * @param active true means that unfinished processes will be fetched 
	 * and false, only finished processes will be fetched. Parameters firstResult and maxResults are used for pagination.
     * @return Fetched processes instances.
     * @throws SystemException in case of an error.
     */
	Collection<HistoryProcessInstance> findProcessesInstancesHistory(String key, Optional<Boolean> active, Integer firstResult, Integer maxResults, CIBUser user) throws SystemException;
	
	/**
	 * Search processes instances with a specific process key.
	 * @param active true means that unfinished processes will be fetched 
	 * and false, only finished processes will be fetched.
     * @return Fetched processes instances.
     * @throws SystemException in case of an error.
     */
	Collection<ProcessInstance> findProcessesInstances(String key, CIBUser user) throws SystemException;
	
	/**
	 * Search statistics from a process.
	 * @param id filter by process id.
	 * @return Fetched processes instances.
	 * @throws SystemException in case of an error.
	 */
	Collection<ProcessStatistics> findProcessStatistics(String id, CIBUser user) throws SystemException;

	/**
	 * Search processes instances by filter.
	 * @param filter
     * @return Fetched processes instances.
     * @throws SystemException in case of an error.
     */
	Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user) throws SystemException;
	
	/**
     * Search process instance with a specific process instance id.
     * @param processInstanceId filter by process instance id.
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
	 * @return process diagram xml that contains diagram to be render.
     * @throws NoObjectFoundException when the process definition searched for could not be found.
     * @throws SystemException in case of any other error.
	 */
	ProcessDiagram fetchDiagram(String processDefinitionId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
	 * Fetch variables from a specific process instance.
	 * The variables found belongs to the history, they have other attributes 
	 * and variables from finished process instances are also fetched.
     * @param processInstanceId filter by process instance id.
	 * @param deserialize 
     * @return Fetched variables.
     * @throws SystemException in case of an error.
     */
	Collection<VariableHistory> fetchProcessInstanceVariablesHistory(String processInstanceId, CIBUser user, Optional<Boolean> deserializeValue) 
			throws SystemException;

	/**
	 * Fetch start-form to start a process
	 * @param processDefinitionId of the process to be started.
	 * @return Startform variables and formReference.
     * @throws NoObjectFoundException when trying to find start form data of a non-existing process definition.
     * @throws SystemException in case of any other error.
	 */
	StartForm fetchStartForm(String processDefinitionId, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
	 * Download bpmn from a process definition id.
	 * @param processDefinitionId filter by process definition id.
	 * @param fileName name of the file content the bpmn.
	 * @return Fetched bpmn
     * @throws SystemException in case of an error.
	 */
	Data downloadBpmn(String processDefinitionId, String fileName, CIBUser user) throws SystemException;

	/**
	 * Get authorizations, filtered by userId and groups in which user belongs.
	 * @param userId filter user identification (username).
	 * @return Fetched bpmn
     * @throws SystemException in case of an error.
	 */	
	Authorizations getUserAuthorization(String userId, CIBUser user) throws SystemException;
	
	/**
     * Search filters
     * @param filter applied in the search
     * @return Collection Filters fetched in the search.
     * @throws SystemException in case of an error.
     */
	Collection<Filter> findFilters(CIBUser user) throws SystemException;
	
	/**
     * Create filter
     * @param filter to be created
     * @throws SystemException in case of an error.
     */
	Filter createFilter(Filter filter, CIBUser user) throws SystemException;
	
	/**
     * Update filter
     * @param filter to be updated
     * @throws NoObjectFoundException when the filter to be changed could not be found.
     * @throws SystemException in case of any other error.
     */
	void updateFilter(Filter filter, CIBUser user) throws SystemException, NoObjectFoundException;
	
	/**
     * Delete filter
     * @param filter to be deleted
     * @throws SystemException in case of an error.
     */
	void deleteFilter(String filterId, CIBUser user) throws SystemException;
	
	/**
     * Activate/Suspend process instance by ID.
     * @param processInstanceId instance id to be suspended or activated.
     * @param suspend if true, the process instance will be activated if false process will be suspended.
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
     * @throws SystemException in case of other error.
	 * @throws UnsupportedTypeException when a process instance cannot be created because of an unsupported value type or an invalid expression used in the process definition.
	 * @throws NoObjectFoundException when the filter to be changed could not be found.
     */
	void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user) throws SystemException, UnsupportedTypeException, NoObjectFoundException;
	
	/**
     * Delete process instance by ID.
     * @param processInstanceId instance id to be deleted.
     * @throws NoObjectFoundException when the filter to be changed could not be found.
     * @throws SystemException in case of any other error.
     */
	void deleteProcessInstance(String processInstanceId, CIBUser user) throws SystemException, NoObjectFoundException;

	/**
     * Fetch incidents for an specific process.
	 * @param processDefinitionKey of the process to fetch incidents.
	 * @throws UnsupportedTypeException when a process instance cannot be created because of an unsupported value type or an invalid expression used in the process definition.
     * @throws SystemException in case of any other error.
     */
	 Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user) throws SystemException, UnsupportedTypeException;

	/**
     * Deploy process-bpmn
	 * @param data metadata of the diagram to be deployed (deployment-name, deployment-source, deploy-changed-only).
	 * @param file of the diagram to be deployed.
	 * @return 
     * @throws SystemException in case of any other error.
     */
	 Deployment deployBpmn(MultiValueMap<String, Object> data, MultiValueMap<String, MultipartFile> file, CIBUser user) throws SystemException;
	
	/**
	 * Start process
	 * @param user who start the process.
	 * @param processDefinitionKey of the process to be started.
	 * @param data variables to start process.
	 * @return information about the process started.
     * @throws UnsupportedTypeException when a process instance cannot be created because of an unsupported value type or an invalid expression used in the process definition.
     * @throws ExpressionEvaluationException when .
     * @throws SystemException in case of any other error.
	 */
	 ProcessStart startProcess(String processDefinitionKey, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;

	/**
	 * Correlates a message to the process engine to either trigger a message start event or an intermediate message catching event
	 * @param user who start the process.
	 * @param data variables to start process.
	 * @return 
     * @throws SystemException in case of any other error.
	 */
	 Collection<Message> correlateMessage(Map<String, Object> data, CIBUser user) throws SystemException;
	 
	/**
	 * Submit form with variables
	 * @param user who start the process.
	 * @param processDefinitionKey of the process to be started.
	 * @param data variables to submit.
	 * @return information about the process started.
     * @throws UnsupportedTypeException when a process instance cannot be created because of an unsupported value type or an invalid expression used in the process definition.
     * @throws ExpressionEvaluationException when .
     * @throws SystemException in case of any other error.
	 */
	 ProcessStart submitForm(String processDefinitionKey, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;

	/**
	 * Modify a variable in the Process Instance.
	 * @param executionId Id of the execution.
	 * @param data to be updated.
	 * @param user User who is modifing the variable.
     * @throws SystemException in case of any other error.
	 */
	 void modifyVariableByExecutionId(String executionId, Map<String, Object> data, CIBUser user) throws SystemException; 

	/**
	 * Modify a variable data in the Process Instance.
	 * @param executionId Id of the execution.
	 * @param variableName Name of the variable.
	 * @param data to be updated 
	 * @param user User who is modifing the variable.
	 * @return Variable modified.
     * @throws SystemException in case of any other error.
	 */ 
	 void modifyVariableDataByExecutionId(String executionId, String variableName, MultipartFile file, CIBUser user) throws SystemException;
	
	 /**
	 * Fetch a variables from a process instance.
	 * @param processInstanceId Id of the instance.
	 * @param user User who is fetching the variables.
	 * @param deserializeValue 
	 * @return Data.
     * @throws SystemException in case of any other error.
	 */ 
	 Collection<Variable> fetchProcessInstanceVariables(String processInstanceId, CIBUser user, Optional<Boolean> deserializeValue) 
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
	 * @param user User who is modifing the variable.
	 * @return Data.
     * @throws SystemException in case of any other error.
	 */ 
	 ResponseEntity<byte[]> fetchHistoryVariableDataById(String id, CIBUser user) throws NoObjectFoundException, SystemException;
	 
	/**
	 * Retrieves all deployments of a given deployment.
	 * @param user who start the process.
	 * @return Fetched deployments
     * @throws SystemException in case of any other error.
	 */
	Collection<Deployment> findDeployments(CIBUser user) throws SystemException;

	/**
	 * Retrieves all deployment resources of a given deployment.
	 * @param user who start the process.
	 * @param deploymentId
	 * @return Fetched deployment resources 
     * @throws SystemException in case of any other error.
	 */
	Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user) throws SystemException;

	/**
	 * Retrieves the binary content of a deployment resource for the given deployment by id.
	 * @return resource data
     * @throws SystemException in case of any other error.
	 */
	Data fetchDataFromDeploymentResource(HttpServletRequest rq, String deploymentId, String resourceId, String fileName) throws SystemException;

	/**
	 * Delete deployment by an Id.
	 * @param deploymentId
	 * @param user who start the process.
	 * @return Fetched deployment resources 
     * @throws SystemException in case of any other error.
	 */
	void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException;

	/**
	 *  Identity links, e.g. to get the candidates user or groups of a task
	 *  
	 * @param taskId
	 * @param type   Filter by the type of links to include. e.g. "candidate"
	 * @param user
	 * @return
	 */
	Collection<IdentityLink> findIdentityLink(String taskId, Optional<String> type, CIBUser user);
	
	/**
	 *  Create identity links, e.g. to set the candidates user or groups of a task
	 *  
	 * @param taskId
	 * @param data variables to set type of the identity link and group or user id.
	 * @param user
	 * @return
	 */
	void createIdentityLink(String taskId, Map<String, Object> type, CIBUser user);
	
	/**
	 *  Delete identity links, e.g. to remove the candidates user or groups of a task
	 *  
	 * @param taskId
	 * @param data variables to remove the identity link.
	 * @param user
	 * @return
	 */
	void deleteIdentityLink(String taskId, Map<String, Object> type, CIBUser user);
	
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
	 * @return
	 */
	Collection<User> findUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike, Optional<String> lastName,
			Optional<String> lastNameLike, Optional<String> email, Optional<String> emailLike, Optional<String> memberOfGroup, Optional<String> memberOfTenant, 
			Optional<String> idIn, Optional<String> firstResult, Optional<String> maxResult, 
			Optional<String> sortBy, Optional<String> sortOrder, CIBUser user);

	/**
	 * Create a new user.
	 * 
	 * @param user
	 * @param flowUser
	 */
	void createUser(NewUser user, CIBUser flowUser) throws InvalidUserIdException;	
	
	/**
	 * Updates a user’s profile.
	 * 
	 * @param userId
	 * @param user the user to Update 
	 * @param flowUser
	 */
	void updateUserProfile(String userId, User user, CIBUser flowUser);	
	
	/**
	 * Add user to a group.
	 * 
	 * @param groupId
	 * @param userId
	 * @param user the user to Update 
	 * @param flowUser
	 */
	void addMemberToGroup(String groupId, String userId, CIBUser flowUser);
	
	/**
	 * Delete user from a group.
	 * 
	 * @param groupId
	 * @param userId
	 * @param user the user to Update 
	 * @param flowUser
	 */	
	void deleteMemberFromGroup(String groupId, String userId, CIBUser flowUser);

	/**
	 * Updates a user’s credentials (password).
	 * 
	 * @param userId
	 * @param data Request Body
	 * 		A JSON object with the following properties:
	 * 		Name 	Type 	Description
	 * 		password 	String 	The user's new password.
	 * 		authenticatedUserPassword 	String 	The password of the authenticated user who changes the password of the user (i.e., the user with passed id as path parameter).	 * 
	 * @param user
	 */
	void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user);

	
	/**
	 * Deletes a user by id.
	 * 
	 * @param userId
	 * @param user
	 */
	void deleteUser(String userId, CIBUser user);

	/**
	 * Get groups by id, ....
	 * 
	 * @param id
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
	 * @param user
	 * @return
	 */
	Collection<UserGroup> findGroups(Optional<String> id, Optional<String> name, Optional<String> nameLike, Optional<String> type, Optional<String> member,
			Optional<String> memberOfTenant, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult, Optional<String> maxResults,
			CIBUser user);

	/**
	 * Create a group
	 * 
	 * @param group
	 * @param user
	 */
	void createGroup(UserGroup group, CIBUser user);

	/**
	 * Updates a group.
	 * 
	 * @param groupId
	 * @param group
	 * @param user
	 */
	void updateGroup(String groupId, UserGroup group, CIBUser user);

	/**
	 * Deletes a group by id.
	 * 
	 * @param groupId
	 * @param user
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
	 * @param user
	 * @return
	 */
	Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type, Optional<String> userIdIn,Optional<String> groupIdIn, 
			Optional<String> resourceType, Optional<String> resourceId, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,Optional<String> maxResults, 
			CIBUser user);

	/**
	 * 	 create a group 
	 * 
	 * @param authorization
	 * @param user
	 */
	ResponseEntity<Authorization> createAuthorization(Authorization authorization, CIBUser user);

	/**
	 * Update a group by id.
	 * 
	 * @param authorizationId
	 * @param data
	 * @param user
	 */
	void updateAuthorization(String authorizationId, Map<String, Object> data, CIBUser user);

	/**
	 * Deletes a group by id.
	 * 
	 * @param authorizationId
	 * @param user
	 */
	void deleteAuthorization(String authorizationId, CIBUser user);

	/**
	 * Search processes instances with a specific process id (in the history).
	 * @param active true means that unfinished processes will be fetched 
	 * and false, only finished processes will be fetched. Parameters firstResult and maxResults are used for pagination.
	 * Parameter text and activityId are used for filtering.
     * @return Fetched processes instances.
     * @throws SystemException in case of an error.
     */
	Collection<HistoryProcessInstance> findProcessesInstancesHistoryById(String id, Optional<String> activityId, Optional<Boolean> active,
			Integer firstResult, Integer maxResults, String text, CIBUser user);
	
	/**
	 * Get user by id.
	 * 
	 * @param userId
	 * @param user
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

	void saveVariableInProcessInstanceId(String processInstanceId, List<Variable> variables, CIBUser user)
			throws SystemException;

	Map<String, Variable> fetchProcessFormVariablesById(String id, CIBUser user) throws SystemException;

	void retryJobById(String jobId, Map<String, Object> data, CIBUser user);
	
	/**
	 * Submit task with saving variables
     * @throws SubmitDeniedException when trying to submit a non-existing task.
     * @throws SystemException in case of any other error.
	 */
	void submit(Task task, List<Variable> formResult, CIBUser user) throws SystemException, SubmitDeniedException;

	Long countIncident(Optional<String> incidentId, Optional<String> incidentType, Optional<String> incidentMessage,
			Optional<String> processDefinitionId, Optional<String> processDefinitionKeyIn,
			Optional<String> processInstanceId, Optional<String> executionId, Optional<String> activityId,
			Optional<String> causeIncidentId, Optional<String> rootCauseIncidentId, Optional<String> configuration,
			Optional<String> tenantIdIn, Optional<String> jobDefinitionIdIn, Optional<String> name, CIBUser user);

	Collection<Incident> findIncident(Optional<String> incidentId, Optional<String> incidentType,
			Optional<String> incidentMessage, Optional<String> processDefinitionId,
			Optional<String> processDefinitionKeyIn, Optional<String> processInstanceId, Optional<String> executionId,
			Optional<String> activityId, Optional<String> causeIncidentId, Optional<String> rootCauseIncidentId,
			Optional<String> configuration, Optional<String> tenantIdIn, Optional<String> jobDefinitionIdIn,
			CIBUser user);

	List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user);

	String findStacktrace(String jobId, CIBUser user);

	/**
	 * Required by OFDKA
	 * Queries for tasks that fulfill a given filter. This method is slightly more powerful than the Get Tasks method because it allows
	 * filtering by multiple process or task variables of types String, Number or Boolean.
	 * @param data variables to apply search.
	 * @return Collection tasks fetched in the search.
	 * @throws SystemException in case of an error.
	 */
	Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException;

	/**
	 * Required by OFDKA
	 * Search process instance with a specific process instance id.
	 * @param processInstanceId filter by process instance id.
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
	 * @return Fetched variables.
	 */
	Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, String deserializeValue,
			CIBUser user) throws SystemException;

	
	/**
	 * Required by OFDKA
	 * Queries for event subscriptions that fulfill given parameters. 
	 * The size of the result set can be retrieved by using the Get Event Subscriptions count method.
	 * @return Collection event subscriptions fetched in the search.
	 */
	Collection<EventSubscription> getEventSubscriptions(Optional<String> processInstanceId, Optional<String> eventType, 
			Optional<String> eventName, CIBUser user);
	
	
	TaskCount findTasksCount(Optional<String> name, Optional<String> nameLike, Optional<String> taskDefinitionKey,
			Optional<String> taskDefinitionKeyIn, CIBUser user);
	
	/**
	 * Reports a business error in the context of a running task by id. The error code must be specified to identify the BPMN error handler.
	 * @param taskId filter by task id.
	 * @param data variables for the BPMN error reporting.
	 * @return 
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
			CIBUser user);
	
	Collection<JobDefinition> findJobDefinitions(String params, CIBUser user);
	void suspendJobDefinition(String jobDefinitionId, String params, CIBUser user);
	void overrideJobDefinitionPriority(String jobDefinitionId, String params, CIBUser user);
	JobDefinition findJobDefinition(String id, CIBUser user);
	
	Collection<Decision> getDecisionDefinitionList(Map<String, Object> queryParams);
	Object getDecisionDefinitionListCount(Map<String, Object> queryParams);
	Decision getDecisionDefinitionByKey(String key);
	Object getDiagramByKey(String key);
	Object evaluateDecisionDefinitionByKey(Map<String, Object> data, String key, CIBUser user);
	void updateHistoryTTLByKey(Map<String, Object> data, String key, CIBUser user);
	Decision getDecisionDefinitionByKeyAndTenant(String key, String tenant);
	Object getDiagramByKeyAndTenant(String key, String tenant);
	Object evaluateDecisionDefinitionByKeyAndTenant(String key, String tenant);
	Object updateHistoryTTLByKeyAndTenant(String key, String tenant);
	Object getXmlByKey(String key);
	Object getXmlByKeyAndTenant(String key, String tenant);
	Decision getDecisionDefinitionById(String id);
	Object getDiagramById(String id);
	Object evaluateDecisionDefinitionById(String id);
	Object updateHistoryTTLById(String id);
	Object getXmlById(String id);

	Collection<Job> getJobs(Map<String, Object> params, CIBUser user);
	void setSuspended(String id, Map<String, Object> data, CIBUser user);

	Integer findHistoryTaksCount(Map<String, Object> filters, CIBUser user);
	
}
