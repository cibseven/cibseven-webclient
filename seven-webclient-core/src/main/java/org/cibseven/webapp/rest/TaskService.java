package org.cibseven.webapp.rest;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskCount;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController @RequestMapping("${services.basePath:/services/v1}")
public class TaskService extends BaseService implements InitializingBean {
	
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("TaskService expects a BpmProvider");
	}	
	
	/*
	@RequestMapping(value = "/task", method = RequestMethod.GET)
	public Collection<Task> findTasks(@RequestParam Optional<String> filter, Locale loc, CIBSevenUser user) {
		return bpmProvider.findTasks(filter.isPresent() ? filter.get() : null, user);
	}
	*/
	
	@Operation(
			summary = "Get number of tasks based on request params",
			description = "<strong>Return: Number of tasks")
	@ApiResponse(responseCode = "404", description= "Task not found")
	@RequestMapping(value = "/task/count", method = RequestMethod.GET)
	public TaskCount findTasksCount(
			@Parameter(description = "Name of the task") @RequestParam Optional<String> name, 
			@Parameter(description = "Task name like ...") @RequestParam Optional<String> nameLike,
			@Parameter(description = "Task definition key") @RequestParam Optional<String> taskDefinitionKey,
			@Parameter(description = "Task definition key in") @RequestParam Optional<String> taskDefinitionKeyIn,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		return sevenProvider.findTasksCount(name, nameLike, taskDefinitionKey, taskDefinitionKeyIn, user);
	}
	 
	//Not used
	@RequestMapping(value = "/task/by-process-instance/{processInstanceId}", method = RequestMethod.GET)
	public Collection<Task> findTasksByProcessInstance(@PathVariable String processInstanceId, Locale loc, CIBUser user) {
		return bpmProvider.findTasksByProcessInstance(processInstanceId, user);
	}

	@Operation(
			summary = "Get tasks which belongs to a specific process instance and a user",
			description = "<strong>Return: Fetched tasks")
	@ApiResponse(responseCode = "404", description= "Task not found")
	@RequestMapping(value = "/task/by-process-instance-asignee", method = RequestMethod.GET)
	public Collection<Task> findTasksByProcessInstanceAsignee(
			@Parameter(description = "Process instance Id") @RequestParam Optional<String> processInstanceId,
			@Parameter(description = "Created after") @RequestParam Optional<String> createdAfter,
			Locale loc, CIBUser user) {
		return bpmProvider.findTasksByProcessInstanceAsignee(processInstanceId, createdAfter, user);
	}
	
	@Operation(
			summary = "Get task with a specific Id",
			description = "<strong>Return: Fetched task")
	@ApiResponse(responseCode = "404", description= "Task not found")
	@RequestMapping(value = "/task/{taskId}", method = RequestMethod.GET)
	public Task findTaskById(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			Locale loc, CIBUser user) {
		return bpmProvider.findTaskById(taskId, user);
	}
	
	@Operation(
			summary = "Get collection of tasks",
			description = "Request body: List of properties which will be use to filter tasks" + "<br>" +
			"<strong>Return: Collection of Tasks fetched in the search")
	@ApiResponse(responseCode = "404", description= "Task not found")
	@RequestMapping(value = "/task/by-filter/{filterId}", method = RequestMethod.POST)
	public Collection<Task> findTasksByFilter(
			@RequestBody TaskFiltering filters,
			@Parameter(description = "Filter Id") @PathVariable String filterId,
			@Parameter(description = "Index of the first result to return") @RequestParam Integer firstResult,
			@Parameter(description = "Maximum number of results to return") @RequestParam Integer maxResults,
			Locale loc, CIBUser user) {
		return bpmProvider.findTasksByFilter(filters, filterId, user, firstResult, maxResults);
	}
	
	@Operation(
			summary = "Get number of tasks by filter id",
			description = "Request body: List of properties which will be use to filter tasks" + "<br>" +
			"<strong>Return: Collection of Tasks fetched in the search")
	@ApiResponse(responseCode = "404", description= "Task not found")
	@RequestMapping(value = "/task/by-filter/{filterId}/count", method = RequestMethod.POST)
	public Integer findTasksCountByFilter(
			@Parameter(description = "Filter Id") @PathVariable String filterId,
			@RequestBody TaskFiltering filters,
			Locale loc, CIBUser user) {
		return bpmProvider.findTasksCountByFilter(filterId, user, filters);
	}
	
	@Operation(
			summary = "Post task without saving any variables",
			description = "Saving the variables is done by the ui-element-template (in ours)" + "<br>" +
			"<strong>Return: void")
	@ApiResponse(responseCode = "403", description = "Forbidden")
	@RequestMapping(value = "/task/submit/{taskId}", method = RequestMethod.POST)
	public void submit(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			Locale loc, CIBUser user) {
		bpmProvider.submit(taskId, user);
	}
	
	@Operation(
			summary = "Get form-reference variable from task",
			description = "<strong>Return: form-reference")
	@ApiResponse(responseCode = "404", description= "Task not found")
	@RequestMapping(value = "/task/{taskId}/form-reference", method = RequestMethod.GET)
	public Object formReference(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			Locale loc, CIBUser user) {
		return bpmProvider.formReference(taskId, user);
	}
	
	@Operation(
			summary = "Set assignee to an specific task",
			description = "UserID will be the assignee" + "<br>" + "<strong>Return: void")
	@ApiResponse(responseCode = "404", description= "Task or User not found")
	@RequestMapping(value = "/task/{taskId}/assignee/{userId}", method = RequestMethod.POST)
	public void setAssignee(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			@Parameter(description = "User to be set as assignee") @PathVariable String userId,
			Locale loc, CIBUser user) {
		bpmProvider.setAssignee(taskId, userId, user);
	}
	
	@Operation(
			summary = "Update task",
			description = "Request body: Task to be updated with the desired values already modified" + "<br>" +
			"<strong>Return: void")
	@RequestMapping(value = "/task/update", method = RequestMethod.PUT)
	public void update(
			@RequestBody Task task,
			Locale loc, CIBUser user) {
		bpmProvider.update(task, user);
	}

	//Requested by OFDKA
	@Operation(
			summary = "Get collection of tasks",
			description = "Required by OFDKA Queries for tasks that fulfill a given filter." + "<br>" + 
			"This method is slightly more powerful than the Get Tasks method because it allows filtering by multiple process or task variables of types String, Number or Boolean."
			+ "<br>" + "Request body: Variables to apply search"
			+ "<br>" + "<strong>Return: Collection tasks fetched in the search")
	@RequestMapping(value = "/task", method = RequestMethod.POST)
	public Collection<Task> findTasksPost(
			@RequestBody Map<String, Object> data,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		return sevenProvider.findTasksPost(data, user);
	}

	@Operation(
			summary = "Get identity links, e.g. to get the candidates user or groups of a task",
			description = "<strong>Return: Identity links")
	@ApiResponse(responseCode = "404", description= "Identity links not found")
	@RequestMapping(value = "/task/{taskId}/identity-links", method = RequestMethod.GET)
	public Collection<IdentityLink> findIdentityLink(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			@Parameter(description = "Type of links to include e.g. 'candidate'") @RequestParam Optional<String> type,
			Locale loc, CIBUser user) {
		return bpmProvider.findIdentityLink(taskId, type, user);
	}
	
	@Operation(
			summary = "Create identity links, e.g. to set the candidates user or groups of a task",
			description = "<strong>Return: void")
	@RequestMapping(value = "/task/{taskId}/identity-links", method = RequestMethod.POST)
	public void createIdentityLink(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			@RequestBody Map<String, Object> data,
			Locale loc, CIBUser user) {
		bpmProvider.createIdentityLink(taskId, data, user);
	}
	
	@Operation(
			summary = "Delete identity links, e.g. to remove the candidates user or groups of a task",
			description = "<strong>Return: void")
	@RequestMapping(value = "/task/{taskId}/identity-links/delete", method = RequestMethod.POST)
	public void deleteIdentityLink(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			@RequestBody Map<String, Object> data,
			Locale loc, CIBUser user) {
		bpmProvider.deleteIdentityLink(taskId, data, user);
	}
	
	@Operation(
			summary = "Get variables from a specific activity",
			description = "<strong>Return: Fetched variables")
	@ApiResponse(responseCode = "404", description= "Activity instance not found")
	@RequestMapping(value = "/task/{activityInstanceId}/variables", method = RequestMethod.GET)
	public Collection<VariableHistory> fetchActivityVariables(
			@Parameter(description = "Activity instance Id") @PathVariable String activityInstanceId,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
		return bpmProvider.fetchActivityVariables(activityInstanceId, user);
	}
	
	@Operation(
			summary = "Download file from process instance by variable name",
			description = "<strong>Return: File data")
	@ApiResponse(responseCode = "404", description= "Variable name not found")
	@RequestMapping(value = "/task/{processInstanceId}/variable/download/{variableName}", method = RequestMethod.GET)
	public ResponseEntity<byte[]> downloadFiles(@PathVariable String processInstanceId, @PathVariable String variableName, CIBUser user) {
		return bpmProvider.fetchProcessInstanceVariableData(processInstanceId, variableName, user);
	}
	
}
