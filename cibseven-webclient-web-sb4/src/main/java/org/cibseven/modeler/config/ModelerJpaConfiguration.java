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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Internal Spring configuration that wires the JPA entities, repositories, REST
 * controllers, and service providers for the modeler. This class is package-private
 * and must not be used directly; it is imported exclusively via
 * {@link ModelerAutoConfiguration}.
 *
 * <p>What the modeler puts into the webclient's persistence unit lives in
 * {@link ModelerPersistenceConfiguration}; the unit itself belongs to the webclient.</p>
 */
@Configuration
@Import(ModelerPersistenceConfiguration.class)
@EnableConfigurationProperties(ElementTemplateProperties.class)
@ComponentScan({
    "org.cibseven.modeler.rest",
    "org.cibseven.modeler.provider",
    "org.cibseven.modeler.repository",
    "org.cibseven.modeler.util"
})
class ModelerJpaConfiguration {
}
