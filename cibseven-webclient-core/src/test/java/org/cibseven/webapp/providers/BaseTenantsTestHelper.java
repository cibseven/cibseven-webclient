package org.cibseven.webapp.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.SevenTenant;
import org.cibseven.webapp.exception.SystemException;

import java.util.Collection;
import java.util.Optional;

@Component
public class BaseTenantsTestHelper {
	
	@Autowired
    private UserProvider userProvider;  
	
    @Autowired
    private TenantProvider tenantProvider;  
    
	public void createTenant(String tenantId, String tenantName, CIBUser user) {
		if (verifyTenant(tenantId, user) == null) {
			SevenTenant newTenant = new SevenTenant(tenantId, tenantName);
			tenantProvider.createTenant(newTenant, user);			
		}
	}
	
	public void deleteTenant(String tenantId, CIBUser user) {
		if (verifyTenant(tenantId, user) != null) {
			tenantProvider.deleteTenant(tenantId, user);
		}
	}
	
	public SevenTenant verifyTenant(String tenantId, CIBUser user) {
		try {
			return tenantProvider.fetchTenant(tenantId, user);
		}
		catch (SystemException e) {
			return null;
		}
	}
	
	public void updateTenant(String tenantId, String tenantName, CIBUser user) {
		if (verifyTenant(tenantId, user) != null) {
			SevenTenant newTenant = new SevenTenant(tenantId, tenantName);
			tenantProvider.udpateTenant(newTenant, user);			
		}
	}
	
	public void addMemberToTenant(String tenantId, String userId, CIBUser user) {
        if (verifyUserMembershipToTenant(tenantId, userId, user) == null) {
    		tenantProvider.addMemberToTenant(tenantId, userId, user);        	
        }
    }
	
	public void deleteMemberFromTenant(String tenantId, String userId, CIBUser user) {
		if (verifyUserMembershipToTenant(tenantId, userId, user) != null) {
			tenantProvider.deleteMemberFromTenant(tenantId, userId, user);
		}
    }
	
	public User verifyUserMembershipToTenant(String tenantId, String userId, CIBUser user) {
		Collection<User> usersOfTenant1 = userProvider.findUsers(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("tenantId"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                user
        );               
        return usersOfTenant1.stream()
        		.filter(u -> userId.equals(userId))
				.findFirst()
				.orElse(null);
	}
	
	/*public void addGroupToTenant(String tenantId, String groupId, CIBUser user) {
		if (verifyGroupMembershipToTenant(tenantId, groupId, user) == null) {
			tenantProvider.addGroupToTenant(tenantId, groupId, user);        	
		}
	}

	public void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user) {
		if (verifyGroupMembershipToTenant(tenantId, groupId, user) != null) {
			tenantProvider.deleteGroupFromTenant(tenantId, groupId, user);
		}
	}
	
	public User verifyGroupMembershipToTenant(String tenantId, String groupId, CIBUser user) {
		
	}
	*/
}
