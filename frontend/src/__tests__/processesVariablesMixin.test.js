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
import { mount, flushPromises } from '@vue/test-utils'
import processesVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import { ProcessService, HistoryService } from '@/services.js'

vi.mock('@/services.js', () => ({
  ProcessService: {
    fetchProcessInstanceVariables: vi.fn(() => Promise.resolve([])),
    fetchVariableDataByExecutionId: vi.fn(() => Promise.resolve(new Blob(['x']))),
    modifyVariableByExecutionId: vi.fn(() => Promise.resolve()),
    modifyVariableDataByExecutionId: vi.fn(() => Promise.resolve())
  },
  HistoryService: {
    fetchProcessInstanceVariablesHistory: vi.fn(() => Promise.resolve([])),
    fetchHistoryVariableDataById: vi.fn(() => Promise.resolve(new Blob(['x'])))
  }
}))

const triggerDownload = vi.fn()

const MockPopper = {
  template: '<div></div>',
  methods: { triggerDownload }
}

// minimal host component standing in for VariablesTable.vue
const HostComponent = {
  mixins: [processesVariablesMixin],
  components: { MockPopper },
  template: '<div><mock-popper ref="importPopper"></mock-popper></div>',
  data() {
    return { filteredVariables: [] }
  }
}

// runtime activity instance tree of a running instance:
// process instance 'pi1' with one active user task and one active async transition
const runtimeTree = {
  id: 'pi1',
  name: 'My Process',
  activityId: 'my-process',
  childActivityInstances: [
    { id: 'userTask1:inst', activityId: 'userTask1', name: 'User Task 1', childActivityInstances: [] }
  ],
  childTransitionInstances: [
    { id: 'asyncTask1:trans', activityId: 'asyncTask1' }
  ]
}

// historic activity instances include the finished external-task activity
const historicActivities = [
  { id: 'pi1', activityId: 'my-process', activityName: 'My Process' },
  { id: 'userTask1:inst', activityId: 'userTask1', activityName: 'User Task 1' },
  { id: 'extTask1:inst', activityId: 'extTask1', activityName: 'External Task 1' }
]

const historyVariables = () => [
  { id: 'v1', name: 'globalVar', type: 'String', value: 'a', activityInstanceId: 'pi1', executionId: 'pi1' },
  { id: 'v2', name: 'liveLocalVar', type: 'String', value: 'b', activityInstanceId: 'userTask1:inst', executionId: 'ex2' },
  { id: 'v3', name: 'finishedScopeVar', type: 'String', value: 'c', activityInstanceId: 'extTask1:inst', executionId: 'ex3' },
  { id: 'v4', name: 'transitionVar', type: 'String', value: 'd', activityInstanceId: 'asyncTask1:trans', executionId: 'ex4' }
]

function createWrapper({ state = 'ACTIVE', historyLevel = 'full', activityInstance = null, activityInstanceHistory = null } = {}) {
  const wrapper = mount(HostComponent, {
    props: {
      selectedInstance: { id: 'pi1', state, processDefinitionName: 'My Process' },
      activityInstance,
      activityInstanceHistory
    }
  })
  // the mixin reads the history level from $root.config
  wrapper.vm.$root.config = { camundaHistoryLevel: historyLevel }
  return wrapper
}

describe('processesVariablesMixin', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    // mockClear keeps implementations, so reset them explicitly to keep the tests order-independent
    ProcessService.fetchProcessInstanceVariables.mockResolvedValue([])
    HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue([])
  })

  describe('loadSelectedInstanceVariables endpoint selection', () => {

    it.each(['full', 'audit'])('queries runtime AND history for ACTIVE instances at history level %s (CIB7-1203)', async (historyLevel) => {
      const wrapper = createWrapper({ state: 'ACTIVE', historyLevel })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()
      expect(ProcessService.fetchProcessInstanceVariables).toHaveBeenCalledWith('pi1', expect.any(Object))
      expect(HistoryService.fetchProcessInstanceVariablesHistory).toHaveBeenCalledWith('pi1', expect.any(Object))
    })

    it.each(['activity', 'none'])('falls back to the runtime endpoint for ACTIVE instances at history level %s', async (historyLevel) => {
      const wrapper = createWrapper({ state: 'ACTIVE', historyLevel })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()
      expect(ProcessService.fetchProcessInstanceVariables).toHaveBeenCalledWith('pi1', expect.any(Object))
      expect(HistoryService.fetchProcessInstanceVariablesHistory).not.toHaveBeenCalled()
    })

    it('uses the history endpoint for finished instances at history level full', async () => {
      const wrapper = createWrapper({ state: 'COMPLETED' })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()
      expect(HistoryService.fetchProcessInstanceVariablesHistory).toHaveBeenCalled()
      expect(ProcessService.fetchProcessInstanceVariables).not.toHaveBeenCalled()
    })

    it('shows no variables for finished instances at history level activity', async () => {
      const wrapper = createWrapper({ state: 'COMPLETED', historyLevel: 'activity' })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()
      expect(ProcessService.fetchProcessInstanceVariables).not.toHaveBeenCalled()
      expect(HistoryService.fetchProcessInstanceVariablesHistory).not.toHaveBeenCalled()
      expect(wrapper.vm.variables).toEqual([])
      expect(wrapper.vm.loading).toBe(false)
    })
  })

  describe('running instances are independent of the configured history level (CIB7-1203)', () => {

    const runtimeVariables = () => [
      { id: 'v1', name: 'globalVar', type: 'String', value: 'a', activityInstanceId: 'pi1', executionId: 'pi1' },
      { id: 'v2', name: 'liveLocalVar', type: 'String', value: 'b', activityInstanceId: 'userTask1:inst', executionId: 'ex2' }
    ]

    it('adds finished-scope variables from history to the runtime variables, without duplicates', async () => {
      ProcessService.fetchProcessInstanceVariables.mockResolvedValue(runtimeVariables())
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue(historyVariables())
      const wrapper = createWrapper({ activityInstance: runtimeTree, activityInstanceHistory: historicActivities })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()

      expect(wrapper.vm.variables.map(v => v.name))
        .toEqual(['finishedScopeVar', 'globalVar', 'liveLocalVar', 'transitionVar'])
      const byName = Object.fromEntries(wrapper.vm.variables.map(v => [v.name, v]))
      expect(byName.globalVar.variableSource).toBe('runtime')
      expect(byName.globalVar.isLive).toBe(true)
      expect(byName.finishedScopeVar.variableSource).toBe('history')
      expect(byName.finishedScopeVar.isLive).toBe(false)
    })

    it('keeps the runtime variables when history is empty because the engine level is lower than configured', async () => {
      ProcessService.fetchProcessInstanceVariables.mockResolvedValue(runtimeVariables())
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue([])
      const wrapper = createWrapper({ historyLevel: 'full', activityInstance: runtimeTree })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()

      expect(wrapper.vm.variables.map(v => v.name)).toEqual(['globalVar', 'liveLocalVar'])
      expect(wrapper.vm.variables.every(v => v.isLive)).toBe(true)
    })

    it('keeps the runtime variables when the history query fails', async () => {
      ProcessService.fetchProcessInstanceVariables.mockResolvedValue(runtimeVariables())
      HistoryService.fetchProcessInstanceVariablesHistory.mockRejectedValue(new Error('history level too low'))
      const wrapper = createWrapper({ historyLevel: 'full', activityInstance: runtimeTree })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()

      expect(wrapper.vm.variables.map(v => v.name)).toEqual(['globalVar', 'liveLocalVar'])
      expect(wrapper.vm.loading).toBe(false)
      expect(wrapper.vm.fetching).toBe(false)
    })

    it.each(['activity', 'none'])('does not call the history endpoint at history level %s', async (historyLevel) => {
      ProcessService.fetchProcessInstanceVariables.mockResolvedValue(runtimeVariables())
      const wrapper = createWrapper({ historyLevel, activityInstance: runtimeTree })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()

      expect(HistoryService.fetchProcessInstanceVariablesHistory).not.toHaveBeenCalled()
      expect(wrapper.vm.variables.map(v => v.name)).toEqual(['globalVar', 'liveLocalVar'])
    })

    it('prefers the runtime row when a variable is returned by both queries', async () => {
      ProcessService.fetchProcessInstanceVariables.mockResolvedValue([
        { id: 'v1', name: 'globalVar', type: 'String', value: 'new', activityInstanceId: 'pi1', executionId: 'pi1' }
      ])
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue([
        { id: 'v1', name: 'globalVar', type: 'String', value: 'stale', activityInstanceId: 'pi1', executionId: 'pi1' }
      ])
      const wrapper = createWrapper({ activityInstance: runtimeTree })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()

      expect(wrapper.vm.variables).toHaveLength(1)
      expect(wrapper.vm.variables[0].value).toBe('new')
      expect(wrapper.vm.variables[0].variableSource).toBe('runtime')
    })

    it('applies a variableValues filter to the finished-scope variables, which the history endpoint ignores', async () => {
      ProcessService.fetchProcessInstanceVariables.mockResolvedValue([])
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue([
        { id: 'v3', name: 'finishedScopeVar', type: 'String', value: 'c', activityInstanceId: 'extTask1:inst' },
        { id: 'v5', name: 'otherVar', type: 'String', value: 'x', activityInstanceId: 'extTask1:inst' }
      ])
      const wrapper = createWrapper({ activityInstance: runtimeTree, activityInstanceHistory: historicActivities })
      wrapper.vm.filter = {
        deserializeValues: false,
        variableValues: [{ name: 'finishedScopeVar', operator: 'eq', value: 'c' }]
      }
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()

      expect(wrapper.vm.variables.map(v => v.name)).toEqual(['finishedScopeVar'])
    })

    it('applies a variableValues filter to the history variables of a finished instance as well', async () => {
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue([
        { id: 'v1', name: 'globalVar', type: 'String', value: 'a', activityInstanceId: 'pi1' },
        { id: 'v3', name: 'finishedScopeVar', type: 'String', value: 'c', activityInstanceId: 'extTask1:inst' }
      ])
      const wrapper = createWrapper({ state: 'COMPLETED', activityInstanceHistory: historicActivities })
      wrapper.vm.filter = {
        deserializeValues: false,
        variableValues: [{ name: 'globalVar', operator: 'eq', value: 'a' }]
      }
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()

      expect(wrapper.vm.variables.map(v => v.name)).toEqual(['globalVar'])
    })

    it('resets the loading flags when a query fails, so a later reload is not blocked', async () => {
      HistoryService.fetchProcessInstanceVariablesHistory.mockRejectedValueOnce(new Error('boom'))
      const wrapper = createWrapper({ state: 'COMPLETED', activityInstanceHistory: historicActivities })

      await expect(wrapper.vm.fetchInstanceVariables('HistoryService', 'fetchProcessInstanceVariablesHistory'))
        .rejects.toThrow('boom')
      expect(wrapper.vm.fetching).toBe(false)
      expect(wrapper.vm.loading).toBe(false)

      // the failed attempt must not block the next one
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue(historyVariables())
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()
      expect(wrapper.vm.variables).toHaveLength(4)
    })
  })

  describe('per-variable liveness (isLive)', () => {

    it('marks variables of finished scopes as not live, others as live', async () => {
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue(historyVariables())
      const wrapper = createWrapper({ activityInstance: runtimeTree, activityInstanceHistory: historicActivities })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()

      const byName = Object.fromEntries(wrapper.vm.variables.map(v => [v.name, v]))
      expect(byName.globalVar.isLive).toBe(true)          // process-instance scope
      expect(byName.liveLocalVar.isLive).toBe(true)       // active activity scope
      expect(byName.transitionVar.isLive).toBe(true)      // async transition scope
      expect(byName.finishedScopeVar.isLive).toBe(false)  // finished activity scope
    })

    it('resolves scope names of finished scopes from the historic activity instances', async () => {
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue(historyVariables())
      const wrapper = createWrapper({ activityInstance: runtimeTree, activityInstanceHistory: historicActivities })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()

      const finished = wrapper.vm.variables.find(v => v.name === 'finishedScopeVar')
      expect(finished.scope).toBe('External Task 1')
      expect(finished.scopeActivityId).toBe('extTask1')
    })

    it('marks all variables of finished instances as not live', async () => {
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue(historyVariables())
      const wrapper = createWrapper({ state: 'COMPLETED', activityInstanceHistory: historicActivities })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()
      expect(wrapper.vm.variables.every(v => v.isLive === false)).toBe(true)
    })

    it('treats all variables as live when no runtime tree is available (e.g. suspended instances)', async () => {
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue(historyVariables())
      const wrapper = createWrapper({ state: 'SUSPENDED' })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()
      expect(wrapper.vm.variables.every(v => v.isLive === true)).toBe(true)
    })

    it('marks runtime-sourced variables as live without needing the tree', async () => {
      ProcessService.fetchProcessInstanceVariables.mockResolvedValue([
        { id: 'v1', name: 'globalVar', type: 'String', value: 'a', activityInstanceId: 'pi1', executionId: 'pi1' }
      ])
      const wrapper = createWrapper({ historyLevel: 'none' })
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()
      expect(wrapper.vm.variables[0].isLive).toBe(true)
    })
  })

  describe('re-annotation when scope maps arrive after the variables', () => {

    it('re-annotates loaded variables without a second fetch', async () => {
      HistoryService.fetchProcessInstanceVariablesHistory.mockResolvedValue(historyVariables())
      const wrapper = createWrapper() // no runtime tree yet
      wrapper.vm.loadSelectedInstanceVariables()
      await flushPromises()
      // fallback while the tree is missing: everything live, raw scope ids
      expect(wrapper.vm.variables.find(v => v.name === 'finishedScopeVar').isLive).toBe(true)

      await wrapper.setProps({ activityInstance: runtimeTree, activityInstanceHistory: historicActivities })
      await flushPromises()

      expect(HistoryService.fetchProcessInstanceVariablesHistory).toHaveBeenCalledTimes(1)
      const finished = wrapper.vm.variables.find(v => v.name === 'finishedScopeVar')
      expect(finished.isLive).toBe(false)
      expect(finished.scope).toBe('External Task 1')
    })
  })

  describe('downloadFile routing', () => {

    it('downloads live file variables via the runtime endpoint', async () => {
      const wrapper = createWrapper()
      wrapper.vm.downloadFile({ id: 'v9', name: 'doc', type: 'File', isLive: true, executionId: 'ex9', valueInfo: { filename: 'doc.txt' } })
      await flushPromises()
      expect(ProcessService.fetchVariableDataByExecutionId).toHaveBeenCalledWith('ex9', 'doc')
      expect(HistoryService.fetchHistoryVariableDataById).not.toHaveBeenCalled()
    })

    it('downloads finished-scope file variables via the history endpoint', async () => {
      const wrapper = createWrapper()
      wrapper.vm.downloadFile({ id: 'v9', name: 'doc', type: 'File', isLive: false, executionId: 'ex9', valueInfo: { filename: 'doc.txt' } })
      await flushPromises()
      expect(HistoryService.fetchHistoryVariableDataById).toHaveBeenCalledWith('v9')
      expect(ProcessService.fetchVariableDataByExecutionId).not.toHaveBeenCalled()
    })
  })
})
