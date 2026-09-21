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
package org.cibseven.webapp.persistence;

import java.util.Collection;

/**
 * Contributes entity packages to the webclient's persistence unit.
 *
 * <p>A feature that stores data shares that unit rather than opening one of its own, so it also
 * shares its transaction manager. Declare a bean of this type to add a package to the unit, and
 * declare {@code @EnableJpaRepositories} for your own repositories against
 * {@link CibsevenJpa#ENTITY_MANAGER_FACTORY} and {@link CibsevenJpa#TRANSACTION_MANAGER}.</p>
 */
@FunctionalInterface
public interface CibsevenEntityPackages {

	Collection<String> packages();
}
