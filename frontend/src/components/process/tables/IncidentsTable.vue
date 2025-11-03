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
  <div ref="scrollableArea"  class="overflow-auto bg-white container-fluid g-0 h-100">
    <PagedScrollableContent
      :loading="loading"
      :loaded-count="incidents.length"
      :total-count="totalCount"
      :chunk-size="maxResults"
      :scrollable-area="$refs.scrollableArea"
      @load-next-page="loadNextPage"
      :show-loading-spinner="loading">
    <FlowTable
      :items="incidents"
      striped
      resizable
      thead-class="sticky-header"
      primary-key="id"
      prefix=""
      native-layout
      :column-definitions="columnDefinitions"
      :columns="visibleColumns"
      :useCase="useCase"
      external-sort
      :sort-by="currentSortBy" :sort-desc="currentSortDesc"
      @external-sort="handleExternalSort">

      <template #cell(state)="row">
        <span v-if="row.item.deleted" class="text-truncate mdi mdi-18px mdi-minus-circle-outline" :title="$t('process-instance.incidents.state') + ': ' + $t('process-instance.incidents.deleted')"><span class="ms-1">{{ $t('process-instance.incidents.deleted') }}</span></span>
        <span v-else-if="row.item.resolved" class="text-truncate mdi mdi-18px mdi-check-circle-outline text-success" :title="$t('process-instance.incidents.state') + ': ' + $t('process-instance.incidents.resolved')"><span class="ms-1">{{ $t('process-instance.incidents.resolved') }}</span></span>
        <span v-else-if="row.item.open" class="text-truncate mdi mdi-18px mdi-alert-outline mt-0 text-warning" :title="$t('process-instance.incidents.state') + ': ' + $t('process-instance.incidents.open')"><span class="ms-1">{{ $t('process-instance.incidents.open') }}</span></span>
        <span v-else class="text-truncate mdi mdi-18px mdi-help-circle-outline" :title="$t('process-instance.incidents.state') + ': ' + $t('process-instance.incidents.unknown')"><span class="ms-1">{{ $t('process-instance.incidents.unknown') }}</span></span>
      </template>

      <template v-slot:cell(incidentType)="table">
        <CopyableActionButton
          :display-value="table.item.incidentType"
          :copy-value="table.item.incidentType" 
          :title="$t('process-instance.incidents.incidentType') + ':\n' + table.item.incidentType"
          :clickable="false"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(incidentMessage)="table">
        <CopyableActionButton 
          :display-value="getIncidentMessage(table.item)"
          :copy-value="getIncidentMessage(table.item)" 
          :title="getIncidentMessage(table.item)"
          class="text-truncate w-100"
          @click="showIncidentMessage(table.item)"
          @copy="copyValueToClipboard"
        />
      </template>

      <template #cell(processInstanceId)="row">
        <CopyableActionButton 
          v-if="row.item.processInstanceId"
          :display-value="row.item.processInstanceId"
          :copy-value="row.item.processInstanceId" 
          :title="row.item.processInstanceId"
          @click="navigateToIncidentProcessInstance(row.item.processInstanceId)"
          @copy="copyValueToClipboard"
        />
        <span v-else class="text-muted fst-italic" :title="$t('commons.notAvailable.tooltip')">{{ $t('commons.notAvailable.label') }}</span>
      </template>

      <template #cell(businessKey)="row">
        <CopyableActionButton v-if="row.item.businessKey !== undefined"
          :display-value="row.item.businessKey"
          :copy-value="row.item.businessKey" 
          :title="row.item.businessKey"
          :clickable="false"
          @copy="copyValueToClipboard"
        />
        <span v-else class="text-muted fst-italic" :title="$t('commons.notAvailable.tooltip')">{{ $t('commons.notAvailable.label') }}</span>
      </template>

      <template v-slot:cell(incidentTimestamp)="table">
        <div :title="formatDateForTooltips(table.item.incidentTimestamp)" class="text-truncate">{{ formatDateForTooltips(table.item.incidentTimestamp) }}</div>
      </template>

      <template v-slot:cell(createTime)="table">
        <div :title="formatDateForTooltips(table.item.createTime)" class="text-truncate">{{ formatDateForTooltips(table.item.createTime) }}</div>
      </template>

      <template v-slot:cell(endTime)="table">
        <div :title="formatDateForTooltips(table.item.endTime)" class="text-truncate">{{ formatDateForTooltips(table.item.endTime) }}</div>
      </template>

      <template v-slot:cell(activityId)="table">
        <CopyableActionButton
          :display-value="$store.state.activity.processActivities[table.item.activityId] || table.item.activityId"
          :copy-value="$store.state.activity.processActivities[table.item.activityId] || table.item.activityId" 
          :title="$t('process-instance.incidents.activity') + ':\n' + ($store.state.activity.processActivities[table.item.activityId] || table.item.activityId)"
          :clickable="false"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(failedActivityId)="table">
        <CopyableActionButton
          :display-value="$store.state.activity.processActivities[table.item.failedActivityId] || table.item.failedActivityId"
          :copy-value="$store.state.activity.processActivities[table.item.failedActivityId] || table.item.failedActivityId" 
          :title="$t('process-instance.incidents.failedActivity') + ':\n' + ($store.state.activity.processActivities[table.item.failedActivityId] || table.item.failedActivityId)"
          :clickable="false"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(executionId)="table">
        <CopyableActionButton
          :display-value="table.item.executionId"
          :copy-value="table.item.executionId" 
          :title="$t('process-instance.incidents.executionId') + ':\n' + table.item.executionId"
          :clickable="false"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(causeIncidentProcessInstanceId)="table">
        <CopyableActionButton 
          :display-value="table.item.causeIncidentProcessInstanceId"
          :copy-value="table.item.causeIncidentProcessInstanceId" 
          :title="$t('process-instance.incidents.causeIncidentProcessInstanceId') + ':\n' + table.item.causeIncidentProcessInstanceId"
          @click="navigateToIncidentProcessInstance(table.item.causeIncidentProcessInstanceId)"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(rootCauseIncidentProcessInstanceId)="table">
        <CopyableActionButton 
          :display-value="table.item.rootCauseIncidentProcessInstanceId"
          :copy-value="table.item.rootCauseIncidentProcessInstanceId" 
          :title="$t('process-instance.incidents.rootCauseIncidentProcessInstanceId') + ':\n' + table.item.rootCauseIncidentProcessInstanceId"
          @click="navigateToIncidentProcessInstance(table.item.rootCauseIncidentProcessInstanceId)"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(annotation)="table">
        <CopyableActionButton
          :display-value="table.item.annotation"
          :copy-value="table.item.annotation" 
          :title="$t('process-instance.incidents.editAnnotation') + ':\n' + table.item.annotation"
          @click="$refs.annotationModal.show(table.item.id, table.item.annotation)"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(actions)="table">
        <b-button v-if="!table.item.endTime" :title="$t('process-instance.incidents.editAnnotation')"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-note-edit-outline"
          @click="$refs.annotationModal.show(table.item.id, table.item.annotation)">
        </b-button>
        <b-button v-if="!table.item.endTime" :title="$t('process-instance.incidents.retryJob')"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-reload"
          @click="$refs.incidentRetryModal.show(table.item)">
        </b-button>
      </template>
    </FlowTable>
      <div v-if="!loading && incidents.length === 0">
        <p class="text-center p-4">{{ $t('process-instance.noIncidents') }}</p>
      </div>
    </PagedScrollableContent>

    <AnnotationModal ref="annotationModal" @set-annotation="setIncidentAnnotation" lang-key="process-instance.incidents"></AnnotationModal>
    <RetryModal ref="incidentRetryModal" @increment-number-retry="incrementNumberRetry" translation-prefix="process-instance.incidents."></RetryModal>
    <StackTraceModal ref="stackTraceModal"></StackTraceModal>
    <SuccessAlert ref="messageCopy">{{ $t('process.copySuccess') }}</SuccessAlert>
    <SuccessAlert ref="successRetryJob">{{ $t('process-instance.successRetryJob') }}</SuccessAlert>
  </div>
</template>

<script>
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { IncidentService, HistoryService } from '@/services.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import RetryModal from '@/components/process/modals/RetryModal.vue'
import AnnotationModal from '@/components/process/modals/AnnotationModal.vue'
import StackTraceModal from '@/components/process/modals/StackTraceModal.vue'
import PagedScrollableContent from '@/components/common-components/PagedScrollableContent.vue'
import CopyableActionButton from '@/components/common-components/CopyableActionButton.vue'
import { formatDateForTooltips } from '@/utils/dates.js'
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'IncidentsTable',
  components: { FlowTable, SuccessAlert, RetryModal, AnnotationModal, StackTraceModal, PagedScrollableContent, CopyableActionButton },
  mixins: [copyToClipboardMixin],
  props: {
    instance: Object,
    process: Object,
    activityInstance: Object,
    isInstanceView: Boolean
  },
  data: function() {
    return {
      loading: true,
      currentSortBy: 'incidentType',
      currentSortDesc: false,
      firstResult: 0,
      maxResults: 1,
      totalCount: null,
    }
  },
  computed: {
    ...mapGetters('incidents', ['incidents']),
    ...mapGetters('instances', ['instances']),
    isHistoricView() {
      switch (this.$root.config.camundaHistoryLevel) {
        case 'none':
        case 'activity':
          return false // always runtime view
        case 'full':
        case 'audit':
        default:
          if (this.isInstanceView) {
            return this.instance?.state !== 'ACTIVE' // historic view for non-active instances
          }
          return true // history view for definition incidents
      }
    },
    visibleColumns() {
      return [
        ...(this.isHistoricView ? ['state'] : []),
        'incidentType',
        'incidentMessage',

        ...(this.isHistoricView ? [
          'createTime',
          'endTime',
        ] : [
          'incidentTimestamp',
        ]),

        ...(this.isInstanceView ? [] : ['processInstanceId']),
        ...(this.isInstanceView ? [] : ['businessKey']),
        'activityId',
        'failedActivityId',
        // 'executionId', // hidden by default
        'causeIncidentProcessInstanceId',
        'rootCauseIncidentProcessInstanceId',
        'annotation',
        'actions'
      ]
    },
    columnDefinitions() {
      return [
        ...(this.isHistoricView ? [{ label: 'process-instance.incidents.state', key: 'state', tdClass: 'pt-1' }] : []),
        { label: 'process-instance.incidents.incidentType', key: 'incidentType' },
        { label: 'process-instance.incidents.message', key: 'incidentMessage' },

        ...(
          this.isHistoricView ? [
            { label: 'process-instance.incidents.createTime', key: 'createTime', groupSeparator: true },
            { label: 'process-instance.incidents.endTime', key: 'endTime' },
          ] : [
            { label: 'process-instance.incidents.timestamp', key: 'incidentTimestamp' },
          ]
        ),

        ...(this.isInstanceView ? [] : [{ label: 'process-instance.incidents.processInstance', key: 'processInstanceId', groupSeparator: true }]),
        ...(this.isInstanceView ? [] : [{ label: 'process.businessKey', key: 'businessKey', sortable: false }]),
        { label: 'process-instance.incidents.activity', key: 'activityId', groupSeparator: this.isInstanceView },
        { label: 'process-instance.incidents.failedActivity', key: 'failedActivityId', sortable: false },
        { label: 'process-instance.incidents.executionId', key: 'executionId' },
        { label: 'process-instance.incidents.causeIncidentProcessInstanceId', key: 'causeIncidentProcessInstanceId', sortable: false },
        { label: 'process-instance.incidents.rootCauseIncidentProcessInstanceId', key: 'rootCauseIncidentProcessInstanceId', sortable: false },
        ...(this.isInstanceView ? [] : [{ label: 'process.tenant', key: 'tenantId' }]),
        { label: 'process-instance.incidents.annotation', key: 'annotation', sortable: false, groupSeparator: true },
        { label: 'process-instance.incidents.actions', key: 'actions', disableToggle: true, sortable: false, groupSeparator: true, tdClass: 'py-0' }
      ]
    },
    useCase() {
      const useCase = this.isInstanceView ? 'process-instance-incidents' : 'process-definition-incidents'
      const viewType = this.isHistoricView ? 'historic' : 'runtime'
      return `${useCase}-${viewType}`
    },
  },
  watch: {
    'instance.id': {
      handler(id) {
        if (id) {
          this.loadIncidentsData(id, true)
        }
      },
      immediate: true
    },
    'process.id': {
      handler(id) {
        if (id && !this.instance) {
          this.loadIncidentsData(id, false)
        }
      },
      immediate: true
    }
  },
  methods: {
    ...mapActions('incidents', ['loadRuntimeIncidents', 'loadHistoryIncidents', 'removeIncident', 'updateIncidentAnnotation', 'setIncidents']),
    formatDateForTooltips,
    async fetchCount(params) {
      const countParams = { ...params }
      delete countParams.firstResult
      delete countParams.maxResults
      return this.isHistoricView ?
        IncidentService.fetchHistoricIncidentsCount(countParams) :
        IncidentService.findIncidentsCount(countParams)
    },
    async fetch(params) {
      return this.isHistoricView ?
        this.loadHistoryIncidents(params) :
        this.loadRuntimeIncidents(params)
    },
    async loadIncidentsData(id, isInstance = true) {
      this.loading = true

      const params = {
        firstResult: this.firstResult,
        maxResults: this.maxResults,
        sortBy: this.currentSortBy === 'state' ? 'incidentState' : this.currentSortBy,
        sortOrder: this.currentSortDesc ? 'asc' : 'desc',
        ...(isInstance ? { processInstanceId: id } : { processDefinitionId: id })
      }

      // clear existing incidents when loading first page
      if (this.firstResult === 0) {
        this.setIncidents([])
        this.totalCount = null
      }

      this.fetchCount(params).then(count => {
        this.totalCount = count
      }).catch(() => {
        this.totalCount = null
      })

      try {
        await this.fetch(params)
        if (!this.isInstanceView && this.incidents.length > 0) {
          this.enrichWithBusinessKey()
        }
      } finally {
        this.loading = false
      }
    },
    loadNextPage() {
      this.firstResult += this.maxResults
      const id = this.isInstanceView ? this.instance.id : this.process.id
      this.loadIncidentsData(id, this.isInstanceView)
    },
    enrichWithBusinessKey() {
      // create mapping of instance IDs to their business keys
      const instanceIdToProcessInfo = this.instances.reduce((map, instance) => {
        map[instance.id] = { businessKey: instance.businessKey }
        return map
      }, {})

      // enrich incidents with business keys
      const enrichedIncidents = this.incidents.map(incident => {
        const instanceInfo = instanceIdToProcessInfo[incident.processInstanceId]
        return {
          ...incident,
          ...instanceInfo,
        }
      })

      this.setIncidents(enrichedIncidents)
    },
    handleExternalSort({ sortBy, sortDesc }) {
      this.currentSortBy = sortBy
      this.currentSortDesc = sortDesc
      this.firstResult = 0
      this.loadIncidentsData(this.isInstanceView ? this.instance.id : this.process.id, this.isInstanceView)
    },
    showIncidentMessage: function(incident) {
      const configuration = incident.historyConfiguration || incident.rootCauseIncidentConfiguration
      if (!configuration) return

      const isHistoric = !!incident.historyConfiguration
      const isExternalTask = incident.incidentType === 'failedExternalTask'
      
      // Select appropriate service method based on incident type and whether it's historic
      const stackTracePromise = this.getStackTracePromise(isHistoric, isExternalTask, configuration)
      
      stackTracePromise.then(res => {
        this.$refs.stackTraceModal.show(res)
      })
    },
    getStackTracePromise(isHistoric, isExternalTask, configuration) {
      if (isExternalTask) {
        return isHistoric 
          ? IncidentService.fetchHistoricIncidentStacktraceByExternalTaskId(configuration)
          : IncidentService.fetchIncidentStacktraceByExternalTaskId(configuration)
      } else {
        return isHistoric
          ? IncidentService.fetchHistoricStacktraceByJobId(configuration)
          : IncidentService.fetchIncidentStacktraceByJobId(configuration)
      }
    },
    incrementNumberRetry: function({ item, params }) {
      // Choose the appropriate retry method based on incident type
      let retryPromise
      if (item.incidentType === 'failedExternalTask') {
        // For external task incidents, use the external task retry endpoint
        retryPromise = IncidentService.retryExternalTaskById(item.configuration, params)
      } else {
        // For other incident types, use job retry
        retryPromise = IncidentService.retryJobById(item.configuration, params)
      }
      retryPromise.then(() => {
        this.removeIncident(item.id)
        this.$refs.incidentRetryModal.hide()
      })
    },
    setIncidentAnnotation: function({ id, params }) {
      IncidentService.setIncidentAnnotation(id, params).then(() => {
        this.updateIncidentAnnotation({ incidentId: id, annotation: params.annotation })
        this.$refs.annotationModal.hide()
      })
    },
    getIncidentMessage(incident) {
      return incident.rootCauseIncidentMessage || incident.incidentMessage
    },
    async navigateToIncidentProcessInstance(processInstanceId) {
      if (!processInstanceId) return
      try {
        const processInstance = await HistoryService.findProcessInstance(processInstanceId)
        const processKey = processInstance.processDefinitionKey
        const versionIndex = processInstance.processDefinitionVersion
        const params = { processKey, versionIndex, instanceId: processInstance.id }
        
        const routeConfig = {
          name: 'process',
          params,
          query: { 
            parentProcessDefinitionId: this.process.id,
            tab: 'incidents'
          }
        }
        
        await this.$router.push(routeConfig)
      } catch (error) {
        console.error('Failed to navigate to incident process instance:', error)
      }
    }
  }
}
</script>
