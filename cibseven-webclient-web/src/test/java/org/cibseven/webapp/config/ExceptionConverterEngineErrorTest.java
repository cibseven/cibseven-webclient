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

import java.net.ConnectException;

import org.cibseven.webapp.exception.EngineNotReachableException;
import org.cibseven.webapp.exception.ErrorMessage;
import org.cibseven.webapp.exception.UnknownEngineException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

/**
 * Engine errors are answered with a fixed message instead of the details of the failed request.
 */
class ExceptionConverterEngineErrorTest {

	@Test
	void answersAnUnreachableEngineWithAFixedMessage() {
		ResourceAccessException error = new ResourceAccessException(
			"I/O error on GET request for 'http://engine.example:8080/engine-rest/task': Connection refused",
			new ConnectException("Connection refused"));

		ErrorMessage message = new ExceptionConverter().engineNotReachable(new EngineNotReachableException(error));

		assertThat(message.getType()).isEqualTo("EngineNotReachableException");
		assertThat(message.getParams()).containsExactly("The process engine could not be reached");
	}

	@Test
	void answersAnUnknownEngineWithAFixedMessage() {
		ErrorMessage message = new ExceptionConverter().application(new UnknownEngineException());

		assertThat(message.getType()).isEqualTo("UnknownEngineException");
		assertThat(message.getParams()).containsExactly("The requested process engine is not configured");
	}
}
