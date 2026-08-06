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

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * Serves plugin files from the classpath under {@code /plugins/**}, so the
 * frontend can import them at runtime.
 *
 * Registered through {@link PluginAutoConfiguration} rather than component
 * scanning, which every product configures differently.
 */
@Configuration @Slf4j
public class PluginResourceConfiguration implements WebMvcConfigurer {

	private final PluginRegistry pluginRegistry;

	public PluginResourceConfiguration(PluginRegistry pluginRegistry) {
		this.pluginRegistry = pluginRegistry;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		if (!pluginRegistry.isEnabled()) return;

		List<Resource> roots = pluginRegistry.getPluginRoots();
		if (roots.isEmpty()) {
			log.debug("Plugins are enabled but no plugin folder was found on the classpath");
			return;
		}

		// One location per contributing jar, so several plugin jars can coexist.
		// Spring resolves requests against these locations only, which keeps a
		// crafted path from reaching anything outside a plugin folder.
		registry.addResourceHandler("/plugins/**")
			.addResourceLocations(roots.toArray(new Resource[0]))
			.setCacheControl(CacheControl.noCache());
		log.info("Serving frontend plugins from {} classpath location(s)", roots.size());
	}
}
