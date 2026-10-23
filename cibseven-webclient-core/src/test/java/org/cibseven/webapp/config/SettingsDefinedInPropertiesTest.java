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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * {@link SevenWebclientProperties} is the single definition of what can be configured under
 * {@code cibseven.webclient} and of the defaults. These checks keep it that way.
 *
 * <ul>
 * <li>No {@code @Value("${cibseven.webclient...}")}: it would bring back a second, possibly
 * different default. Checked in the bytecode of cibseven-interfaces, cibseven-direct-provider
 * and cibseven-webclient-core, so classes whose optional dependencies are missing here are
 * covered as well.</li>
 * <li>No property name spelled out outside {@link SevenWebclientProperties}: annotations that
 * need a name before beans exist ({@code @ConditionalOnProperty}, {@code @RequestMapping},
 * {@code @Scheduled}) use its constants. Checked in the sources of all backend modules,
 * because the compiler inlines constants and the bytecode cannot tell them from literals.</li>
 * </ul>
 */
public class SettingsDefinedInPropertiesTest {

	private static final String PREFIX = "cibseven.webclient.";

	private static final String VALUE = Type.getDescriptor(Value.class);

	private static final List<String> BYTECODE_MODULES = List.of(
		"cibseven-interfaces", "cibseven-direct-provider", "cibseven-webclient-core");

	/** Backend modules of this repository, as sibling directories of this module */
	private static final List<String> SOURCE_MODULES = List.of(
		"cibseven-interfaces", "cibseven-direct-provider", "cibseven-webclient-core",
		"cibseven-webclient-web", "cibseven-webclient-web-sb4");

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

	@Test
	public void webclientSettingsAreNotInjectedWithValue() throws IOException {
		List<String> violations = new ArrayList<>();
		int scanned = 0;
		for (Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath*:org/cibseven/**/*.class")) {
			String location = resource.getURL().toString();
			// Only this repository's modules, and only their main classes
			if (location.contains("test-classes") || BYTECODE_MODULES.stream().noneMatch(location::contains)) {
				continue;
			}
			try (InputStream in = resource.getInputStream()) {
				new ClassReader(in).accept(new ValueCollector(violations), ClassReader.SKIP_CODE);
			}
			scanned++;
		}

		// Guards against a scan that silently finds nothing
		assertTrue(scanned > 100, "only " + scanned + " classes found in " + BYTECODE_MODULES);
		assertEquals(List.of(), violations, "use SevenWebclientProperties instead of @Value for " + PREFIX + "*");
	}

	private static class ValueCollector extends ClassVisitor {

		private final List<String> violations;
		private String className;

		ValueCollector(List<String> violations) {
			super(Opcodes.ASM9);
			this.violations = violations;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			className = Type.getObjectType(name).getClassName();
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			String location = className + "." + name;
			return new FieldVisitor(Opcodes.ASM9) {
				@Override
				public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
					return collect(annotation, location);
				}
			};
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			String location = className + "." + name + "()";
			return new MethodVisitor(Opcodes.ASM9) {
				@Override
				public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
					return collect(annotation, location);
				}

				@Override
				public AnnotationVisitor visitParameterAnnotation(int parameter, String annotation, boolean visible) {
					return collect(annotation, location + " parameter " + parameter);
				}
			};
		}

		private AnnotationVisitor collect(String annotation, String location) {
			if (!VALUE.equals(annotation)) {
				return null;
			}
			return new AnnotationVisitor(Opcodes.ASM9) {
				@Override
				public void visit(String name, Object value) {
					if (value instanceof String expression && expression.contains(PREFIX)) {
						violations.add(location + ": " + expression);
					}
				}
			};
		}
	}
}
