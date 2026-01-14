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
import { describe, it, expect, beforeEach, vi } from 'vitest'

/**
 * Common test utilities for Vuex stores
 */
export const createStoreTestSuite = (storeName, storeModule, customTests = {}) => {
  return describe(`${storeName}`, () => {
    let state
    let mockCommit
    let mockDispatch
    let mockGetters
    let mockRootGetters

    beforeEach(() => {
      // Reset state to initial values
      if (typeof storeModule.state === 'function') {
        state = storeModule.state()
      } else {
        state = structuredClone(storeModule.state)
      }
      
      mockCommit = vi.fn()
      mockDispatch = vi.fn()
      mockGetters = {}
      mockRootGetters = {}
    })

    // Test namespacing
    if (storeModule.namespaced !== undefined) {
      describe('module configuration', () => {
        it(`should ${storeModule.namespaced ? 'be' : 'not be'} namespaced`, () => {
          expect(storeModule.namespaced).toBe(storeModule.namespaced)
        })
      })
    }

    // Test initial state
    describe('initial state', () => {
      it('should have correct initial state structure', () => {
        let initialState
        if (typeof storeModule.state === 'function') {
          initialState = storeModule.state()
        } else {
          initialState = storeModule.state
        }
        expect(initialState).toBeDefined()
        expect(typeof initialState).toBe('object')
      })

      // Custom state tests
      if (customTests.initialState) {
        customTests.initialState(() => state)
      }
    })

    // Test mutations
    if (storeModule.mutations) {
      describe('mutations', () => {
        Object.keys(storeModule.mutations).forEach(mutationName => {
          const mutation = storeModule.mutations[mutationName]

          it(`should have ${mutationName} mutation`, () => {
            expect(typeof mutation).toBe('function')
          })

          // Custom mutation tests
          if (customTests.mutations && customTests.mutations[mutationName]) {
            customTests.mutations[mutationName](mutation, () => state, mutationName)
          }
        })
      })
    }

    // Test getters
    if (storeModule.getters) {
      describe('getters', () => {
        Object.keys(storeModule.getters).forEach(getterName => {
          const getter = storeModule.getters[getterName]

          it(`should have ${getterName} getter`, () => {
            expect(typeof getter).toBe('function')
          })

          // Test getter returns value
          it(`${getterName} should return a value`, () => {
            const result = getter(state, mockGetters, {}, mockRootGetters)
            expect(result).toBeDefined()
          })

          // Custom getter tests
          if (customTests.getters && customTests.getters[getterName]) {
            customTests.getters[getterName](getter, () => state, getterName)
          }
        })
      })
    }

    // Test actions
    if (storeModule.actions) {
      describe('actions', () => {
        Object.keys(storeModule.actions).forEach(actionName => {
          const action = storeModule.actions[actionName]

          it(`should have ${actionName} action`, () => {
            expect(typeof action).toBe('function')
          })

          // Custom action tests
          if (customTests.actions && customTests.actions[actionName]) {
            const getContext = () => ({ commit: mockCommit, dispatch: mockDispatch, state, getters: mockGetters })
            customTests.actions[actionName](action, getContext, actionName)
          }
        })
      })
    }

    // Additional custom tests
    if (customTests.additional) {
      customTests.additional(storeModule, () => state, () => ({ mockCommit, mockDispatch, mockGetters, mockRootGetters }))
    }
  })
}

/**
 * Test helpers for common store patterns
 */
export const testHelpers = {
  // Test array state mutations
  testArrayMutation: (mutation, state, arrayPath, testData) => {
    const initialLength = getNestedValue(state, arrayPath).length
    mutation(state, testData)
    expect(getNestedValue(state, arrayPath)).toHaveLength(initialLength + (Array.isArray(testData) ? testData.length : 1))
  },

  // Test object state mutations
  testObjectMutation: (mutation, state, objectPath, testData) => {
    mutation(state, testData)
    expect(getNestedValue(state, objectPath)).toEqual(expect.objectContaining(testData))
  },

  // Test clear/reset mutations
  testClearMutation: (mutation, state, path, initialValue) => {
    mutation(state)
    expect(getNestedValue(state, path)).toEqual(initialValue)
  }
}

// Helper to get nested object values
const getNestedValue = (obj, path) => {
  return path.split('.').reduce((current, key) => current && current[key], obj)
}