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
import { mount, flushPromises } from '@vue/test-utils'
import { createStore } from 'vuex'
import CalledProcessInstancesTable from '@/components/process/tables/CalledProcessInstancesTable.vue'
import { HistoryService, ProcessService } from '@/services.js'

/*
 * Request races in the Called process instances tab (CIB7-2830).
 *
 * Every list request returns a promise the test settles by hand, so each test decides in which
 * order the answers arrive. The rows on screen must belong to the latest load, each called
 * instance listed once, whatever the order.
 */

vi.mock('@/services.js', () => ({
  HistoryService: { findProcessesInstancesHistory: vi.fn() },
  ProcessService: { findCurrentProcessesInstances: vi.fn() }
}))

function deferred() {
  let resolve, reject
  const promise = new Promise((res, rej) => { resolve = res; reject = rej })
  return { promise, resolve, reject }
}

let calls

const instance = (id, extra = {}) => ({
  id, processDefinitionKey: 'called', processDefinitionVersion: 1, state: 'ACTIVE', ...extra
})
const CALLED = [instance('called-1'), instance('called-2')]
const HISTORY = [
  { activityType: 'callActivity', activityId: 'call-a', calledProcessInstanceId: 'called-1' },
  { activityType: 'callActivity', activityId: 'call-b', calledProcessInstanceId: 'called-2' },
  { activityType: 'userTask', activityId: 'task' }
]

const Host = { extends: CalledProcessInstancesTable, render: () => null }

function mountTable(props = {}, config = { camundaHistoryLevel: 'full', maxProcessesResults: 2 }) {
  const store = createStore({
    state: { process: { list: [{ key: 'called', name: 'Called process' }] } },
    actions: { setHighlightedElement: vi.fn() }
  })
  const wrapper = mount(Host, {
    props: { selectedInstance: { id: 'pi-1', processDefinitionId: 'calling:1:1' }, activityInstanceHistory: null, ...props },
    global: {
      plugins: [store],
      // Mounted directly, the table is its own $root, so `config` comes in as a global property
      mocks: { $t: (msg) => msg, config }
    }
  })
  return wrapper
}

const rowIds = (vm) => vm.matchedCalledList.map(row => row.calledProcessInstance)

async function settle(call, value) {
  if (value instanceof Error) call.reject(value)
  else call.resolve(value)
  await flushPromises()
}

describe('CalledProcessInstancesTable - request races (CIB7-2830)', () => {
  beforeEach(() => {
    calls = []
    const record = (filter, firstResult, maxResults) => {
      const d = deferred()
      calls.push({ filter, firstResult, maxResults, ...d })
      return d.promise
    }
    HistoryService.findProcessesInstancesHistory.mockImplementation(record)
    ProcessService.findCurrentProcessesInstances.mockImplementation(record)
  })

  afterEach(() => {
    vi.clearAllMocks()
    vi.restoreAllMocks()
  })

  it('loads the first page of the selected instance\'s called instances', async () => {
    const { vm } = mountTable()

    expect(calls).toHaveLength(1)
    expect(calls[0]).toMatchObject({ filter: { superProcessInstanceId: 'pi-1' }, firstResult: 0, maxResults: 2 })
    expect(vm.loading).toBe(true)

    await settle(calls[0], CALLED)

    expect(rowIds(vm)).toEqual(['called-1', 'called-2'])
    expect(vm.loading).toBe(false)
  })

  it('uses the runtime query when the history level is none', async () => {
    mountTable({}, { camundaHistoryLevel: 'none' })

    expect(ProcessService.findCurrentProcessesInstances).toHaveBeenCalledTimes(1)
    expect(HistoryService.findProcessesInstancesHistory).not.toHaveBeenCalled()
  })

  // The case of the ticket: the history answers while the first page is still loading
  it.each([
    ['before', true],
    ['after', false]
  ])('lists each called instance once when the history arrives %s the first page', async (_, historyFirst) => {
    const wrapper = mountTable()

    if (historyFirst) {
      await wrapper.setProps({ activityInstanceHistory: HISTORY })
      await settle(calls[0], CALLED)
    } else {
      await settle(calls[0], CALLED)
      await wrapper.setProps({ activityInstanceHistory: HISTORY })
    }

    // The history sends no request of its own, neither page 0 again nor page 2
    expect(calls).toHaveLength(1)
    expect(rowIds(wrapper.vm)).toEqual(['called-1', 'called-2'])
    // and its calling activities are matched to the rows
    expect(wrapper.vm.matchedCalledList.map(row => row.callingActivity?.activityId)).toEqual(['call-a', 'call-b'])
    expect(wrapper.vm.matchedCalledList[0]).toMatchObject({ key: 'called', version: 1, name: 'Called process', state: 'ACTIVE' })
  })

  it('drops the calling activities when the history is cleared', async () => {
    const wrapper = mountTable({ activityInstanceHistory: HISTORY })
    await settle(calls[0], CALLED)

    await wrapper.setProps({ activityInstanceHistory: null })

    expect(wrapper.vm.matchedCalledList.map(row => row.callingActivity)).toEqual([undefined, undefined])
  })

  describe('another instance is selected while the first one\'s list is loading', () => {
    const OTHER = [instance('other-1')]

    it.each([
      ['the older answer arrives first', [0, 1]],
      ['the newer answer arrives first', [1, 0]]
    ])('shows only the newer instance\'s rows when %s', async (_, order) => {
      const wrapper = mountTable()
      await wrapper.setProps({ selectedInstance: { id: 'pi-2' } })

      expect(calls[1]).toMatchObject({ filter: { superProcessInstanceId: 'pi-2' }, firstResult: 0 })
      const answers = [CALLED, OTHER]
      for (const i of order) await settle(calls[i], answers[i])

      expect(rowIds(wrapper.vm)).toEqual(['other-1'])
      expect(wrapper.vm.loading).toBe(false)
    })

    it('keeps the loader on while the newer load runs, also if the older one fails', async () => {
      vi.spyOn(console, 'error').mockImplementation(() => {})
      const wrapper = mountTable()
      await wrapper.setProps({ selectedInstance: { id: 'pi-2' } })

      await settle(calls[0], new Error('older failed'))

      expect(wrapper.vm.loading).toBe(true)
      expect(console.error).not.toHaveBeenCalled()

      await settle(calls[1], OTHER)

      expect(rowIds(wrapper.vm)).toEqual(['other-1'])
      expect(wrapper.vm.loading).toBe(false)
    })

    it('does not keep the older instance\'s rows or paging when the newer one\'s load fails', async () => {
      vi.spyOn(console, 'error').mockImplementation(() => {})
      const wrapper = mountTable()
      await settle(calls[0], CALLED)
      expect(wrapper.vm.firstResult).toBe(2)

      await wrapper.setProps({ selectedInstance: { id: 'pi-2' } })
      // The rows of the older instance go as soon as the newer one is selected
      expect(rowIds(wrapper.vm)).toEqual([])
      await settle(calls[1], new Error('newer failed'))

      expect(rowIds(wrapper.vm)).toEqual([])
      expect(wrapper.vm.firstResult).toBe(0)
      expect(wrapper.vm.loading).toBe(false)
      expect(console.error).toHaveBeenCalledTimes(1)
    })

    it('starts the newer instance\'s list at its first page, also after a page 2 of the older one', async () => {
      const wrapper = mountTable()
      await settle(calls[0], CALLED)
      wrapper.vm.loadCalledProcessInstances(true)
      expect(calls[1]).toMatchObject({ firstResult: 2 })

      await wrapper.setProps({ selectedInstance: { id: 'pi-2' } })
      expect(calls[2]).toMatchObject({ filter: { superProcessInstanceId: 'pi-2' }, firstResult: 0 })

      // Page 2 of the older instance arrives late: it must not be appended to the newer list
      await settle(calls[2], OTHER)
      await settle(calls[1], [instance('called-3')])

      expect(rowIds(wrapper.vm)).toEqual(['other-1'])
      expect(wrapper.vm.firstResult).toBe(2)
    })
  })

  describe('scrolling', () => {
    const scrollToEnd = (wrapper) => wrapper.vm.handleScroll({ target: { scrollTop: 100, clientHeight: 100, scrollHeight: 200 } })

    it('appends the next page at the end of the list', async () => {
      const wrapper = mountTable()
      await settle(calls[0], CALLED)

      scrollToEnd(wrapper)
      expect(calls[1]).toMatchObject({ firstResult: 2, maxResults: 2 })
      await settle(calls[1], [instance('called-3')])

      expect(rowIds(wrapper.vm)).toEqual(['called-1', 'called-2', 'called-3'])
      expect(wrapper.vm.firstResult).toBe(4)
    })

    it('does not load the next page while a page is loading', async () => {
      const wrapper = mountTable()

      scrollToEnd(wrapper)

      expect(calls).toHaveLength(1)
    })

    it('does not load past the last page', async () => {
      const wrapper = mountTable()
      await settle(calls[0], [instance('called-1')])

      scrollToEnd(wrapper)

      expect(calls).toHaveLength(1)
    })
  })

  it('ignores a load in flight once no instance is selected', async () => {
    const wrapper = mountTable()
    await wrapper.setProps({ selectedInstance: null })

    expect(wrapper.vm.loading).toBe(false)
    await settle(calls[0], CALLED)

    expect(rowIds(wrapper.vm)).toEqual([])
  })
})
