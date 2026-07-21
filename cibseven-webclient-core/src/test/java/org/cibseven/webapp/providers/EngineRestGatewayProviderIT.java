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

import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.rest.TestRestTemplateConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Verifies the engine-rest gateway forwards only allow-listed calls, derives the engine
 * target from the user token, and denies everything else by default.
 */
@SpringBootTest
@ContextConfiguration(classes = {EngineRestGatewayProvider.class, TestRestTemplateConfiguration.class, MockUserProviderTestConfiguration.class})
public class EngineRestGatewayProviderIT extends BaseHelper {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }

    private MockWebServer mockWebServer;

    @Autowired
    private EngineRestGatewayProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", mockWebServer.url("/").toString());
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void allowListedGet_forwardsToEngineRest() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"sales\"}]")
                .addHeader("Content-Type", "application/json"));

        ResponseEntity<String> response = provider.get("/group", Map.of(), getCibUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("sales");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        // engine target comes from the (default) user token -> base engine-rest path
        assertThat(request.getPath()).isEqualTo("/engine-rest/group");
    }

    @Test
    void allowListedGet_startForm_forwardsToEngineRest() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"key\":\"embedded:deployment:x.html\"}")
                .addHeader("Content-Type", "application/json"));

        provider.get("/process-definition/proc:1:abc/startForm", Map.of(), getCibUser());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/engine-rest/process-definition/proc:1:abc/startForm");
    }

    @Test
    void allowListedGet_taskForm_forwardsToEngineRest() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"key\":\"embedded:deployment:x.html\"}")
                .addHeader("Content-Type", "application/json"));

        provider.get("/task/42/form", Map.of(), getCibUser());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/engine-rest/task/42/form");
    }

    @Test
    void allowListedGet_deployedStartForm_forwardsToEngineRest() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("<form></form>").addHeader("Content-Type", "text/html"));

        provider.get("/process-definition/proc:1:abc/deployed-start-form", Map.of(), getCibUser());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/engine-rest/process-definition/proc:1:abc/deployed-start-form");
    }

    @Test
    void allowListedGet_forwardsQueryParams() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        provider.get("/task/42/form-variables", Map.of("deserializeValues", "false"), getCibUser());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/engine-rest/task/42/form-variables?deserializeValues=false");
    }

    @Test
    void allowListedPost_submitForm_forwardsBody() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        provider.post("/task/42/submit-form", "{\"variables\":{}}", getCibUser());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/engine-rest/task/42/submit-form");
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"variables\":{}}");
    }

    @ParameterizedTest(name = "GET {0} is denied")
    @ValueSource(strings = {
        // curated middleware resources sharing the /services/v1 namespace (the "silent mismatch" risk)
        "/deployment", "/batch", "/filter", "/job", "/job-definition", "/tenant",
        "/authorization", "/incident", "/variable-instance", "/process-instance",
        "/decision-definition", "/execution", "/metrics",
        // allow-list boundary guards — regexes must not over-match
        "/groups", "/user-tasks", "/process-definition/key/x/statistics",
        "/task/42/variables", "/task/42/comment"
    })
    void nonAllowListedGet_deniedWithoutForwarding(String subPath) {
        assertThatThrownBy(() -> provider.get(subPath, Map.of(), getCibUser()))
                .isInstanceOf(AccessDeniedException.class);
        // deny-by-default must not reach the engine
        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    void formProxy_isNotReachableThroughGateway() {
        // task/form-proxy is CIB-only and must stay on the middleware, never the gateway
        assertThatThrownBy(() -> provider.get("/task/form-proxy", Map.of(), getCibUser()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @ParameterizedTest(name = "POST {0} is denied")
    @ValueSource(strings = {
        "/group", "/user",                       // GET-only resources not reachable via POST
        "/task/42/form-variables",               // GET-allowed, but not as a write
        "/task/42/complete", "/process-definition/key/x/start", "/deployment/create"
    })
    void nonAllowListedPost_deniedWithoutForwarding(String subPath) {
        // submit-form is the only allowed write; everything else is denied
        assertThatThrownBy(() -> provider.post(subPath, "{}", getCibUser()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    void get_engineTargetDerivedFromUserToken() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        // A non-default engine encoded in the token must select that engine, ignoring any client input
        CIBUser user = getCibUser();
        user.setEngine(mockWebServer.url("/").toString() + "|/engine-rest|myengine");

        provider.get("/user", Map.of(), user);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/engine-rest/engine/myengine/user");
    }
}
