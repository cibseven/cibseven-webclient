package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Tenant;

public interface ITenantProvider {
	
	public Collection<Tenant> fetchTenants(Map<String, Object> queryParams, CIBUser user) throws SystemException;
	public Tenant fetchTenant(String tenantId, CIBUser user) throws SystemException;
	
	public void createTenant(Tenant newTenant, CIBUser user) throws InvalidUserIdException;
	public void deleteTenant(String tenantId, CIBUser user);
	public void udpateTenant(Tenant tenant, CIBUser user) throws InvalidUserIdException;
	
	public void addMemberToTenant(String tenantId, String userId, CIBUser user);
	public void deleteMemberFromTenant(String tenantId, String userId, CIBUser user);
	
	public void addGroupToTenant(String tenantId, String groupId, CIBUser user);
	public void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user);
}
