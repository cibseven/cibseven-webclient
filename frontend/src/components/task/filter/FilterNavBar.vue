<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
<template>
  <div v-show="showFilters" class="overflow-auto h-100">
    <div class="h-100 d-flex flex-column">

      <FilterModal ref="filterModal" @select-filter="selectFilter($event)" @filter-alert="$emit('filter-alert', $event)" @set-filter="$emit('set-filter', $event)"></FilterModal>

      <div v-if="$root.config.filtersSearch" class="p-2" style="background-color: rgb(98, 142, 199, 0.3)">
        <b-input-group>
          <template v-slot:prepend>
            <b-input-group-text class="py-0 border-light"><span class="mdi mdi-18px mdi-magnify"
            style="line-height: initial"></span></b-input-group-text>
          </template>
          <b-form-input :title="$t('searches.searchByFilterName')" size="sm" ref="input" type="search" v-model.trim="filter"
          class="form-control border-start-0 ps-0 form-control border-light shadow-none" :placeholder="$t('searches.searchByFilterName')"/>
          <template #append>
            <b-button size="sm" v-hover-style="{ classes: ['text-primary'] }" variant="secondary-outline"
              @click="sortOrder === 'desc' ? sortOrder = 'asc' : sortOrder = 'desc'" class="mdi mdi-16px py-0"
              :class="sortOrder === 'desc' ? 'mdi-arrow-down' : 'mdi-arrow-up'"
              :title="sortOrder === 'desc' ? $t('sorting.desc') : $t('sorting.asc')">
              <span v-if="sortOrder === 'desc'" class="visually-hidden">{{ $t('sorting.desc') }}</span>
              <span v-else class="visually-hidden">{{ $t('sorting.asc') }}</span>
            </b-button>
          </template>
        </b-input-group>
      </div>
      <div v-if="filtersFiltered.length > 0" class="overflow-auto flex-fill">
        <b-list-group>
          <b-list-group-item v-for="filter of filtersFiltered" :key="filter.id" @mouseenter="focused = filter" @mouseleave="focused = null"
            action class="border-0 rounded-0 p-2" :class="filter.id === $store.state.filter.selected.id ? 'active' : ''" :to="'/seven/auth/tasks/' + filter.id">
            <div class="d-flex align-items-center">
              <div class="col-7 p-0" style="word-wrap: break-word">
                <span>{{ filter.name }} <b-badge v-if="filter.tasksNumber" pill variant="light">{{ filter.tasksNumber }}</b-badge></span>
              </div>
              <div :class="getClasses(filter)" class="ms-auto">
                <button v-if="filterByPermissions($root.config.permissions.editFilter, $store.state.filter.selected)"
                  class="btn btn-outline-secondary btn-sm border-0" type="button" :title="$t('nav-bar.filters.edit')" @click.stop="selectFilter(filter); showFilterDialog('edit')">
                  <span class="visually-hidden">{{ $t('nav-bar.filters.edit') }}</span>
                  <span class="mdi mdi-pencil"></span>
                </button>
                <button v-if="filterByPermissions($root.config.permissions.deleteFilter, $store.state.filter.selected)"
                  class="btn btn-outline-secondary btn-sm border-0" type="button" :title="$t('nav-bar.filters.delete')"
                  @click.stop="workingFilter = filter; $refs.confirmDeleteFilter.show()">
                  <span class="visually-hidden">{{ $t('nav-bar.filters.delete') }}</span>
                  <span class="mdi mdi-close"></span>
                </button>
              </div>
              <button v-if="filter.favorite" class="btn btn-outline-secondary btn-sm border-0" type="button"
                @click.stop="deleteFavoriteFilter(filter)" :title="$t('nav-bar.filters.pin')">
                <span class="visually-hidden">{{ $t('nav-bar.filters.pin') }}</span>
                <span class="mdi mdi-pin text-dark"></span>
              </button>
              <button v-else class="btn btn-outline-secondary btn-sm border-0" type="button"
                @click.stop="setFavoriteFilter(filter)" :title="$t('nav-bar.filters.pin')">
                <span class="visually-hidden">{{ $t('nav-bar.filters.pin') }}</span>
                <span class="mdi mdi-pin text-muted"></span>
              </button>
            </div>
          </b-list-group-item>
        </b-list-group>
      </div>
      <BWaitingBox ref="filterLoader" class="d-flex flex-fill justify-content-center pt-4" styling="width:30%">
        <div v-if="filtersFiltered.length < 1">
          <img src="@/assets/images/task/no_tasks.svg" class="d-block mx-auto mt-3 mb-2" style="width: 200px">
          <div class="h5 text-secondary text-center">{{ $t('nav-bar.filters.no-filters') }}</div>
        </div>
      </BWaitingBox>
      <button v-if="filterByPermissions($root.config.permissions.createFilter, null, true)"
        class="btn btn-outline-secondary btn-sm border-light p-2" type="button"
        @click="showFilterDialog('create')">
        <span class="mdi mdi-18px mdi-filter-plus-outline"></span><span class="ms-2">{{ $t('nav-bar.filters.create') }}</span>
      </button>
    </div>
    <ConfirmDialog ref="confirmDeleteFilter" @ok="deleteFilter()" :ok-title="$t('confirm.delete')">
      <i18n-t keypath="confirm.deleteFilter" tag="span" scope="global">
        <template #name>
          <strong>{{ workingFilter.name }}</strong>
        </template>
      </i18n-t>
    </ConfirmDialog>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import FilterModal from '@/components/task/filter/FilterModal.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import { BWaitingBox } from 'cib-common-components'

import { TaskPool } from "@/taskpool.js"
import { TaskService } from '@/services.js'

const MIN_TASKNUMBER_INTERVAL = 10000

export default {
  name: 'FilterNavBar',
  components: { FilterModal, ConfirmDialog, BWaitingBox },
  mixins: [permissionsMixin],
  emits: ['filter-alert', 'n-filters-shown', 'selected-filter', 'set-filter'],
  data: function () {
    return {
      filter: '',
      currentSorting: {},
      showFilters: true,
      selected: null,
      focused: null,
      sortOrder: '',
      interval: null,
      taskpool: new TaskPool(5),
      workingFilter: {},
      isSelectingFilter: false
    }
  },
  watch: {
    '$route.params.filterId': function(to) {
      if (this.isSelectingFilter) {
        this.isSelectingFilter = false
        return
      }
      if (this.$route.query.filtername) {
        this.setFilterByName()
      } else this.checkFilterIdInUrl(to)
    },
    '$store.state.filter.settings': {
      deep: true,
      handler: function() {
        localStorage.setItem('addFilterSettings', JSON.stringify(this.$store.state.filter.settings))
      }
    }
  },
  computed: {
    filtersFiltered: function() {
      var filters = []
      if (this.$store.state.filter.list) {
        filters = this.$store.state.filter.list.filter(filter => {
          return (filter.name) ? filter.name.toUpperCase().includes(this.filter.toUpperCase()) : false
        })
      }
      this.$emit('n-filters-shown', filters.length)
      return filters.sort((a, b) => {
        if (!this.$root.config.filtersSearch || !this.sortOrder) {
          return a.properties.priority - b.properties.priority
        }
        const nameA = a.name.toLowerCase()
        const nameB = b.name.toLowerCase()
        let comparison = nameA.localeCompare(nameB)
        if (comparison === 0) {
          comparison = a.properties.priority - b.properties.priority
        }
        return this.sortOrder === 'desc' ? comparison : -comparison
      })
    }
  },
  mounted: function() {
    this.fetchFilters()
  },
  methods: {
    fetchFilters: function() {
      this.$refs.filterLoader.done = false
      this.$store.dispatch('findFilters').then(response => {
        this.$store.commit('setFilters',
          { filters: this.filtersByPermissions(this.$root.config.permissions.displayFilter, response) })
        if (this.$root.config.taskFilter.tasksNumber.enabled) {
          this.setTasksNumber()
          const interval = Math.max(this.$root.config.taskFilter.tasksNumber.interval, MIN_TASKNUMBER_INTERVAL)
          this.interval = setInterval(() => { this.setTasksNumber() }, interval)
        }
        if (this.$route.query.filtername) {
          this.setFilterByName()
        } else this.checkFilterIdInUrl(this.$route.params.filterId)
        this.$refs.filterLoader.done = true
      })
    },
    setTasksNumber: function() {
      this.$store.state.filter.list.forEach(f => {
        this.taskpool.add(TaskService.findTasksCountByFilter, [f.id, {}]).then(tasksNumber => {
          f.tasksNumber = tasksNumber
        })
      })
    },
    setFilterByName: function() {
      var selectedFilter = this.$store.state.filter.list.find(filter => {
        return filter.name === this.$route.query.filtername
      })
      if (selectedFilter) {
        this.selectFilter(selectedFilter)
      } else this.checkFilterIdInUrl(this.$route.params.filterId)
    },
    checkFilterIdInUrl: function(filterId) {
      if (this.$store.state.filter.list.length > 0 && filterId) {
        var filterStore = this.$store.state.filter.list.find(filter => {
          return filter.id === filterId
        })
        if (filterStore) this.selectFilter(filterStore)
        else this.selectFilter({})
      } else this.selectFilter({})
    },
    selectFilter: function(filter) {
      if (this.$store.state.filter.list.length > 0) {
        var taskId = this.$route.query.filtername ? this.$route.params.filterId : this.$route.params.taskId
        var selectedFilter = this.$store.state.filter.list.find(f => {
          return f.id === filter.id
        })
        try {
          //Use of '*' as special character when we don't specify the filter on a link
          selectedFilter = (!this.$route.params.filterId || this.$route.params.filterId === '*') && localStorage.getItem('filter') ?
            JSON.parse(localStorage.getItem('filter')) : selectedFilter
        } catch(error) {
          console.error('Filter format wrong: corrected')
          console.error(error)
        }
        if ((!this.$route.params.filterId || selectedFilter) || !selectedFilter) {
          this.$store.state.filter.selected = selectedFilter || this.$store.state.filter.list[0]
          this.$emit('selected-filter', this.$store.state.filter.selected.id)
          localStorage.setItem('filter', JSON.stringify(this.$store.state.filter.selected))
          var filterId = this.$route.params.filterId === '*' ? '*' : this.$store.state.filter.selected.id
          var path = '/seven/auth/tasks/' + filterId + (taskId ? '/' + taskId : '')
          if (this.$route.path !== path) {
            this.isSelectingFilter = true
            this.$router.replace(path)
          }
        }
        if (this.$store.state.filter.selected) {
          const f = this.$store.state.filter.selected
          if (f && f.id && !this.$root.config.taskFilter.tasksNumber.enabled) {
            TaskService.findTasksCountByFilter(f.id, {}).then(tasksNumber =>
              this.saveTasksCountInStore(f.id, tasksNumber)
            )
          }
        }
      }
    },
    getClasses: function(filter) {
      var classes = []
      if (filter !== this.focused) classes.push('invisible')
      return classes
    },
    deleteFilter: function() {
      this.$store.dispatch('deleteFilter', { filterId: this.workingFilter.id }).then(() => {
        this.$emit('filter-alert', { message: 'msgFilterDeleted', filter: this.workingFilter.name })
        localStorage.removeItem('filter')
		if (this.$store.state.filter.selected === this.workingFilter && this.$store.state.filter.list[0]) {
			this.selectFilter(this.$store.state.filter.list[0])
			this.workingFilter = {}
		} else if (!this.$store.state.filter.list[0]) this.$router.push('/seven/auth/tasks')
      })
    },
    showFilterDialog: function(mode) {
      this.$refs.filterModal.showFilterDialog(mode)
    },
    setFavoriteFilter: function(filter) {
      this.$store.dispatch('addFavoriteFilter', { filterId: filter.id })
    },
    deleteFavoriteFilter: function(filter) {
      this.$store.dispatch('deleteFavoriteFilter', { filterId: filter.id })
    },
    saveTasksCountInStore(filterId, tasksNumber) {
      const newFilters = this.$store.state.filter.list.map(f => f.id === filterId ? { ...f, tasksNumber } : f)
      this.$store.commit('setFilters', { filters: newFilters })
    }
  },
  beforeUnmount: function() {
    clearInterval(this.interval)
    this.taskpool.clear()
  }
}
</script>

<style lang="css" scoped>
</style>
