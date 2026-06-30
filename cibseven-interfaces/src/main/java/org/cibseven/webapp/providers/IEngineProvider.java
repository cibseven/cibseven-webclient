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

import java.util.Collection;

import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.rest.model.Engine;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.cibseven.webapp.rest.model.NewUser;

import org.springframework.lang.Nullable;

public interface IEngineProvider {
	/** The literal name of the engine called "default" in the engine-rest API. */
	public static final String ENGINE_NAME_DEFAULT = "default";

	/**
	 * Whether no engine was specified (null or empty).
	 */
	public static boolean isEngineUnspecified(String engine) {
		return engine == null || engine.isEmpty();
	}

	/**
	 * Whether the given reference points to the engine literally named "default".
	 * This is distinct from {@link #isEngineUnspecified(String)}: it requires the explicit name
	 * "default"
	 */
	public static boolean isNamedDefaultEngine(String engine) {
		return ENGINE_NAME_DEFAULT.equalsIgnoreCase(engine);
	}

	public static boolean isExternalEngine(String engine) {
		return engine != null && engine.contains("|");
	}

	public Collection<Engine> getProcessEngineDefinitions();

	/**
	 * Returns the configuration of the <em>effective default</em> engine, i.e. the engine the webclient
	 * uses when none is specified: the engine named "default" if present, otherwise the first available engine.
	 */
	public default EngineConfiguration getEffectiveDefaultEngineConfiguration() {
		return getEffectiveDefaultEngineId() == null ? null : getEngineConfiguration(getEffectiveDefaultEngineId());
	}

	public default String getEffectiveDefaultEngineId() {
		Collection<Engine> engines = getProcessEngineDefinitions();
		String effectiveDefaultEngineId = null;
		for (Engine engine : engines) {
			if (IEngineProvider.isNamedDefaultEngine(engine.getName())) {
				effectiveDefaultEngineId = engine.getId();
				break;
			}
		}
		if (effectiveDefaultEngineId == null && !engines.isEmpty()) {
			// If no engine is explicitly named "default", pick the first one as the effective default
			effectiveDefaultEngineId = engines.iterator().next().getId();
		}
		return effectiveDefaultEngineId;
	}
	
	@Nullable public EngineConfiguration getEngineConfiguration(String engineId);
	public Boolean requiresSetup(String engineId);
	public void createSetupUser(NewUser user, String engineId) throws InvalidUserIdException;
}
