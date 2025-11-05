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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.utils.URLUtils;
import org.cibseven.webapp.rest.model.Batch;
import org.cibseven.webapp.rest.model.HistoryBatch;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class BatchProvider extends SevenProviderBase implements IBatchProvider {
	
	@Override
	public Collection<Batch> getBatches(Map<String, Object> params, CIBUser user) {
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/batch", params);
	    List<Batch> batches = Arrays.asList(
	        ((ResponseEntity<Batch[]>) doGet(url, Batch[].class, user, true)).getBody()
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
	    String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/batch/statistics", params);
	    return Arrays.asList(((ResponseEntity<Batch[]>) doGet(url, Batch[].class, user, true)).getBody());
	}

	@Override
	public void deleteBatch(String id, Map<String, Object> params, CIBUser user) {
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/batch/" + id, params);
		doDelete(url, user);
	}
	
	@Override
	public void setBatchSuspensionState(String id, Map<String, Object> params, CIBUser user) {
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/batch/" + id + "/suspended", new HashMap<>());
		doPut(url, params, user);
	}

	@Override
	public Collection<HistoryBatch> getHistoricBatches(Map<String, Object> params, CIBUser user) {
        String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/history/batch", params);
        return Arrays.asList(((ResponseEntity<HistoryBatch[]>) doGet(url, HistoryBatch[].class, user, true)).getBody());
    }
	
	@Override
	public Long getHistoricBatchCount(Map<String, Object> queryParams, CIBUser user) {
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/history/batch/count", queryParams);
		JsonNode response = ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, true)).getBody();
		return response != null ? response.get("count").asLong() : 0L;
    }
    
	@Override
	public HistoryBatch getHistoricBatchById(String id, CIBUser user) {
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/history/batch/" + id, new HashMap<>());
        return doGet(url, HistoryBatch.class, null, true).getBody();
    }
	
	@Override
	public void deleteHistoricBatch(String id, CIBUser user) {
        String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/history/batch/" + id, new HashMap<>());
        doDelete(url, user);
    }
	
	@Override
	public Object setRemovalTime(Map<String, Object> payload, CIBUser user) {
        String url = getEngineRestUrl(user) + "/history/batch/set-removal-time";
        return ((ResponseEntity<Object>) doPost(url, payload, Object.class, user)).getBody();
    }
    
	@Override
	public Object getCleanableBatchReport(Map<String, Object> queryParams, CIBUser user) {
        String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/history/batch/cleanable-batch-report", queryParams);
        return ((ResponseEntity<Object>) doGet(url, Object.class, user, true)).getBody();
    }
    
	@Override
	public Object getCleanableBatchReportCount(CIBUser user) {
        String url = getEngineRestUrl(user) + "/history/batch/cleanable-batch-report/count";
        return ((ResponseEntity<Object>) doGet(url, Object.class, user, false)).getBody();
    }
}
