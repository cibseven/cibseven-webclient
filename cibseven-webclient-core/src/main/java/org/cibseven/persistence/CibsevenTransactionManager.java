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

import org.springframework.orm.jpa.JpaTransactionManager;

import jakarta.persistence.EntityManagerFactory;

/**
 * The transaction manager of the webclient's persistence unit. Unlike a plain
 * {@link JpaTransactionManager} it does not bind its JDBC connection to the thread.
 *
 * <p>The engine can share the data source under a transaction manager of its own: on the run
 * distribution that is Spring Boot's {@code transactionManager}, another {@link JpaTransactionManager}.
 * With the direct provider the engine is then called inside webclient transactions, and a bound
 * connection makes its transaction fail with "Pre-bound JDBC Connection found!" (CIB7-2712). Without
 * it the engine works in a transaction of its own, as it does with the REST provider. The webclient
 * has no plain JDBC access that would need the connection.</p>
 */
public class CibsevenTransactionManager extends JpaTransactionManager {

	public CibsevenTransactionManager(EntityManagerFactory entityManagerFactory) {
		super(entityManagerFactory);
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		// The data source is taken from the factory above; only with one is the connection bound
		setDataSource(null);
	}
}
