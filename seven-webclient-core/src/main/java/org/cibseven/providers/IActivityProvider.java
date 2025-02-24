package org.cibseven.providers;

import java.util.Collection;
import java.util.List;

import org.cibseven.auth.CIBUser;
import org.cibseven.exception.SystemException;
import org.cibseven.rest.model.ActivityInstance;
import org.cibseven.rest.model.ActivityInstanceHistory;

public interface IActivityProvider {
	
	public ActivityInstance findActivityInstance(String processInstanceId, CIBUser user);
	public List<ActivityInstanceHistory> findActivitiesInstancesHistory(String processInstanceId, CIBUser user);
	public ActivityInstance findActivityInstances(String processInstanceId, CIBUser user) throws SystemException;
	public List<ActivityInstanceHistory> findActivityInstanceHistory(String processInstanceId, CIBUser user) throws SystemException;
	public void deleteVariableByExecutionId(String executionId, String variableName, CIBUser user);
	public void deleteVariableHistoryInstance(String id, CIBUser user);
	public Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(String processDefinitionId,
			CIBUser user);
	
}
