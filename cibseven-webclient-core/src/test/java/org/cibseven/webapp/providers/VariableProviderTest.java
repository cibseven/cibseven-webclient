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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

public class VariableProviderTest {

	private MockEngineRest engine;
	private VariableProvider variableProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		variableProvider = engine.wire(new VariableProvider());
		user = MockEngineRest.user();
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	// ---------- task variables ----------

	@Test
	void fetchVariableImpl_asksForTheNamedTaskVariable() throws Exception {
		engine.enqueueJson("{\"name\":\"amount\",\"value\":42,\"type\":\"Integer\"}");

		Variable variable = variableProvider.fetchVariableImpl("task-1", "amount", false, user);

		assertThat(variable.getName()).isEqualTo("amount");
		assertThat(engine.takePath()).isEqualTo("/task/task-1/variables/amount?deserializeValue=false");
	}

	@Test
	void fetchVariable_readsBothTheSerialisedAndDeserialisedForm() throws Exception {
		// the serialised call comes first, then the deserialised one
		engine.enqueueJson("{\"name\":\"payload\",\"value\":\"{\\\"a\\\":1}\",\"type\":\"Object\"}");
		engine.enqueueJson("{\"name\":\"payload\",\"value\":{\"a\":1},\"type\":\"Object\"}");

		Variable variable = variableProvider.fetchVariable("task-1", "payload", false, user);

		// asked for deserializeValue=false first, then =true
		assertThat(engine.takePath()).isEqualTo("/task/task-1/variables/payload?deserializeValue=false");
		assertThat(engine.takePath()).isEqualTo("/task/task-1/variables/payload?deserializeValue=true");
		// with deserializeValue=false the serialised variable is returned, carrying both forms
		assertThat(variable.getValueSerialized()).isEqualTo("{\"a\":1}");
		assertThat(variable.getValueDeserialized()).isNotNull();
	}

	@Test
	void fetchVariable_returnsTheDeserialisedVariableWhenAsked() throws Exception {
		engine.enqueueJson("{\"name\":\"payload\",\"value\":\"raw\",\"type\":\"Object\"}");
		engine.enqueueJson("{\"name\":\"payload\",\"value\":\"cooked\",\"type\":\"Object\"}");

		Variable variable = variableProvider.fetchVariable("task-1", "payload", true, user);

		assertThat(variable.getValue()).isEqualTo("cooked");
		assertThat(variable.getValueSerialized()).isEqualTo("raw");
		assertThat(variable.getValueDeserialized()).isEqualTo("cooked");
	}

	@Test
	void deleteVariable_deletesTheTaskVariable() throws Exception {
		engine.enqueueEmpty(204);

		variableProvider.deleteVariable("task-1", "amount", user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("DELETE");
		assertThat(request.getPath()).endsWith("/task/task-1/variables/amount");
	}

	// ---------- form variables ----------

	@Test
	void fetchFormVariables_mapsTheVariableMap() throws Exception {
		engine.enqueueJson("{\"amount\":{\"name\":\"amount\",\"value\":42,\"type\":\"Integer\"}}");

		Map<String, Variable> variables = variableProvider.fetchFormVariables("task-1", true, user);

		assertThat(variables).containsOnlyKeys("amount");
		assertThat(variables.get("amount").getName()).isEqualTo("amount");
		assertThat(engine.takePath()).startsWith("/task/task-1/form-variables");
	}

	@Test
	void fetchFormVariables_withNamesListsThemInTheQuery() throws Exception {
		engine.enqueueJson("{}");

		variableProvider.fetchFormVariables(List.of("amount", "customer"), "task-1", false, user);

		String path = engine.takePath();
		assertThat(path).startsWith("/task/task-1/form-variables?deserializeValues=false&variableNames=");
		assertThat(path).contains("amount").contains("customer");
	}

	@Test
	void fetchProcessFormVariables_asksByProcessDefinitionKey() throws Exception {
		engine.enqueueJson("{}");

		variableProvider.fetchProcessFormVariables("invoice", true, user);

		assertThat(engine.takePath()).startsWith("/process-definition/key/invoice/form-variables");
	}

	@Test
	void fetchProcessFormVariables_withNamesAddressesTheDefinitionById() throws Exception {
		engine.enqueueJson("{}");

		variableProvider.fetchProcessFormVariables(List.of("amount"), "definition-1", false, user);

		// note the missing "/key/" segment: this overload addresses the definition by id, unlike
		// the single-argument overload above, even though both name the parameter "key"
		assertThat(engine.takePath()).startsWith("/process-definition/definition-1/form-variables");
	}

	// ---------- process instance variables ----------

	@Test
	void fetchProcessInstanceVariables_queriesTwiceAndReturnsTheSerialisedFormByDefault() throws Exception {
		engine.enqueueJson("[{\"name\":\"payload\",\"value\":\"cooked\",\"type\":\"Object\"}]");
		engine.enqueueJson("[{\"name\":\"payload\",\"value\":\"raw\",\"type\":\"Object\"}]");

		Collection<Variable> variables =
			variableProvider.fetchProcessInstanceVariables("pi-1", new HashMap<>(), user);

		// one call per representation, deserialised first
		assertThat(engine.takePath())
			.isEqualTo("/variable-instance?processInstanceIdIn=pi-1&deserializeValues=true");
		assertThat(engine.takePath())
			.isEqualTo("/variable-instance?processInstanceIdIn=pi-1&deserializeValues=false");
		assertThat(variables).hasSize(1);
		assertThat(variables.iterator().next().getValue()).isEqualTo("raw");
	}

	@Test
	void fetchProcessInstanceVariables_returnsTheDeserialisedFormWhenAsked() throws Exception {
		engine.enqueueJson("[{\"name\":\"payload\",\"value\":\"cooked\",\"type\":\"Object\"}]");
		engine.enqueueJson("[{\"name\":\"payload\",\"value\":\"raw\",\"type\":\"Object\"}]");
		Map<String, Object> data = new HashMap<>();
		data.put("deserializeValues", Boolean.TRUE);

		Collection<Variable> variables =
			variableProvider.fetchProcessInstanceVariables("pi-1", data, user);

		assertThat(variables.iterator().next().getValue()).isEqualTo("cooked");
	}

	@Test
	void fetchProcessInstanceVariables_scopesEveryCallToTheInstance() throws Exception {
		engine.enqueueJson("[]");
		engine.enqueueJson("[]");

		variableProvider.fetchProcessInstanceVariables("pi-1", new HashMap<>(), user);

		assertThat(engine.takePath()).contains("processInstanceIdIn=pi-1");
		assertThat(engine.takePath()).contains("processInstanceIdIn=pi-1");
	}

	// ---------- variable history ----------

	@Test
	void fetchActivityVariablesHistory_asksTheHistoryEndpoint() throws Exception {
		engine.enqueueJson("[{\"name\":\"amount\",\"value\":42}]");

		Collection<VariableHistory> variables =
			variableProvider.fetchActivityVariablesHistory("activity-1", user);

		assertThat(variables).hasSize(1);
		assertThat(engine.takePath()).isEqualTo("/history/variable-instance?activityInstanceIdIn=activity-1");
	}

	@Test
	void fetchActivityVariables_asksTheRuntimeEndpoint() throws Exception {
		engine.enqueueJson("[]");

		variableProvider.fetchActivityVariables("activity-1", user);

		assertThat(engine.takePath()).isEqualTo("/variable-instance?activityInstanceIdIn=activity-1");
	}

	// ---------- binary data ----------

	@Test
	void fetchVariableDataByExecutionId_streamsTheLocalVariableData() throws Exception {
		engine.enqueueJson("payload-bytes");

		ResponseEntity<byte[]> response =
			variableProvider.fetchVariableDataByExecutionId("exec-1", "attachment", user);

		assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).isEqualTo("payload-bytes");
		assertThat(engine.takePath()).isEqualTo("/execution/exec-1/localVariables/attachment/data");
	}

	@Test
	void fetchHistoryVariableDataById_streamsTheHistoricVariableData() throws Exception {
		engine.enqueueJson("old-bytes");

		ResponseEntity<byte[]> response = variableProvider.fetchHistoryVariableDataById("var-1", user);

		assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).isEqualTo("old-bytes");
		assertThat(engine.takePath()).isEqualTo("/history/variable-instance/var-1/data");
	}

	@Test
	void modifyVariableByExecutionId_postsTheModificationsToLocalVariables() throws Exception {
		engine.enqueueEmpty(204);
		Map<String, Object> data = new HashMap<>();
		data.put("modifications", new HashMap<>());

		variableProvider.modifyVariableByExecutionId("exec-1", data, user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).endsWith("/execution/exec-1/localVariables/");
	}
}
