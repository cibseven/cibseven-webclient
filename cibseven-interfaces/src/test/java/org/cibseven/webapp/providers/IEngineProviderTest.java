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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.rest.model.Engine;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.cibseven.webapp.rest.model.NewUser;
import org.junit.jupiter.api.Test;

/**
 * Covers how {@link IEngineProvider} decides which engine is the <em>effective default</em> - the
 * one the webclient talks to when a request names no engine.
 */
public class IEngineProviderTest {

	/** Serves a fixed engine list and a configuration per engine id. */
	private static class StubEngineProvider implements IEngineProvider {
		private final Collection<Engine> engines;
		private final Map<String, EngineConfiguration> configurations;

		StubEngineProvider(Collection<Engine> engines, Map<String, EngineConfiguration> configurations) {
			this.engines = engines;
			this.configurations = configurations;
		}

		@Override
		public Collection<Engine> getProcessEngineDefinitions() {
			return engines;
		}

		@Override
		public EngineConfiguration getEngineConfiguration(String engineId) {
			return configurations.get(engineId);
		}

		@Override
		public Boolean requiresSetup(String engineId) {
			return Boolean.FALSE;
		}

		@Override
		public void createSetupUser(NewUser user, String engineId) throws InvalidUserIdException {
			// not part of the default-method contract under test
		}
	}

	private static Engine engine(String id, String name) {
		Engine engine = new Engine(name);
		engine.setId(id);
		return engine;
	}

	// ---------- the static helpers ----------

	@Test
	void isEngineUnspecified_treatsNullAndEmptyAsUnspecified() {
		assertThat(IEngineProvider.isEngineUnspecified(null)).isTrue();
		assertThat(IEngineProvider.isEngineUnspecified("")).isTrue();
		assertThat(IEngineProvider.isEngineUnspecified("default")).isFalse();
		assertThat(IEngineProvider.isEngineUnspecified("alpha")).isFalse();
	}

	@Test
	void isNamedDefaultEngine_matchesTheLiteralNameIgnoringCase() {
		assertThat(IEngineProvider.isNamedDefaultEngine("default")).isTrue();
		assertThat(IEngineProvider.isNamedDefaultEngine("DEFAULT")).isTrue();
		assertThat(IEngineProvider.isNamedDefaultEngine("alpha")).isFalse();
		// distinct from isEngineUnspecified: no name is not the name "default"
		assertThat(IEngineProvider.isNamedDefaultEngine(null)).isFalse();
		assertThat(IEngineProvider.isNamedDefaultEngine("")).isFalse();
	}

	@Test
	void isExternalEngine_recognisesThePipeSeparatedReference() {
		assertThat(IEngineProvider.isExternalEngine("http://host|/engine-rest|remote")).isTrue();
		assertThat(IEngineProvider.isExternalEngine("alpha")).isFalse();
		assertThat(IEngineProvider.isExternalEngine(null)).isFalse();
	}

	// ---------- the effective default ----------

	@Test
	void getEffectiveDefaultEngineId_prefersTheEngineNamedDefault() {
		StubEngineProvider provider = new StubEngineProvider(
			List.of(engine("alpha", "alpha"), engine("default", "default")), Map.of());

		assertThat(provider.getEffectiveDefaultEngineId()).isEqualTo("default");
	}

	@Test
	void getEffectiveDefaultEngineId_fallsBackToTheFirstEngine() {
		StubEngineProvider provider = new StubEngineProvider(
			List.of(engine("alpha", "alpha"), engine("beta", "beta")), Map.of());

		assertThat(provider.getEffectiveDefaultEngineId()).isEqualTo("alpha");
	}

	@Test
	void getEffectiveDefaultEngineId_isNullWhenThereAreNoEngines() {
		StubEngineProvider provider = new StubEngineProvider(List.of(), Map.of());

		assertThat(provider.getEffectiveDefaultEngineId()).isNull();
	}

	@Test
	void getEffectiveDefaultEngineConfiguration_returnsTheConfigurationOfThatEngine() {
		EngineConfiguration configuration = new EngineConfiguration("default", "full", true, false);
		StubEngineProvider provider = new StubEngineProvider(
			List.of(engine("alpha", "alpha"), engine("default", "default")),
			Map.of("default", configuration));

		assertThat(provider.getEffectiveDefaultEngineConfiguration()).isSameAs(configuration);
	}

	@Test
	void getEffectiveDefaultEngineConfiguration_isNullWhenThereIsNoEngineToAsk() {
		StubEngineProvider provider = new StubEngineProvider(List.of(), Map.of());

		// must not call getEngineConfiguration(null) and must not throw
		assertThat(provider.getEffectiveDefaultEngineConfiguration()).isNull();
	}
}
