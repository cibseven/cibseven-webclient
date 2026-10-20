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

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.cibseven.webapp.config.SevenWebclientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Guards the date serialization of the ObjectMapper bean (CIB7-1789).
 */
class SevenWebclientContextJacksonTest {

	private static final int PARSER_MAX_SIZE = 20000000;

	@Test
	void writesDatesAsIso8601ByDefault() throws JsonProcessingException {
		// A quoted, parseable ISO-8601 instant; a bare number would mean epoch millis.
		// Asserted semantically so a future setTimeZone(...) cannot break this test.
		String json = objectMapper(false).writeValueAsString(new Date(0));
		assertThat(json).startsWith("\"").endsWith("\"");
		assertThat(OffsetDateTime.parse(json.replace("\"", "")).toInstant()).isEqualTo(Instant.EPOCH);
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

	@Test
	void bindsTheDocumentedPropertyKey() throws JsonProcessingException {
		// Fails if the key is misspelled or no longer bound into SevenWebclientProperties, which
		// the direct tests above cannot detect.
		SevenWebclientProperties properties = new Binder(new MapConfigurationPropertySource(Map.of(
				"cibseven.webclient.custom.spring.jackson.serialization.write-dates-as-timestamps", "true")))
				.bindOrCreate("cibseven.webclient", SevenWebclientProperties.class);
		assertThat(new SevenWebclientContext(properties).objectMapper().writeValueAsString(new Date(0))).isEqualTo("0");
	}

	@Test
	void writesDurationsAsIso8601ByDefault() throws JsonProcessingException {
		// Spring Boot disables WRITE_DURATIONS_AS_TIMESTAMPS alongside the date feature.
		assertThat(objectMapper(false).writeValueAsString(Duration.ofHours(1))).isEqualTo("\"PT1H\"");
	}

	@Test
	void wiresTheConfiguredMapperIntoTheJsonConverter() throws JsonProcessingException {
		// The defect was only user-visible through this wiring, so guard it explicitly.
		SevenWebclientContext context = newContext(false);
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		context.configureMessageConverters(converters);
		MappingJackson2HttpMessageConverter json = converters.stream()
				.filter(MappingJackson2HttpMessageConverter.class::isInstance)
				.map(MappingJackson2HttpMessageConverter.class::cast)
				.findFirst().orElseThrow();
		assertThat(json.getObjectMapper().writeValueAsString(new Date(0)))
				.startsWith("\"1970-01-01T00:00:00.000");
	}

	private ObjectMapper objectMapper(boolean writeDatesAsTimestamps) {
		return newContext(writeDatesAsTimestamps).objectMapper();
	}

	private SevenWebclientContext newContext(boolean writeDatesAsTimestamps) {
		SevenWebclientProperties properties = new SevenWebclientProperties();
		SevenWebclientProperties.Custom.Spring.Jackson jackson = properties.getCustom().getSpring().getJackson();
		jackson.getParser().setMaxSize(PARSER_MAX_SIZE);
		jackson.getSerialization().setWriteDatesAsTimestamps(writeDatesAsTimestamps);
		return new SevenWebclientContext(properties);
	}
}
