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
import java.util.List;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Incident;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IncidentProviderTest {

	private MockEngineRest engine;
	private IncidentProvider incidentProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		incidentProvider = engine.wire(new IncidentProvider());
		user = MockEngineRest.user();
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	@Test
	void countIncident_readsTheCountField() throws Exception {
		engine.enqueueJson("{\"count\":3}");

		assertThat(incidentProvider.countIncident(new HashMap<>(), user)).isEqualTo(3L);
		assertThat(engine.takePath()).isEqualTo("/incident/count");
	}

	@Test
	void countIncident_returnsZeroWhenTheEngineSendsNoBody() throws Exception {
		engine.enqueueEmpty(204);

		assertThat(incidentProvider.countIncident(new HashMap<>(), user)).isZero();
	}

	@Test
	void countHistoricIncident_asksTheHistoryEndpoint() throws Exception {
		engine.enqueueJson("{\"count\":9}");

		assertThat(incidentProvider.countHistoricIncident(new HashMap<>(), user)).isEqualTo(9L);
		assertThat(engine.takePath()).isEqualTo("/history/incident/count");
	}

	@Test
	void findIncident_mapsTheEngineResponse() throws Exception {
		engine.enqueueJson("[{\"id\":\"inc-1\",\"processInstanceId\":\"pi-1\","
			+ "\"incidentType\":\"failedJob\",\"incidentMessage\":\"boom\"}]");

		Collection<Incident> incidents = incidentProvider.findIncident(new HashMap<>(), user);

		assertThat(incidents).hasSize(1);
		Incident incident = incidents.iterator().next();
		assertThat(incident.getId()).isEqualTo("inc-1");
		assertThat(incident.getIncidentType()).isEqualTo("failedJob");
		assertThat(engine.takePath()).isEqualTo("/incident");
	}

	@Test
	void findIncident_returnsEmptyWhenTheEngineSendsNoBody() throws Exception {
		engine.enqueueEmpty(204);

		assertThat(incidentProvider.findIncident(new HashMap<>(), user)).isEmpty();
	}

	@Test
	void findIncident_doesNotFetchARootCauseWhenTheIncidentIsItsOwnRootCause() throws Exception {
		engine.enqueueJson("[{\"id\":\"inc-1\",\"rootCauseIncidentId\":\"inc-1\"}]");

		incidentProvider.findIncident(new HashMap<>(), user);

		// only the list call - a self-referencing root cause needs no second round-trip
		assertThat(engine.requestCount()).isEqualTo(1);
	}

	@Test
	void findIncident_enrichesTheIncidentFromItsRootCause() throws Exception {
		engine.enqueueJson("[{\"id\":\"inc-1\",\"rootCauseIncidentId\":\"inc-root\"}]");
		engine.enqueueJson("{\"id\":\"inc-root\",\"processInstanceId\":\"pi-root\","
			+ "\"activityId\":\"serviceTask\",\"incidentMessage\":\"the real reason\"}");

		Collection<Incident> incidents = incidentProvider.findIncident(new HashMap<>(), user);

		Incident incident = incidents.iterator().next();
		assertThat(incident.getRootCauseIncidentProcessInstanceId()).isEqualTo("pi-root");
		assertThat(incident.getRootCauseIncidentActivityId()).isEqualTo("serviceTask");
		assertThat(incident.getRootCauseIncidentMessage()).isEqualTo("the real reason");
		assertThat(engine.takePath()).isEqualTo("/incident");
		assertThat(engine.takePath()).isEqualTo("/incident/inc-root");
	}

	@Test
	void findIncident_stillReturnsTheIncidentWhenTheRootCauseLookupFails() throws Exception {
		engine.enqueueJson("[{\"id\":\"inc-1\",\"rootCauseIncidentId\":\"inc-root\"}]");
		engine.enqueueStatus(500, "{\"message\":\"engine exploded\"}");

		Collection<Incident> incidents = incidentProvider.findIncident(new HashMap<>(), user);

		// enrichment is best-effort: a failure is logged, not propagated
		assertThat(incidents).hasSize(1);
		assertThat(incidents.iterator().next().getRootCauseIncidentMessage()).isNull();
	}

	@Test
	void findIncidentByInstanceId_filtersByProcessInstance() throws Exception {
		engine.enqueueJson("[{\"id\":\"inc-1\"}]");

		List<Incident> incidents = incidentProvider.findIncidentByInstanceId("pi-1", user);

		assertThat(incidents).hasSize(1);
		assertThat(engine.takePath()).isEqualTo("/incident?processInstanceId=pi-1");
	}

	@Test
	void fetchIncidents_filtersByProcessDefinitionKey() throws Exception {
		engine.enqueueJson("[]");

		incidentProvider.fetchIncidents("invoice", user);

		assertThat(engine.takePath()).isEqualTo("/incident?processDefinitionKeyIn=invoice");
	}

	@Test
	void setIncidentAnnotation_putsTheAnnotation() throws Exception {
		engine.enqueueEmpty(204);
		Map<String, Object> data = new HashMap<>();
		data.put("annotation", "looked at");

		incidentProvider.setIncidentAnnotation("inc-1", data, user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getPath()).endsWith("/incident/inc-1/annotation");
	}

	@Test
	void retryExternalTask_putsToTheRetriesEndpoint() throws Exception {
		engine.enqueueEmpty(204);
		Map<String, Object> data = new HashMap<>();
		data.put("retries", 3);

		incidentProvider.retryExternalTask("task-1", data, user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getPath()).endsWith("/external-task/task-1/retries");
	}

	@Test
	void findExternalTaskErrorDetails_returnsThePlainBody() throws Exception {
		engine.enqueueJson("java.lang.RuntimeException: boom");

		String details = incidentProvider.findExternalTaskErrorDetails("task-1", user);

		assertThat(details).isEqualTo("java.lang.RuntimeException: boom");
		assertThat(engine.takePath()).isEqualTo("/external-task/task-1/errorDetails");
	}

	@Test
	void findHistoricExternalTaskErrorDetails_asksTheHistoryEndpoint() throws Exception {
		engine.enqueueJson("boom");

		incidentProvider.findHistoricExternalTaskErrorDetails("task-1", user);

		assertThat(engine.takePath()).isEqualTo("/history/external-task-log/task-1/error-details");
	}

	@Test
	void findHistoricIncidents_mapsTheHistoryResponse() throws Exception {
		engine.enqueueJson("[{\"id\":\"inc-1\",\"incidentType\":\"failedJob\"}]");

		Collection<Incident> incidents = incidentProvider.findHistoricIncidents(new HashMap<>(), user);

		assertThat(incidents).hasSize(1);
		assertThat(engine.takePath()).isEqualTo("/history/incident");
	}
}
