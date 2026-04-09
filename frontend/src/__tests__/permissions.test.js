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
import { permissionsMixin } from '../permissions.js'

// ---------------------------------------------------------------------------
// Realistic permission config from config.json / config.js defaults
// ---------------------------------------------------------------------------
const configPermissions = {
  tasklist:                      { application: ['ACCESS'] },
  cockpit:                       { application: ['ACCESS'] },
  displayTasks:                  { task: ['READ', 'UPDATE'] },
  displayFilter:                 { filter: ['READ'] },
  editFilter:                    { filter: ['UPDATE'] },
  deleteFilter:                  { filter: ['DELETE'] },
  createFilter:                  { filter: ['CREATE'] },
  startProcess:                  { processDefinition: ['READ', 'CREATE_INSTANCE'], processInstance: ['CREATE'] },
  displayProcess:                { processDefinition: ['READ'] },
  managementProcess:             { processDefinition: ['READ'] },
  historyProcess:                { processDefinition: ['READ', 'READ_HISTORY'] },
  usersManagement:               { user: ['ALL'] },
  groupsManagement:              { group: ['ALL'] },
  authorizationsManagement:      { authorization: ['ALL'] },
  systemManagement:              { system: ['ALL'] },
  tenantsManagement:             { tenant: ['ALL'] },
  userProfile:                   { application: ['ALL'] },
  deleteProcessInstance:         { processInstance: ['DELETE'] },
  deleteHistoricProcessInstance: { historicProcessInstance: ['DELETE'] },
}

// ---------------------------------------------------------------------------
// Helpers to build realistic server-side authorization objects
// ---------------------------------------------------------------------------
function allow(resourceId, permissions, { userId = 'demo', groupId = null } = {}) {
  return { userId, groupId, resourceId, permissions, type: 1 }
}

function deny(resourceId, permissions, { userId = 'demo', groupId = null } = {}) {
  return { userId, groupId, resourceId, permissions, type: 2 }
}

// ---------------------------------------------------------------------------
// Context factory — binds all mixin methods to a plain object that mirrors
// the component instance shape expected by permissionsMixin
// (this.$root.config, this.$root.user, and inter-method calls via `this`).
// ---------------------------------------------------------------------------
function createContext(userPermissions, { authorizationEnabled = true } = {}) {
  const ctx = {
    $root: {
      config: { authorizationEnabled, permissions: configPermissions },
      user:   { id: 'demo', permissions: userPermissions },
    },
  }
  // Bind every mixin method so they can call each other through `this`
  Object.entries(permissionsMixin.methods).forEach(([name, fn]) => {
    ctx[name] = fn.bind(ctx)
  })
  return ctx
}

// ---------------------------------------------------------------------------
// applicationPermissions
// ---------------------------------------------------------------------------
describe('applicationPermissions', () => {
  it('returns true when authorizationEnabled is false, regardless of permissionsRequired', () => {
    const ctx = createContext({}, { authorizationEnabled: false })
    expect(ctx.applicationPermissions(null,         'tasklist')).toBe(true)
    expect(ctx.applicationPermissions(undefined,    'tasklist')).toBe(true)
    expect(ctx.applicationPermissions({ application: ['ACCESS'] }, 'tasklist')).toBe(true)
  })

  it('returns false when permissionsRequired is null/undefined and authorization is enabled', () => {
    const ctx = createContext({ application: [] })
    expect(ctx.applicationPermissions(null,      'tasklist')).toBe(false)
    expect(ctx.applicationPermissions(undefined, 'tasklist')).toBe(false)
  })

  it('returns true when user has explicit ACCESS on the resource', () => {
    const ctx = createContext({
      application: [allow('tasklist', ['ACCESS'])],
    })
    expect(ctx.applicationPermissions(configPermissions.tasklist, 'tasklist')).toBe(true)
  })

  it('returns true when user has wildcard (*) grant on the resource type', () => {
    const ctx = createContext({
      application: [allow('*', ['ALL'], { userId: null, groupId: 'admin-group' })],
    })
    expect(ctx.applicationPermissions(configPermissions.tasklist,  'tasklist')).toBe(true)
    expect(ctx.applicationPermissions(configPermissions.cockpit,   'cockpit')).toBe(true)
  })

  it('returns false when user has no matching permission entry', () => {
    const ctx = createContext({ application: [] })
    expect(ctx.applicationPermissions(configPermissions.tasklist, 'tasklist')).toBe(false)
  })

  it('returns false when user has only cockpit ACCESS but check is for tasklist', () => {
    const ctx = createContext({
      application: [allow('cockpit', ['ACCESS'])],
    })
    expect(ctx.applicationPermissions(configPermissions.tasklist, 'tasklist')).toBe(false)
    expect(ctx.applicationPermissions(configPermissions.cockpit,  'cockpit')).toBe(true)
  })

  it('returns false when the wildcard grant is revoked (type=2)', () => {
    const ctx = createContext({
      application: [deny('*', ['ALL'])],
    })
    expect(ctx.applicationPermissions(configPermissions.tasklist, 'tasklist')).toBe(false)
  })

  it('returns false when resource is explicitly revoked even if wildcard is granted', () => {
    const ctx = createContext({
      application: [
        allow('*',         ['ALL']),
        deny( 'tasklist',  ['ACCESS']),
      ],
    })
    expect(ctx.applicationPermissions(configPermissions.tasklist, 'tasklist')).toBe(false)
    expect(ctx.applicationPermissions(configPermissions.cockpit,  'cockpit')).toBe(true)
  })
})

// ---------------------------------------------------------------------------
// applicationPermissionsDenied
// ---------------------------------------------------------------------------
describe('applicationPermissionsDenied', () => {
  it('returns false when authorizationEnabled is false', () => {
    const ctx = createContext({}, { authorizationEnabled: false })
    expect(ctx.applicationPermissionsDenied(null,         'tasklist')).toBe(false)
    expect(ctx.applicationPermissionsDenied(configPermissions.tasklist, 'tasklist')).toBe(false)
  })

  it('returns true when permissionsRequired is null/undefined and authorization is enabled', () => {
    const ctx = createContext({ application: [] })
    expect(ctx.applicationPermissionsDenied(null,      'tasklist')).toBe(true)
    expect(ctx.applicationPermissionsDenied(undefined, 'tasklist')).toBe(true)
  })

  it('returns false when no revocation exists for the resource', () => {
    const ctx = createContext({
      application: [allow('tasklist', ['ACCESS'])],
    })
    expect(ctx.applicationPermissionsDenied(configPermissions.tasklist, 'tasklist')).toBe(false)
  })

  it('returns true when the resource is explicitly revoked', () => {
    const ctx = createContext({
      application: [deny('tasklist', ['ACCESS'])],
    })
    expect(ctx.applicationPermissionsDenied(configPermissions.tasklist, 'tasklist')).toBe(true)
  })

  it('returns true when wildcard is revoked', () => {
    const ctx = createContext({
      application: [deny('*', ['ALL'])],
    })
    expect(ctx.applicationPermissionsDenied(configPermissions.tasklist, 'tasklist')).toBe(true)
    expect(ctx.applicationPermissionsDenied(configPermissions.cockpit,  'cockpit')).toBe(true)
  })
})

// ---------------------------------------------------------------------------
// adminManagementPermissions
// ---------------------------------------------------------------------------
describe('adminManagementPermissions', () => {
  it('returns true when user has ALL on user resource (users management)', () => {
    const ctx = createContext({
      user: [allow('*', ['ALL'])],
    })
    expect(ctx.adminManagementPermissions(configPermissions.usersManagement, 'user')).toBe(true)
  })

  it('returns true via group wildcard grant', () => {
    const ctx = createContext({
      user:          [allow('*', ['ALL'], { userId: null, groupId: 'admin-group' })],
      group:         [allow('*', ['ALL'], { userId: null, groupId: 'admin-group' })],
      authorization: [allow('*', ['ALL'], { userId: null, groupId: 'admin-group' })],
      tenant:        [allow('*', ['ALL'], { userId: null, groupId: 'admin-group' })],
      system:        [allow('*', ['ALL'], { userId: null, groupId: 'admin-group' })],
    })
    expect(ctx.adminManagementPermissions(configPermissions.usersManagement,          'user')).toBe(true)
    expect(ctx.adminManagementPermissions(configPermissions.groupsManagement,         'group')).toBe(true)
    expect(ctx.adminManagementPermissions(configPermissions.authorizationsManagement, 'authorization')).toBe(true)
    expect(ctx.adminManagementPermissions(configPermissions.tenantsManagement,        'tenant')).toBe(true)
    expect(ctx.adminManagementPermissions(configPermissions.systemManagement,         'system')).toBe(true)
  })

  it('returns false when user has no user-management permission', () => {
    const ctx = createContext({ user: [] })
    expect(ctx.adminManagementPermissions(configPermissions.usersManagement, 'user')).toBe(false)
  })

  it('returns true when authorizationEnabled is false', () => {
    const ctx = createContext({ user: [] }, { authorizationEnabled: false })
    expect(ctx.adminManagementPermissions(configPermissions.usersManagement, 'user')).toBe(true)
  })
})

// ---------------------------------------------------------------------------
// hasAdminManagementPermissions
// ---------------------------------------------------------------------------
describe('hasAdminManagementPermissions', () => {
  it('returns true when user has at least one admin sub-permission', () => {
    const ctx = createContext({
      user:          [],
      group:         [],
      authorization: [],
      tenant:        [],
      system:        [allow('*', ['ALL'])],
    })
    expect(ctx.hasAdminManagementPermissions(configPermissions)).toBe(true)
  })

  it('returns false when user has none of the admin permissions', () => {
    const ctx = createContext({
      user: [], group: [], authorization: [], tenant: [], system: [],
    })
    expect(ctx.hasAdminManagementPermissions(configPermissions)).toBe(false)
  })

  it('returns true when authorizationEnabled is false', () => {
    const ctx = createContext({}, { authorizationEnabled: false })
    expect(ctx.hasAdminManagementPermissions(configPermissions)).toBe(true)
  })
})

// ---------------------------------------------------------------------------
// processesByPermissions / processByPermissions
// ---------------------------------------------------------------------------
describe('processesByPermissions / processByPermissions', () => {
  const processes = [
    { key: 'order-process', name: 'Order Process' },
    { key: 'invoice-process', name: 'Invoice Process' },
    { key: 'restricted-process', name: 'Restricted Process' },
  ]

  it('marks process as not revoked when user has READ on a specific process key', () => {
    const ctx = createContext({
      processDefinition: [allow('order-process', ['READ'])],
    })
    const result = ctx.processesByPermissions(configPermissions.displayProcess, JSON.parse(JSON.stringify(processes)))
    expect(result.find(p => p.key === 'order-process').revoked).toBe(false)
    expect(result.find(p => p.key === 'invoice-process').revoked).toBe(true)
  })

  it('marks all processes as not revoked when user has wildcard READ', () => {
    const ctx = createContext({
      processDefinition: [allow('*', ['ALL'])],
    })
    const result = ctx.processesByPermissions(configPermissions.displayProcess, JSON.parse(JSON.stringify(processes)))
    result.forEach(p => expect(p.revoked).toBe(false))
  })

  it('processByPermissions returns true for allowed process, false for denied', () => {
    const ctx = createContext({
      processDefinition: [allow('order-process', ['READ'])],
    })
    expect(ctx.processByPermissions(configPermissions.displayProcess, { key: 'order-process' })).toBe(true)
    expect(ctx.processByPermissions(configPermissions.displayProcess, { key: 'invoice-process' })).toBe(false)
  })

  it('all processes are not revoked when authorizationEnabled is false', () => {
    const ctx = createContext({}, { authorizationEnabled: false })
    const result = ctx.processesByPermissions(configPermissions.displayProcess, JSON.parse(JSON.stringify(processes)))
    result.forEach(p => expect(p.revoked).toBe(false))
  })
})

// ---------------------------------------------------------------------------
// filtersByPermissions / filterByPermissions
// ---------------------------------------------------------------------------
describe('filtersByPermissions / filterByPermissions', () => {
  const filters = [
    { id: 'my-tasks',   name: 'My Tasks' },
    { id: 'all-tasks',  name: 'All Tasks' },
    { id: 'my-group',   name: 'My Group Tasks' },
  ]

  it('returns empty array when filters argument is empty or not an array', () => {
    const ctx = createContext({ filter: [] })
    expect(ctx.filtersByPermissions(configPermissions.displayFilter, [])).toEqual([])
    expect(ctx.filtersByPermissions(configPermissions.displayFilter, null)).toEqual([])
    expect(ctx.filtersByPermissions(configPermissions.displayFilter, undefined)).toEqual([])
  })

  it('returns only permitted filters when user has READ on specific filter IDs', () => {
    const ctx = createContext({
      filter: [
        allow('my-tasks',  ['READ']),
        allow('my-group',  ['READ']),
      ],
    })
    const result = ctx.filtersByPermissions(configPermissions.displayFilter, filters)
    expect(result.map(f => f.id)).toEqual(['my-tasks', 'my-group'])
  })

  it('returns all filters when user has wildcard READ', () => {
    const ctx = createContext({
      filter: [allow('*', ['ALL'])],
    })
    const result = ctx.filtersByPermissions(configPermissions.displayFilter, filters)
    expect(result).toHaveLength(filters.length)
  })

  it('filterByPermissions returns true for permitted filter', () => {
    const ctx = createContext({
      filter: [allow('my-tasks', ['READ'])],
    })
    expect(ctx.filterByPermissions(configPermissions.displayFilter, { id: 'my-tasks' })).toBe(true)
    expect(ctx.filterByPermissions(configPermissions.displayFilter, { id: 'all-tasks' })).toBe(false)
  })

  it('filterByPermissions returns false when filter is falsy', () => {
    const ctx = createContext({
      filter: [allow('*', ['ALL'])],
    })
    expect(ctx.filterByPermissions(configPermissions.displayFilter, null)).toBe(false)
  })

  it('filterByPermissions (create=true) returns true when user has wildcard ALL', () => {
    const ctx = createContext({
      filter: [allow('*', ['CREATE'])],
    })
    expect(ctx.filterByPermissions(configPermissions.createFilter, null, true)).toBe(true)
  })

  it('filterByPermissions (create=true) returns false when wildcard * is revoked', () => {
    const ctx = createContext({
      filter: [deny('*', ['CREATE'])],
    })
    expect(ctx.filterByPermissions(configPermissions.createFilter, null, true)).toBe(false)
  })
})

// ---------------------------------------------------------------------------
// tasksByPermissions
// ---------------------------------------------------------------------------
describe('tasksByPermissions', () => {
  const tasks = [
    { id: 'task-1', name: 'Review Invoice' },
    { id: 'task-2', name: 'Approve Order' },
    { id: 'task-3', name: 'Sign Contract' },
  ]

  it('returns only tasks the user has READ+UPDATE permission on', () => {
    const ctx = createContext({
      task: [
        allow('task-1', ['READ', 'UPDATE']),
        allow('task-3', ['READ', 'UPDATE']),
      ],
    })
    const result = ctx.tasksByPermissions(configPermissions.displayTasks, tasks)
    expect(result.map(t => t.id)).toEqual(['task-1', 'task-3'])
  })

  it('returns all tasks when user has wildcard ALL', () => {
    const ctx = createContext({
      task: [allow('*', ['ALL'])],
    })
    const result = ctx.tasksByPermissions(configPermissions.displayTasks, tasks)
    expect(result).toHaveLength(tasks.length)
  })

  it('returns empty array when user has no permissions', () => {
    const ctx = createContext({ task: [] })
    const result = ctx.tasksByPermissions(configPermissions.displayTasks, tasks)
    expect(result).toHaveLength(0)
  })

  it('returns all tasks when authorizationEnabled is false', () => {
    const ctx = createContext({}, { authorizationEnabled: false })
    const result = ctx.tasksByPermissions(configPermissions.displayTasks, tasks)
    expect(result).toHaveLength(tasks.length)
  })
})

