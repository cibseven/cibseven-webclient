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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class PluginRegistryTest {

	private static final String MANIFESTS = "classpath*:/META-INF/cibseven-plugins/*/plugin.json";
	private static final String ROOTS = "classpath*:/META-INF/cibseven-plugins/";

	/**
	 * Resource that reports a path and resolves its folder, like the classpath
	 * resources being replaced - the registry needs both.
	 */
	private static Resource manifest(String path, String json) {
		return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)) {
			@Override
			public java.net.URL getURL() throws IOException {
				return new java.net.URL("file:" + path);
			}

			@Override
			public Resource createRelative(String relative) {
				String folder = path.substring(0, path.lastIndexOf('/') + 1);
				return manifest(folder + relative, json);
			}
		};
	}

	@Test
	public void findsPluginOnTheClasspath() {
		// Reads the fixtures below src/test/resources, i.e. a real classpath scan
		PluginRegistry registry = new PluginRegistry();

		ObjectNode manifest = manifestOf(registry, "test-plugin");

		assertEquals("index.js", manifest.get("entry").asText());
		assertEquals("1", manifest.get("apiVersion").asText());
		assertEquals("translations_en.json", manifest.get("translations").get("en").asText());
	}

	/**
	 * The frontend acts on these, so a field dropped here is a feature that silently
	 * does nothing - which is how stylesheets went missing once.
	 */
	@Test
	public void reportsEveryDocumentedFieldOfTheManifest() {
		ObjectNode manifest = manifestOf(new PluginRegistry(), "test-plugin");

		assertEquals("process-instance-tab", manifest.get("slots").get(0).asText());
		assertEquals("styles.css", manifest.get("styles").get(0).asText());
		assertEquals("translations_en.json", manifest.get("translations").get("en").asText());
	}

	/** Several plugins may share one artifact, each in its own folder. */
	@Test
	public void findsEveryPluginOfOneClasspathEntry() {
		PluginRegistry registry = new PluginRegistry();

		List<ObjectNode> manifests = registry.getManifests();

		assertEquals(2, manifests.size());
		assertEquals("main.js", manifestOf(registry, "second-plugin").get("entry").asText());
	}

	private ObjectNode manifestOf(PluginRegistry registry, String id) {
		return registry.getManifests().stream()
			.filter(manifest -> id.equals(manifest.get("id").asText()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("no plugin \"" + id + "\" was found"));
	}

	/** The folders serve the files, so every accepted plugin needs exactly its own. */
	@Test
	public void findsAFolderForEveryAcceptedPlugin() {
		PluginRegistry registry = new PluginRegistry();

		Map<String, Resource> locations = registry.getPluginLocations();
		assertEquals(registry.getManifests().size(), locations.size());
		assertTrue(locations.keySet().containsAll(List.of("test-plugin", "second-plugin")));
		assertTrue(locations.get("test-plugin").getDescription().contains("test-plugin"));
	}

	@Test
	public void scansOnlyOnce() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/demo/plugin.json", "{\"entry\":\"index.js\",\"apiVersion\":\"1\"}")
		});
		PluginRegistry registry = new PluginRegistry(resolver);

		List<ObjectNode> first = registry.getManifests();
		List<ObjectNode> second = registry.getManifests();

		assertSame(first, second);
		verify(resolver, times(1)).getResources(MANIFESTS);
	}

	/**
	 * A plugin may name every version it was tested against, so the frontend has to see
	 * the list rather than a value flattened into a string.
	 */
	@Test
	public void reportsSeveralDeclaredApiVersionsAsDeclared() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/demo/plugin.json",
				"{\"entry\":\"index.js\",\"apiVersion\":[\"1\",\"2\"]}")
		});

		ObjectNode manifest = new PluginRegistry(resolver).getManifests().get(0);

		assertTrue(manifest.get("apiVersion").isArray());
		assertEquals("1", manifest.get("apiVersion").get(0).asText());
		assertEquals("2", manifest.get("apiVersion").get(1).asText());
	}

	@Test
	public void reportsAnAbsentApiVersionAsEmpty() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/demo/plugin.json", "{\"entry\":\"index.js\"}")
		});

		assertEquals("", new PluginRegistry(resolver).getManifests().get(0).get("apiVersion").asText());
	}

	@Test
	public void takesTheIdFromTheFolderAndIgnoresTheManifest() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/demo/plugin.json",
				"{\"id\":\"claims-to-be-something-else\",\"entry\":\"index.js\",\"apiVersion\":\"1\"}")
		});

		List<ObjectNode> manifests = new PluginRegistry(resolver).getManifests();

		// The folder is where the files are served from, so it decides the id
		assertEquals("demo", manifests.get(0).get("id").asText());
	}

	@Test
	public void skipsManifestWithoutEntry() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/demo/plugin.json", "{\"apiVersion\":\"1\"}")
		});

		assertTrue(new PluginRegistry(resolver).getManifests().isEmpty());
	}

	@Test
	public void skipsUnreadableManifest() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/demo/plugin.json", "this is not json")
		});

		assertTrue(new PluginRegistry(resolver).getManifests().isEmpty());
	}

	@Test
	public void skipsPluginWhoseFolderNameCouldEscapeTheServedLocation() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/../plugin.json", "{\"entry\":\"index.js\",\"apiVersion\":\"1\"}")
		});

		assertTrue(new PluginRegistry(resolver).getManifests().isEmpty());
	}

	@Test
	public void keepsOnlyTheFirstOfTwoPluginsSharingAnId() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/a.jar!/META-INF/cibseven-plugins/demo/plugin.json",
				"{\"entry\":\"first.js\",\"apiVersion\":\"1\"}"),
			manifest("/app/b.jar!/META-INF/cibseven-plugins/demo/plugin.json",
				"{\"entry\":\"second.js\",\"apiVersion\":\"1\"}")
		});

		List<ObjectNode> manifests = new PluginRegistry(resolver).getManifests();

		// Files are served from one classpath root only, so the second would get the first one's
		assertEquals(1, manifests.size());
		assertEquals("first.js", manifests.get(0).get("entry").asText());
	}

	@Test
	public void keepsGoingWhenTheClasspathCannotBeScanned() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenThrow(new IOException("broken classpath"));
		when(resolver.getResources(ROOTS)).thenThrow(new IOException("broken classpath"));
		PluginRegistry registry = new PluginRegistry(resolver);

		assertNotNull(registry.getManifests());
		assertTrue(registry.getManifests().isEmpty());
		assertTrue(registry.getPluginLocations().isEmpty());
	}

	@Test
	public void reportsSeveralPluginsFromSeveralJars() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/a.jar!/META-INF/cibseven-plugins/first/plugin.json",
				"{\"entry\":\"index.js\",\"apiVersion\":\"1\"}"),
			manifest("/app/b.jar!/META-INF/cibseven-plugins/second/plugin.json",
				"{\"entry\":\"main.js\",\"apiVersion\":\"1\"}")
		});

		List<ObjectNode> manifests = new PluginRegistry(resolver).getManifests();

		assertEquals(2, manifests.size());
		assertEquals("first", manifests.get(0).get("id").asText());
		assertEquals("second", manifests.get(1).get("id").asText());
	}

	/** An operator who installed a plugin jar has to learn at boot whether it was picked up. */
	@Test
	public void scansAtStartupSoTheResultIsLoggedBeforeTheFirstRequest() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/a.jar!/META-INF/cibseven-plugins/first/plugin.json",
				"{\"entry\":\"index.js\",\"apiVersion\":\"1\"}")
		});
		PluginRegistry registry = new PluginRegistry(resolver);

		registry.scanAtStartup();

		verify(resolver, times(1)).getResources(MANIFESTS);
		// And the result is kept, so the first request does not scan again
		assertEquals(1, registry.getManifests().size());
		verify(resolver, times(1)).getResources(MANIFESTS);
	}

}
