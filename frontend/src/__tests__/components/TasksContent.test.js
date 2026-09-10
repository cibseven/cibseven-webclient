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
import TasksContent from '@/components/task/TasksContent.vue'
import { TaskService, ProcessService, HistoryService } from '@/services.js'

vi.mock('@/services.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    TaskService: {
      findTasksByFilter: vi.fn(() => Promise.resolve([])),
      findTasksByProcessInstanceAsignee: vi.fn(() => Promise.resolve([])),
      checkActiveTask: vi.fn(() => Promise.resolve())
    },
    ProcessService: {
      findProcessInstance: vi.fn(() => Promise.resolve({})),
      findCurrentProcessesInstances: vi.fn(() => Promise.resolve([]))
    },
    HistoryService: {
      findProcessInstance: vi.fn(() => Promise.resolve({})),
      findTasksByTaskIdHistory: vi.fn(() => Promise.resolve([])),
      findTasksByProcessInstanceHistory: vi.fn(() => Promise.resolve([]))
    }
  }
})

const { canOpenRightTask, setIntervalTaskList, listTasksWithFilter, listTasksWithFilterAuto,
  fetchTasks, updateTask, updateAssignee, completedTask, checkAndOpenTask, openTaskAutomatically,
  openTask, openTaskByTaskInstance, selectedTask, selectedFilter, showFilterAlert,
  preserveExistingVariables, updateProcessesInstances, cleanSelectedTask, cleanSelectedFilter,
  collapseNavbar, showMore, navigateRegion, handleTaskShortcut, executeTaskShortcut,
  checkActiveTask, refreshTasksNumber } = TasksContent.methods

const DEFAULT_CONFIG = {
  taskListTime: '5000',
  maxTaskResults: 20,
  automaticallyOpenTask: false,
  productNamePageTitle: 'CIB seven',
  layout: { showTaskDetailsSidebar: false, showChat: false, showStatusBar: false },
  permissions: { displayTasks: { user: ['*'] } },
  taskFilter: { advancedSearch: { filterEnabled: false } },
  taskSorting: { default: { sortBy: 'created', sortOrder: 'desc' } }
}

const taskLoader = () => ({ done: false })

/**
 * A `this` for TasksContent's methods.
 *
 * The component nests TasksNavBar, FilterNavBar and the sidebar shell, injects `isMobile`
 * and `AuthService`, and reaches into `$refs` of its children — mounting it would mean
 * standing all of that up. Its own logic is what matters here, so the refs and injections
 * are stubbed and the methods are called directly.
 */
function context(overrides = {}) {
  const vm = {
    $t: (key) => key,
    $root: { config: structuredClone(DEFAULT_CONFIG), user: { permissions: [], authToken: 'tok' } },
    $route: { params: {}, query: {}, path: '/seven/auth/tasks/f1' },
    $router: { push: vi.fn(), currentRoute: { path: '/seven/auth/tasks/f1' } },
    $store: {
      state: {
        filter: { selected: { id: 'f1', name: 'My tasks', properties: {} }, settings: {} },
        advancedSearch: { criterias: [], matchAllCriteria: true }
      },
      getters: { formatedCriteriaData: {} },
      dispatch: vi.fn(() => Promise.resolve()),
      commit: vi.fn()
    },
    $refs: {
      navbar: { $refs: { taskLoader: taskLoader(), startProcess: { show: vi.fn() } } },
      filterNavbar: { updateSelectedFilterTasksCountIfNeeded: vi.fn(), fetchFilters: vi.fn() },
      completedTask: { show: vi.fn() },
      filter: { show: vi.fn() }
    },
    isMobile: () => false,
    AuthService: { fetchAuths: vi.fn(() => Promise.resolve(['READ'])) },
    tasksByPermissions: vi.fn((_permissions, tasks) => tasks),
    TasksRightSidebar: {},
    tasks: [],
    task: null,
    assignee: null,
    processInstanceHistory: null,
    processesInstances: [],
    interval: null,
    taskResultsIndex: 20,
    tasksNavbarSize: 0,
    leftOpenTask: true,
    search: '',
    ...overrides
  }
  // Bind the component's own methods so intra-method calls resolve, unless stubbed.
  for (const [name, method] of Object.entries(TasksContent.methods)) {
    if (overrides[name] === undefined) vm[name] = method.bind(vm)
  }
  return vm
}

const flush = async (times = 8) => {
  for (let i = 0; i < times; i++) await Promise.resolve()
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  localStorage.setItem('taskSorting', JSON.stringify({ sortBy: 'created', sortOrder: 'desc' }))
})

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('TasksContent - data', () => {
  const buildData = (thisArg = { canOpenRightTask: () => true }) => TasksContent.data.call(thisArg)

  it('should open the filter sidebar by default', () => {
    expect(buildData().leftOpenFilter).toBe(true)
  })

  it('should restore the persisted filter sidebar state', () => {
    localStorage.setItem('leftOpenFilter', 'false')

    expect(buildData().leftOpenFilter).toBe(false)
  })

  it('should restore the persisted task sidebar state', () => {
    localStorage.setItem('rightOpenTask', 'true')

    expect(buildData().rightOpenTask).toBe(true)
  })

  it('should keep the task sidebar closed when the layout cannot show it', () => {
    localStorage.setItem('rightOpenTask', 'true')

    expect(buildData({ canOpenRightTask: () => false }).rightOpenTask).toBe(false)
  })

  // External mode embeds a single task, so both side panels start collapsed.
  it('should collapse both sidebars in external mode', () => {
    const original = window.location
    delete window.location
    window.location = { href: 'http://host/#/seven/auth/tasks?externalMode=true' }
    localStorage.setItem('leftOpenFilter', 'true')

    const data = buildData()

    expect(data.leftOpenFilter).toBe(false)
    expect(data.leftOpenTask).toBe(false)
    window.location = original
  })
})

describe('TasksContent - canOpenRightTask', () => {
  const layout = (overrides) => ({ showTaskDetailsSidebar: false, showChat: false, showStatusBar: false, ...overrides })

  it('should be false when the sidebar component is not registered', () => {
    const vm = context({ TasksRightSidebar: null })

    expect(canOpenRightTask.call(vm)).toBe(false)
  })

  it.each([
    ['showTaskDetailsSidebar'],
    ['showChat'],
    ['showStatusBar']
  ])('should be true when %s is enabled', (flag) => {
    const vm = context()
    vm.$root.config.layout = layout({ [flag]: true })

    expect(canOpenRightTask.call(vm)).toBe(true)
  })

  it('should be false when the layout enables none of the panels', () => {
    const vm = context()
    vm.$root.config.layout = layout()

    expect(canOpenRightTask.call(vm)).toBe(false)
  })
})

describe('TasksContent - computed', () => {
  it('rightCaptionTask should name the panel only when it can be opened', () => {
    const vm = context({ canOpenRightTask: () => true })
    expect(TasksContent.computed.rightCaptionTask.call(vm)).toBe('task.options')

    const closed = context({ canOpenRightTask: () => false })
    expect(TasksContent.computed.rightCaptionTask.call(closed)).toBeNull()
  })

  it('leftCaptionTask should show the selected filter name', () => {
    expect(TasksContent.computed.leftCaptionTask.call(context())).toBe('My tasks')
  })

  // The filter heading is suppressed while the task list is collapsed.
  it('leftCaptionFilter should only be set while the task list is open', () => {
    expect(TasksContent.computed.leftCaptionFilter.call(context({ leftOpenTask: true }))).toBe('nav-bar.filtersTitle')
    expect(TasksContent.computed.leftCaptionFilter.call(context({ leftOpenTask: false }))).toBe('')
  })

  it('getTasksNavbarSize should pick the column layout for the current size step', () => {
    const vm = context({ tasksNavbarSize: 1, tasksNavbarSizes: [[1], [2], [3]] })

    expect(TasksContent.computed.getTasksNavbarSize.call(vm)).toEqual([2])
  })

  it('TasksRightSidebar should resolve the optional sidebar component', () => {
    const component = { name: 'TasksRightSidebar' }
    const vm = { $options: { components: { TasksRightSidebar: component } } }

    expect(TasksContent.computed.TasksRightSidebar.call(vm)).toBe(component)
  })

  it.each([[{}], [{ components: {} }]])('TasksRightSidebar should be null for $options %j', (options) => {
    expect(TasksContent.computed.TasksRightSidebar.call({ $options: options })).toBeNull()
  })
})

describe('TasksContent - polling', () => {
  it('should poll the task list on the configured interval', () => {
    vi.useFakeTimers()
    const vm = context({
      listTasksWithFilterAuto: vi.fn(),
      refreshTasksNumber: vi.fn(),
      checkActiveTask: vi.fn()
    })

    setIntervalTaskList.call(vm)
    vi.advanceTimersByTime(5000)

    expect(vm.listTasksWithFilterAuto).toHaveBeenCalled()
    expect(vm.refreshTasksNumber).toHaveBeenCalled()
  })

  it('should re-check the open task on each poll', () => {
    vi.useFakeTimers()
    const vm = context({
      task: { id: 't1' },
      listTasksWithFilterAuto: vi.fn(),
      refreshTasksNumber: vi.fn(),
      checkActiveTask: vi.fn()
    })

    setIntervalTaskList.call(vm)
    vi.advanceTimersByTime(5000)

    expect(vm.checkActiveTask).toHaveBeenCalled()
  })

  // '0' disables auto-refresh entirely.
  it('should not poll when the interval is switched off', () => {
    vi.useFakeTimers()
    const vm = context({ listTasksWithFilterAuto: vi.fn() })
    vm.$root.config.taskListTime = '0'

    setIntervalTaskList.call(vm)
    vi.advanceTimersByTime(60000)

    expect(vm.listTasksWithFilterAuto).not.toHaveBeenCalled()
    expect(vm.interval).toBeNull()
  })

  it('refreshTasksNumber should ask the filter sidebar for a fresh count', () => {
    const vm = context()

    refreshTasksNumber.call(vm)

    expect(vm.$refs.filterNavbar.updateSelectedFilterTasksCountIfNeeded).toHaveBeenCalledWith(false)
  })
})

describe('TasksContent - listTasksWithFilter', () => {
  it('should reset the list and re-fetch the first page', () => {
    const vm = context({ tasks: [{ id: 'stale' }], fetchTasks: vi.fn() })

    listTasksWithFilter.call(vm)

    expect(vm.tasks).toEqual([])
    expect(vm.processesInstances).toEqual([])
    expect(vm.taskResultsIndex).toBe(20)
    expect(vm.fetchTasks).toHaveBeenCalledWith(0, 20)
  })

  it('should restart the loader spinner', () => {
    const vm = context({ fetchTasks: vi.fn() })
    vm.$refs.navbar.$refs.taskLoader.done = true

    listTasksWithFilter.call(vm)

    expect(vm.$refs.navbar.$refs.taskLoader.done).toBe(false)
  })
})

describe('TasksContent - listTasksWithFilterAuto', () => {
  const withFilter = (overrides = {}) => {
    const vm = context({ fetchTasks: vi.fn(), ...overrides })
    vm.$route.params.filterId = 'f1'
    return vm
  }

  // A refresh reloads the whole visible page; "show more" appends the next page.
  it('should reload the whole visible range on a plain refresh', () => {
    const vm = withFilter({ taskResultsIndex: 40 })

    listTasksWithFilterAuto.call(vm)

    expect(vm.fetchTasks).toHaveBeenCalledWith(0, 40, undefined, true)
  })

  it('should append the next page when showing more', () => {
    const vm = withFilter({ taskResultsIndex: 40 })

    listTasksWithFilterAuto.call(vm, true)

    expect(vm.fetchTasks).toHaveBeenCalledWith(40, 20, true, false)
    expect(vm.$refs.navbar.$refs.taskLoader.done).toBe(false)
  })

  it('should not fetch when no filter is selected in the store', () => {
    const vm = withFilter()
    vm.$store.state.filter.selected.id = null

    listTasksWithFilterAuto.call(vm)

    expect(vm.fetchTasks).not.toHaveBeenCalled()
  })

  it('should stop the spinner when the route has no filter', () => {
    const vm = context({ fetchTasks: vi.fn() })

    listTasksWithFilterAuto.call(vm)

    expect(vm.fetchTasks).not.toHaveBeenCalled()
    expect(vm.$refs.navbar.$refs.taskLoader.done).toBe(true)
  })

  it('should tolerate the navbar not being rendered yet', () => {
    const vm = context({ fetchTasks: vi.fn() })
    vm.$refs.navbar = null

    expect(() => listTasksWithFilterAuto.call(vm)).not.toThrow()
  })
})

describe('TasksContent - fetchTasks', () => {
  const body = () => TaskService.findTasksByFilter.mock.calls[0][1]

  it('should send the persisted sorting', async () => {
    localStorage.setItem('taskSorting', JSON.stringify({ sortBy: 'created', sortOrder: 'asc' }))
    const vm = context()

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(body().sorting).toEqual([{ sortBy: 'created', sortOrder: 'asc' }])
  })

  // Sorting by anything else still needs a stable tie-breaker, or paging jumps around.
  it('should add a created tie-breaker when sorting by another field', async () => {
    localStorage.setItem('taskSorting', JSON.stringify({ sortBy: 'name', sortOrder: 'asc' }))
    const vm = context()

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(body().sorting).toEqual([
      { sortBy: 'name', sortOrder: 'asc' },
      { sortBy: 'created', sortOrder: 'desc' }
    ])
  })

  it('should turn the search box into an OR query across the searchable fields', async () => {
    const vm = context({ search: 'invoice urgent' })

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(body().orQueries).toEqual([
      { nameLike: '%invoice%', assigneeLike: '%invoice%', processDefinitionId: 'invoice', processInstanceBusinessKeyLike: '%invoice%' },
      { nameLike: '%urgent%', assigneeLike: '%urgent%', processDefinitionId: 'urgent', processInstanceBusinessKeyLike: '%urgent%' }
    ])
  })

  it('should not add an OR query when the search box is empty', async () => {
    const vm = context({ search: '' })

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(body()).not.toHaveProperty('orQueries')
  })

  it('should refresh the advanced search criteria when that feature is on', async () => {
    const vm = context()
    vm.$root.config.taskFilter.advancedSearch.filterEnabled = true

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(vm.$store.dispatch).toHaveBeenCalledWith('loadAdvancedSearchData')
  })

  it('should refresh the user permissions before querying', async () => {
    const vm = context()

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(vm.AuthService.fetchAuths).toHaveBeenCalled()
    expect(vm.$root.user.permissions).toEqual(['READ'])
  })

  // "Match all" folds the criteria into the top-level filter (AND); otherwise they go into
  // an orQueries entry.
  it('should merge advanced criteria into the filter when matching all', async () => {
    const vm = context()
    vm.$store.state.advancedSearch = { criterias: [{ key: 'assignee' }], matchAllCriteria: true }
    vm.$store.getters.formatedCriteriaData = { assignee: [{ operator: 'eq', value: 'demo' }] }

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(body().assignee).toEqual([{ operator: 'eq', value: 'demo' }])
  })

  it('should push advanced criteria into an OR query when matching any', async () => {
    const vm = context()
    vm.$store.state.advancedSearch = { criterias: [{ key: 'assignee' }], matchAllCriteria: false }
    vm.$store.getters.formatedCriteriaData = { assignee: [{ operator: 'eq', value: 'demo' }] }

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(body().orQueries).toEqual([{ assignee: [{ operator: 'eq', value: 'demo' }] }])
  })

  it('should append advanced criteria to an existing search OR query', async () => {
    const vm = context({ search: 'invoice' })
    vm.$store.state.advancedSearch = { criterias: [{ key: 'assignee' }], matchAllCriteria: false }
    vm.$store.getters.formatedCriteriaData = { assignee: [] }

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(body().orQueries).toHaveLength(2)
  })

  // Variable comparisons must be case-insensitive to match what the UI shows.
  it('should ask for case-insensitive variable matching when filtering on process variables', async () => {
    const vm = context()
    vm.$store.state.advancedSearch = { criterias: [{ key: 'processVariables' }], matchAllCriteria: true }
    vm.$store.getters.formatedCriteriaData = { processVariables: [{ name: 'amount' }] }

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(body().variableValuesIgnoreCase) .toBe(true)
  })

  it('should ask for case-insensitive variable matching when an OR query filters on them', async () => {
    const vm = context()
    vm.$store.state.advancedSearch = { criterias: [{ key: 'x' }], matchAllCriteria: false }
    vm.$store.getters.formatedCriteriaData = { processVariables: [{ name: 'amount' }] }

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(body().variableValuesIgnoreCase).toBe(true)
  })

  it('should request the variables the filter displays', async () => {
    const vm = context()
    vm.$store.state.filter.selected.properties = { variables: [{ name: 'amount' }, { name: 'customer' }] }

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(body().variableNames).toEqual(['amount', 'customer'])
  })

  // A background refresh keeps the variables already loaded rather than re-fetching them.
  it('should skip variable names on a background refresh', async () => {
    const vm = context()
    vm.$store.state.filter.selected.properties = { variables: [{ name: 'amount' }] }

    fetchTasks.call(vm, 0, 20, false, true)
    await flush()

    expect(body()).not.toHaveProperty('variableNames')
  })

  it('should query with the selected filter id and the requested page', async () => {
    const vm = context()

    fetchTasks.call(vm, 20, 10)
    await flush()

    expect(TaskService.findTasksByFilter).toHaveBeenCalledWith('f1', expect.any(Object), {
      firstResult: 20, maxResults: 10
    })
  })

  it('should filter the result by the display-tasks permission', async () => {
    const vm = context({ updateProcessesInstances: vi.fn() })
    TaskService.findTasksByFilter.mockResolvedValue([{ id: 't1' }])

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(vm.tasksByPermissions).toHaveBeenCalledWith({ user: ['*'] }, [{ id: 't1' }])
    expect(vm.updateProcessesInstances).toHaveBeenCalledWith([{ id: 't1' }], undefined)
  })

  it('should preserve already loaded variables on a background refresh', async () => {
    const vm = context({ updateProcessesInstances: vi.fn(), preserveExistingVariables: vi.fn() })
    TaskService.findTasksByFilter.mockResolvedValue([{ id: 't1' }])

    fetchTasks.call(vm, 0, 20, false, true)
    await flush()

    expect(vm.preserveExistingVariables).toHaveBeenCalledWith([{ id: 't1' }])
  })

  it('should stop the spinner when the query fails', async () => {
    const vm = context()
    TaskService.findTasksByFilter.mockRejectedValue(new Error('500'))

    fetchTasks.call(vm, 0, 20)
    await flush()

    expect(vm.$refs.navbar.$refs.taskLoader.done).toBe(true)
  })
})

describe('TasksContent - preserveExistingVariables', () => {
  // Re-fetched tasks arrive without variables on a background refresh; carrying the old
  // values over stops the variable columns from blinking empty.
  it('should carry variables over from the task already displayed', () => {
    const vm = context({
      tasks: [{ id: 't1', variables: [{ name: 'amount' }], variableTypes: { amount: 'Integer' } }]
    })
    const incoming = [{ id: 't1' }]

    preserveExistingVariables.call(vm, incoming)

    expect(incoming[0].variables).toEqual([{ name: 'amount' }])
    expect(incoming[0].variableTypes).toEqual({ amount: 'Integer' })
  })

  it('should leave a task that was not previously displayed alone', () => {
    const vm = context({ tasks: [] })
    const incoming = [{ id: 't-new' }]

    preserveExistingVariables.call(vm, incoming)

    expect(incoming[0]).toEqual({ id: 't-new' })
  })
})

describe('TasksContent - updateProcessesInstances', () => {
  it('should decorate tasks with the business key of their instance', async () => {
    const vm = context()
    ProcessService.findCurrentProcessesInstances.mockResolvedValue([{ id: 'pi1', businessKey: 'ORDER-1' }])
    const tasks = [{ id: 't1', processInstanceId: 'pi1' }]

    updateProcessesInstances.call(vm, tasks)
    await flush()

    expect(ProcessService.findCurrentProcessesInstances).toHaveBeenCalledWith({ processInstanceIds: ['pi1'] })
    expect(vm.tasks[0].businessKey).toBe('ORDER-1')
  })

  it('should leave a task without a matching instance undecorated', async () => {
    const vm = context()
    ProcessService.findCurrentProcessesInstances.mockResolvedValue([])

    updateProcessesInstances.call(vm, [{ id: 't1', processInstanceId: 'pi-gone' }])
    await flush()

    expect(vm.tasks[0].businessKey).toBeUndefined()
  })

  it('should append to the existing list when showing more', async () => {
    const vm = context({ tasks: [{ id: 't0' }] })
    ProcessService.findCurrentProcessesInstances.mockResolvedValue([])

    updateProcessesInstances.call(vm, [{ id: 't1', processInstanceId: 'pi1' }], true)
    await flush()

    expect(vm.tasks.map(t => t.id)).toEqual(['t0', 't1'])
  })

  // The business keys are a nicety; failing to load them must not lose the task list.
  it('should still show the tasks when the instance lookup fails', async () => {
    const vm = context()
    ProcessService.findCurrentProcessesInstances.mockRejectedValue(new Error('500'))

    updateProcessesInstances.call(vm, [{ id: 't1', processInstanceId: 'pi1' }])
    await flush()

    expect(vm.tasks.map(t => t.id)).toEqual(['t1'])
    expect(vm.$refs.navbar.$refs.taskLoader.done).toBe(true)
  })

  it.each([[[]], [null]])('should short-circuit for a result of %j', async (tasks) => {
    const vm = context({ tasks: [{ id: 't0' }] })

    updateProcessesInstances.call(vm, tasks)
    await flush()

    expect(ProcessService.findCurrentProcessesInstances).not.toHaveBeenCalled()
    expect(vm.$refs.navbar.$refs.taskLoader.done).toBe(true)
  })
})

describe('TasksContent - selection', () => {
  it('selectedTask should adopt the task and its assignee', () => {
    const vm = context()

    selectedTask.call(vm, { id: 't1', assignee: 'demo', name: 'Approve', processInstanceId: 'pi1' })

    expect(vm.task).toMatchObject({ id: 't1' })
    expect(vm.assignee).toBe('demo')
  })

  it('selectedTask should set the assignee to null for an unassigned task', () => {
    const vm = context()

    selectedTask.call(vm, { id: 't1', name: 'Approve' })

    expect(vm.assignee).toBeNull()
  })

  it('selectedTask should put the task name in the document title', () => {
    const vm = context()

    selectedTask.call(vm, { id: 't1', name: 'Approve invoice' })

    expect(document.title).toBe('CIB seven | start.taskList.title | Approve invoice')
  })

  it('selectedTask should fall back to the product name translation', () => {
    const vm = context()
    vm.$root.config.productNamePageTitle = null

    selectedTask.call(vm, { id: 't1', name: 'Approve' })

    expect(document.title).toContain('login.productName')
  })

  // On a phone the list and the task cannot both be on screen.
  it('selectedTask should collapse the task list on mobile', () => {
    const vm = context({ isMobile: () => true })

    selectedTask.call(vm, { id: 't1', name: 'Approve' })

    expect(vm.leftOpenTask).toBe(false)
  })

  it('selectedTask should load the instance history only when the details sidebar is shown', async () => {
    const vm = context()
    vm.$root.config.layout.showTaskDetailsSidebar = true
    ProcessService.findProcessInstance.mockResolvedValue({ id: 'pi1' })
    HistoryService.findTasksByProcessInstanceHistory.mockResolvedValue([{ id: 't1' }])

    selectedTask.call(vm, { id: 't1', name: 'Approve', processInstanceId: 'pi1' })
    await flush()

    expect(vm.processInstanceHistory).toEqual({ id: 'pi1', tasksHistory: [{ id: 't1' }] })
  })

  it('selectedTask should not load the instance history when the sidebar is hidden', async () => {
    const vm = context()

    selectedTask.call(vm, { id: 't1', name: 'Approve', processInstanceId: 'pi1' })
    await flush()

    expect(ProcessService.findProcessInstance).not.toHaveBeenCalled()
  })

  it('selectedFilter should reload the task list', () => {
    const vm = context({ listTasksWithFilter: vi.fn() })

    selectedFilter.call(vm)

    expect(vm.listTasksWithFilter).toHaveBeenCalled()
  })

  it('cleanSelectedTask should clear the task and return to the filter route', () => {
    const vm = context({ task: { id: 't1' }, processInstanceHistory: {} })
    vm.$route.params.filterId = 'f1'
    vm.$route.path = '/seven/auth/tasks/f1/t1'

    cleanSelectedTask.call(vm)

    expect(vm.task).toBeNull()
    expect(vm.processInstanceHistory).toBeNull()
    expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f1')
  })

  it('cleanSelectedTask should not navigate when already on the filter route', () => {
    const vm = context({ task: { id: 't1' } })
    vm.$route.params.filterId = 'f1'
    vm.$route.path = '/seven/auth/tasks/f1'

    cleanSelectedTask.call(vm)

    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  it('cleanSelectedTask should not navigate when there is no filter in the route', () => {
    const vm = context({ task: { id: 't1' } })

    cleanSelectedTask.call(vm)

    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  it('cleanSelectedFilter should reset the selection to a blank Task filter', () => {
    const vm = context({ tasks: [{ id: 't1' }], cleanSelectedTask: vi.fn() })

    cleanSelectedFilter.call(vm)

    expect(vm.$store.state.filter.selected).toMatchObject({ id: null, resourceType: 'Task', name: '' })
    expect(vm.tasks).toEqual([])
    expect(vm.cleanSelectedTask).toHaveBeenCalled()
  })
})

describe('TasksContent - task lifecycle', () => {
  it('updateTask should replace the task in the list and refresh', () => {
    const vm = context({
      tasks: [{ id: 't1', due: 'old' }],
      processInstanceHistory: { tasksHistory: [{ id: 't1', due: 'old' }] },
      listTasksWithFilterAuto: vi.fn()
    })

    updateTask.call(vm, { id: 't1', due: 'new' })

    expect(vm.tasks[0].due).toBe('new')
    expect(vm.processInstanceHistory.tasksHistory[0].due).toBe('new')
    expect(vm.listTasksWithFilterAuto).toHaveBeenCalled()
  })

  it('completedTask should drop the task, notify and refresh', () => {
    const vm = context({
      task: { id: 't1' },
      tasks: [{ id: 't1' }, { id: 't2' }],
      listTasksWithFilterAuto: vi.fn(),
      checkAndOpenTask: vi.fn()
    })

    completedTask.call(vm, { id: 't1' })

    expect(vm.tasks.map(t => t.id)).toEqual(['t2'])
    expect(vm.$refs.completedTask.show).toHaveBeenCalledWith(2)
    expect(vm.task).toBeNull()
    expect(vm.assignee).toBeNull()
    expect(vm.$refs.filterNavbar.fetchFilters).toHaveBeenCalled()
    expect(vm.listTasksWithFilterAuto).toHaveBeenCalled()
  })

  it('showFilterAlert should surface the filter message', () => {
    const vm = context()

    showFilterAlert.call(vm, { message: 'saved', filter: 'My tasks' })

    expect(vm.filterMessage).toBe('saved')
    expect(vm.filterName).toBe('My tasks')
    expect(vm.$refs.filter.show).toHaveBeenCalledWith(2)
  })

  it('checkAndOpenTask should only follow up when configured to', () => {
    const enabled = context({ openTaskAutomatically: vi.fn() })
    enabled.$root.config.automaticallyOpenTask = true
    checkAndOpenTask.call(enabled, { id: 't1' })
    expect(enabled.openTaskAutomatically).toHaveBeenCalled()

    const disabled = context({ openTaskAutomatically: vi.fn() })
    checkAndOpenTask.call(disabled, { id: 't1' })
    expect(disabled.openTaskAutomatically).not.toHaveBeenCalled()
  })
})

describe('TasksContent - updateAssignee', () => {
  it('should update the open task', () => {
    const vm = context({ task: { id: 't1', assignee: null }, listTasksWithFilterAuto: vi.fn() })

    updateAssignee.call(vm, { taskId: 't1', assignee: 'demo' })

    expect(vm.assignee).toBe('demo')
    expect(vm.task.assignee).toBe('demo')
  })

  it('should ignore an update for a different task', () => {
    const vm = context({ task: { id: 't1', assignee: null }, listTasksWithFilterAuto: vi.fn() })

    updateAssignee.call(vm, { taskId: 't2', assignee: 'demo' })

    expect(vm.task.assignee).toBeNull()
  })

  it('should update the history entry and reselect it', () => {
    const vm = context({
      processInstanceHistory: { tasksHistory: [{ id: 't1', assignee: null }] },
      listTasksWithFilterAuto: vi.fn(),
      selectedTask: vi.fn()
    })

    updateAssignee.call(vm, { taskId: 't1', assignee: 'demo' })

    expect(vm.processInstanceHistory.tasksHistory[0].assignee).toBe('demo')
    expect(vm.selectedTask).toHaveBeenCalled()
  })

  // Assigning from the list row must update that row, not just the open task.
  it('should update the list row when the change came from the task list', () => {
    const vm = context({ tasks: [{ id: 't1', assignee: null }], listTasksWithFilterAuto: vi.fn() })

    updateAssignee.call(vm, { taskId: 't1', assignee: 'demo' }, 'taskList')

    expect(vm.tasks[0].assignee).toBe('demo')
  })

  it('should ignore an unknown list row', () => {
    const vm = context({ tasks: [{ id: 't1' }], listTasksWithFilterAuto: vi.fn() })

    expect(() => updateAssignee.call(vm, { taskId: 't-gone', assignee: 'demo' }, 'taskList')).not.toThrow()
  })

  it.each([[null], [undefined], [{}]])('should treat the payload %j as unassigning', (payload) => {
    const vm = context({ task: { id: 't1', assignee: 'demo' }, listTasksWithFilterAuto: vi.fn() })

    updateAssignee.call(vm, payload)

    expect(vm.task.assignee).toBe('demo')
  })

  it('should always refresh the list', () => {
    const vm = context({ listTasksWithFilterAuto: vi.fn() })

    updateAssignee.call(vm, { taskId: 't1', assignee: 'demo' })

    expect(vm.listTasksWithFilterAuto).toHaveBeenCalled()
  })
})

describe('TasksContent - sidebar and navigation', () => {
  it('collapseNavbar should shrink the navbar before hiding the list', () => {
    const shrinking = context({ tasksNavbarSize: 2 })
    collapseNavbar.call(shrinking)
    expect(shrinking.tasksNavbarSize).toBe(1)
    expect(shrinking.leftOpenTask).toBe(true)

    const collapsing = context({ tasksNavbarSize: 0 })
    collapseNavbar.call(collapsing)
    expect(collapsing.leftOpenTask).toBe(false)
  })

  it('showMore should append a page and advance the offset', () => {
    const vm = context({ taskResultsIndex: 20, listTasksWithFilterAuto: vi.fn() })

    showMore.call(vm)

    expect(vm.listTasksWithFilterAuto).toHaveBeenCalledWith(true)
    expect(vm.taskResultsIndex).toBe(40)
  })

  it.each([['regionFilter'], ['regionTasks']])('navigateRegion should focus the %s sidebar', (region) => {
    const focus = vi.fn()
    const vm = context()
    vm.$refs[region] = { $refs: { leftSidebar: { focus } } }

    navigateRegion.call(vm, region)

    expect(focus).toHaveBeenCalled()
  })

  it('navigateRegion should focus the task title for any other region', () => {
    const focus = vi.fn()
    const vm = context()
    vm.$refs.taskComponent = { $refs: { task: { $refs: { titleTask: { focus } } } } }

    navigateRegion.call(vm, 'regionTask')

    expect(focus).toHaveBeenCalled()
  })

  it('navigateRegion should tolerate the task pane not being rendered', () => {
    const vm = context()

    expect(() => navigateRegion.call(vm, 'regionTask')).not.toThrow()
  })
})

describe('TasksContent - keyboard shortcuts', () => {
  it('should run the shortcut and swallow the key press on a match', () => {
    const vm = context({ executeTaskShortcut: vi.fn() })
    const event = { preventDefault: vi.fn(), key: 'f', ctrlKey: true, altKey: false, shiftKey: false, metaKey: false }

    handleTaskShortcut.call(vm, event, { keys: ['ctrl', 'f'], event: 'focusFilters' })

    expect(event.preventDefault).toHaveBeenCalled()
    expect(vm.executeTaskShortcut).toHaveBeenCalledWith('focusFilters')
  })

  it('should ignore a key press that does not match', () => {
    const vm = context({ executeTaskShortcut: vi.fn() })
    const event = { preventDefault: vi.fn(), key: 'x', ctrlKey: false, altKey: false, shiftKey: false, metaKey: false }

    handleTaskShortcut.call(vm, event, { keys: ['ctrl', 'f'], event: 'focusFilters' })

    expect(event.preventDefault).not.toHaveBeenCalled()
    expect(vm.executeTaskShortcut).not.toHaveBeenCalled()
  })

  it.each([
    ['focusFilters', 'regionFilter'],
    ['focusTasks', 'regionTasks'],
    ['focusTask', 'regionTask']
  ])('%s should focus the %s region', (eventName, region) => {
    const vm = context({ navigateRegion: vi.fn() })

    executeTaskShortcut.call(vm, eventName)

    expect(vm.navigateRegion).toHaveBeenCalledWith(region)
  })

  it('openStartProcess should open the start-process dialog', () => {
    const vm = context()

    executeTaskShortcut.call(vm, 'openStartProcess')

    expect(vm.$refs.navbar.$refs.startProcess.show).toHaveBeenCalled()
  })

  it('openStartProcess should tolerate the navbar not being rendered', () => {
    const vm = context()
    vm.$refs.navbar = null

    expect(() => executeTaskShortcut.call(vm, 'openStartProcess')).not.toThrow()
  })

  it('claimTask should claim the open task', () => {
    const claimCurrentTask = vi.fn()
    const vm = context()
    vm.$refs.taskComponent = { $refs: { task: { claimCurrentTask } } }

    executeTaskShortcut.call(vm, 'claimTask')

    expect(claimCurrentTask).toHaveBeenCalled()
  })

  it('claimTask should tolerate no task being open', () => {
    const vm = context()

    expect(() => executeTaskShortcut.call(vm, 'claimTask')).not.toThrow()
  })

  it('should warn about an unrecognised shortcut', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})

    executeTaskShortcut.call(context(), 'somethingElse')

    expect(warn).toHaveBeenCalledWith('Unknown task shortcut event:', 'somethingElse')
  })
})

describe('TasksContent - checkActiveTask', () => {
  it('should do nothing when no task is open', () => {
    checkActiveTask.call(context())

    expect(TaskService.checkActiveTask).not.toHaveBeenCalled()
  })

  it('should do nothing while the open task is still in the list', () => {
    const vm = context({ task: { id: 't1' }, tasks: [{ id: 't1' }] })

    checkActiveTask.call(vm)

    expect(TaskService.checkActiveTask).not.toHaveBeenCalled()
  })

  // A task that has vanished from the list may simply have moved out of the filter, so the
  // engine is asked whether it still exists before the user is navigated away.
  it('should verify a task that disappeared from the list', () => {
    vi.useFakeTimers()
    const vm = context({ task: { id: 't1' }, tasks: [], interval: 99 })
    const clearIntervalSpy = vi.spyOn(globalThis, 'clearInterval')

    checkActiveTask.call(vm)

    expect(clearIntervalSpy).toHaveBeenCalledWith(99)
    expect(TaskService.checkActiveTask).toHaveBeenCalledWith('t1', 'tok')
  })

  it('should navigate back to the filter and resume polling once the task is gone', async () => {
    const vm = context({ task: { id: 't1' }, tasks: [], setIntervalTaskList: vi.fn() })
    vm.$route.params = { taskId: 't1', filterId: 'f1' }
    vm.$router.currentRoute = { path: '/seven/auth/tasks/f1/t1' }
    TaskService.checkActiveTask.mockRejectedValue(new Error('404'))

    checkActiveTask.call(vm)
    await flush()

    expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f1')
    expect(vm.setIntervalTaskList).toHaveBeenCalled()
  })

  it('should not navigate when the user already moved to another task', async () => {
    const vm = context({ task: { id: 't1' }, tasks: [], setIntervalTaskList: vi.fn() })
    vm.$route.params = { taskId: 't2', filterId: 'f1' }
    TaskService.checkActiveTask.mockRejectedValue(new Error('404'))

    checkActiveTask.call(vm)
    await flush()

    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  it('should not navigate when already on the filter route', async () => {
    const vm = context({ task: { id: 't1' }, tasks: [], setIntervalTaskList: vi.fn() })
    vm.$route.params = { taskId: 't1', filterId: 'f1' }
    vm.$router.currentRoute = { path: '/seven/auth/tasks/f1' }
    TaskService.checkActiveTask.mockRejectedValue(new Error('404'))

    checkActiveTask.call(vm)
    await flush()

    expect(vm.$router.push).not.toHaveBeenCalled()
    expect(vm.setIntervalTaskList).toHaveBeenCalled()
  })
})

describe('TasksContent - automatic task opening', () => {
  it('should look up the started instance when a process was just started', async () => {
    vi.useFakeTimers()
    const vm = context({ openTask: vi.fn() })
    HistoryService.findProcessInstance.mockResolvedValue({ id: 'pi1' })

    openTaskAutomatically.call(vm, { processInstanceId: 'pi1' }, true)
    await flush()

    expect(HistoryService.findProcessInstance).toHaveBeenCalledWith('pi1')
    expect(vm.openTask).toHaveBeenCalled()
  })

  it('should look up the completed task otherwise', async () => {
    vi.useFakeTimers()
    const vm = context({ openTask: vi.fn() })
    HistoryService.findTasksByTaskIdHistory.mockResolvedValue([{ id: 't1' }])

    openTaskAutomatically.call(vm, { id: 't1' }, false)
    await flush()

    expect(HistoryService.findTasksByTaskIdHistory).toHaveBeenCalledWith('t1')
    expect(vm.openTask).toHaveBeenCalled()
  })

  it('should do nothing when the lookup finds nothing', async () => {
    vi.useFakeTimers()
    const vm = context({ openTask: vi.fn() })
    HistoryService.findTasksByTaskIdHistory.mockResolvedValue([])

    openTaskAutomatically.call(vm, { id: 't1' }, false)
    await flush()

    expect(vm.openTask).not.toHaveBeenCalled()
  })

  // The follow-up task may not exist yet, so it is retried a bounded number of times.
  it('should retry a bounded number of times', async () => {
    vi.useFakeTimers()
    const vm = context({ openTask: vi.fn() })
    HistoryService.findTasksByTaskIdHistory.mockResolvedValue([{ id: 't1' }])

    openTaskAutomatically.call(vm, { id: 't1' }, false)
    await flush()
    await vi.advanceTimersByTimeAsync(2500 * 5)

    expect(vm.openTask.mock.calls.length).toBeLessThanOrEqual(4)
  })

  describe('openTask', () => {
    it('should navigate to a task belonging to the same root instance', async () => {
      const vm = context()
      TaskService.findTasksByProcessInstanceAsignee.mockResolvedValue([
        { id: 't-new', processInstanceId: 'root-1' }
      ])

      openTask.call(vm, { rootProcessInstanceId: 'root-1', startTime: '2026-01-01T00:00:00Z' }, 42)
      await flush()

      expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f1/t-new')
    })

    // The query window is nudged back a few seconds to allow for clock skew.
    it('should query slightly before the recorded time', async () => {
      const vm = context()

      openTask.call(vm, { endTime: '2026-01-01T00:00:10Z' }, 42)
      await flush()

      const [, createdAfter] = TaskService.findTasksByProcessInstanceAsignee.mock.calls[0]
      expect(createdAfter).toContain('2026-01-01T')
    })

    it('should query without a lower bound when there is no timestamp', async () => {
      const vm = context()

      openTask.call(vm, {}, 42)
      await flush()

      expect(TaskService.findTasksByProcessInstanceAsignee).toHaveBeenCalledWith(null, undefined)
    })

    it('should fall back to walking the candidate tasks', async () => {
      const vm = context({ openTaskByTaskInstance: vi.fn() })
      TaskService.findTasksByProcessInstanceAsignee.mockResolvedValue([
        { id: 't-other', processInstanceId: 'other' }
      ])

      openTask.call(vm, { rootProcessInstanceId: 'root-1' }, 42)
      await flush()

      expect(vm.openTaskByTaskInstance).toHaveBeenCalled()
    })

    it('should do nothing when no candidate task exists yet', async () => {
      const vm = context({ openTaskByTaskInstance: vi.fn() })
      TaskService.findTasksByProcessInstanceAsignee.mockResolvedValue([])

      openTask.call(vm, {}, 42)
      await flush()

      expect(vm.openTaskByTaskInstance).not.toHaveBeenCalled()
      expect(vm.$router.push).not.toHaveBeenCalled()
    })
  })

  describe('openTaskByTaskInstance', () => {
    it('should navigate once a candidate shares the root instance', async () => {
      const vm = context()
      HistoryService.findTasksByTaskIdHistory.mockResolvedValue([{ id: 't1', rootProcessInstanceId: 'root-1' }])

      openTaskByTaskInstance.call(vm, [], { id: 't1' }, { rootProcessInstanceId: 'root-1' }, 42)
      await flush()

      expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f1/t1')
    })

    it('should walk on to the next candidate when the root instance differs', async () => {
      const vm = context()
      HistoryService.findTasksByTaskIdHistory
        .mockResolvedValueOnce([{ id: 't1', rootProcessInstanceId: 'other' }])
        .mockResolvedValueOnce([{ id: 't2', rootProcessInstanceId: 'root-1' }])

      openTaskByTaskInstance.call(vm, [{ id: 't2' }], { id: 't1' }, { rootProcessInstanceId: 'root-1' }, 42)
      await flush(12)

      expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f1/t2')
    })

    it('should walk on when a candidate has no history entry', async () => {
      const vm = context()
      HistoryService.findTasksByTaskIdHistory
        .mockResolvedValueOnce([])
        .mockResolvedValueOnce([{ id: 't2', rootProcessInstanceId: 'root-1' }])

      openTaskByTaskInstance.call(vm, [{ id: 't2' }], { id: 't1' }, { rootProcessInstanceId: 'root-1' }, 42)
      await flush(12)

      expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f1/t2')
    })

    it('should stop when the candidate list is exhausted', async () => {
      const vm = context()

      openTaskByTaskInstance.call(vm, [], undefined, { rootProcessInstanceId: 'root-1' }, 42)
      await flush()

      expect(HistoryService.findTasksByTaskIdHistory).not.toHaveBeenCalled()
      expect(vm.$router.push).not.toHaveBeenCalled()
    })
  })
})
