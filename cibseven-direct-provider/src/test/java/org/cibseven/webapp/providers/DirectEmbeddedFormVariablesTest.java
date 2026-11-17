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

import java.util.List;
import java.util.Map;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineConfiguration;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.model.bpmn.Bpmn;
import org.cibseven.bpm.model.bpmn.BpmnModelInstance;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CIB7-2757: the variable writes of bpm-sdk's task and process-instance resources, run against a
 * real engine with the bodies the SDK sends, extra properties included.
 */
class DirectEmbeddedFormVariablesTest {

	private static final String ENGINE_NAME = "embeddedformvariables";

	private static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess("form")
		.startEvent()
		.userTask("review")
		.endEvent()
		.done();

	private ProcessEngine engine;
	private DirectTaskProvider taskProvider;
	private DirectVariableProvider variableProvider;
	private CIBUser user;
	private String instanceId;
	private String taskId;

	@BeforeEach
	void start() {
		engine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
			.setJdbcUrl("jdbc:h2:mem:" + ENGINE_NAME + ";DB_CLOSE_DELAY=1000")
			.setProcessEngineName(ENGINE_NAME)
			.setJobExecutorActivate(false)
			.buildProcessEngine();
		DirectProviderUtil providerUtil = new DirectProviderUtil();
		providerUtil.processEngines.put(engine.getName(), engine);
		providerUtil.objectMappers.put(engine.getName(), JacksonConfigurator.configureObjectMapper());
		taskProvider = new DirectTaskProvider(providerUtil);
		variableProvider = new DirectVariableProvider(providerUtil);
		user = new CIBUser("demo");
		user.setEngine(ENGINE_NAME);

		engine.getRepositoryService().createDeployment().addModelInstance("form.bpmn", PROCESS).deploy();
		instanceId = engine.getRuntimeService().startProcessInstanceByKey("form").getId();
		taskId = engine.getTaskService().createTaskQuery().processInstanceId(instanceId).singleResult().getId();
	}

	@AfterEach
	void tearDown() {
		engine.close();
	}

	@Test
	void modifyLocalVariables_setsAndRemovesTaskLocalVariables() {
		engine.getTaskService().setVariableLocal(taskId, "outdated", "x");

		// task.modifyVariables sends its whole params object, the task id included
		taskProvider.modifyLocalVariables(taskId, Map.of(
			"id", taskId,
			"modifications", Map.of(
				"approved", Map.of("value", true, "type", "Boolean"),
				"amount", Map.of("value", 42, "type", "Integer"),
				"comment", Map.of("value", "looks fine", "type", "String")),
			"deletions", List.of("outdated")), user);

		assertThat(engine.getTaskService().getVariablesLocal(taskId))
			.containsExactlyInAnyOrderEntriesOf(Map.of("approved", true, "amount", 42, "comment", "looks fine"));
		// local to the task: the process instance does not see them
		assertThat(engine.getRuntimeService().getVariables(instanceId)).isEmpty();
	}

	@Test
	void modifyLocalVariables_refusesAnUnknownType() {
		Map<String, Object> patch = Map.of("modifications", Map.of("mistyped", Map.of("value", "1", "type", "NoSuchType")));

		assertThatThrownBy(() -> taskProvider.modifyLocalVariables(taskId, patch, user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot modify variables for task " + taskId)
			.hasMessageContaining("NoSuchType");
		assertThat(engine.getTaskService().getVariablesLocal(taskId)).isEmpty();
	}

	@Test
	void modifyLocalVariables_reportsAFileWithoutFilenameAsSystemException() {
		// the engine's conversion refuses it with an IllegalArgumentException, not a RestException
		Map<String, Object> patch = Map.of("modifications", Map.of("document",
			Map.of("value", "aGVsbG8=", "type", "File", "valueInfo", Map.of("mimeType", "text/plain"))));

		assertThatThrownBy(() -> taskProvider.modifyLocalVariables(taskId, patch, user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot modify variables for task " + taskId);
		assertThat(engine.getTaskService().getVariablesLocal(taskId)).isEmpty();
	}

	@Test
	void modifyLocalVariables_reportsABodyItCannotReadAsSystemException() {
		assertThatThrownBy(() -> taskProvider.modifyLocalVariables(taskId, Map.of("modifications", "not a map"), user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot modify variables for task " + taskId);
	}

	@Test
	void modifyVariableByExecutionId_reportsABodyItCannotReadAsSystemException() {
		// the same conversion as the task path; before, the body's error went out unwrapped
		assertThatThrownBy(() -> variableProvider.modifyVariableByExecutionId(instanceId, Map.of("modifications", "not a map"), user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot modify variables for modifyVariableByExecutionId");
		assertThat(engine.getRuntimeService().getVariables(instanceId)).isEmpty();
	}

	@Test
	void putLocalVariable_setsOneTaskLocalVariable() {
		// task.localVariable sends id and varId next to the value
		taskProvider.putLocalVariable(taskId, "decision", Map.of(
			"id", taskId, "varId", "decision", "value", "accept", "type", "String"), user);

		assertThat(engine.getTaskService().getVariableLocal(taskId, "decision")).isEqualTo("accept");
		assertThat(engine.getRuntimeService().getVariable(instanceId, "decision")).isNull();
	}

	@Test
	void putLocalVariable_refusesAnUnknownType() {
		assertThatThrownBy(() -> taskProvider.putLocalVariable(taskId, "mistyped", Map.of("value", "1", "type", "NoSuchType"), user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot convert variable mistyped");
	}

	@Test
	void taskVariableWrites_reportAnUnknownTaskAsSystemException() {
		Map<String, Object> value = Map.of("value", "x", "type", "String");

		assertThatThrownBy(() -> taskProvider.putLocalVariable("no-such-task", "decision", value, user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot put variable decision of task no-such-task");
		assertThatThrownBy(() -> taskProvider.modifyLocalVariables("no-such-task", Map.of("modifications", Map.of("decision", value)), user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot modify variables of task no-such-task");
		assertThatThrownBy(() -> taskProvider.deleteLocalVariable("no-such-task", "decision", user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot delete variable decision of task no-such-task");
	}

	@Test
	void deleteLocalVariable_removesOnlyTheTaskLocalVariable() {
		engine.getRuntimeService().setVariable(instanceId, "decision", "process level");
		engine.getTaskService().setVariableLocal(taskId, "decision", "task level");

		taskProvider.deleteLocalVariable(taskId, "decision", user);

		assertThat(engine.getTaskService().getVariablesLocal(taskId)).isEmpty();
		assertThat(engine.getRuntimeService().getVariable(instanceId, "decision")).isEqualTo("process level");
	}

	@Test
	void putProcessInstanceVariable_setsAProcessVariable() {
		// process-instance.setVariable sends the name next to the value
		variableProvider.putProcessInstanceVariable(instanceId, "customer", Map.of(
			"name", "customer", "value", "ACME", "type", "String"), user);

		assertThat(engine.getRuntimeService().getVariable(instanceId, "customer")).isEqualTo("ACME");
		// visible from the task as a process variable, not as a local one
		assertThat(engine.getTaskService().getVariable(taskId, "customer")).isEqualTo("ACME");
		assertThat(engine.getTaskService().getVariablesLocal(taskId)).isEmpty();
	}

	@Test
	void putProcessInstanceVariable_refusesAnUnknownType() {
		assertThatThrownBy(() -> variableProvider.putProcessInstanceVariable(instanceId, "mistyped", Map.of("value", "1", "type", "NoSuchType"), user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot convert variable mistyped");
	}

	@Test
	void putProcessInstanceVariable_reportsAnUnknownInstanceAsSystemException() {
		assertThatThrownBy(() -> variableProvider.putProcessInstanceVariable("no-such-instance", "customer", Map.of("value", "ACME", "type", "String"), user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot put process instance variable customer");
	}
}
