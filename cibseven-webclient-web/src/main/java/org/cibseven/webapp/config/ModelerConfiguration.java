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
package org.cibseven.webapp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring configuration that activates all modeler components: JPA entities,
 * repositories, REST controllers, and service providers from cibseven-modeler.
 *
 * Controlled by {@code cibseven.webclient.modeler.enabled} (default: {@code true}).
 * Set to {@code false} to fully disable the modeler including its database
 * components — no datasource or schema migrations are then required.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "cibseven.webclient.modeler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EntityScan("org.cibseven.modeler.model")
@EnableJpaRepositories("org.cibseven.modeler.repository")
@ComponentScan({
    "org.cibseven.modeler.rest",
    "org.cibseven.modeler.provider",
    "org.cibseven.modeler.config",
    "org.cibseven.modeler.repository"
})
public class ModelerConfiguration {
}
