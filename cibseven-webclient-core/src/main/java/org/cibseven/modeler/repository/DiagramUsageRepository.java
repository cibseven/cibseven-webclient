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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cibseven.modeler.model.DiagramUsageEntity;

@Repository
public interface DiagramUsageRepository extends JpaRepository<DiagramUsageEntity, String> {

	@Query(value = "select * from diagram_usage where diagram_id = :diagram_id and closed_at is null", nativeQuery = true)
	List<DiagramUsageEntity> checkSessionUser(@Param("diagram_id") String diagram_id);

	@Query(value = "update diagram_usage set close_at = CURRENT_TIMESTAMP where session_id = :session_id and closed_at is null", nativeQuery = true)
	void closeSessionById(@Param("session_id") String session_id);

	@Query(value = "select * from diagram_usage where session_id = :session_id and closed_at is null", nativeQuery = true)
	DiagramUsageEntity getBySessionId(@Param("session_id") String session_id);

}
