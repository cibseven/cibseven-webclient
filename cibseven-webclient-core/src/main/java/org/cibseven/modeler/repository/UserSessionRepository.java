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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.cibseven.modeler.model.UserSessionEntity;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {

	@Transactional
	@Modifying
	@Query(value = "DELETE FROM user_sessions us WHERE us.created_at < (SELECT created_at FROM (SELECT created_at,ROW_NUMBER() OVER (ORDER BY created_at DESC) AS rn FROM user_sessions) AS ranked WHERE ranked.rn = :keepLimit);", nativeQuery = true)
	void removeOldSessions(@Param("keepLimit") int keepLimit);
}
