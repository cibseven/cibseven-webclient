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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Warns once at startup about webclient settings that were removed and are ignored now, so a
 * leftover setting does not go unnoticed. Found in every form Spring Boot accepts (kebab case,
 * camel case, environment variable).
 */
@Component
@Slf4j
public class RemovedSettingsWarning {

	/** Removed settings and what applies instead. */
	static final Map<String, String> REMOVED = new LinkedHashMap<>();

	static {
		REMOVED.put(SevenWebclientProperties.PREFIX + ".custom.spring.jackson.serialization.write-dates-as-timestamps",
				"ignored since 3.0, dates are ISO-8601");
	}

	public RemovedSettingsWarning(Environment environment) {
		removedSettingsIn(environment).forEach((setting, instead) -> log.warn("{} is set but {}", setting, instead));
	}

	/** The removed settings that are still set, with what applies instead. */
	static Map<String, String> removedSettingsIn(Environment environment) {
		Binder binder = Binder.get(environment);
		Map<String, String> set = new LinkedHashMap<>();
		REMOVED.forEach((setting, instead) -> {
			if (binder.bind(setting, Bindable.of(String.class)).isBound()) {
				set.put(setting, instead);
			}
		});
		return set;
	}
}
