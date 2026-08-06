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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Discovers frontend plugins on the classpath.
 *
 * A plugin is a folder below {@code META-INF/cibseven-plugins/} containing a
 * {@code plugin.json} and the files it references. Deploying a plugin therefore
 * means putting one more jar on the classpath - no filesystem layout is involved,
 * which is what makes it behave the same in a war and in a Spring Boot jar.
 *
 * The folder name is the plugin id and the only source of it: it is what the
 * frontend builds its URLs from, so a value inside the manifest could contradict
 * the path the files are actually served from.
 */
@Component @Slf4j
public class PluginRegistry {

	private static final String PLUGINS_ROOT = "META-INF/cibseven-plugins/";
	private static final String MANIFESTS_PATTERN = "classpath*:/" + PLUGINS_ROOT + "*/plugin.json";
	private static final String ROOTS_PATTERN = "classpath*:/" + PLUGINS_ROOT;

	/** Ids end up in URLs, so anything that could leave the plugin folder is rejected */
	private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

	private final boolean enabled;
	private final ResourcePatternResolver resolver;
	private final ObjectMapper mapper = new ObjectMapper();

	private List<ObjectNode> manifests;

	// Annotated because this class has a second constructor for tests: Spring only
	// picks a constructor on its own when there is exactly one.
	@Autowired
	public PluginRegistry(@Value("${cibseven.webclient.plugins.enabled:false}") boolean enabled) {
		this(enabled, new PathMatchingResourcePatternResolver());
	}

	PluginRegistry(boolean enabled, ResourcePatternResolver resolver) {
		this.enabled = enabled;
		this.resolver = resolver;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Manifests of all deployed plugins, empty when plugins are disabled. The
	 * classpath cannot change while the application runs, so the scan result is
	 * kept.
	 */
	public synchronized List<ObjectNode> getManifests() {
		if (manifests == null) {
			manifests = enabled ? scan() : Collections.emptyList();
		}
		return manifests;
	}

	/**
	 * The plugin folders themselves, used to serve plugin files. There is one per
	 * jar contributing plugins, so several plugin jars can be deployed together.
	 */
	public List<Resource> getPluginRoots() {
		if (!enabled) return Collections.emptyList();
		try {
			return List.of(resolver.getResources(ROOTS_PATTERN));
		} catch (IOException e) {
			log.warn("Could not resolve plugin locations below {}", PLUGINS_ROOT, e);
			return Collections.emptyList();
		}
	}

	private List<ObjectNode> scan() {
		List<ObjectNode> found = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		try {
			for (Resource resource : resolver.getResources(MANIFESTS_PATTERN)) {
				ObjectNode manifest = read(resource);
				if (manifest == null) continue;
				// Only one folder per id can be served, so a second one would get the first one's files
				if (!ids.add(manifest.get("id").asText())) {
					log.warn("Ignoring a second plugin with id \"{}\" found at {}",
						manifest.get("id").asText(), resource);
					continue;
				}
				found.add(manifest);
			}
		} catch (IOException e) {
			log.warn("Could not scan for plugins below {}", PLUGINS_ROOT, e);
		}
		log.info("Found {} frontend plugin(s) on the classpath", found.size());
		return Collections.unmodifiableList(found);
	}

	private ObjectNode read(Resource resource) {
		String id = pluginId(resource);
		if (id == null) {
			log.warn("Ignoring plugin manifest outside of a plugin folder: {}", resource);
			return null;
		}
		if (!VALID_ID.matcher(id).matches()) {
			log.warn("Ignoring plugin \"{}\": the folder name is not a valid plugin id", id);
			return null;
		}
		try (InputStream in = resource.getInputStream()) {
			JsonNode json = mapper.readTree(in);
			String entry = json.path("entry").asText(null);
			if (entry == null || entry.isBlank()) {
				log.warn("Ignoring plugin \"{}\": its manifest declares no entry", id);
				return null;
			}
			ObjectNode manifest = JsonNodeFactory.instance.objectNode();
			manifest.put("id", id);
			manifest.put("entry", entry);
			manifest.put("apiVersion", json.path("apiVersion").asText(""));
			if (json.has("slots")) manifest.set("slots", json.get("slots"));
			if (json.has("translations")) manifest.set("translations", json.get("translations"));
			return manifest;
		} catch (IOException e) {
			log.warn("Ignoring plugin \"{}\": its manifest could not be read", id, e);
			return null;
		}
	}

	/**
	 * Derives the plugin id from the folder the manifest was found in, for both
	 * file system and jar resources.
	 */
	private String pluginId(Resource resource) {
		String path;
		try {
			path = resource.getURL().getPath();
		} catch (IOException e) {
			path = resource.getDescription();
		}
		int start = path.indexOf(PLUGINS_ROOT);
		if (start < 0) return null;
		String remainder = path.substring(start + PLUGINS_ROOT.length());
		int end = remainder.indexOf('/');
		return end > 0 ? remainder.substring(0, end) : null;
	}
}
