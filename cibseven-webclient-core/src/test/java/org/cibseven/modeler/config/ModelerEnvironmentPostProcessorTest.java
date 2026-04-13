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
package org.cibseven.modeler.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class ModelerEnvironmentPostProcessorTest {

    private ModelerEnvironmentPostProcessor processor;
    private SpringApplication application;
    private StandardEnvironment environment;

    @BeforeEach
    void setUp() {
        processor = new ModelerEnvironmentPostProcessor();
        application = mock(SpringApplication.class);
        environment = new StandardEnvironment();
    }

    @Test
    void doesNotAddExclusions_whenModelerEnabledIsTrueAndDatasourceConfigured() {
        environment.getPropertySources().addFirst(
            new MapPropertySource("test", Map.of(
                "cibseven.webclient.modeler.enabled", "true",
                "spring.datasource.url", "jdbc:h2:mem:testdb"
            ))
        );

        processor.postProcessEnvironment(environment, application);

        assertNull(environment.getProperty("spring.autoconfigure.exclude"));
    }

    @Test
    void doesNotAddExclusions_whenModelerEnabledIsMissingAndDatasourceConfigured() {
        // default is true — only datasource URL set
        environment.getPropertySources().addFirst(
            new MapPropertySource("test", Map.of("spring.datasource.url", "jdbc:h2:mem:testdb"))
        );

        processor.postProcessEnvironment(environment, application);

        assertNull(environment.getProperty("spring.autoconfigure.exclude"));
    }

    @Test
    void addsExclusionsAndSetsDbConfiguredFalse_whenModelerEnabledAndNoDatasourceUrl() {
        environment.getPropertySources().addFirst(
            new MapPropertySource("test", Map.of("cibseven.webclient.modeler.enabled", "true"))
        );

        processor.postProcessEnvironment(environment, application);

        String exclude = environment.getProperty("spring.autoconfigure.exclude");
        assertTrue(exclude.contains("DataSourceAutoConfiguration"),
            "Expected DataSourceAutoConfiguration to be excluded when datasource URL is missing");
        assertEquals("false", environment.getProperty("cibseven.webclient.modeler.dbConfigured"),
            "Expected modelerDbConfigured to be false when datasource URL is missing");
    }

    @Test
    void addsExclusionsAndSetsDbConfiguredFalse_whenNoDatasourceUrlAndNoModelerProperty() {
        // no properties at all — modeler defaults to enabled, no datasource URL
        processor.postProcessEnvironment(environment, application);

        String exclude = environment.getProperty("spring.autoconfigure.exclude");
        assertTrue(exclude.contains("DataSourceAutoConfiguration"),
            "Expected DataSourceAutoConfiguration to be excluded when datasource URL is missing");
        assertEquals("false", environment.getProperty("cibseven.webclient.modeler.dbConfigured"),
            "Expected modelerDbConfigured to be false when datasource URL is missing");
    }

    @Test
    void addsDataSourceExclusion_whenModelerDisabledAndNoDatasourceUrl() {
        environment.getPropertySources().addFirst(
            new MapPropertySource("test", Map.of("cibseven.webclient.modeler.enabled", "false"))
        );

        processor.postProcessEnvironment(environment, application);

        String exclude = environment.getProperty("spring.autoconfigure.exclude");
        assertTrue(exclude.contains("DataSourceAutoConfiguration"),
            "Expected DataSourceAutoConfiguration to be excluded when no datasource URL is set");
    }

    @Test
    void doesNotExcludeDataSource_whenModelerDisabledButDatasourceUrlConfigured() {
        // BPM engine uses spring.datasource.url — DataSourceAutoConfiguration must stay active
        // so that DataSourceTransactionManagerAutoConfiguration can create PlatformTransactionManager
        environment.getPropertySources().addFirst(
            new MapPropertySource("test", Map.of(
                "cibseven.webclient.modeler.enabled", "false",
                "spring.datasource.url", "jdbc:h2:./process-engine"
            ))
        );

        processor.postProcessEnvironment(environment, application);

        String exclude = environment.getProperty("spring.autoconfigure.exclude");
        assertTrue(exclude == null || !exclude.contains("DataSourceAutoConfiguration"),
            "DataSourceAutoConfiguration must not be excluded when spring.datasource.url is configured");
        assertTrue(exclude.contains("HibernateJpaAutoConfiguration"),
            "Expected HibernateJpaAutoConfiguration to still be excluded");
        assertTrue(exclude.contains("FlywayAutoConfiguration"),
            "Expected FlywayAutoConfiguration to still be excluded");
    }

    @Test
    void addsHibernateJpaExclusion_whenModelerDisabledAndNoDatasourceUrl() {
        environment.getPropertySources().addFirst(
            new MapPropertySource("test", Map.of("cibseven.webclient.modeler.enabled", "false"))
        );

        processor.postProcessEnvironment(environment, application);

        String exclude = environment.getProperty("spring.autoconfigure.exclude");
        assertTrue(exclude.contains("HibernateJpaAutoConfiguration"),
            "Expected HibernateJpaAutoConfiguration to be excluded");
    }

    @Test
    void addsJpaRepositoriesExclusion_whenModelerDisabledAndNoDatasourceUrl() {
        environment.getPropertySources().addFirst(
            new MapPropertySource("test", Map.of("cibseven.webclient.modeler.enabled", "false"))
        );

        processor.postProcessEnvironment(environment, application);

        String exclude = environment.getProperty("spring.autoconfigure.exclude");
        assertTrue(exclude.contains("JpaRepositoriesAutoConfiguration"),
            "Expected JpaRepositoriesAutoConfiguration to be excluded");
    }

    @Test
    void addsFlywayExclusion_whenModelerDisabledAndNoDatasourceUrl() {
        environment.getPropertySources().addFirst(
            new MapPropertySource("test", Map.of("cibseven.webclient.modeler.enabled", "false"))
        );

        processor.postProcessEnvironment(environment, application);

        String exclude = environment.getProperty("spring.autoconfigure.exclude");
        assertTrue(exclude.contains("FlywayAutoConfiguration"),
            "Expected FlywayAutoConfiguration to be excluded");
    }
}
