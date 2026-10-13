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
import AssignUserModal from '@/components/task/AssignUserModal.vue'

const candidates = [
  { id: 'lucash', firstName: 'Lucas', lastName: 'Hernández' },
  { id: 'aliceti', firstName: 'Alice', lastName: 'Tietze' }
]

const showModal = vi.fn()
const hideModal = vi.fn()
// What the findUsers action puts in the store; duplicated like the real one can return
const findUsersAction = vi.fn(() => [candidates[0], candidates[0]])

// A real store so resetUsers/findUsers go through the same mutations as in the app
function createTestStore() {
  return createStore({
    state: { user: { listCandidates: candidates, searchUsers: [] } },
    mutations: { setSearchUsers: (state, users) => { state.user.searchUsers = users } },
    actions: {
      findUsers: (ctx, params) => {
        ctx.commit('setSearchUsers', findUsersAction(params))
        return Promise.resolve()
      }
    }
  })
}

function mountModal() {
  return mountWithDefaults(AssignUserModal, {
    global: {
      plugins: [createTestStore()],
      stubs: {
        'b-avatar': true,
        'b-modal': {
          template: '<div class="modal-stub"><slot></slot></div>',
          emits: ['shown'],
          methods: { show: showModal, hide: hideModal }
        },
        'b-spinner': { template: '<span class="spinner"></span>' }
      },
      // Mounted standalone, $root is the component itself
      mocks: { config: { maxUsersResults: 10 } }
    }
  })
}

const userButtons = (wrapper) => wrapper.findAll('button.list-group-item')

beforeEach(() => {
  vi.useFakeTimers()
  vi.clearAllMocks()
  findUsersAction.mockImplementation(() => [candidates[0], candidates[0]])
})

afterEach(() => {
  vi.useRealTimers()
})

describe('AssignUserModal', () => {
  it('should reset the search and suggest the candidate users when shown', async () => {
    const wrapper = mountModal()
    await wrapper.find('input').setValue('som')

    wrapper.vm.show()
    await wrapper.vm.$nextTick()

    expect(showModal).toHaveBeenCalled()
    expect(wrapper.find('input').element.value).toBe('')
    expect(wrapper.find('h4').text()).toBe('task.candidateUsers')
    expect(userButtons(wrapper).map(b => b.text())).toEqual([
      expect.stringContaining('Lucas Hernández'), expect.stringContaining('Alice Tietze')
    ])
  })

  // Searching only starts at three characters, and waits for the user to stop typing.
  it('should search once three characters have been typed', async () => {
    const wrapper = mountModal()

    await wrapper.find('input').setValue('lu')
    vi.advanceTimersByTime(1000)
    expect(findUsersAction).not.toHaveBeenCalled()

    await wrapper.find('input').setValue('luc')
    expect(wrapper.find('.spinner').exists()).toBe(true)
    vi.advanceTimersByTime(1000)
    await vi.runAllTimersAsync()

    expect(findUsersAction).toHaveBeenCalledWith(expect.objectContaining({ filter: 'luc', likePatternIgnoreCase: true }))
    expect(wrapper.find('h4').text()).toBe('task.searchResults')
  })

  // findUsers merges the first-name, last-name and id queries.
  it('should list each user found only once', async () => {
    const wrapper = mountModal()

    await wrapper.find('input').setValue('luc')
    await vi.runAllTimersAsync()

    expect(userButtons(wrapper)).toHaveLength(1)
  })

  it('should say when the search finds nobody', async () => {
    findUsersAction.mockReturnValueOnce([])
    const wrapper = mountModal()

    await wrapper.find('input').setValue('zzz')
    await vi.runAllTimersAsync()

    expect(wrapper.text()).toContain('task.noUsersFound')
  })

  it('should pick a user and close', async () => {
    const wrapper = mountModal()
    wrapper.vm.show()
    await wrapper.vm.$nextTick()

    await userButtons(wrapper)[1].trigger('click')

    expect(wrapper.emitted('select')).toEqual([['aliceti']])
    expect(hideModal).toHaveBeenCalled()
  })

  it('should open centred on the screen', () => {
    expect(mountModal().find('.modal-stub').classes()).toContain('modal-centered')
  })

  it('should fall back to the user id when the name is unknown', () => {
    expect(mountModal().vm.fullName({ id: 'jdoe' })).toBe('jdoe')
  })
})
