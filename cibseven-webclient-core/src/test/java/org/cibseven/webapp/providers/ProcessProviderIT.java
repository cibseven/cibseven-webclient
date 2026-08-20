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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.HistoryStatistics;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.ProcessInstance;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.StartForm;
import org.cibseven.webapp.rest.TestRestTemplateConfiguration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest
@ContextConfiguration(classes = {ProcessProvider.class, TestRestTemplateConfiguration.class, MockUserProviderTestConfiguration.class})
public class ProcessProviderIT extends BaseHelper {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }

    private MockWebServer mockWebServer;

    @Autowired
    private ProcessProvider processProvider;

    @MockitoBean
    private IIncidentProvider incidentProvider;

    @MockitoBean
    private IEngineProvider engineProvider;

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

        // Configure Mock IEngineProvider with a "full" history level by default
        engineProvider = mock(IEngineProvider.class);
        EngineConfiguration engineConfiguration = new EngineConfiguration();
        engineConfiguration.setHistoryLevel("full");
        when(engineProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(engineConfiguration);
        when(engineProvider.getEngineConfiguration(anyString())).thenReturn(engineConfiguration);

        // Inject mock in ProcessProvider
        ReflectionTestUtils.setField(processProvider, "engineProvider", engineProvider);


        // Configure the base URL for the ProcessProvider to point to the MockWebServer
        String mockBaseUrl = mockWebServer.url("/").toString();
        ReflectionTestUtils.setField(processProvider, "cibsevenUrl", mockBaseUrl);

        // The provider is a context-scoped singleton: without this, what one test remembered about the
        // historic activity statistics query would decide which path the next one takes.
        historicActivityStatisticsQueryUnsupported().clear();
    }

    @SuppressWarnings("unchecked")
    private Set<String> historicActivityStatisticsQueryUnsupported() {
        return (Set<String>) ReflectionTestUtils.getField(processProvider, "historicActivityStatisticsQueryUnsupported");
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

    @Test
    void testFindProcessesInstancesFlagsWithIncident() throws Exception {
        // Arrange
        String processKey = "processKey1";
        CIBUser user = getCibUser();

        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"instance-1\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey1\",\"ended\":false,\"suspended\":false},"
                        + "{\"id\":\"instance-2\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey2\",\"ended\":false,\"suspended\":false}]")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"instance-1\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey1\",\"ended\":false,\"suspended\":false}]")
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<ProcessInstance> instances = processProvider.findProcessesInstances(processKey, user);

        // Assert
        assertThat(instances).hasSize(2);
        Map<String, ProcessInstance> byId = instances.stream()
                .collect(java.util.stream.Collectors.toMap(ProcessInstance::getId, i -> i));
        assertThat(byId.get("instance-1").getWithIncident()).isTrue();
        assertThat(byId.get("instance-2").getWithIncident()).isFalse();

        RecordedRequest firstRequest = mockWebServer.takeRequest();
        assertThat(firstRequest.getMethod()).isEqualTo("GET");
        assertThat(firstRequest.getPath()).isEqualTo("/engine-rest/process-instance?processDefinitionKey=processKey1");

        RecordedRequest secondRequest = mockWebServer.takeRequest();
        assertThat(secondRequest.getMethod()).isEqualTo("POST");
        assertThat(secondRequest.getPath()).isEqualTo("/engine-rest/process-instance");
    }

    @Test
    void testFindCurrentProcessesInstancesFlagsWithIncident() throws Exception {
        // Arrange
        CIBUser user = getCibUser();
        Map<String, Object> data = Map.of("processDefinitionKey", "processKey1");

        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"instance-1\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey1\",\"ended\":false,\"suspended\":false},"
                        + "{\"id\":\"instance-2\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey2\",\"ended\":false,\"suspended\":false}]")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"instance-1\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey1\",\"ended\":false,\"suspended\":false}]")
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<ProcessInstance> instances = processProvider.findCurrentProcessesInstances(data, Optional.empty(), Optional.empty(), user);

        // Assert
        assertThat(instances).hasSize(2);
        Map<String, ProcessInstance> byId = instances.stream()
                .collect(java.util.stream.Collectors.toMap(ProcessInstance::getId, i -> i));
        assertThat(byId.get("instance-1").getWithIncident()).isTrue();
        assertThat(byId.get("instance-2").getWithIncident()).isFalse();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        RecordedRequest firstRequest = mockWebServer.takeRequest();
        assertThat(firstRequest.getMethod()).isEqualTo("POST");
        assertThat(firstRequest.getPath()).isEqualTo("/engine-rest/process-instance");
    }

    @Test
    void testFindCurrentProcessesInstancesSkipsFollowUpCallWhenAlreadyFilteredByIncident() {
        // Arrange
        CIBUser user = getCibUser();
        Map<String, Object> data = Map.of("withIncident", Boolean.TRUE);

        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"instance-1\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey1\",\"ended\":false,\"suspended\":false},"
                        + "{\"id\":\"instance-2\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey2\",\"ended\":false,\"suspended\":false}]")
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<ProcessInstance> instances = processProvider.findCurrentProcessesInstances(data, Optional.empty(), Optional.empty(), user);

        // Assert
        assertThat(instances).hasSize(2);
        assertThat(instances).allSatisfy(i -> assertThat(i.getWithIncident()).isTrue());

        // No follow-up call was made since the caller already requested withIncident=true
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void testFindProcessInstanceFlagsWithIncident() throws Exception {
        // Arrange
        String processInstanceId = "instance-1";
        CIBUser user = getCibUser();

        mockWebServer.enqueue(new MockResponse()
                .setBody(loadMockResponse("mocks/process_instance_mock.json"))
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"instance-1\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey1\",\"ended\":false,\"suspended\":false}]")
                .addHeader("Content-Type", "application/json"));

        // Act
        ProcessInstance instance = processProvider.findProcessInstance(processInstanceId, user);

        // Assert
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo("instance-1");
        assertThat(instance.getWithIncident()).isTrue();

        RecordedRequest firstRequest = mockWebServer.takeRequest();
        assertThat(firstRequest.getMethod()).isEqualTo("GET");
        assertThat(firstRequest.getPath()).isEqualTo("/engine-rest/process-instance/instance-1");

        RecordedRequest secondRequest = mockWebServer.takeRequest();
        assertThat(secondRequest.getMethod()).isEqualTo("POST");
        assertThat(secondRequest.getPath()).isEqualTo("/engine-rest/process-instance");
    }

    @Test
    void testFindProcessInstanceWithoutIncidentIsFlaggedFalse() throws Exception {
        // Arrange
        String processInstanceId = "instance-1";
        CIBUser user = getCibUser();

        mockWebServer.enqueue(new MockResponse()
                .setBody(loadMockResponse("mocks/process_instance_mock.json"))
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        // Act
        ProcessInstance instance = processProvider.findProcessInstance(processInstanceId, user);

        // Assert
        assertThat(instance).isNotNull();
        assertThat(instance.getWithIncident()).isFalse();
    }

    @Test
    void testFindProcessesInstancesRuntimeWithFullHistoryLevel() throws Exception {
        // Arrange
        CIBUser user = getCibUser();
        Map<String, Object> data = Map.of("processDefinitionKey", "processKey1");

        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"instance-1\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey1\",\"ended\":false,\"suspended\":false}]")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"instance-1\",\"processDefinitionId\":\"process-1\",\"businessKey\":\"businessKey1\"}]")
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<HistoryProcessInstance> result = processProvider.findProcessesInstancesRuntime(data, Optional.empty(), Optional.empty(), user);

        // Assert
        assertThat(result).hasSize(1);
        HistoryProcessInstance historyInstance = result.iterator().next();
        assertThat(historyInstance.getId()).isEqualTo("instance-1");

        RecordedRequest firstRequest = mockWebServer.takeRequest();
        assertThat(firstRequest.getMethod()).isEqualTo("POST");
        assertThat(firstRequest.getPath()).isEqualTo("/engine-rest/process-instance");

        RecordedRequest secondRequest = mockWebServer.takeRequest();
        assertThat(secondRequest.getMethod()).isEqualTo("POST");
        assertThat(secondRequest.getPath()).contains("/engine-rest/history/process-instance");
    }

    @Test
    void testFindProcessesInstancesRuntimeWithHistoryLevelNoneEnrichesFromDefinitions() throws Exception {
        // Arrange
        CIBUser user = getCibUser();
        Map<String, Object> data = Map.of("processDefinitionKey", "processKey1");

        EngineConfiguration noneHistoryLevelConfig = new EngineConfiguration();
        noneHistoryLevelConfig.setHistoryLevel("none");
        when(engineProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(noneHistoryLevelConfig);

        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"instance-1\",\"definitionId\":\"process-1\",\"businessKey\":\"businessKey1\","
                        + "\"caseInstanceId\":\"case-1\",\"tenantId\":\"tenant-1\",\"ended\":false,\"suspended\":false}]")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("[{\"id\":\"process-1\",\"key\":\"processKey1\",\"name\":\"Process One\",\"version\":\"3\"}]")
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<HistoryProcessInstance> result = processProvider.findProcessesInstancesRuntime(data, Optional.empty(), Optional.empty(), user);

        // Assert
        assertThat(result).hasSize(1);
        HistoryProcessInstance historyInstance = result.iterator().next();
        assertThat(historyInstance.getId()).isEqualTo("instance-1");
        assertThat(historyInstance.getProcessDefinitionId()).isEqualTo("process-1");
        assertThat(historyInstance.getProcessDefinitionKey()).isEqualTo("processKey1");
        assertThat(historyInstance.getProcessDefinitionName()).isEqualTo("Process One");
        assertThat(historyInstance.getProcessDefinitionVersion()).isEqualTo("3");

        RecordedRequest firstRequest = mockWebServer.takeRequest();
        assertThat(firstRequest.getMethod()).isEqualTo("POST");
        assertThat(firstRequest.getPath()).isEqualTo("/engine-rest/process-instance");

        RecordedRequest secondRequest = mockWebServer.takeRequest();
        assertThat(secondRequest.getMethod()).isEqualTo("GET");
        assertThat(secondRequest.getPath()).contains("/engine-rest/process-definition?processDefinitionIdIn=process-1");
    }

    @Test
    void testFindHistoricActivityStatistics() throws Exception {
        // Arrange
        String processDefinitionId = "process-1";
        CIBUser user = getCibUser();
        Map<String, Object> filters = Map.of("canceled", true);

        String mockResponseBody = loadMockResponse("mocks/history_statistics_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<HistoryStatistics> statistics = processProvider.findHistoricActivityStatistics(processDefinitionId, filters, user);

        // Assert
        assertThat(statistics).isNotNull();
        assertThat(statistics).hasSize(2);

        HistoryStatistics firstStatistic = statistics.iterator().next();
        assertThat(firstStatistic.getId()).isEqualTo("activity-1");
        assertThat(firstStatistic.getInstances()).isEqualTo(4);
        assertThat(firstStatistic.getCanceled()).isEqualTo(1);
        assertThat(firstStatistic.getFinished()).isEqualTo(3);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/engine-rest/history/process-definition/process-1/statistics");
    }

    @Test
    void testFindHistoricActivityStatisticsFallsBackToGetOn405() throws Exception {
        String processDefinitionId = "process-1";
        CIBUser user = getCibUser();
        Map<String, Object> filters = Map.of("canceled", true);

        String mockResponseBody = loadMockResponse("mocks/history_statistics_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(405)
                .setBody("Method Not Allowed")
                .addHeader("Content-Type", "text/plain"));
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        Collection<HistoryStatistics> statistics = processProvider.findHistoricActivityStatistics(processDefinitionId, filters, user);

        assertThat(statistics).isNotNull();
        assertThat(statistics).hasSize(2);

        RecordedRequest postRequest = mockWebServer.takeRequest();
        assertThat(postRequest.getMethod()).isEqualTo("POST");

        RecordedRequest getRequest = mockWebServer.takeRequest();
        assertThat(getRequest.getMethod()).isEqualTo("GET");
        assertThat(getRequest.getPath()).contains("/engine-rest/history/process-definition/process-1/statistics?");
    }

    /**
     * Whether the engine offers the query cannot change while it is running, so the rejected POST is
     * made once and not on every call.
     */
    @Test
    void testFindHistoricActivityStatisticsRemembersThatThePostQueryIsUnsupported() throws Exception {
        String processDefinitionId = "process-1";
        CIBUser user = getCibUser();
        Map<String, Object> filters = Map.of("canceled", true);

        String mockResponseBody = loadMockResponse("mocks/history_statistics_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(405)
                .setBody("Method Not Allowed")
                .addHeader("Content-Type", "text/plain"));
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        assertThat(processProvider.findHistoricActivityStatistics(processDefinitionId, filters, user)).hasSize(2);
        assertThat(processProvider.findHistoricActivityStatistics(processDefinitionId, filters, user)).hasSize(2);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
        assertThat(mockWebServer.takeRequest().getMethod()).isEqualTo("POST");
        assertThat(mockWebServer.takeRequest().getMethod()).isEqualTo("GET");
        // The second call goes straight to the legacy variant instead of being rejected again.
        assertThat(mockWebServer.takeRequest().getMethod()).isEqualTo("GET");
    }
}
