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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
public class PluginServiceTest {

	@Mock
	private PluginRegistry pluginRegistry;

	private static ObjectNode manifest(String id) {
		ObjectNode manifest = JsonNodeFactory.instance.objectNode();
		manifest.put("id", id);
		manifest.put("entry", "index.js");
		manifest.put("apiVersion", "1");
		return manifest;
	}

	@Test
	public void returnsAnEmptyArrayWhenNoPluginIsDeployed() {
		when(pluginRegistry.getManifests()).thenReturn(List.of());

		ObjectNode response = new PluginService(pluginRegistry).getPlugins();

		// The frontend relies on the array being present, not on its content
		assertTrue(response.has("plugins"));
		assertTrue(response.get("plugins").isArray());
		assertTrue(response.get("plugins").isEmpty());
	}

	@Test
	public void returnsTheDiscoveredManifests() {
		when(pluginRegistry.getManifests()).thenReturn(List.of(manifest("first"), manifest("second")));

		ObjectNode response = new PluginService(pluginRegistry).getPlugins();

		assertEquals(2, response.get("plugins").size());
		assertEquals("first", response.get("plugins").get(0).get("id").asText());
		assertEquals("index.js", response.get("plugins").get(0).get("entry").asText());
		assertEquals("second", response.get("plugins").get(1).get("id").asText());
	}
}
