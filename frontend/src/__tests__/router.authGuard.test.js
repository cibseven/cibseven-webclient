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
import { createAxiosMock, resetAxiosMock } from './support/axiosMock.js'

const axios = createAxiosMock()

vi.mock('@/globals.js', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, axios }
})

vi.mock('@/services.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    AuthService: { fetchAuths: vi.fn() },
    SetupService: { getStatus: vi.fn() },
    HistoryService: { ...actual.HistoryService, findProcessInstance: vi.fn() },
    ProcessService: { ...actual.ProcessService, findProcessById: vi.fn(), findProcessInstance: vi.fn() }
  }
})

const { appRoutes, createAppRouter, authGuard, setupGuard, modelerGuard, permissionsGuard } = await import('@/router.js')
const { AuthService, SetupService, HistoryService, ProcessService } = await import('@/services.js')

const BASE = '/services/v1'

const router = createAppRouter(appRoutes)

/** Point the module-level router at a root component stub. */
function setRoot(overrides = {}) {
  const root = {
    user: null,
    config: { servicesBasePath: BASE, permissions: {} },
    applicationPermissions: () => true,
    $refs: { error: { show: vi.fn() } },
    ...overrides
  }
  router.setRoot(root)
  return root
}

const route = (overrides = {}) => ({ query: {}, fullPath: '/seven/auth/start', ...overrides })

/** An axios rejection shaped like the backend's error envelope. */
const httpError = (data) => Object.assign(new Error('request failed'), { response: { data } })

beforeEach(() => {
  resetAxiosMock(axios)
  vi.clearAllMocks()
  localStorage.clear()
  sessionStorage.clear()
  axios.defaults.headers.common = {}
  vi.spyOn(console, 'info').mockImplementation(() => {})
  vi.spyOn(console, 'warn').mockImplementation(() => {})
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('authGuard', () => {
  const authenticated = () => {
    axios.get.mockResolvedValue({ data: { id: 'demo', authToken: 'tok-fresh' } })
    AuthService.fetchAuths.mockResolvedValue(['READ'])
  }

  it('should let an already authenticated user straight through', async () => {
    setRoot({ user: { id: 'demo' } })

    await expect(authGuard(true)(route())).resolves.toBe(true)
    expect(axios.create).not.toHaveBeenCalled()
  })

  describe('successful authentication', () => {
    it('should verify the token and populate the root user with its permissions', async () => {
      const root = setRoot()
      sessionStorage.setItem('token', 'tok-session')
      authenticated()

      await expect(authGuard(true)(route())).resolves.toBe(true)

      expect(axios.create).toHaveBeenCalled()
      expect(axios.get).toHaveBeenCalledWith(`${BASE}/auth`, { headers: { authorization: 'tok-session' } })
      expect(root.user).toEqual({ id: 'demo', authToken: 'tok-fresh', permissions: ['READ'] })
    })

    it('should install the returned token as the default authorization header', async () => {
      setRoot()
      sessionStorage.setItem('token', 'tok-session')
      authenticated()

      await authGuard(true)(route())

      expect(axios.defaults.headers.common.authorization).toBe('tok-fresh')
    })

    // A token handed over in the URL (e.g. an external deep link) is promoted to the
    // session so the rest of the app can use it.
    it('should adopt a token passed in the query string', async () => {
      setRoot()
      authenticated()

      await authGuard(true)(route({ query: { token: 'tok-from-url' } }))

      expect(sessionStorage.getItem('token')).toBe('tok-from-url')
      expect(axios.get).toHaveBeenCalledWith(`${BASE}/auth`, { headers: { authorization: 'tok-from-url' } })
    })

    it('should fall back to a remembered token in localStorage', async () => {
      setRoot()
      localStorage.setItem('token', 'tok-local')
      authenticated()

      await authGuard(true)(route())

      expect(axios.get).toHaveBeenCalledWith(`${BASE}/auth`, { headers: { authorization: 'tok-local' } })
    })

    it('should prefer the session token over the remembered one', async () => {
      setRoot()
      sessionStorage.setItem('token', 'tok-session')
      localStorage.setItem('token', 'tok-local')
      authenticated()

      await authGuard(true)(route())

      expect(axios.get).toHaveBeenCalledWith(`${BASE}/auth`, { headers: { authorization: 'tok-session' } })
    })
  })

  // A network-level failure has no response envelope, so there is nothing to interpret and
  // no point redirecting to a login that is equally unreachable.
  it('should refuse navigation on a transport-level error', async () => {
    setRoot()
    axios.get.mockRejectedValue(new Error('Network Error'))

    await expect(authGuard(true)(route())).resolves.toBe(false)
    expect(console.error).toHaveBeenCalledWith('Strange AJAX error', expect.any(Error))
  })

  describe('token prolongation', () => {
    // The backend answers an expired token with a fresh one in `params`; the guard swaps it
    // in and retries, so the user never sees a login screen for a merely stale token.
    it('should retry with the prolonged token and continue', async () => {
      const root = setRoot()
      sessionStorage.setItem('token', 'tok-expired')
      axios.get
        .mockRejectedValueOnce(httpError({ type: 'TokenExpiredException', params: ['tok-prolonged'] }))
        .mockResolvedValueOnce({ data: { id: 'demo', authToken: 'tok-fresh' } })
      AuthService.fetchAuths.mockResolvedValue([])

      await expect(authGuard(true)(route())).resolves.toBe(true)

      expect(sessionStorage.getItem('token')).toBe('tok-prolonged')
      expect(root.user).toMatchObject({ id: 'demo' })
    })

    it('should write the prolonged token back to localStorage when it lived there', async () => {
      setRoot()
      localStorage.setItem('token', 'tok-expired')
      axios.get
        .mockRejectedValueOnce(httpError({ type: 'TokenExpiredException', params: ['tok-prolonged'] }))
        .mockResolvedValueOnce({ data: { authToken: 'tok-fresh' } })
      AuthService.fetchAuths.mockResolvedValue([])

      await authGuard(true)(route())

      expect(localStorage.getItem('token')).toBe('tok-prolonged')
      expect(sessionStorage.getItem('token')).toBeNull()
    })

    it('should redirect to login when the retry also fails', async () => {
      setRoot()
      sessionStorage.setItem('token', 'tok-expired')
      axios.get
        .mockRejectedValueOnce(httpError({ type: 'TokenExpiredException', params: ['tok-prolonged'] }))
        .mockRejectedValueOnce(httpError({ type: 'AuthenticationException' }))
      SetupService.getStatus.mockResolvedValue({ data: false })

      const target = await authGuard(true)(route())

      expect(target).toEqual({ path: '/seven/login', query: { nextUrl: '/seven/auth/start' } })
    })

    // Without a replacement token there is nothing to retry with.
    it.each([[[]], [undefined]])('should not retry when params is %j', async (params) => {
      setRoot()
      sessionStorage.setItem('token', 'tok-expired')
      axios.get.mockRejectedValue(httpError({ type: 'TokenExpiredException', params }))
      SetupService.getStatus.mockResolvedValue({ data: false })

      await authGuard(true)(route())

      expect(axios.get).toHaveBeenCalledTimes(1)
    })
  })

  describe('redirect to login', () => {
    const rejectWith = (data) => axios.get.mockRejectedValue(httpError(data))

    it('should send an unauthenticated user to the login page', async () => {
      setRoot()
      rejectWith({ type: 'AuthenticationException' })
      SetupService.getStatus.mockResolvedValue({ data: false })

      const target = await authGuard(true)(route({ fullPath: '/seven/auth/tasks' }))

      expect(target).toEqual({ path: '/seven/login', query: { nextUrl: '/seven/auth/tasks' } })
    })

    // A non-strict guard leaves the path undefined so the router stays where it is while
    // still recording where the user was headed.
    it('should leave the path undefined when the guard is not strict', async () => {
      setRoot()
      rejectWith({ type: 'AuthenticationException' })
      SetupService.getStatus.mockResolvedValue({ data: false })

      const target = await authGuard(false)(route())

      expect(target).toEqual({ path: undefined, query: { nextUrl: '/seven/auth/start' } })
    })

    it('should discard the session token', async () => {
      setRoot()
      sessionStorage.setItem('token', 'tok-session')
      localStorage.setItem('token', 'tok-local')
      rejectWith({ type: 'AuthenticationException' })
      SetupService.getStatus.mockResolvedValue({ data: false })

      await authGuard(true)(route())

      expect(sessionStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('token')).toBe('tok-local')
    })

    it('should discard the remembered token when there is no session token', async () => {
      setRoot()
      localStorage.setItem('token', 'tok-local')
      rejectWith({ type: 'AuthenticationException' })
      SetupService.getStatus.mockResolvedValue({ data: false })

      await authGuard(true)(route())

      expect(localStorage.getItem('token')).toBeNull()
    })

    // Expected auth failures are silent; anything else is a real problem the user should
    // see in the error dialog.
    it.each([['AuthenticationException'], ['TokenExpiredException']])('should not surface a %s to the user', async (type) => {
      const root = setRoot()
      rejectWith({ type })
      SetupService.getStatus.mockResolvedValue({ data: false })

      await authGuard(true)(route())

      expect(root.$refs.error.show).not.toHaveBeenCalled()
    })

    it('should show an unexpected error in the error dialog', async () => {
      const root = setRoot()
      rejectWith({ type: 'SomethingElseException', message: 'boom' })
      SetupService.getStatus.mockResolvedValue({ data: false })

      await authGuard(true)(route())

      expect(root.$refs.error.show).toHaveBeenCalledWith({ type: 'SomethingElseException', message: 'boom' })
    })

    it('should show an auth error that carried params, since prolongation was attempted', async () => {
      const root = setRoot()
      sessionStorage.setItem('token', 'tok-expired')
      axios.get
        .mockRejectedValueOnce(httpError({ type: 'TokenExpiredException', params: ['tok-prolonged'] }))
        .mockRejectedValueOnce(httpError({ type: 'TokenExpiredException' }))
      SetupService.getStatus.mockResolvedValue({ data: false })

      await authGuard(true)(route())

      expect(root.$refs.error.show).toHaveBeenCalled()
    })

    // On a fresh installation there is no user to log in as yet, so the setup wizard takes
    // precedence over the login page.
    it('should send the user to setup when the installation is not initialised', async () => {
      setRoot()
      rejectWith({ type: 'AuthenticationException' })
      SetupService.getStatus.mockResolvedValue({ data: true })

      await expect(authGuard(true)(route())).resolves.toEqual({ name: 'setup' })
    })

    it('should fall back to the login page when the setup check itself fails', async () => {
      setRoot()
      rejectWith({ type: 'AuthenticationException' })
      SetupService.getStatus.mockRejectedValue(new Error('503'))

      const target = await authGuard(true)(route())

      expect(target).toEqual({ path: '/seven/login', query: { nextUrl: '/seven/auth/start' } })
    })
  })
})

describe('setupGuard', () => {
  it('should allow the setup page while setup is still required', async () => {
    SetupService.getStatus.mockResolvedValue({ data: true })

    await expect(setupGuard()).resolves.toBe(true)
  })

  // Re-running setup on a configured installation would let anyone create an admin user.
  it('should redirect to login once setup is done', async () => {
    SetupService.getStatus.mockResolvedValue({ data: false })

    await expect(setupGuard()).resolves.toEqual({ name: 'login' })
  })

  it('should redirect to login when the status check fails', async () => {
    SetupService.getStatus.mockRejectedValue(new Error('503'))

    await expect(setupGuard()).resolves.toEqual({ name: 'login' })
    expect(console.error).toHaveBeenCalledWith('Error checking setup status', expect.any(Error))
  })
})

describe('permissionsGuard', () => {
  const from = { fullPath: '/seven/auth/start' }

  it('should allow navigation when the permission is granted', () => {
    setRoot({
      config: { permissions: { cockpit: { user: ['*'] } } },
      applicationPermissions: () => true
    })

    expect(permissionsGuard('cockpit')(route(), from)).toBe(true)
  })

  it('should check the config slice against the permission name', () => {
    const applicationPermissions = vi.fn(() => true)
    setRoot({ config: { permissions: { cockpit: { user: ['*'] } } }, applicationPermissions })

    permissionsGuard('cockpit')(route(), from)

    expect(applicationPermissions).toHaveBeenCalledWith({ user: ['*'] }, 'cockpit')
  })

  // Admin sections gate on a differently named access key than their config slice.
  it('should use the explicit condition as the access key when given', () => {
    const applicationPermissions = vi.fn(() => true)
    setRoot({ config: { permissions: { usersManagement: { user: ['*'] } } }, applicationPermissions })

    permissionsGuard('usersManagement', 'user')(route(), from)

    expect(applicationPermissions).toHaveBeenCalledWith({ user: ['*'] }, 'user')
  })

  it('should redirect to the no-permission page, recording where the user came from', () => {
    setRoot({ config: { permissions: {} }, applicationPermissions: () => false })

    expect(permissionsGuard('cockpit')(route(), from)).toEqual({
      name: 'no-permission',
      query: { permission: 'cockpit', refPath: '/seven/auth/start' }
    })
  })
})

describe('modelerGuard', () => {
  const from = { fullPath: '/seven/auth/start' }

  it('should fall back to the start page when the modeler is disabled', () => {
    setRoot({ config: { modelerEnabled: false, permissions: {} } })

    expect(modelerGuard(route(), from)).toEqual({ name: 'start-configurable' })
  })

  it('should fall back to the start page when there is no config at all', () => {
    router.setRoot({})

    expect(modelerGuard(route(), from)).toEqual({ name: 'start-configurable' })
  })

  it('should delegate to the modeler permission check when enabled', () => {
    const applicationPermissions = vi.fn(() => true)
    setRoot({
      config: { modelerEnabled: true, permissions: { modeler: { user: ['*'] } } },
      applicationPermissions
    })

    expect(modelerGuard(route(), from)).toBe(true)
    expect(applicationPermissions).toHaveBeenCalledWith({ user: ['*'] }, 'modeler')
  })

  it('should redirect to the no-permission page when the modeler is not permitted', () => {
    setRoot({ config: { modelerEnabled: true, permissions: {} }, applicationPermissions: () => false })

    expect(modelerGuard(route(), from)).toMatchObject({ name: 'no-permission' })
  })
})

// The inline hooks nested inside `appRoutes` are anonymous closures, so a real navigation
// would normally be needed to reach them. `getRoutes()` exposes them, which lets each be
// driven directly with fake route objects — no component tree, no navigation.
describe('inline route hooks', () => {
  const routeRecord = (name) => router.getRoutes().find(r => r.name === name)
  const hooks = (name) => [routeRecord(name).beforeEnter].flat().filter(Boolean)
  const from = { fullPath: '/seven/auth/start' }

  describe('no-permission', () => {
    // The page itself is a redirect: it forwards to the start page, which renders the
    // explanation from these query parameters.
    it('should forward the refusal details to the start page', () => {
      const [hook] = hooks('no-permission')

      expect(hook({ query: { permission: 'cockpit', refPath: '/seven/auth/processes' } })).toEqual({
        name: 'start',
        query: { errorType: 'NoPermission', permission: 'cockpit', refPath: '/seven/auth/processes' }
      })
    })

    it('should tolerate a missing query', () => {
      const [hook] = hooks('no-permission')

      expect(hook({})).toEqual({
        name: 'start',
        query: { errorType: 'NoPermission', permission: undefined, refPath: undefined }
      })
    })
  })

  describe('not-found-instanceId', () => {
    it('should forward the unknown instance id to the start page', () => {
      const [hook] = hooks('not-found-instanceId')

      expect(hook({ query: { instanceId: 'pi-missing', refPath: '/x' } })).toEqual({
        name: 'start',
        query: { errorType: 'notFoundInstanceId', instanceId: 'pi-missing', refPath: '/x' }
      })
    })

    it('should tolerate a missing query', () => {
      const [hook] = hooks('not-found-instanceId')

      expect(hook({})).toMatchObject({ name: 'start', query: { errorType: 'notFoundInstanceId' } })
    })
  })

  describe('start-configurable', () => {
    const STARTPAGE_KEY = 'cibseven:preferences:startPage'
    const hook = () => hooks('start-configurable')[0]

    const allowAll = () => setRoot({
      config: { permissions: { cockpit: {}, tasklist: {} } },
      applicationPermissions: () => true
    })

    it('should send the user to the plain start page by default', () => {
      allowAll()

      expect(hook()(route(), from)).toEqual({ name: 'start' })
    })

    it.each([
      ['processes-dashboard', 'processesDashboard'],
      ['decisions-list', 'decision-list'],
      ['human-tasks-dashboard', 'human-tasks'],
      ['tasks', 'tasks'],
      ['start-process', 'start-process'],
      ['start', 'start']
    ])('should honour the %s start-page preference', (preference, expected) => {
      allowAll()
      localStorage.setItem(STARTPAGE_KEY, preference)

      expect(hook()(route(), from)).toEqual({ name: expected })
    })

    it('should fall back to start for an unrecognised preference', () => {
      allowAll()
      localStorage.setItem(STARTPAGE_KEY, 'something-removed-in-a-later-version')

      expect(hook()(route(), from)).toEqual({ name: 'start' })
    })

    // A preference pointing at a section the user may no longer enter must not strand them
    // there: the permission refusal takes precedence over the preference.
    it.each([['processes-dashboard'], ['decisions-list'], ['human-tasks-dashboard']])(
      'should refuse the cockpit preference %s when cockpit is not permitted',
      (preference) => {
        setRoot({ config: { permissions: {} }, applicationPermissions: () => false })
        localStorage.setItem(STARTPAGE_KEY, preference)

        expect(hook()(route(), from)).toMatchObject({
          name: 'no-permission',
          query: { permission: 'cockpit', refPath: '/seven/auth/start' }
        })
      }
    )

    it.each([['tasks'], ['start-process']])(
      'should refuse the tasklist preference %s when tasklist is not permitted',
      (preference) => {
        setRoot({ config: { permissions: {} }, applicationPermissions: () => false })
        localStorage.setItem(STARTPAGE_KEY, preference)

        expect(hook()(route(), from)).toMatchObject({
          name: 'no-permission',
          query: { permission: 'tasklist' }
        })
      }
    )

    it('should still allow the plain start page when nothing is permitted', () => {
      setRoot({ config: { permissions: {} }, applicationPermissions: () => false })

      expect(hook()(route(), from)).toEqual({ name: 'start' })
    })

    it('should send a cockpit user with a tasklist preference to the refusal page', () => {
      setRoot({
        config: { permissions: { cockpit: {}, tasklist: {} } },
        applicationPermissions: (_slice, access) => access === 'cockpit'
      })
      localStorage.setItem(STARTPAGE_KEY, 'tasks')

      expect(hook()(route(), from)).toMatchObject({ name: 'no-permission', query: { permission: 'tasklist' } })
    })
  })

  describe('account', () => {
    // Beyond the userProfile permission, a user may only open *their own* account page,
    // and only when the deployment exposes user settings at all.
    const ownAccountHook = () => hooks('account')[1]

    it('should allow a user to open their own account page', () => {
      setRoot({
        user: { id: 'demo' },
        config: { permissions: { userProfile: {} }, layout: { showUserSettings: true } }
      })

      expect(ownAccountHook()({ params: { userId: 'demo' } }, from)).toBe(true)
    })

    it('should refuse opening somebody else\'s account page', () => {
      setRoot({
        user: { id: 'demo' },
        config: { permissions: { userProfile: {} }, layout: { showUserSettings: true } }
      })

      expect(ownAccountHook()({ params: { userId: 'someone-else' } }, from)).toEqual({
        name: 'no-permission',
        query: { permission: 'userProfile', refPath: '/seven/auth/start' }
      })
    })

    it('should refuse when the deployment hides user settings', () => {
      setRoot({
        user: { id: 'demo' },
        config: { permissions: { userProfile: {} }, layout: { showUserSettings: false } }
      })

      expect(ownAccountHook()({ params: { userId: 'demo' } }, from)).toMatchObject({ name: 'no-permission' })
    })

    it('should refuse when no user id is in the route', () => {
      setRoot({
        user: { id: 'demo' },
        config: { permissions: { userProfile: {} }, layout: { showUserSettings: true } }
      })

      expect(ownAccountHook()({ params: {} }, from)).toMatchObject({ name: 'no-permission' })
    })
  })

  describe('login', () => {
    const hook = () => hooks('login')[0]

    it('should divert to setup when the installation is not initialised', async () => {
      setRoot({ config: { ssoActive: false } })
      SetupService.getStatus.mockResolvedValue({ data: true })

      await expect(hook()(route({ query: {} }))).resolves.toEqual({ name: 'setup' })
    })

    it('should show the login form on an initialised installation without SSO', async () => {
      setRoot({ config: { ssoActive: false } })
      SetupService.getStatus.mockResolvedValue({ data: false })

      await expect(hook()(route({ query: {} }))).resolves.toBe(true)
    })

    it('should show the login form when the setup check fails and SSO is off', async () => {
      setRoot({ config: { ssoActive: false } })
      SetupService.getStatus.mockRejectedValue(new Error('503'))

      await expect(hook()(route({ query: {} }))).resolves.toBe(true)
    })

    // With SSO the app leaves the SPA entirely, so the hook cancels navigation. jsdom
    // refuses a real navigation, hence the stubbed location.
    describe('with SSO active', () => {
      let originalLocation

      beforeEach(() => {
        originalLocation = window.location
        delete window.location
        window.location = { href: '' }
      })

      afterEach(() => {
        window.location = originalLocation
      })

      it('should hand over to the SSO page and cancel SPA navigation', async () => {
        setRoot({ config: { ssoActive: true } })
        SetupService.getStatus.mockResolvedValue({ data: false })

        await expect(hook()(route({ query: { nextUrl: '/seven/auth/tasks' } }))).resolves.toBe(false)
        expect(window.location.href).toBe('./sso-login.html?nextUrl=%2Fseven%2Fauth%2Ftasks')
      })

      it('should hand over with an empty nextUrl when none was requested', async () => {
        setRoot({ config: { ssoActive: true } })
        SetupService.getStatus.mockResolvedValue({ data: false })

        await hook()(route({ query: {} }))

        expect(window.location.href).toBe('./sso-login.html?nextUrl=')
      })

      it('should still hand over when the setup check fails', async () => {
        setRoot({ config: { ssoActive: true } })
        SetupService.getStatus.mockRejectedValue(new Error('503'))

        await expect(hook()(route({ query: { nextUrl: '/x' } }))).resolves.toBe(false)
        expect(window.location.href).toBe('./sso-login.html?nextUrl=%2Fx')
      })
    })
  })

  // Two redirect routes delegate to the shared helpers in utils/redirects.js; these pin
  // the wiring (including that the instance redirect is handed the router itself, which it
  // needs to read the configured history level).
  describe('by-id redirect routes', () => {
    it('should hand the definition redirect the incoming route', async () => {
      const hook = hooks('process-definition-id')[1]
      ProcessService.findProcessById.mockResolvedValue({ key: 'invoice', version: 3 })

      const target = await hook({ params: { definitionId: 'pd1' }, query: {} }, from)

      expect(ProcessService.findProcessById).toHaveBeenCalledWith('pd1', false)
      expect(target).toMatchObject({ name: 'process', params: { processKey: 'invoice', versionIndex: 3 } })
    })

    it('should hand the instance redirect the router so it can read the history level', async () => {
      setRoot({ config: { camundaHistoryLevel: 'full', permissions: {} } })
      const hook = hooks('process-instance-id')[1]
      HistoryService.findProcessInstance.mockResolvedValue({
        processDefinitionKey: 'invoice',
        processDefinitionVersion: 2
      })

      const target = await hook({ params: { instanceId: 'pi1' }, query: {} }, from)

      expect(HistoryService.findProcessInstance).toHaveBeenCalledWith('pi1')
      expect(target).toMatchObject({
        name: 'process',
        params: { processKey: 'invoice', versionIndex: 2, instanceId: 'pi1' }
      })
    })
  })

  describe('props factories', () => {
    const propsOf = (name) => routeRecord(name).props.default

    it('should pass the process coordinates and tenant to ProcessView', () => {
      expect(propsOf('process')({
        params: { processKey: 'invoice', versionIndex: '2', instanceId: 'pi1' },
        query: { tenantId: 't1' }
      })).toEqual({ processKey: 'invoice', versionIndex: '2', instanceId: 'pi1', tenantId: 't1' })
    })

    it('should pass the deployment id to DeploymentsView', () => {
      expect(propsOf('deployments')({ params: { deploymentId: 'd1' } })).toEqual({ deploymentId: 'd1' })
    })

    // The admin user page reuses the account component in edit mode.
    it('should put the admin user page into edit mode', () => {
      expect(propsOf('adminUser')({ params: { userId: 'demo' } })).toEqual({ editMode: true })
    })
  })
})
