package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.SevenTenant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TenantProvider extends SevenProviderBase implements ITenantProvider {
	
	@Value("${user.provider}") String userProvider;
	@Value("${users.search.wildcard:}") String wildcard;
	
	public Collection<SevenTenant> fetchTenants(CIBUser user) throws SystemException {
		String url = camundaUrl + "/engine-rest/tenant";
		return Arrays.asList(((ResponseEntity<SevenTenant[]>) doGet(url, SevenTenant[].class, user, false)).getBody());	
	}	

	public SevenTenant fetchTenant(String tenantId, CIBUser user) throws SystemException {
		String url = camundaUrl + "/engine-rest/tenant/" + tenantId;
		return ((ResponseEntity<SevenTenant>) doGet(url, SevenTenant.class, user, false)).getBody();
	}

	@Override
	public void createTenant(SevenTenant newTenant, CIBUser user) throws InvalidUserIdException {
		String url = camundaUrl + "/engine-rest/tenant/create";

		try {
			//	A JSON object with the following properties:
			//	Name 	Type 	Description
			//	id 	    String 	id (String)
			//	name 	String 	name (String) 
			
			String body = newTenant.json();		
			doPost(url, body , null, user);
			
		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in createUser(...):", se);
			throw se;
		}
	}
	
	@Override
	public void deleteTenant(String tenantId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/tenant/" + tenantId;
		doDelete(url, user);
	}

	@Override
	public void udpateTenant(SevenTenant tenant, CIBUser user)
	{		
		String url = camundaUrl + "/engine-rest/tenant/" + tenant.getId();

		try {
			//	A JSON object with the following properties:
			//	Name 	Type 	Description
			//	id 	    String 	id (String)
			//	name 	String 	name (String) 
			
			String body = tenant.json();		
			doPut(url, body , user);

		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in createUser(...):", se);
			throw se;
		}
	}
	
	@Override
	public void addMemberToTenant(String tenantId, String userId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/tenant/" + tenantId + "/user-members/" + userId;
		doPut(url, "", user);
	}	
	
	@Override
	public void deleteMemberFromTenant(String tenantId, String userId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/tenant/" + tenantId + "/user-members/" + userId;
		doDelete(url, user);
	}

	@Override
	public void addGroupToTenant(String tenantId, String groupId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/tenant/" + tenantId + "/group-members/" + groupId;
		doPut(url, "", user);
	}	
	
	@Override
	public void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/tenant/" + tenantId + "/group-members/" + groupId;
		doDelete(url, user);
	}

	@Override
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
	}
}
