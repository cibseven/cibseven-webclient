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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.cibseven.webapp.rest.model.Authorization;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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

	/** A date with {@code @JsonFormat(shape = STRING)} and no pattern. */
	public static class FormattedDate {
		@JsonFormat(shape = JsonFormat.Shape.STRING)
		public Date date = new Date(0);
	}

	@Test
	void bothMappersWriteLocaleAndStringShapedDatesLikeJackson2() {
		// Jackson 3 alone writes "en-US" and "1970-01-01T00:00:00.000Z"
		for (JsonMapper mapper : new JsonMapper[] { WebclientJsonMapper.plain(),
				WebclientJsonMapper.create(new SevenWebclientProperties()) }) {
			assertThat(mapper.writeValueAsString(Map.of("locale", Locale.US))).isEqualTo("{\"locale\":\"en_US\"}");
			assertThat(mapper.readValue("\"en_US\"", Locale.class)).isEqualTo(Locale.US);
			assertThat(mapper.readValue("\"en-US\"", Locale.class)).isEqualTo(Locale.US);
			assertThat(mapper.writeValueAsString(new FormattedDate())).isEqualTo("{\"date\":\"1970-01-01T00:00:00.000+00:00\"}");
		}
		// other dates: a timestamp with the plain mapper, as with a plain Jackson 2 ObjectMapper
		assertThat(WebclientJsonMapper.plain().writeValueAsString(Map.of("date", new Date(0)))).isEqualTo("{\"date\":0}");
	}

	/** A Locale in a property with type information. */
	public static class PolymorphicLocale {
		@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
		public Object value = Locale.US;
	}

	/** Locales that are left out when empty. */
	public static class NonEmptyLocales {
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		public Locale root = Locale.ROOT;
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		public Locale locale = Locale.US;
	}

	@Test
	void bothMappersWriteLocaleWithTypeInformationAndAsEmptyValueLikeJackson2() {
		for (JsonMapper mapper : new JsonMapper[] { WebclientJsonMapper.plain(),
				WebclientJsonMapper.create(new SevenWebclientProperties()) }) {
			assertThat(mapper.writeValueAsString(new PolymorphicLocale())).isEqualTo("{\"value\":[\"java.util.Locale\",\"en_US\"]}");
			// Locale.ROOT is written as "" and counts as empty, so NON_EMPTY leaves it out
			assertThat(mapper.writeValueAsString(new NonEmptyLocales())).isEqualTo("{\"locale\":\"en_US\"}");
		}
	}

	@Test
	void plainLimitsAStringTo20MillionCharactersLikeJackson2() {
		// Jackson 3 alone accepts up to 100 million characters
		String json = "\"" + "a".repeat(20_000_001) + "\"";

		assertThatThrownBy(() -> WebclientJsonMapper.plain().readValue(json, String.class))
				.isInstanceOf(StreamConstraintsException.class)
				.hasMessageContaining("20000000");
	}

	/** Accessors whose property names Jackson 3 builds differently or not at all. */
	public static class Accessors {
		private String url = "u";
		private String urlValue = "uv";
		private int x = 1;
		private int xCoordinate = 2;
		private boolean ok = true;
		private String iban = "i";
		private String abc = "abc";
		private String links = "l";
		private String value = "v";
		private String first = "f";
		private String a1 = "a1";

		public String getURL() { return url; }
		public void setURL(String url) { this.url = url; }
		public String getURLValue() { return urlValue; }
		public void setURLValue(String urlValue) { this.urlValue = urlValue; }
		public int getX() { return x; }
		public void setX(int x) { this.x = x; }
		public int getXCoordinate() { return xCoordinate; }
		public void setXCoordinate(int xCoordinate) { this.xCoordinate = xCoordinate; }
		public boolean isOK() { return ok; }
		public void setOK(boolean ok) { this.ok = ok; }
		public String getIBAN() { return iban; }
		public void setIBAN(String iban) { this.iban = iban; }
		public String getaBc() { return abc; }
		public void setaBc(String abc) { this.abc = abc; }
		public String get_links() { return links; }
		public void set_links(String links) { this.links = links; }
		public String getvalue() { return value; }
		public void setvalue(String value) { this.value = value; }
		public String get1st() { return first; }
		public void set1st(String first) { this.first = first; }
		public String getA1() { return a1; }
		public void setA1(String a1) { this.a1 = a1; }
	}

	/** Every property has a field of the same name (Jackson 2 naming): the field order is the property order. */
	public static class FieldOrder {
		private String url = "u";
		private boolean ok = true;
		private String iban = "i";
		private int x = 1;

		public String getURL() { return url; }
		public boolean isOK() { return ok; }
		public String getIBAN() { return iban; }
		public int getX() { return x; }
	}

	/** A record with additional getters; Jackson 3 names getURL() like the component URL. */
	public record RecordAccessors(String URL, String getValue) {
		public String getURL() { return "extra"; }
		public boolean isOK() { return true; }
	}

	/** What Jackson 2 writes for {@code new RecordAccessors("u", "v")}. */
	protected static final String RECORD_ACCESSORS_JSON = "{\"URL\":\"u\",\"getValue\":\"v\",\"url\":\"extra\",\"ok\":true}";

	public static class VoidGetter {
		public int getValue() { return 1; }
		public Void getNothing() { return null; }
	}

	public static class LocaleValue {
		public Locale locale;
		public Map<Locale, Integer> keys = new LinkedHashMap<>();
	}

	/** What Jackson 2 writes for {@link Accessors}; the order of the properties without a field is not fixed. */
	protected static final String ACCESSORS_JSON = "{\"url\":\"u\",\"x\":1,\"ok\":true,\"iban\":\"i\",\"value\":\"v\",\"a1\":\"a1\","
			+ "\"urlvalue\":\"uv\",\"xcoordinate\":2,\"aBc\":\"abc\",\"_links\":\"l\",\"1st\":\"f\"}";

	/** Locales whose {@link Locale#toString()} has a script or extensions ("zh_TW_#Hant"). */
	protected static final List<String> LOCALES_WITH_SCRIPT_OR_EXTENSIONS = List.of("zh-Hant-TW", "sr-Latn-RS",
			"th-TH-u-nu-thai", "en-Latn-US-u-ca-japanese", "ja-JP-u-ca-japanese-x-lvariant-JP");

	@Test
	void bothMappersNamePropertiesLikeJackson2() {
		for (JsonMapper mapper : new JsonMapper[] { WebclientJsonMapper.plain(),
				WebclientJsonMapper.create(new SevenWebclientProperties()) }) {
			// Jackson 3 alone writes {"x":1,"a1":"a1","URL":"u","URLValue":"uv","XCoordinate":2,"OK":true,"IBAN":"i"}
			assertThat(mapper.readTree(mapper.writeValueAsString(new Accessors()))).isEqualTo(mapper.readTree(ACCESSORS_JSON));
			// Jackson 3 alone writes {"x":1,"URL":"u","OK":true,"IBAN":"i"}
			assertThat(mapper.writeValueAsString(new FieldOrder())).isEqualTo("{\"url\":\"u\",\"ok\":true,\"iban\":\"i\",\"x\":1}");
			Accessors accessors = mapper.readValue(ACCESSORS_JSON.replace("\"u\"", "\"U\"").replace("\"l\"", "\"L\""), Accessors.class);
			assertThat(accessors.getURL()).isEqualTo("U");
			assertThat(accessors.get_links()).isEqualTo("L");
		}
	}

	@Test
	void bothMappersNameAdditionalRecordGettersLikeJackson2() {
		for (JsonMapper mapper : new JsonMapper[] { WebclientJsonMapper.plain(),
				WebclientJsonMapper.create(new SevenWebclientProperties()) }) {
			// Jackson 3 alone writes {"URL":"extra","getValue":"v","OK":true}: the component's value is lost
			assertThat(mapper.readTree(mapper.writeValueAsString(new RecordAccessors("u", "v")))).isEqualTo(mapper.readTree(RECORD_ACCESSORS_JSON));
			assertThat(mapper.readValue("{\"URL\":\"u\",\"getValue\":\"v\"}", RecordAccessors.class)).isEqualTo(new RecordAccessors("u", "v"));
		}
	}

	@Test
	void bothMappersSkipVoidGettersLikeJackson2() {
		for (JsonMapper mapper : new JsonMapper[] { WebclientJsonMapper.plain(),
				WebclientJsonMapper.create(new SevenWebclientProperties()) }) {
			// Jackson 3 alone writes {"value":1,"nothing":null}
			assertThat(mapper.writeValueAsString(new VoidGetter())).isEqualTo("{\"value\":1}");
		}
	}

	@Test
	void bothMappersReadLocalesWithScriptOrExtensionsLikeJackson2() {
		for (JsonMapper mapper : new JsonMapper[] { WebclientJsonMapper.plain(),
				WebclientJsonMapper.create(new SevenWebclientProperties()) }) {
			for (String tag : LOCALES_WITH_SCRIPT_OR_EXTENSIONS) {
				LocaleValue value = new LocaleValue();
				value.locale = Locale.forLanguageTag(tag);
				value.keys.put(value.locale, 1);

				// Jackson 3 alone reads "zh_TW_#Hant" as zh-TW
				LocaleValue readBack = mapper.readValue(mapper.writeValueAsString(value), LocaleValue.class);

				assertThat(readBack.locale).as(tag).isEqualTo(value.locale);
				assertThat(readBack.keys).as(tag).containsOnlyKeys(value.locale);
			}
		}
	}

	@Test
	void bothMappersWriteMonthAsEnumLikeJackson2() {
		for (JsonMapper mapper : new JsonMapper[] { WebclientJsonMapper.plain(),
				WebclientJsonMapper.create(new SevenWebclientProperties()) }) {
			// Jackson 3 alone writes the month number
			assertThat(mapper.writeValueAsString(Map.of("m", java.time.Month.MARCH))).isEqualTo("{\"m\":\"MARCH\"}");
			assertThat(mapper.writeValueAsString(Map.of(java.time.Month.JANUARY, 1))).isEqualTo("{\"JANUARY\":1}");
		}
	}

	@Test
	void engineMapperWritesYearAndSqlDateAsTextLikeJackson2() {
		// the engine/MVC mapper writes dates as text; Jackson 2 with JavaTimeModule wrote "2020" and toString()
		java.util.TimeZone previous = java.util.TimeZone.getDefault();
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"));
		try {
			JsonMapper mapper = WebclientJsonMapper.create(new SevenWebclientProperties());
			assertThat(mapper.writeValueAsString(Map.of("y", java.time.Year.of(2020)))).isEqualTo("{\"y\":\"2020\"}");
			assertThat(mapper.writeValueAsString(Map.of("d", new java.sql.Date(0)))).isEqualTo("{\"d\":\"1969-12-31\"}");
		} finally {
			java.util.TimeZone.setDefault(previous);
		}
	}
}
