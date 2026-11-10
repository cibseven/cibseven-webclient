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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writes a new key into the content of a model. The modeler saves the key it reads from the
 * content, and the engine deploys what the content says, so a key changed only on the stored model
 * would come back on the next save. The content is edited as text: everything but the key stays as
 * it was.
 */
public final class ModelKeys {

	private static final String BPMN_MODEL = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	private static final String BPMN_DI = "http://www.omg.org/spec/BPMN/20100524/DI";
	private static final Pattern ENCODING = Pattern.compile("^\\s*<\\?xml[^>]*\\sencoding\\s*=\\s*[\"']([A-Za-z0-9._-]+)[\"']");
	// The rest of a start tag up to an attribute, values included: a value may hold a '>'
	private static final String IN_TAG = "(?:[^>\"']|\"[^\"]*\"|'[^']*')*?";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ModelKeys() {
	}

	/**
	 * A BPMN or DMN model with the new key on the element the modeler reads it from: in BPMN the
	 * collaboration or process the diagram shows, in DMN the definitions. References to that element
	 * follow; nothing else changes, whatever key the model was stored under. Unchanged when the file
	 * cannot be read or has no such element.
	 */
	public static byte[] rekeyDiagram(byte[] diagram, String newKey) {
		if (diagram == null || newKey == null) {
			return diagram;
		}
		Element keyElement = keyElement(diagram);
		String oldKey = keyElement == null ? "" : keyElement.getAttribute("id");
		if (oldKey.isEmpty() || oldKey.equals(newKey)) {
			return diagram;
		}
		Charset charset = charsetOf(diagram);
		String xml = new String(diagram, charset);
		String value = Pattern.quote(escape(oldKey));
		String replacement = "$1$2" + Matcher.quoteReplacement(escape(newKey)) + "$2";

		Matcher id = Pattern.compile("(<(?:[\\w.-]+:)?" + Pattern.quote(keyElement.getLocalName()) + "(?=[\\s/>])"
			+ IN_TAG + "\\sid\\s*=\\s*)([\"'])" + value + "\\2").matcher(xml);
		if (!id.find()) {
			return diagram;
		}
		xml = id.replaceFirst(replacement);
		if (BPMN_MODEL.equals(keyElement.getNamespaceURI())) {
			xml = Pattern.compile("(\\s(?:bpmnElement|processRef)\\s*=\\s*)([\"'])" + value + "\\2").matcher(xml)
				.replaceAll(replacement);
		}
		return xml.getBytes(charset);
	}

	/**
	 * A form schema with the new form id. Only the id's value is replaced, so numbers and layout stay
	 * as they were. Unchanged when the schema has no id or cannot be read.
	 */
	public static byte[] rekeyForm(byte[] schema, String newKey) {
		if (schema == null || newKey == null) {
			return schema;
		}
		String json = new String(schema, StandardCharsets.UTF_8);
		try (JsonParser parser = MAPPER.createParser(json)) {
			if (parser.nextToken() != JsonToken.START_OBJECT) {
				return schema;
			}
			while (parser.nextToken() == JsonToken.FIELD_NAME) {
				String field = parser.currentName();
				JsonToken value = parser.nextToken();
				if ("id".equals(field) && value == JsonToken.VALUE_STRING) {
					if (newKey.equals(parser.getText())) {
						return schema;
					}
					int start = (int) parser.currentTokenLocation().getCharOffset();
					int end = endOfString(json, start);
					return (json.substring(0, start) + MAPPER.writeValueAsString(newKey) + json.substring(end))
						.getBytes(StandardCharsets.UTF_8);
				}
				parser.skipChildren();
			}
		} catch (IOException e) {
			// Copied as it is
		}
		return schema;
	}

	/** In BPMN the element of the first diagram's plane, as the modeler shows it; in DMN the definitions. */
	private static Element keyElement(byte[] diagram) {
		Document document;
		try {
			document = secureFactory().newDocumentBuilder().parse(new ByteArrayInputStream(diagram));
		} catch (Exception e) {
			return null;
		}
		Element root = document.getDocumentElement();
		if (!"definitions".equals(root.getLocalName())) {
			return null;
		}
		if (!BPMN_MODEL.equals(root.getNamespaceURI())) {
			return root;
		}
		NodeList planes = document.getElementsByTagNameNS(BPMN_DI, "BPMNPlane");
		String shown = planes.getLength() > 0 ? ((Element) planes.item(0)).getAttribute("bpmnElement") : "";
		// Without a diagram the modeler takes the collaboration, else the process
		for (String name : new String[] { "collaboration", "process" }) {
			NodeList elements = document.getElementsByTagNameNS(BPMN_MODEL, name);
			for (int i = 0; i < elements.getLength(); i++) {
				Element element = (Element) elements.item(i);
				if (shown.isEmpty() || shown.equals(element.getAttribute("id"))) {
					return element;
				}
			}
		}
		return null;
	}

	/** Hardened as for uploads: BPMN and DMN need no DOCTYPE, so none is read. */
	private static DocumentBuilderFactory secureFactory() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		return factory;
	}

	/** The encoding the file declares, so text in another one survives the edit. */
	private static Charset charsetOf(byte[] xml) {
		if (xml.length > 1 && ((xml[0] == (byte) 0xFE && xml[1] == (byte) 0xFF) || (xml[0] == (byte) 0xFF && xml[1] == (byte) 0xFE))) {
			return StandardCharsets.UTF_16;
		}
		Matcher declared = ENCODING.matcher(new String(xml, 0, Math.min(xml.length, 200), StandardCharsets.ISO_8859_1));
		if (declared.find()) {
			try {
				return Charset.forName(declared.group(1));
			} catch (IllegalArgumentException e) {
				// UTF-8, the default of XML
			}
		}
		return StandardCharsets.UTF_8;
	}

	/** The index after the JSON string starting at the quote at {@code start}. */
	private static int endOfString(String json, int start) {
		for (int i = start + 1; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '\\') {
				i++;
			} else if (c == '"') {
				return i + 1;
			}
		}
		return json.length();
	}

	private static String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
			.replace("\"", "&quot;").replace("'", "&apos;");
	}
}
