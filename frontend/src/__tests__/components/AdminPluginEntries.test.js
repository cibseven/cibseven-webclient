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
  const admin = (adminPluginEntries, overrides = {}) => ({
    $t: key => `t:${key}`,
    permissionsUsers: true,
    permissionsUsersManagement: true, permissionsGroupsManagement: false, permissionsTenantsManagement: false,
    permissionsAuthorizationsManagement: false, permissionsSystemManagement: false,
    adminPluginEntries,
    ...overrides
  })

  describe('StartView admin tile', () => {
    const adminOptions = (adminPluginEntries) => {
      const vm = admin(adminPluginEntries)
      vm.groupById = { admin: StartView.computed.navGroups.call({ ...vm, $options: {} }).find(g => g.id === 'admin') }
      return StartView.computed.adminOptions.call(vm)
    }

    it('lists a contributed entry after the built-in ones, with its icon and translated texts', () => {
      const options = adminOptions([notifications])

      expect(options.map(o => o.to)).toEqual(['/seven/auth/admin/users', '/seven/auth/admin/notifications'])
      expect(options.at(-1)).toEqual({
        to: '/seven/auth/admin/notifications', icon: 'mdi-bullhorn-outline', src: 'notifications.svg',
        title: 't:admin.notifications.title', tooltip: 't:admin.notifications.title'
      })
    })

    it('uses a given tooltip', () => {
      expect(adminOptions([{ ...notifications, tooltip: 'admin.notifications.tooltip' }]).at(-1).tooltip)
        .toBe('t:admin.notifications.tooltip')
    })
  })

  describe('UsersManagement cards', () => {
    const vmFor = (adminPluginEntries, overrides) => {
      const vm = admin(adminPluginEntries, overrides)
      vm.adminGroup = UsersManagement.computed.adminGroup.call(vm)
      vm.pluginItems = UsersManagement.computed.pluginItems.call(vm)
      vm.showAccessManagement = UsersManagement.computed.showAccessManagement.call(vm)
      vm.showSystem = UsersManagement.computed.showSystem.call(vm)
      return vm
    }

    it('shows a contributed card after the built-in ones, linking to its route', () => {
      const vm = vmFor([notifications], { permissionsUsersManagement: false, permissionsSystemManagement: true })

      expect(vm.showSystem).toBe(true)
      expect(vm.pluginItems).toHaveLength(1)
      expect(vm.pluginItems[0]).toMatchObject({ title: 'admin.notifications.title', tileImage: 'notifications.svg', to: '/seven/auth/admin/notifications' })
    })

    it('shows only the built-in cards when nothing is contributed', () => {
      expect(vmFor([], { permissionsSystemManagement: true }).pluginItems).toEqual([])
    })

    it('shows the admin page when only a contributed area is reachable', () => {
      const vm = vmFor([notifications], { permissionsUsersManagement: false })

      expect(vm.showAccessManagement).toBe(false)
      expect(UsersManagement.computed.hasTiles.call(vm)).toBe(true)
    })
  })
})
