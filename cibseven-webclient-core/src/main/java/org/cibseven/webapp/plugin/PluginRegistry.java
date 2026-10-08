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
import java.util.LinkedHashSet;
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
 *
 * <p>Plugins listed in {@code cibseven.webclient.plugins.disabled} are skipped, so a
 * plugin that breaks the page can be switched off with a restart, without removing
 * its jar.
 */
@Slf4j
public class PluginRegistry {

	private static final String PLUGINS_ROOT = "META-INF/cibseven-plugins/";
	private static final String MANIFESTS_PATTERN = "classpath*:/" + PLUGINS_ROOT + "*/plugin.json";
	private static final String DISABLED_PROPERTY = "cibseven.webclient.plugins.disabled";

	/** Ids end up in URLs, so anything that could leave the plugin folder is rejected */
	private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

	private static final List<String> OPTIONAL_FIELDS = List.of("slots", "styles", "translations");

	/**
	 * Read for the report only: the plugin list is public, and the loader needs none of
	 * them, so it does not tell anonymous callers which versions are installed.
	 */
	private static final List<String> REPORT_FIELDS = List.of("name", "version", "description");

	/** What the scan made of a plugin it found, as reported to administrators. */
	public enum Status { ACCEPTED, DISABLED, REJECTED }

	/** Why a plugin was rejected. */
	public enum Rejection { OUTSIDE_FOLDER, INVALID_ID, NO_ENTRY, UNREADABLE, DUPLICATE_ID, FOLDER_UNRESOLVED }

	private final ResourcePatternResolver resolver;
	private final ObjectMapper mapper = new ObjectMapper();

	private final Set<String> disabled;
	private List<ObjectNode> manifests;
	private List<ObjectNode> report = Collections.emptyList();
	private Map<String, Resource> locations = Collections.emptyMap();

	// Needed because of the other constructors: Spring picks one on its own only when
	// there is exactly one.
	@Autowired
	public PluginRegistry(PluginProperties properties) {
		this(new PathMatchingResourcePatternResolver(), properties.getDisabled());
	}

	PluginRegistry(ResourcePatternResolver resolver) {
		this(resolver, List.of());
	}

	PluginRegistry(ResourcePatternResolver resolver, List<String> disabled) {
		this.resolver = resolver;
		this.disabled = new LinkedHashSet<>(disabled);
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

	/**
	 * Every plugin the scan found, served or not, with its {@link Status}, the
	 * {@link Rejection} if there is one, and the classpath entry it came from - what the
	 * server log says, for the administration page.
	 */
	public synchronized List<ObjectNode> getReport() {
		getManifests();
		return report;
	}

	private List<ObjectNode> scan() {
		List<ObjectNode> found = new ArrayList<>();
		List<ObjectNode> entries = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		Map<String, Resource> folders = new LinkedHashMap<>();
		Set<String> skipped = new LinkedHashSet<>();
		try {
			for (Resource resource : resolver.getResources(MANIFESTS_PATTERN)) {
				String folderName = pluginId(resource);
				String source = sourceOf(resource);
				// Checked before the manifest is read, so a broken one cannot get in the way
				if (folderName != null && disabled.contains(folderName)) {
					skipped.add(folderName);
					entries.add(entry(idOnly(folderName), Status.DISABLED, null, source));
					continue;
				}
				ObjectNode manifest = idOnly(folderName);
				Rejection rejection = read(resource, folderName, manifest);
				String id = folderName;
				// Only one folder per id can be served, so a second one would get the first one's files
				if (rejection == null && !ids.add(id)) {
					log.warn("Ignoring a second plugin with id \"{}\" found at {}", id, resource);
					rejection = Rejection.DUPLICATE_ID;
				}
				Resource folder = rejection == null ? folderOf(resource, id) : null;
				if (rejection == null && folder == null) rejection = Rejection.FOLDER_UNRESOLVED;
				if (rejection != null) {
					entries.add(entry(manifest, Status.REJECTED, rejection, source));
					continue;
				}
				folders.put(id, folder);
				ObjectNode served = manifest.deepCopy();
				served.remove(REPORT_FIELDS);
				found.add(served);
				entries.add(entry(manifest, Status.ACCEPTED, null, source));
			}
		} catch (IOException e) {
			log.warn("Could not scan for plugins below {}", PLUGINS_ROOT, e);
		}
		locations = Collections.unmodifiableMap(folders);
		report = Collections.unmodifiableList(entries);
		logDisabled(skipped);
		if (folders.isEmpty()) {
			log.info("No frontend plugin found on the classpath");
		} else {
			log.info("Found {} frontend plugin(s) on the classpath: {}", folders.size(), folders.keySet());
		}
		return Collections.unmodifiableList(found);
	}

	/** Tells support whether the switch took effect, and catches a mistyped id. */
	private void logDisabled(Set<String> skipped) {
		skipped.forEach(id -> log.info("Plugin \"{}\" is disabled by {}", id, DISABLED_PROPERTY));
		Set<String> unknown = new LinkedHashSet<>(disabled);
		unknown.removeAll(skipped);
		if (!unknown.isEmpty()) {
			log.warn("{} names no plugin found on the classpath: {}", DISABLED_PROPERTY, unknown);
		}
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

	/**
	 * Reads the manifest into {@code manifest}, which holds the id already. What could be
	 * read is kept even for a rejected plugin, so the report can still name its version.
	 *
	 * @return why the plugin is rejected, or null when it is accepted
	 */
	private Rejection read(Resource resource, String id, ObjectNode manifest) {
		if (id == null) {
			log.warn("Ignoring plugin manifest outside of a plugin folder: {}", resource);
			return Rejection.OUTSIDE_FOLDER;
		}
		if (!VALID_ID.matcher(id).matches()) {
			log.warn("Ignoring plugin \"{}\": the folder name is not a valid plugin id", id);
			return Rejection.INVALID_ID;
		}
		try (InputStream in = resource.getInputStream()) {
			JsonNode json = mapper.readTree(in);
			String entry = json.path("entry").asText(null);
			boolean hasEntry = entry != null && !entry.isBlank();
			if (hasEntry) manifest.put("entry", entry);
			// Passed on as declared: a plugin may name several versions it was tested against
			if (json.has("apiVersion")) manifest.set("apiVersion", json.get("apiVersion"));
			else manifest.put("apiVersion", "");
			// Every documented optional field belongs here; one missing is silently
			// unavailable in the frontend
			for (String field : OPTIONAL_FIELDS) {
				if (json.has(field)) manifest.set(field, json.get(field));
			}
			for (String field : REPORT_FIELDS) {
				if (json.has(field)) manifest.set(field, json.get(field));
			}
			if (!hasEntry) {
				log.warn("Ignoring plugin \"{}\": its manifest declares no entry", id);
				return Rejection.NO_ENTRY;
			}
			return null;
		} catch (IOException e) {
			log.warn("Ignoring plugin \"{}\": its manifest could not be read", id, e);
			return Rejection.UNREADABLE;
		}
	}

	private static ObjectNode idOnly(String id) {
		ObjectNode manifest = JsonNodeFactory.instance.objectNode();
		if (id != null) manifest.put("id", id);
		return manifest;
	}

	/** A copy, so the manifest the frontend loads from carries no status fields. */
	private static ObjectNode entry(ObjectNode manifest, Status status, Rejection rejection, String source) {
		ObjectNode entry = manifest.deepCopy();
		entry.put("status", status.name());
		if (rejection != null) entry.put("reason", rejection.name());
		if (source != null) entry.put("source", source);
		return entry;
	}

	/**
	 * Names the classpath entry a manifest was found in - a jar's file name, or the last
	 * folder of a directory - so an administrator can tell which jar to look at.
	 */
	private static String sourceOf(Resource resource) {
		String location;
		try {
			location = resource.getURL().toString();
		} catch (IOException e) {
			location = resource.getDescription();
		}
		int end = location.indexOf("/" + PLUGINS_ROOT);
		if (end <= 0) return null;
		String root = location.substring(0, end);
		if (root.endsWith("!")) root = root.substring(0, root.length() - 1);
		return root.substring(root.lastIndexOf('/') + 1);
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
