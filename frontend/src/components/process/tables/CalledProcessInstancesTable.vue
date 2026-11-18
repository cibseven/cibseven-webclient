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
    <FlowTable v-if="matchedCalledList.length > 0" striped thead-class="sticky-header" :items="matchedCalledList" primary-key="id"
      sort-by="process" :fields="[
      { label: 'process-instance.calledProcesses.state', key: 'state', class: 'col-1', tdClass: 'py-1 justify-content-center' },
      { label: 'process-instance.calledProcesses.calledProcessInstance', key: 'calledProcessInstance', class: 'col-3', tdClass: 'py-1' },
      { label: 'process-instance.calledProcesses.process', key: 'process', class: 'col-3', tdClass: 'py-1' },
      { label: 'process.businessKey', key: 'businessKey', class: 'col-2', tdClass: 'py-1' },
      { label: 'process-instance.calledProcesses.callingActivity', key: 'callingActivity', class: 'col-3', tdClass: 'py-1' }]">
     <template v-slot:cell(state)="table">
      <span :title="getIconTitle(table.item)" class="mdi mdi-18px" :class="getIconState(table.item)"></span>
    </template>
     <template v-slot:cell(calledProcessInstance)="table">
       <CopyableActionButton
         :display-value="table.item.calledProcessInstance"
         :title="$t('process-instance.calledProcesses.calledProcessInstance') + ':\n' + table.item.calledProcessInstance"
         :to="{
           name: 'process',
           params: {
             processKey: table.item.key,
             versionIndex: table.item.version,
             instanceId: table.item.calledProcessInstance
           },
           query: {
             parentProcessDefinitionId: this.selectedInstance.processDefinitionId,
             ...(table.item.tenantId ? { tenantId: table.item.tenantId } : {} ),
             tab: 'variables',
           }
         }"
         @copy="copyValueToClipboard"
       />
     </template>
     <template v-slot:cell(process)="table">
       <CopyableActionButton
         :display-value="table.item.name || table.item.key"
         :title="$t('process-instance.calledProcesses.process') + ':\n' + (table.item.name || table.item.key)"
         :to="{
           name: 'process',
           params: {
             processKey: table.item.key,
             versionIndex: table.item.version
           },
           query: {
             parentProcessDefinitionId: this.selectedInstance.processDefinitionId,
             ...(table.item.tenantId ? { tenantId: table.item.tenantId } : {} ),
             tab: 'instances',
          }
         }"
         @copy="copyValueToClipboard"
       />
     </template>
      <template v-slot:cell(businessKey)="table">
        <CopyableActionButton
          :display-value="table.item.businessKey"
          :clickable="false"
          @copy="copyValueToClipboard"
        />
      </template>
      <template v-slot:cell(callingActivity)="table">
        <CopyableActionButton
          v-if="table.item.callingActivity"
          :display-value="table.item.callingActivity.activityName || table.item.callingActivity.activityId"
          :copy-value="table.item.callingActivity.activityId"
          :title="$t('process-instance.calledProcesses.callingActivity') + ':\n' + (table.item.callingActivity.activityName || table.item.callingActivity.activityId)"
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
import { HistoryService, ProcessService } from '@/services.js'
import { FlowTable, BWaitingBox, CopyableActionButton, SuccessAlert } from '@cib/common-frontend'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { mapActions } from 'vuex'

export default {
  name: 'CalledProcessInstancesTable',
  components: { FlowTable, BWaitingBox, CopyableActionButton, SuccessAlert },
  mixins: [copyToClipboardMixin],
  data: function() {
    return{
      calledInstanceList: [],
      matchedCalledList: [],
      loading: true,
      firstResult: 0,
      maxResults: this.$root?.config?.maxProcessesResults || 50,
      // Counts the loads, so only the latest one fills the list
      loadRequest: 0
    }
  },
  props: {
    selectedInstance: Object,
    activityInstanceHistory: {
      type: Array,
      default: () => []
    }
  },
  watch: {
    'selectedInstance.id': function() {
      this.loadCalledProcessInstances()
    },
    // The history only names the calling activity of each row, so the loaded rows are matched again:
    // loading them again would race the load in flight and list every called instance twice (CIB7-2830)
    activityInstanceHistory: function() {
      this.matchCalledInstances()
    }
  },
  created: function() {
    this.loadCalledProcessInstances()
  },
  methods: {
    ...mapActions(['setHighlightedElement']),
    handleScroll: function(el) {
      if (Math.ceil(el.target.scrollTop + el.target.clientHeight) >= el.target.scrollHeight) {
        if (this.matchedCalledList.length < this.firstResult || this.loading) return
        this.loadCalledProcessInstances(true)
      }
    },
    async findCalledProcessInstances(filter, firstResult, maxResults) {
      return (this.$root.config.camundaHistoryLevel !== 'none') ?
        HistoryService.findProcessesInstancesHistory(filter, firstResult, maxResults) :
        ProcessService.findCurrentProcessesInstances(filter, firstResult, maxResults)
    },
    /**
     * Loads the first page of the called instances, or with `showMore === true` the next one.
     * A load started later wins: an older answer, success or failure, is ignored, so it neither
     * replaces nor extends the list of the newer one.
     */
    async loadCalledProcessInstances (showMore = false) {
      const request = ++this.loadRequest
      const append = showMore === true
      if (!append) {
        // A new list: never show the rows or continue the paging of the previous instance, also if this load fails
        this.firstResult = 0
        this.calledInstanceList = []
        this.matchedCalledList = []
      }
      if (!this.selectedInstance?.id) {
        this.loading = false
        return
      }
      const firstResult = this.firstResult
      this.loading = true
      try {
        const response = await this.findCalledProcessInstances({"superProcessInstanceId": this.selectedInstance.id}, firstResult, this.maxResults)
        if (request !== this.loadRequest) return
        this.firstResult = firstResult + this.maxResults
        this.calledInstanceList = append ? this.calledInstanceList.concat(response) : response
        this.matchCalledInstances()
      } catch (error) {
        if (request === this.loadRequest) console.error('Error loading called process instances:', error)
      } finally {
        if (request === this.loadRequest) this.loading = false
      }
    },
    matchCalledInstances() {
      this.matchedCalledList = this.calledInstanceList.map(processPL => {
        const key = processPL.processDefinitionKey
        const version = processPL.processDefinitionVersion
        const state = processPL.state
        const tenantId = processPL.tenantId
        const businessKey = processPL.businessKey

        const foundInst = this.activityInstanceHistory?.find(processAIH => {
          if (processAIH.activityType === "callActivity"){
            if (processAIH.calledProcessInstanceId === processPL.id) {
              return processAIH
            }
          }
        })
        const foundProcess = this.$store.state.process.list.find(processSPL => {
          if (key === processSPL.key) {
            return processSPL
          }
        })
        return ({
          calledProcessInstance: processPL.id,
          callingActivity: foundInst,
          key: key,
          version: version,
          name: foundProcess ? foundProcess.name : key,
          state: state,
          tenantId: tenantId,
          businessKey: businessKey
        })
      })
    },
    formatDate,
    getIconTitle: function(instance) {
      switch(instance.state) {
        case 'ACTIVE':
          return this.$t('process.instanceRunning')
        case 'SUSPENDED':
          return this.$t('process.instanceSuspended')
        default:
          return this.$t('process.instanceFinished')
      }
    },
    getIconState: function(instance) {
      switch(instance.state) {
        case 'ACTIVE':
          return 'mdi-chevron-triple-right text-success'
        case 'SUSPENDED':
          return 'mdi-close-circle-outline'
        default:
          return 'mdi-flag-triangle'
      }
    }
  }
}
</script>
