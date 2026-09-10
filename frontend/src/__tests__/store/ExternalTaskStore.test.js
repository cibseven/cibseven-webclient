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
import ExternalTaskStore from '../../store/ExternalTaskStore.js'
import { ExternalTaskService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  ExternalTaskService: {
    fetchExternalTasks: vi.fn()
  }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('ExternalTaskStore', ExternalTaskStore, {
  initialState: (getState) => {
    it('should start with no external tasks', () => {
      expect(getState().externalTasks).toEqual([])
    })
  },

  mutations: {
    setExternalTasks: (mutation, getState) => {
      it('should replace the external task list', () => {
        const state = getState()
        state.externalTasks = [{ id: 'old' }]

        mutation(state, [{ id: 'new', topicName: 'invoice' }])

        expect(state.externalTasks).toEqual([{ id: 'new', topicName: 'invoice' }])
      })
    }
  },

  getters: {
    externalTasks: (getter, getState) => {
      it('should expose the external task list', () => {
        const state = getState()
        state.externalTasks = [{ id: 'et-1' }]
        expect(getter(state)).toEqual([{ id: 'et-1' }])
      })
    }
  },

  actions: {
    loadExternalTasks: (action, getContext) => {
      it('should fetch and store external tasks', async () => {
        const context = getContext()
        ExternalTaskService.fetchExternalTasks.mockResolvedValue([{ id: 'et-1' }])

        await action(context, { processInstanceId: 'pi-1' })

        expect(ExternalTaskService.fetchExternalTasks).toHaveBeenCalledWith({ processInstanceId: 'pi-1' })
        expect(context.commit).toHaveBeenCalledWith('setExternalTasks', [{ id: 'et-1' }])
      })

      it('should propagate service failures', async () => {
        const context = getContext()
        ExternalTaskService.fetchExternalTasks.mockRejectedValue(new Error('Service error'))

        await expect(action(context, {})).rejects.toThrow('Service error')
        expect(context.commit).not.toHaveBeenCalled()
      })
    }
  }
})
