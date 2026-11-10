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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ModelKeysTest {

	private static final String BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" id="Definitions_1">
		  <bpmn:process id="invoice" name="invoice" isExecutable="true">
		    <bpmn:startEvent id="invoice_start" name="invoice" />
		    <bpmn:callActivity id="Call_1" calledElement="invoice" />
		  </bpmn:process>
		  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
		    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="invoice" />
		  </bpmndi:BPMNDiagram>
		</bpmn:definitions>
		""";

	private static String rekey(String xml, String oldKey, String newKey) {
		return new String(ModelKeys.rekeyDiagram(xml.getBytes(StandardCharsets.UTF_8), oldKey, newKey), StandardCharsets.UTF_8);
	}

	@Test
	void givesTheProcessAndItsDiagramTheNewKey() {
		String copy = rekey(BPMN, "invoice", "invoice-copy");

		assertThat(copy).contains("<bpmn:process id=\"invoice-copy\"", "bpmnElement=\"invoice-copy\"");
	}

	/** Names, other ids that only start with the key, and a call of the original keep what they say. */
	@Test
	void leavesEverythingElseAsItIs() {
		String copy = rekey(BPMN, "invoice", "invoice-copy");

		assertThat(copy).contains("name=\"invoice\"", "id=\"invoice_start\"", "calledElement=\"invoice\"");
		assertThat(copy.replace("invoice-copy", "invoice")).isEqualTo(BPMN);
	}

	@Test
	void givesTheCollaborationThatCarriesTheKeyTheNewOne() {
		String xml = """
			<bpmn:definitions id="d">
			  <bpmn:collaboration id="Collaboration_1"><bpmn:participant id="P" processRef="Process_1" /></bpmn:collaboration>
			  <bpmn:process id='Process_1' />
			  <bpmndi:BPMNPlane id="Plane" bpmnElement="Collaboration_1" />
			</bpmn:definitions>
			""";

		String copy = rekey(xml, "Collaboration_1", "Lanes");

		assertThat(copy).contains("<bpmn:collaboration id=\"Lanes\"", "bpmnElement=\"Lanes\"", "processRef=\"Process_1\"");
	}

	@Test
	void followsAProcessReferenceAndSingleQuotes() {
		String xml = "<d><collaboration id=\"C\"><participant id=\"P\" processRef='invoice' /></collaboration><process id='invoice' /></d>";

		assertThat(rekey(xml, "invoice", "billing"))
			.isEqualTo("<d><collaboration id=\"C\"><participant id=\"P\" processRef='billing' /></collaboration><process id='billing' /></d>");
	}

	@Test
	void givesTheDmnDefinitionsTheNewKey() {
		String xml = "<definitions xmlns=\"https://www.omg.org/spec/DMN/20191111/MODEL/\" id=\"rules\" name=\"Rules\"><decision id=\"dec\" /></definitions>";

		assertThat(rekey(xml, "rules", "rules-copy")).contains("id=\"rules-copy\"", "id=\"dec\"");
	}

	@Test
	void writesAKeyWithMarkupCharactersAsXml() {
		assertThat(rekey("<p id=\"a\" />", "a", "x&\"y")).isEqualTo("<p id=\"x&amp;&quot;y\" />");
	}

	/** The stored key may differ from the content already; then nothing in it is the key. */
	@Test
	void leavesAModelWithoutTheKeyAlone() {
		byte[] xml = "<p id=\"other\" bpmnElement=\"invoice\" />".getBytes(StandardCharsets.UTF_8);

		assertThat(ModelKeys.rekeyDiagram(xml, "invoice", "billing")).isSameAs(xml);
	}

	@Test
	void changesNothingWithoutANewKey() {
		byte[] xml = BPMN.getBytes(StandardCharsets.UTF_8);

		assertThat(ModelKeys.rekeyDiagram(xml, "invoice", "invoice")).isSameAs(xml);
		assertThat(ModelKeys.rekeyDiagram(xml, null, "x")).isSameAs(xml);
		assertThat(ModelKeys.rekeyDiagram(xml, "invoice", null)).isSameAs(xml);
		assertThat(ModelKeys.rekeyDiagram(null, "invoice", "x")).isNull();
	}

	@Test
	void givesTheFormTheNewIdAndKeepsItsComponents() throws Exception {
		byte[] schema = """
			{ "type": "default", "id": "invoice-form", "components": [ { "id": "invoice-form", "key": "amount" } ] }
			""".getBytes(StandardCharsets.UTF_8);

		JsonNode copy = new ObjectMapper().readTree(ModelKeys.rekeyForm(schema, "invoice-form", "invoice-form-copy"));

		assertThat(copy.get("id").asText()).isEqualTo("invoice-form-copy");
		assertThat(copy.get("type").asText()).isEqualTo("default");
		assertThat(copy.at("/components/0/id").asText()).isEqualTo("invoice-form");
		assertThat(copy.at("/components/0/key").asText()).isEqualTo("amount");
	}

	@Test
	void leavesAFormWithAnotherIdOrNoneAlone() {
		byte[] other = "{\"id\":\"other\"}".getBytes(StandardCharsets.UTF_8);
		byte[] none = "{}".getBytes(StandardCharsets.UTF_8);
		byte[] list = "[]".getBytes(StandardCharsets.UTF_8);

		assertThat(ModelKeys.rekeyForm(other, "invoice-form", "x")).isSameAs(other);
		assertThat(ModelKeys.rekeyForm(none, "invoice-form", "x")).isSameAs(none);
		assertThat(ModelKeys.rekeyForm(list, "invoice-form", "x")).isSameAs(list);
	}

	@Test
	void copiesAFormItCannotReadAsItIs() {
		byte[] broken = "{ not json".getBytes(StandardCharsets.UTF_8);

		assertThat(ModelKeys.rekeyForm(broken, "invoice-form", "x")).isSameAs(broken);
	}

	@Test
	void changesNoFormWithoutANewId() {
		byte[] schema = "{\"id\":\"f\"}".getBytes(StandardCharsets.UTF_8);

		assertThat(ModelKeys.rekeyForm(schema, "f", "f")).isSameAs(schema);
		assertThat(ModelKeys.rekeyForm(schema, null, "x")).isSameAs(schema);
		assertThat(ModelKeys.rekeyForm(schema, "f", null)).isSameAs(schema);
		assertThat(ModelKeys.rekeyForm(null, "f", "x")).isNull();
	}
}
