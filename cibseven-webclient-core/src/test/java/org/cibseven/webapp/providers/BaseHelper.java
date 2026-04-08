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

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.cibseven.webapp.auth.CIBUser;

/**
 * BaseHelper provides shared utility methods for integration tests,
 * such as loading mock JSON responses and creating test users.
 * <p>
 * This class is intended to support provider-level tests by offering reusable
 * setup helpers. Methods in this class are typically used to reduce boilerplate
 * in test classes involving MockWebServer or user context setup.
 * </p>
 *
 */
public class BaseHelper {

    /**
     * Loads the contents of a mock response file from the classpath.
     * <p>
     * This utility is used in integration tests to simulate HTTP responses from external services.
     * It reads a file (usually a JSON file) and returns its content as a string.
     * </p>
     *
     * @param filePath the relative path to the mock file in the classpath (e.g., "mocks/filter_mock.json")
     * @return the contents of the mock response file as a {@link String}
     * @throws FileNotFoundException if the file cannot be found in the classpath
     * @throws Exception if an I/O error occurs while reading the file or converting its URI
     */
    protected String loadMockResponse(String filePath) throws Exception {
        URL resource = getClass().getClassLoader().getResource(filePath);

        // Throw an exception if the resource was not found
        if (resource == null) {
            throw new FileNotFoundException("Mock response file not found: " + filePath);
        }

        return Files.readString(Paths.get(resource.toURI()));
    }

    /**
     * Utility method to create a mock {@link CIBUser} with predefined values to simulate an authenticated user.
     *
     * @return a {@link CIBUser} object with a mock user ID and an authorization token
     */
    protected CIBUser getCibUser() {
        CIBUser user = new CIBUser();
        user.setUserID("demo");
        user.setAuthToken("Bearer token");
        return user;
    }

}
