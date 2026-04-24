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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
	@ApiResponse(responseCode  = "500", description  = "An unexpected system error occured"),
	@ApiResponse(responseCode  = "401", description  = "Unauthorized")
})
@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/admin")
public class AdminService extends BaseService implements InitializingBean {

	@Autowired BpmProvider bpmProvider;
	
	public void afterPropertiesSet() {
	}	
	
	@Operation(
		summary = "Get a user based on filters",
		description  = "<strong>Return: User/s")
	@ApiResponse(responseCode = "404", description = "User not found")
	@RequestMapping(value = "/user", method = RequestMethod.GET)
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
	@Parameter(description = "Specifies the maximum number of results to return"
			+ "<br>Will return less results if there are no more results left") @RequestParam Optional<String> maxResults,
	@Parameter(description = "Specifies the field to sort by") @RequestParam Optional<String> sortBy,
	@Parameter(description = "Specifies the order of the sorting") @RequestParam Optional<String> sortOrder,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.USER, PermissionConstants.READ_ALL);
		return bpmProvider.findUsers(id, firstName, firstNameLike, lastName, lastNameLike, 
				email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder, user);
	}
	
	/**
	 * Create user
	 * 
	 * @param user A JSON object containing variable key-value pairs. The object contains the following properties: id (String), firstName (String), lastName (String) and email (String). 
	 * 				e.g:  {"id": "jonny1", "firstName":"John", "lastName":"Doe", "email":"aNewEmailAddress"}   
	 * @param loc
	 * @param flowUser
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
		summary = "Create user",
		description  = "Request body: User" + "<br>" +
			"<strong>Return:</strong> 204 No Content")
	@RequestMapping(value = "/user/create", method = RequestMethod.POST)
	public ResponseEntity<Void> createUser(
			@RequestBody NewUser user,
			Locale loc, CIBUser flowUser) {
		checkPermission(flowUser, SevenResourceType.USER, PermissionConstants.CREATE_ALL);
		bpmProvider.createUser(user, flowUser);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}

	/**
	 * update user profile
	 * 
	 * @param userId
	 * @param user A JSON object containing variable key-value pairs. The object contains the following properties: id (String), firstName (String), lastName (String) and email (String). 
	 * 				e.g:  {"id": "jonny1", "firstName":"John", "lastName":"Doe", "email":"aNewEmailAddress"}   
	 * @param loc
	 * @param flowUser
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
			summary = "Update user's profile",
			description = "Request body: User" + "<br>" +
			"<strong>Return:</strong> 204 No Content")
	@ApiResponse(responseCode = "404", description = "User not found")
	@RequestMapping(value = "/user/{userId}/profile", method = RequestMethod.PUT)
	public ResponseEntity<Void> updateUserProfile(
			@Parameter(description = "User Id") @PathVariable String userId, 
			@RequestBody User user, 
			Locale loc, CIBUser flowUser) {
		checkPermission(flowUser, SevenResourceType.USER, PermissionConstants.UPDATE_ALL);
		bpmProvider.updateUserProfile(userId, user, flowUser);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}
	
	/**
	 * Add user to a group
	 * 
	 * @param groupId The ID of the group
	 * @param userId The ID of the user
	 * @param loc The locale
	 * @param flowUser The authenticated user performing the action
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
			summary = "Add user to a group",
			description = "<strong>Return:</strong> 204 No Content")
	@ApiResponse(responseCode = "404", description = "Group or user not found")
	@RequestMapping(value = "/group/{groupId}/members/{userId}", method = RequestMethod.PUT)
	public ResponseEntity<Void> addMemberToGroup(
			@Parameter(description = "Group Id") @PathVariable String groupId, 
			@Parameter(description = "User Id") @PathVariable String userId, 
			Locale loc, CIBUser flowUser) {
		checkPermission(flowUser, SevenResourceType.GROUP, PermissionConstants.DELETE_ALL);
		bpmProvider.addMemberToGroup(groupId, userId, flowUser);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}
	
	/**
	 * Delete user from a group
	 * 
	 * @param groupId
	 * @param userId
	 * @param loc
	 * @param flowUser A JSON object containing variable key-value pairs. The object contains the following properties: id (String), firstName (String), lastName (String) and email (String). 
	 * 				e.g:  {"id": "jonny1", "firstName":"John", "lastName":"Doe", "email":"aNewEmailAddress"}   
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
			summary = "Delete user from a group",
			description = "<strong>Return:</strong> 204 No Content")
	@ApiResponse(responseCode = "404", description = "Group or user not found")
	@RequestMapping(value = "/group/{groupId}/members/{userId}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteMemberFromGroup(
			@Parameter(description = "Group Id") @PathVariable String groupId, 
			@Parameter(description = "User Id") @PathVariable String userId, 
			Locale loc, CIBUser flowUser) {
		checkPermission(flowUser, SevenResourceType.GROUP, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteMemberFromGroup(groupId, userId, flowUser);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}
	
	
	/**
	 * @param userId
	 * @param data Request Body
	 * 		A JSON object with the following properties:
	 * 		Name 	Type 	Description
	 * 		password 	String 	The user's new password.
	 * 		authenticatedUserPassword 	String 	The password of the authenticated user who changes the password of the user (i.e., the user with passed id as path parameter).	 * 
	 * @param loc
	 * @param user
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
			summary = "Update user's credentials",
			description = "Request body: A JSON object with the following properties: password and authenticatedUserPassword" + "<br>" +
			"<strong>Return:</strong> 204 No Content")
	@ApiResponse(responseCode = "404", description  = "User not found")
	@RequestMapping(value = "/user/{userId}/credentials", method = RequestMethod.PUT)
	public ResponseEntity<Void> updateUserCredentials(
			@Parameter(description = "User Id") @PathVariable String userId, 
			@RequestBody Map<String, Object> data, 
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.USER, PermissionConstants.UPDATE_ALL);
		bpmProvider.updateUserCredentials(userId, data, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}

	/**
	 * Deletes a user by id.
	 * 
	 * @param userId
	 * @param loc
	 * @param user
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
			summary = "Delete a user by Id",
			description = "<strong>Return:</strong> 204 No Content")
	@ApiResponse(responseCode = "404", description = "This user does not exist")
	@RequestMapping(value = "/user/{userId}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteUser(
			@Parameter(description = "User Id") @PathVariable String userId, 
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.USER, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteUser(userId, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}

	@Operation(
			summary = "Get a group based on filters",
			description = "<strong>Return: Collection of groups")
	@ApiResponse(responseCode = "404", description = "Group not found")
	@RequestMapping(value = "/group", method = RequestMethod.GET)
	public Collection<UserGroup> findGroups(
			@Parameter(description = "Group Id") @RequestParam Optional<String> id, 
			@Parameter(description = "Group name") @RequestParam Optional<String> name,
			@Parameter(description = "Name that the parameter is a substring of") @RequestParam Optional<String> nameLike,
			@Parameter(description = "Group type") @RequestParam Optional<String> type,
			@Parameter(description = "Groups which the given user id is a member of") @RequestParam Optional<String> member,
			@Parameter(description = "Groups which are members of the given tenant") @RequestParam Optional<String> memberOfTenant,
			@Parameter(description = "Sort the results lexicographically by a given criterion"
					+ "<br>Valid values are id, name and type"
					+ "<br>Must be used in conjunction with the sortOrder parameter") @RequestParam Optional<String> sortBy,
			@Parameter(description = "Sort the results in a given order"
					+ "<br>Values may be asc for ascending order or desc for descending order"
					+ "<br>Must be used in conjunction with the sortBy parameter") @RequestParam Optional<String> sortOrder,
			@Parameter(description = "Specifies the index of the first result to return") @RequestParam Optional<String> firstResult,
			@Parameter(description = "Specifies the maximum number of results to return"
					+ "<br>Will return less results if there are no more results left") @RequestParam Optional<String> maxResults,		
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.GROUP, PermissionConstants.READ_ALL);
		String decodedMember = member.map(value -> {
			try {
				return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
			} catch (UnsupportedEncodingException e) {
				throw new SystemException("UnsupportedEncodingException: " + e.getMessage());
			}
		}).orElse(null);
		
		String decodedMemberOfTenant = memberOfTenant.map(value -> {
			try {
				return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
			} catch (UnsupportedEncodingException e) {
				throw new SystemException("UnsupportedEncodingException: " + e.getMessage());
			}
		}).orElse(null);
		
		return bpmProvider.findGroups(id, 
				name, 
				nameLike, 
				type, 
			    Optional.ofNullable(decodedMember),
			    Optional.ofNullable(decodedMemberOfTenant),
				sortBy,
				sortOrder,
				firstResult,
				maxResults, user);
	}
		
	/**
	 * Create group
	 * 
	 * @param group A JSON object with the following properties:
	 * Name 	Type 	Description
	 * id 	String 	The id of the group.
	 * name 	String 	The name of the group.
	 * type 	String 	The type of the group. A JSON object containing variable key-value pairs. The object contains the following properties: id (String), firstName (String), lastName (String) and email (String). 
	 * 
	 * @param loc
	 * @param user
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
			summary = "Create group",
			description = "Request body: A JSON object with the following properties: id, name and type" + "<br>" +
			"<strong>Return:</strong> 204 No Content")
	@RequestMapping(value = "/group/create", method = RequestMethod.POST)
	public ResponseEntity<Void> createGroup(
			@RequestBody UserGroup group,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.GROUP, PermissionConstants.CREATE_ALL);
		bpmProvider.createGroup(group, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}

	/**
	 * update group
	 * 
	 * @param groupId
	 * @param group A JSON object containing variable key-value pairs. The object contains the following properties: id (String), firstName (String), lastName (String) and email (String). 
	 * 				e.g:  {"id": "jonny1", "firstName":"John", "lastName":"Doe", "email":"aNewEmailAddress"}   
	 * @param loc
	 * @param user
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
			summary = "Update group",
			description = "Request body: A JSON object with the following properties: id, name and type" + "<br>" +
			"<strong>Return:</strong> 204 No Content")
	@ApiResponse(responseCode = "404", description = "Group not found")
	@RequestMapping(value = "/group/{groupId}", method = RequestMethod.PUT)
	public ResponseEntity<Void> updateGroup(
			@Parameter(description = "Group Id") @PathVariable String groupId, 
			@RequestBody UserGroup group, 
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.GROUP, PermissionConstants.UPDATE_ALL);
		bpmProvider.updateGroup(groupId, group, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}

	/**
	 * Deletes a group by id.
	 * 
	 * @param groupId
	 * @param loc
	 * @param user
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
			summary = "Delete a group by Id",
			description = "<strong>Return:</strong> 204 No Content")
	@ApiResponse(responseCode = "404", description = "Group not found")
	@RequestMapping(value = "/group/{groupId}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteGroup(
			@Parameter(description = "Group Id") @PathVariable String groupId, 
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.GROUP, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteGroup(groupId, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}
	
	@Operation(
			summary = "Get authorization based on filters",
			description = "<strong>Return: Collection of authorizations")
	@ApiResponse(responseCode = "404", description = "Authorization not found")
	@RequestMapping(value = "/authorization", method = RequestMethod.GET)
	public Collection<Authorization> findAuthorization(
			@Parameter(description = "Authorization Id") @RequestParam Optional<String> id,
			@Parameter(description = "Authorization type (0=global, 1=grant, 2=revoke)"
					+ "<br>See the User Guide for more information about authorization types") @RequestParam Optional<String> type,
			@Parameter(description = "Comma-separated list of user Ids") @RequestParam Optional<String> userIdIn,
			@Parameter(description = "Comma-separated list of group Idsd") @RequestParam Optional<String> groupIdIn,
			@Parameter(description = "integer representation of the resource type"
					+ "<br>See the User Guide for a list of integer representations of resource types") @RequestParam Optional<String> resourceType,
			@Parameter(description = "Resource Id") @RequestParam Optional<String> resourceId,	
			@Parameter(description = "Sort the results lexicographically by a given criterion"
					+ "<br>Valid values are id, name and type"
					+ "<br>Must be used in conjunction with the sortOrder parameter") @RequestParam Optional<String> sortBy,
			@Parameter(description = "Sort the results in a given order"
					+ "<br>Values may be asc for ascending order or desc for descending order"
					+ "<br>Must be used in conjunction with the sortBy parameter") @RequestParam Optional<String> sortOrder,
			@Parameter(description = "Index of the first result to return") @RequestParam Optional<String> firstResult,
			@Parameter(description = "Maximum number of results to return"
					+ "<br>Will return less results if there are no more results left") @RequestParam Optional<String> maxResults,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.AUTHORIZATION, PermissionConstants.READ_ALL);
		return bpmProvider.findAuthorization(id, type, userIdIn, groupIdIn, resourceType, resourceId,
				sortBy, sortOrder, firstResult, maxResults, user);
	}

	/**
	 * Create authorization
	 * 
	 * @param authorization A JSON object with the following properties:
	 *        Name          Value       Description
	 *        type          Integer     The type of the authorization. (0=global, 1=grant, 2=revoke). See the User Guide for more information about authorization types.
	 *        permissions   String      An array of Strings holding the permissions provided by this authorization.
	 *        userId        String      The id of the user this authorization has been created for. The value "*" represents a global authorization ranging over all users.
	 *        groupId       String      The id of the group this authorization has been created for.
	 *        resourceType  Integer     An integer representing the resource type. See the User Guide for a list of integer representations of resource types.
	 *        resourceId    String      The resource Id. The value "*" represents an authorization ranging over all instances of a resource.
	 * @param loc The locale
	 * @param user The authenticated user performing the action
	 * @return ResponseEntity containing the created Authorization
	 */
	@Operation(
			summary = "Create authorization",
			description = "Request body: A JSON object with the following properties: type, permissions, userId, groupId, resourceType and resourceId" + "<br>" +
			"<strong>Return: ResponseEntity<Authorization>")
	@RequestMapping(value = "/authorization/create", method = RequestMethod.POST)
	public ResponseEntity<Authorization> createAuthorization(
			@RequestBody Authorization authorization,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.AUTHORIZATION, PermissionConstants.CREATE_ALL);
		return bpmProvider.createAuthorization(authorization, user);
	}

	/**
	 * update authorization
	 * 
	 * @param authorizationId
	 * @param loc
	 * @param user
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
			summary = "Update authorization",
			description = "Request body: A JSON object with the following properties: permissions, userId, groupId, resourceType and resourceId" + "<br>" +
			"<strong>Return:</strong> 204 No Content")
	@ApiResponse(responseCode = "404", description = "Authorization not found")
	@RequestMapping(value = "/authorization/{authorizationId}", method = RequestMethod.PUT)
	public ResponseEntity<Void> updateAuthorization(
			@Parameter(description = "Authorization Id") @PathVariable String authorizationId, 
			@RequestBody Map<String, Object> data, 
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.AUTHORIZATION, PermissionConstants.UPDATE_ALL);
		bpmProvider.updateAuthorization(authorizationId, data, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}

	/**
	 * Deletes a authorization by id.
	 * 
	 * @param authorizationId
	 * @param loc
	 * @param user
	 * @return ResponseEntity<Void> with status 204 No Content
	 */
	@Operation(
			summary = "Delete authorization by Id",
			description = "<strong>Return:</strong> 204 No Content")
	@ApiResponse(responseCode = "404", description = "Authorization not found")
	@RequestMapping(value = "/authorization/{authorizationId}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteAuthorization(
			@Parameter(description = "Authorization Id") @PathVariable String authorizationId, 
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.AUTHORIZATION, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteAuthorization(authorizationId, user);
    // return 204 No Content, no body
    return ResponseEntity.noContent().build();
	}

}
