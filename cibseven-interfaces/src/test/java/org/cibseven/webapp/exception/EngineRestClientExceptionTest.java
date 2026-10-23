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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class EngineRestClientExceptionTest {

	@Test
	void keepsTheEngineStatusMessageAndCause() {
		HttpClientErrorException cause = clientError(HttpStatus.BAD_REQUEST,
			"{\"type\":\"InvalidRequestException\",\"message\":\"Cannot set removal time\"}");

		EngineRestClientException exception = new EngineRestClientException(cause);

		assertThat(exception.getStatus()).isEqualTo(400);
		assertThat(exception.getMessage()).isEqualTo("Cannot set removal time");
		assertThat(exception.getCause()).isSameAs(cause);
	}

	@Test
	void fallsBackToTheRawBodyWhenItIsNotJson() {
		EngineRestClientException exception = new EngineRestClientException(
			clientError(HttpStatus.NOT_FOUND, "Not Found"));

		assertThat(exception.getStatus()).isEqualTo(404);
		assertThat(exception.getMessage()).isEqualTo("Not Found");
	}

	@Test
	void fallsBackToTheRawBodyWhenJsonHasNoTextualMessage() {
		assertThat(EngineRestClientException.engineMessage("{\"type\":\"X\"}")).isEqualTo("{\"type\":\"X\"}");
		assertThat(EngineRestClientException.engineMessage("{\"message\":42}")).isEqualTo("{\"message\":42}");
		assertThat(EngineRestClientException.engineMessage("")).isEmpty();
	}

	private static HttpClientErrorException clientError(HttpStatus status, String body) {
		return HttpClientErrorException.create(status, status.getReasonPhrase(),
			HttpHeaders.EMPTY, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
	}
}
