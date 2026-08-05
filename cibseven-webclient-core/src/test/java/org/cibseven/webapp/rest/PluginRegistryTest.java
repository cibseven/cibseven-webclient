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
package org.cibseven.webapp.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class PluginRegistryTest {

	private static final String MANIFESTS = "classpath*:/META-INF/cibseven-plugins/*/plugin.json";
	private static final String ROOTS = "classpath*:/META-INF/cibseven-plugins/";

	/** Resource that reports a path, like the classpath resources being replaced */
	private static Resource manifest(String path, String json) {
		return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)) {
			@Override
			public java.net.URL getURL() throws IOException {
				return new java.net.URL("file:" + path);
			}
		};
	}

	@Test
	public void findsPluginOnTheClasspath() {
		// Reads the fixture below src/test/resources, i.e. a real classpath scan
		PluginRegistry registry = new PluginRegistry(true);

		List<ObjectNode> manifests = registry.getManifests();

		assertEquals(1, manifests.size());
		ObjectNode manifest = manifests.get(0);
		assertEquals("test-plugin", manifest.get("id").asText());
		assertEquals("index.js", manifest.get("entry").asText());
		assertEquals("1", manifest.get("apiVersion").asText());
		assertEquals("translations_en.json", manifest.get("translations").get("en").asText());
	}

	@Test
	public void findsPluginFolderToServeFilesFrom() {
		PluginRegistry registry = new PluginRegistry(true);

		assertFalse(registry.getPluginRoots().isEmpty());
	}

	@Test
	public void reportsNothingWhenDisabled() {
		PluginRegistry registry = new PluginRegistry(false);

		assertFalse(registry.isEnabled());
		assertTrue(registry.getManifests().isEmpty());
		assertTrue(registry.getPluginRoots().isEmpty());
	}

	@Test
	public void doesNotScanWhenDisabled() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		PluginRegistry registry = new PluginRegistry(false, resolver);

		registry.getManifests();

		verify(resolver, times(0)).getResources(anyString());
	}

	@Test
	public void scansOnlyOnce() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/demo/plugin.json", "{\"entry\":\"index.js\",\"apiVersion\":\"1\"}")
		});
		PluginRegistry registry = new PluginRegistry(true, resolver);

		List<ObjectNode> first = registry.getManifests();
		List<ObjectNode> second = registry.getManifests();

		assertSame(first, second);
		verify(resolver, times(1)).getResources(MANIFESTS);
	}

	@Test
	public void takesTheIdFromTheFolderAndIgnoresTheManifest() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/demo/plugin.json",
				"{\"id\":\"claims-to-be-something-else\",\"entry\":\"index.js\",\"apiVersion\":\"1\"}")
		});

		List<ObjectNode> manifests = new PluginRegistry(true, resolver).getManifests();

		// The folder is where the files are served from, so it decides the id
		assertEquals("demo", manifests.get(0).get("id").asText());
	}

	@Test
	public void skipsManifestWithoutEntry() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/demo/plugin.json", "{\"apiVersion\":\"1\"}")
		});

		assertTrue(new PluginRegistry(true, resolver).getManifests().isEmpty());
	}

	@Test
	public void skipsUnreadableManifest() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/demo/plugin.json", "this is not json")
		});

		assertTrue(new PluginRegistry(true, resolver).getManifests().isEmpty());
	}

	@Test
	public void skipsPluginWhoseFolderNameCouldEscapeTheServedLocation() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenReturn(new Resource[] {
			manifest("/app/META-INF/cibseven-plugins/../plugin.json", "{\"entry\":\"index.js\",\"apiVersion\":\"1\"}")
		});

		assertTrue(new PluginRegistry(true, resolver).getManifests().isEmpty());
	}

	@Test
	public void keepsGoingWhenTheClasspathCannotBeScanned() throws IOException {
		ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
		when(resolver.getResources(MANIFESTS)).thenThrow(new IOException("broken classpath"));
		when(resolver.getResources(ROOTS)).thenThrow(new IOException("broken classpath"));
		PluginRegistry registry = new PluginRegistry(true, resolver);

		assertNotNull(registry.getManifests());
		assertTrue(registry.getManifests().isEmpty());
		assertTrue(registry.getPluginRoots().isEmpty());
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

		List<ObjectNode> manifests = new PluginRegistry(true, resolver).getManifests();

		assertEquals(2, manifests.size());
		assertEquals("first", manifests.get(0).get("id").asText());
		assertEquals("second", manifests.get(1).get("id").asText());
	}
}
