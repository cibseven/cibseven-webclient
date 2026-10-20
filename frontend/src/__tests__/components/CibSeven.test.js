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
import { shallowMount } from '@vue/test-utils'
import CibSeven from '@/components/CibSeven.vue'

// Mock services
vi.mock('@/services.js', () => ({
  EngineService: {
    getEngines: vi.fn(() => Promise.resolve([{ name: 'default' }]))
  }
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
      return CibSeven.computed.menuItems.call(context).find(group => group.id === 'admin').items
    }

    // Contributed areas (the enterprise edition's notifications) follow the built-in ones
    it('appends the contributed admin entries after the built-in ones', () => {
      const items = adminItems([{
        id: 'notifications', text: 'admin.notifications.title', tooltip: 'admin.notifications.tooltip',
        to: '/seven/auth/admin/notifications'
      }])

      expect(items.at(-2)).toEqual({ divider: true })
      expect(items.at(-1)).toEqual({
        pluginEntry: true,
        to: '/seven/auth/admin/notifications',
        active: ['seven/auth/admin/notifications'],
        icon: undefined,
        tileImage: undefined,
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
      expect(adminItems([]).some(item => item.pluginEntry)).toBe(false)
    })
  })

  // Focused on load for screen readers, but a ring nobody asked for looks like a glitch
  describe('focusBrandLink', () => {
    const shell = (brandLink, headerFlow = {}) => ({ $refs: { headerFlow, brandHome: { $refs: { brandLink } } } })
    const link = () => {
      const a = document.createElement('a')
      a.href = '#/seven/auth/start'
      document.body.appendChild(a)
      return a
    }

    it('focuses the brand link without its ring', () => {
      const brandLink = link()

      CibSeven.methods.focusBrandLink.call(shell(brandLink))

      expect(document.activeElement).toBe(brandLink)
      expect(brandLink.classList.contains('initial-focus')).toBe(true)
      brandLink.remove()
    })

    it('gives the ring back with the first key press', () => {
      const brandLink = link()
      CibSeven.methods.focusBrandLink.call(shell(brandLink))

      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab' }))

      expect(brandLink.classList.contains('initial-focus')).toBe(false)
      brandLink.remove()
    })

    it('does nothing without the header', () => {
      const brandLink = link()

      CibSeven.methods.focusBrandLink.call(shell(brandLink, null))

      expect(document.activeElement).not.toBe(brandLink)
      expect(brandLink.classList.contains('initial-focus')).toBe(false)
      brandLink.remove()
    })
  })

  describe('Methods', () => {
    it('should filter navbar menus based on show and drop empty tools', () => {
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

    // Contributed entries may give paths with a leading slash (ADMIN_ENTRY_SLOT `active`)
    it('should treat a leading slash in an active entry as optional', () => {
      const mockThis = { $route: { path: '/seven/auth/admin/ee-area/1' } }
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, { active: ['/seven/auth/admin/ee-area'] })).toBe(true)
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, { active: ['/seven/auth/admin/ee-area/1'], activeExact: true })).toBe(true)
    })

    it('should match activeExact entries on whole segments only', () => {
      const mockThis = { $route: { path: '/seven/auth/my-batches' } }
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, { active: ['batches'], activeExact: true })).toBe(false)
      mockThis.$route.path = '/seven/auth/batches'
      expect(CibSeven.methods.isMenuItemActive.call(mockThis, { active: ['batches'], activeExact: true })).toBe(true)
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

    // The logged-out page does the logout: the header's logout only hands over to it
    describe('logout', () => {
      it('should hand over to the logged-out page', () => {
        const push = vi.fn()

        CibSeven.methods.logout.call({ $router: { push } })

        expect(push).toHaveBeenCalledWith({ name: 'loggedOut' })
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

    it('should expose id-keyed navbar menus with no CE-native data group', () => {
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

  describe('Header navigation model', () => {
    const t = key => `t:${key}`
    const route = (name = 'tasklist', path = '/seven/auth/tasks') => ({ name, path })
    const shell = (overrides = {}) => {
      const vm = {
        $t: t,
        $route: route(),
        $root: { user: { id: 'ada' }, config: { layout: { showFeedbackButton: true, showInfoAndHelp: true, showUserSettings: true } } },
        permissionsUserProfile: true,
        helpMenuItems: [
          { type: 'link', href: 'https://help', title: 'help.title', tooltip: 'help.tooltip' },
          { type: 'button', ref: 'about', title: 'about.title', tooltip: 'about.tooltip' }
        ],
        ...overrides
      }
      vm.isMenuItemActive = item => CibSeven.methods.isMenuItemActive.call(vm, item)
      vm.isToolActive = tool => CibSeven.methods.isToolActive.call(vm, tool)
      vm.getToolDefaultTo = tool => CibSeven.methods.getToolDefaultTo.call(vm, tool)
      vm.singleDestination = tool => CibSeven.methods.singleDestination.call(vm, tool)
      return vm
    }
    const tasksTool = {
      id: 'tasks', title: 'start.tasks.title', icon: 'mdi-clipboard-text-outline', defaultTo: '/seven/auth/tasks',
      startTo: { name: 'tasksHome' },
      items: [
        { to: '/seven/auth/tasks', active: ['seven/auth/tasks'], title: 'start.taskList.title' },
        { divider: true },
        { to: '/seven/auth/start-process', active: ['seven/auth/start-process'], title: 'start.startProcess.title', tooltip: 'start.startProcess.tooltip' }
      ]
    }
    const builderTool = {
      id: 'builder', title: 'start.builder.title', icon: 'mdi-tools', defaultTo: { name: 'builderHome' },
      startTo: { name: 'builderHome' }, collapseSingle: true,
      items: [{ to: '/seven/auth/modeler', active: ['seven/auth/modeler'], title: 'start.modeler.title' }]
    }

    it('turns the catalog into clusters: icon to the section, title to its overview, items in sections', () => {
      const [tasks] = CibSeven.computed.navClusters.call(shell({ computedMenuItems: [tasksTool] }))

      expect(tasks).toMatchObject({ id: 'tasks', label: 't:start.tasks.title', to: '/seven/auth/tasks', titleTo: { name: 'tasksHome' }, active: true })
      expect(tasks.sections.map(section => section.map(item => item.label))).toEqual([['t:start.taskList.title'], ['t:start.startProcess.title']])
      expect(tasks.sections[0][0]).toMatchObject({ active: true, tooltip: 't:start.taskList.title' })
      expect(tasks.sections[1][0]).toMatchObject({ active: false, tooltip: 't:start.startProcess.tooltip' })
    })

    it('sends a section with a single tool straight to it, and to its overview page with more', () => {
      const [single] = CibSeven.computed.navClusters.call(shell({ computedMenuItems: [builderTool] }))
      expect(single).toMatchObject({ to: '/seven/auth/modeler', titleTo: '/seven/auth/modeler' })

      const twoTools = { ...builderTool, items: [...builderTool.items, { to: '/seven/auth/forms', title: 'forms' }] }
      const [hub] = CibSeven.computed.navClusters.call(shell({ computedMenuItems: [twoTools] }))
      expect(hub).toMatchObject({ to: { name: 'builderHome' }, titleTo: { name: 'builderHome' } })
    })

    it('offers feedback as an action and help as a menu of links and dialogs', () => {
      const [feedback, help] = CibSeven.computed.utilityClusters.call(shell())

      expect(feedback).toMatchObject({ id: 'feedback', modal: 'report' })
      expect(feedback.sections).toBeUndefined()
      expect(help.sections[0]).toEqual([
        { key: 'https://help', label: 't:help.title', tooltip: 't:help.tooltip', href: 'https://help', external: true, modal: undefined },
        { key: 'about', label: 't:about.title', tooltip: 't:about.tooltip', href: undefined, external: false, modal: 'about' }
      ])
    })

    it('leaves out feedback and help when switched off', () => {
      const vm = shell()
      vm.$root.config.layout = { showFeedbackButton: false, showInfoAndHelp: false }
      expect(CibSeven.computed.utilityClusters.call(vm)).toEqual([])
    })

    it('links the user profile when user settings are shown', () => {
      const vm = shell({ $route: route('userProfile', '/seven/auth/account/ada') })
      expect(CibSeven.computed.userMenuItems.call(vm)).toEqual([{
        key: 'profile', label: 't:start.account.profile.title', tooltip: 't:start.account.profile.tooltip',
        to: '/seven/auth/account/ada', active: true
      }])

      vm.permissionsUserProfile = false
      expect(CibSeven.computed.userMenuItems.call(vm)).toEqual([])
    })

    it('opens the dialog a header item asks for', () => {
      const show = vi.fn()
      CibSeven.methods.onHeaderSelect.call({ $refs: { about: { show } } }, { modal: 'about' })
      expect(show).toHaveBeenCalled()
      expect(() => CibSeven.methods.onHeaderSelect.call({ $refs: {} }, { key: 'other' })).not.toThrow()
    })
  })

  describe('navbar catalog', () => {
    it('applies registered nav group extenders, like the start page does', async () => {
      const { registerNavGroupsExtender } = await import('@/navigation/navGroups.js')
      const unregister = registerNavGroupsExtender(groups => [...groups, { id: 'data', show: true, items: [] }])
      try {
        const groups = CibSeven.computed.menuItems.call({ permissionsTaskList: true })
        expect(groups.at(-1).id).toBe('data')
      } finally {
        unregister()
      }
    })
  })
})
