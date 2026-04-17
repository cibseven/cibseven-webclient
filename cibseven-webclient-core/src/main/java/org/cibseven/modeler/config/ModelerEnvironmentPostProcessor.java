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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Excludes {@code DataSourceAutoConfiguration} when the modeler is disabled
 * ({@code cibseven.webclient.modeler.enabled=false}) and no
 * {@code spring.datasource.url} is configured.
 *
 * <p>This prevents Spring Boot from auto-creating an embedded H2 database when
 * {@code spring-boot-starter-data-jpa} is on the classpath but no datasource is
 * provided. The Hibernate and JPA repository auto-configurations are left alone -
 * Spring Boot's own {@code @ConditionalOnSingleCandidate(DataSource.class)} guards
 * on those will suppress them naturally when no DataSource bean exists.</p>
 *
 * <p>If {@code spring.datasource.url} is present, nothing is excluded - the client
 * application owns the datasource and must not be interfered with.</p>
 *
 * <p>Registered via {@code META-INF/spring/org.springframework.boot.EnvironmentPostProcessor}. Runs at
 * {@link Ordered#LOWEST_PRECEDENCE} so that all application config files
 * ({@code application.yaml}, {@code cibseven-webclient.yaml}) are already loaded
 * before this processor reads their values.</p>
 */
public class ModelerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String MODELER_ENABLED_PROPERTY = "cibseven.webclient.modeler.enabled";
    private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";
    private static final String EXCLUDE_PROPERTY = "spring.autoconfigure.exclude";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean modelerEnabled = environment.getProperty(MODELER_ENABLED_PROPERTY, Boolean.class, false);
        if (modelerEnabled) {
            return;
        }

        String datasourceUrl = environment.getProperty(DATASOURCE_URL_PROPERTY);
        if (datasourceUrl != null && !datasourceUrl.isBlank()) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(EXCLUDE_PROPERTY,
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration");
        environment.getPropertySources().addLast(
            new MapPropertySource("modelerDisabledExclusions", properties)
        );
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
