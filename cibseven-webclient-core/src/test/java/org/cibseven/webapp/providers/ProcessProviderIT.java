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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.StartForm;
import org.cibseven.webapp.rest.TestRestTemplateConfiguration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@ContextConfiguration(classes = {ProcessProvider.class, TestRestTemplateConfiguration.class})
public class ProcessProviderIT extends BaseHelper {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }

    private MockWebServer mockWebServer;

    @Autowired
    private ProcessProvider processProvider;

    @MockitoBean
    private IIncidentProvider incidentProvider;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Configure Mock IIncidentProvider
        incidentProvider = mock(IIncidentProvider.class);
        when(incidentProvider.findIncidentByInstanceId("testInstance", null))
                .thenReturn(Collections.emptyList());

        // Inject mock in ProcessProvider
        ReflectionTestUtils.setField(processProvider, "incidentProvider", incidentProvider);


        // Configure the base URL for the ProcessProvider to point to the MockWebServer
        String mockBaseUrl = mockWebServer.url("/").toString();
        ReflectionTestUtils.setField(processProvider, "cibsevenUrl", mockBaseUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Shutdown the MockWebServer after each test
        mockWebServer.shutdown();
    }

    @Test
    void testFindProcesses() throws Exception {
        // Arrange
        CIBUser user = getCibUser();

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/processes_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<Process> processes = processProvider.findProcesses(user);

        // Assert
        assertThat(processes).isNotNull();
        assertThat(processes).hasSize(2);

        Process firstProcess = processes.iterator().next();
        assertThat(firstProcess.getId()).isEqualTo("process-1");
        assertThat(firstProcess.getKey()).isEqualTo("processKey1");
        assertThat(firstProcess.getName()).isEqualTo("Process One");
    }

    @Test
    void testFindProcessByDefinitionKey() throws Exception {
        // Arrange
        String processKey = "processKey1";
        CIBUser user = getCibUser();

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/process_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Process process = processProvider.findProcessByDefinitionKey(processKey, null, user);

        // Assert
        assertThat(process).isNotNull();
        assertThat(process.getId()).isEqualTo("process-1");
        assertThat(process.getKey()).isEqualTo("processKey1");
        assertThat(process.getName()).isEqualTo("Process One");
    }

    @Test
    void testFetchDiagram() throws Exception {
        // Arrange
        String processDefinitionId = "process-1";
        CIBUser user = getCibUser();

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/process_diagram_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        ProcessDiagram diagram = processProvider.fetchDiagram(processDefinitionId, user);

        // Assert
        assertThat(diagram).isNotNull();
        assertThat(diagram.getId()).isEqualTo("process-1");
        assertThat(diagram.getBpmn20Xml()).isEqualTo("<bpmn>Sample BPMN Diagram</bpmn>");
    }

    @Test
    void testFetchStartForm() throws Exception {
        // Arrange
        String processDefinitionId = "process-1";
        CIBUser user = getCibUser();

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/process_start_form_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        StartForm startForm = processProvider.fetchStartForm(processDefinitionId, user);

        // Assert
        assertThat(startForm).isNotNull();
        assertThat(startForm.getKey()).isEqualTo("startFormKey");
        assertThat(startForm.getContextPath()).isEqualTo("/startFormPath");
        assertThat(startForm.getCamundaFormRef().getKey()).isEqualTo("formKey");
    }

    @Test
    void testStartProcess() throws Exception {
        // Arrange
        String processDefinitionKey = "processKey1";
        CIBUser user = getCibUser();

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/process_instance_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        ProcessStart processStart = processProvider.startProcess(processDefinitionKey, null, null, user);

        // Assert
        assertThat(processStart).isNotNull();
        assertThat(processStart.getId()).isEqualTo("instance-1");
        assertThat(processStart.getDefinitionId()).isEqualTo("process-1");
        assertThat(processStart.getBusinessKey()).isEqualTo("businessKey1");
    }
}
