package org.cibseven.webapp.providers;

import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class BatchProvider extends SevenProviderBase implements IBatchProvider {

	@Override
	public Object getHistoricBatches(Map<String, Object> queryParams) {
        String url = buildUrlWithParams("/history/batch", queryParams);
        return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
    }
	
	@Override
	public Object getHistoricBatchCount(Map<String, Object> queryParams) {
		String url = buildUrlWithParams("/history/batch/count", queryParams);
        return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
    }
    
	@Override
	public Object getHistoricBatchById(String id) {
        String url = "/history/batch/" + id;
        return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
    }
	
	@Override
	public void deleteHistoricBatch(String id) {
        String url = "/history/batch/" + id;
        doDelete(url, null);
    }
	
	@Override
	public Object setRemovalTime(Map<String, Object> payload) {
        String url = "/history/batch/set-removal-time";
        return ((ResponseEntity<Object>) doPost(url, payload, null, null)).getBody();
    }
    
	@Override
	public Object getCleanableBatchReport(Map<String, Object> queryParams) {
        String url = buildUrlWithParams("/history/batch/cleanable-batch-report", queryParams);
        return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
    }
    
	@Override
	public Object getCleanableBatchReportCount() {
        String url = "/history/batch/cleanable-batch-report/count";
        return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
    }
	
	@Override
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
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