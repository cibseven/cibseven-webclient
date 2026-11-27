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
package org.cibseven.webapp.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.exception.ApplicationException;
import org.cibseven.webapp.exception.EngineNotReachableException;
import org.cibseven.webapp.exception.ErrorMessage;
import org.cibseven.webapp.exception.FeatureDisabledException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.PermissionDeniedException;
import org.cibseven.webapp.rest.BaseService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice (assignableTypes = {BaseService.class}) @ResponseBody
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ExceptionConverter {
	
	@ExceptionHandler @ResponseStatus(HttpStatus.UNAUTHORIZED) 
	public ErrorMessage authentication(AuthenticationException x) {
		log.debug("Authentication error occured " + Arrays.toString(x.getData()), x);
		return new ErrorMessage(x.getClass().getSimpleName(), x.getData());
	}

	@ExceptionHandler @ResponseStatus(HttpStatus.FORBIDDEN)
	public ErrorMessage authentication(AccessDeniedException x) {
		log.error("Access denied", x);
		return new ErrorMessage(x.getClass().getSimpleName(), new Object[] { x.getMessage() });
	}

	// The engine refused a permission: the caller's problem, not a server failure
	@ExceptionHandler @ResponseStatus(HttpStatus.FORBIDDEN)
	public ErrorMessage permissionDenied(PermissionDeniedException x) {
		log.info("The engine denied a permission: {}", x.getMessage());
		return new ErrorMessage(x.getClass().getSimpleName(), new Object[] { x.getMessage() });
	}

	@ExceptionHandler @ResponseStatus(HttpStatus.BAD_REQUEST) 
	public ErrorMessage application(ApplicationException x) {
		log.info("Application error occured " + x.getData(), x);
		return new ErrorMessage(x.getClass().getSimpleName(), params(x));
	}
	
	@ExceptionHandler @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorMessage system(RuntimeException x) {
		log.error("System error occured", x);
		return new ErrorMessage(x.getClass().getSimpleName(), new Object[] { x.getMessage() });
	}

	@ExceptionHandler @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorMessage engineNotReachable(EngineNotReachableException x) {
		log.error("Process engine not reachable", x);
		return new ErrorMessage(x.getClass().getSimpleName(), new Object[] { x.getMessage() });
	}

	@ExceptionHandler @ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorMessage featureDisabled(FeatureDisabledException x) {
		log.debug("Feature switched off " + x.getMessage());
		return new ErrorMessage(x.getClass().getSimpleName(), params(x));
	}

	@ExceptionHandler @ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorMessage application(NoObjectFoundException x) {
		log.debug("Application error occured " + x.getData(), x);
		return new ErrorMessage("NoObjectFoundException");
	}

	/**
	 * The params of the answer to an {@link ApplicationException}: its message, then the values of its
	 * data in their order. The frontends read the values by position, so an exception that carries
	 * several values keeps them in an ordered map.
	 */
	static Object[] params(ApplicationException x) {
		List<Object> params = new ArrayList<>();
		params.add(x.getMessage());
		params.addAll(x.getData().values());
		return params.toArray();
	}

}
	