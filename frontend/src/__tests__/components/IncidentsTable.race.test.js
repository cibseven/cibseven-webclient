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
import { h } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createStore } from 'vuex'
import IncidentsTable from '@/components/process/tables/IncidentsTable.vue'
import { IncidentService } from '@/services.js'

/*
 * Request races in the Incidents tab (CIB7-2825).
 *
 * Every list and count request returns a promise the test settles by hand, so each test decides in
 * which order the answers arrive. The situation they share: a load is still waiting for its answer
 * when another one starts (a sort change, an activity picked in the diagram, another instance). The
 * rows, the count and the loader must then belong to the latest load, each incident listed once,
 * whatever the order the answers arrive in, and whether they succeed or fail.
 *
 * The host is the table without its template (only the modals it closes). The table keeps its list
 * itself, so the store has no `incidents` module: only the getters and actions of the diagram.
 */

vi.mock('@/services.js', () => ({
  IncidentService: {
    findIncidents: vi.fn(),
    findIncidentsCount: vi.fn(),
    fetchHistoricIncidents: vi.fn(),
    fetchHistoricIncidentsCount: vi.fn(),
    retryJobById: vi.fn(),
    retryExternalTaskById: vi.fn(),
    setIncidentAnnotation: vi.fn()
  }
}))

function deferred() {
  let resolve, reject
  const promise = new Promise((res, rej) => { resolve = res; reject = rej })
  return { promise, resolve, reject }
}

let lists
let counts

const incident = (id) => ({ id, incidentType: 'failedExternalTask', incidentMessage: `message of ${id}` })
// The same incidents in the two orders, so a test can tell which answer is listed
const FIRST = [incident('inc-1'), incident('inc-2')]
const SORTED = [incident('inc-2'), incident('inc-1')]

const hideRetryModal = vi.fn()
const RetryModal = { methods: { hide: hideRetryModal }, render: () => null }
const hideAnnotationModal = vi.fn()
const AnnotationModal = { methods: { hide: hideAnnotationModal }, render: () => null }
const Host = {
  extends: IncidentsTable,
  render: () => [h(RetryModal, { ref: 'incidentRetryModal' }), h(AnnotationModal, { ref: 'annotationModal' })]
}

// `instances` are the process instances the definition's view has loaded, with their business keys
function createDiagramStore({ instances = [] } = {}) {
  return createStore({
    state: { selectedActivityId: '' },
    getters: {
      selectedActivityId: state => state.selectedActivityId,
      externalTaskActivityIds: () => []
    },
    mutations: { selectActivity(state, id) { state.selectedActivityId = id } },
    actions: { clearActivitySelection: vi.fn(), setHighlightedElement: vi.fn(), selectActivity: vi.fn() },
    modules: {
      instances: { namespaced: true, getters: { instances: () => instances } }
    }
  })
}

// Pass the store of an earlier table to mount the next one in the same app, as the views do when a tab
// is left and opened again
function mountTable({ historyLevel = 'full', props = {}, store = createDiagramStore() } = {}) {
  const wrapper = mount(Host, {
    props: { instance: { id: 'pi-1' }, process: { id: 'pd-1' }, isInstanceView: true, ...props },
    global: {
      plugins: [store],
      // Mounted directly, the table is its own $root, so `config` comes in as a global property
      mocks: { $t: (msg) => msg, config: { camundaHistoryLevel: historyLevel, maxProcessesResults: 2 } }
    }
  })
  return { wrapper, vm: wrapper.vm, store }
}

const rowIds = (vm) => vm.incidents.map(i => i.id)

async function settle(call, value) {
  if (value instanceof Error) call.reject(value)
  else call.resolve(value)
  await flushPromises()
}

describe('IncidentsTable - request races (CIB7-2825)', () => {
  beforeEach(() => {
    lists = []
    counts = []
    const record = (calls) => (params) => {
      const d = deferred()
      calls.push({ params, ...d })
      return d.promise
    }
    IncidentService.fetchHistoricIncidents.mockImplementation(record(lists))
    IncidentService.findIncidents.mockImplementation(record(lists))
    IncidentService.fetchHistoricIncidentsCount.mockImplementation(record(counts))
    IncidentService.findIncidentsCount.mockImplementation(record(counts))
  })

  afterEach(() => {
    vi.clearAllMocks()
    vi.restoreAllMocks()
  })

  it('loads the first page and the count of the instance', async () => {
    const { vm } = mountTable()

    expect(lists).toHaveLength(1)
    expect(lists[0].params).toMatchObject({ processInstanceId: 'pi-1', firstResult: 0, maxResults: 2 })
    await settle(lists[0], FIRST)
    await settle(counts[0], 2)

    expect(rowIds(vm)).toEqual(['inc-1', 'inc-2'])
    expect(vm.totalCount).toBe(2)
    expect(vm.loading).toBe(false)
  })

  // The case of the ticket: the column header is clicked while the first page is still loading
  describe.each([
    ['history', 'full'],
    ['runtime', 'none']
  ])('a sort change while the first page loads (%s incidents)', (_, historyLevel) => {
    it.each([
      ['the older answer arrives first', [0, 1]],
      ['the newer answer arrives first', [1, 0]]
    ])('lists each incident once when %s', async (__, order) => {
      const { vm } = mountTable({ historyLevel })
      vm.handleExternalSort({ sortBy: 'incidentType', sortDesc: true })

      expect(lists).toHaveLength(2)
      expect(lists[1].params).toMatchObject({ firstResult: 0, sortOrder: 'asc' })
      const answers = [FIRST, SORTED]
      for (const i of order) await settle(lists[i], answers[i])

      // Each incident once, in the order of the newer load
      expect(rowIds(vm)).toEqual(['inc-2', 'inc-1'])
      expect(vm.loading).toBe(false)
    })
  })

  it('shows the rows of the newer load, not those of a late older answer', async () => {
    const { vm, store } = mountTable()
    store.commit('selectActivity', 'external-task')
    await flushPromises()

    expect(lists[1].params).toMatchObject({ failedActivityId: 'external-task', firstResult: 0 })
    await settle(lists[1], [incident('of-the-activity')])
    await settle(lists[0], [incident('of-the-instance'), incident('other')])

    expect(rowIds(vm)).toEqual(['of-the-activity'])
  })

  it('keeps the loader on while the newer load runs, also if the older one fails', async () => {
    const { vm } = mountTable()
    const older = vm.loadIncidentsData('pi-1', true)
    const newer = vm.loadIncidentsData('pi-1', true)

    lists[1].reject(new Error('older failed'))
    // Nobody waits for a superseded load (sort, watchers and filter start it and move on): its error
    // would end as an unhandled rejection about a request the user has already replaced
    await expect(older).resolves.toBeUndefined()

    expect(vm.loading).toBe(true)

    await settle(lists[2], SORTED)
    await newer
    expect(rowIds(vm)).toEqual(['inc-2', 'inc-1'])
    expect(vm.loading).toBe(false)
  })

  // Nobody waits for the current load either (sort, watchers, search and scrolling start it and move
  // on): a rethrown error ended as an unhandled rejection, and the table said there were no incidents
  it('shows that the current load failed instead of rejecting', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const { vm } = mountTable()

    lists[0].reject(new Error('current failed'))
    await flushPromises()

    expect(vm.loadFailed).toBe(true)
    expect(vm.loading).toBe(false)
    expect(console.error).toHaveBeenCalledTimes(1)
  })

  // The message replaces the rows without a page change, so screen readers only announce it as an alert
  it('announces the failure message as an alert', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const wrapper = mount(IncidentsTable, {
      props: { instance: { id: 'pi-1' }, process: { id: 'pd-1' }, isInstanceView: true },
      shallow: true,
      global: {
        plugins: [createDiagramStore()],
        stubs: { PagedScrollableContent: { template: '<div><slot /></div>' } },
        mocks: { $t: (msg) => msg, config: { camundaHistoryLevel: 'full', maxProcessesResults: 2 } }
      }
    })

    await settle(lists[0], new Error('current failed'))

    const message = wrapper.find('[role="alert"]')
    expect(message.exists()).toBe(true)
    expect(message.text()).toBe('process-instance.incidents.loadFailed')
  })

  it('does not show a failure of a superseded load', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const { vm } = mountTable()
    vm.handleExternalSort({ sortBy: 'incidentType', sortDesc: true })

    await settle(lists[0], new Error('older failed'))

    expect(vm.loadFailed).toBe(false)
    expect(console.error).not.toHaveBeenCalled()
  })

  it('clears the failure when the next load starts', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const { vm } = mountTable()
    await settle(lists[0], new Error('failed'))

    vm.handleExternalSort({ sortBy: 'incidentType', sortDesc: true })

    expect(vm.loadFailed).toBe(false)
    await settle(lists[1], SORTED)
    expect(rowIds(vm)).toEqual(['inc-2', 'inc-1'])
  })

  // Leaving the tab, or following the link to an incident's instance, unmounts the table while its load
  // may still be waiting. When the list lived in the global store, its late answer reached the next
  // table's list.
  describe('a table that is unmounted while it loads', () => {
    const DEFINITION_VIEW = { props: { instance: null, process: { id: 'pd-1' }, isInstanceView: false } }

    it('does not replace the list of the next table with its late first page', async () => {
      const older = mountTable(DEFINITION_VIEW)
      expect(lists[0].params).toMatchObject({ processDefinitionId: 'pd-1', firstResult: 0 })
      older.wrapper.unmount()

      const newer = mountTable({ store: older.store, props: { instance: { id: 'pi-2' } } })
      expect(lists[1].params).toMatchObject({ processInstanceId: 'pi-2', firstResult: 0 })
      await settle(lists[1], [incident('of-pi-2')])
      await settle(lists[0], [incident('of-the-definition'), incident('other')])

      expect(rowIds(newer.vm)).toEqual(['of-pi-2'])
    })

    // Its counters stayed current, so a late answer still ran on the unmounted table
    it('ignores its late answers', async () => {
      vi.spyOn(console, 'error').mockImplementation(() => {})
      const { wrapper, vm } = mountTable()
      wrapper.unmount()

      await settle(lists[0], new Error('failed after the table was left'))
      await settle(counts[0], 5)

      expect(console.error).not.toHaveBeenCalled()
      expect(vm.loadFailed).toBe(false)
      expect(vm.totalCount).toBe(null)
    })

    it('does not add its late next page to the list of the next table', async () => {
      const older = mountTable(DEFINITION_VIEW)
      await settle(lists[0], [incident('inc-1'), incident('inc-2')])
      older.vm.loadNextPage()
      expect(lists[1].params).toMatchObject({ processDefinitionId: 'pd-1', firstResult: 2 })
      older.wrapper.unmount()

      const newer = mountTable({ store: older.store, props: { instance: { id: 'pi-2' } } })
      await settle(lists[2], [incident('of-pi-2')])
      await settle(lists[1], [incident('inc-3')])

      expect(rowIds(newer.vm)).toEqual(['of-pi-2'])
    })
  })

  // The table is exported for other views (and EE): two tables in the same app each list their own
  // incidents. When the list lived in the global store, both showed the answer of the latest load.
  it('keeps the list of each table to its own loads', async () => {
    const first = mountTable({ props: { instance: { id: 'pi-1' } } })
    const second = mountTable({ store: first.store, props: { instance: { id: 'pi-2' } } })

    await settle(lists[1], [incident('of-pi-2')])
    await settle(lists[0], [incident('of-pi-1')])

    expect(rowIds(first.vm)).toEqual(['of-pi-1'])
    expect(rowIds(second.vm)).toEqual(['of-pi-2'])
  })

  // A saved annotation changes the table's own row (before, the store's `updateIncidentAnnotation` did)
  it('shows a saved annotation in its row', async () => {
    const { vm } = mountTable()
    await settle(lists[0], FIRST)
    IncidentService.setIncidentAnnotation.mockResolvedValue()

    vm.setIncidentAnnotation({ id: 'inc-2', params: { annotation: 'looked at' } })
    await flushPromises()

    expect(IncidentService.setIncidentAnnotation).toHaveBeenCalledWith('inc-2', { annotation: 'looked at' })
    expect(vm.incidents.map(i => i.annotation)).toEqual([undefined, 'looked at'])
    expect(hideAnnotationModal).toHaveBeenCalled()
  })

  // The definition's view lists the incidents of all its instances, each with the instance's business key
  it('adds the business keys of the instances to the rows of a definition', async () => {
    const store = createDiagramStore({ instances: [{ id: 'pi-1', businessKey: 'order-1' }] })
    const { vm } = mountTable({ store, props: { instance: null, isInstanceView: false } })

    await settle(lists[0], [{ ...incident('inc-1'), processInstanceId: 'pi-1' }])
    vm.loadNextPage()
    await settle(lists[1], [{ ...incident('inc-2'), processInstanceId: 'pi-2' }])

    expect(vm.incidents.map(i => i.businessKey)).toEqual(['order-1', undefined])
  })

  // The free-text search loads 800 ms after the last key stroke
  describe('the search', () => {
    beforeEach(() => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it('loads the first page with the text once typing has paused', async () => {
      const { vm } = mountTable()
      await settle(lists[0], FIRST)

      vm.onInput('boom')
      vm.onInput('booms')
      vi.advanceTimersByTime(800)

      expect(lists).toHaveLength(2)
      expect(lists[1].params).toMatchObject({ firstResult: 0, incidentMessageLike: '%booms%' })
      await settle(lists[1], [incident('booms')])
      expect(rowIds(vm)).toEqual(['booms'])
    })

    // Typed, then the link to an incident's instance followed before the search started: the unmounted
    // table's search would empty the next table's list and fill it with its own incidents
    it('does not load once the table is unmounted', async () => {
      const older = mountTable()
      await settle(lists[0], FIRST)
      older.vm.onInput('boom')
      older.wrapper.unmount()

      const newer = mountTable({ store: older.store, props: { instance: { id: 'pi-2' } } })
      await settle(lists[1], [incident('of-pi-2')])
      vi.advanceTimersByTime(800)
      await flushPromises()

      expect(lists).toHaveLength(2)
      expect(rowIds(newer.vm)).toEqual(['of-pi-2'])
    })

    it('waits for each table on its own', async () => {
      const first = mountTable()
      const second = mountTable({ props: { instance: { id: 'pi-2' } } })

      first.vm.onInput('in pi-1')
      second.vm.onInput('in pi-2')
      vi.advanceTimersByTime(800)

      expect(lists.slice(2).map(call => call.params)).toEqual([
        expect.objectContaining({ processInstanceId: 'pi-1', incidentMessageLike: '%in pi-1%' }),
        expect.objectContaining({ processInstanceId: 'pi-2', incidentMessageLike: '%in pi-2%' })
      ])
    })
  })

  // The engine deletes a retried incident from the runtime incidents and marks it resolved in the
  // history: removing the row locally left the total and the offset of the next page wrong
  describe('a retry', () => {
    it.each([
      ['a job', { incidentType: 'failedJob', configuration: 'job-1' }, 'retryJobById', 'job-1'],
      ['an external task', { incidentType: 'failedExternalTask', configuration: 'task-1' }, 'retryExternalTaskById', 'task-1']
    ])('of %s loads the first page and the count again', async (_, item, endpoint, configuration) => {
      const { vm } = mountTable()
      await settle(lists[0], FIRST)
      await settle(counts[0], 3)
      vm.loadNextPage()
      await settle(lists[1], [incident('inc-3')])
      IncidentService[endpoint].mockResolvedValue()

      vm.incrementNumberRetry({ item: { id: 'inc-1', ...item }, params: { retries: 1 } })
      await flushPromises()

      expect(IncidentService[endpoint]).toHaveBeenCalledWith(configuration, { retries: 1 })
      expect(hideRetryModal).toHaveBeenCalled()
      expect(lists[2].params).toMatchObject({ processInstanceId: 'pi-1', firstResult: 0 })
      expect(counts).toHaveLength(2)

      await settle(lists[2], [incident('inc-2'), incident('inc-3')])
      await settle(counts[1], 2)
      expect(rowIds(vm)).toEqual(['inc-2', 'inc-3'])
      expect(vm.totalCount).toBe(2)
    })
  })

  describe('the count', () => {
    it('shows the newer count when the older one arrives late', async () => {
      const { vm } = mountTable()
      vm.handleExternalSort({ sortBy: 'incidentType', sortDesc: true })

      await settle(counts[1], 1)
      await settle(counts[0], 7)

      expect(vm.totalCount).toBe(1)
    })

    it('keeps the newer count when the older one fails late', async () => {
      const { vm } = mountTable()
      vm.handleExternalSort({ sortBy: 'incidentType', sortDesc: true })

      await settle(counts[1], 1)
      await settle(counts[0], new Error('older count failed'))

      expect(vm.totalCount).toBe(1)
    })

    // The count does not depend on the page: asking again while scrolling only costs a request, and a
    // failing one would turn a known total into an unknown one in the middle of the list
    it('is asked for with the first page only', async () => {
      const { vm } = mountTable()
      await settle(lists[0], FIRST)
      await settle(counts[0], 3)

      vm.loadNextPage()
      await settle(lists[1], [incident('inc-3')])

      expect(counts).toHaveLength(1)
      expect(vm.totalCount).toBe(3)
    })

    it('is shown when it answers after the next page has started loading', async () => {
      const { vm } = mountTable()
      await settle(lists[0], FIRST)

      vm.loadNextPage()
      await settle(counts[0], 3)

      expect(vm.totalCount).toBe(3)
    })

    it('shows no count when the current count fails', async () => {
      const { vm } = mountTable()

      await settle(counts[0], new Error('count failed'))

      expect(vm.totalCount).toBe(null)
    })

    // Without a total the list never counts as complete, so every scroll at its end asked for an empty
    // page again; before the count moved to the first page, the next page asked for it again
    it('is asked for again with the next page when it failed', async () => {
      const { vm } = mountTable()
      await settle(counts[0], new Error('count failed'))
      await settle(lists[0], FIRST)

      vm.loadNextPage()

      expect(counts).toHaveLength(2)
      expect(counts[1].params).not.toHaveProperty('firstResult')
      await settle(counts[1], 3)
      expect(vm.totalCount).toBe(3)
    })

    // A count still waiting for its answer is not asked for a second time
    it('is not asked for again with the next page while it loads', async () => {
      const { vm } = mountTable()
      await settle(lists[0], FIRST)

      vm.loadNextPage()

      expect(counts).toHaveLength(1)
    })
  })

  describe('paging', () => {
    it('adds the next page to the list', async () => {
      const { vm } = mountTable()
      await settle(lists[0], [incident('inc-1'), incident('inc-2')])

      vm.loadNextPage()
      expect(lists[1].params).toMatchObject({ firstResult: 2 })
      await settle(lists[1], [incident('inc-3')])

      expect(rowIds(vm)).toEqual(['inc-1', 'inc-2', 'inc-3'])
    })

    // The next scroll must ask for the failed page again, not skip it
    it('asks for a failed next page again on the next scroll', async () => {
      vi.spyOn(console, 'error').mockImplementation(() => {})
      const { vm } = mountTable()
      await settle(lists[0], FIRST)

      vm.loadNextPage()
      await settle(lists[1], new Error('page 2 failed'))
      vm.loadNextPage()

      expect(lists[2].params).toMatchObject({ firstResult: 2 })
      await settle(lists[2], [incident('inc-3')])
      expect(rowIds(vm)).toEqual(['inc-1', 'inc-2', 'inc-3'])
      expect(vm.loadFailed).toBe(false)
    })

    it('does not add a late next page of the old order to the list of the new one', async () => {
      const { vm } = mountTable()
      await settle(lists[0], [incident('inc-1'), incident('inc-2')])
      vm.loadNextPage()

      vm.handleExternalSort({ sortBy: 'incidentType', sortDesc: true })
      expect(lists[2].params).toMatchObject({ firstResult: 0, sortOrder: 'asc' })
      await settle(lists[2], [incident('inc-2'), incident('inc-1')])
      await settle(lists[1], [incident('inc-3')])

      expect(rowIds(vm)).toEqual(['inc-2', 'inc-1'])
    })

    it('starts at the first page when another instance is shown after scrolling', async () => {
      const { wrapper, vm } = mountTable()
      await settle(lists[0], [incident('inc-1'), incident('inc-2')])
      vm.loadNextPage()
      await settle(lists[1], [incident('inc-3')])

      await wrapper.setProps({ instance: { id: 'pi-2' } })

      expect(lists[2].params).toMatchObject({ processInstanceId: 'pi-2', firstResult: 0 })
      await settle(lists[2], [incident('of-pi-2')])
      expect(rowIds(vm)).toEqual(['of-pi-2'])
    })
  })
})
