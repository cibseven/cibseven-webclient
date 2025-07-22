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
import java.util.List;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.ActivityInstance;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.providers.utils.URLUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

@Component
public class ActivityProvider extends SevenProviderBase implements IActivityProvider {

	@Override
	public ActivityInstance findActivityInstance(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId + "/activity-instances";
		return ((ResponseEntity<ActivityInstance>) doGet(url, ActivityInstance.class, user, false)).getBody();		
	}

	@Override
	public List<ActivityInstanceHistory> findActivitiesInstancesHistory(Map<String, Object> queryParams, CIBUser user) {
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl() + "/history/activity-instance", queryParams);
		return Arrays.asList(((ResponseEntity<ActivityInstanceHistory[]>) doGet(url, ActivityInstanceHistory[].class, user, false)).getBody());	
	}

	@Override
	public List<ActivityInstanceHistory> findActivitiesInstancesHistory(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl() + "/history/activity-instance?processInstanceId=" + processInstanceId;
		return Arrays.asList(((ResponseEntity<ActivityInstanceHistory[]>) doGet(url, ActivityInstanceHistory[].class, user, false)).getBody());	
	}
	
	@Override
	public List<ActivityInstanceHistory> findActivityInstanceHistory(String processInstanceId, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/history/activity-instance?processInstanceId=" + processInstanceId;
		return Arrays.asList(doGet(url, ActivityInstanceHistory[].class, user, false).getBody());
	}
	
	@Override
	public ActivityInstance findActivityInstances(String processInstanceId, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId + "/activity-instances";
		return doGet(url, ActivityInstance.class, user, false).getBody();
	}
	
	@Override
	public void deleteVariableByExecutionId(String executionId, String variableName, CIBUser user) {
		String url = getEngineRestUrl() + "/execution/" + executionId + "/localVariables/" + variableName;
		try {
			doDelete(url, user);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	@Override
	public void deleteVariableHistoryInstance(String id, CIBUser user) {
		String url = getEngineRestUrl() + "/history/variable-instance/" + id;
		try {
			doDelete(url, user);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	@Override
	public Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(String processDefinitionId,
			CIBUser user) {
		String url = getEngineRestUrl() + "/history/activity-instance?processDefinitionId=" + processDefinitionId;
		return Arrays.asList(((ResponseEntity<ActivityInstanceHistory[]>) doGet(url, ActivityInstanceHistory[].class, user, false)).getBody());
	}
	
}
