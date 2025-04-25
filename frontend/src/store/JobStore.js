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