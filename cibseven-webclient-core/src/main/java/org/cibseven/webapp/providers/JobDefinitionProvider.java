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
package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.JobDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class JobDefinitionProvider extends SevenProviderBase implements IJobDefinitionProvider {	

	@Override
	public Collection<JobDefinition> findJobDefinitions(String params, CIBUser user) {
		String url = getEngineRestUrl(user) + "/job-definition";
		return Arrays.asList(((ResponseEntity<JobDefinition[]>) doPost(url, params, JobDefinition[].class, user)).getBody());
	}
	
	@Override
	public void suspendJobDefinition(String jobDefinitionId, String params, CIBUser user) {
		String url = getEngineRestUrl(user) + "/job-definition/" + jobDefinitionId + "/suspended";
		doPut(url, params, user);
	}
	
	@Override
	public void overrideJobDefinitionPriority(String jobDefinitionId, String params, CIBUser user) {
		String url = getEngineRestUrl(user) + "/job-definition/" + jobDefinitionId + "/jobPriority";
		doPut(url, params, user);
	}
	
	@Override
	public JobDefinition findJobDefinition(String id, CIBUser user) {
		String url = getEngineRestUrl(user) + "/job-definition/" + id;
		return ((ResponseEntity<JobDefinition>) doGet(url, JobDefinition.class, user, false)).getBody();
	}
	
	@Override
	public void retryJobDefinitionById(String id, Map<String, Object> data, CIBUser user) {
		String url = getEngineRestUrl(user) + "/job-definition/" + id + "/retries";
		doPut(url, data, user);
	}	

}
