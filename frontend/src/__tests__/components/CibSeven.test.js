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
import CibSeven from '@/components/CibSeven.vue'

// Mock services
vi.mock('@/services.js', () => ({
  EngineService: {
    getEngines: vi.fn(() => Promise.resolve([{ name: 'default' }]))
  }
}))

describe('CibSeven.vue', () => {
  describe('Methods', () => {
    it('should filter toolbar menus based on show and drop empty tools', () => {
      const items = [
        { id: 'tasks', show: true, items: [{ title: 'a', to: '/a' }] },
        { id: 'data', show: false, items: [] },
        { id: 'admin', show: true, items: [{ title: 'b', to: '/b', show: false }] },
        { id: 'builder', show: true, items: [{ divider: true }, { title: 'c', to: '/c' }, { divider: true }] }
      ]

      const result = CibSeven.methods.getVisibleMenuItems(items)
      expect(result.map(t => t.id)).toEqual(['tasks', 'builder'])
      expect(result[1].items).toEqual([{ title: 'c', to: '/c' }])
    })

    it('should collapse consecutive and trailing dividers', () => {
      const items = [{
        id: 'cockpit',
        show: true,
        items: [
          { divider: true },
          { title: 'a', to: '/a' },
          { divider: true },
          { divider: true },
          { title: 'b', to: '/b' },
          { divider: true }
        ]
      }]
      const result = CibSeven.methods.getVisibleMenuItems(items)
      expect(result[0].items).toEqual([
        { title: 'a', to: '/a' },
        { divider: true },
        { title: 'b', to: '/b' }
      ])
    })

    it('should resolve tool default route from defaultTo or first item', () => {
      expect(CibSeven.methods.getToolDefaultTo({ defaultTo: '/seven/auth/tasks', items: [] }))
        .toBe('/seven/auth/tasks')
      expect(CibSeven.methods.getToolDefaultTo({
        items: [{ divider: true }, { to: '/seven/auth/admin/users' }]
      })).toBe('/seven/auth/admin/users')
    })

    it('should treat a tool as active when the route matches one of its hubRouteNames', () => {
      const tool = { startTo: { name: 'usersManagement' }, hubRouteNames: ['accessManagement'], items: [] }

      expect(CibSeven.methods.isToolActive.call({ $route: { name: 'accessManagement' } }, tool)).toBe(true)
      expect(CibSeven.methods.isToolActive.call({ $route: { name: 'usersManagement' } }, tool)).toBe(true)
      expect(CibSeven.methods.isToolActive.call({ $route: { name: 'adminUsers' } }, tool)).toBe(false)
    })

    it('should check if menu item is active based on route path', () => {
      const mockThis = {
        $route: { path: '/seven/auth/tasks/123' }
      }

      const activeItem = { active: ['seven/auth/tasks'], to: '/seven/auth/tasks' }
      const inactiveItem = { active: ['seven/auth/processes'], to: '/seven/auth/processes' }
      const noActiveItem = { to: '/seven/auth/admin' }

      expect(CibSeven.methods.isMenuItemActive.call(mockThis, activeItem)).toBe(true)
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, inactiveItem)).toBe(false)
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, noActiveItem)).toBe(false)
    })

    it('should not match a hyphenated sibling route sharing the same prefix', () => {
      const mockThis = {
        $route: { path: '/seven/auth/tasks-home' }
      }
      const tasklistItem = { active: ['seven/auth/tasks'] }
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, tasklistItem)).toBe(false)
    })

    it('should match plural resource list pages via their explicit active pattern', () => {
      const mockThis = {
        $route: { path: '/seven/auth/admin/users' }
      }
      const usersItem = { active: ['seven/auth/admin/users', 'seven/auth/admin/user/', 'seven/auth/admin/create-user'] }
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, usersItem)).toBe(true)

      mockThis.$route.path = '/seven/auth/admin/groups'
      const groupsItem = { active: ['seven/auth/admin/groups', 'seven/auth/admin/group/', 'seven/auth/admin/create-group'] }
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, groupsItem)).toBe(true)
    })

    it('should not match a plural list page from its singular prefix alone', () => {
      const mockThis = {
        $route: { path: '/seven/auth/admin/tenants' }
      }
      const tenantItem = { active: ['seven/auth/admin/tenant'] }
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, tenantItem)).toBe(false)
    })

    it('should match a singular detail page via a trailing-slash active pattern', () => {
      const mockThis = {
        $route: { path: '/seven/auth/admin/user/42' }
      }
      const userItem = { active: ['seven/auth/admin/users', 'seven/auth/admin/user/', 'seven/auth/admin/create-user'] }
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, userItem)).toBe(true)
    })

    it('should match via routeName/activeRouteNames when present, ignoring path entirely', () => {
      const usersItem = { routeName: 'adminUsers', activeRouteNames: ['adminUser', 'createUser'], active: ['seven/auth/admin/does-not-matter'] }

      // Matches its own list-page routeName directly, with no duplicate entry needed in activeRouteNames
      expect(CibSeven.methods.isMenuItemActive.call({ $route: { name: 'adminUsers', path: '/seven/auth/admin/users' } }, usersItem)).toBe(true)
      // Matches sibling detail/create routes via activeRouteNames
      expect(CibSeven.methods.isMenuItemActive.call({ $route: { name: 'adminUser', path: '/seven/auth/admin/user/42' } }, usersItem)).toBe(true)
      // A different item's route name matches neither
      expect(CibSeven.methods.isMenuItemActive.call({ $route: { name: 'adminGroups', path: '/seven/auth/admin/groups' } }, usersItem)).toBe(false)
    })

    it('should match nested detail pages under a trailing-slash active pattern', () => {
      const mockThis = {
        $route: { path: '/seven/auth/process/myKey/1/inst1' }
      }
      const processItem = { active: ['seven/auth/process/'] }
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, processItem)).toBe(true)
    })

    it('should check exact match when activeExact is true', () => {
      const mockThis = {
        $route: { path: '/seven/auth/admin' }
      }

      const exactItem = { active: ['seven/auth/admin'], activeExact: true }
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, exactItem)).toBe(true)

      mockThis.$route.path = '/seven/auth/admin/users'
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, exactItem)).toBe(false)
    })

    it('should navigate to the start page before reloading on logout', () => {
      const originalLocation = window.location
      const reloadOrder = []
      delete window.location
      window.location = {
        hash: '#/seven/auth/processes/123',
        reload: vi.fn(() => reloadOrder.push(window.location.hash))
      }

      CibSeven.methods.logout.call({})

      // reload must fire AFTER the hash moves to the start page, else login returns
      // to the (now-stale) previous page
      expect(reloadOrder).toEqual(['#/'])
      expect(window.location.hash).toBe('#/')

      window.location = originalLocation
    })
  })

  describe('Computed Properties', () => {
    it('should generate helpMenuItems with configured links', () => {
      const mockThis = {
        $root: {
          user: { id: '1' },
          config: {
            flowLinkHelp: 'https://help.example.com',
            flowLinkAccessibility: 'https://accessibility.example.com',
            layout: { showSupportInfo: false }
          }
        }
      }

      const items = CibSeven.computed.helpMenuItems.call(mockThis)
      expect(items.length).toBeGreaterThan(0)
      expect(items.some(item => item.href === 'https://help.example.com')).toBe(true)
    })

    it('should expose id-keyed toolbar menus with no CE-native data group', () => {
      const mockThis = {
        permissionsTaskList: true,
        permissionsCockpit: true,
        permissionsModeler: true,
        permissionsUsers: true,
        permissionsUsersManagement: true,
        permissionsGroupsManagement: false,
        permissionsTenantsManagement: false,
        permissionsAuthorizationsManagement: false,
        permissionsSystemManagement: true,
        startableProcesses: true
      }
      const menus = CibSeven.computed.menuItems.call(mockThis)
      expect(menus.map(m => m.id)).toEqual(['tasks', 'cockpit', 'builder', 'admin'])
      expect(menus.find(m => m.id === 'builder').title).toBe('start.builder.title')
      expect(menus.find(m => m.id === 'cockpit').items.some(i => i.divider)).toBe(true)
    })
  })
})
