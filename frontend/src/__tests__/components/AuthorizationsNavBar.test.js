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
import AuthorizationsNavBar from '@/components/admin/AuthorizationsNavBar.vue'

const { getResourceTypeGroup } = AuthorizationsNavBar.methods
const { groupedResourceTypes } = AuthorizationsNavBar.computed

function grouped(resourcesTypes) {
  return groupedResourceTypes.call({ resourceTypes: resourcesTypes, getResourceTypeGroup })
}

describe('AuthorizationsNavBar', () => {
  describe('getResourceTypeGroup', () => {
    it.each([
      ['user', 'admin'],
      ['authorization', 'admin'],
      ['processDefinition', 'cockpit'],
      ['batch', 'cockpit'],
      ['task', 'tasklist'],
      ['filter', 'tasklist'],
      ['optimize', 'optimize'],
      ['report', 'optimize']
    ])('maps %s to %s', (key, expected) => {
      expect(getResourceTypeGroup({ key })).toBe(expected)
    })

    it('falls back to admin for unknown keys', () => {
      expect(getResourceTypeGroup({ key: 'somethingNew' })).toBe('other')
    })
  })

  describe('groupedResourceTypes', () => {
    it('orders groups as admin, cockpit, tasklist, modeler, optimize', () => {
      const result = grouped({
        '0': { id: '0', key: 'optimize' },
        '1': { id: '1', key: 'task' },
        '2': { id: '2', key: 'processDefinition' },
        '3': { id: '3', key: 'user' }
      })
      expect(result.map(group => group.name)).toEqual(['admin', 'cockpit', 'tasklist', 'optimize'])
    })

    it('omits groups without resource types', () => {
      const result = grouped({ '1': { id: '1', key: 'user' } })
      expect(result.map(group => group.name)).toEqual(['admin'])
    })

    it('keeps the config order of resource types inside a group', () => {
      const result = grouped({
        '1': { id: '1', key: 'user' },
        '2': { id: '2', key: 'group' },
        '3': { id: '3', key: 'task' }
      })
      expect(result[0].resourceTypes.map(resourceType => resourceType.key)).toEqual(['user', 'group'])
      expect(result[1].resourceTypes.map(resourceType => resourceType.key)).toEqual(['task'])
    })

    it('returns no groups when no resource types are configured', () => {
      expect(grouped(undefined)).toEqual([])
    })
  })
})
