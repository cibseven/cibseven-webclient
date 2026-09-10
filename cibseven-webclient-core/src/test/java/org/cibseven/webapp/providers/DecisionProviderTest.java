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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Decision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DecisionProviderTest {

	private MockEngineRest engine;
	private DecisionProvider decisionProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		decisionProvider = engine.wire(new DecisionProvider());
		user = MockEngineRest.user();
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	@Test
	void getDecisionDefinitionList_mapsTheEngineResponse() throws Exception {
		engine.enqueueJson("[{\"id\":\"dec-1\",\"key\":\"risk\",\"name\":\"Risk rating\",\"version\":2}]");

		Collection<Decision> decisions = decisionProvider.getDecisionDefinitionList(new HashMap<>(), user);

		assertThat(decisions).hasSize(1);
		Decision decision = decisions.iterator().next();
		assertThat(decision.getId()).isEqualTo("dec-1");
		assertThat(decision.getKey()).isEqualTo("risk");
		assertThat(decision.getVersion()).isEqualTo(2);
		assertThat(engine.takePath()).isEqualTo("/decision-definition");
	}

	@Test
	void getDecisionDefinitionList_appendsTheQueryParameters() throws Exception {
		engine.enqueueJson("[]");
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("latestVersion", "true");

		decisionProvider.getDecisionDefinitionList(queryParams, user);

		assertThat(engine.takePath()).isEqualTo("/decision-definition?latestVersion=true");
	}

	@Test
	void getDecisionDefinitionList_sendsTheAuthorizationHeaderAndUserId() throws Exception {
		engine.enqueueJson("[]");

		decisionProvider.getDecisionDefinitionList(new HashMap<>(), user);

		var request = engine.take();
		assertThat(request.getHeader("Authorization")).isEqualTo(MockEngineRest.AUTH_TOKEN);
		assertThat(request.getHeader("Context-User-ID")).isEqualTo("demo");
	}

	@Test
	void getDecisionDefinitionListCount_readsTheCountField() throws Exception {
		engine.enqueueJson("{\"count\":7}");

		assertThat(decisionProvider.getDecisionDefinitionListCount(new HashMap<>(), user)).isEqualTo(7L);
		assertThat(engine.takePath()).isEqualTo("/decision-definition/count");
	}

	@Test
	void getDecisionDefinitionByKey_asksForTheKeyEndpoint() throws Exception {
		engine.enqueueJson("{\"id\":\"dec-1\",\"key\":\"risk\"}");

		Decision decision = decisionProvider.getDecisionDefinitionByKey("risk", user);

		assertThat(decision.getId()).isEqualTo("dec-1");
		assertThat(engine.takePath()).isEqualTo("/decision-definition/key/risk");
	}

	@Test
	void getDecisionDefinitionByKeyAndTenant_usesTheTenantIdSegment() throws Exception {
		engine.enqueueJson("{\"id\":\"dec-1\",\"key\":\"risk\",\"tenantId\":\"acme\"}");

		Decision decision = decisionProvider.getDecisionDefinitionByKeyAndTenant("risk", "acme", user);

		assertThat(decision.getTenantId()).isEqualTo("acme");
		assertThat(engine.takePath()).isEqualTo("/decision-definition/key/risk/tenant-id/acme");
	}

	@Test
	void getDiagramByKeyAndTenant_usesTheTenantIdSegment() throws Exception {
		engine.enqueueJson("{}");

		decisionProvider.getDiagramByKeyAndTenant("risk", "acme", user);

		assertThat(engine.takePath()).isEqualTo("/decision-definition/key/risk/tenant-id/acme/diagram");
	}

	@Test
	void updateHistoryTTLByKeyAndTenant_usesTheTenantIdSegment() throws Exception {
		engine.enqueueEmpty(204);
		Map<String, Object> data = new HashMap<>();
		data.put("historyTimeToLive", 30);

		decisionProvider.updateHistoryTTLByKeyAndTenant(data, "risk", "acme", user);

		assertThat(engine.takePath()).isEqualTo("/decision-definition/key/risk/tenant-id/acme/history-time-to-live");
	}

	@Test
	void getXmlByKeyAndTenant_usesTheTenantIdSegment() throws Exception {
		engine.enqueueJson("{\"dmnXml\":\"<definitions/>\"}");

		decisionProvider.getXmlByKeyAndTenant("risk", "acme", user);

		assertThat(engine.takePath()).isEqualTo("/decision-definition/key/risk/tenant-id/acme/xml");
	}

	@Test
	void evaluateDecisionDefinitionByKeyAndTenant_buildsAMalformedTenantUrl() throws Exception {
		engine.enqueueJson("[]");

		decisionProvider.evaluateDecisionDefinitionByKeyAndTenant(new HashMap<>(), "risk", "acme", user);

		// KNOWN BUG (pinned, not fixed): every other tenant-scoped method in this provider builds
		// ".../key/{key}/tenant-id/{tenant}/...", but this one concatenates "/tenant" + tenant with
		// no "-id/" and no separator, so it requests "/tenant-idacme"-style nonsense and the engine
		// answers 404. Evaluating a tenant-scoped decision cannot work through this method.
		// The correct path would be "/decision-definition/key/risk/tenant-id/acme/evaluate".
		assertThat(engine.takePath()).isEqualTo("/decision-definition/key/risk/tenantacme/evaluate");
	}

	@Test
	void evaluateDecisionDefinitionByKey_postsToTheEvaluateEndpoint() throws Exception {
		engine.enqueueJson("[{\"risk\":{\"value\":\"high\",\"type\":\"String\"}}]");
		Map<String, Object> data = new HashMap<>();
		data.put("variables", new HashMap<>());

		decisionProvider.evaluateDecisionDefinitionByKey(data, "risk", user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).endsWith("/decision-definition/key/risk/evaluate");
	}

	@Test
	void getDecisionDefinitionById_asksForTheIdEndpoint() throws Exception {
		engine.enqueueJson("{\"id\":\"dec-1\",\"key\":\"risk\"}");

		decisionProvider.getDecisionDefinitionById("dec-1", java.util.Optional.empty(), user);

		assertThat(engine.takePath()).startsWith("/decision-definition/dec-1");
	}
}
