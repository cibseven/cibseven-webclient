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

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Relays a response the provider got from the engine without the headers that
 * describe the engine connection (Transfer-Encoding, Connection, Content-Length, ...).
 * The servlet container frames our response itself, so forwarding them sends
 * e.g. Transfer-Encoding twice, which strict proxies reject with 502.
 */
public final class EngineResponses {

	static final List<String> FORWARDED_HEADERS = List.of(
			HttpHeaders.CONTENT_TYPE,
			HttpHeaders.CONTENT_DISPOSITION,
			HttpHeaders.CACHE_CONTROL);

	private EngineResponses() {
	}

	public static HttpHeaders forwardableHeaders(HttpHeaders source) {
		HttpHeaders headers = new HttpHeaders();
		for (String name : FORWARDED_HEADERS) {
			List<String> values = source.get(name);
			if (values != null) {
				headers.addAll(name, values);
			}
		}
		return headers;
	}

	public static <T> ResponseEntity<T> relay(ResponseEntity<T> response) {
		return ResponseEntity.status(response.getStatusCode())
				.headers(forwardableHeaders(response.getHeaders()))
				.body(response.getBody());
	}

	/** For deployed forms: 200 with the form, or 204 when the engine has none. */
	public static ResponseEntity<byte[]> relayOrNoContent(ResponseEntity<byte[]> response) {
		byte[] body = response.getBody();
		if (body == null || body.length == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok()
				.headers(forwardableHeaders(response.getHeaders()))
				.body(body);
	}
}
