package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.SevenTenant;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.springframework.http.ResponseEntity;

public interface ITenantProvider {
	
	public Collection<SevenTenant> fetchTenants(CIBUser user) throws SystemException;
	
	public void createTenant(SevenTenant newTenant, CIBUser user) throws InvalidUserIdException;
	public void deleteTenant(String tenantId, CIBUser user);
	
	public void addMemberToTenant(String tenantId, String userId, CIBUser user);
	public void deleteMemberFromTenant(String tenantId, String userId, CIBUser user);
	
	public void addGroupToTenant(String tenantId, String groupId, CIBUser user);
	public void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user);

	/*
	public void updateUserProfile(String userId, User user, CIBUser flowUser);
	public void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user);
	*/
}
