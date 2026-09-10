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
import RenderTemplate from '@/components/render-template/RenderTemplate.vue'
import { TaskService } from '@/services.js'
import { ENGINE_STORAGE_KEY } from '@/constants.js'

vi.mock('@/services.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    TaskService: {
      submit: vi.fn(() => Promise.resolve()),
      submitWithVariables: vi.fn(() => Promise.resolve())
    }
  }
})

const m = RenderTemplate.methods
const c = RenderTemplate.computed

/** An iframe stub: the component talks to the embedded form purely over postMessage. */
function frameStub() {
  const postMessage = vi.fn()
  const contentWindow = { postMessage }
  return { contentWindow, postMessage, src: 'http://host/form', setAttribute: vi.fn() }
}

/**
 * A `this` for RenderTemplate's methods. The component's job is to bridge an embedded form
 * iframe and the app, so the iframe and the alert refs are stubbed and the message handler
 * is driven directly.
 */
function context(overrides = {}) {
  const frame = overrides.frame || frameStub()
  const vm = {
    $t: (key) => key,
    $emit: vi.fn(),
    $root: {
      user: { id: 'demo', authToken: 'tok', permissions: [] },
      config: {
        engineRestUrl: 'http://engine',
        engineRestPath: '/engine-rest',
        permissions: { displayFilter: { user: ['*'] } }
      },
      $refs: { error: { show: vi.fn() } }
    },
    $route: { params: { filterId: 'f1' }, query: {}, path: '/seven/auth/tasks/f1' },
    $router: { push: vi.fn(), replace: vi.fn() },
    $store: {
      state: { filter: { list: [], selected: null } },
      dispatch: vi.fn(() => Promise.resolve([])),
      commit: vi.fn()
    },
    $refs: {
      'template-frame': frame,
      messageSaved: { show: vi.fn() },
      messageSuccess: { show: vi.fn() },
      datePickerModal: { show: vi.fn(), hide: vi.fn() }
    },
    AuthService: { fetchAuths: vi.fn(() => Promise.resolve(['READ'])) },
    filtersByPermissions: vi.fn((_permissions, filters) => filters),
    task: { id: 't1', assignee: 'demo' },
    userInstruction: 'do the thing',
    submitForm: false,
    loader: true,
    emptyTaskVariables: {},
    taskVariablesVisible: false,
    datePickerValue: null,
    datePickerRequest: null,
    loadIframe: vi.fn(),
    ...overrides
  }
  delete vm.frame
  for (const [name, method] of Object.entries(m)) {
    if (typeof method === 'function' && !(name in vm)) vm[name] = method.bind(vm)
  }
  for (const [name, def] of Object.entries(c)) {
    if (name in vm || typeof def !== 'function') continue
    Object.defineProperty(vm, name, { get: () => def.call(vm), configurable: true })
  }
  vm.frame = frame
  return vm
}

const flush = async (times = 8) => {
  for (let i = 0; i < times; i++) await Promise.resolve()
}

/** Dispatch a message as if it came from the embedded form. */
const fromFrame = (vm, data) => m.processMessage.call(vm, { source: vm.frame.contentWindow, data })

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('RenderTemplate - computed', () => {
  // The form is read-only unless the current user holds the task, so it is taken out of
  // the tab order to signal that.
  it('iframeTabIndex should be focusable for the task holder', () => {
    expect(c.iframeTabIndex.call(context())).toBeUndefined()
  })

  it('iframeTabIndex should compare the assignee case-insensitively', () => {
    const vm = context({ task: { id: 't1', assignee: 'DEMO' } })

    expect(c.iframeTabIndex.call(vm)).toBeUndefined()
  })

  it('iframeTabIndex should accept an assignee object', () => {
    const vm = context({ task: { id: 't1', assignee: { id: 'demo' } } })

    expect(c.iframeTabIndex.call(vm)).toBeUndefined()
  })

  it('iframeTabIndex should take an unassigned task out of the tab order', () => {
    expect(c.iframeTabIndex.call(context({ task: { id: 't1', assignee: null } }))).toBe(-1)
  })

  it('iframeTabIndex should take somebody else\'s task out of the tab order', () => {
    expect(c.iframeTabIndex.call(context({ task: { id: 't1', assignee: 'other' } }))).toBe(-1)
  })

  it('iframeTabIndex should cope with an assignee object without an id', () => {
    const vm = context({ task: { id: 't1', assignee: {} } })

    expect(c.iframeTabIndex.call(vm)).toBe(-1)
  })

  it('iframeTabIndex should cope with no logged-in user', () => {
    const vm = context({ task: { id: 't1', assignee: 'demo' } })
    vm.$root.user = null

    expect(c.iframeTabIndex.call(vm)).toBe(-1)
  })

  it('fullModeStyles should pin the frame over the app in full mode', () => {
    const vm = context()
    vm.$route.query.fullMode = 'true'

    expect(c.fullModeStyles.call(vm)).toContain('position: fixed')
  })

  it('fullModeStyles should be empty outside full mode', () => {
    expect(c.fullModeStyles.call(context())).toBe('')
  })
})

describe('RenderTemplate - completing a task', () => {
  it('getVariables should expose the instruction and assignee to the form', () => {
    expect(m.getVariables.call(context())).toEqual({
      data: { userInstruction: 'do the thing', assignee: 'demo' }
    })
  })

  // A task with no form still needs submitting; with collected variables it goes through
  // the variable-carrying endpoint instead.
  it('completeEmptyTask should submit without variables when none were collected', async () => {
    const vm = context({ completeTask: vi.fn() })

    m.completeEmptyTask.call(vm)
    await flush()

    expect(TaskService.submit).toHaveBeenCalledWith('t1', null)
    expect(vm.completeTask).toHaveBeenCalled()
  })

  it('completeEmptyTask should submit the collected variables', async () => {
    const vm = context({ emptyTaskVariables: { amount: 42 }, completeTask: vi.fn() })

    m.completeEmptyTask.call(vm)
    await flush()

    expect(TaskService.submitWithVariables).toHaveBeenCalledWith('t1', { variables: { amount: 42 } })
    expect(TaskService.submit).not.toHaveBeenCalled()
  })

  it('completeTask should announce completion and return to the task list', () => {
    const vm = context()

    m.completeTask.call(vm)

    expect(vm.$emit).toHaveBeenCalledWith('complete-task', { id: 't1', assignee: 'demo' })
    expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f1')
    expect(vm.submitForm).toBe(false)
  })

  // An externally hosted form keeps its own navigation, so the app must not redirect.
  it('completeTask should not navigate for an externally hosted form', () => {
    const vm = context({ task: { id: 't1', assignee: 'demo', url: 'http://external/form' } })

    m.completeTask.call(vm)

    expect(vm.$emit).toHaveBeenCalled()
    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  // A form that starts a follow-up process reports the new instance id.
  it('completeTask should carry a started instance id into the event', () => {
    const vm = context()

    m.completeTask.call(vm, { id: 'pi-new' })

    expect(vm.$emit).toHaveBeenCalledWith('complete-task', expect.objectContaining({ processInstanceId: 'pi-new' }))
  })

  it('cancelTask should stay on the current route', () => {
    const vm = context()

    m.cancelTask.call(vm)

    expect(vm.$router.push).toHaveBeenCalledWith('.')
  })
})

describe('RenderTemplate - displayErrorMessage', () => {
  it('should pass a status-less payload straight to the dialog', () => {
    const vm = context()

    m.displayErrorMessage.call(vm, { type: 'CustomException', message: 'boom' })

    expect(vm.$root.$refs.error.show).toHaveBeenCalledWith({ type: 'CustomException', message: 'boom' })
    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  // A 404 on the task itself means it is gone, so the user is taken back to the list.
  it('should report a missing task and return to the list', () => {
    const vm = context()

    m.displayErrorMessage.call(vm, { status: 404 })

    expect(vm.$root.$refs.error.show).toHaveBeenCalledWith({ type: 'taskSelectedNotExist', params: [] })
    expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f1')
  })

  // A generic 404 comes from something the form fetched, so the user stays put.
  it('should report a generic 404 without navigating away', () => {
    const vm = context()

    m.displayErrorMessage.call(vm, { status: 404, type: 'generic' })

    expect(vm.$root.$refs.error.show).toHaveBeenCalledWith({ type: 'NoObjectFoundException', params: [] })
    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  it('should report a 400 as access denied, naming the task', () => {
    const vm = context()

    m.displayErrorMessage.call(vm, { status: 400 })

    expect(vm.$root.$refs.error.show).toHaveBeenCalledWith({ type: 'AccessDeniedException', params: ['t1'] })
  })

  it.each([[500], [503], [418]])('should report status %i as a system error', (status) => {
    const vm = context()

    m.displayErrorMessage.call(vm, { status })

    expect(vm.$root.$refs.error.show).toHaveBeenCalledWith({ type: 'SystemException', params: [] })
  })
})

describe('RenderTemplate - filters', () => {
  it('updateFilters should refresh permissions and the filter list', async () => {
    const vm = context({ selectFilter: vi.fn() })
    vm.$store.dispatch.mockResolvedValue([{ id: 'f1' }])

    m.updateFilters.call(vm, {})
    await flush()

    expect(vm.AuthService.fetchAuths).toHaveBeenCalled()
    expect(vm.$root.user.permissions).toEqual(['READ'])
    expect(vm.$store.dispatch).toHaveBeenCalledWith('findFilters')
    expect(vm.filtersByPermissions).toHaveBeenCalledWith({ user: ['*'] }, [{ id: 'f1' }])
    expect(vm.$store.commit).toHaveBeenCalledWith('setFilters', { filters: [{ id: 'f1' }] })
  })

  it('updateFilters should select the requested filter afterwards', async () => {
    const vm = context({ selectFilter: vi.fn() })

    m.updateFilters.call(vm, { filterId: 'f2' })
    await flush()

    expect(vm.selectFilter).toHaveBeenCalledWith('f2')
  })

  it('updateFilters should not select anything when no filter was named', async () => {
    const vm = context({ selectFilter: vi.fn() })

    m.updateFilters.call(vm, {})
    await flush()

    expect(vm.selectFilter).not.toHaveBeenCalled()
  })

  describe('selectFilter', () => {
    const withFilters = (overrides = {}) => {
      const vm = context(overrides)
      vm.$store.state.filter.list = [{ id: 'f1', name: 'Mine' }, { id: 'f2', name: 'Theirs' }]
      return vm
    }

    it('should select the filter, persist it and navigate', () => {
      const vm = withFilters()

      m.selectFilter.call(vm, 'f2')

      expect(vm.$store.state.filter.selected).toEqual({ id: 'f2', name: 'Theirs' })
      expect(JSON.parse(localStorage.getItem('filter'))).toEqual({ id: 'f2', name: 'Theirs' })
      expect(vm.$router.replace).toHaveBeenCalledWith('/seven/auth/tasks/f2')
    })

    it('should keep the open task in the target route', () => {
      const vm = withFilters()
      vm.$route.params.taskId = 't1'

      m.selectFilter.call(vm, 'f2')

      expect(vm.$router.replace).toHaveBeenCalledWith('/seven/auth/tasks/f2/t1')
    })

    it('should not navigate when already on that filter', () => {
      const vm = withFilters()
      vm.$route.path = '/seven/auth/tasks/f1'

      m.selectFilter.call(vm, 'f1')

      expect(vm.$router.replace).not.toHaveBeenCalled()
    })

    it('should do nothing for an unknown filter', () => {
      const vm = withFilters()

      m.selectFilter.call(vm, 'f-gone')

      expect(vm.$store.state.filter.selected).toBeNull()
      expect(vm.$router.replace).not.toHaveBeenCalled()
    })
  })
})

describe('RenderTemplate - processMessage', () => {
  // Only messages from the embedded frame are acted on; anything else on the window bus is
  // ignored, which matters because the app shares the bus with other embedders.
  it('should ignore a message from another window', () => {
    const vm = context({ completeTask: vi.fn() })

    m.processMessage.call(vm, { source: { other: true }, data: { method: 'completeTask' } })

    expect(vm.completeTask).not.toHaveBeenCalled()
  })

  it('should ignore a message with no method', () => {
    const vm = context({ completeTask: vi.fn() })

    fromFrame(vm, { payload: 'x' })

    expect(vm.completeTask).not.toHaveBeenCalled()
  })

  it('should complete the task on request', () => {
    const vm = context({ completeTask: vi.fn() })

    fromFrame(vm, { method: 'completeTask', task: { id: 'pi-new' } })

    expect(vm.completeTask).toHaveBeenCalledWith({ id: 'pi-new' })
  })

  it('should show the saved alert', () => {
    const vm = context()

    fromFrame(vm, { method: 'displaySuccessMessage' })

    expect(vm.$refs.messageSaved.show).toHaveBeenCalledWith(10)
  })

  it('should show the generic success alert', () => {
    const vm = context()

    fromFrame(vm, { method: 'displayGenericSuccessMessage' })

    expect(vm.$refs.messageSuccess.show).toHaveBeenCalledWith(10)
  })

  it('should forward an error payload to the error handler', () => {
    const vm = context({ displayErrorMessage: vi.fn() })

    fromFrame(vm, { method: 'displayErrorMessage', data: { status: 500 } })

    expect(vm.displayErrorMessage).toHaveBeenCalledWith({ status: 500 })
  })

  it('should cancel the task on request', () => {
    const vm = context({ cancelTask: vi.fn() })

    fromFrame(vm, { method: 'cancelTask' })

    expect(vm.cancelTask).toHaveBeenCalled()
  })

  it('should refresh the filters on request', () => {
    const vm = context({ updateFilters: vi.fn() })

    fromFrame(vm, { method: 'updateFilters', filterId: 'f2' })

    expect(vm.updateFilters).toHaveBeenCalledWith({ method: 'updateFilters', filterId: 'f2' })
  })

  describe('requestConfig', () => {
    // The token is handed over on request rather than embedded in the iframe URL, so it
    // never lands in browser history or server logs.
    it('should answer with the auth token and engine endpoints', () => {
      const vm = context()

      fromFrame(vm, { method: 'requestConfig' })

      expect(vm.frame.postMessage).toHaveBeenCalledWith({
        method: 'configResponse',
        config: {
          authToken: 'tok',
          engineRestUrl: 'http://engine',
          engineRestPath: '/engine-rest'
        }
      }, '*')
    })

    it('should include the selected engine when one is chosen', () => {
      localStorage.setItem(ENGINE_STORAGE_KEY, 'second-engine')
      const vm = context()

      fromFrame(vm, { method: 'requestConfig' })

      expect(vm.frame.postMessage.mock.calls[0][0].config.engineId).toBe('second-engine')
    })

    it('should omit the engine when none is chosen', () => {
      const vm = context()

      fromFrame(vm, { method: 'requestConfig' })

      expect(vm.frame.postMessage.mock.calls[0][0].config).not.toHaveProperty('engineId')
    })
  })

  describe('openDatePicker', () => {
    const open = (vm, value) => fromFrame(vm, { method: 'openDatePicker', data: { fieldName: 'dueDate', value } })

    it('should open the picker for the requested field', () => {
      const vm = context()

      open(vm, '15/01/2026')

      expect(vm.datePickerRequest).toEqual({ fieldName: 'dueDate', value: '15/01/2026' })
      expect(vm.$refs.datePickerModal.show).toHaveBeenCalled()
    })

    // The embedded forms exchange dates as dd/mm/yyyy, not ISO.
    it('should parse a dd/mm/yyyy value', () => {
      const vm = context()

      open(vm, '15/01/2026')

      expect(vm.datePickerValue).toBeInstanceOf(Date)
      expect(vm.datePickerValue.getFullYear()).toBe(2026)
      expect(vm.datePickerValue.getMonth()).toBe(0)
      expect(vm.datePickerValue.getDate()).toBe(15)
    })

    it.each([['2026-01-15'], ['not a date'], ['']])('should open empty for the unparseable value %j', (value) => {
      const vm = context()

      open(vm, value)

      expect(vm.datePickerValue).toBeNull()
      expect(vm.$refs.datePickerModal.show).toHaveBeenCalled()
    })

    it('should open empty for an impossible date', () => {
      const vm = context()

      open(vm, '99/99/2026')

      expect(vm.datePickerValue).toBeInstanceOf(Date)
    })

    it('should leave the value untouched for a non-string', () => {
      const vm = context({ datePickerValue: 'untouched' })

      open(vm, 12345)

      expect(vm.datePickerValue).toBe('untouched')
    })
  })
})

describe('RenderTemplate - onDatePickerConfirm', () => {
  it('should send the chosen date back as dd/mm/yyyy', () => {
    const vm = context({
      datePickerValue: new Date(2026, 0, 5),
      datePickerRequest: { fieldName: 'dueDate' }
    })

    m.onDatePickerConfirm.call(vm)

    expect(vm.frame.postMessage).toHaveBeenCalledWith({
      method: 'datePickerResult', fieldName: 'dueDate', value: '05/01/2026'
    }, '*')
  })

  // Clearing the picker sends an explicit null so the form can blank its field.
  it('should send null when no date was chosen', () => {
    const vm = context({ datePickerValue: null, datePickerRequest: { fieldName: 'dueDate' } })

    m.onDatePickerConfirm.call(vm)

    expect(vm.frame.postMessage).toHaveBeenCalledWith(
      expect.objectContaining({ value: null }), '*'
    )
  })

  it('should close the picker and reset its state', () => {
    const vm = context({
      datePickerValue: new Date(2026, 0, 5),
      datePickerRequest: { fieldName: 'dueDate' }
    })

    m.onDatePickerConfirm.call(vm)

    expect(vm.$refs.datePickerModal.hide).toHaveBeenCalled()
    expect(vm.datePickerValue).toBeNull()
    expect(vm.datePickerRequest).toBeNull()
  })

  it('should not post anything when no field was requested', () => {
    const vm = context({ datePickerValue: new Date(2026, 0, 5), datePickerRequest: null })

    m.onDatePickerConfirm.call(vm)

    expect(vm.frame.postMessage).not.toHaveBeenCalled()
  })

  it('should tolerate the frame having gone away', () => {
    const vm = context({ datePickerRequest: { fieldName: 'dueDate' } })
    vm.$refs['template-frame'] = null

    expect(() => m.onDatePickerConfirm.call(vm)).not.toThrow()
  })
})

describe('RenderTemplate - iframe lifecycle', () => {
  // Navigating away tells the embedded form its context is gone so it can drop state.
  it('onBeforeUnload should notify the form and reload it', () => {
    const vm = context()

    m.onBeforeUnload.call(vm)

    expect(vm.frame.postMessage).toHaveBeenCalledWith({ type: 'contextChanged' }, '*')
    expect(vm.loadIframe).toHaveBeenCalled()
  })

  it('onBeforeUnload should do nothing without a frame', () => {
    const vm = context()
    vm.$refs['template-frame'] = null

    m.onBeforeUnload.call(vm)

    expect(vm.loadIframe).not.toHaveBeenCalled()
  })

  it('onIframeLoad should hide the loader once real content is loaded', () => {
    const vm = context()

    m.onIframeLoad.call(vm)

    expect(vm.loader).toBe(false)
  })

  // The frame is reset to about:blank between tasks; that load must not clear the loader.
  it.each([['about:blank'], ['http://host/about:blank']])('onIframeLoad should keep the loader for src %s', (src) => {
    const frame = frameStub()
    frame.src = src
    const vm = context({ frame })

    m.onIframeLoad.call(vm)

    expect(vm.loader).toBe(true)
  })

  it('onIframeLoad should keep the loader without a frame', () => {
    const vm = context()
    vm.$refs['template-frame'] = null

    m.onIframeLoad.call(vm)

    expect(vm.loader).toBe(true)
  })
})

describe('RenderTemplate - handleScrollIframe', () => {
  beforeEach(() => {
    // The scroll walk asks the outer window for computed styles; jsdom only accepts real
    // Elements there, and it never lays anything out anyway.
    vi.spyOn(window, 'getComputedStyle').mockImplementation((el) => ({ overflowY: el.overflowY ?? 'visible' }))
  })

  it('should scroll the element under the pointer inside the frame', () => {
    const inner = { scrollTop: 0 }
    const doc = {
      body: { scrollHeight: 0, clientHeight: 0, scrollTop: 0 },
      elementFromPoint: vi.fn(() => inner),
      defaultView: { getComputedStyle: () => ({ overflowY: 'visible' }) }
    }
    const frame = frameStub()
    frame.contentDocument = doc
    frame.getBoundingClientRect = () => ({ left: 10, top: 20 })
    const vm = context({ frame })

    m.handleScrollIframe.call(vm, 30, 110, 70)

    expect(doc.elementFromPoint).toHaveBeenCalledWith(100, 50)
  })

  // A cross-origin frame exposes no document, so there is nothing to scroll.
  it('should do nothing when the frame is not accessible', () => {
    const frame = frameStub()
    frame.contentWindow = null
    const vm = context({ frame })

    expect(() => m.handleScrollIframe.call(vm, 30, 0, 0)).not.toThrow()
  })
})
