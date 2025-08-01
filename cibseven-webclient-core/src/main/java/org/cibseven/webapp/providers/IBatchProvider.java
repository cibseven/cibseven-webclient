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
    public Long getHistoricBatchCount(Map<String, Object> queryParams, CIBUser user);
    public HistoryBatch getHistoricBatchById(String id, CIBUser user);
    public void deleteHistoricBatch(String id, CIBUser user);
    public Object setRemovalTime(Map<String, Object> payload);
    public Object getCleanableBatchReport(Map<String, Object> queryParams);
    public Object getCleanableBatchReportCount();
}