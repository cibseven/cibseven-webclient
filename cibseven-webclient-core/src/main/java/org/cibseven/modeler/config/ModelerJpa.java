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

import org.cibseven.persistence.CibsevenJpa;

/**
 * Names of the JPA beans the modeler runs on.
 *
 * @deprecated since 2.3.0, use {@link CibsevenJpa}. The unit is no longer the modeler's alone, so
 *             it is named after the webclient rather than after one of its features. The bean names
 *             below still resolve: the beans carry their former names as aliases.
 */
@Deprecated(since = "2.3.0", forRemoval = true)
public final class ModelerJpa {

	/** @deprecated use {@link CibsevenJpa#ENTITY_MANAGER_FACTORY} */
	@Deprecated(since = "2.3.0", forRemoval = true)
	public static final String ENTITY_MANAGER_FACTORY = CibsevenJpa.LEGACY_ENTITY_MANAGER_FACTORY;

	/** @deprecated use {@link CibsevenJpa#TRANSACTION_MANAGER} */
	@Deprecated(since = "2.3.0", forRemoval = true)
	public static final String TRANSACTION_MANAGER = CibsevenJpa.LEGACY_TRANSACTION_MANAGER;

	/** @deprecated use {@link CibsevenJpa#DATA_SOURCE} */
	@Deprecated(since = "2.3.0", forRemoval = true)
	public static final String DATA_SOURCE = CibsevenJpa.LEGACY_DATA_SOURCE;

	/**
	 * @deprecated use {@link CibsevenJpa#PERSISTENCE_UNIT}. Unlike the bean names, this value
	 *             changed: a persistence unit has no aliases, so anything naming the unit itself,
	 *             such as {@code @PersistenceContext(unitName = "modeler")}, has to be updated.
	 */
	@Deprecated(since = "2.3.0", forRemoval = true)
	public static final String PERSISTENCE_UNIT = CibsevenJpa.PERSISTENCE_UNIT;

	/** @deprecated use {@link ModelerPersistence#ENTITY_PACKAGE} */
	@Deprecated(since = "2.3.0", forRemoval = true)
	public static final String ENTITY_PACKAGE = ModelerPersistence.ENTITY_PACKAGE;

	/** @deprecated use {@link ModelerPersistence#REPOSITORY_PACKAGE} */
	@Deprecated(since = "2.3.0", forRemoval = true)
	public static final String REPOSITORY_PACKAGE = ModelerPersistence.REPOSITORY_PACKAGE;

	private ModelerJpa() {
	}
}
