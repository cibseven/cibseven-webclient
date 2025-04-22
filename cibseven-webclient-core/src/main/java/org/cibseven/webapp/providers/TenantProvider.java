package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Tenant;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TenantProvider extends SevenProviderBase implements ITenantProvider {
	
	private String buildUrlWithParams(String path, Map<String, Object> queryParams) {
	    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(camundaUrl + "/engine-rest" + path);
	    queryParams.forEach((key, value) -> {
	        if (value != null) {
	            builder.queryParam(key, value);
	        }
	    });
	    return builder.toUriString();
	}
	
	@Override
	public Collection<Tenant> fetchTenants(Map<String, Object> queryParams, CIBUser user) throws SystemException {
		String url = buildUrlWithParams("/tenant", queryParams);
		return Arrays.asList(((ResponseEntity<Tenant[]>) doGet(url, Tenant[].class, user, false)).getBody());	
	}	

	@Override
	public Tenant fetchTenant(String tenantId, CIBUser user) throws SystemException {
		String url = camundaUrl + "/engine-rest/tenant/" + tenantId;
		return ((ResponseEntity<Tenant>) doGet(url, Tenant.class, user, false)).getBody();
	}

	@Override
	public void createTenant(Tenant newTenant, CIBUser user) throws InvalidUserIdException {
		String url = camundaUrl + "/engine-rest/tenant/create";
		try {
			doPost(url, newTenant.json(), null, user);
		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.error("Exception in createTenant(...):", se);
			throw se;
		}
	}
	
	@Override
	public void deleteTenant(String tenantId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/tenant/" + tenantId;
		doDelete(url, user);
	}

	@Override
	public void udpateTenant(Tenant tenant, CIBUser user) {		
		String url = camundaUrl + "/engine-rest/tenant/" + tenant.getId();
		try {		
			doPut(url, tenant.json() , user);
		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.error("Exception in updateTenant(...):", se);
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

}
