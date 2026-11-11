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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.rest.CustomRestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Guards the JSON mapper of the webclient's REST services and of its engine REST client: the date
 * serialization (CIB7-1789), the parser limit, and that it stays the webclient's own (CIB7-1535).
 */
class SevenWebclientContextJacksonTest {

	private static final int PARSER_MAX_SIZE = 20000000;

	@Test
	void writesDatesAsIso8601() {
		// A quoted, parseable ISO-8601 instant; a bare number would mean epoch millis.
		// Asserted semantically so a future setTimeZone(...) cannot break this test.
		String json = jsonMapper(PARSER_MAX_SIZE).writeValueAsString(new Date(0));
		assertThat(json).startsWith("\"").endsWith("\"");
		assertThat(OffsetDateTime.parse(json.replace("\"", "")).toInstant()).isEqualTo(Instant.EPOCH);
	}

	@Test
	void writesDurationsAsIso8601() {
		assertThat(jsonMapper(PARSER_MAX_SIZE).writeValueAsString(Duration.ofHours(1))).isEqualTo("\"PT1H\"");
	}

	/**
	 * {@code cibseven.webclient.custom.spring.jackson.serialization.write-dates-as-timestamps} was the
	 * interim opt-out of the ISO-8601 dates; it is gone with Jackson 3, and a leftover setting must
	 * neither fail the start nor bring the timestamps back.
	 */
	@Test
	void ignoresTheRemovedWriteDatesAsTimestampsKey() {
		SevenWebclientProperties properties = new Binder(new MapConfigurationPropertySource(Map.of(
				"cibseven.webclient.custom.spring.jackson.serialization.write-dates-as-timestamps", "true")))
				.bindOrCreate("cibseven.webclient", SevenWebclientProperties.class);

		assertThat(new SevenWebclientContext(properties).jsonMapper().writeValueAsString(new Date(0)))
				.startsWith("\"1970-01-01T00:00:00.000");
	}

	@Test
	void appliesTheConfiguredParserStringLimit() {
		assertThat(jsonMapper(PARSER_MAX_SIZE).tokenStreamFactory().streamReadConstraints().getMaxStringLength())
				.isEqualTo(PARSER_MAX_SIZE);

		JsonMapper limited = jsonMapper(10);
		assertThat(limited.readValue("\"0123456789\"", String.class)).isEqualTo("0123456789");
		assertThatThrownBy(() -> limited.readValue("\"0123456789A\"", String.class))
				.isInstanceOf(StreamConstraintsException.class);
	}

	@Test
	void bindsTheParserLimitKey() {
		SevenWebclientProperties properties = new Binder(new MapConfigurationPropertySource(Map.of(
				"cibseven.webclient.custom.spring.jackson.parser.max-size", "42")))
				.bindOrCreate("cibseven.webclient", SevenWebclientProperties.class);

		assertThat(new SevenWebclientContext(properties).jsonMapper().tokenStreamFactory().streamReadConstraints()
				.getMaxStringLength()).isEqualTo(42);
	}

	@Test
	void wiresTheMapperIntoTheJsonConverterOfTheServices() {
		// The date defect of CIB7-1789 was only user-visible through this wiring, so guard it explicitly.
		SevenWebclientContext context = newContext(PARSER_MAX_SIZE);
		HttpMessageConverters.ServerBuilder builder = HttpMessageConverters.forServer();
		context.configureMessageConverters(builder);
		JacksonJsonHttpMessageConverter json = StreamSupport.stream(builder.build().spliterator(), false)
				.filter(JacksonJsonHttpMessageConverter.class::isInstance)
				.map(JacksonJsonHttpMessageConverter.class::cast)
				.findFirst().orElseThrow();

		assertThat(json.getMapper()).isSameAs(context.jsonMapper());
	}

	@Test
	void wiresTheMapperIntoTheEngineRestClient() {
		SevenWebclientContext context = newContext(PARSER_MAX_SIZE);
		CustomRestTemplate template = context.customRestTemplate();

		assertThat(template.getMessageConverters())
				.filteredOn(JacksonJsonHttpMessageConverter.class::isInstance)
				.singleElement()
				.extracting(converter -> ((JacksonJsonHttpMessageConverter) converter).getMapper())
				.isSameAs(context.jsonMapper());
	}

	/**
	 * A {@code JsonMapper} (or Jackson 2 {@code ObjectMapper}) bean would make Spring Boot's own mapper
	 * back off in an application that embeds the webclient, and with it the application's
	 * {@code spring.jackson.*} settings.
	 */
	@Test
	void doesNotDeclareAMapperBean() {
		Method[] beans = Arrays.stream(SevenWebclientContext.class.getDeclaredMethods())
				.filter(method -> method.isAnnotationPresent(Bean.class))
				.toArray(Method[]::new);

		assertThat(beans).isNotEmpty();
		assertThat(beans).noneMatch(method -> method.getReturnType().getName().startsWith("tools.jackson.")
				|| method.getReturnType().getName().startsWith("com.fasterxml.jackson."));
	}

	private JsonMapper jsonMapper(int parserMaxSize) {
		return newContext(parserMaxSize).jsonMapper();
	}

	private SevenWebclientContext newContext(int parserMaxSize) {
		SevenWebclientProperties properties = new SevenWebclientProperties();
		properties.getCustom().getSpring().getJackson().getParser().setMaxSize(parserMaxSize);
		return new SevenWebclientContext(properties);
	}
}
