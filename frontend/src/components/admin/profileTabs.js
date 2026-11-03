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
import ProfileUserInformationTab from '@/components/admin/profile/ProfileUserInformationTab.vue'
import ProfileUserPasswordTab from '@/components/admin/profile/ProfileUserPasswordTab.vue'
import ProfileUserGroupsTab from '@/components/admin/profile/ProfileUserGroupsTab.vue'
import ProfileUserTenantsTab from '@/components/admin/profile/ProfileUserTenantsTab.vue'
import ProfilePreferencesTab from '@/components/admin/ProfilePreferencesTab.vue'
import ProfileGroupInformationTab from '@/components/admin/profile/ProfileGroupInformationTab.vue'
import ProfileGroupUsersTab from '@/components/admin/profile/ProfileGroupUsersTab.vue'
import ProfileGroupTenantsTab from '@/components/admin/profile/ProfileGroupTenantsTab.vue'
import { profileTabRouteName } from '@/components/admin/utils.js'

// The routes of the pages that show each kind of tab
const PROFILE_PAGES = {
  user: ['account', 'adminUser'],
  group: ['adminGroup']
}

const hasDetails = ({ user }) => !user.noInfo

// The tabs of the user and group pages, in menu order, read by the router, the pages and the account menu. A tab has
// an id (its route's path), a label (menuLabel/menuTooltip where the account menu differs), a component, an optional
// beforeEnter guard and props(page), and show(context). The context is the page, or the account menu's object with
// the same fields: user, editMode and ownAccount (group on the group page), config and applicationPermissions.
const BUILT_IN_TABS = {
  user: [
    {
      id: 'profile', label: 'admin.users.profile', menuLabel: 'start.account.profile.title',
      menuTooltip: 'start.account.profile.tooltip', component: ProfileUserInformationTab, show: hasDetails
    },
    {
      id: 'password', label: 'password.recover.changePassword', component: ProfileUserPasswordTab,
      show: ({ user, config }) => !user.noInfo && !!(config.userEditable && config.userPasswordChangeEnabled)
    },
    { id: 'groups', label: 'admin.users.groups', component: ProfileUserGroupsTab, show: hasDetails },
    { id: 'tenants', label: 'admin.tenants.title', component: ProfileUserTenantsTab, show: hasDetails },
    {
      id: 'preferences', label: 'admin.preferences.title', component: ProfilePreferencesTab,
      show: ({ ownAccount }) => ownAccount, props: () => ({})
    }
  ],
  group: [
    { id: 'information', label: 'admin.groups.information', component: ProfileGroupInformationTab, show: () => true },
    { id: 'users', label: 'admin.groups.users', component: ProfileGroupUsersTab, show: () => true },
    { id: 'tenants', label: 'admin.tenants.title', component: ProfileGroupTenantsTab, show: () => true }
  ]
}

const PROFILE_TABS = {}

// Drops the registered tabs, e.g. between tests
export function resetProfileTabs() {
  Object.entries(BUILT_IN_TABS).forEach(([kind, tabs]) => { PROFILE_TABS[kind] = [...tabs] })
}
resetProfileTabs()

// A copy: change the list with registerProfileTab
export function profileTabs(kind) {
  return [...PROFILE_TABS[kind]]
}

function profileTabRoute(page, tab) {
  const route = { path: tab.id, name: profileTabRouteName(page, tab.id), component: tab.component }
  if (tab.beforeEnter) route.beforeEnter = tab.beforeEnter
  return route
}

// The child routes of a page's tabs, e.g. 'preferences' named 'accountPreferences' under 'account'
export function profileTabRoutes(page, kind) {
  return PROFILE_TABS[kind].map(tab => profileTabRoute(page, tab))
}

// Adds the routes of tabs registered after appRoutes was built, under the pages the router has. createAppRouter calls
// it; a route of a tab whose component was replaced is added again, which replaces it.
export function addProfileTabRoutes(router) {
  const components = new Map(router.getRoutes().map(route => [route.name, route.components?.default]))
  Object.entries(PROFILE_PAGES).forEach(([kind, pages]) => {
    pages.filter(page => router.hasRoute(page)).forEach(page => {
      PROFILE_TABS[kind].forEach(tab => {
        const route = profileTabRoute(page, tab)
        if (components.get(route.name) !== tab.component) router.addRoute(page, route)
      })
    })
  })
}

// Puts a tab before the one named `before` (else last), or in place of the tab with its id. Call it before
// createAppRouter.
export function registerProfileTab(kind, tab, { before } = {}) {
  const tabs = PROFILE_TABS[kind]
  const taken = tabs.findIndex(other => other.id === tab.id)
  if (taken >= 0) {
    tabs.splice(taken, 1, tab)
  } else {
    const index = tabs.findIndex(other => other.id === before)
    tabs.splice(index >= 0 ? index : tabs.length, 0, tab)
  }
}
