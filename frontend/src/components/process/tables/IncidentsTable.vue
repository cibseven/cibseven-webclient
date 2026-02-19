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
  <div class="bg-white container-fluid overflow-y-scroll g-0 h-100">
    <PagedScrollableContent
      :loading="loading"
      :loaded-count="incidents.length"
      :total-count="totalCount"
      :chunk-size="maxResults"
      :scrollable-area="scrollableArea"
      @load-next-page="loadNextPage"
      :show-loading-spinner="loading">

      <div class="d-flex w-100">
        <div class="col-6 p-3">
          <b-input-group size="sm">
            <template #prepend>
              <b-button :title="$t('searches.search')" aria-hidden="true" size="sm" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </template>
            <b-form-input :title="$t('searches.search')" size="sm" :placeholder="$t('searches.search')" @input="(evt) => onInput(evt.target.value.trim())"></b-form-input>
          </b-input-group>
        </div>
        <div v-if="selectedActivityId" class="col-6 p-3">
          <RemovableBadge
            @on-remove="removeSelectedActivityBadge"
            :tooltip-remove="$t('process-instance.incidents.activityIdBadge.remove')"
            :label="$t('process-instance.incidents.activityIdBadge.title', { activityId: selectedActivityId })"
            :tooltip="$t('process-instance.incidents.activityIdBadge.tooltip', { activityId: selectedActivityId })"/>
        </div>
      </div>

    <FlowTable
      :items="incidents"
      striped
      resizable
      thead-class="sticky-header"
      primary-key="id"
      native-layout
      :column-definitions="columnDefinitions"
      :columns="visibleColumns"
      :useCase="useCase"
      external-sort
      :sort-by="currentSortBy" :sort-desc="currentSortDesc"
      @external-sort="handleExternalSort">

      <template #cell(incidentState)="row">
        <div class="text-truncate position-relative w-100">
          <span v-if="row.item.deleted" class="text-truncate mdi mdi-18px mdi-minus-circle-outline" :title="$t('process-instance.incidents.state') + ': ' + $t('process-instance.incidents.deleted')"><span class="ms-1">{{ $t('process-instance.incidents.deleted') }}</span></span>
          <span v-else-if="row.item.resolved" class="text-truncate mdi mdi-18px mdi-check-circle-outline text-success" :title="$t('process-instance.incidents.state') + ': ' + $t('process-instance.incidents.resolved')"><span class="ms-1">{{ $t('process-instance.incidents.resolved') }}</span></span>
          <span v-else-if="row.item.open" class="text-truncate mdi mdi-18px mdi-alert-outline mt-0 text-warning" :title="$t('process-instance.incidents.state') + ': ' + $t('process-instance.incidents.open')"><span class="ms-1">{{ $t('process-instance.incidents.open') }}</span></span>
          <span v-else class="text-truncate mdi mdi-18px mdi-help-circle-outline" :title="$t('process-instance.incidents.state') + ': ' + $t('process-instance.incidents.unknown')"><span class="ms-1">{{ $t('process-instance.incidents.unknown') }}</span></span>
        </div>
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
          :to="linkToIncidentProcessInstance(row.item.processInstanceId)"
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
          :title="getActivityIdTooltip(table.item.activityId)"
          :clickable="false"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(failedActivityId)="table">
        <CopyableActionButton
          :display-value="$store.state.activity.processActivities[table.item.failedActivityId] || table.item.failedActivityId"
          :copy-value="$store.state.activity.processActivities[table.item.failedActivityId] || table.item.failedActivityId"
          :title="getActivityIdTooltip(table.item.failedActivityId)"
          :clickable="!selectedActivityId"
          @click="selectFailedActivityId(table.item.failedActivityId)"
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
          :to="linkToIncidentProcessInstance(table.item.causeIncidentProcessInstanceId, process.id)"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(rootCauseIncidentProcessInstanceId)="table">
        <CopyableActionButton
          :display-value="table.item.rootCauseIncidentProcessInstanceId"
          :copy-value="table.item.rootCauseIncidentProcessInstanceId"
          :title="$t('process-instance.incidents.rootCauseIncidentProcessInstanceId') + ':\n' + table.item.rootCauseIncidentProcessInstanceId"
          :to="linkToIncidentProcessInstance(table.item.rootCauseIncidentProcessInstanceId, process.id)"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(annotation)="table">
        <CopyableActionButton
          :display-value="table.item.annotation"
          :copy-value="table.item.annotation"
          :title="$t('process-instance.incidents.editAnnotation') + ':\n' + table.item.annotation"
          :clickable="!table.item.endTime"
          @click="$refs.annotationModal.show(table.item.id, table.item.annotation)"
          @copy="copyValueToClipboard"
        />
      </template>

      <template v-slot:cell(actions)="table">
        <div class="d-flex">
          <CellActionButton v-if="!table.item.endTime" :title="$t('process-instance.incidents.editAnnotation')"
            icon="mdi-note-edit-outline"
            @click="$refs.annotationModal.show(table.item.id, table.item.annotation)">
          </CellActionButton>
          <CellActionButton v-if="!table.item.endTime" :title="$t('process-instance.incidents.retryJob')"
            icon="mdi-reload"
            @click="$refs.incidentRetryModal.show(table.item)">
          </CellActionButton>
        </div>
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
import { IncidentService } from '@/services.js'
import { FlowTable, SuccessAlert, PagedScrollableContent, CopyableActionButton } from '@cib/common-frontend'
import RetryModal from '@/components/process/modals/RetryModal.vue'
import AnnotationModal from '@/components/process/modals/AnnotationModal.vue'
import StackTraceModal from '@/components/process/modals/StackTraceModal.vue'
import CellActionButton from '@/components/common-components/CellActionButton.vue'
import RemovableBadge from '@/components/common-components/RemovableBadge.vue'
import { formatDateForTooltips } from '@/utils/dates.js'
import { mapGetters, mapActions } from 'vuex'
import { debounce } from '@/utils/debounce.js'

export default {
  name: 'IncidentsTable',
  components: { FlowTable, SuccessAlert, RetryModal, AnnotationModal, StackTraceModal, PagedScrollableContent, CopyableActionButton, CellActionButton, RemovableBadge },
  mixins: [copyToClipboardMixin],
  props: {
    instance: Object,
    process: Object,
    tenantId: String,
    activityInstance: Object,
    isInstanceView: Boolean,
    scrollableArea: Object,
  },
  data: function() {
    return {
      freeText: '',
      loading: true,
      currentSortBy: 'incidentType',
      currentSortDesc: false,
      firstResult: 0,
      maxResults: this.$root?.config?.maxProcessesResults || 50,
      totalCount: null,
    }
  },
  computed: {
    ...mapGetters('incidents', ['incidents']),
    ...mapGetters('instances', ['instances']),
    ...mapGetters(['selectedActivityId']),
    isHistoricView() {
      switch (this.$root.config.camundaHistoryLevel) {
        case 'none':
        case 'activity':
        case 'audit':
          return false // always runtime view
        case 'full':
        default:
          return true // always history view
      }
    },
    visibleColumns() {
      return [
        ...(this.isHistoricView ? ['incidentState'] : []),
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
        ...(this.isHistoricView ? [{ label: 'process-instance.incidents.state', key: 'incidentState', tdClass: 'pt-1' }] : []),
        { label: 'process-instance.incidents.incidentType', key: 'incidentType' },
        { label: 'process-instance.incidents.message', key: 'incidentMessage' },
        ...(this.isHistoricView ? [{ label: 'process-instance.incidents.createTime', key: 'createTime', groupSeparator: true }] : []),
        ...(this.isHistoricView ? [{ label: 'process-instance.incidents.endTime', key: 'endTime' }] : []),
        ...(this.isHistoricView ? [] : [{ label: 'process-instance.incidents.timestamp', key: 'incidentTimestamp' }]),
        ...(this.isInstanceView ? [] : [{ label: 'process-instance.incidents.processInstance', key: 'processInstanceId', groupSeparator: true }]),
        ...(this.isInstanceView ? [] : [{ label: 'process.businessKey', key: 'businessKey', sortable: false }]),
        { label: 'process-instance.incidents.activity', key: 'activityId', groupSeparator: this.isInstanceView },
        { label: 'process-instance.incidents.failedActivity', key: 'failedActivityId', sortable: false },
        { label: 'process-instance.incidents.executionId', key: 'executionId' },
        { label: 'process-instance.incidents.causeIncidentProcessInstanceId', key: 'causeIncidentProcessInstanceId', sortable: false },
        { label: 'process-instance.incidents.rootCauseIncidentProcessInstanceId', key: 'rootCauseIncidentProcessInstanceId', sortable: false },
        ...(this.isInstanceView ? [] : [{ label: 'process.tenant', key: 'tenantId' }]),
        { label: 'process-instance.incidents.annotation', key: 'annotation', sortable: false, groupSeparator: true },
        { label: 'process-instance.incidents.actions', key: 'actions', disableToggle: true, sortable: false, groupSeparator: true, tdClass: 'py-0', thClass: 'text-truncate' },
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
    },
    'selectedActivityId': {
      handler() {
        this.firstResult = 0
        const id = this.isInstanceView ? this.instance.id : this.process.id
        this.loadIncidentsData(id, this.isInstanceView)
      }
    }
  },
  methods: {
    ...mapActions(['clearActivitySelection', 'setHighlightedElement', 'selectActivity']),
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
        sortBy: this.currentSortBy,
        sortOrder: this.currentSortDesc ? 'asc' : 'desc',
        ...(isInstance ? { processInstanceId: id } : { processDefinitionId: id }),
        ...(this.selectedActivityId ? { failedActivityId: this.selectedActivityId } : {} ),
        ...(this.freeText ? { incidentMessageLike: `%${this.freeText}%` } : {} ),
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
      const id = this.isInstanceView ? this.instance.id : this.process.id
      this.loadIncidentsData(id, this.isInstanceView)
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
    linkToIncidentProcessInstance(processInstanceId, parentProcessDefinitionId = '') {
      return {
        name: 'process-instance-id',
        params: {
          instanceId: processInstanceId,
        },
        query: {
          ...(parentProcessDefinitionId ? { parentProcessDefinitionId } : {}
          ),
          tab: 'incidents',
        }
      }
    },
    onInput: debounce(800, function(freeText) {
      this.freeText = freeText
      this.firstResult = 0
      const id = this.isInstanceView ? this.instance.id : this.process.id
      this.loadIncidentsData(id, this.isInstanceView)
    }),
    removeSelectedActivityBadge() {
      this.clearActivitySelection()
      this.setHighlightedElement('')
    },
    selectFailedActivityId(failedActivityId) {
      this.selectActivity({ activityId: failedActivityId })
      this.setHighlightedElement(failedActivityId)
    },
    getActivityIdTooltip(activityId) {
      const activityName = this.$store.state.activity.processActivities[activityId] || activityId
      return this.$t('process-instance.incidents.activity') + ':\n' + activityName + '\n\n' + this.$t('decision.activityId') + ':\n' + activityId
    },
  }
}
</script>
