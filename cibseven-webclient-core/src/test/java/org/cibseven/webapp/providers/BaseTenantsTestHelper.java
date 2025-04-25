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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.cibseven.webapp.rest.model.Tenant;
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
		// ExistingTenantRequestException not implemented
		if (verifyTenant(tenantId, user) == null) {
			Tenant newTenant = new Tenant(tenantId, tenantName);
			tenantProvider.createTenant(newTenant, user);			
		}
	}
	
	public void deleteTenant(String tenantId, CIBUser user) {
		// deleteTenant does not throw exception if tenant does not exist
		tenantProvider.deleteTenant(tenantId, user);
	}
	
	public Tenant verifyTenant(String tenantId, CIBUser user) {
		try {
			return tenantProvider.fetchTenant(tenantId, user);
		}
		catch (SystemException e) {
			return null;
		}
	}
	
	public void updateTenant(String tenantId, String tenantName, CIBUser user) {
		if (verifyTenant(tenantId, user) != null) {
			Tenant newTenant = new Tenant(tenantId, tenantName);
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
                Optional.of(tenantId),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                user
        );               
        return usersOfTenant1.stream()
        		.filter(u -> userId.equals(u.getId()))
				.findFirst()
				.orElse(null);
	}
	
	public void addGroupToTenant(String tenantId, String groupId, CIBUser user) {
		if (verifyGroupMembershipToTenant(tenantId, groupId, user) == null) {
			tenantProvider.addGroupToTenant(tenantId, groupId, user);        	
		}
	}

	public void deleteGroupFromTenant(String tenantId, String groupId, CIBUser user) {
		if (verifyGroupMembershipToTenant(tenantId, groupId, user) != null) {
			tenantProvider.deleteGroupFromTenant(tenantId, groupId, user);
		}
	}
	
	public UserGroup verifyGroupMembershipToTenant(String tenantId, String groupId, CIBUser user) {
		Collection<UserGroup> groupsOfTenant = userProvider.findGroups(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),				
                Optional.empty(),
                Optional.empty(),
                Optional.of(tenantId),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(), 
                Optional.empty(), 
				user);        
		return groupsOfTenant.stream()
	 		.filter(g -> groupId.equals(g.getId()))
				.findFirst()
				.orElse(null);	
	}	
}
