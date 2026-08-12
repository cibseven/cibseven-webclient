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
import StartView from '@/components/start/StartView.vue'
import startTileOptionsMixin from '@/mixins/startTileOptionsMixin.js'
import { SURFACES } from '@/navigation/navGroups.js'

vi.mock('@cib/common-frontend', () => ({
  ErrorDialog: { name: 'ErrorDialog', render() { return null } }
}))

function evaluateStartView(overrides = {}) {
  const vm = {
    permissionsTaskList: true,
    permissionsCockpit: true,
    permissionsModeler: true,
    permissionsUsers: true,
    permissionsUsersManagement: true,
    permissionsGroupsManagement: true,
    permissionsTenantsManagement: true,
    permissionsAuthorizationsManagement: true,
    permissionsSystemManagement: true,
    $t: (key) => key,
    $store: { state: { process: { list: [{ revoked: false, startableInTasklist: true }] } } },
    $root: { config: { productNamePageTitle: 'Test', permissions: {} } },
    $options: { components: {} },
    ...overrides
  }
  vm.getPluginOptions = startTileOptionsMixin.methods.getPluginOptions.bind(vm)
  vm.mergeOptions = startTileOptionsMixin.methods.mergeOptions.bind(vm)
  Object.defineProperty(vm, 'startableProcesses', {
    get: () => StartView.computed.startableProcesses.call(vm)
  })
  Object.defineProperty(vm, 'navGroups', {
    get: () => StartView.computed.navGroups.call(vm)
  })
  Object.defineProperty(vm, 'groupById', {
    get: () => StartView.computed.groupById.call(vm)
  })
  Object.defineProperty(vm, 'builtInTasksOptions', {
    get: () => StartView.computed.builtInTasksOptions.call(vm)
  })
  Object.defineProperty(vm, 'builtInBuilderOptions', {
    get: () => StartView.computed.builtInBuilderOptions.call(vm)
  })
  Object.defineProperty(vm, 'tasksOptions', {
    get: () => StartView.computed.tasksOptions.call(vm)
  })
  Object.defineProperty(vm, 'builderOptions', {
    get: () => StartView.computed.builderOptions.call(vm)
  })
  Object.defineProperty(vm, 'dataOptions', {
    get: () => StartView.computed.dataOptions.call(vm)
  })
  Object.defineProperty(vm, 'cockpitOptions', {
    get: () => StartView.computed.cockpitOptions.call(vm)
  })
  Object.defineProperty(vm, 'adminOptions', {
    get: () => StartView.computed.adminOptions.call(vm)
  })
  Object.defineProperty(vm, 'tiles', {
    get: () => StartView.computed.tiles.call(vm)
  })
  return vm
}

describe('StartView.vue nav tiles', () => {
  it('exposes CE tile ids without data when catalog data group is empty', () => {
    const vm = evaluateStartView()
    expect(vm.tiles).toEqual(['tasks', 'cockpit', 'builder', 'admin'])
  })

  it('builds tasks hover options from shared nav (tasklist then start process)', () => {
    const vm = evaluateStartView()
    expect(vm.tasksOptions.map(o => o.to)).toEqual(['/seven/auth/tasks', '/seven/auth/start-process'])
  })

  it('omits cockpit dashboard from hover options and collapses admin system entries', () => {
    const vm = evaluateStartView()
    expect(vm.cockpitOptions.some(o => o.to === '/seven/auth/processes')).toBe(false)
    expect(vm.cockpitOptions.some(o => o.to === '/seven/auth/processes/list')).toBe(true)
    expect(vm.adminOptions.some(o => o.to === '/seven/auth/admin/system')).toBe(true)
    expect(vm.adminOptions.some(o => o.to === '/seven/auth/admin/system/system-diagnostics')).toBe(false)
    expect(vm.adminOptions.some(o => o.to === '/seven/auth/admin/system/execution-metrics')).toBe(false)
  })

  it('keeps cockpit tile hover CE-shaped when NavGroupsExtender adds toolbar-only EE items', () => {
    const extender = {
      methods: {
        extend(groups) {
          const clone = groups.map(g => ({ ...g, items: g.items ? [...g.items] : g.items }))
          const cockpit = clone.find(g => g.id === 'cockpit')
          cockpit.items.push({
            surfaces: [SURFACES.TOOLBAR],
            to: '/seven/auth/operation-log',
            title: 'operation-log.title',
            tooltip: 'operation-log.description'
          })
          return clone
        }
      }
    }
    const vm = evaluateStartView({
      $options: { components: { NavGroupsExtender: extender } }
    })
    expect(vm.cockpitOptions.some(o => o.to === '/seven/auth/operation-log')).toBe(false)
    expect(vm.cockpitOptions.map(o => o.to)).toEqual([
      '/seven/auth/processes/list',
      '/seven/auth/decisions/list',
      '/seven/auth/human-tasks',
      '/seven/auth/deployments',
      '/seven/auth/batches'
    ])
  })

  it('shows data tile from extender-filled catalog (not DataTileOptionsPlugin)', () => {
    const extender = {
      methods: {
        extend(groups) {
          const clone = groups.map(g => ({ ...g, items: g.items ? [...g.items] : g.items }))
          const data = clone.find(g => g.id === 'data')
          data.show = true
          data.items = [{
            to: '/seven/auth/ins7ght',
            title: 'start.ins7ght.title',
            tooltip: 'start.ins7ght.tooltip',
            icon: 'mdi-chart-box-outline'
          }]
          return clone
        }
      }
    }
    const vm = evaluateStartView({
      $options: { components: { NavGroupsExtender: extender } }
    })
    expect(vm.dataOptions.map(o => o.to)).toEqual(['/seven/auth/ins7ght'])
    expect(vm.tiles).toContain('data')
  })

  it('ignores DataTileOptionsPlugin for start-tile data options', () => {
    const plugin = {
      methods: {
        getOptions() {
          return [{ to: '/seven/auth/ins7ght', title: 'Ins7ght', icon: 'mdi-chart-box-outline', tooltip: 'Ins7ght' }]
        }
      }
    }
    const vm = evaluateStartView({
      $options: { components: { DataTileOptionsPlugin: plugin } }
    })
    expect(vm.dataOptions).toEqual([])
    expect(vm.tiles).not.toContain('data')
  })
})
