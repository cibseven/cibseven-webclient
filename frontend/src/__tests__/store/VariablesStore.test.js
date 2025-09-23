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
import VariablesStore from '../../store/VariablesStore.js'
import { ProcessService, HistoryService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  ProcessService: {
    fetchProcessInstanceVariables: vi.fn()
  },
  HistoryService: {
    fetchProcessInstanceVariablesHistory: vi.fn()
  }
}))

createStoreTestSuite('VariablesStore', VariablesStore, {
  initialState: (getState) => {
    it('should have empty variables array', () => {
      expect(getState().variables).toEqual([])
    })
  },

  mutations: {
    setVariables: (mutation, getState) => {
      it('should set variables', () => {
        const state = getState()
        const variables = [
          { id: '1', name: 'var1', value: 'value1', type: 'String' },
          { id: '2', name: 'var2', value: 42, type: 'Integer' }
        ]
        mutation(state, variables)
        expect(state.variables).toEqual(variables)
      })

      it('should replace existing variables', () => {
        const state = getState()
        state.variables = [{ id: 'old', name: 'oldVar' }]
        const newVariables = [{ id: 'new', name: 'newVar' }]
        
        mutation(state, newVariables)
        expect(state.variables).toEqual(newVariables)
      })

      it('should handle empty array', () => {
        const state = getState()
        state.variables = [{ id: '1', name: 'var1' }]
        mutation(state, [])
        expect(state.variables).toEqual([])
      })
    }
  },

  getters: {
    variables: (getter, getState) => {
      it('should return variables array', () => {
        const state = getState()
        const variables = [
          { id: '1', name: 'var1', value: 'test' },
          { id: '2', name: 'var2', value: 123 }
        ]
        state.variables = variables
        expect(getter(state)).toEqual(variables)
      })

      it('should return empty array when no variables', () => {
        const state = getState()
        state.variables = []
        expect(getter(state)).toEqual([])
      })
    }
  },

  actions: {
    loadVariables: (action, getContext) => {
      it('should clear variables when no instanceId provided', async () => {
        const context = getContext()
        const result = await action(context, { instanceId: null })
        
        expect(context.commit).toHaveBeenCalledWith('setVariables', [])
        expect(result).toEqual([])
      })

      it('should clear variables when instanceId is undefined', async () => {
        const context = getContext()
        const result = await action(context, { instanceId: undefined })
        
        expect(context.commit).toHaveBeenCalledWith('setVariables', [])
        expect(result).toEqual([])
      })

      it('should load active process variables', async () => {
        const context = getContext()
        const mockVariables = [
          { id: '1', name: 'var1', value: 'test', type: 'String' },
          { id: '2', name: 'var2', value: '42', type: 'Integer' }
        ]
        ProcessService.fetchProcessInstanceVariables.mockResolvedValue(mockVariables)

        const params = {
          instanceId: 'instance-1',
          isActive: true,
          camundaHistoryLevel: 'full'
        }

        const result = await action(context, params)

        expect(ProcessService.fetchProcessInstanceVariables).toHaveBeenCalledWith(
          'instance-1',
          { deserializeValues: false }
        )
        expect(context.commit).toHaveBeenCalledWith('setVariables', expect.arrayContaining([
          expect.objectContaining({
            id: '1',
            name: 'var1',
            value: 'test',
            modify: false
          })
        ]))
        expect(result).toEqual(expect.any(Array))
      })

      it('should load historical variables when not active and history level is full', async () => {
        const context = getContext()
        const mockVariables = [
          { id: '1', name: 'histVar', value: 'historical', type: 'String' }
        ]
        HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue(mockVariables)

        const params = {
          instanceId: 'instance-1',
          isActive: false,
          camundaHistoryLevel: 'full'
        }

        const result = await action(context, params)

        expect(HistoryService.fetchProcessInstanceVariablesHistory).toHaveBeenCalledWith(
          'instance-1',
          { deserializeValues: false }
        )
        expect(context.commit).toHaveBeenCalledWith('setVariables', expect.any(Array))
        expect(result).toEqual(expect.any(Array))
      })

      it('should return empty array when not active and history level is not full', async () => {
        const context = getContext()
        // Reset mock call counts before this test
        ProcessService.fetchProcessInstanceVariables.mockClear()
        HistoryService.fetchProcessInstanceVariablesHistory.mockClear()
        
        const params = {
          instanceId: 'instance-1',
          isActive: false,
          camundaHistoryLevel: 'activity'
        }

        const result = await action(context, params)

        expect(result).toEqual([])
        expect(ProcessService.fetchProcessInstanceVariables).not.toHaveBeenCalled()
        expect(HistoryService.fetchProcessInstanceVariablesHistory).not.toHaveBeenCalled()
      })

      it('should handle JSON parsing for Object type variables', async () => {
        const context = getContext()
        const mockVariables = [
          { id: '1', name: 'jsonVar', value: '{"key": "value", "number": 42}', type: 'Object' }
        ]
        ProcessService.fetchProcessInstanceVariables.mockResolvedValue(mockVariables)

        const params = {
          instanceId: 'instance-1',
          isActive: true
        }

        await action(context, params)

        expect(context.commit).toHaveBeenCalledWith('setVariables', 
          expect.arrayContaining([
            expect.objectContaining({
              id: '1',
              name: 'jsonVar',
              value: { key: 'value', number: 42 },
              modify: false
            })
          ])
        )
      })

      it('should handle invalid JSON for Object type variables', async () => {
        const context = getContext()
        // Reset mock call counts before this test
        ProcessService.fetchProcessInstanceVariables.mockClear()
        
        const mockVariables = [
          { id: '1', name: 'invalidJson', value: 'invalid json', type: 'Object' }
        ]
        ProcessService.fetchProcessInstanceVariables
          .mockResolvedValueOnce(mockVariables)
          .mockResolvedValueOnce([
            { id: '1', name: 'invalidJson', value: 'serialized value', type: 'Object' }
          ])

        const params = {
          instanceId: 'instance-1',
          isActive: true
        }

        await action(context, params)

        // Should call service twice - once for initial load, once for serialization
        expect(ProcessService.fetchProcessInstanceVariables).toHaveBeenCalledTimes(2)
        expect(context.commit).toHaveBeenCalledWith('setVariables', 
          expect.arrayContaining([
            expect.objectContaining({
              id: '1',
              name: 'invalidJson',
              value: 'serialized value'
            })
          ])
        )
      })

      it('should add scope information when activityInstancesGrouped provided', async () => {
        const context = getContext()
        const mockVariables = [
          { id: '1', name: 'var1', value: 'test', type: 'String', activityInstanceId: 'act1' }
        ]
        const activityInstancesGrouped = {
          'act1': { id: 'act1', name: 'Activity 1' }
        }
        
        ProcessService.fetchProcessInstanceVariables.mockResolvedValue(mockVariables)

        const params = {
          instanceId: 'instance-1',
          isActive: true,
          activityInstancesGrouped
        }

        await action(context, params)

        expect(context.commit).toHaveBeenCalledWith('setVariables', 
          expect.arrayContaining([
            expect.objectContaining({
              id: '1',
              scope: { id: 'act1', name: 'Activity 1' }
            })
          ])
        )
      })

      it('should handle service errors', async () => {
        const context = getContext()
        const error = new Error('Service error')
        ProcessService.fetchProcessInstanceVariables.mockRejectedValue(error)

        const params = {
          instanceId: 'instance-1',
          isActive: true
        }

        await expect(action(context, params)).rejects.toThrow('Service error')
      })
    },

    clearVariables: (action, getContext) => {
      it('should commit setVariables with empty array', () => {
        const context = getContext()
        action(context)
        expect(context.commit).toHaveBeenCalledWith('setVariables', [])
      })
    }
  }
})