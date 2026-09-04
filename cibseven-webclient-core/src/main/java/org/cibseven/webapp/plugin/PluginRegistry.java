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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Discovers frontend plugins on the classpath: a folder below
 * {@code META-INF/cibseven-plugins/} holding a {@code plugin.json} and the files it
 * references. Deploying one is putting a jar on the classpath, so it works the same
 * in a war and in a Spring Boot jar.
 *
 * <p>The folder name is the plugin id and the only source of it, because that is
 * what the frontend builds its URLs from.
 */
@Slf4j
public class PluginRegistry {

	private static final String PLUGINS_ROOT = "META-INF/cibseven-plugins/";
	private static final String MANIFESTS_PATTERN = "classpath*:/" + PLUGINS_ROOT + "*/plugin.json";

	/** Ids end up in URLs, so anything that could leave the plugin folder is rejected */
	private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

	private static final List<String> OPTIONAL_FIELDS = List.of("slots", "styles", "translations");

	private final ResourcePatternResolver resolver;
	private final ObjectMapper mapper = new ObjectMapper();

	private List<ObjectNode> manifests;
	private Map<String, Resource> locations = Collections.emptyMap();

	// Needed because of the second constructor: Spring picks one on its own only when
	// there is exactly one.
	@Autowired
	public PluginRegistry() {
		this(new PathMatchingResourcePatternResolver());
	}

	PluginRegistry(ResourcePatternResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * Scans at startup rather than on the first request, so an operator who dropped a
	 * plugin jar in sees at boot whether it was picked up.
	 */
	@PostConstruct
	void scanAtStartup() {
		getManifests();
	}

	/** Manifests of all deployed plugins; cached, as the classpath cannot change at runtime. */
	public synchronized List<ObjectNode> getManifests() {
		if (manifests == null) {
			manifests = scan();
		}
		return manifests;
	}

	/**
	 * Folder of every accepted plugin, by id, used to serve its files. Only accepted
	 * manifests are in here, so a rejected or duplicate plugin serves nothing.
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
		if (folders.isEmpty()) {
			log.info("No frontend plugin found on the classpath");
		} else {
			log.info("Found {} frontend plugin(s) on the classpath: {}", folders.size(), folders.keySet());
		}
		return Collections.unmodifiableList(found);
	}

	/** The folder the manifest came from, so files are served from its own jar. */
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
			// Passed on as declared: a plugin may name several versions it was tested against
			if (json.has("apiVersion")) manifest.set("apiVersion", json.get("apiVersion"));
			else manifest.put("apiVersion", "");
			// Every documented optional field belongs here; one missing is silently
			// unavailable in the frontend
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
