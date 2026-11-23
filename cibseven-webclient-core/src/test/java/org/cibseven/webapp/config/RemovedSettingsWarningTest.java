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
package org.cibseven.webapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

/**
 * The interim opt-out of the ISO-8601 dates is gone with Jackson 3 (CIB7-1535); a leftover setting is
 * ignored, but reported.
 */
class RemovedSettingsWarningTest {

	private static final String WRITE_DATES_AS_TIMESTAMPS =
			"cibseven.webclient.custom.spring.jackson.serialization.write-dates-as-timestamps";

	@Test
	void reportsTheRemovedDateSetting() {
		MockEnvironment environment = new MockEnvironment().withProperty(WRITE_DATES_AS_TIMESTAMPS, "true");

		assertThat(RemovedSettingsWarning.removedSettingsIn(environment))
				.containsExactly(Map.entry(WRITE_DATES_AS_TIMESTAMPS, "ignored since 3.0, dates are ISO-8601"));
		// and starts anyway
		new RemovedSettingsWarning(environment);
	}

	@Test
	void findsItInCamelCaseAndAsEnvironmentVariable() {
		MockEnvironment camelCase = new MockEnvironment()
				.withProperty("cibseven.webclient.custom.spring.jackson.serialization.writeDatesAsTimestamps", "false");
		StandardEnvironment variable = new StandardEnvironment();
		variable.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(
				"CIBSEVEN_WEBCLIENT_CUSTOM_SPRING_JACKSON_SERIALIZATION_WRITEDATESASTIMESTAMPS", "true")));

		assertThat(RemovedSettingsWarning.removedSettingsIn(camelCase)).containsKey(WRITE_DATES_AS_TIMESTAMPS);
		assertThat(RemovedSettingsWarning.removedSettingsIn(variable)).containsKey(WRITE_DATES_AS_TIMESTAMPS);
	}

	@Test
	void staysQuietWithoutIt() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("cibseven.webclient.custom.spring.jackson.parser.max-size", "100");
		environment.getPropertySources().addLast(new MapPropertySource("other", Map.of("spring.jackson.serialization.write-dates-as-timestamps", "true")));

		assertThat(RemovedSettingsWarning.removedSettingsIn(environment)).isEmpty();
	}
}
