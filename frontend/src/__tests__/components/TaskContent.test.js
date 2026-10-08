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
import { createStore } from 'vuex'
import { mountWithDefaults } from '../support/mountWithDefaults.js'

vi.mock('@/services.js', () => ({
  TaskService: { findIdentityLinks: vi.fn(() => Promise.resolve([])), setAssignee: vi.fn(), findTaskById: vi.fn() },
  AdminService: { findUsers: vi.fn(() => Promise.resolve([])) }
}))

import TaskContent from '@/components/task/TaskContent.vue'
import TaskView from '@/components/task/TaskView.vue'

const hideMenu = vi.fn()
const showModal = vi.fn()

const stubs = {
  RenderTemplate: true,
  FilterableSelect: true,
  ConfirmDialog: true,
  TaskAssignationModal: true,
  'b-popover': true,
  'b-input-group': { template: '<div><slot></slot></div>' },
  'b-input-group-prepend': { template: '<div><slot></slot></div>' },
  'b-form-tag': { template: '<span><slot></slot></span>' },
  'b-form-checkbox': true,
  AssignUserModal: {
    name: 'AssignUserModal',
    emits: ['select'],
    template: '<div class="assign-user-modal"></div>',
    methods: { show: showModal }
  },
  'b-dropdown-divider': { template: '<li class="divider"></li>' },
  // Renders both slots so the menu can be inspected without opening it
  'b-dropdown': {
    props: ['label'],
    template: '<div><button class="toggle" :aria-label="label"><slot name="button-content"></slot></button><ul><slot></slot></ul></div>',
    methods: { hide: hideMenu }
  }
}

function mountTaskContent({ task = { id: 't1', name: 'Prepare bank transfer', assignee: null }, mobile = true, props = {}, authorizationEnabled = false } = {}) {
  return mountWithDefaults(TaskContent, {
    props: { task, ...props },
    global: {
      stubs,
      provide: { isMobile: () => mobile },
      mocks: {
        // Mounted standalone, $root is the component itself, so `$root.user`/`$root.config` resolve to these
        user: { id: 'demo', permissions: {} },
        config: { layout: {}, permissions: {}, authorizationEnabled }
      },
      plugins: [createTestStore()]
    }
  })
}

// `mapActions('task', …)` needs a real namespaced store, so a minimal one stands in for ours.
function createTestStore() {
  return createStore({
    modules: {
      task: {
        namespaced: true,
        state: { selectedAssignee: null },
        getters: { getAssigneeByTaskId: () => () => undefined },
        actions: { setSelectedAssignee: () => {} }
      },
      user: { state: { searchUsers: [], listCandidates: [] } }
    },
    mutations: { setCandidateUsers: () => {}, setSearchUsers: () => {} }
  })
}

beforeEach(() => {
  vi.useFakeTimers()
  hideMenu.mockClear()
  showModal.mockClear()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('TaskContent - mobile task header', () => {
  it('should show the task name', () => {
    const wrapper = mountTaskContent()

    expect(wrapper.find('h3').text()).toBe('Prepare bank transfer')
  })

  it('should ask to go back to the task list from the clipboard button', async () => {
    const wrapper = mountTaskContent()

    await wrapper.find('button[aria-label="task.backToTaskList"]').trigger('click')

    expect(wrapper.emitted('show-task-list')).toHaveLength(1)
  })

  // The right sidebar only exists when a host (e.g. flow-webclient) registers one.
  it('should hide the options button when there is no right sidebar', () => {
    const wrapper = mountTaskContent()

    expect(wrapper.find('button[aria-label="task.options"]').exists()).toBe(false)
  })

  it('should ask to open the options when the right sidebar is available', async () => {
    const wrapper = mountTaskContent({ props: { hasOptions: true } })

    const optionsButton = wrapper.find('button[aria-label="task.options"]')
    expect(optionsButton.find('.mdi-dots-vertical').exists()).toBe(true)
    await optionsButton.trigger('click')

    expect(wrapper.emitted('show-options')).toHaveLength(1)
  })

  it('should mark an unassigned task with the question icon', () => {
    const wrapper = mountTaskContent()

    expect(wrapper.find('button.toggle').attributes('aria-label')).toBe('task.notAssigned')
    expect(wrapper.find('button.toggle .mdi-account-question').exists()).toBe(true)
  })

  it('should name the assignee in the menu of an assigned task', () => {
    const wrapper = mountTaskContent({ task: { id: 't1', name: 'Prepare bank transfer', assignee: 'lucash' } })

    expect(wrapper.find('button.toggle .mdi-account').exists()).toBe(true)
    expect(wrapper.find('.dropdown-item-text').text()).toBe('task.assignedTo')
  })

  it('should assign the task to the current user from the menu', async () => {
    const wrapper = mountTaskContent()
    const setSelectedAssignee = vi.spyOn(wrapper.vm, 'setSelectedAssignee')

    await wrapper.findAll('button').find(b => b.text() === 'task.assignToMe').trigger('click')

    expect(hideMenu).toHaveBeenCalled()
    expect(setSelectedAssignee).toHaveBeenCalledWith({ taskId: 't1', assignee: 'demo' })
  })

  // Taking over someone else's task is still offered; checkAssignee asks for confirmation.
  it('should offer "assign to me" while someone else holds the task', () => {
    const wrapper = mountTaskContent({ task: { id: 't1', name: 'Prepare bank transfer', assignee: 'lucash' } })

    expect(wrapper.text()).toContain('task.assignToMe')
  })

  it('should not offer "assign to me" when the task is already mine', () => {
    const wrapper = mountTaskContent({ task: { id: 't1', name: 'Prepare bank transfer', assignee: 'Demo' } })

    expect(wrapper.text()).not.toContain('task.assignToMe')
  })

  it('should open the candidate group modal from the menu', async () => {
    const wrapper = mountTaskContent()
    const openModal = vi.spyOn(wrapper.vm, 'openTaskAssignationModal').mockImplementation(() => {})

    await wrapper.findAll('button').find(b => b.text() === 'admin.groups.addCandidateGroups').trigger('click')

    expect(hideMenu).toHaveBeenCalled()
    expect(openModal).toHaveBeenCalled()
  })

  // With authorization on and no cockpit permission configured the entry is hidden.
  it('should hide the candidate group entry without cockpit permission', () => {
    const wrapper = mountTaskContent({ task: { id: 't1', name: 'Prepare bank transfer', assignee: 'demo' }, authorizationEnabled: true })

    expect(wrapper.text()).not.toContain('admin.groups.addCandidateGroups')
  })

  const menuButton = (wrapper, label) => wrapper.findAll('button').find(b => b.text() === label)

  it('should remove the assignment from the menu', async () => {
    const wrapper = mountTaskContent({ task: { id: 't1', name: 'Prepare bank transfer', assignee: 'lucash' } })
    const setSelectedAssignee = vi.spyOn(wrapper.vm, 'setSelectedAssignee')
    const update = vi.spyOn(wrapper.vm, 'update').mockImplementation(() => {})

    await menuButton(wrapper, 'task.unassign').trigger('click')

    expect(hideMenu).toHaveBeenCalled()
    expect(setSelectedAssignee).toHaveBeenCalledWith({ taskId: 't1', assignee: null })
    expect(update).toHaveBeenCalled()
  })

  it('should not offer to remove the assignment of an unassigned task', () => {
    const wrapper = mountTaskContent()

    expect(menuButton(wrapper, 'task.unassign')).toBeUndefined()
  })

  it('should open the user search to assign an unassigned task to someone', async () => {
    const wrapper = mountTaskContent()

    await menuButton(wrapper, 'task.assign').trigger('click')

    expect(hideMenu).toHaveBeenCalled()
    expect(showModal).toHaveBeenCalled()
  })

  // Reassigning an assigned task goes through removing the assignment first.
  it('should not offer to assign someone while the task is assigned', () => {
    const wrapper = mountTaskContent({ task: { id: 't1', name: 'Prepare bank transfer', assignee: 'lucash' } })

    expect(menuButton(wrapper, 'task.assign')).toBeUndefined()
  })

  // Picking a user assigns straight away, as on desktop.
  it('should assign the user picked in the search', async () => {
    const wrapper = mountTaskContent()
    const setSelectedAssignee = vi.spyOn(wrapper.vm, 'setSelectedAssignee')

    wrapper.findComponent({ name: 'AssignUserModal' }).vm.$emit('select', 'lucash')
    await wrapper.vm.$nextTick()

    expect(setSelectedAssignee).toHaveBeenCalledWith({ taskId: 't1', assignee: 'lucash' })
  })

  // The shared modal keeps its desktop placement; only the mobile header centres it.
  it('should centre the candidate group modal only on mobile', () => {
    const modal = (mobile) => mountTaskContent({ mobile }).findComponent({ name: 'TaskAssignationModal' })

    expect(modal(true).classes()).toContain('modal-centered')
    expect(modal(false).classes()).not.toContain('modal-centered')
  })

  // Desktop and mobile share the same permission check for candidate groups.
  it('should gate the desktop candidate group button on the cockpit permission', () => {
    const button = (authorizationEnabled) => mountTaskContent({ mobile: false, authorizationEnabled })
      .findAll('button').find(b => b.text() === 'admin.groups.addCandidateGroups')

    expect(button(false)).toBeDefined()
    expect(button(true)).toBeUndefined()
  })

  it('should not render the mobile header on desktop', () => {
    const wrapper = mountTaskContent({ mobile: false })

    expect(wrapper.find('button[aria-label="task.backToTaskList"]').exists()).toBe(false)
    expect(wrapper.find('.assign-user-modal').exists()).toBe(false)
  })
})

describe('TaskContent - assigneeStatus', () => {
  const { assigneeStatus } = TaskContent.computed
  const $t = (key, params) => (params ? `${key}:${params.join(',')}` : key)

  it('should name the assignee of an assigned task', () => {
    const vm = { $t, task: { assignee: 'lucash' }, getCompleteName: 'Lucas H' }

    expect(assigneeStatus.call(vm)).toBe('task.assignedTo:Lucas H')
  })

  // getCompleteName dereferences the assignee, so it must not be read for an unassigned task.
  it('should say the task is not assigned without reading the assignee name', () => {
    const vm = { $t, task: { assignee: null } }
    Object.defineProperty(vm, 'getCompleteName', { get: () => { throw new Error('read without assignee') } })

    expect(assigneeStatus.call(vm)).toBe('task.notAssigned')
  })
})

describe('TaskView', () => {
  const TaskContentStub = {
    name: 'TaskContent',
    props: ['task', 'hasOptions'],
    emits: ['show-task-list', 'show-options'],
    template: '<div></div>'
  }

  // TaskView is the routed wrapper, so the mobile header only works if it relays both ways.
  it('should pass hasOptions down and relay the mobile header events', async () => {
    const wrapper = mountWithDefaults(TaskView, {
      props: { task: { id: 't1' }, hasOptions: true },
      global: { stubs: { TaskContent: TaskContentStub } }
    })
    const content = wrapper.findComponent(TaskContentStub)

    expect(content.props('hasOptions')).toBe(true)
    content.vm.$emit('show-task-list')
    content.vm.$emit('show-options')

    expect(wrapper.emitted('show-task-list')).toHaveLength(1)
    expect(wrapper.emitted('show-options')).toHaveLength(1)
  })
})
