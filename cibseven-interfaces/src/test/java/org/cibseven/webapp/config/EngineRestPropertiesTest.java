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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Engine REST servers are looked up in the configured sources only.
 */
class EngineRestPropertiesTest {

	private EngineRestProperties properties;
	private EngineRestSource dev;

	@BeforeEach
	void setUp() {
		properties = new EngineRestProperties();
		properties.setUrl("http://localhost:8080");
		properties.setPath("/engine-rest");
		dev = source("https://dev.example.com/", "/engine-rest", "secret-dev");
		properties.getAdditionalEngineRest().add(dev);
	}

	@Test
	void findsAConfiguredAdditionalSource() {
		assertThat(properties.findEngineRestSource("https://dev.example.com", "/engine-rest")).containsSame(dev);
	}

	@Test
	void ignoresATrailingSlashOnTheUrlAndAMissingLeadingSlashOnThePath() {
		assertThat(properties.findEngineRestSource("https://dev.example.com/", "engine-rest")).containsSame(dev);
	}

	@Test
	void findsTheDefaultSource() {
		Optional<EngineRestSource> found = properties.findEngineRestSource("http://localhost:8080/", "engine-rest");

		assertThat(found).isPresent();
		assertThat(found.get().getUrl()).isEqualTo("http://localhost:8080");
		assertThat(found.get().getJwtSecret()).isNull();
	}

	@Test
	void prefersAnAdditionalSourceWithTheDefaultUrl() {
		EngineRestSource sameAsDefault = source("http://localhost:8080", "/engine-rest", "secret-local");
		properties.getAdditionalEngineRest().add(0, sameAsDefault);

		assertThat(properties.findEngineRestSource("http://localhost:8080", "/engine-rest")).containsSame(sameAsDefault);
	}

	@Test
	void findsNoUnconfiguredServer() {
		assertThat(properties.findEngineRestSource("http://unknown.example:8080", "/engine-rest")).isEmpty();
		assertThat(properties.findEngineRestSource("http://other.example", "/engine-rest")).isEmpty();
	}

	@Test
	void rejectsAConfiguredHostWithAnotherPath() {
		assertThat(properties.findEngineRestSource("https://dev.example.com", "/actuator")).isEmpty();
	}

	@Test
	void rejectsAUrlThatOnlyStartsLikeAConfiguredOne() {
		assertThat(properties.findEngineRestSource("https://dev.example.com.other.test", "/engine-rest")).isEmpty();
		assertThat(properties.findEngineRestSource("https://dev.example.com@other.test", "/engine-rest")).isEmpty();
	}

	@Test
	void rejectsANullUrl() {
		assertThat(properties.findEngineRestSource(null, "/engine-rest")).isEmpty();
	}

	@Test
	void skipsASourceWithoutUrlAndANullList() {
		List<EngineRestSource> sources = new ArrayList<>();
		sources.add(null);
		sources.add(new EngineRestSource());
		properties.setAdditionalEngineRest(sources);
		assertThat(properties.findEngineRestSource("https://dev.example.com", "/engine-rest")).isEmpty();

		properties.setAdditionalEngineRest(null);
		assertThat(properties.findEngineRestSource("http://localhost:8080", "/engine-rest")).isPresent();
	}

	@Test
	void anEmptyPathStandsForTheDefaultRestPath() {
		EngineRestSource noPath = source("https://qa.example.com", null, null);
		properties.getAdditionalEngineRest().add(noPath);

		assertThat(noPath.effectivePath()).isEqualTo("/engine-rest");
		assertThat(properties.findEngineRestSource("https://qa.example.com", "")).containsSame(noPath);
		assertThat(properties.findEngineRestSource("https://qa.example.com", null)).containsSame(noPath);
	}

	private static EngineRestSource source(String url, String path, String jwtSecret) {
		EngineRestSource source = new EngineRestSource();
		source.setUrl(url);
		source.setPath(path);
		source.setJwtSecret(jwtSecret);
		return source;
	}
}
