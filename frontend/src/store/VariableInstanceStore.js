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

import { VariableInstanceService } from '@/services.js'

const VariableInstanceStore = {
  namespaced: true,
  state: { 
    current: null
  },
  mutations: {
    setVariableInstance: function(state, variableInstance) {
      state.current = variableInstance
    },
    clearVariableInstance: function(state) {
      state.current = null
    }
  },
  actions: {
    getVariableInstance: function(ctx, params) {
      return VariableInstanceService.getVariableInstance(params.id, params.deserializeValue)
        .then(variableInstance => {
          ctx.commit('setVariableInstance', variableInstance)
          return variableInstance
        })
    },
    clearVariableInstance: function(ctx) {
      ctx.commit('clearVariableInstance')
    }
  },
  getters: {
    currentVariableInstance: function(state) {
      return state.current
    }
  }
}

export default VariableInstanceStore
