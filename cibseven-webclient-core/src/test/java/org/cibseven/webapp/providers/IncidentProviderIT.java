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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.IncidentProvider;
import org.cibseven.webapp.rest.model.Incident;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@ContextConfiguration(classes = {IncidentProvider.class})
public class IncidentProviderIT {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }
	
    @Autowired
    private IncidentProvider incidentProvider;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String mockBaseUrl = mockWebServer.url("/").toString();
        ReflectionTestUtils.setField(incidentProvider, "camundaUrl", mockBaseUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    private String loadMockResponse(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(filePath).toURI())));
    }

    @Test
    void testCountIncident() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String mockResponseBody = "{\"count\": 5}";

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Long count = incidentProvider.countIncident(
                Optional.of("incident-1"), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), user);

        // Assert
        assertThat(count).isNotNull();
        assertThat(count).isEqualTo(5);
    }

    @Test
    void testFindIncident() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String mockResponseBody = loadMockResponse("mocks/incidents_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<Incident> incidents = incidentProvider.findIncident(
                Optional.of("incident-1"), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), user);

        // Assert
        assertThat(incidents).isNotNull();
        assertThat(incidents).hasSize(2);

        Incident firstIncident = incidents.iterator().next();
        assertThat(firstIncident.getId()).isEqualTo("incident-1");
        assertThat(firstIncident.getIncidentType()).isEqualTo("failedJob");
    }

    @Test
    void testFindIncidentByInstanceId() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String mockResponseBody = loadMockResponse("mocks/incidents_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        List<Incident> incidents = incidentProvider.findIncidentByInstanceId("process-instance-1", user);

        // Assert
        assertThat(incidents).isNotNull();
        assertThat(incidents).hasSize(2);

        Incident firstIncident = incidents.get(0);
        assertThat(firstIncident.getProcessInstanceId()).isEqualTo("process-instance-1");
    }

    @Test
    void testFetchIncidents() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String mockResponseBody = loadMockResponse("mocks/incidents_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<Incident> incidents = incidentProvider.fetchIncidents("process-key-1", user);

        // Assert
        assertThat(incidents).isNotNull();
        assertThat(incidents).hasSize(2);

        Incident firstIncident = incidents.iterator().next();
        assertThat(firstIncident.getProcessDefinitionId()).isEqualTo("process-def-1");
    }
}
