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

import java.util.Optional;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineConfiguration;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Decision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CIB7-2392: the same decision key deployed without a tenant (twice) and for the tenant acme (once).
 * The decision view asks for one tenant's versions, or for those without a tenant.
 */
class DirectDecisionVersionsOfTenantTest {

	private static final String ENGINE_NAME = "decisionversionsoftenant";

	private static final String RISK_DMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/" xmlns:camunda="http://camunda.org/schema/1.0/dmn"
		    id="risk-definitions" name="Risk" namespace="http://camunda.org/schema/1.0/dmn">
		  <decision id="risk" name="Risk %s" camunda:historyTimeToLive="7">
		    <decisionTable id="table">
		      <input id="amount"><inputExpression id="ie" typeRef="integer"><text>amount</text></inputExpression></input>
		      <output id="level" name="level" typeRef="string"/>
		      <rule id="rule-1"><inputEntry id="i1"><text></text></inputEntry><outputEntry id="o1"><text>"low"</text></outputEntry></rule>
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
			.setJobExecutorActivate(false)
			.buildProcessEngine();
		DirectProviderUtil providerUtil = new DirectProviderUtil();
		providerUtil.processEngines.put(engine.getName(), engine);
		providerUtil.objectMappers.put(engine.getName(), JacksonConfigurator.configureObjectMapper());
		decisionProvider = new DirectDecisionProvider(providerUtil);
		user = new CIBUser("demo");
		user.setEngine(ENGINE_NAME);

		deploy("without tenant 1", null);
		deploy("without tenant 2", null);
		deploy("of acme", "acme");
	}

	private void deploy(String name, String tenantId) {
		engine.getRepositoryService().createDeployment().tenantId(tenantId)
			.addString("risk.dmn", RISK_DMN.formatted(name)).deploy();
	}

	@AfterEach
	void tearDown() {
		engine.close();
	}

	@Test
	void listsOnlyTheVersionsOfTheTenant() {
		assertThat(decisionProvider.getDecisionVersionsByKey("risk", "acme", false, Optional.of(true), user))
			.extracting(Decision::getName, Decision::getVersion, Decision::getTenantId)
			.containsExactly(tuple("Risk of acme", 1, "acme"));
	}

	@Test
	void listsOnlyTheVersionsWithoutTenantNewestFirst() {
		assertThat(decisionProvider.getDecisionVersionsByKey("risk", null, true, Optional.of(true), user))
			.extracting(Decision::getName, Decision::getVersion, Decision::getTenantId)
			.containsExactly(tuple("Risk without tenant 2", 2, null), tuple("Risk without tenant 1", 1, null));
	}

	@Test
	void listsEveryTenantsVersionsWithoutARestriction() {
		assertThat(decisionProvider.getDecisionVersionsByKey("risk", Optional.of(true), user))
			.extracting(Decision::getTenantId)
			.containsExactlyInAnyOrder(null, null, "acme");
	}

	@Test
	void listsNothingForATenantWithoutTheKey() {
		assertThat(decisionProvider.getDecisionVersionsByKey("risk", "other", false, Optional.of(true), user)).isEmpty();
	}
}
