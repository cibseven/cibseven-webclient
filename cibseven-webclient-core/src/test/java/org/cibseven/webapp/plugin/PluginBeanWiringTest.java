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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * Lets Spring create the plugin beans, instead of calling their constructors
 * directly as the other tests do. Without this, a class that Spring cannot
 * instantiate - for instance because it has several constructors and none is
 * annotated - only shows up when the application is started.
 */
public class PluginBeanWiringTest {

	private AnnotationConfigApplicationContext enabledContext() {
		return context("cibseven.webclient.plugins.enabled=true");
	}

	private AnnotationConfigApplicationContext context(String... properties) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, properties);
		// The controller extends BaseService, which autowires these
		context.registerBean(BpmProvider.class, () -> Mockito.mock(BpmProvider.class));
		context.registerBean(BaseUserProvider.class, () -> Mockito.mock(BaseUserProvider.class));
		context.register(PluginRegistry.class, PluginService.class);
		context.refresh();
		return context;
	}

	@Test
	public void createsThePluginBeans() {
		try (AnnotationConfigApplicationContext context = enabledContext()) {
			assertNotNull(context.getBean(PluginRegistry.class));
			assertNotNull(context.getBean(PluginService.class));
		}
	}

	/** A registry that exists always scans, and the list it reports is what it found. */
	@Test
	public void servesWhateverTheRegistryFound() {
		try (AnnotationConfigApplicationContext context = enabledContext()) {
			assertTrue(context.getBean(PluginService.class).getPlugins().has("plugins"));
		}
	}

	/**
	 * The controller carries {@code @RestController} and would therefore be picked up
	 * by a product that component-scans this package. Its own condition is what keeps
	 * the endpoints off there too, rather than only in {@link PluginAutoConfiguration}.
	 */
	@Test
	public void registersNoEndpointsWhenPluginsAreDisabled() {
		try (AnnotationConfigApplicationContext context = context()) {
			assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(PluginService.class));
		}
	}
}
