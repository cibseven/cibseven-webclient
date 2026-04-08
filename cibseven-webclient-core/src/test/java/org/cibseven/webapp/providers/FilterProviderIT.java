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

import org.cibseven.webapp.rest.TestRestTemplateConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Filter;
import org.cibseven.webapp.rest.model.FilterCriterias;
import org.cibseven.webapp.rest.model.FilterProperties;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@ContextConfiguration(classes = {FilterProvider.class, TestRestTemplateConfiguration.class, MockUserProviderTestConfiguration.class})
public class FilterProviderIT extends BaseHelper {

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
        ReflectionTestUtils.setField(filterProvider, "cibsevenUrl", mockBaseUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Shutdown the MockWebServer after each test
        mockWebServer.shutdown();
    }

    @Test
    void testFindFilters() throws Exception {
        // Arrange
        CIBUser user = getCibUser();

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
        CIBUser user = getCibUser();

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
        CIBUser user = getCibUser();

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
        CIBUser user = getCibUser();

        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        // Act
        filterProvider.deleteFilter(filterId, user);

        // Assert
        var request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/engine-rest/filter/filter-1");
    }
}
