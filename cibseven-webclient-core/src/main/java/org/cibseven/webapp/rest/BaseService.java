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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.ErrorMessage;
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
	 */
	@SuppressWarnings("unchecked")
	protected <T> ResponseEntity<T> errorResponse(Exception cause, String message, HttpStatus status) {
		ErrorMessage body = new ErrorMessage(cause.getClass().getSimpleName(), new Object[] { message });
		return (ResponseEntity<T>) ResponseEntity.status(status).body(body);
	}
}
