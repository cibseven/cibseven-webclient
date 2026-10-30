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
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { AdminService } from '@/services.js'
import IdentityAuthorizations from '@/components/admin/IdentityAuthorizations.vue'
import ProfileUser from '@/components/admin/ProfileUser.vue'
import ProfileGroup from '@/components/admin/ProfileGroup.vue'
import { mountWithDefaults } from '../support/mountWithDefaults.js'

vi.mock('@/services.js', () => ({
  AdminService: { findAuthorizations: vi.fn(() => Promise.resolve([])) },
  SetupService: {}
}))

const CONFIG = {
  admin: {
    resourcesTypes: {
      '0': { id: '0', key: 'application', permissions: ['ACCESS'] },
      '1': { id: '1', key: 'user', permissions: ['READ'] },
      '6': { id: '6', key: 'processDefinition', permissions: ['READ'] }
    },
    // Deployment configuration has carried the ids both as numbers and as strings.
    types: { '0': { id: 0, key: 'global' }, '1': { id: '1', key: 'allow' }, '2': { id: 2, key: 'deny' } }
  }
}

const FILTER = 5

function store() {
  return { state: { filter: { list: [{ id: 'f1', name: 'My tasks' }, { id: 'f2', name: 'Group tasks' }] } } }
}

function context(overrides = {}) {
  const vm = {
    $t: (key, params) => params ? `${key}:${params.join(',')}` : key,
    $root: { config: structuredClone(CONFIG) },
    $store: store(),
    ...IdentityAuthorizations.data.call({}),
    userId: null,
    groupId: null,
    ...overrides
  }
  for (const [name, method] of Object.entries(IdentityAuthorizations.methods)) {
    if (!(name in vm)) vm[name] = method.bind(vm)
  }
  for (const [name, def] of Object.entries(IdentityAuthorizations.computed)) {
    if (name in vm) continue
    Object.defineProperty(vm, name, { get: () => def.call(vm), configurable: true })
  }
  return vm
}

function authorization(overrides = {}) {
  return { id: 'a1', type: 1, permissions: ['ALL'], userId: 'demo', groupId: null, resourceType: 0, resourceId: '*', ...overrides }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('IdentityAuthorizations queries', () => {
  it.each([
    [{ userId: 'demo' }, false, [{ userIdIn: 'demo' }]],
    [{ groupId: 'sales' }, false, [{ groupIdIn: 'sales' }]],
    [{ userId: 'demo' }, true, [{ userIdIn: 'demo' }, { userIdIn: '*' }, { groupIdIn: '*' }]],
    [{ groupId: 'sales' }, true, [{ groupIdIn: 'sales' }, { userIdIn: '*' }, { groupIdIn: '*' }]],
    // Without an identity there is nothing to list, not even the wildcard rows.
    [{}, true, []]
  ])('for %j with wildcard %j asks %j', (identity, includeWildcard, expected) => {
    const vm = context({ ...identity, includeWildcard })
    expect(vm.queries()).toEqual(expected)
  })
})

describe('IdentityAuthorizations loadAuthorizations', () => {
  it('lists the authorizations explicitly assigned to the user', async () => {
    const rows = [authorization()]
    AdminService.findAuthorizations.mockResolvedValueOnce(rows)
    const vm = context({ userId: 'demo' })

    await vm.loadAuthorizations()

    expect(AdminService.findAuthorizations).toHaveBeenCalledTimes(1)
    expect(AdminService.findAuthorizations).toHaveBeenCalledWith({ userIdIn: 'demo' })
    expect(vm.authorizations).toEqual(rows)
    expect(vm.loading).toBe(false)
  })

  it('merges the wildcard authorizations and drops duplicates by id', async () => {
    const own = authorization({ id: 'own', groupId: 'sales', userId: null })
    const everyUser = authorization({ id: 'global', type: 0, userId: '*' })
    AdminService.findAuthorizations
      .mockResolvedValueOnce([own])
      .mockResolvedValueOnce([everyUser])
      .mockResolvedValueOnce([everyUser])
    const vm = context({ groupId: 'sales', includeWildcard: true })

    await vm.loadAuthorizations()

    expect(AdminService.findAuthorizations).toHaveBeenCalledTimes(3)
    expect(vm.authorizations).toEqual([own, everyUser])
  })

  it('clears the list without a request when no identity is given', async () => {
    const vm = context({ authorizations: [authorization()] })

    await vm.loadAuthorizations()

    expect(AdminService.findAuthorizations).not.toHaveBeenCalled()
    expect(vm.authorizations).toEqual([])
  })

  // Toggling the checkbox quickly starts a second load before the first one has answered;
  // the older, slower answer must not replace the newer list.
  it('ignores a response that was overtaken by a newer request', async () => {
    let resolveFirst
    AdminService.findAuthorizations
      .mockImplementationOnce(() => new Promise(resolve => { resolveFirst = resolve }))
      .mockResolvedValueOnce([authorization({ id: 'new' })])
    const vm = context({ userId: 'demo' })

    const first = vm.loadAuthorizations()
    await vm.loadAuthorizations()
    resolveFirst([authorization({ id: 'old' })])
    await first

    expect(vm.authorizations.map(a => a.id)).toEqual(['new'])
    expect(vm.loading).toBe(false)
  })

  it('resets the loading state when the request fails', async () => {
    AdminService.findAuthorizations.mockRejectedValueOnce(new Error('403'))
    const vm = context({ userId: 'demo' })

    await expect(vm.loadAuthorizations()).rejects.toThrow('403')

    expect(vm.loading).toBe(false)
  })
})

describe('IdentityAuthorizations reloading', () => {
  it.each(['userId', 'groupId', 'includeWildcard'])('reloads when %s changes', (key) => {
    const vm = { loadAuthorizations: vi.fn() }
    IdentityAuthorizations.watch[key].call(vm)
    expect(vm.loadAuthorizations).toHaveBeenCalledTimes(1)
  })

  it('loads on creation', () => {
    const vm = { loadAuthorizations: vi.fn() }
    IdentityAuthorizations.created.call(vm)
    expect(vm.loadAuthorizations).toHaveBeenCalledTimes(1)
  })
})

describe('IdentityAuthorizations display', () => {
  it('names a configured resource type and links to its authorization table', () => {
    const vm = context()
    expect(vm.resourceTypeName(6)).toBe('admin.authorizations.resourcesTypes.processDefinition')
    expect(vm.resourceTypeRoute(6)).toEqual({
      name: 'authorizationType', params: { resourceTypeId: '6', resourceTypeKey: 'processDefinition' }
    })
  })

  it('shows the raw id of a resource type missing from the configuration, without a link', () => {
    const vm = context()
    expect(vm.resourceTypeName(99)).toBe('99')
    expect(vm.resourceTypeRoute(99)).toBeNull()
  })

  it('copes with a configuration without admin section', () => {
    const vm = context({ $root: { config: {} } })
    expect(vm.resourceTypeName(0)).toBe('0')
    expect(vm.typeName(1)).toBe('1')
  })

  it.each([
    [0, 'admin.authorizations.types.global'],
    [1, 'admin.authorizations.types.allow'],
    [2, 'admin.authorizations.types.deny'],
    [7, '7']
  ])('names the authorization type %j as %j', (type, expected) => {
    expect(context().typeName(type)).toBe(expected)
  })

  it.each([
    [['READ', 'UPDATE'], 'READ, UPDATE'],
    [['ALL'], 'ALL'],
    [[], 'NONE'],
    [undefined, 'NONE']
  ])('prints the permissions %j as %j', (permissions, expected) => {
    expect(context().permissionsText(permissions)).toBe(expected)
  })

  it('sorts the rows by resource type name, then by resource id', () => {
    const vm = context({
      authorizations: [
        authorization({ id: '1', resourceType: 6, resourceId: 'invoice' }),
        authorization({ id: '2', resourceType: 0, resourceId: 'tasklist' }),
        authorization({ id: '3', resourceType: 6, resourceId: '*' }),
        authorization({ id: '4', resourceType: 0, resourceId: null })
      ]
    })
    expect(vm.rows.map(a => a.id)).toEqual(['4', '2', '3', '1'])
    // Sorting works on a copy, the loaded list keeps its order.
    expect(vm.authorizations.map(a => a.id)).toEqual(['1', '2', '3', '4'])
  })

  it('offers the resource type as the first column', () => {
    expect(context().fields.map(f => f.key)).toEqual(['resourceTypeName', 'typeName', 'userIdGroupId', 'permissionsText', 'resourceId'])
  })

  it('adds the resource name column as soon as a filter authorization is listed', () => {
    const vm = context({ authorizations: [authorization(), authorization({ id: 'f', resourceType: FILTER, resourceId: 'f1' })] })
    expect(vm.fields.map(f => f.key)).toEqual(['resourceTypeName', 'typeName', 'userIdGroupId', 'permissionsText', 'name', 'resourceId'])
  })

  it.each([
    [FILTER, 'f2', 'Group tasks'],
    // A filter that is gone, or one the admin may not see, has no name to show.
    [FILTER, 'unknown', ''],
    [FILTER, '*', ''],
    // Other resource types have no name, even when their id happens to match a filter id.
    [6, 'f1', '']
  ])('names resource type %j with id %j as %j', (resourceType, resourceId, expected) => {
    expect(context().resourceName(authorization({ resourceType, resourceId }))).toBe(expected)
  })
})

describe('IdentityAuthorizations rendered', () => {
  const stubs = {
    ContentBlock: {
      props: ['title'],
      template: '<section><h2>{{ title }}</h2><div class="actions"><slot name="actions"></slot></div><slot></slot></section>'
    },
    FlowTable: {
      props: ['items', 'fields'],
      template: `<table><tr v-for="item in items" :key="item.id">
        <td v-for="field in fields" :key="field.key" :class="field.key">
          <slot :name="'cell(' + field.key + ')'" :item="item">{{ item[field.key] }}</slot>
        </td></tr></table>`
    },
    'b-form-checkbox': {
      props: ['modelValue'],
      emits: ['update:modelValue'],
      template: '<label><input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)"><slot></slot></label>'
    },
    BWaitingBox: true
  }

  // Mounted directly, $root is the component itself, so `config` comes in as a global property.
  function mountHost(props, extraStubs = {}) {
    return mountWithDefaults(IdentityAuthorizations, {
      props, i18nPlugin: false,
      global: { stubs: { ...stubs, ...extraStubs }, mocks: { config: structuredClone(CONFIG), $store: store() } }
    })
  }

  async function settle() {
    for (let i = 0; i < 5; i++) await Promise.resolve()
  }

  it('renders one row per authorization with readable cells', async () => {
    AdminService.findAuthorizations.mockResolvedValueOnce([
      authorization({ id: 'u', resourceType: 6, resourceId: 'invoice', permissions: ['READ', 'UPDATE'] }),
      authorization({ id: 'g', type: 2, userId: null, groupId: 'sales', resourceType: 99, permissions: [] })
    ])
    const wrapper = mountHost({ userId: 'demo', title: 'Authorizations of demo' })
    await settle()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('h2').text()).toBe('Authorizations of demo')
    const rows = wrapper.findAll('tr')
    expect(rows).toHaveLength(2)
    // Sorted by resource type name: the unknown type "99" comes before the translation key.
    expect(rows[0].find('.resourceTypeName').text()).toBe('99')
    expect(rows[0].find('.resourceTypeName a').exists()).toBe(false)
    expect(rows[0].find('.typeName').text()).toBe('admin.authorizations.types.deny')
    expect(rows[0].find('.userIdGroupId').text()).toBe('sales')
    expect(rows[0].find('.userIdGroupId .mdi-account-group').exists()).toBe(true)
    expect(rows[0].find('.permissionsText').text()).toBe('NONE')
    expect(rows[1].find('.resourceTypeName a').text()).toBe('admin.authorizations.resourcesTypes.processDefinition')
    expect(rows[1].find('.userIdGroupId .mdi-account').exists()).toBe(true)
    expect(rows[1].find('.permissionsText').text()).toBe('READ, UPDATE')
  })

  it('shows the name of a filter next to its id', async () => {
    AdminService.findAuthorizations.mockResolvedValueOnce([
      authorization({ id: 'f', resourceType: FILTER, resourceId: 'f1', permissions: ['READ'] })
    ])
    const wrapper = mountHost({ userId: 'demo' })
    await settle()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('td.name').text()).toBe('My tasks')
    expect(wrapper.find('td.resourceId').text()).toBe('f1')
  })

  it('asks for the wildcard authorizations once the checkbox is ticked', async () => {
    const wrapper = mountHost({ groupId: 'sales' })
    await settle()
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('admin.noResults')
    expect(AdminService.findAuthorizations).toHaveBeenCalledTimes(1)

    await wrapper.find('input[type="checkbox"]').setValue(true)
    await settle()

    expect(AdminService.findAuthorizations).toHaveBeenCalledWith({ userIdIn: '*' })
    expect(AdminService.findAuthorizations).toHaveBeenCalledWith({ groupIdIn: '*' })
  })

  // A user's own entries leave out what the user gets through groups, so the list must not pass for the effective permissions
  it('warns above a user\'s list that it holds only the explicitly assigned authorizations', async () => {
    const wrapper = mountHost({ userId: 'demo' })
    await settle()

    const banner = wrapper.find('.alert-info')
    expect(banner.exists()).toBe(true)
    expect(banner.text()).toBe('admin.authorizations.identity.userHint')
    expect(banner.find('.mdi').attributes('aria-hidden')).toBe('true')
    // The banner sits above the content block, not inside it
    expect(wrapper.find('section .alert-info').exists()).toBe(false)
  })

  it('shows no such banner above a group\'s list', async () => {
    const wrapper = mountHost({ groupId: 'sales' })
    await settle()

    expect(wrapper.find('.alert-info').exists()).toBe(false)
  })

  it('shows the loading state while the request is pending', async () => {
    AdminService.findAuthorizations.mockImplementationOnce(() => new Promise(() => {}))
    const wrapper = mountHost({ userId: 'demo' })
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('admin.loading')
  })

  // FlowTable sorts a column by item[field.key]. The resource type and type ids are numbered in another
  // order than their titles (application 0, user 1, processDefinition 6; global 0, allow 1, deny 2), so
  // a column keyed by the id would come out in an order that looks random.
  it.each([
    [0, 'resource type', [
      'admin.authorizations.resourcesTypes.application',
      'admin.authorizations.resourcesTypes.processDefinition',
      'admin.authorizations.resourcesTypes.user'
    ]],
    [1, 'type', [
      'admin.authorizations.types.allow',
      'admin.authorizations.types.deny',
      'admin.authorizations.types.global'
    ]]
  ])('sorts column %i (%s) by its visible titles', async (column, label, ascending) => {
    AdminService.findAuthorizations.mockResolvedValueOnce([
      authorization({ id: 'u', resourceType: 1, type: 2 }),
      authorization({ id: 'a', resourceType: 0, type: 0 }),
      authorization({ id: 'p', resourceType: 6, type: 1 })
    ])
    const wrapper = mountHost({ userId: 'demo' }, { FlowTable: false })
    await settle()
    await wrapper.vm.$nextTick()
    const cells = () => wrapper.findAll('tbody tr').map(row => row.findAll('td')[column].text())

    await wrapper.findAll('thead th')[column].trigger('click')
    expect(cells()).toEqual(ascending)

    await wrapper.findAll('thead th')[column].trigger('click')
    expect(cells()).toEqual([...ascending].reverse())
  })
})

describe.each([
  ['ProfileUser', ProfileUser],
  ['ProfileGroup', ProfileGroup]
])('%s authorizations tab', (name, Component) => {
  it.each([
    [true, true],
    [false, false]
  ])('is offered when the authorization admin permission check returns %j', (allowed, expected) => {
    const applicationPermissions = vi.fn(() => allowed)
    const required = { authorization: ['ALL'] }
    const vm = { applicationPermissions, $root: { config: { permissions: { authorizationsManagement: required } } } }

    expect(Component.computed.canReadAuthorizations.call(vm)).toBe(expected)
    expect(applicationPermissions).toHaveBeenCalledWith(required, 'authorization')
  })
})
