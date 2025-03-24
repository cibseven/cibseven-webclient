package org.cibseven.providers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.Data;
import org.cibseven.NamedByteArrayDataSource;
import org.cibseven.auth.CIBUser;
import org.cibseven.exception.ExpressionEvaluationException;
import org.cibseven.exception.InvalidUserIdException;
import org.cibseven.exception.NoObjectFoundException;
import org.cibseven.exception.SystemException;
import org.cibseven.exception.UnexpectedTypeException;
import org.cibseven.exception.UnsupportedTypeException;
import org.cibseven.rest.model.ActivityInstance;
import org.cibseven.rest.model.ActivityInstanceHistory;
import org.cibseven.rest.model.Authorization;
import org.cibseven.rest.model.Authorizations;
import org.cibseven.rest.model.SevenUser;
import org.cibseven.rest.model.SevenVerifyUser;
import org.cibseven.rest.model.Deployment;
import org.cibseven.rest.model.DeploymentResource;
import org.cibseven.rest.model.EventSubscription;
import org.cibseven.rest.model.Filter;
import org.cibseven.rest.model.IdentityLink;
import org.cibseven.rest.model.Incident;
import org.cibseven.rest.model.Message;
import org.cibseven.rest.model.NewUser;
import org.cibseven.rest.model.Process;
import org.cibseven.rest.model.ProcessDiagram;
import org.cibseven.rest.model.ProcessInstance;
import org.cibseven.rest.model.ProcessStart;
import org.cibseven.rest.model.ProcessStatistics;
import org.cibseven.rest.model.StartForm;
import org.cibseven.rest.model.Task;
import org.cibseven.rest.model.TaskCount;
import org.cibseven.rest.model.TaskFiltering;
import org.cibseven.rest.model.TaskHistory;
import org.cibseven.rest.model.User;
import org.cibseven.rest.model.UserGroup;
import org.cibseven.rest.model.Variable;
import org.cibseven.rest.model.VariableHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SevenProvider extends SevenProviderBase implements BpmProvider {
	
    @Autowired private IDeploymentProvider deploymentProvider;
    @Autowired private IVariableProvider variableProvider;
    @Autowired private ITaskProvider taskProvider;
    @Autowired private IProcessProvider processProvider;
    @Autowired private IActivityProvider activityProvider;
    @Autowired private IFilterProvider filterProvider;
    @Autowired private IUtilsProvider utilsProvider;
    @Autowired private IIncidentProvider incidentProvider;
    @Autowired private IUserProvider userProvider;

    
    /*
	
	████████  █████  ███████ ██   ██     ██████  ██████   ██████  ██    ██ ██ ██████  ███████ ██████  
	   ██    ██   ██ ██      ██  ██      ██   ██ ██   ██ ██    ██ ██    ██ ██ ██   ██ ██      ██   ██ 
	   ██    ███████ ███████ █████       ██████  ██████  ██    ██ ██    ██ ██ ██   ██ █████   ██████  
	   ██    ██   ██      ██ ██  ██      ██      ██   ██ ██    ██  ██  ██  ██ ██   ██ ██      ██   ██ 
	   ██    ██   ██ ███████ ██   ██     ██      ██   ██  ██████    ████   ██ ██████  ███████ ██   ██ 
	                                                                                                                                                                                            
     */
    
	@Override
	public Collection<Task> findTasks(String filter, CIBUser user) {
		return taskProvider.findTasks(filter, user);
	}

	@Override
	public TaskCount findTasksCount(Optional<String> name, Optional<String> nameLike, Optional<String> taskDefinitionKey, Optional<String> taskDefinitionKeyIn, CIBUser user) {
		return taskProvider.findTasksCount(name, nameLike, taskDefinitionKey, taskDefinitionKeyIn, user);
	}
	
	@Override
	public Collection<Task> findTasksByProcessInstance(String processInstanceId, CIBUser user) {
		return taskProvider.findTasksByProcessInstance(processInstanceId, user);
	}
	
	@Override
	public Collection<Task> findTasksByProcessInstanceAsignee(Optional<String> processInstanceId, Optional<String> createdAfter, CIBUser user) {
		return taskProvider.findTasksByProcessInstanceAsignee(processInstanceId, createdAfter, user);
	}
	
	@Override
	public Task findTaskById(String id, CIBUser user) {
		return taskProvider.findTaskById(id, user);
	}
	

	@Override
	public void update(Task task, CIBUser user) {
		taskProvider.update(task, user);
	}
	
	@Override
	public void setAssignee(String taskId, String assignee, CIBUser user) {
		taskProvider.setAssignee(taskId, assignee, user);
	}
	
	@Override
	public void submit(String taskId, CIBUser user) {
		taskProvider.submit(taskId, user);
	}
	
	@Override
	public void submit(Task task, List<Variable> formResult, CIBUser user) {
		taskProvider.submit(task, formResult, user);;
	}

	@Override
	public Object formReference(String taskId, CIBUser user) {
		return taskProvider.formReference(taskId, user);
	}
	
	@Override
	public Collection<Task> findTasksByFilter(TaskFiltering filters, String filterId, CIBUser user, Integer firstResult, Integer maxResults) {
		return taskProvider.findTasksByFilter(filters, filterId, user, firstResult, maxResults);
	}
	
	@Override
	public Integer findTasksCountByFilter(String filterId, CIBUser user, TaskFiltering filters) {
		return taskProvider.findTasksCountByFilter(filterId, user, filters);
	}
	
	@Override
	public Collection<TaskHistory> findTasksByProcessInstanceHistory(String processInstanceId, CIBUser user) {
		return taskProvider.findTasksByProcessInstanceHistory(processInstanceId, user);
	}
	
	@Override
	public Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId, CIBUser user) {
		return taskProvider.findTasksByDefinitionKeyHistory(taskDefinitionKey, processInstanceId, user);
	}
	
	@Override
	public Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException {
		return taskProvider.findTasksPost(data, user);
	}
	
	@Override
	public Collection<IdentityLink> findIdentityLink(String taskId, Optional<String> type, CIBUser user) {
		return taskProvider.findIdentityLink(taskId, type, user);
	}

	@Override
	public void createIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
		taskProvider.createIdentityLink(taskId, data, user);
	}
	
	@Override
	public void deleteIdentityLink(String taskId, Map<String, Object> data, CIBUser user) {
		taskProvider.deleteIdentityLink(taskId, data, user);
	}
	
	@Override
	public void handleBpmnError(String taskId, Map<String, Object> data, CIBUser user) throws SystemException {
		taskProvider.handleBpmnError(taskId, data, user);
	}

	@Override
	public Collection<TaskHistory> findTasksByTaskIdHistory(String taskId, CIBUser user) {
		return taskProvider.findTasksByTaskIdHistory(taskId, user);
	}	
	
	@Override
	public ResponseEntity<byte[]> getDeployedForm(String taskId, CIBUser user) {
		return taskProvider.getDeployedForm(taskId, user);
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
		return processProvider.findProcesses(user);
	}
	
	@Override
	public Collection<Process> findProcessesWithInfo(CIBUser user) {
		return processProvider.findProcessesWithInfo(user);
	}
	
	@Override
	public Collection<Process> findProcessesWithFilters(String filters, CIBUser user) {
		return processProvider.findProcessesWithFilters(filters, user);
	}	
	
	@Override
	public Process findProcessByDefinitionKey(String key, CIBUser user) {
		return processProvider.findProcessByDefinitionKey(key, user);
	}
	
	@Override
	public Collection<Process> findProcessVersionsByDefinitionKey(String key, Optional<Boolean> lazyLoad, CIBUser user) {
		return processProvider.findProcessVersionsByDefinitionKey(key, lazyLoad, user);
	}

	@Override
	public Process findProcessById(String id, Optional<Boolean> extraInfo, CIBUser user) throws SystemException {
		return processProvider.findProcessById(id, extraInfo, user);
	}
		
	@Override
	public Collection<ProcessInstance> findProcessesInstances(String key, CIBUser user) {
		return processProvider.findProcessesInstances(key, user);
	}
	
	@Override
	public ProcessDiagram fetchDiagram(String id, CIBUser user) {
		return processProvider.fetchDiagram(id, user);
	}
	
	@Override
	public StartForm fetchStartForm(String processDefinitionId, CIBUser user) {
		return processProvider.fetchStartForm(processDefinitionId, user);
	}
	
	@Override
	public Data downloadBpmn(String id, String fileName, CIBUser user) {
		return processProvider.downloadBpmn(id, fileName, user);
	}
	
	@Override
	public void suspendProcessInstance(String processInstanceId, Boolean suspend, CIBUser user) {
		processProvider.suspendProcessInstance(processInstanceId, suspend, user);
	}
	
	@Override
	public void deleteProcessInstance(String processInstanceId, CIBUser user) {
		processProvider.deleteProcessInstance(processInstanceId, user);
	}
	
	@Override
	public void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user) {
		processProvider.suspendProcessDefinition(processDefinitionId, suspend, includeProcessInstances, executionDate, user);
	}
	
	@Override
	public ProcessStart startProcess(String processDefinitionKey, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		return processProvider.startProcess(processDefinitionKey, data, user);
	}
	
	@Override
	public ProcessStart submitForm(String processDefinitionKey, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		return processProvider.submitForm(processDefinitionKey, data, user);
	}
	
	@Override
	public Collection<ProcessStatistics> findProcessStatistics(String processId, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		return processProvider.findProcessStatistics(processId, user);
	}
	
	@Override
	public Collection<ProcessInstance> findProcessesInstancesHistory(String key, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, CIBUser user) {
		return processProvider.findProcessesInstancesHistory(key, active, firstResult, maxResults, user);
	}
	
	@Override
	public Collection<ProcessInstance> findProcessesInstancesHistoryById(String id, Optional<String> activityId, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, String text, CIBUser user) {
		return processProvider.findProcessesInstancesHistoryById(id, activityId, active, firstResult, maxResults, text, user);
	}	
	
	@Override
	public ProcessInstance findProcessInstance(String processInstanceId, CIBUser user) {
		return processProvider.findProcessInstance(processInstanceId, user);
	}

	@Override
	public Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, String deserializeValue, CIBUser user) throws SystemException  {
		return processProvider.fetchProcessInstanceVariable(processInstanceId, variableName, deserializeValue, user);
	}	
	
	@Override
	public ProcessInstance findHistoryProcessInstanceHistory(String processInstanceId, CIBUser user) {
		return processProvider.findHistoryProcessInstanceHistory(processInstanceId, user);
	}
	
	@Override
	public Collection<Process> findCalledProcessDefinitions(String processDefinitionId, CIBUser user) {
		return processProvider.findCalledProcessDefinitions(processDefinitionId, user);
	}
	
	@Override
	public ResponseEntity<byte[]> getDeployedStartForm(String processDefinitionId, CIBUser user) {
		return processProvider.getDeployedStartForm(processDefinitionId, user);
	}

	@Override
	public void updateHistoryTimeToLive(String id, Map<String, Object> data, CIBUser user) {
		processProvider.updateHistoryTimeToLive(id, data, user);
	}

	@Override
	public void deleteProcessInstanceFromHistory(String id, CIBUser user) {
		processProvider.deleteProcessInstanceFromHistory(id, user);
	}
	
	@Override
	public void deleteProcessDefinition(String id, Optional<Boolean> cascade, CIBUser user) {
		processProvider.deleteProcessDefinition(id, cascade, user);
	}
	
	@Override
	public Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user)
			throws SystemException {
		return processProvider.findCurrentProcessesInstances(data, user);
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
		return filterProvider.findFilters(user);
	}

	@Override
	public Filter createFilter(Filter filter, CIBUser user) {
		return filterProvider.createFilter(filter, user);
	}

	@Override
	public void updateFilter(Filter filter, CIBUser user) {
		filterProvider.updateFilter(filter, user);
	}

	@Override
	public void deleteFilter(String filterId, CIBUser user) {
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
		return deploymentProvider.deployBpmn(data, file, user);
		
	}
	
	@Override
	public Collection<Deployment> findDeployments(CIBUser user) {
		return deploymentProvider.findDeployments(user);
	}

	@Override
	public Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user) {
		return deploymentProvider.findDeploymentResources(deploymentId, user);
	}

	@Override
	public Data fetchDataFromDeploymentResource(HttpServletRequest rq, String deploymentId, String resourceId, String fileName) {
		return deploymentProvider.fetchDataFromDeploymentResource(rq, deploymentId, resourceId, fileName);
	}
	
	@Override
	public void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException {
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
		return activityProvider.findActivityInstance(processInstanceId, user);
	}
		
	
	@Override
	public List<ActivityInstanceHistory> findActivitiesInstancesHistory(String processInstanceId, CIBUser user) {
		return activityProvider.findActivitiesInstancesHistory(processInstanceId, user);
	}
	
	@Override
	public ActivityInstance findActivityInstances(String processInstanceId, CIBUser user) throws SystemException {
		return activityProvider.findActivityInstances(processInstanceId, user);
	}
	
	@Override
	public List<ActivityInstanceHistory> findActivityInstanceHistory(String processInstanceId, CIBUser user) throws SystemException {
		return activityProvider.findActivityInstanceHistory(processInstanceId, user);
	}	

	@Override
	public void deleteVariableByExecutionId(String executionId, String variableName, CIBUser user) {
		activityProvider.deleteVariableByExecutionId(executionId, variableName, user);
	}

	@Override
	public void deleteVariableHistoryInstance(String id, CIBUser user) {
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
		return utilsProvider.correlateMessage(data, user);
	}

	@Override
	public String findStacktrace(String jobId, CIBUser user) {
		return utilsProvider.findStacktrace(jobId, user);
	}
	
	@Override
	public void retryJobById(String jobId, Map<String, Object> data, CIBUser user) {
		utilsProvider.retryJobById(jobId, data, user);
	}

	@Override
	public Collection<EventSubscription> getEventSubscriptions(Optional<String> processInstanceId,
			Optional<String> eventType, Optional<String> eventName, CIBUser user) {
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
		return userProvider.getUserAuthorization(userId, user);
	}
	
	public Collection<SevenUser> fetchUsers(CIBUser user) throws SystemException {
		return userProvider.fetchUsers(user);
	}
	
	public SevenVerifyUser verifyUser(String username, String password, CIBUser user) throws SystemException {
		return userProvider.verifyUser(username, password, user);
	}
	
	@Override
	public Collection<User> findUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike, Optional<String> lastName, Optional<String> lastNameLike,
			Optional<String> email, Optional<String> emailLike, Optional<String> memberOfGroup, Optional<String> memberOfTenant, Optional<String> idIn, 
			Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder, CIBUser user) {
		return userProvider.findUsers(id, firstName, firstNameLike, lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder, user);
	}
	
	@Override
	public void createUser(NewUser user, CIBUser flowUser) throws InvalidUserIdException {
		userProvider.createUser(user, flowUser);
	}
	
	@Override
	public void updateUserProfile(String userId, User user, CIBUser flowUser) {
		userProvider.updateUserProfile(userId, user, flowUser);
	}
	
	@Override
	public void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user) {
		userProvider.updateUserCredentials(userId, data, user);
	}
	
	@Override
	public void addMemberToGroup(String groupId, String userId, CIBUser user) {
		userProvider.addMemberToGroup(groupId, userId, user);
	}
	
	@Override
	public void deleteMemberFromGroup(String groupId, String userId, CIBUser user) {
		userProvider.deleteMemberFromGroup(groupId, userId, user);
	}

	@Override
	public void deleteUser(String userId, CIBUser user) {
		userProvider.deleteUser(userId, user);
	}
	
	@Override
	public SevenUser getUserProfile(String userId, CIBUser user) {
		return userProvider.getUserProfile(userId, user);
	}

	@Override
	public Collection<UserGroup> findGroups(Optional<String> id, Optional<String> name, Optional<String> nameLike, Optional<String> type,
			Optional<String> member, Optional<String> memberOfTenant, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
			Optional<String> maxResults, CIBUser user) {
		return userProvider.findGroups(id, name, nameLike, type, member, memberOfTenant, sortBy, sortOrder, firstResult, maxResults, user);
	}

	@Override
	public void createGroup(UserGroup group, CIBUser user) {
		userProvider.createGroup(group, user);
	}

	@Override
	public void updateGroup(String groupId, UserGroup group, CIBUser user) {
		userProvider.updateGroup(groupId, group, user);
	}

	@Override
	public void deleteGroup(String groupId, CIBUser user) {
		userProvider.deleteGroup(groupId, user);
	}

	@Override
	public Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type, Optional<String> userIdIn, Optional<String> groupIdIn,
			Optional<String> resourceType, Optional<String> resourceId, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
			Optional<String> maxResults, CIBUser user) {
		return userProvider.findAuthorization(id, type, userIdIn, groupIdIn, resourceType, resourceId, sortBy, sortOrder, firstResult, maxResults, user);
	}

	@Override
	public ResponseEntity<Authorization> createAuthorization(Authorization authorization, CIBUser user) {
		return userProvider.createAuthorization(authorization, user);
	}

	@Override
	public void updateAuthorization(String authorizationId, Map<String, Object> data, CIBUser user) {
		userProvider.updateAuthorization(authorizationId, data, user);
	}

	@Override
	public void deleteAuthorization(String authorizationId, CIBUser user) {
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
	public Long countIncident(Optional<String> incidentId, Optional<String> incidentType, Optional<String> incidentMessage, Optional<String> processDefinitionId,
			Optional<String> processDefinitionKeyIn, Optional<String> processInstanceId, Optional<String> executionId, Optional<String> activityId, 
			Optional<String> causeIncidentId, Optional<String> rootCauseIncidentId, Optional<String> configuration, Optional<String> tenantIdIn, 
			Optional<String> jobDefinitionIdIn, Optional<String> name, CIBUser user) {
		return incidentProvider.countIncident(incidentId, incidentType, incidentMessage, 
				processDefinitionId, processDefinitionKeyIn, processInstanceId, executionId, activityId, 
				causeIncidentId, rootCauseIncidentId, configuration, tenantIdIn, jobDefinitionIdIn, name, user);
	}

	@Override
	public Collection<Incident> findIncident(Optional<String> incidentId, Optional<String> incidentType, Optional<String> incidentMessage, Optional<String> processDefinitionId, 
			Optional<String> processDefinitionKeyIn, Optional<String> processInstanceId, Optional<String> executionId, Optional<String> activityId, Optional<String> causeIncidentId,
			Optional<String> rootCauseIncidentId, Optional<String> configuration, Optional<String> tenantIdIn, Optional<String> jobDefinitionIdIn, CIBUser user) {
		return incidentProvider.findIncident(incidentId, incidentType, incidentMessage, 
				processDefinitionId, processDefinitionKeyIn, processInstanceId, executionId, 
				activityId, causeIncidentId, rootCauseIncidentId, configuration, tenantIdIn, jobDefinitionIdIn, user);
	}
	
	@Override
	public List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user) {
		return incidentProvider.findIncidentByInstanceId(processInstanceId, user);
	}

	@Override
	public Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user) {
		return incidentProvider.fetchIncidents(processDefinitionKey, user);
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
		variableProvider.modifyVariableByExecutionId(executionId, data, user);
	}
	
	@Override
	public void modifyVariableDataByExecutionId(String executionId, String variableName, MultipartFile file, CIBUser user) throws SystemException {
		variableProvider.modifyVariableDataByExecutionId(executionId, variableName, file, user);
	}
	
	@Override
	public Collection<Variable> fetchProcessInstanceVariables(String processInstanceId, CIBUser user, Optional<Boolean> deserializeValue) throws SystemException {
		return variableProvider.fetchProcessInstanceVariables(processInstanceId, user, deserializeValue);
	}
	
	@Override
	public ResponseEntity<byte[]> fetchVariableDataByExecutionId(String executionId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException  {
		return variableProvider.fetchVariableDataByExecutionId(executionId, variableName, user);
	}	
	
	@Override
	public Collection<VariableHistory> fetchProcessInstanceVariablesHistory(String processInstanceId, CIBUser user, Optional<Boolean> deserializeValue) {
		return variableProvider.fetchProcessInstanceVariablesHistory(processInstanceId, user, deserializeValue);
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
		return variableProvider.fetchHistoryVariableDataById(id, user);
	}
	
	@Override
	public Variable fetchVariable(String taskId, String variableName, 
			Optional<Boolean> deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException {		
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
	public ResponseEntity<byte[]> fetchProcessInstanceVariableData(String processInstanceId, String variableName,
			CIBUser user) throws NoObjectFoundException, SystemException {
		return variableProvider.fetchProcessInstanceVariableData(processInstanceId, variableName, user);
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
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
	}

	@Override
	public void putLocalExecutionVariable(String executionId, String varName, Map<String, Object> data, CIBUser user) {
		variableProvider.putLocalExecutionVariable(executionId, varName, data, user);
	}

	@Override
	public Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(String processDefinitionId,
			CIBUser user) {
		return activityProvider.findActivitiesProcessDefinitionHistory(processDefinitionId, user);
	}

}
