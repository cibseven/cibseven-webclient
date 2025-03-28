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
        <ProcessDetailsSidebar ref="navbar" v-if="instances"
          :processKey="processKey"
          :processDefinitions="processDefinitions"
          :versionIndex="versionIndex"
          @onRefreshProcessDefinitions="onRefreshProcessDefinitions"
          @onDeleteProcessDefinition="onDeleteProcessDefinition"
          :instances="instances"></ProcessDetailsSidebar>
      </template>
      <transition name="slide-in" mode="out-in">
        <Process ref="process" v-if="process && instances && !selectedInstance && !instanceId"
          :activity-id="activityId"
          :loading="loading"
          :process="process"
          :processKey="processKey"
          :versionIndex="versionIndex"
          :instances="instances"
          :activity-instance="activityInstance"
          :first-result="firstResult"
          :max-results="maxResults"
          :activity-instance-history="activityInstanceHistory"
          :incidents="incidents"
          @show-more="showMore()"
          @activity-id="filterByActivityId($event)"
          @instance-deleted="onInstanceDeleted()"
          @task-selected="setSelectedTask($event)"
          @filter-instances="filterInstances($event)"
          @open-subprocess="openSubprocess($event)"
          @update-items="updateItems"
        ></Process>
      </transition>
      <transition name="slide-in" mode="out-in">
        <ProcessVariablesTable ref="navbar-variables" v-if="process && selectedInstance && instanceId"
          :process="process"
          :activity-instance="activityInstance"
          :activity-instance-history="activityInstanceHistory"
          @open-subprocess="openSubprocess($event)"
          :selected-instance="selectedInstance"
          @task-selected="setSelectedTask($event)"></ProcessVariablesTable>
      </transition>
    </SidebarsFlow>
    <TaskPopper ref="importPopper"></TaskPopper>
  </div>
</template>

<script>
import { nextTick } from 'vue'
import moment from 'moment'
import { TaskService, ProcessService, HistoryService, IncidentService, JobService } from '@/services.js'
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
  name: 'ProcessDefinitionView',
  components: { Process, ProcessDetailsSidebar, ProcessVariablesTable, SidebarsFlow, TaskPopper },
  props: {
    processKey: { type: String, required: true },
    versionIndex: { type: String, required: true },
    instanceId: { type: String, required: true }
  },
  data: function() {
    return {
      leftOpen: true,
      rightOpen: false,
      process: null, // selected process definition
      instances: null,
      processDefinitions: [],
      lazyLoadHistory: this.$root.config.lazyLoadHistory,
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
      if (!this.process) return ''
      return this.process.name ? this.process.name : this.process.key
    }
  },
  created: function() {
    return this.loadProcessByDefinitionKey().then((redirected) => {
      if (!redirected && this.instanceId) {
        if (this.instances) {
          const selectedInstance = this.instances.find((instance) => instance.id == this.instanceId)
          if (selectedInstance) {
            this.setSelectedInstance({ selectedInstance: selectedInstance })
          }
        }
      }
    })
  },
  beforeUpdate: function() {
    if (this.process != null && this.process.version !== this.versionIndex) {
      // different process-definition was selected
      this.selectedInstance = null
      this.activityInstance = null
      this.activityInstanceHistory = null
      this.task = null
      this.loadProcessByDefinitionKey()
    }
    else if (this.selectedInstance == null && this.instanceId) {
      if (this.instances) {
        const selectedInstance = this.instances.find((instance) => instance.id == this.instanceId)
        if (selectedInstance) {
          this.setSelectedInstance({ selectedInstance: selectedInstance })
        }
      }
    }
    else {
      this.selectedInstance = null
      this.activityInstance = null
      this.activityInstanceHistory = null
      this.task = null
    }
  },
  methods: {
    updateItems: function(sortedItems) {
      this.instances = sortedItems
    },
    onDeleteProcessDefinition: function(params) {
      ProcessService.deleteProcessDefinition(params.processDefinition.id, true).then(() => {
        // reload versions
        ProcessService.findProcessVersionsByDefinitionKey(this.processKey).then(versions => {
          if (versions.length === 0) {
            // no more process-definitions with such key
            this.$router.replace('/seven/auth/processes')
          }
          else if (params.processDefinition.version !== this.versionIndex) {
            // remove deleted process-definition from the list
            this.processDefinitions = versions
          }
          else {
            // Find nearest process-definition to deleted one and select it.
            //
            // 5 4 3 2 1
            //     ^      - deleted
            //   ^   ^    - one of final nextVersionIndex
            //
            var nextVersionIndex = versions[0].version
            versions.forEach((version) => {
              const currentDistance = Math.abs(Number(nextVersionIndex) - Number(params.processDefinition.version))
              const thisDistance = Math.abs(Number(nextVersionIndex) - Number(version.version))
              if (currentDistance > thisDistance) {
                nextVersionIndex = version.version
              }
            })

            this.$router.replace({
              name: 'process',
              params: {
                processKey: params.processDefinition.key,
                versionIndex: nextVersionIndex,
              }
            })
          }
        })
      })
    },
    // call from:
    // - user have deleted a non-selected process definition (this.process is still valid)
    // - user clicked "refresh process definitions" button
    onRefreshProcessDefinitions: function(params) {
      return ProcessService.findProcessVersionsByDefinitionKey(this.processKey).then(versions => {
        this.processDefinitions = versions
        if (this.processDefinitions.length > 0) {
          this.calcProcessDefinitionsStats(this.process, params.lazyLoadHistory)
        }
        return versions
      })
    },
    loadProcessByDefinitionKey: function() {
      return ProcessService.findProcessVersionsByDefinitionKey(this.processKey).then(versions => {
        const requestedDefinition = versions.find(processDefinition => processDefinition.version === this.versionIndex)
        if (requestedDefinition) {
          this.processDefinitions = versions
          const needCalcStats = this.process == null
          return this.loadProcessVersion(requestedDefinition).then(() => {
            if (needCalcStats) {
              this.calcProcessDefinitionsStats(requestedDefinition, this.lazyLoadHistory)
            }
            // false - no redirect
            return false
          })
        }
        else {
          // definition is no longer available
          // let's redirect to the latest one
          this.$router.push('/seven/auth/process/' + this.processKey)
          // true - redirect
          return true
        }
      })
    },
    calcProcessDefinitionsStats: function(selectedProcess, lazyLoadHistory) {
      if (lazyLoadHistory) {
        this.processDefinitions.forEach(v => {
          v.runningInstances = '-'
          v.allInstances = '-'
          v.completedInstances = '-'
        })

        if (selectedProcess) {
          ProcessService.findProcessById(selectedProcess.id, true).then(process => {
            for (let v of this.processDefinitions) {
              if (v.id === process.id) {
                Object.assign(v, process)
                break
              }
            }
          })
        }
      }

      // false - no redirect
      return false
    },
    loadProcessActivitiesHistory: function() {
      HistoryService.findActivitiesProcessDefinitionHistory(this.process.id).then(activities => {
        this.process.activitiesHistory = activities
      })
    },
    loadProcessVersion: function(process) {
      return new Promise((resolve) => {
        if (!this.process || this.process.id !== process.id) {
          this.firstResult = 0
          this.process = process
          if (!this.process.statistics) this.loadStatistics()
          if (!this.process.activitiesHistory) this.loadProcessActivitiesHistory()
          return Promise.all([
            this.loadInstances(),
            this.loadIncidents()
          ]).then(() => {
            resolve();
          })
        }
        resolve();
      });
    },
    loadInstances: function(showMore) {
      if (this.$root.config.camundaHistoryLevel !== 'none') {
        this.loading = true
        return HistoryService.findProcessesInstancesHistoryById(this.process.id, this.activityId,
          this.firstResult, this.maxResults, this.filter
        ).then(instances => {
          this.loading = false
          if (!showMore) this.instances = instances
          else this.instances = !this.instances ? instances : this.instances.concat(instances)
        })
      }
      else {
        return ProcessService.findProcessVersionsByDefinitionKey(this.process.key).then(versions => {
          this.processDefinitions = versions

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
                instance.processDefinitionId = versions[i].id
                instance.processDefinitionVersion = versions[i].version
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
    onInstanceDeleted: function() {
      this.setSelectedInstance({ selectedInstance: null })
      this.firstResult = 0
      return Promise.all([
        this.loadInstances(),
        this.loadIncidents(),
        this.loadStatistics(),
        this.loadProcessActivitiesHistory()
      ]).then(() => {
        this.calcProcessDefinitionsStats(this.process, this.lazyLoadHistory)
        this.$refs.process.refreshDiagram()
      })
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
        // do not load the same data once again
        if (this.selectedInstance && this.selectedInstance.id === selectedInstance.id) return
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
        this.setJobs()
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
    },
    async setJobs() {
      this.selectedInstance.jobs = await JobService.getJobs({ processInstanceId: this.selectedInstance.id })
    }
  }
}
</script>
