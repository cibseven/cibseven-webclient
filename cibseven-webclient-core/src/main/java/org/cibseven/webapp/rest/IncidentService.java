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
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Incident;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/incident")
public class IncidentService extends BaseService implements InitializingBean {

	@Autowired
	BpmProvider bpmProvider;
	SevenProvider sevenProvider;

	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else
			throw new SystemException("IncidentService expects a BpmProvider");
	}

	@Operation(summary = "Get number of incidents", description = "<strong>Return: Number of incidents")
	@ApiResponse(responseCode = "404", description = "Incident not found")
	@RequestMapping(value = "/count", method = RequestMethod.GET)
	public Long countIncident(@RequestParam Map<String, Object> params, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return sevenProvider.countIncident(params, user);
	}

	@Operation(summary = "Get incident/s", description = "<strong>Return: Collection of incident/s")
	@ApiResponse(responseCode = "404", description = "Incident not found")
	@RequestMapping(method = RequestMethod.GET)
	public Collection<Incident> findIncident(@RequestParam Map<String, Object> params, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return sevenProvider.findIncident(params, user);
	}

	@Operation(summary = "Get stack trace", description = "<strong>Return: Stacktrace")
	@ApiResponse(responseCode = "404", description = "Job not found")
	@RequestMapping(value = "/{jobId}/stacktrace", method = RequestMethod.GET)
	public String findStacktrace(
			@Parameter(description = "Job Id") @PathVariable String jobId,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		// checkPermission(user, SevenResourceType.JOB_DEFINITION,
		// PermissionConstants.READ_ALL);

		return sevenProvider.findStacktrace(jobId, user);
	}

	@Operation(summary = "Get external task error details", description = "<strong>Return: Error details")
	@ApiResponse(responseCode = "404", description = "External task not found")
	@RequestMapping(value = "/external-task/{externalTaskId}/errorDetails", method = RequestMethod.GET)
	public String findExternalTaskErrorDetails(
			@Parameter(description = "External Task Id") @PathVariable String externalTaskId,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		return sevenProvider.findExternalTaskErrorDetails(externalTaskId, user);
	}

	@Operation(summary = "Get historic external task error details", description = "<strong>Return: Historic error details")
	@ApiResponse(responseCode = "404", description = "Historic external task not found")
	@RequestMapping(value = "/history/external-task/{externalTaskId}/errorDetails", method = RequestMethod.GET)
	public String findHistoricExternalTaskErrorDetails(
			@Parameter(description = "External Task Id") @PathVariable String externalTaskId,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		return sevenProvider.findHistoricExternalTaskErrorDetails(externalTaskId, user);
	}

	@Operation(summary = "Increment job retries by job id", description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Job not found")
	@RequestMapping(value = "/job/{jobId}/retries", method = RequestMethod.PUT)
	public ResponseEntity<Void> retryJobByID(
			@Parameter(description = "Job Id") @PathVariable String jobId,
			@RequestBody Map<String, Object> data,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		// checkPermission(user, SevenResourceType.JOB_DEFINITION,
		// PermissionConstants.UPDATE_ALL);
		sevenProvider.retryJobById(jobId, data, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Retry external task by setting retries", description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "External task not found")
	@RequestMapping(value = "/external-task/{externalTaskId}/retries", method = RequestMethod.PUT)
	public ResponseEntity<Void> retryExternalTask(
			@Parameter(description = "External Task Id") @PathVariable String externalTaskId,
			@RequestBody Map<String, Object> data,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.PROCESS_INSTANCE, PermissionConstants.UPDATE_ALL);
		sevenProvider.retryExternalTask(externalTaskId, data, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Set annotation for incident by id", description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Incident not found")
	@PutMapping("/{incidentId}/annotation")
	public ResponseEntity<Void> setIncidentAnnotation(
			@Parameter(description = "Incident Id") @PathVariable String incidentId,
			@RequestBody Map<String, Object> data,
			Locale locale, HttpServletRequest request) {
		CIBUser user = checkAuthorization(request, true);
		bpmProvider.setIncidentAnnotation(incidentId, data, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}

}
