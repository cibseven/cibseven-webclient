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
import { it, expect } from 'vitest'
import DiagramStore from '../../store/DiagramStore.js'
import { createStoreTestSuite } from './store-test-utils.js'

createStoreTestSuite('DiagramStore', DiagramStore, {
  initialState: (getState) => {
    it('should have diagramReady as false initially', () => {
      expect(getState().diagramReady).toBe(false)
    })
  },

  mutations: {
    setDiagramStatus: (mutation, getState) => {
      it('should set diagram status to true', () => {
        const state = getState()
        mutation(state, true)
        expect(state.diagramReady).toBe(true)
      })
      
      it('should set diagram status to false', () => {
        const state = getState()
        state.diagramReady = true // Set to true first
        mutation(state, false)
        expect(state.diagramReady).toBe(false)
      })

      it('should handle boolean conversion', () => {
        const state = getState()
        mutation(state, 1) // truthy value
        expect(state.diagramReady).toBe(1)
        
        mutation(state, 0) // falsy value
        expect(state.diagramReady).toBe(0)
        
        mutation(state, null)
        expect(state.diagramReady).toBeNull()
      })
    }
  },

  getters: {
    isDiagramReady: (getter, getState) => {
      it('should return true when diagram is ready', () => {
        const state = getState()
        state.diagramReady = true
        expect(getter(state)).toBe(true)
      })
      
      it('should return false when diagram is not ready', () => {
        const state = getState()
        state.diagramReady = false
        expect(getter(state)).toBe(false)
      })

      it('should return the exact value from state', () => {
        const state = getState()
        state.diagramReady = 'ready'
        expect(getter(state)).toBe('ready')
        
        state.diagramReady = null
        expect(getter(state)).toBeNull()
        
        state.diagramReady = undefined
        expect(getter(state)).toBeUndefined()
      })
    }
  },

  actions: {
    setDiagramReady: (action, getContext) => {
      it('should commit setDiagramStatus with true', () => {
        const context = getContext()
        action(context, true)
        expect(context.commit).toHaveBeenCalledWith('setDiagramStatus', true)
      })

      it('should commit setDiagramStatus with false', () => {
        const context = getContext()
        action(context, false)
        expect(context.commit).toHaveBeenCalledWith('setDiagramStatus', false)
      })

      it('should commit setDiagramStatus with any value', () => {
        const context = getContext()
        action(context, 'custom-status')
        expect(context.commit).toHaveBeenCalledWith('setDiagramStatus', 'custom-status')
        
        action(context, null)
        expect(context.commit).toHaveBeenCalledWith('setDiagramStatus', null)
        
        action(context, undefined)
        expect(context.commit).toHaveBeenCalledWith('setDiagramStatus', undefined)
      })
    }
  }
})