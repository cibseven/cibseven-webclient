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
import DeploymentsView from '@/components/deployment/DeploymentsView.vue'
import { ProcessService } from '@/services.js'

vi.mock('@/services.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ProcessService: {
      findDeployments: vi.fn(() => Promise.resolve([])),
      findDeploymentsCount: vi.fn(() => Promise.resolve(0)),
      findDeploymentResources: vi.fn(() => Promise.resolve([])),
      deleteDeployment: vi.fn(() => Promise.resolve())
    }
  }
})

const m = DeploymentsView.methods
const c = DeploymentsView.computed

const deployment = (overrides = {}) => ({
  id: 'd1', name: 'Invoice', deploymentTime: '2026-01-15T10:00:00Z', isSelected: false, ...overrides
})

/**
 * A `this` for DeploymentsView's methods. The component hosts the deployment list, the
 * resources sidebar and the upload dialog; only its own paging, grouping and deletion
 * logic is under test, so the refs and the injected `loadProcesses` are stubbed.
 */
function context(overrides = {}) {
  const vm = {
    $t: (key) => key,
    $route: { query: {} },
    $router: { push: vi.fn() },
    $refs: { deploymentsDeleted: { show: vi.fn() }, success: { show: vi.fn() } },
    loadProcesses: vi.fn(),
    deploymentId: null,
    rightOpen: false,
    groups: [],
    deployments: [],
    deployment: null,
    totalCount: undefined,
    loading: false,
    deleteLoader: false,
    filter: '',
    firstResult: 0,
    maxResults: 50,
    sortBy: 'deploymentTime',
    sortOrder: 'desc',
    resources: [],
    resourcesLoading: false,
    deploymentsDelData: { total: 0, deleted: 0 },
    deploymentsReady: false,
    searchDeployment: false,
    ...overrides
  }
  for (const [name, method] of Object.entries(m)) {
    if (overrides[name] === undefined) vm[name] = method.bind(vm)
  }
  // Computed properties the methods read.
  for (const [name, def] of Object.entries(c)) {
    if (overrides[name] !== undefined || typeof def !== 'function') continue
    Object.defineProperty(vm, name, { get: () => def.call(vm), configurable: true })
  }
  return vm
}

const flush = async (times = 10) => {
  for (let i = 0; i < times; i++) await Promise.resolve()
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('DeploymentsView - data', () => {
  it('should seed the filter from the URL', () => {
    const data = DeploymentsView.data.call({ $route: { query: { filter: 'invoice' } } })

    expect(data.filter).toBe('invoice')
  })

  it('should start with an empty filter when the URL has none', () => {
    expect(DeploymentsView.data.call({ $route: { query: {} } }).filter).toBe('')
  })

  it('should default to newest deployments first, 50 per page', () => {
    const data = DeploymentsView.data.call({ $route: { query: {} } })

    expect(data).toMatchObject({ sortBy: 'deploymentTime', sortOrder: 'desc', maxResults: 50, firstResult: 0 })
  })
})

describe('DeploymentsView - computed', () => {
  it('nameLike should wrap the filter in wildcards', () => {
    expect(c.nameLike.call(context({ filter: 'invoice' }))).toBe('%invoice%')
  })

  it('nameLike should be empty for an empty filter', () => {
    expect(c.nameLike.call(context({ filter: '' }))).toBe('')
  })

  it('deploymentsSelected should list the checked deployments', () => {
    const vm = context({ deployments: [deployment({ isSelected: true }), deployment({ id: 'd2' })] })

    expect(c.deploymentsSelected.call(vm).map(d => d.id)).toEqual(['d1'])
  })

  it('isAllChecked should be true only when every deployment is checked', () => {
    const all = context({ deployments: [deployment({ isSelected: true }), deployment({ id: 'd2', isSelected: true })] })
    expect(c.isAllChecked.get.call(all)).toBe(true)

    const some = context({ deployments: [deployment({ isSelected: true }), deployment({ id: 'd2' })] })
    expect(c.isAllChecked.get.call(some)).toBe(false)
  })

  it('isAllChecked should be false when the list is empty', () => {
    expect(c.isAllChecked.get.call(context({ deployments: [] }))).toBe(false)
  })

  it('isAllChecked should check or uncheck every deployment', () => {
    const vm = context({ deployments: [deployment(), deployment({ id: 'd2' })] })

    c.isAllChecked.set.call(vm, true)
    expect(vm.deployments.every(d => d.isSelected)).toBe(true)

    c.isAllChecked.set.call(vm, false)
    expect(vm.deployments.every(d => !d.isSelected)).toBe(true)
  })

  // The count is unknown until the first request answers, and "all loaded" must not be
  // claimed in that window or paging stops before it starts.
  it('allLoaded should be false while the total is unknown', () => {
    expect(c.allLoaded.call(context({ totalCount: undefined, deployments: [] }))).toBe(false)
  })

  it('allLoaded should be true once the list reaches the total', () => {
    expect(c.allLoaded.call(context({ totalCount: 2, deployments: [deployment(), deployment()] }))).toBe(true)
  })

  it('allLoaded should be false while deployments remain', () => {
    expect(c.allLoaded.call(context({ totalCount: 5, deployments: [deployment()] }))).toBe(false)
  })

  it('sortingFields should offer sorting by time and name', () => {
    expect(c.sortingFields.call(context()).map(f => f.value)).toEqual(['deploymentTime', 'name'])
  })

  it('DeploymentsViewActionsPlugin should resolve the optional plugin slot', () => {
    const component = { name: 'plugin' }
    expect(c.DeploymentsViewActionsPlugin.call({
      $options: { components: { DeploymentsViewActionsPlugin: component } }
    })).toBe(component)
  })

  it.each([[{}], [{ components: {} }]])('DeploymentsViewActionsPlugin should be null for $options %j', (options) => {
    expect(c.DeploymentsViewActionsPlugin.call({ $options: options })).toBeNull()
  })
})

describe('DeploymentsView - loading', () => {
  it('refreshTotalCount should store the reported count', async () => {
    const vm = context()
    ProcessService.findDeploymentsCount.mockResolvedValue(7)

    m.refreshTotalCount.call(vm)
    await flush()

    expect(ProcessService.findDeploymentsCount).toHaveBeenCalledWith('')
    expect(vm.totalCount).toBe(7)
  })

  it('refreshTotalCount should apply the current filter', async () => {
    const vm = context({ filter: 'invoice' })

    m.refreshTotalCount.call(vm)
    await flush()

    expect(ProcessService.findDeploymentsCount).toHaveBeenCalledWith('%invoice%')
  })

  it('refreshTotalCount should fall back to unknown when the count fails', async () => {
    const vm = context({ totalCount: 5 })
    ProcessService.findDeploymentsCount.mockRejectedValue(new Error('500'))

    m.refreshTotalCount.call(vm)
    await flush()

    expect(vm.totalCount).toBeUndefined()
  })

  it('loadDeployments should append the page and group it', async () => {
    const vm = context()
    ProcessService.findDeployments.mockResolvedValue([deployment()])

    m.loadDeployments.call(vm, 0)
    await flush()

    expect(ProcessService.findDeployments).toHaveBeenCalledWith('', 0, 50, 'deploymentTime', 'desc')
    expect(vm.deployments).toHaveLength(1)
    expect(vm.loading).toBe(false)
  })

  it('loadDeployments should stop the spinner when the request fails', async () => {
    const vm = context()
    ProcessService.findDeployments.mockRejectedValue(new Error('500'))

    m.loadDeployments.call(vm, 0)
    await flush()

    expect(vm.loading).toBe(false)
    expect(console.error).toHaveBeenCalled()
  })

  // A deep link can point at a deployment far down the list, so pages are fetched until it
  // turns up.
  it('loadDeployments should keep paging until the linked deployment is found', async () => {
    const vm = context({ deploymentId: 'd-target', searchDeployment: true, totalCount: 100 })
    ProcessService.findDeployments
      .mockResolvedValueOnce([deployment({ id: 'd1' })])
      .mockResolvedValueOnce([deployment({ id: 'd-target' })])

    m.loadDeployments.call(vm, 0)
    await flush(20)

    expect(ProcessService.findDeployments).toHaveBeenCalledTimes(2)
    expect(vm.searchDeployment).toBe(false)
    expect(vm.deploymentsReady).toBe(true)
  })

  it('loadDeployments should stop paging once the linked deployment is on the page', async () => {
    const vm = context({ deploymentId: 'd1', searchDeployment: true })
    ProcessService.findDeployments.mockResolvedValue([deployment({ id: 'd1' })])

    m.loadDeployments.call(vm, 0)
    await flush(20)

    expect(ProcessService.findDeployments).toHaveBeenCalledTimes(1)
    expect(vm.deploymentsReady).toBe(true)
  })

  it('loadNextChunk should skip a request while one is in flight', () => {
    const vm = context({ loading: true, loadDeployments: vi.fn() })

    m.loadNextChunk.call(vm, 0)

    expect(vm.loadDeployments).not.toHaveBeenCalled()
  })

  it('loadNextChunk should skip a request once everything is loaded', () => {
    const vm = context({ totalCount: 1, deployments: [deployment()], loadDeployments: vi.fn() })

    m.loadNextChunk.call(vm, 0)

    expect(vm.loadDeployments).not.toHaveBeenCalled()
  })

  it('loadNextChunk should fetch the next page otherwise', () => {
    const vm = context({ loadDeployments: vi.fn() })

    m.loadNextChunk.call(vm, 50)

    expect(vm.loading).toBe(true)
    expect(vm.loadDeployments).toHaveBeenCalledWith(50)
  })

  it('loadNextPage should refresh the count and fetch from the current offset', () => {
    const vm = context({
      deployments: [deployment()],
      refreshTotalCount: vi.fn(),
      loadNextChunk: vi.fn()
    })

    m.loadNextPage.call(vm)

    expect(vm.refreshTotalCount).toHaveBeenCalled()
    expect(vm.loadNextChunk).toHaveBeenCalledWith(1)
  })

  it('loadToSelectedDeployment should mark ready immediately when already loaded', async () => {
    const vm = context({
      deploymentId: 'd1',
      deployments: [deployment({ id: 'd1' })],
      refreshTotalCount: vi.fn(),
      loadDeployments: vi.fn()
    })

    await m.loadToSelectedDeployment.call(vm)

    expect(vm.deploymentsReady).toBe(true)
    expect(vm.searchDeployment).toBe(true)
  })

  it('loadToSelectedDeployment should start a search when not yet loaded', async () => {
    const vm = context({ deploymentId: 'd-target', refreshTotalCount: vi.fn(), loadDeployments: vi.fn() })

    await m.loadToSelectedDeployment.call(vm)

    expect(vm.deploymentsReady).toBe(false)
    expect(vm.loadDeployments).toHaveBeenCalledWith(0)
  })

  it('performSearch should discard the current list and reload', () => {
    const vm = context({
      groups: [{ name: 'x' }],
      deployments: [deployment()],
      deployment: deployment(),
      loadNextPage: vi.fn()
    })

    m.performSearch.call(vm)

    expect(vm.groups).toEqual([])
    expect(vm.deployments).toEqual([])
    expect(vm.deployment).toBeNull()
    expect(vm.loadNextPage).toHaveBeenCalled()
  })
})

describe('DeploymentsView - processDeployments', () => {
  it('should fall back to the id when a deployment has no name', () => {
    const vm = context()
    const deployments = [deployment({ name: null })]

    m.processDeployments.call(vm, deployments)

    expect(deployments[0].name).toBe('d1')
  })

  it('should reset the selection state of every deployment', () => {
    const vm = context()
    const deployments = [deployment({ isSelected: true })]

    m.processDeployments.call(vm, deployments)

    expect(deployments[0].isSelected).toBe(false)
  })

  // Sorted by time the list is grouped into day headings.
  it('should group by deployment day when sorting by time', () => {
    const vm = context({ sortBy: 'deploymentTime' })

    m.processDeployments.call(vm, [
      deployment({ id: 'd1', deploymentTime: '2026-01-15T10:00:00Z' }),
      deployment({ id: 'd2', deploymentTime: '2026-01-15T18:00:00Z' }),
      deployment({ id: 'd3', deploymentTime: '2026-01-16T09:00:00Z' })
    ])

    expect(vm.groups).toHaveLength(2)
    expect(vm.groups[0].data.map(d => d.id)).toEqual(['d1', 'd2'])
    expect(vm.groups[1].data.map(d => d.id)).toEqual(['d3'])
  })

  // Sorted by name it is grouped under an initial-letter heading instead.
  it('should group by initial letter when sorting by name', () => {
    const vm = context({ sortBy: 'name' })

    m.processDeployments.call(vm, [
      deployment({ id: 'd1', name: 'alpha' }),
      deployment({ id: 'd2', name: 'Avocado' }),
      deployment({ id: 'd3', name: 'beta' })
    ])

    expect(vm.groups.map(g => g.name)).toEqual(['A', 'B'])
  })

  it('should start every group expanded', () => {
    const vm = context()

    m.processDeployments.call(vm, [deployment()])

    expect(vm.groups[0].visible).toBe(true)
  })

  it('should select the deployment the route points at', () => {
    const vm = context({ deploymentId: 'd1', selectDeployment: vi.fn() })

    m.processDeployments.call(vm, [deployment({ id: 'd1' })])

    expect(vm.selectDeployment).toHaveBeenCalledWith(expect.objectContaining({ id: 'd1' }))
  })

  it('should append to the trailing group rather than starting a new one', () => {
    const vm = context({ sortBy: 'name', groups: [{ visible: true, name: 'A', data: [deployment({ id: 'd0' })] }] })

    m.processDeployments.call(vm, [deployment({ id: 'd1', name: 'alpha' })])

    expect(vm.groups).toHaveLength(1)
    expect(vm.groups[0].data).toHaveLength(2)
  })
})

describe('DeploymentsView - resources', () => {
  it('selectDeployment should open the sidebar and load the resources', () => {
    const vm = context({ findDeploymentResources: vi.fn() })
    const d = deployment()

    m.selectDeployment.call(vm, d)

    expect(vm.deployment).toBe(d)
    expect(vm.rightOpen).toBe(true)
    expect(vm.findDeploymentResources).toHaveBeenCalledWith('d1')
  })

  it('findDeploymentResources should load and store the resources', async () => {
    const vm = context()
    ProcessService.findDeploymentResources.mockResolvedValue([{ id: 'r1', name: 'invoice.bpmn' }])

    m.findDeploymentResources.call(vm, 'd1')
    await flush()

    expect(vm.resources).toEqual([{ id: 'r1', name: 'invoice.bpmn' }])
    expect(vm.resourcesLoading).toBe(false)
  })

  it('findDeploymentResources should clear the list when the request fails', async () => {
    const vm = context({ resources: [{ id: 'stale' }] })
    ProcessService.findDeploymentResources.mockRejectedValue(new Error('500'))

    m.findDeploymentResources.call(vm, 'd1')
    await flush()

    expect(vm.resources).toBeNull()
    expect(vm.resourcesLoading).toBe(false)
  })
})

describe('DeploymentsView - deletion', () => {
  it('deleteDeployment should remove it from the list and navigate away', async () => {
    const vm = context({
      deploymentId: 'd1',
      deployments: [deployment({ id: 'd1' }), deployment({ id: 'd2' })],
      groups: [{ name: 'g', data: [deployment({ id: 'd1' })] }]
    })

    m.deleteDeployment.call(vm)
    await flush()

    expect(ProcessService.deleteDeployment).toHaveBeenCalledWith('d1', true)
    expect(vm.deployments.map(d => d.id)).toEqual(['d2'])
    expect(vm.deployment).toBeNull()
    expect(vm.resources).toBeNull()
    expect(vm.loadProcesses).toHaveBeenCalledWith(false)
    expect(vm.$refs.deploymentsDeleted.show).toHaveBeenCalled()
    expect(vm.$router.push).toHaveBeenCalledWith({ name: 'deployments' })
  })

  // An emptied day heading has to disappear along with its last deployment.
  it('deleteDeployment should drop a group left empty', async () => {
    const vm = context({
      deploymentId: 'd1',
      deployments: [deployment({ id: 'd1' })],
      groups: [{ name: 'g', data: [deployment({ id: 'd1' })] }]
    })

    m.deleteDeployment.call(vm)
    await flush()

    expect(vm.groups).toEqual([])
  })

  it('deleteDeployment should keep a group that still has deployments', async () => {
    const vm = context({
      deploymentId: 'd1',
      deployments: [deployment({ id: 'd1' })],
      groups: [{ name: 'g', data: [deployment({ id: 'd1' }), deployment({ id: 'd2' })] }]
    })

    m.deleteDeployment.call(vm)
    await flush()

    expect(vm.groups).toHaveLength(1)
    expect(vm.groups[0].data.map(d => d.id)).toEqual(['d2'])
  })

  describe('deleteDeployments (bulk)', () => {
    it('should delete every checked deployment and report progress', async () => {
      const vm = context({
        deployments: [
          deployment({ id: 'd1', isSelected: true }),
          deployment({ id: 'd2', isSelected: true }),
          deployment({ id: 'd3' })
        ],
        groups: []
      })

      await m.deleteDeployments.call(vm)

      expect(ProcessService.deleteDeployment).toHaveBeenCalledTimes(2)
      expect(vm.deploymentsDelData).toEqual({ total: 2, deleted: 2 })
      expect(vm.deployments.map(d => d.id)).toEqual(['d3'])
      expect(vm.deleteLoader).toBe(false)
      expect(vm.$refs.deploymentsDeleted.show).toHaveBeenCalled()
    })

    // One failure must not abort the rest of the batch.
    it('should carry on after a failed deletion', async () => {
      const vm = context({
        deployments: [deployment({ id: 'd1', isSelected: true }), deployment({ id: 'd2', isSelected: true })],
        groups: []
      })
      ProcessService.deleteDeployment
        .mockRejectedValueOnce(new Error('409'))
        .mockResolvedValueOnce(undefined)

      await m.deleteDeployments.call(vm)

      expect(vm.deploymentsDelData).toEqual({ total: 2, deleted: 1 })
      expect(vm.deployments.map(d => d.id)).toEqual(['d1'])
      expect(console.error).toHaveBeenCalled()
    })

    it('should close the sidebar when the open deployment is deleted', async () => {
      const open = deployment({ id: 'd1', isSelected: true })
      const vm = context({ deployments: [open], deployment: open, groups: [] })

      await m.deleteDeployments.call(vm)

      expect(vm.deployment).toBeNull()
      expect(vm.$router.push).toHaveBeenCalledWith({ name: 'deployments' })
    })

    it('should leave the sidebar alone when a different deployment is deleted', async () => {
      const open = deployment({ id: 'd-open' })
      const vm = context({
        deployments: [deployment({ id: 'd1', isSelected: true }), open],
        deployment: open,
        groups: []
      })

      await m.deleteDeployments.call(vm)

      expect(vm.deployment).toBe(open)
    })

    it('should do nothing when nothing is checked', async () => {
      const vm = context({ deployments: [deployment()], groups: [] })

      await m.deleteDeployments.call(vm)

      expect(ProcessService.deleteDeployment).not.toHaveBeenCalled()
      expect(vm.deploymentsDelData).toEqual({ total: 0, deleted: 0 })
    })
  })
})

describe('DeploymentsView - sorting and refresh', () => {
  it('changeSortingOrder should flip the order, persist it and reload', () => {
    const vm = context({ deployments: [deployment()], groups: [{ name: 'g' }], loadNextPage: vi.fn() })

    m.changeSortingOrder.call(vm)

    expect(vm.sortOrder).toBe('asc')
    expect(localStorage.getItem('cibseven:deployments.sortOrder')).toBe('asc')
    expect(vm.deployments).toEqual([])
    expect(vm.groups).toEqual([])
    expect(vm.loadNextPage).toHaveBeenCalled()
  })

  it('changeSortingOrder should flip back on a second click', () => {
    const vm = context({ sortOrder: 'asc', loadNextPage: vi.fn() })

    m.changeSortingOrder.call(vm)

    expect(vm.sortOrder).toBe('desc')
  })

  it('onDeploymentSuccess should confirm and refresh', () => {
    const vm = context({ refreshDeployments: vi.fn() })

    m.onDeploymentSuccess.call(vm)

    expect(vm.$refs.success.show).toHaveBeenCalled()
    expect(vm.refreshDeployments).toHaveBeenCalled()
  })

  describe('refreshDeployments', () => {
    it('should prepend deployments that are not yet listed', async () => {
      const vm = context({ deployments: [deployment({ id: 'd1' })], refreshTotalCount: vi.fn() })
      ProcessService.findDeployments.mockResolvedValue([deployment({ id: 'd-new' }), deployment({ id: 'd1' })])

      m.refreshDeployments.call(vm)
      await flush()

      expect(vm.deployments.map(d => d.id)).toEqual(['d-new', 'd1'])
      expect(vm.refreshTotalCount).toHaveBeenCalled()
      expect(vm.loading).toBe(false)
    })

    it('should regroup the list after prepending', async () => {
      const vm = context({ deployments: [], refreshTotalCount: vi.fn() })
      ProcessService.findDeployments.mockResolvedValue([deployment({ id: 'd-new' })])

      m.refreshDeployments.call(vm)
      await flush()

      expect(vm.groups).toHaveLength(1)
    })

    // Polling re-fetches the newest page, so unchanged results must be a no-op.
    it('should do nothing when there is nothing new', async () => {
      const vm = context({
        deployments: [deployment({ id: 'd1' })],
        groups: [{ name: 'g', data: [] }],
        refreshTotalCount: vi.fn()
      })
      ProcessService.findDeployments.mockResolvedValue([deployment({ id: 'd1' })])

      m.refreshDeployments.call(vm)
      await flush()

      expect(vm.deployments).toHaveLength(1)
      expect(vm.groups).toHaveLength(1)
      expect(vm.refreshTotalCount).not.toHaveBeenCalled()
    })
  })
})
