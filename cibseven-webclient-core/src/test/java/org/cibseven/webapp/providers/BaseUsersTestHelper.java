package org.cibseven.webapp.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Credentials;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.exception.ExistingUserRequestException;
import org.cibseven.webapp.exception.SystemException;

@Component
public class BaseUsersTestHelper {

	@Autowired
    private UserProvider userProvider;  
	
	public void createUser(String id, String firstName, String lastName, String password, CIBUser user) {
		try {
            User user1 = new User(id, firstName, lastName, null);
            Credentials credentials = new Credentials(password);        
            NewUser newUser = new NewUser(user1, credentials);
            userProvider.createUser(newUser, user);			
		} catch (ExistingUserRequestException e) { /* noop */}
	}
	
	public SevenVerifyUser verifyUser(String id, String password, CIBUser user) {
		try {
			return userProvider.verifyUser(id, password, user);
		}
		catch (SystemException e) {
			return null;
		}
	}	
}
