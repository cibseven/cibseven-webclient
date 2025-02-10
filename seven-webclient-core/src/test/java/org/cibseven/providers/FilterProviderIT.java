package org.cibseven.providers;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import org.cibseven.auth.CIBUser;
import org.cibseven.rest.model.Filter;
import org.cibseven.rest.model.FilterCriterias;
import org.cibseven.rest.model.FilterProperties;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@ContextConfiguration(classes = {FilterProvider.class})
public class FilterProviderIT {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }
	
    private MockWebServer mockWebServer;

    @Autowired
    private FilterProvider filterProvider;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Configure the base URL for the FilterProvider to point to the MockWebServer
        String mockBaseUrl = mockWebServer.url("/").toString();
        ReflectionTestUtils.setField(filterProvider, "camundaUrl", mockBaseUrl);
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
    void testFindFilters() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");


        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/filter_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        var filters = filterProvider.findFilters(user);

        // Assert
        assertThat(filters).isNotNull();
        assertThat(filters).hasSize(2);

        Filter firstFilter = filters.iterator().next();
        assertThat(firstFilter.getId()).isEqualTo("filter-1");
        assertThat(firstFilter.getName()).isEqualTo("Filter One");
        assertThat(firstFilter.getResourceType()).isEqualTo("Task");
        assertThat(firstFilter.getOwner()).isEqualTo("user1");
    }

    @Test
    void testCreateFilter() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        Filter filter = new Filter(
            "filter-1",
            "Task",
            "New Filter",
            "user1",
            new FilterCriterias(),
            new FilterProperties("blue", false, "Test Description", true, 10)
        );

        // Load the mock response from a file
        String mockResponseBody = loadMockResponse("mocks/filter_create_mock.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // Act
        Filter createdFilter = filterProvider.createFilter(filter, user);

        // Assert
        assertThat(createdFilter).isNotNull();
        assertThat(createdFilter.getId()).isEqualTo("filter-1");
        assertThat(createdFilter.getName()).isEqualTo("New Filter");
        assertThat(createdFilter.getOwner()).isEqualTo("user1");
    }

    @Test
    void testUpdateFilter() throws Exception {
        // Arrange
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        Filter filter = new Filter(
            "filter-1",
            "Task",
            "Updated Filter",
            "user1",
            new FilterCriterias(),
            new FilterProperties("red", true, "Updated Description", true, 20)
        );

        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        // Act
        filterProvider.updateFilter(filter, user);

        // Assert
        var request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getPath()).isEqualTo("/engine-rest/filter/filter-1");
    }

    @Test
    void testDeleteFilter() throws Exception {
        // Arrange
        String filterId = "filter-1";
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        // Act
        filterProvider.deleteFilter(filterId, user);

        // Assert
        var request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/engine-rest/filter/filter-1");
    }
}