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
package org.cibseven.webapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;

import org.cibseven.webapp.rest.CustomRestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

/**
 * Guards the String decoding of the engine REST template on Spring 7 (CIB7-2204): engine-rest sends
 * stack traces, error details and rendered forms without a charset, and Spring's default would read
 * them as ISO-8859-1.
 */
class SevenWebclientContextRestTemplateTest {

	private static final String NON_ASCII = "Größe ungültig – 姓名 fehlt";

	@Test
	void decodesTextWithoutCharsetAsUtf8() {
		CustomRestTemplate template = customRestTemplate();
		MockRestServiceServer engine = MockRestServiceServer.bindTo(template).build();
		engine.expect(requestTo("http://engine/engine-rest/job/job-1/stacktrace"))
				.andRespond(withSuccess(NON_ASCII.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN));

		String body = template.getForObject("http://engine/engine-rest/job/job-1/stacktrace", String.class);

		assertThat(body).isEqualTo(NON_ASCII);
		engine.verify();
	}

	@Test
	void honoursACharsetTheEngineDeclares() {
		CustomRestTemplate template = customRestTemplate();
		MockRestServiceServer engine = MockRestServiceServer.bindTo(template).build();
		engine.expect(requestTo("http://engine/engine-rest/job/job-1/stacktrace"))
				.andRespond(withSuccess("Größe".getBytes(StandardCharsets.ISO_8859_1),
						new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.ISO_8859_1)));

		assertThat(template.getForObject("http://engine/engine-rest/job/job-1/stacktrace", String.class))
				.isEqualTo("Größe");
	}

	private static CustomRestTemplate customRestTemplate() {
		SevenWebclientContext context = new SevenWebclientContext();
		context.jacksonParserMaxSize = 20000000;
		return context.customRestTemplate();
	}
}
