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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenUserProvider;
import org.cibseven.webapp.rest.model.Task;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.cibseven.webapp.rest.TestRestTemplateConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {TaskProvider.class, SevenUserProvider.class, TestRestTemplateConfiguration.class})
public class TaskProviderIT extends BaseHelper {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }

    private MockWebServer mockWebServer;

    @Autowired
    private TaskProvider taskProvider;

    @MockitoBean
    private IVariableProvider variableProvider;


    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Configure Mock IIncidentProvider
        variableProvider = mock(IVariableProvider.class);

        // Configure Mock IVariableProvider
        variableProvider = mock(IVariableProvider.class);

        doNothing().when(variableProvider).submitVariables("instance-1", null, null, "process-1");

        // Inject mock in TaskProvider
        ReflectionTestUtils.setField(taskProvider, "variableProvider", variableProvider);

        // Configure the base URL for the TaskProvider to point to the MockWebServer
        String mockBaseUrl = mockWebServer.url("/").toString();
        ReflectionTestUtils.setField(taskProvider, "cibsevenUrl", mockBaseUrl);
    }

	@AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testFindTasks() throws Exception {
        // Arrange
        CIBUser user = getCibUser();

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/tasks_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<Task> tasks = taskProvider.findTasks(null, user);

        // Assert
        assertThat(tasks).isNotNull();
        assertThat(tasks).hasSize(2);

        Task firstTask = tasks.iterator().next();
        assertThat(firstTask.getId()).isEqualTo("task-1");
        assertThat(firstTask.getName()).isEqualTo("Task One");
    }

    @Test
    void testFindTasksCount() throws Exception {
        // Arrange
        CIBUser user = getCibUser();

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/task_count_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> params = Map.of();
        // Act
        Integer taskCount = taskProvider.findTasksCount(params, user);

        // Assert
        assertThat(taskCount).isNotNull();
        assertThat(taskCount).isEqualTo(42);
    }

    @Test
    void testFindTasksByProcessInstance() throws Exception {
        // Arrange
        String processInstanceId = "process-1";
        CIBUser user = getCibUser();

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/tasks_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Collection<Task> tasks = taskProvider.findTasksByProcessInstance(processInstanceId, user);

        // Assert
        assertThat(tasks).isNotNull();
        assertThat(tasks).hasSize(2);
    }

    @Test
    void testFindTaskById() throws Exception {
        // Arrange
        String taskId = "task-1";
        CIBUser user = getCibUser();

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/task_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Task task = taskProvider.findTaskById(taskId, user);

        // Assert
        assertThat(task).isNotNull();
        assertThat(task.getId()).isEqualTo("task-1");
        assertThat(task.getName()).isEqualTo("Task One");
    }

}
