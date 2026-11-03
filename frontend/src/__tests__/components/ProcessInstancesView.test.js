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
import ProcessInstancesView from '@/components/process/ProcessInstancesView.vue'
import { ProcessService } from '@/services.js'

vi.mock('@/services.js', async (importOriginal) => ({
  ...(await importOriginal()),
  ProcessService: { findProcessStatistics: vi.fn() }
}))

describe('ProcessInstancesView', () => {
  describe('syncStatisticsWithInstances', () => {
    const viewContext = (process) => {
      const ctx = {
        process,
        loadHistoricActivityStatisticsForInstances: vi.fn(),
        setStatistics: vi.fn()
      }
      ctx.reloadRuntimeStatistics = ProcessInstancesView.methods.reloadRuntimeStatistics.bind(ctx)
      return ctx
    }
    const sync = (ctx, filter) => ProcessInstancesView.methods.syncStatisticsWithInstances.call(ctx, filter)

    beforeEach(() => {
      ProcessService.findProcessStatistics.mockReset()
    })

    // The diagram reads running tokens from runtime, so they must not go stale while history refreshes
    it('reloads the runtime statistics together with the filtered history', async () => {
      const process = { id: 'pd-1' }
      const statistics = [{ id: 'Fail', instances: 3 }]
      ProcessService.findProcessStatistics.mockResolvedValue(statistics)
      const ctx = viewContext(process)

      sync(ctx, { unfinished: true })
      await vi.waitFor(() => expect(ctx.setStatistics).toHaveBeenCalled())

      expect(ctx.loadHistoricActivityStatisticsForInstances).toHaveBeenCalledWith({ processDefinitionId: 'pd-1', filter: { unfinished: true } })
      expect(ProcessService.findProcessStatistics).toHaveBeenCalledWith('pd-1')
      expect(ctx.setStatistics).toHaveBeenCalledWith({ process, statistics })
    })

    it('stores the statistics on the process they were requested for', async () => {
      const first = { id: 'pd-1' }
      let resolve
      ProcessService.findProcessStatistics.mockReturnValue(new Promise(r => { resolve = r }))
      const ctx = viewContext(first)

      sync(ctx, {})
      ctx.process = { id: 'pd-2' }
      resolve([])
      await vi.waitFor(() => expect(ctx.setStatistics).toHaveBeenCalled())

      expect(ctx.setStatistics).toHaveBeenCalledWith({ process: first, statistics: [] })
    })

    it('loads nothing without a process', () => {
      const ctx = viewContext(null)

      sync(ctx, {})

      expect(ctx.loadHistoricActivityStatisticsForInstances).not.toHaveBeenCalled()
      expect(ProcessService.findProcessStatistics).not.toHaveBeenCalled()
    })
  })

  describe('matchedDeepLink', () => {
    it('returns the deep link entry matching the active tab', () => {
      const context = {
        activeTab: 'myExternalLinkId',
        $root: { config: { deepLinks: { processDefinition: [{ id: 'myExternalLinkId', url: 'https://external.example', type: 'tab' }] } } }
      }
      expect(ProcessInstancesView.computed.matchedDeepLink.call(context)).toEqual({ id: 'myExternalLinkId', url: 'https://external.example', type: 'tab', text: 'deepLinks.processDefinition.myExternalLinkId.title' })
    })

    it('returns undefined when the active tab is a built-in tab', () => {
      const context = { activeTab: 'instances', $root: { config: {} } }
      expect(ProcessInstancesView.computed.matchedDeepLink.call(context)).toBeUndefined()
    })

    it('returns undefined when the matching entry is a button-type link', () => {
      const context = {
        activeTab: 'myButtonLinkId',
        $root: { config: { deepLinks: { processDefinition: [{ id: 'myButtonLinkId', url: 'https://external.example', type: 'button' }] } } }
      }
      expect(ProcessInstancesView.computed.matchedDeepLink.call(context)).toBeUndefined()
    })
  })

  describe('isUnfiltered', () => {
    const unfiltered = (computedFilter) => ProcessInstancesView.computed.isUnfiltered.call({ computedFilter })

    it.each([
      ['an empty filter', {}],
      ['only the unfinished switch on', { unfinished: true }],
      ['the unfinished switch off', { unfinished: undefined }],
      ['empty values', { editField: '', processInstanceIds: [], businessKey: null }]
    ])('is unfiltered with %s', (_, filter) => {
      expect(unfiltered(filter)).toBe(true)
    })

    // Every instance with a token in the selected activity is listed in these modes
    it.each([
      ['active', { unfinished: true, activityIdIn: ['Fail'] }],
      ['activeOrExecuted', { unfinished: true, activeOrExecutedActivityIdIn: ['Fail'] }]
    ])('is unfiltered with a selected activity in %s mode', (_, filter) => {
      expect(unfiltered(filter)).toBe(true)
    })

    it.each([
      ['a text search', { unfinished: true, editField: 'abc' }],
      ['another criterion', { withIncidents: true }],
      // Tokens still waiting in the activity belong to instances that have not executed it yet
      ['a selected activity in executed mode', { executedActivityIdIn: ['Fail'] }],
      ['a selected activity and a text search', { activityIdIn: ['Fail'], editField: 'abc' }]
    ])('is filtered with %s', (_, filter) => {
      expect(unfiltered(filter)).toBe(false)
    })
  })

  describe('useRuntimeStats and runtimeActivityId', () => {
    const props = (isUnfiltered, selectedActivityId) => {
      const context = { isUnfiltered, selectedActivityId }
      return {
        useRuntimeStats: ProcessInstancesView.computed.useRuntimeStats.call(context),
        runtimeActivityId: ProcessInstancesView.computed.runtimeActivityId.call(context)
      }
    }

    it('uses runtime for every activity without filter', () => {
      expect(props(true, null)).toEqual({ useRuntimeStats: true, runtimeActivityId: null })
    })

    it('uses runtime only for the selected activity when it is the only filter', () => {
      expect(props(true, 'Fail')).toEqual({ useRuntimeStats: false, runtimeActivityId: 'Fail' })
    })

    it('uses no runtime when other criteria narrow the list', () => {
      expect(props(false, 'Fail')).toEqual({ useRuntimeStats: false, runtimeActivityId: null })
    })
  })

  describe('matchedDeepLinkParams', () => {
    it('builds process definition context and language params', () => {
      const context = {
        process: { id: 'def-1', key: 'myProcess', version: '3', versionTag: 'v3', tenantId: 'definition-tenant' },
        currentLanguage: () => 'en'
      }
      expect(ProcessInstancesView.computed.matchedDeepLinkParams.call(context)).toEqual({
        processDefinitionId: 'def-1',
        processDefinitionKey: 'myProcess',
        processDefinitionVersion: '3',
        processDefinitionVersionTag: 'v3',
        processDefinitionTenantId: 'definition-tenant',
        lang: 'en'
      })
    })
  })
})
