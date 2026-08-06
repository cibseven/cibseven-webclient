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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Serves the plugin fixture below {@code src/test/resources/META-INF/cibseven-plugins}
 * through the real resource handler, which the other tests only check the registration of.
 */
public class PluginResourceServingTest {

	@Configuration
	@EnableWebMvc
	static class WebConfig {

		PluginRegistry pluginRegistry(boolean enabled) {
			return new PluginRegistry(enabled);
		}
	}

	private MockMvc mockMvc(boolean pluginsEnabled) {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
			"cibseven.webclient.plugins.enabled=" + pluginsEnabled);
		context.register(WebConfig.class, PluginRegistry.class, PluginResourceConfiguration.class);
		context.refresh();
		return MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	public void servesAPluginFileFromTheClasspath() throws Exception {
		mockMvc(true).perform(get("/plugins/test-plugin/index.js"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("export function register")));
	}

	@Test
	public void servesTheManifestOfADeployedPlugin() throws Exception {
		mockMvc(true).perform(get("/plugins/test-plugin/plugin.json"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"apiVersion\"")));
	}

	@Test
	public void answersNotFoundForAnUnknownPlugin() throws Exception {
		mockMvc(true).perform(get("/plugins/does-not-exist/index.js"))
			.andExpect(status().isNotFound());
	}

	/** The served location is the plugin folder, so a crafted path must not reach beyond it. */
	@Test
	public void doesNotServeAnythingOutsideThePluginFolders() throws Exception {
		mockMvc(true).perform(get("/plugins/test-plugin/../../../application.yml"))
			.andExpect(status().isNotFound());
	}

	@Test
	public void servesNothingWhenPluginsAreDisabled() throws Exception {
		mockMvc(false).perform(get("/plugins/test-plugin/index.js"))
			.andExpect(status().isNotFound());
	}
}
