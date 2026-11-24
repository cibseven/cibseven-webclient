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
import ProcessDefinitionView from '@/components/process/ProcessDefinitionView.vue'
import { ProcessService, HistoryService, TaskService } from '@/services.js'

vi.mock('@/services.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ProcessService: {
      findProcessInstance: vi.fn(() => Promise.resolve(null)),
      findProcessById: vi.fn(() => Promise.resolve({})),
      findProcessVersionsByDefinitionKey: vi.fn(() => Promise.resolve([])),
      findProcessStatistics: vi.fn(() => Promise.resolve([])),
      findActivityInstance: vi.fn(() => Promise.resolve({})),
      deleteProcessDefinition: vi.fn(() => Promise.resolve())
    },
    HistoryService: {
      findProcessInstance: vi.fn(() => Promise.resolve(null)),
      findActivitiesInstancesHistory: vi.fn(() => Promise.resolve([])),
      findTasksByDefinitionKeyHistory: vi.fn(() => Promise.resolve([])),
      fetchActivityVariablesHistory: vi.fn(() => Promise.resolve([]))
    },
    TaskService: { fetchActivityVariables: vi.fn(() => Promise.resolve([])) }
  }
})

const m = ProcessDefinitionView.methods
const c = ProcessDefinitionView.computed

const version = (overrides = {}) => ({ id: 'pd:2:abc', key: 'invoice', version: '2', ...overrides })

/**
 * A `this` for ProcessDefinitionView's methods. The component composes the instances view,
 * the diagram and both sidebars; its own version-resolution, instance-selection and export
 * logic is what these tests exercise, so children and mapped store helpers are stubbed.
 */
function context(overrides = {}) {
  const vm = {
    $t: (key) => key,
    $root: { config: { camundaHistoryLevel: 'full', lazyLoadHistory: false } },
    $route: { query: {} },
    $router: { push: vi.fn(), replace: vi.fn() },
    $store: { dispatch: vi.fn(() => Promise.resolve()) },
    $refs: { process: { refreshDiagram: vi.fn() }, importPopper: { triggerDownload: vi.fn() } },
    formatDate: (value) => (value ? `fmt(${value})` : ''),
    getProcessById: vi.fn(() => Promise.resolve(null)),
    clearActivitySelection: vi.fn(),
    saveLeftOpen: vi.fn(),
    getSavedLeftOpen: vi.fn(() => true),
    processKey: 'invoice',
    versionIndex: '2',
    instanceId: undefined,
    tenantId: undefined,
    leftOpen: true,
    process: null,
    processDefinitions: [],
    errorVersionNotFound: null,
    errorLoadingInstanceId: null,
    selectedInstance: null,
    task: null,
    activityInstance: null,
    activityInstanceHistory: null,
    filter: { unfinished: true },
    parentProcess: null,
    instances: [],
    ...overrides
  }
  // Bind only what the context does not already provide, so the stubs above (the mapped
  // store helpers, the date formatter, the `instances` getter) survive.
  for (const [name, method] of Object.entries(m)) {
    if (typeof method === 'function' && !(name in vm)) vm[name] = method.bind(vm)
  }
  for (const [name, def] of Object.entries(c)) {
    if (name in vm || typeof def !== 'function') continue
    Object.defineProperty(vm, name, { get: () => def.call(vm), configurable: true })
  }
  return vm
}

beforeEach(() => {
  vi.resetAllMocks()
  localStorage.clear()
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('ProcessDefinitionView - computed', () => {
  it('sidebarScope should switch to the instance scope once one is selected', () => {
    expect(c.sidebarScope.call(context())).toBe('process-definition')
    expect(c.sidebarScope.call(context({ selectedInstance: { id: 'pi1' } }))).toBe('process-instance')
    expect(c.sidebarScope.call(context({ instanceId: 'pi1' }))).toBe('process-instance')
  })

  it('shortendLeftCaption should follow the sidebar scope', () => {
    expect(c.shortendLeftCaption.call(context())).toBe('process.details.historyVersions')
    expect(c.shortendLeftCaption.call(context({ instanceId: 'pi1' }))).toBe('process-instance.info')
  })

  // Until the definition is loaded the key is the best available label.
  it('processName should fall back through name, key and route key', () => {
    expect(c.processName.call(context())).toBe('invoice')
    expect(c.processName.call(context({ process: { key: 'k' } }))).toBe('k')
    expect(c.processName.call(context({ process: { key: 'k', name: 'Nice name' } }))).toBe('Nice name')
  })

  it('computedVersionIndex should prefer the loaded definition version', () => {
    expect(c.computedVersionIndex.call(context())).toBe('2')
    expect(c.computedVersionIndex.call(context({ process: { version: '5' } }))).toBe('5')
  })
})

describe('ProcessDefinitionView - sidebar persistence', () => {
  it('onLeftOpenChanged should persist the new state under the current scope', () => {
    const vm = context({ instanceId: 'pi1' })

    m.onLeftOpenChanged.call(vm, false)

    expect(vm.leftOpen).toBe(false)
    expect(vm.saveLeftOpen).toHaveBeenCalledWith('process-instance', false)
  })

  it('data should restore the persisted state for the entered scope', () => {
    const getSavedLeftOpen = vi.fn(() => false)

    const data = ProcessDefinitionView.data.call({ instanceId: 'pi1', getSavedLeftOpen })

    expect(getSavedLeftOpen).toHaveBeenCalledWith('process-instance')
    expect(data.leftOpen).toBe(false)
  })

  it('data should use the definition scope when no instance is addressed', () => {
    const getSavedLeftOpen = vi.fn(() => true)

    ProcessDefinitionView.data.call({ instanceId: undefined, getSavedLeftOpen })

    expect(getSavedLeftOpen).toHaveBeenCalledWith('process-definition')
  })
})

describe('ProcessDefinitionView - unfinished filter persistence', () => {
  const getSavedLeftOpen = vi.fn(() => true)

  it('data should default the "unfinished" filter to true when no preference is stored', () => {
    const data = ProcessDefinitionView.data.call({ instanceId: undefined, getSavedLeftOpen })

    expect(data.filter).toEqual({ unfinished: true })
  })

  // The key must be absent (not `unfinished: false`), since the search box treats
  // its mere presence - regardless of value - as the criterion being applied.
  it('data should restore the "unfinished" filter as off (key omitted) when the stored preference says so', () => {
    localStorage.setItem('cibseven:preferences:unfinishedInstancesFilter', 'false')

    const data = ProcessDefinitionView.data.call({ instanceId: undefined, getSavedLeftOpen })

    expect(data.filter).toEqual({})
    expect('unfinished' in data.filter).toBe(false)
  })

  it('filterInstances should persist the "unfinished" filter when it is turned off', () => {
    const vm = context()

    m.filterInstances.call(vm, { unfinished: undefined })

    expect(localStorage.getItem('cibseven:preferences:unfinishedInstancesFilter')).toBe('false')
  })

  it('filterInstances should persist the "unfinished" filter when it is turned back on', () => {
    localStorage.setItem('cibseven:preferences:unfinishedInstancesFilter', 'false')
    const vm = context()

    m.filterInstances.call(vm, { unfinished: true })

    expect(localStorage.getItem('cibseven:preferences:unfinishedInstancesFilter')).toBe('true')
  })
})

describe('ProcessDefinitionView - findProcessInstance', () => {
  // With history off the instance only exists in the runtime API.
  it('should read from history when history is enabled', async () => {
    const vm = context()

    await m.findProcessInstance.call(vm, 'pi1')

    expect(HistoryService.findProcessInstance).toHaveBeenCalledWith('pi1')
    expect(ProcessService.findProcessInstance).not.toHaveBeenCalled()
  })

  it('should read from the runtime API when history is off', async () => {
    const vm = context()
    vm.$root.config.camundaHistoryLevel = 'none'

    await m.findProcessInstance.call(vm, 'pi1')

    expect(ProcessService.findProcessInstance).toHaveBeenCalledWith('pi1')
    expect(HistoryService.findProcessInstance).not.toHaveBeenCalled()
  })
})

describe('ProcessDefinitionView - onRouteChanged', () => {
  const route = (overrides = {}) => ({ processKey: 'invoice', tenantId: undefined, versionIndex: '2', instanceId: undefined, ...overrides })

  it.each([
    ['the first sync', undefined, route()],
    ['a different key', route(), route({ processKey: 'order' })],
    ['a different instance', route(), route({ instanceId: 'pi1' })],
    ['leaving the instance', route({ instanceId: 'pi1' }), route()]
  ])('should clear the activity selection on %s', async (_, previous, next) => {
    const vm = context({ syncWithRoute: vi.fn() })

    await m.onRouteChanged.call(vm, next, previous)

    expect(vm.clearActivitySelection).toHaveBeenCalled()
    expect(vm.syncWithRoute).toHaveBeenCalled()
  })

  // the activity picked in the instances view filters its table, also across versions
  it('should keep the activity selection when only the version changes', async () => {
    const vm = context({ syncWithRoute: vi.fn() })

    await m.onRouteChanged.call(vm, route({ versionIndex: '1' }), route())

    expect(vm.clearActivitySelection).not.toHaveBeenCalled()
    expect(vm.syncWithRoute).toHaveBeenCalled()
  })
})

/**
 * Navigation scenarios run through the component's real loading methods against a fake engine.
 * The request counts are the point: CIB7-1895 was about every request of the instance view going out twice.
 */
describe('ProcessDefinitionView - syncWithRoute', () => {
  const definitionId = (key, v) => `${key}:${v}:id`
  const activeInstance = (overrides = {}) => ({ id: 'pi1', processDefinitionId: definitionId('invoice', '2'), state: 'ACTIVE', ...overrides })

  // Every call answers with new objects, like the backend does.
  function engine({ versions = ['2', '1'], instances = { pi1: activeInstance() }, tenantId = null } = {}) {
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async (key) =>
      versions.map(v => ({ id: definitionId(key, v), key, version: v, incidents: 7, deploymentTime: 'deployed' })))
    ProcessService.findProcessById.mockImplementation(async (id) => {
      const [key, v] = id.split(':')
      return { id, key, version: v, name: `${key} v${v}`, tenantId, incidents: 0, deploymentTime: null }
    })
    HistoryService.findProcessInstance.mockImplementation(async (id) => instances[id] ?? null)
    ProcessService.findActivityInstance.mockResolvedValue({ id: 'ai' })
    HistoryService.findActivitiesInstancesHistory.mockResolvedValue([{ id: 'history' }])
  }

  const open = (vm) => vm.onRouteChanged(vm.routeState, undefined)

  async function navigate(vm, props) {
    const previous = vm.routeState
    Object.assign(vm, props)
    await vm.onRouteChanged(vm.routeState, previous)
  }

  function deferred() {
    let resolve
    const promise = new Promise(r => { resolve = r })
    return { promise, resolve }
  }

  it('should open a definition with one request per resource', async () => {
    engine()
    const vm = context({ tenantId: 't1' })

    await open(vm)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', 't1', false)
    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessById).toHaveBeenCalledWith('invoice:2:id', true)
    expect(HistoryService.findProcessInstance).not.toHaveBeenCalled()
    expect(vm.process).toBe(vm.processDefinitions[0])
    expect(vm.process.name).toBe('invoice v2')
    expect(vm.errorVersionNotFound).toBeNull()
  })

  // ProcessInstancesView loads them together with its instances; loading here too doubled the request
  it('should leave the statistics to the instances view', async () => {
    engine()
    const vm = context()

    await open(vm)

    expect(vm.$store.dispatch).not.toHaveBeenCalledWith('loadStatistics', expect.anything())
  })

  // CIB7-1895: a deep link (or F5 on the instance view) fetched all of this twice
  it('should open an instance link with one request per resource', async () => {
    engine()
    const vm = context({ instanceId: 'pi1', tenantId: 't1' })

    await open(vm)

    expect(HistoryService.findProcessInstance).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', 't1', false)
    expect(vm.$store.dispatch).toHaveBeenCalledOnce()
    expect(vm.$store.dispatch).toHaveBeenCalledWith('loadStatistics', vm.process)
    expect(ProcessService.findActivityInstance).toHaveBeenCalledOnce()
    expect(HistoryService.findActivitiesInstancesHistory).toHaveBeenCalledOnce()
    expect(vm.process).toBe(vm.processDefinitions[0])
    expect(vm.process.name).toBe('invoice v2')
    expect(vm.selectedInstance).toEqual(activeInstance())
    expect(vm.activityInstance).toEqual({ id: 'ai' })
    expect(vm.activityInstanceHistory).toEqual([{ id: 'history' }])
  })

  // the backend does not fill them in on the definition: incidents always come as 0, the deployment time as null
  it('should keep the incidents and deployment time of the version entry', async () => {
    engine()
    const vm = context({ instanceId: 'pi1' })

    await open(vm)

    expect(vm.process).toMatchObject({ incidents: 7, deploymentTime: 'deployed' })
  })

  it('should reset the lazy statistics placeholders of the versions', async () => {
    engine()
    const vm = context({ instanceId: 'pi1' })
    vm.$root.config.lazyLoadHistory = true

    await open(vm)

    expect(vm.processDefinitions[1]).toMatchObject({ runningInstances: '-', incidents: '-' })
  })

  it('should switch the version without loading the versions again', async () => {
    engine()
    const vm = context()
    await open(vm)
    vi.clearAllMocks()

    await navigate(vm, { versionIndex: '1' })

    expect(ProcessService.findProcessVersionsByDefinitionKey).not.toHaveBeenCalled()
    expect(ProcessService.findProcessById).toHaveBeenCalledWith('invoice:1:id', true)
    expect(vm.process).toBe(vm.processDefinitions[1])
  })

  // A stale link to a deleted version should still show something useful, flagged.
  it('should fall back to the latest version and flag the missing one', async () => {
    engine()
    const vm = context({ versionIndex: '42' })

    await open(vm)

    expect(vm.errorVersionNotFound).toBe('42')
    expect(vm.process).toBe(vm.processDefinitions[0])
  })

  it('should only flag the error when the key has no versions at all', async () => {
    engine({ versions: [] })
    const vm = context()

    await open(vm)

    expect(vm.errorVersionNotFound).toBe('2')
    expect(vm.process).toBeNull()
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
  })

  it('should start over when the key changes', async () => {
    engine()
    const vm = context({ parentProcess: { id: 'parent' } })
    await open(vm)
    vi.clearAllMocks()

    await navigate(vm, { processKey: 'order' })

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('order', undefined, false)
    expect(vm.process.key).toBe('order')
    expect(vm.parentProcess).toBeNull()
  })

  it('should select an instance from the list without reloading the definition', async () => {
    engine()
    const vm = context()
    await open(vm)
    vi.clearAllMocks()

    await navigate(vm, { instanceId: 'pi1' })

    expect(HistoryService.findProcessInstance).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessVersionsByDefinitionKey).not.toHaveBeenCalled()
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
    expect(vm.selectedInstance.id).toBe('pi1')
  })

  it('should go back to the definition without any request', async () => {
    engine()
    const vm = context({ instanceId: 'pi1' })
    await open(vm)
    vi.clearAllMocks()

    await navigate(vm, { instanceId: undefined })

    expect(vm.selectedInstance).toBeNull()
    expect(vm.activityInstance).toBeNull()
    expect(vm.activityInstanceHistory).toBeNull()
    expect(ProcessService.findProcessVersionsByDefinitionKey).not.toHaveBeenCalled()
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
    expect(HistoryService.findProcessInstance).not.toHaveBeenCalled()
  })

  // the instance view of the previous instance must not stay on screen while the next one loads
  it('should drop the previous instance as soon as another one is asked for', async () => {
    engine({ instances: { pi1: activeInstance(), pi2: activeInstance({ id: 'pi2' }) } })
    const vm = context({ instanceId: 'pi1' })
    await open(vm)
    const lookup = deferred()
    HistoryService.findProcessInstance.mockReturnValueOnce(lookup.promise)

    const navigation = navigate(vm, { instanceId: 'pi2' })
    await Promise.resolve()
    expect(vm.selectedInstance).toBeNull()

    lookup.resolve(activeInstance({ id: 'pi2' }))
    await navigation
    expect(vm.selectedInstance.id).toBe('pi2')
  })

  it('should not look the selected instance up again', async () => {
    engine()
    const vm = context({ instanceId: 'pi1' })
    await open(vm)
    vi.clearAllMocks()

    await vm.syncWithRoute()

    expect(HistoryService.findProcessInstance).not.toHaveBeenCalled()
    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
  })

  // A slow response of the previous navigation used to overwrite the newer one.
  it('should drop the results of an older navigation', async () => {
    engine()
    const slowVersions = deferred()
    ProcessService.findProcessVersionsByDefinitionKey.mockReturnValueOnce(slowVersions.promise)
    const vm = context()

    const first = open(vm)
    await vi.waitFor(() => expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalled())
    await navigate(vm, { processKey: 'order' })
    slowVersions.resolve([{ id: 'invoice:2:id', key: 'invoice', version: '2' }])
    await first

    expect(vm.processDefinitions[0].key).toBe('order')
    expect(vm.process.key).toBe('order')
  })

  it('should drop the instance of an older navigation', async () => {
    engine({ instances: { pi1: activeInstance(), pi2: activeInstance({ id: 'pi2' }) } })
    const slowLookup = deferred()
    HistoryService.findProcessInstance.mockReturnValueOnce(slowLookup.promise)
    const vm = context({ instanceId: 'pi1' })

    const first = open(vm)
    await navigate(vm, { instanceId: 'pi2' })
    slowLookup.resolve(activeInstance())
    await first

    expect(vm.selectedInstance.id).toBe('pi2')
  })

  it('should flag loading while the route is loaded', async () => {
    engine()
    const slowVersions = deferred()
    ProcessService.findProcessVersionsByDefinitionKey.mockReturnValueOnce(slowVersions.promise)
    const vm = context({ loading: false })

    const run = open(vm)
    expect(vm.loading).toBe(true)
    slowVersions.resolve([{ id: 'invoice:2:id', key: 'invoice', version: '2' }])
    await run

    expect(vm.loading).toBe(false)
  })

  it('should keep loading while a newer navigation is still loading', async () => {
    engine()
    const slowOrder = deferred()
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async (key) =>
      key === 'order' ? slowOrder.promise : [{ id: 'invoice:2:id', key, version: '2' }])
    const vm = context()

    const first = open(vm)
    const second = navigate(vm, { processKey: 'order' })
    await first

    expect(vm.loading).toBe(true)
    slowOrder.resolve([{ id: 'order:2:id', key: 'order', version: '2' }])
    await second
    expect(vm.loading).toBe(false)
  })

  it('should stop loading when a request fails', async () => {
    engine()
    ProcessService.findProcessVersionsByDefinitionKey.mockRejectedValue(new Error('500'))
    const vm = context()

    await expect(open(vm)).rejects.toThrow('500')

    expect(vm.loading).toBe(false)
  })

  it('should record the failure message when the instance lookup throws', async () => {
    engine()
    HistoryService.findProcessInstance.mockRejectedValue(new Error('404 not found'))
    const vm = context({ instanceId: 'pi1' })

    await open(vm)

    expect(vm.errorLoadingInstanceId).toBe('404 not found')
    expect(vm.selectedInstance).toBeNull()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', undefined, false)
  })

  it('should forget the failure when leaving the instance', async () => {
    engine()
    HistoryService.findProcessInstance.mockRejectedValue(new Error('404 not found'))
    const vm = context({ instanceId: 'pi1' })
    await open(vm)

    await navigate(vm, { instanceId: undefined })

    expect(vm.errorLoadingInstanceId).toBeNull()
  })

  // An instance already listed in the table can be shown even if history has purged it.
  it('should fall back to an instance already in the store', async () => {
    engine({ instances: {} })
    const vm = context({ instanceId: 'pi1', instances: [activeInstance()] })

    await open(vm)

    expect(vm.selectedInstance.id).toBe('pi1')
  })

  it('should look the instance up in the runtime API when history is off', async () => {
    engine()
    ProcessService.findProcessInstance.mockResolvedValue(activeInstance())
    const vm = context({ instanceId: 'pi1' })
    vm.$root.config.camundaHistoryLevel = 'none'

    await open(vm)

    expect(ProcessService.findProcessInstance).toHaveBeenCalledWith('pi1')
    expect(HistoryService.findProcessInstance).not.toHaveBeenCalled()
  })

  // Arriving from a call activity, the parent is shown as a breadcrumb.
  it('should load the parent process named in the query', async () => {
    engine()
    const getProcessById = vi.fn(() => Promise.resolve({ id: 'pd-parent', name: 'Parent' }))
    const vm = context({ getProcessById, $route: { query: { parentProcessDefinitionId: 'pd-parent' } } })

    await open(vm)

    expect(getProcessById).toHaveBeenCalledWith({ id: 'pd-parent' })
    expect(vm.parentProcess).toEqual({ id: 'pd-parent', name: 'Parent' })
  })

  it('should clear the parent process when the query does not name one', async () => {
    engine()
    const vm = context()
    await open(vm)
    vm.parentProcess = { id: 'stale' }

    await navigate(vm, { versionIndex: '1' })

    expect(vm.parentProcess).toBeNull()
  })
})

describe('ProcessDefinitionView - selectDefinition', () => {
  // ProcessInstancesView only reloads them when the definition id changes
  it('should keep the statistics when the same version is selected from a reloaded list', async () => {
    const statistics = [{ id: 'task' }]
    const fresh = version()
    const vm = context({ process: version({ statistics }) })

    await m.selectDefinition.call(vm, fresh)

    expect(vm.process).toBe(fresh)
    expect(fresh.statistics).toBe(statistics)
  })

  it('should not carry statistics over to another version', async () => {
    const fresh = version({ id: 'pd:1:abc', version: '1' })
    const vm = context({ process: version({ statistics: [{ id: 'task' }] }) })

    await m.selectDefinition.call(vm, fresh)

    expect(fresh.statistics).toBeUndefined()
  })

  it('should not load statistics the instance view already has', async () => {
    const vm = context({ instanceId: 'pi1' })

    await m.selectDefinition.call(vm, version({ statistics: [] }))

    expect(vm.$store.dispatch).not.toHaveBeenCalled()
  })
})

describe('ProcessDefinitionView - selectInstance', () => {
  it('should load the runtime activity tree and history for an active instance', async () => {
    const vm = context()
    ProcessService.findActivityInstance.mockResolvedValue({ id: 'ai1' })
    HistoryService.findActivitiesInstancesHistory.mockResolvedValue([{ id: 'h1' }])

    await m.selectInstance.call(vm, { id: 'pi1', state: 'ACTIVE' })

    expect(ProcessService.findActivityInstance).toHaveBeenCalledWith('pi1')
    expect(vm.activityInstance).toEqual({ id: 'ai1' })
    expect(vm.activityInstanceHistory).toEqual([{ id: 'h1' }])
  })

  // Runtime instances report `ended` rather than a `state`.
  it('should treat a not-ended runtime instance as active', async () => {
    const vm = context()

    await m.selectInstance.call(vm, { id: 'pi1', ended: false })

    expect(ProcessService.findActivityInstance).toHaveBeenCalled()
  })

  it('should load only the history for a finished instance', async () => {
    const vm = context()
    HistoryService.findActivitiesInstancesHistory.mockResolvedValue([{ id: 'h1' }])

    await m.selectInstance.call(vm, { id: 'pi1', state: 'COMPLETED' })

    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
    expect(vm.activityInstanceHistory).toEqual([{ id: 'h1' }])
  })

  it('should load nothing for a finished instance when history is off', async () => {
    const vm = context()
    vm.$root.config.camundaHistoryLevel = 'none'

    await m.selectInstance.call(vm, { id: 'pi1', state: 'COMPLETED' })

    expect(HistoryService.findActivitiesInstancesHistory).not.toHaveBeenCalled()
  })

  // Re-selecting the open instance would re-fetch everything for no reason.
  it('should skip reloading the instance that is already selected', async () => {
    const vm = context({ selectedInstance: { id: 'pi1', state: 'ACTIVE' } })

    await m.selectInstance.call(vm, { id: 'pi1', state: 'ACTIVE' })

    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
  })

  it('should drop the previous instance data before selecting another one', async () => {
    const vm = context({ selectedInstance: { id: 'old' }, activityInstance: { id: 'ai-old' }, task: { id: 't1' } })
    vm.$root.config.camundaHistoryLevel = 'none'

    await m.selectInstance.call(vm, { id: 'pi1', state: 'COMPLETED' })

    expect(vm.selectedInstance.id).toBe('pi1')
    expect(vm.activityInstance).toBeNull()
    expect(vm.task).toBeNull()
  })

  it('should drop the activity data of an instance that is no longer selected', async () => {
    const vm = context()
    ProcessService.findActivityInstance.mockImplementation(async () => {
      vm.selectedInstance = { id: 'pi2' }
      return { id: 'ai1' }
    })

    await m.selectInstance.call(vm, { id: 'pi1', state: 'ACTIVE' })

    expect(vm.activityInstance).toBeNull()
    expect(HistoryService.findActivitiesInstancesHistory).not.toHaveBeenCalled()
  })

  it('should drop the history of an instance that is no longer selected', async () => {
    const vm = context()
    HistoryService.findActivitiesInstancesHistory.mockImplementation(async () => {
      vm.selectedInstance = { id: 'pi2' }
      return [{ id: 'h1' }]
    })

    await m.selectInstance.call(vm, { id: 'pi1', state: 'COMPLETED' })

    expect(vm.activityInstanceHistory).toBeNull()
  })
})

describe('ProcessDefinitionView - statistics placeholders', () => {
  // Lazy loading defers the counts, so the columns show a dash instead of a stale number.
  it('should placeholder every count when lazy loading', () => {
    const vm = context({ processDefinitions: [version(), version({ id: 'pd:1:abc' })] })

    m.resetStatsLazyLoad.call(vm, true)

    expect(vm.processDefinitions.every(v =>
      v.runningInstances === '-' && v.allInstances === '-' && v.completedInstances === '-'
    )).toBe(true)
  })

  // the backend sends 0 incidents when it did not count them
  it('should placeholder the incidents when lazy loading', () => {
    const vm = context({ processDefinitions: [version({ incidents: 0 })] })

    m.resetStatsLazyLoad.call(vm, true)

    expect(vm.processDefinitions[0].incidents).toBe('-')
  })

  it('should leave the counts alone when not lazy loading', () => {
    const vm = context({ processDefinitions: [version({ runningInstances: 5 })] })

    m.resetStatsLazyLoad.call(vm, false)

    expect(vm.processDefinitions[0].runningInstances).toBe(5)
  })

  it('loadStatistics should load the statistics for the loaded definition', async () => {
    const vm = context({ process: version() })

    await m.loadStatistics.call(vm)

    expect(vm.$store.dispatch).toHaveBeenCalledWith('loadStatistics', vm.process)
  })
})

describe('ProcessDefinitionView - setSelectedTask', () => {
  const selectedInstance = { id: 'pi1' }

  it('should load the task and its variables from the runtime API while it is open', async () => {
    const vm = context({ selectedInstance })
    HistoryService.findTasksByDefinitionKeyHistory.mockResolvedValue([{ id: 't1', activityInstanceId: 'ai1' }])
    TaskService.fetchActivityVariables.mockResolvedValue([{ name: 'amount', type: 'Integer', value: 42 }])

    await m.setSelectedTask.call(vm, { id: 'task_1' })

    expect(HistoryService.findTasksByDefinitionKeyHistory).toHaveBeenCalledWith('task_1', 'pi1')
    expect(TaskService.fetchActivityVariables).toHaveBeenCalledWith('ai1')
    expect(vm.task.variables).toEqual([{ name: 'amount', type: 'Integer', value: 42 }])
  })

  it('should load the variables from history once the task has ended', async () => {
    const vm = context({ selectedInstance })
    HistoryService.findTasksByDefinitionKeyHistory.mockResolvedValue([
      { id: 't1', activityInstanceId: 'ai1', endTime: '2026-01-01T00:00:00Z' }
    ])

    await m.setSelectedTask.call(vm, { id: 'task_1' })

    expect(HistoryService.fetchActivityVariablesHistory).toHaveBeenCalledWith('ai1')
    expect(TaskService.fetchActivityVariables).not.toHaveBeenCalled()
  })

  // Object variables are shown as JSON text rather than as [object Object].
  it('should serialise Object variables to JSON', async () => {
    const vm = context({ selectedInstance })
    HistoryService.findTasksByDefinitionKeyHistory.mockResolvedValue([{ id: 't1', activityInstanceId: 'ai1' }])
    TaskService.fetchActivityVariables.mockResolvedValue([{ name: 'order', type: 'Object', value: { a: 1 } }])

    await m.setSelectedTask.call(vm, { id: 'task_1' })

    expect(vm.task.variables[0].value).toBe('{"a":1}')
  })

  it('should clear the task when history has no entry for it', async () => {
    const vm = context({ selectedInstance, task: { id: 'stale' } })
    HistoryService.findTasksByDefinitionKeyHistory.mockResolvedValue([])

    await m.setSelectedTask.call(vm, { id: 'task_1' })

    expect(vm.task).toBeNull()
  })

  it('should do nothing without a selected instance', async () => {
    await m.setSelectedTask.call(context(), { id: 'task_1' })

    expect(HistoryService.findTasksByDefinitionKeyHistory).not.toHaveBeenCalled()
  })

  it('should do nothing without a task', async () => {
    await m.setSelectedTask.call(context({ selectedInstance }), null)

    expect(HistoryService.findTasksByDefinitionKeyHistory).not.toHaveBeenCalled()
  })
})

describe('ProcessDefinitionView - onDeleteProcessDefinition', () => {
  const params = { processDefinition: version({ key: 'invoice', version: '2' }) }

  it('should return to the process list when the last version is deleted', async () => {
    const vm = context()
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([])

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(ProcessService.deleteProcessDefinition).toHaveBeenCalledWith('pd:2:abc', true)
    expect(vm.$router.replace).toHaveBeenCalledWith('/seven/auth/processes')
  })

  // Deleting a version other than the open one just refreshes the list.
  it('should stay put when a non-selected version is deleted', async () => {
    const remaining = version({ id: 'pd:3:abc', version: '3' })
    const vm = context({ versionIndex: '3', process: version({ id: 'pd:3:abc', version: '3' }) })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([remaining])

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(vm.$router.replace).not.toHaveBeenCalled()
    expect(vm.processDefinitions).toEqual([remaining])
    expect(vm.process).toBe(remaining)
    expect(ProcessService.findProcessById).toHaveBeenCalledWith('pd:3:abc', true)
  })

  // Deleting the open version navigates to whichever remaining version is nearest.
  it('should navigate to the nearest remaining version', async () => {
    const vm = context({ process: version({ version: '2' }) })
    const versions = [version({ version: '5' }), version({ version: '3' }), version({ version: '1' })]
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue(versions)

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(vm.$router.replace).toHaveBeenCalledWith({
      name: 'process',
      params: { processKey: 'invoice', versionIndex: '3' },
      query: {}
    })
    expect(vm.processDefinitions).toBe(versions)
  })

  it('should keep the tenant in the query when navigating', async () => {
    const vm = context({ process: version({ version: '2' }), tenantId: 't1' })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([version({ version: '1' })])

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', 't1', false)
    expect(vm.$router.replace).toHaveBeenCalledWith(expect.objectContaining({
      query: { tenantId: 't1' }
    }))
  })
})

describe('ProcessDefinitionView - onRefreshProcessDefinitions', () => {
  it('should reload the versions and re-select the open one', async () => {
    const statistics = [{ id: 'task' }]
    const fresh = version()
    const vm = context({ process: version({ statistics }), resetStatsLazyLoad: vi.fn() })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([fresh, version({ id: 'pd:1:abc', version: '1' })])

    const result = await m.onRefreshProcessDefinitions.call(vm, true)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', undefined, true)
    expect(vm.resetStatsLazyLoad).toHaveBeenCalledWith(true)
    expect(vm.process).toBe(fresh)
    expect(fresh.statistics).toBe(statistics)
    expect(result).toHaveLength(2)
  })

  it('should flag the open version when no versions remain', async () => {
    const vm = context({ process: version() })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([])

    await m.onRefreshProcessDefinitions.call(vm, false)

    expect(vm.errorVersionNotFound).toBe('2')
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
  })

  it('should tell which reload is running while it runs', async () => {
    const vm = context({ process: version(), syncDefinition: vi.fn() })
    let seen
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async () => { seen = vm.refreshingVersions; return [version()] })

    await m.onRefreshProcessDefinitions.call(vm, false)
    expect(seen).toBe('all')
    expect(vm.refreshingVersions).toBeNull()

    await m.onRefreshProcessDefinitions.call(vm, true)
    expect(seen).toBe('refresh')
    expect(vm.refreshingVersions).toBeNull()
  })

  it('should end the reload state when the versions cannot be loaded', async () => {
    const vm = context()
    ProcessService.findProcessVersionsByDefinitionKey.mockRejectedValue(new Error('down'))

    await expect(m.onRefreshProcessDefinitions.call(vm, false)).rejects.toThrow('down')

    expect(vm.refreshingVersions).toBeNull()
  })
})

describe('ProcessDefinitionView - misc', () => {
  it('onInstanceDeleted should refresh the statistics and counts and redraw', async () => {
    const entry = version({ incidents: 3 })
    const vm = context({ process: entry })
    ProcessService.findProcessById.mockResolvedValue({ id: 'pd:2:abc', runningInstances: 4, incidents: 0 })

    await m.onInstanceDeleted.call(vm)

    expect(vm.$store.dispatch).toHaveBeenCalledWith('loadStatistics', entry)
    expect(entry).toMatchObject({ runningInstances: 4, incidents: 3 })
    expect(vm.$refs.process.refreshDiagram).toHaveBeenCalled()
  })

  it('filterInstances should adopt the new filter', () => {
    const vm = context()

    m.filterInstances.call(vm, { unfinished: false, finished: true })

    expect(vm.filter).toEqual({ unfinished: false, finished: true })
  })
})

describe('ProcessDefinitionView - exportCSV', () => {
  const instance = (overrides = {}) => ({
    state: 'ACTIVE',
    businessKey: 'ORDER-1',
    startTime: '2026-01-01T00:00:00Z',
    endTime: null,
    id: 'pi1',
    startUserId: 'demo',
    processDefinitionName: 'Invoice',
    processDefinitionVersion: 2,
    ...overrides
  })

  const csvOf = async (vm) => {
    m.exportCSV.call(vm)
    const [blob] = vm.$refs.importPopper.triggerDownload.mock.calls[0]
    return blob.text()
  }

  it('should write a semicolon-separated header row', async () => {
    const vm = context({ instances: [] })

    const csv = await csvOf(vm)

    expect(csv.split('\n')[0]).toBe([
      'process.state', 'process.businessKey', 'process.startTime', 'process.endTime',
      'process.id', 'process.startUserId', 'process.details.definitionName',
      'process.details.definitionVersion'
    ].join(';'))
  })

  it('should write one row per instance in header order', async () => {
    const vm = context({ instances: [instance()] })

    const csv = await csvOf(vm)

    expect(csv.split('\n')[1]).toBe(
      'ACTIVE;ORDER-1;fmt(2026-01-01T00:00:00Z);;pi1;demo;Invoice;2'
    )
  })

  it('should format both timestamps through the shared date formatter', async () => {
    const vm = context({ instances: [instance({ endTime: '2026-01-02T00:00:00Z' })] })

    const csv = await csvOf(vm)

    expect(csv).toContain('fmt(2026-01-01T00:00:00Z);fmt(2026-01-02T00:00:00Z)')
  })

  it('should hand the download a timestamped csv filename', () => {
    const vm = context({ instances: [] })

    m.exportCSV.call(vm)

    const [blob, filename] = vm.$refs.importPopper.triggerDownload.mock.calls[0]
    expect(blob.type).toBe('text/csv')
    expect(filename).toMatch(/^Management_Instances_\d{8}_\d{4}\.csv$/)
  })
})

// CIB7-2717: the next visit loads them again, so a deployment made in between is seen
describe('ProcessDefinitionView - leaving the process pages', () => {
  it('should forget the called process definitions', () => {
    const vm = { clearStaticCalledProcessDefinitions: vi.fn() }

    ProcessDefinitionView.beforeUnmount.call(vm)

    expect(vm.clearStaticCalledProcessDefinitions).toHaveBeenCalled()
  })
})
