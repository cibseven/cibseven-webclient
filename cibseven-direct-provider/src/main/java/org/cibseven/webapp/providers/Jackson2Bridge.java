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
package org.cibseven.webapp.providers;

import java.io.IOException;

import org.cibseven.webapp.config.WebclientJsonMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * The border between the direct provider, which is still on Jackson 2, and the rest of the
 * webclient, which is on Jackson 3 (CIB7-1535).
 * <p>
 * The direct provider converts between the webclient's models and the DTOs of
 * {@code cibseven-engine-rest-core} with the Jackson 2 mapper of the engine's REST API. Some
 * models carry Jackson 3 trees, such as the criteria of a filter; Jackson 2 does not know them and
 * would take them for beans. {@link #register(ObjectMapper)} teaches a Jackson 2 mapper to read and
 * write them as JSON, and {@link #toJackson3(com.fasterxml.jackson.databind.JsonNode)} hands a tree
 * the direct provider built to the rest of the webclient.
 * <p>
 * Goes away once {@code cibseven-engine-rest-core} and with it the direct provider are on
 * Jackson 3.
 */
public final class Jackson2Bridge {

	private static final ObjectMapper JACKSON2 = new ObjectMapper();

	private Jackson2Bridge() {
	}

	/** Lets {@code mapper} read and write the Jackson 3 trees of the webclient's models. */
	public static ObjectMapper register(ObjectMapper mapper) {
		SimpleModule module = new SimpleModule("cibseven-webclient-jackson3-trees");
		module.addSerializer(tools.jackson.databind.JsonNode.class, new Jackson3TreeSerializer());
		module.addDeserializer(tools.jackson.databind.JsonNode.class,
				new Jackson3TreeDeserializer<>(tools.jackson.databind.JsonNode.class));
		module.addDeserializer(tools.jackson.databind.node.ObjectNode.class,
				new Jackson3TreeDeserializer<>(tools.jackson.databind.node.ObjectNode.class));
		module.addDeserializer(tools.jackson.databind.node.ArrayNode.class,
				new Jackson3TreeDeserializer<>(tools.jackson.databind.node.ArrayNode.class));
		return mapper.registerModule(module);
	}

	/** The same tree as Jackson 3 tree; {@code null} stays {@code null}. */
	public static tools.jackson.databind.JsonNode toJackson3(com.fasterxml.jackson.databind.JsonNode tree) {
		try {
			return tree == null ? null : WebclientJsonMapper.plain().readTree(JACKSON2.writeValueAsString(tree));
		} catch (IOException e) {
			// A Jackson 2 tree always writes valid JSON
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The same tree as Jackson 2 tree; {@code null} stays {@code null}. Written with the webclient's
	 * mapper rather than {@code toString()}, which Jackson 3 limits to 500 levels of nesting.
	 */
	public static com.fasterxml.jackson.databind.JsonNode toJackson2(tools.jackson.databind.JsonNode tree) {
		try {
			return tree == null ? null : JACKSON2.readTree(WebclientJsonMapper.plain().writeValueAsString(tree));
		} catch (IOException e) {
			// A Jackson 3 tree always writes valid JSON
			throw new IllegalStateException(e);
		}
	}

	private static final class Jackson3TreeSerializer extends StdSerializer<tools.jackson.databind.JsonNode> {

		private static final long serialVersionUID = 1L;

		Jackson3TreeSerializer() {
			super(tools.jackson.databind.JsonNode.class);
		}

		@Override
		public void serialize(tools.jackson.databind.JsonNode value, JsonGenerator generator, SerializerProvider provider)
				throws IOException {
			provider.defaultSerializeValue(toJackson2(value), generator);
		}
	}

	private static final class Jackson3TreeDeserializer<T extends tools.jackson.databind.JsonNode> extends StdDeserializer<T> {

		private static final long serialVersionUID = 1L;

		private final Class<T> type;

		Jackson3TreeDeserializer(Class<T> type) {
			super(type);
			this.type = type;
		}

		@Override
		public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
			tools.jackson.databind.JsonNode tree = toJackson3(context.readTree(parser));
			if (!type.isInstance(tree)) {
				return context.reportInputMismatch(this, "Expected a JSON %s, got %s",
						type == tools.jackson.databind.node.ArrayNode.class ? "array" : "object", tree.getNodeType());
			}
			return type.cast(tree);
		}
	}
}
