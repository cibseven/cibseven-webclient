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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Reads the configuration files of the webclient, {@code cibseven-webclient.properties} and
 * {@code cibseven-webclient.yaml}, from the classpath, the same way in the standalone application,
 * in the Run distribution and when the webclient is embedded into another application. The engine
 * REST API reads {@code cibseven-webclient.properties} as well, e.g. for the shared JWT secret.
 *
 * <p>The files rank just above the default properties: every configuration file of the
 * application, environment variables and system properties override them. The YAML file overrides
 * the properties file. Called by the {@code EnvironmentPostProcessor} of each web module, after
 * Spring Boot has loaded the application's configuration.</p>
 */
public final class WebclientConfigFiles {

	/** In ascending precedence */
	static final List<String> LOCATIONS = List.of(
		"classpath:cibseven-webclient.properties", "classpath:cibseven-webclient.yaml");

	/** Name of the property source holding the default properties in both Spring Boot 3 and 4 */
	private static final String DEFAULT_PROPERTIES = "defaultProperties";

	private WebclientConfigFiles() {
	}

	public static void addTo(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
		if (!environment.getProperty(SevenWebclientProperties.PREFIX + ".enabled", Boolean.class, true)) {
			return;
		}
		MutablePropertySources sources = environment.getPropertySources();
		// Each source goes directly above the previous one, the first above the default properties
		String below = sources.contains(DEFAULT_PROPERTIES) ? DEFAULT_PROPERTIES : null;
		for (String location : LOCATIONS) {
			Resource resource = resourceLoader.getResource(location);
			if (!resource.exists()) {
				continue;
			}
			for (PropertySource<?> source : load(resource, location)) {
				if (below != null) {
					sources.addBefore(below, source);
				}
				else {
					sources.addLast(source);
				}
				below = source.getName();
			}
		}
	}

	private static List<PropertySource<?>> load(Resource resource, String location) {
		PropertySourceLoader loader = location.endsWith(".yaml")
			? new YamlPropertySourceLoader()
			: new PropertiesPropertySourceLoader();
		try {
			return loader.load(location, resource);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not read " + location, e);
		}
	}
}
