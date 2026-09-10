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
import { it, expect, beforeEach } from 'vitest'
import AdvancedSearchStore from '../../store/AdvancedSearchStore.js'
import { createStoreTestSuite } from './store-test-utils.js'

const STORAGE_KEY = '_advancedSearch'

const criteria = (overrides = {}) => ({
  key: 'processDefinitionKey',
  name: 'Process',
  operator: 'eq',
  value: 'invoice',
  ...overrides
})

beforeEach(() => {
  localStorage.clear()
})

createStoreTestSuite('AdvancedSearchStore', AdvancedSearchStore, {
  initialState: (getState) => {
    it('should match all criteria and hold no criteria', () => {
      expect(getState()).toEqual({ matchAllCriteria: true, criterias: [] })
    })
  },

  mutations: {
    initializeAdvancedSearch: (mutation, getState) => {
      it('should adopt the persisted payload', () => {
        const state = getState()
        mutation(state, { matchAllCriteria: false, criterias: [criteria()] })

        expect(state.matchAllCriteria).toBe(false)
        expect(state.criterias).toEqual([criteria()])
      })

      // Nothing in localStorage yields `null` after JSON.parse, which must reset to the
      // defaults rather than throwing.
      it('should fall back to defaults when the payload is null', () => {
        const state = getState()
        state.matchAllCriteria = false
        state.criterias = [criteria()]

        mutation(state, null)

        expect(state.matchAllCriteria).toBe(true)
        expect(state.criterias).toEqual([])
      })
    },

    setAdvancedSearch: (mutation, getState) => {
      it('should persist non-empty criteria to localStorage', () => {
        const state = getState()
        const params = { matchAllCriteria: false, criterias: [criteria()] }

        mutation(state, params)

        expect(state.matchAllCriteria).toBe(false)
        expect(JSON.parse(localStorage.getItem(STORAGE_KEY))).toEqual(params)
      })

      it('should drop the persisted entry when criteria are cleared', () => {
        const state = getState()
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ matchAllCriteria: true, criterias: [criteria()] }))

        mutation(state, { matchAllCriteria: true, criterias: [] })

        expect(state.criterias).toEqual([])
        expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
      })
    }
  },

  getters: {
    formatedCriteriaData: (getter, getState) => {
      it('should group criteria by key, dropping the key from each entry', () => {
        const state = getState()
        state.criterias = [
          criteria({ key: 'a', name: 'A1', value: '1' }),
          criteria({ key: 'a', name: 'A2', value: '2' }),
          criteria({ key: 'b', name: 'B1', value: '3' })
        ]

        expect(getter(state)).toEqual({
          a: [
            { name: 'A1', operator: 'eq', value: '1' },
            { name: 'A2', operator: 'eq', value: '2' }
          ],
          b: [{ name: 'B1', operator: 'eq', value: '3' }]
        })
      })

      it('should return an empty object when there are no criteria', () => {
        expect(getter(getState())).toEqual({})
      })
    }
  },

  actions: {
    updateAdvancedSearch: (action, getContext) => {
      it('should delegate to the setAdvancedSearch mutation', () => {
        const context = getContext()
        const params = { matchAllCriteria: false, criterias: [criteria()] }

        action(context, params)

        expect(context.commit).toHaveBeenCalledWith('setAdvancedSearch', params)
      })
    },

    loadAdvancedSearchData: (action, getContext) => {
      it('should initialize from the persisted payload', () => {
        const context = getContext()
        const stored = { matchAllCriteria: false, criterias: [criteria()] }
        localStorage.setItem(STORAGE_KEY, JSON.stringify(stored))

        action(context)

        expect(context.commit).toHaveBeenCalledWith('initializeAdvancedSearch', stored)
      })

      it('should initialize with null when nothing is persisted', () => {
        const context = getContext()

        action(context)

        expect(context.commit).toHaveBeenCalledWith('initializeAdvancedSearch', null)
      })
    }
  }
})
