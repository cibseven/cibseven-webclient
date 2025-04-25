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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import org.cibseven.webapp.auth.CIBUser;

@SpringBootTest(properties = {
	    "camunda.engineRest.url=http://localhost:8080"
	})
@ContextConfiguration(classes = {
		BaseUsersTestHelper.class,
		UserProvider.class,
		BaseTenantsTestHelper.class,
		TenantProvider.class,
		BaseGroupsTestHelper.class})
public class TenantProviderIT {
	
	@Autowired
	private BaseUsersTestHelper baseUsersTestHelper;
	
	@Autowired
	private BaseGroupsTestHelper baseGroupsTestHelper;
	
	@Autowired
	private BaseTenantsTestHelper baseTenantsTestHelper;
    
    @Test
    public void testTenantCreation() {
    	// Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");
        
    	baseTenantsTestHelper.createTenant("tenantDemo1", "tenantDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user)).isNotNull();
    	
    	baseTenantsTestHelper.createTenant("tenantDemo2", "tenantDemo2", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo2", user)).isNotNull();
    	
    	baseTenantsTestHelper.deleteTenant("tenantDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user)).isNull();
    	
    	baseTenantsTestHelper.deleteTenant("tenantDemo2", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo2", user)).isNull();
    }   
    
    @Test
    public void testTenantUpdate() {
    	// Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");
        
    	baseTenantsTestHelper.createTenant("tenantDemo1", "tenantDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user)).isNotNull();
    	
    	baseTenantsTestHelper.updateTenant("tenantDemo1", "tenantDemoUpdated", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user).getName()).isEqualTo("tenantDemoUpdated");
    	
    	baseTenantsTestHelper.deleteTenant("tenantDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user)).isNull();
    }
    
    @Test
    public void testUserMembershipToTenant() {
    	// Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        // Create demo1 user
        baseUsersTestHelper.createUser("demo1", "demo1", "demo1", "", user);
        assertThat(baseUsersTestHelper.verifyUser("demo1", "", user).isAuthenticated()).isTrue();
        
        // Create tenant        
        baseTenantsTestHelper.createTenant("tenantDemo1", "tenantDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user)).isNotNull();
    	
    	// Add demo1 user to tenant
    	baseTenantsTestHelper.addMemberToTenant("tenantDemo1", "demo1", user);
    	assertThat(baseTenantsTestHelper.verifyUserMembershipToTenant("tenantDemo1", "demo1", user)).isNotNull();

		// Remove demo1 user from tenant
    	baseTenantsTestHelper.deleteMemberFromTenant("tenantDemo1", "demo1", user);
    	assertThat(baseTenantsTestHelper.verifyUserMembershipToTenant("tenantDemo1", "demo1", user)).isNull();
    	
    	// Remove tenant
    	baseTenantsTestHelper.deleteTenant("tenantDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user)).isNull();
    	
    	// Remove demo1 user
    	baseUsersTestHelper.deleteUser("demo1", user);
        assertThat(baseUsersTestHelper.verifyUser("demo1", "", user).isAuthenticated()).isFalse();
    }
    
    @Test
    public void testGroupMembershipToTenant() {
    	// Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");
        
        // Create group
        baseGroupsTestHelper.createGroup("groupDemo1", "groupDemo1", "WORKFLOW", user);
        assertThat(baseGroupsTestHelper.verifyGroup("groupDemo1", user)).isNotNull();
        
        // Create tenant        
        baseTenantsTestHelper.createTenant("tenantDemo1", "tenantDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user)).isNotNull();
    	
    	// Add groupDemo1 to tenant
    	baseTenantsTestHelper.addGroupToTenant("tenantDemo1", "groupDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyGroupMembershipToTenant("tenantDemo1", "groupDemo1", user)).isNotNull();

		// Remove demo1 user from tenant
    	baseTenantsTestHelper.deleteGroupFromTenant("tenantDemo1", "groupDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyGroupMembershipToTenant("tenantDemo1", "groupDemo1", user)).isNull();
    	
    	// Remove tenant
    	baseTenantsTestHelper.deleteTenant("tenantDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user)).isNull();
    	
    	// Remove group
    	baseGroupsTestHelper.deleteGroup("groupDemo1", user);
        assertThat(baseGroupsTestHelper.verifyGroup("groupDemo1", user)).isNull();
    }
}
