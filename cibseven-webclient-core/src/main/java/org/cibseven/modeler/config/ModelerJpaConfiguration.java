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

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Internal Spring configuration that wires the JPA entities, repositories, REST
 * controllers, and service providers for the modeler. This class is package-private
 * and must not be used directly; it is imported exclusively via
 * {@link ModelerAutoConfiguration}.
 */
@Configuration
@EntityScan("org.cibseven.modeler.model")
@EnableJpaRepositories("org.cibseven.modeler.repository")
@EnableConfigurationProperties(ElementTemplateProperties.class)
@ComponentScan({
    "org.cibseven.modeler.rest",
    "org.cibseven.modeler.provider",
    "org.cibseven.modeler.repository"
})
class ModelerJpaConfiguration {
}
