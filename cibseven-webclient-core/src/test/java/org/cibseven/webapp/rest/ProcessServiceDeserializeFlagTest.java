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
package org.cibseven.webapp.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The {@code deserialize} flag of the chat-comments and status-dataset endpoints (CIB7-2357).
 * It used to be compared by reference ({@code deserialize == "true"}), so a {@code true} that
 * arrived as a request parameter never matched and the serialized value came back. A direct
 * method call with a {@code "true"} literal cannot catch that (literals are interned), so the
 * flag is sent through the real dispatcher here.
 */
public class ProcessServiceDeserializeFlagTest {

	private static final String BASE = "/services/v1";

	private BpmProvider bpmProvider;
	private CIBUser user;
	private MockMvc mvc;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		bpmProvider = mock(BpmProvider.class);
		user = new CIBUser("demo");
		BaseUserProvider<?> baseUserProvider = mock(BaseUserProvider.class);
		when(baseUserProvider.checkAuthorization(any(), anyBoolean())).thenReturn(user);

		ProcessService service = new ProcessService();
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(service, "baseUserProvider", baseUserProvider);

		mvc = MockMvcBuilders.standaloneSetup(service)
				.addPlaceholderValue("cibseven.webclient.services.basePath", BASE)
				.build();
		when(bpmProvider.fetchProcessInstanceVariable(any(), any(), anyBoolean(), any())).thenReturn(new Variable());
	}

	@ParameterizedTest(name = "{0} with deserialize={1} -> {3}")
	@CsvSource(nullValues = "ABSENT", value = {
		"chat-comments,  ABSENT, chatComments,   true",
		"chat-comments,  true,   chatComments,   true",
		"chat-comments,  false,  chatComments,   false",
		"status-dataset, ABSENT, _statusDataset, true",
		"status-dataset, true,   _statusDataset, true",
		"status-dataset, false,  _statusDataset, false",
	})
	void theDeserializeFlagIsHonoured(String endpoint, String flag, String variableName, boolean expected) throws Exception {
		MockHttpServletRequestBuilder request = get(BASE + "/process/process-instance/pi-1/" + endpoint)
				.param("processDefinitionKey", "invoice");
		if (flag != null) {
			request.param("deserialize", flag);
		}

		mvc.perform(request).andExpect(status().isOk());

		verify(bpmProvider).fetchProcessInstanceVariable(eq("pi-1"), eq(variableName), eq(expected), eq(user));
	}
}
