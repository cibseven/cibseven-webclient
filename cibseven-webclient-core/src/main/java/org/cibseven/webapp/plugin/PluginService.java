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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.cibseven.webapp.rest.BaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Tells the frontend which plugins are deployed and serves their files. Both are
 * readable without authentication, as the frontend needs them before anybody has
 * logged in; plugin <em>data</em> goes through the regular authenticated endpoints.
 *
 * <p>Extends {@code BaseService} without needing it: distributions mount the
 * webclient below an application path ({@code /webapp} in CIB seven Run) by prefixing
 * exactly the controllers assignable to {@code BaseService}. That is also why the
 * files are served here instead of by a resource handler, which would get no such
 * prefix.
 */
@ApiResponses({ @ApiResponse(responseCode = "500", description = "An unexpected system error occured") })
@ConditionalOnProperty(prefix = "cibseven.webclient.plugins", name = "enabled")
@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/plugins") @Slf4j
public class PluginService extends BaseService {

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

	@Operation(
			summary = "Get a file of a deployed frontend plugin",
			description = "<strong>Return: the file, or 404 when the plugin or the file does not exist")
	@GetMapping("/{pluginId}/**")
	public ResponseEntity<Resource> getPluginFile(@PathVariable String pluginId, HttpServletRequest rq) {
		// Only accepted plugins have a folder, so a rejected manifest serves nothing and
		// no id can reach another plugin's files.
		Resource folder = pluginRegistry.getPluginLocations().get(pluginId);
		String file = requestedFile(pluginId, rq);
		if (folder == null || file == null) {
			return ResponseEntity.notFound().build();
		}
		try {
			Resource resource = folder.createRelative(file);
			// Checked against the resolved location rather than the request: whatever the
			// path did, only something inside the plugin folder is served.
			if (!resource.exists() || !resource.isReadable()
					|| !resource.getURL().toString().startsWith(folder.getURL().toString())) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok()
					.cacheControl(CacheControl.noCache())
					.contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
					.body(resource);
		} catch (IOException e) {
			log.debug("Could not serve \"{}\" of plugin \"{}\"", file, pluginId, e);
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * The path below the plugin folder, taken from the request URI so it is found
	 * whatever application path the distribution mounts the webclient under.
	 *
	 * @return the file path, or null when there is none or it could leave the folder
	 */
	private static String requestedFile(String pluginId, HttpServletRequest rq) {
		String marker = "/plugins/" + pluginId + "/";
		String uri = rq.getRequestURI();
		if (uri == null) {
			return null;
		}
		int start = uri.indexOf(marker);
		if (start < 0) {
			return null;
		}
		String file = uri.substring(start + marker.length());
		// Escaped characters are not decoded here, so anything encoded is refused rather
		// than guessed at; an absolute path or a drive letter would leave the folder.
		if (file.isEmpty() || file.startsWith("/") || file.contains("%")
				|| file.contains("\\") || file.contains(":")) {
			return null;
		}
		String clean = StringUtils.cleanPath(file);
		return clean.contains("..") ? null : clean;
	}
}
