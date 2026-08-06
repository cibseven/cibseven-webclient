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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * Lets Spring create the plugin beans, instead of calling their constructors
 * directly as the other tests do. Without this, a class that Spring cannot
 * instantiate - for instance because it has several constructors and none is
 * annotated - only shows up when the application is started.
 */
public class PluginBeanWiringTest {

	private AnnotationConfigApplicationContext context(String... properties) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, properties);
		context.register(PluginRegistry.class, PluginService.class, PluginResourceConfiguration.class);
		context.refresh();
		return context;
	}

	@Test
	public void createsPluginBeansWithPluginsDisabledByDefault() {
		try (AnnotationConfigApplicationContext context = context()) {
			PluginRegistry registry = context.getBean(PluginRegistry.class);

			assertNotNull(registry);
			assertFalse(registry.isEnabled());
			assertNotNull(context.getBean(PluginService.class));
			assertNotNull(context.getBean(PluginResourceConfiguration.class));
		}
	}

	@Test
	public void readsTheEnabledFlagFromConfiguration() {
		try (AnnotationConfigApplicationContext context = context("cibseven.webclient.plugins.enabled=true")) {
			assertTrue(context.getBean(PluginRegistry.class).isEnabled());
		}
	}

	@Test
	public void servesAnEmptyPluginListWhenDisabled() {
		try (AnnotationConfigApplicationContext context = context()) {
			assertTrue(context.getBean(PluginService.class).getPlugins().get("plugins").isEmpty());
		}
	}
}
