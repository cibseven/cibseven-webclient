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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

import org.cibseven.webapp.rest.model.Authorization;
import org.junit.jupiter.api.Test;

import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The webclient's mappers keep Jackson 2's defaults where Jackson 3 changed them (CIB7-1535).
 */
class WebclientJsonMapperTest {

	enum Color {
		RED;

		@Override
		public String toString() {
			return "red";
		}
	}

	/** Declared out of alphabetical order on purpose. */
	record Item(String zeta, int alpha, Color color) {
	}

	@Test
	void plainKeepsJackson2Defaults() {
		JsonMapper plain = WebclientJsonMapper.plain();

		// declaration order and enum names, not alphabetical order and toString()
		assertThat(plain.writeValueAsString(new Item("z", 1, Color.RED))).isEqualTo("{\"zeta\":\"z\",\"alpha\":1,\"color\":\"RED\"}");
		// null for a primitive is its default
		assertThat(plain.readValue("{\"type\":null,\"resourceType\":null}", Authorization.class).getResourceType()).isZero();
		// unknown properties fail, as with a plain Jackson 2 ObjectMapper
		assertThatThrownBy(() -> plain.readValue("{\"zeta\":\"z\",\"unknown\":1}", Item.class))
				.isInstanceOf(UnrecognizedPropertyException.class);
		// dates as timestamps, as with a plain Jackson 2 ObjectMapper
		assertThat(plain.writeValueAsString(new Date(0))).isEqualTo("0");
		assertThat(WebclientJsonMapper.plain()).isSameAs(plain);
	}

	@Test
	void theServicesMapperIgnoresUnknownPropertiesAndWritesIsoDates() {
		JsonMapper mapper = WebclientJsonMapper.create(new SevenWebclientProperties());

		assertThat(mapper.readValue("{\"zeta\":\"z\",\"alpha\":null,\"unknown\":{\"a\":[1]}}", Item.class))
				.isEqualTo(new Item("z", 0, null));
		assertThat(mapper.writeValueAsString(Map.of("date", new Date(0)))).isEqualTo("{\"date\":\"1970-01-01T00:00:00.000+00:00\"}");
		assertThat(mapper.writeValueAsString(LocalDate.of(2026, 11, 7))).isEqualTo("\"2026-11-07\"");
		assertThat(mapper.writeValueAsString(Duration.ofMinutes(90))).isEqualTo("\"PT1H30M\"");
		assertThat(mapper.writeValueAsString(new Item("z", 1, Color.RED))).isEqualTo("{\"zeta\":\"z\",\"alpha\":1,\"color\":\"RED\"}");
	}

	@Test
	void theServicesMapperLimitsTheLengthOfAString() {
		SevenWebclientProperties properties = new SevenWebclientProperties();
		properties.getCustom().getSpring().getJackson().getParser().setMaxSize(5);
		JsonMapper mapper = WebclientJsonMapper.create(properties);

		assertThat(mapper.readValue("\"12345\"", String.class)).isEqualTo("12345");
		assertThatThrownBy(() -> mapper.readValue("\"123456\"", String.class)).isInstanceOf(StreamConstraintsException.class);
	}

	/**
	 * Jackson 3 allows only 500 levels of nesting; variables nested deeper worked on Jackson 2, which
	 * allowed 1000, for reading and for writing.
	 */
	@Test
	void bothMappersKeepTheNestingDepthOfJackson2() {
		String nested1000 = "[".repeat(1000) + "]".repeat(1000);
		String nested1001 = "[".repeat(1001) + "]".repeat(1001);

		for (JsonMapper mapper : new JsonMapper[] { WebclientJsonMapper.plain(),
				WebclientJsonMapper.create(new SevenWebclientProperties()) }) {
			Object read = mapper.readValue(nested1000, Object.class);
			assertThat(mapper.writeValueAsString(read)).isEqualTo(nested1000);
			// not toString(): Jackson 3 writes a tree there with its own default limit of 500
			assertThat(mapper.writeValueAsString(mapper.readTree(nested1000))).isEqualTo(nested1000);
			assertThatThrownBy(() -> mapper.readValue(nested1001, Object.class))
					.isInstanceOf(StreamConstraintsException.class)
					.hasMessageContaining("1000");
		}
	}

	/**
	 * Same JSON either way; Jackson 2's escapes keep the bytes the same as before: characters outside
	 * the BMP as a surrogate pair, "/" not escaped.
	 */
	@Test
	void bothMappersEscapeLikeJackson2() {
		for (JsonMapper mapper : new JsonMapper[] { WebclientJsonMapper.plain(),
				WebclientJsonMapper.create(new SevenWebclientProperties()) }) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			mapper.writeValue(out, "a😀ä https://example.com/a/b");

			assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("\"a\\uD83D\\uDE00ä https://example.com/a/b\"");
			assertThat(mapper.writeValueAsString("</script>")).isEqualTo("\"</script>\"");
		}
	}
}
