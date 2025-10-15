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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.TestRestTemplateConfiguration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@ContextConfiguration(classes = {UserProvider.class, TestRestTemplateConfiguration.class, MockUserProviderTestConfiguration.class})
public class UserProviderIT extends BaseHelper {

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
        ReflectionTestUtils.setField(userProvider, "cibsevenUrl", mockBaseUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testFetchUsers() throws Exception {
        // Arrange
        CIBUser user = getCibUser();

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
        CIBUser user = getCibUser();

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
        CIBUser user = getCibUser();

        String mockResponseBody = loadMockResponse("mocks/verify_user_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        SevenVerifyUser result = userProvider.verifyUser(username, password, user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
    }    

}
