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
	    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getEngineRestUrl() + path);
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
		String url = getEngineRestUrl() + "/tenant/" + tenantId;
		return ((ResponseEntity<Tenant>) doGet(url, Tenant.class, user, false)).getBody();
	}

	@Override
	public void createTenant(Tenant newTenant, CIBUser user) throws InvalidUserIdException {
		String url = getEngineRestUrl() + "/tenant/create";
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
		String url = getEngineRestUrl() + "/tenant/" + tenantId;
		doDelete(url, user);
	}

	@Override
	public void updateTenant(Tenant tenant, CIBUser user) {
		String url = getEngineRestUrl() + "/tenant/" + tenant.getId();
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
		String url = getEngineRestUrl() + "/tenant/" + tenantId + "/user-members/" + userId;
		doPut(url, "", user);
	}	
	
	@Override
	public void deleteMemberFromTenant(String tenantId, String userId, CIBUser user) {
		String url = getEngineRestUrl() + "/tenant/" + tenantId + "/user-members/" + userId;
		doDelete(url, user);
	}

	@Override
	public void addGroupToTenant(String tenantId, String groupId, CIBUser user) {
		String url = getEngineRestUrl() + "/tenant/" + tenantId + "/group-members/" + groupId;
		doPut(url, "", user);
	}	
	
	@Override
	public void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user) {
		String url = getEngineRestUrl() + "/tenant/" + tenantId + "/group-members/" + groupId;
		doDelete(url, user);
	}

}
