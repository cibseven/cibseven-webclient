import { DecisionService } from '@/services.js'

const DecisionStore = {
  state: {
    list: [],
    historicInstances: [],
    historicInstance: null,
    historicCount: 0
  },
  mutations: {
    setDecisions: function (state, params) {
      state.list = params.decisions
    },
    setHistoricInstances: function (state, instances) {
      state.historicInstances = instances
    },
    setHistoricInstance: function (state, instance) {
      state.historicInstance = instance
    },
    setHistoricCount: function (state, count) {
      state.historicCount = count
    }
  },
  actions: {

    // ────────────────────────────────────────────────────────────────
    //  Definitions
    // ────────────────────────────────────────────────────────────────

    async getDecisionList({ commit }, params) {
      const decisions = await DecisionService.getDecisionList(params)
      commit('setDecisions', { decisions })
      return decisions
    },

    async getDecisionByKey({ state, dispatch }, params) {
      if (state.list && state.list.length > 0) {
        const found = state.list.find(decision => decision.key === params.key)
        if (found) return found
      }
      const newList = await dispatch("getDecisionList")
      const foundAfterReload = newList.find(decision => decision.key === params.key)
      if (foundAfterReload) return foundAfterReload
      return DecisionService.getDecisionByKey(params.key)
    },

    async getDecisionByKeyAndTenant(_, { key, tenant }) {
      return DecisionService.getDecisionByKeyAndTenant(key, tenant)
    },

    // ────────────────────────────────────────────────────────────────
    //  Evaluation
    // ────────────────────────────────────────────────────────────────

    async evaluateByKey(_, { key, data }) {
      return DecisionService.evaluateByKey(key, data)
    },

    async evaluateByKeyAndTenant(_, { key, tenant, data }) {
      return DecisionService.evaluateByKeyAndTenant(key, tenant, data)
    },

    async evaluateById(_, { id, data }) {
      return DecisionService.evaluateById(id, data)
    },

    // ────────────────────────────────────────────────────────────────
    //  XML & Diagram
    // ────────────────────────────────────────────────────────────────

    async getDiagramByKey(_, key) {
      return DecisionService.getDiagramByKey(key)
    },

    async getDiagramById(_, id) {
      return DecisionService.getDiagramById(id)
    },

    async getDiagramByKeyAndTenant(_, { key, tenant }) {
      return DecisionService.getDiagramByKeyAndTenant(key, tenant)
    },

    async getXmlByKey(_, key) {
      return DecisionService.getXmlByKey(key)
    },

    async getXmlByKeyAndTenant(_, { key, tenant }) {
      return DecisionService.getXmlByKeyAndTenant(key, tenant)
    },

    async getXmlById(_, id) {
      return DecisionService.getXmlById(id)
    },

    // ────────────────────────────────────────────────────────────────
    //  History Time To Live (TTL)
    // ────────────────────────────────────────────────────────────────

    async updateHistoryTTLByKey(_, { key, data }) {
      return DecisionService.updateHistoryTTLByKey(key, data)
    },

    async updateHistoryTTLByKeyAndTenant(_, { key, tenant, data }) {
      return DecisionService.updateHistoryTTLByKeyAndTenant(key, tenant, data)
    },

    async updateHistoryTTLById(_, { id, data }) {
      return DecisionService.updateHistoryTTLById(id, data)
    },

    // ────────────────────────────────────────────────────────────────
    //  Historic Decision Instances
    // ────────────────────────────────────────────────────────────────

    async getHistoricDecisionInstances({ commit }, params) {
      const result = await DecisionService.getHistoricDecisionInstances(params)
      commit('setHistoricInstances', result)
      return result
    },

    async getHistoricDecisionInstanceCount({ commit }, params) {
      const result = await DecisionService.getHistoricDecisionInstanceCount(params)
      if (result?.count != null) {
        commit('setHistoricCount', result.count)
      }
      return result
    },

    async getHistoricDecisionInstanceById({ commit }, { id, params }) {
      const result = await DecisionService.getHistoricDecisionInstanceById(id, params)
      commit('setHistoricInstance', result)
      return result
    },

    async deleteHistoricDecisionInstances(_, payload) {
      return DecisionService.deleteHistoricDecisionInstances(payload)
    },

    async setHistoricDecisionInstanceRemovalTime(_, payload) {
      return DecisionService.setHistoricDecisionInstanceRemovalTime(payload)
    }
  }
}

export default DecisionStore
