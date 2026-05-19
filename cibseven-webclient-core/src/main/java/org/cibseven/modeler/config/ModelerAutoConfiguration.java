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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot auto-configuration entry point for the CIB seven Modeler.
 *
 * <p>Activates all modeler components (JPA entities, repositories, REST controllers,
 * and service providers) when {@code cibseven.webclient.modeler.enabled=true}.
 * Defaults to disabled ({@code matchIfMissing = false}).</p>
 *
 * <p>Schema creation is the responsibility of the parent project. This module
 * only wires the JPA entities and repositories against an already-existing schema.</p>
 *
 * <p>When the modeler is disabled and no {@code spring.datasource.url} is configured,
 * {@link ModelerEnvironmentPostProcessor} excludes {@code DataSourceAutoConfiguration}
 * to prevent Spring Boot from auto-creating an embedded H2 database. Hibernate and JPA
 * repository auto-configurations are not excluded — Spring Boot's own conditionals
 * suppress them naturally when no {@code DataSource} bean exists.</p>
 *
 * <p>Registered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.</p>
 */
@AutoConfiguration(after = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ConditionalOnProperty(
    prefix = "cibseven.webclient.modeler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Import(ModelerJpaConfiguration.class)
public class ModelerAutoConfiguration {
}
