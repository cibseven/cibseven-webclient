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

import java.util.Collections;
import java.util.Map;

import lombok.Getter;

/**
 * An error the caller can act on. The message is for the log; {@code data} names the values the
 * error is about. The REST layer sends the values of {@code data}, in its iteration order, as the
 * {@code params} of the error response, or the message alone when {@code data} is empty. A subclass
 * that carries several values therefore passes an ordered map, such as a {@link java.util.LinkedHashMap},
 * and one whose value may be null a map that allows it, not {@link Map#of}.
 */
abstract public class ApplicationException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_MESSAGE = "An application error occurred";

	/** Never null; empty when the error carries no values. */
	@Getter protected Map<String, Object> data;

	public ApplicationException(String message, Map<String, Object> data) {
		super(message == null ? DEFAULT_MESSAGE : message);
		this.data = data == null ? Collections.emptyMap() : data;
	}

	public ApplicationException(String message, Throwable cause, Map<String, Object> data) {
		super(message == null ? DEFAULT_MESSAGE : message, cause);
		this.data = data == null ? Collections.emptyMap() : data;
	}

	public ApplicationException(String message, Throwable cause) {
		this(message, cause, null);
	}

}
