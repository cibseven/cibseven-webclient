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
  <div class="overflow-auto bg-white container-fluid g-0 h-100" >
    <flow-table v-if="!loading && matchedCalledList.length > 0" striped thead-class="sticky-header" :items="matchedCalledList" primary-key="id" prefix="process-instance.calledProcesses."
      sort-by="label" :sort-desc="true" :fields="[
      { label: 'state', key: 'state', class: 'col-1', tdClass: 'py-1 border-end border-top-0 justify-content-center' },
      { label: 'calledProcessInstace', key: 'calledProcessInstace', class: 'col-3', tdClass: 'py-1 border-end border-top-0' },
      { label: 'process', key: 'process', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'callingActivity', key: 'callingActivity', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'startTime', key: 'startTime', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'endTime', key: 'endTime', class: 'col-2', tdClass: 'py-1 border-top-0' }]">
     <template v-slot:cell(state)="table">
      <span :title="getIconTitle(table.item)" class="mdi mdi-18px" :class="getIconState(table.item)"></span>
    </template>
     <template v-slot:cell(calledProcessInstace)="table">
       <button :title="table.item.calledProcessInstace" class="btn btn-link text-truncate p-0 text-info text-start" @click="openInstance(table.item)">{{ table.item.calledProcessInstace }}</button>
     </template>
     <template v-slot:cell(process)="table">
        <button :title="table.item.name" class="btn btn-link text-truncate p-0 text-info text-start" @click="openSubprocess(table.item)">{{ table.item.name }}</button>
      </template>
      <template v-slot:cell(callingActivity)="table">
        <div :title="table.item.callingActivity.activityName" class="text-truncate">{{ table.item.callingActivity.activityName }}</div>
      </template>
      <template v-slot:cell(startTime)="table">
        <div :title="formatDate(table.item.startTime)" class="text-truncate">{{ formatDate(table.item.startTime) }}</div>
      </template>
      <template v-slot:cell(endTime)="table">
        <div :title="formatDate(table.item.endTime)" class="text-truncate">{{ formatDate(table.item.endTime) }}</div>
      </template>
    </flow-table>
    <div v-else-if="loading">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
    <div v-else>
      <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
    </div>
  </div>
</template>

<script>
import { formatDate } from '@/utils/dates.js'
import { ProcessService } from '@/services.js'
import processesVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'CalledProcessInstancesTable',
  components: { FlowTable, BWaitingBox},
  mixins: [processesVariablesMixin],
  data: function() {
    return{
      calledInstanceList: [],
      matchedCalledList: [],
      loading: true
    }
  },
  props:{
    selectedInstance: Object
  },
  watch: {
    'selectedInstance.id': function() {
      this.loadCalledProcessInstances()
    }
  },
  created: function() {
    this.loadCalledProcessInstances()
  },
  methods: {
    loadCalledProcessInstances: function() {
      if (!this.selectedInstance || !this.selectedInstance.id) {
        this.loading = false
        return
      }
      this.loading = true
      ProcessService.findCurrentProcessesInstances({"superProcessInstance": this.selectedInstance.id}).then(response => {
        this.calledInstanceList = response
        let key = null
        this.matchedCalledList = this.calledInstanceList.map(processPL => {
          key = processPL.definitionId.match(/^[^:]+/).at(0)
          let foundInst = this.activityInstanceHistory.find(processAIH => {
            if (processAIH.activityType === "callActivity"){
              if (processAIH.calledProcessInstanceId === processPL.id){
                return processAIH
              }
            }
          })
          let foundProcess = this.$store.state.process.list.find(processSPL => {
            if (key === processSPL.key) {
              return processSPL
            }
          })
          return ({
            calledProcessInstace: processPL.id,
            callingActivity: foundInst,
            key: key,
            version: processPL.definitionId.match(/(?!:)\d(?=:)/).at(0),
            name: foundProcess ? foundProcess.name : key,
            endTime: foundInst.endTime,
            startTime: foundInst.startTime
          })
        })
        this.loading = false
      }).catch(error => {
        console.error('Error loading called process instances:', error)
        this.loading = false
      })
    },
    formatDate,
    openSubprocess: function(event) {
      this.$router.push({ name: 'process', params: { processKey: event.key, versionIndex: event.version } })
    },
    openInstance: function(event) {
      this.$router.push({ name: 'process', params: { processKey: event.key, versionIndex: event.version, instanceId: event.calledProcessInstace } })
    },
    getIconTitle: function(instance) {
      if (instance.endTime) {
        return this.$t('process.instanceFinished')
      }
      return this.$t('process.instanceRunning')
    },
    getIconState: function(instance) {
      if (instance.endTime) {
        return 'mdi-close-circle-outline'
      }
      return 'mdi-chevron-triple-right text-success'
    }
  }
}
</script>
