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
import { describe, it, expect } from 'vitest'
import TasksStartView from '@/components/start/TasksStartView.vue'
import startTileOptionsMixin from '@/mixins/startTileOptionsMixin.js'

function evaluateTasksStartView(overrides = {}) {
  const vm = {
    permissionsTaskList: false,
    $t: (key) => key,
    $store: { state: { process: { list: [] } } },
    $options: { components: {} },
    ...overrides
  }
  vm.getPluginOptions = startTileOptionsMixin.methods.getPluginOptions.bind(vm)
  vm.mergeOptions = startTileOptionsMixin.methods.mergeOptions.bind(vm)
  Object.defineProperty(vm, 'startableProcesses', {
    get: () => TasksStartView.computed.startableProcesses.call(vm)
  })
  Object.defineProperty(vm, 'builtInItems', {
    get: () => TasksStartView.computed.builtInItems.call(vm)
  })
  Object.defineProperty(vm, 'items', {
    get: () => TasksStartView.computed.items.call(vm)
  })
  return vm
}

describe('TasksStartView.vue', () => {
  it('shows no items without the tasklist permission', () => {
    const vm = evaluateTasksStartView()
    expect(vm.items).toEqual([])
  })

  it('shows only Task list when start-process is unavailable', () => {
    const vm = evaluateTasksStartView({ permissionsTaskList: true })
    expect(vm.items.map(i => i.title)).toEqual(['start.taskList.title'])
  })

  it('shows Task list and Start process when a process is startable', () => {
    const vm = evaluateTasksStartView({
      permissionsTaskList: true,
      $store: { state: { process: { list: [{ revoked: false, startableInTasklist: true }] } } }
    })
    expect(vm.items.map(i => i.title)).toEqual(['start.taskList.title', 'start.startProcess.title'])
  })

  it('merges in options from a registered TasksTileOptionsPlugin', () => {
    const plugin = {
      methods: {
        getOptions() {
          return [{ to: { name: 'extra' }, title: 'Extra', src: 'extra.svg' }]
        }
      }
    }
    const vm = evaluateTasksStartView({
      permissionsTaskList: true,
      $options: { components: { TasksTileOptionsPlugin: plugin } }
    })
    expect(vm.items.map(i => i.title)).toEqual(['start.taskList.title', 'Extra'])
  })

  it('drops plugin options missing a required field', () => {
    const plugin = {
      methods: {
        getOptions() {
          return [{ to: { name: 'extra' }, title: 'Extra' }] // no src
        }
      }
    }
    const vm = evaluateTasksStartView({
      $options: { components: { TasksTileOptionsPlugin: plugin } }
    })
    expect(vm.items).toEqual([])
  })
})
