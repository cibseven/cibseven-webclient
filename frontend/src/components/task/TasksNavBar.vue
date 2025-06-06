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
  <div v-show="showTasks" class="overflow-auto h-100">
    <div class="h-100 d-flex flex-column border border-end-0 border-top-0 bg-light">

      <b-button style="top: 3px; right: 30px" class="border-0 position-absolute" v-if="$root.config.layout.showTaskListManualRefresh"
        variant="btn-outline-primary" size="sm" :title="$t('nav-bar.refresh')" @click="refreshTasks()" :style="pauseRefreshButton ? 'opacity: 0.5' : ''">
        <span class="visually-hidden">{{ $t('nav-bar.refresh') }}</span>
        <span class="mdi mdi-18px mdi-refresh"></span>
      </b-button>

      <div class="py-1 px-2 mb-1 bg-task-filter">
        <SmartSearch class="m-1 mb-2"
          :maxlength="50"
          :options="$root.config.taskFilter.smartSearch.options"
          @search-filter="$emit('search-filter', $event)"
          @open-advanced-search="$refs.advancedSearchModal.show()"></SmartSearch>
        <hr class="my-0">
        <div class="row g-0 d-flex align-items-center">
          <b-form-group class="mb-0 col-auto">
            <b-input-group size="sm" class="align-items-center">
              <b-dropdown ref="sortingList" size="sm" variant="link" class="me-1" toggle-class="text-decoration-none" :title="$t('sorting.sortBy')" no-caret>
                <template #button-content>
                  <i v-hover-style="{ classes: ['text-primary'] }" class="mdi mdi-18px mdi-filter-variant"></i><span class="visually-hidden">{{ $t('sorting.sortBy') }}</span>
                </template>
                <b-dd-item-btn v-for="item in filteredFields" :key="item" @click="taskSorting.sortBy = item; setSorting('key'); $refs.sortingList.hide()" :class="taskSorting.sortBy === item ? 'active' : ''">
                  {{ $t('sorting.' + item) }}
                </b-dd-item-btn>
              </b-dropdown>
              <b-button variant="link" class="text-decoration-none px-0" @click="$refs.sortingList.show()">{{ $t('sorting.' + taskSorting.sortBy) }}</b-button>
              <template v-slot:append>
                <b-button size="sm" v-hover-style="{ classes: ['text-primary'] }" variant="secondary-outline" @click="setSorting('order')" class="mdi mdi-18px ms-1"
                  :class="taskSorting.sortOrder === 'desc' ? 'mdi-arrow-down' : 'mdi-arrow-up'"
                  :title="taskSorting.sortOrder === 'desc' ? $t('sorting.desc') : $t('sorting.asc')">
                  <span v-if="taskSorting.sortOrder === 'desc'" class="visually-hidden">{{ $t('sorting.desc') }}</span>
                  <span v-else class="visually-hidden">{{ $t('sorting.asc') }}</span>
                </b-button>
              </template>
            </b-input-group>
          </b-form-group>
          <div v-if="$root.config.layout.showFilterReminderDate || $root.config.layout.showFilterDueDate ||
            ($root.config.taskFilter.advancedSearch.processVariables.length > 0 && $root.config.taskFilter.advancedSearch.filterEnabled)" class="ms-auto col-auto">
            <b-button ref="filters" v-hover-style="{ classes: ['text-primary'] }" variant="link" :title="$t('nav-bar.filtersAdditionalsTitle')">
              <span class="mdi mdi-18px"
                :class="$store.state.filter.settings.reminder || $store.state.filter.settings.dueDate || $store.state.advancedSearch.criterias.length > 0 ? 'mdi-filter-menu text-primary' : 'mdi-filter-menu-outline'"></span>
            </b-button>
          </div>
        </div>
      </div>
      <b-popover :target="function() { return $refs.filters }" placement="bottom" triggers="click blur" @show="loadAdvancedFilters">
        <div><h5 class="mt-3">{{ $t('task.showOnlyAddFilters') }}</h5></div>
        <hr class="my-2">
        <div class="mt-2">
          <b-form-checkbox v-if="$root.config.layout.showFilterReminderDate" v-model="$store.state.filter.settings.reminder">
            <h5 class="d-flex fw-normal"><span class="mdi mdi-16px mdi-alarm align-middle pe-1"></span> {{ $t('nav-bar.reminder') }}</h5>
          </b-form-checkbox>
        </div>
        <div>
          <b-form-checkbox v-if="$root.config.layout.showFilterDueDate" v-model="$store.state.filter.settings.dueDate">
            <h5 class="d-flex fw-normal"><span class="mdi mdi-16px mdi-calendar-alert align-middle pe-1"></span> {{ $t('nav-bar.dueDate') }}</h5>
          </b-form-checkbox>
        </div>
        <div v-if="$root.config.taskFilter.advancedSearch.filterEnabled">
          <div v-for="(criteria, index) in advancedFilter" :key="index">
            <b-form-checkbox v-model="criteria.check">
              <h5 class="d-flex fw-normal">{{ criteria.displayName }}</h5>
            </b-form-checkbox>
            <b-form-input v-if="criteria.check && criteria.defaultValue === ''" v-model="criteria.value" size="sm" class="mb-2"></b-form-input>
          </div>
        </div>
      </b-popover>
      <div class="overflow-auto flex-fill border-bottom" @scroll="handleScrollTasks">
        <div v-if="tasksFiltered.length > 0">
          <b-list-group class="mx-3">
            <b-list-group-item @click="selectedTask(task)" v-for="task of tasksFiltered" :key="task.id" :ref="'taskItem-' + task.id" @mouseenter="focused = task" @mouseleave="focused = null"
              class="rounded-0 mt-3 p-2 bg-white border-0" :class="task.id === $route.params.taskId ? 'active shadow' : ''" draggable="false"
              tabindex=0 style="cursor: pointer" v-on:keyup.enter="selectedTask(task)" action>
              <div class="d-flex align-items-center">
                <h6 style="max-width: 100%; font-size: 1rem">
                  <span class="fw-bold">{{ task.name }}</span>
                </h6>
                <div class="d-flex ms-auto">
                  <b-button @click="$refs['followUp' + task.id][0].show()" v-if="$root.config.layout.showFilterReminderDate" size="sm" :class="getReminderClasses(task)" variant="outline-secondary" class="mdi mdi-18px mdi-alarm border-0" :title="getDateFormatted(task.followUp, 'L', 'setReminder')"></b-button>
                  <b-button @click="$refs['due' + task.id][0].show()" v-if="$root.config.layout.showFilterDueDate" size="sm" :class="getDueClasses(task)" variant="outline-secondary" class="mdi mdi-18px mdi-calendar-alert border-0" :title="getDateFormatted(task.due, 'L', 'setDeadline')"></b-button>
                </div>
              </div>
              <div v-if="task.businessKey && $root.config.layout.showBusinessKey" class="d-flex align-items-center mb-1">
                <span :title="$t(task.businessKey)">{{ task.businessKey }}</span><br>
              </div>
              <div v-if="getProcessName(task.processDefinitionId)" class="fw-normal h5">
                {{ getProcessName(task.processDefinitionId) }}
              </div>
              <div class="d-flex align-items-center">
    <!-- 						<span class="mdi mdi-18px mdi-calendar-month mdi-dark"></span> -->
                <div class="h6 fw-normal m-0" :title="getDateFormatted(task.createdOriginal, 'L LTS')">{{ getDateFormatted(task.createdOriginal) }}</div><br>
                <div class="d-flex ms-auto">
                  <div class="h6 text-end p-0 fw-normal m-0" v-if="task.assignee != null"><span class="mdi mdi-18px mdi-account text-secondary"></span><span class="p-1">{{ getCompleteName(task) }}</span></div>
                  <div class="h6 text-end p-0 fw-normal n-0" v-if="task.assignee == null">
                    <b-button variant="link" class="p-0 text-dark" @click.stop="checkAssignee(task)"><span class="mdi mdi-18px mdi-account-question text-secondary"></span> {{ $t('task.assignToMe') }}</b-button>
                  </div>
                </div>
              </div>
              <b-modal :ref="'followUp' + task.id" @show="copyTaskForDateManagement(task, 'followUp')" @hide="selectedDateT = {}">
                <b-calendar @input="setTime(null, 'followUp')" v-model="selectedDateT.followUp" value-as-date :start-weekday="1" :locale="currentLanguage()" block
                :label-no-date-selected="$t('cib-datepicker2.noDate')" :date-format-options="{ year: 'numeric', month: '2-digit', day: '2-digit' }"
                :label-reset-button="$t('cib-datepicker2.reset')" :label-today-button="$t('cib-datepicker2.today')" :date-disabled-fn="isInThePast" label-help="">
                  <div class="d-flex">
                    <b-button size="sm" variant="outline-primary"  @click="selectedDateT.followUp = new Date();">
                      {{ $t('cib-datepicker2.today') }}
                    </b-button>
                    <b-button size="sm" variant="outline-danger" class="ms-auto" @click="selectedDateT.followUp = null">
                      {{ $t('cib-datepicker2.reset') }}
                    </b-button>
                  </div>
                </b-calendar>
                <template v-slot:modal-footer>
                  <b-button @click="$refs['followUp' + task.id][0].hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
                  <b-button @click="setDate(task, 'followUp')" variant="primary">{{ $t('task.setReminder') }}</b-button>
                </template>
              </b-modal>
              <b-modal :ref="'due' + task.id" @show="copyTaskForDateManagement(task, 'due')" @hide="selectedDateT = {}">
                <b-calendar @input="setTime(selectedDateT.dueTime, 'due')" v-model="selectedDateT.due" value-as-date :start-weekday="1" :locale="currentLanguage()" block
                :label-no-date-selected="$t('cib-datepicker2.noDate')" :date-format-options="{ year: 'numeric', month: '2-digit', day: '2-digit' }"
                :label-reset-button="$t('cib-datepicker2.reset')" :label-today-button="$t('cib-datepicker2.today')" label-help="">
                  <div class="d-flex">
                    <b-button size="sm" variant="outline-primary" @click="selectedDateT.due = new Date(); setTime(selectedDateT.dueTime, 'due')">
                      {{ $t('cib-datepicker2.today') }}
                    </b-button>
                    <b-button size="sm" variant="outline-danger" class="ms-auto" @click="selectedDateT.due = null">
                      {{ $t('cib-datepicker2.reset') }}
                    </b-button>
                  </div>
                </b-calendar>
                <hr>
                <b-form-timepicker v-model="selectedDateT.dueTime" @input="setTime($event, 'due')" no-close-button :label-no-time-selected="$t('cib-timepicker.noDate')"
                  reset-button class="flex-fill" reset-value="23:59:00" :label-reset-button="$t('cib-timepicker.reset')" :locale="currentLanguage()"></b-form-timepicker>
                <template v-slot:modal-footer>
                  <b-button :title="$t('confirm.cancel')" @click="$refs['due' + task.id][0].hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
                  <b-button :title="$t('task.setDeadline')" @click="setDate(task, 'due')" variant="primary">{{ $t('task.setDeadline') }}</b-button>
                </template>
              </b-modal>
            </b-list-group-item>
          </b-list-group>
  <!-- 				<div v-if="tasksFiltered.length >= taskResultsIndex" class="text-center mt-3"> -->
  <!-- 					<b-button variant="outline-secondary" @click="$emit('show-more')"> -->
  <!-- 						{{ $t('task.showMore') }} -->
  <!-- 					</b-button> -->
  <!-- 				</div> -->
        </div>
        <BWaitingBox v-show="tasksFiltered.length < 1" ref="taskLoader" class="d-flex justify-content-center pt-4" styling="width:30%">
          <div v-if="tasksFiltered.length < 1 && $store.state.filter.selected.name === 'default'">
            <img :alt="$t('nav-bar.no-tasks-pending')" src="@/assets/images/task/no_tasks_pending.svg" class="d-block mx-auto mt-3 mb-2" style="width: 200px">
            <div class="h5 text-secondary text-center">{{ $t('nav-bar.no-tasks-pending') }}</div>
          </div>
          <div v-if="tasksFiltered.length < 1 && $store.state.filter.selected.name !== 'default'">
            <img :alt="$t('nav-bar.no-tasks')" src="@/assets/images/task/no_tasks.svg" class="d-block mx-auto mt-3 mb-2" style="width: 200px">
            <div class="h5 text-secondary text-center">{{ $t('nav-bar.no-tasks') }}</div>
          </div>
        </BWaitingBox>
      </div>
      <StartProcess ref="startProcess"
        @process-started="$emit('process-started', $event)"></StartProcess>
      <AdvancedSearchModal v-if="$root.config.taskFilter.advancedSearch.modalEnabled" ref="advancedSearchModal" @refresh-tasks="$emit('refresh-tasks')"></AdvancedSearchModal>
    </div>
    <ConfirmDialog ref="confirmTaskAssign" @ok="claim($event)">
      <span>{{ $t('confirm.assignUser') }}</span>
    </ConfirmDialog>
  </div>
</template>

<script>
import { moment } from '@/globals.js'
import { TaskService, AdminService } from '@/services.js'
import { debounce } from '@/utils/debounce.js'
import StartProcess from '@/components/start-process/StartProcess.vue'
import AdvancedSearchModal from '@/components/task/AdvancedSearchModal.vue'
import SmartSearch from '@/components/task/SmartSearch.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'TasksNavBar',
  components: { StartProcess, AdvancedSearchModal, SmartSearch, ConfirmDialog, BWaitingBox },
  props: { tasks: Array, taskResultsIndex: Number },
  inject: ['currentLanguage','isMobile'],
  data: function () {
    return {
      currentSorting: {},
      showTasks: true,
      selected: null,
      mode: 'create',
      focused: null,
      taskSorting: { sortBy: null, sortOrder: 'desc' },
      selectedDateT: {},
      selectedFilter: '',
      pauseRefreshButton: false,
      advancedFilter: [],
      advancedFilterAux: null,
	    justSelectedFromList: false
    }
  },
  watch: {
    '$route.params.taskId': {
      immediate: true,
      handler: function (taskId) {
        this.checkTaskIdInUrl(taskId)
		    if (taskId && !this.justSelectedFromList) {
		      this.scrollToSelectedTask()	
		    }
        this.justSelectedFromList = false;
	    }
    },
    'advancedFilter': {
      deep: true,
      handler: function (newValues) {
        if (JSON.stringify(newValues) !== this.advancedFilterAux) {
          this.updateAdvancedFilters()
        }
      }
    }
  },
  computed: {
    tasksFiltered: function() {
      var tasks = []
      if (this.tasks) {
        tasks = this.tasks.filter(task => {
          var reminder = task.followUp && moment(task.followUp)
          var dueDate = task.due && moment(task.due)
          if (!this.$store.state.filter.settings.reminder && !this.$store.state.filter.settings.dueDate) return true
          return this.dateFitsFilter(reminder, this.$store.state.filter.settings.reminder) ||
            this.dateFitsFilter(dueDate, this.$store.state.filter.settings.dueDate)
        })
      }
      return tasks
    },
    filteredFields() {
      return this.$root.config.taskSorting.fields.filter(item => this.showFields(item))
    }
  },
  created: function () {
    // Clean previous way of filtering TODO: Delete in a while 3 months.
    this.taskSorting = JSON.parse(localStorage.getItem('taskSorting'))
    if (this.$root.config.taskFilter.advancedSearch.filterEnabled) {
      this.loadAdvancedFilters()
    }
  },
  methods: {
    loadAdvancedFilters: function() {
      this.advancedFilter = []
      this.$root.config.taskFilter.advancedSearch.processVariables.forEach(pv => {
        var criteria = this.$store.state.advancedSearch.criterias
          .find(obj => obj.id === pv.key && (obj.operator === 'eq' || obj.operator === 'like'))
        var advancedFilterObj = {
          key: pv.key,
          variableName: pv.variableName,
          displayName: pv.displayName,
          type: typeof pv.value === 'boolean' ? 'Boolean' : 'String',
          defaultValue: pv.value,
          operator: pv.operator
        }
        if (criteria) {
          advancedFilterObj.check = true
          advancedFilterObj.value = pv.type === 'Boolean' ? '' : criteria.value
          if (pv.operator === 'like') advancedFilterObj.value = advancedFilterObj.value.slice(1, -1)
        }
        else {
          advancedFilterObj.check = false
          advancedFilterObj.value = pv.type === 'Boolean' ? '' : pv.value
        }
        this.advancedFilter.push(advancedFilterObj)
        this.advancedFilterAux = JSON.stringify(this.advancedFilter)
      })
    },
    updateAdvancedFilters: debounce(800, function() {
      var criterias = this.advancedFilter
        .filter(filterItem => filterItem.check && (filterItem.value || filterItem.type === 'Boolean'))
        .map(filterItem => {
          var value = filterItem.type === 'Boolean' ? filterItem.defaultValue : filterItem.value
          if (filterItem.operator === 'like') value = '%' + value + '%'
          return {
            id: filterItem.key,
            key: 'processVariables',
            name: filterItem.variableName,
            operator: filterItem.operator,
            value: value
          }
        })

      this.$store.dispatch('updateAdvancedSearch', {
        matchAllCriteria: true,
        criterias: criterias
      })

      this.advancedFilterAux = JSON.stringify(this.advancedFilter)
      this.$emit('refresh-tasks')
    }),
    checkTaskIdInUrl: function(taskId) {
      if (this.tasks.length > 0 && taskId && this.$route.params.filterId !== '*') {
        var index = this.tasks.findIndex(task => {
          return task.id === taskId
        })
        if (index > -1) this.$emit('selected-task', this.tasks[index])
        else {
          this.handleTaskLink(taskId)
        }
      } else if (taskId) this.handleTaskLink(taskId)
    },
    handleTaskLink: function(taskId) {
      TaskService.findTaskById(taskId).then(task => {
        if (task.assignee && (task.assignee.toLowerCase() === this.$root.user.userID.toLowerCase()))
          return this.$emit('selected-task', task)
        else {
          TaskService.findIdentityLinks(taskId).then(identityLinks => {
            var userIdLink = identityLinks.find(i => {
              return i.type === 'candidate' && i.userId && i.userId.toLowerCase() === this.$root.user.userID.toLowerCase()
            })
            if (userIdLink) return this.$emit('selected-task', task)
            this.manageCandidateGroups(identityLinks, task)
          })
        }
      })
    },
    manageCandidateGroups: function(identityLinks, task) {
      var promises = []
      for (var i in identityLinks) {
        if (identityLinks[i].type === 'candidate' && identityLinks[i].groupId) {
          var promise = AdminService.findUsers({ memberOfGroup: identityLinks[i].groupId }).then(users => {
            return users.some(u => {
              return u.id.toLowerCase() === this.$root.user.userID.toLowerCase()
            })
          })
          promises.push(promise)
        }
      }
      Promise.all(promises).then(results => {
        if (results.some(r => { return r })) this.$emit('selected-task', task)
        else {
          this.$root.$refs.error.show({ type: 'AccessDeniedException', params: [task.id] })
          this.$router.push('/seven/auth/tasks/' + this.$store.state.filter.selected.id)
        }
      })
    },
    getDateFormatted: function(date, format, emptyMsg) {
      if (!date) return this.$t('task.' + emptyMsg)
      if (format) return moment(date).format(format)
      else return moment(date).fromNow()
    },
    getDueClasses: function(task) {
      var classes = []
      if (!task.due) classes.push('text-muted')
      else if (moment(task.due).isBefore(moment())) classes.push('text-danger')
      else if (moment().add(this.$root.config.warnOnDueExpirationIn, 'hours').isAfter(moment(task.due))) classes.push('text-warning')
      if (!this.isMobile() && task !== this.focused && task.id !== this.selectedDateT.id && !task.due) classes.push('invisible')
      return classes
    },
    getReminderClasses: function(task) {
      var classes = []
      if (!task.followUp) classes.push('text-muted')
      if (!this.isMobile() && task !== this.focused && task.id !== this.selectedDateT.id && !task.followUp) classes.push('invisible')
      return classes
    },
    claim: function(task) {
      TaskService.setAssignee(task.id, this.$root.user.id).then(() => {
        task.assignee = this.$root.user.id
        this.$emit('update-assignee', task.assignee)
      })
    },
    checkAssignee: function(task) {
      TaskService.findTaskById(task.id).then(task => {
        if (task.assignee === null) this.claim(task)
        else this.$refs.confirmTaskAssign.show(task)
      })
    },
    selectedTask: function(task) {
	    this.justSelectedFromList = true;
      var selection = window.getSelection()
      var filterId = this.$store.state.filter.selected ?
        this.$store.state.filter.selected.id : this.$route.params.filterId
      if (!selection.toString()) {
        var route = '/seven/auth/tasks/' + filterId + '/' + task.id
        if (this.$router.currentRoute.path !== route){
          this.$router.push(route)
        }
      }
    },
    getProcessName: function(processDefinitionId) {
      if (processDefinitionId === null || !this.$root.config.layout.showProcessName) return ''
      var process = this.$store.state.process.list.find(item => {
        return (item.id.split(':')[0] === processDefinitionId.split(':')[0])
      })
      return process && process.name ? process.name : ''
    },
    getCompleteName: function(task) {
      if (this.$root.user.id.toLowerCase() === task.assignee.toLowerCase()) return this.$root.user.id // .displayName
      else {
        if (this.$store.state.user.listCandidates) {
          var user = this.$store.state.user.listCandidates.find(user => {
            return user.id.toLowerCase() === task.assignee.toLowerCase()
          })
          if ((user) && (user.displayName)) return user.displayName
        }
        return task.assignee
      }
    },
    dateFitsFilter: function(date, filter) {
      return filter && date
    },
    setSorting: function(field) {
      if (field === 'order') {
        this.taskSorting.sortOrder = this.taskSorting.sortOrder === 'desc' ? 'asc' : 'desc'
      }
      localStorage.setItem('taskSorting', JSON.stringify(this.taskSorting))
      this.$emit('refresh-tasks')
    },
    isInThePast: function(ymd, date) {
      return date < moment().startOf('day')
    },
    setDate: function(task, type) {
      task[type] = this.selectedDateT[type] ? moment(this.selectedDateT[type]).format('YYYY-MM-DDTHH:mm:ss.SSSZZ') : null
      TaskService.update(task)
      this.$refs[type + task.id][0].hide()
    },
    handleScrollTasks: function(el) {
      if (this.tasks.length < this.taskResultsIndex) return
      if (Math.ceil(el.target.scrollTop + el.target.clientHeight) >= el.target.scrollHeight) {
        this.$emit('show-more')
      }
    },
    setTime: function(time, type) {
      if (!this.selectedDateT[type]) return
      if (time) {
        var timeSplit = time.split(':')
        this.selectedDateT[type].setHours(timeSplit[0])
        this.selectedDateT[type].setMinutes(timeSplit[1])
      } else {
        if (type === 'followUp') {
          this.selectedDateT.followUp.setHours(0,0,0,0)
        }
      }
    },
    copyTaskForDateManagement: function(task, type) {
      this.selectedDateT = JSON.parse(JSON.stringify(task))
      if (task[type]) {
        this.selectedDateT[type] = new Date(task[type])
        if (type === 'due') this.selectedDateT.dueTime = this.selectedDateT[type].getHours() + ':' + this.selectedDateT[type].getMinutes()
      }
    },
    pauseButton: function() {
      if (this.$root.config.pauseButtonTime) {
        this.pauseRefreshButton = true
        setTimeout(() => this.pauseRefreshButton = false, this.$root.config.pauseButtonTime)
      }
    },
    showFields: function (item) {
      if (item === 'followUpDate') {
        return this.$root.config.layout.showFilterReminderDate
      } else if (item === 'dueDate') {
        return this.$root.config.layout.showFilterDueDate
      } else {
        return true
      }
    },
    refreshTasks() {
      if (!this.pauseRefreshButton) {
        this.$emit('refresh-tasks')
        this.pauseButton()
      }
    },
	  scrollToSelectedTask(retryCount = 0) {
      const MAX_SCROLL_RETRIES = 5;
	    const taskId = this.$route.params.taskId;
	    const ref = this.$refs['taskItem-' + taskId];
	    let el = null;
	    if (Array.isArray(ref)) {
	      el = ref[0]?.$el || ref[0];
	    } else if (ref && ref.$el) {
	      el = ref.$el;
	    } else {
	      el = ref;
	    }
	    if (el && typeof el.scrollIntoView === 'function') {
	      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
	      this.pendingScrollToTaskId = null;
	    } else if (retryCount < MAX_SCROLL_RETRIES){
	      setTimeout(() => this.scrollToSelectedTask(retryCount + 1), 100);
	    } else {
	      console.warn(`scrollToSelectedTask: Element not found after ${MAX_SCROLL_RETRIES} retries.`);
      }
	  }
  }
}
</script>
