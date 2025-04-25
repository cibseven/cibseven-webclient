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
		String url = camundaUrl + "/engine-rest/job";
		return Arrays.asList(((ResponseEntity<Job[]>) doPost(url, params, Job[].class, user)).getBody());
	}

	@Override
	public void setSuspended(String id, Map<String, Object> data, CIBUser user) {
		String url = camundaUrl + "/engine-rest/job/" + id + "/suspended";
		doPut(url, data, user);
	}

	@Override
	public void deleteJob(String id, CIBUser user) {
		String url = camundaUrl + "/engine-rest/job/" + id;
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
	    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(camundaUrl + "/engine-rest" + path);
	    queryParams.forEach((key, value) -> {
	        if (value != null) {
	            builder.queryParam(key, value);
	        }
	    });
	    return builder.toUriString();
	}	
	
}