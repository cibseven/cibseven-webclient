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
  <div class="d-flex flex-column">
    <div class="d-flex ps-3 py-2">
      <b-button :title="$t('start.cockpit.processes.title')" variant="outline-secondary" href="#/seven/auth/processes/list" class="mdi mdi-18px mdi-arrow-left border-0"></b-button>
      <h4 class="ps-1 m-0 align-items-center d-flex" style="border-width: 3px !important">{{ processName }}</h4>
      <b-button :disabled="!instances || instances.length === 0" :title="$t('process.exportInstances')" variant="outline-secondary" @click="exportCSV()"
        class="ms-auto me-3 mdi mdi-18px mdi-download-outline border-0"></b-button>
    </div>
    <SidebarsFlow ref="sidebars" class="border-top overflow-auto" :left-open="leftOpen" @update:left-open="leftOpen = $event" :left-caption="shortendLeftCaption">
      <template v-slot:left>
        <ProcessDetailsSidebar ref="navbar" v-if="process"
          :process-key="processKey"
          :process-definitions="processDefinitions"
          :version-index="versionIndex"
          @on-refresh-process-definitions="onRefreshProcessDefinitions"
          @on-delete-process-definition="onDeleteProcessDefinition"
          :instances="instances"></ProcessDetailsSidebar>
      </template>
      <transition name="slide-in" mode="out-in">
        <ProcessInstancesView ref="process" v-if="process && !selectedInstance && !instanceId"
          :loading="loading"
          :process="process"
          :process-key="processKey"
          :version-index="versionIndex"
          :activity-instance="activityInstance"
          :activity-instance-history="activityInstanceHistory"
          :tenant-id="tenantId"
          :filter="filter"
          @instance-deleted="onInstanceDeleted()"
          @task-selected="setSelectedTask($event)"
          @filter-instances="filterInstances($event)"
        ></ProcessInstancesView>
      </transition>
      <transition name="slide-in" mode="out-in">
        <ProcessInstanceView ref="navbar-variables" v-if="process && selectedInstance && instanceId"
          :process="process"
          :activity-instance="activityInstance"
          :activity-instance-history="activityInstanceHistory"
          :selected-instance="selectedInstance"
          @task-selected="setSelectedTask($event)"></ProcessInstanceView>
      </transition>
    </SidebarsFlow>
    <TaskPopper ref="importPopper"></TaskPopper>
  </div>
</template>

<script>
import { moment } from '@/globals.js'
import { TaskService, ProcessService, HistoryService } from '@/services.js'
import ProcessInstancesView from '@/components/process/ProcessInstancesView.vue'
import ProcessDetailsSidebar from '@/components/process/ProcessDetailsSidebar.vue'
import ProcessInstanceView from '@/components/process/ProcessInstanceView.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'
import TaskPopper from '@/components/common-components/TaskPopper.vue'
import { mapGetters, mapActions } from 'vuex'
import { formatDate } from '@/utils/dates.js'

function getStringObjByKeys(keys, obj) {
  var result = ''
  keys.forEach(key => {
    result += obj[key] + ';'
  })
  return result.slice(0, -1)
}

export default {
  name: 'ProcessDefinitionView',
  components: { ProcessInstancesView, ProcessDetailsSidebar, ProcessInstanceView, SidebarsFlow, TaskPopper },
  props: {
    processKey: { type: String, required: true },
    versionIndex: { type: String, required: true },
    instanceId: { type: String, required: true },
    tenantId: { type: String }
  },
  watch: {
    processKey: 'loadProcessFromRoute',
    versionIndex() {
     if (this.process.key === this.processKey){
      const process = this.processDefinitions.find(processDefinition => processDefinition.version === this.versionIndex)
      if (process) this.loadProcessVersion(process)
     }
    }
  },
  data: function() {
    return {
      leftOpen: true,
      process: null, // selected process definition
      processDefinitions: [],
      selectedInstance: null,
      task: null,
      activityInstance: null,
      activityInstanceHistory: null,
      filter: '',
      loading: false
    }
  },
  computed: {
    ...mapGetters('instances', ['instances']),
    shortendLeftCaption: function() {
      return this.$t('process.details.historyVersions')
    },
    processName: function() {
      if (!this.process) return ''
      return this.process.name ? this.process.name : this.process.key
    },
  },
  created: function() {
    this.clearActivitySelection()
    this.loadProcessFromRoute()
  },
  beforeUpdate: function() {
    if (this.process != null && this.process.version !== this.versionIndex) {
      // different process-definition was selected
      this.selectedInstance = null
      this.activityInstance = null
      this.activityInstanceHistory = null
      this.task = null
    }
    else if (this.selectedInstance == null && this.instanceId) {
      this.loadInstanceById(this.instanceId)
    }
    else if (!this.instanceId) {
      this.selectedInstance = null
      // Don't clear activityInstance if it's set for filtering purposes (has only an id)
      if (this.activityInstance && Object.keys(this.activityInstance).length > 1) {
        this.activityInstance = null
      }
      this.activityInstanceHistory = null
      this.task = null
    }
  },
  methods: {
    ...mapActions(['clearActivitySelection']),
    formatDate,
    loadInstanceById: function(instanceId) {
      // Always use HistoryService for process instance fetching
      HistoryService.findProcessInstance(instanceId).then(instance => {
        if (instance) {
          this.setSelectedInstance({ selectedInstance: instance })
        }
      }).catch(() => {
        // Fallback to checking store instances
        const selectedInstance = this.instances.find(i => i.id == instanceId)
        if (this.instances) {
          if (selectedInstance) {
            this.setSelectedInstance({ selectedInstance })
          }
        }
      })
    },
    loadProcessFromRoute: function() {
      this.loadProcessByDefinitionKey().then((redirected) => {
        if (!redirected && this.instanceId) {
          this.loadInstanceById(this.instanceId)
        }
      })
    },
    onDeleteProcessDefinition: function(params) {
      ProcessService.deleteProcessDefinition(params.processDefinition.id, true).then(() => {
        // reload versions
        ProcessService.findProcessVersionsByDefinitionKey(this.processKey, this.tenantId, this.$root.config.lazyLoadHistory)
        .then(versions => {
          if (versions.length === 0) {
            // no more process-definitions with such key
            this.$router.replace('/seven/auth/processes')
          } else if (params.processDefinition.version !== this.versionIndex) {
            // remove deleted process-definition from the list
            this.processDefinitions = versions
          }  else {
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
              },
              query: this.$route.query
            })
            this.processDefinitions = versions
          }
        })
      })
    },
    // call from:
    // - user have deleted a non-selected process definition (this.process is still valid)
    // - user clicked "refresh process definitions" button
    onRefreshProcessDefinitions: function(lazyLoad) {
      return ProcessService.findProcessVersionsByDefinitionKey(this.processKey, this.tenantId, lazyLoad).then(versions => {
        this.processDefinitions = versions
        if (this.processDefinitions.length > 0) {
          this.resetStatsLazyLoad(lazyLoad)
          this.loadProcessVersion(this.process)
        }
        return versions
      })
    },
    loadProcessByDefinitionKey: function() {
      return ProcessService.findProcessVersionsByDefinitionKey(this.processKey, this.tenantId, this.$root.config.lazyLoadHistory)
      .then(versions => {
        const requestedDefinition = versions.find(processDefinition => processDefinition.version === this.versionIndex)
        if (requestedDefinition) {
          this.processDefinitions = versions
          const needCalcStats = this.process == null
          if (needCalcStats) {
            this.resetStatsLazyLoad(this.$root.config.lazyLoadHistory)
          }
          return this.loadProcessVersion(requestedDefinition).then(() => {
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
    resetStatsLazyLoad: function(lazyLoad) {
      if (lazyLoad) {
        this.processDefinitions.forEach(v => {
          v.runningInstances = '-'
          v.allInstances = '-'
          v.completedInstances = '-'
        })
      }
      // false - no redirect
      return false
    },
    findProcessAndAssignData(selectedProcess) {
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
    },
    loadProcessActivitiesHistory: function() {
      HistoryService.findActivitiesProcessDefinitionHistory(this.process.id).then(activities => {
        this.process.activitiesHistory = activities
      })
    },
    loadProcessVersion: function(process) {
      return new Promise(() => {
        this.process = process
        this.findProcessAndAssignData(process)
        if (!this.process.statistics) this.loadStatistics()
        if (!this.process.activitiesHistory) this.loadProcessActivitiesHistory()
        return Promise.resolve() // Instances are now loaded by InstancesTable
      })
    },
    loadStatistics: function() {
      ProcessService.findProcessStatistics(this.process.id).then(statistics => {
        this.$store.dispatch('setStatistics', { process: this.process, statistics: statistics })
      })
    },
    onInstanceDeleted: function() {
      this.setSelectedInstance({ selectedInstance: null })
      return Promise.all([
        this.loadStatistics(),
        this.loadProcessActivitiesHistory()
      ]).then(() => {
        this.findProcessAndAssignData(this.process)
        this.$refs.process.refreshDiagram()
      })
    },
    setSelectedInstance: function(evt) {
      var selectedInstance = evt.selectedInstance
      if (!selectedInstance) {
        this.selectedInstance = null
      }
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
        }.bind(this))
      }
    },
    filterInstances: function(filter) {
      this.filter = filter
      // InstancesTable will automatically reload when filter changes
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
        const formattedValues = { 
          ...v, 
          startTime: this.formatDate(v.startTime), 
          endTime: this.formatDate(v.endTime) 
        }
        csvContent += getStringObjByKeys(keys, formattedValues) + '\n'
      })
      var csvBlob = new Blob([csvContent], { type: 'text/csv' })
      var filename = 'Management_Instances_' + moment().format('YYYYMMDD_HHmm') + '.csv'
      this.$refs.importPopper.triggerDownload(csvBlob, filename)
    }
  }
}
</script>
