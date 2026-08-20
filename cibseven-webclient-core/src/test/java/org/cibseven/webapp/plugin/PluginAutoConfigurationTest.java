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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * The plugin beans reach every product through auto-configuration, because each of
 * them component-scans a different set of packages and none scans this one.
 */
public class PluginAutoConfigurationTest {

	private static final String IMPORTS =
		"META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

	// The controller extends BaseService, which every product wires these into
	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withBean(BpmProvider.class, () -> Mockito.mock(BpmProvider.class))
		.withBean(BaseUserProvider.class, () -> Mockito.mock(BaseUserProvider.class))
		.withConfiguration(AutoConfigurations.of(PluginAutoConfiguration.class));

	@Test
	public void contributesThePluginBeans() {
		runner.run(context -> assertThat(context)
			.hasSingleBean(PluginRegistry.class)
			.hasSingleBean(PluginService.class));
	}

	@Test
	public void contributesThemWithPluginsDisabled() {
		// Disabled is the default: the beans exist, they just discover and serve nothing
		runner.run(context -> assertThat(context.getBean(PluginRegistry.class).isEnabled()).isFalse());
	}

	@Test
	public void readsTheEnabledFlagFromConfiguration() {
		runner.withPropertyValues("cibseven.webclient.plugins.enabled=true")
			.run(context -> assertThat(context.getBean(PluginRegistry.class).isEnabled()).isTrue());
	}

	@Test
	public void isRegisteredAsAnAutoConfiguration() throws Exception {
		// Without this entry the beans silently disappear from every product
		String imports = StreamUtils.copyToString(
			new ClassPathResource(IMPORTS).getInputStream(), StandardCharsets.UTF_8);

		assertThat(imports).contains(PluginAutoConfiguration.class.getName());
	}
}
