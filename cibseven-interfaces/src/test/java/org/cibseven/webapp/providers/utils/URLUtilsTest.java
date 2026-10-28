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
package org.cibseven.webapp.providers.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class URLUtilsTest {

	@Test
	void buildUrlWithParams_withoutParams_returnsTheBaseUrlUnchanged() {
		assertThat(URLUtils.buildUrlWithParams("http://engine/engine-rest/decision-definition", Collections.emptyMap()))
			.isEqualTo("http://engine/engine-rest/decision-definition");
	}

	@Test
	void buildUrlWithParams_appendsTheParamsInOrder() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("key", "risk");
		params.put("firstResult", 0);
		params.put("maxResults", 50);

		assertThat(URLUtils.buildUrlWithParams("http://engine/engine-rest/decision-definition", params))
			.isEqualTo("http://engine/engine-rest/decision-definition?key=risk&firstResult=0&maxResults=50");
	}

	@Test
	void buildUrlWithParams_skipsNullValues() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("key", "risk");
		params.put("tenantId", null);

		assertThat(URLUtils.buildUrlWithParams("http://engine/engine-rest/decision-definition", params))
			.isEqualTo("http://engine/engine-rest/decision-definition?key=risk");
	}
}
