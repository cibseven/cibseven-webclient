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
}
