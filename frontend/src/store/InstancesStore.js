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

import { HistoryService, ProcessService } from '@/services.js'

export default {
  namespaced: true,
  state: {
    instances: []
  },
  mutations: {
    setInstances(state, instances) {
      state.instances = instances
    },
    appendInstances(state, instances) {
      state.instances = state.instances.concat(instances)
    },
    removeInstance(state, instanceId) {
      state.instances = state.instances.filter(instance => instance.id !== instanceId)
    },
    updateInstanceState(state, { instanceId, newState }) {
      const instance = state.instances.find(instance => instance.id === instanceId)
      if (instance) {
        instance.state = newState
      }
    }
  },
  actions: {
    async loadInstances({ commit }, { processId, activityId, filter, showMore = false, tenantId, camundaHistoryLevel, firstResult, maxResults, sortingCriteria = [], fetchIncidents = false }) {
      if (camundaHistoryLevel !== 'none') {
        const instances = await HistoryService.findProcessesInstancesHistoryById(
          processId, 
          activityId,
          firstResult, 
          maxResults, 
          filter,
          null, // active parameter
          sortingCriteria, // Pass sorting criteria to service
          fetchIncidents // Pass fetchIncidents parameter to service
        )
        if (showMore) {
          commit('appendInstances', instances)
        } else {
          commit('setInstances', instances)
        }
        return instances
      } else {
        const versions = await ProcessService.findProcessVersionsByDefinitionKey(processId, tenantId, true)
        const promises = versions.map(() => 
          HistoryService.findProcessesInstancesHistoryById(processId, activityId, firstResult, maxResults, filter, null, sortingCriteria, fetchIncidents)
        )
        const responses = await Promise.all(promises)
        if (!showMore) commit('setInstances', [])
        let i = 0
        responses.forEach(instances => {
          instances.forEach(instance => {
            instance.processDefinitionId = versions[i].id
            instance.processDefinitionVersion = versions[i].version
          })
          commit('appendInstances', instances)
          i++
        })
        return responses.flat()
      }
    },
    resetInstances({ commit }) {
      commit('setInstances', [])
    }
  },
  getters: {
    instances: state => state.instances
  }
}
