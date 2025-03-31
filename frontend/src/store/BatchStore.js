
import { BatchService } from "@/services.js"

const BatchStore = {
    state: {
        historicBatches: [],
        cleanableBatchReport: [],
        selectedHistoricBatch: null,
        historicBatchCount: 0,
        cleanableBatchReportCount: 0
    },
    mutations: {
        setHistoricBatches(state, batches) {
            state.historicBatches = batches
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
        }
    },
    actions: {
        async fetchHistoricBatches({ commit }, query) {
            const result = await BatchService.getHistoricBatches(query)
            commit('setHistoricBatches', result)
            return result
        },
        async fetchHistoricBatch({ commit }, id) {
            const result = await BatchService.getHistoricBatch(id)
            commit('setSelectedHistoricBatch', result)
            return result
        },
        async fetchHistoricBatchCount({ commit }, query) {
            const result = await BatchService.getHistoricBatchCount(query)
            commit('setHistoricBatchCount', result.count)
            return result.count
        },
        async fetchCleanableBatchReport({ commit }, query) {
            const result = await BatchService.getCleanableBatchReport(query)
            commit('setCleanableBatchReport', result)
            return result
        },
        async fetchCleanableBatchReportCount({ commit }) {
            const result = await BatchService.getCleanableBatchReportCount()
            commit('setCleanableBatchReportCount', result.count)
            return result.count
        },
        async deleteHistoricBatch(_, id) {
            return BatchService.deleteHistoricBatch(id)
        },
        async setRemovalTime(_, payload) {
            return BatchService.setHistoricBatchRemovalTime(payload)
        }
    },
    getters: {
        historicBatches: state => state.historicBatches,
        cleanableBatchReport: state => state.cleanableBatchReport,
        selectedHistoricBatch: state => state.selectedHistoricBatch,
        historicBatchCount: state => state.historicBatchCount,
        cleanableBatchReportCount: state => state.cleanableBatchReportCount
    }
}

export default BatchStore