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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import BatchStore from '../../store/BatchStore.js'
import { BatchService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  BatchService: {
    getBatches: vi.fn(),
    deleteBatch: vi.fn(),
    getHistoricBatches: vi.fn(),
    getHistoricBatchById: vi.fn(),
    getBatchStatistics: vi.fn(),
    setBatchSuspensionState: vi.fn(),
    getHistoricBatchCount: vi.fn(),
    getCleanableBatchReport: vi.fn(),
    getCleanableBatchReportCount: vi.fn(),
    deleteHistoricBatch: vi.fn(),
    setHistoricBatchRemovalTime: vi.fn()
  }
}))

const batch = (overrides = {}) => ({ id: 'b-1', type: 'instance-deletion', endTime: null, ...overrides })

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('BatchStore', BatchStore, {
  initialState: (getState) => {
    it('should start with empty collections and zero counts', () => {
      expect(getState()).toEqual({
        runtimeBatches: [],
        historicBatches: [],
        cleanableBatchReport: [],
        selectedHistoricBatch: null,
        historicBatchCount: 0,
        cleanableBatchReportCount: 0
      })
    })
  },

  mutations: {
    setRuntimeBatches: (mutation, getState) => {
      it('should replace the runtime batch list', () => {
        const state = getState()
        state.runtimeBatches = [batch({ id: 'old' })]
        mutation(state, [batch({ id: 'new' })])
        expect(state.runtimeBatches).toEqual([batch({ id: 'new' })])
      })
    },

    removeRuntimeBatch: (mutation, getState) => {
      it('should remove the runtime batch with the given id', () => {
        const state = getState()
        state.runtimeBatches = [batch({ id: 'a' }), batch({ id: 'b' })]
        mutation(state, 'a')
        expect(state.runtimeBatches.map(b => b.id)).toEqual(['b'])
      })
    },

    setHistoricBatches: (mutation, getState) => {
      it('should replace the historic batch list', () => {
        const state = getState()
        state.historicBatches = [batch({ id: 'old' })]
        mutation(state, [batch({ id: 'new' })])
        expect(state.historicBatches).toEqual([batch({ id: 'new' })])
      })
    },

    appendHistoricBatches: (mutation, getState) => {
      it('should append to the end of the historic batch list', () => {
        const state = getState()
        state.historicBatches = [batch({ id: 'a' })]
        mutation(state, [batch({ id: 'b' })])
        expect(state.historicBatches.map(b => b.id)).toEqual(['a', 'b'])
      })
    },

    prependHistoricBatches: (mutation, getState) => {
      // Newly finished batches belong at the top of the list, ahead of what is displayed.
      it('should prepend to the front of the historic batch list', () => {
        const state = getState()
        state.historicBatches = [batch({ id: 'a' })]
        mutation(state, [batch({ id: 'z' })])
        expect(state.historicBatches.map(b => b.id)).toEqual(['z', 'a'])
      })
    },

    removeHistoricBatch: (mutation, getState) => {
      it('should remove the historic batch with the given id', () => {
        const state = getState()
        state.historicBatches = [batch({ id: 'a' }), batch({ id: 'b' })]
        mutation(state, 'b')
        expect(state.historicBatches.map(b => b.id)).toEqual(['a'])
      })
    },

    setCleanableBatchReport: (mutation, getState) => {
      it('should replace the cleanable batch report', () => {
        const state = getState()
        mutation(state, [{ batchType: 'x', finishedBatchesCount: 2 }])
        expect(state.cleanableBatchReport).toEqual([{ batchType: 'x', finishedBatchesCount: 2 }])
      })
    },

    setSelectedHistoricBatch: (mutation, getState) => {
      it('should store the selected historic batch', () => {
        const state = getState()
        mutation(state, batch({ id: 'sel' }))
        expect(state.selectedHistoricBatch).toEqual(batch({ id: 'sel' }))
      })
    },

    setHistoricBatchCount: (mutation, getState) => {
      it('should store the historic batch count', () => {
        const state = getState()
        mutation(state, 12)
        expect(state.historicBatchCount).toBe(12)
      })
    },

    setCleanableBatchReportCount: (mutation, getState) => {
      it('should store the cleanable batch report count', () => {
        const state = getState()
        mutation(state, 4)
        expect(state.cleanableBatchReportCount).toBe(4)
      })
    }
  },

  getters: {
    runtimeBatches: (getter, getState) => {
      it('should expose the runtime batch list', () => {
        const state = getState()
        state.runtimeBatches = [batch()]
        expect(getter(state)).toEqual([batch()])
      })
    },
    historicBatches: (getter, getState) => {
      it('should expose the historic batch list', () => {
        const state = getState()
        state.historicBatches = [batch()]
        expect(getter(state)).toEqual([batch()])
      })
    },
    cleanableBatchReport: (getter, getState) => {
      it('should expose the cleanable batch report', () => {
        const state = getState()
        state.cleanableBatchReport = [{ batchType: 'x' }]
        expect(getter(state)).toEqual([{ batchType: 'x' }])
      })
    },
    selectedHistoricBatch: (getter, getState) => {
      it('should expose the selected historic batch', () => {
        const state = getState()
        state.selectedHistoricBatch = batch()
        expect(getter(state)).toEqual(batch())
      })
    },
    historicBatchCount: (getter, getState) => {
      it('should expose the historic batch count', () => {
        const state = getState()
        state.historicBatchCount = 7
        expect(getter(state)).toBe(7)
      })
    },
    cleanableBatchReportCount: (getter, getState) => {
      it('should expose the cleanable batch report count', () => {
        const state = getState()
        state.cleanableBatchReportCount = 9
        expect(getter(state)).toBe(9)
      })
    }
  },

  actions: {
    getRuntimeBatches: (action, getContext) => {
      it('should fetch, commit and return the runtime batches', async () => {
        const context = getContext()
        BatchService.getBatches.mockResolvedValue([batch()])

        const result = await action(context)

        expect(BatchService.getBatches).toHaveBeenCalledWith({})
        expect(context.commit).toHaveBeenCalledWith('setRuntimeBatches', [batch()])
        expect(result).toEqual([batch()])
      })
    },

    deleteRuntimeBatch: (action, getContext) => {
      it('should delete then drop the batch from state', async () => {
        const context = getContext()
        BatchService.deleteBatch.mockResolvedValue(undefined)

        await action(context, 'b-1')

        expect(BatchService.deleteBatch).toHaveBeenCalledWith('b-1')
        expect(context.commit).toHaveBeenCalledWith('removeRuntimeBatch', 'b-1')
      })

      it('should not touch state when the deletion fails', async () => {
        const context = getContext()
        BatchService.deleteBatch.mockRejectedValue(new Error('Conflict'))

        await expect(action(context, 'b-1')).rejects.toThrow('Conflict')
        expect(context.commit).not.toHaveBeenCalled()
      })
    },

    getHistoricBatches: (action, getContext) => {
      // A batch without an endTime is still running, so it must not show as historic.
      it('should drop batches that have not finished', async () => {
        BatchService.getHistoricBatches.mockResolvedValue([
          batch({ id: 'done', endTime: '2026-01-01T00:00:00Z' }),
          batch({ id: 'running', endTime: null }),
          batch({ id: 'also-running', endTime: undefined })
        ])

        const result = await action(getContext(), { maxResults: 10 })

        expect(BatchService.getHistoricBatches).toHaveBeenCalledWith({ maxResults: 10 })
        expect(result.map(b => b.id)).toEqual(['done'])
      })
    },

    loadHistoricBatches: (action, getContext) => {
      it('should replace the list by default', async () => {
        const context = getContext()
        context.dispatch.mockResolvedValue([batch({ id: 'done' })])

        const result = await action(context, { query: { maxResults: 5 } })

        expect(context.dispatch).toHaveBeenCalledWith('getHistoricBatches', { maxResults: 5 })
        expect(context.commit).toHaveBeenCalledWith('setHistoricBatches', [batch({ id: 'done' })])
        expect(result).toEqual([batch({ id: 'done' })])
      })

      it('should append when asked to', async () => {
        const context = getContext()
        context.dispatch.mockResolvedValue([batch({ id: 'done' })])

        await action(context, { query: {}, append: true })

        expect(context.commit).toHaveBeenCalledWith('appendHistoricBatches', [batch({ id: 'done' })])
        expect(context.commit).not.toHaveBeenCalledWith('setHistoricBatches', expect.anything())
      })
    },

    prependNewHistoricBatches: (action, getContext) => {
      // Polling re-fetches the newest page, so already-displayed batches come back too.
      it('should prepend only batches not already in state', async () => {
        const context = getContext()
        context.state.historicBatches = [batch({ id: 'known' })]
        context.dispatch.mockResolvedValue([batch({ id: 'known' }), batch({ id: 'fresh' })])

        const result = await action(context, {})

        expect(context.commit).toHaveBeenCalledWith('prependHistoricBatches', [batch({ id: 'fresh' })])
        expect(result).toEqual([batch({ id: 'fresh' })])
      })

      it('should not commit when every batch is already known', async () => {
        const context = getContext()
        context.state.historicBatches = [batch({ id: 'known' })]
        context.dispatch.mockResolvedValue([batch({ id: 'known' })])

        const result = await action(context, {})

        expect(context.commit).not.toHaveBeenCalled()
        expect(result).toEqual([])
      })
    },

    getHistoricBatch: (action, getContext) => {
      it('should fetch, commit and return the selected historic batch', async () => {
        const context = getContext()
        BatchService.getHistoricBatchById.mockResolvedValue(batch({ id: 'b-9' }))

        const result = await action(context, 'b-9')

        expect(BatchService.getHistoricBatchById).toHaveBeenCalledWith('b-9')
        expect(context.commit).toHaveBeenCalledWith('setSelectedHistoricBatch', batch({ id: 'b-9' }))
        expect(result).toEqual(batch({ id: 'b-9' }))
      })
    },

    getHistoricBatchCount: (action, getContext) => {
      it('should unwrap and store the count', async () => {
        const context = getContext()
        BatchService.getHistoricBatchCount.mockResolvedValue({ count: 42 })

        const result = await action(context, { type: 'x' })

        expect(BatchService.getHistoricBatchCount).toHaveBeenCalledWith({ type: 'x' })
        expect(context.commit).toHaveBeenCalledWith('setHistoricBatchCount', 42)
        expect(result).toBe(42)
      })
    },

    getCleanableBatchReport: (action, getContext) => {
      it('should fetch, commit and return the report', async () => {
        const context = getContext()
        BatchService.getCleanableBatchReport.mockResolvedValue([{ batchType: 'x' }])

        const result = await action(context, { maxResults: 10 })

        expect(BatchService.getCleanableBatchReport).toHaveBeenCalledWith({ maxResults: 10 })
        expect(context.commit).toHaveBeenCalledWith('setCleanableBatchReport', [{ batchType: 'x' }])
        expect(result).toEqual([{ batchType: 'x' }])
      })
    },

    getCleanableBatchReportCount: (action, getContext) => {
      it('should unwrap and store the report count', async () => {
        const context = getContext()
        BatchService.getCleanableBatchReportCount.mockResolvedValue({ count: 3 })

        const result = await action(context)

        expect(context.commit).toHaveBeenCalledWith('setCleanableBatchReportCount', 3)
        expect(result).toBe(3)
      })
    },

    deleteHistoricBatch: (action, getContext) => {
      it('should delete then drop the historic batch from state', async () => {
        const context = getContext()
        BatchService.deleteHistoricBatch.mockResolvedValue(undefined)

        await action(context, 'b-1')

        expect(BatchService.deleteHistoricBatch).toHaveBeenCalledWith('b-1')
        expect(context.commit).toHaveBeenCalledWith('removeHistoricBatch', 'b-1')
      })
    }
  },

  additional: (storeModule) => {
    describe('pass-through actions', () => {
      it('getBatchStatistics should delegate to the service', async () => {
        BatchService.getBatchStatistics.mockResolvedValue([{ id: 'b-1', remainingJobs: 2 }])

        const result = await storeModule.actions.getBatchStatistics({ commit: vi.fn() }, { maxResults: 5 })

        expect(BatchService.getBatchStatistics).toHaveBeenCalledWith({ maxResults: 5 })
        expect(result).toEqual([{ id: 'b-1', remainingJobs: 2 }])
      })

      it('setBatchSuspensionState should delegate to the service', async () => {
        BatchService.setBatchSuspensionState.mockResolvedValue(undefined)

        await storeModule.actions.setBatchSuspensionState({ commit: vi.fn() }, { id: 'b-1', params: { suspended: true } })

        expect(BatchService.setBatchSuspensionState).toHaveBeenCalledWith('b-1', { suspended: true })
      })

      it('setHistoricBatchRemovalTime should delegate to the service', async () => {
        BatchService.setHistoricBatchRemovalTime.mockResolvedValue({ id: 'batch-op' })

        const payload = { historicBatchQuery: {}, calculatedRemovalTime: true }
        const result = await storeModule.actions.setHistoricBatchRemovalTime({ commit: vi.fn() }, payload)

        expect(BatchService.setHistoricBatchRemovalTime).toHaveBeenCalledWith(payload)
        expect(result).toEqual({ id: 'batch-op' })
      })
    })
  }
})
