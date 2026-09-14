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
package org.cibseven.modeler.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.model.ModelSource;
import org.cibseven.modeler.provider.FolderProvider;
import org.cibseven.modeler.provider.FolderProvider.FolderContents;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidFolderException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * The folders models are organized in. The listing is flat and carries the parent of every
 * folder, so one call is enough to render the tree.
 */
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized"),
	@ApiResponse(responseCode = "403", description = "The user may not use the modeler")
})
@RestController @Slf4j
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}/modeler/folders")
public class FolderService extends ModelerBaseService {

	@Autowired
	private FolderProvider folderProvider;

	@Operation(
		summary = "Get the folders of a source",
		description = "<strong>Return: every folder of the source, each with its parent")
	@GetMapping
	public List<FolderEntity> findAll(
			@RequestParam(defaultValue = "DATABASE") ModelSource source, HttpServletRequest rq) {
		checkModelerAccess(rq);
		folderProvider.defaultFolder(source);
		return folderProvider.findAll(source);
	}

	@Operation(summary = "Get one folder", description = "<strong>Return: the folder")
	@GetMapping("/{id}")
	public FolderEntity find(@PathVariable String id, HttpServletRequest rq) {
		checkModelerAccess(rq);
		return folderProvider.find(id);
	}

	@Operation(
		summary = "Get what a folder holds",
		description = "<strong>Return: the number of folders, diagrams and forms below it, for a delete confirmation")
	@GetMapping("/{id}/contents")
	public FolderContents contents(@PathVariable String id, HttpServletRequest rq) {
		checkModelerAccess(rq);
		return folderProvider.contents(id);
	}

	@Operation(summary = "Create a folder", description = "<strong>Return: the created folder")
	@PostMapping
	public FolderEntity create(@RequestBody Map<String, String> folder, HttpServletRequest rq) {
		CIBUser user = checkModelerAccess(rq);
		return folderProvider.create(sourceOf(folder), folder.get("parentId"), folder.get("name"), user.getId());
	}

	@Operation(
		summary = "Rename or move a folder",
		description = "<strong>Return: the folder, keeping its id so what is below it stays reachable. A parentId of null moves it to the top level")
	@PutMapping("/{id}")
	public FolderEntity update(@PathVariable String id, @RequestBody Map<String, String> folder,
			HttpServletRequest rq) {
		CIBUser user = checkModelerAccess(rq);
		FolderEntity updated = folderProvider.find(id);
		if (folder.containsKey("name")) {
			updated = folderProvider.rename(id, folder.get("name"), user.getId());
		}
		if (folder.containsKey("parentId")) {
			updated = folderProvider.move(id, folder.get("parentId"), user.getId());
		}
		return updated;
	}

	@Operation(
		summary = "Delete a folder with everything below it",
		description = "<strong>Return: what was removed. The models are gone for good, as deleting one has always been")
	@DeleteMapping("/{id}")
	public FolderContents delete(@PathVariable String id, HttpServletRequest rq) {
		checkModelerAccess(rq);
		return folderProvider.delete(id);
	}

	/** The source a folder without a parent is created at the top level of. */
	private static ModelSource sourceOf(Map<String, String> folder) {
		String source = folder.get("source");
		try {
			return source == null ? ModelSource.DATABASE : ModelSource.valueOf(source);
		} catch (IllegalArgumentException e) {
			throw new InvalidFolderException("source", "there is no source with that name");
		}
	}
}
