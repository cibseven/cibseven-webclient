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
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Batch;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/batch")
public class BatchService extends BaseService implements InitializingBean {

    @Autowired BpmProvider bpmProvider;
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("BatchService expects a BpmProvider");
	}

    @GetMapping
	public Collection<Batch> getBatches(
			@RequestParam Map<String, Object> params,
			HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.READ_ALL);
		return bpmProvider.getBatches(params, user);
	}
    
    @GetMapping("/statistics")
	public Collection<Batch> getBatchStatistics(
			@RequestParam Map<String, Object> params,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.READ_ALL);
		return bpmProvider.getBatchStatistics(params, user);
	}
    
    @DeleteMapping("/{id}")
   	public void deleteBatch(
   			@PathVariable String id,
			@RequestParam Map<String, Object> params,
			HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteBatch(id, params, user);
	}
    
    @PutMapping("/{id}/suspended")
   	public void setBatchSuspensionState(
   			@PathVariable String id,
   			@RequestBody Map<String, Object> params,
			HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.UPDATE_ALL);
		bpmProvider.setBatchSuspensionState(id, params, user);
	}

}