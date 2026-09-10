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
import TasksNavBar from '@/components/task/TasksNavBar.vue'
import { TaskService, AdminService } from '@/services.js'

vi.mock('@/services.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    TaskService: {
      findTaskById: vi.fn(() => Promise.resolve({})),
      findIdentityLinks: vi.fn(() => Promise.resolve([])),
      setAssignee: vi.fn(() => Promise.resolve()),
      update: vi.fn(() => Promise.resolve())
    },
    AdminService: { findUsers: vi.fn(() => Promise.resolve([])) }
  }
})

const m = TasksNavBar.methods

const DEFAULT_CONFIG = {
  warnOnDueExpirationIn: 24,
  pauseButtonTime: 1000,
  layout: { showProcessName: true, showFilterReminderDate: true, showFilterDueDate: true },
  taskFilter: { advancedSearch: { filterEnabled: false, processVariables: [] } },
  taskSorting: { fields: ['created', 'followUpDate', 'dueDate'] }
}

/**
 * A `this` for TasksNavBar's methods. The component renders the task list rows plus the
 * start-process and advanced-search dialogs; only its own logic is under test here, so the
 * store, route and child refs are stubbed.
 */
function context(overrides = {}) {
  const vm = {
    $t: (key) => key,
    $emit: vi.fn(),
    $nextTick: (fn) => (fn ? Promise.resolve().then(fn) : Promise.resolve()),
    $root: {
      config: structuredClone(DEFAULT_CONFIG),
      user: { id: 'demo', userID: 'demo' },
      $refs: { error: { show: vi.fn() } }
    },
    $route: { params: { filterId: 'f1' } },
    $router: { push: vi.fn(), currentRoute: { path: '/seven/auth/tasks/f1' } },
    $store: {
      state: {
        filter: { selected: { id: 'f1', properties: {} }, settings: { reminder: false, dueDate: false } },
        advancedSearch: { criterias: [], matchAllCriteria: true },
        user: { listCandidates: [] },
        process: { list: [] }
      },
      dispatch: vi.fn(() => Promise.resolve())
    },
    $refs: {
      confirmTaskAssign: { show: vi.fn() },
      due: { hide: vi.fn() },
      followUp: { hide: vi.fn() }
    },
    isMobile: () => false,
    tasks: [],
    taskResultsIndex: 20,
    focused: null,
    selectedDateT: {},
    expandedTasks: {},
    advancedFilter: [],
    advancedFilterAux: null,
    taskSorting: { sortBy: 'created', sortOrder: 'desc' },
    pauseRefreshButton: false,
    justSelectedFromList: false,
    pendingScrollToTaskId: null,
    filterVariables: [],
    showUndefinedVariable: false,
    ...overrides
  }
  for (const [name, method] of Object.entries(m)) {
    if (typeof method === 'function' && overrides[name] === undefined) vm[name] = method.bind(vm)
  }
  return vm
}

const flush = async (times = 8) => {
  for (let i = 0; i < times; i++) await Promise.resolve()
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
})

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('TasksNavBar - computed', () => {
  describe('tasksFiltered', () => {
    const task = (overrides = {}) => ({ id: 't1', due: null, followUp: null, ...overrides })

    // With neither date filter active the list is shown as-is.
    it('should return every task when no date filter is set', () => {
      const vm = context({ tasks: [task(), task({ id: 't2' })] })

      expect(TasksNavBar.computed.tasksFiltered.call(vm)).toHaveLength(2)
    })

    it('should keep only tasks with a reminder when filtering on reminders', () => {
      const vm = context({ tasks: [task({ followUp: '2026-01-01' }), task({ id: 't2' })] })
      vm.$store.state.filter.settings = { reminder: true, dueDate: false }

      expect(TasksNavBar.computed.tasksFiltered.call(vm).map(t => t.id)).toEqual(['t1'])
    })

    it('should keep only tasks with a due date when filtering on due dates', () => {
      const vm = context({ tasks: [task({ due: '2026-01-01' }), task({ id: 't2' })] })
      vm.$store.state.filter.settings = { reminder: false, dueDate: true }

      expect(TasksNavBar.computed.tasksFiltered.call(vm).map(t => t.id)).toEqual(['t1'])
    })

    // Both filters together are an OR: a task needs either date.
    it('should keep tasks carrying either date when both filters are set', () => {
      const vm = context({
        tasks: [task({ due: '2026-01-01' }), task({ id: 't2', followUp: '2026-01-01' }), task({ id: 't3' })]
      })
      vm.$store.state.filter.settings = { reminder: true, dueDate: true }

      expect(TasksNavBar.computed.tasksFiltered.call(vm).map(t => t.id)).toEqual(['t1', 't2'])
    })

    it('should return an empty list when there are no tasks', () => {
      expect(TasksNavBar.computed.tasksFiltered.call(context({ tasks: null }))).toEqual([])
    })
  })

  it('filteredFields should hide the date columns the layout switches off', () => {
    const vm = context()
    vm.$root.config.layout.showFilterReminderDate = false
    vm.$root.config.layout.showFilterDueDate = true

    expect(TasksNavBar.computed.filteredFields.call(vm)).toEqual(['created', 'dueDate'])
  })

  it('filterVariables should expose the variables the filter declares', () => {
    const vm = context()
    vm.$store.state.filter.selected.properties = { variables: [{ name: 'amount' }] }

    expect(TasksNavBar.computed.filterVariables.call(vm)).toEqual([{ name: 'amount' }])
  })

  it.each([[{}], [{ properties: null }]])('filterVariables should default to empty for %j', (selected) => {
    const vm = context()
    vm.$store.state.filter.selected = selected

    expect(TasksNavBar.computed.filterVariables.call(vm)).toEqual([])
  })

  it('showUndefinedVariable should reflect the filter property', () => {
    const vm = context()
    vm.$store.state.filter.selected.properties = { showUndefinedVariable: true }

    expect(TasksNavBar.computed.showUndefinedVariable.call(vm)).toBe(true)
  })

  it('showUndefinedVariable should default to false', () => {
    expect(TasksNavBar.computed.showUndefinedVariable.call(context())).toBe(false)
  })
})

describe('TasksNavBar - variable display', () => {
  const task = { variables: { amount: 42, when: '2026-01-01T00:00:00Z' }, variableTypes: { amount: 'Integer', when: 'Date' } }

  it('should render a plain variable through the shared value formatter', () => {
    expect(m.displayValue.call(context(), task, { name: 'amount' })).toBe('42')
  })

  // Dates get the app's date format rather than the raw ISO string.
  it('should format a Date variable', () => {
    const result = m.displayValue.call(context(), task, { name: 'when' })

    expect(result).not.toContain('T00:00:00Z')
  })

  it('should tolerate a task without variables', () => {
    expect(() => m.displayValue.call(context(), {}, { name: 'amount' })).not.toThrow()
  })

  describe('displayTooltip', () => {
    it('should show the label and technical name in the header', () => {
      const result = m.displayTooltip.call(context(), task, { name: 'amount', label: 'Amount' })

      expect(result).toContain('Amount (amount):')
      expect(result).toContain('42')
    })

    it('should fall back to the technical name when there is no label', () => {
      expect(m.displayTooltip.call(context(), task, { name: 'amount' })).toContain('amount:')
    })

    it('should format a Date variable for the tooltip', () => {
      const result = m.displayTooltip.call(context(), task, { name: 'when' })

      expect(result).not.toContain('T00:00:00Z')
    })

    // An undefined variable shows only its name, so the column still lines up.
    it('should show only the header when the variable has no value', () => {
      expect(m.displayTooltip.call(context(), { variables: {} }, { name: 'amount' })).toBe('amount:\n')
    })
  })

  describe('visibleFilterVariables', () => {
    const filterVariables = [{ name: 'amount' }, { name: 'customer' }]

    it('should show only variables the task actually has', () => {
      const vm = context({ filterVariables })

      const visible = m.visibleFilterVariables.call(vm, { variables: { amount: 42 } })

      expect(visible.map(v => v.name)).toEqual(['amount'])
    })

    it('should treat a null value as absent', () => {
      const vm = context({ filterVariables })

      expect(m.visibleFilterVariables.call(vm, { variables: { amount: null } })).toEqual([])
    })

    // The filter can opt into showing the empty slots so the columns stay aligned.
    it('should show every declared variable when undefined ones are shown', () => {
      const vm = context({ filterVariables, showUndefinedVariable: true })

      expect(m.visibleFilterVariables.call(vm, { variables: {} })).toHaveLength(2)
    })

    it('should tolerate a task with no variables at all', () => {
      const vm = context({ filterVariables })

      expect(m.visibleFilterVariables.call(vm, {})).toEqual([])
    })
  })

  it('toggleTaskVariables should flip the expanded state for that task', () => {
    const vm = context()

    m.toggleTaskVariables.call(vm, 't1')
    expect(vm.expandedTasks.t1).toBe(true)

    m.toggleTaskVariables.call(vm, 't1')
    expect(vm.expandedTasks.t1).toBe(false)
  })
})

describe('TasksNavBar - date presentation', () => {
  it('should show a placeholder translation when there is no date', () => {
    expect(m.getDateFormatted.call(context(), null, null, 'noDueDate')).toBe('task.noDueDate')
  })

  it('should apply an explicit format when given one', () => {
    expect(m.getDateFormatted.call(context(), '2026-01-15T10:30:00Z', 'YYYY-MM-DD')).toBe('2026-01-15')
  })

  it('should fall back to a relative description', () => {
    const result = m.getDateFormatted.call(context(), new Date().toISOString(), null)

    expect(result).toMatch(/ago|in |few seconds/)
  })

  describe('getDueClasses', () => {
    // Overdue is red; approaching the configured warning window is amber.
    it('should mark an overdue task in danger', () => {
      const vm = context()

      expect(m.getDueClasses.call(vm, { due: '2020-01-01T00:00:00Z' })).toContain('text-danger')
    })

    it('should mark a task due within the warning window', () => {
      const vm = context()
      const soon = new Date(Date.now() + 60 * 60 * 1000).toISOString()

      expect(m.getDueClasses.call(vm, { due: soon })).toContain('text-warning')
    })

    it('should not colour a task due beyond the warning window', () => {
      const vm = context()
      const later = new Date(Date.now() + 72 * 60 * 60 * 1000).toISOString()

      const classes = m.getDueClasses.call(vm, { due: later })

      expect(classes).not.toContain('text-warning')
      expect(classes).not.toContain('text-danger')
    })

    // The add-date button only appears on hover on the desktop, to keep rows uncluttered.
    it('should hide the action button for a task with no due date', () => {
      const vm = context()

      expect(m.getDueClasses.call(vm, { id: 't1' })).toContain('action-button-hidden')
    })

    it('should reveal the action button for the focused task', () => {
      const task = { id: 't1' }
      const vm = context({ focused: task })

      expect(m.getDueClasses.call(vm, task)).not.toContain('action-button-hidden')
    })

    it('should reveal the action button while its date picker is open', () => {
      const vm = context({ selectedDateT: { id: 't1' } })

      expect(m.getDueClasses.call(vm, { id: 't1' })).not.toContain('action-button-hidden')
    })

    it('should always show the action button on mobile', () => {
      const vm = context({ isMobile: () => true })

      expect(m.getDueClasses.call(vm, { id: 't1' })).not.toContain('action-button-hidden')
    })
  })

  describe('getReminderClasses', () => {
    it('should hide the action button for a task with no reminder', () => {
      expect(m.getReminderClasses.call(context(), { id: 't1' })).toContain('action-button-hidden')
    })

    it('should show the action button for a task that has one', () => {
      expect(m.getReminderClasses.call(context(), { id: 't1', followUp: '2026-01-01' })).toEqual([])
    })

    it('should always show the action button on mobile', () => {
      const vm = context({ isMobile: () => true })

      expect(m.getReminderClasses.call(vm, { id: 't1' })).toEqual([])
    })
  })

  it('dateFitsFilter should require both the filter and the date', () => {
    expect(m.dateFitsFilter.call(context(), '2026-01-01', true)).toBeTruthy()
    expect(m.dateFitsFilter.call(context(), null, true)).toBeFalsy()
    expect(m.dateFitsFilter.call(context(), '2026-01-01', false)).toBeFalsy()
  })

  it('isInThePast should compare against the start of today', () => {
    expect(m.isInThePast.call(context(), null, new Date('2020-01-01'))).toBe(true)
    expect(m.isInThePast.call(context(), null, new Date('2999-01-01'))).toBe(false)
  })
})

describe('TasksNavBar - date editing', () => {
  it('copyTaskForDateManagement should snapshot the task for editing', () => {
    const vm = context()
    const task = { id: 't1', due: '2026-01-15T10:30:00Z' }

    m.copyTaskForDateManagement.call(vm, task, 'due')

    expect(vm.taskToEdit).toBe(task)
    expect(vm.selectedDateT.id).toBe('t1')
    expect(vm.selectedDateT.due).toBeInstanceOf(Date)
    expect(vm.selectedDateT.dueTime).toMatch(/^\d+:\d+$/)
  })

  it('copyTaskForDateManagement should not derive a time for a reminder', () => {
    const vm = context()

    m.copyTaskForDateManagement.call(vm, { id: 't1', followUp: '2026-01-15T10:30:00Z' }, 'followUp')

    expect(vm.selectedDateT.followUp).toBeInstanceOf(Date)
    expect(vm.selectedDateT.dueTime).toBeUndefined()
  })

  it('copyTaskForDateManagement should leave an absent date alone', () => {
    const vm = context()

    m.copyTaskForDateManagement.call(vm, { id: 't1', due: null }, 'due')

    expect(vm.selectedDateT.due).toBeNull()
  })

  it('setDate should persist the chosen date and close the picker', () => {
    const vm = context({ selectedDateT: { due: new Date('2026-01-15T10:30:00Z') } })
    const task = { id: 't1' }

    m.setDate.call(vm, task, 'due')

    expect(task.due).toContain('2026-01-15')
    expect(TaskService.update).toHaveBeenCalledWith(task)
    expect(vm.$refs.due.hide).toHaveBeenCalled()
  })

  // Clearing the picker clears the date on the task.
  it('setDate should clear the date when the picker is empty', () => {
    const vm = context({ selectedDateT: {} })
    const task = { id: 't1', due: 'something' }

    m.setDate.call(vm, task, 'due')

    expect(task.due).toBeNull()
    expect(TaskService.update).toHaveBeenCalled()
  })

  it('setTime should apply the chosen hours and minutes', () => {
    const vm = context({ selectedDateT: { due: new Date('2026-01-15T00:00:00Z') } })

    m.setTime.call(vm, '14:45', 'due')

    expect(vm.selectedDateT.due.getHours()).toBe(14)
    expect(vm.selectedDateT.due.getMinutes()).toBe(45)
  })

  // A reminder with no time means the start of that day.
  it('setTime should reset a reminder to midnight when no time is given', () => {
    const vm = context({ selectedDateT: { followUp: new Date('2026-01-15T14:45:00Z') } })

    m.setTime.call(vm, null, 'followUp')

    expect(vm.selectedDateT.followUp.getHours()).toBe(0)
    expect(vm.selectedDateT.followUp.getMinutes()).toBe(0)
  })

  it('setTime should do nothing when no date is being edited', () => {
    const vm = context({ selectedDateT: {} })

    expect(() => m.setTime.call(vm, '14:45', 'due')).not.toThrow()
  })

  it('setTime should leave a due date untouched when no time is given', () => {
    const due = new Date('2026-01-15T14:45:00Z')
    const vm = context({ selectedDateT: { due } })

    m.setTime.call(vm, null, 'due')

    expect(vm.selectedDateT.due.getHours()).toBe(due.getHours())
  })
})

describe('TasksNavBar - claiming', () => {
  it('claim should assign the task to the current user and announce it', async () => {
    const vm = context()
    const task = { id: 't1', assignee: null }

    m.claim.call(vm, task)
    await flush()

    expect(TaskService.setAssignee).toHaveBeenCalledWith('t1', 'demo')
    expect(task.assignee).toBe('demo')
    expect(vm.$emit).toHaveBeenCalledWith('update-assignee', { taskId: 't1', assignee: 'demo' })
  })

  // The list may be stale, so the task is re-read before claiming.
  it('checkAssignee should claim a task that is still unassigned', async () => {
    const vm = context({ claim: vi.fn() })
    TaskService.findTaskById.mockResolvedValue({ id: 't1', assignee: null })

    m.checkAssignee.call(vm, { id: 't1' })
    await flush()

    expect(vm.claim).toHaveBeenCalledWith({ id: 't1', assignee: null })
  })

  it('checkAssignee should ask for confirmation when somebody already has it', async () => {
    const vm = context({ claim: vi.fn() })
    TaskService.findTaskById.mockResolvedValue({ id: 't1', assignee: 'other' })

    m.checkAssignee.call(vm, { id: 't1' })
    await flush()

    expect(vm.claim).not.toHaveBeenCalled()
    expect(vm.$refs.confirmTaskAssign.show).toHaveBeenCalledWith({ id: 't1', assignee: 'other' })
  })
})

describe('TasksNavBar - deep links to a task', () => {
  describe('checkTaskIdInUrl', () => {
    it('should select a task that is already in the loaded list', () => {
      const task = { id: 't1' }
      const vm = context({ tasks: [task], handleTaskLink: vi.fn() })

      m.checkTaskIdInUrl.call(vm, 't1')

      expect(vm.$emit).toHaveBeenCalledWith('selected-task', task)
      expect(vm.handleTaskLink).not.toHaveBeenCalled()
    })

    it('should resolve a task that is not in the list', () => {
      const vm = context({ tasks: [{ id: 'other' }], handleTaskLink: vi.fn() })

      m.checkTaskIdInUrl.call(vm, 't1')

      expect(vm.handleTaskLink).toHaveBeenCalledWith('t1')
    })

    it('should resolve the task when the list is still empty', () => {
      const vm = context({ tasks: [], handleTaskLink: vi.fn() })

      m.checkTaskIdInUrl.call(vm, 't1')

      expect(vm.handleTaskLink).toHaveBeenCalledWith('t1')
    })

    // The '*' pseudo-filter shows every task, so the list is not authoritative.
    it('should resolve the task directly under the all-tasks filter', () => {
      const vm = context({ tasks: [{ id: 't1' }], handleTaskLink: vi.fn() })
      vm.$route.params.filterId = '*'

      m.checkTaskIdInUrl.call(vm, 't1')

      expect(vm.handleTaskLink).toHaveBeenCalledWith('t1')
    })

    it('should do nothing without a task id', () => {
      const vm = context({ tasks: [{ id: 't1' }], handleTaskLink: vi.fn() })

      m.checkTaskIdInUrl.call(vm, undefined)

      expect(vm.handleTaskLink).not.toHaveBeenCalled()
      expect(vm.$emit).not.toHaveBeenCalled()
    })
  })

  describe('handleTaskLink', () => {
    it('should open a task assigned to the current user', async () => {
      const vm = context()
      TaskService.findTaskById.mockResolvedValue({ id: 't1', assignee: 'DEMO' })

      m.handleTaskLink.call(vm, 't1')
      await flush()

      expect(vm.$emit).toHaveBeenCalledWith('selected-task', { id: 't1', assignee: 'DEMO' })
      expect(TaskService.findIdentityLinks).not.toHaveBeenCalled()
    })

    // With no identity links there is nobody to exclude, so the task opens.
    it('should open a task that has no identity links', async () => {
      const vm = context()
      TaskService.findTaskById.mockResolvedValue({ id: 't1', assignee: null })
      TaskService.findIdentityLinks.mockResolvedValue([])

      m.handleTaskLink.call(vm, 't1')
      await flush()

      expect(vm.$emit).toHaveBeenCalledWith('selected-task', { id: 't1', assignee: null })
    })

    it('should open a task the user is a candidate for', async () => {
      const vm = context()
      TaskService.findTaskById.mockResolvedValue({ id: 't1', assignee: null })
      TaskService.findIdentityLinks.mockResolvedValue([{ type: 'candidate', userId: 'Demo' }])

      m.handleTaskLink.call(vm, 't1')
      await flush()

      expect(vm.$emit).toHaveBeenCalledWith('selected-task', expect.objectContaining({ id: 't1' }))
    })

    it('should fall through to the candidate groups otherwise', async () => {
      const vm = context({ manageCandidateGroups: vi.fn() })
      TaskService.findTaskById.mockResolvedValue({ id: 't1', assignee: null })
      TaskService.findIdentityLinks.mockResolvedValue([{ type: 'candidate', groupId: 'sales' }])

      m.handleTaskLink.call(vm, 't1')
      await flush()

      expect(vm.manageCandidateGroups).toHaveBeenCalled()
    })

    it.each([[null], [undefined]])('should open the task when identity links are %j', async (links) => {
      const vm = context()
      TaskService.findTaskById.mockResolvedValue({ id: 't1', assignee: null })
      TaskService.findIdentityLinks.mockResolvedValue(links)

      m.handleTaskLink.call(vm, 't1')
      await flush()

      expect(vm.$emit).toHaveBeenCalledWith('selected-task', expect.objectContaining({ id: 't1' }))
    })
  })

  describe('manageCandidateGroups', () => {
    it('should open the task when the user is in one of the candidate groups', async () => {
      const vm = context()
      AdminService.findUsers.mockResolvedValue([{ id: 'DEMO' }])

      m.manageCandidateGroups.call(vm, [{ type: 'candidate', groupId: 'sales' }], { id: 't1' })
      await flush()

      expect(AdminService.findUsers).toHaveBeenCalledWith({ memberOfGroup: 'sales' })
      expect(vm.$emit).toHaveBeenCalledWith('selected-task', { id: 't1' })
    })

    // Following a link to somebody else's task must not silently show it.
    it('should refuse and navigate back when the user is in none of them', async () => {
      const vm = context()
      AdminService.findUsers.mockResolvedValue([{ id: 'someone-else' }])

      m.manageCandidateGroups.call(vm, [{ type: 'candidate', groupId: 'sales' }], { id: 't1' })
      await flush()

      expect(vm.$root.$refs.error.show).toHaveBeenCalledWith({
        type: 'AccessDeniedException', params: ['t1']
      })
      expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f1')
    })

    it('should refuse when there are no candidate groups to check', async () => {
      const vm = context()

      m.manageCandidateGroups.call(vm, [{ type: 'candidate', userId: 'other' }], { id: 't1' })
      await flush()

      expect(AdminService.findUsers).not.toHaveBeenCalled()
      expect(vm.$root.$refs.error.show).toHaveBeenCalled()
    })
  })
})

describe('TasksNavBar - selection and navigation', () => {
  beforeEach(() => {
    vi.spyOn(window, 'getSelection').mockReturnValue({ toString: () => '' })
  })

  it('selectedTask should navigate to the task within the selected filter', () => {
    const vm = context()

    m.selectedTask.call(vm, { id: 't1' })

    expect(vm.justSelectedFromList).toBe(true)
    expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f1/t1')
  })

  it('selectedTask should not navigate when already on that task', () => {
    const vm = context()
    vm.$router.currentRoute.path = '/seven/auth/tasks/f1/t1'

    m.selectedTask.call(vm, { id: 't1' })

    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  // Clicking to select text inside a row should not also open the task.
  it('selectedTask should not navigate while text is selected', () => {
    window.getSelection.mockReturnValue({ toString: () => 'copied text' })
    const vm = context()

    m.selectedTask.call(vm, { id: 't1' })

    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  it('selectedTask should fall back to the route filter when the store has none', () => {
    const vm = context()
    vm.$store.state.filter.selected = null
    vm.$route.params.filterId = 'f-route'

    m.selectedTask.call(vm, { id: 't1' })

    expect(vm.$router.push).toHaveBeenCalledWith('/seven/auth/tasks/f-route/t1')
  })

  it('handleScrollTasks should ask for more once the list is scrolled to the bottom', () => {
    const vm = context({ tasks: new Array(20).fill({ id: 't' }), taskResultsIndex: 20 })

    m.handleScrollTasks.call(vm, { target: { scrollTop: 500, clientHeight: 300, scrollHeight: 800 } })

    expect(vm.$emit).toHaveBeenCalledWith('show-more')
  })

  it('handleScrollTasks should stay quiet part-way down the list', () => {
    const vm = context({ tasks: new Array(20).fill({ id: 't' }), taskResultsIndex: 20 })

    m.handleScrollTasks.call(vm, { target: { scrollTop: 100, clientHeight: 300, scrollHeight: 800 } })

    expect(vm.$emit).not.toHaveBeenCalled()
  })

  // A short list is already complete, so there is nothing more to load.
  it('handleScrollTasks should stay quiet when the last page was partial', () => {
    const vm = context({ tasks: [{ id: 't1' }], taskResultsIndex: 20 })

    m.handleScrollTasks.call(vm, { target: { scrollTop: 500, clientHeight: 300, scrollHeight: 800 } })

    expect(vm.$emit).not.toHaveBeenCalled()
  })
})

describe('TasksNavBar - names', () => {
  it('getProcessName should resolve the process name from its definition id', () => {
    const vm = context()
    vm.$store.state.process.list = [{ id: 'invoice:2:abc', name: 'Invoice' }]

    expect(m.getProcessName.call(vm, 'invoice:1:xyz')).toBe('Invoice')
  })

  it('getProcessName should return nothing for an unknown definition', () => {
    expect(m.getProcessName.call(context(), 'unknown:1:abc')).toBe('')
  })

  it('getProcessName should return nothing when the layout hides it', () => {
    const vm = context()
    vm.$root.config.layout.showProcessName = false
    vm.$store.state.process.list = [{ id: 'invoice:2:abc', name: 'Invoice' }]

    expect(m.getProcessName.call(vm, 'invoice:1:xyz')).toBe('')
  })

  it('getProcessName should return nothing without a definition id', () => {
    expect(m.getProcessName.call(context(), null)).toBe('')
  })

  it('getProcessName should return nothing for a nameless process', () => {
    const vm = context()
    vm.$store.state.process.list = [{ id: 'invoice:2:abc' }]

    expect(m.getProcessName.call(vm, 'invoice:1:xyz')).toBe('')
  })

  describe('getCompleteName', () => {
    it('should return the current user id for their own task', () => {
      expect(m.getCompleteName.call(context(), { assignee: 'DEMO' })).toBe('demo')
    })

    it('should return a candidate display name when known', () => {
      const vm = context()
      vm.$store.state.user.listCandidates = [{ id: 'other', displayName: 'Other User' }]

      expect(m.getCompleteName.call(vm, { assignee: 'other' })).toBe('Other User')
    })

    it('should fall back to the raw assignee when no candidate matches', () => {
      expect(m.getCompleteName.call(context(), { assignee: 'other' })).toBe('other')
    })

    it('should fall back when the candidate has no display name', () => {
      const vm = context()
      vm.$store.state.user.listCandidates = [{ id: 'other' }]

      expect(m.getCompleteName.call(vm, { assignee: 'other' })).toBe('other')
    })

    it('should fall back when candidates are not loaded', () => {
      const vm = context()
      vm.$store.state.user.listCandidates = null

      expect(m.getCompleteName.call(vm, { assignee: 'other' })).toBe('other')
    })
  })
})

describe('TasksNavBar - sorting and refresh', () => {
  it('setSorting should persist the sorting and ask for a refresh', () => {
    const vm = context()

    m.setSorting.call(vm, 'created')

    expect(JSON.parse(localStorage.getItem('taskSorting'))).toEqual({ sortBy: 'created', sortOrder: 'desc' })
    expect(vm.$emit).toHaveBeenCalledWith('refresh-tasks')
  })

  // Clicking the order toggle flips the direction rather than changing the field.
  it('setSorting should flip the direction for the order toggle', () => {
    const vm = context()

    m.setSorting.call(vm, 'order')
    expect(vm.taskSorting.sortOrder).toBe('asc')

    m.setSorting.call(vm, 'order')
    expect(vm.taskSorting.sortOrder).toBe('desc')
  })

  it.each([
    ['followUpDate', 'showFilterReminderDate'],
    ['dueDate', 'showFilterDueDate']
  ])('showFields should gate %s on the layout flag', (field, flag) => {
    const vm = context()
    vm.$root.config.layout[flag] = false

    expect(m.showFields.call(vm, field)).toBe(false)

    vm.$root.config.layout[flag] = true
    expect(m.showFields.call(vm, field)).toBe(true)
  })

  it('showFields should always show any other field', () => {
    expect(m.showFields.call(context(), 'created')).toBe(true)
  })

  it('refreshTasks should refresh the list and the count, then debounce the button', () => {
    const vm = context({ pauseButton: vi.fn() })

    m.refreshTasks.call(vm)

    expect(vm.$emit).toHaveBeenCalledWith('refresh-tasks')
    expect(vm.$emit).toHaveBeenCalledWith('refresh-tasks-number')
    expect(vm.pauseButton).toHaveBeenCalled()
  })

  // Prevents hammering the backend by holding the refresh button down.
  it('refreshTasks should ignore a click while the button is paused', () => {
    const vm = context({ pauseRefreshButton: true, pauseButton: vi.fn() })

    m.refreshTasks.call(vm)

    expect(vm.$emit).not.toHaveBeenCalled()
  })

  it('pauseButton should release the button after the configured delay', () => {
    vi.useFakeTimers()
    const vm = context()

    m.pauseButton.call(vm)
    expect(vm.pauseRefreshButton).toBe(true)

    vi.advanceTimersByTime(1000)
    expect(vm.pauseRefreshButton).toBe(false)
  })

  it('pauseButton should do nothing when no delay is configured', () => {
    const vm = context()
    vm.$root.config.pauseButtonTime = 0

    m.pauseButton.call(vm)

    expect(vm.pauseRefreshButton).toBe(false)
  })
})

describe('TasksNavBar - advanced filters', () => {
  const processVariable = (overrides = {}) => ({
    key: 'pv_status', variableName: 'status', displayName: 'Status',
    value: 'open', operator: 'eq', type: 'String', ...overrides
  })

  describe('loadAdvancedFilters', () => {
    it('should build an unchecked entry from the configured default', () => {
      const vm = context()
      vm.$root.config.taskFilter.advancedSearch.processVariables = [processVariable()]

      m.loadAdvancedFilters.call(vm)

      expect(vm.advancedFilter[0]).toMatchObject({
        key: 'pv_status', variableName: 'status', displayName: 'Status',
        type: 'String', defaultValue: 'open', operator: 'eq', check: false, value: 'open'
      })
    })

    it('should mark a filter as boolean when its default is one', () => {
      const vm = context()
      vm.$root.config.taskFilter.advancedSearch.processVariables = [processVariable({ value: true, type: 'Boolean' })]

      m.loadAdvancedFilters.call(vm)

      expect(vm.advancedFilter[0]).toMatchObject({ type: 'Boolean', value: '' })
    })

    it('should check an entry that matches a stored criterion', () => {
      const vm = context()
      vm.$root.config.taskFilter.advancedSearch.processVariables = [processVariable()]
      vm.$store.state.advancedSearch.criterias = [{ id: 'pv_status', operator: 'eq', value: 'closed' }]

      m.loadAdvancedFilters.call(vm)

      expect(vm.advancedFilter[0]).toMatchObject({ check: true, value: 'closed' })
    })

    // A 'like' criterion is stored wrapped in % wildcards, which the input must not show.
    it('should strip the wildcards from a stored like criterion', () => {
      const vm = context()
      vm.$root.config.taskFilter.advancedSearch.processVariables = [processVariable({ operator: 'like' })]
      vm.$store.state.advancedSearch.criterias = [{ id: 'pv_status', operator: 'like', value: '%open%' }]

      m.loadAdvancedFilters.call(vm)

      expect(vm.advancedFilter[0].value).toBe('open')
    })

    it('should blank a checked boolean entry', () => {
      const vm = context()
      vm.$root.config.taskFilter.advancedSearch.processVariables = [processVariable({ type: 'Boolean' })]
      vm.$store.state.advancedSearch.criterias = [{ id: 'pv_status', operator: 'eq', value: 'x' }]

      m.loadAdvancedFilters.call(vm)

      expect(vm.advancedFilter[0].value).toBe('')
    })

    // Variables declared by the filter itself get their own like-search entries.
    it('should add an entry for each variable the filter declares', () => {
      const vm = context({ filterVariables: [{ name: 'amount', label: 'Amount' }] })

      m.loadAdvancedFilters.call(vm)

      expect(vm.advancedFilter[0]).toMatchObject({
        key: 'fv_amount', variableName: 'amount', displayName: 'Amount',
        type: 'String', operator: 'like', source: 'filter', check: false, value: ''
      })
    })

    it('should name a filter variable by its technical name when unlabelled', () => {
      const vm = context({ filterVariables: [{ name: 'amount' }] })

      m.loadAdvancedFilters.call(vm)

      expect(vm.advancedFilter[0].displayName).toBe('amount')
    })

    it('should check a filter variable that has a stored criterion', () => {
      const vm = context({ filterVariables: [{ name: 'amount' }] })
      vm.$store.state.advancedSearch.criterias = [{ id: 'fv_amount', operator: 'like', value: '%42%' }]

      m.loadAdvancedFilters.call(vm)

      expect(vm.advancedFilter[0]).toMatchObject({ check: true, value: '42' })
    })

    it('should snapshot the built filters for change detection', () => {
      const vm = context({ filterVariables: [{ name: 'amount' }] })

      m.loadAdvancedFilters.call(vm)

      expect(vm.advancedFilterAux).toBe(JSON.stringify(vm.advancedFilter))
    })
  })

  describe('updateAdvancedFilters', () => {
    // The method is debounced in the component, so the timers are driven explicitly.
    const run = (vm) => {
      vi.useFakeTimers()
      m.updateAdvancedFilters.call(vm)
      vi.advanceTimersByTime(800)
    }

    it('should publish the checked filters as search criteria', () => {
      const vm = context({
        advancedFilter: [{ key: 'pv_status', variableName: 'status', operator: 'eq', type: 'String', check: true, value: 'open' }]
      })

      run(vm)

      expect(vm.$store.dispatch).toHaveBeenCalledWith('updateAdvancedSearch', {
        matchAllCriteria: true,
        criterias: [{ id: 'pv_status', key: 'processVariables', name: 'status', operator: 'eq', value: 'open' }]
      })
    })

    it('should wrap a like value in wildcards', () => {
      const vm = context({
        advancedFilter: [{ key: 'fv_amount', variableName: 'amount', operator: 'like', type: 'String', check: true, value: '42' }]
      })

      run(vm)

      expect(vm.$store.dispatch.mock.calls[0][1].criterias[0].value).toBe('%42%')
    })

    it('should send a boolean filter with its configured value', () => {
      const vm = context({
        advancedFilter: [{ key: 'pv_flag', variableName: 'flag', operator: 'eq', type: 'Boolean', check: true, value: '', defaultValue: true }]
      })

      run(vm)

      expect(vm.$store.dispatch.mock.calls[0][1].criterias[0].value).toBe(true)
    })

    it('should skip unchecked filters', () => {
      const vm = context({
        advancedFilter: [{ key: 'pv_status', variableName: 'status', operator: 'eq', type: 'String', check: false, value: 'open' }]
      })

      run(vm)

      expect(vm.$store.dispatch.mock.calls[0][1].criterias).toEqual([])
    })

    it('should skip a checked filter with no value entered', () => {
      const vm = context({
        advancedFilter: [{ key: 'pv_status', variableName: 'status', operator: 'eq', type: 'String', check: true, value: '' }]
      })

      run(vm)

      expect(vm.$store.dispatch.mock.calls[0][1].criterias).toEqual([])
    })

    it('should refresh the task list and re-snapshot the filters', () => {
      const vm = context({ advancedFilter: [] })

      run(vm)

      expect(vm.$emit).toHaveBeenCalledWith('refresh-tasks')
      expect(vm.advancedFilterAux).toBe('[]')
    })
  })
})

describe('TasksNavBar - scrollToSelectedTask', () => {
  const withRef = (ref) => {
    const vm = context({ pendingScrollToTaskId: 't1' })
    vm.$route.params.taskId = 't1'
    vm.$refs['taskItem-t1'] = ref
    return vm
  }

  it('should scroll a component ref into view and clear the pending id', () => {
    const scrollIntoView = vi.fn()
    const vm = withRef({ $el: { scrollIntoView } })

    m.scrollToSelectedTask.call(vm)

    expect(scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'center' })
    expect(vm.pendingScrollToTaskId).toBeNull()
  })

  // v-for refs arrive as an array.
  it('should scroll the first entry of an array ref', () => {
    const scrollIntoView = vi.fn()
    const vm = withRef([{ $el: { scrollIntoView } }])

    m.scrollToSelectedTask.call(vm)

    expect(scrollIntoView).toHaveBeenCalled()
  })

  it('should scroll a bare element ref', () => {
    const scrollIntoView = vi.fn()
    const vm = withRef({ scrollIntoView })

    m.scrollToSelectedTask.call(vm)

    expect(scrollIntoView).toHaveBeenCalled()
  })

  it('should scroll a bare element inside an array ref', () => {
    const scrollIntoView = vi.fn()
    const vm = withRef([{ scrollIntoView }])

    m.scrollToSelectedTask.call(vm)

    expect(scrollIntoView).toHaveBeenCalled()
  })

  // The row may not be rendered yet, so the scroll is retried a bounded number of times.
  it('should retry while the row is not rendered and then give up with a warning', () => {
    vi.useFakeTimers()
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    const vm = withRef(null)

    m.scrollToSelectedTask.call(vm)
    vi.advanceTimersByTime(100 * 6)

    expect(warn).toHaveBeenCalledWith('scrollToSelectedTask: Element not found after 5 retries.')
    expect(vm.pendingScrollToTaskId).toBe('t1')
  })
})
