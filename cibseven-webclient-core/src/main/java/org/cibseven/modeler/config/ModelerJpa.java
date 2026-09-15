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

/**
 * Names of the JPA beans the modeler owns.
 *
 * <p>The modeler is embedded into applications that bring their own JPA setup, so it must neither
 * claim {@code @Primary} nor attach itself to the beans named {@code entityManagerFactory} and
 * {@code transactionManager}: in an embedding application those are the host's beans, holding the
 * host's persistence unit and, quite possibly, a different database. The modeler therefore runs on
 * its own persistence unit under the names below, and every modeler component that needs an entity
 * manager or a transaction refers to them explicitly.</p>
 *
 * <p>A host that wants the modeler tables in a separate database can define a {@code DataSource}
 * bean named {@link #DATA_SOURCE}; without one the modeler uses the application's primary
 * {@code DataSource}, which is what a standalone webclient does.</p>
 */
public final class ModelerJpa {

	/** Bean name of the modeler's entity manager factory. */
	public static final String ENTITY_MANAGER_FACTORY = "modelerEntityManagerFactory";

	/** Bean name of the modeler's transaction manager, for {@code @Transactional} qualifiers. */
	public static final String TRANSACTION_MANAGER = "modelerTransactionManager";

	/** Optional bean name of a dedicated data source for the modeler tables. */
	public static final String DATA_SOURCE = "modelerDataSource";

	/** Name of the modeler's persistence unit. */
	public static final String PERSISTENCE_UNIT = "modeler";

	/** Package holding the modeler's JPA entities. */
	public static final String ENTITY_PACKAGE = "org.cibseven.modeler.model";

	/** Package holding the modeler's Spring Data repositories. */
	public static final String REPOSITORY_PACKAGE = "org.cibseven.modeler.repository";

	private ModelerJpa() {
	}
}
