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

import org.cibseven.webapp.auth.CIBUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobProviderTest {

	private static final String NON_ASCII = "Unknown property used in expression: ${Größe_姓名}\n\tat äöüß";

	private MockEngineRest engine;
	private JobProvider jobProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		jobProvider = engine.wire(new JobProvider());
		user = MockEngineRest.user();
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	@Test
	void getHistoryJobLogStacktrace_decodesUtf8WithoutCharset() throws Exception {
		// engine-rest sends stack traces as text/plain without a charset
		engine.enqueuePlainText(NON_ASCII);

		assertThat(jobProvider.getHistoryJobLogStacktrace("id-1", user)).isEqualTo(NON_ASCII);
		assertThat(engine.takePath()).isEqualTo("/history/job-log/id-1/stacktrace");
	}
}
