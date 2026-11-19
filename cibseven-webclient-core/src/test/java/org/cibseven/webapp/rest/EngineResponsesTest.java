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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EngineResponsesTest {

	private static final byte[] BODY = "<form/>".getBytes(StandardCharsets.UTF_8);

	@Mock
	private BpmProvider bpmProvider;

	private final CIBUser user = new CIBUser("demo");

	/** What engine-rest answers for a streamed resource: the connection headers next to the content ones. */
	private static HttpHeaders engineHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.TRANSFER_ENCODING, "chunked");
		headers.add(HttpHeaders.CONNECTION, "keep-alive");
		headers.add("Keep-Alive", "timeout=60");
		headers.add(HttpHeaders.CONTENT_LENGTH, "999");
		headers.add(HttpHeaders.DATE, "Fri, 13 Nov 2026 17:00:00 GMT");
		headers.add(HttpHeaders.SET_COOKIE, "JSESSIONID=engine-session");
		headers.add(HttpHeaders.CONTENT_TYPE, "application/xhtml+xml");
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice.pdf\"");
		headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");
		return headers;
	}

	private static <T> ResponseEntity<T> engineResponse(T body) {
		return ResponseEntity.ok().headers(engineHeaders()).body(body);
	}

	private static void assertOnlyContentHeaders(HttpHeaders headers) {
		assertThat(headers.headerNames()).containsExactlyInAnyOrder(
				HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_DISPOSITION, HttpHeaders.CACHE_CONTROL);
		assertThat(headers.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/xhtml+xml");
		assertThat(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("attachment; filename=\"invoice.pdf\"");
		assertThat(headers.getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-cache");
	}

	private <S extends BaseService> S withProvider(S service) {
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
		return service;
	}

	// ---------- helper ----------

	@Test
	void forwardableHeaders_keepsOnlyTheContentHeaders() {
		assertOnlyContentHeaders(EngineResponses.forwardableHeaders(engineHeaders(), BODY));
	}

	@Test
	void forwardableHeaders_isEmptyWhenTheEngineSentNoContentHeaders() {
		HttpHeaders source = new HttpHeaders();
		source.add(HttpHeaders.TRANSFER_ENCODING, "chunked");

		assertThat(EngineResponses.forwardableHeaders(source, BODY).isEmpty()).isTrue();
	}

	@Test
	void forwardableHeaders_keepsTheContentEncodingOfAnUnchangedByteBody() {
		HttpHeaders source = engineHeaders();
		source.add(HttpHeaders.CONTENT_ENCODING, "gzip");

		assertThat(EngineResponses.forwardableHeaders(source, BODY).getFirst(HttpHeaders.CONTENT_ENCODING))
				.isEqualTo("gzip");
	}

	@Test
	void forwardableHeaders_dropsTheContentEncodingOfADecodedStringBody() {
		// the converter already turned the bytes into text, which Spring writes uncompressed
		HttpHeaders source = engineHeaders();
		source.add(HttpHeaders.CONTENT_ENCODING, "gzip");

		assertOnlyContentHeaders(EngineResponses.forwardableHeaders(source, "<form/>"));
	}

	@Test
	void relay_keepsStatusAndBody() {
		ResponseEntity<byte[]> source = ResponseEntity.status(HttpStatus.ACCEPTED)
				.headers(engineHeaders()).body(BODY);

		ResponseEntity<byte[]> relayed = EngineResponses.relay(source);

		assertThat(relayed.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(relayed.getBody()).isEqualTo(BODY);
		assertOnlyContentHeaders(relayed.getHeaders());
	}

	// ---------- endpoints that hand on an engine response (CIB7-2766) ----------

	@Test
	void deployedStartForm_doesNotForwardTheEngineConnectionHeaders() {
		when(bpmProvider.getDeployedStartForm("def-1", user)).thenReturn(engineResponse(BODY));

		ResponseEntity<byte[]> response = withProvider(new ProcessService())
				.getDeployedStartForm("def-1", Locale.ENGLISH, user);

		assertThat(response.getBody()).isEqualTo(BODY);
		assertOnlyContentHeaders(response.getHeaders());
	}

	@Test
	void deployedTaskForm_doesNotForwardTheEngineConnectionHeaders() {
		when(bpmProvider.getDeployedForm("task-1", user)).thenReturn(engineResponse(BODY));

		ResponseEntity<byte[]> response = withProvider(new TaskService())
				.getDeployedForm("task-1", Locale.ENGLISH, user);

		assertThat(response.getBody()).isEqualTo(BODY);
		assertOnlyContentHeaders(response.getHeaders());
	}

	@Test
	void renderedTaskForm_doesNotForwardTheEngineConnectionHeaders() {
		when(bpmProvider.getRenderedForm("task-1", new HashMap<>(), user)).thenReturn(engineResponse("<form/>"));

		ResponseEntity<String> response = withProvider(new TaskService())
				.getRenderedForm("task-1", new HashMap<>(), Locale.ENGLISH, user);

		assertThat(response.getBody()).isEqualTo("<form/>");
		assertOnlyContentHeaders(response.getHeaders());
	}

	@Test
	void renderedStartForm_doesNotForwardTheEngineConnectionHeaders() {
		when(bpmProvider.getRenderedStartForm("def-1", new HashMap<>(), user)).thenReturn(engineResponse("<form/>"));

		ResponseEntity<String> response = withProvider(new ProcessDefinitionService())
				.getRenderedStartForm("def-1", new HashMap<>(), Locale.ENGLISH, user);

		assertThat(response.getBody()).isEqualTo("<form/>");
		assertOnlyContentHeaders(response.getHeaders());
	}

	@Test
	void relayOrNoContent_answers204WithoutAForm() {
		ResponseEntity<byte[]> empty = ResponseEntity.ok().headers(engineHeaders()).body(new byte[0]);

		ResponseEntity<byte[]> response = EngineResponses.relayOrNoContent(empty);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getHeaders().isEmpty()).isTrue();
	}

	@Test
	void executionVariableData_doesNotForwardTheEngineConnectionHeaders() {
		when(bpmProvider.fetchVariableDataByExecutionId("exec-1", "file", user)).thenReturn(engineResponse(BODY));

		ResponseEntity<byte[]> response = withProvider(new ProcessService())
				.fetchVariableDataByExecutionId("exec-1", "file", Locale.ENGLISH, user);

		assertThat(response.getBody()).isEqualTo(BODY);
		assertOnlyContentHeaders(response.getHeaders());
	}

	@Test
	void historicVariableData_doesNotForwardTheEngineConnectionHeaders() {
		when(bpmProvider.fetchHistoryVariableDataById("var-1", user)).thenReturn(engineResponse(BODY));

		ResponseEntity<byte[]> response = withProvider(new HistoryProcessService())
				.fetchHistoryVariableDataById("var-1", Locale.ENGLISH, user);

		assertThat(response.getBody()).isEqualTo(BODY);
		assertOnlyContentHeaders(response.getHeaders());
	}
}
