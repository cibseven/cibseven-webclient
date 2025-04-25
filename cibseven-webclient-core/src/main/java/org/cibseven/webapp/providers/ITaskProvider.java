package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.CandidateGroupTaskCount;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskCount;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskHistory;
import org.cibseven.webapp.rest.model.Variable;
import org.springframework.http.ResponseEntity;

/*
 * Enables dependency injection and facilitates testing.
 */
public interface ITaskProvider {
	
	public Collection<Task> findTasks(String filter, CIBUser user);
	public TaskCount findTasksCount(Optional<String> name, Optional<String> nameLike, Optional<String> taskDefinitionKey, Optional<String> taskDefinitionKeyIn, CIBUser user);
	public Collection<Task> findTasksByProcessInstance(String processInstanceId, CIBUser user);
	public Collection<Task> findTasksByProcessInstanceAsignee(Optional<String> processInstanceId, Optional<String> createdAfter, CIBUser user);
	public Task findTaskById(String id, CIBUser user);
	public void update(Task task, CIBUser user);
	public void setAssignee(String taskId, String assignee, CIBUser user);
	public void submit(String taskId, CIBUser user);
	public void submit(Task task, List<Variable> formResult, CIBUser user);
	public Object formReference(String taskId, CIBUser user);
	public Collection<Task> findTasksByFilter(TaskFiltering filters, String filterId, CIBUser user, Integer firstResult, Integer maxResults);
	public Integer findTasksCountByFilter(String filterId, CIBUser user, TaskFiltering filters);
	public Collection<TaskHistory> findTasksByProcessInstanceHistory(String processInstanceId, CIBUser user);
	public Collection<TaskHistory> findTasksByDefinitionKeyHistory(String taskDefinitionKey, String processInstanceId, CIBUser user);
	public Collection<Task> findTasksPost(Map<String, Object> data, CIBUser user) throws SystemException;
	public Collection<IdentityLink> findIdentityLink(String taskId, Optional<String> type, CIBUser user);
	public void createIdentityLink(String taskId, Map<String, Object> data, CIBUser user);
	public void deleteIdentityLink(String taskId, Map<String, Object> data, CIBUser user);
	public void handleBpmnError(String taskId, Map<String, Object> data, CIBUser user) throws SystemException;
	public Collection<TaskHistory> findTasksByTaskIdHistory(String taskId, CIBUser user);
	public ResponseEntity<byte[]> getDeployedForm(String taskId, CIBUser user);
	public Integer findHistoryTaksCount(Map<String, Object> filters, CIBUser user);
	public Collection<CandidateGroupTaskCount> getTaskCountByCandidateGroup(CIBUser user);
	
}
