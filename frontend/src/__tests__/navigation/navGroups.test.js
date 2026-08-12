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
  projectGroupsForToolbar,
  projectStartHoverOptions,
  navItemsToTileOptions,
  accessManagementCatalogItems,
  itemVisibleOnSurface
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
  it('returns five groups in toolbar order', () => {
    const groups = buildNavGroups(fullCtx)
    expect(groups.map(g => g.id)).toEqual(['tasks', 'cockpit', 'builder', 'data', 'admin'])
  })

  it('hides data by default and sets cockpit defaultTo to processes', () => {
    const groups = buildNavGroups(fullCtx)
    expect(groups.find(g => g.id === 'data').show).toBe(false)
    expect(groups.find(g => g.id === 'cockpit').defaultTo).toBe('/seven/auth/processes')
  })

  it('gates start-process on startableProcesses', () => {
    const withStart = buildNavGroups(fullCtx)
    const withoutStart = buildNavGroups({ ...fullCtx, startableProcesses: false })
    expect(withStart.find(g => g.id === 'tasks').items.some(i => i.to === '/seven/auth/start-process' && i.show !== false)).toBe(true)
    expect(withoutStart.find(g => g.id === 'tasks').items.find(i => i.to === '/seven/auth/start-process').show).toBe(false)
  })

  it('filterVisibleNavGroups drops hidden tools and empty data', () => {
    const visible = filterVisibleNavGroups(buildNavGroups(fullCtx))
    expect(visible.map(g => g.id)).toEqual(['tasks', 'cockpit', 'builder', 'admin'])
  })

  it('projectGroupsForToolbar keeps dashboard and system leaves', () => {
    const cockpit = projectGroupsForToolbar(buildNavGroups(fullCtx)).find(g => g.id === 'cockpit')
    const admin = projectGroupsForToolbar(buildNavGroups(fullCtx)).find(g => g.id === 'admin')
    expect(cockpit.items.some(i => i.to === '/seven/auth/processes')).toBe(true)
    expect(admin.items.some(i => i.to === '/seven/auth/admin/system/system-diagnostics')).toBe(true)
  })

  it('navItemsToTileOptions skips dividers and resolves titles', () => {
    const cockpit = buildNavGroups(fullCtx).find(g => g.id === 'cockpit')
    const options = navItemsToTileOptions(cockpit.items, key => key)
    expect(options.every(o => o.to && o.title)).toBe(true)
    expect(options.some(o => o.to === '/seven/auth/processes')).toBe(true)
    expect(options.length).toBe(cockpit.items.filter(i => !i.divider).length)
  })

  it('projectStartHoverOptions omits toolbar-only dashboard and collapses system', () => {
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
    expect(hubs.map(i => i.hub)).toEqual(['users', 'groups', 'tenants', 'authorizations'])
    expect(hubs.every(i => i.routeName && itemVisibleOnSurface(i, SURFACES.START_HOVER))).toBe(true)
  })
})
