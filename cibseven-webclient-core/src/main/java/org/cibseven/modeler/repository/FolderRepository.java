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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cibseven.modeler.model.FolderEntity;

public interface FolderRepository extends JpaRepository<FolderEntity, String> {

	/** A folder at the top level, where the name is not scoped by a parent. */
	Optional<FolderEntity> findFirstByParentIdIsNullAndNameOrderByCreatedAsc(String name);

	/** A folder at the top level that its creator named, as a user folder is named after its user. */
	Optional<FolderEntity> findFirstByParentIdIsNullAndNameAndCreatedByOrderByCreatedAsc(String name, String createdBy);

	List<FolderEntity> findByParentIdOrderByNameAsc(String parentId);

	List<FolderEntity> findAllByOrderByNameAsc();

	Optional<FolderEntity> findByParentIdAndName(String parentId, String name);

	/** The projects: the folders at the top level. */
	@Query("select f.id from FolderEntity f where f.parentId is null")
	List<String> findProjectIds();

	/** Pending changes go to the database first, so the update does not overtake them. */
	@Modifying(flushAutomatically = true)
	@Query("update FolderEntity f set f.projectId = :projectId where f.id in :ids")
	int setProject(@Param("ids") Collection<String> ids, @Param("projectId") String projectId);
}
