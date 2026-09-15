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
import { createAxiosMock, resetAxiosMock } from '../support/axiosMock.js'

const axios = createAxiosMock()

vi.mock('@/globals.js', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, axios }
})

const {
  getTheme, hasHeader, isMobile, checkExternalReturn, updateAppTitle, applyTheme,
  loadTheme, fetchAndStoreProcesses, fetchDecisionsIfEmpty, setupTaskNotifications,
  handleAxiosError
} = await import('@/utils/init.js')
const { getServicesBasePath } = await import('@/services.js')
const platform = (await import('platform')).default

let originalLocation

beforeEach(() => {
  originalLocation = window.location
  localStorage.clear()
  resetAxiosMock(axios)
  axios.defaults.headers.common = {}
})

afterEach(() => {
  window.location = originalLocation
  vi.restoreAllMocks()
  document.head.querySelectorAll('link[rel="icon"], style').forEach(el => el.remove())
})

/** Replace window.location, which jsdom will not let us assign `href` on. */
function stubLocation(props) {
  delete window.location
  window.location = { href: '', hash: '', ...props }
  return window.location
}

describe('getTheme', () => {
  it('should return the configured theme', () => {
    expect(getTheme({ theme: 'dark' })).toBe('dark')
  })

  it.each([[{}], [{ theme: '' }], [{ theme: null }]])('should default to cib for config %j', (config) => {
    expect(getTheme(config)).toBe('cib')
  })
})

describe('hasHeader', () => {
  // The embedding page can suppress the app header via the hash.
  it('should read the header flag out of the hash', () => {
    stubLocation({ hash: '#/seven/auth/start&header=false' })

    expect(hasHeader()).toBe('false')
  })

  it('should default to true when the hash carries no header flag', () => {
    stubLocation({ hash: '#/seven/auth/start' })

    expect(hasHeader()).toBe('true')
  })

  it('should default to true for an empty hash', () => {
    stubLocation({ hash: '' })

    expect(hasHeader()).toBe('true')
  })

  it('should find the flag among several parameters', () => {
    stubLocation({ hash: '#/x?foo=1&header=false&bar=2' })

    expect(hasHeader()).toBe('false')
  })

  it('should url-decode the flag value', () => {
    stubLocation({ hash: '#/x&header=a%20b' })

    expect(hasHeader()).toBe('a b')
  })

  // Quirk worth pinning: the parser splits on '&' only and merely strips a '?' from the
  // key, so a flag placed directly after the '?' has the route path glued onto its name
  // ('#/x?header' becomes the key '#/xheader') and is therefore never seen. Only a flag
  // that follows an '&' is honoured, which is how the app actually builds these hashes.
  it('should not see a header flag placed directly after the question mark', () => {
    stubLocation({ hash: '#/seven/auth/start?header=false' })

    expect(hasHeader()).toBe('true')
  })
})

describe('isMobile', () => {
  it.each([['Android'], ['iOS']])('should report true on %s', (family) => {
    vi.spyOn(platform, 'os', 'get').mockReturnValue({ family })

    expect(isMobile()).toBe(true)
  })

  it.each([['Windows'], ['OS X'], ['Linux']])('should report false on %s', (family) => {
    vi.spyOn(platform, 'os', 'get').mockReturnValue({ family })

    expect(isMobile()).toBe(false)
  })
})

describe('checkExternalReturn', () => {
  // An external system returns the user with the token in the hash. It has to be moved
  // into storage and stripped from the URL, so it is not left in the address bar or in
  // the browser history.
  it('should store a token from the hash and strip it from the URL', () => {
    const location = stubLocation({})

    checkExternalReturn('http://host/#/x&token=abc123', '#/x&token=abc123')

    expect(localStorage.getItem('token')).toBe('abc123')
    expect(location.href).toBe('#/x')
  })

  it('should read a token that is followed by further parameters', () => {
    const location = stubLocation({})

    checkExternalReturn('http://host/', '#/x&token=abc123&foo=1')

    expect(localStorage.getItem('token')).toBe('abc123')
    expect(location.href).toBe('#/x&foo=1')
  })

  it('should url-decode the token', () => {
    stubLocation({})

    checkExternalReturn('http://host/', '#/x&token=a%2Bb%3Dc')

    expect(localStorage.getItem('token')).toBe('a+b=c')
  })

  it('should do nothing when the hash carries no token', () => {
    const location = stubLocation({ href: 'untouched' })

    checkExternalReturn('http://host/#/x', '#/x')

    expect(localStorage.getItem('token')).toBeNull()
    expect(location.href).toBe('untouched')
  })
})

describe('updateAppTitle', () => {
  it('should show just the product name on its own', () => {
    updateAppTitle('CIB seven')

    expect(document.title).toBe('CIB seven')
  })

  it('should append the section name', () => {
    updateAppTitle('CIB seven', 'Tasks')

    expect(document.title).toBe('CIB seven | Tasks')
  })

  it('should append the task name after the section', () => {
    updateAppTitle('CIB seven', 'Tasks', 'Approve invoice')

    expect(document.title).toBe('CIB seven | Tasks | Approve invoice')
  })

  // Without a section there is nothing for the task name to hang off, so it is dropped.
  it('should ignore the task name when there is no section', () => {
    updateAppTitle('CIB seven', undefined, 'Approve invoice')

    expect(document.title).toBe('CIB seven')
  })
})

describe('applyTheme', () => {
  it('should add a favicon link pointing into the theme folder', () => {
    applyTheme('dark')

    const link = document.head.querySelector('link[rel="icon"]')
    expect(link).not.toBeNull()
    expect(link.getAttribute('href')).toBe('themes/dark/favicon.ico')
    expect(link.getAttribute('type')).toBe('image/x-icon')
  })
})

describe('loadTheme', () => {
  // The theme SCSS files are resolved through import.meta.glob, so only real theme names
  // are loadable; anything else warns rather than throwing.
  it('should warn and add no style for an unknown theme', async () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})

    await loadTheme('no-such-theme')

    expect(warn).toHaveBeenCalledWith(
      'Theme "no-such-theme" not found, available themes:',
      expect.any(Array)
    )
    expect(document.head.querySelector('style')).toBeNull()
  })

  it('should inject a style element for a bundled theme', async () => {
    await loadTheme('cib')

    expect(document.head.querySelector('style')).not.toBeNull()
  })
})

describe('fetchAndStoreProcesses', () => {
  const setup = ({ extraInfo = false, favorites = null, processes = [{ key: 'invoice' }] } = {}) => {
    const app = { processesByPermissions: vi.fn((_permissions, result) => result) }
    const store = {
      dispatch: vi.fn(() => Promise.resolve(processes)),
      commit: vi.fn()
    }
    const config = { permissions: { startProcess: { user: ['*'] } } }
    if (favorites) localStorage.setItem('favorites', JSON.stringify(favorites))
    return { app, store, config, extraInfo }
  }

  it('should fetch the plain process list by default', async () => {
    const { app, store, config } = setup()

    await fetchAndStoreProcesses(app, store, config, false)

    expect(store.dispatch).toHaveBeenCalledWith('findProcesses')
  })

  it('should fetch the enriched list when extra info is requested', async () => {
    const { app, store, config } = setup()

    await fetchAndStoreProcesses(app, store, config, true)

    expect(store.dispatch).toHaveBeenCalledWith('findProcessesWithInfo')
  })

  it('should filter the result by the start-process permission and store it', async () => {
    const { app, store, config } = setup({ processes: [{ key: 'invoice' }] })

    await fetchAndStoreProcesses(app, store, config, false)

    // The permission slice is passed by reference; the process objects are mutated in
    // place afterwards, so the recorded argument already carries `loading`.
    expect(app.processesByPermissions.mock.calls[0][0]).toEqual({ user: ['*'] })
    expect(store.commit).toHaveBeenCalledWith('setProcesses', {
      processes: [{ key: 'invoice', loading: false }]
    })
  })

  // Every process starts out not loading, so the list can show per-row spinners later.
  it('should mark every process as not loading', async () => {
    const { app, store, config } = setup({ processes: [{ key: 'a' }, { key: 'b' }] })

    await fetchAndStoreProcesses(app, store, config, false)

    const { processes } = store.commit.mock.calls[0][1]
    expect(processes.every(p => p.loading === false)).toBe(true)
  })

  it('should apply favorites persisted in local storage', async () => {
    const { app, store, config } = setup({ favorites: ['invoice'] })

    await fetchAndStoreProcesses(app, store, config, false)

    expect(store.dispatch).toHaveBeenCalledWith('setFavorites', { favorites: ['invoice'] })
  })

  it('should not touch favorites when none are persisted', async () => {
    const { app, store, config } = setup()

    await fetchAndStoreProcesses(app, store, config, false)

    expect(store.dispatch).not.toHaveBeenCalledWith('setFavorites', expect.anything())
  })
})

describe('fetchDecisionsIfEmpty', () => {
  const storeWith = (list, dispatched = []) => ({
    state: { decision: { list } },
    dispatch: vi.fn(() => Promise.resolve(dispatched)),
    commit: vi.fn()
  })

  it('should fetch and reduce the decision list when the store is empty', async () => {
    const store = storeWith([], [{
      key: 'invoice', id: 'dd1', name: 'Invoice', latestVersion: 2, extra: 'dropped'
    }])

    const result = await fetchDecisionsIfEmpty(store)

    expect(store.dispatch).toHaveBeenCalledWith('getDecisionList', { latestVersion: true })
    expect(result).toEqual([{ key: 'invoice', id: 'dd1', name: 'Invoice', latestVersion: 2 }])
    expect(store.commit).toHaveBeenCalledWith('setDecisions', { decisions: result })
  })

  // Avoids a redundant round trip on every navigation into the decisions section.
  it('should return the cached list without fetching when one is present', async () => {
    const cached = [{ key: 'invoice' }]
    const store = storeWith(cached)

    const result = await fetchDecisionsIfEmpty(store)

    expect(result).toBe(cached)
    expect(store.dispatch).not.toHaveBeenCalled()
    expect(store.commit).not.toHaveBeenCalled()
  })
})

describe('setupTaskNotifications', () => {
  let postMessage
  let addEventListener
  let originalWorker
  let originalNotification

  const app = { $t: (key) => key, processesByPermissions: vi.fn() }

  const rootWith = ({ enabled = true, interval = 30 } = {}) => ({
    config: { notifications: { tasks: { enabled, interval } } },
    user: { id: 'demo' }
  })

  beforeEach(() => {
    postMessage = vi.fn()
    addEventListener = vi.fn()
    originalWorker = window.Worker
    originalNotification = window.Notification

    window.Worker = vi.fn(function MockWorker() {
      this.postMessage = postMessage
      this.addEventListener = addEventListener
    })
    window.Notification = vi.fn(function MockNotification() {
      this.close = vi.fn()
    })
    window.Notification.permission = 'granted'
    sessionStorage.clear()
  })

  afterEach(() => {
    window.Worker = originalWorker
    window.Notification = originalNotification
  })

  it('should start the worker and hand it the polling configuration', () => {
    sessionStorage.setItem('token', 'tok-session')

    setupTaskNotifications(app, rootWith({ interval: 45 }), 'cib')

    expect(window.Worker).toHaveBeenCalled()
    expect(postMessage).toHaveBeenCalledWith({
      type: 'setup',
      interval: 45,
      authToken: 'tok-session',
      userId: 'demo',
      servicesBasePath: `${window.location.origin}/${getServicesBasePath()}`
    })
  })

  it('should trigger an immediate check after setup', () => {
    setupTaskNotifications(app, rootWith(), 'cib')

    expect(postMessage).toHaveBeenNthCalledWith(2, { type: 'checkNewTasks' })
  })

  it('should fall back to the remembered token', () => {
    localStorage.setItem('token', 'tok-local')

    setupTaskNotifications(app, rootWith(), 'cib')

    expect(postMessage).toHaveBeenCalledWith(expect.objectContaining({ authToken: 'tok-local' }))
  })

  it('should not start a worker when notifications are disabled in the config', () => {
    setupTaskNotifications(app, rootWith({ enabled: false }), 'cib')

    expect(window.Worker).not.toHaveBeenCalled()
  })

  // The user can mute task notifications locally, independently of the deployment config.
  it('should not start a worker when the user muted notifications', () => {
    localStorage.setItem('tasksCheckNotificationsDisabled', 'true')

    setupTaskNotifications(app, rootWith(), 'cib')

    expect(window.Worker).not.toHaveBeenCalled()
  })

  it.each([['denied'], ['default']])('should not start a worker when permission is %s', (permission) => {
    window.Notification.permission = permission

    setupTaskNotifications(app, rootWith(), 'cib')

    expect(window.Worker).not.toHaveBeenCalled()
  })

  it('should not start a worker when the browser has no Worker support', () => {
    window.Worker = undefined

    expect(() => setupTaskNotifications(app, rootWith(), 'cib')).not.toThrow()
  })

  describe('worker messages', () => {
    const emit = (data) => {
      setupTaskNotifications(app, rootWith(), 'dark')
      const handler = addEventListener.mock.calls.find(([event]) => event === 'message')[1]
      handler({ data })
    }

    it('should raise a themed notification when the worker reports new tasks', () => {
      emit({ type: 'sendNotification' })

      expect(window.Notification).toHaveBeenCalledWith('notification.newTasksTitle', {
        body: 'notification.newTasks',
        tag: 'cib-flow-check-new-tasks',
        icon: 'themes/dark/notification-icon.svg'
      })
    })

    it.each([[{ type: 'somethingElse' }], [null], [undefined]])('should ignore the message %j', (data) => {
      emit(data)

      expect(window.Notification).not.toHaveBeenCalled()
    })

    // Clicking through should dismiss the toast and bring the app back to the foreground.
    it('should close the notification and focus the window when clicked', () => {
      const focus = vi.spyOn(window, 'focus').mockImplementation(() => {})
      emit({ type: 'sendNotification' })

      const notification = window.Notification.mock.instances[0]
      notification.onclick()

      expect(notification.close).toHaveBeenCalled()
      expect(focus).toHaveBeenCalled()
    })
  })
})

describe('handleAxiosError on 401', () => {
  const routerWith = (path = '/seven/auth/tasks') => ({
    currentRoute: { value: { name: 'tasks' }, path },
    push: vi.fn()
  })
  const rootWith = () => ({ user: { authToken: 'tok-old' }, $refs: { error: { show: vi.fn() } } })

  const unauthorized = (data) => ({
    response: { status: 401, data },
    config: { headers: {} }
  })

  beforeEach(() => {
    sessionStorage.clear()
    vi.spyOn(console, 'info').mockImplementation(() => {})
    vi.spyOn(console, 'warn').mockImplementation(() => {})
  })

  // The interceptor swaps in the prolonged token and replays the original request, so a
  // merely stale token never surfaces to the user.
  it('should prolong the token and repeat the failed request', async () => {
    sessionStorage.setItem('token', 'tok-expired')
    const root = rootWith()
    const error = unauthorized({ type: 'TokenExpiredException', params: ['tok-prolonged'] })

    const promise = handleAxiosError(routerWith(), root, error)

    expect(sessionStorage.getItem('token')).toBe('tok-prolonged')
    expect(root.user.authToken).toBe('tok-prolonged')
    expect(error.config.headers.authorization).toBe('tok-prolonged')
    expect(axios.request).toHaveBeenCalledWith(error.config)
    await promise
  })

  it('should write the prolonged token back to localStorage when it lived there', async () => {
    localStorage.setItem('token', 'tok-expired')
    const error = unauthorized({ type: 'TokenExpiredException', params: ['tok-prolonged'] })

    await handleAxiosError(routerWith(), rootWith(), error)

    expect(localStorage.getItem('token')).toBe('tok-prolonged')
    expect(sessionStorage.getItem('token')).toBeNull()
  })

  it('should log the user out and redirect when the token cannot be prolonged', async () => {
    sessionStorage.setItem('token', 'tok-session')
    const root = rootWith()
    const router = routerWith('/seven/auth/tasks')

    await expect(handleAxiosError(router, root, unauthorized({ type: 'AuthenticationException' })))
      .rejects.toBeDefined()

    expect(sessionStorage.getItem('token')).toBeNull()
    expect(root.user).toBeNull()
    expect(axios.defaults.headers.common.authorization).toBe('')
    expect(router.push).toHaveBeenCalledWith('/seven/login?nextUrl=/seven/auth/tasks')
  })

  it('should clear the remembered token when there is no session token', async () => {
    localStorage.setItem('token', 'tok-local')

    await expect(handleAxiosError(routerWith(), rootWith(), unauthorized({ type: 'AuthenticationException' })))
      .rejects.toBeDefined()

    expect(localStorage.getItem('token')).toBeNull()
  })

  it('should redirect without a nextUrl when the current path is unknown', async () => {
    // Built inline rather than via routerWith(), whose default would fill the path back in.
    const router = { currentRoute: { value: { name: 'tasks' } }, push: vi.fn() }

    await expect(handleAxiosError(router, rootWith(), unauthorized({ type: 'AuthenticationException' })))
      .rejects.toBeDefined()

    expect(router.push).toHaveBeenCalledWith('/seven/login')
  })

  it.each([[[]], [undefined]])('should treat an expired token with params %j as a logout', async (params) => {
    const router = routerWith()

    await expect(handleAxiosError(router, rootWith(), unauthorized({ type: 'TokenExpiredException', params })))
      .rejects.toBeDefined()

    expect(router.push).toHaveBeenCalled()
  })

  it('should reject other statuses without showing a dialog', async () => {
    const root = rootWith()

    await expect(handleAxiosError(routerWith(), root, { response: { status: 404, data: {} } }))
      .rejects.toBeDefined()

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })
})

describe('handleAxiosError without a response', () => {
  const routerWith = (name) => ({ currentRoute: { value: { name }, path: '/x' }, push: vi.fn() })
  const rootWith = () => ({ user: {}, $refs: { error: { show: vi.fn() } } })

  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  // A transport failure has no envelope to interpret; the message is turned into a
  // translation key, which is why the space is replaced.
  it('should show a network error as a translatable type', async () => {
    const root = rootWith()

    await expect(handleAxiosError(routerWith('tasks'), root, new Error('Network Error')))
      .rejects.toBeDefined()

    expect(root.$refs.error.show).toHaveBeenCalledWith({ type: 'Network_Error' })
    expect(console.error).toHaveBeenCalledWith('Strange AJAX error', expect.any(Error))
  })

  // An aborted request is normal when the user navigates away mid-flight.
  it('should stay silent for an aborted request', async () => {
    const root = rootWith()

    await expect(handleAxiosError(routerWith('tasks'), root, new Error('Request aborted')))
      .rejects.toBeDefined()

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })

  // Deployed forms run inside an iframe and report errors to the embedding page over
  // postMessage, so an error dialog there would be invisible or duplicated.
  it.each([['deployed-form'], ['start-deployed-form']])('should stay silent on the %s route', async (name) => {
    const root = rootWith()

    await expect(handleAxiosError(routerWith(name), root, new Error('Network Error')))
      .rejects.toBeDefined()

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })

  it('should suppress a 500 dialog on a deployed form route', async () => {
    const root = rootWith()
    const error = { response: { status: 500, data: { type: 'SystemException' } } }

    await expect(handleAxiosError(routerWith('deployed-form'), root, error)).rejects.toBeDefined()

    expect(root.$refs.error.show).not.toHaveBeenCalled()
  })

  it('should show a known 500 exception outside deployed forms', async () => {
    const root = rootWith()
    const error = { response: { status: 500, data: { type: 'SystemException' } } }

    await expect(handleAxiosError(routerWith('tasks'), root, error)).rejects.toBeDefined()

    expect(root.$refs.error.show).toHaveBeenCalledWith({ type: 'SystemException' })
  })
})
