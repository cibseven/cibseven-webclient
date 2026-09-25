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
import { describe, it, expect, vi, afterEach } from 'vitest'
import navigationPermissionsMixin, { ADMIN_ENTRY_SLOT } from '@/mixins/navigationPermissionsMixin.js'
import { registerPlugin, resetPlugins } from '@/plugins/pluginsConfig.js'

describe('navigationPermissionsMixin', () => {
  describe('permissionsUserProfile', () => {
    // Regression test: the router's 'account' route guard (permissionsGuard('userProfile'))
    // and the account nav link in CibSeven.vue must agree on the same allow-list check,
    // both driven by this computed property.
    it('is granted when applicationPermissions allows userProfile', () => {
      const mockThis = {
        $root: {
          user: { id: '1' },
          config: { permissions: { userProfile: { user: ['*'] } } }
        },
        applicationPermissions: (permissions, access) => permissions && access === 'userProfile'
      }

      expect(navigationPermissionsMixin.computed.permissionsUserProfile.call(mockThis)).toBe(true)
    })

    it('is denied when userProfile is not explicitly granted', () => {
      const mockThis = {
        $root: {
          user: { id: '1' },
          config: { permissions: {} }
        },
        applicationPermissions: () => false
      }

      expect(navigationPermissionsMixin.computed.permissionsUserProfile.call(mockThis)).toBe(false)
    })

    it('is falsy when there is no logged-in user', () => {
      const mockThis = {
        $root: {
          user: null,
          config: { permissions: { userProfile: { user: ['*'] } } }
        },
        applicationPermissions: () => true
      }

      expect(navigationPermissionsMixin.computed.permissionsUserProfile.call(mockThis)).toBeFalsy()
    })
  })

  // The remaining nine checks share one shape: require a logged-in user, then delegate to
  // `applicationPermissions` with the config slice and the access key the router guards
  // use. The table pins each property to its own slice and key, so a copy-paste slip
  // between two of them (e.g. tenant vs. group) fails here.
  describe.each([
    ['permissionsTaskList', 'tasklist', 'tasklist'],
    ['permissionsCockpit', 'cockpit', 'cockpit'],
    ['permissionsModeler', 'modeler', 'modeler'],
    ['permissionsUsersManagement', 'usersManagement', 'user'],
    ['permissionsGroupsManagement', 'groupsManagement', 'group'],
    ['permissionsTenantsManagement', 'tenantsManagement', 'tenant'],
    ['permissionsAuthorizationsManagement', 'authorizationsManagement', 'authorization'],
    ['permissionsSystemManagement', 'systemManagement', 'system']
  ])('%s', (property, configKey, accessKey) => {
    const contextWith = (applicationPermissions, config = {}) => ({
      $root: {
        user: { id: '1' },
        config: { permissions: { [configKey]: { user: ['*'] } }, ...config }
      },
      applicationPermissions
    })

    it('passes the matching config slice and access key to applicationPermissions', () => {
      const applicationPermissions = vi.fn(() => true)

      const result = navigationPermissionsMixin.computed[property].call(contextWith(applicationPermissions))

      expect(applicationPermissions).toHaveBeenCalledWith({ user: ['*'] }, accessKey)
      expect(result).toBe(true)
    })

    it('is denied when applicationPermissions refuses', () => {
      expect(navigationPermissionsMixin.computed[property].call(contextWith(() => false))).toBe(false)
    })

    it('is falsy when there is no logged-in user', () => {
      const mockThis = contextWith(() => true)
      mockThis.$root.user = null

      expect(navigationPermissionsMixin.computed[property].call(mockThis)).toBeFalsy()
    })
  })

  // The modeler has an extra kill switch on top of the permission check.
  describe('permissionsModeler', () => {
    const contextWith = (modelerEnabled) => ({
      $root: {
        user: { id: '1' },
        config: { modelerEnabled, permissions: { modeler: { user: ['*'] } } }
      },
      applicationPermissions: () => true
    })

    it('is denied when the modeler is switched off', () => {
      expect(navigationPermissionsMixin.computed.permissionsModeler.call(contextWith(false))).toBe(false)
    })

    it.each([[true], [undefined]])('is granted when modelerEnabled is %j', (modelerEnabled) => {
      expect(navigationPermissionsMixin.computed.permissionsModeler.call(contextWith(modelerEnabled))).toBe(true)
    })
  })

  // Users is the odd one out: it asks whether the user may reach *any* admin section.
  describe('permissionsUsers', () => {
    it('delegates to hasAdminManagementPermissions with the whole permissions config', () => {
      const hasAdminManagementPermissions = vi.fn(() => true)
      const permissions = { usersManagement: { user: ['*'] } }
      const mockThis = {
        $root: { user: { id: '1' }, config: { permissions } },
        hasAdminManagementPermissions
      }

      const result = navigationPermissionsMixin.computed.permissionsUsers.call(mockThis)

      expect(hasAdminManagementPermissions).toHaveBeenCalledWith(permissions)
      expect(result).toBe(true)
    })

    it('is denied when no admin section is reachable', () => {
      const mockThis = {
        $root: { user: { id: '1' }, config: { permissions: {} } },
        hasAdminManagementPermissions: () => false
      }

      expect(navigationPermissionsMixin.computed.permissionsUsers.call(mockThis)).toBe(false)
    })

    it('is falsy when there is no logged-in user', () => {
      const mockThis = {
        $root: { user: null, config: { permissions: {} } },
        hasAdminManagementPermissions: () => true
      }

      expect(navigationPermissionsMixin.computed.permissionsUsers.call(mockThis)).toBeFalsy()
    })

    // Someone whose only admin area is a contributed one (e.g. system notifications) must still
    // reach the admin page, or the entry would be unreachable
    it('is granted through a contributed admin entry alone', () => {
      const mockThis = {
        $root: { user: { id: '1' }, config: { permissions: {} } },
        hasAdminManagementPermissions: () => false,
        adminPluginEntries: [{ id: 'notifications' }]
      }

      expect(navigationPermissionsMixin.computed.permissionsUsers.call(mockThis)).toBe(true)
    })
  })

  describe('adminPluginEntries', () => {
    afterEach(() => resetPlugins())

    const entry = (overrides = {}) => ({
      id: 'notifications', text: 'admin.notifications.title', to: '/seven/auth/admin/notifications',
      permissions: { system: ['READ'] }, resource: 'system', ...overrides
    })

    const context = (applicationPermissions, user = { id: '1' }) => ({ $root: { user }, applicationPermissions })

    it('lists the registered entries the user is allowed to open, in registration order', () => {
      registerPlugin(ADMIN_ENTRY_SLOT, null, entry())
      registerPlugin(ADMIN_ENTRY_SLOT, null, entry({ id: 'reports', text: 'admin.reports.title', to: '/seven/auth/admin/reports',
        permissions: { report: ['READ'] }, resource: 'report' }))
      registerPlugin(ADMIN_ENTRY_SLOT, null, entry({ id: 'forbidden', permissions: { system: ['ALL'] } }))
      const applicationPermissions = vi.fn((permissions) => !permissions.system?.includes('ALL'))

      const result = navigationPermissionsMixin.computed.adminPluginEntries.call(context(applicationPermissions))

      expect(result.map(e => e.id)).toEqual(['notifications', 'reports'])
      expect(applicationPermissions).toHaveBeenCalledWith({ system: ['READ'] }, 'system')
    })

    it('ignores an entry without a route or a title, which could not be rendered', () => {
      registerPlugin(ADMIN_ENTRY_SLOT, null, entry({ id: 'no-route', to: undefined }))
      registerPlugin(ADMIN_ENTRY_SLOT, null, entry({ id: 'no-title', text: undefined }))

      expect(navigationPermissionsMixin.computed.adminPluginEntries.call(context(() => true))).toEqual([])
    })

    it('is empty without a logged-in user', () => {
      registerPlugin(ADMIN_ENTRY_SLOT, null, entry())

      expect(navigationPermissionsMixin.computed.adminPluginEntries.call(context(() => true, null))).toEqual([])
    })
  })
})
