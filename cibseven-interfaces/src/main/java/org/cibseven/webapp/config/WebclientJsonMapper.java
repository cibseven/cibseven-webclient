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


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.IllformedLocaleException;
import java.util.Locale;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.jdk.JDKFromStringDeserializer;
import tools.jackson.databind.introspect.AccessorNamingStrategy;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy;
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
				// getURL() is "url" and getvalue() a property, like Jackson 2 (see Jackson2AccessorNaming)
				.accessorNaming(new Jackson2AccessorNaming())
				// Jackson 3 writes a getter returning Void as property with value null, Jackson 2 did not
				.disable(MapperFeature.ALLOW_VOID_VALUED_PROPERTIES)
				// ToStringSerializer: "en_US" as in Jackson 2, also with type ids and "" as empty value (Locale.ROOT, NON_EMPTY)
				// Jackson2LocaleDeserializer reads "zh_TW_#Hant" back with its script, like Jackson 2
				.addModule(new SimpleModule("cibseven-webclient-jackson2-compatibility")
						.addSerializer(Locale.class, new ToStringSerializer(Locale.class))
						.addDeserializer(Locale.class, Jackson2LocaleDeserializer.forLocale())
						.addKeyDeserializer(Locale.class, new Jackson2LocaleKeyDeserializer()));
	}

	/**
	 * Property names of getters and setters like Jackson 2, which Jackson 3 no longer offers: leading upper-case letters
	 * are lower-cased ({@code getURL()} is {@code "url"}, not {@code "URL"}), and accessors whose name continues with a
	 * lower-case letter, a digit or {@code "_"} ({@code getvalue()}, {@code get1st()}, {@code get_links()}) are
	 * properties as well. Record components keep their names; additional getters of a record are named like Jackson 2
	 * too (Jackson 3 names {@code getURL()} "URL", the name of a component {@code URL}, and drops that component's value).
	 */
	public static class Jackson2AccessorNaming extends DefaultAccessorNamingStrategy.Provider {

		private static final long serialVersionUID = 1L;

		public Jackson2AccessorNaming() {
			super("set", JsonPOJOBuilder.DEFAULT_WITH_PREFIX, "get", "is", null);
		}

		@Override
		public AccessorNamingStrategy forPOJO(MapperConfig<?> config, AnnotatedClass targetClass) {
			return new LegacyNaming(config, targetClass, _setterPrefix, _getterPrefix, _isGetterPrefix);
		}

		@Override
		public AccessorNamingStrategy forBuilder(MapperConfig<?> config, AnnotatedClass builderClass,
				BeanDescription valueTypeDesc) {
			AnnotationIntrospector introspector = config.getAnnotationIntrospector();
			String mutatorPrefix = null;
			if (valueTypeDesc != null) {
				mutatorPrefix = introspector.findBuilderPrefix(config, valueTypeDesc.getClassInfo());
			}
			if (mutatorPrefix == null) {
				JsonPOJOBuilder.Value builderConfig = introspector.findPOJOBuilderConfig(config, builderClass);
				mutatorPrefix = (builderConfig != null) ? builderConfig.withPrefix : _withPrefix;
			}
			return new LegacyNaming(config, builderClass, mutatorPrefix, _getterPrefix, _isGetterPrefix);
		}

		@Override
		public AccessorNamingStrategy forRecord(MapperConfig<?> config, AnnotatedClass recordClass) {
			if (config.isEnabled(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY)) {
				return new DefaultAccessorNamingStrategy.RecordNamingStrict(config, recordClass); // components only: no names to build
			}
			return new LegacyRecordNaming(config, recordClass);
		}

		protected static class LegacyNaming extends DefaultAccessorNamingStrategy {

			protected LegacyNaming(MapperConfig<?> config, AnnotatedClass forClass, String mutatorPrefix,
					String getterPrefix, String isGetterPrefix) {
				super(config, forClass, mutatorPrefix, getterPrefix, isGetterPrefix, null);
			}

			@Override
			protected String stdManglePropertyName(String basename, int offset) {
				return legacyManglePropertyName(basename, offset);
			}
		}

		protected static class LegacyRecordNaming extends DefaultAccessorNamingStrategy.RecordNaming {

			protected LegacyRecordNaming(MapperConfig<?> config, AnnotatedClass forClass) {
				super(config, forClass);
			}

			@Override
			protected String stdManglePropertyName(String basename, int offset) {
				return legacyManglePropertyName(basename, offset);
			}
		}

		/** Jackson 2's {@code legacyManglePropertyName}: lower-cases all leading upper-case letters. */
		protected static String legacyManglePropertyName(String basename, int offset) {
			int end = basename.length();
			if (end == offset) {
				return null;
			}
			char c = basename.charAt(offset);
			char d = Character.toLowerCase(c);
			if (c == d) {
				return basename.substring(offset);
			}
			StringBuilder name = new StringBuilder(end - offset);
			name.append(d);
			for (int i = offset + 1; i < end; ++i) {
				c = basename.charAt(i);
				d = Character.toLowerCase(c);
				if (c == d) {
					name.append(basename, i, end);
					break;
				}
				name.append(d);
			}
			return name.toString();
		}
	}

	/**
	 * Reads a {@link Locale} like Jackson 2, also the form {@link Locale#toString()} writes for a script or extensions
	 * ({@code "zh_TW_#Hant"}, {@code "th_TH_#u-nu-thai"}): Jackson 3 would drop the script and the extensions.
	 * All other forms are read by Jackson 3's own deserializer.
	 */
	public static class Jackson2LocaleDeserializer extends JDKFromStringDeserializer {

		private static final long serialVersionUID = 1L;

		public Jackson2LocaleDeserializer() {
			super(Locale.class, STD_LOCALE);
		}

		/** This deserializer typed for {@link SimpleModule#addDeserializer}; Jackson's base class is typed {@code Object}. */
		@SuppressWarnings("unchecked")
		public static ValueDeserializer<Locale> forLocale() {
			return (ValueDeserializer<Locale>) (ValueDeserializer<?>) new Jackson2LocaleDeserializer();
		}

		@Override
		public Object _deserialize(String value, DeserializationContext ctxt)
				throws MalformedURLException, UnknownHostException {
			return value.contains(LOCALE_EXTENSION_MARKER) ? localeWithScriptOrExtensions(value) : super._deserialize(value, ctxt);
		}
	}

	/** Reads {@link Locale} map keys like {@link Jackson2LocaleDeserializer}. */
	public static class Jackson2LocaleKeyDeserializer extends KeyDeserializer {

		protected final Jackson2LocaleDeserializer deserializer = new Jackson2LocaleDeserializer();

		@Override
		public Object deserializeKey(String key, DeserializationContext ctxt) {
			try {
				return deserializer._deserialize(key, ctxt);
			} catch (IOException e) { // not thrown for a Locale
				return ctxt.handleWeirdKey(Locale.class, key, "not a valid Locale: %s", e.getMessage());
			}
		}
	}

	protected static final String LOCALE_EXTENSION_MARKER = "_#";

	/** Jackson 2's parsing of {@code language_country_variant_#script_extensions} (FromStringDeserializer, 2.21). */
	@SuppressWarnings("deprecation") // Locale constructors, as in Jackson 2
	protected static Locale localeWithScriptOrExtensions(String value) {
		int ix = firstHyphenOrUnderscore(value);
		String first = value.substring(0, ix);
		value = value.substring(ix + 1);
		ix = firstHyphenOrUnderscore(value);
		if (ix < 0) {
			return new Locale(first, value);
		}
		String second = value.substring(0, ix);
		int extensionMarker = value.indexOf(LOCALE_EXTENSION_MARKER);
		if (extensionMarker < 0) {
			return new Locale(first, second, value.substring(ix + 1));
		}
		String third = "";
		try {
			if (extensionMarker > 0 && extensionMarker > ix) {
				third = value.substring(ix + 1, extensionMarker);
			}
			value = value.substring(extensionMarker + 2);
			int underscore = value.indexOf('_');
			if (underscore < 0) {
				int hyphen = value.indexOf('-');
				if (hyphen < 0) {
					return new Locale.Builder().setLanguage(first).setRegion(second).setVariant(third).setScript(value).build();
				}
				return new Locale.Builder().setLanguage(first).setRegion(second).setVariant(third)
						.setExtension(value.charAt(0), value.substring(hyphen + 1)).build();
			}
			int length = value.length();
			Locale.Builder builder = new Locale.Builder().setLanguage(first).setRegion(second).setVariant(third)
					.setScript(value.substring(0, underscore));
			if ((underscore + 1) < length) {
				builder = builder.setExtension(value.charAt(underscore + 1), value.substring(Math.min(length, underscore + 3)));
			}
			return builder.build();
		} catch (IllformedLocaleException e) {
			return new Locale(first, second, third);
		}
	}

	protected static int firstHyphenOrUnderscore(String value) {
		for (int i = 0; i < value.length(); ++i) {
			char c = value.charAt(i);
			if (c == '_' || c == '-') {
				return i;
			}
		}
		return -1;
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
