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
package org.cibseven.webapp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The webclient has to build and release without the CIB seven engine. The engine ships
 * cibseven-direct-provider on top of cibseven-interfaces, so an engine dependency here - even a
 * test-scoped one - brings back the cycle removed in CIB7-1464.
 * <p>
 * Jars are imported on purpose: cibseven-direct-provider kept the {@code org.cibseven.webapp.providers}
 * package when it moved to the engine repo, so if it reaches this module's classpath again, its
 * classes are checked as well and fail the rule.
 */
@AnalyzeClasses(packages = { "org.cibseven.webapp", "org.cibseven.modeler", "org.cibseven.persistence" })
public class NoEngineDependencyTest {

	@ArchTest
	public static final ArchRule webclient_does_not_depend_on_the_engine = noClasses()
		.should().dependOnClassesThat().resideInAnyPackage(
			"org.cibseven.bpm..",
			"org.cibseven.spin..",
			"org.cibseven.connect..",
			"org.cibseven.commons..",
			"org.cibseven.templateengines..")
		.because("the engine depends on the webclient (cibseven-direct-provider), never the other way round");

}
