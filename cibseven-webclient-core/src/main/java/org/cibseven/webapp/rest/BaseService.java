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

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.exception.ApplicationException;
import org.cibseven.webapp.exception.ErrorMessage;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.providers.BpmProvider;

public class BaseService {

	@Autowired
	protected BpmProvider bpmProvider;

	@SuppressWarnings("rawtypes")
	@Autowired
	protected BaseUserProvider baseUserProvider;

	protected CIBUser checkAuthorization(HttpServletRequest rq, boolean basicAuthAllowed) {
		return baseUserProvider.checkAuthorization(rq, basicAuthAllowed);
	}

	/**
	 * An error answer in the shape the frontend reads (see {@code utils/error.js}): an {@link ErrorMessage}
	 * typed after the cause, whose only param is the given text. Pass a text you chose, never
	 * {@code cause.getMessage()}: engine exceptions carry technical details that are not for the user.
	 * <p>
	 * The cause goes to the server log instead, so production issues stay traceable: a warning with its
	 * message every time, and its stack trace too unless it is an expected situation (the object is gone,
	 * the user may not do this), whose stack trace is logged at debug level only.
	 */
	@SuppressWarnings("unchecked")
	protected <T> ResponseEntity<T> errorResponse(Exception cause, String message, HttpStatus status) {
		Logger log = LoggerFactory.getLogger(getClass());
		if (cause instanceof NoObjectFoundException || cause instanceof AccessDeniedException) {
			log.warn("{} ({}: {})", message, cause.getClass().getSimpleName(), causeText(cause));
			log.debug("Stack trace of the cause", cause);
		} else {
			log.warn("{} ({}: {})", message, cause.getClass().getSimpleName(), causeText(cause), cause);
		}
		ErrorMessage body = new ErrorMessage(cause.getClass().getSimpleName(), new Object[] { message });
		return (ResponseEntity<T>) ResponseEntity.status(status).body(body);
	}

	/**
	 * The cause's own text: its message, followed by the values an {@link ApplicationException} names in
	 * {@code data}.
	 */
	private static String causeText(Exception cause) {
		if (cause instanceof ApplicationException ae && !ae.getData().isEmpty()) {
			return cause.getMessage() + " " + ae.getData();
		}
		return cause.getMessage();
	}
}
