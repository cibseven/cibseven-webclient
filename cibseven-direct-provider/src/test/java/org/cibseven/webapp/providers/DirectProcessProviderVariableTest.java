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

import java.util.Map;

import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineConfiguration;
import org.cibseven.bpm.engine.authorization.Authorization;
import org.cibseven.bpm.engine.authorization.Permissions;
import org.cibseven.bpm.engine.authorization.Resources;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.model.bpmn.Bpmn;
import org.cibseven.bpm.model.bpmn.BpmnModelInstance;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Variable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@link DirectProcessProvider#fetchProcessInstanceVariable} against a real in-memory engine (CIB7-2380).
 * Which scope a variable belongs to and what a user may read are the engine's rules, so stubbing the
 * engine would prove nothing. The expected answers are those of engine-rest's
 * GET /process-instance/{id}/variables/{name}, which the default provider calls.
 */
class DirectProcessProviderVariableTest {

	private static final String ENGINE_NAME = "instancevariables";
	private static final String VARIABLE = "_statusDataset";

	/** The user task runs inside an embedded sub process, i.e. in a child execution of the instance. */
	private static final BpmnModelInstance SCOPED_PROCESS = Bpmn.createExecutableProcess("scoped")
		.startEvent()
		.subProcess("sub").embeddedSubProcess()
			.startEvent()
			.userTask("inner")
			.endEvent()
		.subProcessDone()
		.endEvent()
		.done();

	private ProcessEngine engine;
	private DirectProcessProvider provider;
	private final CIBUser tester = testerOn(ENGINE_NAME);

	@AfterEach
	void tearDown() {
		if (engine != null) {
			engine.getIdentityService().clearAuthentication();
			engine.close();
		}
	}

	@Test
	void returnsTheVariableOfTheInstance() {
		start(false);
		String instanceId = startInstance(Map.of(VARIABLE, "instance"));

		assertThat(provider.fetchProcessInstanceVariable(instanceId, VARIABLE, true, tester).getValue())
			.isEqualTo("instance");
	}

	/** The case the variable-instance query got wrong: two results, read as "missing". */
	@Test
	void returnsTheInstanceVariableWhenAChildExecutionHoldsOneOfTheSameName() {
		start(false);
		String instanceId = startInstance(Map.of(VARIABLE, "instance"));
		engine.getRuntimeService().setVariableLocal(childExecutionOf(instanceId), VARIABLE, "child");

		assertThat(provider.fetchProcessInstanceVariable(instanceId, VARIABLE, true, tester).getValue())
			.isEqualTo("instance");
	}

	@Test
	void treatsAVariableOnlyAChildExecutionHoldsAsMissing() {
		start(false);
		String instanceId = startInstance(Map.of());
		engine.getRuntimeService().setVariableLocal(childExecutionOf(instanceId), VARIABLE, "child");

		assertThatThrownBy(() -> provider.fetchProcessInstanceVariable(instanceId, VARIABLE, true, tester))
			.isInstanceOf(NoObjectFoundException.class);
	}

	@Test
	void treatsAnAbsentVariableAsMissing() {
		start(false);
		String instanceId = startInstance(Map.of());

		assertThatThrownBy(() -> provider.fetchProcessInstanceVariable(instanceId, VARIABLE, true, tester))
			.isInstanceOf(NoObjectFoundException.class);
	}

	/** The other case the query got wrong: its authorization-filtered empty result was read as "missing". */
	@Test
	void failsInsteadOfReportingMissingWhenTheReadIsDenied() {
		start(true);
		String instanceId = startInstance(Map.of(VARIABLE, "instance"));
		engine.getIdentityService().setAuthentication("tester", null);

		assertThatThrownBy(() -> provider.fetchProcessInstanceVariable(instanceId, VARIABLE, true, tester))
			.isInstanceOf(SystemException.class)
			.isNotInstanceOf(NoObjectFoundException.class)
			.hasCauseInstanceOf(AuthorizationException.class);
	}

	@Test
	void returnsTheVariableWhenTheReadIsGranted() {
		start(true);
		String instanceId = startInstance(Map.of(VARIABLE, "instance"));
		Authorization read = engine.getAuthorizationService().createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
		read.setUserId("tester");
		read.setResource(Resources.PROCESS_INSTANCE);
		read.setResourceId(Authorization.ANY);
		read.addPermission(Permissions.READ);
		engine.getAuthorizationService().saveAuthorization(read);
		engine.getIdentityService().setAuthentication("tester", null);

		assertThat(provider.fetchProcessInstanceVariable(instanceId, VARIABLE, true, tester).getValue())
			.isEqualTo("instance");
	}

	/** Deploys and starts while no user is authenticated, so the engine checks nothing until a test authenticates. */
	private String startInstance(Map<String, Object> variables) {
		engine.getRepositoryService().createDeployment().addModelInstance("scoped.bpmn", SCOPED_PROCESS).deploy();
		return engine.getRuntimeService().startProcessInstanceByKey("scoped", variables).getId();
	}

	private String childExecutionOf(String instanceId) {
		String childId = engine.getRuntimeService().createExecutionQuery()
			.processInstanceId(instanceId).activityId("inner").singleResult().getId();
		assertThat(childId).isNotEqualTo(instanceId);
		return childId;
	}

	/**
	 * A real {@code DirectProviderUtil} with the engine and the object mapper it would create on lookup
	 * seeded into its caches, as in DirectUserProviderAuthorizationTest.
	 */
	private void start(boolean authorizationEnabled) {
		engine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
			.setJdbcUrl("jdbc:h2:mem:" + ENGINE_NAME + authorizationEnabled + ";DB_CLOSE_DELAY=1000")
			.setProcessEngineName(ENGINE_NAME)
			.setAuthorizationEnabled(authorizationEnabled)
			.setJobExecutorActivate(false)
			.buildProcessEngine();
		DirectProviderUtil providerUtil = new DirectProviderUtil();
		providerUtil.processEngines.put(engine.getName(), engine);
		providerUtil.objectMappers.put(engine.getName(), JacksonConfigurator.configureObjectMapper());
		provider = new DirectProcessProvider(providerUtil, null);
	}

	private static CIBUser testerOn(String engineName) {
		CIBUser user = new CIBUser("tester");
		user.setEngine(engineName);
		return user;
	}
}
