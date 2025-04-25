/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cibseven.webapp.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Credentials;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.exception.ExistingUserRequestException;

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
	
	public void deleteUser(String id, CIBUser user) {
		// deleteUser does not throw exception if user does not exist
        userProvider.deleteUser(id, user);
	}
	
	public SevenVerifyUser verifyUser(String id, String password, CIBUser user) {
		return userProvider.verifyUser(id, password, user);
	}
}
