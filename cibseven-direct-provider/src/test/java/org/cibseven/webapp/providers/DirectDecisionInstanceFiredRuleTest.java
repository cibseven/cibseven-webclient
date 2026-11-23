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
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineConfiguration;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.variable.Variables;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.HistoricDecisionInstance;
import org.cibseven.webapp.rest.model.HistoricDecisionOutputInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CIB7-2716: the decision instance view marks the rules that fired from the outputs' rule id, so the
 * direct provider has to pass it on, through the list query the view loads the instance with. The
 * table is the one of CIB7UNION-10: hit policy First, and
 * rules 1, 3 and 4 share the output "true".
 */
class DirectDecisionInstanceFiredRuleTest {

	private static final String ENGINE_NAME = "decisionfiredrule";

	private static final String CTM_ORDER_DMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/" xmlns:camunda="http://camunda.org/schema/1.0/dmn"
		    id="cib7-2716" name="CIB7-2716" namespace="http://camunda.org/schema/1.0/dmn">
		  <decision id="ctmOrder" name="CTM order" camunda:historyTimeToLive="7">
		    <decisionTable id="table" hitPolicy="FIRST">
		      <input id="isCtmOrder"><inputExpression id="ie1" typeRef="boolean"><text>isCtmOrder</text></inputExpression></input>
		      <input id="count"><inputExpression id="ie2" typeRef="integer"><text>count</text></inputExpression></input>
		      <output id="result" name="result" typeRef="boolean"/>
		      <rule id="rule-1"><inputEntry id="r1i1"><text>true</text></inputEntry><inputEntry id="r1i2"><text></text></inputEntry><outputEntry id="r1o"><text>true</text></outputEntry></rule>
		      <rule id="rule-2"><inputEntry id="r2i1"><text></text></inputEntry><inputEntry id="r2i2"><text>0</text></inputEntry><outputEntry id="r2o"><text>false</text></outputEntry></rule>
		      <rule id="rule-3"><inputEntry id="r3i1"><text>false</text></inputEntry><inputEntry id="r3i2"><text>1</text></inputEntry><outputEntry id="r3o"><text>true</text></outputEntry></rule>
		      <rule id="rule-4"><inputEntry id="r4i1"><text></text></inputEntry><inputEntry id="r4i2"><text>&gt; 1</text></inputEntry><outputEntry id="r4o"><text>true</text></outputEntry></rule>
		      <rule id="rule-5"><inputEntry id="r5i1"><text></text></inputEntry><inputEntry id="r5i2"><text></text></inputEntry><outputEntry id="r5o"><text>false</text></outputEntry></rule>
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

		engine.getRepositoryService().createDeployment().addString("ctm-order.dmn", CTM_ORDER_DMN).deploy();
	}

	@AfterEach
	void tearDown() {
		engine.close();
	}

	@Test
	void outputsNameTheRuleThatFired() {
		engine.getDecisionService().evaluateDecisionTableByKey("ctmOrder",
			Variables.createVariables().putValue("isCtmOrder", false).putValue("count", 1));
		String instanceId = engine.getHistoryService().createHistoricDecisionInstanceQuery().singleResult().getId();

		// the query DecisionInstance.vue sends to load the instance it shows
		Collection<HistoricDecisionInstance> instances = decisionProvider.getHistoricDecisionInstances(new HashMap<>(Map.of(
			"decisionInstanceId", instanceId,
			"includeInputs", true,
			"includeOutputs", true,
			"disableBinaryFetching", true,
			"disableCustomObjectDeserialization", true,
			"maxResults", 1)), user);

		assertThat(instances).hasSize(1);
		assertThat(instances.iterator().next().getOutputs())
			.extracting(HistoricDecisionOutputInstance::getClauseId, HistoricDecisionOutputInstance::getRuleId,
				HistoricDecisionOutputInstance::getRuleOrder, HistoricDecisionOutputInstance::getValue)
			.containsExactly(tuple("result", "rule-3", 1, true));
	}
}
