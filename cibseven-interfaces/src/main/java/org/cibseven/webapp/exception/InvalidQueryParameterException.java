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

/**
 * A query parameter the engine rejects: a value its converter cannot read (a date, an enum), or a
 * value missing or unknown where the query needs one (a sort order, a period unit, a report type).
 * <p>
 * Answered as 400 by the ExceptionConverter, with the engine's message as the only entry of
 * {@link #getData()}, as engine-rest answers the same GET request (CIB7-2801, CIB7-2724).
 * </p>
 */
public class InvalidQueryParameterException extends ApplicationException {

	private static final long serialVersionUID = 1L;

	private final String message;

	public InvalidQueryParameterException(String message, Throwable cause) {
		// data is sent to the client as the error's params: the message only, the cause stays out of it
		super(message);
		this.message = message;
		if (cause != null) initCause(cause);
	}

	@Override
	public String getMessage() {
		return message;
	}
}
