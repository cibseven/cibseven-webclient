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

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.User;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Test configuration that provides a mock BaseUserProvider for integration tests.
 * This is needed for tests that require a BaseUserProvider bean but don't need full authentication.
 */
@TestConfiguration
public class MockUserProviderTestConfiguration {

    @Bean
    @Primary
    public BaseUserProvider<StandardLogin> mockBaseUserProvider() {
        return new MockBaseUserProvider();
    }

    /**
     * Mock implementation of BaseUserProvider for testing purposes.
     * This provides minimal functionality needed for provider tests.
     */
    private static class MockBaseUserProvider extends BaseUserProvider<StandardLogin> {

        @Override
        public User login(StandardLogin params, HttpServletRequest rq) {
            // Return a mock user for testing
            CIBUser user = new CIBUser();
            user.setUserID("testuser");
            user.setAuthToken("Bearer test-token");
            return user;
        }

        @Override
        public void logout(User user) {
            // No-op for testing
        }

        @Override
        public User getSelfInfoJSessionId(String userId, String jSessionId, HttpServletRequest rq) {
            // Return a mock user for testing
            CIBUser user = new CIBUser();
            user.setUserID(userId);
            user.setAuthToken("Bearer test-token");
            return user;
        }

        @Override
        public Object authenticateUser(HttpServletRequest request) {
            // Return a mock authentication object
            return "mock-auth";
        }

        @Override
        public StandardLogin createLoginParams() {
            return new StandardLogin();
        }

        @Override
        public String getEngineRestToken(CIBUser user) {
            return "Bearer test-token";
        }

        @Override
        public User getUserInfo(User user, String userId) {
            // Return the same user for testing
            return user;
        }

        @Override
        public String serialize(User user) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(user);
            } catch (JsonProcessingException e) {
                return "{}";
            }
        }

        @Override
        public User verify(Claims userClaims) {
            // Return null for testing (no verification needed)
            return null;
        }

        @Override
        public User deserialize(String json, String token) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                CIBUser user = mapper.readValue(json, CIBUser.class);
                user.setAuthToken(token);
                return user;
            } catch (Exception e) {
                // Return a mock user if deserialization fails
                CIBUser user = new CIBUser();
                user.setUserID("testuser");
                user.setAuthToken(token);
                return user;
            }
        }
    }
}