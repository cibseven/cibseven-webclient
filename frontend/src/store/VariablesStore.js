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

const serviceMap = {
  ProcessService: ProcessService,
  HistoryService: HistoryService
}

export default {
  namespaced: true,
  state: () => ({
    variables: []
  }),
  mutations: {
    setVariables(state, variables) {
      state.variables = variables
    }
  },
  actions: {
    async loadVariables({ commit }, { instanceId, activityInstancesGrouped, isActive, camundaHistoryLevel }) {
      if (!instanceId) {
        commit('setVariables', [])
        return []
      }
      let service, method
      if (isActive) {
        service = 'ProcessService'
        method = 'fetchProcessInstanceVariables'
      } else {
        if (camundaHistoryLevel === 'full') {
          service = 'HistoryService'
          method = 'fetchProcessInstanceVariablesHistory'
        } else {
          return []
        }
      }
      const variablesToSerialize = []
      const filter = { deserializeValue: false }
      let variables = await serviceMap[service][method](instanceId, filter)
      variables.forEach(variable => {
        try {
          variable.value = variable.type === 'Object' ? JSON.parse(variable.value) : variable.value
        } catch {
          variablesToSerialize.push(variable.id)
        }
        variable.modify = false
      })
      if (variablesToSerialize.length > 0) {
        const dVariables = await serviceMap[service][method](instanceId, filter)
        dVariables.forEach(dVariable => {
          const variableToSerialize = variables.find(variable => variable.id === dVariable.id)
          if (variableToSerialize) {
            variableToSerialize.value = dVariable.value
          }
        })
      }
      if (activityInstancesGrouped) {
        variables.forEach(v => {
          v.scope = activityInstancesGrouped[v.activityInstanceId]
        })
      }
      commit('setVariables', variables)
      return variables
    },
    clearVariables({ commit }) {
      commit('setVariables', [])
    }
  },
  getters: {
    variables: state => state.variables
  }
}
