package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.ActivityInstance;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.springframework.http.HttpHeaders;
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
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
	}

	@Override
	public Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(String processDefinitionId,
			CIBUser user) {
		String url = camundaUrl + "/engine-rest/history/activity-instance?processDefinitionId=" + processDefinitionId;
		return Arrays.asList(((ResponseEntity<ActivityInstanceHistory[]>) doGet(url, ActivityInstanceHistory[].class, user, false)).getBody());
	}
	
}
