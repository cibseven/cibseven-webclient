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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.cibseven.webapp.SevenWebclientApplication;
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Excludes {@code DataSourceAutoConfiguration} and {@code JndiDataSourceAutoConfiguration} when the
 * standalone webclient runs with the modeler disabled, so it needs no database: no embedded H2
 * database is created and the JNDI data source the WildFly WAR names does not have to exist. The
 * exclusions are added to any {@code spring.autoconfigure.exclude} the application configures.
 *
 * <p>An application that embeds the webclient keeps its data sources: it is left untouched.</p>
 *
 * <p>Registered in META-INF/spring.factories.</p>
 */
public class ModelerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String DATA_SOURCE_AUTO_CONFIGURATION =
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration";
    static final String JNDI_DATA_SOURCE_AUTO_CONFIGURATION =
        "org.springframework.boot.jdbc.autoconfigure.JndiDataSourceAutoConfiguration";

    private static final String MODELER_ENABLED_PROPERTY = SevenWebclientProperties.MODELER_PREFIX + ".enabled";
    private static final String EXCLUDE_PROPERTY = "spring.autoconfigure.exclude";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!SevenWebclientApplication.class.equals(application.getMainApplicationClass())) {
            return;
        }
        if (environment.getProperty(MODELER_ENABLED_PROPERTY, Boolean.class, false)) {
            return;
        }

        List<String> excluded = new ArrayList<>(Binder.get(environment)
            .bind(EXCLUDE_PROPERTY, Bindable.listOf(String.class))
            .orElse(List.of()));
        List.of(DATA_SOURCE_AUTO_CONFIGURATION, JNDI_DATA_SOURCE_AUTO_CONFIGURATION).stream()
            .filter(exclusion -> !excluded.contains(exclusion))
            .forEach(excluded::add);
        environment.getPropertySources().addFirst(
            new MapPropertySource("modelerDisabledExclusions", Map.of(EXCLUDE_PROPERTY, String.join(",", excluded)))
        );
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
