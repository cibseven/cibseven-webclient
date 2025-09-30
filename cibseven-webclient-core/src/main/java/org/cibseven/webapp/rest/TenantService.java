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
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Tenant;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
	@ApiResponse(responseCode= "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode= "401", description = "Unauthorized")
})
@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/tenant")
public class TenantService extends BaseService implements InitializingBean {

	@Autowired BpmProvider bpmProvider;
	
	public void afterPropertiesSet() {
	}

	@GetMapping
    public Collection<Tenant> getTenants(@RequestParam Map<String, Object> queryParams, CIBUser user) {
        return bpmProvider.fetchTenants(queryParams, user);
    }

    @GetMapping("/{tenantId}")
    public Tenant getTenant(@PathVariable String tenantId, CIBUser user) {
        return bpmProvider.fetchTenant(tenantId, user);
    }

    @PostMapping
    public ResponseEntity<Void> createTenant(@RequestBody Tenant tenant, CIBUser user) {
    	bpmProvider.createTenant(tenant, user);
      // return 204 No Content, no body
      return ResponseEntity.noContent().build();
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<Void> updateTenant(@PathVariable String tenantId, @RequestBody Tenant tenant, CIBUser user) {
        tenant.setId(tenantId);
        bpmProvider.updateTenant(tenant, user);
        // return 204 No Content, no body
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@PathVariable String tenantId, CIBUser user) {
    	bpmProvider.deleteTenant(tenantId, user);
        // return 204 No Content, no body
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tenantId}/users/{userId}")
    public ResponseEntity<Void> addUserToTenant(@PathVariable String tenantId, @PathVariable String userId, CIBUser user) {
    	bpmProvider.addMemberToTenant(tenantId, userId, user);
        // return 204 No Content, no body
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tenantId}/users/{userId}")
    public ResponseEntity<Void> removeUserFromTenant(@PathVariable String tenantId, @PathVariable String userId, CIBUser user) {
    	bpmProvider.deleteMemberFromTenant(tenantId, userId, user);
        // return 204 No Content, no body
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tenantId}/groups/{groupId}")
    public ResponseEntity<Void> addGroupToTenant(@PathVariable String tenantId, @PathVariable String groupId, CIBUser user) {
    	bpmProvider.addGroupToTenant(tenantId, groupId, user);
        // return 204 No Content, no body
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tenantId}/groups/{groupId}")
    public ResponseEntity<Void> removeGroupFromTenant(@PathVariable String tenantId, @PathVariable String groupId, CIBUser user) {
    	bpmProvider.deleteGroupFromTenant(tenantId, groupId, user);
        // return 204 No Content, no body
        return ResponseEntity.noContent().build();
    }

}
