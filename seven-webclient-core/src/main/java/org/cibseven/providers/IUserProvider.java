package org.cibseven.providers;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.auth.CIBUser;
import org.cibseven.exception.InvalidUserIdException;
import org.cibseven.exception.SystemException;
import org.cibseven.rest.model.Authorization;
import org.cibseven.rest.model.Authorizations;
import org.cibseven.rest.model.SevenUser;
import org.cibseven.rest.model.SevenVerifyUser;
import org.cibseven.rest.model.NewUser;
import org.cibseven.rest.model.User;
import org.cibseven.rest.model.UserGroup;
import org.springframework.http.ResponseEntity;

public interface IUserProvider {

	public Authorizations getUserAuthorization(String userId, CIBUser user);	
	public Collection<SevenUser> fetchUsers(CIBUser user) throws SystemException;
	public SevenVerifyUser verifyUser(String username, String password, CIBUser user) throws SystemException;
	public Collection<User> findUsers(Optional<String> id, Optional<String> firstName, Optional<String> firstNameLike, Optional<String> lastName, Optional<String> lastNameLike,
			Optional<String> email, Optional<String> emailLike, Optional<String> memberOfGroup, Optional<String> memberOfTenant, Optional<String> idIn,
			Optional<String> firstResult, Optional<String> maxResults, Optional<String> sortBy, Optional<String> sortOrder, CIBUser user);

	public void createUser(NewUser user, CIBUser flowUser) throws InvalidUserIdException;
	public void updateUserProfile(String userId, User user, CIBUser flowUser);
	public void updateUserCredentials(String userId, Map<String, Object> data, CIBUser user);
	public void addMemberToGroup(String groupId, String userId, CIBUser user);
	public void deleteMemberFromGroup(String groupId, String userId, CIBUser user);
	public void deleteUser(String userId, CIBUser user);	
	public SevenUser getUserProfile(String userId, CIBUser user);
	
	public Collection<UserGroup> findGroups(Optional<String> id, Optional<String> name, Optional<String> nameLike, Optional<String> type,
			Optional<String> member, Optional<String> memberOfTenant, Optional<String> sortBy, Optional<String> sortOrder,
			Optional<String> firstResult, Optional<String> maxResults,CIBUser user);
	
	public void createGroup(UserGroup group, CIBUser user);
	public void updateGroup(String groupId, UserGroup group, CIBUser user);
	public void deleteGroup(String groupId, CIBUser user);
	public Collection<Authorization> findAuthorization(Optional<String> id, Optional<String> type, Optional<String> userIdIn, Optional<String> groupIdIn,
			Optional<String> resourceType, Optional<String> resourceId, Optional<String> sortBy, Optional<String> sortOrder, Optional<String> firstResult, 
			Optional<String> maxResults, CIBUser user);
	
	public ResponseEntity<Authorization> createAuthorization(Authorization authorization, CIBUser user);
	public void updateAuthorization(String authorizationId, Map<String, Object> data, CIBUser user);
	public void deleteAuthorization(String authorizationId, CIBUser user);	
	
}
