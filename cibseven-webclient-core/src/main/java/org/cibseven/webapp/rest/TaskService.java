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
package org.cibseven.webapp.rest;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import java.nio.charset.StandardCharsets;

import org.cibseven.webapp.NamedByteArrayDataSource;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.logger.TaskLogger;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.CandidateGroupTaskCount;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.TaskFiltering;
import org.cibseven.webapp.rest.model.TaskForm;
import org.cibseven.webapp.rest.model.StartForm;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController("WebclientTaskService") @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}")
public class TaskService extends BaseService implements InitializingBean {
	
	SevenProvider sevenProvider;
	
	@Autowired
	private CustomRestTemplate restTemplate;
	
	@Value("${cibseven.webclient.engineRest.url:./}") 
	private String cibsevenUrl;
	
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
			summary = "Get number of tasks based on filters",
			description = "<strong>Return: Number of tasks")
	@ApiResponse(responseCode = "404", description= "Task not found")
	@PostMapping("/task/count")
	public Integer findTasksCount(
			@RequestBody Map<String, Object> filters,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
		return sevenProvider.findTasksCount(filters, user);
	}
	 
	//Not used
	@RequestMapping(value = "/task/by-process-instance/{processInstanceId}", method = RequestMethod.GET)
	public Collection<Task> findTasksByProcessInstance(@PathVariable String processInstanceId, Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
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
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
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
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
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
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
		return bpmProvider.findTasksCountByFilter(filterId, user, filters);
	}
	
	@Operation(
			summary = "Post task without saving any variables",
			description = "Saving the variables is done by the ui-element-template (in ours)" + "<br>" +
			"<strong>Return: void")
	@ApiResponse(responseCode = "403", description = "Forbidden")
	@RequestMapping(value = "/task/submit/{taskId}", method = RequestMethod.POST)
	public ResponseEntity<Void> submit(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.UPDATE_ALL);
		bpmProvider.submit(taskId, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}
	
	@Operation(
			summary = "Get form-reference variable from task",
			description = "<strong>Return: form-reference")
	@ApiResponse(responseCode = "404", description= "Task not found")
	@RequestMapping(value = "/task/{taskId}/form-reference", method = RequestMethod.GET)
	public Object formReference(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
		return bpmProvider.formReference(taskId, user);
	}
	
	@Operation(
			summary = "Get deployed form for task",
			description = "<strong>Return: Form data as bytes (JSON or HTML)")
	@ApiResponse(responseCode = "404", description= "Task or form not found")
	@RequestMapping(value = "/task/{taskId}/deployed-form", method = RequestMethod.GET)
	public ResponseEntity<byte[]> getDeployedForm(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
		
		try {
			ResponseEntity<byte[]> response = bpmProvider.getDeployedForm(taskId, user);
			byte[] body = response.getBody();
			
			if (body == null || body.length == 0) {
				return ResponseEntity.noContent().build();
			}
			
			return ResponseEntity.ok()
					.headers(response.getHeaders())
					.body(body);
			
		} catch (Exception e) {
			throw new SystemException("Error getting deployed form: " + e.getMessage(), e);
		}
	}

	@Operation(
			summary = "Retrieves the form configuration data associated with a specific task",
			description = "<strong>Return: TaskForm object containing key, camundaFormRef, and contextPath</strong>")
	@ApiResponse(responseCode = "404", description= "Task not found")
	@RequestMapping(value = "/task/{taskId}/form", method = RequestMethod.GET)
	public Object form(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
		return bpmProvider.form(taskId, user);
	}

	@Operation(
			summary = "Get rendered form HTML for task",
			description = "<strong>Return: Rendered form HTML as string</strong>")
	@ApiResponse(responseCode = "404", description= "Task or rendered form not found")
	@RequestMapping(value = "/task/{taskId}/rendered-form", method = RequestMethod.GET, produces = "text/html")
	public ResponseEntity<String> getRenderedForm(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			@RequestParam Map<String, Object> params,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
		return bpmProvider.getRenderedForm(taskId, params, user);
	}

	@Operation(
			summary = "Get form variables for a specific task",
			description = "<strong>Return: Form variables for the task</strong>")
	@ApiResponse(responseCode = "404", description = "Task not found")
	@RequestMapping(value = "/task/{taskId}/form-variables", method = RequestMethod.GET)
	public Map<String, Variable> fetchFormVariables(
			@PathVariable String taskId,
			@RequestParam(required = false, defaultValue = "true") boolean deserializeValues,
			@RequestParam(required = false) String variableNames,
			CIBUser user) throws Exception {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
		List<String> variableListName = null;
		if (variableNames != null && !variableNames.isEmpty()) {
			variableListName = List.of(variableNames.split(","));
		}
		if (variableListName != null) {
			return bpmProvider.fetchFormVariables(variableListName, taskId, deserializeValues, user);
		} else {
			return bpmProvider.fetchFormVariables(taskId, deserializeValues, user);
		}
	}

	@Operation(summary = "Submit form with variables", description = "Request body: Form variables to submit" + "<br>" +
			"<strong>Return: void</strong>")
	@ApiResponse(responseCode = "404", description = "Task not found")
	@RequestMapping(value = "/task/{taskId}/submit-form", method = RequestMethod.POST)
	public void submit(
			@Parameter(description = "Task id") @PathVariable String taskId,
			@RequestBody String formResult,
			Locale loc, HttpServletRequest rq, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.UPDATE_ALL);
		bpmProvider.submit(taskId, formResult, user);
	}
	
	@Operation(
			summary = "Set assignee to an specific task",
			description = "UserID will be the assignee" + "<br>" + "<strong>Return: void")
	@ApiResponse(responseCode = "404", description= "Task or User not found")
	@RequestMapping(value = "/task/{taskId}/assignee/{userId}", method = RequestMethod.POST)
	public ResponseEntity<Void> setAssignee(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			@Parameter(description = "User to be set as assignee") @PathVariable String userId,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.UPDATE_ALL);
		bpmProvider.setAssignee(taskId, userId, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}
	
	@Operation(
			summary = "Update task",
			description = "Request body: Task to be updated with the desired values already modified" + "<br>" +
			"<strong>Return: void")
	@RequestMapping(value = "/task/update", method = RequestMethod.PUT)
	public ResponseEntity<Void> update(
			@RequestBody Task task,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.UPDATE_ALL);
		bpmProvider.update(task, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
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
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
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
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
		return bpmProvider.findIdentityLink(taskId, type, user);
	}
	
	@Operation(
			summary = "Create identity links, e.g. to set the candidates user or groups of a task",
			description = "<strong>Return: void")
	@RequestMapping(value = "/task/{taskId}/identity-links", method = RequestMethod.POST)
	public ResponseEntity<Void> createIdentityLink(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			@RequestBody Map<String, Object> data,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.UPDATE_ALL);
		bpmProvider.createIdentityLink(taskId, data, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}
	
	@Operation(
			summary = "Delete identity links, e.g. to remove the candidates user or groups of a task",
			description = "<strong>Return: void")
	@RequestMapping(value = "/task/{taskId}/identity-links/delete", method = RequestMethod.POST)
	public ResponseEntity<Void> deleteIdentityLink(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			@RequestBody Map<String, Object> data,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteIdentityLink(taskId, data, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}
	
	@Operation(
			summary = "Get variables from a specific activity",
			description = "<strong>Return: Fetched variables")
	@ApiResponse(responseCode = "404", description= "Activity instance not found")
	@RequestMapping(value = "/task/{activityInstanceId}/variables", method = RequestMethod.GET)
	public Collection<VariableHistory> fetchActivityVariables(
			@Parameter(description = "Activity instance Id") @PathVariable String activityInstanceId,
			Locale loc, CIBUser user) {
        checkPermission(user, SevenResourceType.PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.fetchActivityVariables(activityInstanceId, user);
	}
	
	@Operation(
			summary = "Download file from process instance by variable name",
			description = "<strong>Return: File data")
	@ApiResponse(responseCode = "404", description= "Variable name not found")
	@RequestMapping(value = "/task/{processInstanceId}/variable/download/{variableName}", method = RequestMethod.GET)
	public ResponseEntity<byte[]> downloadFiles(@PathVariable String processInstanceId, @PathVariable String variableName, CIBUser user) {
        checkPermission(user, SevenResourceType.PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.fetchProcessInstanceVariableData(processInstanceId, variableName, user);
	}
	
	 @Consumes(MediaType.APPLICATION_JSON)
	  @RequestMapping(value = "/task/{taskId}/submit-variables", method = RequestMethod.POST)
	  public ResponseEntity<String> submitVariables(@PathVariable String taskId, @RequestBody List<Variable> formResult, 
	      @RequestParam String processInstanceId, @RequestParam Optional<String> assignee, @RequestParam String processDefinitionId,
	      @RequestParam Optional<Boolean> close, @RequestParam Optional<String> name, HttpServletRequest rq) {
	    String taskName = name.orElse("null");
	    TaskLogger logger = new TaskLogger(processDefinitionId, processInstanceId, taskName, taskId);
	    try {
	      CIBUser userAuth = (CIBUser) baseUserProvider.authenticateUser(rq);
          checkPermission(userAuth, SevenResourceType.PROCESS_INSTANCE, PermissionConstants.UPDATE_ALL);
	      logger.info("[INFO] Submit variables in task with name=" + taskName + " and ID=" + taskId + " (" + getClass().getSimpleName() + ")");
	      Task task = bpmProvider.findTaskById(taskId, userAuth);
	      if (!task.getAssignee().equals(userAuth.getUserID())) {
	        throw new AccessDeniedException("The user submiting the task is not the one assigned");
	      }
	      
	      if (close.orElse(false)) {
	        bpmProvider.submit(task, formResult, userAuth);
	      } else {
	        bpmProvider.submitVariables(processInstanceId, formResult, userAuth, processDefinitionId);
	      }
	      
	      logger.info("[INFO] Submited variables in task with name=" + taskName + " and ID=" + taskId + " (" + getClass().getSimpleName() + ")");
	      return new ResponseEntity<>("ok", new HttpHeaders(), HttpStatus.OK);
	    } catch (Exception e) {
	      logger.info("[INFO] Exception when submiting variables in task with name=" + taskName + " and ID=" + taskId + " (" + getClass().getSimpleName() + ")", e);
	      if (e instanceof NoObjectFoundException) return generateErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
	      if (e instanceof TokenExpiredException) throw e;
	      else return generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
	    }
	  }
	  
	  @SuppressWarnings("unchecked")
	  protected <T> ResponseEntity<T> generateErrorResponse(String message, HttpStatus status) {
	    ResponseEntity<?> response = new ResponseEntity<>(message, status);
	    return (ResponseEntity<T>) response;
	  }

	  @RequestMapping(value = "/task/{taskId}/variable/{variableName}", method = RequestMethod.GET)
	  public Variable fetchVariable(@PathVariable String taskId, @PathVariable String variableName, 
	      @RequestParam Optional<Boolean> deserialize, HttpServletRequest rq) {
	    CIBUser userAuth = (CIBUser) baseUserProvider.authenticateUser(rq);
        checkPermission(userAuth, SevenResourceType.PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		boolean deserializeValue = deserialize.orElse(true);
	    return bpmProvider.fetchVariable(taskId, variableName, deserializeValue, userAuth);
	  }
	  
	  @RequestMapping(value = "/task/{taskId}/variable/{variableName}/data", method = RequestMethod.GET)
	  public byte[] fetchVariableFileData(@PathVariable String taskId, @PathVariable String variableName, 
	      @RequestParam Optional<Boolean> deserialize, HttpServletRequest rq) {
	    CIBUser userAuth = (CIBUser) baseUserProvider.authenticateUser(rq);
        checkPermission(userAuth, SevenResourceType.TASK, PermissionConstants.READ_ALL);
	    NamedByteArrayDataSource res = bpmProvider.fetchVariableFileData(taskId, variableName, userAuth);
	    return res.getContent();
	  }


		@Operation(summary = "Upload file data to a task variable", description = "Upload binary data or file to a specific task variable"
				+ "<br>" +
				"<strong>Request body: Multipart form data with 'data' (binary) and 'valueType' (Bytes/File) parts")
		@ApiResponse(responseCode = "404", description = "Task or variable not found")
		@RequestMapping(value = "/task/{id}/variables/{variableName}/data", method = RequestMethod.POST, consumes = "multipart/form-data")
		public ResponseEntity<Void> uploadVariableFileData(
				@Parameter(description = "Task Id") @PathVariable String id,
				@Parameter(description = "Variable name") @PathVariable String variableName,
				@RequestParam(value = "data", required = true) MultipartFile data,
				@RequestParam(value = "valueType", required = false, defaultValue = "File") String valueType,
				HttpServletRequest rq) {
			CIBUser userAuth = (CIBUser) baseUserProvider.authenticateUser(rq);
			checkPermission(userAuth, SevenResourceType.TASK, PermissionConstants.UPDATE_ALL);
			try {
				bpmProvider.uploadVariableFileData(id, variableName, data, valueType, userAuth);
				return ResponseEntity.noContent().build();
			} catch (Exception e) {
				if (e instanceof NoObjectFoundException) {
					return ResponseEntity.notFound().build();
				}
				throw new RuntimeException("Failed to upload variable file data", e);
			}
		}

	  @RequestMapping(value = "/task/{taskId}", method = RequestMethod.POST)
	  public Map<String, Variable> fetchVariables(@PathVariable String taskId, 
	      @RequestParam Optional<Boolean> deserialize, 
	      @RequestParam Optional<String> locale, CIBUser user) throws Exception {
        checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
	    return bpmProvider.fetchFormVariables(taskId, deserialize.orElse(false), user);
	  }

	  @RequestMapping(value = "/task/{taskId}/variable/{variableName}", method = RequestMethod.DELETE)
	  public ResponseEntity<Void> deleteVariable(@PathVariable String taskId, @PathVariable String variableName, HttpServletRequest rq) {
	    CIBUser userAuth = (CIBUser) baseUserProvider.authenticateUser(rq);
        checkPermission(userAuth, SevenResourceType.TASK, PermissionConstants.DELETE_ALL);
	    bpmProvider.deleteVariable(taskId, variableName, userAuth);
      // return 204 No Content, no body
      return ResponseEntity.noContent().build();
	  }
	  
	  @RequestMapping(value = "/task/{taskId}/bpmnError", method = RequestMethod.POST)
	  public ResponseEntity<Void> handleBpmnError(@PathVariable String taskId, @RequestBody Map<String, Object> data, 
	      @RequestParam Optional<String> locale, CIBUser user) throws Exception {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.UPDATE_ALL);
	    bpmProvider.handleBpmnError(taskId, data, user);
      // return 204 No Content, no body
      return ResponseEntity.noContent().build();
	  }

	  @GetMapping("/task/report/candidate-group-count")
	  public Collection<CandidateGroupTaskCount> getTaskCountByCandidateGroup(@RequestParam Optional<String> locale, CIBUser user) throws Exception {
	    return bpmProvider.getTaskCountByCandidateGroup(user);
	  }

	  @Operation(
			summary = "Proxy embedded form content from engine",
			description = "<strong>Fetches embedded form HTML from the engine. Retrieves form info from engine, extracts form path, and fetches the HTML content.</strong>")
	  @ApiResponse(responseCode = "200", description = "Form HTML content")
	  @ApiResponse(responseCode = "403", description = "Only HTML files are allowed")
	  @ApiResponse(responseCode = "404", description = "Form not found")
	  @GetMapping("/task/form-proxy")
	  public ResponseEntity<String> proxyFormContent(
			@Parameter(description = "Reference ID (task ID or process definition ID)") @RequestParam String referenceId,
			@Parameter(description = "Whether this is a start form") @RequestParam boolean isStartForm,
			CIBUser user) {
		checkPermission(user, SevenResourceType.TASK, PermissionConstants.READ_ALL);
		
		// Get form info from the engine
		String formKey;
		String contextPath = null;
		
		if (isStartForm) {
			StartForm startForm = sevenProvider.fetchStartForm(referenceId, user);
			if (startForm == null) {
				throw new SystemException("Start form not found for process definition: " + referenceId);
			}
			formKey = startForm.getKey();
			contextPath = startForm.getContextPath();
		} else {
			Object formResult = sevenProvider.form(referenceId, user);
			if (formResult instanceof String && "empty-task".equals(formResult)) {
				throw new SystemException("Task form not found for task: " + referenceId);
			}
			if (!(formResult instanceof TaskForm)) {
				throw new SystemException("Unexpected form result type: " + formResult.getClass().getName());
			}
			TaskForm taskForm = (TaskForm) formResult;
			formKey = taskForm.getKey();
			contextPath = taskForm.getContextPath();
		}
		
		if (formKey == null || formKey.isEmpty()) {
			throw new SystemException("Form key is null or empty");
		}
		
		// Construct form path from form key
		// Example: "embedded:app:forms/assign-reviewer.html" with contextPath "/" becomes "/forms/assign-reviewer.html"
		String formPath = formKey
				.replace("embedded:", "")
				.replace("app:", (contextPath != null ? contextPath : "") + "/")
				.replaceAll("^(\\/+|([^/]))", "/$2")
				.replaceAll("\\/\\/+", "/");
		
		// Security: Only allow HTML files
		if (!formPath.toLowerCase().endsWith(".html") && !formPath.toLowerCase().endsWith(".htm")) {
			throw new AccessDeniedException("Only HTML files are allowed. Form path: " + formPath);
		}
		
		// Build base URL from user's engine ID
		String baseUrl = null;
		String engineId = user.getEngine();
		
		if (engineId != null && !engineId.isEmpty() && engineId.contains("|")) {
			// Parse the engine ID format: "url|path|engineName"
			String[] parts = engineId.split("\\|", 3);
			if (parts.length == 3) {
				baseUrl = parts[0];
			}
		} else {
			// Use default configured engine URL for legacy format or default engine
			baseUrl = cibsevenUrl;
		}
		
		if (baseUrl == null) {
			throw new SystemException("Cannot determine engine URL");
		}
		
		// Remove trailing slash from base URL and ensure formPath starts with /
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		if (!formPath.startsWith("/")) {
			formPath = "/" + formPath;
		}
		
		// Build full URL
		String fullUrl = baseUrl + formPath;
		
		try {
			// Fetch the form content from the engine
			ResponseEntity<String> response = restTemplate.getForEntity(fullUrl, String.class);
			
			// Return the HTML content with appropriate headers
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8");
			
			return ResponseEntity
					.status(response.getStatusCode())
					.headers(headers)
					.body(response.getBody());
		} catch (Exception e) {
			throw new SystemException("Error fetching form from URL: " + fullUrl + " - " + e.getMessage(), e);
		}
	  }
}
