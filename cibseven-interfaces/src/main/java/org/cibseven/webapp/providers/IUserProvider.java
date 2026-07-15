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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.springframework.http.ResponseEntity;

public interface IUserProvider {

	public long countUsers(Map<String, Object> filters, CIBUser user) throws SystemException;
	public Authorizations getUserAuthorization(String userId, CIBUser user);	
	public Collection<SevenUser> fetchUsers(CIBUser user) throws SystemException;
	public SevenVerifyUser verifyUser(StandardLogin login, CIBUser user) throws SystemException;
	public Collection<User> findUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike, Optional<String> lastName, Optional<String> lastNameLike,
			Optional<String> email, Optional<String> emailLike, Optional<String> memberOfGroup, Optional<String> memberOfTenant, Optional<String> idIn,
			Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder,
			Optional<Boolean> likePatternIgnoreCase, CIBUser user);

	public void createUser(NewUser user, CIBUser flowUser) throws InvalidUserIdException;
	public void updateUserProfile(String userId, User user, CIBUser flowUser);
	public void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user);
	public void addMemberToGroup(String groupId, String userId, CIBUser user);
	public void deleteMemberFromGroup(String groupId, String userId, CIBUser user);
	public void deleteUser(String userId, CIBUser user);	
	public SevenUser getUserProfile(String userId, CIBUser user);
	
	public Collection<UserGroup> findGroups(Optional<String> id, Optional<String> name, Optional<String> nameLike, Optional<String> type,
			Optional<String> member, Optional<String> memberOfTenant, Optional<String> sortBy, Optional<String> sortOrder,
			Optional<String> firstResult, Optional<String> maxResults,CIBUser user);
	
	public void createGroup(UserGroup group, CIBUser user);
	public void updateGroup(String groupId, UserGroup group, CIBUser user);
	public void deleteGroup(String groupId, CIBUser user);
	public Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type, Optional<String> userIdIn, Optional<String> groupIdIn,
			Optional<String> resourceType, Optional<String> resourceId, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult, 
			Optional<String> maxResults, CIBUser user);
	
	public ResponseEntity<Authorization> createAuthorization(Authorization authorization, CIBUser user);
	public void updateAuthorization(String authorizationId, Map<String, Object> data, CIBUser user);
	public void deleteAuthorization(String authorizationId, CIBUser user);	
	
	/**
	 * Builds an {@link Authorizations} object by grouping the given authorizations by
	 * their resource type. Shared by all {@link IUserProvider} implementations.
	 */
	default Authorizations buildAuthorizations(Collection<Authorization> userAuthorizations) {
		Authorizations auths = new Authorizations();
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
		return auths;
	}
	
}
