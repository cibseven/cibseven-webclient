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
import { flushPromises } from '@vue/test-utils'
import { callWith } from '../support/callWithContext.js'
import { mountWithDefaults } from '../support/mountWithDefaults.js'
import { ProcessService, HistoryService } from '@/services.js'

vi.mock('@/services.js', () => ({
  ProcessService: { findDeployment: vi.fn(), updateHistoryTimeToLive: vi.fn() },
  HistoryService: { findProcessesInstancesHistory: vi.fn() }
}))

const ProcessDefinitionDetails = (await import('@/components/process/ProcessDefinitionDetails.vue')).default

beforeEach(() => {
  vi.clearAllMocks()
})

describe('ProcessDefinitionDetails.vue', () => {
  describe('ttlDescriptionItems', () => {
    it('describes the definition name and version as title/value pairs', () => {
      const { result } = callWith(ProcessDefinitionDetails.computed.ttlDescriptionItems, {
        version: { name: 'Invoice', version: 2 }
      })
      expect(result).toEqual([
        { title: 'process.details.definitionName', value: 'Invoice' },
        { title: 'process.details.definitionVersion', value: 2 }
      ])
    })
  })

  describe('historyTimeToLiveDisplay', () => {
    // The labels themselves are covered in utils/historyTimeToLive.test.js; this only checks
    // that the definition's own TTL is passed to the shared helper.
    it.each([
      ['an explicit 0', 0, 'historyTimeToLive.choiceZero'],
      ['a positive number', 30, '30 historyTimeToLive.days'],
      ['no TTL (null)', null, 'historyTimeToLive.choiceUnlimited'],
      ['no TTL (undefined)', undefined, 'historyTimeToLive.choiceUnlimited']
    ])('renders %s through the shared helper', (_, historyTimeToLive, expected) => {
      const { result } = callWith(ProcessDefinitionDetails.computed.historyTimeToLiveDisplay, { historyTimeToLive })
      expect(result).toBe(expected)
    })

    // A definition without a TTL keeps its history forever, whatever the engine default is:
    // the default is only applied to models at deployment time, never to an existing definition.
    it.each([
      ['an engine default of 180 days', { historyTimeToLive: 180, enforceHistoryTimeToLive: false }],
      ['an engine default of 0', { historyTimeToLive: 0, enforceHistoryTimeToLive: true }],
      ['no engine default', { historyTimeToLive: -1, enforceHistoryTimeToLive: false }],
      ['an engine that does not report it', {}]
    ])('shows a definition without TTL as unlimited with %s', (_, config) => {
      const { result } = callWith(ProcessDefinitionDetails.computed.historyTimeToLiveDisplay, {
        historyTimeToLive: null,
        $root: { config }
      })
      expect(result).toBe('historyTimeToLive.choiceUnlimited')
    })
  })

  describe('editHistoryTimeToLive', () => {
    it('opens the shared modal with the current TTL', () => {
      const show = vi.fn()
      callWith(ProcessDefinitionDetails.methods.editHistoryTimeToLive, {
        historyTimeToLive: 30,
        $refs: { ttlModal: { show } }
      })

      expect(show).toHaveBeenCalledWith({ historyTimeToLive: 30 })
    })
  })

  describe('onTtlUpdated', () => {
    it('saves the new TTL and emits the legacy onUpdateHistoryTimeToLive event', async () => {
      ProcessService.updateHistoryTimeToLive.mockResolvedValue({})
      const { context } = callWith(ProcessDefinitionDetails.methods.onTtlUpdated,
        { version: { id: 'def-1' }, historyTimeToLive: 30 },
        { ttlValue: null }
      )
      await Promise.resolve()
      await Promise.resolve()

      expect(ProcessService.updateHistoryTimeToLive).toHaveBeenCalledWith('def-1', { historyTimeToLive: null })
      expect(context.historyTimeToLive).toBeNull()
      expect(context.$emit).toHaveBeenCalledWith('onUpdateHistoryTimeToLive', 'def-1', null)
    })

    it('forwards a rejected save to the shared error banner', async () => {
      const error = { response: { data: { message: 'denied' } } }
      ProcessService.updateHistoryTimeToLive.mockRejectedValue(error)
      const { context } = callWith(ProcessDefinitionDetails.methods.onTtlUpdated,
        { version: { id: 'def-1' }, historyTimeToLive: 30 },
        { ttlValue: 0 }
      )
      await Promise.resolve()
      await Promise.resolve()

      expect(context.$root.$refs.error.show).toHaveBeenCalledWith(error.response.data)
      // The optimistic value must not stick when the save actually failed.
      expect(context.historyTimeToLive).toBe(30)
    })
  })
})

const version = { id: 'invoice:37:abc', key: 'invoice', name: 'Invoice', version: '37', deploymentId: 'dep-37', historyTimeToLive: 10 }

function mountDetails(props = {}, historyLevel = 'full') {
  return mountWithDefaults(ProcessDefinitionDetails, {
    props: { version, versionIndex: '37', ...props },
    i18nPlugin: false,
    global: {
      stubs: { SuccessAlert: true, EditHistoryTimeToLiveModal: true },
      mocks: { config: { camundaHistoryLevel: historyLevel } }
    }
  })
}

// first call: oldest start (asc), second call: newest start (desc)
function startTimes(first, last) {
  HistoryService.findProcessesInstancesHistory.mockImplementation(async query =>
    query.sorting[0].sortOrder === 'asc' ? [{ startTime: first }] : [{ startTime: last }])
}

describe('ProcessDefinitionDetails.vue mounted', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('links the deployment it receives, without loading it itself', async () => {
    const wrapper = mountDetails({ selectedDeployment: { id: 'dep-37', deploymentTime: '2025-05-16T12:44:00.000+0200' } })
    await flushPromises()

    expect(ProcessService.findDeployment).not.toHaveBeenCalled()
    expect(wrapper.find('a').text()).toBe('dep-37')
  })

  it('shows the deployment id as plain text when it receives no deployment', async () => {
    const wrapper = mountDetails()
    await flushPromises()

    expect(ProcessService.findDeployment).not.toHaveBeenCalled()
    expect(wrapper.find('a').exists()).toBe(false)
    expect(wrapper.text()).toContain('dep-37')
  })

  // the version card already shows these
  it('leaves out the version tag, deployment time and running and completed counts', async () => {
    const wrapper = mountDetails({ selectedDeployment: { id: 'dep-37', deploymentTime: '2025-05-16T12:44:00.000+0200' } })
    await flushPromises()

    expect(wrapper.text()).not.toContain('process.details.versionTag')
    expect(wrapper.text()).not.toContain('deployment.deploymentTime')
    expect(wrapper.text()).not.toContain('process.details.unfinishedInstances')
    expect(wrapper.text()).not.toContain('process.details.finishedInstances')
    expect(wrapper.text()).toContain('process.details.totalInstances')
  })

  it('loads the first and last start right away when mounted already shown', async () => {
    startTimes('2025-01-01T10:00:00.000+0100', '2025-06-01T10:00:00.000+0200')
    const wrapper = mountDetails({ shown: true })
    await flushPromises()

    expect(HistoryService.findProcessesInstancesHistory).toHaveBeenCalledTimes(2)
    expect(wrapper.vm.maxTimestamp).toBe('2025-06-01T10:00:00.000+0200')
  })

  it('loads the first and last start of the version once, when it is shown', async () => {
    startTimes('2025-01-01T10:00:00.000+0100', '2025-06-01T10:00:00.000+0200')
    const wrapper = mountDetails()
    await flushPromises()
    expect(HistoryService.findProcessesInstancesHistory).not.toHaveBeenCalled()

    await wrapper.setProps({ shown: true })
    await flushPromises()
    await wrapper.setProps({ shown: false })
    await wrapper.setProps({ shown: true })
    await flushPromises()

    expect(HistoryService.findProcessesInstancesHistory).toHaveBeenCalledTimes(2)
    expect(HistoryService.findProcessesInstancesHistory).toHaveBeenCalledWith(
      { processDefinitionId: version.id, sorting: [{ sortBy: 'startTime', sortOrder: 'asc' }] }, 0, 1)
    expect(wrapper.vm.minTimestamp).toBe('2025-01-01T10:00:00.000+0100')
    expect(wrapper.vm.maxTimestamp).toBe('2025-06-01T10:00:00.000+0200')
  })

  it('asks no start times when there is no history', async () => {
    const wrapper = mountDetails({}, 'none')
    await wrapper.setProps({ shown: true })
    await flushPromises()

    expect(HistoryService.findProcessesInstancesHistory).not.toHaveBeenCalled()
  })

  it('clears the start times and asks again on the next show when they cannot be loaded', async () => {
    HistoryService.findProcessesInstancesHistory.mockRejectedValueOnce(new Error('down'))
    const wrapper = mountDetails()
    await wrapper.setProps({ shown: true })
    await flushPromises()

    expect(wrapper.vm.minTimestamp).toBeNull()
    expect(wrapper.vm.timestampsLoaded).toBe(false)

    startTimes('2025-01-01T10:00:00.000+0100', '2025-06-01T10:00:00.000+0200')
    await wrapper.setProps({ shown: false })
    await wrapper.setProps({ shown: true })
    await flushPromises()
    expect(wrapper.vm.minTimestamp).toBe('2025-01-01T10:00:00.000+0100')
  })

  it('reloads the start times of the new version while shown', async () => {
    startTimes('2025-01-01T10:00:00.000+0100', '2025-06-01T10:00:00.000+0200')
    const wrapper = mountDetails({ shown: true })
    await wrapper.setProps({ shown: true })
    await flushPromises()

    startTimes('2026-01-01T10:00:00.000+0100', '2026-02-01T10:00:00.000+0100')
    await wrapper.setProps({ version: { ...version, id: 'invoice:38:def', version: '38' } })
    await flushPromises()

    expect(HistoryService.findProcessesInstancesHistory).toHaveBeenLastCalledWith(
      { processDefinitionId: 'invoice:38:def', sorting: [{ sortBy: 'startTime', sortOrder: 'desc' }] }, 0, 1)
    expect(wrapper.vm.minTimestamp).toBe('2026-01-01T10:00:00.000+0100')
  })

  it('reloads the start times when the instance counts of the version change', async () => {
    startTimes('2025-01-01T10:00:00.000+0100', '2025-06-01T10:00:00.000+0200')
    const wrapper = mountDetails()
    await wrapper.setProps({ shown: true })
    await flushPromises()

    await wrapper.setProps({ version: { ...version, allInstances: 5 } })
    await flushPromises()

    expect(HistoryService.findProcessesInstancesHistory).toHaveBeenCalledTimes(4)
  })

  it('drops the start times of a version that is no longer the one shown', async () => {
    let resolveOld
    HistoryService.findProcessesInstancesHistory.mockImplementation(() => new Promise(resolve => { resolveOld = resolve }))
    const wrapper = mountDetails()
    await wrapper.setProps({ shown: true })

    startTimes('2026-01-01T10:00:00.000+0100', '2026-02-01T10:00:00.000+0100')
    await wrapper.setProps({ version: { ...version, id: 'invoice:38:def', version: '38' } })
    await flushPromises()
    resolveOld([{ startTime: '1999-01-01T00:00:00.000+0100' }])
    await flushPromises()

    expect(wrapper.vm.minTimestamp).toBe('2026-01-01T10:00:00.000+0100')
  })

  it('links to the super process instance of the selected version', () => {
    const wrapper = mountDetails({ selectedInstance: { superProcessInstanceId: 'parent-1' } })

    expect(wrapper.text()).toContain('parent-1')
    expect(wrapper.vm.routeToSuperProcessInstance('parent-1'))
      .toMatchObject({ name: 'process-instance-id', params: { instanceId: 'parent-1' } })
  })
})
