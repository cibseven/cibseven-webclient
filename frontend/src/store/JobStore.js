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
import { JobService, IncidentService } from "@/services.js"

const JobStore = {
  state: {
    jobs: [],
    jobLogs: []
  },
  mutations: {
    setJobs(state, jobs) {
      state.jobs = jobs
    },
    setJobLogs(state, logs) {
      state.jobLogs = logs
    },
    removeJob(state, id) {
      state.jobs = state.jobs.filter(job => job.id !== id)
    }
  },
  actions: {
    async getJobs({ commit }, query) {
      const result = await JobService.getJobs(query)
      commit('setJobs', result)
      return result
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
    async getHistoryJobLogStacktrace(_, id) {
      return JobService.getHistoryJobLogStacktrace(id)
    },
    async deleteJob({ commit }, id) {
      await JobService.deleteJob(id)
      commit('removeJob', id)
    }
  },
  getters: {
    jobs: state => state.jobs,
    jobLogs: state => state.jobLogs
  }
}

export default JobStore