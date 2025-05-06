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
import java.util.HashMap;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Job;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class JobProvider extends SevenProviderBase implements IJobProvider {

	@Override
	public Collection<Job> getJobs(Map<String, Object> params, CIBUser user) {
		String url = cibsevenUrl + "/engine-rest/job";
		return Arrays.asList(((ResponseEntity<Job[]>) doPost(url, params, Job[].class, user)).getBody());
	}

	@Override
	public void setSuspended(String id, Map<String, Object> data, CIBUser user) {
		String url = cibsevenUrl + "/engine-rest/job/" + id + "/suspended";
		doPut(url, data, user);
	}

	@Override
	public void deleteJob(String id, CIBUser user) {
		String url = cibsevenUrl + "/engine-rest/job/" + id;
		doDelete(url, user);
	}

	@Override
	public Collection<Object> getHistoryJobLog(Map<String, Object> params, CIBUser user) {
		String url = buildUrlWithParams("/history/job-log", params);
		Collection<Object> jobLogs = Arrays.asList(
	        ((ResponseEntity<Object[]>) doGet(url, Object[].class, user, false)).getBody()
	    );
		return jobLogs;
	}
	
	@Override
	public String getHistoryJobLogStacktrace(String id, CIBUser user) {
		String url = buildUrlWithParams("/history/job-log/" + id + "/stacktrace", new HashMap<>());
		return doGetWithHeader(url, String.class, user, false, MediaType.ALL).getBody();
	}
	
	private String buildUrlWithParams(String path, Map<String, Object> queryParams) {
	    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(cibsevenUrl + "/engine-rest" + path);
	    queryParams.forEach((key, value) -> {
	        if (value != null) {
	            builder.queryParam(key, value);
	        }
	    });
	    return builder.toUriString();
	}	
	
}