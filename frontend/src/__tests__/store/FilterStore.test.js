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
import FilterStore from '../../store/FilterStore.js'
import { FilterService, TaskService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  FilterService: {
    findFilters: vi.fn(),
    updateFilter: vi.fn(),
    createFilter: vi.fn(),
    deleteFilter: vi.fn()
  },
  TaskService: {
    findTasksCountByFilter: vi.fn()
  }
}))

const FAVORITES_KEY = 'favoriteFilters'

const filter = (overrides = {}) => ({ id: 'f-1', name: 'My tasks', favorite: false, ...overrides })

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
})

createStoreTestSuite('FilterStore', FilterStore, {
  initialState: (getState) => {
    it('should start with an empty list and a blank Task selection', () => {
      const state = getState()
      expect(state.list).toEqual([])
      expect(state.selected.resourceType).toBe('Task')
      expect(state.selected.id).toBeNull()
      expect(state.selected.properties).toEqual({
        color: '#555555',
        showUndefinedVariable: false,
        description: '',
        refresh: true,
        priority: 50
      })
    })

    it('should expose boolean reminder and dueDate settings', () => {
      const state = getState()
      expect(typeof state.settings.reminder).toBe('boolean')
      expect(typeof state.settings.dueDate).toBe('boolean')
    })
  },

  mutations: {
    setFilters: (mutation, getState) => {
      it('should flag filters that are stored as favorites', () => {
        const state = getState()
        localStorage.setItem(FAVORITES_KEY, JSON.stringify(['f-2']))

        mutation(state, { filters: [filter({ id: 'f-1' }), filter({ id: 'f-2' })] })

        expect(state.list.find(f => f.id === 'f-2').favorite).toBe(true)
        expect(state.list.find(f => f.id === 'f-1').favorite).toBe(false)
      })

      // Favorites are stored by id, so ids of filters deleted server-side must be pruned
      // or they accumulate in localStorage forever.
      it('should prune favorite ids that no longer exist', () => {
        const state = getState()
        localStorage.setItem(FAVORITES_KEY, JSON.stringify(['f-1', 'gone']))

        mutation(state, { filters: [filter({ id: 'f-1' })] })

        expect(JSON.parse(localStorage.getItem(FAVORITES_KEY))).toEqual(['f-1'])
      })

      it('should cope with no favorites stored at all', () => {
        const state = getState()

        mutation(state, { filters: [filter()] })

        expect(state.list.every(f => f.favorite === false)).toBe(true)
        expect(JSON.parse(localStorage.getItem(FAVORITES_KEY))).toEqual([])
      })
    },

    updateFilter: (mutation, getState) => {
      it('should replace the filter at the given index', () => {
        const state = getState()
        state.list = [filter({ id: 'a' }), filter({ id: 'b' })]

        mutation(state, { index: 1, filter: filter({ id: 'b', name: 'renamed' }) })

        expect(state.list[1].name).toBe('renamed')
        expect(state.list[0].id).toBe('a')
      })
    },

    addFilter: (mutation, getState) => {
      it('should append the new filter', () => {
        const state = getState()
        state.list = [filter({ id: 'a' })]

        mutation(state, { filter: filter({ id: 'b' }) })

        expect(state.list.map(f => f.id)).toEqual(['a', 'b'])
      })
    },

    deleteFilter: (mutation, getState) => {
      it('should drop the filter with the given id', () => {
        const state = getState()
        state.list = [filter({ id: 'a' }), filter({ id: 'b' })]

        mutation(state, { filterId: 'a' })

        expect(state.list.map(f => f.id)).toEqual(['b'])
      })
    },

    addFavoriteFilter: (mutation, getState) => {
      it('should flag the filter and persist its id', () => {
        const state = getState()
        state.list = [filter({ id: 'f-1' })]

        mutation(state, { filterId: 'f-1' })

        expect(state.list[0].favorite).toBe(true)
        expect(JSON.parse(localStorage.getItem(FAVORITES_KEY))).toEqual(['f-1'])
      })

      it('should append to already persisted favorites', () => {
        const state = getState()
        state.list = [filter({ id: 'f-1' })]
        localStorage.setItem(FAVORITES_KEY, JSON.stringify(['other']))

        mutation(state, { filterId: 'f-1' })

        expect(JSON.parse(localStorage.getItem(FAVORITES_KEY))).toEqual(['other', 'f-1'])
      })
    },

    deleteFavoriteFilter: (mutation, getState) => {
      it('should unflag the filter and drop its persisted id', () => {
        const state = getState()
        state.list = [filter({ id: 'f-1', favorite: true })]
        localStorage.setItem(FAVORITES_KEY, JSON.stringify(['f-1', 'other']))

        mutation(state, { filterId: 'f-1' })

        expect(state.list[0].favorite).toBe(false)
        expect(JSON.parse(localStorage.getItem(FAVORITES_KEY))).toEqual(['other'])
      })

      it('should unflag the filter when nothing is persisted', () => {
        const state = getState()
        state.list = [filter({ id: 'f-1', favorite: true })]

        mutation(state, { filterId: 'f-1' })

        expect(state.list[0].favorite).toBe(false)
        expect(localStorage.getItem(FAVORITES_KEY)).toBeNull()
      })
    },

    updateFilterTasksCount: (mutation, getState) => {
      it('should update the count on the matching list entry', () => {
        const state = getState()
        state.list = [filter({ id: 'f-1' }), filter({ id: 'f-2' })]

        mutation(state, { filterId: 'f-1', tasksNumber: 5, tasksNumberLastUpdated: 1000 })

        expect(state.list[0].tasksNumber).toBe(5)
        expect(state.list[0].tasksNumberLastUpdated).toBe(1000)
        expect(state.list[1].tasksNumber).toBeUndefined()
      })

      // The selected filter is a separate object from the list entry, so the badge in the
      // nav bar only refreshes if it is updated too.
      it('should also update the selected filter when it is the same one', () => {
        const state = getState()
        state.list = [filter({ id: 'f-1' })]
        state.selected.id = 'f-1'

        mutation(state, { filterId: 'f-1', tasksNumber: 7, tasksNumberLastUpdated: 2000 })

        expect(state.selected.tasksNumber).toBe(7)
        expect(state.selected.tasksNumberLastUpdated).toBe(2000)
      })

      it('should leave the selected filter alone when a different one changed', () => {
        const state = getState()
        state.list = [filter({ id: 'f-1' })]
        state.selected.id = 'f-2'

        mutation(state, { filterId: 'f-1', tasksNumber: 7, tasksNumberLastUpdated: 2000 })

        expect(state.selected.tasksNumber).toBeUndefined()
      })

      it('should be a no-op for an unknown filter id', () => {
        const state = getState()
        state.list = [filter({ id: 'f-1' })]

        expect(() => mutation(state, { filterId: 'missing', tasksNumber: 1, tasksNumberLastUpdated: 1 })).not.toThrow()
        expect(state.list[0].tasksNumber).toBeUndefined()
      })
    }
  },

  getters: {
    selectedFilterTasksNumber: (getter, getState) => {
      it('should return the selected task count', () => {
        const state = getState()
        state.selected.tasksNumber = 5
        expect(getter(state)).toBe(5)
      })

      it('should fall back to 0 when the count is unknown', () => {
        expect(getter(getState())).toBe(0)
      })
    },

    selectedFilterTasksNumberLastUpdated: (getter, getState) => {
      it('should return the last update timestamp', () => {
        const state = getState()
        state.selected.tasksNumberLastUpdated = 1234
        expect(getter(state)).toBe(1234)
      })

      it('should fall back to 0 when never updated', () => {
        expect(getter(getState())).toBe(0)
      })
    }
  },

  actions: {
    findFilters: (action, getContext) => {
      it('should delegate to the filter service', async () => {
        FilterService.findFilters.mockResolvedValue([filter()])

        const result = await action(getContext())

        expect(FilterService.findFilters).toHaveBeenCalled()
        expect(result).toEqual([filter()])
      })
    },

    updateFilter: (action, getContext) => {
      it('should persist the change and update the list entry in place', async () => {
        const context = getContext()
        context.state.list = [filter({ id: 'a' }), filter({ id: 'f-1' })]
        FilterService.updateFilter.mockResolvedValue(undefined)

        await action(context, { filter: filter({ id: 'f-1', name: 'renamed' }) })

        expect(FilterService.updateFilter).toHaveBeenCalledWith(filter({ id: 'f-1', name: 'renamed' }))
        expect(context.commit).toHaveBeenCalledWith('updateFilter', {
          index: 1,
          filter: filter({ id: 'f-1', name: 'renamed' })
        })
      })

      // Guards against updating a filter the store does not know about, which would
      // otherwise write to index -1.
      it('should reject without calling the service for an unknown filter', async () => {
        const context = getContext()
        context.state.list = []

        await expect(action(context, { filter: filter({ id: 'f-1' }) })).rejects.toBeUndefined()
        expect(FilterService.updateFilter).not.toHaveBeenCalled()
        expect(context.commit).not.toHaveBeenCalled()
      })
    },

    createFilter: (action, getContext) => {
      it('should create then add the filter returned by the server', async () => {
        const context = getContext()
        FilterService.createFilter.mockResolvedValue(filter({ id: 'server-id' }))

        const result = await action(context, { filter: filter({ id: null }) })

        expect(FilterService.createFilter).toHaveBeenCalledWith(filter({ id: null }))
        expect(context.commit).toHaveBeenCalledWith('addFilter', { filter: filter({ id: 'server-id' }) })
        expect(result).toEqual(filter({ id: 'server-id' }))
      })
    },

    deleteFilter: (action, getContext) => {
      it('should delete then drop the filter from state', async () => {
        const context = getContext()
        FilterService.deleteFilter.mockResolvedValue(undefined)

        await action(context, { filterId: 'f-1' })

        expect(FilterService.deleteFilter).toHaveBeenCalledWith('f-1')
        expect(context.commit).toHaveBeenCalledWith('deleteFilter', { filterId: 'f-1' })
      })

      it('should not touch state when the deletion fails', async () => {
        const context = getContext()
        FilterService.deleteFilter.mockRejectedValue(new Error('Forbidden'))

        await expect(action(context, { filterId: 'f-1' })).rejects.toThrow('Forbidden')
        expect(context.commit).not.toHaveBeenCalled()
      })
    },

    addFavoriteFilter: (action, getContext) => {
      it('should forward the id to the mutation', () => {
        const context = getContext()
        action(context, { filterId: 'f-1' })
        expect(context.commit).toHaveBeenCalledWith('addFavoriteFilter', { filterId: 'f-1' })
      })
    },

    deleteFavoriteFilter: (action, getContext) => {
      it('should forward the id to the mutation', () => {
        const context = getContext()
        action(context, { filterId: 'f-1' })
        expect(context.commit).toHaveBeenCalledWith('deleteFavoriteFilter', { filterId: 'f-1' })
      })
    },

    updateFilterTasksCount: (action, getContext) => {
      it('should fetch the count and commit it with a timestamp', async () => {
        const context = getContext()
        TaskService.findTasksCountByFilter.mockResolvedValue(5)
        vi.spyOn(Date, 'now').mockReturnValue(1717171717)

        const result = await action(context, { filterId: 'f-1', filters: { assignee: 'demo' } })

        expect(TaskService.findTasksCountByFilter).toHaveBeenCalledWith('f-1', { assignee: 'demo' })
        expect(context.commit).toHaveBeenCalledWith('updateFilterTasksCount', {
          filterId: 'f-1', tasksNumber: 5, tasksNumberLastUpdated: 1717171717
        })
        expect(result).toEqual({ tasksNumber: 5, tasksNumberLastUpdated: 1717171717 })

        Date.now.mockRestore()
      })

      it('should default to an empty filter payload', async () => {
        const context = getContext()
        TaskService.findTasksCountByFilter.mockResolvedValue(0)

        await action(context, { filterId: 'f-1' })

        expect(TaskService.findTasksCountByFilter).toHaveBeenCalledWith('f-1', {})
      })
    }
  }
})

// The module body migrates legacy persisted settings on first import, so these cases need
// a fresh module registry with localStorage primed beforehand.
describe('FilterStore module initialisation', () => {
  afterEach(() => {
    vi.resetModules()
    localStorage.clear()
  })

  const importFresh = async () => {
    vi.resetModules()
    return (await import('../../store/FilterStore.js')).default
  }

  it('should drop the obsolete filterSettings entry', async () => {
    localStorage.setItem('filterSettings', JSON.stringify({ reminder: true }))

    await importFresh()

    expect(localStorage.getItem('filterSettings')).toBeNull()
  })

  it('should adopt persisted addFilterSettings', async () => {
    localStorage.setItem('addFilterSettings', JSON.stringify({ reminder: true, dueDate: true }))

    const store = await importFresh()

    expect(store.state.settings).toEqual({ reminder: true, dueDate: true })
  })

  it('should default to false when no settings are persisted', async () => {
    const store = await importFresh()

    expect(store.state.settings).toEqual({ reminder: false, dueDate: false })
  })

  // A non-boolean `reminder` is the legacy shape; it is reset and re-persisted.
  it('should repair settings whose reminder is not a boolean', async () => {
    localStorage.setItem('addFilterSettings', JSON.stringify({ reminder: 'yes', dueDate: 'yes' }))

    const store = await importFresh()

    expect(store.state.settings).toEqual({ reminder: false, dueDate: false })
    expect(JSON.parse(localStorage.getItem('filterSettings'))).toEqual({ reminder: false, dueDate: false })
  })
})
