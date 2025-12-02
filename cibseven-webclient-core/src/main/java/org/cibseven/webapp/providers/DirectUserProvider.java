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
package org.cibseven.webapp.providers;

import static org.cibseven.webapp.auth.SevenAuthorizationUtils.resourceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.authorization.AuthorizationQuery;
import org.cibseven.bpm.engine.authorization.Permissions;
import org.cibseven.bpm.engine.identity.Group;
import org.cibseven.bpm.engine.identity.GroupQuery;
import org.cibseven.bpm.engine.identity.UserQuery;
import org.cibseven.bpm.engine.impl.identity.Authentication;
import org.cibseven.bpm.engine.impl.util.PermissionConverter;
import org.cibseven.bpm.engine.rest.dto.authorization.AuthorizationDto;
import org.cibseven.bpm.engine.rest.dto.authorization.AuthorizationQueryDto;
import org.cibseven.bpm.engine.rest.dto.identity.GroupQueryDto;
import org.cibseven.bpm.engine.rest.dto.identity.UserQueryDto;
import org.cibseven.bpm.engine.rest.dto.task.GroupDto;
import org.cibseven.bpm.engine.rest.dto.task.UserDto;
import org.cibseven.bpm.engine.rest.util.QueryUtil;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

public class DirectUserProvider implements IUserProvider {

	private String userProvider;
	private String wildcard;
	DirectProviderUtil directProviderUtil;

	public DirectUserProvider(DirectProviderUtil directProviderUtil, String userProvider, String wildcard) {
		this.userProvider = userProvider;
		this.directProviderUtil = directProviderUtil;
		this.wildcard = wildcard;
	}

	@Override
	public Collection<SevenUser> fetchUsers(CIBUser user) throws SystemException {
		UserQueryDto queryDto = new UserQueryDto();
		queryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		UserQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));
		query.userId(user.getId());
		List<org.cibseven.bpm.engine.identity.User> resultList = QueryUtil.list(query, null, null);

		Collection<SevenUser> users = new ArrayList<>();
		for (org.cibseven.bpm.engine.identity.User resultUser : resultList) {
			users.add(directProviderUtil.convertValue(resultUser, SevenUser.class, user));
		}
		return users;
	}

	@Override
	public Authorizations getUserAuthorization(String userId, CIBUser user) {
		AuthorizationQueryDto queryDto = new AuthorizationQueryDto();
		queryDto.setUserIdIn(new String[] { userId });
		queryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		AuthorizationQuery userQuery = queryDto.toQuery(directProviderUtil.getProcessEngine(user));

		List<org.cibseven.bpm.engine.authorization.Authorization> userAuthorizationList = QueryUtil.list(userQuery, null,
				null);
		GroupQuery groupQuery = directProviderUtil.getProcessEngine(user).getIdentityService().createGroupQuery();
		List<Group> userGroups = groupQuery.groupMember(userId).orderByGroupName().asc().unlimitedList();

		Set<UserDto> allGroupUsers = new HashSet<>();
		List<GroupDto> allGroups = new ArrayList<>();

		List<String> listGroups = new ArrayList<>();
		for (Group group : userGroups) {
			List<org.cibseven.bpm.engine.identity.User> groupUsers = directProviderUtil.getProcessEngine(user).getIdentityService().createUserQuery()
					.memberOfGroup(group.getId()).unlimitedList();

			for (org.cibseven.bpm.engine.identity.User groupUser : groupUsers) {
				if (!user.getId().equals(userId)) {
					allGroupUsers.add(new UserDto(groupUser.getId(), groupUser.getFirstName(), groupUser.getLastName()));
				}
			}
			allGroups.add(new GroupDto(group.getId(), group.getName()));
			listGroups.add(group.getId());
		}

		AuthorizationQueryDto groupIdQueryDto = new AuthorizationQueryDto();
		groupIdQueryDto.setGroupIdIn(listGroups.toArray(new String[0]));
		groupIdQueryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		AuthorizationQuery groupIdQuery = groupIdQueryDto.toQuery(directProviderUtil.getProcessEngine(user));
		List<org.cibseven.bpm.engine.authorization.Authorization> groupIdResultList = QueryUtil.list(groupIdQuery, null,
				null);
		Collection<Authorization> groupsAuthorizations = createAuthorizationCollection(groupIdResultList);

		AuthorizationQueryDto globalIdQueryDto = new AuthorizationQueryDto();
		globalIdQueryDto.setType(0);
		globalIdQueryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		AuthorizationQuery globalIdQuery = globalIdQueryDto.toQuery(directProviderUtil.getProcessEngine(user));
		List<org.cibseven.bpm.engine.authorization.Authorization> globalIdResultList = QueryUtil.list(globalIdQuery, null,
				null);
		Collection<Authorization> globalAuthorizations = createAuthorizationCollection(globalIdResultList);

		Authorizations auths = new Authorizations();
		Collection<Authorization> userAuthorizations = createAuthorizationCollection(userAuthorizationList);
		userAuthorizations.addAll(groupsAuthorizations);
		userAuthorizations.addAll(globalAuthorizations);

		auths.setApplication(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.APPLICATION)));
		auths.setFilter(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.FILTER)));
		auths.setProcessDefinition(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.PROCESS_DEFINITION)));
		auths.setProcessInstance(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.PROCESS_INSTANCE)));
		auths.setTask(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.TASK)));
		auths.setAuthorization(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.AUTHORIZATION)));
		auths.setUser(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.USER)));
		auths.setGroup(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.GROUP)));
		auths.setDecisionDefinition(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.DECISION_DEFINITION)));
		auths.setDecisionRequirementsDefinition(
				SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.DECISION_REQUIREMENTS_DEFINITION)));
		auths.setDeployment(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.DEPLOYMENT)));
		// auths.setCaseDefinition(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.CASE_DEFINITION)));
		// auths.setCaseInstance(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.CASE_INSTANCE)));
		// auths.setJobDefinition(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.JOB_DEFINITION)));
		auths.setBatch(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.BATCH)));
		auths.setGroupMembership(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.GROUP_MEMBERSHIP)));
		auths.setHistoricTask(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.HISTORIC_TASK)));
		auths.setHistoricProcessInstance(
				SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.HISTORIC_PROCESS_INSTANCE)));
		auths.setTenant(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.TENANT)));
		auths.setTenantMembership(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.TENANT_MEMBERSHIP)));
		auths.setReport(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.REPORT)));
		auths.setDashboard(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.DASHBOARD)));
		auths.setUserOperationLogCategory(
				SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.USER_OPERATION_LOG_CATEGORY)));
		auths.setSystem(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.SYSTEM)));
		// auths.setMessage(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.MESSAGE)));
		// auths.setEventSubscription(SevenProviderBase.filterResources(userAuthorizations, resourceType(SevenResourceType.EVENT_SUBSCRIPTION)));

		return auths;
	}

	@Override
	public SevenVerifyUser verifyUser(String username, String password, CIBUser user) throws SystemException {
		if ((username == null || username.isBlank()) || (password == null || password.isBlank()))
			throw new SystemException("Username and password are required");
		SevenVerifyUser verifyUser = new SevenVerifyUser();
		boolean valid = directProviderUtil.getProcessEngine(user).getIdentityService().checkPassword(username, password);
		verifyUser.setAuthenticated(valid);
		verifyUser.setAuthenticatedUser(username);
		return verifyUser;
	}

	@Override
	public Collection<User> findUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike,
			Optional<String> lastName, Optional<String> lastNameLike, Optional<String> email, Optional<String> emailLike,
			Optional<String> memberOfGroup, Optional<String> memberOfTenant, Optional<String> idIn,
			Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder,
			CIBUser user) {
		String wcard = getWildcard();
		if (!userProvider.equals("org.cibseven.webapp.auth.SevenUserProvider")) {
			Collection<User> result = getUsers(id, firstName, Optional.of(firstNameLike.get()), lastName, lastNameLike, 
					email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy,
					sortOrder, wcard, user);
			return result;
		}

		if (firstNameLike.isPresent()) { // javier, JAVIER, Javier
			Collection<User> lowerCaseResult = getUsers(id, firstName, Optional.of(firstNameLike.get().toLowerCase()), lastName,
					lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy,
					sortOrder, wcard, user);
			Collection<User> upperCaseResult = getUsers(id, firstName, Optional.of(firstNameLike.get().toUpperCase()), lastName,
					lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy,
					sortOrder, wcard, user);
			Collection<User> normalCaseResult = getUsers(id, firstName,
					Optional.of(firstNameLike.get().substring(0, 2).toUpperCase() + firstNameLike.get().substring(2).toLowerCase()),
					lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy,
					sortOrder, wcard, user);

			Collection<User> res = new ArrayList<User>();
			res.addAll(lowerCaseResult);
			res.addAll(upperCaseResult);
			res.addAll(normalCaseResult);

			return res;
		}

		if (lastNameLike.isPresent()) { // javier, JAVIER, Javier
			Collection<User> lowerCaseResult = getUsers(id, firstName, firstNameLike, lastName,
					Optional.of(lastNameLike.get().toLowerCase()), email, emailLike, memberOfGroup, memberOfTenant, idIn,
					firstResult, maxResults, sortBy, sortOrder, wcard, user);
			Collection<User> upperCaseResult = getUsers(id, firstName, firstNameLike, lastName,
					Optional.of(lastNameLike.get().toLowerCase()), email, emailLike, memberOfGroup, memberOfTenant, idIn,
					firstResult, maxResults, sortBy, sortOrder, wcard, user);
			Collection<User> normalCaseResult = getUsers(id, firstName, firstNameLike, lastName,
					Optional.of(lastNameLike.get().substring(0, 2).toUpperCase() + lastNameLike.get().substring(2).toLowerCase()),
					email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder, wcard, user);

			Collection<User> res = new ArrayList<User>();
			res.addAll(lowerCaseResult);
			res.addAll(upperCaseResult);
			res.addAll(normalCaseResult);

			return res;
		}

		return getUsers(id, firstName, firstNameLike, lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant,
				idIn, firstResult, maxResults, sortBy, sortOrder, wcard, user);
	}

	private String getWildcard () {
		String wcard = "";
		if (wildcard != null && !wildcard.equals("")) wcard = wildcard;
		else {
			if (userProvider.equals("org.cibseven.webapp.auth.LdapUserProvider") || userProvider.equals("org.cibseven.webapp.auth.AdfsUserProvider")) {
				wcard = "*";
			} else wcard = "%";
		}
		return wcard;
	}

	@Override
	public void createUser(NewUser user, CIBUser flowUser) throws InvalidUserIdException {
		User profile = user.getProfile();
		org.cibseven.bpm.engine.identity.User newUser = directProviderUtil.getProcessEngine(flowUser).getIdentityService().newUser(profile.getId());
		newUser.setId(profile.getId());
		newUser.setFirstName(profile.getFirstName());
		newUser.setLastName(profile.getLastName());
		newUser.setEmail(profile.getEmail());
		newUser.setPassword(user.getCredentials().getPassword());
		directProviderUtil.getProcessEngine(flowUser).getIdentityService().saveUser(newUser);
	}

	@Override
	public void updateUserProfile(String userId, User user, CIBUser flowUser) {
		if (directProviderUtil.getProcessEngine(flowUser).getIdentityService().isReadOnly()) {
			throw new SystemException("Identity service implementation is read-only.");
		}

		org.cibseven.bpm.engine.identity.User dbUser = findUserObject(user.getId(), flowUser);
		if (dbUser == null) {
			throw new NoObjectFoundException(new SystemException("User with id " + user.getId() + " does not exist"));
		}

		dbUser.setId(user.getId());
		dbUser.setFirstName(user.getFirstName());
		dbUser.setLastName(user.getLastName());
		dbUser.setEmail(user.getEmail());
		directProviderUtil.getProcessEngine(flowUser).getIdentityService().saveUser(dbUser);
	}

	@Override
	public void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user) {
		if (directProviderUtil.getProcessEngine(user).getIdentityService().isReadOnly()) {
			throw new SystemException("Identity service implementation is read-only.");
		}
		Authentication currentAuthentication = directProviderUtil.getProcessEngine(user).getIdentityService().getCurrentAuthentication();
		if (currentAuthentication != null && currentAuthentication.getUserId() != null) {
			if (!directProviderUtil.getProcessEngine(user).getIdentityService().checkPassword(currentAuthentication.getUserId(),
					(String) data.get("authenticatedUserPassword"))) {
				throw new SystemException("The given authenticated user password is not valid.");
			}
		}

		org.cibseven.bpm.engine.identity.User dbUser = findUserObject(userId, user);
		if (dbUser == null) {
			throw new NoObjectFoundException(new SystemException("User with id " + user.getId() + " does not exist"));
		}

		dbUser.setPassword((String) data.get("password"));
		directProviderUtil.getProcessEngine(user).getIdentityService().saveUser(dbUser);
	}

	@Override
	public void addMemberToGroup(String groupId, String userId, CIBUser user) {
		if (directProviderUtil.getProcessEngine(user).getIdentityService().isReadOnly()) {
			throw new SystemException("Identity service implementation is read-only.");
		}
		directProviderUtil.getProcessEngine(user).getIdentityService().createMembership(userId, groupId);
	}

	@Override
	public void deleteMemberFromGroup(String groupId, String userId, CIBUser user) {
		if (directProviderUtil.getProcessEngine(user).getIdentityService().isReadOnly()) {
			throw new SystemException("Identity service implementation is read-only.");
		}
		directProviderUtil.getProcessEngine(user).getIdentityService().deleteMembership(userId, groupId);
	}

	@Override
	public void deleteUser(String userId, CIBUser user) {
		directProviderUtil.getProcessEngine(user).getIdentityService().deleteUser(userId);
	}

	@Override
	public SevenUser getUserProfile(String userId, CIBUser user) {
		List<org.cibseven.bpm.engine.identity.User> users = directProviderUtil.getProcessEngine(user).getIdentityService().createUserQuery().userId(userId).list();
		org.cibseven.bpm.engine.identity.User identityUser = null;
		if (users.isEmpty()) {
			return null;
		} else if (users.size() == 1) {
			identityUser = users.get(0);
		} else {
			identityUser = users.stream().filter(u -> u.getId().equals(userId)).findFirst().orElse(null);
			if (identityUser == null) {
				identityUser = users.get(0);
			}
		}
		return directProviderUtil.convertValue(identityUser, SevenUser.class, user);
	}

	@Override
	public Collection<UserGroup> findGroups(Optional<String> id, Optional<String> name, Optional<String> nameLike,
			Optional<String> type, Optional<String> member, Optional<String> memberOfTenant, Optional<String> sortBy,
			Optional<String> sortOrder, Optional<String> firstResult, Optional<String> maxResults, CIBUser user) {
		final String wcard = "%";
		GroupQueryDto queryDto = new GroupQueryDto();
		queryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		// set parameters
		if (id.isPresent())
			queryDto.setId(id.get());
		if (name.isPresent())
			queryDto.setName(name.get());
		if (nameLike.isPresent())
			queryDto.setNameLike(nameLike.get().replace("*", wcard));
		;
		if (type.isPresent())
			queryDto.setType(type.get());
		if (member.isPresent())
			queryDto.setMember(member.get());
		if (memberOfTenant.isPresent())
			queryDto.setMemberOfTenant(memberOfTenant.get());
		if (sortBy.isPresent())
			queryDto.setSortBy(sortBy.get());
		if (sortOrder.isPresent())
			queryDto.setSortOrder(sortOrder.get().equals("asc") ? "asc" : "desc");
		GroupQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));

		Integer first = firstResult.isPresent() ? Integer.parseInt(firstResult.get()) : null;
		Integer max = maxResults.isPresent() ? Integer.parseInt(maxResults.get()) : null;
		List<Group> resultList = QueryUtil.list(query, first, max);

		Collection<UserGroup> userGroups = createUserGroups(resultList);
		return userGroups;
	}

	@Override
	public void createGroup(UserGroup group, CIBUser user) {
		if (directProviderUtil.getProcessEngine(user).getIdentityService().isReadOnly()) {
			throw new SystemException("Identity service implementation is read-only.");
		}
		Group newGroup = directProviderUtil.getProcessEngine(user).getIdentityService().newGroup(group.getId());
		newGroup.setId(group.getId());
		newGroup.setName(group.getName());
		newGroup.setType(group.getType());
		directProviderUtil.getProcessEngine(user).getIdentityService().saveGroup(newGroup);
	}

	@Override
	public void updateGroup(String groupId, UserGroup group, CIBUser user) {
		if (directProviderUtil.getProcessEngine(user).getIdentityService().isReadOnly()) {
			throw new SystemException("Identity service implementation is read-only.");
		}

		Group dbGroup = findGroupObject(groupId, user);
		if (dbGroup == null) {
			throw new NoObjectFoundException(new SystemException("Group with id " + groupId + " does not exist"));
		}

		dbGroup.setId(group.getId());
		dbGroup.setName(group.getName());
		dbGroup.setType(group.getType());

		directProviderUtil.getProcessEngine(user).getIdentityService().saveGroup(dbGroup);
	}

	@Override
	public void deleteGroup(String groupId, CIBUser user) {
		if (directProviderUtil.getProcessEngine(user).getIdentityService().isReadOnly()) {
			throw new SystemException("Identity service implementation is read-only.");
		}
		directProviderUtil.getProcessEngine(user).getIdentityService().deleteGroup(groupId);
	}

	@Override
	public Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type,
			Optional<String> userIdIn, Optional<String> groupIdIn, Optional<String> resourceType, Optional<String> resourceId,
			Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult, Optional<String> maxResults,
			CIBUser user) {
		AuthorizationQueryDto queryDto = new AuthorizationQueryDto();
		queryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		if (id.isPresent())
			queryDto.setId(id.get());
		if (type.isPresent())
			queryDto.setType(Integer.parseInt(type.get()));
		if (userIdIn.isPresent())
			queryDto.setUserIdIn(new String[] { userIdIn.get() });
		if (groupIdIn.isPresent())
			queryDto.setGroupIdIn(new String[] { groupIdIn.get() });
		if (resourceType.isPresent())
			queryDto.setResourceType(Integer.parseInt(resourceType.get()));
		if (resourceId.isPresent())
			queryDto.setResourceId(resourceId.get());
		if (sortOrder.isPresent())
			queryDto.setSortOrder(sortOrder.get());
		if (sortOrder.isPresent())
			queryDto.setSortOrder(sortOrder.get());
		Integer firstResultParam = null;
		if (firstResult.isPresent())
			firstResultParam = Integer.parseInt(firstResult.get());
		Integer maxResultsParam = null;
		if (maxResults.isPresent())
			maxResultsParam = Integer.parseInt(maxResults.get());
		return queryAuthorizations(queryDto, firstResultParam, maxResultsParam, user);
	}

	private List<Authorization> queryAuthorizations(AuthorizationQueryDto queryDto, Integer firstResult,
			Integer maxResults, CIBUser user) {
		queryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		AuthorizationQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));

		List<org.cibseven.bpm.engine.authorization.Authorization> resultList = QueryUtil.list(query, firstResult, maxResults);
		List<AuthorizationDto> authorizationDtoList = AuthorizationDto.fromAuthorizationList(resultList,
				directProviderUtil.getProcessEngine(user).getProcessEngineConfiguration());
		List<Authorization> authorizationList = new ArrayList<>();
		for (AuthorizationDto authorizationDto : authorizationDtoList) {
			authorizationList.add(directProviderUtil.convertValue(authorizationDto, Authorization.class, user));
		}
		return authorizationList;
	}

	@Override
	public ResponseEntity<Authorization> createAuthorization(Authorization authorization, CIBUser user) {
		org.cibseven.bpm.engine.authorization.Authorization newAuthorization = directProviderUtil.getProcessEngine(user).getAuthorizationService()
				.createNewAuthorization(authorization.getType());
		newAuthorization.setGroupId(authorization.getGroupId());
		newAuthorization.setUserId(authorization.getUserId());
		newAuthorization.setResourceType(authorization.getResourceType());
		newAuthorization.setResourceId(authorization.getResourceId());
		newAuthorization.setPermissions(PermissionConverter.getPermissionsForNames(authorization.getPermissions(),
				authorization.getResourceType(), directProviderUtil.getProcessEngine(user).getProcessEngineConfiguration()));

		newAuthorization = directProviderUtil.getProcessEngine(user).getAuthorizationService().saveAuthorization(newAuthorization);

		Authorization resultAuthorization = new Authorization();
		resultAuthorization.setGroupId(newAuthorization.getGroupId());
		resultAuthorization.setId(newAuthorization.getId());
		resultAuthorization.setPermissions(PermissionConverter.getNamesForPermissions(newAuthorization,
				newAuthorization.getPermissions(Permissions.values())));
		resultAuthorization.setResourceId(newAuthorization.getResourceId());
		resultAuthorization.setResourceType(newAuthorization.getResourceType());
		resultAuthorization.setType(newAuthorization.getAuthorizationType());
		resultAuthorization.setUserId(newAuthorization.getUserId());
		return new ResponseEntity<Authorization>(resultAuthorization, HttpStatusCode.valueOf(200));
	}

	@Override
	public void updateAuthorization(String authorizationId, Map<String, Object> data, CIBUser user) {
		org.cibseven.bpm.engine.authorization.Authorization dbAuthorization = directProviderUtil.getProcessEngine(user).getAuthorizationService().createAuthorizationQuery()
				.authorizationId(authorizationId).singleResult();

		if (dbAuthorization == null) {
			throw new NoObjectFoundException(new SystemException("Authorization with id " + authorizationId + " does not exist."));
		}
		AuthorizationDto authorizationDto = new AuthorizationDto();
		if (data.containsKey("groupId"))
			authorizationDto.setGroupId((String) data.get("groupId"));
		if (data.containsKey("permissions")) {
			List<String> permissionList = (List<String>) data.get("permissions");
			authorizationDto.setPermissions(permissionList.toArray(new String[0]));
		}
		if (data.containsKey("resourceId"))
			authorizationDto.setResourceId((String) data.get("resourceId"));
		if (data.containsKey("resourceType"))
			authorizationDto.setResourceType((Integer) data.get("resourceType"));
		if (data.containsKey("type"))
			authorizationDto.setType((Integer) data.get("type"));
		if (data.containsKey("userId"))
			authorizationDto.setUserId((String) data.get("userId"));
		AuthorizationDto.update(authorizationDto, dbAuthorization, directProviderUtil.getProcessEngine(user).getProcessEngineConfiguration());
		// save
		directProviderUtil.getProcessEngine(user).getAuthorizationService().saveAuthorization(dbAuthorization);
	}

	@Override
	public void deleteAuthorization(String authorizationId, CIBUser user) {
		directProviderUtil.getProcessEngine(user).getAuthorizationService().deleteAuthorization(authorizationId);
	}

	private org.cibseven.bpm.engine.identity.User findUserObject(String id, CIBUser user) {
		org.cibseven.bpm.engine.identity.User dbUser = null;
		try {
			List<org.cibseven.bpm.engine.identity.User> users = directProviderUtil.getProcessEngine(user).getIdentityService().createUserQuery().userId(id).list();

			if (users.size() == 1) {
				dbUser = users.get(0);
			} else if (!users.isEmpty()) {

				dbUser = users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);

				if (dbUser == null) {
					dbUser = users.get(0);
				}
			}
		} catch (ProcessEngineException e) {
			throw new SystemException("Exception while performing user query: " + e.getMessage());
		}
		return dbUser;
	}

	private Group findGroupObject(String groupId, CIBUser user) {
		try {
			return directProviderUtil.getProcessEngine(user).getIdentityService().createGroupQuery().groupId(groupId).singleResult();
		} catch (ProcessEngineException e) {
			throw new SystemException("Exception while performing group query: " + e.getMessage());
		}
	}

	private Collection<User> getUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike,
			Optional<String> lastName, Optional<String> lastNameLike, Optional<String> email, Optional<String> emailLike,
			Optional<String> memberOfGroup, Optional<String> memberOfTenant, Optional<String> idIn,
			Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder,
			String wcard, CIBUser user) {
		UserQueryDto queryDto = new UserQueryDto();
		queryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		UserQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));
		if (memberOfGroup.isPresent())
			query.memberOfGroup(memberOfGroup.get());
		if (memberOfTenant.isPresent())
			query.memberOfTenant(memberOfTenant.get());
		if (sortBy.isPresent()) {
			String sortByValue = sortBy.get();
			switch (sortByValue) {
			case "userId":
				query.orderByUserId();
				break;
			case "firstName":
				query.orderByUserFirstName();
				break;
			case "lastName":
				query.orderByUserLastName();
				break;
			case "email":
				query.orderByUserEmail();
				break;
			default:
			}
		}
		if (email.isPresent())
			query.userEmail(email.get());
		if (emailLike.isPresent())
			query.userEmailLike(emailLike.get().replace("*", wcard));
		if (firstName.isPresent())
			query.userFirstName(firstName.get());
		if (firstNameLike.isPresent())
			query.userFirstNameLike(firstNameLike.get().replace("*", wcard));
		if (id.isPresent())
			query.userId(id.get());
		if (lastName.isPresent())
			query.userLastName(lastName.get());
		if (lastNameLike.isPresent())
			query.userLastNameLike(lastNameLike.get().replace("*", wcard));
		Integer first = firstResult.isPresent() ? Integer.parseInt(firstResult.get()) : null;
		Integer max = maxResults.isPresent() ? Integer.parseInt(maxResults.get()) : null;
		List<org.cibseven.bpm.engine.identity.User> resultList = QueryUtil.list(query, first, max);

		Collection<User> userCollection = createUsers(resultList);
		return userCollection;
	}

	private Collection<User> createUsers(List<org.cibseven.bpm.engine.identity.User> resultList) {
		Collection<User> users = new ArrayList<>();
		for (org.cibseven.bpm.engine.identity.User resultUser : resultList) {
			User user = new User();
			user.setEmail(resultUser.getEmail());
			user.setFirstName(resultUser.getFirstName());
			user.setId(resultUser.getId());
			user.setLastName(resultUser.getLastName());
			users.add(user);
		}
		return users;
	}

	private Collection<Authorization> createAuthorizationCollection(
			List<org.cibseven.bpm.engine.authorization.Authorization> userAuthorizationList) {
		Collection<Authorization> resultAuthorization = new ArrayList<>();
		for (org.cibseven.bpm.engine.authorization.Authorization userAuthorization : userAuthorizationList) {
			resultAuthorization.add(createAuthorization(userAuthorization));
		}
		return resultAuthorization;
	}

	private Authorization createAuthorization(org.cibseven.bpm.engine.authorization.Authorization userAuthorization) {
		Authorization newUserAuthorization = new Authorization();
		newUserAuthorization.setGroupId(userAuthorization.getGroupId());
		newUserAuthorization.setId(userAuthorization.getId());
		newUserAuthorization.setPermissions(PermissionConverter.getNamesForPermissions(userAuthorization,
				userAuthorization.getPermissions(Permissions.values())));
		newUserAuthorization.setResourceId(userAuthorization.getResourceId());
		newUserAuthorization.setResourceType(userAuthorization.getResourceType());
		newUserAuthorization.setType(userAuthorization.getAuthorizationType());
		newUserAuthorization.setUserId(userAuthorization.getUserId());
		return newUserAuthorization;
	}

	private Collection<UserGroup> createUserGroups(List<Group> resultList) {
		Collection<UserGroup> userGroups = new ArrayList<>();
		for (Group group : resultList) {
			userGroups.add(createUserGroup(group));
		}
		return userGroups;
	}

	private UserGroup createUserGroup(Group group) {
		UserGroup userGroup = new UserGroup();
		userGroup.setId(group.getId());
		userGroup.setName(group.getName());
		userGroup.setType(group.getType());
		return userGroup;
	}

}
