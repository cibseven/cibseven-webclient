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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Serves the plugin fixture below {@code src/test/resources/META-INF/cibseven-plugins}
 * through the real controller, which the other tests only check the registration of.
 */
public class PluginResourceServingTest {

	/** What the distributions configure, so the endpoints sit where the frontend asks. */
	private static final String BASE_PATH = "/services/v1";

	@Configuration
	@EnableWebMvc
	static class WebConfig {

		// PluginService extends BaseService, which autowires these
		@Bean
		BpmProvider bpmProvider() {
			return Mockito.mock(BpmProvider.class);
		}

		@Bean
		BaseUserProvider<?> baseUserProvider() {
			return Mockito.mock(BaseUserProvider.class);
		}
	}

	private MockMvc mockMvc(String... properties) {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		// The controller only exists where plugins are enabled
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "cibseven.webclient.plugins.enabled=true");
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, properties);
		context.register(WebConfig.class, PluginRegistry.class, PluginService.class);
		context.refresh();
		return MockMvcBuilders.webAppContextSetup(context).build();
	}

	/** Both endpoints sit below the services base path, like every other one. */
	@Test
	public void listsTheDeployedPluginsBelowTheServicesBasePath() throws Exception {
		mockMvc().perform(get(BASE_PATH + "/plugins"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"test-plugin\"")));
	}

	/**
	 * A distribution may move the whole API, and the frontend builds the plugin URLs
	 * from the base path it read from '/info' - so the endpoints have to follow it.
	 */
	@Test
	public void followsAConfiguredServicesBasePath() throws Exception {
		MockMvc mockMvc = mockMvc("cibseven.webclient.services.basePath=custom/api");

		mockMvc.perform(get("/custom/api/plugins")).andExpect(status().isOk());
		mockMvc.perform(get("/custom/api/plugins/test-plugin/index.js")).andExpect(status().isOk());
		mockMvc.perform(get(BASE_PATH + "/plugins")).andExpect(status().isNotFound());
	}

	@Test
	public void servesAPluginFileFromTheClasspath() throws Exception {
		mockMvc().perform(get(BASE_PATH + "/plugins/test-plugin/index.js"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("export function register")));
	}

	@Test
	public void servesTheManifestOfADeployedPlugin() throws Exception {
		mockMvc().perform(get(BASE_PATH + "/plugins/test-plugin/plugin.json"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"apiVersion\"")));
	}

	/** The browser imports the entry as a module, so it has to arrive as JavaScript. */
	@Test
	public void servesAPluginFileWithItsOwnContentType() throws Exception {
		mockMvc().perform(get(BASE_PATH + "/plugins/test-plugin/index.js"))
			.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("javascript")));
		mockMvc().perform(get(BASE_PATH + "/plugins/test-plugin/plugin.json"))
			.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("json")));
	}

	/**
	 * A distribution can mount the webclient below an application path, and the
	 * frontend then asks for the files there.
	 */
	@Test
	public void servesAPluginFileBelowAnApplicationPath() throws Exception {
		mockMvc().perform(get("/webapp" + BASE_PATH + "/plugins/test-plugin/index.js").servletPath("/webapp"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("export function register")));
	}

	/** Both plugins of one artifact are served, each under its own id. */
	@Test
	public void servesEveryPluginOfOneClasspathEntry() throws Exception {
		mockMvc().perform(get(BASE_PATH + "/plugins/second-plugin/main.js"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("export function register")));
	}

	@Test
	public void answersNotFoundForAnUnknownPlugin() throws Exception {
		mockMvc().perform(get(BASE_PATH + "/plugins/does-not-exist/index.js"))
			.andExpect(status().isNotFound());
	}

	@Test
	public void answersNotFoundForAFileThePluginDoesNotShip() throws Exception {
		mockMvc().perform(get(BASE_PATH + "/plugins/test-plugin/missing.js"))
			.andExpect(status().isNotFound());
	}

	/** The served location is the plugin folder, so a crafted path must not reach beyond it. */
	@Test
	public void doesNotServeAnythingOutsideThePluginFolders() throws Exception {
		mockMvc().perform(get(BASE_PATH + "/plugins/test-plugin/../../../application.yml"))
			.andExpect(status().isNotFound());
	}

	/** Encoding the traversal must not get past the check either. */
	@Test
	public void doesNotServeAnEncodedPathOutsideThePluginFolders() throws Exception {
		mockMvc().perform(get(BASE_PATH + "/plugins/test-plugin/%2e%2e/%2e%2e/application.yml"))
			.andExpect(status().isNotFound());
	}

	/** One plugin's id must not serve another plugin's files. */
	@Test
	public void doesNotServeAnotherPluginsFiles() throws Exception {
		mockMvc().perform(get(BASE_PATH + "/plugins/test-plugin/../second-plugin/main.js"))
			.andExpect(status().isNotFound());
	}

}
