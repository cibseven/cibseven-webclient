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
import java.util.Locale;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.rest.model.HistoryBatch;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;

@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/history/batch")
public class HistoryBatchService extends BaseService implements InitializingBean {

	@Autowired BpmProvider bpmProvider;
	
	public void afterPropertiesSet() {
	}


	@GetMapping
	public Collection<HistoryBatch> getBatches(
			@RequestParam Map<String, Object> params,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.READ_HISTORY_ALL);
		return bpmProvider.getHistoricBatches(params, user);
	}

	/**
	 * Returns the count of historic batches matching the given parameters.
	 * GET /history/batch/count
	 */
	@GetMapping("/count")
	public Long getHistoricBatchesCount(
			@RequestParam Map<String, Object> params,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.READ_HISTORY_ALL);
		return bpmProvider.getHistoricBatchCount(params, user);
	}
	
	@GetMapping("/{id}")
	public HistoryBatch getHistoricBatchById(
			@Parameter(description = "Batch id") @PathVariable String id, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.READ_HISTORY_ALL);
		return bpmProvider.getHistoricBatchById(id, user);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteHistoricBatch(
			@Parameter(description = "Batch id") @PathVariable String id, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.DELETE_HISTORY_ALL);
		bpmProvider.deleteHistoricBatch(id, user);
	  // return 204 No Content, no body
	  return ResponseEntity.noContent().build();
	}

}