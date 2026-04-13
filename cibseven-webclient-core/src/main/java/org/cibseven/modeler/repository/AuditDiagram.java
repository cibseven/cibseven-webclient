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
package org.cibseven.modeler.repository;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Objects;

@Repository
public class AuditDiagram {

	@PersistenceContext
	private EntityManager entityManager;

	@Value("${cibseven.modeler.historicMaxResults:30}")
	private int historicMaxResults;

	private AuditReader getAuditReader() {
		return AuditReaderFactory.get(entityManager);
	}

	public <T> List<Object[]> getRevisions(final Class<T> tClass, final String prop, final Object propValue) {
		if (Objects.isNull(tClass) || !StringUtils.hasText(prop) || Objects.isNull(propValue)) {
			throw new IllegalArgumentException("Invalid params.");
		}

		try {
			return getAuditReader().createQuery()
					.forRevisionsOfEntity(tClass, true, false)
					.add(AuditEntity.property(prop).eq(propValue))
					.getResultList();
		} finally {
			entityManager.close();
		}
	}

	public <T> List<Object[]> getHistory(final Class<T> tClass, final String changedProperty, final String prop, final Object propValue) {
		if (Objects.isNull(tClass) || !StringUtils.hasText(prop)) {
			throw new IllegalArgumentException("Invalid params.");
		}

		try {
			return getAuditReader().createQuery()
					.forRevisionsOfEntity(tClass, false, true)
					.add(AuditEntity.revisionNumber().gt(0))
					.add(AuditEntity.property(changedProperty).hasChanged())
					.add(AuditEntity.property(prop).eq(propValue))
					.addOrder(AuditEntity.revisionNumber().desc())
					.setMaxResults(historicMaxResults)
					.getResultList();
		} finally {
			entityManager.close();
		}
	}

}
