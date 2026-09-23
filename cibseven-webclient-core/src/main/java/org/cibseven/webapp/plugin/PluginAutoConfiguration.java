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
package org.cibseven.webapp.plugin;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Wires plugin discovery and the plugin endpoints, and only when
 * {@code cibseven.webclient.plugins.enabled} is set: with plugins off there is no
 * registry, no scan and no endpoint at all.
 *
 * <p>Registered in {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * instead of relying on component scanning: every product scans a different set of
 * packages, and this module is the one they all depend on.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "cibseven.webclient.plugins", name = "enabled")
@Import({PluginRegistry.class, PluginService.class})
public class PluginAutoConfiguration {
}
