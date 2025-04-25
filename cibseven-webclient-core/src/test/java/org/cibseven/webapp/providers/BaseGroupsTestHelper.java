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
