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

import org.cibseven.templates.uilocator.ExternalUiTask;
import org.cibseven.templates.uilocator.repository.EngineClient;
import org.cibseven.templates.uilocator.repository.ExternalUiTaskProvider;
import org.cibseven.templates.uilocator.repository.MemoryExternalUiTaskProvider;
import org.cibseven.templates.uilocator.repository.SevenEngineRestClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UiLocatorWebProperties.class)
public class UiLocatorConfiguration {

	@Bean
	EngineClient engineClient(UiLocatorWebProperties properties) {
		return new SevenEngineRestClient(properties.getEngineRestUrl());
	}

	@Bean
	ExternalUiTaskProvider externalUiTaskProvider(UiLocatorWebProperties properties) {
		MemoryExternalUiTaskProvider provider = new MemoryExternalUiTaskProvider();
		for (ExternalUiTask task : properties.getTasks()) {
			if (task != null && task.getKey() != null && !task.getKey().isBlank()) {
				provider.addExternalUiTask(task);
			}
		}
		return provider;
	}
}