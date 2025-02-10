package org.cibseven.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import org.cibseven.auth.CIBUser;
import org.cibseven.rest.model.Task;
import org.cibseven.rest.model.TaskCount;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@ContextConfiguration(classes = {TaskProvider.class})
public class TaskProviderIT {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }
	
    private MockWebServer mockWebServer;

    @Autowired
    private TaskProvider taskProvider;
    
    @MockBean
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
        ReflectionTestUtils.setField(taskProvider, "camundaUrl", mockBaseUrl);
    }

    private String eq(Object object) {
		// TODO Auto-generated method stub
		return null;
	}

	@AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // Utility method to load mock responses from JSON files
    private String loadMockResponse(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(filePath).toURI())));
    }

    @Test
    void testFindTasks() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

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
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/task_count_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        TaskCount taskCount = taskProvider.findTasksCount(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), user);

        // Assert
        assertThat(taskCount).isNotNull();
        assertThat(taskCount.getCount()).isEqualTo(42);
    }

    @Test
    void testFindTasksByProcessInstance() throws Exception {
        // Arrange
        String processInstanceId = "process-1";
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

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
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

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
