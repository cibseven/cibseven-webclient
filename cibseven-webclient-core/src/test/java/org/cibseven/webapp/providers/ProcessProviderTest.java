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
import java.util.Optional;

import org.cibseven.webapp.Data;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProcessProviderTest {

	private MockEngineRest engine;
	private ProcessProvider processProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		processProvider = engine.wire(new ProcessProvider());
		user = MockEngineRest.user();
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	// ---------- definitions ----------

	@Test
	void findProcesses_asksForTheLatestVersionsSortedByName() throws Exception {
		engine.enqueueJson("[{\"id\":\"id-1\",\"key\":\"invoice\",\"name\":\"Invoice Receipt\",\"version\":3}]");

		Collection<Process> processes = processProvider.findProcesses(user);

		assertThat(processes).hasSize(1);
		assertThat(processes.iterator().next().getKey()).isEqualTo("invoice");
		assertThat(engine.takePath())
			.isEqualTo("/process-definition?latestVersion=true&sortBy=name&sortOrder=desc");
	}

	@Test
	void findProcessByDefinitionKey_withoutTenantAsksTheKeyEndpoint() throws Exception {
		engine.enqueueJson("{\"id\":\"id-1\",\"key\":\"invoice\"}");

		Process process = processProvider.findProcessByDefinitionKey("invoice", null, user);

		assertThat(process.getId()).isEqualTo("id-1");
		assertThat(engine.takePath()).isEqualTo("/process-definition/key/invoice");
	}

	@Test
	void findProcessByDefinitionKey_appendsTheTenantSegment() throws Exception {
		engine.enqueueJson("{\"id\":\"id-1\",\"key\":\"invoice\",\"tenantId\":\"acme\"}");

		processProvider.findProcessByDefinitionKey("invoice", "acme", user);

		assertThat(engine.takePath()).isEqualTo("/process-definition/key/invoice/tenant-id/acme");
	}

	@Test
	void findProcessVersionsByDefinitionKey_excludesTenantedVersionsWhenNoTenantIsGiven() throws Exception {
		engine.enqueueJson("[]");

		processProvider.findProcessVersionsByDefinitionKey("invoice", null, Optional.of(true), user);

		assertThat(engine.takePath()).isEqualTo(
			"/process-definition?key=invoice&sortBy=version&sortOrder=desc&withoutTenantId=true");
	}

	@Test
	void findProcessVersionsByDefinitionKey_filtersByTenantWhenGiven() throws Exception {
		engine.enqueueJson("[]");

		processProvider.findProcessVersionsByDefinitionKey("invoice", "acme", Optional.of(true), user);

		assertThat(engine.takePath()).isEqualTo(
			"/process-definition?key=invoice&sortBy=version&sortOrder=desc&tenantIdIn=acme");
	}

	@Test
	void findProcessVersionsByDefinitionKey_lazyLoadSkipsTheInstanceCounts() throws Exception {
		engine.enqueueJson("[{\"id\":\"id-1\",\"key\":\"invoice\",\"version\":1}]");

		processProvider.findProcessVersionsByDefinitionKey("invoice", null, Optional.of(true), user);

		// only the version list; no per-version history counts
		assertThat(engine.requestCount()).isEqualTo(1);
	}

	@Test
	void findProcessById_asksForTheDefinitionById() throws Exception {
		engine.enqueueJson("{\"id\":\"id-1\",\"key\":\"invoice\"}");

		Process process = processProvider.findProcessById("id-1", Optional.empty(), user);

		assertThat(process.getId()).isEqualTo("id-1");
		assertThat(engine.takePath()).isEqualTo("/process-definition/id-1");
	}

	@Test
	void findProcessesWithFilters_urlDecodesTheFilterString() throws Exception {
		engine.enqueueJson("[]");

		processProvider.findProcessesWithFilters("name=Invoice%20Receipt", user);

		assertThat(engine.take().getPath()).contains("name=Invoice");
	}

	// ---------- diagrams ----------

	@Test
	void fetchDiagram_readsTheBpmnXml() throws Exception {
		engine.enqueueJson("{\"id\":\"id-1\",\"bpmn20Xml\":\"<definitions/>\"}");

		ProcessDiagram diagram = processProvider.fetchDiagram("id-1", user);

		assertThat(diagram.getBpmn20Xml()).isEqualTo("<definitions/>");
		assertThat(engine.takePath()).isEqualTo("/process-definition/id-1/xml");
	}

	@Test
	void downloadBpmn_wrapsTheDiagramAsANamedAttachment() throws Exception {
		engine.enqueueJson("{\"id\":\"id-1\",\"bpmn20Xml\":\"<definitions/>\"}");

		Data data = processProvider.downloadBpmn("id-1", "invoice.bpmn", user);

		assertThat(data.getName()).isEqualTo("invoice.bpmn");
		assertThat(data.getContentType()).isEqualTo("application/bpmn+xml");
		assertThat(data.getSize()).isEqualTo("<definitions/>".length());
	}

	// ---------- instances ----------

	@Test
	void findProcessesInstances_filtersByDefinitionKeyAndFlagsIncidents() throws Exception {
		engine.enqueueJson("[{\"id\":\"pi-1\"},{\"id\":\"pi-2\"}]");
		// the follow-up query returns only the instances that do have an incident
		engine.enqueueJson("[{\"id\":\"pi-2\"}]");

		Collection<ProcessInstance> instances = processProvider.findProcessesInstances("invoice", user);

		assertThat(instances).hasSize(2);
		assertThat(instances).extracting(ProcessInstance::getId, ProcessInstance::getWithIncident)
			.containsExactly(tuple("pi-1", Boolean.FALSE), tuple("pi-2", Boolean.TRUE));
		assertThat(engine.takePath()).isEqualTo("/process-instance?processDefinitionKey=invoice");
		assertThat(engine.takePath()).isEqualTo("/process-instance");
	}

	@Test
	void findProcessesInstances_skipsTheIncidentQueryWhenThereAreNoInstances() throws Exception {
		engine.enqueueJson("[]");

		assertThat(processProvider.findProcessesInstances("invoice", user)).isEmpty();

		// no point asking which of zero instances have incidents
		assertThat(engine.requestCount()).isEqualTo(1);
	}

	@Test
	void findProcessInstance_asksForTheInstanceAndWhetherItHasAnIncident() throws Exception {
		engine.enqueueJson("{\"id\":\"pi-1\"}");
		engine.enqueueJson("[{\"id\":\"pi-1\"}]");

		ProcessInstance instance = processProvider.findProcessInstance("pi-1", user);

		assertThat(instance.getWithIncident()).isTrue();
		assertThat(engine.takePath()).isEqualTo("/process-instance/pi-1");
		assertThat(engine.takePath()).isEqualTo("/process-instance");
	}

	@Test
	void findProcessInstance_reportsNoIncidentWhenTheFollowUpFindsNone() throws Exception {
		engine.enqueueJson("{\"id\":\"pi-1\"}");
		engine.enqueueJson("[]");

		ProcessInstance instance = processProvider.findProcessInstance("pi-1", user);

		assertThat(instance.getWithIncident()).isFalse();
	}

	@Test
	void deleteProcessInstance_deletesTheInstance() throws Exception {
		engine.enqueueEmpty(204);

		processProvider.deleteProcessInstance("pi-1", user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("DELETE");
		assertThat(request.getPath()).endsWith("/process-instance/pi-1");
	}

	@Test
	void suspendProcessInstance_putsTheSuspendedFlag() throws Exception {
		engine.enqueueEmpty(204);

		processProvider.suspendProcessInstance("pi-1", Boolean.TRUE, user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getPath()).endsWith("/process-instance/pi-1/suspended");
		assertThat(request.getBody().readUtf8()).isEqualTo("{ \"suspended\": true }");
	}

	@Test
	void suspendProcessDefinition_sendsValidJsonWhenThereIsNoExecutionDate() throws Exception {
		engine.enqueueEmpty(204);

		processProvider.suspendProcessDefinition("id-1", Boolean.TRUE, Boolean.FALSE, null, user);

		String body = engine.take().getBody().readUtf8();
		assertThat(body).isEqualTo(
			"{ \"suspended\": true,\"includeProcessInstances\": false,\"executionDate\": null }");
	}

	@Test
	void suspendProcessDefinition_buildsInvalidJsonForAnExecutionDate() throws Exception {
		engine.enqueueEmpty(204);

		processProvider.suspendProcessDefinition("id-1", Boolean.TRUE, Boolean.FALSE, "2026-01-01T10:00:00", user);

		// KNOWN BUG (pinned, not fixed): the body is assembled by string concatenation and the
		// executionDate is interpolated unquoted, so a non-null date produces malformed JSON
		// (`"executionDate": 2026-01-01T10:00:00`) that the engine rejects. Scheduling a delayed
		// suspension through this method cannot work. Only the null case happens to be valid JSON.
		String body = engine.take().getBody().readUtf8();
		assertThat(body).contains("\"executionDate\": 2026-01-01T10:00:00");
		assertThat(body).doesNotContain("\"executionDate\": \"2026-01-01T10:00:00\"");
	}

	// ---------- counts and history ----------

	@Test
	void countProcessesInstancesHistory_postsTheFilterAndReadsTheCount() throws Exception {
		engine.enqueueJson("{\"count\":12}");

		Long count = processProvider.countProcessesInstancesHistory(new HashMap<>(), user);

		assertThat(count).isEqualTo(12L);
		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).endsWith("/history/process-instance/count");
	}

	@Test
	void countProcessesInstancesRuntime_postsToTheRuntimeCountEndpoint() throws Exception {
		engine.enqueueJson("{\"count\":4}");

		assertThat(processProvider.countProcessesInstancesRuntime(new HashMap<>(), user)).isEqualTo(4L);
		assertThat(engine.takePath()).isEqualTo("/process-instance/count");
	}

	@Test
	void findHistoryProcessInstanceHistory_asksTheHistoryEndpoint() throws Exception {
		engine.enqueueJson("{\"id\":\"pi-1\",\"state\":\"COMPLETED\"}");

		HistoryProcessInstance instance = processProvider.findHistoryProcessInstanceHistory("pi-1", user);

		assertThat(instance.getState()).isEqualTo("COMPLETED");
		assertThat(engine.takePath()).isEqualTo("/history/process-instance/pi-1");
	}

	@Test
	void findProcessStatistics_asksForFailedJobsAndIncidents() throws Exception {
		engine.enqueueJson("[]");

		processProvider.findProcessStatistics("id-1", user);

		assertThat(engine.takePath())
			.isEqualTo("/process-definition/id-1/statistics?failedJobs=true&incidents=true");
	}

	@Test
	void findCalledProcessDefinitions_asksForTheStaticallyCalledDefinitions() throws Exception {
		engine.enqueueJson("[]");

		processProvider.findCalledProcessDefinitions("id-1", user);

		assertThat(engine.takePath())
			.isEqualTo("/process-definition/id-1/static-called-process-definitions");
	}

	@Test
	void fetchStartForm_asksForTheStartFormKey() throws Exception {
		engine.enqueueJson("{\"key\":\"embedded:app:forms/start.html\"}");

		processProvider.fetchStartForm("id-1", user);

		assertThat(engine.takePath()).isEqualTo("/process-definition/id-1/startForm");
	}
}
