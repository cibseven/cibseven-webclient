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
import org.springframework.web.client.RestTemplate;

public class CustomRestTemplateTest {

	@Test
	void defaultConstructor_decodesStringsAsUtf8() {
		assertStringConvertersUseUtf8(new CustomRestTemplate());
	}

	@Test
	void requestFactoryConstructor_decodesStringsAsUtf8() {
		assertStringConvertersUseUtf8(new CustomRestTemplate(new SimpleClientHttpRequestFactory()));
	}

	@Test
	void customStringConverterAddedLater_isKeptAndTakesPrecedence() {
		// a customer's own converter must neither be replaced nor switched to UTF-8
		StringHttpMessageConverter custom = new StringHttpMessageConverter(StandardCharsets.ISO_8859_1);
		CustomRestTemplate template = new CustomRestTemplate();
		template.addConverter(custom);
		template.initialize();

		assertThat(template.getMessageConverters().get(0)).isSameAs(custom);
		assertThat(custom.getDefaultCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
	}

	@Test
	void plainRestTemplates_keepSpringsDefault() {
		// the change applies to this template's own converter, not to a shared or static default
		new CustomRestTemplate();

		StringHttpMessageConverter plain = new RestTemplate().getMessageConverters().stream()
			.filter(StringHttpMessageConverter.class::isInstance)
			.map(StringHttpMessageConverter.class::cast)
			.findFirst().orElseThrow();
		assertThat(plain.getDefaultCharset()).isEqualTo(StringHttpMessageConverter.DEFAULT_CHARSET);
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
