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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.bpm.engine.management.Metrics;
import org.cibseven.bpm.engine.management.MetricsQuery;
import org.cibseven.bpm.engine.rest.dto.converter.DateConverter;
import org.cibseven.bpm.engine.rest.dto.telemetry.TelemetryDataDto;
import org.cibseven.bpm.engine.telemetry.TelemetryData;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Metric;

import com.fasterxml.jackson.databind.JsonNode;

public class DirectSystemProvider implements ISystemProvider {

	DirectProviderUtil directProviderUtil;

	DirectSystemProvider(DirectProviderUtil directProviderUtil){
		this.directProviderUtil = directProviderUtil;
	}

	@Override
	public JsonNode getTelemetryData(CIBUser user) {
		TelemetryData data = directProviderUtil.getProcessEngine(user).getManagementService().getTelemetryData();
		JsonNode node = directProviderUtil.getObjectMapper(user).valueToTree(TelemetryDataDto.fromEngineDto(data));
		return node;
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

	protected Collection<Metric> getAnnualMetrics(Map<String, Object> queryParams, CIBUser user) {
		Collection<Metric> metrics = new ArrayList<>();
		int currentYear = ZonedDateTime.now(ZoneId.systemDefault()).getYear();
		for (int year = currentYear; year > 2012; year--) {
			Collection<Metric> yearMetrics = getAnnualMetricsForYear(queryParams, user, year);
			metrics.addAll(yearMetrics);
			// if all metrics for the year are zero, we can stop fetching
			boolean allZero = yearMetrics.stream().allMatch(m -> m.getSum() == 0);
			if (allZero) {
				break;
			}
		}
		return metrics;
	}

	protected Collection<Metric> getAnnualMetricsForYear(Map<String, Object> queryParams, CIBUser user, int year) {
		Collection<Metric> metrics = new ArrayList<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

		List<String> metricNames = Optional.ofNullable(queryParams.get("metrics"))
			.map(Object::toString)
			.filter(s -> !s.isEmpty())
			.map(s -> Arrays.asList(s.split(",")))
			.orElse(Arrays.asList("process-instances", "decision-instances", "task-users"));

		for (String metric : metricNames) {

			String startOfYear = ZonedDateTime.now(ZoneId.systemDefault()).withDayOfYear(1).withYear(year).format(formatter);
			String endOfYear = ZonedDateTime.now(ZoneId.systemDefault()).withDayOfYear(1).withYear(year + 1).format(formatter);
			Map<String, Object> params = createSumParamsMap(metric, startOfYear, endOfYear);

			int count = getSum(metric, params, user);

			Metric metricsData = new Metric();
			metricsData.setMetric(metric);
			metricsData.setSubscriptionYear(year);
			metricsData.setSum(count);

			metrics.add(metricsData);
		}

		return metrics;
	}

	protected Collection<Metric> getMonthlyMetrics(Map<String, Object> queryParams, CIBUser user) {
		Collection<Metric> metrics = new ArrayList<>();
		List<String> metricNames = Optional.ofNullable(queryParams.get("metrics"))
			.map(Object::toString)
			.filter(s -> !s.isEmpty())
			.map(s -> Arrays.asList(s.split(",")))
			.orElse(Arrays.asList("process-instances", "decision-instances", "task-users"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
		String subsStartDate = queryParams.get("subscriptionStartDate").toString();
		ZonedDateTime subsStartDateParsed = ZonedDateTime.parse(subsStartDate, formatter);

		String startDate = queryParams.get("startDate").toString();
		ZonedDateTime startDateParsed = ZonedDateTime.parse(startDate, formatter);

		for (ZonedDateTime stDate = startDateParsed; !stDate.isAfter(subsStartDateParsed); stDate = stDate.plusMonths(1)) {
			ZonedDateTime startDayM = stDate.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);
			ZonedDateTime endDayM = stDate.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999_000_000);
			for (String metric : metricNames) {

				Map<String, Object> params = createSumParamsMap(metric, startDayM.format(formatter), endDayM.format(formatter));
				int count = getSum(metric, params, user);

				Metric metricsData = new Metric();
				metricsData.setMetric(metric);
				metricsData.setSubscriptionYear(startDayM.getYear());
				metricsData.setSubscriptionMonth(startDayM.getMonthValue());
				metricsData.setSum(count);

				metrics.add(metricsData);
			}
		}

		return metrics;
	}

	private int getSum(String metricsName, Map<String, Object> queryParams, CIBUser user) {
		DateConverter dateConverter = new DateConverter();
		dateConverter.setObjectMapper(directProviderUtil.getObjectMapper(user));

		long result = 0;

		if (Metrics.UNIQUE_TASK_WORKERS.equals(metricsName) || Metrics.TASK_USERS.equals(metricsName)) {
			result = directProviderUtil.getProcessEngine(user).getManagementService().getUniqueTaskWorkerCount(extractStartDate(queryParams, dateConverter),
					extractEndDate(queryParams, dateConverter));
		} else {
			MetricsQuery query = directProviderUtil.getProcessEngine(user).getManagementService().createMetricsQuery().name(metricsName);

			applyQueryParams(queryParams, dateConverter, query);
			result = query.sum();
		}
		return (int) result;
	}

	private void applyQueryParams(Map<String, Object> queryParameters, DateConverter dateConverter, MetricsQuery query) {
		Date startDate = extractStartDate(queryParameters, dateConverter);
		Date endDate = extractEndDate(queryParameters, dateConverter);
		if (startDate != null) {
			query.startDate(startDate);
		}
		if (endDate != null) {
			query.endDate(endDate);
		}
	}

	private Date extractEndDate(Map<String, Object> queryParameters, DateConverter dateConverter) {
		if (queryParameters.containsKey("endDate")) {
			return dateConverter.convertQueryParameterToType((String) queryParameters.get("endDate"));
		}
		return null;
	}

	private Date extractStartDate(Map<String, Object> queryParameters, DateConverter dateConverter) {
		if (queryParameters.containsKey("startDate")) {
			return dateConverter.convertQueryParameterToType((String) queryParameters.get("startDate"));
		}
		return null;
	}

	private Map<String, Object> createSumParamsMap(String metric, String startDate, String endDate) {
		Map<String, Object> params = new HashMap<>();
		params.put("metric", metric);
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		return params;
	}

}
