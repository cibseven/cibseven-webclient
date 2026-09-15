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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.TestRestTemplateConfiguration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

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

        // The provider is a context-scoped singleton: without this, what one test remembered about the
        // self endpoint would decide which path the next one takes.
        selfAuthorizationEndpointUnsupported().clear();
    }

    @SuppressWarnings("unchecked")
    private Set<String> selfAuthorizationEndpointUnsupported() {
        return (Set<String>) ReflectionTestUtils.getField(userProvider, "selfAuthorizationEndpointUnsupported");
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
    void testGetUserAuthorizationUsesTheSelfEndpoint() throws Exception {
        CIBUser user = getCibUser();

        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"application-grant\",\"type\":1,\"permissions\":[\"ACCESS\"],\"userId\":\"demo\","
                        + "\"resourceType\":0,\"resourceId\":\"*\"},"
                        + "{\"id\":\"group-grant\",\"type\":1,\"permissions\":[\"READ\"],\"groupId\":\"sales\","
                        + "\"resourceType\":6,\"resourceId\":\"invoice\"},"
                        + "{\"id\":\"global-grant\",\"type\":0,\"permissions\":[\"READ\"],\"userId\":\"*\","
                        + "\"resourceType\":7,\"resourceId\":\"*\"}]")
                .addHeader("Content-Type", "application/json"));

        Authorizations authorizations = userProvider.getUserAuthorization(user);

        // A single request replaces the three of the legacy path: /self already answers with the
        // authorizations of the user, of its groups and the global ones.
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/engine-rest/authorization/self");

        assertThat(authorizations.getApplication()).extracting(Authorization::getId).containsExactly("application-grant");
        assertThat(authorizations.getProcessDefinition()).extracting(Authorization::getId).containsExactly("group-grant");
        assertThat(authorizations.getTask()).extracting(Authorization::getId).containsExactly("global-grant");
        assertThat(authorizations.getFilter()).isEmpty();
    }

    /**
     * A denied request is the engine's answer, not a missing endpoint: it has to reach the caller
     * instead of being retried against the legacy queries.
     */
    @Test
    void testGetUserAuthorizationDoesNotFallBackWhenSelfEndpointReturns403() {
        CIBUser user = getCibUser();

        mockWebServer.enqueue(new MockResponse().setResponseCode(403));

        assertThatThrownBy(() -> userProvider.getUserAuthorization(user)).isInstanceOf(RuntimeException.class);
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    /**
     * Whether the engine offers the endpoint cannot change while it is running, so the rejected request
     * is made once and not on every authorization fetch.
     */
    @Test
    void testGetUserAuthorizationRemembersThatTheSelfEndpointIsUnsupported() throws Exception {
        CIBUser user = getCibUser();

        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        enqueueLegacyAuthorizationResponses();
        enqueueLegacyAuthorizationResponses();

        assertThat(userProvider.getUserAuthorization(user)).isNotNull();
        assertThat(userProvider.getUserAuthorization(user)).isNotNull();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(7);
        assertThat(recordedPaths(7)).containsExactly(
                "/engine-rest/authorization/self",
                "/engine-rest/authorization?userIdIn=demo",
                "/engine-rest/group?member=demo",
                "/engine-rest/authorization?type=0",
                "/engine-rest/authorization?userIdIn=demo",
                "/engine-rest/group?member=demo",
                "/engine-rest/authorization?type=0");
    }

    /**
     * The 401 counterpart: it is also raised when the credential is simply not valid (any more), so
     * remembering it would keep the legacy path after the credential is renewed.
     */
    @Test
    void testGetUserAuthorizationRetriesTheSelfEndpointAfterUnauthorized() throws Exception {
        CIBUser user = getCibUser();

        mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        enqueueLegacyAuthorizationResponses();
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        enqueueLegacyAuthorizationResponses();

        assertThat(userProvider.getUserAuthorization(user)).isNotNull();
        assertThat(userProvider.getUserAuthorization(user)).isNotNull();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(8);
        assertThat(recordedPaths(8)).containsExactly(
                "/engine-rest/authorization/self",
                "/engine-rest/authorization?userIdIn=demo",
                "/engine-rest/group?member=demo",
                "/engine-rest/authorization?type=0",
                "/engine-rest/authorization/self",
                "/engine-rest/authorization?userIdIn=demo",
                "/engine-rest/group?member=demo",
                "/engine-rest/authorization?type=0");
    }

    @Test
    void testGetUserAuthorizationFallsBackWhenSelfEndpointIsResolvedAsAuthorizationId() throws Exception {
        CIBUser user = getCibUser();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("Authorization with id self does not exist."));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        assertThat(userProvider.getUserAuthorization(user)).isNotNull();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(4);

        RecordedRequest selfRequest = mockWebServer.takeRequest();
        RecordedRequest userRequest = mockWebServer.takeRequest();
        RecordedRequest groupRequest = mockWebServer.takeRequest();
        RecordedRequest globalRequest = mockWebServer.takeRequest();

        assertThat(selfRequest.getPath()).isEqualTo("/engine-rest/authorization/self");
        assertThat(userRequest.getPath()).isEqualTo("/engine-rest/authorization?userIdIn=demo");
        assertThat(groupRequest.getPath()).isEqualTo("/engine-rest/group?member=demo");
        assertThat(globalRequest.getPath()).isEqualTo("/engine-rest/authorization?type=0");
    }

    @Test
    void testGetUserAuthorizationFallsBackWhenSelfEndpointReturns405() throws Exception {
        testGetUserAuthorizationFallsBackForStatus(405);
    }

    @Test
    void testGetUserAuthorizationFallsBackWhenSelfEndpointReturns401() throws Exception {
        testGetUserAuthorizationFallsBackForStatus(401);
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
                Optional.empty(),
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
        StandardLogin login = new StandardLogin(username, password);
        SevenVerifyUser result = userProvider.verifyUser(login, user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    void testVerifyUserConcurrent5_0() throws Exception {
        verifyUserConcurrent(5, 0);
    }

    @Test
    void testVerifyUserConcurrent0_5() throws Exception {
        verifyUserConcurrent(0, 5);
    }

    @Test
    void testVerifyUserConcurrent1_5() throws Exception {
        verifyUserConcurrent(1, 5);
    }

    @Test
    void testVerifyUserConcurrent5_1() throws Exception {
        verifyUserConcurrent(5, 1);
    }

    @Test
    void testVerifyUserConcurrent5_5() throws Exception {
        verifyUserConcurrent(5, 5);
    }

    void verifyUserConcurrent(int numberOfCallsSuccess, int numberOfCallsFailed) throws Exception {
        // Arrange
        String username = "john";
        String password = "password123";

        String mockResponseBody = loadMockResponse("mocks/verify_user_mock.json");
        String mockResponseBodyFailed = "{\n" +
                "  \"authenticated\": false,\n" +
                "  \"userId\": \"john\"\n" +
                "}";

        // Enqueue responses for each parallel call
        for (int i = 0; i < numberOfCallsSuccess; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(mockResponseBody)
                    .addHeader("Content-Type", "application/json"));
        }

        for (int i = 0; i < numberOfCallsFailed; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(mockResponseBodyFailed)
                    .addHeader("Content-Type", "application/json"));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfCallsSuccess + numberOfCallsFailed);

        try {
            // Act - Create parallel CompletableFuture tasks
            List<CompletableFuture<SevenVerifyUser>> futures = IntStream.range(0, numberOfCallsSuccess + numberOfCallsFailed)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        try {
                            CIBUser user = getCibUser();
                            StandardLogin login = new StandardLogin(username, password);
                            return userProvider.verifyUser(login, user);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to verify user", e);
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            // Wait for all futures to complete
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            allOf.join();

            // Collect results
            List<SevenVerifyUser> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // Assert
            assertThat(results).hasSize(numberOfCallsSuccess + numberOfCallsFailed);
            
            int returnedFailed = (int) results.stream().filter(result -> !result.isAuthenticated()).count();
            assertThat(returnedFailed).isEqualTo(numberOfCallsFailed);

            int returnedSuccess = (int) results.stream().filter(SevenVerifyUser::isAuthenticated).count();
            assertThat(returnedSuccess).isEqualTo(numberOfCallsSuccess);

            // Verify all mock requests were consumed
            assertThat(mockWebServer.getRequestCount()).isEqualTo(numberOfCallsSuccess + numberOfCallsFailed);
        } finally {
            executorService.shutdown();
        }
    }

    /** The three responses the legacy path expects: user authorizations, group memberships, global ones. */
    private void enqueueLegacyAuthorizationResponses() {
        for (int i = 0; i < 3; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("[]")
                    .addHeader("Content-Type", "application/json"));
        }
    }

    private List<String> recordedPaths(int count) throws Exception {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            paths.add(mockWebServer.takeRequest().getPath());
        }
        return paths;
    }

    private void testGetUserAuthorizationFallsBackForStatus(int statusCode) throws Exception {
        CIBUser user = getCibUser();

        mockWebServer.enqueue(new MockResponse().setResponseCode(statusCode));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        assertThat(userProvider.getUserAuthorization(user)).isNotNull();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(4);

        RecordedRequest selfRequest = mockWebServer.takeRequest();
        RecordedRequest userRequest = mockWebServer.takeRequest();
        RecordedRequest groupRequest = mockWebServer.takeRequest();
        RecordedRequest globalRequest = mockWebServer.takeRequest();

        assertThat(selfRequest.getPath()).isEqualTo("/engine-rest/authorization/self");
        assertThat(userRequest.getPath()).isEqualTo("/engine-rest/authorization?userIdIn=demo");
        assertThat(groupRequest.getPath()).isEqualTo("/engine-rest/group?member=demo");
        assertThat(globalRequest.getPath()).isEqualTo("/engine-rest/authorization?type=0");
    }

}
