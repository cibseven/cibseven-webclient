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
package org.cibseven.webapp.rest;

import java.util.Collection;

import org.cibseven.webapp.rest.model.Engine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occurred")
})
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/engine")
public class EngineService extends BaseService {

	@Operation(summary = "Get process engine names", description = "Retrieves the names of all process engines available on the engine")
	@ApiResponse(responseCode = "200", description = "List of engine names successfully retrieved")
	@GetMapping
	public Collection<Engine> getProcessEngineNames() {
		return bpmProvider.getProcessEngineNames(null);
	}
}
