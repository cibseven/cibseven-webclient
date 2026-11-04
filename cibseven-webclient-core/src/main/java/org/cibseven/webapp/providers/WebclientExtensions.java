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

import java.time.Duration;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.cibseven.webapp.exception.FeatureDisabledException;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Whether the engine has the webclient tables, which hold the modeler, the chat and the
 * notifications. The engine's {@code webclientExtensionsEnabled} comes first: what the engine has
 * switched off stays off, whatever the webclient's own properties say. What it reports as on, or
 * does not report at all (engines before 3.0), is left to those properties.
 * <p>
 * All engines share the one datasource of these tables, so the default engine's switch counts. It
 * is read at runtime, as the engine may not be reachable when the webclient starts, and kept for a
 * minute so that the endpoints using these tables do not ask the engine on every call. An engine
 * that cannot be reached is asked again next time.
 */
@Component
@Slf4j
public class WebclientExtensions {

	/** Never asks an engine, so the switch stays unknown: for a service built by hand. */
	public static final WebclientExtensions UNKNOWN = new WebclientExtensions(() -> null, System::currentTimeMillis);

	static final long KEPT_MILLIS = Duration.ofMinutes(1).toMillis();

	private final Supplier<EngineConfiguration> engineConfiguration;
	private final LongSupplier clock;

	private volatile Boolean engineSwitch;
	private volatile long readAt;
	private volatile boolean read;

	@Autowired
	public WebclientExtensions(BpmProvider bpmProvider) {
		this(bpmProvider::getEffectiveDefaultEngineConfiguration, System::currentTimeMillis);
	}

	WebclientExtensions(Supplier<EngineConfiguration> engineConfiguration, LongSupplier clock) {
		this.engineConfiguration = engineConfiguration;
		this.clock = clock;
	}

	/**
	 * The engine's switch as last read, read again once it is older than a minute.
	 *
	 * @return {@code true} or {@code false} as the engine reports it, {@code null} while it is not
	 *         reported or the engine cannot be reached
	 */
	@Nullable
	public Boolean engineSwitch() {
		if (read && clock.getAsLong() - readAt < KEPT_MILLIS) {
			return engineSwitch;
		}
		return readEngineSwitch();
	}

	/** Asks the engine now, for work that waits for its answer, such as loading data at startup. */
	@Nullable
	public Boolean readEngineSwitch() {
		try {
			EngineConfiguration configuration = engineConfiguration.get();
			engineSwitch = configuration == null ? null : configuration.getWebclientExtensionsEnabled();
			readAt = clock.getAsLong();
			read = true;
		} catch (RuntimeException e) {
			log.debug("Could not read whether the engine has the webclient tables; asking again next time", e);
		}
		return engineSwitch;
	}

	/** Whether the engine has switched the webclient tables off. */
	public boolean switchedOff() {
		return Boolean.FALSE.equals(engineSwitch());
	}

	/**
	 * A feature using the webclient tables: on when its own property is, unless the engine has
	 * switched the tables off.
	 */
	public boolean enabled(boolean property) {
		return property && !switchedOff();
	}

	/**
	 * For the endpoints using the webclient tables, which would otherwise fail on tables that are
	 * not there.
	 *
	 * @throws FeatureDisabledException when the engine has switched the webclient tables off
	 */
	public void require(String feature) {
		if (switchedOff()) {
			throw new FeatureDisabledException(feature);
		}
	}
}
