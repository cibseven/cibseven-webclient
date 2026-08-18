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
import java.util.LinkedHashMap;
import java.util.Map;
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

	/** Ids end up in URLs, so anything that could leave the plugin folder is rejected */
	private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

	private static final List<String> OPTIONAL_FIELDS = List.of("slots", "styles", "translations");

	private final boolean enabled;
	private final ResourcePatternResolver resolver;
	private final ObjectMapper mapper = new ObjectMapper();

	private List<ObjectNode> manifests;
	private Map<String, Resource> locations = Collections.emptyMap();

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
	 * Folder of every accepted plugin, by id, used to serve its files. Keyed by id
	 * and taken from the manifest that was accepted, so a plugin whose manifest was
	 * rejected serves nothing, and a second folder of the same id cannot serve its
	 * files under the accepted plugin's id.
	 */
	public synchronized Map<String, Resource> getPluginLocations() {
		getManifests();
		return locations;
	}

	private List<ObjectNode> scan() {
		List<ObjectNode> found = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		Map<String, Resource> folders = new LinkedHashMap<>();
		try {
			for (Resource resource : resolver.getResources(MANIFESTS_PATTERN)) {
				ObjectNode manifest = read(resource);
				if (manifest == null) continue;
				String id = manifest.get("id").asText();
				// Only one folder per id can be served, so a second one would get the first one's files
				if (!ids.add(id)) {
					log.warn("Ignoring a second plugin with id \"{}\" found at {}", id, resource);
					continue;
				}
				Resource folder = folderOf(resource, id);
				if (folder == null) continue;
				folders.put(id, folder);
				found.add(manifest);
			}
		} catch (IOException e) {
			log.warn("Could not scan for plugins below {}", PLUGINS_ROOT, e);
		}
		locations = Collections.unmodifiableMap(folders);
		log.info("Found {} frontend plugin(s) on the classpath", found.size());
		return Collections.unmodifiableList(found);
	}

	/**
	 * The folder a manifest was found in, so the plugin's files are served from the
	 * jar its manifest came from rather than from whichever jar happens to be first
	 * on the classpath.
	 */
	private Resource folderOf(Resource manifest, String id) {
		try {
			return manifest.createRelative("");
		} catch (IOException e) {
			log.warn("Ignoring plugin \"{}\": its folder could not be resolved", id, e);
			return null;
		}
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
			// every optional field of the documented manifest has to be passed on;
			// one left out here is silently missing in the frontend
			for (String field : OPTIONAL_FIELDS) {
				if (json.has(field)) manifest.set(field, json.get(field));
			}
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
