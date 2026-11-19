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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TenantProviderTest {

	private MockEngineRest engine;
	private TenantProvider tenantProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		tenantProvider = engine.wire(new TenantProvider());
		user = MockEngineRest.user();
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	@Test
	void fetchTenant_mapsTheEngineTenant() throws Exception {
		engine.enqueueJson("{\"id\":\"tenant-a\",\"name\":\"Tenant A\"}");

		Tenant tenant = tenantProvider.fetchTenant("tenant-a", user);

		assertThat(tenant.getName()).isEqualTo("Tenant A");
		assertThat(engine.takePath()).isEqualTo("/tenant/tenant-a");
	}

	@Test
	void fetchTenant_throwsWhenTheTenantDoesNotExist() {
		engine.enqueueStatus(404,
			"{\"type\":\"InvalidRequestException\",\"message\":\"Tenant with id missing does not exist\"}");

		// answered as 404 by the ExceptionConverter, like the direct provider does (CIB7-2375)
		assertThatThrownBy(() -> tenantProvider.fetchTenant("missing", user))
			.isInstanceOf(NoObjectFoundException.class);
	}

	@Test
	void fetchTenant_keepsOtherEngineErrorsAsSystemException() {
		engine.enqueueStatus(500, "{\"type\":\"ProcessEngineException\",\"message\":\"db down\"}");

		assertThatThrownBy(() -> tenantProvider.fetchTenant("tenant-a", user))
			.isInstanceOf(SystemException.class);
	}
}
