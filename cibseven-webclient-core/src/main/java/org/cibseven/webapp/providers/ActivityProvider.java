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

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.ActivityInstance;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ActivityProvider extends SevenProviderBase implements IActivityProvider {

	@Override
	public ActivityInstance findActivityInstance(String processInstanceId, CIBUser user) {
		String url = camundaUrl+ "/engine-rest/process-instance/" + processInstanceId + "/activity-instances";
		return ((ResponseEntity<ActivityInstance>) doGet(url, ActivityInstance.class, user, false)).getBody();		
	}

	@Override
	public List<ActivityInstanceHistory> findActivitiesInstancesHistory(String processInstanceId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/history/activity-instance?processInstanceId=" + processInstanceId;
		return Arrays.asList(((ResponseEntity<ActivityInstanceHistory[]>) doGet(url, ActivityInstanceHistory[].class, user, false)).getBody());	
	}
	
	@Override
	public ActivityInstance findActivityInstances(String processInstanceId, CIBUser user) throws SystemException {
		String url = camundaUrl+ "/engine-rest/process-instance/" + processInstanceId + "/activity-instances";
		return doGet(url, ActivityInstance.class, user, false).getBody();
	}
	
	@Override
	public List<ActivityInstanceHistory> findActivityInstanceHistory(String processInstanceId, CIBUser user) throws SystemException {
		String url = camundaUrl + "/engine-rest/history/activity-instance?processInstanceId=" + processInstanceId;
		return Arrays.asList(doGet(url, ActivityInstanceHistory[].class, user, false).getBody());
	}

	@Override
	public void deleteVariableByExecutionId(String executionId, String variableName, CIBUser user) {
		String url = camundaUrl + "/engine-rest/execution/" + executionId + "/localVariables/" + variableName;
		doDelete(url, user);
	}

	@Override
	public void deleteVariableHistoryInstance(String id, CIBUser user) {
		String url = camundaUrl + "/engine-rest/history/variable-instance/" + id;
		doDelete(url, user);
	}

	@Override
	public Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(String processDefinitionId,
			CIBUser user) {
		String url = camundaUrl + "/engine-rest/history/activity-instance?processDefinitionId=" + processDefinitionId;
		return Arrays.asList(((ResponseEntity<ActivityInstanceHistory[]>) doGet(url, ActivityInstanceHistory[].class, user, false)).getBody());
	}
	
}
