package org.cibseven.webapp.rest;

import java.util.Collection;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.JobDefinition;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;

@ApiResponses({
	@ApiResponse(responseCode= "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode= "401", description = "Unauthorized")
})
@RestController @RequestMapping("${services.basePath:/services/v1}" + "/job-definition")
public class JobDefinitionService extends BaseService implements InitializingBean {

	@Autowired BpmProvider bpmProvider;
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("AdminService expects a BpmProvider");
	}
	
	@Operation(
			summary = "Get job definition/s",
			description = "<strong>Return: Collection of job definition/s")
	@ApiResponse(responseCode= "404", description = "Job definition not found")
	@RequestMapping(value = "", method = RequestMethod.POST)
	public Collection<JobDefinition> findJobDefinitions(@RequestBody String params, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		return sevenProvider.findJobDefinitions(params, user);
	}
	
	@Operation(
		    summary = "Suspend job definition",
		    description = "<strong>Suspends or activates a job definition by ID</strong>")
	@ApiResponse(responseCode = "404", description = "Job definition not found")
	@RequestMapping(value = "/{jobDefinitionId}/suspend", method = RequestMethod.PUT)
	public void suspendJobDefinition(@PathVariable String jobDefinitionId, @RequestBody String params, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
        sevenProvider.suspendJobDefinition(jobDefinitionId, params, user);
	}
	
	@Operation(
		    summary = "Override job definition priority",
		    description = "<strong>Override job definition priority by ID</strong>")
	@ApiResponse(responseCode = "404", description = "Job definition not found")
	@RequestMapping(value = "/{jobDefinitionId}/job-priority", method = RequestMethod.PUT)
	public void overrideJobDefinitionPriority(@PathVariable String jobDefinitionId, @RequestBody String params, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
        sevenProvider.overrideJobDefinitionPriority(jobDefinitionId, params, user);
	}
	
}
