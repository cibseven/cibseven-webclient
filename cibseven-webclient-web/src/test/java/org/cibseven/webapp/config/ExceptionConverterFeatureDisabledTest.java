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

import org.cibseven.webapp.exception.ErrorMessage;
import org.cibseven.webapp.exception.FeatureDisabledException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A switched-off feature is a 404 naming it, not the 500 of the tables that are not there. */
class ExceptionConverterFeatureDisabledTest {

	@Test
	void answersASwitchedOffFeatureWithNotFound() throws Exception {
		ErrorMessage message = new ExceptionConverter().featureDisabled(new FeatureDisabledException("modeler"));

		assertThat(message.getType()).isEqualTo("FeatureDisabledException");
		assertThat(message.getParams()).containsExactly("modeler");
		assertThat(ExceptionConverter.class.getMethod("featureDisabled", FeatureDisabledException.class)
			.getAnnotation(ResponseStatus.class).value()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
