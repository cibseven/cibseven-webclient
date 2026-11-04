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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.cibseven.webapp.exception.FeatureDisabledException;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.junit.jupiter.api.Test;

class WebclientExtensionsTest {

	private final AtomicLong now = new AtomicLong(1_000);
	private final AtomicInteger asked = new AtomicInteger();

	private WebclientExtensions reporting(Boolean engineSwitch) {
		return asking(() -> {
			EngineConfiguration configuration = new EngineConfiguration();
			configuration.setWebclientExtensionsEnabled(engineSwitch);
			return configuration;
		});
	}

	private WebclientExtensions asking(Supplier<EngineConfiguration> engine) {
		return new WebclientExtensions(() -> {
			asked.incrementAndGet();
			return engine.get();
		}, now::get);
	}

	@Test
	void switchedOffByTheEngineKeepsEveryFeatureOff() {
		WebclientExtensions extensions = reporting(false);

		assertThat(extensions.engineSwitch()).isFalse();
		assertThat(extensions.switchedOff()).isTrue();
		assertThat(extensions.enabled(true)).isFalse();
		assertThatThrownBy(() -> extensions.require("modeler"))
			.isInstanceOf(FeatureDisabledException.class)
			.satisfies(e -> assertThat(((FeatureDisabledException) e).getData()).containsExactly("modeler"));
	}

	@Test
	void switchedOnByTheEngineLeavesTheFeaturesToTheirProperties() {
		WebclientExtensions extensions = reporting(true);

		assertThat(extensions.enabled(true)).isTrue();
		assertThat(extensions.enabled(false)).isFalse();
		assertThatCode(() -> extensions.require("modeler")).doesNotThrowAnyException();
	}

	/** Engines before 3.0 do not report the switch, and an engine without the endpoint none at all. */
	@Test
	void notReportedLeavesTheFeaturesToTheirProperties() {
		for (WebclientExtensions extensions : new WebclientExtensions[] { reporting(null), asking(() -> null) }) {
			assertThat(extensions.engineSwitch()).isNull();
			assertThat(extensions.enabled(true)).isTrue();
			assertThat(extensions.enabled(false)).isFalse();
			assertThatCode(() -> extensions.require("modeler")).doesNotThrowAnyException();
		}
	}

	@Test
	void keepsTheAnswerForAMinute() {
		WebclientExtensions extensions = reporting(false);

		extensions.engineSwitch();
		now.addAndGet(WebclientExtensions.KEPT_MILLIS - 1);
		extensions.switchedOff();
		assertThat(asked).hasValue(1);

		now.addAndGet(1);
		extensions.switchedOff();
		assertThat(asked).hasValue(2);
	}

	@Test
	void readingNowAlwaysAsksTheEngine() {
		WebclientExtensions extensions = reporting(true);

		extensions.readEngineSwitch();
		extensions.readEngineSwitch();

		assertThat(asked).hasValue(2);
	}

	/** It may not be up yet; the features behave as before until it answers. */
	@Test
	void asksAnEngineThatCannotBeReachedAgainNextTime() {
		AtomicInteger calls = new AtomicInteger();
		WebclientExtensions extensions = asking(() -> {
			if (calls.incrementAndGet() == 1) {
				throw new IllegalStateException("connection refused");
			}
			EngineConfiguration configuration = new EngineConfiguration();
			configuration.setWebclientExtensionsEnabled(false);
			return configuration;
		});

		assertThat(extensions.engineSwitch()).isNull();
		assertThat(extensions.enabled(true)).isFalse();
		assertThat(asked).hasValue(2);
	}

	@Test
	void unknownNeverAsks() {
		assertThat(WebclientExtensions.UNKNOWN.engineSwitch()).isNull();
		assertThat(WebclientExtensions.UNKNOWN.enabled(true)).isTrue();
	}

	@Test
	void asksTheDefaultEngine() {
		BpmProvider bpmProvider = mock(BpmProvider.class);
		EngineConfiguration configuration = new EngineConfiguration();
		configuration.setWebclientExtensionsEnabled(false);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(configuration);

		assertThat(new WebclientExtensions(bpmProvider).switchedOff()).isTrue();
	}
}
