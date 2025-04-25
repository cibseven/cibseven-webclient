package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.Map;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Job;

public interface IJobProvider {

	public Collection<Job> getJobs(Map<String, Object> params, CIBUser user);

	public void setSuspended(String id, Map<String, Object> data, CIBUser user);

	public Collection<Object> getHistoryJobLog(Map<String, Object> params, CIBUser user);
	
	public String getHistoryJobLogStacktrace(String id, CIBUser user);

	public void deleteJob(String id, CIBUser user);
	
}