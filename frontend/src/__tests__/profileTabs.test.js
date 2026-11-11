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
import { describe, it, expect, afterEach } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { profileTabs, profileTabRoutes, registerProfileTab, addProfileTabRoutes, resetProfileTabs } from '@/components/admin/profileTabs.js'

const ADA = { id: 'ada', firstName: 'Ada' }
const CONFIG = { userEditable: true, userPasswordChangeEnabled: true }
const Extra = { name: 'Extra', template: '<div></div>' }

// The tabs as the page or the account menu offers them in the given context
function shown(kind, context) {
  return profileTabs(kind).filter(tab => tab.show(context)).map(tab => tab.id)
}

afterEach(resetProfileTabs)

describe('profile tabs', () => {
  it('offers every user tab on the own account of a user the engine knows', () => {
    expect(shown('user', { user: ADA, editMode: false, ownAccount: true, config: CONFIG }))
      .toEqual(['profile', 'password', 'groups', 'tenants', 'preferences'])
  })

  // e.g. from SSO, or a user who may not read their own details
  it('offers only the preferences of a user the engine has no details of', () => {
    expect(shown('user', { user: { id: 'ada', noInfo: true }, ownAccount: true, config: CONFIG })).toEqual(['preferences'])
  })

  it('offers the preferences only on the own account, and the password change only when switched on', () => {
    expect(shown('user', { user: ADA, editMode: true, ownAccount: false, config: CONFIG })).not.toContain('preferences')
    expect(shown('user', { user: ADA, ownAccount: true, config: { userEditable: true } })).not.toContain('password')
    expect(shown('user', { user: ADA, ownAccount: true, config: { userPasswordChangeEnabled: true } })).not.toContain('password')
  })

  it('offers every group tab', () => {
    expect(shown('group', { group: { id: 'sales' } })).toEqual(['information', 'users', 'tenants'])
  })

  it('gives the account menu its own label for the profile', () => {
    expect(profileTabs('user')[0]).toMatchObject({ label: 'admin.users.profile', menuLabel: 'start.account.profile.title' })
  })

  it('gives a page one child route per tab, named after the page and the tab', () => {
    const routes = profileTabRoutes('adminGroup', 'group')
    expect(routes.map(({ path, name }) => [path, name])).toEqual([
      ['information', 'adminGroupInformation'], ['users', 'adminGroupUsers'], ['tenants', 'adminGroupTenants']
    ])
    expect(routes[0].component).toBe(profileTabs('group')[0].component)
    expect(routes[0]).not.toHaveProperty('beforeEnter')
  })

  // e.g. a tab only authorization admins may open
  it('guards the route of a tab with its beforeEnter', () => {
    const guard = () => true
    registerProfileTab('group', { id: 'extra', label: 'extra', component: Extra, show: () => true, beforeEnter: guard })
    expect(profileTabRoutes('adminGroup', 'group').at(-1)).toMatchObject({ name: 'adminGroupExtra', beforeEnter: guard })
  })
})

describe('registerProfileTab', () => {
  const extra = { id: 'extra', label: 'extra', component: Extra, show: () => true }

  it('puts a tab last, or before the one named', () => {
    registerProfileTab('group', extra)
    expect(profileTabs('group').map(tab => tab.id)).toEqual(['information', 'users', 'tenants', 'extra'])

    registerProfileTab('user', extra, { before: 'preferences' })
    expect(profileTabs('user').map(tab => tab.id)).toEqual(['profile', 'password', 'groups', 'tenants', 'extra', 'preferences'])
  })

  it('puts a tab last when the one named is not there', () => {
    registerProfileTab('group', extra, { before: 'unknown' })
    expect(profileTabs('group').at(-1)).toBe(extra)
  })

  it('replaces the tab with the same id in its place', () => {
    const tenants = { ...extra, id: 'tenants' }
    registerProfileTab('group', tenants)
    expect(profileTabs('group').map(tab => tab.id)).toEqual(['information', 'users', 'tenants'])
    expect(profileTabs('group')[2]).toBe(tenants)
  })

  it('leaves the list alone when a caller changes the copy it got', () => {
    profileTabs('group').push(extra)
    profileTabs('group').splice(0, Infinity)
    expect(profileTabs('group').map(tab => tab.id)).toEqual(['information', 'users', 'tenants'])
  })
})

describe('resetProfileTabs', () => {
  it('drops the registered tabs and brings back the replaced ones', () => {
    const information = profileTabs('group')[0]
    registerProfileTab('group', { id: 'information', label: 'other', component: Extra, show: () => true })
    registerProfileTab('user', { id: 'extra', label: 'extra', component: Extra, show: () => true })
    resetProfileTabs()
    expect(profileTabs('group')[0]).toBe(information)
    expect(profileTabs('user').map(tab => tab.id)).toEqual(['profile', 'password', 'groups', 'tenants', 'preferences'])
  })
})

describe('addProfileTabRoutes', () => {
  // A router with the group page and its built-in tabs, as createAppRouter gives it
  function groupRouter() {
    return createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/group/:groupId', name: 'adminGroup', component: Extra, children: profileTabRoutes('adminGroup', 'group') }]
    })
  }
  const component = (router, name) => router.getRoutes().find(route => route.name === name).components.default

  it('gives a tab registered later its routes under the pages the router has', () => {
    const router = groupRouter()
    registerProfileTab('group', { id: 'extra', label: 'extra', component: Extra, show: () => true })
    addProfileTabRoutes(router)
    expect(router.hasRoute('adminGroupExtra')).toBe(true)
    expect(router.resolve({ name: 'adminGroupExtra', params: { groupId: 'sales' } }).path).toBe('/group/sales/extra')
    // The router has no user pages to add the user tabs to
    expect(router.hasRoute('accountProfile')).toBe(false)
  })

  it('swaps the component of a replaced tab, and leaves the other routes alone', () => {
    const router = groupRouter()
    const users = component(router, 'adminGroupUsers')
    registerProfileTab('group', { id: 'tenants', label: 'tenants', component: Extra, show: () => true })
    addProfileTabRoutes(router)
    expect(component(router, 'adminGroupTenants')).toBe(Extra)
    expect(component(router, 'adminGroupUsers')).toBe(users)
    expect(router.getRoutes().filter(route => route.name === 'adminGroupTenants')).toHaveLength(1)
  })
})
