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

import java.nio.charset.StandardCharsets;

import org.cibseven.webapp.exception.EngineRestClientException;
import org.cibseven.webapp.exception.ErrorMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class ExceptionConverterTest {

	private final ExceptionConverter converter = new ExceptionConverter();

	/** CIB7-2158: a client error from engine-rest reaches the webclient's caller with the same status. */
	@Test
	void engineRestClientError_answersWithTheEngineStatus() {
		EngineRestClientException exception = new EngineRestClientException(HttpClientErrorException.create(
			HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
			"{\"type\":\"InvalidRequestException\",\"message\":\"bad date\"}".getBytes(StandardCharsets.UTF_8),
			StandardCharsets.UTF_8));

		ResponseEntity<ErrorMessage> response = converter.engineRestClientError(exception);

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody().getType()).isEqualTo("EngineRestClientException");
		assertThat(response.getBody().getParams()).containsExactly("bad date");
	}

	@Test
	void systemError_staysInternalServerError() {
		ErrorMessage message = converter.system(new IllegalStateException("boom"));

		assertThat(message.getType()).isEqualTo("IllegalStateException");
		assertThat(message.getParams()).containsExactly("boom");
	}
}
