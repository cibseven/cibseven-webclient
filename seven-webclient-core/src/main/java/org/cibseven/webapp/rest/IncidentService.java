package org.cibseven.webapp.rest;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenUserProvider;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Incident;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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
	@ApiResponse(responseCode= "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode= "401", description = "Unauthorized")
})
@RestController @RequestMapping("${services.basePath:/services/v1}" + "/incident")
public class IncidentService extends BaseService implements InitializingBean {

	@Autowired BpmProvider bpmProvider;
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("AdminService expects a BpmProvider");
	}	
	
	@Operation(
			summary = "Get number of incidents",
			description = "<strong>Return: Number of incidents")
	@ApiResponse(responseCode= "404", description = "Incident not found")
	@RequestMapping(value = "/count", method = RequestMethod.GET)
	public Long countIncident(
			@Parameter(description = "Incident Id") @RequestParam Optional<String> incidentId,
			@Parameter(description = "Incidents that belong to the given incident type"
					+ "<br>See the User Guide for a list of incident types") @RequestParam Optional<String> incidentType,
			@Parameter(description = "Incidents that have the given incident message") @RequestParam Optional<String> incidentMessage,
			@Parameter(description = "Incidents that belong to a process definition with the given Id") @RequestParam Optional<String> processDefinitionId,
			@Parameter(description = "Incidents that belong to a process definition with the given Key") @RequestParam Optional<String> processDefinitionKeyIn,
			@Parameter(description = "Incidents that belong to a process instance with the given Id") @RequestParam Optional<String> processInstanceId,
			@Parameter(description = "Incidents that belong to an execution with the given Id") @RequestParam Optional<String> executionId,
			@Parameter(description = "Incidents that belong to an activity with the given Id") @RequestParam Optional<String> activityId,
			@Parameter(description = "Incidents that have the given incident Id as cause incident") @RequestParam Optional<String> causeIncidentId,
			@Parameter(description = "Incidents that have the given incident Id as root cause incident") @RequestParam Optional<String> rootCauseIncidentId,
			@Parameter(description = "Incidents that have the given parameter set as configuration") @RequestParam Optional<String> configuration,
			@Parameter(description = "Incidents that have one of the given comma-separated tenant Ids") @RequestParam Optional<String> tenantIdIn,
			@Parameter(description = "Incidents that have one of the given comma-separated job definition Ids") @RequestParam Optional<String> jobDefinitionIdIn,
			@Parameter(description = "Incidents that have one of the given name") @RequestParam Optional<String> name,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		return sevenProvider.countIncident(incidentId, incidentType, incidentMessage, processDefinitionId, processDefinitionKeyIn, processInstanceId, executionId, activityId,
				causeIncidentId, rootCauseIncidentId, configuration, tenantIdIn, jobDefinitionIdIn, name, user);
	}
	
	@Operation(
			summary = "Get incident/s",
			description = "<strong>Return: Collection of incident/s")
	@ApiResponse(responseCode= "404", description = "Incident not found")
	@RequestMapping(method = RequestMethod.GET)
	public Collection<Incident> findIncident(
			@Parameter(description = "Incident Id") @RequestParam Optional<String> incidentId,
			@Parameter(description = "Incidents that belong to the given incident type"
					+ "<br>See the User Guide for a list of incident types") @RequestParam Optional<String> incidentType,
			@Parameter(description = "Incidents that have the given incident message") @RequestParam Optional<String> incidentMessage,
			@Parameter(description = "Incidents that belong to a process definition with the given Id") @RequestParam Optional<String> processDefinitionId,
			@Parameter(description = "Incidents that belong to a process definition with the given Key") @RequestParam Optional<String> processDefinitionKeyIn,
			@Parameter(description = "Incidents that belong to a process instance with the given Id") @RequestParam Optional<String> processInstanceId,
			@Parameter(description = "Incidents that belong to an execution with the given Id") @RequestParam Optional<String> executionId,
			@Parameter(description = "Incidents that belong to an activity with the given Id") @RequestParam Optional<String> activityId,
			@Parameter(description = "Incidents that have the given incident Id as cause incident") @RequestParam Optional<String> causeIncidentId,
			@Parameter(description = "Incidents that have the given incident Id as root cause incident") @RequestParam Optional<String> rootCauseIncidentId,
			@Parameter(description = "Incidents that have the given parameter set as configuration") @RequestParam Optional<String> configuration,
			@Parameter(description = "Incidents that have one of the given comma-separated tenant Ids") @RequestParam Optional<String> tenantIdIn,
			@Parameter(description = "Incidents that have one of the given comma-separated job definition Ids") @RequestParam Optional<String> jobDefinitionIdIn,	
			Locale loc, HttpServletRequest rq) 
	{
		CIBUser user = checkAuthorization(rq, true, false);
		return sevenProvider.findIncident(incidentId, incidentType, incidentMessage, processDefinitionId, processDefinitionKeyIn, processInstanceId, executionId, activityId,
				causeIncidentId, rootCauseIncidentId, configuration, tenantIdIn, jobDefinitionIdIn, user);
	}
		
	@Operation(
			summary = "Get stack trace",
			description = "<strong>Return: Stacktrace")
	@ApiResponse(responseCode= "404", description = "Job not found")
	@RequestMapping(value = "/{jobId}/stacktrace", method = RequestMethod.GET)
	public String findStacktrace(
			@Parameter(description = "Job Id") @PathVariable String jobId, 
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		return sevenProvider.findStacktrace(jobId, user);
	}
	
	@Operation(
			summary = "Increment job retries by job id",
			description = "<strong>Return: void")
	@ApiResponse(responseCode= "404", description = "Job not found")
	@RequestMapping(value = "/job/{jobId}/retries", method = RequestMethod.PUT)
	public void retryJobByID(
			@Parameter(description = "Job Id") @PathVariable String jobId, 
			@RequestBody Map<String, Object> data, 
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		sevenProvider.retryJobById(jobId, data, user);
	}
	
}
