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
package org.cibseven.webapp.rest.model;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineConfiguration {
	/**
	 * The history level assumed when the engine does not report one, i.e. with engine-rest versions
	 * prior to 2.2.0 (no configuration endpoint) and the legacy {@code cibseven.webclient.historyLevel}
	 * property not set.
	 * <p>
	 * {@code full} matches every shipped distribution (Tomcat {@code bpm-platform.xml}, WildFly
	 * {@code standalone.xml}, Run) and the Spring Boot starter default ({@code CamundaBpmProperties}).
	 * The bare engine default {@code audit} ({@code ProcessEngineConfiguration.HISTORY_DEFAULT})
	 * only applies to custom engine configurations that omit the history property.
	 */
	public static final String DEFAULT_HISTORY_LEVEL = "full";

	private String engineName;
	private String historyLevel = DEFAULT_HISTORY_LEVEL;
	private boolean authorizationEnabled = true;
	private boolean enablePasswordPolicy;

	/**
	 * The engine-wide default history time to live, in days, as reported by the engine's
	 * {@code GET /engine/{name}/configuration} endpoint (or read directly from the engine
	 * configuration by the direct provider).
	 * <ul>
	 *   <li>{@code n >= 0}: the engine applies this TTL at deployment time to definitions whose
	 *   model does not set its own one ({@code 0}: history is removed by the next cleanup run).</li>
	 *   <li>{@code -1}: the engine has no default configured.</li>
	 *   <li>{@code null}: <b>unknown</b>, the engine does not report it (engines before 3.0.0, which
	 *   omit the property). It must not be mistaken for {@code -1}.</li>
	 * </ul>
	 * Boxed on purpose: a primitive with a fallback default would make an engine that does not
	 * report the value indistinguishable from one that really has that value.
	 * <p>
	 * Read with {@link HistoryTimeToLiveDeserializer}, which also accepts the raw string some
	 * engine builds report (e.g. {@code "P180D"}), so an unexpected value never makes the whole
	 * engine configuration unreadable.
	 *
	 * @since 3.0.0 (engine version)
	 */
	@JsonDeserialize(using = HistoryTimeToLiveDeserializer.class)
	private Integer historyTimeToLive;

	/**
	 * Whether the engine rejects the deployment of a model resource (BPMN, DMN, CMMN) for which no
	 * history time to live can be resolved, and rejects setting a definition's TTL to {@code null}.
	 * <ul>
	 *   <li>{@code true} / {@code false}: the value reported by the engine.</li>
	 *   <li>{@code null}: <b>unknown</b>, the engine does not report it (engines before 3.0.0, which
	 *   omit the property). It must not be mistaken for {@code true} (the engine default), since
	 *   the engine may have enforcement switched off, nor for {@code false}.</li>
	 * </ul>
	 * Boxed on purpose, see {@link #historyTimeToLive}. Note that Lombok generates
	 * {@code getEnforceHistoryTimeToLive()} (not {@code isEnforceHistoryTimeToLive()}) for a
	 * {@link Boolean}.
	 *
	 * @since 3.0.0 (engine version)
	 */
	private Boolean enforceHistoryTimeToLive;

	public String getHistoryLevel() {
		return historyLevel != null ? historyLevel.toLowerCase() : DEFAULT_HISTORY_LEVEL;
	}

	/**
	 * Lenient reader for {@link #historyTimeToLive}.
	 * <p>
	 * Engine builds differ in how they report the value:
	 * <ul>
	 *   <li>engine-rest with the typed DTO (3.0.0): a JSON number in days, {@code -1} if no
	 *   default is configured, e.g. {@code 180};</li>
	 *   <li>engine-rest builds between the first exposure of the field and the typed DTO: the raw
	 *   configured string, in days or as an ISO-8601 period in days, e.g. {@code "180"} or
	 *   {@code "P180D"}, or {@code null} if no default is configured;</li>
	 *   <li>engines before 3.0.0: no property at all.</li>
	 * </ul>
	 * A plain {@link Integer} field cannot read {@code "P180D"}: Jackson fails, the whole
	 * {@code GET /configuration} response becomes unreadable, {@code /info/properties} answers
	 * with HTTP 500 and the frontend, which waits for it on startup, stays blank.
	 * <p>
	 * This reader therefore accepts a JSON integer as-is, and a string holding a whole number of
	 * days in either form ({@code "180"}, {@code "P180D"}, surrounding whitespace allowed),
	 * parsed the same way as the engine's {@code ParseUtil.parseHistoryTimeToLive}. Anything
	 * else (an empty or unparsable string, a fraction, a number out of {@code int} range, a
	 * boolean, an object or an array) is read as {@code null}, i.e. "unknown", and logged, so the
	 * rest of the engine configuration (history level, authorization, ...) stays usable.
	 * A JSON {@code null} or a missing property stays {@code null} as usual.
	 */
	public static class HistoryTimeToLiveDeserializer extends JsonDeserializer<Integer> {

		private static final Logger LOG = LoggerFactory.getLogger(HistoryTimeToLiveDeserializer.class);

		// a whole number of days, plain or as an ISO-8601 period in days, like the engine accepts
		private static final Pattern DAYS = Pattern.compile("^(?:P(\\d+)D|(\\d+))$");

		@Override
		public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
			JsonToken token = parser.currentToken();
			if (token == JsonToken.VALUE_NUMBER_INT && parser.getNumberType() == JsonParser.NumberType.INT) {
				return parser.getIntValue();
			}
			if (token == JsonToken.VALUE_STRING) {
				return parseDays(parser.getText());
			}
			if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
				// consume the whole structure, so the remaining properties are still read
				parser.skipChildren();
			}
			LOG.warn("Ignoring unsupported historyTimeToLive value {} reported by the engine; treating it as unknown", token);
			return null;
		}

		private static Integer parseDays(String text) {
			Matcher matcher = DAYS.matcher(text.trim());
			if (matcher.matches()) {
				// group 1: the ISO-8601 form "P<n>D", group 2: the plain form "<n>"
				String days = matcher.group(matcher.group(1) != null ? 1 : 2);
				try {
					return Integer.valueOf(days);
				} catch (NumberFormatException e) {
					// more digits than fit into an int - handled as unparsable below
				}
			}
			LOG.warn("Ignoring unparsable historyTimeToLive value '{}' reported by the engine; treating it as unknown", text);
			return null;
		}
	}
}
