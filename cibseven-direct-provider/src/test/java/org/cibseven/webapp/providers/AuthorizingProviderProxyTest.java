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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.PermissionDeniedException;
import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The engine classes are not on the REST layer's classpath, so the proxy is where a permission the engine
 * refuses becomes the exception answered with 403.
 */
class AuthorizingProviderProxyTest {

	interface Provider {
		String asUser(CIBUser user);
		String withoutUser();
	}

	private final CIBUser user = new CIBUser("demo");
	private DirectProviderUtil util;
	private Provider target;
	private Provider proxy;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		util = mock(DirectProviderUtil.class);
		when(util.runAsUser(eq(user), any(Supplier.class))).thenAnswer(call -> ((Supplier<Object>) call.getArgument(1)).get());
		target = mock(Provider.class);
		proxy = AuthorizingProviderProxy.wrap(target, Provider.class, util);
	}

	@Test
	void aRefusedPermissionBecomesAPermissionDeniedException() {
		AuthorizationException denied = new AuthorizationException("demo may not update task-1");
		when(target.asUser(user)).thenThrow(denied);

		assertThatThrownBy(() -> proxy.asUser(user))
			.isInstanceOf(PermissionDeniedException.class)
			// still a SystemException, for the callers catching the engine failures as one
			.isInstanceOf(SystemException.class)
			.hasMessage("demo may not update task-1")
			.hasCause(denied);
	}

	@Test
	void aRefusedPermissionBecomesAPermissionDeniedExceptionWithoutAUserToo() {
		when(target.withoutUser()).thenThrow(new AuthorizationException("not allowed"));

		assertThatThrownBy(() -> proxy.withoutUser())
			.isInstanceOf(PermissionDeniedException.class)
			.hasMessage("not allowed");
	}

	@Test
	void otherExceptionsReachTheCallerUnchanged() {
		NoObjectFoundException missing = new NoObjectFoundException(new SystemException("gone"));
		when(target.asUser(user)).thenThrow(missing);

		assertThatThrownBy(() -> proxy.asUser(user)).isSameAs(missing);
	}

	@Test
	void theResultOfTheCallIsReturned() {
		when(target.asUser(user)).thenReturn("result");

		assertThat(proxy.asUser(user)).isEqualTo("result");
	}
}
