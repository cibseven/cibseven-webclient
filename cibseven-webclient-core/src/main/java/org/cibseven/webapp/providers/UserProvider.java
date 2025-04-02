package org.cibseven.webapp.providers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

import static org.cibseven.webapp.auth.SevenAuthorizationUtils.*;

@Slf4j
@Component
public class UserProvider extends SevenProviderBase implements IUserProvider {
	
	@Value("${user.provider}") String userProvider;
	@Value("${users.search.wildcard:}") String wildcard;
	
	@Override
	public Authorizations getUserAuthorization(String userId, CIBUser user) {
		Authorizations auths = new Authorizations();
		try {
			String urlUsers = camundaUrl + "/engine-rest/authorization";
			UriComponentsBuilder builder;
			
			builder = UriComponentsBuilder.fromHttpUrl(urlUsers).queryParam("userIdIn", URLEncoder.encode(userId, StandardCharsets.UTF_8.toString()));
	
			Collection<Authorization> userAuthorizations = new ArrayList<Authorization>(Arrays.asList(((ResponseEntity<Authorization[]>) doGet(builder, Authorization[].class, user)).getBody()));
			
			String urlGroup = camundaUrl + "/engine-rest/group";
			builder = UriComponentsBuilder.fromHttpUrl(urlGroup).queryParam("member", URLEncoder.encode(userId, StandardCharsets.UTF_8.toString()));
			Collection<UserGroup> userGroups = Arrays.asList(((ResponseEntity<UserGroup[]>) doGet(builder, UserGroup[].class, user)).getBody());
			
			String listGroups = "";
			
			for (UserGroup userGroup : userGroups) {
				listGroups += userGroup.getId() + ",";
			}
			
			if (userGroups.size() > 0) {
				String urlGroupAuthorizations = camundaUrl + "/engine-rest/authorization";
				builder = UriComponentsBuilder.fromHttpUrl(urlGroupAuthorizations).queryParam("groupIdIn", URLEncoder.encode(listGroups, StandardCharsets.UTF_8.toString()));
				Collection<Authorization> groupsAuthorizations = Arrays.asList(((ResponseEntity<Authorization[]>) doGet(builder, Authorization[].class, user)).getBody());
				userAuthorizations.addAll(groupsAuthorizations);
			}

			builder = UriComponentsBuilder.fromHttpUrl(urlUsers).queryParam("type", 0);
			Collection<Authorization> globalAuthorizations = Arrays.asList(((ResponseEntity<Authorization[]>) doGet(builder, Authorization[].class, user)).getBody());
			userAuthorizations.addAll(globalAuthorizations);

			auths.setApplication(filterResources(userAuthorizations, resourceType(APPLICATION)));
			auths.setFilter(filterResources(userAuthorizations, resourceType(FILTER)));
			auths.setProcessDefinition(filterResources(userAuthorizations, resourceType(PROCESS_DEFINITION)));
			auths.setProcessInstance(filterResources(userAuthorizations, resourceType(PROCESS_INSTANCE)));
			auths.setTask(filterResources(userAuthorizations, resourceType(TASK)));
			auths.setAuthorization(filterResources(userAuthorizations, resourceType(AUTHORIZATION)));
			auths.setUser(filterResources(userAuthorizations, resourceType(USER)));
			auths.setGroup(filterResources(userAuthorizations, resourceType(GROUP)));
			auths.setDecisionDefinition(filterResources(userAuthorizations, resourceType(DECISION_DEFINITION)));
			auths.setDecisionRequirementsDefinition(filterResources(userAuthorizations, resourceType(DECISION_REQUIREMENTS_DEFINITION)));
			auths.setHistory(filterResources(userAuthorizations, resourceType(HISTORY)));
			auths.setDeployment(filterResources(userAuthorizations, resourceType(DEPLOYMENT)));
			auths.setCaseDefinition(filterResources(userAuthorizations, resourceType(CASE_DEFINITION)));
			auths.setCaseInstance(filterResources(userAuthorizations, resourceType(CASE_INSTANCE)));
			auths.setJobDefinition(filterResources(userAuthorizations, resourceType(JOB_DEFINITION)));
			auths.setBatch(filterResources(userAuthorizations, resourceType(BATCH)));
			auths.setGroupMembership(filterResources(userAuthorizations, resourceType(GROUP_MEMBERSHIP)));
			auths.setHistoricTask(filterResources(userAuthorizations, resourceType(HISTORIC_TASK)));
			auths.setHistoricProcessInstance(filterResources(userAuthorizations, resourceType(HISTORIC_PROCESS_INSTANCE)));
			auths.setTenant(filterResources(userAuthorizations, resourceType(TENANT)));
			auths.setTenantMembership(filterResources(userAuthorizations, resourceType(TENANT_MEMBERSHIP)));
			auths.setReport(filterResources(userAuthorizations, resourceType(REPORT)));
			auths.setDashboard(filterResources(userAuthorizations, resourceType(DASHBOARD)));
			auths.setUserOperationLogCategory(filterResources(userAuthorizations, resourceType(USER_OPERATION_LOG_CATEGORY)));
			auths.setSystem(filterResources(userAuthorizations, resourceType(SYSTEM)));
			auths.setMessage(filterResources(userAuthorizations, resourceType(MESSAGE)));
			auths.setEventSubscription(filterResources(userAuthorizations, resourceType(EVENT_SUBSCRIPTION)));
			
		} catch (UnsupportedEncodingException e) {
			throw new SystemException(e);
		}

		return auths;
	}
	
	public Collection<SevenUser> fetchUsers(CIBUser user) throws SystemException {
		String url = camundaUrl + "/engine-rest/user";
		return Arrays.asList(((ResponseEntity<SevenUser[]>) doGet(url, SevenUser[].class, user, false)).getBody());	
	}
	
	public SevenVerifyUser verifyUser(String username, String password, CIBUser user) throws SystemException {
		String url = camundaUrl + "/engine-rest/identity/verify";
		String body = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
		return ((ResponseEntity<SevenVerifyUser>) doPost(url, body, SevenVerifyUser.class, user)).getBody();	
	}	
	
	@Override
	public Collection<User> findUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike, Optional<String> lastName,
			Optional<String> lastNameLike, Optional<String> email, Optional<String> emailLike, Optional<String> memberOfGroup, Optional<String> memberOfTenant,
			Optional<String> idIn, Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder, CIBUser user) {
		
		if (!userProvider.equals("de.cib.cibflow.auth.SevenUserProvider")) {
			String url = createFindUserCaseInsensitiveUrl(id, firstName, firstNameLike, lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder); 
			return Arrays.asList(((ResponseEntity<User[]>) doGet(url, User[].class, user, true)).getBody());
		}

		// WORKAROUND for case insensitive search in case of SevenUserProvider (TODO should be moved to CIB seven)
		if (firstNameLike.isPresent()) { // javier, JAVIER, Javier
			String lowerCaseUrl = createFindUserCaseInsensitiveUrl(id, firstName, Optional.of(firstNameLike.get().toLowerCase()), lastName, lastNameLike, email, emailLike, memberOfGroup, 
					memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
			String upperCaseUrl = createFindUserCaseInsensitiveUrl(id, firstName, Optional.of(firstNameLike.get().toUpperCase()), lastName, lastNameLike, email, emailLike, memberOfGroup, 
					memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
			String normalCaseUrl = createFindUserCaseInsensitiveUrl(id, firstName, Optional.of(firstNameLike.get().substring(0, 2).toUpperCase() + firstNameLike.get().substring(2).toLowerCase()), 
					lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
			
	        Collection<User> lowerCaseResult = Arrays.asList(((ResponseEntity<User[]>) doGet(lowerCaseUrl, User[].class, user, true)).getBody());
	        Collection<User> upperCaseResult = Arrays.asList(((ResponseEntity<User[]>) doGet(upperCaseUrl, User[].class, user, true)).getBody());
	        Collection<User> normalCaseResult = Arrays.asList(((ResponseEntity<User[]>) doGet(normalCaseUrl, User[].class, user, true)).getBody());
	        
	        Collection<User> res = new ArrayList<User>();
	        res.addAll(lowerCaseResult);
	        res.addAll(upperCaseResult);
	        res.addAll(normalCaseResult);
	        
	        return res;
		}
		
		if (lastNameLike.isPresent()) { // medina, MEDINA, Medina
			String lowerCaseUrl = createFindUserCaseInsensitiveUrl(id, firstName, firstNameLike, lastName, Optional.of(lastNameLike.get().toLowerCase()), email, emailLike, memberOfGroup, 
					memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
			String upperCaseUrl = createFindUserCaseInsensitiveUrl(id, firstName, firstNameLike, lastName, Optional.of(lastNameLike.get().toUpperCase()), email, emailLike, memberOfGroup, 
					memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
			String normalCaseUrl = createFindUserCaseInsensitiveUrl(id, firstName, firstNameLike, lastName, Optional.of(lastNameLike.get().substring(0, 2).toUpperCase() + lastNameLike.get().substring(2).toLowerCase()), 
					email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder);
			
	        Collection<User> lowerCaseResult = Arrays.asList(((ResponseEntity<User[]>) doGet(lowerCaseUrl, User[].class, user, true)).getBody());
	        Collection<User> upperCaseResult = Arrays.asList(((ResponseEntity<User[]>) doGet(upperCaseUrl, User[].class, user, true)).getBody());
	        Collection<User> normalCaseResult = Arrays.asList(((ResponseEntity<User[]>) doGet(normalCaseUrl, User[].class, user, true)).getBody());
	        
	        Collection<User> res = new ArrayList<User>();
	        res.addAll(lowerCaseResult);
	        res.addAll(upperCaseResult);
	        res.addAll(normalCaseResult);
	        
	        return res;
		}
		
		String url = createFindUserCaseInsensitiveUrl(id, firstName, firstNameLike, lastName, lastNameLike, email, emailLike, memberOfGroup, memberOfTenant, idIn, firstResult, maxResults, sortBy, sortOrder); 
		return Arrays.asList(((ResponseEntity<User[]>) doGet(url, User[].class, user, true)).getBody());
	}
	
	private String createFindUserCaseInsensitiveUrl(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike, Optional<String> lastName, 
			Optional<String> lastNameLike, Optional<String> email, Optional<String> emailLike, Optional<String> memberOfGroup, Optional<String> memberOfTenant,
			Optional<String> idIn, Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder) {
		
		String url = camundaUrl + "/engine-rest/user";
		
		String param = "";
		
		String wcard = getWildcard();	
		
		if (firstNameLike.isPresent()) { // javier, JAVIER, Javier
			try {
				String fnDecoded = URLDecoder.decode(firstNameLike.get(), "UTF-8");
				param += addQueryParameter(param, "firstNameLike", Optional.of(fnDecoded.replace("*", wcard)), true);
			} catch (UnsupportedEncodingException e) {
				throw new SystemException(e);
			}
		}
		if (lastNameLike.isPresent()) { // medina, MEDINA, Medina
			try {
				String lnDecoded = URLDecoder.decode(lastNameLike.get(), "UTF-8");
				param += addQueryParameter(param, "lastNameLike", Optional.of(lnDecoded.replace("*", wcard)), true);
			} catch (UnsupportedEncodingException e) {
				throw new SystemException(e);
			}
		}
		if (emailLike.isPresent()) { 
			try {
				String eDecoded = URLDecoder.decode(emailLike.get(), "UTF-8");
				param += addQueryParameter(param, "emailLike", Optional.of(eDecoded.replace("*", wcard)), true);
			} catch (UnsupportedEncodingException e) {
				throw new SystemException(e);
			}
		}
		if (id.isPresent()) { 
			try {
				String eDecoded = URLDecoder.decode(id.get(), "UTF-8");
				param += addQueryParameter(param, "id", Optional.of(eDecoded), true);
			} catch (UnsupportedEncodingException e) {
				throw new SystemException(e);
			}
		}
		
		param += addQueryParameter(param, "firstName", firstName, true);
		param += addQueryParameter(param, "lastName", lastName, true);
		param += addQueryParameter(param, "email", email, true);
		param += addQueryParameter(param, "memberOfGroup", memberOfGroup, true);
		param += addQueryParameter(param, "memberOfTenant", memberOfTenant, true);
		param += addQueryParameter(param, "idIn", idIn, true);
		param += addQueryParameter(param, "firstResult", firstResult, true);
		param += addQueryParameter(param, "maxResults", maxResults, true);
		param += addQueryParameter(param, "sortBy", sortBy, true);
		param += addQueryParameter(param, "sortOrder", sortOrder, true);

		url += param;

		return url;
	}
	
	@Override
	public void createUser(NewUser user, CIBUser flowUser) throws InvalidUserIdException {
		String url = camundaUrl + "/engine-rest/user/create";

		try {
			//	A JSON object with the following properties:
			//	Name 	Type 	Description
			//	profile 	Array 	A JSON object containing variable key-value pairs. The object contains the following properties: id (String), firstName (String), lastName (String) and email (String).
			//	credentials 	Array 	A JSON object containing variable key-value pairs. The object contains the following property: password (String). 	 * 
			
			String body = "{\"profile\":"
					+ user.getProfile().json()
					+ ",\"credentials\":"
					+ user.getCredentials().json()
					+ "}";
		
			doPost(url, body , null, flowUser);

		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in createUser(...):", se);
			throw se;
		}
	}
	
	@Override
	public void updateUserProfile(String userId, User user, CIBUser flowUser) 
	{
		String url = camundaUrl + "/engine-rest/user/" + userId + "/profile";

		try 
		{
			doPut(url, user.json(), flowUser);
		} 
		catch (JsonProcessingException e) 
		{
			SystemException se = new SystemException(e);
			log.info("Exception in updateUserProfile(...):", se);
			throw se;
		}
	}

	@Override
	public void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user) {
		String url = camundaUrl + "/engine-rest/user/" + userId + "/credentials";

		doPut(url, data, user);
	}
	
	@Override
	public void addMemberToGroup(String groupId, String userId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/group/" + groupId + "/members/" + userId;
		doPut(url, "", user);
	}
	
	@Override
	public void deleteMemberFromGroup(String groupId, String userId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/group/" + groupId + "/members/" + userId;
		doDelete(url, user);
	}

	@Override
	public void deleteUser(String userId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/user/" + userId;

		doDelete(url, user);
	}
	
	@Override
	public SevenUser getUserProfile(String userId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/user/" + userId + "/profile";

		return doGet(url, SevenUser.class, null, false).getBody();
	}

	@Override
	public Collection<UserGroup> findGroups(Optional<String> id, Optional<String> name, Optional<String> nameLike, Optional<String> type, 
			Optional<String> member, Optional<String> memberOfTenant, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
			Optional<String> maxResults, CIBUser user) {
		String url = camundaUrl + "/engine-rest/group";
		
		String param = "";
		String wcard = getWildcard();

		if (nameLike.isPresent()) {
			try {
				String eDecoded = URLDecoder.decode(nameLike.get(), "UTF-8");
				param += addQueryParameter(param, "nameLike", Optional.of(eDecoded.replace("*", wcard)), true);
			} catch (UnsupportedEncodingException e) {
				throw new SystemException(e);
			}
		}

		param += addQueryParameter(param, "id", id, true);
		param += addQueryParameter(param, "name", name, true);
		param += addQueryParameter(param, "type", type, true);
		param += addQueryParameter(param, "member", member, true);
		param += addQueryParameter(param, "memberOfTenant", memberOfTenant, true);
		param += addQueryParameter(param, "sortBy", sortBy, true);
		param += addQueryParameter(param, "sortOrder", sortOrder, true);
		param += addQueryParameter(param, "firstResult", firstResult, true);
		param += addQueryParameter(param, "maxResults", maxResults, true);
		
		url += param;
		
		return Arrays.asList(((ResponseEntity<UserGroup[]>) doGet(url, UserGroup[].class, user, true)).getBody());
	}

	@Override
	public void createGroup(UserGroup group, CIBUser user) {
		String url = camundaUrl + "/engine-rest/group/create";

		try {
			doPost(url, group.json() , null, user);
		} 
		catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in createGroup(...):", se);
			throw se;
		}
	}

	@Override
	public void updateGroup(String groupId, UserGroup group, CIBUser user) {
		String url = camundaUrl + "/engine-rest/group/" + groupId;
		try {
			doPut(url, group.json(), user);
		} 
		catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in updateGroup(...):", se);
			throw se;
		}
	}

	@Override
	public void deleteGroup(String groupId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/group/" + groupId;
		doDelete(url, user);
	}

	@Override
	public Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type, Optional<String> userIdIn, Optional<String> groupIdIn, 
			Optional<String> resourceType, Optional<String> resourceId, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult,
			Optional<String> maxResults, CIBUser user) {
		String url = camundaUrl + "/engine-rest/authorization";
		
		String param = "";
		param += addQueryParameter(param, "id", id, true);
		param += addQueryParameter(param, "type", type, true);
		param += addQueryParameter(param, "userIdIn", userIdIn, true);
		param += addQueryParameter(param, "groupIdIn", groupIdIn, true);
		param += addQueryParameter(param, "resourceType", resourceType, true);
		param += addQueryParameter(param, "resourceId", resourceId, true);
		param += addQueryParameter(param, "sortBy", sortBy, true);
		param += addQueryParameter(param, "sortOrder", sortOrder, true);
		param += addQueryParameter(param, "firstResult", firstResult, true);
		param += addQueryParameter(param, "maxResults", maxResults, true);
		
		url += param;
		
		return Arrays.asList(((ResponseEntity<Authorization[]>) doGet(url, Authorization[].class, user, false)).getBody());
	}

	@Override
	public ResponseEntity<Authorization> createAuthorization(Authorization authorization, CIBUser user) {
		String url = camundaUrl + "/engine-rest/authorization/create";

		try {
			return doPost(url, authorization.json(), Authorization.class, user);
		} 
		catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in createAuthorization(...):", se);
			throw se;
		}
	}

	@Override
	public void updateAuthorization(String authorizationId, Map<String, Object> data, CIBUser user) {
		String url = camundaUrl + "/engine-rest/authorization/" + authorizationId;
		doPut(url, data, user);
	}

	@Override
	public void deleteAuthorization(String authorizationId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/authorization/" + authorizationId;
		doDelete(url, user);
	}

	@Override
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
	}
	
	private String getWildcard () {
		String wcard = "";
		if (!wildcard.equals("")) wcard = wildcard;
		else {
			if (userProvider.equals("org.cibseven.webapp.auth.LdapUserProvider") || userProvider.equals("org.cibseven.webapp.auth.AdfsUserProvider")) {
				wcard = "*";				
			} else wcard = "%";
		}
		return wcard;
	}
	
}
