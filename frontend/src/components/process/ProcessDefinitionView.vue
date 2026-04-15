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
        <template v-if="errorVersionNotFound !== null">
          <WarningBox :message="$t('process.definitionVersionNotFound', [errorVersionNotFound])"/>
          <WarningBox v-if="processDefinitions.length === 0" :message="$t('process.definitionNotFound', { processKey: processKey, tenantId: tenantId })"/>
        </template>
        <ProcessDetailsSidebar ref="navbar" v-if="process && (!selectedInstance && !instanceId)"
          :process-key="processKey"
          :process-definitions="processDefinitions"
          :version-index="computedVersionIndex"
          @on-refresh-process-definitions="onRefreshProcessDefinitions"
          @on-delete-process-definition="onDeleteProcessDefinition"
          :instances="instances"
          :selected-instance="selectedInstance"
        ></ProcessDetailsSidebar>
        <ProcessInstanceDetailsSidebar v-else-if="(selectedInstance || instanceId)"
          :instance="selectedInstance"
          :process-definition="process"
        ></ProcessInstanceDetailsSidebar>
      </template>
      <transition name="slide-in" mode="out-in">
        <ProcessInstancesView ref="process" v-if="process && !selectedInstance && !instanceId"
          :loading="loading"
          :process="process"
          :process-key="processKey"
          :version-index="computedVersionIndex"
          :activity-instance="activityInstance"
          :activity-instance-history="activityInstanceHistory"
          :tenant-id="tenantId"
          :filter="filter"
          :parent-process="parentProcess"
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
          :tenant-id="tenantId"
          @task-selected="setSelectedTask($event)"></ProcessInstanceView>
      </transition>
      <transition name="slide-in" mode="out-in">
        <WarningBox v-if="errorVersionNotFound !== null && processDefinitions.length === 0" :message="$t('process.definitionNotFound', { processKey: processKey, tenantId: tenantId })"/>
      </transition>
      <transition name="slide-in" mode="out-in">
        <WarningBox v-if="errorLoadingInstanceId !== null" :message="$t('process.instanceNotFound', { instanceId: instanceId, error: errorLoadingInstanceId })"/>
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
import ProcessInstanceDetailsSidebar from '@/components/process/ProcessInstanceDetailsSidebar.vue'
import WarningBox from '@/components/common-components/WarningBox.vue'
import { SidebarsFlow, TaskPopper } from '@cib/common-frontend'
import { mapGetters, mapActions } from 'vuex'
import { formatDate } from '@/utils/dates.js'

function getStringObjByKeys(keys, obj) { // TODO rewrite to use join()
  let result = ''
  keys.forEach(key => {
    result += obj[key] + ';'
  })
  return result.slice(0, -1)
}

export default {
  name: 'ProcessDefinitionView',
  components: { ProcessInstancesView, ProcessDetailsSidebar, ProcessInstanceView, ProcessInstanceDetailsSidebar, SidebarsFlow, TaskPopper, WarningBox },
  props: {
    processKey: { type: String, required: true },
    versionIndex: { type: String, required: true },
    instanceId: { type: String },
    tenantId: { type: String }
  },
  watch: {
    processKey: {
      async handler() {
        // Reset process state when processKey changes
        this.process = null
        this.selectedInstance = null
        this.activityInstance = null
        this.activityInstanceHistory = null
        this.parentProcess = null
        this.errorVersionNotFound = null
        this.clearActivitySelection()

        await this.loadProcessDefinitionFromRoute()
      },
      immediate: true      
    },
    async versionIndex() {
      if (this.process && this.process.key === this.processKey) {
        await this.switchToDefinitionVersion()
      }
    },
    async instanceId() {
      if (this.process && this.process.key === this.processKey && this.instanceId) {
        await this.loadInstanceById(this.instanceId)
      }
    }
  },
  data() {
    return {
      leftOpen: true,
      process: null, // selected process definition
      processDefinitions: [],
      errorVersionNotFound: null,
      errorLoadingInstanceId: null,
      selectedInstance: null,
      task: null,
      activityInstance: null,
      activityInstanceHistory: null,
      filter: {},
      loading: false,
      parentProcess: null
    }
  },
  computed: {
    ...mapGetters('instances', ['instances']),
    shortendLeftCaption() {
      return this.$t('process.details.historyVersions')
    },
    processName() {
      if (!this.process) return this.processKey
      return this.process.name || this.process.key
    },
    computedVersionIndex() {
      return this.process?.version || this.versionIndex
    },
  },
  async beforeUpdate() {
    if (this.process != null && this.process.version !== this.versionIndex) {
      // different process-definition was selected
      this.selectedInstance = null
      this.activityInstance = null
      this.activityInstanceHistory = null
      this.task = null
    }
    else if (this.selectedInstance == null && this.instanceId) {
      await this.loadInstanceById(this.instanceId)
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
    ...mapActions(['clearActivitySelection', 'getProcessById']),
    formatDate,
    async findProcessInstance(instanceId) {
      return (this.$root.config.camundaHistoryLevel !== 'none') ?
        HistoryService.findProcessInstance(instanceId) :
        ProcessService.findProcessInstance(instanceId)
    },
    async loadInstanceById(instanceId) {
      this.selectedInstance = null
      this.activityInstanceHistory = null
      this.task = null

      let instance = null
      try {
        this.errorLoadingInstanceId = null
        instance = await this.findProcessInstance(instanceId)
      } catch (error) {
        // ignore error, fallback below
        this.errorLoadingInstanceId = error.message
      }
      if (!instance && this.instances) {
        // Fallback to checking store instances
        instance = this.instances.find(i => i.id == instanceId)
      }
      if (instance) {
        await this.setSelectedInstance({ selectedInstance: instance })
      }
    },
    async loadProcessDefinitionFromRoute() {

      let tenantId = this.tenantId
      
      if (this.instanceId) {
        await this.loadInstanceById(this.instanceId)
        if (this.selectedInstance) {
          // instance found, load its process definition
          await ProcessService.findProcessById(this.selectedInstance.processDefinitionId, true).then(process => {
            this.process = process
          })
          if (this.process) {
            await this.loadStatistics()
            tenantId = this.process.tenantId
          }
        }
      }

      await ProcessService.findProcessVersionsByDefinitionKey(this.processKey, tenantId, this.$root.config.lazyLoadHistory).then(async versions => {
        this.processDefinitions = versions
        const needCalcStats = this.process == null
        if (needCalcStats) {
          this.resetStatsLazyLoad(this.$root.config.lazyLoadHistory)
        }

        await this.switchToDefinitionVersion()
      })
    },
    async switchToDefinitionVersion() {
      const requestedDefinition = this.processDefinitions.find(processDefinition => processDefinition.version === this.versionIndex)
      if (requestedDefinition) {
        await this.loadProcessVersion(requestedDefinition)
        this.errorVersionNotFound = null
      }
      else if (this.processDefinitions.length > 0) {
        // version from URL not found, load latest version
        this.errorVersionNotFound = this.versionIndex
        await this.loadProcessVersion(this.processDefinitions[0])
      }
      else {
        // no process definitions with such key
        this.errorVersionNotFound = this.versionIndex
      }

      if (this.instanceId) {
        await this.loadInstanceById(this.instanceId)
      }
    },
    async onDeleteProcessDefinition(params) {
      await ProcessService.deleteProcessDefinition(params.processDefinition.id, true).then(async () => {
        // reload versions
        await ProcessService.findProcessVersionsByDefinitionKey(this.processKey, this.tenantId, this.$root.config.lazyLoadHistory)
        .then(versions => {
          if (versions.length === 0) {
            // no more process-definitions with such key
            this.$router.replace('/seven/auth/processes')
          } else if (params.processDefinition.version !== this.computedVersionIndex) {
            // remove deleted process-definition from the list
            this.processDefinitions = versions
          }  else {
            // Find nearest process-definition to deleted one and select it.
            //
            // 5 4 3 2 1
            //     ^      - deleted
            //   ^   ^    - one of final nextVersionIndex
            //
            let nextVersionIndex = versions[0].version
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
              query: {
                ...this.$route.query,
                ...(this.tenantId ? { tenantId: this.tenantId } : {}),
              }
            })
            this.processDefinitions = versions
          }
        })
      })
    },
    // call from:
    // - user have deleted a non-selected process definition (this.process is still valid)
    // - user clicked "refresh process definitions" button
    async onRefreshProcessDefinitions(lazyLoad) {
      const versions = await ProcessService.findProcessVersionsByDefinitionKey(this.processKey, this.tenantId, lazyLoad)
      this.processDefinitions = versions
      if (this.processDefinitions.length > 0) {
        this.resetStatsLazyLoad(lazyLoad)
        await this.loadProcessVersion(this.process)
      }
      return versions
    },
    resetStatsLazyLoad(lazyLoad) {
      if (lazyLoad) {
        this.processDefinitions.forEach(v => {
          v.runningInstances = '-'
          v.allInstances = '-'
          v.completedInstances = '-'
        })
      }
    },
    async findProcessAndAssignData(selectedProcess) {
      if (selectedProcess) {
          await ProcessService.findProcessById(selectedProcess.id, true).then(process => {
            for (const v of this.processDefinitions) {
              if (v.id === process.id) {
                Object.assign(v, process)
                break
              }
            }
          })
        }
    },
    async loadProcessVersion(process) {
      this.process = process
      await this.findProcessAndAssignData(process)
      if (!this.process.statistics) await this.loadStatistics()

      // Load parent process if parentProcessDefinitionId exists in route query
      if (this.$route.query.parentProcessDefinitionId) {
        await this.getProcessById({ id: this.$route.query.parentProcessDefinitionId }).then(response => {
          this.parentProcess = response
        })
      } else {
        this.parentProcess = null
      }
    },
    async loadStatistics() {
      await ProcessService.findProcessStatistics(this.process.id).then(statistics => {
        this.$store.dispatch('setStatistics', { process: this.process, statistics: statistics })
      })
    },
    async onInstanceDeleted() {
      await this.setSelectedInstance({ selectedInstance: null })
      await this.loadStatistics()
      await this.findProcessAndAssignData(this.process)
      this.$refs.process.refreshDiagram()
    },
    async setSelectedInstance(evt) {
      const selectedInstance = evt.selectedInstance
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
          await ProcessService.findActivityInstance(selectedInstance.id).then(async activityInstance => {
            this.activityInstance = activityInstance
            await HistoryService.findActivitiesInstancesHistory(selectedInstance.id).then(activityInstanceHistory => {
              this.activityInstanceHistory = activityInstanceHistory
            })
          })
        } else {
          //History
          if (this.$root.config.camundaHistoryLevel !== 'none') {
            await HistoryService.findActivitiesInstancesHistory(selectedInstance.id).then(activityInstanceHistory => {
              this.activityInstanceHistory = activityInstanceHistory
            })
          }
        }
      }
    },
    async setSelectedTask(selectedTask) {
      if (this.selectedInstance && selectedTask) {
        await HistoryService.findTasksByDefinitionKeyHistory(selectedTask.id, this.selectedInstance.id).then(async function(task) {
          if (task.length === 0) {
            this.task = null
            return
          }
          this.task = task[0]
          const serviceCall = !this.task.endTime ? TaskService.fetchActivityVariables :
            HistoryService.fetchActivityVariablesHistory
          await serviceCall(this.task.activityInstanceId).then(variables => {
            variables.forEach(variable => {
              variable.value = variable.type === 'Object' ? JSON.stringify(variable.value) : variable.value
            })
            this.task.variables = variables
          })
        }.bind(this))
      }
    },
    filterInstances(filter) {
      this.filter = filter
      // InstancesTable will automatically reload when filter changes
    },
    getIconState(state) {
      switch(state) {
        case 'ACTIVE':
          return 'mdi-chevron-triple-right text-success'
        case 'SUSPENDED':
          return 'mdi-close-circle-outline'
      }
      return 'mdi-flag-triangle'
    },
    exportCSV() {
      const headers = [
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
      let csvContent = headers.map(h => h.text).join(';') + '\n'
      const keys = headers.map(h => h.key)
      this.instances.forEach(v => {
        const formattedValues = {
          ...v,
          startTime: this.formatDate(v.startTime),
          endTime: this.formatDate(v.endTime)
        }
        csvContent += getStringObjByKeys(keys, formattedValues) + '\n'
      })
      const csvBlob = new Blob([csvContent], { type: 'text/csv' })
      const filename = 'Management_Instances_' + moment().format('YYYYMMDD_HHmm') + '.csv'
      this.$refs.importPopper.triggerDownload(csvBlob, filename)
    }
  }
}
</script>
