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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cibseven.bpm.engine.BadUserRequestException;
import org.cibseven.bpm.engine.batch.BatchQuery;
import org.cibseven.bpm.engine.batch.BatchStatistics;
import org.cibseven.bpm.engine.batch.BatchStatisticsQuery;
import org.cibseven.bpm.engine.batch.history.HistoricBatch;
import org.cibseven.bpm.engine.batch.history.HistoricBatchQuery;
import org.cibseven.bpm.engine.history.CleanableHistoricBatchReport;
import org.cibseven.bpm.engine.history.CleanableHistoricBatchReportResult;
import org.cibseven.bpm.engine.history.SetRemovalTimeSelectModeForHistoricBatchesBuilder;
import org.cibseven.bpm.engine.rest.dto.batch.BatchDto;
import org.cibseven.bpm.engine.rest.dto.batch.BatchQueryDto;
import org.cibseven.bpm.engine.rest.dto.batch.BatchStatisticsDto;
import org.cibseven.bpm.engine.rest.dto.batch.BatchStatisticsQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricActivityInstanceDto;
import org.cibseven.bpm.engine.rest.dto.history.HistoricIncidentQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.CleanableHistoricBatchReportDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.CleanableHistoricBatchReportResultDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.HistoricBatchDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.HistoricBatchQueryDto;
import org.cibseven.bpm.engine.rest.dto.history.batch.removaltime.SetRemovalTimeToHistoricBatchesDto;
import org.cibseven.bpm.engine.rest.util.QueryUtil;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Batch;
import org.cibseven.webapp.rest.model.HistoryBatch;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

public class DirectBatchProvider implements IBatchProvider {

	DirectProviderUtil directProviderUtil;

	DirectBatchProvider(DirectProviderUtil directProviderUtil){
		this.directProviderUtil = directProviderUtil;
	}

	@Override
	public Collection<Batch> getBatches(Map<String, Object> params, CIBUser user) {
		DirectProviderUtil.PagedParams paged = directProviderUtil.extractPagedParams(params);
		BatchQueryDto queryDto = new BatchQueryDto(directProviderUtil.getObjectMapper(user), paged.getQueryParams());
		BatchQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));

		List<Batch> batchResults = directProviderUtil.listAndConvert(query, paged.getFirstResult(), paged.getMaxResults(),
				BatchDto::fromBatch, Batch.class, user);

		batchResults.forEach(batch -> {

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

		return batchResults;
	}

	@Override
	public Collection<Batch> getBatchStatistics(Map<String, Object> params, CIBUser user) {
		DirectProviderUtil.PagedParams paged = directProviderUtil.extractPagedParams(params);
		BatchStatisticsQueryDto queryDto = new BatchStatisticsQueryDto(directProviderUtil.getObjectMapper(user), paged.getQueryParams());
		BatchStatisticsQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));

		List<BatchStatistics> batchStatisticsList = QueryUtil.list(query, paged.getFirstResult(), paged.getMaxResults());

		List<Batch> statisticsResults = new ArrayList<>();
		for (BatchStatistics batchStatistics : batchStatisticsList) {
			statisticsResults.add(directProviderUtil.convertValue(BatchStatisticsDto.fromBatchStatistics(batchStatistics), Batch.class, user));
		}

		return statisticsResults;
	}

	@Override
	public void deleteBatch(String id, Map<String, Object> params, CIBUser user) {
		Boolean cascade = false;
		if (params.containsKey("cascade"))
			cascade = params.get("cascade").equals("true");
		try {
			directProviderUtil.getProcessEngine(user).getManagementService().deleteBatch(id, cascade);
		} catch (BadUserRequestException e) {
			throw new SystemException("Unable to delete batch with id '" + id + "'", e);
		}
	}

	@Override
	public void setBatchSuspensionState(String id, Map<String, Object> params, CIBUser user) {
		Boolean suspended = false;
		if (params.containsKey("suspended"))
			suspended = params.get("suspended").equals("true");

		if (suspended) {
			try {
				directProviderUtil.getProcessEngine(user).getManagementService().suspendBatchById(id);
			} catch (BadUserRequestException e) {
				throw new SystemException("Unable to suspend batch with id '" + id + "'", e);
			}
		} else {
			try {
				directProviderUtil.getProcessEngine(user).getManagementService().activateBatchById(id);
			} catch (BadUserRequestException e) {
				throw new SystemException("Unable to activate batch with id '" + id + "'", e);
			}
		}
	}

	@Override
	public Collection<HistoryBatch> getHistoricBatches(Map<String, Object> params, CIBUser user) {
		HistoricBatchQueryDto queryDto = directProviderUtil.parseQueryDto(params, HistoricBatchQueryDto.class, user);
		HistoricBatchQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));
		DirectProviderUtil.PagedParams paged = directProviderUtil.extractPagedParams(params);
		List<HistoricBatch> matchingBatches = QueryUtil.list(query, paged.getFirstResult(), paged.getMaxResults());

		List<HistoryBatch> batchResults = new ArrayList<>();
		for (HistoricBatch matchingBatch : matchingBatches) {
			batchResults.add(directProviderUtil.convertValue(HistoricBatchDto.fromBatch(matchingBatch), HistoryBatch.class, user));
		}
		return batchResults;
	}

	@Override
	public Long getHistoricBatchCount(Map<String, Object> queryParams, CIBUser user) {
		HistoricBatchQueryDto queryDto = directProviderUtil.parseQueryDto(queryParams, HistoricBatchQueryDto.class, user);
		HistoricBatchQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));
		return query.count();
	}

	@Override
	public HistoryBatch getHistoricBatchById(String id, CIBUser user) {
		HistoricBatch batch = directProviderUtil.getProcessEngine(user).getHistoryService().createHistoricBatchQuery().batchId(id).singleResult();

		if (batch == null) {
			throw new NoObjectFoundException(new SystemException("Historic batch with id '" + id + "' does not exist"));
		}

		return directProviderUtil.convertValue(HistoricBatchDto.fromBatch(batch), HistoryBatch.class, user);
	}

	@Override
	public void deleteHistoricBatch(String id, CIBUser user) {
		try {
			directProviderUtil.getProcessEngine(user).getHistoryService().deleteHistoricBatch(id);
		} catch (BadUserRequestException e) {
			throw new SystemException("Unable to delete historic batch with id '" + id + "'", e);
		}
	}

	@Override
	public Object setRemovalTime(Map<String, Object> payload, CIBUser user) {
		SetRemovalTimeToHistoricBatchesDto dto = directProviderUtil.getObjectMapper(user).convertValue(payload, SetRemovalTimeToHistoricBatchesDto.class);
		HistoricBatchQuery historicBatchQuery = null;

		if (dto.getHistoricBatchQuery() != null) {
			historicBatchQuery = dto.getHistoricBatchQuery().toQuery(directProviderUtil.getProcessEngine(user));
		}

		SetRemovalTimeSelectModeForHistoricBatchesBuilder builder = directProviderUtil.getProcessEngine(user).getHistoryService().setRemovalTimeToHistoricBatches();

		if (dto.isCalculatedRemovalTime()) {
			builder.calculatedRemovalTime();
		}

		Date removalTime = dto.getAbsoluteRemovalTime();
		if (dto.getAbsoluteRemovalTime() != null) {
			builder.absoluteRemovalTime(removalTime);
		}

		if (dto.isClearedRemovalTime()) {
			builder.clearedRemovalTime();
		}

		builder.byIds(dto.getHistoricBatchIds());
		builder.byQuery(historicBatchQuery);

		org.cibseven.bpm.engine.batch.Batch batch = builder.executeAsync();
		return directProviderUtil.convertValue(BatchDto.fromBatch(batch), Batch.class, user);
	}

	@Override
	public Object getCleanableBatchReport(Map<String, Object> queryParams, CIBUser user) {
		MultivaluedMap<String, String> multiValueMap = directProviderUtil.toMultivaluedMap(queryParams);
		CleanableHistoricBatchReportDto queryDto = new CleanableHistoricBatchReportDto(directProviderUtil.getObjectMapper(user), multiValueMap);
		CleanableHistoricBatchReport query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));

		List<CleanableHistoricBatchReportResult> reportResult = QueryUtil.list(query, null, null);

		return CleanableHistoricBatchReportResultDto.convert(reportResult);
	}

	@Override
	public Object getCleanableBatchReportCount(CIBUser user) {
		MultivaluedMap<String, String> multiValueMap = new MultivaluedHashMap<>();
		CleanableHistoricBatchReportDto queryDto = new CleanableHistoricBatchReportDto(directProviderUtil.getObjectMapper(user), multiValueMap);
		queryDto.setObjectMapper(directProviderUtil.getObjectMapper(user));
		CleanableHistoricBatchReport query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));
		return query.count();
	}

	@Override
	public Long getRuntimeBatchCount(Map<String, Object> queryParams, CIBUser user) {
		MultivaluedMap<String, String> multiValueMap = directProviderUtil.toMultivaluedMap(queryParams);
		BatchQueryDto queryDto = new BatchQueryDto(directProviderUtil.getObjectMapper(user), multiValueMap);
		BatchQuery query = queryDto.toQuery(directProviderUtil.getProcessEngine(user));
		return query.count();
	}
}
