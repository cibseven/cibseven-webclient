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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Metric;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SystemProvider extends SevenProviderBase implements ISystemProvider {

	private int getSum(String metric, Map<String, Object> queryParams, CIBUser user) {
		String url = getEngineRestUrl() + "/metrics/" + metric + "/sum";
		String params = "";
		for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
			Optional<String> value = Optional.ofNullable(entry.getValue()).map(Object::toString);
			params += addQueryParameter(params, entry.getKey(), value, true);
	    }
		url += params;
		return ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, true)).getBody().get("result").asInt();
	}
	
	private Map<String, Object> createSumParamsMap(String metric, String startDate, String endDate) {
	    Map<String, Object> params = new HashMap<>();
	    params.put("metric", metric);
	    params.put("startDate", startDate);
	    params.put("endDate", endDate);
	    return params;
	}

	@Override
	public JsonNode getTelemetryData(CIBUser user) {
		String url = getEngineRestUrl() + "/telemetry/data";
		return ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, false)).getBody();
	}

	@Override
	public Collection<Metric> getMetrics(Map<String, Object> queryParams, CIBUser user) {
		Collection<Metric> metrics = new ArrayList<>();
		List<Map<String, Object>> queryData = new ArrayList<>();
		List<String> metricNames = Optional.ofNullable(queryParams.get("metrics"))
		        .map(Object::toString)
		        .filter(s -> !s.isEmpty())
		        .map(s -> Arrays.asList(s.split(",")))
		        .orElse(Arrays.asList("process-instances", "decision-instances", "task-users"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
		String currentDate = ZonedDateTime.now(ZoneId.systemDefault()).format(formatter);
		String groupBy = Optional.ofNullable(queryParams.get("groupBy"))
		        .map(Object::toString)
		        .orElse("month");
		String subsStartDate = queryParams.get("subscriptionStartDate").toString();
		ZonedDateTime subsStartDateParsed = ZonedDateTime.parse(subsStartDate, formatter);
		if (groupBy.equals("year")) {
			String prevDate = subsStartDateParsed.minusYears(1).format(formatter);
			for (String metric : metricNames) {
			    queryData.add(createSumParamsMap(metric, subsStartDate, currentDate));
			    queryData.add(createSumParamsMap(metric, prevDate, subsStartDate));
			}
		} else if (groupBy.equals("month")) {
			String startDate = queryParams.get("startDate").toString();
			ZonedDateTime startDateParsed = ZonedDateTime.parse(startDate, formatter);
			for (ZonedDateTime stDate = startDateParsed; !stDate.isAfter(subsStartDateParsed); stDate = stDate.plusMonths(1)) {
			    ZonedDateTime startDayM = stDate.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);
			    ZonedDateTime endDayM = stDate.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999_000_000);
			    for (String metric : metricNames) {
			    	 queryData.add(createSumParamsMap(metric, startDayM.format(formatter), endDayM.format(formatter)));
			    }
			}
		}
		for (Map<String, Object> params : queryData) {
			Metric metricsData = new Metric();
			metricsData.setMetric(params.get("metric").toString());
			ZonedDateTime startDate = ZonedDateTime.parse(params.get("startDate").toString(), formatter);
			metricsData.setSubscriptionYear(startDate.getYear());
			if (groupBy.equals("month")) {
				metricsData.setSubscriptionMonth(startDate.getMonthValue());
			}
			int count = getSum(metricsData.getMetric(), params, user);
			metricsData.setSum(count);
			metrics.add(metricsData);
		}
		return metrics;
	}
}
