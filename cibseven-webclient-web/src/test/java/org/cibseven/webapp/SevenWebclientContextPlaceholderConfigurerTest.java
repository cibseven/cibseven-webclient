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
package org.cibseven.webapp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Guards against a second PropertySourcesPlaceholderConfigurer next to Spring Boot's (CIB7-1993).
 */
class SevenWebclientContextPlaceholderConfigurerTest {

	// Test-only secret; the user provider refuses to start without a base64 secret of this length.
	private static final String TEST_JWT_SECRET = "X2juIHYcym5ztMnJ86lt5vsk9TPj8y81wJk6OCwZILOqxC3BKCopont9lCC2mKOiVQTSBciVuBXwoqJRFS08VfoP8zsKiLTtF5lZigFB5ykkWsNIFdY9938V0UuUx2hPfeIZwLvftsYVcpAGDXTMVJssENq";

	@Test
	void registersExactlyOnePlaceholderConfigurer() {
		new ApplicationContextRunner()
				.withPropertyValues("cibseven.webclient.authentication.jwtSecret=" + TEST_JWT_SECRET)
				.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
				.withUserConfiguration(SevenWebclientContext.class)
				.run(ctx -> assertThat(ctx.getBeansOfType(PropertySourcesPlaceholderConfigurer.class)).hasSize(1));
	}
}
