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

public interface IEngineProvider {
	public static final String DEFAULT_ENGINE_NAME = "default";

	public static boolean isDefaultEngine(String engine) {
		return engine == null || engine.isEmpty() || DEFAULT_ENGINE_NAME.equalsIgnoreCase(engine);
	}

	public static boolean isExternalEngine(String engine) {
		return engine != null && engine.contains("|");
	}

	public Collection<Engine> getProcessEngineNames();
	public EngineConfiguration getDefaultEngineConfiguration();
	public EngineConfiguration getEngineConfiguration(String engineName);
	public Boolean requiresSetup(String engine);
	public void createSetupUser(NewUser user, String engine) throws InvalidUserIdException;
}
