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
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import CibSeven from '@/components/CibSeven.vue'
import { AuthService } from '@/services.js'
import { LOGGED_OUT_KEY } from '@/constants.js'

// Mock services
vi.mock('@/services.js', () => ({
  EngineService: {
    getEngines: vi.fn(() => Promise.resolve([{ name: 'default' }]))
  },
  AuthService: { logout: vi.fn() }
}))

describe('CibSeven.vue', () => {
  // Contributions to the banner slot (the enterprise edition's system notifications) are for
  // logged-in users only and sit between the header and the main area
  describe('app banner slot', () => {
    const mountShell = (user) => shallowMount(CibSeven, {
      global: {
        mocks: {
          user, header: 'false', $t: key => key, $route: { path: '/seven/auth/start', name: 'start', meta: {} },
          config: { permissions: {}, layout: {}, supportedLanguages: [] },
          $store: { state: { process: { list: [] } }, getters: {}, dispatch: vi.fn(() => Promise.resolve()) }
        },
        provide: { isMobile: false },
        stubs: { 'router-view': true, 'b-modal': true, PluginSlot: { name: 'PluginSlot', template: '<div class="plugin-slot"></div>', props: ['name', 'params'] } }
      }
    })

    it('offers the app-banner slot to a logged-in user, before the main area', () => {
      const wrapper = mountShell({ id: 'demo' })

      const slot = wrapper.findComponent({ name: 'PluginSlot' })
      expect(slot.exists()).toBe(true)
      expect(slot.props('name')).toBe('app-banner')
      expect(slot.props('params')).toEqual({ user: { id: 'demo' } })
      // the banner comes first, so it pushes the page down instead of covering it
      const html = wrapper.html()
      expect(html.indexOf('plugin-slot')).toBeLessThan(html.indexOf('<main'))
    })

    it('offers no banner without a logged-in user', () => {
      expect(mountShell(null).findComponent({ name: 'PluginSlot' }).exists()).toBe(false)
    })
  })

  describe('admin menu', () => {
    const adminItems = (adminPluginEntries) => {
      const context = {
        permissionsTaskList: false, permissionsCockpit: false, permissionsModeler: false, startableProcesses: null,
        permissionsUsers: true, permissionsUsersManagement: true, permissionsGroupsManagement: false,
        permissionsTenantsManagement: false, permissionsAuthorizationsManagement: false, permissionsSystemManagement: false,
        adminPluginEntries
      }
      return CibSeven.computed.menuItems.call(context).find(group => group.groupTitle === 'start.admin.title').items
    }

    // Contributed areas (the enterprise edition's notifications) follow the built-in ones
    it('appends the contributed admin entries after the built-in ones', () => {
      const items = adminItems([{
        id: 'notifications', text: 'admin.notifications.title', tooltip: 'admin.notifications.tooltip',
        to: '/seven/auth/admin/notifications'
      }])

      expect(items.at(-1)).toEqual({
        show: true,
        to: '/seven/auth/admin/notifications',
        active: ['seven/auth/admin/notifications'],
        tooltip: 'admin.notifications.tooltip',
        title: 'admin.notifications.title'
      })
    })

    it('uses the title as tooltip and keeps given active paths', () => {
      const items = adminItems([{
        id: 'reports', text: 'admin.reports.title', to: '/seven/auth/admin/reports', active: ['seven/auth/admin/report']
      }])

      expect(items.at(-1)).toMatchObject({ tooltip: 'admin.reports.title', active: ['seven/auth/admin/report'] })
    })

    it('adds nothing when no entry is contributed', () => {
      expect(adminItems([]).at(-1).to).toBe('/seven/auth/admin/system')
    })
  })

  describe('Methods', () => {
    it('should filter menu items based on show property', () => {
      const items = [
        { show: true, groupTitle: 'Group 1', items: [] },
        { show: false, groupTitle: 'Group 2', items: [] },
        { show: true, groupTitle: 'Group 3', items: [] }
      ]
      
      const result = CibSeven.methods.getVisibleMenuItems(items)
      expect(result.length).toBe(2)
      expect(result[0].groupTitle).toBe('Group 1')
      expect(result[1].groupTitle).toBe('Group 3')
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

    describe('logout with SSO', () => {
      const originalLocation = window.location
      const ssoThis = { $root: { config: { ssoActive: true } } }

      beforeEach(() => {
        AuthService.logout.mockReset()
        sessionStorage.clear()
        localStorage.setItem('accessToken', 'a')
        delete window.location
        window.location = { hash: '#/seven/auth/tasks', href: 'http://localhost/', reload: vi.fn() }
      })

      afterEach(() => {
        window.location = originalLocation
        localStorage.clear()
      })

      it('should go to the end session URL the backend returns, instead of reloading', async () => {
        AuthService.logout.mockResolvedValue({ endSessionUrl: 'https://idp/logout?client_id=c' })

        await CibSeven.methods.logout.call(ssoThis)

        // asks the identity provider to return to the app, and marks that it should wait for the user
        expect(AuthService.logout).toHaveBeenCalledWith('http://localhost/')
        expect(sessionStorage.getItem(LOGGED_OUT_KEY)).toBe('1')
        expect(window.location.href).toBe('https://idp/logout?client_id=c')
        expect(window.location.reload).not.toHaveBeenCalled()
        expect(localStorage.getItem('accessToken')).toBeNull()
      })

      it('should reload to the start page when the backend names no end session URL', async () => {
        AuthService.logout.mockResolvedValue({})

        await CibSeven.methods.logout.call(ssoThis)

        expect(window.location.hash).toBe('#/')
        expect(window.location.reload).toHaveBeenCalled()
        expect(sessionStorage.getItem(LOGGED_OUT_KEY)).toBeNull()
      })

      it('should still log out locally when the backend call fails', async () => {
        const error = vi.spyOn(console, 'error').mockImplementation(() => {})
        AuthService.logout.mockRejectedValue(new Error('401'))

        await CibSeven.methods.logout.call(ssoThis)

        expect(localStorage.getItem('accessToken')).toBeNull()
        expect(window.location.reload).toHaveBeenCalled()
        error.mockRestore()
      })

      it('should not call the backend without SSO', async () => {
        await CibSeven.methods.logout.call({ $root: { config: { ssoActive: false } } })

        expect(AuthService.logout).not.toHaveBeenCalled()
      })
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
  })
})
