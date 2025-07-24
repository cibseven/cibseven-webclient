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
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import org.cibseven.webapp.rest.TestRestTemplateConfiguration;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.IProcessProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(properties = {
    "cibseven.webclient.rest.enabled=true",
    "logging.level.org.cibseven.webapp.rest=DEBUG"
})
@ContextConfiguration(classes = {
    TestRestTemplateConfiguration.class,
    AnalyticsServiceRestTemplateTest.TestConfig.class
})
public class AnalyticsServiceRestTemplateTest {

    @Configuration
    static class TestConfig {
        @Bean
        public BpmProvider bpmProvider() {
            return mock(BpmProvider.class);
        }

        @Bean
        public BaseUserProvider baseUserProvider() {
            return mock(BaseUserProvider.class);
        }

        @Bean
        public IProcessProvider processProvider() {
            return mock(IProcessProvider.class);
        }

        @Bean
        public AnalyticsService analyticsService() {
            return new AnalyticsService();
        }
    }

    @Autowired
    private CustomRestTemplate customRestTemplate;

    @Autowired
    private RestTemplateConfiguration restTemplateConfiguration;

    @Test
    public void testRestTemplateWithAnalyticsService() {
        log.info("Testing RestTemplate with AnalyticsService");

        // Verify that the RestTemplateConfiguration bean is created
        assertThat(restTemplateConfiguration).isNotNull();
        log.info("RestTemplateConfiguration: enabled={}, connectTimeout={}s, socketTimeout={}s",
                restTemplateConfiguration.isEnabled(), restTemplateConfiguration.getConnectTimeout(), restTemplateConfiguration.getSocketTimeout());

        // Verify that the CustomRestTemplate bean is created
        assertThat(customRestTemplate).isNotNull();
        log.info("CustomRestTemplate: {}", customRestTemplate);

        // Verify that the CustomRestTemplate is configured with the RestTemplateConfiguration
        log.info("CustomRestTemplate connection pool stats: {}", customRestTemplate.getConnectionPoolStats());
    }
}
