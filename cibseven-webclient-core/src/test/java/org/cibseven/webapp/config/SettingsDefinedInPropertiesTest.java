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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * {@link SevenWebclientProperties} is the single definition of what can be configured under
 * {@code cibseven.webclient} and of the defaults. No property name is spelled out anywhere else:
 * annotations that need a name before beans exist ({@code @ConditionalOnProperty},
 * {@code @RequestMapping}, {@code @Scheduled}) use its constants. Checked in the sources of all
 * backend modules, because the compiler inlines constants and the bytecode cannot tell them from
 * literals. {@code @Value} is checked by {@code WebclientSettingsArchTest} in cibseven-webclient-web.
 */
public class SettingsDefinedInPropertiesTest {

	private static final String PREFIX = "cibseven.webclient.";

	/** Backend modules of this repository, as sibling directories of this module */
	private static final List<String> SOURCE_MODULES = List.of(
		"cibseven-interfaces", "cibseven-direct-provider", "cibseven-webclient-core",
		"cibseven-webclient-web");

	private static final Pattern LITERAL = Pattern.compile("\"[^\"\n]*" + Pattern.quote(SevenWebclientProperties.PREFIX) + "[^\"\n]*\"");

	// Simple on purpose: a comment marker inside a string literal can only hide a violation, not invent one
	private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/|//[^\n]*", Pattern.DOTALL);

	@Test
	public void propertyNamesAreOnlySpelledOutInTheProperties() throws IOException {
		List<String> violations = new ArrayList<>();
		int scanned = 0;
		for (String module : SOURCE_MODULES) {
			Path sources = Path.of("..", module, "src", "main", "java");
			try (Stream<Path> files = Files.walk(sources)) {
				for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
					scanned++;
					if (file.endsWith("SevenWebclientProperties.java")) {
						continue;
					}
					String code = COMMENT.matcher(Files.readString(file)).replaceAll("");
					Matcher literal = LITERAL.matcher(code);
					while (literal.find()) {
						violations.add(sources.relativize(file) + ": " + literal.group());
					}
				}
			}
		}

		assertTrue(scanned > 100, "only " + scanned + " source files found in " + SOURCE_MODULES);
		assertEquals(List.of(), violations, "use the constants of SevenWebclientProperties for " + PREFIX + "*");
	}
}
