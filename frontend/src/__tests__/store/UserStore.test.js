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
      expect(getState()).toEqual({ listCandidates: [], searchUsers: [], userIdMap: {}, pendingUsersMap: {} })
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
    },

    addDisplayNames: (mutation, getState) => {
      it('should cache the loaded users and clear their pending flag', () => {
        const state = getState()
        state.pendingUsersMap = { a: true, other: true }

        mutation(state, { userIds: ['a'], userMap: { a: user({ id: 'a' }) } })

        expect(state.userIdMap).toEqual({ a: user({ id: 'a' }) })
        expect(state.pendingUsersMap).toEqual({ other: true })
      })

      // Unknown users are cached as null so that the next poll does not ask for them again.
      it('should cache ids missing from the response as null', () => {
        const state = getState()
        state.pendingUsersMap = { ghost: true }

        mutation(state, { userIds: ['ghost'], userMap: {} })

        expect(state.userIdMap).toEqual({ ghost: null })
        expect(state.pendingUsersMap).toEqual({})
      })

      it('should keep users cached earlier', () => {
        const state = getState()
        state.userIdMap = { old: user({ id: 'old' }) }

        mutation(state, { userIds: ['a'], userMap: { a: user({ id: 'a' }) } })

        expect(Object.keys(state.userIdMap)).toEqual(['old', 'a'])
      })
    },

    markPendingUsers: (mutation, getState) => {
      it('should flag the given ids as pending', () => {
        const state = getState()

        mutation(state, ['a', 'b'])

        expect(state.pendingUsersMap).toEqual({ a: true, b: true })
      })
    },

    clearPendingUsers: (mutation, getState) => {
      it('should remove only the given ids from the pending flags', () => {
        const state = getState()
        state.pendingUsersMap = { a: true, b: true }

        mutation(state, ['a'])

        expect(state.pendingUsersMap).toEqual({ b: true })
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
    },
    
    addUsersByIds: (action, getContext) => {
      const contextReturning = (userMap) => {
        const context = getContext()
        context.dispatch = vi.fn(() => Promise.resolve(userMap))
        return context
      }

      it('should fetch the missing ids once each, skipping unassigned tasks', async () => {
        const userMap = { a: user({ id: 'a' }) }
        const context = contextReturning(userMap)

        await action(context, ['a', null, 'a', undefined])

        expect(context.dispatch).toHaveBeenCalledWith('fetchUsersByIds', ['a'])
        expect(context.commit).toHaveBeenCalledWith('addDisplayNames', { userIds: ['a'], userMap })
      })

      // Marking before the request keeps a second call (e.g. the next poll) from asking again.
      it('should mark the ids as pending before fetching them', async () => {
        const context = contextReturning({})
        context.dispatch = vi.fn(() => {
          expect(context.commit).toHaveBeenCalledWith('markPendingUsers', ['a', 'b'])
          return Promise.resolve({})
        })

        await action(context, ['a', 'b'])

        expect(context.dispatch).toHaveBeenCalledOnce()
      })

      it('should not fetch ids that are already cached, including unknown ones', async () => {
        const context = contextReturning({})
        context.state.userIdMap = { a: user({ id: 'a' }), ghost: null }

        await action(context, ['a', 'ghost'])

        expect(context.dispatch).not.toHaveBeenCalled()
        expect(context.commit).not.toHaveBeenCalled()
      })

      it('should not fetch ids whose request is still pending', async () => {
        const context = contextReturning({})
        context.state.pendingUsersMap = { a: true }

        await action(context, ['a', 'b'])

        expect(context.dispatch).toHaveBeenCalledWith('fetchUsersByIds', ['b'])
      })

      it('should do nothing for an empty list', async () => {
        const context = contextReturning({})

        await action(context, [])

        expect(context.dispatch).not.toHaveBeenCalled()
        expect(context.commit).not.toHaveBeenCalled()
      })

      // User ids are case-sensitive (CIB7-136): "Demo" in the cache says nothing about "demo".
      it('should treat ids differing only in case as different users', async () => {
        const context = contextReturning({ demo: user({ id: 'demo' }) })
        context.state.userIdMap = { Demo: user({ id: 'Demo' }) }

        await action(context, ['demo'])

        expect(context.dispatch).toHaveBeenCalledWith('fetchUsersByIds', ['demo'])
      })

      // A failed request must not leave the ids pending forever, so the next poll retries them.
      it('should clear the pending flags and not cache anything when the request fails', async () => {
        const context = getContext()
        context.dispatch = vi.fn(() => Promise.reject(new Error('boom')))
        const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})

        await action(context, ['a'])

        expect(context.commit).toHaveBeenCalledWith('clearPendingUsers', ['a'])
        expect(context.commit).not.toHaveBeenCalledWith('addDisplayNames', expect.anything())
        expect(consoleError).toHaveBeenCalled()
        consoleError.mockRestore()
      })
    }
  }
})
