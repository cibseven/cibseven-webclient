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

import javax.sql.DataSource;

import org.cibseven.persistence.CibsevenJpa;
import org.cibseven.webapp.exception.FeatureDisabledException;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Whether the engine has the webclient tables, which hold the modeler, the chat and the
 * notifications. What the engine reports as not there stays off, whatever the webclient's own
 * properties say. What it reports as there, or does not report at all (engines before 3.0), is left
 * to those properties.
 * <p>
 * The tables share the database of the default engine, so its answer counts, also with several
 * engines. A host that gives them a database of their own ({@link CibsevenJpa#DATA_SOURCE}) leaves
 * the engine nothing to say about them, and the properties decide alone.
 * <p>
 * The answer is read at runtime, as the engine may not be reachable when the webclient starts, and
 * kept for a minute, so that the endpoints using these tables do not ask the engine on every call.
 * An engine that cannot be reached is asked again a minute later, and the last answer stays until then.
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
	public WebclientExtensions(BpmProvider bpmProvider,
			@Qualifier(CibsevenJpa.DATA_SOURCE) ObjectProvider<DataSource> ownDataSource) {
		this(ownDataSource.getIfAvailable() == null ? bpmProvider::getEffectiveDefaultEngineConfiguration : () -> null,
			System::currentTimeMillis);
		if (ownDataSource.getIfAvailable() != null) {
			log.info("The webclient tables have a database of their own, so the engine is not asked whether they are there");
		}
	}

	/** Asks the default engine, as when the webclient tables share its database. */
	public WebclientExtensions(BpmProvider bpmProvider) {
		this(bpmProvider::getEffectiveDefaultEngineConfiguration, System::currentTimeMillis);
	}

	WebclientExtensions(Supplier<EngineConfiguration> engineConfiguration, LongSupplier clock) {
		this.engineConfiguration = engineConfiguration;
		this.clock = clock;
	}

	/**
	 * The engine's answer as last read, read again once it is older than a minute.
	 *
	 * @return {@code true} or {@code false} as the engine reports it, {@code null} while it is not
	 *         reported or the engine has not been reached yet
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
		} catch (RuntimeException e) {
			log.debug("Could not read whether the engine has the webclient tables; keeping the last answer", e);
		}
		// A failure counts as an answer too: an engine that is down is not asked on every request
		readAt = clock.getAsLong();
		read = true;
		return engineSwitch;
	}

	/** Whether the engine reports the webclient tables as not there, as last read. */
	public boolean switchedOff() {
		return Boolean.FALSE.equals(engineSwitch());
	}

	/** Whether the engine reports the webclient tables as not there, asked now. */
	public boolean switchedOffNow() {
		return Boolean.FALSE.equals(readEngineSwitch());
	}

	/**
	 * A feature using the webclient tables: on when its own property is, unless the engine reports
	 * the tables as not there.
	 */
	public boolean enabled(boolean property) {
		return property && !switchedOff();
	}

	/**
	 * For the endpoints using the webclient tables, which would otherwise fail on tables that are
	 * not there.
	 *
	 * @throws FeatureDisabledException when the engine reports the webclient tables as not there
	 */
	public void require(String feature) {
		if (switchedOff()) {
			throw new FeatureDisabledException(feature);
		}
	}
}
