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
package org.cibseven.webapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class WebclientConfigFilesTest {

	/** Serves the files below config-files/ for the classpath locations, or nothing */
	private static ResourceLoader classpathWith(boolean files) {
		return new DefaultResourceLoader() {
			@Override
			public Resource getResource(String location) {
				return new ClassPathResource((files ? "config-files/" : "missing/") + location.substring("classpath:".length()));
			}
		};
	}

	private static StandardEnvironment environment(Map<String, Object> defaults) {
		StandardEnvironment environment = new StandardEnvironment();
		if (defaults != null) {
			environment.getPropertySources().addLast(new MapPropertySource("defaultProperties", defaults));
		}
		return environment;
	}

	private static SevenWebclientProperties bind(StandardEnvironment environment) {
		return Binder.get(environment).bindOrCreate(SevenWebclientProperties.PREFIX, SevenWebclientProperties.class);
	}

	private static List<String> names(StandardEnvironment environment) {
		return environment.getPropertySources().stream().map(PropertySource::getName).toList();
	}

	@Test
	public void yamlOverridesProperties() {
		StandardEnvironment environment = environment(null);
		WebclientConfigFiles.addTo(environment, classpathWith(true));

		SevenWebclientProperties properties = bind(environment);
		assertEquals("from-yaml", properties.getTheme());
		assertEquals("from-yaml", properties.getHistoryLevel());
		assertEquals("from-properties", properties.getProductNamePageTitle());
	}

	@Test
	public void ranksBetweenTheApplicationConfigurationAndTheDefaults() {
		StandardEnvironment environment = environment(Map.of(
			"cibseven.webclient.theme", "from-defaults",
			"cibseven.webclient.url", "from-defaults"));
		environment.getPropertySources().addFirst(new MapPropertySource("application",
			Map.of("cibseven.webclient.history-level", "from-application")));
		WebclientConfigFiles.addTo(environment, classpathWith(true));

		SevenWebclientProperties properties = bind(environment);
		assertEquals("from-application", properties.getHistoryLevel());
		assertEquals("from-yaml", properties.getTheme());
		assertEquals("from-defaults", properties.getUrl());
		List<String> names = names(environment);
		assertEquals(names.size() - 1, names.indexOf("defaultProperties"));
		assertEquals(names.size() - 2, names.indexOf("classpath:cibseven-webclient.properties"));
		assertEquals(names.size() - 3, names.indexOf("classpath:cibseven-webclient.yaml"));
	}

	@Test
	public void addsNothingWithoutTheFiles() {
		StandardEnvironment environment = environment(Map.of());
		List<String> before = names(environment);
		WebclientConfigFiles.addTo(environment, classpathWith(false));

		assertEquals(before, names(environment));
	}

	@Test
	public void addsNothingWhenTheWebclientIsDisabled() {
		StandardEnvironment environment = environment(Map.of());
		environment.getPropertySources().addFirst(new MapPropertySource("application",
			Map.of("cibseven.webclient.enabled", "false")));
		List<String> before = names(environment);
		WebclientConfigFiles.addTo(environment, classpathWith(true));

		assertEquals(before, names(environment));
		assertEquals("cib", bind(environment).getTheme());
	}
}
