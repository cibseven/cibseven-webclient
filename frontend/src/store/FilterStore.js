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

import { FilterService, TaskService } from '@/services.js'

if (localStorage.getItem('filterSettings')) {
  localStorage.removeItem('filterSettings')
}

var settings = JSON.parse(localStorage.getItem('addFilterSettings') || JSON.stringify({
  reminder: false,
  dueDate: false
}))

if (typeof settings.reminder !== 'boolean') {
  settings.reminder = false
  settings.dueDate = false
  localStorage.setItem('filterSettings', JSON.stringify(settings))
}

const FilterStore = {
  state: {
    list: [],
    selected: {
      id: null,
      resourceType: 'Task',
      name: '',
      owner: null,
      query: {},
          properties: {
              color: '#555555',
              showUndefinedVariable: false,
              description: '',
              refresh: true,
              priority: 50
          }
    },
    settings: settings
  },
  mutations: {
    setFilters: function(state, params) {
      var favorites = localStorage.getItem('favoriteFilters') ?
        JSON.parse(localStorage.getItem('favoriteFilters')) : []
      params.filters.forEach(f => {
        if (favorites.includes(f.id)) f.favorite = true
        else f.favorite = false
      })
      favorites = favorites.filter(fid => {
        return params.filters.find(fl => {
          return fl.id === fid
        })
      })
      localStorage.setItem('favoriteFilters', JSON.stringify(favorites))
      state.list = params.filters
    },
    updateFilter: function(state, params) {
      state.list[params.index] = params.filter
    },
    addFilter: function(state, params) {
      state.list.push(params.filter)
    },
    deleteFilter: function(state, params) {
      state.list = state.list.filter(filter => {
        return filter.id !== params.filterId
      })
    },
    deleteFavoriteFilter: function(state, params) {
      var indx = state.list.findIndex(f => {
        return f.id === params.filterId
      })
      state.list[indx].favorite = false
      var favorites = localStorage.getItem('favoriteFilters')
      if (favorites) {
        favorites = JSON.parse(favorites)
        favorites = favorites.filter(f => {
          return f !== params.filterId
        })
        localStorage.setItem('favoriteFilters', JSON.stringify(favorites))
      }
    },
    addFavoriteFilter: function(state, params) {
      var indx = state.list.findIndex(f => {
        return f.id === params.filterId
      })
      state.list[indx].favorite = true
      var favorites = localStorage.getItem('favoriteFilters') ?
        JSON.parse(localStorage.getItem('favoriteFilters')) : []
      favorites.push(params.filterId)
      localStorage.setItem('favoriteFilters', JSON.stringify(favorites))
    },
    updateFilterTasksCount: function(state, params) {
      // Update the filter in the list
      const filterIndex = state.list.findIndex(f => f.id === params.filterId)
      if (filterIndex !== -1) {
        state.list[filterIndex].tasksNumber = params.tasksNumber
        state.list[filterIndex].tasksNumberLastUpdated = params.tasksNumberLastUpdated
      }
      
      // Update the selected filter if it matches
      if (state.selected.id === params.filterId) {
        state.selected.tasksNumber = params.tasksNumber
        state.selected.tasksNumberLastUpdated = params.tasksNumberLastUpdated
      }
    }
  },
  getters: {
    selectedFilterTasksNumber: (state) => {
      return state.selected.tasksNumber || 0
    },
    selectedFilterTasksNumberLastUpdated: (state) => {
      return state.selected.tasksNumberLastUpdated || 0
    }
  },
  actions: {
    findFilters: function() {
      return FilterService.findFilters()
    },
    updateFilter: function(ctx, params) {
      var selectedFilterIndx = ctx.state.list.findIndex(filter => {
        return filter.id === params.filter.id
      })
      if (selectedFilterIndx > -1) {
        return FilterService.updateFilter(params.filter).then(() => {
          ctx.commit('updateFilter', { index: selectedFilterIndx, filter: params.filter })
          return Promise.resolve()
        })
      } else return Promise.reject()
    },
    createFilter: function(ctx, params) {
      return FilterService.createFilter(params.filter).then(filter => {
        ctx.commit('addFilter', { filter: filter })
        return Promise.resolve(filter)
      })
    },
    deleteFilter: function(ctx, params) {
      return FilterService.deleteFilter(params.filterId).then(() => {
        ctx.commit('deleteFilter', { filterId: params.filterId })
      })
    },
    addFavoriteFilter: function(ctx, params) {
      ctx.commit('addFavoriteFilter', { filterId: params.filterId })
    },
    deleteFavoriteFilter: function(ctx, params) {
      ctx.commit('deleteFavoriteFilter', { filterId: params.filterId })
    },
    updateFilterTasksCount: function(ctx, params) {
      return TaskService.findTasksCountByFilter(params.filterId, params.filters || {}).then(tasksNumber => {
        const tasksNumberLastUpdated = Date.now()
        ctx.commit('updateFilterTasksCount', { 
          filterId: params.filterId, 
          tasksNumber, 
          tasksNumberLastUpdated 
        })
        return { tasksNumber, tasksNumberLastUpdated }
      })
    }
  }
}

export default FilterStore
