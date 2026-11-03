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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.cibseven.webapp.SevenWebclientApplication;
import org.cibseven.webapp.rest.InfoService;
import org.springframework.beans.factory.annotation.Value;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * {@link SevenWebclientProperties} is the single definition of what can be configured under
 * {@code cibseven.webclient} and of the defaults. A {@code @Value("${cibseven.webclient...}")}
 * would bring back a second, possibly different default, so none is allowed on fields, methods,
 * constructors or their parameters.
 *
 * <p>Runs in this module because it sees the main classes of every backend module. ArchUnit reads
 * the bytecode without loading it, so classes whose optional dependencies are missing are covered
 * too. Property names spelled out as string literals are checked in the sources by
 * {@code SettingsDefinedInPropertiesTest} in cibseven-webclient-core.</p>
 */
@AnalyzeClasses(packages = "org.cibseven", importOptions = WebclientSettingsArchTest.BackendMainClasses.class)
class WebclientSettingsArchTest {

	/** The main classes of this repository's backend modules, from their jars or class folders */
	static class BackendMainClasses implements ImportOption {

		private static final List<String> MODULES = List.of(
			"cibseven-interfaces", "cibseven-webclient-core", "cibseven-webclient-web");

		private static final ImportOption MAIN = new ImportOption.DoNotIncludeTests();

		@Override
		public boolean includes(Location location) {
			return MAIN.includes(location) && MODULES.stream().anyMatch(location::contains);
		}
	}

	private static final DescribedPredicate<JavaAnnotation<?>> WEBCLIENT_VALUE = DescribedPredicate.describe(
		"@Value for " + SevenWebclientProperties.PREFIX + ".*",
		annotation -> annotation.getRawType().isEquivalentTo(Value.class)
			&& annotation.get("value").map(Object::toString).orElse("").contains(SevenWebclientProperties.PREFIX + "."));

	private static final ArchCondition<JavaCodeUnit> HAVE_A_PARAMETER_WITH_WEBCLIENT_VALUE =
		new ArchCondition<>("have a parameter annotated with " + WEBCLIENT_VALUE.getDescription()) {
			@Override
			public void check(JavaCodeUnit codeUnit, ConditionEvents events) {
				for (JavaParameter parameter : codeUnit.getParameters()) {
					boolean violated = parameter.getAnnotations().stream().anyMatch(WEBCLIENT_VALUE);
					events.add(new SimpleConditionEvent(parameter, violated,
						parameter.getDescription() + " is annotated with " + WEBCLIENT_VALUE.getDescription()));
				}
			}
		};

	@ArchTest
	static final ArchRule fieldsDoNotInjectWebclientSettings =
		noFields().should().beAnnotatedWith(WEBCLIENT_VALUE)
			.because("the settings are bound into SevenWebclientProperties");

	@ArchTest
	static final ArchRule codeUnitsDoNotInjectWebclientSettings =
		noCodeUnits().should().beAnnotatedWith(WEBCLIENT_VALUE)
			.orShould(HAVE_A_PARAMETER_WITH_WEBCLIENT_VALUE)
			.because("the settings are bound into SevenWebclientProperties");

	/** Guards against an import option that silently leaves a module out */
	@ArchTest
	static void everyBackendModuleIsAnalyzed(JavaClasses classes) {
		// One class of each module, in the order of BackendMainClasses.MODULES
		for (Class<?> type : List.of(SevenWebclientProperties.class, InfoService.class,
				SevenWebclientApplication.class)) {
			assertTrue(classes.contain(type), type.getName() + " was not analyzed");
		}
	}
}
