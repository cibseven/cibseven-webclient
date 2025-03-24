package org.cibseven.providers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.cibseven.auth.CIBUser;
import org.cibseven.rest.model.ActivityInstance;
import org.cibseven.rest.model.ActivityInstanceHistory;
import org.cibseven.rest.model.TransitionInstance;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = {ActivityProvider.class})
public class ActivityProviderIT {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }
	
    private MockWebServer mockWebServer;

    @Autowired
    private ActivityProvider activityProvider;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Configure the base URL for the ActivityProvider to point to the MockWebServer
        String mockBaseUrl = mockWebServer.url("/").toString();
        ReflectionTestUtils.setField(activityProvider, "camundaUrl", mockBaseUrl);
    }
    
    @AfterEach
    void tearDown() throws Exception {
    	// Shutdown the MockWebServer after each test
        mockWebServer.shutdown();
    }    

    // Utility method to load mock responses from JSON files
    private String loadMockResponse(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(filePath).toURI())));
    }
    
    @Test
    void testFindActivityInstance() throws Exception {
    	// Arrange: Prepare inputs and configure the MockWebServer
        String processInstanceId = "12345";
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/activity_instance_mock.json");
        
        // Enqueue the mock response for the MockWebServer
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act: Call the method under test
        ActivityInstance result = activityProvider.findActivityInstance(processInstanceId, user);

        // Assert: Validate the returned ActivityInstance object
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("activity-instance-1");
        assertThat(result.getName()).isEqualTo("Sample Activity");
        assertThat(result.getActivityId()).isEqualTo("activity-1");
        assertThat(result.getActivityName()).isEqualTo("User Task");
        assertThat(result.getActivityType()).isEqualTo("userTask");
        assertThat(result.getExecutionIds()).containsExactly("execution-1");
        assertThat(result.getParentActivityInstanceId()).isEqualTo("parent-activity-1");
        assertThat(result.getProcessDefinitionId()).isEqualTo("process-def-1");
        assertThat(result.getProcessInstanceId()).isEqualTo("process-instance-1");

        // Verify childActivityInstances
        assertThat(result.getChildActivityInstances()).isNotNull();
        assertThat(result.getChildActivityInstances()).hasSize(1);
        assertThat(result.getChildActivityInstances().get(0).getId()).isEqualTo("child-activity-1");
        assertThat(result.getChildActivityInstances().get(0).getActivityName()).isEqualTo("Child Activity");
        assertThat(result.getChildActivityInstances().get(0).getActivityType()).isEqualTo("serviceTask");

        // Verify childTransitionInstances
        assertThat(result.getChildTransitionInstances()).isNotNull();
        assertThat(result.getChildTransitionInstances()).hasSize(1);

        // Validate the attributes of the first TransitionInstance
        TransitionInstance transition = result.getChildTransitionInstances().get(0);
        assertThat(transition.getId()).isEqualTo("transition-1");
        assertThat(transition.getParentActivityInstanceId()).isEqualTo("parent-activity-1");
        assertThat(transition.getProcessInstanceId()).isEqualTo("process-instance-1");
        assertThat(transition.getProcessDefinitionId()).isEqualTo("process-def-1");
        assertThat(transition.getActivityId()).isEqualTo("activity-2");
        assertThat(transition.getActivityName()).isEqualTo("Transition Activity");
        assertThat(transition.getActivityType()).isEqualTo("transitionType");
        assertThat(transition.getExecutionId()).isEqualTo("execution-2");
        assertThat(transition.getTargetActivityId()).isEqualTo("target-activity-1");
        assertThat(transition.getIncidentIds()).containsExactly("incident-1", "incident-2");

        // Verify the request sent to the MockWebServer
        var request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/engine-rest/process-instance/12345/activity-instances");
    }
    
    @Test
    void testFindActivitiesInstancesHistory() throws Exception {
        // Arrange
        String processInstanceId = "12345";
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/activity_instances_history_mock.json");

        // Enqueue the mock response for the MockWebServer
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        List<ActivityInstanceHistory> result = activityProvider.findActivitiesInstancesHistory(processInstanceId, user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        ActivityInstanceHistory firstInstance = result.get(0);
        assertThat(firstInstance.getActivityId()).isEqualTo("activity-1");
        assertThat(firstInstance.getActivityName()).isEqualTo("User Task");
        assertThat(firstInstance.getActivityType()).isEqualTo("userTask");
        assertThat(firstInstance.getId()).isEqualTo("instance-1");

        ActivityInstanceHistory secondInstance = result.get(1);
        assertThat(secondInstance.getActivityId()).isEqualTo("activity-2");
        assertThat(secondInstance.getActivityName()).isEqualTo("Service Task");
        assertThat(secondInstance.getActivityType()).isEqualTo("serviceTask");
        assertThat(secondInstance.getId()).isEqualTo("instance-2");

        // Verify the request sent to the MockWebServer
        var request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/engine-rest/history/activity-instance?processInstanceId=12345");
    }

    @Test
    void testDeleteVariableByExecutionId() throws Exception {
        // Arrange
        String executionId = "execution-123";
        String variableName = "variable-1";
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        // Enqueue a mock response for the MockWebServer
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        // Act
        activityProvider.deleteVariableByExecutionId(executionId, variableName, user);

        // Verify the request sent to the MockWebServer
        var request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/engine-rest/execution/execution-123/localVariables/variable-1");
    }

    @Test
    void testDeleteVariableHistoryInstance() throws Exception {
        // Arrange
        String variableId = "variable-history-123";
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        // Enqueue a mock response for the MockWebServer
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        // Act
        activityProvider.deleteVariableHistoryInstance(variableId, user);

        // Verify the request sent to the MockWebServer
        var request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/engine-rest/history/variable-instance/variable-history-123");
    }    

}
