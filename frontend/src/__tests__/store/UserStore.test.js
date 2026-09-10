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
import { it, expect, vi, beforeEach } from 'vitest'
import UserStore from '../../store/UserStore.js'
import { AdminService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  AdminService: {
    findUsers: vi.fn()
  }
}))

const user = (overrides = {}) => ({ id: 'demo', firstName: 'Demo', lastName: 'User', ...overrides })

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('UserStore', UserStore, {
  initialState: (getState) => {
    it('should start with empty candidate and search lists', () => {
      expect(getState()).toEqual({ listCandidates: [], searchUsers: [] })
    })
  },

  mutations: {
    setCandidateUsers: (mutation, getState) => {
      it('should replace the candidate list', () => {
        const state = getState()
        state.listCandidates = [user({ id: 'old' })]

        mutation(state, [user({ id: 'new' })])

        expect(state.listCandidates).toEqual([user({ id: 'new' })])
      })
    },

    concatCandidateUsers: (mutation, getState) => {
      it('should append to the candidate list', () => {
        const state = getState()
        state.listCandidates = [user({ id: 'a' })]

        mutation(state, [user({ id: 'b' })])

        expect(state.listCandidates.map(u => u.id)).toEqual(['a', 'b'])
      })
    },

    setSearchUsers: (mutation, getState) => {
      it('should replace the search result list', () => {
        const state = getState()
        state.searchUsers = [user({ id: 'old' })]

        mutation(state, [user({ id: 'new' })])

        expect(state.searchUsers).toEqual([user({ id: 'new' })])
      })
    },

    concatSearchUsers: (mutation, getState) => {
      it('should append to the search result list', () => {
        const state = getState()
        state.searchUsers = [user({ id: 'a' })]

        mutation(state, [user({ id: 'b' })])

        expect(state.searchUsers.map(u => u.id)).toEqual(['a', 'b'])
      })
    }
  },

  actions: {
    findUsersByCandidates: (action, getContext) => {
      it('should look up the given ids and append them to both lists', async () => {
        const context = getContext()
        AdminService.findUsers.mockResolvedValue([user({ id: 'a' }), user({ id: 'b' })])

        await action(context, { idIn: ['a', 'b'] })

        expect(AdminService.findUsers).toHaveBeenCalledWith({ idIn: 'a,b' })
        expect(context.commit).toHaveBeenCalledWith('concatCandidateUsers', [user({ id: 'a' }), user({ id: 'b' })])
        expect(context.commit).toHaveBeenCalledWith('concatSearchUsers', [user({ id: 'a' }), user({ id: 'b' })])
      })

      it('should not call the service for an empty id list', () => {
        const context = getContext()

        const result = action(context, { idIn: [] })

        expect(result).toBeUndefined()
        expect(AdminService.findUsers).not.toHaveBeenCalled()
        expect(context.commit).not.toHaveBeenCalled()
      })
    },

    findUsers: (action, getContext) => {
      // With candidates already loaded the filtering happens client-side, so no request.
      it('should filter locally when candidates are present', async () => {
        const context = getContext()
        context.state.listCandidates = [
          user({ id: 'alice', firstName: 'Alice', lastName: 'Adams' }),
          user({ id: 'bob', firstName: 'Bob', lastName: 'Brown' })
        ]

        await action(context, { filter: 'ali' })

        expect(AdminService.findUsers).not.toHaveBeenCalled()
        expect(context.commit).toHaveBeenCalledWith('setSearchUsers', [
          user({ id: 'alice', firstName: 'Alice', lastName: 'Adams' })
        ])
      })

      it('should match a candidate on an exact, case-insensitive id', async () => {
        const context = getContext()
        context.state.listCandidates = [user({ id: 'Alice', firstName: 'Zed', lastName: 'Zulu' })]

        await action(context, { filter: 'alice' })

        expect(context.commit).toHaveBeenCalledWith('setSearchUsers', [
          user({ id: 'Alice', firstName: 'Zed', lastName: 'Zulu' })
        ])
      })

      it('should match a candidate on a last-name substring', async () => {
        const context = getContext()
        context.state.listCandidates = [user({ id: 'x', firstName: 'Zed', lastName: 'Brown' })]

        await action(context, { filter: 'row' })

        expect(context.commit).toHaveBeenCalledWith('setSearchUsers', [
          user({ id: 'x', firstName: 'Zed', lastName: 'Brown' })
        ])
      })

      it('should tolerate candidates without first or last names', async () => {
        const context = getContext()
        context.state.listCandidates = [{ id: 'nameless' }]

        await action(context, { filter: 'zzz' })

        expect(context.commit).toHaveBeenCalledWith('setSearchUsers', [])
      })

      // With no candidates the store fans out to three queries and concatenates them.
      it('should query first name, last name and id when no candidates are loaded', async () => {
        const context = getContext()
        AdminService.findUsers
          .mockResolvedValueOnce([user({ id: 'byFirst' })])
          .mockResolvedValueOnce([user({ id: 'byLast' })])
          .mockResolvedValueOnce([user({ id: 'byId' })])

        await action(context, { filter: 'dem', maxResults: 10, likePatternIgnoreCase: true })

        expect(AdminService.findUsers).toHaveBeenNthCalledWith(1, {
          firstNameLike: '*dem*', maxResults: 10, likePatternIgnoreCase: true
        })
        expect(AdminService.findUsers).toHaveBeenNthCalledWith(2, {
          lastNameLike: '*dem*', maxResults: 10, likePatternIgnoreCase: true
        })
        expect(AdminService.findUsers).toHaveBeenNthCalledWith(3, { id: 'dem' })
        expect(context.commit).toHaveBeenCalledWith('setSearchUsers', [
          user({ id: 'byFirst' }), user({ id: 'byLast' }), user({ id: 'byId' })
        ])
      })
    },

    fetchUsersByIds: (action, getContext) => {
      it('should return a map keyed by user id', async () => {
        AdminService.findUsers.mockResolvedValue([user({ id: 'a' }), user({ id: 'b' })])

        const result = await action(getContext(), ['a', 'b'])

        expect(AdminService.findUsers).toHaveBeenCalledWith({ idIn: 'a,b' })
        expect(result).toEqual({ a: user({ id: 'a' }), b: user({ id: 'b' }) })
      })

      it.each([[undefined], [null], [[]]])('should resolve to an empty map for %j without calling the service', async (ids) => {
        const result = await action(getContext(), ids)

        expect(result).toEqual({})
        expect(AdminService.findUsers).not.toHaveBeenCalled()
      })
    }
  }
})
