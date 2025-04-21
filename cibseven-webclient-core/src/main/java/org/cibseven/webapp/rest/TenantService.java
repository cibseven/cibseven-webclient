package org.cibseven.webapp.rest;

import java.util.Collection;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Metric;
import org.cibseven.webapp.rest.model.Tenant;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
	@ApiResponse(responseCode= "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode= "401", description = "Unauthorized")
})
@RestController @RequestMapping("${services.basePath:/services/v1}" + "/tenant")
public class TenantService extends BaseService implements InitializingBean {

	@Autowired BpmProvider bpmProvider;
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("SystemService expects a BpmProvider");
	}

	@GetMapping
    public Collection<Tenant> getAllTenants(CIBUser user) {
        return bpmProvider.fetchTenants(user);
    }

    @GetMapping("/{tenantId}")
    public Tenant getTenant(@PathVariable String tenantId, CIBUser user) {
        return bpmProvider.fetchTenant(tenantId, user);
    }

    @PostMapping
    public void createTenant(@RequestBody Tenant tenant, CIBUser user) {
    	bpmProvider.createTenant(tenant, user);
    }

    @PutMapping("/{tenantId}")
    public void updateTenant(@PathVariable String tenantId, @RequestBody Tenant tenant, CIBUser user) {
        tenant.setId(tenantId); // ensure consistency
        bpmProvider.udpateTenant(tenant, user);
    }

    @DeleteMapping("/{tenantId}")
    public void deleteTenant(@PathVariable String tenantId, CIBUser user) {
    	bpmProvider.deleteTenant(tenantId, user);
    }

    @PostMapping("/{tenantId}/users/{userId}")
    public void addUserToTenant(@PathVariable String tenantId, @PathVariable String userId, CIBUser user) {
    	bpmProvider.addMemberToTenant(tenantId, userId, user);
    }

    @DeleteMapping("/{tenantId}/users/{userId}")
    public void removeUserFromTenant(@PathVariable String tenantId, @PathVariable String userId, CIBUser user) {
    	bpmProvider.deleteMemberFromTenant(tenantId, userId, user);
    }

    @PostMapping("/{tenantId}/groups/{groupId}")
    public void addGroupToTenant(@PathVariable String tenantId, @PathVariable String groupId, CIBUser user) {
    	bpmProvider.addGroupToTenant(tenantId, groupId, user);
    }

    @DeleteMapping("/{tenantId}/groups/{groupId}")
    public void removeGroupFromTenant(@PathVariable String tenantId, @PathVariable String groupId, CIBUser user) {
    	bpmProvider.deleteGroupFromTenant(tenantId, groupId, user);
    }

}
