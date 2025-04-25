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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.UtilsProvider;
import org.cibseven.webapp.rest.model.EventSubscription;
import org.cibseven.webapp.rest.model.Message;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@ContextConfiguration(classes = {UtilsProvider.class})
public class UtilsProviderIT {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }
	
    @Autowired
    private UtilsProvider utilsProvider;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String mockBaseUrl = mockWebServer.url("/").toString();
        ReflectionTestUtils.setField(utilsProvider, "camundaUrl", mockBaseUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    private String loadMockResponse(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(filePath).toURI())));
    }

    @Test
    void testCorrelateMessage() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String mockResponseBody = loadMockResponse("mocks/message_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> requestData = Map.of("messageName", "testMessage");

        // Act
        Collection<Message> messages = utilsProvider.correlateMessage(requestData, user);

        // Assert
        assertThat(messages).isNotNull();
        assertThat(messages).hasSize(2);

        Message message = messages.iterator().next();
        assertThat(message.getResultType()).isEqualTo("Execution");
    }

    @Test
    void testFindStacktrace() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String stacktrace = "Sample stacktrace content";

        mockWebServer.enqueue(new MockResponse()
                .setBody(stacktrace)
                .addHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE));

        // Act
        String result = utilsProvider.findStacktrace("job-1", user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(stacktrace);
    }

    @Test
    void testRetryJobById() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204));

        Map<String, Object> requestData = Map.of("retries", 3);

        // Act & Assert
        utilsProvider.retryJobById("job-1", requestData, user);
    }

    @Test
    void testGetEventSubscriptions() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String mockResponseBody = loadMockResponse("mocks/event_subscriptions_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<EventSubscription> subscriptions = utilsProvider.getEventSubscriptions(
                Optional.of("process-1"), Optional.of("message"), Optional.empty(), user);

        // Assert
        assertThat(subscriptions).isNotNull();
        assertThat(subscriptions).hasSize(2);

        EventSubscription subscription = subscriptions.iterator().next();
        assertThat(subscription.getId()).isEqualTo("event-subscription-1");
        assertThat(subscription.getEventType()).isEqualTo("message");
    }
}
