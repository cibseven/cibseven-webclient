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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import jakarta.servlet.ServletContext;

import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

@ExtendWith(MockitoExtension.class)
public class PluginResourceConfigurationTest {

	@Mock
	private PluginRegistry pluginRegistry;

	private ResourceHandlerRegistry registry() {
		ServletContext servletContext = new MockServletContext();
		StaticWebApplicationContext applicationContext = new StaticWebApplicationContext();
		applicationContext.setServletContext(servletContext);
		return new ResourceHandlerRegistry(applicationContext, servletContext);
	}

	@SuppressWarnings("unchecked")
	private Map<String, ?> handlers(ResourceHandlerRegistry registry) {
		SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping)
			org.springframework.test.util.ReflectionTestUtils.invokeMethod(registry, "getHandlerMapping");
		return mapping == null ? Map.of() : (Map<String, ?>) mapping.getUrlMap();
	}

	@Test
	public void servesPluginFilesWhenPluginsAreDeployed() {
		when(pluginRegistry.isEnabled()).thenReturn(true);
		when(pluginRegistry.getPluginLocations()).thenReturn(
			Map.of("demo-report", new ByteArrayResource(new byte[0])));
		ResourceHandlerRegistry registry = registry();

		new PluginResourceConfiguration(pluginRegistry).addResourceHandlers(registry);

		assertEquals(1, handlers(registry).size());
		assertTrue(handlers(registry).containsKey("/plugins/demo-report/**"));
	}

	/** A rejected manifest means no files: the registry only reports accepted plugins. */
	@Test
	public void servesOnePathPerAcceptedPlugin() {
		when(pluginRegistry.isEnabled()).thenReturn(true);
		when(pluginRegistry.getPluginLocations()).thenReturn(Map.of(
			"demo-report", new ByteArrayResource(new byte[0]),
			"other", new ByteArrayResource(new byte[0])));
		ResourceHandlerRegistry registry = registry();

		new PluginResourceConfiguration(pluginRegistry).addResourceHandlers(registry);

		assertEquals(2, handlers(registry).size());
		assertTrue(handlers(registry).keySet().containsAll(
			java.util.List.of("/plugins/demo-report/**", "/plugins/other/**")));
	}

	@Test
	public void registersNothingWhenPluginsAreDisabled() {
		when(pluginRegistry.isEnabled()).thenReturn(false);
		ResourceHandlerRegistry registry = registry();

		new PluginResourceConfiguration(pluginRegistry).addResourceHandlers(registry);

		assertTrue(handlers(registry).isEmpty());
	}

	@Test
	public void registersNothingWhenNoPluginFolderExists() {
		when(pluginRegistry.isEnabled()).thenReturn(true);
		when(pluginRegistry.getPluginLocations()).thenReturn(Map.of());
		ResourceHandlerRegistry registry = registry();

		new PluginResourceConfiguration(pluginRegistry).addResourceHandlers(registry);

		assertTrue(handlers(registry).isEmpty());
	}
}
