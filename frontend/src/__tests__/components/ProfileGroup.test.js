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
import { AdminService } from '@/services.js'
import ProfileGroup from '@/components/admin/ProfileGroup.vue'
import { createComponentContext } from '../support/callWithContext.js'

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

  it('loads the group again when the route names another one', () => {
    const vm = { $route: { params: { groupId: 'hr' } }, loadGroup: vi.fn() }
    ProfileGroup.watch['$route.params.groupId'].call(vm)
    expect(vm.loadGroup).toHaveBeenCalledWith('hr')
  })
})
