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

import java.util.Collection;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Metric;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SystemProvider extends SevenProviderBase implements ISystemProvider {

	@Override
	public int getSum(String metric, Map<String, Object> queryParams, CIBUser user) {
		String url = getEngineRestUrl(user) + "/metrics/" + metric + "/sum";
		url += encodeQueryParams(queryParams);
		JsonNode body =  ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, true)).getBody();
		if (body == null) {
			throw new NullPointerException();
		}
		return body.get("result").asInt();
	}
	

	@Override
	public JsonNode getTelemetryData(CIBUser user) {
		String url = getEngineRestUrl(user) + "/telemetry/data";
		return doGet(url, JsonNode.class, user, false).getBody();
	}

	@Override
	public Collection<Metric> getMetrics(Map<String, Object> queryParams, CIBUser user) {
		String groupBy = queryParams.getOrDefault("groupBy", "month").toString();

		switch (groupBy) {
			case "year":
				return getAnnualMetrics(queryParams, user);
			case "month":
				return getMonthlyMetrics(queryParams, user);
			default:
				throw new IllegalArgumentException("Invalid groupBy parameter: " + groupBy);
		}
	}

}
