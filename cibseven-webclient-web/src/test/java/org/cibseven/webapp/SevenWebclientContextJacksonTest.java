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
package org.cibseven.webapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Guards the date serialization of the ObjectMapper bean (CIB7-1789).
 */
class SevenWebclientContextJacksonTest {

	private static final int PARSER_MAX_SIZE = 20000000;

	@Test
	void writesDatesAsIso8601ByDefault() throws JsonProcessingException {
		// A quoted value means ISO-8601; a bare number would mean epoch millis.
		assertThat(objectMapper(false).writeValueAsString(new Date(0)))
				.startsWith("\"")
				.contains("T")
				.endsWith("\"");
	}

	@Test
	void writesDatesAsEpochMillisWhenConfigured() throws JsonProcessingException {
		// Opt-out for consumers that adapted to the previous output.
		assertThat(objectMapper(true).writeValueAsString(new Date(0))).isEqualTo("0");
	}

	@Test
	void appliesConfiguredParserStringLimit() {
		// The parser limit must keep working next to the date setting.
		assertThat(objectMapper(false).getFactory().streamReadConstraints().getMaxStringLength())
				.isEqualTo(PARSER_MAX_SIZE);
	}

	private ObjectMapper objectMapper(boolean writeDatesAsTimestamps) {
		SevenWebclientContext context = new SevenWebclientContext();
		context.jacksonParserMaxSize = PARSER_MAX_SIZE;
		context.writeDatesAsTimestamps = writeDatesAsTimestamps;
		return context.objectMapper();
	}
}
