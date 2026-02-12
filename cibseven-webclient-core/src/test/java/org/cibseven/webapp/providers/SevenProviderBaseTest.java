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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.cibseven.webapp.auth.CIBUser;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test class for SevenProviderBase to verify configurable engine REST path functionality
 */
public class SevenProviderBaseTest {

    /**
     * Test provider implementation for testing purposes
     */
    private static class TestProvider extends SevenProviderBase {
        // Empty implementation for testing
    }

    @Test
    public void testGetEngineRestUrl_DefaultPath() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        String result = provider.getEngineRestUrl();
        assertEquals("http://localhost:8080/engine-rest", result);
    }

    @Test
    public void testGetEngineRestUrl_CustomPath() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/different-path");

        String result = provider.getEngineRestUrl();
        assertEquals("http://localhost:8080/different-path", result);
    }

    @Test
    public void testGetEngineRestUrl_BaseUrlWithTrailingSlash() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080/");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        String result = provider.getEngineRestUrl();
        assertEquals("http://localhost:8080/engine-rest", result);
    }

    @Test
    public void testGetEngineRestUrl_PathWithoutLeadingSlash() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "engine-rest");

        String result = provider.getEngineRestUrl();
        assertEquals("http://localhost:8080/engine-rest", result);
    }

    @Test
    public void testGetEngineRestUrl_BothWithSlashes() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080/");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        String result = provider.getEngineRestUrl();
        assertEquals("http://localhost:8080/engine-rest", result);
    }

    @Test
    public void testGetEngineRestUrl_RelativeBaseUrl() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "./");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        String result = provider.getEngineRestUrl();
        assertEquals("./engine-rest", result);
    }

    // Tests for getEngineRestUrl(CIBUser user)

    @Test
    public void testGetEngineRestUrl_WithUser_PipeFormat() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        CIBUser user = mock(CIBUser.class);
        when(user.getEngine()).thenReturn("http://remote:9090|/custom-path|myengine");

        String result = provider.getEngineRestUrl(user);
        assertEquals("http://remote:9090/custom-path/engine/myengine", result);
    }

    @Test
    public void testGetEngineRestUrl_WithUser_PipeFormatWithTrailingSlash() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        CIBUser user = mock(CIBUser.class);
        when(user.getEngine()).thenReturn("http://remote:9090/|custom-path|myengine");

        String result = provider.getEngineRestUrl(user);
        assertEquals("http://remote:9090/custom-path/engine/myengine", result);
    }

    @Test
    public void testGetEngineRestUrl_WithUser_LegacyFormatNonDefault() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        CIBUser user = mock(CIBUser.class);
        when(user.getEngine()).thenReturn("myengine");

        String result = provider.getEngineRestUrl(user);
        assertEquals("http://localhost:8080/engine-rest/engine/myengine", result);
    }

    @Test
    public void testGetEngineRestUrl_WithUser_LegacyFormatDefault() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        CIBUser user = mock(CIBUser.class);
        when(user.getEngine()).thenReturn("default");

        String result = provider.getEngineRestUrl(user);
        assertEquals("http://localhost:8080/engine-rest", result);
    }

    @Test
    public void testGetEngineRestUrl_WithUser_NullEngine() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        CIBUser user = mock(CIBUser.class);
        when(user.getEngine()).thenReturn(null);

        String result = provider.getEngineRestUrl(user);
        assertEquals("http://localhost:8080/engine-rest", result);
    }

    @Test
    public void testGetEngineRestUrl_WithUser_EmptyEngine() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        CIBUser user = mock(CIBUser.class);
        when(user.getEngine()).thenReturn("");

        String result = provider.getEngineRestUrl(user);
        assertEquals("http://localhost:8080/engine-rest", result);
    }

    @Test
    public void testGetEngineRestUrl_NullUser() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        String result = provider.getEngineRestUrl(null);
        assertEquals("http://localhost:8080/engine-rest", result);
    }

    @Test
    public void testGetEngineRestUrl_WithUser_IncompletePipeFormat() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        CIBUser user = mock(CIBUser.class);
        // Pipe format but incomplete (only 2 parts instead of 3)
        when(user.getEngine()).thenReturn("http://remote:9090|/custom-path");

        String result = provider.getEngineRestUrl(user);
        // Should fall back to default behavior
        assertEquals("http://localhost:8080/engine-rest", result);
    }

    @Test
    public void testGetEngineRestUrl_WithUser_PipeFormatWithDefaultEngine() {
        TestProvider provider = new TestProvider();
        ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");

        CIBUser user = mock(CIBUser.class);
        // Pipe format with "default" as engine name
        when(user.getEngine()).thenReturn("http://remote:9090|/custom-path|default");

        String result = provider.getEngineRestUrl(user);
        // Should use the pipe format regardless of "default" in the engine name
        assertEquals("http://remote:9090/custom-path/engine/default", result);
    }
}
