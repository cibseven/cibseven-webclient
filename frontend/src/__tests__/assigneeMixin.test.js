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
import { describe, it, expect, vi } from 'vitest'
import assigneeMixin from '@/mixins/assigneeMixin.js'

const { get, set } = assigneeMixin.computed.assignee

/**
 * A `this` for the computed property. `storeAssignee` is what the task store getter
 * answers — `undefined` meaning "the store knows nothing about this task", which is
 * distinct from a stored `null` meaning "explicitly unassigned".
 */
function context({ task, storeAssignee, setSelectedAssignee } = {}) {
  return {
    task,
    setSelectedAssignee,
    $store: { getters: { 'task/getAssigneeByTaskId': () => storeAssignee } }
  }
}

describe('assigneeMixin', () => {
  describe('get', () => {
    it('should prefer the assignee held in the store', () => {
      const vm = context({ task: { id: 't1', assignee: 'from-task' }, storeAssignee: 'from-store' })

      expect(get.call(vm)).toBe('from-store')
    })

    // A pending unassignment is stored as null and must win over the task's stale value,
    // otherwise the UI snaps back to the old assignee.
    it('should honour a stored null over the task assignee', () => {
      const vm = context({ task: { id: 't1', assignee: 'from-task' }, storeAssignee: null })

      expect(get.call(vm)).toBeNull()
    })

    it('should fall back to the task assignee when the store knows nothing', () => {
      const vm = context({ task: { id: 't1', assignee: 'from-task' }, storeAssignee: undefined })

      expect(get.call(vm)).toBe('from-task')
    })

    it('should return null when neither the store nor the task has an assignee', () => {
      const vm = context({ task: { id: 't1', assignee: undefined }, storeAssignee: undefined })

      expect(get.call(vm)).toBeNull()
    })

    it('should return null when the task assignee is explicitly null', () => {
      const vm = context({ task: { id: 't1', assignee: null }, storeAssignee: undefined })

      expect(get.call(vm)).toBeNull()
    })

    it.each([[undefined], [null], [{}], [{ id: undefined }]])('should return null for task %j', (task) => {
      expect(get.call(context({ task, storeAssignee: 'ignored' }))).toBeNull()
    })

    it('should look the assignee up by the task id', () => {
      const getter = vi.fn(() => 'demo')
      const vm = { task: { id: 't1' }, $store: { getters: { 'task/getAssigneeByTaskId': getter } } }

      get.call(vm)

      expect(getter).toHaveBeenCalledWith('t1')
    })
  })

  describe('set', () => {
    it('should forward the new assignee together with the task id', () => {
      const setSelectedAssignee = vi.fn()
      const vm = context({ task: { id: 't1' }, setSelectedAssignee })

      set.call(vm, 'demo')

      expect(setSelectedAssignee).toHaveBeenCalledWith({ taskId: 't1', assignee: 'demo' })
    })

    it('should forward a null assignee when unassigning', () => {
      const setSelectedAssignee = vi.fn()
      const vm = context({ task: { id: 't1' }, setSelectedAssignee })

      set.call(vm, null)

      expect(setSelectedAssignee).toHaveBeenCalledWith({ taskId: 't1', assignee: null })
    })

    it.each([[undefined], [null]])('should do nothing when the task is %j', (task) => {
      const setSelectedAssignee = vi.fn()

      set.call(context({ task, setSelectedAssignee }), 'demo')

      expect(setSelectedAssignee).not.toHaveBeenCalled()
    })

    // Components that only read the assignee do not map the action, so the setter has to
    // tolerate its absence rather than throwing.
    it('should do nothing when the component does not provide the action', () => {
      const vm = context({ task: { id: 't1' }, setSelectedAssignee: undefined })

      expect(() => set.call(vm, 'demo')).not.toThrow()
    })

    it('should do nothing when the action is not a function', () => {
      const vm = context({ task: { id: 't1' }, setSelectedAssignee: 'not-a-function' })

      expect(() => set.call(vm, 'demo')).not.toThrow()
    })
  })
})
