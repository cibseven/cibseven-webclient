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

import { ProcessService } from '@/services.js'

export default {
  namespaced: true,
  state: {
    calledProcessDefinitions: []
  },
  mutations: {
    setCalledProcessDefinitions(state, calledProcessDefinitions) {
      state.calledProcessDefinitions = calledProcessDefinitions
    }
  },
  actions: {
    async loadCalledProcessDefinitions({ commit }, processId) {
      const calledProcessDefinitions = await ProcessService.findCalledProcessDefinitions(processId)
      // Load current instances for each called process
      const instancePromises = calledProcessDefinitions.map(async (process) => {
        try {
          process.currentInstances = await ProcessService.findCurrentProcessesInstances({
            processDefinitionId: process.id
          })
        } catch (error) {
          process.currentInstances = []
        }
      })
      await Promise.all(instancePromises)
      commit('setCalledProcessDefinitions', calledProcessDefinitions)
      return calledProcessDefinitions
    }
  },
  getters: {
    calledProcessDefinitions: state => state.calledProcessDefinitions
  }
}
