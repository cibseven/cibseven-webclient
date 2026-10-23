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
package org.cibseven.webapp.exception;

import org.springframework.web.client.HttpStatusCodeException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

/**
 * Engine-rest answered with a 4xx that no more specific exception maps. It keeps the engine's
 * status so the webclient answers its own caller with the same client error instead of a 500.
 */
public class EngineRestClientException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Getter private final int status;

	public EngineRestClientException(HttpStatusCodeException cause) {
		super(engineMessage(cause.getResponseBodyAsString()), cause);
		this.status = cause.getStatusCode().value();
	}

	/**
	 * Engine-rest reports errors as {@code {"type":"...","message":"..."}}; returns that message,
	 * or the whole body when it is not in that shape.
	 */
	static String engineMessage(String body) {
		try {
			JsonNode message = MAPPER.readTree(body).get("message");
			if (message != null && message.isTextual()) return message.asText();
		} catch (Exception e) {
			// not JSON - fall through to the raw body
		}
		return body;
	}
}
