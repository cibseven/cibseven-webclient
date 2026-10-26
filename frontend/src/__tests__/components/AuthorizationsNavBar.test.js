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
import { mount } from '@vue/test-utils'
import AuthorizationsNavBar from '@/components/admin/AuthorizationsNavBar.vue'

const { getResourceTypeGroup, getResourceIndex, isActive, editRoute } = AuthorizationsNavBar.methods
const { groups } = AuthorizationsNavBar.computed

function grouped(resourcesTypes) {
  return groups.call({ resourceTypes: resourcesTypes, getResourceTypeGroup, getResourceIndex })
}

function type(id, key) {
  return { id: String(id), key }
}

const slotStub = { template: '<div><slot /></div>' }

// VTU wraps the component in its own root, so $root.config is not reachable;
// override the single computed that reads it instead.
function mountNavBar(resourcesTypes, path = '/seven/auth/admin/authorizations') {
  const NavBar = {
    ...AuthorizationsNavBar,
    computed: { ...AuthorizationsNavBar.computed, resourceTypes: () => resourcesTypes }
  }
  return mount(NavBar, {
    global: {
      mocks: { $t: key => key, $route: { path } },
      stubs: { BListGroup: slotStub, BListGroupItem: slotStub }
    }
  })
}

describe('AuthorizationsNavBar', () => {
  describe('getResourceTypeGroup', () => {
    it.each([
      ['application', 'application'],
      ['user', 'admin'],
      ['group', 'admin'],
      ['tenant', 'admin'],
      ['authorization', 'authorizations'],
      ['groupMembership', 'authorizations'],
      ['tenantMembership', 'authorizations'],
      ['processDefinition', 'processes'],
      ['batch', 'processes'],
      ['task', 'tasks'],
      ['filter', 'tasks'],
      ['system', 'system']
    ])('maps %s to %s', (key, expected) => {
      expect(getResourceTypeGroup(key)).toBe(expected)
    })

    it('falls back to other for unknown keys', () => {
      expect(getResourceTypeGroup('somethingNew')).toBe('other')
    })
  })

  describe('getResourceIndex', () => {
    it('follows the declaration order of the mapping', () => {
      expect(getResourceIndex('application')).toBeLessThan(getResourceIndex('user'))
      expect(getResourceIndex('user')).toBeLessThan(getResourceIndex('system'))
    })

    it('puts unknown keys last', () => {
      expect(getResourceIndex('somethingNew')).toBe(Number.MAX_SAFE_INTEGER)
    })
  })

  describe('groups', () => {
    it('orders groups as application, admin, authorizations, processes, tasks, other, system', () => {
      const result = grouped({
        '1': type(1, 'system'),
        '2': type(2, 'somethingNew'),
        '3': type(3, 'task'),
        '4': type(4, 'processDefinition'),
        '5': type(5, 'authorization'),
        '6': type(6, 'user'),
        '7': type(7, 'application')
      })
      expect(result.map(group => group.name))
        .toEqual(['application', 'admin', 'authorizations', 'processes', 'tasks', 'other', 'system'])
    })

    it('omits groups without resource types', () => {
      const result = grouped({ '1': type(1, 'user') })
      expect(result.map(group => group.name)).toEqual(['admin'])
    })

    it('sorts resource types inside a group by the mapping order, not the config order', () => {
      const result = grouped({
        '1': type(1, 'filter'),
        '2': type(2, 'historicTask'),
        '3': type(3, 'task')
      })
      expect(result).toHaveLength(1)
      expect(result[0].resourceTypes.map(resourceType => resourceType.key)).toEqual(['task', 'historicTask', 'filter'])
    })

    it('places unknown keys in other', () => {
      const result = grouped({
        '1': type(1, 'somethingNew'),
        '2': type(2, 'anotherNew')
      })
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('other')
      expect(result[0].resourceTypes.map(resourceType => resourceType.key)).toEqual(['somethingNew', 'anotherNew'])
    })

    it.each(['report', 'dashboard', 'operationLogCategory'])('has no dedicated group for the removed %s resource type', key => {
      expect(getResourceIndex(key)).toBe(Number.MAX_SAFE_INTEGER)
    })

    it('returns no groups when no resource types are configured', () => {
      expect(grouped(undefined)).toEqual([])
    })
  })

  describe('route helpers', () => {
    it('isActive matches the id and key at the end of the path', () => {
      const ctx = { $route: { path: '/seven/auth/admin/authorizations/2/group' } }
      expect(isActive.call(ctx, type(2, 'group'))).toBe(true)
      expect(isActive.call(ctx, type(1, 'user'))).toBe(false)
    })

    it('editRoute targets the authorizationType route', () => {
      expect(editRoute(type(2, 'group'))).toEqual({
        name: 'authorizationType',
        params: { resourceTypeId: '2', resourceTypeKey: 'group' }
      })
    })
  })

  describe('rendering', () => {
    const resourcesTypes = {
      '1': type(1, 'user'),
      '2': type(2, 'group'),
      '8': type(8, 'task'),
      '12': type(12, 'system')
    }

    it('renders one item per resource type', () => {
      const wrapper = mountNavBar(resourcesTypes)
      const labels = wrapper.findAll('span').map(span => span.text())
      expect(labels).toEqual([
        'admin.authorizations.resourcesTypes.user',
        'admin.authorizations.resourcesTypes.group',
        'admin.authorizations.resourcesTypes.task',
        'admin.authorizations.resourcesTypes.system'
      ])
    })

    it('renders a separator between groups but not before the first one', () => {
      const wrapper = mountNavBar(resourcesTypes)
      expect(wrapper.findAll('hr')).toHaveLength(2)
    })

    it('renders no separator for a single group', () => {
      const wrapper = mountNavBar({ '1': type(1, 'user'), '2': type(2, 'group') })
      expect(wrapper.findAll('hr')).toHaveLength(0)
    })
  })
})
