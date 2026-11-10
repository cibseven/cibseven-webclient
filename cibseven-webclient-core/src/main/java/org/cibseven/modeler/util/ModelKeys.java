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
package org.cibseven.modeler.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Writes a new key into the content of a model. The modeler reads the key from the content when it
 * saves, and the engine deploys what the content says, so a key changed only on the stored model
 * would come back on the next save.
 */
public final class ModelKeys {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ModelKeys() {
	}

	/**
	 * A BPMN or DMN model with the key changed on the element that carries it (the process, a
	 * collaboration or the DMN definitions) and on the references to that element. Element ids are
	 * unique in the file, so no other element is touched. Unchanged when no element has the old key.
	 */
	public static byte[] rekeyDiagram(byte[] diagram, String oldKey, String newKey) {
		if (diagram == null || oldKey == null || newKey == null || oldKey.equals(newKey)) {
			return diagram;
		}
		String xml = new String(diagram, StandardCharsets.UTF_8);
		String value = Pattern.quote(escape(oldKey));
		if (!Pattern.compile("\\sid\\s*=\\s*([\"'])" + value + "\\1").matcher(xml).find()) {
			return diagram;
		}
		Matcher attributes = Pattern.compile("(\\s(?:id|bpmnElement|processRef)\\s*=\\s*)([\"'])" + value + "\\2")
			.matcher(xml);
		return attributes.replaceAll("$1$2" + Matcher.quoteReplacement(escape(newKey)) + "$2")
			.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * A form schema with the new form id. Unchanged when its id is another one, or it cannot be read:
	 * the copy is made all the same.
	 */
	public static byte[] rekeyForm(byte[] schema, String oldKey, String newKey) {
		if (schema == null || oldKey == null || newKey == null || oldKey.equals(newKey)) {
			return schema;
		}
		try {
			JsonNode root = MAPPER.readTree(schema);
			if (!(root instanceof ObjectNode form) || !oldKey.equals(form.path("id").asText(null))) {
				return schema;
			}
			form.put("id", newKey);
			return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(form);
		} catch (IOException e) {
			return schema;
		}
	}

	private static String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
			.replace("\"", "&quot;").replace("'", "&apos;");
	}
}
