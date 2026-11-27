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

import java.util.HashMap;
import java.util.Map;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineConfiguration;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.variable.Variables;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.HistoricDecisionInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CIB7-2711: the services endpoint hands the historic decision instance query to the direct provider
 * as the plain request parameters, so a list parameter like decisionDefinitionKeyIn arrives as one
 * comma separated string. The provider has to split it the way engine-rest does instead of failing
 * with a 500.
 */
class DirectHistoricDecisionInstanceQueryTest {

	private static final String ENGINE_NAME = "historicdecisionquery";

	private static final String DMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/" xmlns:camunda="http://camunda.org/schema/1.0/dmn"
		    id="cib7-2711" name="CIB7-2711" namespace="http://camunda.org/schema/1.0/dmn">
		  <decision id="alpha" name="Alpha" camunda:historyTimeToLive="7">
		    <decisionTable id="alphaTable">
		      <input id="alphaIn"><inputExpression id="alphaExpr" typeRef="integer"><text>x</text></inputExpression></input>
		      <output id="alphaOut" name="result" typeRef="string"/>
		      <rule id="alphaRule"><inputEntry id="alphaEntry"><text></text></inputEntry><outputEntry id="alphaOutEntry"><text>"a"</text></outputEntry></rule>
		    </decisionTable>
		  </decision>
		  <decision id="beta" name="Beta" camunda:historyTimeToLive="7">
		    <decisionTable id="betaTable">
		      <input id="betaIn"><inputExpression id="betaExpr" typeRef="integer"><text>x</text></inputExpression></input>
		      <output id="betaOut" name="result" typeRef="string"/>
		      <rule id="betaRule"><inputEntry id="betaEntry"><text></text></inputEntry><outputEntry id="betaOutEntry"><text>"b"</text></outputEntry></rule>
		    </decisionTable>
		  </decision>
		  <decision id="gamma" name="Gamma" camunda:historyTimeToLive="7">
		    <decisionTable id="gammaTable">
		      <input id="gammaIn"><inputExpression id="gammaExpr" typeRef="integer"><text>x</text></inputExpression></input>
		      <output id="gammaOut" name="result" typeRef="string"/>
		      <rule id="gammaRule"><inputEntry id="gammaEntry"><text></text></inputEntry><outputEntry id="gammaOutEntry"><text>"c"</text></outputEntry></rule>
		    </decisionTable>
		  </decision>
		</definitions>
		""";

	private ProcessEngine engine;
	private DirectDecisionProvider decisionProvider;
	private CIBUser user;

	@BeforeEach
	void start() {
		engine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
			.setJdbcUrl("jdbc:h2:mem:" + ENGINE_NAME + ";DB_CLOSE_DELAY=1000")
			.setProcessEngineName(ENGINE_NAME)
			// decision instances are only recorded with history level full
			.setHistory(ProcessEngineConfiguration.HISTORY_FULL)
			.setJobExecutorActivate(false)
			.buildProcessEngine();
		DirectProviderUtil providerUtil = new DirectProviderUtil();
		providerUtil.processEngines.put(engine.getName(), engine);
		providerUtil.objectMappers.put(engine.getName(), JacksonConfigurator.configureObjectMapper());
		decisionProvider = new DirectDecisionProvider(providerUtil);
		user = new CIBUser("demo");
		user.setEngine(ENGINE_NAME);

		engine.getRepositoryService().createDeployment().addString("cib7-2711.dmn", DMN).deploy();
		for (String key : new String[] { "alpha", "beta", "gamma" }) {
			engine.getDecisionService().evaluateDecisionTableByKey(key, Variables.createVariables().putValue("x", 1));
		}
	}

	@AfterEach
	void tearDown() {
		engine.close();
	}

	/** The request the Batch Operations criterion "Decision Definition Keys In" sends, with two keys. */
	@Test
	void listFiltersBySeveralCommaSeparatedKeys() {
		Map<String, Object> params = new HashMap<>(Map.of(
			"decisionDefinitionKeyIn", "alpha,beta",
			"firstResult", "0",
			"maxResults", "20"));

		assertThat(decisionProvider.getHistoricDecisionInstances(params, user))
			.extracting(HistoricDecisionInstance::getDecisionDefinitionKey)
			.containsExactlyInAnyOrder("alpha", "beta");
	}

	@Test
	void listFiltersByASingleKey() {
		Map<String, Object> params = new HashMap<>(Map.of("decisionDefinitionKeyIn", "gamma"));

		assertThat(decisionProvider.getHistoricDecisionInstances(params, user))
			.extracting(HistoricDecisionInstance::getDecisionDefinitionKey)
			.containsExactly("gamma");
	}

	@Test
	void listAnswersEmptyForKeysThatMatchNothing() {
		Map<String, Object> params = new HashMap<>(Map.of("decisionDefinitionKeyIn", "unknown1,unknown2"));

		assertThat(decisionProvider.getHistoricDecisionInstances(params, user)).isEmpty();
	}

	@Test
	void listFiltersByCommaSeparatedDefinitionIds() {
		String alphaId = definitionId("alpha");
		String gammaId = definitionId("gamma");
		Map<String, Object> params = new HashMap<>(Map.of("decisionDefinitionIdIn", alphaId + "," + gammaId));

		assertThat(decisionProvider.getHistoricDecisionInstances(params, user))
			.extracting(HistoricDecisionInstance::getDecisionDefinitionKey)
			.containsExactlyInAnyOrder("alpha", "gamma");
	}

	@Test
	void countFiltersByCommaSeparatedKeys() {
		Map<String, Object> params = new HashMap<>(Map.of("decisionDefinitionKeyIn", "beta,gamma"));

		assertThat(decisionProvider.getHistoricDecisionInstanceCount(params, user)).isEqualTo(2L);
	}

	@Test
	void getByIdAcceptsTheIncludeFlagsAsStrings() {
		String betaInstanceId = engine.getHistoryService().createHistoricDecisionInstanceQuery()
			.decisionDefinitionKey("beta").singleResult().getId();
		Map<String, Object> params = new HashMap<>(Map.of(
			"includeInputs", "true",
			"includeOutputs", "true"));

		HistoricDecisionInstance instance = decisionProvider.getHistoricDecisionInstanceById(betaInstanceId, params, user);

		assertThat(instance.getDecisionDefinitionKey()).isEqualTo("beta");
		assertThat(instance.getOutputs()).hasSize(1);
	}

	private String definitionId(String key) {
		return engine.getRepositoryService().createDecisionDefinitionQuery().decisionDefinitionKey(key).singleResult().getId();
	}
}
