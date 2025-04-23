package org.cibseven.webapp.providers;

import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.ExistingGroupRequestException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

@Component
public class BaseGroupsTestHelper {

	@Autowired
    private UserProvider userProvider; 
	
	public void createGroup(String groupId, String groupName, String groupType, CIBUser user) {
		try {
            UserGroup newGroup = new UserGroup(groupId, groupName, groupType);
            userProvider.createGroup(newGroup, user);	
		} catch (ExistingGroupRequestException e) { /* noop */}
	}
	
	public void deleteGroup(String groupId, CIBUser user) {
		// deleteGroup does not throw exception if group does not exist
        userProvider.deleteGroup(groupId, user);
	}
	
	public UserGroup verifyGroup(String groupId, CIBUser user) {
		 Collection<UserGroup> groups = userProvider.findGroups(
            Optional.of(groupId),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            user);               
        return groups.stream()
        		.filter(f -> groupId.equals(f.getId()))
				.findFirst()
				.orElse(null);
	}
}
