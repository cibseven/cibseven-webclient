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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.cibseven.webapp.exception.ErrorMessage;
import org.cibseven.webapp.exception.InvalidRequestException;
import org.cibseven.webapp.exception.PermissionDeniedException;
import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

/**
 * A permission or a request the engine refuses is the caller's problem: answered with 403 and 400, as
 * engine-rest answers it, although both are SystemExceptions, which are answered with 500.
 */
class ExceptionConverterClientErrorTest {

	private final ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionConverter.class);

	@Test
	void aRefusedPermissionIsAnsweredWith403() {
		assertThat(statusFor(new PermissionDeniedException("denied", new RuntimeException()))).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void aRejectedRequestIsAnsweredWith400() {
		assertThat(statusFor(new InvalidRequestException("bad value", new RuntimeException()))).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void anyOtherSystemExceptionIsStillAnsweredWith500() {
		assertThat(statusFor(new SystemException("boom"))).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void theEngineReasonIsPassedOnWithoutPrefix() {
		ErrorMessage denied = new ExceptionConverter().permissionDenied(
			new PermissionDeniedException("demo may not update task-1", new RuntimeException()));
		ErrorMessage invalid = new ExceptionConverter().invalidRequest(
			new InvalidRequestException("Cannot convert variable amount: abc", new RuntimeException()));

		assertThat(denied.getType()).isEqualTo("PermissionDeniedException");
		assertThat(denied.getParams()).containsExactly("demo may not update task-1");
		assertThat(invalid.getType()).isEqualTo("InvalidRequestException");
		assertThat(invalid.getParams()).containsExactly("Cannot convert variable amount: abc");
	}

	private HttpStatus statusFor(Exception exception) {
		Method handler = resolver.resolveMethod(exception);
		return AnnotatedElementUtils.findMergedAnnotation(handler, ResponseStatus.class).code();
	}
}
