<template>
  <SidebarsFlow ref="regionFilter" role="region" :aria-label="$t('seven.filters')" @selected-filter="selectedFilter()" v-model:left-open="leftOpenFilter" :left-caption="leftCaptionFilter" :rightSize="[12, 4, 2, 2, 2]" :leftSize="[12, 4, 2, 2, 2]">
    <GlobalEvents @keydown.alt.1.prevent="navigateRegion('regionFilter')"></GlobalEvents>
    <GlobalEvents @keydown.alt.2.prevent="navigateRegion('regionTasks')"></GlobalEvents>
    <GlobalEvents @keydown.alt.3.prevent="navigateRegion('regionTask')"></GlobalEvents>
    <template v-slot:left>
      <FilterNavBar ref="filterNavbar" @filter-alert="showFilterAlert($event)"
        @selected-filter="selectedFilter()" @set-filter="filter = $event;listTasksWithFilter()" @selected-task="selectedTask($event)"
        @display-popover="displayPopover($event)" @refresh-tasks="listTasksWithFilter()" @n-filters-shown="nFiltersShown = $event" class="border-0 bg-white"></FilterNavBar>
    </template>
    <template v-slot:filter>
      <FilterNavCollapsed v-if="!leftOpenFilter && leftCaptionFilter" v-model:left-open="leftOpenFilter"></FilterNavCollapsed>
    </template>
    <SidebarsFlow ref="regionTasks" role="region" :aria-label="$t('seven.allTasks')" class="h-100 bg-light" :number="nTasksShown" header-margin="55px" v-model:left-open="leftOpenTask"
      :leftSize="getTasksNavbarSize" :left-caption="leftCaptionTask">
      <template v-slot:left>
        <TasksNavBar @filter-alert="showFilterAlert($event)" ref="navbar" :tasks="tasks" @selected-task="selectedTask($event)"
          @update-assignee="updateAssignee($event, 'task')" @set-filter="filter = $event;listTasksWithFilter()"
          @open-sidebar-date="rightOpenTask = true" @show-more="showMore()" :taskResultsIndex="taskResultsIndex"
          @process-started="listTasksWithFilter();$refs.processStarted.show(10); checkAndOpenTask($event, true)" @display-popover="displayPopover($event)"
          @search-filter="search = $event" @refresh-tasks="listTasksWithFilter()"></TasksNavBar>
      </template>

      <transition name="slide-in" mode="out-in">
        <router-view v-if="task !== null" role="region" :aria-label="$t('task.selectedTask')" ref="down" class="h-100" style="overflow-y: auto" v-slot="{ Component }">
          <component :is="Component" ref="taskComponent" @display-popover="displayPopover($event)" @update-task="updateTask($event)"
            @update-assignee="updateAssignee($event, 'taskList')" :task="task" @complete-task="completedTask($event)" />
        </router-view>
        <BWaitingBox v-else-if="task === null && $route.query.externalMode !== undefined" class="h-100 d-flex justify-content-center" styling="width:20%"></BWaitingBox>
        <div v-else class="text-secondary text-center">
          <img :alt="$t('seven.selectTask')" src="/assets/images/task/tasklist_empty.svg" class="mt-5" style="max-width: 250px">
          <h5>{{ $t('seven.selectTask') }}</h5>
        </div>
      </transition>
      <template v-slot:leftIcon>
        <div>
          <b-button style="top: 3px; right: 0" :title="tasksNavbarSize !== 2 ? $t('task.expand') : $t('task.collapse')"
          class="border-0 position-absolute" size="sm" variant="link" @click.stop="manageNavbarSize()">
            <span v-if="tasksNavbarSize !== 2" class="mdi mdi-18px mdi-chevron-right"></span>
            <span v-else class="mdi mdi-18px mdi-chevron-left"></span>
          </b-button>
          <b-button style="top: 3px; right: 60px" class="border-0 position-absolute" variant="link" size="sm" :title="$t('task.collapse')" @click.stop="collapseNavbar()">
            <span class="mdi mdi-18px mdi-chevron-left"></span>
          </b-button>
        </div>
      </template>
    </SidebarsFlow>
    <SuccessAlert top="0" style="z-index: 1031" ref="completedTask">{{ $t('seven.taskCompleted') }}</SuccessAlert>
    <SuccessAlert top="0" style="z-index: 1031" ref="processStarted">{{ $t('process.processStarted') }}</SuccessAlert>
    <SuccessAlert top="0" style="z-index: 1031" ref="filter">
      <i18n-t :keypath="'nav-bar.filters.' + filterMessage" tag="span" scope="global">
        <template #name>
          <strong>{{ filterName }}</strong>
        </template>
      </i18n-t>
    </SuccessAlert>
  </SidebarsFlow>
</template>

<script>
import moment from 'moment'
import { permissionsMixin } from '@/permissions.js'
import { TaskService, ProcessService, HistoryService, AuthService } from '@/services.js'
import { debounce } from '@/utils/debounce.js'
import TasksNavBar from '@/components/task/TasksNavBar.vue'
import FilterNavBar from '@/components/task/filter/FilterNavBar.vue'
import FilterNavCollapsed from '@/components/task/filter/FilterNavCollapsed.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import { BWaitingBox } from 'cib-common-components'
import { updateAppTitle } from '@/utils/init'

export default {
  name: 'Tasks',
  components: { TasksNavBar, FilterNavBar, FilterNavCollapsed, SidebarsFlow, SuccessAlert, BWaitingBox },
  inject: ['isMobile'],
  mixins: [permissionsMixin],
  data: function () {
    var leftOpenFilter = localStorage.getItem('leftOpenFilter') ?
      localStorage.getItem('leftOpenFilter') === 'true' : true
    var externalMode = window.location.href.includes('externalMode') ? true : false
    if (externalMode) leftOpenFilter = false
    return {
      leftOpenFilter: leftOpenFilter,
      leftOpenTask: !externalMode,
      rightOpenTask:  localStorage.getItem('rightOpenTask') === 'true' && this.canOpenRightTask(),
      tasks: [],
      task: null,
      processInstanceHistory: null,
      processesInstances: null, //Only needed to fetch the businessKey of every instance.
      filter: null,
      interval: null,
      filterMessage: '',
      filterName: '',
      nTasksShown: 0,
      nFiltersShown: 0,
      tasksNavbarSizes: [[12, 6, 4, 4, 3], [12, 6, 4, 5, 4], [12, 6, 4, 6, 5]],
      tasksNavbarSize: 0,
      taskResultsIndex: 0,
      search: ''
    }
  },
  computed: {
    rightCaptionTask: function() {
      if (this.canOpenRightTask())
        return this.$t('task.options')
      return null
    },
    leftCaptionTask: function() {
      return this.$store.state.filter.selected.name
    },
    leftCaptionFilter: function() {
      return this.leftOpenTask ? this.$t('seven.filters') : ''
    },
    getTasksNavbarSize: function() { return this.tasksNavbarSizes[this.tasksNavbarSize] }
  },
  watch: {
    rightOpenTask: function(newVal) {
      localStorage.setItem('rightOpenTask', newVal)
    },
    '$route.params.taskId': function() { if (!this.$route.params.taskId) this.cleanSelectedTask() },
    '$route.params.filterId': function() { if (!this.$route.params.filterId) this.cleanSelectedFilter() },
    leftOpenTask: function(leftOpen) {
      if (leftOpen) {
        this.leftOpenFilter = !localStorage.getItem('leftOpenFilter') || localStorage.getItem('leftOpenFilter') === 'true'
      } else this.leftOpenFilter = false
    },
    leftOpenFilter: function() {
      if (this.leftOpenTask) localStorage.setItem('leftOpenFilter', this.leftOpenFilter)
    },
    search: debounce(800, function() { this.listTasksWithFilter() })
  },
  created: function() {
    var taskSorting = localStorage.getItem('taskSorting') ? JSON.parse(localStorage.getItem('taskSorting')) : {}
    if (!localStorage.getItem('taskSorting') ||
      (localStorage.getItem('taskSorting') && (taskSorting.sorting || taskSorting.order))) {
      localStorage.setItem('taskSorting', JSON.stringify(this.$root.config.taskSorting.default))
    }
    this.taskResultsIndex += this.$root.config.maxTaskResults
    this.setIntervalTaskList()
  },
  methods: {
    canOpenRightTask: function() {
      return (this.$root.config.layout.showTaskDetailsSidebar ||
          this.$root.config.layout.showChat ||
          this.$root.config.layout.showStatusBar)
    },
    setIntervalTaskList: function() {
      if (this.$root.config.taskListTime !== '0') {
        this.interval = setInterval(() => {
          this.listTasksWithFilterAuto()
          if (this.task) this.checkActiveTask()
        }, this.$root.config.taskListTime)
      }
    },
    listTasksWithFilter: function() {
      this.tasks = []
      this.processesInstances = []
      this.nTasksShown = 0
      if (this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = false
      this.fetchTasks(0, this.taskResultsIndex)
    },
    listTasksWithFilterAuto: function(showMore) {
      if (this.$route.params.filterId) {
        if (showMore) this.$refs.navbar.$refs.taskLoader.done = false
        var firstResult = showMore ? this.taskResultsIndex : 0
        var maxResults = showMore ? this.$root.config.maxTaskResults : this.taskResultsIndex
        if (this.$store.state.filter.selected.id) {
          this.fetchTasks(firstResult, maxResults, showMore)
        }
      } else {
        if (this.$refs.navbar && this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = true
      }
    },
    fetchTasks: function(firstResult, maxResults, showMore) {
      var taskSorting = [JSON.parse(localStorage.getItem('taskSorting'))]
      //If necessary we add the created extra sorting so the data is well sorted
      if (taskSorting[0].sortBy !== 'created') taskSorting.push({ sortBy: 'created', sortOrder: 'desc' })
      var filters = { sorting: taskSorting }
      if (this.search) {
        filters.orQueries = [
          {
            nameLike: '%' + this.search + '%',
            assigneeLike: '%' + this.search + '%',
            processDefinitionId: this.search,
            processInstanceBusinessKeyLike: '%' + this.search + '%'
          }
        ]
      }
      if (this.$root.config.taskFilter.advancedSearch.filterEnabled) {
        this.$store.dispatch('loadAdvancedSearchData')
      }
      AuthService.fetchAuths().then(permissions => {
        this.$root.user.permissions = permissions
        var advCriterias = this.$store.state.advancedSearch.criterias
        if (advCriterias.length > 0) {
          if (this.$store.state.advancedSearch.matchAllCriteria === true) {
            for (var key in this.$store.getters.formatedCriteriaData) {
              if (this.$store.getters.formatedCriteriaData.hasOwnProperty(key)) {
                filters[key] = this.$store.getters.formatedCriteriaData[key]
              }
            }
          }
          else {
            if (!filters.orQueries) filters.orQueries = []
            filters.orQueries.push(this.$store.getters.formatedCriteriaData)
          }
        }
        TaskService.findTasksByFilter(this.$store.state.filter.selected.id, filters,
          { firstResult: firstResult, maxResults: maxResults }).then(result => {
          var tasks = this.tasksByPermissions(this.$root.config.permissions.displayTasks, result)
          TaskService.findTasksCountByFilter(this.$store.state.filter.selected.id, filters).then(count => {
            this.nTasksShown = count
          })
          //Only needed to fetch the businessKey of every instance.
          this.updateProcessesInstances(tasks, showMore)
        }, () => {
          if (this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = true
        })
      })
    },
    updateTask: function(updatedTask) {
      this.processInstanceHistory.tasksHistory[0].due = updatedTask.due
      var index = this.tasks.findIndex(task => {
        return task.id === updatedTask.id
      })
      //Because of the longpoll this.task is not necessarily the same object like in this.tasks
      this.tasks.splice(index, 1, updatedTask)
      this.listTasksWithFilterAuto()
    },
    updateAssignee: function(assignee, target) {
      if (this.processInstanceHistory) this.processInstanceHistory.tasksHistory[0].assignee = assignee
      if (target === 'taskList') {
        var currentTaskIndex = this.tasks.findIndex(task => {
          return task.id === this.task.id
        })
        if (currentTaskIndex !== -1) this.tasks[currentTaskIndex].assignee = assignee
      }
      this.listTasksWithFilterAuto()
    },
    completedTask: function(task) {
      this.tasks = this.tasks.filter(t => { return t.id !== task.id })
      this.$refs.completedTask.show(2)
      this.processInstanceHistory = null
      this.listTasksWithFilterAuto()
      this.checkAndOpenTask(JSON.parse(JSON.stringify(this.task)))
      this.task = null
    },
    checkAndOpenTask: function(task, started) {
      if (this.$root.config.automaticallyOpenTask) this.openTaskAutomatically(task, started)
    },
    openTaskAutomatically: function(task, started) {
      var counter = 0
      const intervalTime = 2500
      const maxExecutions = 3
      var method = started ? HistoryService.findProcessInstance(task.processInstanceId) :
        HistoryService.findTasksByTaskIdHistory(task.id)
      method.then(response => {
        var data = Array.isArray(response) ? response[0] : response
        if (data) {
          var intervalId = setInterval(() => {
            counter++
            if (counter > maxExecutions) clearInterval(intervalId)
            else this.openTask(data, intervalId)
          }, intervalTime)
          counter++
          this.openTask(data, intervalId)
        }
      })
    },
    openTask: function(resOrin, intervalId) {
      var createdAfter = resOrin.endTimeOriginal || resOrin.startTimeOriginal
      if (createdAfter) createdAfter = moment(createdAfter).subtract(5, 'seconds').format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
      TaskService.findTasksByProcessInstanceAsignee(null, createdAfter).then(tasks => {
        if (tasks.length > 0) {
          var taskRedirect = tasks.find(t => t.processInstanceId === resOrin.rootProcessInstanceId)
          if (taskRedirect) {
            clearInterval(intervalId)
            this.$router.push('/seven/auth/tasks/' + this.$store.state.filter.selected.id + '/' + taskRedirect.id)
          } else {
            this.openTaskByTaskInstance(tasks, tasks.shift(), resOrin, intervalId)
          }
        }
      })
    },
    openTaskByTaskInstance: function(tasks, task, resOrin, intervalId) {
      if (task) {
        HistoryService.findTasksByTaskIdHistory(task.id).then(taskH => {
          if (taskH[0]) {
            if (taskH[0].rootProcessInstanceId === resOrin.rootProcessInstanceId) {
              clearInterval(intervalId)
              this.$router.push('/seven/auth/tasks/' + this.$store.state.filter.selected.id + '/' + taskH[0].id)
            } else {
              this.openTaskByTaskInstance(tasks, tasks.shift(), resOrin, intervalId)
            }
          } else {
            this.openTaskByTaskInstance(tasks, tasks.shift(), resOrin, intervalId)
          }
        })
      }
    },
    selectedTask: function(task) {
      this.task = task
      updateAppTitle(
        this.$root.config.productNamePageTitle,
        this.$t('start.taskList'),
        task.name
      )
      if (this.isMobile()) {
        this.leftOpenTask = false
      }
      // Only needed when the task side detail is load.
      if (this.$root.config.layout.showTaskDetailsSidebar) {
        ProcessService.findProcessInstance(this.task.processInstanceId).then(instance => {
          HistoryService.findTasksByProcessInstanceHistory(this.task.processInstanceId).then(tasksHistory => {
            this.processInstanceHistory = instance
            this.processInstanceHistory.tasksHistory = tasksHistory
          })
        })
      }
    },
    selectedFilter: function() {
      this.task = null
      this.listTasksWithFilter()
    },
    displayPopover: function(evt) {
      if (this.$refs.taskComponent)
        this.$refs.taskComponent.$refs.task.displayPopover = localStorage.getItem('showPopoverHowToAssign') === 'false' ? false : evt
    },
    showFilterAlert: function(evt) {
      this.filterMessage = evt.message
      this.filterName = evt.filter
      this.$refs.filter.show(2)
    },
    updateProcessesInstances: function(tasks, showMore) {
      if (tasks && tasks.length > 0) {
        var processesInstancesIds = []
        tasks.forEach(task => {
          processesInstancesIds.push(task.processInstanceId)
        })
        // TODO -> processInstanceIds needs to be removed from here, this link the webclient with Camunda and this needs to be
        // transparent.
        ProcessService.findCurrentProcessesInstances({ processInstanceIds : processesInstancesIds }).then(instances => {
          this.processesInstances = instances
          tasks.forEach(task => {
            var instance = this.processesInstances.find(p => {
              return p.id === task.processInstanceId
            })
            if (instance) task.businessKey = instance.businessKey
          })
          this.tasks = showMore ? [...this.tasks, ...tasks] : tasks
          this.checkActiveTask()
          if (this.$refs.navbar && this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = true
        }, () => {
          this.tasks = showMore ? [...this.tasks, ...tasks] : tasks
          this.checkActiveTask()
          if (this.$refs.navbar && this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = true
        })
      } else {
        this.tasks = showMore ? [...this.tasks, ...tasks] : tasks
        this.checkActiveTask()
        if (this.$refs.navbar && this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = true
      }
    },
    cleanSelectedTask: function() {
      this.processInstanceHistory = null
      this.task = null
      if (this.$route.params.filterId) {
        this.listTasksWithFilterAuto()
        var path = '/seven/auth/tasks/' + this.$route.params.filterId
        if (path !== this.$route.path) this.$router.push(path)
      }
    },
    cleanSelectedFilter: function() {
      this.$store.state.filter.selected = {
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
      }
      this.tasks = []
      this.cleanSelectedTask()
    },
    manageNavbarSize: function() {
      if (this.tasksNavbarSize === 2) {
        this.leftOpenTask = false
        this.tasksNavbarSize = 0
      }
      else this.tasksNavbarSize++
    },
    collapseNavbar: function () {
      this.leftOpenTask = false
      this.tasksNavbarSize = 0
    },
    showMore: function() {
      this.listTasksWithFilterAuto(true)
      this.taskResultsIndex += this.$root.config.maxTaskResults
    },
    navigateRegion: function(region) {
      if (['regionFilter', 'regionTasks'].includes(region)) {
        this.$refs[region].$refs.leftSidebar.focus()
      } else {
        if (this.$refs.taskComponent) this.$refs.taskComponent.$refs.task.$refs.titleTask.focus()
      }
    },
    checkActiveTask: function() {
      if (this.task) {
        var task = this.tasks.find(t => { return t.id === this.task.id })
        if (!task) {
          clearInterval(this.interval)
          TaskService.checkActiveTask(this.task.id, this.$root.user.authToken).catch(() => {
            if (this.$route.params.taskId === this.task.id) {
              if (this.$router.currentRoute.path !== '/seven/auth/tasks/' + this.$route.params.filterId){
                this.$router.push('/seven/auth/tasks/' + this.$route.params.filterId)
              }
              this.setIntervalTaskList()
            }
          })
        }
      }
    }
  },
  beforeRouteLeave: function(to, from, next) {
    clearInterval(this.interval)
    next()
  }
}
</script>
