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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cibseven.bpm.engine.ExternalTaskService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.externaltask.ExternalTaskQuery;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.webapp.auth.CIBUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DirectExternalTaskProviderTest {

	private ExternalTaskService externalTaskService;
	private DirectExternalTaskProvider externalTaskProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		user = new CIBUser("testUser");

		ProcessEngine processEngine = mock(ProcessEngine.class);
		externalTaskService = mock(ExternalTaskService.class);
		when(processEngine.getExternalTaskService()).thenReturn(externalTaskService);

		DirectProviderUtil directProviderUtil = Mockito.spy(new DirectProviderUtil());
		doReturn(processEngine).when(directProviderUtil).getProcessEngine(any(CIBUser.class));
		doReturn(JacksonConfigurator.configureObjectMapper()).when(directProviderUtil).getObjectMapper(any(CIBUser.class));

		externalTaskProvider = new DirectExternalTaskProvider(directProviderUtil);
	}

	@Test
	void getExternalTasks_pagesTheQueryOnce() {
		ExternalTaskQuery query = mock(ExternalTaskQuery.class, withSettings().defaultAnswer(RETURNS_SELF));
		when(externalTaskService.createExternalTaskQuery()).thenReturn(query);
		when(query.listPage(10, 25)).thenReturn(Collections.emptyList());

		Map<String, Object> params = new HashMap<>();
		params.put("firstResult", "10");
		params.put("maxResults", "25");
		params.put("topicName", "invoice");

		assertThat(externalTaskProvider.getExternalTasks(params, user)).isEmpty();
		verify(query).topicName("invoice");
		verify(query, times(1)).listPage(10, 25);
		verify(query, never()).list();
	}
}
