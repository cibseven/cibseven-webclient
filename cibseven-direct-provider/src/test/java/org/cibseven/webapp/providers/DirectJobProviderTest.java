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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.bpm.engine.HistoryService;
import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.exception.NotFoundException;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.runtime.JobQuery;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import tools.jackson.databind.ObjectMapper;

public class DirectJobProviderTest {

	private DirectProviderUtil directProviderUtil;
	private ManagementService managementService;
	private HistoryService historyService;
	private DirectJobProvider jobProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		user = new CIBUser("testUser");

		ProcessEngine processEngine = mock(ProcessEngine.class);
		managementService = mock(ManagementService.class);
		historyService = mock(HistoryService.class);
		when(processEngine.getManagementService()).thenReturn(managementService);
		when(processEngine.getHistoryService()).thenReturn(historyService);

		ObjectMapper objectMapper = JacksonConfigurator.configureObjectMapper();

		directProviderUtil = Mockito.spy(new DirectProviderUtil());
		doReturn(processEngine).when(directProviderUtil).getProcessEngine(any(CIBUser.class));
		doReturn(objectMapper).when(directProviderUtil).getObjectMapper(any(CIBUser.class));

		jobProvider = new DirectJobProvider(directProviderUtil);
	}

	@Test
	void getJobs_pagesAndFiltersWithoutThePagingKeys() {
		JobQuery query = mock(JobQuery.class, withSettings().defaultAnswer(RETURNS_SELF));
		when(managementService.createJobQuery()).thenReturn(query);
		when(query.listPage(10, 25)).thenReturn(Collections.emptyList());

		Map<String, Object> params = new HashMap<>();
		params.put("firstResult", "10");
		params.put("maxResults", "25");
		params.put("processInstanceId", "pi-1");

		assertThat(jobProvider.getJobs(params, user)).isEmpty();
		verify(query).processInstanceId("pi-1");
		verify(query).listPage(10, 25);
		verify(query, never()).list();
	}

	@Test
	void deleteJob_delegatesToTheManagementService() {
		jobProvider.deleteJob("job-1", user);

		verify(managementService).deleteJob("job-1");
	}

	@Test
	void deleteJob_letsAuthorizationFailuresThrough() {
		doThrow(new AuthorizationException("denied")).when(managementService).deleteJob("job-1");

		assertThatThrownBy(() -> jobProvider.deleteJob("job-1", user))
			.isInstanceOf(AuthorizationException.class);
	}

	@Test
	void deleteJob_wrapsOtherEngineFailures() {
		doThrow(new ProcessEngineException("boom")).when(managementService).deleteJob("job-1");

		assertThatThrownBy(() -> jobProvider.deleteJob("job-1", user)).isInstanceOf(SystemException.class);
	}

	@Test
	void getHistoryJobLogStacktrace_returnsTheStacktrace() {
		when(historyService.getHistoricJobLogExceptionStacktrace("job-1")).thenReturn("at Foo.bar()");

		assertThat(jobProvider.getHistoryJobLogStacktrace("job-1", user)).isEqualTo("at Foo.bar()");
	}

	@Test
	void getHistoryJobLogStacktrace_wrapsEngineFailures() {
		when(historyService.getHistoricJobLogExceptionStacktrace("job-1"))
			.thenThrow(new ProcessEngineException("boom"));

		assertThatThrownBy(() -> jobProvider.getHistoryJobLogStacktrace("job-1", user))
			.isInstanceOf(SystemException.class);
	}

	@Test
	void recalculateDueDate_defaultsToNotCreationDateBased() {
		jobProvider.recalculateDueDate("job-1", new HashMap<>(), user);

		verify(managementService).recalculateJobDuedate("job-1", false);
	}

	@Test
	void recalculateDueDate_honoursTheCreationDateBasedFlag() {
		Map<String, Object> params = new HashMap<>();
		params.put("creationDateBased", "true");

		jobProvider.recalculateDueDate("job-1", params, user);

		verify(managementService).recalculateJobDuedate("job-1", true);
	}

	/** A caller from code passes a real Boolean, not the string REST delivers (CIB7-2806). */
	@Test
	void recalculateDueDate_acceptsABooleanFlag() {
		Map<String, Object> params = new HashMap<>();
		params.put("creationDateBased", Boolean.TRUE);

		jobProvider.recalculateDueDate("job-1", params, user);

		verify(managementService).recalculateJobDuedate("job-1", true);
	}

	@Test
	void recalculateDueDate_treatsANullFlagAsFalse() {
		Map<String, Object> params = new HashMap<>();
		params.put("creationDateBased", null);

		jobProvider.recalculateDueDate("job-1", params, user);

		verify(managementService).recalculateJobDuedate("job-1", false);
	}

	@Test
	void recalculateDueDate_translatesAnUnknownJobIntoSystemException() {
		doThrow(new NotFoundException("gone")).when(managementService).recalculateJobDuedate("missing", false);

		// deliberately rewritten from the engine's 400 to something the REST layer maps to 404
		assertThatThrownBy(() -> jobProvider.recalculateDueDate("missing", new HashMap<>(), user))
			.isInstanceOf(SystemException.class);
	}

	@Test
	void setSuspended_rejectsAJobDefinitionIdBecauseTheIdIsTakenFromThePath() {
		Map<String, Object> params = new HashMap<>();
		params.put("jobDefinitionId", "definition-1");

		assertThatThrownBy(() -> jobProvider.setSuspended("process-1", params, user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Either processDefinitionId or processDefinitionKey can be set");
	}
}
