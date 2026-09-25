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
import StartView from '@/components/start/StartView.vue'
import UsersManagement from '@/components/admin/UsersManagement.vue'

// An area an extension adds to the admin navigation (see navigationPermissionsMixin.ADMIN_ENTRY_SLOT)
const notifications = {
  id: 'notifications', text: 'admin.notifications.title', icon: 'mdi-bullhorn-outline',
  image: 'notifications.svg', to: '/seven/auth/admin/notifications'
}

describe('contributed admin entries', () => {
  describe('StartView admin tile', () => {
    const adminOptions = (adminPluginEntries) => StartView.computed.adminOptions.call({
      $t: key => `t:${key}`,
      permissionsUsersManagement: true, permissionsGroupsManagement: false, permissionsTenantsManagement: false,
      permissionsAuthorizationsManagement: false, permissionsSystemManagement: false,
      adminPluginEntries
    })

    it('lists a contributed entry after the built-in ones, with its icon and translated texts', () => {
      const options = adminOptions([notifications])

      expect(options.map(o => o.to)).toEqual(['/seven/auth/admin/users', '/seven/auth/admin/notifications'])
      expect(options.at(-1)).toEqual({
        to: '/seven/auth/admin/notifications', icon: 'mdi-bullhorn-outline',
        title: 't:admin.notifications.title', tooltip: 't:admin.notifications.title'
      })
    })

    it('uses a given tooltip', () => {
      expect(adminOptions([{ ...notifications, tooltip: 'admin.notifications.tooltip' }]).at(-1).tooltip)
        .toBe('t:admin.notifications.tooltip')
    })
  })

  describe('UsersManagement cards', () => {
    const items = (adminPluginEntries) => UsersManagement.computed.items.call({
      $root: { config: { permissions: {} } },
      // only the built-in System card is granted, so the list shows both kinds
      applicationPermissions: (permissions, resource) => resource === 'system',
      adminPluginEntries
    })

    it('shows a contributed card after the built-in ones, linking to its route', () => {
      const cards = items([notifications])

      expect(cards.map(c => c.title)).toEqual(['admin.system.title', 'admin.notifications.title'])
      expect(cards.at(-1)).toMatchObject({ image: 'notifications.svg', link: '/seven/auth/admin/notifications' })
    })

    it('shows only the built-in cards when nothing is contributed', () => {
      expect(items([]).map(c => c.title)).toEqual(['admin.system.title'])
    })
  })
})
