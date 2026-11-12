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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.impl.telemetry.dto.ApplicationServerImpl;
import org.cibseven.bpm.engine.impl.telemetry.dto.DatabaseImpl;
import org.cibseven.bpm.engine.impl.telemetry.dto.InternalsImpl;
import org.cibseven.bpm.engine.impl.telemetry.dto.JdkImpl;
import org.cibseven.bpm.engine.impl.telemetry.dto.ProductImpl;
import org.cibseven.bpm.engine.impl.telemetry.dto.TelemetryDataImpl;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.webapp.auth.CIBUser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import tools.jackson.databind.JsonNode;

class DirectSystemProviderTest {

	/** The telemetry as engine-rest writes it: its property names and its date format. */
	@Test
	void returnsTheTelemetryAsEngineRestWritesIt() throws Exception {
		ProcessEngine engine = mock(ProcessEngine.class);
		ManagementService managementService = mock(ManagementService.class);
		when(engine.getManagementService()).thenReturn(managementService);
		InternalsImpl internals = new InternalsImpl(new DatabaseImpl("h2", "2.4.240"),
				new ApplicationServerImpl("Apache Tomcat", "11.0.25"), null, new JdkImpl("17.0.12", "Oracle"));
		internals.setCommands(Map.of());
		internals.setMetrics(Map.of());
		internals.setWebapps(Set.of());
		internals.setCamundaIntegration(Set.of());
		internals.setDataCollectionStartDate(new Date(0));
		when(managementService.getTelemetryData())
				.thenReturn(new TelemetryDataImpl("8343cc7a", new ProductImpl("CIB seven", "3.0.0", "community", internals)));
		DirectProviderUtil directProviderUtil = Mockito.spy(new DirectProviderUtil());
		doReturn(engine).when(directProviderUtil).getProcessEngine(any(CIBUser.class));
		// the date format takes the default time zone when the mapper is built
		java.util.TimeZone previous = java.util.TimeZone.getDefault();
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Berlin"));
		JsonNode telemetry;
		try {
			doReturn(JacksonConfigurator.configureObjectMapper()).when(directProviderUtil).getObjectMapper(any(CIBUser.class));
			telemetry = new DirectSystemProvider(directProviderUtil).getTelemetryData(new CIBUser("demo"));
		} finally {
			java.util.TimeZone.setDefault(previous);
		}

		assertThat(telemetry.get("installation").asString()).isEqualTo("8343cc7a");
		assertThat(telemetry.get("product").get("name").asString()).isEqualTo("CIB seven");
		assertThat(telemetry.get("product").get("internals").get("database").get("vendor").asString()).isEqualTo("h2");
		// byte for byte what engine-rest 2.2 wrote in Europe/Berlin
		assertThat(telemetry.get("product").get("internals").get("data-collection-start-date").asString())
				.isEqualTo("1970-01-01T01:00:00.000+0100");
	}
}
