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
import { it, expect, vi } from 'vitest'
import JobStore from '../../store/JobStore.js'
import { JobService, JobDefinitionService, IncidentService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  JobService: {
    getJobs: vi.fn(),
    setSuspended: vi.fn(),
    deleteJob: vi.fn(),
    getHistoryJobLog: vi.fn(),
    getHistoryJobLogStacktrace: vi.fn()
  },
  JobDefinitionService: {
    findJobDefinitions: vi.fn(),
    suspendJobDefinition: vi.fn(),
    overrideJobDefinitionPriority: vi.fn(),
    retryJobDefinitionById: vi.fn()
  },
  IncidentService: {
    retryJobById: vi.fn()
  }
}))

createStoreTestSuite('JobStore', JobStore, {
  initialState: (getState) => {
    it('should have empty jobs array', () => {
      const state = getState()
      expect(state.jobs).toEqual([])
    })

    it('should have empty jobLogs array', () => {
      const state = getState()
      expect(state.jobLogs).toEqual([])
    })

    it('should have empty jobDefinitions array', () => {
      const state = getState()
      expect(state.jobDefinitions).toEqual([])
    })
  },

  mutations: {
    setJobs: (mutation, state) => {
      it('should set jobs array', () => {
        const jobs = [
          { id: '1', name: 'Job 1', suspended: false },
          { id: '2', name: 'Job 2', suspended: true }
        ]
        mutation(state, jobs)
        expect(state.jobs).toEqual(jobs)
      })

      it('should replace existing jobs', () => {
        state.jobs = [{ id: 'old', name: 'Old Job' }]
        const newJobs = [{ id: 'new', name: 'New Job' }]
        
        mutation(state, newJobs)
        expect(state.jobs).toEqual(newJobs)
      })
    },

    setJobLogs: (mutation, state) => {
      it('should set job logs', () => {
        const logs = [
          { id: '1', jobId: 'job1', timestamp: '2023-01-01' },
          { id: '2', jobId: 'job2', timestamp: '2023-01-02' }
        ]
        mutation(state, logs)
        expect(state.jobLogs).toEqual(logs)
      })
    },

    setJobDefinitions: (mutation, state) => {
      it('should set job definitions', () => {
        const definitions = [
          { id: '1', key: 'def1', suspended: false },
          { id: '2', key: 'def2', suspended: true }
        ]
        mutation(state, definitions)
        expect(state.jobDefinitions).toEqual(definitions)
      })
    },

    removeJob: (mutation, state) => {
      it('should remove job by id', () => {
        state.jobs = [
          { id: '1', name: 'Job 1' },
          { id: '2', name: 'Job 2' },
          { id: '3', name: 'Job 3' }
        ]
        
        mutation(state, '2')
        
        expect(state.jobs).toHaveLength(2)
        expect(state.jobs.find(job => job.id === '2')).toBeUndefined()
        expect(state.jobs).toEqual([
          { id: '1', name: 'Job 1' },
          { id: '3', name: 'Job 3' }
        ])
      })

      it('should handle non-existent job id', () => {
        state.jobs = [{ id: '1', name: 'Job 1' }]
        
        mutation(state, 'non-existent')
        
        expect(state.jobs).toHaveLength(1)
        expect(state.jobs[0].id).toBe('1')
      })
    },

    updateJobSuspended: (mutation, state) => {
      it('should update job suspended state', () => {
        state.jobs = [
          { id: '1', name: 'Job 1', suspended: false },
          { id: '2', name: 'Job 2', suspended: true }
        ]
        
        mutation(state, { jobId: '1', suspended: true })
        
        expect(state.jobs[0].suspended).toBe(true)
        expect(state.jobs[1].suspended).toBe(true) // unchanged
      })

      it('should handle non-existent job', () => {
        state.jobs = [{ id: '1', suspended: false }]
        
        mutation(state, { jobId: 'non-existent', suspended: true })
        
        expect(state.jobs[0].suspended).toBe(false) // unchanged
      })
    },

    updateJobDefinitionSuspended: (mutation, state) => {
      it('should update job definition suspended state', () => {
        state.jobDefinitions = [
          { id: '1', key: 'def1', suspended: false },
          { id: '2', key: 'def2', suspended: false },
          { id: '3', key: 'def3', suspended: true }
        ]
        
        mutation(state, { jobDefinitionId: '1', suspended: true })
        
        expect(state.jobDefinitions[0].suspended).toBe(true)
        expect(state.jobDefinitions[1].suspended).toBe(false) // unchanged
        expect(state.jobDefinitions[2].suspended).toBe(true) // unchanged
      })
    },

    updateJobDefinitionPriority: (mutation, state) => {
      it('should update job definition priority', () => {
        state.jobDefinitions = [
          { id: '1', key: 'def1', overridingJobPriority: null }
        ]
        
        mutation(state, { jobDefinitionId: '1', priority: 10 })
        
        expect(state.jobDefinitions[0].overridingJobPriority).toBe(10)
      })

      it('should handle null priority', () => {
        state.jobDefinitions = [
          { id: '1', key: 'def1', overridingJobPriority: 5 }
        ]
        
        mutation(state, { jobDefinitionId: '1', priority: null })
        
        expect(state.jobDefinitions[0].overridingJobPriority).toBeNull()
      })
    }
  },

  getters: {
    jobs: (getter, state) => {
      it('should return jobs array', () => {
        const jobs = [{ id: '1', name: 'Test Job' }]
        state.jobs = jobs
        expect(getter(state)).toEqual(jobs)
      })
    },

    jobLogs: (getter, state) => {
      it('should return job logs array', () => {
        const logs = [{ id: '1', jobId: 'job1' }]
        state.jobLogs = logs
        expect(getter(state)).toEqual(logs)
      })
    },

    jobDefinitions: (getter, state) => {
      it('should return job definitions array', () => {
        const definitions = [{ id: '1', key: 'def1' }]
        state.jobDefinitions = definitions
        expect(getter(state)).toEqual(definitions)
      })
    }
  },

  actions: {
    loadJobs: (action, getContext) => {
      it('should load and commit jobs', async () => {
        const context = getContext()
        const mockJobs = [
          { id: '1', name: 'Test Job', suspended: false }
        ]
        const query = { suspended: false, processInstanceId: 'proc1' }
        
        JobService.getJobs.mockResolvedValue(mockJobs)

        const result = await action(context, query)

        expect(JobService.getJobs).toHaveBeenCalledWith(query)
        expect(context.commit).toHaveBeenCalledWith('setJobs', mockJobs)
        expect(result).toEqual(mockJobs)
      })

      it('should handle service errors', async () => {
        const context = getContext()
        const error = new Error('Service error')
        JobService.getJobs.mockRejectedValue(error)

        await expect(action(context, {})).rejects.toThrow('Service error')
      })
    },

    loadJobDefinitions: (action, getContext) => {
      it('should load and commit job definitions', async () => {
        const context = getContext()
        const mockDefinitions = [
          { id: '1', key: 'def1', suspended: false }
        ]
        const query = { processDefinitionId: 'proc1' }
        
        JobDefinitionService.findJobDefinitions.mockResolvedValue(mockDefinitions)

        const result = await action(context, query)

        expect(JobDefinitionService.findJobDefinitions).toHaveBeenCalledWith(query)
        expect(context.commit).toHaveBeenCalledWith('setJobDefinitions', mockDefinitions)
        expect(result).toEqual(mockDefinitions)
      })
    },

    loadJobDefinitionsByProcessDefinition: (action, getContext) => {
      it('should dispatch loadJobDefinitions with processDefinitionId', async () => {
        const context = getContext()
        const processDefinitionId = 'proc-def-1'
        const mockResult = [{ id: '1', key: 'def1' }]
        
        context.dispatch.mockResolvedValue(mockResult)

        const result = await action(context, processDefinitionId)

        expect(context.dispatch).toHaveBeenCalledWith('loadJobDefinitions', { processDefinitionId })
        expect(result).toEqual(mockResult)
      })
    },

    suspendJobDefinition: (action, getContext) => {
      it('should suspend job definition and update state', async () => {
        const context = getContext()
        const jobDefinitionId = 'def-1'
        const params = { suspended: true }
        
        JobDefinitionService.suspendJobDefinition.mockResolvedValue()

        await action(context, { jobDefinitionId, params })

        expect(JobDefinitionService.suspendJobDefinition).toHaveBeenCalledWith(jobDefinitionId, params)
        expect(context.commit).toHaveBeenCalledWith('updateJobDefinitionSuspended', { jobDefinitionId, suspended: true })
      })
    },

    overrideJobDefinitionPriority: (action, getContext) => {
      it('should override job definition priority', async () => {
        const context = getContext()
        const jobDefinitionId = 'def-1'
        const params = { priority: 10 }
        
        JobDefinitionService.overrideJobDefinitionPriority.mockResolvedValue()

        await action(context, { jobDefinitionId, params })

        expect(JobDefinitionService.overrideJobDefinitionPriority).toHaveBeenCalledWith(jobDefinitionId, params)
        expect(context.commit).toHaveBeenCalledWith('updateJobDefinitionPriority', { jobDefinitionId, priority: 10 })
      })

      it('should handle clearing priority', async () => {
        const context = getContext()
        const jobDefinitionId = 'def-1'
        const params = {} // No priority means clearing
        
        JobDefinitionService.overrideJobDefinitionPriority.mockResolvedValue()

        await action(context, { jobDefinitionId, params })

        expect(context.commit).toHaveBeenCalledWith('updateJobDefinitionPriority', { jobDefinitionId, priority: null })
      })
    },

    getHistoryJobLog: (action, getContext) => {
      it('should get history job log and commit result', async () => {
        const context = getContext()
        const mockLogs = [
          { id: '1', jobId: 'job1', timestamp: '2023-01-01' }
        ]
        const params = { jobId: 'job1' }
        
        JobService.getHistoryJobLog.mockResolvedValue(mockLogs)

        const result = await action(context, params)

        expect(JobService.getHistoryJobLog).toHaveBeenCalledWith(params)
        expect(context.commit).toHaveBeenCalledWith('setJobLogs', mockLogs)
        expect(result).toEqual(mockLogs)
      })
    },

    retryJobById: (action, getContext) => {
      it('should retry job by id', async () => {
        const context = getContext()
        const id = 'job-1'
        const retries = 3
        const mockResult = { success: true }
        
        IncidentService.retryJobById.mockResolvedValue(mockResult)

        const result = await action(context, { id, retries })

        expect(IncidentService.retryJobById).toHaveBeenCalledWith(id, { retries })
        expect(result).toEqual(mockResult)
      })
    },

    retryJobDefinitionById: (action, getContext) => {
      it('should retry job definition by id', async () => {
        const context = getContext()
        const id = 'def-1'
        const params = { retries: 5 }
        const mockResult = { success: true }
        
        JobDefinitionService.retryJobDefinitionById.mockResolvedValue(mockResult)

        const result = await action(context, { id, params })

        expect(JobDefinitionService.retryJobDefinitionById).toHaveBeenCalledWith(id, params)
        expect(result).toEqual(mockResult)
      })
    },

    setSuspended: (action, getContext) => {
      it('should set job suspended state', async () => {
        const context = getContext()
        const jobId = 'job-1'
        const suspended = true
        
        JobService.setSuspended.mockResolvedValue()

        await action(context, { jobId, suspended })

        expect(JobService.setSuspended).toHaveBeenCalledWith(jobId, { suspended })
        expect(context.commit).toHaveBeenCalledWith('updateJobSuspended', { jobId, suspended })
      })
    },

    getHistoryJobLogStacktrace: (action, getContext) => {
      it('should get history job log stacktrace', async () => {
        const context = getContext()
        const id = 'log-1'
        const mockStacktrace = 'java.lang.Exception: Test error\n  at ...'
        
        JobService.getHistoryJobLogStacktrace.mockResolvedValue(mockStacktrace)

        const result = await action(context, id)

        expect(JobService.getHistoryJobLogStacktrace).toHaveBeenCalledWith(id)
        expect(result).toBe(mockStacktrace)
      })
    },

    deleteJob: (action, getContext) => {
      it('should delete job and remove from state', async () => {
        const context = getContext()
        const id = 'job-1'
        
        JobService.deleteJob.mockResolvedValue()

        await action(context, id)

        expect(JobService.deleteJob).toHaveBeenCalledWith(id)
        expect(context.commit).toHaveBeenCalledWith('removeJob', id)
      })
    },

    clearJobLogs: (action, getContext) => {
      it('should clear job logs', () => {
        const context = getContext()
        action(context)
        expect(context.commit).toHaveBeenCalledWith('setJobLogs', [])
      })
    }
  }
})