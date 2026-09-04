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
import navigationPermissionsMixin from '@/mixins/navigationPermissionsMixin.js'

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
})
