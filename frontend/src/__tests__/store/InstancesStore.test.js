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
import InstancesStore from '../../store/InstancesStore.js'
import { HistoryService, ProcessService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  HistoryService: {
    findProcessesInstancesHistoryById: vi.fn()
  },
  ProcessService: {
    findProcessById: vi.fn(),
    findCurrentProcessesInstances: vi.fn()
  }
}))

const baseParams = {
  processId: 'pd-1',
  filter: null,
  camundaHistoryLevel: 'full',
  firstResult: 0,
  maxResults: 50
}

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('InstancesStore', InstancesStore, {
  initialState: (getState) => {
    it('should start with no instances', () => {
      expect(getState().instances).toEqual([])
    })
  },

  mutations: {
    setInstances: (mutation, getState) => {
      it('should replace the instance list', () => {
        const state = getState()
        state.instances = [{ id: 'old' }]

        mutation(state, [{ id: 'new' }])

        expect(state.instances).toEqual([{ id: 'new' }])
      })
    },

    appendInstances: (mutation, getState) => {
      it('should append to the instance list', () => {
        const state = getState()
        state.instances = [{ id: 'a' }]

        mutation(state, [{ id: 'b' }, { id: 'c' }])

        expect(state.instances.map(i => i.id)).toEqual(['a', 'b', 'c'])
      })
    },

    removeInstance: (mutation, getState) => {
      it('should remove the instance with the given id', () => {
        const state = getState()
        state.instances = [{ id: 'a' }, { id: 'b' }]

        mutation(state, 'a')

        expect(state.instances.map(i => i.id)).toEqual(['b'])
      })

      it('should leave the list untouched for an unknown id', () => {
        const state = getState()
        state.instances = [{ id: 'a' }]

        mutation(state, 'missing')

        expect(state.instances.map(i => i.id)).toEqual(['a'])
      })
    },

    updateInstanceState: (mutation, getState) => {
      it('should update the state of the matching instance', () => {
        const state = getState()
        state.instances = [{ id: 'a', state: 'ACTIVE' }, { id: 'b', state: 'ACTIVE' }]

        mutation(state, { instanceId: 'b', newState: 'SUSPENDED' })

        expect(state.instances.find(i => i.id === 'b').state).toBe('SUSPENDED')
        expect(state.instances.find(i => i.id === 'a').state).toBe('ACTIVE')
      })

      it('should be a no-op when the instance is not present', () => {
        const state = getState()
        state.instances = [{ id: 'a', state: 'ACTIVE' }]

        expect(() => mutation(state, { instanceId: 'missing', newState: 'SUSPENDED' })).not.toThrow()
        expect(state.instances[0].state).toBe('ACTIVE')
      })
    }
  },

  getters: {
    instances: (getter, getState) => {
      it('should expose the instance list', () => {
        const state = getState()
        state.instances = [{ id: 'pi-1' }]
        expect(getter(state)).toEqual([{ id: 'pi-1' }])
      })
    }
  },

  actions: {
    loadInstances: (action, getContext) => {
      it('should load from history and replace the list when history is enabled', async () => {
        const context = getContext()
        HistoryService.findProcessesInstancesHistoryById.mockResolvedValue([{ id: 'pi-1' }])

        const result = await action(context, { ...baseParams, sortingCriteria: [{ sortBy: 'startTime' }], fetchIncidents: true })

        expect(HistoryService.findProcessesInstancesHistoryById).toHaveBeenCalledWith(
          'pd-1', 0, 50, null, null, [{ sortBy: 'startTime' }], true
        )
        expect(context.commit).toHaveBeenCalledWith('setInstances', [{ id: 'pi-1' }])
        expect(result).toEqual([{ id: 'pi-1' }])
      })

      // "Show more" paginates, so results must be appended rather than replacing the page.
      it('should append instead of replacing when showMore is set', async () => {
        const context = getContext()
        HistoryService.findProcessesInstancesHistoryById.mockResolvedValue([{ id: 'pi-2' }])

        await action(context, { ...baseParams, showMore: true })

        expect(context.commit).toHaveBeenCalledWith('appendInstances', [{ id: 'pi-2' }])
        expect(context.commit).not.toHaveBeenCalledWith('setInstances', expect.anything())
      })

      it('should default sortingCriteria and fetchIncidents when not supplied', async () => {
        const context = getContext()
        HistoryService.findProcessesInstancesHistoryById.mockResolvedValue([])

        await action(context, baseParams)

        expect(HistoryService.findProcessesInstancesHistoryById).toHaveBeenCalledWith(
          'pd-1', 0, 50, null, null, [], false
        )
      })

      // With history disabled the runtime API is used, which returns neither the process
      // definition fields nor an `incidents` array nor a derived `state`, so the store
      // has to synthesise all of them.
      it('should decorate runtime instances when history is disabled', async () => {
        const context = getContext()
        ProcessService.findProcessById.mockResolvedValue({ id: 'pd-1', version: 3, key: 'invoice' })
        ProcessService.findCurrentProcessesInstances.mockResolvedValue([
          { id: 'a', suspended: true, ended: false },
          { id: 'b', suspended: false, ended: true },
          { id: 'c', suspended: false, ended: false }
        ])

        const result = await action(context, {
          ...baseParams, camundaHistoryLevel: 'none', tenantId: 't-1'
        })

        expect(ProcessService.findProcessById).toHaveBeenCalledWith('pd-1', true)
        expect(ProcessService.findCurrentProcessesInstances).toHaveBeenCalledWith(
          { processDefinitionId: 'pd-1', tenantId: 't-1' }, 0, 50
        )
        expect(result.map(i => i.state)).toEqual(['SUSPENDED', 'COMPLETED', 'ACTIVE'])
        expect(result.every(i => i.processDefinitionKey === 'invoice' && i.processDefinitionVersion === 3)).toBe(true)
        expect(result.every(i => Array.isArray(i.incidents) && i.incidents.length === 0)).toBe(true)
        expect(context.commit).toHaveBeenCalledWith('appendInstances', result)
      })

      it('should forward activityIdIn to the runtime query when the filter carries it', async () => {
        const context = getContext()
        ProcessService.findProcessById.mockResolvedValue({ id: 'pd-1', version: 1, key: 'invoice' })
        ProcessService.findCurrentProcessesInstances.mockResolvedValue([])

        await action(context, {
          ...baseParams, camundaHistoryLevel: 'none', filter: { activityIdIn: ['task_1'] }
        })

        expect(ProcessService.findCurrentProcessesInstances).toHaveBeenCalledWith(
          { processDefinitionId: 'pd-1', tenantId: undefined, activityIdIn: ['task_1'] }, 0, 50
        )
      })

      it('should propagate history service failures', async () => {
        const context = getContext()
        HistoryService.findProcessesInstancesHistoryById.mockRejectedValue(new Error('Service error'))

        await expect(action(context, baseParams)).rejects.toThrow('Service error')
      })
    },

    resetInstances: (action, getContext) => {
      it('should clear the instance list', () => {
        const context = getContext()
        action(context)
        expect(context.commit).toHaveBeenCalledWith('setInstances', [])
      })
    }
  }
})
