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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { AdminService } from '@/services.js'
import ProfileUser from '@/components/admin/ProfileUser.vue'
import { createComponentContext } from '../support/callWithContext.js'
import { translationStubs } from '../support/mountWithDefaults.js'

vi.mock('@/services.js', () => ({
  AdminService: { findUsers: vi.fn(), deleteUser: vi.fn(() => Promise.resolve()) }
}))

const ADA = { id: 'ada', firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.org' }

function context(overrides = {}) {
  const vm = createComponentContext({
    ...ProfileUser.data.call({}),
    editMode: false,
    $route: { name: 'accountProfile', params: { userId: 'ada' }, query: {} },
    ...overrides
  })
  vm.$root.user = { id: 'ada' }
  vm.$root.config = { userEditable: true, userPasswordChangeEnabled: true }
  for (const [name, method] of Object.entries(ProfileUser.methods)) {
    if (!(name in vm)) vm[name] = method.bind(vm)
  }
  for (const [name, def] of Object.entries(ProfileUser.computed)) {
    if (!(name in vm)) Object.defineProperty(vm, name, { get: () => def.call(vm), configurable: true })
  }
  return vm
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('ProfileUser tabs', () => {
  it('offers every tab of a user the engine knows, on their own account', () => {
    const vm = context({ user: { ...ADA } })
    expect(vm.visibleTabs.map(tab => tab.id)).toEqual(['profile', 'password', 'groups', 'tenants', 'preferences'])
  })

  it.each([
    ['without a password change', { userPasswordChangeEnabled: false }, ['profile', 'groups', 'tenants', 'preferences']],
    ['without user editing', { userEditable: false }, ['profile', 'groups', 'tenants', 'preferences']]
  ])('leaves out the password tab %s', (_, config, tabs) => {
    const vm = context({ user: { ...ADA } })
    Object.assign(vm.$root.config, config)
    expect(vm.visibleTabs.map(tab => tab.id)).toEqual(tabs)
  })

  // The details come from the engine; a user it has none of (e.g. from SSO) only has preferences
  it('offers only the preferences of a user the engine has no details of', () => {
    const vm = context({ user: { id: 'ada', noInfo: true } })
    expect(vm.visibleTabs.map(tab => tab.id)).toEqual(['preferences'])
  })

  it('offers the preferences only on the page of the signed-in user', () => {
    const vm = context({ user: { ...ADA, id: 'bob' }, editMode: true })
    expect(vm.visibleTabs.map(tab => tab.id)).not.toContain('preferences')
  })

  it.each([
    [false, 'accountGroups', 'groups'],
    [true, 'adminUserGroups', 'groups'],
    // The same tab under the other page is not this page's tab
    [false, 'adminUserGroups', undefined],
    [true, 'adminUser', undefined]
  ])('in edit mode %s, reads the route %s as the tab %s', (editMode, name, tab) => {
    const vm = context({ editMode, user: { ...ADA }, $route: { name, params: { userId: 'ada' } } })
    expect(vm.selectedTab?.id).toBe(tab)
  })

  it('links each tab to its route under the page', () => {
    expect(context().tabRoute({ id: 'tenants' })).toEqual({ name: 'accountTenants', params: { userId: 'ada' } })
    expect(context({ editMode: true }).tabRoute({ id: 'tenants' })).toEqual({ name: 'adminUserTenants', params: { userId: 'ada' } })
  })

  it('gives a tab the user and the edit mode, and the preferences nothing', () => {
    const vm = context({ user: { ...ADA }, editMode: true, $route: { name: 'adminUserProfile', params: {} } })
    expect(vm.tabProps).toEqual({ user: vm.user, editMode: true, 'onUpdate:user': expect.any(Function) })

    vm.$route.name = 'adminUserPreferences'
    expect(vm.tabProps).toEqual({})
  })

  // The information tab saves a copy of the user, which the page then shows
  it('takes the user a tab saved', () => {
    const vm = context({ user: { ...ADA }, $route: { name: 'accountProfile', params: {} } })
    vm.tabProps['onUpdate:user']({ ...ADA, lastName: 'Byron' })
    expect(vm.user.lastName).toBe('Byron')
  })

  // This is how an extension of the page adds a tab
  it('gives an added tab its own props', () => {
    const vm = context({ user: { ...ADA }, $route: { name: 'accountExtra', params: {} } })
    vm.tabs.push({ id: 'extra', label: 'extra', show: () => true, props: v => ({ userId: v.user.id }) })
    expect(vm.selectedTab.id).toBe('extra')
    expect(vm.tabProps).toEqual({ userId: 'ada' })
  })
})

describe('ProfileUser openShownTab', () => {
  it('swaps a tab the user does not have for the first one they have', () => {
    const vm = context({ user: { id: 'ada', noInfo: true } })
    vm.openShownTab()
    expect(vm.$router.replace).toHaveBeenCalledWith({ name: 'accountPreferences', params: { userId: 'ada' } })
  })

  // EE adds the authorizations before the preferences; a user without details still opens the preferences
  it('swaps to the preferences of a user without details, ahead of an added tab', () => {
    const vm = context({ user: { id: 'ada', noInfo: true } })
    vm.tabs.splice(4, 0, { id: 'extra', label: 'extra', show: () => true })
    vm.openShownTab()
    expect(vm.$router.replace).toHaveBeenCalledWith({ name: 'accountPreferences', params: { userId: 'ada' } })
  })

  it('swaps to the first tab shown when a user with details opens a hidden one', () => {
    const vm = context({ user: { ...ADA }, $route: { name: 'accountPassword', params: { userId: 'ada' } } })
    vm.$root.config.userPasswordChangeEnabled = false
    vm.openShownTab()
    expect(vm.$router.replace).toHaveBeenCalledWith({ name: 'accountProfile', params: { userId: 'ada' } })
  })

  it.each([
    ['a tab that is shown', { user: { ...ADA } }],
    ['a route that is no tab', { user: { id: 'ada', noInfo: true }, $route: { name: 'account', params: {} } }],
    // Somebody else's page without details has no tab to swap to
    ['a page without any tab', { user: { id: 'bob', noInfo: true }, editMode: true, $route: { name: 'adminUserProfile', params: {} } }]
  ])('stays on %s', (_, overrides) => {
    const vm = context(overrides)
    vm.openShownTab()
    expect(vm.$router.replace).not.toHaveBeenCalled()
  })

  it('checks the tab again when another tab opens, once the user is loaded', () => {
    const vm = { user: { id: null }, openShownTab: vi.fn() }
    ProfileUser.watch['$route.name'].call(vm)
    expect(vm.openShownTab).not.toHaveBeenCalled()

    vm.user.id = 'ada'
    ProfileUser.watch['$route.name'].call(vm)
    expect(vm.openShownTab).toHaveBeenCalledTimes(1)
  })
})

describe('ProfileUser loadUser', () => {
  it('loads the user, then checks the tab', async () => {
    AdminService.findUsers.mockResolvedValue([ADA])
    const vm = context({ openShownTab: vi.fn() })
    await vm.loadUser('ada')
    expect(AdminService.findUsers).toHaveBeenCalledWith({ id: 'ada' })
    expect(vm.user).toEqual(ADA)
    expect(vm.openShownTab).toHaveBeenCalled()
  })

  it('stands in a user without details when the engine has none', async () => {
    AdminService.findUsers.mockResolvedValue([])
    const vm = context({ openShownTab: vi.fn() })
    await vm.loadUser('sso-user')
    expect(vm.user).toEqual({ id: 'sso-user', firstName: null, lastName: null, email: null, noInfo: true })
  })

  it('loads the user again when the route names another one', () => {
    const vm = { editMode: true, $route: { params: { userId: 'bob' } }, loadUser: vi.fn() }
    ProfileUser.watch['$route.params.userId'].call(vm)
    expect(vm.loadUser).toHaveBeenCalledWith('bob')
  })
})

describe('ProfileUser rendered', () => {
  const ProfileTab = { name: 'ProfileTab', props: ['user', 'editMode'], template: '<div class="tab">{{ user.firstName }}</div>' }
  const PreferencesTab = { name: 'PreferencesTab', template: '<div class="tab">preferences</div>' }

  async function mountAt(path) {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{
        path: '/account/:userId', name: 'account', component: ProfileUser, children: [
          { path: 'profile', name: 'accountProfile', component: ProfileTab },
          { path: 'preferences', name: 'accountPreferences', component: PreferencesTab }
        ]
      }]
    })
    router.push(path)
    await router.isReady()
    // VTU mounts into a root of its own, which the page reads the signed-in user and the config from
    const App = {
      created() { Object.assign(this.$root, { user: { id: 'ada' }, config: {} }) },
      template: '<router-view></router-view>'
    }
    const wrapper = mount(App, {
      global: {
        plugins: [router],
        stubs: {
          SidebarsFlow: { template: '<div><slot name="left"></slot><slot></slot></div>' },
          'b-list-group': { template: '<ul><slot></slot></ul>' },
          'b-list-group-item': { props: ['to', 'active'], template: '<li :class="{ active }"><slot></slot></li>' },
          'b-button': true,
          'b-modal': true,
          SuccessAlert: true
        },
        mocks: translationStubs
      }
    })
    await flushPromises()
    return { wrapper, router }
  }

  it('shows the open tab in its router view with the user, and marks it in the menu', async () => {
    AdminService.findUsers.mockResolvedValue([ADA])
    const { wrapper } = await mountAt('/account/ada/profile')

    const tab = wrapper.findComponent(ProfileTab)
    expect(tab.props()).toEqual({ user: ADA, editMode: false })
    expect(wrapper.find('li.active').text()).toBe('admin.users.profile')
  })

  it('opens the preferences of a user the engine has no details of', async () => {
    AdminService.findUsers.mockResolvedValue([])
    const { wrapper, router } = await mountAt('/account/ada/profile')

    expect(router.currentRoute.value.name).toBe('accountPreferences')
    expect(wrapper.findAll('li').map(li => li.text())).toEqual(['admin.preferences.title'])
    expect(wrapper.find('.tab').text()).toBe('preferences')
  })
})
