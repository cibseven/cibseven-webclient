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
import VariableInstanceStore from '../../store/VariableInstanceStore.js'
import { VariableInstanceService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  VariableInstanceService: {
    getVariableInstance: vi.fn()
  }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('VariableInstanceStore', VariableInstanceStore, {
  initialState: (getState) => {
    it('should start with no current variable instance', () => {
      expect(getState().current).toBeNull()
    })
  },

  mutations: {
    setVariableInstance: (mutation, getState) => {
      it('should store the variable instance', () => {
        const state = getState()
        mutation(state, { id: 'v-1', name: 'amount', value: 42 })
        expect(state.current).toEqual({ id: 'v-1', name: 'amount', value: 42 })
      })
    },

    clearVariableInstance: (mutation, getState) => {
      it('should reset the current variable instance to null', () => {
        const state = getState()
        state.current = { id: 'v-1' }
        mutation(state)
        expect(state.current).toBeNull()
      })
    }
  },

  getters: {
    currentVariableInstance: (getter, getState) => {
      it('should expose the current variable instance', () => {
        const state = getState()
        state.current = { id: 'v-1' }
        expect(getter(state)).toEqual({ id: 'v-1' })
      })
    }
  },

  actions: {
    getVariableInstance: (action, getContext) => {
      it('should fetch, commit and return the variable instance', async () => {
        const context = getContext()
        const instance = { id: 'v-1', name: 'amount', value: 42 }
        VariableInstanceService.getVariableInstance.mockResolvedValue(instance)

        const result = await action(context, { id: 'v-1', deserializeValue: true })

        expect(VariableInstanceService.getVariableInstance).toHaveBeenCalledWith('v-1', true)
        expect(context.commit).toHaveBeenCalledWith('setVariableInstance', instance)
        expect(result).toEqual(instance)
      })

      it('should propagate service failures', async () => {
        const context = getContext()
        VariableInstanceService.getVariableInstance.mockRejectedValue(new Error('Service error'))

        await expect(action(context, { id: 'v-1' })).rejects.toThrow('Service error')
      })
    },

    clearVariableInstance: (action, getContext) => {
      it('should delegate to the clear mutation', () => {
        const context = getContext()
        action(context)
        expect(context.commit).toHaveBeenCalledWith('clearVariableInstance')
      })
    }
  }
})
