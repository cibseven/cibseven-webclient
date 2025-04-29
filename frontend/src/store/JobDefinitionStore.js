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
import { JobDefinitionService } from '@/services.js'

const JobDefinitionStore = {
    namespaced: true,
    state: { 
        list: [] 
    },
    mutations: {
        setJobDefinitions(state, jobDefinitions) {
            state.list = jobDefinitions
        }
    },
    actions: {
        async getJobDefinitions({ commit }, params) {
            const data = await JobDefinitionService.findJobDefinitions(params)
            commit('setJobDefinitions', data)
        },
        async suspendJobDefinition({ dispatch }, { jobDefinitionId, params, fetchParams }) {
            await JobDefinitionService.suspendJobDefinition(jobDefinitionId, params)
            await dispatch('getJobDefinitions', fetchParams)
        },
        async overridePriority({ dispatch }, { jobDefinitionId, params, fetchParams }) {
            await JobDefinitionService.overrideJobDefinitionPriority(jobDefinitionId, params)
            await dispatch('getJobDefinitions', fetchParams)
        },
        async retryJobDefinitionById(_, { id, params }) {
            return JobDefinitionService.retryJobDefinitionById(id, params)
        },
    },
    getters: {
        getJobDefinitions: (state) => state.list
    }
}

export default JobDefinitionStore
