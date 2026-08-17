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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.runtime.ProcessInstanceQuery;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.rest.model.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DirectProcessProviderTest {

	private DirectProviderUtil directProviderUtil;
	private RuntimeService runtimeService;
	private ProcessInstanceQuery query;
	private DirectProcessProvider processProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		user = new CIBUser("testUser");

		ProcessEngine processEngine = mock(ProcessEngine.class);
		runtimeService = mock(RuntimeService.class);
		when(processEngine.getRuntimeService()).thenReturn(runtimeService);

		query = mock(ProcessInstanceQuery.class, withSettings().defaultAnswer(RETURNS_SELF));
		when(runtimeService.createProcessInstanceQuery()).thenReturn(query);

		ObjectMapper objectMapper = new ObjectMapper();
		JacksonConfigurator.configureObjectMapper(objectMapper);

		directProviderUtil = Mockito.spy(new DirectProviderUtil());
		doReturn(processEngine).when(directProviderUtil).getProcessEngine(any(CIBUser.class));
		doReturn(objectMapper).when(directProviderUtil).getObjectMapper(any(CIBUser.class));

		processProvider = new DirectProcessProvider(directProviderUtil, mock(SevenDirectProvider.class));
	}

	private org.cibseven.bpm.engine.runtime.ProcessInstance mockEngineInstance(String id) {
		org.cibseven.bpm.engine.runtime.ProcessInstance instance = mock(org.cibseven.bpm.engine.runtime.ProcessInstance.class);
		when(instance.getId()).thenReturn(id);
		when(instance.getProcessDefinitionId()).thenReturn("processDefinition1");
		when(instance.getProcessDefinitionKey()).thenReturn("processKey1");
		when(instance.getBusinessKey()).thenReturn("businessKey1");
		return instance;
	}

	@Test
	void findCurrentProcessesInstances_flagsOnlyInstancesWithOpenIncidents() {
		org.cibseven.bpm.engine.runtime.ProcessInstance withIncident = mockEngineInstance("instance-1");
		org.cibseven.bpm.engine.runtime.ProcessInstance withoutIncident = mockEngineInstance("instance-2");

		when(query.list())
				.thenReturn(List.of(withIncident, withoutIncident))
				.thenReturn(List.of(withIncident));

		Map<String, Object> data = new HashMap<>();
		data.put("processDefinitionKey", "processKey1");

		Collection<ProcessInstance> result = processProvider.findCurrentProcessesInstances(data, Optional.empty(), Optional.empty(), user);

		assertThat(result).hasSize(2);
		Map<String, ProcessInstance> byId = result.stream().collect(java.util.stream.Collectors.toMap(ProcessInstance::getId, i -> i));
		assertThat(byId.get("instance-1").getWithIncident()).isTrue();
		assertThat(byId.get("instance-2").getWithIncident()).isFalse();

		// one query for the matching instances, one follow-up query to detect incidents
		verify(runtimeService, times(2)).createProcessInstanceQuery();
	}

	@Test
	void findCurrentProcessesInstances_skipsFollowUpQueryWhenCallerAlreadyFilteredByIncident() {
		org.cibseven.bpm.engine.runtime.ProcessInstance instance = mockEngineInstance("instance-1");
		when(query.list()).thenReturn(List.of(instance));

		Map<String, Object> data = new HashMap<>();
		data.put("withIncident", Boolean.TRUE);

		Collection<ProcessInstance> result = processProvider.findCurrentProcessesInstances(data, Optional.empty(), Optional.empty(), user);

		assertThat(result).hasSize(1);
		assertThat(result.iterator().next().getWithIncident()).isTrue();

		verify(runtimeService, times(1)).createProcessInstanceQuery();
	}

	@Test
	void findCurrentProcessesInstances_returnsEmptyCollectionWithoutFollowUpQuery() {
		when(query.list()).thenReturn(List.of());

		Collection<ProcessInstance> result = processProvider.findCurrentProcessesInstances(new HashMap<>(), Optional.empty(), Optional.empty(), user);

		assertThat(result).isEmpty();
		verify(runtimeService, times(1)).createProcessInstanceQuery();
	}

	@Test
	void findProcessesInstances_flagsOnlyInstancesWithOpenIncidents() {
		org.cibseven.bpm.engine.runtime.ProcessInstance withIncident = mockEngineInstance("instance-1");
		org.cibseven.bpm.engine.runtime.ProcessInstance withoutIncident = mockEngineInstance("instance-2");

		when(query.list())
				.thenReturn(List.of(withIncident, withoutIncident))
				.thenReturn(List.of(withIncident));

		Collection<ProcessInstance> result = processProvider.findProcessesInstances("processKey1", user);

		assertThat(result).hasSize(2);
		Map<String, ProcessInstance> byId = result.stream().collect(java.util.stream.Collectors.toMap(ProcessInstance::getId, i -> i));
		assertThat(byId.get("instance-1").getWithIncident()).isTrue();
		assertThat(byId.get("instance-2").getWithIncident()).isFalse();

		// one query for the matching instances, one follow-up query to detect incidents
		verify(runtimeService, times(2)).createProcessInstanceQuery();
	}

	@Test
	void findProcessesInstances_returnsEmptyCollectionWithoutFollowUpQuery() {
		when(query.list()).thenReturn(List.of());

		Collection<ProcessInstance> result = processProvider.findProcessesInstances("processKey1", user);

		assertThat(result).isEmpty();
		verify(runtimeService, times(1)).createProcessInstanceQuery();
	}

	@Test
	void findProcessInstance_flagsWithIncident() {
		org.cibseven.bpm.engine.runtime.ProcessInstance engineInstance = mockEngineInstance("instance-1");
		when(query.singleResult()).thenReturn(engineInstance);
		when(query.list()).thenReturn(List.of(engineInstance));

		ProcessInstance result = processProvider.findProcessInstance("instance-1", user);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("instance-1");
		assertThat(result.getWithIncident()).isTrue();

		verify(runtimeService, times(2)).createProcessInstanceQuery();
	}

	@Test
	void findProcessInstance_flagsFalseWhenNoOpenIncident() {
		org.cibseven.bpm.engine.runtime.ProcessInstance engineInstance = mockEngineInstance("instance-1");
		when(query.singleResult()).thenReturn(engineInstance);
		when(query.list()).thenReturn(List.of());

		ProcessInstance result = processProvider.findProcessInstance("instance-1", user);

		assertThat(result).isNotNull();
		assertThat(result.getWithIncident()).isFalse();
	}

	@Test
	void findProcessInstance_throwsWhenInstanceNotFound() {
		when(query.singleResult()).thenReturn(null);

		assertThatThrownBy(() -> processProvider.findProcessInstance("missing", user))
				.isInstanceOf(NoObjectFoundException.class);

		// no follow-up incident query since the instance itself does not exist
		verify(runtimeService, times(1)).createProcessInstanceQuery();
	}
}
