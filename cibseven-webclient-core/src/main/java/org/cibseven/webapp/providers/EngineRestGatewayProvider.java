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
package org.cibseven.webapp.providers;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.providers.utils.URLUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Allow-listed gateway that forwards the engine-rest calls made by embedded forms
 * (bpm-sdk / {@code CamSDK}) to the engine.
 *
 * <p>The engine target and the auth header are always derived from the authenticated
 * {@link CIBUser} (the token), never from the client. Only the {@code method + path}
 * combinations embedded forms need are permitted; everything else is denied by default.</p>
 */
@Slf4j
@Component
public class EngineRestGatewayProvider extends SevenProviderBase {

	/** GET calls embedded forms make: form loading, form variables, and identity/history lookups. */
	private static final List<Pattern> ALLOWED_GET = List.of(
			// form loading
			Pattern.compile("^/process-definition/.+/startForm$"),
			Pattern.compile("^/process-definition/.+/rendered-form$"),
			Pattern.compile("^/process-definition/.+/deployed-start-form$"),
			Pattern.compile("^/task/[^/]+/form$"),
			Pattern.compile("^/task/[^/]+/rendered-form$"),
			Pattern.compile("^/task/[^/]+/deployed-form$"),
			// form variables
			Pattern.compile("^/task/[^/]+/form-variables$"),
			Pattern.compile("^/process-definition/.+/form-variables$"),
			// identity / history
			Pattern.compile("^/group(/.*)?$"),
			Pattern.compile("^/user(/.*)?$"),
			Pattern.compile("^/history/.+$"));

	/**
	 * POST calls embedded forms make. CIB seven exposes a POST query-by-body variant of the group
	 * resource (postQueryGroups {@code POST /group}), which the sdk uses to populate the group
	 * dropdown, so that is allowed alongside form submission. Writes other than {@code submit-form}
	 * (create/delete/complete/…) stay denied. Extend here if a form needs another POST query variant.
	 */
	private static final List<Pattern> ALLOWED_POST = List.of(
			// group query (postQueryGroups + count-by-POST)
			Pattern.compile("^/group(/count)?$"),
			// form submission (the only allowed write)
			Pattern.compile("^/task/[^/]+/submit-form$"),
			Pattern.compile("^/process-definition/.+/submit-form$"));

	/**
	 * Forward an allow-listed GET to engine-rest.
	 *
	 * @param subPath the path after {@code /services/v1/engine-rest} (leading slash included), e.g. {@code /group}
	 * @param params query parameters to forward
	 * @param user the authenticated user; determines both the engine target and the auth header
	 * @return the raw engine-rest response (body + status + content-type preserved)
	 */
	public ResponseEntity<String> get(String subPath, Map<String, Object> params, CIBUser user) {
		assertAllowed(ALLOWED_GET, "GET", subPath);
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + subPath, params);
		return doGetWithHeader(url, String.class, user, true, MediaType.ALL);
	}

	/**
	 * Forward an allow-listed POST to engine-rest.
	 *
	 * @param subPath the path after {@code /services/v1/engine-rest} (leading slash included), e.g. {@code /task/42/submit-form}
	 * @param body the JSON request body to forward
	 * @param user the authenticated user; determines both the engine target and the auth header
	 * @return the raw engine-rest response (body + status + content-type preserved)
	 */
	public ResponseEntity<String> post(String subPath, String body, CIBUser user) {
		assertAllowed(ALLOWED_POST, "POST", subPath);
		String url = getEngineRestUrl(user) + subPath;
		return doPost(url, body, String.class, user);
	}

	private void assertAllowed(List<Pattern> allowList, String method, String subPath) {
		if (subPath == null || allowList.stream().noneMatch(p -> p.matcher(subPath).matches())) {
			log.warn("engine-rest gateway denied {} {}", method, subPath);
			throw new AccessDeniedException(method + " " + subPath);
		}
	}
}
