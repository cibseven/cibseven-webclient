package org.cibseven.providers;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import org.cibseven.auth.CIBUser;

import org.cibseven.rest.model.User;
import org.cibseven.rest.model.SevenVerifyUser;
import org.cibseven.rest.model.SevenUser;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@ContextConfiguration(classes = {UserProvider.class})
public class UserProviderIT {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }
	
    private MockWebServer mockWebServer;

    @Autowired
    private UserProvider userProvider;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String mockBaseUrl = mockWebServer.url("/").toString();
        ReflectionTestUtils.setField(userProvider, "camundaUrl", mockBaseUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    private String loadMockResponse(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(filePath).toURI())));
    }
    
    @Test
    void testFetchUsers() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String mockResponseBody = loadMockResponse("mocks/users_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<SevenUser> users = userProvider.fetchUsers(user);

        // Assert
        assertThat(users).isNotNull();
        assertThat(users).hasSize(2);

        SevenUser firstUser = users.iterator().next();
        assertThat(firstUser.getId()).isEqualTo("user-1");
        assertThat(firstUser.getFirstName()).isEqualTo("John");
    }

    @Test
    void testFindUsers() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String mockResponseBody = loadMockResponse("mocks/users_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<User> users = userProvider.findUsers(
                Optional.empty(),
                Optional.of("John"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("firstName"),
                Optional.of("asc"),
                user
        );

        // Assert
        assertThat(users).isNotNull();
        assertThat(users).hasSize(2);

        User firstUser = users.iterator().next();
        assertThat(firstUser.getId()).isEqualTo("user-1");
        assertThat(firstUser.getFirstName()).isEqualTo("John");
    }
    
    @Test
    void testVerifyUser() throws Exception {
        // Arrange
        String username = "john";
        String password = "password123";
        CIBUser cibUser = new CIBUser();
        cibUser.setAuthToken("Bearer token");
        String mockResponseBody = loadMockResponse("mocks/verify_user_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        SevenVerifyUser result = userProvider.verifyUser(username, password, cibUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
    }    

}
