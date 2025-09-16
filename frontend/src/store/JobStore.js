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
import { JobService, IncidentService, JobDefinitionService } from "@/services.js"

const JobStore = {
  namespaced: true,
  state: () => ({
    jobs: [],
    jobLogs: [],
    jobDefinitions: []
  }),
  mutations: {
    setJobs(state, jobs) {
      state.jobs = jobs
    },
    setJobLogs(state, logs) {
      state.jobLogs = logs
    },
    setJobDefinitions(state, jobDefinitions) {
      state.jobDefinitions = jobDefinitions
    },
    removeJob(state, id) {
      state.jobs = state.jobs.filter(job => job.id !== id)
    },
    updateJobSuspended(state, { jobId, suspended }) {
      const job = state.jobs.find(job => job.id === jobId)
      if (job) {
        job.suspended = suspended
      }
    },
    updateJobDefinitionSuspended(state, { jobDefinitionId, suspended }) {
      const jobDefinition = state.jobDefinitions.find(jd => jd.id === jobDefinitionId)
      if (jobDefinition) {
        jobDefinition.suspended = suspended
      }
    },
    updateJobDefinitionPriority(state, { jobDefinitionId, priority }) {
      const jobDefinition = state.jobDefinitions.find(jd => jd.id === jobDefinitionId)
      if (jobDefinition) {
        jobDefinition.overridingJobPriority = priority
      }
    }
  },
  actions: {
    async loadJobs({ commit }, query) {
      const result = await JobService.getJobs(query)
      commit('setJobs', result)
      return result
    },
    async loadJobDefinitions({ commit }, query) {
      const result = await JobDefinitionService.findJobDefinitions(query)
      commit('setJobDefinitions', result)
      return result
    },
    async loadJobDefinitionsByProcessDefinition({ dispatch }, processDefinitionId) {
      return dispatch('loadJobDefinitions', { processDefinitionId })
    },
    async suspendJobDefinition({ commit }, { jobDefinitionId, params }) {
      await JobDefinitionService.suspendJobDefinition(jobDefinitionId, params)
      // Update the suspended state based on the params
      commit('updateJobDefinitionSuspended', { jobDefinitionId, suspended: params.suspended })
    },
    async overrideJobDefinitionPriority({ commit }, { jobDefinitionId, params }) {
      await JobDefinitionService.overrideJobDefinitionPriority(jobDefinitionId, params)
      // Update the priority - null if clearing, otherwise the priority value
      const priority = params.priority || null
      commit('updateJobDefinitionPriority', { jobDefinitionId, priority })
    },
    async getHistoryJobLog({ commit }, params) {
      const result = await JobService.getHistoryJobLog(params)
      commit('setJobLogs', result)
      return result
    },
    async retryJobById(_, { id, retries }) {
      const params = { retries }
      return IncidentService.retryJobById(id, params)
    },
    async retryJobDefinitionById(_, { id, params }) {
      return JobDefinitionService.retryJobDefinitionById(id, params)
    },
    async setSuspended({ commit }, { jobId, suspended }) {
      await JobService.setSuspended(jobId, { suspended })
      commit('updateJobSuspended', { jobId, suspended })
    },
    async getHistoryJobLogStacktrace(_, id) {
      return JobService.getHistoryJobLogStacktrace(id)
    },
    async deleteJob({ commit }, id) {
      await JobService.deleteJob(id)
      commit('removeJob', id)
    },
    clearJobLogs({ commit }) {
      commit('setJobLogs', [])
    }
  },
  getters: {
    jobs: state => state.jobs,
    jobLogs: state => state.jobLogs,
    jobDefinitions: state => state.jobDefinitions
  }
}

export default JobStore