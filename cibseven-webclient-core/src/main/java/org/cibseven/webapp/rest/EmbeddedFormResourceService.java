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
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Engine-rest compatible identity paths consumed by embedded forms.
 *
 * <p>Embedded forms are rendered by {@code bpm-sdk} (CamSDK), which issues its own calls at
 * runtime against engine-native paths such as {@code group} and {@code user}. The sdk client is
 * pointed at the middleware ({@code servicesBasePath}) rather than at the engine, so those paths
 * have to resolve here. The curated equivalents live under {@code /admin/group} and
 * {@code /admin/user}, which the sdk does not know about.</p>
 *
 * <p>This controller is deliberately a <em>path alias only</em>: the parameters are declared so
 * Spring can bind the request, but every call delegates to {@link AdminService}, so the
 * permission checks, member decoding and provider calls stay single-sourced there and cannot
 * drift. Because the delegate goes through {@code bpmProvider}, these paths work with every
 * provider implementation, REST and direct alike.</p>
 *
 * <p>Deliberately not covered here: the {@code history/task} path the sdk may also request is
 * served by the enterprise edition, which maps it as a licensed feature. Adding a second mapping
 * in this module would make the context fail to start and would expose an ungated equivalent.</p>
 */
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}")
public class EmbeddedFormResourceService extends BaseService {

	private static final Optional<String> NONE = Optional.empty();

	private final AdminService adminService;

	public EmbeddedFormResourceService(AdminService adminService) {
		this.adminService = adminService;
	}

	@Operation(
			summary = "Get groups based on filters (engine-rest compatible path used by embedded forms)",
			description = "Alias of /admin/group at the path bpm-sdk requests." + "<br>"
			+ "<strong>Return: Collection of groups")
	@GetMapping("/group")
	public Collection<UserGroup> findGroups(
			@Parameter(description = "Group Id") @RequestParam Optional<String> id,
			@Parameter(description = "Group name") @RequestParam Optional<String> name,
			@Parameter(description = "Name that the parameter is a substring of") @RequestParam Optional<String> nameLike,
			@Parameter(description = "Group type") @RequestParam Optional<String> type,
			@Parameter(description = "Groups which the given user id is a member of") @RequestParam Optional<String> member,
			@Parameter(description = "Groups which are members of the given tenant") @RequestParam Optional<String> memberOfTenant,
			@Parameter(description = "Sort the results lexicographically by a given criterion") @RequestParam Optional<String> sortBy,
			@Parameter(description = "Sort the results in a given order (asc or desc)") @RequestParam Optional<String> sortOrder,
			@Parameter(description = "Specifies the index of the first result to return") @RequestParam Optional<String> firstResult,
			@Parameter(description = "Specifies the maximum number of results to return") @RequestParam Optional<String> maxResults,
			Locale loc, CIBUser user) {
		return adminService.findGroups(id, name, nameLike, type, member, memberOfTenant,
				sortBy, sortOrder, firstResult, maxResults, loc, user);
	}

	/**
	 * Query-by-body variant of the group resource. CIB seven exposes a POST variant alongside the
	 * GET one, and the sdk uses it to populate group dropdowns, so the same filter criteria are
	 * accepted in the request body instead of as query parameters.
	 */
	@Operation(
			summary = "Query groups by request body (engine-rest compatible path used by embedded forms)",
			description = "Body accepts the same criteria as the query parameters of GET /group." + "<br>"
			+ "<strong>Return: Collection of groups")
	@PostMapping("/group")
	public Collection<UserGroup> queryGroups(
			@RequestBody(required = false) Map<String, Object> filters,
			Locale loc, CIBUser user) {
		return adminService.findGroups(
				filter(filters, "id"),
				filter(filters, "name"),
				filter(filters, "nameLike"),
				filter(filters, "type"),
				filter(filters, "member"),
				filter(filters, "memberOfTenant"),
				filter(filters, "sortBy"),
				filter(filters, "sortOrder"),
				filter(filters, "firstResult"),
				filter(filters, "maxResults"),
				loc, user);
	}

	/**
	 * Group count in the engine's {@code {"count": n}} shape.
	 *
	 * <p>There is no count method on the provider, so the matching groups are queried and counted.
	 * Group sets here back a dropdown and are small, so this is acceptable.</p>
	 */
	@Operation(
			summary = "Count groups matching the given filters (engine-rest compatible path used by embedded forms)",
			description = "<strong>Return: JSON object with a single count property")
	@GetMapping("/group/count")
	public Map<String, Integer> findGroupCount(
			@Parameter(description = "Group Id") @RequestParam Optional<String> id,
			@Parameter(description = "Group name") @RequestParam Optional<String> name,
			@Parameter(description = "Name that the parameter is a substring of") @RequestParam Optional<String> nameLike,
			@Parameter(description = "Group type") @RequestParam Optional<String> type,
			@Parameter(description = "Groups which the given user id is a member of") @RequestParam Optional<String> member,
			@Parameter(description = "Groups which are members of the given tenant") @RequestParam Optional<String> memberOfTenant,
			Locale loc, CIBUser user) {
		return Map.of("count", findGroups(id, name, nameLike, type, member, memberOfTenant,
				Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), loc, user).size());
	}

	/** Query-by-body counterpart of {@link #findGroupCount}. */
	@Operation(
			summary = "Count groups matching the filters in the request body (engine-rest compatible path)",
			description = "<strong>Return: JSON object with a single count property")
	@PostMapping("/group/count")
	public Map<String, Integer> queryGroupCount(
			@RequestBody(required = false) Map<String, Object> filters,
			Locale loc, CIBUser user) {
		return Map.of("count", queryGroups(filters, loc, user).size());
	}

	/**
	 * Single group lookup. The literal {@code /group/count} mapping takes precedence over this
	 * template, so counting is not swallowed by the id path.
	 */
	@Operation(
			summary = "Get a single group by id (engine-rest compatible path used by embedded forms)",
			description = "<strong>Return: The group")
	@ApiResponse(responseCode = "404", description = "Group not found")
	@GetMapping("/group/{id}")
	public ResponseEntity<UserGroup> findGroup(
			@Parameter(description = "Group Id") @PathVariable String id,
			Locale loc, CIBUser user) {
		return findGroups(Optional.of(id), NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE, loc, user)
				.stream().findFirst()
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Operation(
			summary = "Get users based on filters (engine-rest compatible path used by embedded forms)",
			description = "Alias of /admin/user at the path bpm-sdk requests." + "<br>"
			+ "<strong>Return: Collection of users")
	@GetMapping("/user")
	public Collection<User> findUsers(
			@Parameter(description = "User Id") @RequestParam Optional<String> id,
			@Parameter(description = "User first name") @RequestParam Optional<String> firstName,
			@Parameter(description = "First name that the parameter is a substring of") @RequestParam Optional<String> firstNameLike,
			@Parameter(description = "User last name") @RequestParam Optional<String> lastName,
			@Parameter(description = "Last name that the parameter is a substring of") @RequestParam Optional<String> lastNameLike,
			@Parameter(description = "User email") @RequestParam Optional<String> email,
			@Parameter(description = "Email that the parameter is a substring of") @RequestParam Optional<String> emailLike,
			@Parameter(description = "Users which are members of the given group") @RequestParam Optional<String> memberOfGroup,
			@Parameter(description = "Users which are members of the given tenant") @RequestParam Optional<String> memberOfTenant,
			@Parameter(description = "Comma-separated list of user ids") @RequestParam Optional<String> idIn,
			@Parameter(description = "Specifies the index of the first result to return") @RequestParam Optional<String> firstResult,
			@Parameter(description = "Specifies the maximum number of results to return") @RequestParam Optional<String> maxResults,
			@Parameter(description = "Specifies the field to sort by") @RequestParam Optional<String> sortBy,
			@Parameter(description = "Specifies the order of the sorting") @RequestParam Optional<String> sortOrder,
			@Parameter(description = "Whether to perform case-insensitive like pattern matching") @RequestParam Optional<Boolean> likePatternIgnoreCase,
			Locale loc, CIBUser user) {
		return adminService.findUsers(id, firstName, firstNameLike, lastName, lastNameLike,
				email, emailLike, memberOfGroup, memberOfTenant, idIn,
				firstResult, maxResults, sortBy, sortOrder, likePatternIgnoreCase, loc, user);
	}

	/**
	 * User count in the engine's {@code {"count": n}} shape.
	 *
	 * <p>As with the group count, there is no provider-level count, so the matching users are
	 * queried and counted.</p>
	 */
	@Operation(
			summary = "Count users matching the given filters (engine-rest compatible path used by embedded forms)",
			description = "<strong>Return: JSON object with a single count property")
	@GetMapping("/user/count")
	public Map<String, Integer> findUserCount(
			@Parameter(description = "User Id") @RequestParam Optional<String> id,
			@Parameter(description = "User first name") @RequestParam Optional<String> firstName,
			@Parameter(description = "First name that the parameter is a substring of") @RequestParam Optional<String> firstNameLike,
			@Parameter(description = "User last name") @RequestParam Optional<String> lastName,
			@Parameter(description = "Last name that the parameter is a substring of") @RequestParam Optional<String> lastNameLike,
			@Parameter(description = "User email") @RequestParam Optional<String> email,
			@Parameter(description = "Email that the parameter is a substring of") @RequestParam Optional<String> emailLike,
			@Parameter(description = "Users which are members of the given group") @RequestParam Optional<String> memberOfGroup,
			@Parameter(description = "Users which are members of the given tenant") @RequestParam Optional<String> memberOfTenant,
			Locale loc, CIBUser user) {
		return Map.of("count", findUsers(id, firstName, firstNameLike, lastName, lastNameLike,
				email, emailLike, memberOfGroup, memberOfTenant, NONE, NONE, NONE, NONE, NONE,
				Optional.empty(), loc, user).size());
	}

	/**
	 * Single user profile lookup. The engine's profile representation is the same set of fields the
	 * user query already returns, so the lookup is filtered by id.
	 */
	@Operation(
			summary = "Get a single user profile (engine-rest compatible path used by embedded forms)",
			description = "<strong>Return: The user profile")
	@ApiResponse(responseCode = "404", description = "User not found")
	@GetMapping("/user/{id}/profile")
	public ResponseEntity<User> findUserProfile(
			@Parameter(description = "User Id") @PathVariable String id,
			Locale loc, CIBUser user) {
		return findUsers(Optional.of(id), NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE,
				NONE, NONE, NONE, NONE, Optional.empty(), loc, user)
				.stream().findFirst()
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	/** Reads a single criterion out of a query-by-body request. */
	private Optional<String> filter(Map<String, Object> filters, String key) {
		if (filters == null) {
			return Optional.empty();
		}
		Object value = filters.get(key);
		return value == null ? Optional.empty() : Optional.of(String.valueOf(value));
	}
}
