package org.cibseven.webapp.providers;

import java.util.Collection;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.SevenTenant;

public interface ITenantProvider {
	
	public Collection<SevenTenant> fetchTenants(CIBUser user) throws SystemException;
	public SevenTenant fetchTenant(String tenantId, CIBUser user) throws SystemException;
	
	public void createTenant(SevenTenant newTenant, CIBUser user) throws InvalidUserIdException;
	public void deleteTenant(String tenantId, CIBUser user);
	public void udpateTenant(SevenTenant tenant, CIBUser user) throws InvalidUserIdException;
	
	public void addMemberToTenant(String tenantId, String userId, CIBUser user);
	public void deleteMemberFromTenant(String tenantId, String userId, CIBUser user);
	
	public void addGroupToTenant(String tenantId, String groupId, CIBUser user);
	public void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user);
}
