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
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { appRoutes, createAppRouter } from '@/router.js'
import { axios } from '@/globals.js'

vi.mock('@/globals.js', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, axios: { create: vi.fn(), defaults: { headers: { common: {} } } } }
})

// Every route nested under /seven/auth must go through authGuard(true) (via the 'auth'
// parent record) before anything else runs. Discovered dynamically so a new route added
// under /seven/auth is automatically covered here without editing this file.
const enumerationRouter = createAppRouter(appRoutes)
const AUTH_ROUTE_NAMES = enumerationRouter.getRoutes()
  .filter(r => r.path === '/seven/auth' || r.path.startsWith('/seven/auth/'))
  .map(r => r.name)

// The specific permission each route (directly or via an ancestor) requires, mirroring
// router.js. `null` means "authentication only, no additional permission gate". Every
// name in AUTH_ROUTE_NAMES must be classified here (enforced below) so a newly added
// route can't silently end up untested.
const ROUTE_PERMISSIONS = {
  auth: null,
  start: null,
  'start-configurable': null,
  'no-permission': null,
  'not-found-instanceId': null,
  admin: null,
  usersManagement: null,

  account: { permission: 'userProfile' },
  'start-process': { permission: 'tasklist' },
  'task-id': { permission: 'tasklist' },
  tasks: { permission: 'tasklist' },
  tasklist: { permission: 'tasklist' },
  batches: { permission: 'cockpit' },
  cockpit: { permission: 'cockpit' },
  processesDashboard: { permission: 'cockpit' },
  processManagement: { permission: 'cockpit' },
  'process-definition-id': { permission: 'cockpit' },
  'process-instance-id': { permission: 'cockpit' },
  process: { permission: 'cockpit' },
  decisions: { permission: 'cockpit' },
  'decision-list': { permission: 'cockpit' },
  decision: { permission: 'cockpit' },
  'decision-version': { permission: 'cockpit' },
  'decision-instance': { permission: 'cockpit' },
  deployments: { permission: 'cockpit' },
  'human-tasks': { permission: 'cockpit' },
  modeler: { permission: 'modeler' },
  adminUsers: { permission: 'usersManagement', condition: 'user' },
  adminUser: { permission: 'usersManagement', condition: 'user' },
  adminGroups: { permission: 'groupsManagement', condition: 'group' },
  adminGroup: { permission: 'groupsManagement', condition: 'group' },
  adminTenants: { permission: 'tenantsManagement', condition: 'tenant' },
  adminTenant: { permission: 'tenantsManagement', condition: 'tenant' },
  adminSystem: { permission: 'systemManagement', condition: 'system' },
  'system-diagnostics': { permission: 'systemManagement', condition: 'system' },
  'execution-metrics': { permission: 'systemManagement', condition: 'system' },
  authorizations: { permission: 'authorizationsManagement', condition: 'authorization' },
  authorizationType: { permission: 'authorizationsManagement', condition: 'authorization' },
  createUser: { permission: 'usersManagement', condition: 'user' },
  createGroup: { permission: 'groupsManagement', condition: 'group' },
  createTenant: { permission: 'tenantsManagement', condition: 'tenant' },
}

// Routes outside /seven/auth that carry their own [authGuard, permissionsGuard(...)] pair.
const STANDALONE_GUARDED_ROUTES = {
  'deployed-form': { permission: 'tasklist', guardIndex: 1 },
  'start-deployed-form': { permission: 'tasklist', guardIndex: 1 },
}

function dummyParams(path) {
  const params = {}
  for (const match of path.matchAll(/:([A-Za-z0-9_]+)\??/g)) params[match[1]] = 'test-id'
  return params
}

// Resolves a named route on the given router instance, auto-filling any dynamic
// segments (:id, :id?) with dummy values so required params never block resolution.
function resolveRoute(router, name) {
  const path = enumerationRouter.getRoutes().find(r => r.name === name).path
  return router.resolve({ name, params: dummyParams(path) })
}

function createRootStub(overrides = {}) {
  return {
    user: null,
    config: {
      permissions: {},
      layout: { showUserSettings: true },
      authorizationEnabled: true,
      modelerEnabled: true,
      servicesBasePath: '/services/v1',
      ssoActive: false,
    },
    applicationPermissions: vi.fn(() => false),
    $refs: { error: { show: vi.fn() } },
    ...overrides,
  }
}

async function runGuardChain(matched, to, from) {
  for (const record of matched) {
    const guards = Array.isArray(record.beforeEnter) ? record.beforeEnter : (record.beforeEnter ? [record.beforeEnter] : [])
    for (const guard of guards) {
      const result = await guard(to, from)
      if (result !== undefined && result !== true) return result
    }
  }
  return true
}

// Finds the guard that actually gates `name`: its own beforeEnter, or -- for a route
// that inherits its permission check from a parent (e.g. 'tasklist' under 'tasks') --
// the nearest ancestor's beforeEnter.
function ownGuardOf(router, name, guardIndex = 0) {
  const matched = resolveRoute(router, name).matched
  for (let i = matched.length - 1; i >= 0; i--) {
    if (!matched[i].beforeEnter) continue
    const guards = Array.isArray(matched[i].beforeEnter) ? matched[i].beforeEnter : [matched[i].beforeEnter]
    return guards[guardIndex]
  }
  return undefined
}

describe('router guards', () => {
  let router

  beforeEach(() => {
    vi.clearAllMocks()
    router = createAppRouter(appRoutes)
    // authGuard logs via console.debug/error on every unauthenticated attempt below --
    // expected noise from the guard doing its job, so silence it for these tests.
    vi.spyOn(console, 'debug').mockImplementation(() => {})
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('route classification', () => {
    it('classifies every /seven/auth/* route in ROUTE_PERMISSIONS', () => {
      const missing = AUTH_ROUTE_NAMES.filter(name => !(name in ROUTE_PERMISSIONS))
      expect(missing).toEqual([])
    })
  })

  describe('1) every /seven/auth/* route requires an authenticated user', () => {
    beforeEach(() => {
      // No response on the rejection => authGuard treats it as an unauthenticated visitor.
      axios.create.mockReturnValue({ get: vi.fn().mockRejectedValue(new Error('network error')) })
    })

    it.each(AUTH_ROUTE_NAMES)('blocks navigation to "%s" when there is no logged-in user', async (name) => {
      // Grant every permission so the auth gate is the only thing that can block this
      // navigation -- otherwise a permission redirect would mask a broken auth check.
      router.setRoot(createRootStub({ user: null, applicationPermissions: vi.fn(() => true) }))

      const from = router.resolve('/')
      const to = resolveRoute(router, name)

      const result = await runGuardChain(to.matched, to, from)

      expect(result).not.toBe(true)
    })

    it('allows navigation past the auth gate once the user is authenticated', async () => {
      router.setRoot(createRootStub({ user: { id: 'u1' } }))

      const from = router.resolve('/')
      const to = router.resolve({ name: 'start' })

      const result = await runGuardChain(to.matched, to, from)

      expect(result).toBe(true)
    })
  })

  describe('2) permission-gated routes require the exact permission they declare', () => {
    const guardedRoutes = Object.entries(ROUTE_PERMISSIONS).filter(([, rule]) => rule)

    it.each(guardedRoutes)('"%s" requires permission %o', async (name, { permission, condition }) => {
      const from = router.resolve('/')
      const to = resolveRoute(router, name)
      const expectedAccess = condition || permission

      // with permission: the user has exactly the required access
      let root = createRootStub({ applicationPermissions: vi.fn((_, access) => access === expectedAccess) })
      router.setRoot(root)
      let guard = ownGuardOf(router, name)
      expect(await guard(to, from)).toBe(true)
      expect(root.applicationPermissions).toHaveBeenCalledWith(root.config.permissions[permission], expectedAccess)

      // without permission: the user has nothing granted
      root = createRootStub({ applicationPermissions: vi.fn(() => false) })
      router.setRoot(root)
      guard = ownGuardOf(router, name)
      expect(await guard(to, from)).toEqual({
        name: 'no-permission',
        query: { permission, refPath: from.fullPath },
      })

      // wrong permission: the user has some other, unrelated permission granted
      root = createRootStub({ applicationPermissions: vi.fn((_, access) => access === 'some-unrelated-permission') })
      router.setRoot(root)
      guard = ownGuardOf(router, name)
      expect(await guard(to, from)).toEqual({
        name: 'no-permission',
        query: { permission, refPath: from.fullPath },
      })
    })
  })

  describe('standalone guarded routes (deployed-form / start-deployed-form)', () => {
    it.each(Object.entries(STANDALONE_GUARDED_ROUTES))('"%s" is blocked for an unauthenticated user', async (name) => {
      axios.create.mockReturnValue({ get: vi.fn().mockRejectedValue(new Error('network error')) })
      router.setRoot(createRootStub({ user: null }))

      const from = router.resolve('/')
      const to = resolveRoute(router, name)

      const result = await runGuardChain(to.matched, to, from)

      expect(result).not.toBe(true)
    })

    it.each(Object.entries(STANDALONE_GUARDED_ROUTES))('"%s" requires permission %o once authenticated', async (name, { permission, guardIndex }) => {
      const from = router.resolve('/')
      const to = resolveRoute(router, name)

      let root = createRootStub({ user: { id: 'u1' }, applicationPermissions: vi.fn((_, access) => access === permission) })
      router.setRoot(root)
      let guard = ownGuardOf(router, name, guardIndex)
      expect(await guard(to, from)).toBe(true)

      root = createRootStub({ user: { id: 'u1' }, applicationPermissions: vi.fn(() => false) })
      router.setRoot(root)
      guard = ownGuardOf(router, name, guardIndex)
      expect(await guard(to, from)).toEqual({
        name: 'no-permission',
        query: { permission, refPath: from.fullPath },
      })
    })
  })
})
