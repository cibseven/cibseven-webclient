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

const ProcessStore = {
  state: { list: [] },
  mutations: {
    setProcesses: function (state, param) {
      state.list = param.processes
    },
    setFavorite: function (state, params) {
      params.process.favorite = params.value
    },
    setStatistics: function (state, params) {
      params.process.statistics = params.statistics
    },
    setSuspended: function (state, params) {
      params.process.suspended = params.suspended
    }
  },
  actions: {
    findProcesses: function () {
      return ProcessService.findProcesses()
    },
    findProcessesWithInfo: function () {
      return ProcessService.findProcessesWithInfo()
    },
    getProcessByDefinitionKey: function (ctx, params) {
      var process = ctx.state.list.find(process => { 
        return process.key === params.key && process.tenantId === params.tenantId 
      })
      if (process) return Promise.resolve(process)
      else return ProcessService.findProcessByDefinitionKey(params.key, params.tenantId)
    },
    getProcessById: function (ctx, params) {
      var process = ctx.state.list.find(process => { return process.id === params.id })
      if (process) return Promise.resolve(process)
      else return ProcessService.findProcessById(params.id)
    },
    setFavorites: function (ctx, params) {
      ctx.state.list.forEach(process => {
        if (params.favorites.indexOf(process.key) !== -1) ctx.commit('setFavorite', { process: process, value: true })
        else ctx.commit('setFavorite', { process: process, value: false })
      })
    },
    setStatistics: function(ctx, params) {
      ctx.commit('setStatistics', { process: params.process, statistics: params.statistics })
    },
    setSuspended: function (ctx, params) {
      ctx.commit('setSuspended', { process: params.process, suspended: params.suspended })
    }
  }
}

export default ProcessStore
