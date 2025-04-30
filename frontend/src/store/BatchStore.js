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

import { BatchService } from "@/services.js"

const BatchStore = {
  state: {
    runtimeBatches: [],
    historicBatches: [],
    cleanableBatchReport: [],
    selectedHistoricBatch: null,
    historicBatchCount: 0,
    cleanableBatchReportCount: 0
  },
  mutations: {
    setRuntimeBatches(state, batches) {
      state.runtimeBatches = batches
    },
    removeRuntimeBatch(state, id) {
      state.runtimeBatches = state.runtimeBatches.filter(b => b.id !== id)
    },
    setHistoricBatches(state, batches) {
        state.historicBatches = batches
    },
    appendHistoricBatches(state, newBatches) {
      state.historicBatches = state.historicBatches.concat(newBatches)
    },
    setCleanableBatchReport(state, report) {
        state.cleanableBatchReport = report
    },
    setSelectedHistoricBatch(state, batch) {
        state.selectedHistoricBatch = batch
    },
    setHistoricBatchCount(state, count) {
        state.historicBatchCount = count
    },
    setCleanableBatchReportCount(state, count) {
        state.cleanableBatchReportCount = count
    },
    removeHistoricBatch(state, id) {
      state.historicBatches = state.historicBatches.filter(b => b.id !== id)
    }
  },
  actions: {
    async getRuntimeBatches({ commit }) {
        const result = await BatchService.getBatches({})
        commit('setRuntimeBatches', result)
        return result
    },
    async deleteRuntimeBatch({ commit }, id) {
      await BatchService.deleteBatch(id)
      commit('removeRuntimeBatch', id)
    },
    async getHistoricBatches(_, query) {
      const result = await BatchService.getHistoricBatches(query)
      return result.filter(batch => batch.endTime != null)
    },
    async loadHistoricBatches({ dispatch, commit }, { query, append = false }) {
      const filtered = await dispatch('getHistoricBatches', query)
      if (append) {
        commit('appendHistoricBatches', filtered)
      } else {
        commit('setHistoricBatches', filtered)
      }
      return filtered
    },
    async getHistoricBatch({ commit }, id) {
        const result = await BatchService.getHistoricBatchById(id)
        commit('setSelectedHistoricBatch', result)
        return result
    },
    async getBatchStatistics(_, query) {
      return BatchService.getBatchStatistics(query)
    },  
    async setBatchSuspensionState(_, { id, params }) {
      return BatchService.setBatchSuspensionState(id, params)
    },
    async getHistoricBatchCount({ commit }, query) {
        const result = await BatchService.getHistoricBatchCount(query)
        commit('setHistoricBatchCount', result.count)
        return result.count
    },
    async getCleanableBatchReport({ commit }, query) {
        const result = await BatchService.getCleanableBatchReport(query)
        commit('setCleanableBatchReport', result)
        return result
    },
    async getCleanableBatchReportCount({ commit }) {
        const result = await BatchService.getCleanableBatchReportCount()
        commit('setCleanableBatchReportCount', result.count)
        return result.count
    },
    async deleteHistoricBatch({ commit }, id) {
      await BatchService.deleteHistoricBatch(id)
      commit('removeHistoricBatch', id)
    },
    async setHistoricBatchRemovalTime(_, payload) {
        return BatchService.setHistoricBatchRemovalTime(payload)
    }
  },
  getters: {
    runtimeBatches: state => state.runtimeBatches,
    historicBatches: state => state.historicBatches,
    cleanableBatchReport: state => state.cleanableBatchReport,
    selectedHistoricBatch: state => state.selectedHistoricBatch,
    historicBatchCount: state => state.historicBatchCount,
    cleanableBatchReportCount: state => state.cleanableBatchReportCount
  }
}

export default BatchStore
