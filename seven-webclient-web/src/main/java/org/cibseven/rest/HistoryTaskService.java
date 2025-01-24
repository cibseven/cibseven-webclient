package org.cibseven.rest;

import java.util.Collection;
import java.util.Locale;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.cib.cibflow.api.rest.camunda.model.TaskHistory;
import de.cib.cibflow.api.rest.camunda.model.VariableHistory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import de.cib.cibflow.CIBFlowUser;

@ApiResponses({
	@ApiResponse(responseCode= "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode= "401", description = "Unauthorized")
})
@RestController @RequestMapping("/flow-engine")
public class HistoryTaskService extends BaseService {
	
	@Operation(
			summary = "Get variables from a specific activity",
			description = "The variables found belongs to the history, they have other attributes and variables from finished activities are also fetched" + "<br>"
			+ "<strong>Return: Collection of fetched variables")
	@ApiResponse(responseCode= "404", description = "Activity instance not found")
	@RequestMapping(value = "/task-history/{activityInstanceId}/variables", method = RequestMethod.GET)
	public Collection<VariableHistory> fetchActivityVariablesHistory(
			@Parameter(description = "Activity instance Id") @PathVariable String activityInstanceId,
			Locale loc, CIBFlowUser user) {
		return bpmProvider.fetchActivityVariablesHistory(activityInstanceId, user);
	}
	
	@Operation(
			summary = "Get tasks which belongs to a specific process instance and filtered by a definition key",
			description = "The tasks found belongs to the history, they have other attributes and finished tasks are also fetched" + "<br>"
			+ "<strong>Return: Collection of fetched tasks")
	@ApiResponse(responseCode= "404", description = "Task/s not found")
	@RequestMapping(value = "/task-history/by-process-key", method = RequestMethod.GET)
	public Collection<TaskHistory> findTasksByDefinitionKeyHistory(
			@Parameter(description = "Restrict to tasks that have the given key") @RequestParam String taskDefinitionKey,
			@Parameter(description = "Process instance Id") @RequestParam String processInstanceId,
			Locale loc, CIBFlowUser user) {
		return bpmProvider.findTasksByDefinitionKeyHistory(taskDefinitionKey, processInstanceId, user);
	}

	@Operation(
			summary = "Get tasks which belongs to a specific process instance",
			description = "The tasks found belongs to the history, they have other attributes and finished tasks are also fetched" + "<br>"
			+ "<strong>Return: Collection of fetched tasks")
	@ApiResponse(responseCode= "404", description = "Task/s not found")
	@RequestMapping(value = "/task-history/by-process-instance/{processInstanceId}", method = RequestMethod.GET)
	public Collection<TaskHistory> findTasksByProcessInstanceHistory(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			Locale loc, CIBFlowUser user) {
		return bpmProvider.findTasksByProcessInstanceHistory(processInstanceId, user);
	}
	
	@Operation(
			summary = "Get tasks which belongs to a specific task id",
			description = "The tasks found belongs to the history, they have other attributes and finished tasks are also fetched" + "<br>"
			+ "<strong>Return: Collection of fetched tasks")
	@ApiResponse(responseCode= "404", description = "Task/s not found")
	@RequestMapping(value = "/task-history/by-task-id/{taskId}", method = RequestMethod.GET)
	public Collection<TaskHistory> findTasksByTaskIdHistory(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			Locale loc, CIBFlowUser user) {
		return bpmProvider.findTasksByTaskIdHistory(taskId, user);
	}

}
