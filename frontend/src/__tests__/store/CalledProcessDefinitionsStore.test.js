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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import CalledProcessDefinitionsStore from '../../store/CalledProcessDefinitionsStore.js'
import { ProcessService, HistoryService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  ProcessService: {
    findCalledProcessDefinitions: vi.fn()
  },
  HistoryService: {
    findActivitiesProcessDefinitionHistory: vi.fn(),
    findProcessesInstancesHistory: vi.fn()
  }
}))

/** A statically referenced call activity, as returned by the static-call endpoint. */
const staticDef = (overrides = {}) => ({
  id: 'sub-1',
  key: 'sub',
  version: 1,
  name: 'Sub process',
  tenantId: null,
  calledFromActivityIds: ['call_1'],
  ...overrides
})

/**
 * Wire up the action context the way the component does: the static definitions come from
 * this module's own getter, while the activity statistics and the activity-id-to-name map
 * come from the root store.
 */
function contextWith(getContext, { statics = [], historicStats = [], activityNames = {} } = {}) {
  const context = getContext()
  context.getters.getStaticCalledProcessDefinitionsFor = () => statics
  context.rootGetters.getHistoricActivityStatistics = () => historicStats
  context.rootGetters.getProcessActivities = activityNames
  return context
}

/** The payload of the last `setCalledProcessDefinitions` commit. */
function lastGrouping(context) {
  const calls = context.commit.mock.calls.filter(([name]) => name === 'setCalledProcessDefinitions')
  return calls.at(-1)[1]
}

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('CalledProcessDefinitionsStore', CalledProcessDefinitionsStore, {
  initialState: (getState) => {
    it('should start with empty lists and unknown static definitions', () => {
      expect(getState()).toEqual({
        calledProcessDefinitions: [],
        allCalledProcessDefinitions: [],
        staticCalledProcessDefinitions: null,
        staticCalledProcessDefinitionsId: null
      })
    })
  },

  getters: {
    calledProcessDefinitions: (getter, getState) => {
      it('should expose the displayed list', () => {
        const state = getState()
        state.calledProcessDefinitions = [{ id: 'a' }]
        expect(getter(state)).toEqual([{ id: 'a' }])
      })
    },

    getStaticCalledProcessDefinitions: (getter, getState) => {
      it('should expose the static definitions', () => {
        const state = getState()
        state.staticCalledProcessDefinitions = [staticDef()]
        expect(getter(state)).toEqual([staticDef()])
      })

      it('should keep returning a list while they are unknown', () => {
        expect(getter(getState())).toEqual([])
      })
    },

    getStaticCalledProcessDefinitionsFor: (getter, getState) => {
      const loadedFor = (state, processDefinitionId) => {
        state.staticCalledProcessDefinitions = [staticDef()]
        state.staticCalledProcessDefinitionsId = processDefinitionId
        return state
      }

      it('should expose the static definitions of the definition they were loaded for', () => {
        expect(getter(loadedFor(getState(), 'pd-1'))('pd-1')).toEqual([staticDef()])
      })

      // The store is global: after switching definitions it may still hold the previous one's data
      it('should hide the static definitions of another definition', () => {
        expect(getter(loadedFor(getState(), 'pd-other'))('pd-1')).toBeNull()
      })

      // Loading or failed: an empty list would wrongly mean "calls nothing static"
      it('should report them as unknown until they have loaded', () => {
        const state = loadedFor(getState(), 'pd-1')
        state.staticCalledProcessDefinitions = null
        expect(getter(state)('pd-1')).toBeNull()
      })

      it('should return nothing without a definition id', () => {
        expect(getter(loadedFor(getState(), 'pd-1'))(undefined)).toBeNull()
      })
    },

    mapSelectedActivity: (getter) => {
      it('should narrow a definition down to the selected activity', () => {
        const definition = {
          id: 'sub-1',
          activities: [{ activityId: 'call_1' }, { activityId: 'call_2' }]
        }

        expect(getter()(definition, 'call_2')).toEqual({
          id: 'sub-1',
          activities: [{ activityId: 'call_2' }]
        })
      })
    },

    getCalledProcessState: (getter) => {
      const state = (activities) => getter()({ activities })

      it('should report running when a dynamic activity has an unfinished instance', () => {
        expect(state([{ isStatic: false, instances: [{ ended: false }] }]))
          .toBe('process-instance.calledProcessDefinitions.running')
      })

      it('should report referenced when only static activities are present', () => {
        expect(state([{ isStatic: true, instances: [] }]))
          .toBe('process-instance.calledProcessDefinitions.referenced')
      })

      it('should report both when a definition is statically referenced and running', () => {
        expect(state([
          { isStatic: true, instances: [] },
          { isStatic: false, instances: [{ ended: false }] }
        ])).toBe('process-instance.calledProcessDefinitions.runningAndReferenced')
      })

      it('should report unknown when every dynamic instance has ended', () => {
        expect(state([{ isStatic: false, instances: [{ ended: true }] }]))
          .toBe('process-instance.calledProcessDefinitions.unknown')
      })

      it('should report unknown for a definition with no activities', () => {
        expect(state([])).toBe('process-instance.calledProcessDefinitions.unknown')
      })
    }
  },

  actions: {
    loadStaticCalledProcessDefinitions: (action, getContext) => {
      it('should fetch and store the static definitions', async () => {
        const context = getContext()
        context.commit.mockImplementation((name, value) => CalledProcessDefinitionsStore.mutations[name](context.state, value))
        ProcessService.findCalledProcessDefinitions.mockResolvedValue([staticDef()])

        await action(context, { processDefinitionId: 'pd-1' })

        expect(ProcessService.findCalledProcessDefinitions).toHaveBeenCalledWith('pd-1')
        expect(context.commit).toHaveBeenCalledWith('setStaticCalledProcessDefinitions', [staticDef()])
      })

      it('should drop the data of another definition before loading', async () => {
        const context = getContext()
        context.state.staticCalledProcessDefinitionsId = 'pd-other'
        context.commit.mockImplementation((name, value) => CalledProcessDefinitionsStore.mutations[name](context.state, value))
        ProcessService.findCalledProcessDefinitions.mockResolvedValue([staticDef()])

        await action(context, { processDefinitionId: 'pd-1' })

        // null before the id, so the old list is never read under the new id
        expect(context.commit.mock.calls).toEqual([
          ['setStaticCalledProcessDefinitions', null],
          ['setStaticCalledProcessDefinitionsId', 'pd-1'],
          ['setStaticCalledProcessDefinitions', [staticDef()]]
        ])
        expect(context.state.staticCalledProcessDefinitionsId).toBe('pd-1')
      })

      it('should keep the data while the same definition reloads', async () => {
        const context = getContext()
        context.state.staticCalledProcessDefinitionsId = 'pd-1'
        ProcessService.findCalledProcessDefinitions.mockResolvedValue([staticDef()])

        await action(context, { processDefinitionId: 'pd-1' })

        expect(context.commit.mock.calls).toEqual([['setStaticCalledProcessDefinitions', [staticDef()]]])
      })

      // e.g. the instance view's mount and watcher, or the definition view's mount and watcher
      it('should share one request between loads of the same definition', async () => {
        const context = getContext()
        context.commit.mockImplementation((name, value) => CalledProcessDefinitionsStore.mutations[name](context.state, value))
        ProcessService.findCalledProcessDefinitions.mockResolvedValue([staticDef()])

        await Promise.all([
          action(context, { processDefinitionId: 'pd-1' }),
          action(context, { processDefinitionId: 'pd-1' })
        ])
        await action(context, { processDefinitionId: 'pd-1' })

        expect(ProcessService.findCalledProcessDefinitions).toHaveBeenCalledTimes(2)
      })

      // Each store has its own state; a load in flight in one must not answer for another
      it('should not share a request between stores', async () => {
        const first = getContext()
        const second = { ...getContext(), commit: vi.fn(), state: structuredClone(CalledProcessDefinitionsStore.state) }
        ProcessService.findCalledProcessDefinitions.mockResolvedValue([staticDef()])

        await Promise.all([
          action(first, { processDefinitionId: 'pd-1' }),
          action(second, { processDefinitionId: 'pd-1' })
        ])

        expect(ProcessService.findCalledProcessDefinitions).toHaveBeenCalledTimes(2)
      })

      // Only the links depend on it, so callers (e.g. the definition view switching its diagram) must not fail
      it('should leave the definitions unknown and not reject when the request fails', async () => {
        const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
        const context = getContext()
        context.commit.mockImplementation((name, value) => CalledProcessDefinitionsStore.mutations[name](context.state, value))
        ProcessService.findCalledProcessDefinitions.mockRejectedValueOnce(new Error('forbidden'))

        await expect(action(context, { processDefinitionId: 'pd-1' })).resolves.toBeUndefined()

        expect(consoleError).toHaveBeenCalled()
        expect(context.state.staticCalledProcessDefinitions).toBeNull()
        ProcessService.findCalledProcessDefinitions.mockResolvedValueOnce([staticDef()])
        await action(context, { processDefinitionId: 'pd-1' })
        expect(context.state.staticCalledProcessDefinitions).toEqual([staticDef()])
        consoleError.mockRestore()
      })

      it('should ignore an answer that arrives after another definition was requested', async () => {
        const context = getContext()
        context.commit.mockImplementation((name, value) => CalledProcessDefinitionsStore.mutations[name](context.state, value))
        let resolveFirst
        ProcessService.findCalledProcessDefinitions
          .mockReturnValueOnce(new Promise(resolve => { resolveFirst = resolve }))
          .mockResolvedValueOnce([staticDef({ id: 'sub-2' })])

        const first = action(context, { processDefinitionId: 'pd-1' })
        await action(context, { processDefinitionId: 'pd-2' })
        resolveFirst([staticDef()])
        await first

        expect(context.state.staticCalledProcessDefinitionsId).toBe('pd-2')
        expect(context.state.staticCalledProcessDefinitions).toEqual([staticDef({ id: 'sub-2' })])
      })
    },

    filterByActivity: (action, getContext) => {
      it('should keep only definitions touching the activity, narrowed to it', () => {
        const context = getContext()
        context.state.allCalledProcessDefinitions = [
          { id: 'a', activities: [{ activityId: 'call_1' }, { activityId: 'call_2' }] },
          { id: 'b', activities: [{ activityId: 'call_3' }] }
        ]
        context.getters.mapSelectedActivity = CalledProcessDefinitionsStore.getters.mapSelectedActivity()

        action(context, 'call_1')

        expect(context.commit).toHaveBeenCalledWith('setCalledProcessDefinitions', [
          { id: 'a', activities: [{ activityId: 'call_1' }] }
        ])
      })

      it('should restore the full list when the selection is cleared', () => {
        const context = getContext()
        context.state.allCalledProcessDefinitions = [{ id: 'a', activities: [] }]

        action(context, null)

        expect(context.commit).toHaveBeenCalledWith('setCalledProcessDefinitions', [{ id: 'a', activities: [] }])
      })
    },

    loadCalledProcessDefinitions: (action, getContext) => {
      // e.g. the table loading right after a definition switch, before the definition view's load finished
      it('should wait for the static definitions when they are not loaded yet', async () => {
        const context = contextWith(getContext)
        let loaded = null
        context.getters.getStaticCalledProcessDefinitionsFor = () => loaded
        context.dispatch.mockImplementation(async () => { loaded = [staticDef()] })

        await action(context, { processId: 'pd-1' })

        expect(context.dispatch).toHaveBeenCalledWith('loadStaticCalledProcessDefinitions', { processDefinitionId: 'pd-1' })
        expect(lastGrouping(context).map(def => def.definitionId)).toEqual(['sub-1'])
      })

      // Shown as an error by the table instead of a table that silently lacks the static rows
      it('should fail when the static definitions could not be loaded', async () => {
        const context = contextWith(getContext)
        context.getters.getStaticCalledProcessDefinitionsFor = () => null

        await expect(action(context, { processId: 'pd-1' })).rejects.toThrow('could not be loaded')

        expect(context.commit.mock.calls.filter(([name]) => name === 'setCalledProcessDefinitions')).toEqual([['setCalledProcessDefinitions', []]])
      })

      // Switching definitions while the table loads: the slower, older load must not fill the table
      it('should drop an older load that finishes after a newer one started', async () => {
        const first = contextWith(getContext, {
          historicStats: [{ id: 'call_dyn', instances: 1 }],
          activityNames: { call_dyn: 'Dynamic call' }
        })
        let finishFirst
        HistoryService.findActivitiesProcessDefinitionHistory
          .mockReturnValueOnce(new Promise(resolve => { finishFirst = resolve }))
        HistoryService.findProcessesInstancesHistory.mockResolvedValue([])

        const older = action(first, { processId: 'pd-old' })
        const newer = { ...first, commit: vi.fn() }
        newer.getters.getStaticCalledProcessDefinitionsFor = () => [staticDef()]
        newer.rootGetters = { ...first.rootGetters, getHistoricActivityStatistics: () => [] }
        await action(newer, { processId: 'pd-new' })
        const commitsBefore = first.commit.mock.calls.length
        finishFirst([{ activityId: 'call_dyn', calledProcessInstanceId: 'x' }])
        await older

        expect(first.commit.mock.calls.length).toBe(commitsBefore)
        expect(lastGrouping(newer).map(def => def.definitionId)).toEqual(['sub-1'])
      })

      it('should only use the static definitions loaded for this process definition', async () => {
        const context = contextWith(getContext)
        const forDefinition = vi.fn(() => [staticDef()])
        context.getters.getStaticCalledProcessDefinitionsFor = forDefinition

        await action(context, { processId: 'pd-1' })

        expect(forDefinition).toHaveBeenCalledWith('pd-1')
        expect(lastGrouping(context).map(def => def.definitionId)).toEqual(['sub-1'])
      })

      it('should clear both lists before rebuilding them', async () => {
        const context = contextWith(getContext)

        await action(context, { processId: null })

        expect(context.commit).toHaveBeenNthCalledWith(1, 'setCalledProcessDefinitions', [])
        expect(context.commit).toHaveBeenNthCalledWith(2, 'setAllCalledProcessDefinitions', [])
      })

      it('should group a static definition and resolve its activity name', async () => {
        const context = contextWith(getContext, {
          statics: [staticDef()],
          historicStats: [{ id: 'call_1', instances: 0 }],
          activityNames: { call_1: 'Call sub process' }
        })

        await action(context, { processId: null })

        expect(lastGrouping(context)).toEqual([expect.objectContaining({
          definitionKey: 'sub',
          version: '1',
          isStatic: true,
          isRunning: false,
          label: 'sub',
          activities: [expect.objectContaining({
            activityId: 'call_1',
            activityName: 'Call sub process',
            isStatic: true,
            instances: [{ processInstanceId: null, ended: true }]
          })]
        })])
      })

      it('should fall back to the activity id when no name is known', async () => {
        const context = contextWith(getContext, { statics: [staticDef()] })

        await action(context, { processId: null })

        expect(lastGrouping(context)[0].activities[0].activityName).toBe('call_1')
      })

      // A statistics entry with a non-zero instance count means the call activity is still
      // running, which the grouping records as `ended: false`.
      it('should mark a static activity as not ended while instances remain', async () => {
        const context = contextWith(getContext, {
          statics: [staticDef()],
          historicStats: [{ id: 'call_1', instances: 2 }]
        })

        await action(context, { processId: null })

        expect(lastGrouping(context)[0].activities[0].instances).toEqual([{ processInstanceId: null, ended: false }])
      })

      it('should collapse repeated activity ids into one activity entry', async () => {
        const context = contextWith(getContext, {
          statics: [staticDef({ calledFromActivityIds: ['call_1', 'call_1'] })]
        })

        await action(context, { processId: null })

        const activities = lastGrouping(context)[0].activities
        expect(activities).toHaveLength(1)
        expect(activities[0].instances).toHaveLength(2)
      })

      it('should tolerate a static definition without calledFromActivityIds', async () => {
        const context = contextWith(getContext, {
          statics: [staticDef({ calledFromActivityIds: undefined })]
        })

        await action(context, { processId: null })

        expect(lastGrouping(context)).toEqual([expect.objectContaining({ definitionKey: 'sub', activities: [] })])
      })

      // Intermediate commits must carry final labels and must not share objects with later commits,
      // otherwise later in-loop mutations would reach state that was already committed.
      it('should commit detached snapshots with final labels for every chunk', async () => {
        const context = contextWith(getContext, {
          statics: [],
          historicStats: [{ id: 'call_dyn', instances: 1 }],
          activityNames: { call_dyn: 'Dynamic call' }
        })
        HistoryService.findActivitiesProcessDefinitionHistory.mockResolvedValue([
          { calledProcessInstanceId: 'pi-a', activityId: 'call_dyn', endTime: null },
          { calledProcessInstanceId: 'pi-b', activityId: 'call_dyn', endTime: null }
        ])
        const info = (id) => ({
          id,
          processDefinitionId: 'dyn-1',
          processDefinitionKey: 'dyn',
          processDefinitionVersion: 1,
          processDefinitionName: 'Dynamic',
          tenantId: null
        })
        HistoryService.findProcessesInstancesHistory
          .mockResolvedValueOnce([info('pi-a')])
          .mockResolvedValueOnce([info('pi-b')])

        await action(context, { processId: 'pi-1', chunkSize: 1 })

        const commits = context.commit.mock.calls
          .filter(([name]) => name === 'setCalledProcessDefinitions')
          .map(([, payload]) => payload)
          .filter(payload => payload.length > 0)
        expect(commits).toHaveLength(2)
        const [afterSecondChunkStart, final] = commits
        expect(afterSecondChunkStart[0].label).toBe('dyn')
        expect(afterSecondChunkStart[0]).not.toBe(final[0])
        expect(afterSecondChunkStart[0].activities[0]).not.toBe(final[0].activities[0])
        expect(afterSecondChunkStart[0].activities[0].instances).toHaveLength(2)
        expect(final[0].activities[0].instances).toHaveLength(2)
      })

      // Two versions of the same key must stay distinguishable in the UI, so the label
      // keeps the version suffix; a single version shows the bare key.
      it('should disambiguate labels only when a key has several versions', async () => {
        const context = contextWith(getContext, {
          statics: [
            staticDef({ id: 'sub-1', version: 1 }),
            staticDef({ id: 'sub-2', version: 2 })
          ]
        })

        await action(context, { processId: null })

        expect(lastGrouping(context).map(d => d.label).sort()).toEqual(['sub:1', 'sub:2'])
      })

      it('should not query history when there are no dynamic calls', async () => {
        const context = contextWith(getContext, {
          statics: [staticDef()],
          historicStats: [{ id: 'call_1', instances: 1 }],
          activityNames: { call_1: 'Call sub process' }
        })

        await action(context, { processId: 'pi-1' })

        expect(HistoryService.findActivitiesProcessDefinitionHistory).not.toHaveBeenCalled()
      })

      // A statistics entry that has a known activity name but is *not* among the static
      // call activities means the process called something at runtime.
      it('should load dynamic calls and group them by definition key and version', async () => {
        const context = contextWith(getContext, {
          statics: [],
          historicStats: [{ id: 'call_dyn', instances: 1 }],
          activityNames: { call_dyn: 'Dynamic call' }
        })
        HistoryService.findActivitiesProcessDefinitionHistory.mockResolvedValue([
          { calledProcessInstanceId: 'pi-child', activityId: 'call_dyn', activityName: 'Dynamic call', endTime: null }
        ])
        HistoryService.findProcessesInstancesHistory.mockResolvedValue([
          {
            id: 'pi-child',
            processDefinitionId: 'dyn-1',
            processDefinitionKey: 'dyn',
            processDefinitionVersion: 2,
            processDefinitionName: 'Dynamic',
            tenantId: null
          }
        ])

        await action(context, { processId: 'pi-1' })

        expect(HistoryService.findActivitiesProcessDefinitionHistory).toHaveBeenCalledWith('pi-1', {
          activityType: 'callActivity',
          unfinished: true
        })
        expect(HistoryService.findProcessesInstancesHistory).toHaveBeenCalledWith({ processInstanceIds: ['pi-child'] })
        expect(lastGrouping(context)).toEqual([expect.objectContaining({
          definitionKey: 'dyn',
          version: '2',
          isStatic: false,
          isRunning: true,
          label: 'dyn',
          activities: [expect.objectContaining({
            activityId: 'call_dyn',
            activityName: 'Dynamic call',
            isStatic: false,
            instances: [{ processInstanceId: 'pi-child', ended: false }]
          })]
        })])
      })

      it('should ignore call activities that have already finished', async () => {
        const context = contextWith(getContext, {
          historicStats: [{ id: 'call_dyn', instances: 1 }],
          activityNames: { call_dyn: 'Dynamic call' }
        })
        HistoryService.findActivitiesProcessDefinitionHistory.mockResolvedValue([
          { calledProcessInstanceId: 'pi-done', activityId: 'call_dyn', endTime: '2026-01-01T00:00:00Z' },
          { calledProcessInstanceId: null, activityId: 'call_dyn', endTime: null }
        ])

        await action(context, { processId: 'pi-1' })

        expect(HistoryService.findProcessesInstancesHistory).not.toHaveBeenCalled()
        expect(lastGrouping(context)).toEqual([])
      })

      // Instance ids are looked up in chunks to keep the query string bounded.
      it('should request instance details in chunks of the given size', async () => {
        const context = contextWith(getContext, {
          historicStats: [{ id: 'call_dyn', instances: 1 }],
          activityNames: { call_dyn: 'Dynamic call' }
        })
        HistoryService.findActivitiesProcessDefinitionHistory.mockResolvedValue(
          ['a', 'b', 'c'].map(id => ({ calledProcessInstanceId: id, activityId: 'call_dyn', endTime: null }))
        )
        HistoryService.findProcessesInstancesHistory.mockResolvedValue([])

        await action(context, { processId: 'pi-1', chunkSize: 2 })

        expect(HistoryService.findProcessesInstancesHistory).toHaveBeenCalledTimes(2)
        expect(HistoryService.findProcessesInstancesHistory).toHaveBeenNthCalledWith(1, { processInstanceIds: ['a', 'b'] })
        expect(HistoryService.findProcessesInstancesHistory).toHaveBeenNthCalledWith(2, { processInstanceIds: ['c'] })
      })

      it('should de-duplicate repeated called instance ids before querying', async () => {
        const context = contextWith(getContext, {
          historicStats: [{ id: 'call_dyn', instances: 1 }],
          activityNames: { call_dyn: 'Dynamic call' }
        })
        HistoryService.findActivitiesProcessDefinitionHistory.mockResolvedValue([
          { calledProcessInstanceId: 'pi-child', activityId: 'call_dyn', endTime: null },
          { calledProcessInstanceId: 'pi-child', activityId: 'call_dyn', endTime: null }
        ])
        HistoryService.findProcessesInstancesHistory.mockResolvedValue([])

        await action(context, { processId: 'pi-1' })

        expect(HistoryService.findProcessesInstancesHistory).toHaveBeenCalledWith({ processInstanceIds: ['pi-child'] })
      })

      // When the same key:version is both statically referenced and running, the grouping
      // keeps a single entry carrying both flags (rather than emitting a duplicate row),
      // and its activities cover the static and the dynamic call site. That combination is
      // what makes `getCalledProcessState` report `runningAndReferenced`.
      it('should merge a static and a running definition into one dual-flagged entry', async () => {
        const context = contextWith(getContext, {
          statics: [staticDef({ key: 'dyn', version: 2, calledFromActivityIds: ['call_static'] })],
          historicStats: [{ id: 'call_dyn', instances: 1 }, { id: 'call_static', instances: 0 }],
          activityNames: { call_dyn: 'Dynamic call', call_static: 'Static call' }
        })
        HistoryService.findActivitiesProcessDefinitionHistory.mockResolvedValue([
          { calledProcessInstanceId: 'pi-child', activityId: 'call_dyn', endTime: null }
        ])
        HistoryService.findProcessesInstancesHistory.mockResolvedValue([
          {
            id: 'pi-child',
            processDefinitionId: 'dyn-1',
            processDefinitionKey: 'dyn',
            processDefinitionVersion: 2,
            processDefinitionName: 'Dynamic',
            tenantId: null
          }
        ])

        await action(context, { processId: 'pi-1' })

        const grouping = lastGrouping(context)
        expect(grouping).toHaveLength(1)
        expect(grouping[0]).toEqual(expect.objectContaining({
          definitionKey: 'dyn',
          version: '2',
          isStatic: true,
          isRunning: true
        }))
        expect(grouping[0].activities.map(a => a.activityId).sort()).toEqual(['call_dyn', 'call_static'])
        expect(CalledProcessDefinitionsStore.getters.getCalledProcessState()(grouping[0]))
          .toBe('process-instance.calledProcessDefinitions.runningAndReferenced')
      })

      it('should propagate history service failures', async () => {
        const context = contextWith(getContext, {
          historicStats: [{ id: 'call_dyn', instances: 1 }],
          activityNames: { call_dyn: 'Dynamic call' }
        })
        HistoryService.findActivitiesProcessDefinitionHistory.mockRejectedValue(new Error('Service error'))

        await expect(action(context, { processId: 'pi-1' })).rejects.toThrow('Service error')
      })

      it('should tolerate missing statistics and activity maps', async () => {
        const context = getContext()
        context.getters.getStaticCalledProcessDefinitionsFor = () => null
        context.rootGetters.getHistoricActivityStatistics = () => null
        context.rootGetters.getProcessActivities = null

        await action(context, { processId: null })

        expect(lastGrouping(context)).toEqual([])
      })
    }
  },

  additional: (storeModule, getState) => {
    describe('list-replacing mutations', () => {
      // Each of these mutations does nothing but replace the identically-shaped field, so
      // a table covers all three without repeating the same lines per mutation.
      const cases = [
        ['setCalledProcessDefinitions', 'calledProcessDefinitions', [{ id: 'new' }]],
        ['setAllCalledProcessDefinitions', 'allCalledProcessDefinitions', [{ id: 'new' }]],
        ['setStaticCalledProcessDefinitions', 'staticCalledProcessDefinitions', [staticDef()]],
        ['setStaticCalledProcessDefinitionsId', 'staticCalledProcessDefinitionsId', 'pd-1']
      ]

      it.each(cases)('%s should replace state.%s', (mutationName, field, value) => {
        const state = getState()
        storeModule.mutations[mutationName](state, value)
        expect(state[field]).toEqual(value)
      })
    })
  }
})
