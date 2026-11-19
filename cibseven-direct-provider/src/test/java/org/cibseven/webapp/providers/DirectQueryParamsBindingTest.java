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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineConfiguration;
import org.cibseven.bpm.engine.identity.Tenant;
import org.cibseven.bpm.engine.rest.dto.history.HistoricJobLogDto;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.runtime.Job;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidQueryParameterException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.rest.model.Decision;
import org.cibseven.webapp.rest.model.ExternalTask;
import org.cibseven.webapp.rest.model.HistoricDecisionInstance;
import org.cibseven.webapp.rest.model.Incident;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CIB7-2724: the GET endpoints of the direct provider bind their query parameters the way engine-rest
 * does, so a comma-separated {@code ...In} list filters instead of failing or matching nothing.
 * <p>
 * The same process and decision are deployed for the tenants a, b and c; each test asks for
 * {@code tenantIdIn=a,b} as the query string carries it and expects exactly a and b.
 */
class DirectQueryParamsBindingTest {

	private static final String ENGINE_NAME = "queryparamsbinding";
	private static final List<String> TENANTS = List.of("a", "b", "c");
	private static final Map<String, Object> TENANTS_A_AND_B = Map.of("tenantIdIn", "a,b");

	private static final String PROCESS_BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
		    xmlns:camunda="http://camunda.org/schema/1.0/bpmn" id="definitions" targetNamespace="http://bpmn.io/schema/bpmn">
		  <bpmn:process id="order" isExecutable="true" camunda:historyTimeToLive="7">
		    <bpmn:startEvent id="start"/>
		    <bpmn:sequenceFlow id="f1" sourceRef="start" targetRef="work"/>
		    <bpmn:serviceTask id="work" camunda:type="external" camunda:topic="work" camunda:asyncBefore="true"/>
		    <bpmn:sequenceFlow id="f2" sourceRef="work" targetRef="end"/>
		    <bpmn:endEvent id="end"/>
		  </bpmn:process>
		</bpmn:definitions>
		""";

	private static final String RISK_DMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/" xmlns:camunda="http://camunda.org/schema/1.0/dmn"
		    id="risk-definitions" name="Risk" namespace="http://camunda.org/schema/1.0/dmn">
		  <decision id="risk" name="Risk" camunda:historyTimeToLive="7">
		    <decisionTable id="table">
		      <input id="amount"><inputExpression id="ie" typeRef="integer"><text>amount</text></inputExpression></input>
		      <output id="level" name="level" typeRef="string"/>
		      <rule id="rule-1"><inputEntry id="i1"><text></text></inputEntry><outputEntry id="o1"><text>"low"</text></outputEntry></rule>
		    </decisionTable>
		  </decision>
		</definitions>
		""";

	private ProcessEngine engine;
	private DirectProviderUtil providerUtil;
	private CIBUser user;

	@BeforeEach
	void start() {
		engine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
			.setJdbcUrl("jdbc:h2:mem:" + ENGINE_NAME + ";DB_CLOSE_DELAY=1000")
			.setProcessEngineName(ENGINE_NAME)
			.setHistory(ProcessEngineConfiguration.HISTORY_FULL)
			.setJobExecutorActivate(false)
			.buildProcessEngine();
		providerUtil = new DirectProviderUtil();
		providerUtil.processEngines.put(engine.getName(), engine);
		providerUtil.objectMappers.put(engine.getName(), JacksonConfigurator.configureObjectMapper());
		user = new CIBUser("demo");
		user.setEngine(ENGINE_NAME);

		for (String tenant : TENANTS) {
			engine.getRepositoryService().createDeployment().tenantId(tenant)
				.addString("order.bpmn", PROCESS_BPMN)
				.addString("risk.dmn", RISK_DMN)
				.deploy();
			engine.getRuntimeService().createProcessInstanceByKey("order").processDefinitionTenantId(tenant).execute();
			engine.getDecisionService().evaluateDecisionTableByKey("risk").decisionDefinitionTenantId(tenant)
				.variables(Map.of("amount", 1)).evaluate();
			Tenant engineTenant = engine.getIdentityService().newTenant(tenant);
			engineTenant.setName("Tenant " + tenant);
			engine.getIdentityService().saveTenant(engineTenant);
		}
		// runs the async continuation: a job log per tenant, and the external task waits
		for (Job job : engine.getManagementService().createJobQuery().list())
			engine.getManagementService().executeJob(job.getId());
		for (org.cibseven.bpm.engine.externaltask.ExternalTask externalTask : engine.getExternalTaskService().createExternalTaskQuery().list())
			engine.getRuntimeService().createIncident("custom", externalTask.getExecutionId(), "configuration");
	}

	@AfterEach
	void tearDown() {
		engine.close();
	}

	@Test
	void historicActivitiesAreFilteredByACommaSeparatedTenantList() {
		DirectActivityProvider provider = new DirectActivityProvider(providerUtil);

		assertThat(provider.findActivitiesInstancesHistory(TENANTS_A_AND_B, user))
			.isNotEmpty()
			.extracting(ActivityInstanceHistory::getTenantId)
			.containsOnly("a", "b")
			.contains("a", "b");
	}

	@Test
	void historicActivitiesOfAProcessDefinitionAreFilteredByACommaSeparatedTenantList() {
		DirectActivityProvider provider = new DirectActivityProvider(providerUtil);
		String definitionOfA = engine.getRepositoryService().createProcessDefinitionQuery().tenantIdIn("a").singleResult().getId();

		assertThat(provider.findActivitiesProcessDefinitionHistory(definitionOfA, TENANTS_A_AND_B, user))
			.isNotEmpty()
			.extracting(ActivityInstanceHistory::getTenantId)
			.containsOnly("a");
		assertThat(provider.findActivitiesProcessDefinitionHistory(definitionOfA, Map.of("tenantIdIn", "b,c"), user)).isEmpty();
	}

	@Test
	void decisionDefinitionsAreFilteredByACommaSeparatedTenantList() {
		DirectDecisionProvider provider = new DirectDecisionProvider(providerUtil);

		assertThat(provider.getDecisionDefinitionList(TENANTS_A_AND_B, user))
			.extracting(Decision::getTenantId)
			.containsExactlyInAnyOrder("a", "b");
		assertThat(provider.getDecisionDefinitionListCount(TENANTS_A_AND_B, user)).isEqualTo(2L);
	}

	@Test
	void historicDecisionInstancesAreFilteredByACommaSeparatedTenantList() {
		DirectDecisionProvider provider = new DirectDecisionProvider(providerUtil);

		assertThat(provider.getHistoricDecisionInstances(TENANTS_A_AND_B, user))
			.extracting(HistoricDecisionInstance::getTenantId)
			.containsExactlyInAnyOrder("a", "b");
		assertThat(provider.getHistoricDecisionInstanceCount(TENANTS_A_AND_B, user)).isEqualTo(2L);
	}

	@Test
	void aHistoricDecisionInstanceByIdHonoursACommaSeparatedTenantList() {
		DirectDecisionProvider provider = new DirectDecisionProvider(providerUtil);
		String instanceOfA = engine.getHistoryService().createHistoricDecisionInstanceQuery().tenantIdIn("a").singleResult().getId();

		assertThat(provider.getHistoricDecisionInstanceById(instanceOfA, TENANTS_A_AND_B, user).getTenantId()).isEqualTo("a");
		assertThatThrownBy(() -> provider.getHistoricDecisionInstanceById(instanceOfA, Map.of("tenantIdIn", "b,c"), user))
			.isInstanceOf(NoObjectFoundException.class);
	}

	@Test
	void externalTasksAreFilteredByACommaSeparatedTenantList() {
		DirectExternalTaskProvider provider = new DirectExternalTaskProvider(providerUtil);

		assertThat(provider.getExternalTasks(TENANTS_A_AND_B, user))
			.extracting(ExternalTask::getTenantId)
			.containsExactlyInAnyOrder("a", "b");
	}

	@Test
	void incidentsAreFilteredByACommaSeparatedTenantList() {
		DirectIncidentProvider provider = new DirectIncidentProvider(providerUtil);

		assertThat(provider.findIncident(TENANTS_A_AND_B, user))
			.extracting(Incident::getTenantId)
			.containsExactlyInAnyOrder("a", "b");
		assertThat(provider.countIncident(TENANTS_A_AND_B, user)).isEqualTo(2L);
	}

	@Test
	void historicIncidentsAreFilteredByACommaSeparatedTenantList() {
		DirectIncidentProvider provider = new DirectIncidentProvider(providerUtil);

		assertThat(provider.findHistoricIncidents(TENANTS_A_AND_B, user))
			.extracting(Incident::getTenantId)
			.containsExactlyInAnyOrder("a", "b");
		assertThat(provider.countHistoricIncident(TENANTS_A_AND_B, user)).isEqualTo(2L);
	}

	@Test
	void historicJobLogsAreFilteredByACommaSeparatedTenantList() {
		DirectJobProvider provider = new DirectJobProvider(providerUtil);

		Collection<Object> jobLogs = provider.getHistoryJobLog(TENANTS_A_AND_B, user);

		assertThat(jobLogs)
			.isNotEmpty()
			.extracting(jobLog -> ((HistoricJobLogDto) jobLog).getTenantId())
			.containsOnly("a", "b")
			.contains("a", "b");
	}

	/**
	 * {@code sortOrder} without {@code sortBy} binds, but the engine rejects it when it builds the query
	 * ({@code toQuery}); engine-rest answers that with 400, so every GET endpoint must as well.
	 */
	@Test
	void everyGetEndpointAnswersASortOrderWithoutSortByWithABadRequest() {
		DirectActivityProvider activities = new DirectActivityProvider(providerUtil);
		DirectDecisionProvider decisions = new DirectDecisionProvider(providerUtil);
		DirectExternalTaskProvider externalTasks = new DirectExternalTaskProvider(providerUtil);
		DirectIncidentProvider incidents = new DirectIncidentProvider(providerUtil);
		DirectJobProvider jobs = new DirectJobProvider(providerUtil);
		DirectTenantProvider tenants = new DirectTenantProvider(providerUtil);
		String definitionOfA = engine.getRepositoryService().createProcessDefinitionQuery().tenantIdIn("a").singleResult().getId();
		String decisionInstanceOfA = engine.getHistoryService().createHistoricDecisionInstanceQuery().tenantIdIn("a").singleResult().getId();
		Map<String, Object> sortOrderOnly = Map.of("sortOrder", "asc");

		List<Runnable> calls = List.of(
			() -> activities.findActivitiesInstancesHistory(sortOrderOnly, user),
			() -> activities.findActivitiesProcessDefinitionHistory(definitionOfA, sortOrderOnly, user),
			() -> decisions.getDecisionDefinitionList(sortOrderOnly, user),
			() -> decisions.getDecisionDefinitionListCount(sortOrderOnly, user),
			() -> decisions.getHistoricDecisionInstances(sortOrderOnly, user),
			() -> decisions.getHistoricDecisionInstanceCount(sortOrderOnly, user),
			() -> decisions.getHistoricDecisionInstanceById(decisionInstanceOfA, sortOrderOnly, user),
			() -> externalTasks.getExternalTasks(sortOrderOnly, user),
			() -> incidents.countIncident(sortOrderOnly, user),
			() -> incidents.countHistoricIncident(sortOrderOnly, user),
			() -> incidents.findIncident(sortOrderOnly, user),
			() -> incidents.findHistoricIncidents(sortOrderOnly, user),
			() -> jobs.getHistoryJobLog(sortOrderOnly, user),
			() -> tenants.fetchTenants(sortOrderOnly, user));

		for (Runnable call : calls)
			assertThatThrownBy(call::run)
				.isInstanceOf(InvalidQueryParameterException.class)
				.hasMessageContaining("sortBy");
	}

	@Test
	void tenantsAreSortedAsTheQueryStringAsks() {
		// TenantQueryDto has no list parameter; the sorting shows that the parameters are bound
		DirectTenantProvider provider = new DirectTenantProvider(providerUtil);

		assertThat(provider.fetchTenants(Map.of("nameLike", "Tenant %", "sortBy", "name", "sortOrder", "desc"), user))
			.extracting(org.cibseven.webapp.rest.model.Tenant::getId)
			.containsExactly("c", "b", "a");
	}
}
