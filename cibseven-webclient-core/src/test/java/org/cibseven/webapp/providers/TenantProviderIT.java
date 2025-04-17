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
		TenantProvider.class})
public class TenantProviderIT {
	
	@Autowired
	private BaseUsersTestHelper baseUsersTestHelper;
	
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
        assertThat(baseUsersTestHelper.verifyUser("demo1", "", user)).isNotNull();
        
        // Create tenant        
        baseTenantsTestHelper.createTenant("tenantDemo1", "tenantDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user)).isNotNull();
    	
    	// Add demo1 user to tenant
    	baseTenantsTestHelper.addMemberToTenant("tenantDemo1", "demo1", user);
    	assertThat(baseTenantsTestHelper.verifyUserMembershipToTenant("tenantDemo1", "demo1", user)).isNull();

		// Remove demo1 user from tenant
    	baseTenantsTestHelper.deleteMemberFromTenant("tenantDemo1", "demo1", user);
    	assertThat(baseTenantsTestHelper.verifyUserMembershipToTenant("tenantDemo1", "demo1", user)).isNull();
    	
    	// Remove tenant
    	baseTenantsTestHelper.deleteTenant("tenantDemo1", user);
    	assertThat(baseTenantsTestHelper.verifyTenant("tenantDemo1", user)).isNull();
    }
    
    @Test
    public void testGroupMembershipToTenant() {
    	// TODO
    }
}
