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
import { describe, it, expect, vi, afterEach } from 'vitest'
import StartView from '@/components/start/StartView.vue'
import navigationPermissionsMixin from '@/mixins/navigationPermissionsMixin.js'
import { SURFACES, registerNavGroupsExtender } from '@/navigation/navGroups.js'

const unregisterExtenders = []
const useExtender = extend => unregisterExtenders.push(registerNavGroupsExtender(extend))
afterEach(() => unregisterExtenders.splice(0).forEach(unregister => unregister()))

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
    $route: { query: {} },
    $options: { components: {} },
    images: { task: 'task.svg', modeler: 'modeler.svg', management: 'management.svg', admin: 'admin.svg' },
    ...overrides
  }
  Object.defineProperty(vm, 'startableProcesses', {
    get: () => navigationPermissionsMixin.computed.startableProcesses.call(vm)
  })
  Object.defineProperty(vm, 'navGroups', {
    get: () => StartView.computed.navGroups.call(vm)
  })
  Object.defineProperty(vm, 'groupById', {
    get: () => StartView.computed.groupById.call(vm)
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
  Object.defineProperty(vm, 'visibleTiles', {
    get: () => StartView.computed.visibleTiles.call(vm)
  })
  Object.defineProperty(vm, 'tiles', {
    get: () => StartView.computed.tiles.call(vm)
  })
  Object.defineProperty(vm, 'builderTile', {
    get: () => StartView.computed.builderTile.call(vm)
  })
  Object.defineProperty(vm, 'dataTile', {
    get: () => StartView.computed.dataTile.call(vm)
  })
  vm.$router = { replace: vi.fn() }
  vm.redirectIfTasksOnly = StartView.methods.redirectIfTasksOnly.bind(vm)
  return vm
}

const TASKS_ONLY_PERMISSIONS = {
  permissionsCockpit: false,
  permissionsModeler: false,
  permissionsUsers: false
}

describe('StartView.vue nav tiles', () => {
  it('exposes CE tile ids with no data tile, since CE has no data group of its own', () => {
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
    expect(vm.adminOptions.some(o => o.to?.name === 'adminSystem')).toBe(true)
    expect(vm.adminOptions.some(o => o.to === '/seven/auth/admin/system/system-diagnostics')).toBe(false)
    expect(vm.adminOptions.some(o => o.to === '/seven/auth/admin/system/execution-metrics')).toBe(false)
  })

  it('keeps cockpit tile hover CE-shaped when a registered extender adds navbar-only EE items', () => {
    useExtender(groups => {
      const clone = groups.map(g => ({ ...g, items: g.items ? [...g.items] : g.items }))
      const cockpit = clone.find(g => g.id === 'cockpit')
      cockpit.items.push({
        surfaces: [SURFACES.NAVBAR],
        to: '/seven/auth/operation-log',
        title: 'operation-log.title',
        tooltip: 'operation-log.description'
      })
      return clone
    })
    const vm = evaluateStartView()
    expect(vm.cockpitOptions.some(o => o.to === '/seven/auth/operation-log')).toBe(false)
    expect(vm.cockpitOptions.map(o => o.to)).toEqual([
      '/seven/auth/processes/list',
      '/seven/auth/decisions/list',
      '/seven/auth/human-tasks',
      '/seven/auth/deployments',
      '/seven/auth/batches'
    ])
  })

  it('shows a data tile only once an extender injects a whole data group (not DataTileOptionsPlugin)', () => {
    useExtender(groups => {
      const clone = [...groups]
      clone.push({
        id: 'data',
        title: 'start.data.title',
        show: true,
        items: [{
          to: '/seven/auth/ins7ght',
          title: 'start.ins7ght.title',
          tooltip: 'start.ins7ght.tooltip',
          icon: 'mdi-chart-box-outline'
        }]
      })
      return clone
    })
    const vm = evaluateStartView()
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

describe('StartView.vue single-option tile collapsing', () => {
  it('collapses the builder tile to Modeler when it is the only builder option', () => {
    const vm = evaluateStartView()
    expect(vm.builderOptions).toHaveLength(1)
    expect(vm.builderTile).toEqual({ to: '/seven/auth/modeler', title: 'start.modeler.title', options: null })
  })

  it('collapses the data tile to Ins7ght when the extender injects a single-item data group', () => {
    useExtender(groups => {
      const clone = [...groups]
      clone.push({
        id: 'data',
        title: 'start.data.title',
        show: true,
        tileImage: 'ins7ght.svg',
        items: [{
          to: '/seven/auth/ins7ght',
          title: 'start.ins7ght.title',
          tooltip: 'start.ins7ght.tooltip',
          icon: 'mdi-chart-box-outline'
        }]
      })
      return clone
    })
    const vm = evaluateStartView()
    expect(vm.dataTile).toEqual({ to: '/seven/auth/ins7ght', title: 'start.ins7ght.title', src: 'ins7ght.svg', options: null })
  })

  it('keeps the data tile for a data group with several tools, leading to its default and listing them', () => {
    const tool = (to, title) => ({ to, title, tooltip: title, icon: 'mdi-database-outline' })
    useExtender(groups => {
      return [...groups, {
        id: 'data',
        title: 'start.data.title',
        show: true,
        defaultTo: '/seven/auth/ins7ght',
        tileImage: 'data.svg',
        items: [tool('/seven/auth/ins7ght', 'start.ins7ght.title'), { divider: true }, tool('/seven/auth/dataflow', 'dataflow.title')]
      }]
    })
    const vm = evaluateStartView()

    expect(vm.tiles).toContain('data')
    expect(vm.dataTile).toMatchObject({ to: '/seven/auth/ins7ght', title: 'start.data.title', src: 'data.svg' })
    expect(vm.dataTile.options.map(o => o.to)).toEqual(['/seven/auth/ins7ght', '/seven/auth/dataflow'])
  })

  it('shows no data tile when there is no injected data group at all', () => {
    const vm = evaluateStartView()
    expect(vm.dataTile).toBeNull()
  })
})

describe('StartView.vue tasks-only redirect', () => {
  it('does not redirect when other tiles are also available', () => {
    const vm = evaluateStartView()
    vm.redirectIfTasksOnly()
    expect(vm.$router.replace).not.toHaveBeenCalled()
  })

  it('redirects to the tasklist while the process list is still empty', () => {
    const vm = evaluateStartView({
      ...TASKS_ONLY_PERMISSIONS,
      $store: { state: { process: { list: [] } } }
    })
    expect(vm.tiles).toEqual(['tasks'])
    vm.redirectIfTasksOnly()
    expect(vm.$router.replace).toHaveBeenCalledWith('/seven/auth/tasks')
  })

  // Same target either way, so the landing page does not depend on load timing
  it('redirects to the tasklist as well once a startable process is loaded', () => {
    const vm = evaluateStartView({
      ...TASKS_ONLY_PERMISSIONS,
      $store: { state: { process: { list: [{ revoked: false, startableInTasklist: true }] } } }
    })
    expect(vm.tiles).toEqual(['tasks'])
    vm.redirectIfTasksOnly()
    expect(vm.$router.replace).toHaveBeenCalledWith('/seven/auth/tasks')
  })

  it('does not redirect while an error dialog needs to explain a prior redirect', () => {
    const vm = evaluateStartView({
      ...TASKS_ONLY_PERMISSIONS,
      $route: { query: { errorType: 'NoPermission' } }
    })
    vm.redirectIfTasksOnly()
    expect(vm.$router.replace).not.toHaveBeenCalled()
  })
})

// A tile must lead somewhere the user may open: tasksHome / builderHome need tasklist / modeler permission
describe('StartView.vue tiles come from the catalog only', () => {
  const plugin = { methods: { getOptions: () => [{ to: '/seven/auth/flow-tool', title: 'Flow tool' }] } }

  it('shows no Tasks or Builder tile for options a tile-options plugin adds on its own', () => {
    const vm = evaluateStartView({
      permissionsTaskList: false,
      permissionsModeler: false,
      $options: { components: { TasksTileOptionsPlugin: plugin, BuilderTileOptionsPlugin: plugin } }
    })
    expect(vm.tiles).toEqual(['cockpit', 'admin'])
  })

  it('does not redirect to the guarded tasklist without tasklist permission', () => {
    const vm = evaluateStartView({
      ...TASKS_ONLY_PERMISSIONS,
      permissionsTaskList: false,
      $options: { components: { TasksTileOptionsPlugin: plugin } }
    })
    vm.redirectIfTasksOnly()
    expect(vm.$router.replace).not.toHaveBeenCalled()
  })
})

describe('StartView.vue waits for plugins before redirecting', () => {
  const settle = (vm, settled) => StartView.watch.pluginsSettled.handler.call(vm, settled)

  it('decides nothing while plugins are still loading', () => {
    const vm = evaluateStartView(TASKS_ONLY_PERMISSIONS)
    settle(vm, false)
    expect(vm.$router.replace).not.toHaveBeenCalled()
  })

  it('redirects once plugins have loaded and Tasks is still the only tile', () => {
    const vm = evaluateStartView(TASKS_ONLY_PERMISSIONS)
    settle(vm, true)
    expect(vm.$router.replace).toHaveBeenCalledWith('/seven/auth/tasks')
  })

  it('stays on the start page when a plugin contributed an admin area meanwhile', () => {
    const vm = evaluateStartView({
      ...TASKS_ONLY_PERMISSIONS,
      permissionsUsers: true,
      adminPluginEntries: [{ text: 'admin.notifications.title', to: '/seven/auth/admin/notifications', icon: 'mdi-bell' }]
    })
    settle(vm, true)
    expect(vm.tiles).toEqual(['tasks', 'admin'])
    expect(vm.$router.replace).not.toHaveBeenCalled()
  })

  it('reads the shared plugin state', () => {
    expect(typeof StartView.computed.pluginsSettled.call({})).toBe('boolean')
  })
})
