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
import { it, expect, vi, beforeEach } from 'vitest'
import ProcessStore from '../../store/ProcessStore.js'
import { ProcessService, HistoryService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  ProcessService: {
    findProcesses: vi.fn(),
    findProcessesWithInfo: vi.fn(),
    findProcessByDefinitionKey: vi.fn(),
    findProcessById: vi.fn()
  },
  HistoryService: {
    findHistoryActivityStatistics: vi.fn(),
    findHistoryActivityStatisticsForInstances: vi.fn()
  }
}))

const process = (overrides = {}) => ({ id: 'pd-1', key: 'invoice', tenantId: null, ...overrides })

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('ProcessStore', ProcessStore, {
  initialState: (getState) => {
    it('should start with an empty list and no activity statistics', () => {
      expect(getState()).toEqual({ list: [], historicActivityStatistics: {} })
    })
  },

  mutations: {
    setProcesses: (mutation, getState) => {
      it('should replace the process list', () => {
        const state = getState()
        mutation(state, { processes: [process()] })
        expect(state.list).toEqual([process()])
      })
    },

    setFavorite: (mutation, getState) => {
      it('should set the favorite flag on the given process', () => {
        const state = getState()
        const target = process()
        mutation(state, { process: target, value: true })
        expect(target.favorite).toBe(true)
      })
    },

    setStatistics: (mutation, getState) => {
      it('should attach statistics to the given process', () => {
        const state = getState()
        const target = process()
        mutation(state, { process: target, statistics: [{ instances: 3 }] })
        expect(target.statistics).toEqual([{ instances: 3 }])
      })
    },

    setSuspended: (mutation, getState) => {
      it('should set the suspended flag on the given process', () => {
        const state = getState()
        const target = process()
        mutation(state, { process: target, suspended: true })
        expect(target.suspended).toBe(true)
      })
    },

    setHistoricActivityStatistics: (mutation, getState) => {
      it('should store statistics under the given key without dropping other keys', () => {
        const state = getState()
        state.historicActivityStatistics = { other: [{ id: 'x' }] }

        mutation(state, { key: 'pd-1', data: [{ id: 'task_1' }] })

        expect(state.historicActivityStatistics).toEqual({
          other: [{ id: 'x' }],
          'pd-1': [{ id: 'task_1' }]
        })
      })
    },

    clearHistoricActivityStatistics: (mutation, getState) => {
      it('should drop only the given key', () => {
        const state = getState()
        state.historicActivityStatistics = { a: [1], b: [2] }

        mutation(state, 'a')

        expect(state.historicActivityStatistics).toEqual({ b: [2] })
      })

      it('should drop every key when called without one', () => {
        const state = getState()
        state.historicActivityStatistics = { a: [1], b: [2] }

        mutation(state)

        expect(state.historicActivityStatistics).toEqual({})
      })
    },

    removeProcessByKeyTenant: (mutation, getState) => {
      // Key alone is not unique across tenants, so both must match to remove.
      it('should remove only the process matching both key and tenant', () => {
        const state = getState()
        state.list = [
          process({ id: '1', key: 'invoice', tenantId: 't-1' }),
          process({ id: '2', key: 'invoice', tenantId: 't-2' }),
          process({ id: '3', key: 'order', tenantId: 't-1' })
        ]

        mutation(state, { key: 'invoice', tenantId: 't-1' })

        expect(state.list.map(p => p.id)).toEqual(['2', '3'])
      })
    }
  },

  getters: {
    getHistoricActivityStatistics: (getter, getState) => {
      it('should return the statistics stored for the key', () => {
        const state = getState()
        state.historicActivityStatistics = { 'pd-1': [{ id: 'task_1' }] }
        expect(getter(state)('pd-1')).toEqual([{ id: 'task_1' }])
      })

      it('should return an empty array for an unknown key', () => {
        expect(getter(getState())('missing')).toEqual([])
      })
    }
  },

  actions: {
    findProcesses: (action, getContext) => {
      it('should delegate to the process service', async () => {
        ProcessService.findProcesses.mockResolvedValue([process()])

        const result = await action(getContext())

        expect(ProcessService.findProcesses).toHaveBeenCalled()
        expect(result).toEqual([process()])
      })
    },

    findProcessesWithInfo: (action, getContext) => {
      it('should delegate to the process service', async () => {
        ProcessService.findProcessesWithInfo.mockResolvedValue([process()])

        const result = await action(getContext())

        expect(ProcessService.findProcessesWithInfo).toHaveBeenCalled()
        expect(result).toEqual([process()])
      })
    },

    getProcessByDefinitionKey: (action, getContext) => {
      // The cached list is consulted first so that navigating between tabs does not refetch.
      it('should resolve from the cached list without calling the service', async () => {
        const context = getContext()
        context.state.list = [process({ key: 'invoice', tenantId: 't-1' })]

        const result = await action(context, { key: 'invoice', tenantId: 't-1' })

        expect(result).toEqual(process({ key: 'invoice', tenantId: 't-1' }))
        expect(ProcessService.findProcessByDefinitionKey).not.toHaveBeenCalled()
      })

      it('should fall back to the service when the tenant does not match', async () => {
        const context = getContext()
        context.state.list = [process({ key: 'invoice', tenantId: 't-1' })]
        ProcessService.findProcessByDefinitionKey.mockResolvedValue(process({ tenantId: 't-2' }))

        const result = await action(context, { key: 'invoice', tenantId: 't-2' })

        expect(ProcessService.findProcessByDefinitionKey).toHaveBeenCalledWith('invoice', 't-2')
        expect(result).toEqual(process({ tenantId: 't-2' }))
      })
    },

    getProcessById: (action, getContext) => {
      it('should resolve from the cached list without calling the service', async () => {
        const context = getContext()
        context.state.list = [process({ id: 'pd-1' })]

        const result = await action(context, { id: 'pd-1' })

        expect(result).toEqual(process({ id: 'pd-1' }))
        expect(ProcessService.findProcessById).not.toHaveBeenCalled()
      })

      it('should fall back to the service for an unknown id', async () => {
        const context = getContext()
        ProcessService.findProcessById.mockResolvedValue(process({ id: 'pd-9' }))

        const result = await action(context, { id: 'pd-9' })

        expect(ProcessService.findProcessById).toHaveBeenCalledWith('pd-9')
        expect(result).toEqual(process({ id: 'pd-9' }))
      })
    },

    setFavorites: (action, getContext) => {
      it('should mark listed processes favorite and clear the rest', () => {
        const context = getContext()
        const favored = process({ key: 'invoice' })
        const other = process({ key: 'order' })
        context.state.list = [favored, other]

        action(context, { favorites: ['invoice'] })

        expect(context.commit).toHaveBeenCalledWith('setFavorite', { process: favored, value: true })
        expect(context.commit).toHaveBeenCalledWith('setFavorite', { process: other, value: false })
      })
    },

    setStatistics: (action, getContext) => {
      it('should forward the statistics payload to the mutation', () => {
        const context = getContext()
        const target = process()

        action(context, { process: target, statistics: [{ instances: 1 }] })

        expect(context.commit).toHaveBeenCalledWith('setStatistics', { process: target, statistics: [{ instances: 1 }] })
      })
    },

    setSuspended: (action, getContext) => {
      it('should forward the suspended flag to the mutation', () => {
        const context = getContext()
        const target = process()

        action(context, { process: target, suspended: true })

        expect(context.commit).toHaveBeenCalledWith('setSuspended', { process: target, suspended: true })
      })
    },

    clearHistoricActivityStatistics: (action, getContext) => {
      it('should forward the key to the mutation', () => {
        const context = getContext()
        action(context, 'pd-1')
        expect(context.commit).toHaveBeenCalledWith('clearHistoricActivityStatistics', 'pd-1')
      })
    },

    removeProcessByKeyTenant: (action, getContext) => {
      it('should forward key and tenant to the mutation', () => {
        const context = getContext()
        action(context, { key: 'invoice', tenantId: 't-1' })
        expect(context.commit).toHaveBeenCalledWith('removeProcessByKeyTenant', { key: 'invoice', tenantId: 't-1' })
      })
    },

    loadHistoricActivityStatistics: (action, getContext) => {
      it('should request the full statistics set and store it under the definition id', async () => {
        const context = getContext()
        HistoryService.findHistoryActivityStatistics.mockResolvedValue([{ id: 'task_1' }])

        await action(context, { processDefinitionId: 'pd-1' })

        expect(HistoryService.findHistoryActivityStatistics).toHaveBeenCalledWith('pd-1', {
          canceled: true, completedScoped: true, finished: true, incidents: true
        })
        expect(context.commit).toHaveBeenCalledWith('setHistoricActivityStatistics', {
          key: 'pd-1', data: [{ id: 'task_1' }]
        })
      })

      it('should let caller params override the defaults', async () => {
        const context = getContext()
        HistoryService.findHistoryActivityStatistics.mockResolvedValue([])

        await action(context, { processDefinitionId: 'pd-1', params: { incidents: false, extra: 1 } })

        expect(HistoryService.findHistoryActivityStatistics).toHaveBeenCalledWith('pd-1', {
          canceled: true, completedScoped: true, finished: true, incidents: false, extra: 1
        })
      })
    },

    loadHistoricActivityStatisticsForInstances: (action, getContext) => {
      // Note the different parameter names the instance-scoped endpoint expects.
      it('should request the include-* statistics set and store it under the definition id', async () => {
        const context = getContext()
        HistoryService.findHistoryActivityStatisticsForInstances.mockResolvedValue([{ id: 'task_1' }])

        await action(context, { processDefinitionId: 'pd-1' })

        expect(HistoryService.findHistoryActivityStatisticsForInstances).toHaveBeenCalledWith('pd-1', {
          includeCanceled: true, includeCompleteScope: true, includeFinished: true, includeIncidents: true
        }, null)
        expect(context.commit).toHaveBeenCalledWith('setHistoricActivityStatistics', {
          key: 'pd-1', data: [{ id: 'task_1' }]
        })
      })

      it('should let the filter override the defaults', async () => {
        const context = getContext()
        HistoryService.findHistoryActivityStatisticsForInstances.mockResolvedValue([])

        await action(context, { processDefinitionId: 'pd-1', filter: { includeFinished: false } })

        expect(HistoryService.findHistoryActivityStatisticsForInstances).toHaveBeenCalledWith('pd-1', {
          includeCanceled: true, includeCompleteScope: true, includeFinished: false, includeIncidents: true
        }, null)
      })
    }
  }
})
