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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import org.cibseven.webapp.auth.CIBUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * A variable name goes into the engine-rest path as exactly one segment: with ; ? # or % in it the
 * engine used to act on another variable, or the request failed (CIB7-2787).
 */
public class VariableNameEncodingTest {

	private static final String VARIABLE_JSON = "{\"type\":\"File\",\"value\":null,"
		+ "\"valueInfo\":{\"filename\":\"notes.txt\",\"mimeType\":\"text/plain\"}}";

	private MockEngineRest engine;
	private VariableProvider variableProvider;
	private TaskProvider taskProvider;
	private ProcessProvider processProvider;
	private ActivityProvider activityProvider;
	private CIBUser user;

	static Stream<Arguments> names() {
		return Stream.of(
			Arguments.of("amount", "amount"),
			Arguments.of("a;b", "a%3Bb"),
			Arguments.of("a?b", "a%3Fb"),
			Arguments.of("a#b", "a%23b"),
			Arguments.of("100%", "100%25"),
			Arguments.of("a b", "a%20b"),
			Arguments.of("Müller", "M%C3%BCller"),
			Arguments.of("...", "..."));
	}

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		variableProvider = engine.wire(new VariableProvider());
		taskProvider = engine.wire(new TaskProvider());
		processProvider = engine.wire(new ProcessProvider());
		activityProvider = engine.wire(new ActivityProvider());
		user = MockEngineRest.user();
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	private RecordedRequest take(String method) throws InterruptedException {
		RecordedRequest request = engine.take();
		assertThat(request.getMethod()).isEqualTo(method);
		return request;
	}

	private String path(RecordedRequest request) {
		return request.getPath().substring(MockEngineRest.ENGINE_REST_PATH.length());
	}

	private static MockMultipartFile upload() {
		return new MockMultipartFile("data", "notes.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
	}

	// ---------- task variables ----------

	@ParameterizedTest
	@MethodSource("names")
	void putLocalVariable(String name, String encoded) throws Exception {
		engine.enqueueEmpty(204);
		taskProvider.putLocalVariable("task-1", name, Map.of("value", "x", "type", "String"), user);
		assertThat(path(take("PUT"))).isEqualTo("/task/task-1/localVariables/" + encoded);
	}

	@ParameterizedTest
	@MethodSource("names")
	void deleteLocalVariable(String name, String encoded) throws Exception {
		engine.enqueueEmpty(204);
		taskProvider.deleteLocalVariable("task-1", name, user);
		assertThat(path(take("DELETE"))).isEqualTo("/task/task-1/localVariables/" + encoded);
	}

	@ParameterizedTest
	@MethodSource("names")
	void fetchVariable(String name, String encoded) throws Exception {
		engine.enqueueJson(VARIABLE_JSON);
		engine.enqueueJson(VARIABLE_JSON);
		variableProvider.fetchVariable("task-1", name, false, user);
		assertThat(path(take("GET"))).isEqualTo("/task/task-1/variables/" + encoded + "?deserializeValue=false");
		assertThat(path(take("GET"))).isEqualTo("/task/task-1/variables/" + encoded + "?deserializeValue=true");
	}

	@ParameterizedTest
	@MethodSource("names")
	void deleteVariable(String name, String encoded) throws Exception {
		engine.enqueueEmpty(204);
		variableProvider.deleteVariable("task-1", name, user);
		assertThat(path(take("DELETE"))).isEqualTo("/task/task-1/variables/" + encoded);
	}

	@ParameterizedTest
	@MethodSource("names")
	void fetchVariableFileData(String name, String encoded) throws Exception {
		engine.enqueueJson(VARIABLE_JSON);
		engine.enqueueJson(VARIABLE_JSON);
		engine.enqueueBytes("hello".getBytes(StandardCharsets.UTF_8), "application/octet-stream");
		variableProvider.fetchVariableFileData("task-1", name, user);
		take("GET");
		take("GET");
		assertThat(path(take("GET"))).isEqualTo("/task/task-1/variables/" + encoded + "/data");
	}

	@ParameterizedTest
	@MethodSource("names")
	void uploadVariableFileData(String name, String encoded) throws Exception {
		engine.enqueueEmpty(204);
		variableProvider.uploadVariableFileData("task-1", name, upload(), "File", user);
		assertThat(path(take("POST"))).isEqualTo("/task/task-1/variables/" + encoded + "/data");
	}

	// ---------- process instance variables ----------

	@ParameterizedTest
	@MethodSource("names")
	void fetchVariableByProcessInstanceId(String name, String encoded) throws Exception {
		engine.enqueueJson(VARIABLE_JSON);
		engine.enqueueJson(VARIABLE_JSON);
		variableProvider.fetchVariableByProcessInstanceId("pi-1", name, user);
		assertThat(path(take("GET"))).isEqualTo("/process-instance/pi-1/variables/" + encoded + "?deserializeValue=false");
		assertThat(path(take("GET"))).isEqualTo("/process-instance/pi-1/variables/" + encoded + "?deserializeValue=true");
	}

	@ParameterizedTest
	@MethodSource("names")
	void fetchProcessInstanceVariable(String name, String encoded) throws Exception {
		engine.enqueueJson(VARIABLE_JSON);
		engine.enqueueJson(VARIABLE_JSON);
		processProvider.fetchProcessInstanceVariable("pi-1", name, true, user);
		assertThat(path(take("GET"))).isEqualTo("/process-instance/pi-1/variables/" + encoded + "?deserializeValue=false");
		assertThat(path(take("GET"))).isEqualTo("/process-instance/pi-1/variables/" + encoded + "?deserializeValue=true");
	}

	@ParameterizedTest
	@MethodSource("names")
	void putProcessInstanceVariable(String name, String encoded) throws Exception {
		engine.enqueueEmpty(204);
		variableProvider.putProcessInstanceVariable("pi-1", name, Map.of("value", "x", "type", "String"), user);
		assertThat(path(take("PUT"))).isEqualTo("/process-instance/pi-1/variables/" + encoded);
	}

	@ParameterizedTest
	@MethodSource("names")
	void fetchProcessInstanceVariableData(String name, String encoded) throws Exception {
		engine.enqueueJson(VARIABLE_JSON);
		engine.enqueueJson(VARIABLE_JSON);
		engine.enqueueBytes("hello".getBytes(StandardCharsets.UTF_8), "application/octet-stream");
		variableProvider.fetchProcessInstanceVariableData("pi-1", name, user);
		take("GET");
		take("GET");
		assertThat(path(take("GET"))).isEqualTo("/process-instance/pi-1/variables/" + encoded + "/data");
	}

	@ParameterizedTest
	@MethodSource("names")
	void uploadProcessInstanceVariableFileData(String name, String encoded) throws Exception {
		engine.enqueueEmpty(204);
		variableProvider.uploadProcessInstanceVariableFileData("pi-1", name, upload(), "File", user);
		assertThat(path(take("POST"))).isEqualTo("/process-instance/pi-1/variables/" + encoded + "/data");
	}

	// ---------- execution variables ----------

	@ParameterizedTest
	@MethodSource("names")
	void putLocalExecutionVariable(String name, String encoded) throws Exception {
		engine.enqueueEmpty(204);
		variableProvider.putLocalExecutionVariable("exec-1", name, Map.of("value", "x", "type", "String"), user);
		assertThat(path(take("PUT"))).isEqualTo("/execution/exec-1/localVariables/" + encoded);
	}

	@ParameterizedTest
	@MethodSource("names")
	void deleteVariableByExecutionId(String name, String encoded) throws Exception {
		engine.enqueueEmpty(204);
		activityProvider.deleteVariableByExecutionId("exec-1", name, user);
		assertThat(path(take("DELETE"))).isEqualTo("/execution/exec-1/localVariables/" + encoded);
	}

	@ParameterizedTest
	@MethodSource("names")
	void fetchVariableDataByExecutionId(String name, String encoded) throws Exception {
		engine.enqueueBytes("hello".getBytes(StandardCharsets.UTF_8), "application/octet-stream");
		variableProvider.fetchVariableDataByExecutionId("exec-1", name, user);
		assertThat(path(take("GET"))).isEqualTo("/execution/exec-1/localVariables/" + encoded + "/data");
	}

	@ParameterizedTest
	@MethodSource("names")
	void modifyVariableDataByExecutionId(String name, String encoded) throws Exception {
		engine.enqueueEmpty(204);
		variableProvider.modifyVariableDataByExecutionId("exec-1", name, upload(), "File", user);
		assertThat(path(take("POST"))).isEqualTo("/execution/exec-1/localVariables/" + encoded + "/data");
	}

	// ---------- the URL itself ----------

	@Test
	void engineRestUri_encodesTheIdsTooAndKeepsTheEnginePath() throws Exception {
		user.setEngine(variableProvider.properties.getEngineRest().getUrl() + "|" + MockEngineRest.ENGINE_REST_PATH + "|other");
		engine.enqueueEmpty(204);

		variableProvider.deleteVariable("task 1", "a;b", user);

		assertThat(path(take("DELETE"))).isEqualTo("/engine/other/task/task%201/variables/a%3Bb");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"http://cibseven_engine:8080/my%20path/engine-rest | http://cibseven_engine:8080/my%20path/engine-rest",
		"http://localhost:8080/engine-rest/engine/my engine | http://localhost:8080/engine-rest/engine/my%20engine",
		"http://localhost:8080/engine-rest/engine/Müller    | http://localhost:8080/engine-rest/engine/M%C3%BCller",
		"http://localhost:8080/engine-rest/engine/100%      | http://localhost:8080/engine-rest/engine/100%25"
	})
	void engineRestUri_encodesTheConfiguredUrlOnce(String configured, String expected) {
		VariableProvider provider = new VariableProvider() {
			@Override
			protected String getEngineRestUrl(CIBUser user) {
				return configured;
			}
		};

		assertThat(provider.engineRestUri(user, "task", "task-1", "variables", "a;b").toString())
			.isEqualTo(expected + "/task/task-1/variables/a%3Bb");
	}

	@Test
	void engineRestUri_encodesAnEngineNameWithASpace() throws Exception {
		user.setEngine(variableProvider.properties.getEngineRest().getUrl() + "|" + MockEngineRest.ENGINE_REST_PATH + "|my engine");
		engine.enqueueEmpty(204);

		variableProvider.deleteVariable("task-1", "x", user);

		assertThat(path(take("DELETE"))).isEqualTo("/engine/my%20engine/task/task-1/variables/x");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { ".", ".." })
	void engineRestUri_rejectsASegmentThatWouldPointAtAnotherEndpoint(String name) {
		assertThatThrownBy(() -> variableProvider.deleteVariable("task-1", name, user))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(engine.requestCount()).isZero();
	}
}
