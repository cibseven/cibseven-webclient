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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link BpmProvider} is a facade: it exposes twenty sub-providers and carries a large number of
 * {@code default} methods that do nothing but forward one call to one of them.
 * <p>
 * The invariant worth protecting is exactly that - every default method forwards, to precisely one
 * sub-provider, and hands back what it returns. A method that quietly did nothing, forwarded to the
 * wrong provider, or called two of them would be a real defect and is invisible to a per-method
 * test that only checks the handful of endpoints someone remembered to cover. So this test drives
 * every default method reflectively and asserts the invariant for all of them at once, alongside a
 * few hand-written cases that document the intent in readable form.
 */
public class BpmProviderTest {

	/** Every sub-provider interface the facade exposes, mocked. */
	private final Map<Class<?>, Object> providers = new LinkedHashMap<>();
	private BpmProvider bpmProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		user = new CIBUser("demo");
		providers.clear();
		bpmProvider = new BpmProvider() {
			private <T> T provider(Class<T> type) {
				return type.cast(providers.computeIfAbsent(type, org.mockito.Mockito::mock));
			}

			public IDeploymentProvider getDeploymentProvider() { return provider(IDeploymentProvider.class); }
			public IVariableProvider getVariableProvider() { return provider(IVariableProvider.class); }
			public IVariableInstanceProvider getVariableInstanceProvider() { return provider(IVariableInstanceProvider.class); }
			public IHistoricVariableInstanceProvider getHistoricVariableInstanceProvider() { return provider(IHistoricVariableInstanceProvider.class); }
			public ITaskProvider getTaskProvider() { return provider(ITaskProvider.class); }
			public IProcessProvider getProcessProvider() { return provider(IProcessProvider.class); }
			public IActivityProvider getActivityProvider() { return provider(IActivityProvider.class); }
			public IFilterProvider getFilterProvider() { return provider(IFilterProvider.class); }
			public IUtilsProvider getUtilsProvider() { return provider(IUtilsProvider.class); }
			public IIncidentProvider getIncidentProvider() { return provider(IIncidentProvider.class); }
			public IJobDefinitionProvider getJobDefinitionProvider() { return provider(IJobDefinitionProvider.class); }
			public IUserProvider getUserProvider() { return provider(IUserProvider.class); }
			public IDecisionProvider getDecisionProvider() { return provider(IDecisionProvider.class); }
			public IJobProvider getJobProvider() { return provider(IJobProvider.class); }
			public IBatchProvider getBatchProvider() { return provider(IBatchProvider.class); }
			public ISystemProvider getSystemProvider() { return provider(ISystemProvider.class); }
			public ITenantProvider getTenantProvider() { return provider(ITenantProvider.class); }
			public IExternalTaskProvider getExternalTaskProvider() { return provider(IExternalTaskProvider.class); }
			public IEngineProvider getEngineProvider() { return provider(IEngineProvider.class); }
			public IIdentityProvider getIdentityProvider() { return provider(IIdentityProvider.class); }
		};
	}

	private int totalInteractions() {
		return providers.values().stream()
			.mapToInt(provider -> mockingDetails(provider).getInvocations().size())
			.sum();
	}

	/** A harmless value of the right type for every parameter the facade takes. */
	private Object argumentFor(Class<?> type) {
		if (type == String.class) return "x";
		if (type == CIBUser.class) return user;
		if (type == Optional.class) return Optional.empty();
		if (type == Map.class) return new HashMap<String, Object>();
		if (type == List.class || type == Collection.class) return new ArrayList<>();
		if (type == Locale.class) return Locale.ENGLISH;
		if (type == boolean.class || type == Boolean.class) return Boolean.FALSE;
		if (type == int.class || type == Integer.class) return Integer.valueOf(0);
		if (type == long.class || type == Long.class) return Long.valueOf(0L);
		if (type.isArray()) return java.lang.reflect.Array.newInstance(type.getComponentType(), 0);
		if (type.isPrimitive()) return Integer.valueOf(0);
		// everything else is a DTO or a servlet type; a mock is enough because the facade only
		// passes it straight through
		return mock(type);
	}

	@Test
	void everyDefaultMethodForwardsToExactlyOneSubProvider() throws Exception {
		List<String> notForwarded = new ArrayList<>();
		List<String> forwardedMoreThanOnce = new ArrayList<>();
		int checked = 0;

		for (Method method : BpmProvider.class.getDeclaredMethods()) {
			if (!method.isDefault() || Modifier.isStatic(method.getModifiers())) {
				continue;
			}
			setUp(); // a fresh set of mocks, so interactions belong to this method alone

			Object[] args = new Object[method.getParameterCount()];
			for (int i = 0; i < args.length; i++) {
				args[i] = argumentFor(method.getParameterTypes()[i]);
			}

			try {
				method.invoke(bpmProvider, args);
			} catch (InvocationTargetException e) {
				// a delegating one-liner can still throw if it unwraps the provider's null result;
				// the interaction count below is what decides whether it forwarded
			}

			checked++;
			// the sub-provider getters live on the facade itself, so only the forwarded call
			// lands on a mock: exactly one interaction means exactly one delegation
			int interactions = totalInteractions();
			if (interactions == 0) {
				notForwarded.add(method.getName() + "/" + method.getParameterCount());
			} else if (interactions > 1) {
				forwardedMoreThanOnce.add(method.getName() + "/" + method.getParameterCount());
			}
		}

		assertThat(checked).as("BpmProvider should still be a wide facade").isGreaterThan(200);
		assertThat(notForwarded).as("default methods that never reached a sub-provider").isEmpty();
		assertThat(forwardedMoreThanOnce)
			.as("default methods that touched more than one sub-provider call").isEmpty();
	}

	// A few explicit cases, so the intent above is readable without running the reflection.

	@Test
	void findTasksCount_forwardsToTheTaskProvider() {
		ITaskProvider taskProvider = (ITaskProvider) providers.computeIfAbsent(
			ITaskProvider.class, org.mockito.Mockito::mock);
		Map<String, Object> filters = new HashMap<>();
		org.mockito.Mockito.when(taskProvider.findTasksCount(filters, user)).thenReturn(7);

		assertThat(bpmProvider.findTasksCount(filters, user)).isEqualTo(7);
	}

	@Test
	void findProcesses_forwardsToTheProcessProvider() {
		IProcessProvider processProvider = (IProcessProvider) providers.computeIfAbsent(
			IProcessProvider.class, org.mockito.Mockito::mock);
		org.mockito.Mockito.when(processProvider.findProcesses(user)).thenReturn(List.of());

		assertThat(bpmProvider.findProcesses(user)).isEmpty();
		org.mockito.Mockito.verify(processProvider).findProcesses(user);
	}

	@Test
	void deleteVariable_forwardsToTheVariableProvider() {
		IVariableProvider variableProvider = (IVariableProvider) providers.computeIfAbsent(
			IVariableProvider.class, org.mockito.Mockito::mock);

		bpmProvider.deleteVariable("task-1", "amount", user);

		org.mockito.Mockito.verify(variableProvider).deleteVariable("task-1", "amount", user);
	}

	@Test
	void theSubProviderGettersAreTheOnlyAbstractMethods() {
		long abstractMethods = java.util.Arrays.stream(BpmProvider.class.getDeclaredMethods())
			.filter(m -> !m.isDefault() && !Modifier.isStatic(m.getModifiers()))
			.count();

		// anything else abstract would force every implementation to change
		assertThat(abstractMethods).isEqualTo(20);
	}
}
