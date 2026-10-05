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

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;

public class CustomRestTemplateTest {

	@Test
	void defaultConstructor_decodesStringsAsUtf8() {
		assertStringConvertersUseUtf8(new CustomRestTemplate());
	}

	@Test
	void requestFactoryConstructor_decodesStringsAsUtf8() {
		assertStringConvertersUseUtf8(new CustomRestTemplate(new SimpleClientHttpRequestFactory()));
	}

	private static void assertStringConvertersUseUtf8(CustomRestTemplate template) {
		// engine-rest sends text without a charset; the String converter's default decides the decoding
		List<StringHttpMessageConverter> converters = template.getMessageConverters().stream()
			.filter(StringHttpMessageConverter.class::isInstance)
			.map(StringHttpMessageConverter.class::cast)
			.toList();

		assertThat(converters).isNotEmpty();
		assertThat(converters).allSatisfy(converter ->
			assertThat(converter.getDefaultCharset()).isEqualTo(StandardCharsets.UTF_8));
	}
}
