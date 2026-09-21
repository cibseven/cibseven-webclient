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
package org.cibseven.modeler.config;

import org.cibseven.persistence.CibsevenEntityPackages;

/**
 * Contributes entity packages to the modeler's persistence unit.
 *
 * @deprecated since 2.3.0, use {@link CibsevenEntityPackages}. A bean of this type is still
 *             collected, because the unit it contributed to is the one the webclient now owns.
 */
@Deprecated(since = "2.3.0", forRemoval = true)
@FunctionalInterface
public interface ModelerEntityPackages extends CibsevenEntityPackages {
}
