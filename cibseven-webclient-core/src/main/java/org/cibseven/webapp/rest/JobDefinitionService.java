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
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.JobDefinition;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
		else throw new SystemException("JobDefinitionService expects a BpmProvider");
	}
	
	@Operation(
			summary = "Get job definition/s",
			description = "<strong>Return: Collection of job definition/s</strong>")
	@PostMapping("")
	public Collection<JobDefinition> findJobDefinitions(@RequestBody String params, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, SevenResourceType.JOB_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.findJobDefinitions(params, user);
	}
	
	@Operation(
		    summary = "Suspend job definition",
		    description = "<strong>Suspends or activates a job definition by ID</strong>")
	@ApiResponse(responseCode = "404", description = "Job definition not found")
	@PutMapping("/{jobDefinitionId}/suspend")
	public void suspendJobDefinition(@PathVariable String jobDefinitionId, @RequestBody String params, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, SevenResourceType.JOB_DEFINITION, PermissionConstants.UPDATE_ALL);
        bpmProvider.suspendJobDefinition(jobDefinitionId, params, user);
	}
	
	@Operation(
		    summary = "Override job definition priority",
		    description = "<strong>Override job definition priority by ID</strong>")
	@ApiResponse(responseCode = "404", description = "Job definition not found")
	@PutMapping("/{jobDefinitionId}/job-priority")
	public void overrideJobDefinitionPriority(@PathVariable String jobDefinitionId, @RequestBody String params, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, SevenResourceType.JOB_DEFINITION, PermissionConstants.UPDATE_ALL);
        bpmProvider.overrideJobDefinitionPriority(jobDefinitionId, params, user);
	}
	
	@Operation(
		    summary = "Get job definition",
		    description = "<strong>Return: Job definition from id</strong>")
	@ApiResponse(responseCode = "404", description = "Job definition not found")
	@GetMapping("/{id}")
	public JobDefinition findJobDefinition(@PathVariable String id, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, SevenResourceType.JOB_DEFINITION, PermissionConstants.READ_ALL);
        return bpmProvider.findJobDefinition(id, user);
	}
	
	@Operation(
	    summary = "Retry job by ID",
	    description = "<strong>Retries a job by setting the number of retries for the job with the given ID</strong>")
	@ApiResponse(responseCode = "404", description = "Job not found")
	@PutMapping("/{id}/retries")
	public void retryJobDefinitionById(@PathVariable String id, @RequestBody Map<String, Object> data, HttpServletRequest rq) {
	    CIBUser user = checkAuthorization(rq, true, false);
	    checkPermission(user, SevenResourceType.JOB_DEFINITION, PermissionConstants.UPDATE_ALL);
	    bpmProvider.retryJobDefinitionById(id, data, user);
	}
}
