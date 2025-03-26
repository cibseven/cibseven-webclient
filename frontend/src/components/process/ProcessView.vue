<template>
  <div class="d-flex flex-column">
    <div class="d-flex ps-3 py-2">
      <b-button :title="$t('start.admin')" variant="outline-secondary" href="#/seven/auth/processes/list" class="mdi mdi-18px mdi-arrow-left border-0"></b-button>
      <h4 class="ps-1 m-0 align-items-center d-flex" style="border-width: 3px !important">{{ processName }}</h4>
      <b-button :disabled="!instances || instances.length === 0" :title="$t('process.exportInstances')" variant="outline-secondary" @click="exportCSV()"
        class="ms-auto me-3 mdi mdi-18px mdi-download-outline border-0"></b-button>
    </div>
    <SidebarsFlow ref="sidebars" class="border-top overflow-auto" v-model:left-open="leftOpen" :left-caption="shortendLeftCaption">
      <template v-slot:left>
        <ProcessDetailsSidebar ref="navbar" v-if="instances" :process="process" :selected-instance="selectedInstance" :selected-task="task"
          @load-version-process="loadVersionProcess($event)" @load-instances="loadInstances()"
          :instances="instances"></ProcessDetailsSidebar>
      </template>
      <transition name="slide-in" mode="out-in">
        <Process ref="process" v-if="instances && !selectedInstance" :activity-id="activityId" :loading="loading" :process="process" :instances="instances" :activity-instance="activityInstance" :first-result="firstResult" :max-results="maxResults" @show-more="showMore()"
        :activity-instance-history="activityInstanceHistory" @activity-id="filterByActivityId($event)" @instance-deleted="clearInstance()" @instance-selected="setSelectedInstance($event)" @task-selected="setSelectedTask($event)" @filter-instances="filterInstances($event)"
        @open-subprocess="openSubprocess($event)" @update-items="updateItems" :incidents="incidents"
        ></Process>
      </transition>
      <transition name="slide-in" mode="out-in">
        <ProcessVariablesTable ref="navbar-variables" v-if="selectedInstance" :process="process" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory" @open-subprocess="openSubprocess($event)"
        :selected-instance="selectedInstance" @clear-state="setSelectedInstance({ selectedInstance: null })" @task-selected="setSelectedTask($event)" @unselect-instance="selectedInstance = null"></ProcessVariablesTable>
      </transition>
    </SidebarsFlow>
    <TaskPopper ref="importPopper"></TaskPopper>
  </div>
</template>

<script>
import { nextTick } from 'vue'
import moment from 'moment'
import { TaskService, ProcessService, HistoryService, IncidentService } from '@/services.js'
import Process from '@/components/process/Process.vue'
import ProcessDetailsSidebar from '@/components/process/ProcessDetailsSidebar.vue'
import ProcessVariablesTable from '@/components/process/ProcessVariablesTable.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'
import TaskPopper from '@/components/common-components/TaskPopper.vue'

function getStringObjByKeys(keys, obj) {
  var result = ''
  keys.forEach(key => {
    result += obj[key] + ';'
  })
  return result.slice(0, -1)
}

export default {
  name: 'ProcessView',
  components: { Process, ProcessDetailsSidebar, ProcessVariablesTable, SidebarsFlow, TaskPopper },
  props: {
    processKey: String,
    versionIndex: { type: String, default: '' }
   },
  data: function() {
    return {
      leftOpen: true,
      rightOpen: false,
      process: null,
      instances: null,
      processDefinitions: null,
      selectedInstance: null,
      task: null,
      activityInstance: null,
      activityInstanceHistory: null,
      firstResult: 0,
      maxResults: this.$root.config.maxProcessesResults,
      filter: '',
      activityId: '',
      loading: false,
      incidents: []
    }
  },
  computed: {
    shortendLeftCaption: function() {
      return this.$t('process.details.historyVersions')
    },
    shortendRightCaption: function() {
      return this.$t('process.details.variable')
    },
    processName: function() {
      if (!this.process) return this.$t('process.process')
      return this.process.name ? this.process.name : this.process.key
    }
  },
  created: function() {
    if (this.$route.query.processId) {
      this.loadProcessById(this.$route.query.processId)
    }
    else {
      this.loadProcessByDefinitionKey(this.processKey, this.versionIndex)
    }
  },
  methods: {
    updateItems: function(sortedItems) {
      this.instances = sortedItems
    },
    loadProcessById: function(id) {
      this.firstResult = 0
      this.$store.dispatch('getProcessById', { id: id }).then(process => {
        this.loadProcessVersion(process)
      })
    },
    loadProcessByDefinitionKey: function(processKey, versionIndex) {
      if (!versionIndex) {
        this.$store.dispatch('getProcessByDefinitionKey', { key: processKey }).then(process => {
          this.loadProcessVersion(process)
        })
      }
      else {
        ProcessService.findProcessVersionsByDefinitionKey(processKey).then(processDefinitions => {
          this.processDefinitions = processDefinitions
          let requestedDefinition = processDefinitions.find(processDefinition => processDefinition.version === this.versionIndex)
          if (requestedDefinition) {
              this.loadProcessVersion(requestedDefinition)
          } else {
            // definition is no longer available
            // let's redirect to the latest one
            this.loadProcessByDefinitionKey(processKey, undefined)
            this.$router.push('/seven/auth/process/' + processKey)
          }
        })
      }
    },
    loadProcessActivitiesHistory: function() {
      HistoryService.findActivitiesProcessDefinitionHistory(this.process.id).then(activities => {
        this.process.activitiesHistory = activities
      })
    },
    loadProcessVersion: function(process) {
      if (!this.process || this.process.id !== process.id) {
        this.firstResult = 0
        this.process = process
        this.loadInstances()
        this.loadIncidents()
        if (!this.process.statistics) this.loadStatistics()
        if (!this.process.activitiesHistory) this.loadProcessActivitiesHistory()
      }
    },
    loadInstances: function(showMore) {
      if (this.$root.config.camundaHistoryLevel !== 'none') {
        this.loading = true
        HistoryService.findProcessesInstancesHistoryById(this.process.id, this.activityId,
          this.firstResult, this.maxResults, this.filter
        ).then(instances => {
          this.loading = false
          if (!showMore) this.instances = instances
          else this.instances = !this.instances ? instances : this.instances.concat(instances)
        })
      }
      else {
        ProcessService.findProcessVersionsByDefinitionKey(this.process.key).then(processDefinitions => {
          this.processDefinitions = processDefinitions
          var promises = []
          this.processDefinitions.forEach(() => {
            promises.push(HistoryService.findProcessesInstancesHistoryById(this.process.id, this.activityId, this.firstResult,
              this.maxResults, this.filter))
          })
          Promise.all(promises).then(response => {
            if (!showMore) this.instances = []
            var i = 0
            response.forEach(instances => {
              instances.forEach(instance => {
                instance.processDefinitionId = processDefinitions[i].id
                instance.processDefinitionVersion = processDefinitions[i].version
              })
              this.instances = this.instances.concat(instances)
              i++
            })
          })
        })
      }
    },
    loadStatistics: function() {
      ProcessService.findProcessStatistics(this.process.id).then(statistics => {
        this.$store.dispatch('setStatistics', { process: this.process, statistics: statistics })
      })
    },
    clearInstance: function() {
      this.setSelectedInstance({ selectedInstance: null })
      this.$refs.process.clearState()
      this.firstResult = 0
      this.loadInstances()
    },
    setSelectedInstance: function(evt) {
      var selectedInstance = evt.selectedInstance
      if (!selectedInstance) {
        this.rightOpen = false
        this.selectedInstance = null
      } else this.rightOpen = true
      this.task = null
      this.activityInstance = null
      this.activityInstanceHistory = selectedInstance ? this.activityInstanceHistory : null
      if (selectedInstance) {
        if (this.selectedInstance && this.selectedInstance.id === selectedInstance.id && !evt.reload) return
        this.selectedInstance = selectedInstance
        if (this.selectedInstance.state === 'ACTIVE') {
          //Management
          ProcessService.findActivityInstance(selectedInstance.id).then(activityInstance => {
            this.activityInstance = activityInstance
            HistoryService.findActivitiesInstancesHistory(selectedInstance.id).then(activityInstanceHistory => {
              this.activityInstanceHistory = activityInstanceHistory
            })
          })
        } else {
          //History
          if (this.$root.config.camundaHistoryLevel !== 'none') {
            HistoryService.findActivitiesInstancesHistory(selectedInstance.id).then(activityInstanceHistory => {
              this.activityInstanceHistory = activityInstanceHistory
            })
          }
        }
      }
    },
    setSelectedTask: function(selectedTask) {
      if (this.selectedInstance && selectedTask) {
        HistoryService.findTasksByDefinitionKeyHistory(selectedTask.id, this.selectedInstance.id).then(function(task) {
          if (task.length === 0) {
            this.task = null
            return
          }
          this.task = task[0]
          var serviceCall = !this.task.endTime ? TaskService.fetchActivityVariables :
            HistoryService.fetchActivityVariablesHistory
          serviceCall(this.task.activityInstanceId).then(variables => {
            variables.forEach(variable => {
              variable.value = variable.type === 'Object' ? JSON.stringify(variable.value) : variable.value
            })
            this.task.variables = variables
          })
        })
      }
    },
    filterByActivityId: function(event) {
      this.activityId = event
      this.instances = []
      this.firstResult = 0
      this.loadInstances()
    },
    showMore: function() {
      this.firstResult += this.$root.config.maxProcessesResults
      this.loadInstances(true)
    },
    filterInstances: function(filter) {
      this.filter = filter
      this.firstResult = 0
      this.loadInstances()
    },
    getIconState: function(state) {
      switch(state) {
        case 'ACTIVE':
          return 'mdi-chevron-triple-right text-success'
        case 'SUSPENDED':
          return 'mdi-close-circle-outline'
      }
      return 'mdi-flag-triangle'
    },
    loadVersionProcess: function(event) {
      this.selectedInstance = null
      nextTick(function() {
        this.activityId = ''
        this.$refs.process.clearState()
        this.loadProcessVersion(event)
      }.bind(this))
    },
    openSubprocess: function(event) {
      this.activityId = ''
      this.setSelectedInstance({ selectedInstance: null })
      this.loadProcessVersion(event)
      nextTick(function() {
        this.$refs.navbar.getVersions()
      }.bind(this))
      this.$router.push('/seven/auth/process/' + event.key)
    },
    exportCSV: function() {
      var headers = [
        { text: 'state', key: 'state' },
        { text: 'businessKey', key: 'businessKey' },
        { text: 'startTime', key: 'startTime' },
        { text: 'endTime', key: 'endTime' },
        { text: 'id', key: 'id' },
        { text: 'startUserId', key: 'startUserId' },
        { text: 'details.definitionName', key: 'processDefinitionName' },
        { text: 'details.definitionVersion', key: 'processDefinitionVersion' }
      ]
      headers.forEach(h => h.text = this.$t('process.' + h.text))
      var csvContent = headers.map(h => h.text).join(';') + '\n'
      var keys = headers.map(h => h.key)
      this.instances.forEach(v => {
        csvContent += getStringObjByKeys(keys, v) + '\n'
      })
      var csvBlob = new Blob([csvContent], { type: 'text/csv' })
      var filename = 'Management_Instances_' + moment().format('YYYYMMDD_HHmm') + '.csv'
      this.$refs.importPopper.triggerDownload(csvBlob, filename)
    },
    async loadIncidents() {
      this.incidents = await IncidentService.findIncidents(this.process.id)
    }
  }
}
</script>
