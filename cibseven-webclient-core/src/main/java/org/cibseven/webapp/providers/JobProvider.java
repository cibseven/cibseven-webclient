package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.JobDefinition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class JobProvider extends SevenProviderBase implements IJobProvider {	

	@Override
	public Collection<JobDefinition> findJobDefinitions(String params, CIBUser user) {
		String url = camundaUrl + "/engine-rest/job-definition";
		return Arrays.asList(((ResponseEntity<JobDefinition[]>) doPost(url, params, JobDefinition[].class, user)).getBody());
	}
	
	@Override
	public void suspendJobDefinition(String jobDefinitionId, String params, CIBUser user) {
		String url = camundaUrl + "/engine-rest/job-definition/" + jobDefinitionId + "/suspended";
		doPut(url, params, user);
	}
	
	@Override
	public void overrideJobDefinitionPriority(String jobDefinitionId, String params, CIBUser user) {
		String url = camundaUrl + "/engine-rest/job-definition/" + jobDefinitionId + "/jobPriority";
		doPut(url, params, user);
	}
	
	@Override
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
	}	

}
