package org.cibseven.webapp.providers;

import java.util.Collection;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.JobDefinition;

public interface IJobDefinitionProvider {
	public Collection<JobDefinition> findJobDefinitions(String params, CIBUser user);

	public void suspendJobDefinition(String jobDefinitionId, String params, CIBUser user);	
	
	public void overrideJobDefinitionPriority(String jobDefinitionId, String params, CIBUser user);	
}
