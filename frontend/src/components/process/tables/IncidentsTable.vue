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
  <div class="overflow-auto bg-white container-fluid g-0 h-100">
    <div v-if="loading">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
    <FlowTable v-else-if="incidents.length > 0" :items="incidents"
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
        <span v-if="row.item.deleted">{{ $t('process-instance.incidents.deleted') }}</span>
        <span v-else-if="row.item.resolved">{{ $t('process-instance.incidents.resolved') }}</span>
        <span v-else-if="row.item.open">{{ $t('process-instance.incidents.open') }}</span>
        <span v-else>{{ $t('process-instance.incidents.unknown') }}</span>
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
      <template v-slot:cell(createTime)="table">
        <div :title="formatDateForTooltips(table.item.createTime)" class="text-truncate">{{ formatDateForTooltips(table.item.createTime) }}</div>
      </template>
      <template v-slot:cell(endTime)="table">
        <div :title="formatDateForTooltips(table.item.endTime)" class="text-truncate">{{ formatDateForTooltips(table.item.endTime) }}</div>
      </template>
      <template v-slot:cell(activityId)="table">
        <div :title="table.item.activityId" class="text-truncate">{{ $store.state.activity.processActivities[table.item.activityId] || table.item.activityId }}</div>
      </template>
      <template v-slot:cell(failedActivityId)="table">
        <div :title="table.item.failedActivityId" class="text-truncate">{{ $store.state.activity.processActivities[table.item.failedActivityId] || table.item.failedActivityId }}</div>
      </template>
      <template v-slot:cell(causeIncidentProcessInstanceId)="table">
        <CopyableActionButton 
          :display-value="table.item.causeIncidentProcessInstanceId"
          :copy-value="table.item.causeIncidentProcessInstanceId" 
          :title="table.item.causeIncidentProcessInstanceId"
          @click="navigateToIncidentProcessInstance(table.item.causeIncidentProcessInstanceId)"
          @copy="copyValueToClipboard"
        />
      </template>
      <template v-slot:cell(rootCauseIncidentProcessInstanceId)="table">
        <CopyableActionButton 
          :display-value="table.item.rootCauseIncidentProcessInstanceId"
          :copy-value="table.item.rootCauseIncidentProcessInstanceId" 
          :title="table.item.rootCauseIncidentProcessInstanceId"
          @click="navigateToIncidentProcessInstance(table.item.rootCauseIncidentProcessInstanceId)"
          @copy="copyValueToClipboard"
        />
      </template>
      <template v-slot:cell(incidentType)="table">
        <div :title="table.item.incidentType" class="text-truncate">{{ table.item.incidentType }}</div>
      </template>
      <template v-slot:cell(annotation)="table">
        <div :title="table.item.annotation" class="text-truncate w-100" @click="copyValueToClipboard(table.item.annotation)">
          {{ table.item.annotation }}
        </div>
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
    <div v-else-if="!loading">
      <p class="text-center p-4">{{ $t('process-instance.noIncidents') }}</p>
    </div>

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
import { BWaitingBox } from 'cib-common-components'
import CopyableActionButton from '@/components/common-components/CopyableActionButton.vue'
import { formatDateForTooltips } from '@/utils/dates.js'
import { createSortComparator } from '@/utils/sort.js'
import { mapGetters, mapActions } from 'vuex'


// Helper to get incident state as a sortable number
function getStateAsNumber(incident) {
  if (incident.deleted) return 3
  if (incident.resolved) return 2
  if (incident.open) return 1
  return 4 // unknown
}

export default {
  name: 'IncidentsTable',
  components: { FlowTable, SuccessAlert, RetryModal, AnnotationModal, StackTraceModal, BWaitingBox, CopyableActionButton },
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
      currentSortDesc: false
    }
  },
  computed: {
    ...mapGetters('incidents', ['incidents']),
    ...mapGetters('instances', ['instances']),
    visibleColumns() {
      return [
        'state',
        'incidentMessage',
        ...(this.isInstanceView ? [] : ['processInstanceId']),
        ...(this.isInstanceView ? [] : ['businessKey']),
        'createTime',
        'endTime',
        'activityId',
        'failedActivityId',
        'causeIncidentProcessInstanceId',
        'rootCauseIncidentProcessInstanceId',
        'incidentType',
        'annotation',
        'actions'
      ]
    },
    columnDefinitions() {
      return [
        { label: 'process-instance.incidents.state', key: 'state' },
        { label: 'process-instance.incidents.incidentType', key: 'incidentType' },
        { label: 'process-instance.incidents.message', key: 'incidentMessage' },
        { label: 'process-instance.incidents.createTime', key: 'createTime', groupSeparator: true },
        { label: 'process-instance.incidents.endTime', key: 'endTime' },
        ...(this.isInstanceView ? [] : [{ label: 'process-instance.incidents.processInstance', key: 'processInstanceId', groupSeparator: true }]),
        ...(this.isInstanceView ? [] : [{ label: 'process.businessKey', key: 'businessKey' }]),
        { label: 'process-instance.incidents.activity', key: 'activityId' },
        { label: 'process-instance.incidents.failedActivity', key: 'failedActivityId' },
        { label: 'process-instance.incidents.causeIncidentProcessInstanceId', key: 'causeIncidentProcessInstanceId' },
        { label: 'process-instance.incidents.rootCauseIncidentProcessInstanceId', key: 'rootCauseIncidentProcessInstanceId' },
        { label: 'process-instance.incidents.annotation', key: 'annotation', groupSeparator: true },
        { label: 'process-instance.incidents.actions', key: 'actions', disableToggle: true, sortable: false, groupSeparator: true, tdClass: 'py-0' }
      ]
    },
    useCase() {
      return this.isInstanceView ? 'process-instance-incidents' : 'process-definition-incidents'
    }
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
    ...mapActions('incidents', ['loadIncidents', 'removeIncident', 'updateIncidentAnnotation', 'setIncidents']),
    formatDateForTooltips,
    async loadIncidentsData(id, isInstance = true) {
      this.loading = true
      const params = {
        sortBy: 'incidentType',
        sortOrder: 'asc',
        ...(isInstance ? { processInstanceId: id } : { processDefinitionId: id })
      }
      try {
        await this.loadIncidents(params)
        if (!this.isInstanceView && this.incidents.length > 0) {
          this.enrichWithBusinessKey()
        }
      } finally {
        this.loading = false
      }
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

      let sortedIncidents
      if (sortBy === 'state') {
        // Custom sorting logic for state field
        sortedIncidents = [...this.incidents].sort(
          createSortComparator(getStateAsNumber, sortDesc)
        )
      } else {
        // For other fields, use standard sorting
        sortedIncidents = [...this.incidents].sort(
          createSortComparator(item => item[sortBy], sortDesc)
        )
      }
      this.setIncidents(sortedIncidents)
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
