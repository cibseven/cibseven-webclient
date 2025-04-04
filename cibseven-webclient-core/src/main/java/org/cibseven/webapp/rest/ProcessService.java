package org.cibseven.webapp.rest;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.Data;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.exception.AnonUserBlockedException;
import org.cibseven.webapp.exception.ApplicationException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.logger.TaskLogger;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.ActivityInstance;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.rest.model.DeploymentResource;
import org.cibseven.webapp.rest.model.EventSubscription;
import org.cibseven.webapp.rest.model.Message;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.ProcessInstance;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.ProcessStatistics;
import org.cibseven.webapp.rest.model.StartForm;
import org.cibseven.webapp.rest.model.Variable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;

import static org.cibseven.webapp.auth.SevenAuthorizationUtils.*;

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController @RequestMapping("${services.basePath:/services/v1}" + "/process")
public class ProcessService extends BaseService implements InitializingBean {
	
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("ProcessService expects a BpmProvider");
	}
	
	@Operation(
			summary = "Get all processes",
			description = "<strong>Return: Collection of processes")
	@ApiResponse(responseCode = "400", description = "There is at least one invalid parameter value")
	@RequestMapping(method = RequestMethod.GET)
	public Collection<Process> findProcesses(Locale loc, CIBUser user) {
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.findProcesses(user);
	}
	
	@Operation(
			summary = "Get all processes with number of incidents and process instances",
			description = "<strong>Return: Collection of processes with number of incidents and process instances")
	@ApiResponse(responseCode = "400", description = "There is at least one invalid parameter value")
	@RequestMapping(value = "/extra-info", method = RequestMethod.GET)
	public Collection<Process> findProcessesWithInfo(Locale loc, CIBUser user) {
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.findProcessesWithInfo(user);
	}
	
	@Operation(
			summary = "Get processes with filters",
			description = "Request body: Filters to be applied" + "<br>" + 
			"<strong>Return: Collection of processes")
	@ApiResponses({
		@ApiResponse(responseCode = "400", description = "There is at least one invalid parameter value"),
		@ApiResponse(responseCode = "404", description = "Process not found")
	})
	@RequestMapping(method = RequestMethod.POST)
	public Collection<Process> findProcessesWithFilters(
			@RequestBody Optional<String> filters,
			Locale loc, CIBUser user) {
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.findProcessesWithFilters(filters.orElse(""), user);
	}
	
	@Operation(
			summary = "Get process with a specific key",
			description = "<strong>Return: Process")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/{key}", method = RequestMethod.GET)
	public Process findProcessByDefinitionKey(
			@Parameter(description = "Process definition key") @PathVariable String key,
			Locale loc, HttpServletRequest request) {
		CIBUser user = checkAuthorization(request, false, true);
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.findProcessByDefinitionKey(key, user);
	}
	
	@Operation(
			summary = "Get processes versions with a specific key",
			description = "<strong>Return: Collections of processes")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "process-definition/versions/{key}", method = RequestMethod.GET)
	public Collection<Process> findProcessVersionsByDefinitionKey(
			@Parameter(description = "Process definition key") @PathVariable String key,
			@RequestParam Optional<Boolean> lazyLoad, Locale loc, CIBUser user) {
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.findProcessVersionsByDefinitionKey(key, lazyLoad, user);
	}
	
	@Operation(
			summary = "Get process with a specific Id",
			description = "<strong>Return: Process")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/process-definition-id/{id}", method = RequestMethod.GET)
	public Process findProcessById(
			@Parameter(description = "Process definition Id") @PathVariable String id,
			@RequestParam Optional<Boolean> extraInfo, Locale loc, CIBUser user) {
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.findProcessById(id, extraInfo, user);
	}

	@Operation(
			summary = "Get processes by filter",
			description = "Request body: Filters to be applied" + "<br>" +
			"<strong>Return: Collection of processes")
	@ApiResponses({
		@ApiResponse(responseCode = "400", description = "There is at least one invalid parameter value"),
		@ApiResponse(responseCode = "404", description = "Process not found")
	})
	@RequestMapping(value = "/instances", method = RequestMethod.POST)
	public Collection<ProcessInstance> findCurrentProcessesInstances(
			@RequestBody Map<String, Object> data,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findCurrentProcessesInstances(data, user);
	}
	
	@Operation(
			summary = "Get processes instances with a specific process key",
			description = "<strong>Return: Collection of processes instances")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/instances/by-process-key/{key}", method = RequestMethod.GET)
	public Collection<ProcessInstance> findProcessesInstances(
			@Parameter(description = "Process instance key") @PathVariable String key,
			Locale loc, CIBUser user) {
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findProcessesInstances(key, user);
	}
	
	@Operation(
			summary = "Get activity that belongs to a process instance",
			description = "<strong>Return: Activity")
	@ApiResponse(responseCode = "404", description = "Activity not found")
	@RequestMapping(value = "/activity/by-process-instance/{processInstanceId}", method = RequestMethod.GET)
	public ActivityInstance findActivityInstance(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			Locale loc, CIBUser user) {
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findActivityInstance(processInstanceId, user);
	}
	
	@Operation(
			summary = "Get called processes definitions from a process instance",
			description = "<strong>Return: Processes")
	@ApiResponse(responseCode = "404", description = "Processes definitions not found")
	@RequestMapping(value = "/called-process-definitions/{processDefinitionId}", method = RequestMethod.GET)
	public Collection<Process> findCalledProcessDefinitions(
			@Parameter(description = "Process definition id") @PathVariable String processDefinitionId,
			Locale loc, CIBUser user) {
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.findCalledProcessDefinitions(processDefinitionId, user);
	}

	@Operation(
			summary = "Get process diagram",
			description = "A XML that contains the specification to render the diagram" + "<br>" + 
			"<strong>Return: Process diagram XML that contains diagram to be render")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/{processId}/diagram", method = RequestMethod.GET) @CrossOrigin
	public ProcessDiagram fetchDiagram(
			@Parameter(description = "Process definition Id") @PathVariable String processId,
			Locale loc, CIBUser user) {
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.fetchDiagram(processId, user);
	}
	
	// Legacy
	@RequestMapping(value = "/{key}/start-v", method = RequestMethod.POST)
	public ProcessStart startProcessLegacy(@PathVariable String key, @RequestBody Map<String, Object> data, Locale loc, HttpServletRequest rq) {
		return startProcess(key, data, loc, rq);
	}	
	
	@Operation(
			summary = "Start process",
			description = "Request body: Variables to start process" + "<br>" + 
			"<strong>Return: Information about the process started")
	@ApiResponses({
		@ApiResponse(responseCode = "404", description = "Process not found"),
		@ApiResponse(responseCode = "412", description = "Expression cannot be evaluated"),
		@ApiResponse(responseCode = "415", description = "Unsupported value type")
	})
	@RequestMapping(value = "/{key}/start", method = RequestMethod.POST)
	public ProcessStart startProcess(
			@Parameter(description = "Process to be started") @PathVariable String key,
			@RequestBody Map<String, Object> data,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, true);
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.CREATE_ALL);
		return bpmProvider.startProcess(key, data, user);
	}
	
	@Operation(
			summary = "Get start-form to start a process",
			description = "<strong>Return: Startform variables and formReference")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/{processDefinitionId}/start-form", method = RequestMethod.GET)
	public StartForm fetchStartForm(
			@Parameter(description = "Process to be started") @PathVariable String processDefinitionId,
			Locale loc, HttpServletRequest request) {
		CIBUser user = checkAuthorization(request, false, true);
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		if (user.isAnonUser())
			checkSpecificProcessRights(user, findProcessById(processDefinitionId, Optional.of(false), loc, user).getKey());
		return bpmProvider.fetchStartForm(processDefinitionId, user);
	}
	
	@Operation(
			summary = "Download BPMN",
			description = "<strong>Return: BPMN")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/{processId}/data", method = RequestMethod.GET)
	public ResponseEntity<InputStreamSource> downloadBpmn(
			@Parameter(description = "Process definition Id") @PathVariable String processId,
			@Parameter(description = "Name of the file containing the BPMN") @RequestParam String filename,
			@Parameter(description = "Token") @RequestParam String token,
			HttpServletRequest rq, HttpServletResponse res)	{		
		try {
			rq = new HeaderModifyingRequestWrapper(rq, token);
			CIBUser user = checkAuthorization(rq, true, false);
			checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
			try {
				return response(bpmProvider.downloadBpmn(processId, filename, user));
			} catch (AuthenticationException x) {
				res.sendRedirect("../../../#/flow/auth/processes");
				throw x;
			}
		} catch (IOException x) {
			throw new SystemException(x);
		}
	}
	
	@Operation(
			summary = "Activate/Suspend process instance by Id",
			description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/instance/{processInstanceId}/suspend", method = RequestMethod.PUT)
	public void suspendProcessInstance(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			@Parameter(description = "If true, the process instance will be activated"
					+ "<br>If false, the process will be suspended") @RequestParam Boolean suspend,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.UPDATE_ALL);
		bpmProvider.suspendProcessInstance(processInstanceId, suspend, user);
	}
	
	@Operation(
			summary = "Delete process instance by Id",
			description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/instance/{processInstanceId}/delete", method = RequestMethod.DELETE)
	public void deleteProcessInstance(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
		checkPermission(user, PROCESS_INSTANCE, List.of("ALL", "DELETE"));
		bpmProvider.deleteProcessInstance(processInstanceId, user);
	}
	
	@Operation(
			summary = "Delete process definition by Id",
			description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/{id}/delete", method = RequestMethod.DELETE)
	public void deleteProcessDefinition(
			@Parameter(description = "Process definition Id") @PathVariable String id,
			@RequestParam Optional<Boolean> cascade,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteProcessDefinition(id, cascade, user);
	}
	
	@Operation(
			summary = "Activate/Suspend process by Id",
			description = "<strong>Return: void")
	@ApiResponses({
		@ApiResponse(responseCode = "404", description = "Process not found"),
		@ApiResponse(responseCode = "415", description = "Unsupported value type")
	})
	@RequestMapping(value = "/{processId}/suspend", method = RequestMethod.PUT)
	public void suspendProcessDefinition(
			@Parameter(description = "Process definition Id") @PathVariable String processId,
			@Parameter(description = "If true, the process instance will be activated"
					+ "<br>If false, the process will be suspended") @RequestParam Boolean suspend,
			@Parameter(description = "Whether to activate or suspend also all process instances of the given process definition") @RequestParam Boolean includeProcessInstances,
			@Parameter(description = "The date on which the given process definition will be activated or suspended"
					+ "<br>ej. 2013-01-23T14:42:45. yyyy-MM-dd'T'HH:mm:ss"
					+ "<br>If null, the suspension state of the given process definition is updated immediately") @RequestParam Optional<String> executionDate,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.UPDATE_ALL);
		bpmProvider.suspendProcessDefinition(processId, suspend, includeProcessInstances, executionDate.orElse(null), user);
	}

	@Operation(
			summary = "Get incidents for a specific process",
			description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/{key}/incidents", method = RequestMethod.GET)
	public void fetchIncidents(
			@Parameter(description = "Process key") @PathVariable String key,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		bpmProvider.fetchIncidents(key, user);
	}

	@Operation(
			summary = "Deploy process BPMN",
			description = "<strong>Return: Deployment")
	@RequestMapping(value = "/deployment/create", method = RequestMethod.POST)
	public Deployment deployBpmn(
			@Parameter(description = "Metadata of the diagram to be deployed (deployment-name, deployment-source, deploy-changed-only)") @RequestParam MultiValueMap<String, Object> data,
			@Parameter(description = "Diagram to be deployed") @RequestParam MultiValueMap<String, MultipartFile> file,
			HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
    	checkPermission(user, DEPLOYMENT, PermissionConstants.CREATE_ALL);
		return bpmProvider.deployBpmn(data, file, user);
	}

	@Operation(
			summary = "Check deployment",
			description = "<strong>Return: Boolean (always true)")
	@RequestMapping(value = "/deployment", method = RequestMethod.GET)
	public boolean checkDeployBpmn(HttpServletRequest rq) {
		checkAuthorization(rq, true, false);
		return true;
	}

	@Operation(
			summary = "Get all deployments of a given deployment",
			description = "<strong>Return: Collection of deployments")
	@RequestMapping(value = "/deployments", method = RequestMethod.GET)
	public Collection<Deployment> findDeployments(CIBUser user) {
    	checkPermission(user, DEPLOYMENT, PermissionConstants.READ_ALL);
		return bpmProvider.findDeployments(user);
	}
	
	@Operation(
			summary = "Get all deployment resources of a given deployment",
			description = "<strong>Return: Collection of deployment resources")
	@ApiResponse(responseCode = "404", description = "Deployment not found")
	@RequestMapping(value = "/deployments/{deploymentId}/resources", method = RequestMethod.GET)
	public Collection<DeploymentResource> findDeploymentResources(
			@Parameter(description = "Deployment Id") @PathVariable String deploymentId,
			CIBUser user) {
		checkPermission(user, DEPLOYMENT, PermissionConstants.READ_ALL);
		return bpmProvider.findDeploymentResources(deploymentId, user);
	}
	
	@Operation(
			summary = "Get binary content of a deployment resource for the given deployment by id",
			description = "<strong>Return: Resource data")
	@ApiResponse(responseCode = "404", description = "Deployment or Resource not found")
	@RequestMapping(value = "/deployments/{deploymentId}/resources/{resourceId}/data", method = RequestMethod.GET)
	public ResponseEntity<InputStreamSource> fetchDataFromDeploymentResource(
			@Parameter(description = "Deployment Id") @PathVariable String deploymentId,
			@Parameter(description = "Resource Id") @PathVariable String resourceId,
			@Parameter(description = "Name of the resource file") @RequestParam String filename,
			@Parameter(description = "Token") @RequestParam String token,
			HttpServletRequest rq, HttpServletResponse res) {
		try {
			rq = new HeaderModifyingRequestWrapper(rq, token);
			try {
				return response(bpmProvider.fetchDataFromDeploymentResource(rq, deploymentId, resourceId, filename));
			} catch (ApplicationException x) {
				res.sendRedirect("../../../#/flow/auth/deployments/" + deploymentId + "/resources/" + resourceId);
				throw x;
			}
		} catch (IOException x) {
			throw new SystemException(x);
		}
	}
	
	@Operation(
			summary = "Delete deployment by Id",
			description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Deployment not found")
	@RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.DELETE)
	public void deleteDeployment(
			@Parameter(description = "Deployment Id") @PathVariable String deploymentId,
			@Parameter(description = "Delete in cascade?") @RequestParam Boolean cascade,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
		checkPermission(user, DEPLOYMENT, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteDeployment(deploymentId, cascade, user);
	}
	
	@Operation(
			summary = "Correlates a message to the process engine to either trigger a message start event or an intermediate message catching event",
			description = "<strong>Return: Collection of messages")
	@RequestMapping(value = "/message", method = RequestMethod.POST)
	public Collection<Message> correlateMessage(
			@Parameter(description = "Variables to start process") @RequestBody Map<String, Object> data,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkCockpitRights(user);
		checkPermission(user, MESSAGE, PermissionConstants.CREATE_ALL);
		return bpmProvider.correlateMessage(data, user);
	}
	
	@Operation(
			summary = "Submit form with variables",
			description = "Request body: Variables to submit" + "<br>" +
			"<strong>Return: Information about the process started")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/{key}/submit-form", method = RequestMethod.POST)
	public ProcessStart submitForm(
			@Parameter(description = "Process to be started") @PathVariable String key,
			@RequestBody Map<String, Object> data,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
	    checkPermission(user, PROCESS_INSTANCE, PermissionConstants.CREATE_ALL);
		return bpmProvider.submitForm(key, data, user);
	}
	
	@Operation(
			summary = "Modify a variable in the process instance",
			description = "Request body: Data to be updated" + "<br>" +
			"<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Execution not found")
	@RequestMapping(value = "/execution/{executionId}/localVariables", method = RequestMethod.POST)
	public void modifyVariableByExecutionId(
			@Parameter(description = "Id of the execution") @PathVariable String executionId,
			@RequestBody Map<String, Object> data,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
        checkPermission(user, PROCESS_INSTANCE, PermissionConstants.UPDATE_ALL);
		bpmProvider.modifyVariableByExecutionId(executionId, data, user);
	}
	
	@Operation(
			summary = "Modify a variable data in the process instance",
			description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Execution not found")
	@RequestMapping(value = "/execution/{executionId}/localVariables/{variableName}/data", method = RequestMethod.POST)
	public void modifyVariableDataByExecutionId(
			@Parameter(description = "Execution Id") @PathVariable String executionId,
			@Parameter(description = "Name of the variable") @PathVariable String variableName,
			@Parameter(description = "Data to be updated") @RequestParam MultipartFile file,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
        checkPermission(user, PROCESS_INSTANCE, PermissionConstants.UPDATE_ALL);
		bpmProvider.modifyVariableDataByExecutionId(executionId, variableName, file, user);
	}
	
	@Operation(
			summary = "Get a variable data in the process instance",
			description = "<strong>Return: Data")
	@ApiResponse(responseCode = "404", description = "Execution not found")
	@RequestMapping(value = "/execution/{executionId}/localVariables/{variableName}/data", method = RequestMethod.GET)
	public ResponseEntity<byte[]> fetchVariableDataByExecutionId(
			@Parameter(description = "Execution Id") @PathVariable String executionId,
			@Parameter(description = "Name of the variable") @PathVariable String variableName,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
        checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.fetchVariableDataByExecutionId(executionId, variableName, user);
	}
	
	@Operation(
			summary = "Delete a variable in the process instance")
	@ApiResponse(responseCode = "404", description = "Execution not found")
	@RequestMapping(value = "/execution/{executionId}/localVariables/{variableName}", method = RequestMethod.DELETE)
	public void deleteVariableByExecutionId(
			@Parameter(description = "Id of the execution") @PathVariable String executionId,
			@PathVariable String variableName,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteVariableByExecutionId(executionId, variableName, user);
	}
	
	@Operation(
			summary = "Get variables from a process instance",
			description = "<strong>Return: Collection of variables")
	@ApiResponse(responseCode = "404", description = "Process instance not found")
	@RequestMapping(value = "/variable-instance/process-instance/{processInstanceId}/variables", method = RequestMethod.GET)
	public Collection<Variable> fetchProcessInstanceVariables(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			@Parameter(description = "Deserialize value") @RequestParam Optional<Boolean> deserialize,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
        checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.fetchProcessInstanceVariables(processInstanceId, user, deserialize);
	}
	
	@Operation(
			summary = "Get statistics from a process",
			description = "<strong>Return: Collection of processes instance statistics")
	@ApiResponse(responseCode = "404", description = "Process instance not found")
	@RequestMapping(value = "/process-definition/{processId}/statistics", method = RequestMethod.GET)
	public Collection<ProcessStatistics> findProcessStatistics(
			@Parameter(description = "Process Id") @PathVariable String processId,
			Locale loc, CIBUser user) {
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.findProcessStatistics(processId, user);
	}
	
	//Requested by OFDKA
	@Operation(
			summary = "Get process instance with a specific process instance id",
			description =  "Requested by OFDKA" + "<br>" +
			"<strong>Return: Process instance")
	@ApiResponse(responseCode = "404", description = "Process instance not found")
	@RequestMapping(value = "/process-instance/{processInstanceId}", method = RequestMethod.GET)
	public ProcessInstance findProcessInstance(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return sevenProvider.findProcessInstance(processInstanceId, user);
	}
	
	//Requested by OFDKA
	@Operation(
			summary = "Get data of a variable of a given process instance by Id",
			description = "Requested by OFDKA" + "<br>" +
			"<strong>Return: Variable data")
	@ApiResponse(responseCode = "404", description = "Process instance not found")
	@RequestMapping(value = "/process-instance/{processInstanceId}/variables/{variableName}/data", method = RequestMethod.GET)
	public ResponseEntity<byte[]> findProcessInstanceVariableData(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			@Parameter(description = "Varaible name") @PathVariable String variableName,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkCockpitRights(user);
        checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return sevenProvider.fetchProcessInstanceVariableData(processInstanceId, variableName, user);
	}
		
	@Operation(
			description = "Get a variable of a given process instance by Id" +
			"<strong>Return: Variables")
	@ApiResponse(responseCode = "404", description = "Process instance not found")
	@RequestMapping(value = "/process-instance/{processInstanceId}/variables/{variableName}", method = RequestMethod.GET)
	public Variable findProcessInstanceVariable(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			@Parameter(description = "Variable name") @PathVariable String variableName,
			@Parameter(description = "Deserialize value") @RequestParam(required = false) String deserializeValue,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkCockpitRights(user);
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return sevenProvider.fetchProcessInstanceVariable(processInstanceId, variableName, deserializeValue, user);
	}
	
	@Operation(
			description = "Get a variable of a given process instance by Id" +
			"<strong>Return: Variables")
	@ApiResponse(responseCode = "404", description = "Process instance not found")
	@RequestMapping(value = "/process-instance/{processInstanceId}/chat-comments", method = RequestMethod.GET)
	public Variable fetchChatComments(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			@Parameter(description = "Process definition Id") @RequestParam String processDefinitionKey,
			@Parameter(description = "Deserialize value") @RequestParam(required = false) String deserialize,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		
		// TODO: Check the permissiosn, but not considered the groups, needs to be checked.
		// checkSpecificProcessRights(user, processDefinitionKey);
		try {
			return sevenProvider.fetchProcessInstanceVariable(processInstanceId, "chatComments", deserialize, user);	
		} catch(NoObjectFoundException e) {
			return null;
		}
		
	}
	
	@Operation(
			description = "Get a variable of a given process instance by Id" +
			"<strong>Return: Variables")
	@ApiResponse(responseCode = "404", description = "Process instance not found")
	@RequestMapping(value = "/process-instance/{processInstanceId}/status-dataset", method = RequestMethod.GET)
	public Variable fetchStatusDataset(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			@Parameter(description = "Process definition Id") @RequestParam String processDefinitionKey,
			@Parameter(description = "Deserialize value") @RequestParam(required = false) String deserialize,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		// TODO: Check the permissiosn, but not considered the groups, needs to be checked.
		// checkSpecificProcessRights(user, processDefinitionKey);
		try {
			return sevenProvider.fetchProcessInstanceVariable(processInstanceId, "_statusDataset", deserialize, user);	
		} catch(NoObjectFoundException e) {
			return null;
		}
		
	}

	
	@RequestMapping(value = "/process-instance/{processInstanceId}/submit-variables", method = RequestMethod.POST)
	public ResponseEntity<String> submitVariables(@RequestBody List<Variable> variables, 
			@PathVariable String processInstanceId, @RequestParam Optional<String> processDefinitionKey, HttpServletRequest rq) {
		
		CIBUser userAuth = (CIBUser) checkAuthorization(rq, true, false);
        checkPermission(userAuth, PROCESS_INSTANCE, PermissionConstants.UPDATE_ALL);
		bpmProvider.submitVariables(processInstanceId, variables, userAuth, processDefinitionKey.orElse("cib flow"));
		return new ResponseEntity<>("ok", new HttpHeaders(), HttpStatus.OK);
		
	}	
	
	ResponseEntity<InputStreamSource> response(Data ds) {
		HttpHeaders headers = new HttpHeaders(); // http://stackoverflow.com/questions/5673260/downloading-a-file-from-spring-controllers
		headers.setContentType(org.springframework.http.MediaType.valueOf(ds.getContentType())); // better with Firefox, Chrome worked fine without
		headers.setContentDispositionFormData("attachment", ds.getName());
		if (ds.getSize() != -1)
			headers.setContentLength(ds.getSize());
		return new ResponseEntity<>(ds.getInput(), headers, HttpStatus.OK);
	}
	
	//Requested by OFDKA
	@Operation(
			summary = "Get event subscriptions that fulfill given parameters",
			description = "Requested by OFDKA" + "<br>" +
			"<strong>Return: Collection of event subscriptions")
	@RequestMapping(value = "/event-subscriptions", method = RequestMethod.GET)
	public Collection<EventSubscription> getEventSubscriptions(
			@Parameter(description = "Process instance Id") @RequestParam Optional<String> processInstanceId,
			@Parameter(description = "Event type") @RequestParam Optional<String> eventType,
			@Parameter(description = "Event name") @RequestParam Optional<String> eventName,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, EVENT_SUBSCRIPTION, PermissionConstants.READ_ALL);
		return sevenProvider.getEventSubscriptions(processInstanceId, eventType, eventName, user);
	}
	
	@Operation(
			summary = "Update process definition history time to live",
			description = "<strong>Return: void")
	@RequestMapping(value = "/{id}/history-time-to-live", method = RequestMethod.PUT)
	public void updateHistoryTimeToLive(@PathVariable String id, 
			@RequestBody Map<String, Object> data, Locale loc, CIBUser user) {
		checkCockpitRights(user);
		checkPermission(user, PROCESS_DEFINITION, PermissionConstants.UPDATE_ALL);
		bpmProvider.updateHistoryTimeToLive(id, data, user);
	}
	
	
	@Operation(summary = "Create a variable in the process instance")
	@ApiResponse(responseCode = "404", description = "Execution not found")
	@RequestMapping(value = "/execution/{executionId}/localVariables/{varName}", method = RequestMethod.PUT)
	public void putLocalExecutionVariable(
			@Parameter(description = "Id of the execution") @PathVariable String executionId,
			@Parameter(description = "Variable name") @PathVariable String varName,
			@RequestBody Map<String, Object> data,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
        checkPermission(user, PROCESS_INSTANCE, PermissionConstants.UPDATE_ALL);
		bpmProvider.putLocalExecutionVariable(executionId, varName, data, user);
	}
	
	  @Consumes(MediaType.APPLICATION_JSON)
	  @RequestMapping(value = "{processDefinitionId}/submit-startform-variables", method = RequestMethod.POST)
	  public ResponseEntity<ProcessStart> submitStartFormVariables(
	      @PathVariable String processDefinitionId, 
	      @RequestBody List<Variable> formResult, 
	      @RequestParam Optional<String> assignee, HttpServletRequest rq) {
	    CIBUser user;
	    try {
	      user = (CIBUser) baseUserProvider.authenticateUser(rq);
	    } catch(AnonUserBlockedException e) {
	      user = (CIBUser) e.getUser();
	      Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
	      hasSpecificProcessRights(authorizations, processDefinitionId);
	    }
	    String[] processDefinitionIdChunks = processDefinitionId.split(":");
	    String processDefinitionUuid = processDefinitionIdChunks.length >= 3 ? processDefinitionIdChunks[2] : processDefinitionIdChunks[0];
	    TaskLogger logger = new TaskLogger(processDefinitionId, processDefinitionUuid, processDefinitionUuid, processDefinitionUuid);
	    
	    logger.info("[INFO] Start process with key=" + processDefinitionId + " (" + getClass().getSimpleName() + ")");
	    ProcessStart processStart = bpmProvider.submitStartFormVariables(processDefinitionId, formResult, user);
	    logger.info("[INFO] Started process with key=" + processDefinitionId + " (" + getClass().getSimpleName() + ")");
	    return new ResponseEntity<>(processStart, new HttpHeaders(), HttpStatus.OK);
	  }
	  
	  @Consumes(MediaType.APPLICATION_JSON)
	  @RequestMapping(value = "{processInstanceId}/variables", method = RequestMethod.POST)
	  public ResponseEntity<String> saveVariable(@PathVariable String processInstanceId, @RequestBody List<Variable> variables, HttpServletRequest rq) {
	    try {
	      CIBUser userAuth = (CIBUser) baseUserProvider.authenticateUser(rq);
	      checkPermission(userAuth, PROCESS_INSTANCE, PermissionConstants.UPDATE_ALL);
	      bpmProvider.saveVariableInProcessInstanceId(processInstanceId, variables, userAuth);    
	      return new ResponseEntity<>("ok", new HttpHeaders(), HttpStatus.OK);
	    } catch (Exception e) {
	      if (e instanceof NoObjectFoundException) return generateErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
	      else return generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
	    }
	  }
	  
	  @Consumes(MediaType.APPLICATION_JSON)
	  @RequestMapping(value = "{processInstanceId}/variable/{variableName}", method = RequestMethod.GET)
	  public ResponseEntity<Variable> fetchVariableByProcessInstanceId(@PathVariable String processInstanceId, @PathVariable String variableName, HttpServletRequest rq) {
	    try {
	      CIBUser userAuth = (CIBUser) baseUserProvider.authenticateUser(rq);
	      checkPermission(userAuth, PROCESS_INSTANCE, PermissionConstants.READ_ALL);
	      Variable variable = bpmProvider.fetchVariableByProcessInstanceId(processInstanceId, variableName, userAuth);    
	      return new ResponseEntity<>(variable, new HttpHeaders(), HttpStatus.OK);
	    } catch (Exception e) {
	      if (e instanceof NoObjectFoundException) return generateErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
	      else return generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
	    }
	  }
	  
	  @SuppressWarnings("unchecked")
	  protected <T> ResponseEntity<T> generateErrorResponse(String message, HttpStatus status) {
	    ResponseEntity<?> response = new ResponseEntity<>(message, status);
	    return (ResponseEntity<T>) response;
	  }
	
}