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
  <div class="overflow-auto bg-white container-fluid g-0 h-100" @scroll="handleScroll">
    <FlowTable v-if="matchedCalledList.length > 0" striped thead-class="sticky-header" :items="matchedCalledList" primary-key="id" prefix="process-instance.calledProcesses."
      sort-by="process" :fields="[
      { label: 'state', key: 'state', class: 'col-1', tdClass: 'py-1 justify-content-center' },
      { label: 'calledProcessInstance', key: 'calledProcessInstance', class: 'col-4', tdClass: 'py-1' },
      { label: 'process', key: 'process', class: 'col-4', tdClass: 'py-1' },
      { label: 'callingActivity', key: 'callingActivity', class: 'col-3', tdClass: 'py-1' }]">
     <template v-slot:cell(state)="table">
      <span :title="getIconTitle(table.item)" class="mdi mdi-18px" :class="getIconState(table.item)"></span>
    </template>
     <template v-slot:cell(calledProcessInstance)="table">
       <CopyableActionButton
         :display-value="table.item.calledProcessInstance"
         :title="table.item.name"
         :to="{
           name: 'process',
           params: {
             processKey: table.item.key,
             versionIndex: table.item.version,
             instanceId: table.item.calledProcessInstance
           },
           query: { parentProcessDefinitionId: this.selectedInstance.processDefinitionId, tab: 'variables' }
         }"
         @copy="copyValueToClipboard"
       />
     </template>
     <template v-slot:cell(process)="table">
       <CopyableActionButton
         :display-value="table.item.name || table.item.key"
         :title="table.item.name || table.item.key"
         :to="{
           name: 'process',
           params: {
             processKey: table.item.key,
             versionIndex: table.item.version
           },
           query: { parentProcessDefinitionId: this.selectedInstance.processDefinitionId, tab: 'instances' }
         }"
         @copy="copyValueToClipboard"
       />
     </template>
      <template v-slot:cell(callingActivity)="table">
        <CopyableActionButton
          :display-value="table.item.callingActivity.activityName || table.item.callingActivity.activityId"
          :copy-value="table.item.callingActivity.activityId"
          :title="table.item.callingActivity.activityName || table.item.callingActivity.activityId"
          @click="setHighlightedElement(table.item.callingActivity.activityId)"
          @copy="copyValueToClipboard"
        />
      </template>
    </FlowTable>
    <div v-if="loading">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
    <div v-else-if="matchedCalledList.length === 0">
      <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
    </div>
    <SuccessAlert ref="messageCopy" style="z-index: 9999"> {{ $t('process.copySuccess') }} </SuccessAlert>
  </div>
</template>

<script>
import { formatDate } from '@/utils/dates.js'
import { HistoryService } from '@/services.js'
import processesVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import { BWaitingBox } from 'cib-common-components'
import CopyableActionButton from '@/components/common-components/CopyableActionButton.vue'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import { mapActions } from 'vuex'

export default {
  name: 'CalledProcessInstancesTable',
  components: { FlowTable, BWaitingBox, CopyableActionButton, SuccessAlert },
  mixins: [processesVariablesMixin, copyToClipboardMixin],
  data: function() {
    return{
      calledInstanceList: [],
      matchedCalledList: [],
      loading: true,
      firstResult: 0,
      maxResults: this.$root?.config?.maxProcessesResults || 50
    }
  },
  props:{
    selectedInstance: Object
  },
  watch: {
    'selectedInstance.id': function() {
      this.firstResult = 0
      this.loadCalledProcessInstances()
    },
    activityInstanceHistory: 'loadCalledProcessInstances'
  },
  created: function() {
    if (this.activityInstanceHistory) {
      this.firstResult = 0
      this.loadCalledProcessInstances()
    }
  },
  methods: {
    ...mapActions(['setHighlightedElement']),
    handleScroll: function(el) {
      if (Math.ceil(el.target.scrollTop + el.target.clientHeight) >= el.target.scrollHeight) {
        if (this.matchedCalledList.length < this.firstResult || this.loading) return
        this.loadCalledProcessInstances(true)
      }
    },
    loadCalledProcessInstances (showMore = false) {
      if (!this.selectedInstance || !this.selectedInstance.id) {
        this.loading = false
        return
      }
      this.loading = true
      HistoryService.findProcessesInstancesHistory({"superProcessInstanceId": this.selectedInstance.id}, this.firstResult, this.maxResults).then(response => {
        this.firstResult += this.maxResults
        if (!showMore) {
          this.calledInstanceList = response
        } else if (response.length > 0) {
          this.calledInstanceList = this.calledInstanceList.concat(response)
        }    
        
        this.matchedCalledList = this.calledInstanceList.map(processPL => {
          const key = processPL.processDefinitionKey
          const version = processPL.processDefinitionVersion
          
          let foundInst = this.activityInstanceHistory.find(processAIH => {
            if (processAIH.activityType === "callActivity"){
              if (processAIH.calledProcessInstanceId === processPL.id) {
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
            calledProcessInstance: processPL.id,
            callingActivity: foundInst,
            key: key,
            version: version,
            name: foundProcess ? foundProcess.name : key
          })
        })
        this.loading = false
      }).catch(error => {
        console.error('Error loading called process instances:', error)
        this.loading = false
      })
    },
    formatDate,
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
