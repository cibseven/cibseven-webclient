package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Batch;
import org.cibseven.webapp.rest.model.HistoryBatch;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class BatchProvider extends SevenProviderBase implements IBatchProvider {
	
	@Override
	public Collection<Batch> getBatches(Map<String, Object> params, CIBUser user) {
		String url = buildUrlWithParams("/batch", params);
	    List<Batch> batches = Arrays.asList(
	        ((ResponseEntity<Batch[]>) doGet(url, Batch[].class, user, false)).getBody()
	    );

	    batches.forEach(batch -> {
	    	String batchId = batch.getId();

	        Map<String, Object> statParams = new HashMap<>();
	        statParams.put("batchId", batchId);

	        Collection<Batch> statList = getBatchStatistics(statParams, user);
	        if (!statList.isEmpty()) {
	        	Batch stats = statList.iterator().next();
	            batch.setCompletedJobs(stats.getCompletedJobs());
	            batch.setRemainingJobs(stats.getRemainingJobs());
	            batch.setFailedJobs(stats.getFailedJobs());
	        }
        });
	    
	    return batches;
	}
	
	@Override
	public Collection<Batch> getBatchStatistics(Map<String, Object> params, CIBUser user) {
	    String url = buildUrlWithParams("/batch/statistics", params);
	    return Arrays.asList(((ResponseEntity<Batch[]>) doGet(url, Batch[].class, user, false)).getBody());
	}

	@Override
	public void deleteBatch(String id, Map<String, Object> params, CIBUser user) {
		String url = buildUrlWithParams("/batch/" + id, params);
		doDelete(url, user);
	}
	
	@Override
	public void setBatchSuspensionState(String id, Map<String, Object> params, CIBUser user) {
		String url = buildUrlWithParams("/batch/" + id + "/suspended", new HashMap<>());
		doPut(url, params, user);
	}

	@Override
	public Collection<HistoryBatch> getHistoricBatches(Map<String, Object> params, CIBUser user) {
        String url = buildUrlWithParams("/history/batch", params);
        return Arrays.asList(((ResponseEntity<HistoryBatch[]>) doGet(url, HistoryBatch[].class, user, false)).getBody());
    }
	
	@Override
	public Object getHistoricBatchCount(Map<String, Object> queryParams) {
		String url = buildUrlWithParams("/history/batch/count", queryParams);
        return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
    }
    
	@Override
	public HistoryBatch getHistoricBatchById(String id, CIBUser user) {
		String url = buildUrlWithParams("/history/batch/" + id, new HashMap<>());
        return doGet(url, HistoryBatch.class, null, false).getBody();
    }
	
	@Override
	public void deleteHistoricBatch(String id, CIBUser user) {
        String url = buildUrlWithParams("/history/batch/" + id, new HashMap<>());
        doDelete(url, user);
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