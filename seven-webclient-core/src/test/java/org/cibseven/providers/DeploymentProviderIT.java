package org.cibseven.providers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import org.cibseven.auth.CIBUser;
import org.cibseven.rest.model.Deployment;
import org.cibseven.rest.model.DeploymentResource;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = {DeploymentProvider.class})
public class DeploymentProviderIT {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }
	
    private MockWebServer mockWebServer;

    @Autowired
    private DeploymentProvider deploymentProvider;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Configure the base URL for the DeploymentProvider to point to the MockWebServer
        String mockBaseUrl = mockWebServer.url("/").toString();
        ReflectionTestUtils.setField(deploymentProvider, "camundaUrl", mockBaseUrl);
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
    void testFindDeployments() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/deployment_mock.json");
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        List<Deployment> deployments = (List<Deployment>) deploymentProvider.findDeployments(user);

        // Assert
        assertThat(deployments).isNotNull();
        assertThat(deployments).hasSize(2);

        Deployment firstDeployment = deployments.get(0);
        assertThat(firstDeployment.getId()).isEqualTo("deployment-1");
        assertThat(firstDeployment.getName()).isEqualTo("Deployment One");
        assertThat(firstDeployment.getSource()).isEqualTo("source-1");
        assertThat(firstDeployment.getTenantId()).isEqualTo("tenant-1");
        assertThat(firstDeployment.getDeployedProcessDefinitions()).containsKey("process1");

        Deployment secondDeployment = deployments.get(1);
        assertThat(secondDeployment.getId()).isEqualTo("deployment-2");
        assertThat(secondDeployment.getName()).isEqualTo("Deployment Two");
        assertThat(secondDeployment.getSource()).isEqualTo("source-2");
        assertThat(secondDeployment.getTenantId()).isEqualTo("tenant-2");
    }

    @Test
    void testFindDeploymentResources() throws Exception {
        // Arrange
        String deploymentId = "deployment-1";
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/deployment_resource_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        List<DeploymentResource> resources = (List<DeploymentResource>) deploymentProvider.findDeploymentResources(deploymentId, user);

        // Assert
        assertThat(resources).isNotNull();
        assertThat(resources).hasSize(2);

        DeploymentResource firstResource = resources.get(0);
        assertThat(firstResource.getId()).isEqualTo("resource-1");
        assertThat(firstResource.getName()).isEqualTo("Resource One");
        assertThat(firstResource.getDeploymentId()).isEqualTo("deployment-1");

        DeploymentResource secondResource = resources.get(1);
        assertThat(secondResource.getId()).isEqualTo("resource-2");
        assertThat(secondResource.getName()).isEqualTo("Resource Two");
        assertThat(secondResource.getDeploymentId()).isEqualTo("deployment-1");
    }

    @Test
    void testDeleteDeployment() throws Exception {
        // Arrange
        String deploymentId = "deployment-1";
        boolean cascade = true;
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        // Act
        deploymentProvider.deleteDeployment(deploymentId, cascade, user);

        // Assert
        var request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/engine-rest/deployment/deployment-1?cascade=true");
    }
}
