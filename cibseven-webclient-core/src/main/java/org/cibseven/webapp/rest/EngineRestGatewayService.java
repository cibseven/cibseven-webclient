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

import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.EngineRestGatewayProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Allow-listed engine-rest gateway for embedded forms.
 *
 * <p>Lives on a prefix no other controller owns ({@code /services/v1/engine-rest/**}),
 * so it never collides with the curated middleware controllers under
 * {@code /services/v1}. The embedded-form bpm-sdk client is pointed here; requests are
 * forwarded verbatim to engine-rest by {@link EngineRestGatewayProvider}, which enforces
 * a deny-by-default allow-list and derives the engine target + auth from the token.</p>
 */
@ApiResponses({
	@ApiResponse(responseCode = "403", description = "Requested engine-rest path is not allow-listed for embedded forms"),
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/engine-rest")
public class EngineRestGatewayService extends BaseService {

	@Autowired
	EngineRestGatewayProvider engineRestGatewayProvider;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Operation(
		summary = "Forward an allow-listed engine-rest GET for embedded forms",
		description = "Forwards to engine-rest only for allow-listed paths (e.g. group, user, history, form-variables). "
			+ "Engine target and authorization are derived from the token, never from the client.")
	@GetMapping("/**")
	public ResponseEntity<String> get(HttpServletRequest rq, @RequestParam Map<String, Object> params, CIBUser user) {
		return engineRestGatewayProvider.get(extractSubPath(rq), params, user);
	}

	@Operation(
		summary = "Forward an allow-listed engine-rest POST for embedded forms",
		description = "Forwards to engine-rest only for allow-listed writes (form submission). "
			+ "Engine target and authorization are derived from the token, never from the client.")
	@PostMapping("/**")
	public ResponseEntity<String> post(HttpServletRequest rq, @RequestBody(required = false) String body, CIBUser user) {
		return engineRestGatewayProvider.post(extractSubPath(rq), body, user);
	}

	/**
	 * Extracts the part of the request path matched by the {@code /**} wildcard, i.e. everything
	 * after {@code .../engine-rest}. Returns it with a leading slash (e.g. {@code /group/count}).
	 */
	private String extractSubPath(HttpServletRequest rq) {
		String path = (String) rq.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String bestMatch = (String) rq.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		String within = pathMatcher.extractPathWithinPattern(bestMatch, path);
		return within.isEmpty() ? "/" : "/" + within;
	}
}
