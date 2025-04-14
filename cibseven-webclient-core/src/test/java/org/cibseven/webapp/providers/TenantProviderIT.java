package org.cibseven.webapp.providers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.Credentials;
import org.cibseven.webapp.rest.model.SevenTenant;

import java.util.Collection;
import java.util.Optional;

@SpringBootTest(properties = {
	    "camunda.engineRest.url=http://localhost:8080"
	})
@ContextConfiguration(classes = {UserProvider.class, TenantProvider.class})
public class TenantProviderIT {

    @Autowired
    private UserProvider userProvider;
    
    @Autowired
    private TenantProvider tenantProvider;    
    
    // TODO: Rename or split it in other tests
    @Test
    public void testGeneral() {
    	// Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        // FetchUsers        
        Collection<SevenUser> users = userProvider.fetchUsers(user);        
        assertThat(users).isNotNull();

        // Create demo1 user
        if (users.stream().noneMatch(u -> "demo1".equals(u.getId())))
        {
            int usersSize = users.size();
            User user1 = new User("demo1", "demo1", "demo1", null);
            Credentials credentials = new Credentials();        
            NewUser newUser = new NewUser(user1, credentials);
            userProvider.createUser(newUser, user);

            // Check user creation
            users = userProvider.fetchUsers(user);        
            assertThat(users).isNotNull();
            assertThat(users).hasSize(usersSize + 1);
        }
        
        // Create demo2 user
        if (users.stream().noneMatch(u -> "demo2".equals(u.getId())))
        {
            int usersSize = users.size();
            User user2 = new User("demo2", "demo2", "demo2", null);
            Credentials credentials = new Credentials();        
            NewUser newUser = new NewUser(user2, credentials);
            userProvider.createUser(newUser, user);

            // Check user creation
            users = userProvider.fetchUsers(user);        
            assertThat(users).isNotNull();
            assertThat(users).hasSize(usersSize + 1);
        }        
        
        // Fetch tenants   
        Collection<SevenTenant> tenants = tenantProvider.fetchTenants(user);        
        assertThat(tenants).isNotNull();
        
    	// Create tenant1
        if (tenants.stream().noneMatch(u -> "tenant1".equals(u.getId())))
        {
            int tenantsSize = tenants.size();
            SevenTenant tenant = new SevenTenant("tenant1", "tenant1");
            tenantProvider.createTenant(tenant, user);

            // Check tenant creation
            tenants =  tenantProvider.fetchTenants(user);
            assertThat(tenants).isNotNull();
            assertThat(tenants).hasSize(tenantsSize + 1);
        }     
        
        // Create tenant2
        if (tenants.stream().noneMatch(u -> "tenant2".equals(u.getId())))
        {
            int tenantsSize = tenants.size();
            SevenTenant tenant = new SevenTenant("tenant2", "tenant2");
            tenantProvider.createTenant(tenant, user);

            // Check tenant creation
            tenants =  tenantProvider.fetchTenants(user);
            assertThat(tenants).isNotNull();
            assertThat(tenants).hasSize(tenantsSize + 1);
        }
        
        Collection<User> usersOfTenant1 = userProvider.findUsers(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("tenant1"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                user
        );               
        assertThat(usersOfTenant1).isNotNull();        
        if (usersOfTenant1.stream().noneMatch(u -> "demo1".equals(u.getId())))
        {
	        // Add demo to tenant1        
	        tenantProvider.addMemberToTenant("tenant1", "demo1", user);                
	        // Check membership
	        usersOfTenant1 = userProvider.findUsers(
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.of("tenant1"),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                user
	        );               
	        assertThat(usersOfTenant1).isNotNull();
	        assertThat(usersOfTenant1).anySatisfy(u -> assertThat(u.getId()).isEqualTo("demo1"));
        }
        
        Collection<User> usersOfTenant2 = userProvider.findUsers(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("tenant1"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                user
        );               
        assertThat(usersOfTenant2).isNotNull();        
        if (usersOfTenant2.stream().noneMatch(u -> "demo2".equals(u.getId())))
        {
	        // Add demo to tenant1        
	        tenantProvider.addMemberToTenant("tenant2", "demo2", user);                
	        // Check membership
	        usersOfTenant2 = userProvider.findUsers(
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.of("tenant2"),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.empty(),
	                Optional.of("asc"),
	                user
	        );               
	        assertThat(usersOfTenant2).isNotNull();
	        assertThat(usersOfTenant2).anySatisfy(u -> assertThat(u.getId()).isEqualTo("demo2"));
        }
    }
}
