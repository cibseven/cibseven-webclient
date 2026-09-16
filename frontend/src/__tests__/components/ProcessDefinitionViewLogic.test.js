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
    loading: false,
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
  vi.clearAllMocks()
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

describe('ProcessDefinitionView - loadInstanceById', () => {
  it('should select the instance it finds', async () => {
    const vm = context({ setSelectedInstance: vi.fn() })
    HistoryService.findProcessInstance.mockResolvedValue({ id: 'pi1' })

    await m.loadInstanceById.call(vm, 'pi1')

    expect(vm.setSelectedInstance).toHaveBeenCalledWith({ selectedInstance: { id: 'pi1' } })
    expect(vm.errorLoadingInstanceId).toBeNull()
  })

  it('should clear the previous selection first', async () => {
    const vm = context({
      selectedInstance: { id: 'old' },
      activityInstanceHistory: [{}],
      task: { id: 't1' },
      setSelectedInstance: vi.fn()
    })

    await m.loadInstanceById.call(vm, 'pi1')

    expect(vm.activityInstanceHistory).toBeNull()
    expect(vm.task).toBeNull()
  })

  it('should record the failure message when the lookup throws', async () => {
    const vm = context({ setSelectedInstance: vi.fn() })
    HistoryService.findProcessInstance.mockRejectedValue(new Error('404 not found'))

    await m.loadInstanceById.call(vm, 'pi1')

    expect(vm.errorLoadingInstanceId).toBe('404 not found')
  })

  // An instance already listed in the table can be used even if history has purged it.
  it('should fall back to an instance already in the store', async () => {
    const vm = context({ instances: [{ id: 'pi1', state: 'ACTIVE' }], setSelectedInstance: vi.fn() })
    HistoryService.findProcessInstance.mockResolvedValue(null)

    await m.loadInstanceById.call(vm, 'pi1')

    expect(vm.setSelectedInstance).toHaveBeenCalledWith({ selectedInstance: { id: 'pi1', state: 'ACTIVE' } })
  })

  it('should select nothing when the instance is nowhere to be found', async () => {
    const vm = context({ instances: [], setSelectedInstance: vi.fn() })
    HistoryService.findProcessInstance.mockResolvedValue(null)

    await m.loadInstanceById.call(vm, 'pi-gone')

    expect(vm.setSelectedInstance).not.toHaveBeenCalled()
  })
})

describe('ProcessDefinitionView - switchToDefinitionVersion', () => {
  it('should load the version the URL asks for', async () => {
    const requested = version({ version: '2' })
    const vm = context({ processDefinitions: [version({ version: '3' }), requested], loadProcessVersion: vi.fn() })

    await m.switchToDefinitionVersion.call(vm)

    expect(vm.loadProcessVersion).toHaveBeenCalledWith(requested)
    expect(vm.errorVersionNotFound).toBeNull()
  })

  // A stale link to a deleted version should still show something useful, flagged.
  it('should fall back to the latest version and flag the missing one', async () => {
    const latest = version({ version: '9' })
    const vm = context({ versionIndex: '42', processDefinitions: [latest], loadProcessVersion: vi.fn() })

    await m.switchToDefinitionVersion.call(vm)

    expect(vm.errorVersionNotFound).toBe('42')
    expect(vm.loadProcessVersion).toHaveBeenCalledWith(latest)
  })

  it('should only flag the error when the key has no versions at all', async () => {
    const vm = context({ processDefinitions: [], loadProcessVersion: vi.fn() })

    await m.switchToDefinitionVersion.call(vm)

    expect(vm.errorVersionNotFound).toBe('2')
    expect(vm.loadProcessVersion).not.toHaveBeenCalled()
  })

  it('should reload the addressed instance after switching', async () => {
    const vm = context({
      instanceId: 'pi1',
      processDefinitions: [version()],
      loadProcessVersion: vi.fn(),
      loadInstanceById: vi.fn()
    })

    await m.switchToDefinitionVersion.call(vm)

    expect(vm.loadInstanceById).toHaveBeenCalledWith('pi1')
  })
})

describe('ProcessDefinitionView - loadProcessDefinitionFromRoute', () => {
  it('should load the versions for the route key', async () => {
    const vm = context({ switchToDefinitionVersion: vi.fn(), resetStatsLazyLoad: vi.fn() })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([version()])

    await m.loadProcessDefinitionFromRoute.call(vm)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', undefined, false)
    expect(vm.processDefinitions).toEqual([version()])
    expect(vm.switchToDefinitionVersion).toHaveBeenCalled()
  })

  // Arriving by instance link, the definition is discovered from the instance and its
  // tenant then scopes the version lookup.
  it('should resolve the definition and tenant from an addressed instance', async () => {
    const vm = context({
      instanceId: 'pi1',
      loadInstanceById: vi.fn(function () { this.selectedInstance = { id: 'pi1', processDefinitionId: 'pd:2:abc' } }),
      loadStatistics: vi.fn(),
      switchToDefinitionVersion: vi.fn(),
      resetStatsLazyLoad: vi.fn()
    })
    ProcessService.findProcessById.mockResolvedValue({ id: 'pd:2:abc', tenantId: 't1' })

    await m.loadProcessDefinitionFromRoute.call(vm)

    expect(ProcessService.findProcessById).toHaveBeenCalledWith('pd:2:abc', true)
    expect(vm.loadStatistics).toHaveBeenCalled()
    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', 't1', false)
  })

  // Runtime instances carry `definitionId` rather than `processDefinitionId`.
  it('should accept the runtime definitionId spelling', async () => {
    const vm = context({
      instanceId: 'pi1',
      loadInstanceById: vi.fn(function () { this.selectedInstance = { id: 'pi1', definitionId: 'pd:2:abc' } }),
      loadStatistics: vi.fn(),
      switchToDefinitionVersion: vi.fn(),
      resetStatsLazyLoad: vi.fn()
    })

    await m.loadProcessDefinitionFromRoute.call(vm)

    expect(ProcessService.findProcessById).toHaveBeenCalledWith('pd:2:abc', true)
  })

  it('should reset the lazy statistics placeholders when no definition is loaded yet', async () => {
    const vm = context({ switchToDefinitionVersion: vi.fn(), resetStatsLazyLoad: vi.fn() })
    vm.$root.config.lazyLoadHistory = true

    await m.loadProcessDefinitionFromRoute.call(vm)

    expect(vm.resetStatsLazyLoad).toHaveBeenCalledWith(true)
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

  it('should leave the counts alone when not lazy loading', () => {
    const vm = context({ processDefinitions: [version({ runningInstances: 5 })] })

    m.resetStatsLazyLoad.call(vm, false)

    expect(vm.processDefinitions[0].runningInstances).toBe(5)
  })

  it('loadStatistics should publish the statistics for the loaded definition', async () => {
    const vm = context({ process: version() })
    ProcessService.findProcessStatistics.mockResolvedValue([{ id: 'task_1' }])

    await m.loadStatistics.call(vm)

    expect(ProcessService.findProcessStatistics).toHaveBeenCalledWith('pd:2:abc')
    expect(vm.$store.dispatch).toHaveBeenCalledWith('setStatistics', {
      process: vm.process, statistics: [{ id: 'task_1' }]
    })
  })
})

describe('ProcessDefinitionView - loadProcessVersion', () => {
  it('should adopt the version and enrich it', async () => {
    const vm = context({ findProcessAndAssignData: vi.fn(), loadStatistics: vi.fn() })
    const v = version({ statistics: [] })

    await m.loadProcessVersion.call(vm, v)

    expect(vm.process).toBe(v)
    expect(vm.findProcessAndAssignData).toHaveBeenCalledWith(v)
    expect(vm.loadStatistics).not.toHaveBeenCalled()
  })

  it('should load statistics when the version has none yet', async () => {
    const vm = context({ findProcessAndAssignData: vi.fn(), loadStatistics: vi.fn() })

    await m.loadProcessVersion.call(vm, version())

    expect(vm.loadStatistics).toHaveBeenCalled()
  })

  // Arriving from a call activity, the parent is shown as a breadcrumb.
  it('should load the parent process named in the query', async () => {
    const getProcessById = vi.fn(() => Promise.resolve({ id: 'pd-parent', name: 'Parent' }))
    const vm = context({ findProcessAndAssignData: vi.fn(), loadStatistics: vi.fn(), getProcessById })
    vm.$route.query.parentProcessDefinitionId = 'pd-parent'

    await m.loadProcessVersion.call(vm, version())

    expect(getProcessById).toHaveBeenCalledWith({ id: 'pd-parent' })
    expect(vm.parentProcess).toEqual({ id: 'pd-parent', name: 'Parent' })
  })

  it('should clear the parent process when the query does not name one', async () => {
    const vm = context({
      parentProcess: { id: 'stale' },
      findProcessAndAssignData: vi.fn(),
      loadStatistics: vi.fn()
    })

    await m.loadProcessVersion.call(vm, version())

    expect(vm.parentProcess).toBeNull()
  })
})

describe('ProcessDefinitionView - findProcessAndAssignData', () => {
  it('should merge the enriched definition into the matching version entry', async () => {
    const entry = version({ id: 'pd:2:abc' })
    const vm = context({ processDefinitions: [entry] })
    ProcessService.findProcessById.mockResolvedValue({ id: 'pd:2:abc', name: 'Invoice', runningInstances: 3 })

    await m.findProcessAndAssignData.call(vm, entry)

    expect(entry).toMatchObject({ name: 'Invoice', runningInstances: 3 })
  })

  it('should leave other version entries untouched', async () => {
    const other = version({ id: 'pd:1:abc' })
    const vm = context({ processDefinitions: [other] })
    ProcessService.findProcessById.mockResolvedValue({ id: 'pd:2:abc', name: 'Invoice' })

    await m.findProcessAndAssignData.call(vm, version())

    expect(other.name).toBeUndefined()
  })

  it('should do nothing without a selected process', async () => {
    await m.findProcessAndAssignData.call(context(), null)

    expect(ProcessService.findProcessById).not.toHaveBeenCalled()
  })
})

describe('ProcessDefinitionView - setSelectedInstance', () => {
  it('should load the runtime activity tree and history for an active instance', async () => {
    const vm = context()
    ProcessService.findActivityInstance.mockResolvedValue({ id: 'ai1' })
    HistoryService.findActivitiesInstancesHistory.mockResolvedValue([{ id: 'h1' }])

    await m.setSelectedInstance.call(vm, { selectedInstance: { id: 'pi1', state: 'ACTIVE' } })

    expect(ProcessService.findActivityInstance).toHaveBeenCalledWith('pi1')
    expect(vm.activityInstance).toEqual({ id: 'ai1' })
    expect(vm.activityInstanceHistory).toEqual([{ id: 'h1' }])
  })

  // Runtime instances report `ended` rather than a `state`.
  it('should treat a not-ended runtime instance as active', async () => {
    const vm = context()

    await m.setSelectedInstance.call(vm, { selectedInstance: { id: 'pi1', ended: false } })

    expect(ProcessService.findActivityInstance).toHaveBeenCalled()
  })

  it('should load only the history for a finished instance', async () => {
    const vm = context()
    HistoryService.findActivitiesInstancesHistory.mockResolvedValue([{ id: 'h1' }])

    await m.setSelectedInstance.call(vm, { selectedInstance: { id: 'pi1', state: 'COMPLETED' } })

    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
    expect(vm.activityInstanceHistory).toEqual([{ id: 'h1' }])
  })

  it('should load nothing for a finished instance when history is off', async () => {
    const vm = context()
    vm.$root.config.camundaHistoryLevel = 'none'

    await m.setSelectedInstance.call(vm, { selectedInstance: { id: 'pi1', state: 'COMPLETED' } })

    expect(HistoryService.findActivitiesInstancesHistory).not.toHaveBeenCalled()
  })

  // Re-selecting the open instance would re-fetch everything for no reason.
  it('should skip reloading the instance that is already selected', async () => {
    const vm = context({ selectedInstance: { id: 'pi1', state: 'ACTIVE' } })

    await m.setSelectedInstance.call(vm, { selectedInstance: { id: 'pi1', state: 'ACTIVE' } })

    expect(ProcessService.findActivityInstance).not.toHaveBeenCalled()
  })

  it('should clear everything when the selection is cleared', async () => {
    const vm = context({
      selectedInstance: { id: 'pi1' },
      activityInstance: { id: 'ai1' },
      activityInstanceHistory: [{}],
      task: { id: 't1' }
    })

    await m.setSelectedInstance.call(vm, { selectedInstance: null })

    expect(vm.selectedInstance).toBeNull()
    expect(vm.activityInstance).toBeNull()
    expect(vm.activityInstanceHistory).toBeNull()
    expect(vm.task).toBeNull()
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
    const vm = context({ process: version({ version: '3' }), resetStatsLazyLoad: vi.fn(), loadProcessVersion: vi.fn() })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([version({ version: '3' })])

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(vm.$router.replace).not.toHaveBeenCalled()
    expect(vm.loadProcessVersion).toHaveBeenCalledWith(vm.process)
  })

  // Deleting the open version navigates to whichever remaining version is nearest.
  it('should navigate to the nearest remaining version', async () => {
    const vm = context({ process: version({ version: '2' }), resetStatsLazyLoad: vi.fn() })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([
      version({ version: '5' }), version({ version: '3' }), version({ version: '1' })
    ])

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(vm.$router.replace).toHaveBeenCalledWith({
      name: 'process',
      params: { processKey: 'invoice', versionIndex: '3' },
      query: {}
    })
  })

  it('should keep the tenant in the query when navigating', async () => {
    const vm = context({ process: version({ version: '2' }), tenantId: 't1', resetStatsLazyLoad: vi.fn() })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([version({ version: '1' })])

    await m.onDeleteProcessDefinition.call(vm, params)

    expect(vm.$router.replace).toHaveBeenCalledWith(expect.objectContaining({
      query: { tenantId: 't1' }
    }))
  })
})

describe('ProcessDefinitionView - onRefreshProcessDefinitions', () => {
  it('should reload the versions and re-select the open one', async () => {
    const vm = context({ process: version(), resetStatsLazyLoad: vi.fn(), loadProcessVersion: vi.fn() })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([version()])

    const result = await m.onRefreshProcessDefinitions.call(vm, true)

    expect(ProcessService.findProcessVersionsByDefinitionKey).toHaveBeenCalledWith('invoice', undefined, true)
    expect(vm.resetStatsLazyLoad).toHaveBeenCalledWith(true)
    expect(vm.loadProcessVersion).toHaveBeenCalledWith(vm.process)
    expect(result).toEqual([version()])
  })

  it('should not try to select anything when no versions remain', async () => {
    const vm = context({ resetStatsLazyLoad: vi.fn(), loadProcessVersion: vi.fn() })
    ProcessService.findProcessVersionsByDefinitionKey.mockResolvedValue([])

    await m.onRefreshProcessDefinitions.call(vm, false)

    expect(vm.loadProcessVersion).not.toHaveBeenCalled()
  })
})

describe('ProcessDefinitionView - misc', () => {
  it('onInstanceDeleted should clear the selection, refresh statistics and redraw', async () => {
    const vm = context({
      process: version(),
      setSelectedInstance: vi.fn(),
      loadStatistics: vi.fn(),
      findProcessAndAssignData: vi.fn()
    })

    await m.onInstanceDeleted.call(vm)

    expect(vm.setSelectedInstance).toHaveBeenCalledWith({ selectedInstance: null })
    expect(vm.loadStatistics).toHaveBeenCalled()
    expect(vm.findProcessAndAssignData).toHaveBeenCalledWith(vm.process)
    expect(vm.$refs.process.refreshDiagram).toHaveBeenCalled()
  })

  it('filterInstances should adopt the new filter', () => {
    const vm = context()

    m.filterInstances.call(vm, { unfinished: false, finished: true })

    expect(vm.filter).toEqual({ unfinished: false, finished: true })
  })

  it.each([
    ['ACTIVE', 'mdi-chevron-triple-right text-success'],
    ['SUSPENDED', 'mdi-close-circle-outline'],
    ['COMPLETED', 'mdi-flag-triangle'],
    [undefined, 'mdi-flag-triangle']
  ])('getIconState should map %s to its icon', (state, expected) => {
    expect(m.getIconState.call(context(), state)).toBe(expected)
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
