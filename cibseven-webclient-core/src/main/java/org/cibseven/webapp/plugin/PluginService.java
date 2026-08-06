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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Tells the frontend which plugins are deployed.
 *
 * Deliberately a controller of its own instead of another method on
 * {@code InfoService}: the endpoint has to stay available in products that
 * replace that service, and it needs neither the engine nor a user.
 *
 * The response is readable without authentication, like the rest of {@code /info}
 * - the frontend needs it before anybody has logged in. It lists which plugins
 * exist, nothing more; plugin data is fetched through the regular authenticated
 * endpoints.
 *
 * Unlike the other controllers this one does not extend {@code BaseService}: it
 * neither talks to the engine nor checks permissions, and inheriting would make
 * it depend on beans it never uses.
 */
@ApiResponses({ @ApiResponse(responseCode = "500", description = "An unexpected system error occured") })
@RestController @RequestMapping("/info/plugins")
public class PluginService {

	private final PluginRegistry pluginRegistry;

	public PluginService(PluginRegistry pluginRegistry) {
		this.pluginRegistry = pluginRegistry;
	}

	@Operation(
			summary = "Get the frontend plugins deployed on the classpath",
			description = "<strong>Return: JSON object with a \"plugins\" array of manifests")
	@GetMapping
	public ObjectNode getPlugins() {
		ArrayNode plugins = JsonNodeFactory.instance.arrayNode();
		pluginRegistry.getManifests().forEach(plugins::add);

		ObjectNode response = JsonNodeFactory.instance.objectNode();
		response.set("plugins", plugins);
		return response;
	}
}
