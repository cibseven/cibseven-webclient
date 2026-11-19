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

import java.util.Collection;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Metric;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
	@ApiResponse(responseCode= "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode= "401", description = "Unauthorized")
})
@RestController @RequestMapping(SevenWebclientProperties.SERVICES_BASE_PATH + "/system")
public class SystemService extends BaseService implements InitializingBean {

 @Autowired BpmProvider bpmProvider;

	private final CustomRestTemplate restTemplate;

	public SystemService(CustomRestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void afterPropertiesSet() {
	}

	@GetMapping("/telemetry/data")
	public JsonNode getTelemetryData(CIBUser user) {
		return bpmProvider.getTelemetryData(user);
	}

	@GetMapping("/metrics/data")
		public Collection<Metric> getMetrics(@RequestParam Map<String, Object> queryParams, CIBUser user) {
			return bpmProvider.getMetrics(queryParams, user);
	}

	/**
	 * Statistics of the connection pool towards the engine, for logged-in users. Deliberately
	 * answered without a call to the engine (such as a permission check): that call would go
	 * through this very pool, change the figures it reports and block when the pool is exhausted.
	 */
	@GetMapping("/pool-status")
	public String poolStatus(CIBUser user) {
		return restTemplate.getConnectionPoolStats();
	}

}
