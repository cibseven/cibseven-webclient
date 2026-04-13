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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.cibseven.modeler.model.UnifiedDiagram;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.model.ProcessDiagramReduce;

@Repository
public interface ProcessDiagramRepository extends JpaRepository<ProcessDiagramEntity, String> {

	ProcessDiagramEntity findByName(String name);

	ProcessDiagramEntity findByProcesskey(String processkey);

	List<ProcessDiagramReduce> findAllBy(Pageable pageable);

	@Query("select p from ProcessDiagramEntity p " +
		"where (lower(p.name) like lower(concat('%', :keyword, '%')) " +
		"or lower(p.processkey) like lower(concat('%', :keyword, '%'))) " +
		"and (:diagramType = '' or p.type like concat('%', :diagramType, '%'))")
	List<ProcessDiagramReduce> findAllFiltered(
		@Param("keyword") String keyword,
		@Param("diagramType") String diagramType,
		Pageable pageable);

	@Query(value =
		"SELECT * FROM ( " +
		"   SELECT p.id, p.name, p.type, p.processkey, " +
		"          NULL AS formid, " +
		"          p.description, p.created, p.updated, p.updated_by, p.version " +
		"   FROM processes_diagrams p " +
		"   WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(:keyword) " +
		"          OR LOWER(p.processkey) LIKE LOWER(:keyword)) " +
		"   AND (:type IS NULL OR p.type LIKE :type) " +
		"   UNION ALL " +
		"   SELECT f.id, f.formid, 'form', f.formid, f.formid, " +
		"          f.description, f.created, f.updated, f.updated_by, f.version " +
		"   FROM forms f " +
		"   WHERE (:keyword IS NULL OR LOWER(f.formid) LIKE LOWER(:keyword)) " +
		"   AND (:type IS NULL OR 'form' LIKE :type) " +
		") t " +
		"ORDER BY t.updated DESC",
		nativeQuery = true)
	List<UnifiedDiagram> findAllUnified(
		@Param("keyword") String keyword,
		@Param("type") String type,
		Pageable pageable);

	@Transactional
	@Modifying
	@Query(value = "DELETE FROM processes_diagrams_aud pa "
			+ " WHERE diagram IS NOT NULL "
			+ "AND NOT EXISTS ( "
			+ "    SELECT 1 "
			+ "    FROM ( "
			+ "        SELECT id, version, "
			+ "               ROW_NUMBER() OVER (PARTITION BY id ORDER BY version DESC) AS rn "
			+ "        FROM processes_diagrams_aud "
			+ "        WHERE version IS NOT NULL"
			+ "    ) AS ranked "
			+ "    WHERE ranked.id = pa.id "
			+ "      AND ranked.version = pa.version "
			+ "      AND ranked.rn <= :versionLimit )", nativeQuery = true)
	public void deleteOldRecords(@Param("versionLimit") int versionLimit);

}
