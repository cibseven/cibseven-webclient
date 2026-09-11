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
import HistoricVariableInstanceStore from '../../store/HistoricVariableInstanceStore.js'
import { HistoricVariableInstanceService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  HistoricVariableInstanceService: {
    getHistoricVariableInstance: vi.fn()
  }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('HistoricVariableInstanceStore', HistoricVariableInstanceStore, {
  initialState: (getState) => {
    it('should start with no current historic variable instance', () => {
      expect(getState().current).toBeNull()
    })
  },

  mutations: {
    setHistoricVariableInstance: (mutation, getState) => {
      it('should store the historic variable instance', () => {
        const state = getState()
        mutation(state, { id: 'hv-1', name: 'amount' })
        expect(state.current).toEqual({ id: 'hv-1', name: 'amount' })
      })
    },

    clearHistoricVariableInstance: (mutation, getState) => {
      it('should reset the current historic variable instance to null', () => {
        const state = getState()
        state.current = { id: 'hv-1' }
        mutation(state)
        expect(state.current).toBeNull()
      })
    }
  },

  getters: {
    currentHistoricVariableInstance: (getter, getState) => {
      it('should expose the current historic variable instance', () => {
        const state = getState()
        state.current = { id: 'hv-1' }
        expect(getter(state)).toEqual({ id: 'hv-1' })
      })
    }
  },

  actions: {
    getHistoricVariableInstance: (action, getContext) => {
      it('should fetch, commit and return the historic variable instance', async () => {
        const context = getContext()
        const instance = { id: 'hv-1', name: 'amount', value: 7 }
        HistoricVariableInstanceService.getHistoricVariableInstance.mockResolvedValue(instance)

        const result = await action(context, { id: 'hv-1', deserializeValue: false })

        expect(HistoricVariableInstanceService.getHistoricVariableInstance).toHaveBeenCalledWith('hv-1', false)
        expect(context.commit).toHaveBeenCalledWith('setHistoricVariableInstance', instance)
        expect(result).toEqual(instance)
      })

      it('should propagate service failures', async () => {
        const context = getContext()
        HistoricVariableInstanceService.getHistoricVariableInstance.mockRejectedValue(new Error('Service error'))

        await expect(action(context, { id: 'hv-1' })).rejects.toThrow('Service error')
      })
    },

    clearHistoricVariableInstance: (action, getContext) => {
      it('should delegate to the clear mutation', () => {
        const context = getContext()
        action(context)
        expect(context.commit).toHaveBeenCalledWith('clearHistoricVariableInstance')
      })
    }
  }
})
