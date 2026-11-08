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


import java.util.Locale;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.util.StdDateFormat;

/**
 * The Jackson 3 mappers of the webclient backend. Both start from Jackson 2's defaults, so the JSON
 * the frontend, engine-rest and other clients see stays what it was on Jackson 2 (CIB7-1535):
 * Jackson 3 changed several defaults, such as sorting properties alphabetically, failing on
 * {@code null} for primitives, writing enums with {@code toString()} and allowing only half the
 * nesting depth.
 * <p>
 * Neither mapper is a bean on purpose. A {@code JsonMapper} bean would make Spring Boot's own mapper
 * back off in an application that embeds the webclient, and with it the application's
 * {@code spring.jackson.*} settings; the webclient's REST services, in turn, must not depend on them.
 */
public final class WebclientJsonMapper {

	/**
	 * Jackson 2's maximum nesting depth of a JSON document, for reading and writing; Jackson 3 lowered
	 * it to 500, which would reject deeply nested variables that worked before.
	 */
	public static final int MAX_NESTING_DEPTH = 1000;

	/** Jackson 2's maximum length of a JSON string value, when none is configured (Jackson 3: 100,000,000). */
	public static final int DEFAULT_MAX_STRING_LENGTH = 20_000_000;

	private static final JsonMapper PLAIN = builderWithJackson2Defaults(DEFAULT_MAX_STRING_LENGTH).build();

	private WebclientJsonMapper() {
	}

	/**
	 * A mapper with Jackson 2's defaults and nothing else, for the places that created a plain
	 * {@code new ObjectMapper()} on Jackson 2, such as the {@code json()} request bodies of the
	 * models. It is shared, so it must not be reconfigured; {@link JsonMapper#rebuild()} gives a
	 * builder for a variant.
	 */
	public static JsonMapper plain() {
		return PLAIN;
	}

	/**
	 * The mapper of the webclient's REST services and of its engine REST client: Jackson 2's
	 * defaults, unknown properties ignored, dates and durations as ISO-8601 strings, and the
	 * configured maximum length of a JSON string value
	 * ({@code cibseven.webclient.custom.spring.jackson.parser.max-size}).
	 */
	public static JsonMapper create(SevenWebclientProperties properties) {
		return builder(properties).build();
	}

	/** The builder of {@link #create(SevenWebclientProperties)}, for a variant of that mapper. */
	public static JsonMapper.Builder builder(SevenWebclientProperties properties) {
		return builderWithJackson2Defaults(properties.getCustom().getSpring().getJackson().getParser().getMaxSize())
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
				.disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
	}

	/**
	 * A builder with Jackson 2's defaults and limits, and the given maximum length of a JSON string
	 * value, like a Jackson 2 {@code ObjectMapper} with that limit.
	 */
	public static JsonMapper.Builder builderWithJackson2Defaults(int maxStringLength) {
		return withJackson2Output(JsonMapper.builder(jackson2Factory(maxStringLength)).configureForJackson2());
	}

	/**
	 * What Jackson 3 writes differently from Jackson 2 out of the box: a {@link Locale} as language tag
	 * ("en-US" instead of "en_US") and a zero offset of a date written as text as "Z" (instead of "+00:00").
	 */
	private static JsonMapper.Builder withJackson2Output(JsonMapper.Builder builder) {
		return builder
				.defaultDateFormat(new StdDateFormat().withColonInTimeZone(true).withZeroOffsetAsZ(false))
				// defaultDateFormat(...) turns WRITE_DATES_AS_TIMESTAMPS off; Jackson 2's default is on
				.enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
				// ToStringSerializer: "en_US" as in Jackson 2, also with type ids and "" as empty value (Locale.ROOT, NON_EMPTY)
				.addModule(new SimpleModule("cibseven-webclient-jackson2-compatibility")
						.addSerializer(Locale.class, new ToStringSerializer(Locale.class)));
	}

	private static JsonFactory jackson2Factory(int maxStringLength) {
		StreamReadConstraints.Builder read = StreamReadConstraints.defaults().rebuild()
				.maxNestingDepth(MAX_NESTING_DEPTH)
				.maxStringLength(maxStringLength);
		// Jackson 2's defaults of the streaming layer too: "/" not escaped, characters outside the BMP
		// (emoji) escaped as a surrogate pair, Jackson 2's number parsing; the same JSON either way,
		// kept so the bytes stay the same too
		return JsonFactory.builderWithJackson2Defaults()
				.streamReadConstraints(read.build())
				.streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build())
				.build();
	}
}
