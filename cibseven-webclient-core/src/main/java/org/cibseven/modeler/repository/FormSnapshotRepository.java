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

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cibseven.modeler.model.FormSnapshotEntity;

@Repository
public interface FormSnapshotRepository
		extends JpaRepository<FormSnapshotEntity, FormSnapshotEntity.SnapshotId> {

	/**
	 * The history of one form, newest first: only the saves that changed the schema itself, so a
	 * changed description does not show up as a version to go back to.
	 */
	@Query("SELECT s FROM FormSnapshotEntity s "
			+ "WHERE s.id = :id AND s.schemaMod = TRUE ORDER BY s.rev DESC")
	List<FormSnapshotEntity> findHistory(@Param("id") String id, Pageable pageable);
}
