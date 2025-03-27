package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Job;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class JobProvider extends SevenProviderBase implements IJobProvider {

	@Override
	public Collection<Job> getJobs(Map<String, Object> params, CIBUser user) {
		String url = camundaUrl + "/engine-rest/job";
		return Arrays.asList(((ResponseEntity<Job[]>) doPost(url, params, Job[].class, user)).getBody());
	}

	@Override
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
	}

	@Override
	public void setSuspended(String id, Map<String, Object> data, CIBUser user) {
		String url = camundaUrl + "/engine-rest/job/" + id + "/suspended";
		doPut(url, data, user);
	}
	
	
}