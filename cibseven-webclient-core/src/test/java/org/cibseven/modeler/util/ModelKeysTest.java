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

class ModelKeysTest {

	private static final String NS = "xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
		+ "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\"";

	private static final String PROCESS = """
		<?xml version="1.0" encoding="UTF-8"?>
		<bpmn:definitions %s id="Definitions_1">
		  <bpmn:process id="invoice" name="invoice" isExecutable="true">
		    <bpmn:startEvent id="invoice_start" name="invoice" />
		    <bpmn:userTask id="Approve">
		      <bpmn:extensionElements><camunda:formData><camunda:formField id="invoice" type="boolean" /></camunda:formData></bpmn:extensionElements>
		    </bpmn:userTask>
		    <bpmn:callActivity id="Call_1" calledElement="invoice" />
		  </bpmn:process>
		  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
		    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="invoice" />
		  </bpmndi:BPMNDiagram>
		</bpmn:definitions>
		""".formatted(NS);

	private static final String COLLABORATION = """
		<bpmn:definitions %s id="d">
		  <bpmn:collaboration id="Collaboration_1"><bpmn:participant id="P" processRef="Process_1" /></bpmn:collaboration>
		  <bpmn:process id='Process_1' />
		  <bpmndi:BPMNDiagram id="D"><bpmndi:BPMNPlane id="Plane" bpmnElement="Collaboration_1" /></bpmndi:BPMNDiagram>
		</bpmn:definitions>
		""".formatted(NS);

	private static String rekey(String xml, String newKey) {
		return new String(ModelKeys.rekeyDiagram(xml.getBytes(StandardCharsets.UTF_8), newKey), StandardCharsets.UTF_8);
	}

	private static String rekeyForm(String json, String newKey) {
		return new String(ModelKeys.rekeyForm(json.getBytes(StandardCharsets.UTF_8), newKey), StandardCharsets.UTF_8);
	}

	@Test
	void givesTheProcessAndItsDiagramTheNewKey() {
		String copy = rekey(PROCESS, "invoice-copy");

		assertThat(copy).contains("<bpmn:process id=\"invoice-copy\"", "bpmnElement=\"invoice-copy\"");
	}

	/** Names, a form field that happens to share the key, other ids and a call of the original keep what they say. */
	@Test
	void leavesEverythingElseAsItIs() {
		String copy = rekey(PROCESS, "invoice-copy");

		assertThat(copy).contains("name=\"invoice\"", "id=\"invoice_start\"", "<camunda:formField id=\"invoice\"",
			"calledElement=\"invoice\"");
		assertThat(copy.replace("invoice-copy", "invoice")).isEqualTo(PROCESS);
	}

	/** With a pool the modeler shows, and saves, the collaboration; the process keeps its id. */
	@Test
	void givesTheCollaborationTheDiagramShowsTheNewKey() {
		String copy = rekey(COLLABORATION, "Lanes");

		assertThat(copy).contains("<bpmn:collaboration id=\"Lanes\"", "bpmnElement=\"Lanes\"", "processRef=\"Process_1\"",
			"<bpmn:process id='Process_1'");
	}

	@Test
	void followsTheProcessReferenceOfAParticipantAndSingleQuotes() {
		String xml = """
			<bpmn:definitions %s id="d">
			  <bpmn:collaboration id="C"><bpmn:participant id="P" processRef='invoice' /></bpmn:collaboration>
			  <bpmn:process id='invoice' />
			  <bpmndi:BPMNDiagram id="D"><bpmndi:BPMNPlane id="Plane" bpmnElement='invoice' /></bpmndi:BPMNDiagram>
			</bpmn:definitions>
			""".formatted(NS);

		assertThat(rekey(xml, "billing")).isEqualTo(xml.replace("'invoice'", "'billing'"));
	}

	/** Without a diagram the modeler takes the collaboration, else the first process. */
	@Test
	void takesTheCollaborationElseTheProcessWithoutADiagram() {
		String pool = "<bpmn:definitions " + NS + " id=\"d\"><bpmn:collaboration id=\"C\" /><bpmn:process id=\"p\" /></bpmn:definitions>";
		String plain = "<bpmn:definitions " + NS + " id=\"d\"><bpmn:process id=\"p\" /><bpmn:process id=\"q\" /></bpmn:definitions>";

		assertThat(rekey(pool, "k")).contains("<bpmn:collaboration id=\"k\"", "<bpmn:process id=\"p\"");
		assertThat(rekey(plain, "k")).contains("<bpmn:process id=\"k\"", "<bpmn:process id=\"q\"");
	}

	@Test
	void findsTheIdBehindAnAttributeValueThatHoldsAnAngleBracket() {
		String xml = "<bpmn:definitions " + NS + " id=\"d\"><bpmn:process name=\"a > b\" id=\"p\" /></bpmn:definitions>";

		assertThat(rekey(xml, "k")).contains("<bpmn:process name=\"a > b\" id=\"k\"");
	}

	/** The modeler saves a DMN under its definitions id; decisions and the references to them stay. */
	@Test
	void givesTheDmnDefinitionsTheNewKey() {
		String xml = """
			<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/" xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/" id="rules" name="Rules">
			  <decision id="dec"><informationRequirement id="r"><requiredDecision href="#other" /></informationRequirement></decision>
			  <decision id="other" />
			  <dmndi:DMNDI><dmndi:DMNDiagram id="dia"><dmndi:DMNShape id="s" dmnElementRef="dec" /></dmndi:DMNDiagram></dmndi:DMNDI>
			</definitions>
			""";

		assertThat(rekey(xml, "rules-copy")).isEqualTo(xml.replace("id=\"rules\"", "id=\"rules-copy\""));
	}

	@Test
	void keepsTheEncodingTheFileDeclares() {
		String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><bpmn:definitions " + NS
			+ " id=\"d\"><bpmn:process id=\"p\" name=\"Prüfung\" /></bpmn:definitions>";

		byte[] copy = ModelKeys.rekeyDiagram(xml.getBytes(StandardCharsets.ISO_8859_1), "k");

		assertThat(new String(copy, StandardCharsets.ISO_8859_1)).isEqualTo(xml.replace("id=\"p\"", "id=\"k\""));
	}

	@Test
	void readsAFileWithAByteOrderMarkAsUtf16() {
		String xml = "<bpmn:definitions " + NS + " id=\"d\"><bpmn:process id=\"p\" name=\"Größe\" /></bpmn:definitions>";

		byte[] copy = ModelKeys.rekeyDiagram(xml.getBytes(StandardCharsets.UTF_16), "k");

		assertThat(new String(copy, StandardCharsets.UTF_16)).isEqualTo(xml.replace("id=\"p\"", "id=\"k\""));
	}

	@Test
	void writesAKeyWithMarkupCharactersAsXml() {
		String xml = "<bpmn:definitions " + NS + " id=\"d\"><bpmn:process id=\"p\" /></bpmn:definitions>";

		assertThat(rekey(xml, "x&\"y")).contains("<bpmn:process id=\"x&amp;&quot;y\"");
	}

	@Test
	void leavesWhatIsNoModelItCanReadAlone() {
		for (String xml : new String[] { "<bpmn/>", "not xml", "<bpmn:definitions " + NS + " id=\"d\" />",
				"<bpmn:definitions " + NS + " id=\"d\"><bpmn:process /></bpmn:definitions>",
				"<bpmn:definitions " + NS + " id=\"d\"><bpmn:process id=\"p\" />"
					+ "<bpmndi:BPMNDiagram id=\"D\"><bpmndi:BPMNPlane id=\"x\" bpmnElement=\"gone\" /></bpmndi:BPMNDiagram></bpmn:definitions>",
				"<!DOCTYPE d [<!ENTITY e \"x\">]><bpmn:definitions " + NS + " id=\"d\"><bpmn:process id=\"p\" /></bpmn:definitions>" }) {
			byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
			assertThat(ModelKeys.rekeyDiagram(bytes, "k")).as(xml).isSameAs(bytes);
		}
	}

	/** An id the text does not spell as the parser reads it, such as one with a character reference */
	@Test
	void leavesAKeyItCannotFindInTheTextAlone() {
		byte[] xml = ("<bpmn:definitions " + NS + " id=\"d\"><bpmn:process id=\"p&#45;1\" /></bpmn:definitions>").getBytes(StandardCharsets.UTF_8);

		assertThat(ModelKeys.rekeyDiagram(xml, "k")).isSameAs(xml);
	}

	@Test
	void changesNothingWithoutANewKey() {
		byte[] xml = PROCESS.getBytes(StandardCharsets.UTF_8);

		assertThat(ModelKeys.rekeyDiagram(xml, "invoice")).isSameAs(xml);
		assertThat(ModelKeys.rekeyDiagram(xml, null)).isSameAs(xml);
		assertThat(ModelKeys.rekeyDiagram(null, "x")).isNull();
	}

	/** Only the id's value changes: component ids, decimals and the layout stay byte for byte. */
	@Test
	void givesTheFormTheNewIdAndKeepsEverythingElse() {
		String schema = """
			{
			  "components": [ { "key": "amount", "defaultValue": 1.10, "id": "invoice-form" } ],
			  "limit": 12345678901234567.25,
			  "id": "invoice-form",
			  "type": "default"
			}
			""";

		assertThat(rekeyForm(schema, "invoice-form-copy"))
			.isEqualTo(schema.replace("\"id\": \"invoice-form\",", "\"id\": \"invoice-form-copy\","));
	}

	@Test
	void replacesAFormIdWithEscapesAndWritesTheNewOneAsJson() {
		assertThat(rekeyForm("{\"id\":\"in\\\"voice\",\"type\":\"default\"}", "a\"b"))
			.isEqualTo("{\"id\":\"a\\\"b\",\"type\":\"default\"}");
	}

	@Test
	void leavesAFormWithoutAnIdItCanReadAlone() {
		for (String json : new String[] { "{}", "[]", "{ not json", "{\"type\":\"default\",\"id\":3}", "{\"id\":\"invoice-form" }) {
			byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
			assertThat(ModelKeys.rekeyForm(bytes, "x")).as(json).isSameAs(bytes);
		}
	}

	@Test
	void changesNoFormWithoutANewId() {
		byte[] schema = "{\"id\":\"f\"}".getBytes(StandardCharsets.UTF_8);

		assertThat(ModelKeys.rekeyForm(schema, "f")).isSameAs(schema);
		assertThat(ModelKeys.rekeyForm(schema, null)).isSameAs(schema);
		assertThat(ModelKeys.rekeyForm(null, "x")).isNull();
	}
}
