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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Metric;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Covers the default methods of {@link ISystemProvider}, which do the year-by-year metric
 * walk-back on top of whatever the concrete provider reports for a single sum.
 */
public class ISystemProviderTest {

	/** Records the sums it was asked for and answers from a fixed table. */
	private static class RecordingSystemProvider implements ISystemProvider {
		private final Map<String, Integer> sumsByYear;
		private final List<Map<String, Object>> requests = new ArrayList<>();

		RecordingSystemProvider(Map<String, Integer> sumsByYear) {
			this.sumsByYear = sumsByYear;
		}

		@Override
		public int getSum(String metricsName, Map<String, Object> queryParams, CIBUser user) {
			requests.add(new HashMap<>(queryParams));
			String startDate = String.valueOf(queryParams.get("startDate"));
			return sumsByYear.getOrDefault(startDate.substring(0, 4), 0);
		}

		@Override
		public JsonNode getTelemetryData(CIBUser user) {
			return null;
		}

		@Override
		public Collection<Metric> getMetrics(Map<String, Object> queryParams, CIBUser user) {
			return List.of();
		}
	}

	private static int currentYear() {
		return ZonedDateTime.now(ZoneId.systemDefault()).getYear();
	}

	@Test
	void createSumParamsMap_carriesTheMetricAndTheDateRange() {
		RecordingSystemProvider provider = new RecordingSystemProvider(Map.of());

		Map<String, Object> params =
			provider.createSumParamsMap("process-instances", "2026-01-01", "2027-01-01");

		assertThat(params)
			.containsEntry("metric", "process-instances")
			.containsEntry("startDate", "2026-01-01")
			.containsEntry("endDate", "2027-01-01");
	}

	@Test
	void getAnnualMetricsForYear_defaultsToTheThreeBilledMetrics() {
		RecordingSystemProvider provider = new RecordingSystemProvider(Map.of());

		Collection<Metric> metrics =
			provider.getAnnualMetricsForYear(new HashMap<>(), null, 2026);

		assertThat(metrics).extracting(Metric::getMetric)
			.containsExactly("process-instances", "decision-instances", "task-users");
		assertThat(metrics).allSatisfy(metric -> assertThat(metric.getSubscriptionYear()).isEqualTo(2026));
	}

	@Test
	void getAnnualMetricsForYear_honoursAnExplicitMetricList() {
		RecordingSystemProvider provider = new RecordingSystemProvider(Map.of());
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("metrics", "process-instances,task-users");

		assertThat(provider.getAnnualMetricsForYear(queryParams, null, 2026))
			.extracting(Metric::getMetric).containsExactly("process-instances", "task-users");
	}

	@Test
	void getAnnualMetricsForYear_treatsAnEmptyMetricListAsAbsent() {
		RecordingSystemProvider provider = new RecordingSystemProvider(Map.of());
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("metrics", "");

		// an empty request parameter must not produce a single metric named ""
		assertThat(provider.getAnnualMetricsForYear(queryParams, null, 2026)).hasSize(3);
	}

	@Test
	void getAnnualMetricsForYear_asksForTheWholeCalendarYear() {
		RecordingSystemProvider provider = new RecordingSystemProvider(Map.of());
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("metrics", "process-instances");

		provider.getAnnualMetricsForYear(queryParams, null, 2026);

		Map<String, Object> request = provider.requests.get(0);
		assertThat(String.valueOf(request.get("startDate"))).startsWith("2026-01-01");
		// the range ends at the start of the following year
		assertThat(String.valueOf(request.get("endDate"))).startsWith("2027-01-01");
	}

	@Test
	void getAnnualMetricsForYear_reportsTheSumTheProviderReturns() {
		RecordingSystemProvider provider =
			new RecordingSystemProvider(Map.of(String.valueOf(2026), 42));
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("metrics", "process-instances");

		assertThat(provider.getAnnualMetricsForYear(queryParams, null, 2026))
			.singleElement().extracting(Metric::getSum).isEqualTo(42);
	}

	@Test
	void getAnnualMetrics_stopsAtTheFirstYearWithNoUsage() {
		// usage in the current year only; the year before is all zeroes
		RecordingSystemProvider provider =
			new RecordingSystemProvider(Map.of(String.valueOf(currentYear()), 5));
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("metrics", "process-instances");

		Collection<Metric> metrics = provider.getAnnualMetrics(queryParams, null);

		// the current year (5) plus the first all-zero year, then it gives up walking back
		assertThat(metrics).extracting(Metric::getSubscriptionYear)
			.containsExactly(currentYear(), currentYear() - 1);
	}

	@Test
	void getAnnualMetrics_stopsImmediatelyWhenThereIsNoUsageAtAll() {
		RecordingSystemProvider provider = new RecordingSystemProvider(Map.of());
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("metrics", "process-instances");

		// a fresh installation must not walk back to 2013
		assertThat(provider.getAnnualMetrics(queryParams, null))
			.extracting(Metric::getSubscriptionYear).containsExactly(currentYear());
	}

	@Test
	void getAnnualMetrics_keepsWalkingBackWhileThereIsUsage() {
		RecordingSystemProvider provider = new RecordingSystemProvider(Map.of(
			String.valueOf(currentYear()), 5,
			String.valueOf(currentYear() - 1), 3,
			String.valueOf(currentYear() - 2), 1));
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("metrics", "process-instances");

		assertThat(provider.getAnnualMetrics(queryParams, null))
			.extracting(Metric::getSubscriptionYear)
			.containsExactly(currentYear(), currentYear() - 1, currentYear() - 2, currentYear() - 3);
	}
}
