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

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.rest.model.Filter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController("WebclientFilterService") @RequestMapping("${services.basePath:/services/v1}" + "/filter")
public class FilterService extends BaseService{
	
	@Operation(
			summary = "Get collections of filters",
			description = "Get all filters" + "<br>" +
			"<strong>Return: Collection filters fetched in the search")
	@RequestMapping(method = RequestMethod.GET)
	public Collection<Filter> findFilter(
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.FILTER, PermissionConstants.READ_ALL);
		return bpmProvider.findFilters(user);
	}
	
	@Operation(
			summary = "Create filter",
			description = "Request body: A JSON object with the following properties: id, resourceType, name, owner, query and properties" + "<br>" +
			"<strong>Return: Filter")
	@RequestMapping(method = RequestMethod.POST)
	public Filter createFilter(
			@RequestBody Filter filter,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.FILTER, PermissionConstants.CREATE_ALL);
		return bpmProvider.createFilter(filter, user);
	}
	
	@Operation(
			summary = "Update filter",
			description = "Request body: A JSON object with the following properties: id, resourceType, name, owner, query and properties" + "<br>" +
			"<strong>Return: void")
	@RequestMapping(method = RequestMethod.PUT)
	public void updateFilter(
			@RequestBody Filter filter,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.FILTER, PermissionConstants.UPDATE_ALL);
		bpmProvider.updateFilter(filter, user);
	}
	
	@Operation(
			summary = "Delete filter",
			description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Filter not found")
	@RequestMapping(value = "/{filterId}", method = RequestMethod.DELETE)
	public void deleteFilter(
			@Parameter(description = "Filter Id") @PathVariable String filterId,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.FILTER, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteFilter(filterId, user);
	}
	
}
