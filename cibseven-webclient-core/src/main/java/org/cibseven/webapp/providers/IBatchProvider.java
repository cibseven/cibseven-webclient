package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Batch;
import org.cibseven.webapp.rest.model.HistoryBatch;

public interface IBatchProvider {

	public Collection<Batch> getBatches(Map<String, Object> queryParams, CIBUser user);
	public Collection<Batch> getBatchStatistics(Map<String, Object> queryParams, CIBUser user);
	public void deleteBatch(String id, Map<String, Object> queryParams, CIBUser user);
	public void setBatchSuspensionState(String id, Map<String, Object> queryParams, CIBUser user);
	public Collection<HistoryBatch> getHistoricBatches(Map<String, Object> queryParams, CIBUser user);
    public Object getHistoricBatchCount(Map<String, Object> queryParams);
    public HistoryBatch getHistoricBatchById(String id, CIBUser user);
    public void deleteHistoricBatch(String id, CIBUser user);
    public Object setRemovalTime(Map<String, Object> payload);
    public Object getCleanableBatchReport(Map<String, Object> queryParams);
    public Object getCleanableBatchReportCount();
}