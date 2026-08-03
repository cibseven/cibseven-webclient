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

import java.util.Arrays;
import java.util.Optional;

import org.cibseven.webapp.auth.AuthorizationChecker;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.UnknownResourceTypeException;
import org.cibseven.webapp.rest.model.AuthorizationCheckResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;

/**
 * Answers whether the calling user holds a permission on a resource.
 *
 * <p>The engine offers the same operation, but only for callers it has authenticated itself, which
 * rules it out for a middleware whose engine-rest may run without an authentication filter. Here the
 * caller is known from its token, so the question can always be answered — for the caller, and only
 * for the caller: asking about somebody else would expose their permissions.</p>
 *
 * <p>Resources the webclient owns are covered as well, most notably the {@code modeler} application
 * resource, because the evaluation runs on the authorizations rather than inside the engine.</p>
 */
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/authorization")
public class AuthorizationCheckService extends BaseService {

	@Autowired
	private AuthorizationChecker authorizationChecker;

	@Operation(
		summary = "Check whether the calling user holds a permission on a resource",
		description = "Evaluates the permission for the authenticated caller." + "<br>"
			+ "With authorization disabled in the engine, nothing is enforced and the answer is always true." + "<br>"
			+ "<strong>Return: the check result, with the isAuthorized flag")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Check performed"),
		@ApiResponse(responseCode = "400", description = "Unknown resource type")
	})
	@GetMapping("/check")
	public AuthorizationCheckResult isUserAuthorized(
			@Parameter(description = "Permission to check, e.g. ACCESS or READ") @RequestParam String permissionName,
			@Parameter(description = "Numeric resource type, e.g. 0 for application") @RequestParam int resourceType,
			@Parameter(description = "Id of the resource, e.g. modeler. Omit to check the wildcard resource")
			@RequestParam Optional<String> resourceId,
			@Parameter(description = "Name of the resource type, echoed back for engine-compatible responses")
			@RequestParam Optional<String> resourceName,
			@NotNull CIBUser user) {
		SevenResourceType type = resourceTypeOf(resourceType);
		String resource = resourceId.orElse(null);
		boolean authorized = authorizationChecker.isAuthorized(user, type, resource, permissionName);
		return new AuthorizationCheckResult(permissionName,
			resourceName.orElseGet(() -> type.name().toLowerCase()), resource, authorized);
	}

	private SevenResourceType resourceTypeOf(int resourceType) {
		return Arrays.stream(SevenResourceType.values())
			.filter(candidate -> candidate.getType() == resourceType)
			.findFirst()
			.orElseThrow(() -> new UnknownResourceTypeException(resourceType));
	}
}
