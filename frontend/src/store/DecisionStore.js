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
import { DecisionService } from '@/services.js'

const DecisionStore = {
  state: {
    list: [],
    selectedDecisionVersion: null,
    selectedInstance: null,
    instances: [],
    isLoading: false
  },
  mutations: {
    setDecisions: function (state, params) {
      state.list = params.decisions
    },
    setLoading: function (state, isLoading) {
      state.isLoading = isLoading
    },
    setHistoricInstancesForKey: function (state, { key, version, instances }) {
      const decision = state.list.find(decision => decision.key === key)
      if (decision) {
        const targetVersion = decision.versions.find(v => String(v.version) === String(version))
        if (targetVersion) {
          targetVersion.historicInstances = instances
        }
      }
    },
    setHistoricInstances(state, instances) {
      state.instances = instances
    },
    setDecisionVersions(state, { key, versions }) {
      const decision = state.list.find(decision => decision.key === key)
      if (decision) decision.versions = versions
    },
    setSelectedDecisionVersion(state, { key, version }) {
      const decision = state.list.find(decision => decision.key === key)
      if (decision && decision.versions) {
        const targetVersion = decision.versions.find(v => String(v.version) === String(version))
        if (targetVersion) {
          state.selectedDecisionVersion = targetVersion
        }
      }
    },
    setSelectedInstance(state, instance) {
      state.selectedInstance = instance
    },
    updateVersion(state, { key, newVersion }) {
      const decision = state.list.find(d => d.key === key)
      if (decision) {
        const index = decision.versions.findIndex(v => v.id === newVersion.id)
        if (index !== -1) {
          decision.versions.splice(index, 1, newVersion)
        }
      }
    }
  },
  getters: {
    getFilteredDecisions: (state) => (filter) => {
      if (!state.list) return []
      let decisions = state.list
      // If filter is provided and not empty, apply filtering
      if (filter && filter.trim() !== '') {
        const filterUpper = filter.toUpperCase()
        decisions = state.list.filter(decision => {
          return (
            decision.key.toUpperCase().includes(filterUpper) ||
            (decision.name && decision.name.toUpperCase().includes(filterUpper))
          )
        })
      }
      // Sort the results
      decisions.sort((objA, objB) => {
        const nameA = objA.name ? objA.name.toUpperCase() : ''
        const nameB = objB.name ? objB.name.toUpperCase() : ''
        return nameA.localeCompare(nameB)
      })
      return decisions
    },
    getDecisionByKey: (state) => (key) => {
      return state.list.find(d => d.key === key)
    },
    getDecisionInstances: (state) => (key, version) => {
      const decision = state.list.find(d => d.key === key)
      if (decision) {
        const targetVersion = decision.versions.find(v => String(v.version) === String(version))
        return targetVersion?.historicInstances || []
      }
    },
    getDecisionVersions: (state) => (key) => {
      const decision = state.list.find(d => d.key === key)
      return decision?.versions || []
    },
    getSelectedDecisionVersion: (state) => () => {
      return state.selectedDecisionVersion
    },
    getDecisionVersion: (state) => ({ key, version }) => {
      const decision = state.list.find(d => d.key === key)
      return decision?.versions?.find(v => String(v.version) === String(version)) || null
    },
    decisionInstances: (state) => state.instances,
    isLoading: (state) => state.isLoading
  },
  actions: {

    // ────────────────────────────────────────────────────────────────
    //  Definitions
    // ────────────────────────────────────────────────────────────────

    async getDecisionList({ commit }, params) {
      commit('setLoading', true)
      try {
        const decisions = await DecisionService.getDecisionList(params)
        const reduced = decisions.map(d => ({ 
          key: d.key, 
          id: d.id, 
          name: d.name, 
          latestVersion: d.version, 
          tenantId: d.tenantId,
          decisionRequirementsDefinitionId: d.decisionRequirementsDefinitionId,
          decisionRequirementsDefinitionKey: d.decisionRequirementsDefinitionKey
        }))
        commit('setDecisions', { decisions: reduced })
        return reduced
      } finally {
        commit('setLoading', false)
      }
    },
    async getDecisionByKey({ state }, params) {
      if (state.list && state.list.length > 0) {
        const found = state.list.find(decision => decision.key === params.key)
        if (found) return found
      }
      const newList = await DecisionService.getDecisionList(params)
      const foundAfterReload = newList.find(decision => decision.key === params.key)
      if (foundAfterReload) return foundAfterReload
      return DecisionService.getDecisionByKey(params.key)
    },
    async getDecisionByKeyAndTenant(_, { key, tenant }) {
      return DecisionService.getDecisionByKeyAndTenant(key, tenant)
    },
    async getDecisionVersionsByKey({ commit }, { key, lazyLoad }) {
      const result = await DecisionService.getDecisionVersionsByKey(key, lazyLoad)
      if (lazyLoad) {
        result.forEach(v => {
          v.allInstances = '-'
        })
      }
      commit('setDecisionVersions', { key, versions: result })
      return result
    },
    async refreshDecisionVersions({ commit, getters }, key, lazyLoad) {
      // Get fresh versions from service
      const freshVersions = await DecisionService.getDecisionVersionsByKey(key, lazyLoad)
      // Get current versions from store
      const currentVersions = getters.getDecisionVersions(key)
      // Find new versions (versions that don't exist in current list)
      const newVersions = freshVersions.filter(freshVersion => 
        !currentVersions.some(currentVersion => currentVersion.id === freshVersion.id)
      )
      if (newVersions.length > 0) {
        // Merge current versions with new versions, sorted by version number descending
        const mergedVersions = [...currentVersions, ...newVersions]
          .sort((a, b) => Number.parseInt(b.version) - Number.parseInt(a.version))
        // Update store with merged versions
        commit('setDecisionVersions', { key, versions: mergedVersions })
      }
    },
    async getDecisionById(_, id) {
      const decisions = await DecisionService.getDecisionList({ decisionDefinitionId: id })
      if (decisions && decisions.length > 0) {
        return decisions[0]
      } else {
        return null
      }
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

    async getHistoricDecisionInstances({ commit }, { key, version, params }) {
      const result = await DecisionService.getHistoricDecisionInstances(params)
      commit('setHistoricInstancesForKey', { key, version, instances: result })
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
