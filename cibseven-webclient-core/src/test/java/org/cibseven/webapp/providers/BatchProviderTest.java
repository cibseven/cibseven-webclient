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

import java.util.List;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.EngineRestClientException;
import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.RecordedRequest;

class BatchProviderTest {

	private MockEngineRest engine;
	private BatchProvider provider;
	private CIBUser user;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		provider = engine.wire(new BatchProvider());
		user = MockEngineRest.user();
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	@Test
	void setRemovalTime_postsThePayloadToTheHistoricBatchEndpoint() throws Exception {
		engine.enqueueJson("{\"id\":\"batch-1\",\"type\":\"batch-set-removal-time\"}");

		Object result = provider.setRemovalTime(payload("2026-01-01T00:00:00.000+0000"), user);

		RecordedRequest request = engine.take();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/engine-rest/history/batch/set-removal-time");
		assertThat(request.getBody().readUtf8()).contains("\"absoluteRemovalTime\":\"2026-01-01T00:00:00.000+0000\"");
		assertThat(result).asString().contains("batch-1");
	}

	/**
	 * CIB7-2158: engine-rest refuses a malformed date with a 400. That is the caller's mistake, so it
	 * must reach the caller as the same 400 with the engine's message, not as a 500 SystemException.
	 */
	@Test
	void setRemovalTime_malformedDate_forwardsTheEngineClientError() {
		engine.enqueueStatus(400, "{\"type\":\"InvalidRequestException\","
			+ "\"message\":\"Cannot deserialize value of type `java.util.Date` from String \\\"2026-01-01T00:00:00Z\\\"\"}");

		assertThatThrownBy(() -> provider.setRemovalTime(payload("2026-01-01T00:00:00Z"), user))
			.isInstanceOfSatisfying(EngineRestClientException.class, e -> {
				assertThat(e.getStatus()).isEqualTo(400);
				assertThat(e.getMessage()).startsWith("Cannot deserialize value of type `java.util.Date`");
			});
	}

	/** A genuine engine failure is still reported as a technical problem. */
	@Test
	void setRemovalTime_engineFailure_staysASystemException() {
		engine.enqueueStatus(500, "{\"type\":\"ProcessEngineException\",\"message\":\"database down\"}");

		assertThatThrownBy(() -> provider.setRemovalTime(payload("2026-01-01T00:00:00.000+0000"), user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("database down");
	}

	private static Map<String, Object> payload(String removalTime) {
		return Map.of("absoluteRemovalTime", removalTime, "historicBatchIds", List.of("batch-1"));
	}
}
