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
package org.cibseven.persistence;

/**
 * Names of the JPA beans the webclient owns.
 *
 * <p>The webclient is embedded into applications that bring their own JPA setup, so it must neither
 * claim {@code @Primary} nor attach itself to the beans named {@code entityManagerFactory} and
 * {@code transactionManager}: in an embedding application those are the host's beans, holding the
 * host's persistence unit and, quite possibly, a different database. Every webclient feature that
 * stores data shares the one persistence unit named below, and refers to these names explicitly.</p>
 *
 * <p>A host that wants those tables in a separate database can define a {@code DataSource} bean
 * named {@link #DATA_SOURCE}; without one the unit uses the application's primary
 * {@code DataSource}, which is what a standalone webclient does.</p>
 */
public final class CibsevenJpa {

	/** Bean name of the webclient's entity manager factory. */
	public static final String ENTITY_MANAGER_FACTORY = "cibsevenEntityManagerFactory";

	/** Bean name of the webclient's transaction manager, for {@code @Transactional} qualifiers. */
	public static final String TRANSACTION_MANAGER = "cibsevenTransactionManager";

	/** Optional bean name of a dedicated data source for the webclient tables. */
	public static final String DATA_SOURCE = "cibsevenDataSource";

	/** Name of the webclient's persistence unit. Internal to Spring: no table name depends on it. */
	public static final String PERSISTENCE_UNIT = "cibseven";

	private CibsevenJpa() {
	}
}
