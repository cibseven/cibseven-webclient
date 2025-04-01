package org.cibseven.webapp.providers;

import java.util.Map;

public interface IBatchProvider {

	public Object getHistoricBatches(Map<String, Object> queryParams);
    public Object getHistoricBatchCount(Map<String, Object> queryParams);
    public Object getHistoricBatchById(String id);
    public void deleteHistoricBatch(String id);
    public Object setRemovalTime(Map<String, Object> payload);
    public Object getCleanableBatchReport(Map<String, Object> queryParams);
    public Object getCleanableBatchReportCount();
}