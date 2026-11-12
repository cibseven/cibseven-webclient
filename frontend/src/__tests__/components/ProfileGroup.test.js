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
import ProfileGroup from '@/components/admin/ProfileGroup.vue'
import WarningBox from '@/components/common-components/WarningBox.vue'
import { createComponentContext } from '../support/callWithContext.js'
import { translationStubs } from '../support/mountWithDefaults.js'

vi.mock('@/services.js', () => ({
  AdminService: { findGroups: vi.fn() }
}))

const SALES = { id: 'sales', name: 'Sales', type: 'WORKFLOW' }

function context(overrides = {}) {
  const vm = createComponentContext({
    ...ProfileGroup.data.call({}),
    $route: { name: 'adminGroupInformation', params: { groupId: 'sales' }, query: {} },
    ...overrides
  })
  for (const [name, method] of Object.entries(ProfileGroup.methods)) {
    if (!(name in vm)) vm[name] = method.bind(vm)
  }
  for (const [name, def] of Object.entries(ProfileGroup.computed)) {
    if (!(name in vm)) Object.defineProperty(vm, name, { get: () => def.call(vm), configurable: true })
  }
  return vm
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('ProfileGroup tabs', () => {
  it('offers the information, the users and the tenants of the group', () => {
    expect(context().visibleTabs.map(tab => tab.id)).toEqual(['information', 'users', 'tenants'])
  })

  it.each([
    ['adminGroupUsers', 'users'],
    ['adminGroup', undefined],
    ['adminUserTenants', undefined]
  ])('reads the route %s as the tab %s', (name, tab) => {
    expect(context({ $route: { name, params: {} } }).selectedTab?.id).toBe(tab)
  })

  it('links each tab to its route under the page', () => {
    expect(context().tabRoute({ id: 'users' })).toEqual({ name: 'adminGroupUsers', params: { groupId: 'sales' } })
  })

  it('gives a tab the group, and an added tab its own props', () => {
    const vm = context({ group: { ...SALES } })
    expect(vm.tabProps).toEqual({ group: vm.group, 'onUpdate:group': expect.any(Function) })
    vm.tabProps['onUpdate:group']({ ...SALES, name: 'Sales EMEA' })
    expect(vm.group.name).toBe('Sales EMEA')

    vm.tabs.push({ id: 'extra', label: 'extra', show: () => true, props: v => ({ groupId: v.group.id }) })
    vm.$route.name = 'adminGroupExtra'
    expect(vm.tabProps).toEqual({ groupId: 'sales' })
  })

  it('swaps a tab that is not shown for the first one that is', () => {
    const vm = context({ $route: { name: 'adminGroupExtra', params: { groupId: 'sales' } } })
    vm.tabs.push({ id: 'extra', label: 'extra', show: () => false })
    vm.openShownTab()
    expect(vm.$router.replace).toHaveBeenCalledWith({ name: 'adminGroupInformation', params: { groupId: 'sales' } })
  })

  it.each([
    ['a tab that is shown', 'adminGroupUsers'],
    ['a route that is no tab', 'adminGroup']
  ])('stays on %s', (_, name) => {
    const vm = context({ $route: { name, params: {} } })
    vm.openShownTab()
    expect(vm.$router.replace).not.toHaveBeenCalled()
  })

  it('checks the tab again when another tab opens, once the group is loaded', () => {
    const vm = { group: { id: null }, openShownTab: vi.fn() }
    ProfileGroup.watch['$route.name'].call(vm)
    expect(vm.openShownTab).not.toHaveBeenCalled()

    vm.group.id = 'sales'
    ProfileGroup.watch['$route.name'].call(vm)
    expect(vm.openShownTab).toHaveBeenCalledTimes(1)
  })
})

describe('ProfileGroup loadGroup', () => {
  it('loads the group, then checks the tab', async () => {
    AdminService.findGroups.mockResolvedValue([SALES])
    const vm = context({ openShownTab: vi.fn() })
    vm.loadGroup('sales')
    await vi.waitFor(() => expect(vm.openShownTab).toHaveBeenCalled())
    expect(AdminService.findGroups).toHaveBeenCalledWith({ id: 'sales' })
    expect(vm.group).toEqual(SALES)
  })

  it('marks a group the engine does not know as not found, without checking the tab', async () => {
    AdminService.findGroups.mockResolvedValue([])
    const vm = context({ openShownTab: vi.fn(), $route: { name: 'adminGroupInformation', params: { groupId: 'no-such-group' } } })
    await vm.loadGroup('no-such-group')
    expect(vm.notFound).toBe(true)
    expect(vm.group).toEqual({ id: null, name: null, type: null })
    expect(vm.openShownTab).not.toHaveBeenCalled()
  })

  it('clears the not found mark once the route names a group the engine knows', async () => {
    AdminService.findGroups.mockResolvedValue([SALES])
    const vm = context({ openShownTab: vi.fn(), notFound: true })
    await vm.loadGroup('sales')
    expect(vm.notFound).toBe(false)
    expect(vm.group).toEqual(SALES)
  })

  // Browser back from hr to sales, with the response for hr coming last
  it('ignores the response for a group the route no longer names', async () => {
    const HR = { id: 'hr', name: 'HR', type: 'WORKFLOW' }
    let resolveHr
    AdminService.findGroups
      .mockReturnValueOnce(new Promise(resolve => { resolveHr = resolve }))
      .mockResolvedValueOnce([SALES])
    const vm = context({ openShownTab: vi.fn(), $route: { name: 'adminGroupInformation', params: { groupId: 'hr' } } })
    const hr = vm.loadGroup('hr')
    vm.$route.params.groupId = 'sales'
    await vm.loadGroup('sales')
    resolveHr([HR])
    await hr
    expect(vm.group).toEqual(SALES)
    expect(vm.openShownTab).toHaveBeenCalledTimes(1)
  })

  it('loads the group again when the route names another one', () => {
    const vm = { $route: { params: { groupId: 'hr' } }, loadGroup: vi.fn() }
    ProfileGroup.watch['$route.params.groupId'].call(vm)
    expect(vm.loadGroup).toHaveBeenCalledWith('hr')
  })
})

describe('ProfileGroup rendered', () => {
  const InformationTab = { name: 'InformationTab', props: ['group'], template: '<div class="tab">{{ group.name }}</div>' }

  async function mountAt(path) {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/groups', name: 'adminGroups', component: { template: '<div></div>' } },
        {
          path: '/group/:groupId', name: 'adminGroup', component: ProfileGroup, children: [
            { path: 'information', name: 'adminGroupInformation', component: InformationTab }
          ]
        }
      ]
    })
    router.push(path)
    await router.isReady()
    const wrapper = mount({ template: '<router-view></router-view>' }, {
      global: {
        plugins: [router],
        stubs: {
          SidebarsFlow: { props: ['leftCaption'], template: '<div><h1>{{ leftCaption }}</h1><slot name="left"></slot><slot></slot></div>' },
          'b-list-group': { template: '<ul><slot></slot></ul>' },
          'b-list-group-item': { props: ['to', 'active'], template: '<li :class="{ active }"><slot></slot></li>' },
          'b-button': { template: '<a class="back"><slot></slot></a>' }
        },
        mocks: translationStubs
      }
    })
    await flushPromises()
    return wrapper
  }

  it('shows the open tab with the group', async () => {
    AdminService.findGroups.mockResolvedValue([SALES])
    const wrapper = await mountAt('/group/sales/information')
    expect(wrapper.find('h1').text()).toBe('admin.groups.title - sales')
    expect(wrapper.find('.tab').text()).toBe('Sales')
    expect(wrapper.findComponent(WarningBox).exists()).toBe(false)
  })

  it('tells that a group the engine does not know does not exist, and leads back to the list', async () => {
    AdminService.findGroups.mockResolvedValue([])
    const wrapper = await mountAt('/group/no-such-group/information')
    expect(wrapper.find('h1').text()).toBe('admin.groups.title - no-such-group')
    expect(wrapper.findComponent(WarningBox).props('message')).toBe('admin.groups.notFound')
    expect(wrapper.find('a.back').exists()).toBe(true)
    expect(wrapper.find('.tab').exists()).toBe(false)
    expect(wrapper.findAll('li')).toHaveLength(0)
  })
})
