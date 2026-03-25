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
package org.cibseven.webapp.compat;

import java.util.ArrayList;

import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * RestTemplate factory for Spring Boot 4 (Jackson 3 / tools.jackson).
 * Creates RestTemplate instances with the appropriate Jackson message converter.
 */
public final class RestTemplateFactory {

	private RestTemplateFactory() {}

	/**
	 * Creates a new RestTemplate configured with Jackson 3 message converter.
	 *
	 * @param sourceTemplate the template whose message converters are copied as a base
	 * @param jacksonParserMaxSize maximum string length for Jackson parser
	 * @return a new RestTemplate instance
	 */
	public static RestTemplate createPatchRestTemplate(RestTemplate sourceTemplate, int jacksonParserMaxSize) {
		RestTemplate restTemplate = new RestTemplate();

		StreamReadConstraints streamReadConstraints = StreamReadConstraints
				.builder()
				.maxStringLength(jacksonParserMaxSize)
				.build();
		JsonFactory jsonFactory = JsonFactory.builder()
				.streamReadConstraints(streamReadConstraints)
				.build();
		JsonMapper objectMapper = JsonMapper.builder(jsonFactory)
				.build();

		restTemplate.setMessageConverters(new ArrayList<>(sourceTemplate.getMessageConverters()));
		restTemplate.getMessageConverters().add(new JacksonJsonHttpMessageConverter(objectMapper));

		return restTemplate;
	}

}
