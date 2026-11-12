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
import usersMixin from '@/mixins/usersMixin.js'

const { getCompleteName } = usersMixin.computed
const { getDisplayName, findUsers, resetUsers } = usersMixin.methods

function context(overrides = {}) {
  const vm = {
    task: { assignee: 'demo' },
    loadingUsers: true,
    $t: (key) => key,
    $refs: { ariaLiveText: { textContent: 'stale' } },
    $root: { user: { id: 'me', displayName: 'Me Myself' }, config: {} },
    $store: {
      state: { user: { listCandidates: [], searchUsers: [], userIdMap: {} } },
      commit: vi.fn(),
      dispatch: vi.fn(() => Promise.resolve())
    },
    ...overrides
  }
  vm.getDisplayName = getDisplayName.bind(vm)
  return vm
}

const candidate = (overrides = {}) => ({ id: 'demo', displayName: 'Demo User', ...overrides })

beforeEach(() => {
  vi.clearAllMocks()
})

describe('usersMixin', () => {
  describe('getDisplayName', () => {
    // The logged-in user is not in the cache; their display name comes with the session.
    it('should return the display name of the logged-in user for their own id', () => {
      expect(context().getDisplayName('me')).toBe('Me Myself')
    })

    it('should join first and last name of a cached user', () => {
      const vm = context()
      vm.$store.state.user.userIdMap = { demo: { id: 'demo', firstName: 'Demo', lastName: 'User' } }

      expect(vm.getDisplayName('demo')).toBe('Demo User')
    })

    // User ids are case-sensitive (CIB7-136): "Demo" and "demo" can be two different users.
    it('should match ids case-sensitively', () => {
      const vm = context()
      vm.$store.state.user.userIdMap = { Demo: { id: 'Demo', firstName: 'Other', lastName: 'Person' } }

      expect(vm.getDisplayName('demo')).toBe('demo')
      expect(vm.getDisplayName('ME')).toBe('ME')
    })

    it.each([
      ['not cached yet', {}],
      ['unknown to the backend (cached as null)', { demo: null }],
      ['cached with first name only', { demo: { id: 'demo', firstName: 'Demo' } }],
      ['cached with last name only', { demo: { id: 'demo', lastName: 'User' } }],
      ['cached without names', { demo: { id: 'demo' } }]
    ])('should fall back to the id when the user is %s', (_, userIdMap) => {
      const vm = context()
      vm.$store.state.user.userIdMap = userIdMap

      expect(vm.getDisplayName('demo')).toBe('demo')
    })

    it.each([[null], [undefined], ['']])('should return an empty string for an unassigned task (%j)', (assignee) => {
      expect(context().getDisplayName(assignee)).toBe('')
    })
  })

  describe('getCompleteName', () => {
    it('should return the display name of the current task assignee', () => {
      const vm = context()
      vm.$store.state.user.userIdMap = { demo: { id: 'demo', firstName: 'Demo', lastName: 'User' } }

      expect(getCompleteName.call(vm)).toBe('Demo User')
    })

    it.each([[{ assignee: null }], [null]])('should return an empty string without an assignee (task %j)', (task) => {
      expect(getCompleteName.call(context({ task }))).toBe('')
    })
  })

  describe('findUsers', () => {
    it('should dispatch a search with the configured limits and provider', async () => {
      const vm = context({ $root: { user: { id: 'me' }, config: { maxUsersResults: 25, userProvider: 'ldap' } } })

      findUsers.call(vm, 'dem', true)
      await Promise.resolve()

      expect(vm.$store.dispatch).toHaveBeenCalledWith('findUsers', {
        maxResults: 25,
        filter: 'dem',
        userProvider: 'ldap',
        likePatternIgnoreCase: true
      })
    })

    it('should default the result limit to 10', async () => {
      const vm = context()

      findUsers.call(vm, 'dem')
      await Promise.resolve()

      expect(vm.$store.dispatch).toHaveBeenCalledWith('findUsers', expect.objectContaining({ maxResults: 10 }))
    })

    // The live region is cleared first so that screen readers re-announce the count even
    // when the same number of users comes back twice in a row.
    it('should clear the live region before searching', () => {
      const vm = context()

      findUsers.call(vm, 'dem')

      expect(vm.$refs.ariaLiveText.textContent).toBe('')
    })

    it('should announce the result and stop the spinner when users are found', async () => {
      const vm = context()
      vm.$store.dispatch = vi.fn(() => {
        vm.$store.state.user.searchUsers = [candidate()]
        return Promise.resolve()
      })

      findUsers.call(vm, 'dem')
      await Promise.resolve()

      expect(vm.$refs.ariaLiveText.textContent).toBe('task.usersFound')
      expect(vm.loadingUsers).toBe(false)
    })

    it('should announce nothing when no users are found', async () => {
      const vm = context()

      findUsers.call(vm, 'dem')
      await Promise.resolve()

      expect(vm.$refs.ariaLiveText.textContent).toBe('')
      expect(vm.loadingUsers).toBe(false)
    })

    it('should stop the spinner when the search fails', async () => {
      const vm = context()
      vm.$store.dispatch = vi.fn(() => Promise.reject(new Error('boom')))

      findUsers.call(vm, 'dem')
      await Promise.resolve()
      await Promise.resolve()

      expect(vm.loadingUsers).toBe(false)
    })
  })

  describe('resetUsers', () => {
    // Called from an input event, the reset clears the list; called without one it restores
    // the candidate list so the dropdown shows the task's own candidates again.
    it('should clear the search results when triggered by an event', () => {
      const vm = context()

      resetUsers.call(vm, { type: 'input' })

      expect(vm.$store.commit).toHaveBeenCalledWith('setSearchUsers', [])
    })

    it('should restore the candidate list when triggered without an event', () => {
      const vm = context()
      vm.$store.state.user.listCandidates = [candidate()]

      resetUsers.call(vm, undefined)

      expect(vm.$store.commit).toHaveBeenCalledWith('setSearchUsers', [candidate()])
    })
  })
})
