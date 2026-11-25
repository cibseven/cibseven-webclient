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
import { ProcessService, HistoryService } from '@/services.js'

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
      findActivitiesInstancesHistory: vi.fn(() => Promise.resolve([]))
    }
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
    errorInstanceDefinitionId: null,
    errorInstanceDefinitionKey: null,
    selectedInstance: null,
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
  /**
   * Scenario: the instance view passes the instance's tenant to ProcessInstanceView, its incidents table and plugins;
   *   while the instance is still loading, only the URL can tell it.
   * Link: #/seven/auth/process/invoice/2/pi1?tenantId=t1 (instance view) and #/seven/auth/process/invoice/2?tenantId=t1 (definition view)
   * Expected: 't1' while the instance loads; null on the definition view, whose tenantId is the definition's.
   * Guards against: handing a definition's tenant to the instance view as if it were the instance's.
   */
  it('instanceTenantId should be the tenant of the URL until the instance is loaded, and none without an instance', () => {
    expect(c.instanceTenantId.call(context({ instanceId: 'pi1', tenantId: 't1' }))).toBe('t1')
    expect(c.instanceTenantId.call(context({ tenantId: 't1' }))).toBeNull()
  })

  /**
   * Scenario: an instance link may name no tenant (bookmark, external system), or one the instance does not have.
   * Link: #/seven/auth/process/invoice/2/pi1 for an instance of tenant t1, #/seven/auth/process/invoice/2/pi1?tenantId=t1 for one without a tenant
   * Expected: once the instance is loaded, its own tenant wins over the URL: 't1', resp. null.
   * Guards against: P14, ProcessInstanceView, IncidentsTable and the plugin slots getting no tenant for a t1 instance.
   */
  it('instanceTenantId should be the tenant of the loaded instance', () => {
    expect(c.instanceTenantId.call(context({ instanceId: 'pi1', selectedInstance: { id: 'pi1', tenantId: 't1' } }))).toBe('t1')
    expect(c.instanceTenantId.call(context({ instanceId: 'pi1', tenantId: 't1', selectedInstance: { id: 'pi1', tenantId: null } }))).toBeNull()
  })

  /**
   * Scenario: with an instance, the URL's tenantId is the instance's tenant; an instance of tenant t1 can be started
   *   from the shared invoice, deployed without a tenant.
   * Link: #/seven/auth/process/invoice/2/pi1?tenantId=t1 for such an instance, and #/seven/auth/process/invoice/2?tenantId=t1 for t1's own invoice
   * Expected: the tenant of the open definition: null for the shared one, 't1' for t1's own.
   * Guards against: refresh, delete and the instances view using the instance's tenant, which has no versions of a
   *   shared definition (empty list, "definition not found").
   */
  it('definitionTenantId should be the tenant of the open definition', () => {
    expect(c.definitionTenantId.call(context({ instanceId: 'pi1', tenantId: 't1', process: version({ tenantId: null }) }))).toBeNull()
    expect(c.definitionTenantId.call(context({ tenantId: 't1', process: version({ tenantId: 't1' }) }))).toBe('t1')
  })

  /**
   * Scenario: before any definition is open, i.e. on the first load of a link, only the URL names a tenant.
   * Link: #/seven/auth/process/invoice/2?tenantId=t1
   * Expected: 't1'.
   * Happy path: the first versions request of a definition link uses the URL's tenant.
   */
  it('definitionTenantId should assume the tenant of the URL until a definition is open', () => {
    expect(c.definitionTenantId.call(context({ tenantId: 't1' }))).toBe('t1')
  })

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

  it('middlePaneError should be null without an error', () => {
    expect(c.middlePaneError.call(context())).toBeNull()
  })

  /**
   * Scenario: the URL asks for a version that does not exist, but the key has other versions.
   * Link: #/seven/auth/process/invoice/99
   * Expected: no middle-pane error; the latest version is open and only the sidebar warns.
   * Guards against: hiding a definition that can be shown behind a "not found" box.
   */
  it('middlePaneError should be null for a missing version when other versions are shown', () => {
    expect(c.middlePaneError.call(context({ errorVersionNotFound: '99', processDefinitions: [version()] }))).toBeNull()
  })

  it('middlePaneError should tell the definition was not found when the key has no versions', () => {
    expect(c.middlePaneError.call(context({ errorVersionNotFound: '99', tenantId: 't1' }))).toEqual({
      key: 'process.definitionNotFound',
      params: { processKey: 'invoice', tenantId: 't1' }
    })
  })

  it('middlePaneError should tell the instance was not found with the server response', () => {
    expect(c.middlePaneError.call(context({ instanceId: 'pi1', errorLoadingInstanceId: '404 not found' }))).toEqual({
      key: 'process.instanceNotFound',
      params: { instanceId: 'pi1', error: '404 not found' }
    })
  })

  /**
   * Scenario: the instance was found nowhere and no request failed, so '' stands for an empty server response.
   * Expected: the instance-not-found message without a "Server response:" part.
   * Guards against: P42, an empty "Server response:" suggesting a server error was received but lost.
   */
  it('middlePaneError should tell the instance was not found without a server response when there is none', () => {
    expect(c.middlePaneError.call(context({ instanceId: 'pi1', errorLoadingInstanceId: '' }))).toEqual({
      key: 'process.instanceNotFoundNoResponse',
      params: { instanceId: 'pi1' }
    })
  })

  /**
   * Scenario: the instance failed to load but was taken from the instances table, and its definition cannot be read.
   * Expected: the missing definition is told, as the instance is known.
   */
  it('middlePaneError should prefer the missing definition of the instance over the instance error', () => {
    const vm = context({ instanceId: 'pi1', errorLoadingInstanceId: 'boom', errorInstanceDefinitionId: 'invoice:5:id' })
    expect(c.middlePaneError.call(vm)).toEqual({
      key: 'process.instanceDefinitionNotFound',
      params: { instanceId: 'pi1', definitionId: 'invoice:5:id' }
    })
  })

  /**
   * Scenario: the link names invoice, but the instance belongs to order, whose definition was read.
   * Expected: the warning names the instance's key and definition and the URL's key, rather than "could not be loaded".
   */
  it('middlePaneError should tell the instance belongs to another process when its definition was read', () => {
    const vm = context({ instanceId: 'pi1', errorInstanceDefinitionId: 'order:1:id', errorInstanceDefinitionKey: 'order' })
    expect(c.middlePaneError.call(vm)).toEqual({
      key: 'process.instanceOfOtherProcess',
      params: { instanceId: 'pi1', definitionId: 'order:1:id', definitionKey: 'order', processKey: 'invoice' }
    })
  })

  // #/seven/auth/process/invoice/99/pi-gone: the sidebar tells the missing version, the middle pane the missing instance
  it('middlePaneError should prefer the instance error over a missing version', () => {
    const vm = context({ instanceId: 'pi-gone', errorLoadingInstanceId: 'gone', errorVersionNotFound: '99' })
    expect(c.middlePaneError.call(vm).key).toBe('process.instanceNotFound')
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

  /**
   * Scenario: the user picked an activity in the diagram (it filters the instances table and is highlighted), then
   *   opens a route where it means nothing: the first open, another key, another instance, or back from an instance.
   * Link: e.g. from #/seven/auth/process/invoice/2 to #/seven/auth/process/order/2, to #/seven/auth/process/invoice/2/pi1, or from there back to #/seven/auth/process/invoice/2
   * Expected: the activity selection is cleared and the route is loaded.
   * Guards against: an activity of the previous diagram filtering or highlighting the next one.
   */
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

  /**
   * Scenario: the user filters the instances by an activity and switches the version in the sidebar; the activity
   *   usually exists in the other version too.
   * Link: from #/seven/auth/process/invoice/2 to #/seven/auth/process/invoice/1
   * Expected: the activity selection stays and the route is loaded.
   * Guards against: losing the user's activity filter on every version switch.
   */
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

  /**
   * Scenario: the user opens a process definition, e.g. from the processes list.
   * Link: #/seven/auth/process/invoice/2?tenantId=t1
   * Expected: one versions request (with t1), one details request for v2, no instance lookup; v2 is open, nothing flagged.
   * Guards against: CIB7-1895, every request going out twice (watchers per prop and beforeUpdate each loading).
   */
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

  /**
   * Scenario: on the definition view, ProcessInstancesView loads the activity statistics together with its instances.
   * Link: #/seven/auth/process/invoice/2
   * Expected: this view dispatches no loadStatistics.
   * Guards against: the statistics request going out twice for every definition opened.
   */
  it('should leave the statistics to the instances view', async () => {
    engine()
    const vm = context()

    await open(vm)

    expect(vm.$store.dispatch).not.toHaveBeenCalledWith('loadStatistics', expect.anything())
  })

  /**
   * Scenario: the user opens an instance link of an active instance: a deep link, or F5 on the instance view.
   * Link: #/seven/auth/process/invoice/2/pi1?tenantId=t1
   * Expected: one request each for the instance, the versions (with t1), v2's details, the statistics, the activity
   *   tree and the activity history; v2 and pi1 are shown with them.
   * Guards against: CIB7-1895, every request of the instance view going out twice.
   */
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

  /**
   * Scenario: an instance link without ?tenantId (bookmark, external link, the EE variable log before its fix) for an
   *   instance of tenant t1, whose invoice is deployed for t1 only.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: the versions without a tenant miss its definition, so the definition is fetched once with its details and
   *   the versions are loaded again with t1; v2 and pi1 are shown, nothing flagged.
   * Guards against: P1, "definition not found" for a valid instance; P28, fetching the definition twice.
   */
  it('should load the versions with the tenant of the instance definition when the URL names none', async () => {
    engine({ tenantId: 't1' })
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async (key, tenantId) =>
      tenantId === 't1' ? [{ id: definitionId(key, '2'), key, version: '2', tenantId }] : [])
    const vm = context({ instanceId: 'pi1' })

    await open(vm)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenNthCalledWith(1, 'invoice', undefined, false)
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenNthCalledWith(2, 'invoice', 't1', false)
    // fetched once with its details to learn its tenant, then reused for the version entry
    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessById).toHaveBeenCalledWith('invoice:2:id', true)
    expect(vm.process.id).toBe('invoice:2:id')
    expect(vm.process.name).toBe('invoice v2')
    expect(vm.errorVersionNotFound).toBeNull()
    expect(vm.selectedInstance.id).toBe('pi1')
  })

  /**
   * Scenario: an instance of tenant t1 started from the shared invoice (deployed without a tenant); InstancesTable puts
   *   the instance's tenant into the link. Then another instance of the same definition is opened.
   * Link: #/seven/auth/process/invoice/2/pi1?tenantId=t1, then #/seven/auth/process/invoice/2/pi2?tenantId=t1
   * Expected: t1 has no invoice, so the versions are loaded again without a tenant and pi1 is shown on the shared v2;
   *   pi2 then finds its definition among them and nothing is reloaded.
   * Guards against: an empty versions list for every instance of a shared definition, and a reload for each instance.
   */
  it('should load the versions of a shared definition for an instance of a tenant', async () => {
    engine({ instances: { pi1: activeInstance(), pi2: activeInstance({ id: 'pi2' }) } })
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async (key, tenantId) =>
      tenantId ? [] : [{ id: definitionId(key, '2'), key, version: '2', tenantId: null }])
    const vm = context({ instanceId: 'pi1', tenantId: 't1' })

    await open(vm)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenNthCalledWith(1, 'invoice', 't1', false)
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenNthCalledWith(2, 'invoice', null, false)
    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(vm.process.id).toBe('invoice:2:id')
    expect(vm.selectedInstance.id).toBe('pi1')

    // another instance of the same definition finds it in the loaded versions
    vi.clearAllMocks()
    await navigate(vm, { instanceId: 'pi2' })

    expect(ProcessService.findProcessVersionsByDefinitionKey).not.toHaveBeenCalled()
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
    expect(vm.selectedInstance.id).toBe('pi2')
  })

  /**
   * Scenario: t1 has its own invoice v1, the shared invoice is v2. The user opens a t1 instance of the shared v2, then
   *   t1's own definition.
   * Link: #/seven/auth/process/invoice/2/pi1?tenantId=t1, then #/seven/auth/process/invoice/1?tenantId=t1
   * Expected: the definition view loads t1's versions again and opens t1's v1.
   * Guards against: P10, the shared versions kept for the instance being shown under the t1 URL.
   */
  it('should load the versions of the URL tenant when leaving an instance of a shared definition', async () => {
    engine()
    // t1 has its own invoice v1, the shared invoice is v2
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async (key, tenantId) => tenantId === 't1' ?
      [{ id: definitionId(key, '1'), key, version: '1', tenantId }] :
      [{ id: definitionId(key, '2'), key, version: '2', tenantId: null }])
    const vm = context({ instanceId: 'pi1', tenantId: 't1' })
    await open(vm)
    expect(vm.process.id).toBe('invoice:2:id')
    vi.clearAllMocks()

    await navigate(vm, { instanceId: undefined, versionIndex: '1' })

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', 't1', false)
    expect(vm.process.id).toBe('invoice:1:id')
    expect(vm.errorVersionNotFound).toBeNull()
  })

  /**
   * Scenario: t1 has its own invoice v2, the shared invoice is v3. The user opens an instance link without a tenant for a
   *   t1 instance, then a definition link without a tenant.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/process/invoice/3
   * Expected: the versions without a tenant are loaded again and the shared v3 opens.
   * Guards against: P10, t1's versions loaded for the instance being shown for a link without a tenant.
   */
  it('should load the versions without a tenant when leaving an instance link without one', async () => {
    engine({ tenantId: 't1' })
    // t1 has its own invoice v2, the shared invoice is v3
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async (key, tenantId) => tenantId === 't1' ?
      [{ id: definitionId(key, '2'), key, version: '2', tenantId }] :
      [{ id: definitionId(key, '3'), key, version: '3', tenantId: null }])
    const vm = context({ instanceId: 'pi1' })
    await open(vm)
    expect(vm.process.id).toBe('invoice:2:id')
    vi.clearAllMocks()

    await navigate(vm, { instanceId: undefined, versionIndex: '3' })

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', undefined, false)
    expect(vm.process.id).toBe('invoice:3:id')
    expect(vm.errorVersionNotFound).toBeNull()
  })

  /**
   * Scenario: the instance runs on invoice v5, which is not among the loaded versions and cannot be read: no permission
   *   on another tenant's definition, or deleted while its history remains. Then the user opens the definition view.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/process/invoice/2
   * Expected: no definition is open and errorInstanceDefinitionId names invoice:5:id, so the middle pane says the
   *   definition was not found; the instance and its activities are not loaded. The definition view then opens v2 as usual.
   * Guards against: P11, a blank page after the failed request; P18, pi1 drawn on v2's diagram; P25, activity
   *   requests for an instance nothing shows.
   */
  it('should flag the definition of the instance when it cannot be read', async () => {
    engine({ instances: { pi1: activeInstance({ processDefinitionId: definitionId('invoice', '5') }) } })
    const findProcessById = ProcessService.findProcessById.getMockImplementation()
    ProcessService.findProcessById.mockImplementation(async (id, extraInfo) => {
      if (id === 'invoice:5:id') throw new Error('403 forbidden')
      return findProcessById(id, extraInfo)
    })
    const vm = context({ instanceId: 'pi1' })

    await open(vm)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledOnce()
    expect(vm.process).toBeNull()
    expect(vm.errorInstanceDefinitionId).toBe('invoice:5:id')
    // it was not read, so its key is unknown and the warning says it could not be loaded
    expect(vm.errorInstanceDefinitionKey).toBeNull()
    expect(vm.middlePaneError.key).toBe('process.instanceDefinitionNotFound')
    expect(vm.errorVersionNotFound).toBeNull()
    // nothing shows the instance, so neither it nor its activities are loaded
    expect(vm.selectedInstance).toBeNull()
    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
    expect(HistoryService.findActivitiesInstancesHistory).not.toHaveBeenCalled()
    expect(vm.loading).toBe(false)

    // the definition view of the key opens as usual
    await navigate(vm, { instanceId: undefined })

    expect(vm.errorInstanceDefinitionId).toBeNull()
    expect(vm.process.id).toBe('invoice:2:id')
  })

  /**
   * Scenario: the user views instance pi1 of invoice and edits only the key of its link to order.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/process/order/2/pi1
   * Expected: pi1's definition (invoice v2) is not among order's versions and is flagged missing; pi1 and its activity
   *   data are cleared along with the definition, so neither pane shows pi1.
   * Guards against: P36, pi1's details and stale activities staying in the sidebar while the middle pane says the
   *   definition was not found.
   */
  it('should clear an already selected instance when its definition is flagged missing', async () => {
    engine()
    const vm = context({ instanceId: 'pi1' })
    await open(vm)
    expect(vm.selectedInstance.id).toBe('pi1')

    await navigate(vm, { processKey: 'order' })

    expect(vm.errorInstanceDefinitionId).toBe('invoice:2:id')
    expect(vm.process).toBeNull()
    expect(vm.selectedInstance).toBeNull()
    expect(vm.activityInstance).toBeNull()
    expect(vm.activityInstanceHistory).toBeNull()
  })

  /**
   * Scenario: a hand-edited or wrong link names order, but the instance belongs to invoice; its definition can be read.
   *   Then the user corrects the key in the link.
   * Link: #/seven/auth/process/order/1/pi1, then #/seven/auth/process/invoice/2/pi1
   * Expected: the definition is read once and flagged with its key, so the middle pane says pi1 belongs to invoice
   *   (definition invoice:2:id), not to order. With the key corrected, the flag is gone and pi1 is shown on v2.
   * Guards against: P41, the warning saying the definition "could not be loaded" although it was read.
   */
  it('should tell the instance belongs to another process when the URL names another key', async () => {
    engine()
    const vm = context({ processKey: 'order', versionIndex: '1', instanceId: 'pi1' })

    await open(vm)

    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(vm.process).toBeNull()
    expect(vm.selectedInstance).toBeNull()
    expect(vm.middlePaneError).toEqual({
      key: 'process.instanceOfOtherProcess',
      params: { instanceId: 'pi1', definitionId: 'invoice:2:id', definitionKey: 'invoice', processKey: 'order' }
    })

    await navigate(vm, { processKey: 'invoice', versionIndex: '2' })

    expect(vm.errorInstanceDefinitionId).toBeNull()
    expect(vm.errorInstanceDefinitionKey).toBeNull()
    expect(vm.middlePaneError).toBeNull()
    expect(vm.process.id).toBe('invoice:2:id')
    expect(vm.selectedInstance.id).toBe('pi1')
  })

  /**
   * Scenario: a stale bookmark or a hand-edited link names version 2, but the instance runs on version 1.
   * Link: #/seven/auth/process/invoice/2/pi1 for an instance of v1
   * Expected: v1 opens and the view shows version 1, nothing is flagged, no extra request; the URL keeps /2/.
   * Guards against: P13, the instance's activities drawn on v2's diagram, pointing to wrong or missing elements.
   */
  it('should show the instance on its own definition when the URL names another version', async () => {
    engine({ instances: { pi1: activeInstance({ processDefinitionId: definitionId('invoice', '1') }) } })
    const vm = context({ versionIndex: '2', instanceId: 'pi1' })

    await open(vm)

    expect(vm.process.id).toBe('invoice:1:id')
    expect(vm.computedVersionIndex).toBe('1')
    expect(vm.errorVersionNotFound).toBeNull()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessById).toHaveBeenCalledWith('invoice:1:id', true)
    expect(vm.selectedInstance.id).toBe('pi1')
  })

  /**
   * Scenario: the versions of the key cannot be loaded at all (403/500 from the versions endpoint, which also reads
   *   every version's deployment time), but the instance and its definition can.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: the instance's own definition, fetched once, becomes the only version entry; v2 and pi1 are shown,
   *   nothing is flagged and loading ends.
   * Guards against: P22, both panes blank where main still showed the instance.
   */
  it('should show the instance on its own definition when no versions of the key can be loaded', async () => {
    engine()
    ProcessService.findProcessVersionsByDefinitionKey.mockRejectedValue(new Error('403 forbidden'))
    const vm = context({ instanceId: 'pi1' })

    await open(vm)

    expect(vm.processDefinitions).toHaveLength(1)
    expect(vm.process.id).toBe('invoice:2:id')
    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(vm.errorInstanceDefinitionId).toBeNull()
    expect(vm.selectedInstance.id).toBe('pi1')
    expect(vm.loading).toBe(false)
  })

  /**
   * Scenario: the first versions request, without a tenant, fails (500); the instance belongs to tenant t1.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: the reload with the definition's tenant t1 brings the full list; v2 and pi1 are shown.
   * Guards against: P22, one failed request costing the whole instance view.
   */
  /**
   * Scenario: an instance link without a tenant for an instance of tenant t1; the versions without a tenant are empty
   *   and the reload with t1 fails, so the instance is shown on its own definition as the only entry. Then the user
   *   opens a definition link without a tenant.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/process/invoice/1
   * Expected: the definition view loads the versions without a tenant again; there are none, so it says the
   *   definition was not found.
   * Guards against: P31, t1's definition kept as the versions of a page without a tenant, as if they were its own.
   */
  it('should load the versions again after an instance was shown on its own definition alone', async () => {
    engine({ tenantId: 't1' })
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async (key, tenantId) => {
      if (tenantId === 't1') throw new Error('500')
      return []
    })
    const vm = context({ instanceId: 'pi1' })
    await open(vm)
    expect(vm.processDefinitions).toHaveLength(1)
    expect(vm.process.id).toBe('invoice:2:id')
    vi.clearAllMocks()

    await navigate(vm, { instanceId: undefined, versionIndex: '1' })

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', undefined, false)
    expect(vm.process).toBeNull()
    expect(vm.errorVersionNotFound).toBe('1')
  })

  it('should load the versions with the tenant of the definition when the first versions request fails', async () => {
    engine({ tenantId: 't1' })
    const versions = ProcessService.findProcessVersionsByDefinitionKey.getMockImplementation()
    ProcessService.findProcessVersionsByDefinitionKey.mockRejectedValueOnce(new Error('500'))
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(versions)
    const vm = context({ instanceId: 'pi1' })

    await open(vm)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenNthCalledWith(2, 'invoice', 't1', false)
    expect(vm.processDefinitions).toHaveLength(2)
    expect(vm.process.id).toBe('invoice:2:id')
    expect(vm.selectedInstance.id).toBe('pi1')
  })


  /**
   * Scenario: t1 has its own invoice v2, the shared invoice is v1. The user opens a t1 instance of the shared v1, then a
   *   link to an instance that does not exist (deleted, cleaned up, mistyped).
   * Link: #/seven/auth/process/invoice/1/a?tenantId=t1, then #/seven/auth/process/invoice/2/missing?tenantId=t1
   * Expected: t1's versions are loaded again and t1's v2 opens; no instance is selected.
   * Guards against: P27, the shared versions kept for the previous instance shown under the t1 URL: the wrong
   *   definition, or "version 2 not found" although t1 has a v2.
   */
  it('should load the versions of the URL tenant again when an instance of the key is not found', async () => {
    engine({ instances: { a: activeInstance({ id: 'a', processDefinitionId: definitionId('invoice', '1') }) } })
    // t1 has its own invoice v2, the shared invoice is v1
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async (key, tenantId) => tenantId === 't1' ?
      [{ id: 'invoice:2:t1', key, version: '2', tenantId }] :
      [{ id: definitionId(key, '1'), key, version: '1', tenantId: null }])
    const vm = context({ instanceId: 'a', versionIndex: '1', tenantId: 't1' })
    await open(vm)
    expect(vm.process.id).toBe('invoice:1:id')
    vi.clearAllMocks()

    await navigate(vm, { instanceId: 'missing', versionIndex: '2' })

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledOnce()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', 't1', false)
    expect(vm.process.id).toBe('invoice:2:t1')
    expect(vm.errorVersionNotFound).toBeNull()
    expect(vm.selectedInstance).toBeNull()
  })

  /**
   * Scenario: the versions shown already are the URL tenant's when the next link points to a missing instance.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/process/invoice/2/missing
   * Expected: no versions request.
   * Guards against: reloading the right versions for nothing on every missing instance.
   */
  it('should not load the versions again when an instance of the key is not found', async () => {
    engine({ instances: { pi1: activeInstance() } })
    const vm = context({ instanceId: 'pi1' })
    await open(vm)
    vi.clearAllMocks()

    await navigate(vm, { instanceId: 'missing' })

    expect(ProcessService.findProcessVersionsByDefinitionKey).not.toHaveBeenCalled()
  })

  /**
   * Scenario: a deep link to an instance needs the instance and the versions of its key; neither waits for the other.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: the versions request goes out while the instance lookup is still pending; pi1 is shown once both arrive.
   * Guards against: the two requests running one after the other, doubling the wait.
   */
  it('should look the instance up while the versions load', async () => {
    engine()
    const lookup = deferred()
    HistoryService.findProcessInstance.mockReturnValueOnce(lookup.promise)
    const vm = context({ instanceId: 'pi1' })

    const run = open(vm)
    await vi.waitFor(() => expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalled())
    expect(HistoryService.findProcessInstance).toHaveBeenCalledWith('pi1')

    lookup.resolve(activeInstance())
    await run
    expect(vm.selectedInstance.id).toBe('pi1')
  })

  /**
   * Scenario: the versions list carries each version's incidents and deployment time; the backend's definition
   *   details do not fill them in (incidents always 0, deployment time null).
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: the open version keeps the incidents (7) and the deployment time of its versions entry.
   * Guards against: the sidebar showing 0 incidents and no deployment time once the details are merged in.
   */
  it('should keep the incidents and deployment time of the version entry', async () => {
    engine()
    const vm = context({ instanceId: 'pi1' })

    await open(vm)

    expect(vm.process).toMatchObject({ incidents: 7, deploymentTime: 'deployed' })
  })

  /**
   * Scenario: with lazyLoadHistory on, the versions come without their counts, which the sidebar loads on demand.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: the counts of the versions show '-' placeholders.
   * Guards against: showing the backend's empty counts as real numbers.
   */
  it('should reset the lazy statistics placeholders of the versions', async () => {
    engine()
    const vm = context({ instanceId: 'pi1' })
    vm.$root.config.lazyLoadHistory = true

    await open(vm)

    expect(vm.processDefinitions[1]).toMatchObject({ runningInstances: '-', incidents: '-' })
  })

  /**
   * Scenario: the user clicks another version in the sidebar.
   * Link: from #/seven/auth/process/invoice/2 to #/seven/auth/process/invoice/1
   * Expected: only v1's details are requested; the versions are kept and v1 opens.
   * Guards against: reloading the whole versions list on every version switch.
   */
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

  /**
   * Scenario: a stale link to a version that was deleted, or a mistyped version.
   * Link: #/seven/auth/process/invoice/42
   * Expected: the latest version opens, and version 42 is flagged as not found in the sidebar.
   * Guards against: an empty page for a key that still has versions.
   */
  it('should fall back to the latest version and flag the missing one', async () => {
    engine()
    const vm = context({ versionIndex: '42' })

    await open(vm)

    expect(vm.errorVersionNotFound).toBe('42')
    expect(vm.process).toBe(vm.processDefinitions[0])
  })

  /**
   * Scenario: a link to a key without any versions: all deleted, or never deployed for this tenant.
   * Link: #/seven/auth/process/invoice/2
   * Expected: version 2 is flagged and no definition is open, so both panes say the definition was not found; no
   *   details are requested.
   * Guards against: a details request for a definition that does not exist.
   */
  it('should only flag the error when the key has no versions at all', async () => {
    engine({ versions: [] })
    const vm = context()

    await open(vm)

    expect(vm.errorVersionNotFound).toBe('2')
    expect(vm.process).toBeNull()
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user goes from one process to another, e.g. through a call activity or the processes list.
   * Link: from #/seven/auth/process/invoice/2 to #/seven/auth/process/order/2
   * Expected: order's versions are loaded and order v2 opens; invoice's parent breadcrumb is gone.
   * Guards against: invoice's versions or breadcrumb staying on the order page.
   */
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

  /**
   * Scenario: the user clicks an instance in the instances table of the open definition.
   * Link: from #/seven/auth/process/invoice/2 to #/seven/auth/process/invoice/2/pi1
   * Expected: only the instance is looked up; the versions and the definition are kept; pi1 is selected.
   * Guards against: reloading the definition for every instance opened from its table.
   */
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

  /**
   * Scenario: the user leaves the instance view through the breadcrumb back to its definition.
   * Link: from #/seven/auth/process/invoice/2/pi1 to #/seven/auth/process/invoice/2
   * Expected: the instance and its activity data are cleared, without any request.
   * Guards against: reloading the versions, the definition or the instance when going back.
   */
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

  /**
   * Scenario: the user opens another instance while the previous one is shown, and the new lookup is slow.
   * Link: from #/seven/auth/process/invoice/2/pi1 to #/seven/auth/process/invoice/2/pi2
   * Expected: pi1 disappears at once; pi2 is shown when its lookup arrives.
   * Guards against: pi1's data staying on screen under pi2's URL while it loads.
   */
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

  /**
   * Scenario: a run starts for the route already shown, without a route change.
   * Link: #/seven/auth/process/invoice/2/pi1, unchanged
   * Expected: neither the instance nor its activity tree is requested again.
   * Guards against: reloading the open instance for nothing.
   */
  it('should not look the selected instance up again', async () => {
    engine()
    const vm = context({ instanceId: 'pi1' })
    await open(vm)
    vi.clearAllMocks()

    await vm.syncWithRoute()

    expect(HistoryService.findProcessInstance).not.toHaveBeenCalled()
    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user opens invoice and, before its slow versions arrive, moves on to order.
   * Link: #/seven/auth/process/invoice/2, then #/seven/auth/process/order/2
   * Expected: order's versions and definition stay when invoice's reply arrives late.
   * Guards against: a slow reply of the previous navigation overwriting the page the user is on.
   */
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

  /**
   * Scenario: the user opens pi1 and, before its slow lookup arrives, opens pi2.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/process/invoice/2/pi2
   * Expected: pi2 stays selected when pi1's lookup arrives late.
   * Guards against: pi1 shown under pi2's URL.
   */
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

  /**
   * Scenario: the route is loading.
   * Link: #/seven/auth/process/invoice/2
   * Expected: loading is true while the versions are pending and false once the route is loaded.
   * Happy path: drives the loading box of the middle pane (P8).
   */
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

  /**
   * Scenario: the user moves from invoice to order; invoice's run finishes first, order's is still pending.
   * Link: #/seven/auth/process/invoice/2, then #/seven/auth/process/order/2
   * Expected: loading stays true until order's run is done.
   * Guards against: the loading box disappearing while the page the user is on still loads.
   */
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

  /**
   * Scenario: the versions request of a definition link fails (500, or a timeout).
   * Link: #/seven/auth/process/invoice/2
   * Expected: loading ends, no definition is open and version 2 is flagged, so the sidebar and the middle pane say
   *   the definition was not found; the global error dialog tells why the request failed.
   * Guards against: P32, both panes left blank with no warning; a loading box that never goes away after an error.
   */
  it('should tell the definition was not found when its versions cannot be loaded', async () => {
    engine()
    ProcessService.findProcessVersionsByDefinitionKey.mockRejectedValue(new Error('500'))
    const vm = context()

    await open(vm)

    expect(vm.loading).toBe(false)
    expect(vm.process).toBeNull()
    expect(vm.processDefinitions).toEqual([])
    expect(vm.errorVersionNotFound).toBe('2')
  })

  /**
   * Scenario: the user opens invoice, whose versions request fails slowly (500), and moves on to order at once.
   * Link: #/seven/auth/process/invoice/2, then #/seven/auth/process/order/2
   * Expected: invoice's run resolves instead of rejecting; order stays open and loading ends.
   * Guards against: P21, invoice's failure escaping the route watcher as an unhandled error while the user is on
   *   order. The global axios handler still shows its error dialog when the request fails.
   */
  it('should drop the failure of an older navigation', async () => {
    engine()
    const slowVersions = deferred()
    ProcessService.findProcessVersionsByDefinitionKey.mockReturnValueOnce(slowVersions.promise)
    const vm = context()

    const first = open(vm)
    await vi.waitFor(() => expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalled())
    await navigate(vm, { processKey: 'order' })
    slowVersions.resolve(Promise.reject(new Error('500')))

    await expect(first).resolves.toBeUndefined()
    expect(vm.process.key).toBe('order')
    expect(vm.loading).toBe(false)
  })

  /**
   * Scenario: an instance link to an instance that cannot be loaded (404); the middle pane shows "instance not found",
   *   not the instance view, so no diagram of the definition is shown.
   * Link: #/seven/auth/process/invoice/2/unknown
   * Expected: the definition's statistics are not requested.
   * Guards against: P34, a statistics request whose result is never shown.
   */
  it('should load no statistics for an instance that cannot be loaded', async () => {
    engine()
    HistoryService.findProcessInstance.mockRejectedValue(new Error('404 not found'))
    const vm = context({ instanceId: 'unknown' })

    await open(vm)

    expect(vm.errorLoadingInstanceId).toBe('404 not found')
    expect(vm.process.id).toBe('invoice:2:id')
    expect(vm.$store.dispatch).not.toHaveBeenCalledWith('loadStatistics', expect.anything())
  })

  /**
   * Scenario: the user opens a link to an instance that cannot be loaded (e.g. a mistyped id), then corrects it to an
   *   instance of the same version.
   * Link: #/seven/auth/process/invoice/2/typo, then #/seven/auth/process/invoice/2/pi1
   * Expected: v2 stays open without loading its details again, and its statistics are loaded for pi1's diagram.
   * Guards against: P39, pi1 drawn without the statistics overlay, as the first link loaded none (P34) and the open
   *   version was kept as it was.
   */
  it('should load the statistics of the open version for an instance found after one that was not', async () => {
    engine()
    const vm = context({ instanceId: 'typo' })
    await open(vm)
    expect(vm.process.id).toBe('invoice:2:id')
    expect(vm.$store.dispatch).not.toHaveBeenCalledWith('loadStatistics', expect.anything())
    vi.clearAllMocks()

    await navigate(vm, { instanceId: 'pi1' })

    expect(vm.process.id).toBe('invoice:2:id')
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
    expect(vm.$store.dispatch).toHaveBeenCalledWith('loadStatistics', vm.process)
    expect(vm.selectedInstance.id).toBe('pi1')
  })

  /**
   * Scenario: an instance link whose lookup answers without an error but finds nothing, and the instance is not in
   *   the instances table either.
   * Link: #/seven/auth/process/invoice/2/missing
   * Expected: errorLoadingInstanceId is set to an empty server response, so the middle pane says the instance was
   *   not found; no instance is selected.
   * Guards against: a blank middle pane, as neither the instance view nor a warning had anything to show.
   */
  it('should tell the instance was not found when its lookup finds nothing', async () => {
    engine()
    const vm = context({ instanceId: 'missing' })

    await open(vm)

    expect(vm.errorLoadingInstanceId).toBe('')
    expect(vm.selectedInstance).toBeNull()
  })

  /**
   * Scenario: an instance link to an instance that cannot be loaded (404: deleted, or its history cleaned up).
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: errorLoadingInstanceId holds the server's message, no instance is selected, the versions still load.
   * Happy path: the middle pane shows "instance not found" with the reason.
   */
  it('should record the failure message when the instance lookup throws', async () => {
    engine()
    HistoryService.findProcessInstance.mockRejectedValue(new Error('404 not found'))
    const vm = context({ instanceId: 'pi1' })

    await open(vm)

    expect(vm.errorLoadingInstanceId).toBe('404 not found')
    expect(vm.selectedInstance).toBeNull()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', undefined, false)
  })

  /**
   * Scenario: the user leaves an instance that could not be loaded for its definition.
   * Link: from #/seven/auth/process/invoice/2/pi1 (404) to #/seven/auth/process/invoice/2
   * Expected: the failure message is cleared.
   * Guards against: the definition view showing the previous instance's "not found" warning.
   */
  it('should forget the failure when leaving the instance', async () => {
    engine()
    HistoryService.findProcessInstance.mockRejectedValue(new Error('404 not found'))
    const vm = context({ instanceId: 'pi1' })
    await open(vm)

    await navigate(vm, { instanceId: undefined })

    expect(vm.errorLoadingInstanceId).toBeNull()
  })

  /**
   * Scenario: the user opens an instance whose lookup fails slowly (404), and meanwhile opens another process.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/process/order/2
   * Expected: no failure message on the order page; order is open.
   * Guards against: P3, the middle pane showing "instance not found" (for instanceId undefined) on a page without an
   *   instance.
   */
  it('should drop the instance lookup failure of an older navigation', async () => {
    engine()
    const slowLookup = deferred()
    HistoryService.findProcessInstance.mockReturnValueOnce(slowLookup.promise)
    const vm = context({ instanceId: 'pi1' })

    const first = open(vm)
    await navigate(vm, { processKey: 'order', instanceId: undefined })
    slowLookup.resolve(Promise.reject(new Error('404 not found')))
    await first

    expect(vm.errorLoadingInstanceId).toBeNull()
    expect(vm.process.key).toBe('order')
  })

  /**
   * Scenario: an instance listed in the instances table, whose lookup finds nothing because history has cleaned it
   *   up meanwhile.
   * Link: #/seven/auth/process/invoice/2/pi1, opened from the instances table
   * Expected: the instance from the table is selected.
   * Happy path: the user still sees the instance they clicked.
   */
  it('should fall back to an instance already in the store', async () => {
    engine({ instances: {} })
    const vm = context({ instanceId: 'pi1', instances: [activeInstance()] })

    await open(vm)

    expect(vm.selectedInstance.id).toBe('pi1')
  })

  /**
   * Scenario: the engine runs with history level none, so instances exist only in the runtime API.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: the instance is looked up in the runtime API, not in history.
   * Guards against: a history request that can only fail.
   */
  it('should look the instance up in the runtime API when history is off', async () => {
    engine()
    ProcessService.findProcessInstance.mockResolvedValue(activeInstance())
    const vm = context({ instanceId: 'pi1' })
    vm.$root.config.camundaHistoryLevel = 'none'

    await open(vm)

    expect(ProcessService.findProcessInstance).toHaveBeenCalledWith('pi1')
    expect(HistoryService.findProcessInstance).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user arrives from a call activity; the link names the calling definition.
   * Link: #/seven/auth/process/invoice/2?parentProcessDefinitionId=pd-parent
   * Expected: the parent is looked up and shown as the breadcrumb.
   * Happy path: the user can go back to the calling process.
   */
  it('should load the parent process named in the query', async () => {
    engine()
    const getProcessById = vi.fn(() => Promise.resolve({ id: 'pd-parent', name: 'Parent' }))
    const vm = context({ getProcessById, $route: { query: { parentProcessDefinitionId: 'pd-parent' } } })

    await open(vm)

    expect(getProcessById).toHaveBeenCalledWith({ id: 'pd-parent' })
    expect(vm.parentProcess).toEqual({ id: 'pd-parent', name: 'Parent' })
  })

  /**
   * Scenario: the user had a parent breadcrumb, then opens a link without a parent.
   * Link: #/seven/auth/process/invoice/2, then #/seven/auth/process/invoice/1
   * Expected: no breadcrumb.
   * Guards against: a breadcrumb that does not belong to the page.
   */
  it('should clear the parent process when the query does not name one', async () => {
    engine()
    const vm = context()
    await open(vm)
    vm.parentProcess = { id: 'stale' }

    await navigate(vm, { versionIndex: '1' })

    expect(vm.parentProcess).toBeNull()
  })

  /**
   * Scenario: a call activity calls its own process: BpmnViewer opens the same key and version with another
   *   parentProcessDefinitionId; browser back/forward between two such entries does the same.
   * Link: #/seven/auth/process/invoice/2?parentProcessDefinitionId=pd-first, then #/seven/auth/process/invoice/2?parentProcessDefinitionId=pd-second
   * Expected: a run starts and the breadcrumb shows pd-second; the versions and the definition are not requested, the
   *   activity selection stays.
   * Guards against: P24, the breadcrumb keeping the previous parent.
   */
  it('should look the parent up again when only the parent in the query changes', async () => {
    engine()
    const getProcessById = vi.fn(async ({ id }) => ({ id }))
    const vm = context({ getProcessById, $route: { query: { parentProcessDefinitionId: 'pd-first' } } })
    await open(vm)
    expect(vm.parentProcess).toEqual({ id: 'pd-first' })
    vi.clearAllMocks()

    const previous = vm.routeState
    vm.$route = { query: { parentProcessDefinitionId: 'pd-second' } }
    expect(vm.routeState).not.toEqual(previous)
    await vm.onRouteChanged(vm.routeState, previous)

    expect(vm.parentProcess).toEqual({ id: 'pd-second' })
    expect(ProcessService.findProcessVersionsByDefinitionKey).not.toHaveBeenCalled()
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
    expect(vm.clearActivitySelection).not.toHaveBeenCalled()
  })

  /**
   * Scenario: on a definition opened from a call activity, the user switches tabs: each switch pushes ?tab=, and as
   *   routeState reads the query, it is re-evaluated with the same values.
   * Link: from #/seven/auth/process/invoice/2?parentProcessDefinitionId=pd-parent to the same link with &tab=variables
   * Expected: nothing is loaded: no run, no parent lookup, no versions or definition request, loading stays false.
   * Guards against: P30, every tab switch reloading the route, and restarting a load still in progress.
   */
  it('should load nothing when a navigation changes only other parts of the query', async () => {
    engine()
    const vm = context({ $route: { query: { parentProcessDefinitionId: 'pd-parent' } } })
    await open(vm)
    vi.clearAllMocks()

    const previous = vm.routeState
    vm.$route = { query: { parentProcessDefinitionId: 'pd-parent', tab: 'variables' } }
    await vm.onRouteChanged(vm.routeState, previous)

    expect(vm.getProcessById).not.toHaveBeenCalled()
    expect(ProcessService.findProcessVersionsByDefinitionKey).not.toHaveBeenCalled()
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
    expect(vm.clearActivitySelection).not.toHaveBeenCalled()
    expect(vm.loading).toBe(false)
  })

  /**
   * Scenario: the user arrives at invoice from a call activity, its parent breadcrumb still loading, then opens order.
   * Link: #/seven/auth/process/invoice/2?parentProcessDefinitionId=pd-parent, then #/seven/auth/process/order/2
   * Expected: no breadcrumb on order when the parent arrives late.
   * Guards against: P2, the slow parent of the previous navigation shown as order's breadcrumb.
   */
  it('should drop the parent of an older navigation', async () => {
    engine()
    const parent = deferred()
    const vm = context({ getProcessById: vi.fn(() => parent.promise), $route: { query: { parentProcessDefinitionId: 'pd-parent' } } })

    const first = open(vm)
    await vi.waitFor(() => expect(vm.getProcessById).toHaveBeenCalled())
    vm.$route = { query: {} }
    await navigate(vm, { processKey: 'order' })
    parent.resolve({ id: 'pd-parent' })
    await first

    expect(vm.parentProcess).toBeNull()
    expect(vm.process.key).toBe('order')
  })

  /**
   * Scenario: an instance opened from the instances table keeps parentProcessDefinitionId in its query, as
   *   InstancesTable spreads the query; ProcessInstanceView resolves its own parent.
   * Link: #/seven/auth/process/invoice/2/pi1?parentProcessDefinitionId=pd-parent
   * Expected: no parent lookup and no breadcrumb from this view.
   * Guards against: P17, a request nothing uses, whose failure kept the instance from being shown.
   */
  it('should not look the parent up for an instance', async () => {
    engine()
    const vm = context({ instanceId: 'pi1', $route: { query: { parentProcessDefinitionId: 'pd-parent' } } })

    await open(vm)

    expect(vm.getProcessById).not.toHaveBeenCalled()
    expect(vm.parentProcess).toBeNull()
  })

  /**
   * Scenario: the parent named in the query was deleted, or the user may not read it (404).
   * Link: #/seven/auth/process/invoice/2?parentProcessDefinitionId=pd-parent
   * Expected: v2 opens without a breadcrumb and loading ends.
   * Guards against: P17, a blank page because of the breadcrumb.
   */
  it('should open the definition without the parent when the parent cannot be read', async () => {
    engine()
    const vm = context({
      getProcessById: vi.fn(() => Promise.reject(new Error('404 not found'))),
      $route: { query: { parentProcessDefinitionId: 'pd-parent' } }
    })

    await open(vm)

    expect(vm.process.id).toBe('invoice:2:id')
    expect(vm.parentProcess).toBeNull()
    expect(vm.loading).toBe(false)
  })

  /**
   * Scenario: the details request of the instance's definition fails (500); its versions entry is there.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: v2 is open as listed and pi1 is shown.
   * Guards against: P17, a blank middle pane because of a failed details request.
   */
  it('should show the instance when the details of its definition cannot be loaded', async () => {
    engine()
    ProcessService.findProcessById.mockRejectedValue(new Error('500'))
    const vm = context({ instanceId: 'pi1' })

    await open(vm)

    expect(vm.process.id).toBe('invoice:2:id')
    expect(vm.selectedInstance.id).toBe('pi1')
  })

  /**
   * Scenario: the instance runs on invoice v5 of tenant t1; the link names no tenant, so the loaded versions (v2, v1)
   *   miss it, and the reload with t1 fails (500).
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: the instance's own definition, read once, replaces the loaded versions as the only entry; v5 and pi1 are
   *   shown, nothing is flagged, and the next route loads the versions again.
   * Guards against: P17, a blank page after the failed reload; P18, pi1 drawn on v2's diagram; P38, the read
   *   definition flagged as "could not be loaded" because the versions of the URL's tenant were kept.
   */
  it('should show the instance on its own definition when the versions of its tenant cannot be loaded', async () => {
    engine({ instances: { pi1: activeInstance({ processDefinitionId: definitionId('invoice', '5') }) }, tenantId: 't1' })
    const versions = ProcessService.findProcessVersionsByDefinitionKey.getMockImplementation()
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async (key, tenantId, lazyLoad) => {
      if (tenantId === 't1') throw new Error('500')
      return versions(key, tenantId, lazyLoad)
    })
    const vm = context({ instanceId: 'pi1' })

    await open(vm)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', 't1', false)
    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(vm.processDefinitions.map(definition => definition.id)).toEqual(['invoice:5:id'])
    expect(vm.process.id).toBe('invoice:5:id')
    expect(vm.errorInstanceDefinitionId).toBeNull()
    expect(vm.selectedInstance.id).toBe('pi1')
    expect(vm.versionsScope).toBeNull()
  })

  /**
   * Scenario: the user opens an instance link and goes to Tasks while the versions and the instance load.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/tasks
   * Expected: the run stops: no details, statistics, activity tree or history request, and nothing is selected.
   * Guards against: P43, requests for a page that is gone, whose failures show the error dialog on Tasks.
   */
  it('should stop loading the route when the page is left meanwhile', async () => {
    engine()
    const versions = ProcessService.findProcessVersionsByDefinitionKey.getMockImplementation()
    const vm = context({ instanceId: 'pi1', clearStaticCalledProcessDefinitions: vi.fn() })
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async (...args) => {
      ProcessDefinitionView.beforeUnmount.call(vm)
      return versions(...args)
    })

    await open(vm)

    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
    expect(vm.$store.dispatch).not.toHaveBeenCalled()
    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
    expect(HistoryService.findActivitiesInstancesHistory).not.toHaveBeenCalled()
    expect(vm.process).toBeNull()
    expect(vm.selectedInstance).toBeNull()
  })

  /**
   * Scenario: the user opens an instance link and leaves the page while the details of its definition load.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/tasks
   * Expected: no statistics, activity tree or history request follows.
   * Guards against: P43, the run going on after unmount, as these steps only check the open definition and instance.
   */
  it('should request no statistics or activities when the page is left while the details load', async () => {
    engine()
    const details = ProcessService.findProcessById.getMockImplementation()
    const vm = context({ instanceId: 'pi1', clearStaticCalledProcessDefinitions: vi.fn() })
    ProcessService.findProcessById.mockImplementation(async (...args) => {
      ProcessDefinitionView.beforeUnmount.call(vm)
      return details(...args)
    })

    await open(vm)

    expect(ProcessService.findProcessById).toHaveBeenCalledOnce()
    expect(vm.$store.dispatch).not.toHaveBeenCalled()
    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
    expect(HistoryService.findActivitiesInstancesHistory).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user leaves the page while the activity tree of the open instance loads.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/tasks
   * Expected: the activity history is not requested.
   * Guards against: P43, a history request for an instance that is no longer shown.
   */
  it('should request no activity history when the page is left while the activity tree loads', async () => {
    engine()
    const vm = context({ instanceId: 'pi1', clearStaticCalledProcessDefinitions: vi.fn() })
    ProcessService.findActivityInstance.mockImplementation(async () => {
      ProcessDefinitionView.beforeUnmount.call(vm)
      return { id: 'ai' }
    })

    await open(vm)

    expect(ProcessService.findActivityInstance).toHaveBeenCalledOnce()
    expect(HistoryService.findActivitiesInstancesHistory).not.toHaveBeenCalled()
    expect(vm.activityInstance).toBeNull()
  })

  /**
   * Scenario: the user leaves the page while the statistics of the open definition load, and that request fails (403).
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/tasks
   * Expected: the failure is dropped with the run: syncWithRoute resolves.
   * Guards against: P43, the failure of a page that is gone rethrown from syncWithRoute.
   */
  it('should drop the failure of a run whose page was left', async () => {
    engine()
    const vm = context({ instanceId: 'pi1', clearStaticCalledProcessDefinitions: vi.fn() })
    vm.$store.dispatch.mockImplementation(async () => {
      ProcessDefinitionView.beforeUnmount.call(vm)
      throw new Error('403 forbidden')
    })

    await expect(open(vm)).resolves.toBeUndefined()
  })
})

describe('ProcessDefinitionView - syncDefinition', () => {
  // syncs to the given definition as the only version, the one the URL asks for
  function syncTo(vm, definition) {
    Object.assign(vm, { processDefinitions: [definition], versionIndex: definition.version })
    return m.syncDefinition.call(vm)
  }

  /**
   * Scenario: the user refreshes the versions list; the open version comes back as a new object.
   * Link: #/seven/auth/process/invoice/2
   * Expected: the new entry is selected and keeps the statistics its diagram shows.
   * Guards against: the diagram losing its statistics overlays, as ProcessInstancesView only reloads them when the
   *   definition id changes.
   */
  it('should keep the statistics when the same version is selected from a reloaded list', async () => {
    const statistics = [{ id: 'task' }]
    const fresh = version()
    const vm = context({ process: version({ statistics }) })

    await syncTo(vm, fresh)

    expect(vm.process).toBe(fresh)
    expect(fresh.statistics).toBe(statistics)
  })

  /**
   * Scenario: the user switches from v2 to v1.
   * Link: from #/seven/auth/process/invoice/2 to #/seven/auth/process/invoice/1
   * Expected: v1 does not get v2's statistics.
   * Guards against: v2's statistics drawn on v1's diagram.
   */
  it('should not carry statistics over to another version', async () => {
    const fresh = version({ id: 'pd:1:abc', version: '1' })
    const vm = context({ process: version({ statistics: [{ id: 'task' }] }) })

    await syncTo(vm, fresh)

    expect(fresh.statistics).toBeUndefined()
  })

  /**
   * Scenario: the instance view opens a definition whose statistics are already loaded.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: no loadStatistics request.
   * Guards against: a duplicate statistics request.
   */
  it('should not load statistics the instance view already has', async () => {
    const vm = context({ instanceId: 'pi1', selectedInstance: { id: 'pi1' } })

    await syncTo(vm, version({ statistics: [] }))

    expect(vm.$store.dispatch).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user opens another process while the details of the current definition are loading.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/process/order/2
   * Expected: invoice's late details are not written, and no statistics are loaded for it.
   * Guards against: P2, a slow reply for the previous definition written over the current page.
   */
  it('should drop the details of a definition that is no longer selected', async () => {
    ProcessService.findProcessById.mockImplementation(async (id) => {
      vm.process = version({ id: 'order:2:id', key: 'order' })
      return { id, name: 'Invoice' }
    })
    const vm = context({ instanceId: 'pi1' })
    const first = version()

    await syncTo(vm, first)

    expect(first.name).toBeUndefined()
    expect(vm.$store.dispatch).not.toHaveBeenCalled()
  })
})

describe('ProcessDefinitionView - loadRoute instance selection', () => {
  // runs loadRoute with the versions loaded and the definition in sync, so only its instance step works
  function select(vm, instance) {
    Object.assign(vm, {
      instanceId: instance.id,
      versionsScope: { processKey: 'invoice', tenantId: null },
      lookUpInstance: vi.fn(async () => instance),
      syncDefinition: vi.fn()
    })
    return m.loadRoute.call(vm, () => false)
  }

  /**
   * Scenario: the user opens an active instance.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: its runtime activity tree (current activities) and its activity history are loaded and shown.
   * Happy path: the diagram shows where the instance is and where it has been.
   */
  it('should load the runtime activity tree and history for an active instance', async () => {
    const vm = context()
    ProcessService.findActivityInstance.mockResolvedValue({ id: 'ai1' })
    HistoryService.findActivitiesInstancesHistory.mockResolvedValue([{ id: 'h1' }])

    await select(vm, { id: 'pi1', state: 'ACTIVE' })

    expect(ProcessService.findActivityInstance).toHaveBeenCalledWith('pi1')
    expect(vm.activityInstance).toEqual({ id: 'ai1' })
    expect(vm.activityInstanceHistory).toEqual([{ id: 'h1' }])
  })

  /**
   * Scenario: the user opens a suspended instance; the history API reports it with state SUSPENDED. A suspended
   *   instance is still running and has current activities.
   * Link: #/seven/auth/process/invoice/2/pi1 for a suspended instance
   * Expected: the runtime activity tree is loaded.
   * Guards against: P19, no token or current-activity markers for a running instance.
   */
  it('should load the runtime activity tree for a suspended instance', async () => {
    const vm = context()
    ProcessService.findActivityInstance.mockResolvedValue({ id: 'ai1' })

    await select(vm, { id: 'pi1', state: 'SUSPENDED' })

    expect(ProcessService.findActivityInstance).toHaveBeenCalledWith('pi1')
    expect(vm.activityInstance).toEqual({ id: 'ai1' })
  })

  /**
   * Scenario: the user opens a finished instance: completed, cancelled by a user, or ended by the engine.
   * Link: #/seven/auth/process/invoice/2/pi1 for a finished instance
   * Expected: no runtime activity tree is requested; a finished instance has none.
   * Guards against: a runtime request that can only fail.
   */
  it.each(['COMPLETED', 'EXTERNALLY_TERMINATED', 'INTERNALLY_TERMINATED'])('should load no runtime activity tree for a %s instance', async (state) => {
    const vm = context()

    await select(vm, { id: 'pi1', state })

    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
  })

  /**
   * Scenario: with history off, the instance comes from the runtime API, which reports `ended` rather than a state.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: a not-ended instance is treated as running and its activity tree is loaded.
   * Guards against: no current activities for instances from the runtime API.
   */
  it('should treat a not-ended runtime instance as active', async () => {
    const vm = context()

    await select(vm, { id: 'pi1', ended: false })

    expect(ProcessService.findActivityInstance).toHaveBeenCalled()
  })

  /**
   * Scenario: the user opens a completed instance.
   * Link: #/seven/auth/process/invoice/2/pi1 for a completed instance
   * Expected: only its activity history is loaded; a finished instance has no runtime activity tree.
   * Happy path: the diagram shows the path the instance took.
   */
  it('should load only the history for a finished instance', async () => {
    const vm = context()
    HistoryService.findActivitiesInstancesHistory.mockResolvedValue([{ id: 'h1' }])

    await select(vm, { id: 'pi1', state: 'COMPLETED' })

    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
    expect(vm.activityInstanceHistory).toEqual([{ id: 'h1' }])
  })

  /**
   * Scenario: history level none and a finished instance: neither a runtime tree nor a history exists.
   * Link: #/seven/auth/process/invoice/2/pi1 for a finished instance
   * Expected: no history request.
   * Guards against: a history request that can only fail.
   */
  it('should load nothing for a finished instance when history is off', async () => {
    const vm = context()
    vm.$root.config.camundaHistoryLevel = 'none'

    await select(vm, { id: 'pi1', state: 'COMPLETED' })

    expect(HistoryService.findActivitiesInstancesHistory).not.toHaveBeenCalled()
  })

  /**
   * Scenario: history level none; an active instance from the runtime API.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: the activity tree is loaded; the history is not requested and stays null.
   * Guards against: P5, a history request that can only fail, sent for every active instance.
   */
  it('should load only the runtime activity tree for an active instance when history is off', async () => {
    const vm = context()
    vm.$root.config.camundaHistoryLevel = 'none'
    ProcessService.findActivityInstance.mockResolvedValue({ id: 'ai1' })

    await select(vm, { id: 'pi1', ended: false })

    expect(vm.activityInstance).toEqual({ id: 'ai1' })
    expect(HistoryService.findActivitiesInstancesHistory).not.toHaveBeenCalled()
    expect(vm.activityInstanceHistory).toBeNull()
  })

  /**
   * Scenario: a run for the instance that is already open, e.g. after another route change of the same instance.
   * Link: #/seven/auth/process/invoice/2/pi1, unchanged
   * Expected: its activity tree is not requested again.
   * Guards against: re-fetching everything for the open instance.
   */
  it('should skip reloading the instance that is already selected', async () => {
    const vm = context({ selectedInstance: { id: 'pi1', state: 'ACTIVE' } })

    await select(vm, { id: 'pi1', state: 'ACTIVE' })

    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user opens pi1 while another instance's data is shown.
   * Link: from #/seven/auth/process/invoice/2/old to #/seven/auth/process/invoice/2/pi1
   * Expected: pi1 is selected and the previous instance's activity data is gone.
   * Guards against: the old instance's activity data shown with pi1.
   */
  it('should drop the previous instance data before selecting another one', async () => {
    const vm = context({ selectedInstance: { id: 'old' }, activityInstance: { id: 'ai-old' } })
    vm.$root.config.camundaHistoryLevel = 'none'

    await select(vm, { id: 'pi1', state: 'COMPLETED' })

    expect(vm.selectedInstance.id).toBe('pi1')
    expect(vm.activityInstance).toBeNull()
  })

  /**
   * Scenario: the active instance ended between its lookup and the activity tree request, which returns 404.
   * Link: #/seven/auth/process/invoice/2/pi1
   * Expected: pi1 stays shown, its activity tree stays null and its history is still loaded.
   * Guards against: P12, an unhandled error and a diagram without history.
   */
  it('should still load the history when the activity tree cannot be loaded', async () => {
    const vm = context()
    ProcessService.findActivityInstance.mockRejectedValue(new Error('404 not found'))
    HistoryService.findActivitiesInstancesHistory.mockResolvedValue([{ id: 'h1' }])

    await select(vm, { id: 'pi1', state: 'ACTIVE' })

    expect(vm.selectedInstance.id).toBe('pi1')
    expect(vm.activityInstance).toBeNull()
    expect(vm.activityInstanceHistory).toEqual([{ id: 'h1' }])
  })

  /**
   * Scenario: the history request of a completed instance fails (500).
   * Link: #/seven/auth/process/invoice/2/pi1 for a completed instance
   * Expected: pi1 stays shown and its history stays null.
   * Guards against: P12, the failed request aborting the instance view.
   */
  it('should keep the instance when the history cannot be loaded', async () => {
    const vm = context()
    HistoryService.findActivitiesInstancesHistory.mockRejectedValue(new Error('500'))

    await select(vm, { id: 'pi1', state: 'COMPLETED' })

    expect(vm.selectedInstance.id).toBe('pi1')
    expect(vm.activityInstanceHistory).toBeNull()
  })

  /**
   * Scenario: the user opens pi2 while pi1's activity tree is loading.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/process/invoice/2/pi2
   * Expected: pi1's late activity tree is not shown and its history is not requested.
   * Guards against: pi1's current activities drawn on pi2.
   */
  it('should drop the activity data of an instance that is no longer selected', async () => {
    const vm = context()
    ProcessService.findActivityInstance.mockImplementation(async () => {
      vm.selectedInstance = { id: 'pi2' }
      return { id: 'ai1' }
    })

    await select(vm, { id: 'pi1', state: 'ACTIVE' })

    expect(vm.activityInstance).toBeNull()
    expect(HistoryService.findActivitiesInstancesHistory).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user opens pi2 while pi1's activity history is loading.
   * Link: #/seven/auth/process/invoice/2/pi1, then #/seven/auth/process/invoice/2/pi2
   * Expected: pi1's late history is not shown.
   * Guards against: pi1's history drawn on pi2.
   */
  it('should drop the history of an instance that is no longer selected', async () => {
    const vm = context()
    HistoryService.findActivitiesInstancesHistory.mockImplementation(async () => {
      vm.selectedInstance = { id: 'pi2' }
      return [{ id: 'h1' }]
    })

    await select(vm, { id: 'pi1', state: 'COMPLETED' })

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

describe('ProcessDefinitionView - onDeleteProcessDefinition', () => {
  const params = { processDefinition: version({ key: 'invoice', version: '2' }) }

  it('should return to the process list when the last version is deleted', async () => {
    const vm = context()
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([])

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(ProcessService.deleteProcessDefinition).toHaveBeenCalledWith('pd:2:abc', true)
    expect(vm.$router.replace).toHaveBeenCalledWith('/seven/auth/processes')
  })

  /**
   * Scenario: the user deletes a version other than the open one in the sidebar.
   * Link: #/seven/auth/process/invoice/3, deleting version 2
   * Expected: no redirect; the versions list is refreshed and the open v3 is selected from it.
   * Happy path: the user stays on the version they are looking at.
   */
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

  /**
   * Scenario: the user deletes the open version 2; versions 5, 3 and 1 remain.
   * Link: #/seven/auth/process/invoice/2
   * Expected: a redirect to the nearest remaining version, #/seven/auth/process/invoice/3.
   * Happy path: the user stays on the process after deleting the version they were looking at.
   */
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

  /**
   * Scenario: the user deletes the open version of tenant t1's invoice.
   * Link: #/seven/auth/process/invoice/2?tenantId=t1
   * Expected: the remaining versions are loaded with t1 and the redirect keeps tenantId=t1.
   * Guards against: landing on the versions of another tenant, or on none.
   */
  it('should keep the tenant in the query when navigating', async () => {
    const vm = context({ process: version({ version: '2', tenantId: 't1' }), tenantId: 't1' })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([version({ version: '1' })])

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', 't1', false)
    expect(vm.$router.replace).toHaveBeenCalledWith(expect.objectContaining({
      query: { tenantId: 't1' }
    }))
  })

  /**
   * Scenario: a version is deleted while the URL names tenant t1, the tenant of an instance, and the open definition
   *   is the shared invoice (deployed without a tenant).
   * Link: #/seven/auth/process/invoice/2/pi1?tenantId=t1&tab=variables
   * Expected: the remaining versions are loaded without a tenant; the redirect to the nearest version keeps tab and
   *   drops tenantId, as the definition view names the definition's tenant.
   * Guards against: an empty list and a jump to the processes list, or a definition link naming the instance's tenant.
   */
  it('should use the tenant of the open definition rather than the one of the instance', async () => {
    const vm = context({
      process: version({ version: '2', tenantId: null }),
      instanceId: 'pi1',
      tenantId: 't1',
      $route: { query: { tenantId: 't1', tab: 'variables' } }
    })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([version({ version: '1', tenantId: null })])

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', null, false)
    expect(vm.$router.replace).toHaveBeenCalledWith(expect.objectContaining({
      query: { tab: 'variables' }
    }))
  })

  /**
   * Scenario: the user deletes a version of invoice and opens order before the reply arrives.
   * Link: #/seven/auth/process/invoice/2, then #/seven/auth/process/order/2
   * Expected: order's versions and definition stay, and there is no redirect.
   * Guards against: P4, invoice's versions replacing order's, or a redirect away from order.
   */
  it('should drop the versions when another key is opened meanwhile', async () => {
    const vm = context({ process: version(), versionsScope: { processKey: 'invoice', tenantId: null } })
    const order = [version({ id: 'order:2:id', key: 'order' })]
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async () => {
      Object.assign(vm, { processKey: 'order', versionsScope: { processKey: 'order', tenantId: null }, processDefinitions: order, process: order[0] })
      return []
    })

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(vm.processDefinitions).toBe(order)
    expect(vm.process).toBe(order[0])
    expect(vm.$router.replace).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user deletes version 2 of invoice, but the delete request fails (e.g. 403, or 500).
   * Link: #/seven/auth/process/invoice/3, deleting version 2
   * Expected: the handler resolves; the versions are not reloaded and stay as they are, and there is no redirect.
   * Guards against: P40, an unhandled rejection; the error dialog tells what failed.
   */
  it('should keep the versions when the delete fails', async () => {
    const versions = [version({ id: 'pd:3:abc', version: '3' }), version({ version: '2' })]
    const vm = context({ versionIndex: '3', process: versions[0], processDefinitions: versions })
    ProcessService.deleteProcessDefinition.mockRejectedValueOnce(new Error('403 forbidden'))

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(ProcessService.findProcessVersionsByDefinitionKey).not.toHaveBeenCalled()
    expect(vm.processDefinitions).toBe(versions)
    expect(vm.process).toBe(versions[0])
    expect(vm.$router.replace).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user deletes version 2 of invoice while version 3 is open; the delete succeeds, but the versions
   *   request that follows times out.
   * Link: #/seven/auth/process/invoice/3, deleting version 2
   * Expected: the handler resolves; v2 is dropped from the versions, v3 stays open with its details, no redirect.
   * Guards against: P40, the deleted version still listed in the sidebar, and an unhandled rejection.
   */
  it('should drop the deleted version when the versions cannot be reloaded', async () => {
    const open = version({ id: 'pd:3:abc', version: '3' })
    const vm = context({ versionIndex: '3', process: open, processDefinitions: [open, version({ version: '2' })] })
    ProcessService.findProcessVersionsByDefinitionKey.mockRejectedValueOnce(new Error('timeout'))

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(vm.processDefinitions).toEqual([open])
    expect(vm.process).toBe(open)
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
    expect(vm.$router.replace).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user deletes the open version 2 of invoice; the delete succeeds, but the versions request that
   *   follows times out. Versions 3 and 1 remain.
   * Link: #/seven/auth/process/invoice/2
   * Expected: v2 is dropped from the versions and the view redirects to the nearest remaining version,
   *   #/seven/auth/process/invoice/3.
   * Guards against: P40, the deleted version staying open with no redirect.
   */
  it('should navigate to the nearest remaining version when the versions cannot be reloaded', async () => {
    const deleted = version({ version: '2' })
    const remaining = [version({ id: 'pd:3:abc', version: '3' }), version({ id: 'pd:1:abc', version: '1' })]
    const vm = context({ process: deleted, processDefinitions: [remaining[0], deleted, remaining[1]] })
    ProcessService.findProcessVersionsByDefinitionKey.mockRejectedValueOnce(new Error('timeout'))

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(vm.processDefinitions).toEqual(remaining)
    expect(vm.$router.replace).toHaveBeenCalledWith({
      name: 'process',
      params: { processKey: 'invoice', versionIndex: '3' },
      query: {}
    })
  })
})

describe('ProcessDefinitionView - onRefreshProcessDefinitions', () => {
  /**
   * Scenario: the user clicks the sidebar's refresh button with lazy loading on.
   * Link: #/seven/auth/process/invoice/2
   * Expected: the versions are reloaded lazily with '-' placeholders; the open version is selected from the new list
   *   and keeps its statistics.
   * Happy path: newly deployed versions show up without leaving the page.
   */
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

  /**
   * Scenario: the versions are refreshed while the URL names tenant t1, the tenant of an instance, and the open
   *   definition is the shared invoice.
   * Link: #/seven/auth/process/invoice/2/pi1?tenantId=t1
   * Expected: the versions are reloaded without a tenant and nothing is flagged.
   * Guards against: an empty list flagged "definition not found" after a refresh.
   */
  it('should reload the versions with the tenant of the open definition', async () => {
    const shared = version({ tenantId: null })
    const vm = context({ process: shared, instanceId: 'pi1', tenantId: 't1' })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([version({ tenantId: null })])

    await m.onRefreshProcessDefinitions.call(vm, false)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', null, false)
    expect(vm.errorVersionNotFound).toBeNull()
  })

  /**
   * Scenario: another user deleted every version of the key; this user clicks refresh.
   * Link: #/seven/auth/process/invoice/2
   * Expected: the definition, its breadcrumb and the versions are cleared and the version is flagged, so both panes
   *   say the definition was not found.
   * Guards against: P7, the deleted definition staying open next to an empty versions list.
   */
  it('should drop the open definition and flag it when no versions remain', async () => {
    const vm = context({ process: version(), processDefinitions: [version()], parentProcess: { id: 'parent' } })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([])

    await m.onRefreshProcessDefinitions.call(vm, false)

    expect(vm.process).toBeNull()
    expect(vm.processDefinitions).toEqual([])
    expect(vm.parentProcess).toBeNull()
    expect(vm.errorVersionNotFound).toBe('2')
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the sidebar has two buttons: refresh the versions, or load all their data.
   * Link: #/seven/auth/process/invoice/2
   * Expected: refreshingVersions is 'all' or 'refresh' while the request runs, so the right button spins, and null
   *   once it is done.
   * Happy path: the user sees which reload is running.
   */
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

  /**
   * Scenario: the refresh request fails, e.g. the engine is down.
   * Link: #/seven/auth/process/invoice/2
   * Expected: the error is passed on and refreshingVersions is null again.
   * Guards against: a button that spins forever after an error.
   */
  it('should end the reload state when the versions cannot be loaded', async () => {
    const vm = context()
    ProcessService.findProcessVersionsByDefinitionKey.mockRejectedValue(new Error('down'))

    await expect(m.onRefreshProcessDefinitions.call(vm, false)).rejects.toThrow('down')

    expect(vm.refreshingVersions).toBeNull()
  })

  /**
   * Scenario: the user refreshes the versions of invoice and opens order before the reply arrives.
   * Link: #/seven/auth/process/invoice/2, then #/seven/auth/process/order/2
   * Expected: order's versions and definition stay; the refresh state still ends.
   * Guards against: P4, invoice's versions replacing order's and selecting an invoice version on the order page.
   */
  it('should drop the versions when another key is opened meanwhile', async () => {
    const vm = context({ process: version(), versionsScope: { processKey: 'invoice', tenantId: null } })
    const order = [version({ id: 'order:2:id', key: 'order' })]
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async () => {
      Object.assign(vm, { processKey: 'order', versionsScope: { processKey: 'order', tenantId: null }, processDefinitions: order, process: order[0] })
      return [version()]
    })

    await m.onRefreshProcessDefinitions.call(vm, false)

    expect(vm.processDefinitions).toBe(order)
    expect(vm.process).toBe(order[0])
    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
    expect(vm.refreshingVersions).toBeNull()
  })
})

describe('ProcessDefinitionView - misc', () => {
  /**
   * Scenario: the user deletes an instance in the instances table.
   * Link: #/seven/auth/process/invoice/2
   * Expected: the statistics are reloaded, the version's counts updated (its own incidents count kept) and the diagram
   *   redrawn.
   * Happy path: the table, the counts and the diagram agree after the deletion.
   */
  it('onInstanceDeleted should refresh the statistics and counts and redraw', async () => {
    const entry = version({ incidents: 3 })
    const vm = context({ process: entry })
    ProcessService.findProcessById.mockResolvedValue({ id: 'pd:2:abc', runningInstances: 4, incidents: 0 })

    await m.onInstanceDeleted.call(vm)

    expect(vm.$store.dispatch).toHaveBeenCalledWith('loadStatistics', entry)
    expect(entry).toMatchObject({ runningInstances: 4, incidents: 3 })
    expect(vm.$refs.process.refreshDiagram).toHaveBeenCalled()
  })

  /**
   * Scenario: the user deletes an instance and opens another process before the refresh is done.
   * Link: #/seven/auth/process/invoice/2, then #/seven/auth/process/order/2
   * Expected: invoice's late details are not written and nothing throws.
   * Guards against: P20, a TypeError on the cleared definition.
   */
  it('onInstanceDeleted should drop the details once another definition is open', async () => {
    const entry = version()
    const vm = context({ process: entry })
    ProcessService.findProcessById.mockImplementation(async () => {
      Object.assign(vm, { process: null, $refs: {} })
      return { id: 'pd:2:abc', runningInstances: 4 }
    })

    await m.onInstanceDeleted.call(vm)

    expect(entry.runningInstances).toBeUndefined()
  })

  /**
   * Scenario: the instances view is already unmounted when the refresh after an instance deletion ends.
   * Link: #/seven/auth/process/invoice/2, left meanwhile
   * Expected: the handler ends without an error.
   * Guards against: P20, calling refreshDiagram on an unmounted view.
   */
  it('onInstanceDeleted should not redraw an instances view that is gone', async () => {
    const vm = context({ process: version(), $refs: {} })
    ProcessService.findProcessById.mockResolvedValue({ id: 'pd:2:abc' })

    await expect(m.onInstanceDeleted.call(vm)).resolves.toBeUndefined()
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

  /**
   * Scenario: the user deletes the open version (a cascade delete can take seconds) and meanwhile opens Tasks or
   *   Deployments.
   * Link: #/seven/auth/process/invoice/2, then e.g. #/seven/auth/tasks
   * Expected: no redirect when the delete ends.
   * Guards against: P23, the user thrown back to the nearest remaining version, or to the processes list.
   */
  it.each([
    ['the nearest remaining version', [version({ version: '1' })]],
    ['the processes list', []]
  ])('should not redirect to %s when a delete ends after the page was left', async (_, versions) => {
    const vm = context({ process: version(), versionsScope: { processKey: 'invoice', tenantId: null }, clearStaticCalledProcessDefinitions: vi.fn() })
    ProcessService.deleteProcessDefinition.mockImplementation(async () => ProcessDefinitionView.beforeUnmount.call(vm))
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue(versions)

    await m.onDeleteProcessDefinition.call(vm, { processDefinition: version() })

    expect(vm.$router.replace).not.toHaveBeenCalled()
  })

  /**
   * Scenario: the user refreshes the versions and leaves the process page before the reply arrives.
   * Link: #/seven/auth/process/invoice/2, then another page
   * Expected: the late versions are not written.
   * Guards against: P23, a reply written into a view the user has left.
   */
  it('should drop the versions of a refresh that ends after the page was left', async () => {
    const shown = [version()]
    const vm = context({
      process: shown[0], processDefinitions: shown, versionsScope: { processKey: 'invoice', tenantId: null }, clearStaticCalledProcessDefinitions: vi.fn()
    })
    ProcessService.findProcessVersionsByDefinitionKey.mockImplementation(async () => {
      ProcessDefinitionView.beforeUnmount.call(vm)
      return []
    })

    await m.onRefreshProcessDefinitions.call(vm, false)

    expect(vm.processDefinitions).toBe(shown)
  })
})
