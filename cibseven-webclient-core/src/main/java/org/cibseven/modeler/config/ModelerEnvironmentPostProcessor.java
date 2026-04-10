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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Automatically excludes JPA, DataSource, and Flyway Spring Boot auto-configurations
 * when the modeler is disabled ({@code cibseven.webclient.modeler.enabled=false}).
 *
 * <p>This prevents Spring Boot from requiring a configured datasource when the modeler
 * feature is turned off. Without this, {@code spring-boot-starter-data-jpa} being on
 * the classpath would cause a startup failure if no {@code spring.datasource.url}
 * is provided.</p>
 *
 * <p>Registered via {@code META-INF/spring.factories}. Runs at
 * {@link Ordered#LOWEST_PRECEDENCE} so that all application config files
 * ({@code application.yaml}, {@code cibseven-webclient.yaml}) are already loaded
 * into the environment before this processor reads their values.</p>
 */
public class ModelerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String MODELER_ENABLED_PROPERTY = "cibseven.webclient.modeler.enabled";
    private static final String MODELER_DB_CONFIGURED_PROPERTY = "cibseven.webclient.modeler.dbConfigured";
    private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";

    private static final String EXCLUDE_PROPERTY = "spring.autoconfigure.exclude";

    private static final String EXCLUSIONS = String.join(",",
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean modelerEnabled = environment.getProperty(MODELER_ENABLED_PROPERTY, Boolean.class, true);

        if (!modelerEnabled) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(EXCLUDE_PROPERTY, EXCLUSIONS);
            environment.getPropertySources().addLast(
                new MapPropertySource("modelerDisabledAutoConfigExclusions", properties)
            );
            return;
        }

        String datasourceUrl = environment.getProperty(DATASOURCE_URL_PROPERTY);
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(EXCLUDE_PROPERTY, EXCLUSIONS);
            properties.put(MODELER_DB_CONFIGURED_PROPERTY, false);
            environment.getPropertySources().addLast(
                new MapPropertySource("modelerDbNotConfiguredExclusions", properties)
            );
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
