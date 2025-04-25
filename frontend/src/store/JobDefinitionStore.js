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
