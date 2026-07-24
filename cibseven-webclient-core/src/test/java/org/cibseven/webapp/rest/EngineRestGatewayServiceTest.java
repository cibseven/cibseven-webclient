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
package org.cibseven.webapp.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.EngineRestGatewayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Unit tests for the thin controller — focuses on the sub-path extraction that strips the
 * {@code .../engine-rest} prefix and hands the remainder to the provider.
 */
public class EngineRestGatewayServiceTest {

    private EngineRestGatewayProvider provider;
    private EngineRestGatewayService service;

    @BeforeEach
    void setUp() {
        provider = Mockito.mock(EngineRestGatewayProvider.class);
        service = new EngineRestGatewayService();
        ReflectionTestUtils.setField(service, "engineRestGatewayProvider", provider);
    }

    private MockHttpServletRequest requestFor(String fullPath) {
        MockHttpServletRequest rq = new MockHttpServletRequest();
        rq.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, fullPath);
        rq.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/services/v1/engine-rest/**");
        return rq;
    }

    @Test
    void get_extractsSubPathAfterEngineRestPrefix() {
        when(provider.get(any(), any(), any())).thenReturn(ResponseEntity.ok("body"));
        CIBUser user = new CIBUser("demo");

        ResponseEntity<String> response = service.get(
                requestFor("/services/v1/engine-rest/group/count"), Map.of(), user);

        assertThat(response.getBody()).isEqualTo("body");
        verify(provider).get(eq("/group/count"), eq(Map.of()), eq(user));
    }

    @Test
    void get_rootPathBecomesSlash() {
        when(provider.get(any(), any(), any())).thenReturn(ResponseEntity.ok(""));

        service.get(requestFor("/services/v1/engine-rest"), Map.of(), new CIBUser("demo"));

        verify(provider).get(eq("/"), any(), any());
    }

    @Test
    void post_extractsSubPathAndForwardsBody() {
        when(provider.post(any(), any(), any())).thenReturn(ResponseEntity.noContent().build());
        CIBUser user = new CIBUser("demo");

        service.post(requestFor("/services/v1/engine-rest/task/42/submit-form"), "{\"variables\":{}}", user);

        verify(provider).post(eq("/task/42/submit-form"), eq("{\"variables\":{}}"), eq(user));
    }
}
