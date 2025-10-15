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
import TaskStore from '@/store/TaskStore'

describe('TaskStore', () => {
  let state

  beforeEach(() => {
    state = {
      selectedAssignee: { taskId: null, assignee: null }
    }
  })

  describe('initial state', () => {
    it('should have correct initial state structure', () => {
      expect(TaskStore.state).toEqual({
        selectedAssignee: { taskId: null, assignee: null }
      })
    })

    it('should be namespaced', () => {
      expect(TaskStore.namespaced).toBe(true)
    })
  })

  describe('mutations', () => {
    describe('setSelectedAssignee', () => {
      it('should set taskId and assignee', () => {
        const payload = { taskId: 'task-123', assignee: 'john.doe' }
        
        TaskStore.mutations.setSelectedAssignee(state, payload)
        
        expect(state.selectedAssignee).toEqual({
          taskId: 'task-123',
          assignee: 'john.doe'
        })
      })

      it('should handle null assignee', () => {
        const payload = { taskId: 'task-456', assignee: null }
        
        TaskStore.mutations.setSelectedAssignee(state, payload)
        
        expect(state.selectedAssignee).toEqual({
          taskId: 'task-456',
          assignee: null
        })
      })

      it('should overwrite existing selectedAssignee', () => {
        state.selectedAssignee = { taskId: 'old-task', assignee: 'old.user' }
        const payload = { taskId: 'new-task', assignee: 'new.user' }
        
        TaskStore.mutations.setSelectedAssignee(state, payload)
        
        expect(state.selectedAssignee).toEqual({
          taskId: 'new-task',
          assignee: 'new.user'
        })
      })
    })

    describe('clearSelectedAssignee', () => {
      it('should reset selectedAssignee to initial state', () => {
        state.selectedAssignee = { taskId: 'task-123', assignee: 'john.doe' }
        
        TaskStore.mutations.clearSelectedAssignee(state)
        
        expect(state.selectedAssignee).toEqual({
          taskId: null,
          assignee: null
        })
      })

      it('should work when selectedAssignee is already cleared', () => {
        TaskStore.mutations.clearSelectedAssignee(state)
        
        expect(state.selectedAssignee).toEqual({
          taskId: null,
          assignee: null
        })
      })
    })
  })

  describe('getters', () => {
    describe('getSelectedAssignee', () => {
      it('should return the selectedAssignee object', () => {
        state.selectedAssignee = { taskId: 'task-123', assignee: 'john.doe' }
        
        const result = TaskStore.getters.getSelectedAssignee(state)
        
        expect(result).toEqual({ taskId: 'task-123', assignee: 'john.doe' })
      })

      it('should return initial state when no assignee is selected', () => {
        const result = TaskStore.getters.getSelectedAssignee(state)
        
        expect(result).toEqual({ taskId: null, assignee: null })
      })
    })

    describe('getAssigneeByTaskId', () => {
      it('should return assignee when taskId matches', () => {
        state.selectedAssignee = { taskId: 'task-123', assignee: 'john.doe' }
        
        const getter = TaskStore.getters.getAssigneeByTaskId(state)
        const result = getter('task-123')
        
        expect(result).toBe('john.doe')
      })

      it('should return null when taskId does not match', () => {
        state.selectedAssignee = { taskId: 'task-123', assignee: 'john.doe' }
        
        const getter = TaskStore.getters.getAssigneeByTaskId(state)
        const result = getter('task-456')
        
        expect(result).toBeNull()
      })

      it('should return null when selectedAssignee taskId is null', () => {
        state.selectedAssignee = { taskId: null, assignee: 'john.doe' }
        
        const getter = TaskStore.getters.getAssigneeByTaskId(state)
        const result = getter('task-123')
        
        expect(result).toBeNull()
      })

      it('should return null when selectedAssignee is null', () => {
        state.selectedAssignee = null
        
        const getter = TaskStore.getters.getAssigneeByTaskId(state)
        const result = getter('task-123')
        
        expect(result).toBeNull()
      })

      it('should return assignee even when assignee is null but taskId matches', () => {
        state.selectedAssignee = { taskId: 'task-123', assignee: null }
        
        const getter = TaskStore.getters.getAssigneeByTaskId(state)
        const result = getter('task-123')
        
        expect(result).toBeNull()
      })
    })
  })

  describe('actions', () => {
    let commit

    beforeEach(() => {
      commit = vi.fn()
    })

    describe('setSelectedAssignee', () => {
      it('should commit setSelectedAssignee when taskId is provided', () => {
        const payload = { taskId: 'task-123', assignee: 'john.doe' }
        
        TaskStore.actions.setSelectedAssignee({ commit }, payload)
        
        expect(commit).toHaveBeenCalledWith('setSelectedAssignee', payload)
        expect(commit).toHaveBeenCalledTimes(1)
      })

      it('should commit clearSelectedAssignee when taskId is null', () => {
        const payload = { taskId: null, assignee: 'john.doe' }
        
        TaskStore.actions.setSelectedAssignee({ commit }, payload)
        
        expect(commit).toHaveBeenCalledWith('clearSelectedAssignee')
        expect(commit).toHaveBeenCalledTimes(1)
      })

      it('should commit clearSelectedAssignee when taskId is undefined', () => {
        const payload = { taskId: undefined, assignee: 'john.doe' }
        
        TaskStore.actions.setSelectedAssignee({ commit }, payload)
        
        expect(commit).toHaveBeenCalledWith('clearSelectedAssignee')
        expect(commit).toHaveBeenCalledTimes(1)
      })

      it('should commit clearSelectedAssignee when taskId is empty string', () => {
        const payload = { taskId: '', assignee: 'john.doe' }
        
        TaskStore.actions.setSelectedAssignee({ commit }, payload)
        
        expect(commit).toHaveBeenCalledWith('clearSelectedAssignee')
        expect(commit).toHaveBeenCalledTimes(1)
      })

      it('should commit setSelectedAssignee when taskId is valid string', () => {
        const payload = { taskId: 'valid-task-id', assignee: 'jane.doe' }
        
        TaskStore.actions.setSelectedAssignee({ commit }, payload)
        
        expect(commit).toHaveBeenCalledWith('setSelectedAssignee', payload)
        expect(commit).toHaveBeenCalledTimes(1)
      })
    })

    describe('clearSelectedAssignee', () => {
      it('should commit clearSelectedAssignee mutation', () => {
        TaskStore.actions.clearSelectedAssignee({ commit })
        
        expect(commit).toHaveBeenCalledWith('clearSelectedAssignee')
        expect(commit).toHaveBeenCalledTimes(1)
      })
    })
  })
})
