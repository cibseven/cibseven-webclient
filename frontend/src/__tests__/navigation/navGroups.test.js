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
import {
  SURFACES,
  buildNavGroups,
  filterVisibleNavGroups,
  projectGroupsForNavbar,
  projectStartHoverOptions,
  singleOptionTile,
  tasksOnlyRedirectTarget,
  accessManagementCatalogItems,
  itemVisibleOnSurface,
  buildVisibleNavGroups,
  getVisibleGroup,
  itemsToSections
} from '@/navigation/navGroups.js'

const fullCtx = {
  permissionsTaskList: true,
  startableProcesses: true,
  permissionsCockpit: true,
  permissionsModeler: true,
  permissionsUsers: true,
  permissionsUsersManagement: true,
  permissionsGroupsManagement: true,
  permissionsTenantsManagement: true,
  permissionsAuthorizationsManagement: true,
  permissionsSystemManagement: true
}

describe('navGroups factory', () => {
  it('returns four groups in navbar order, with no CE-native data group', () => {
    const groups = buildNavGroups(fullCtx)
    expect(groups.map(g => g.id)).toEqual(['tasks', 'cockpit', 'builder', 'admin'])
    expect(groups.find(g => g.id === 'data')).toBeUndefined()
  })

  it('sets cockpit defaultTo to processes', () => {
    const groups = buildNavGroups(fullCtx)
    expect(groups.find(g => g.id === 'cockpit').defaultTo).toBe('/seven/auth/processes')
  })

  it('gates start-process on startableProcesses', () => {
    const withStart = buildNavGroups(fullCtx)
    const withoutStart = buildNavGroups({ ...fullCtx, startableProcesses: false })
    expect(withStart.find(g => g.id === 'tasks').items.some(i => i.to === '/seven/auth/start-process' && i.show !== false)).toBe(true)
    expect(withoutStart.find(g => g.id === 'tasks').items.find(i => i.to === '/seven/auth/start-process').show).toBe(false)
  })

  it('filterVisibleNavGroups keeps all groups visible with full permissions', () => {
    const visible = filterVisibleNavGroups(buildNavGroups(fullCtx))
    expect(visible.map(g => g.id)).toEqual(['tasks', 'cockpit', 'builder', 'admin'])
  })

  it('filterVisibleNavGroups keeps an EE/Flow-injected data group', () => {
    const groups = buildNavGroups(fullCtx)
    groups.push({
      id: 'data',
      title: 'start.data.title',
      show: true,
      items: [{ to: '/seven/auth/ins7ght', title: 'start.ins7ght.title' }]
    })
    const visible = filterVisibleNavGroups(groups)
    expect(visible.map(g => g.id)).toEqual(['tasks', 'cockpit', 'builder', 'admin', 'data'])
  })

  it('projectGroupsForNavbar keeps dashboard and system leaves', () => {
    const cockpit = projectGroupsForNavbar(buildNavGroups(fullCtx)).find(g => g.id === 'cockpit')
    const admin = projectGroupsForNavbar(buildNavGroups(fullCtx)).find(g => g.id === 'admin')
    expect(cockpit.items.some(i => i.to === '/seven/auth/processes')).toBe(true)
    expect(admin.items.some(i => i.to === '/seven/auth/admin/system/system-diagnostics')).toBe(true)
  })

  it('projectStartHoverOptions omits navbar-only dashboard and collapses system', () => {
    const cockpit = buildNavGroups(fullCtx).find(g => g.id === 'cockpit')
    const admin = buildNavGroups(fullCtx).find(g => g.id === 'admin')
    const cockpitOptions = projectStartHoverOptions(cockpit.items, key => key)
    const adminOptions = projectStartHoverOptions(admin.items, key => key)
    expect(cockpitOptions.some(o => o.to === '/seven/auth/processes')).toBe(false)
    expect(cockpitOptions.some(o => o.to === '/seven/auth/processes/list')).toBe(true)
    expect(adminOptions.map(o => o.to)).toEqual([
      '/seven/auth/admin/users',
      '/seven/auth/admin/groups',
      '/seven/auth/admin/tenants',
      '/seven/auth/admin/authorizations',
      '/seven/auth/admin/system'
    ])
    expect(adminOptions.at(-1).title).toBe('admin.system.title')
  })

  it('accessManagementCatalogItems returns identity hubs only', () => {
    const admin = buildNavGroups(fullCtx).find(g => g.id === 'admin')
    const hubs = accessManagementCatalogItems(admin.items)
    expect(hubs.map(i => i.routeName)).toEqual(['adminUsers', 'adminGroups', 'adminTenants', 'authorizations'])
    expect(hubs.every(i => i.routeName && itemVisibleOnSurface(i, SURFACES.START_HOVER))).toBe(true)
    expect(hubs.every(i => i.tileImage)).toBe(true)
  })

  it('admin group lists accessManagement as an extra active-hub route name', () => {
    const admin = buildNavGroups(fullCtx).find(g => g.id === 'admin')
    expect(admin.hubRouteNames).toEqual(['accessManagement'])
  })

  it('getVisibleGroup resolves a single group by id from a vm-shaped ctx, or undefined when hidden', () => {
    const vm = { ...fullCtx, startableProcesses: fullCtx.startableProcesses }
    expect(getVisibleGroup(vm, 'tasks').id).toBe('tasks')
    expect(getVisibleGroup({ ...vm, permissionsModeler: false }, 'builder')).toBeUndefined()
  })

  describe('buildVisibleNavGroups', () => {
    const dataGroup = { id: 'data', title: 'start.data.title', show: true, items: [{ to: '/seven/auth/ins7ght', title: 'start.ins7ght.title' }] }
    const extending = (methods) => ({ ...fullCtx, $options: { components: { NavGroupsExtender: methods } } })

    it('returns the filtered CE catalog when no extender is registered', () => {
      expect(buildVisibleNavGroups(fullCtx).map(g => g.id)).toEqual(['tasks', 'cockpit', 'builder', 'admin'])
    })

    it('applies a registered NavGroupsExtender with the vm as `this`, then filters', () => {
      const vm = extending({
        methods: {
          extend(groups) {
            expect(this).toBe(vm)
            return [...groups, dataGroup, { id: 'hidden', show: false, items: [{ to: '/x' }] }]
          }
        }
      })
      expect(buildVisibleNavGroups(vm).map(g => g.id)).toEqual(['tasks', 'cockpit', 'builder', 'admin', 'data'])
    })

    it('finds the extender methods on a compiled SFC (__vccOpts)', () => {
      const vm = extending({ __vccOpts: { methods: { extend: groups => [...groups, dataGroup] } } })
      expect(getVisibleGroup(vm, 'data').id).toBe('data')
    })
  })

  it('singleOptionTile returns null for zero or multiple options', () => {
    expect(singleOptionTile([])).toBeNull()
    expect(singleOptionTile(null)).toBeNull()
    expect(singleOptionTile([{ to: '/a', title: 'A' }, { to: '/b', title: 'B' }])).toBeNull()
  })

  it('singleOptionTile resolves the lone option to a direct tile target', () => {
    expect(singleOptionTile([{ to: '/seven/auth/modeler', title: 'Modeler', icon: 'mdi-drawing-box' }]))
      .toEqual({ to: '/seven/auth/modeler', title: 'Modeler' })
  })

  it('tasksOnlyRedirectTarget is null when Tasks is not the sole tile', () => {
    expect(tasksOnlyRedirectTarget(['tasks', 'cockpit'], [{ to: '/seven/auth/tasks', title: 'Task list' }])).toBeNull()
    expect(tasksOnlyRedirectTarget(['cockpit'], [])).toBeNull()
    expect(tasksOnlyRedirectTarget([], [])).toBeNull()
  })

  it('tasksOnlyRedirectTarget goes straight to the tasklist when it is the only tasks option', () => {
    const target = tasksOnlyRedirectTarget(['tasks'], [{ to: '/seven/auth/tasks', title: 'Task list' }])
    expect(target).toBe('/seven/auth/tasks')
  })

  it('tasksOnlyRedirectTarget goes to the tasks hub when start-process is also available', () => {
    const target = tasksOnlyRedirectTarget(['tasks'], [
      { to: '/seven/auth/tasks', title: 'Task list' },
      { to: '/seven/auth/start-process', title: 'Start process' }
    ])
    expect(target).toEqual({ name: 'tasksHome' })
  })
})

describe('itemsToSections', () => {
  it('splits items at dividers and drops empty sections', () => {
    const a = { to: '/a' }, b = { to: '/b' }, c = { to: '/c' }
    expect(itemsToSections([{ divider: true }, a, b, { divider: true }, { divider: true }, c, { divider: true }]))
      .toEqual([[a, b], [c]])
  })

  it('returns no sections for no items', () => {
    expect(itemsToSections(undefined)).toEqual([])
    expect(itemsToSections([{ divider: true }])).toEqual([])
  })
})

describe('contributed admin entries in the catalog', () => {
  const adminGroup = (adminPluginEntries) => buildNavGroups({ permissionsUsers: true, adminPluginEntries }).find(g => g.id === 'admin')

  it('follows the built-in entries behind a divider', () => {
    const items = adminGroup([{ text: 'admin.notifications.title', to: '/seven/auth/admin/notifications', image: 'n.svg', icon: 'mdi-bell' }]).items
    expect(items.at(-2)).toEqual({ divider: true })
    expect(items.at(-1)).toMatchObject({ pluginEntry: true, tileImage: 'n.svg', icon: 'mdi-bell', tooltip: 'admin.notifications.title' })
  })

  it('adds nothing without contributions', () => {
    expect(adminGroup(undefined).items.some(item => item.pluginEntry)).toBe(false)
  })
})
