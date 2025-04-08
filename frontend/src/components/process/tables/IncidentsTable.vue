<template>
  <div class="overflow-auto bg-white position-absolute container-fluid g-0" style="top: 0; bottom: 0">
    <FlowTable v-if="localIncidents.length > 0" striped thead-class="sticky-header" :items="localIncidents" primary-key="id" prefix="process-instance.incidents."
      sort-by="label" :sort-desc="true" :fields="[
      { label: 'message', key: 'incidentMessage', class: 'col-3', tdClass: 'py-1 border-end border-top-0' },
      { label: 'timestamp', key: 'incidentTimestamp', class: 'col-1', tdClass: 'py-1 border-end border-top-0' },
      { label: 'activity', key: 'activityId', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'failedActivity', key: 'failedActivityId', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'incidentType', key: 'incidentType', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'annotation', key: 'annotation', class: 'col-1', tdClass: 'position-relative py-1 border-end border-top-0' },
      { label: 'actions', key: 'actions', class: 'col-1', sortable: false, tdClass: 'py-1 border-top-0' }]">
      <template v-slot:cell(incidentMessage)="table">
        <div :title="table.item.incidentMessage" class="text-truncate" @click="showIncidentMessage(table.item.configuration)">{{ table.item.incidentMessage }}</div>
      </template>
      <template v-slot:cell(incidentTimestamp)="table">
        <div :title="table.item.incidentTimestamp" class="text-truncate">{{ showPrettyTimestamp(table.item.incidentTimestamp) }}</div>
      </template>
      <template v-slot:cell(activityId)="table">
        <div :title="table.item.activityId" class="text-truncate">{{ $store.state.activity.processActivities[table.item.activityId] }}</div>
      </template>
      <template v-slot:cell(failedActivityId)="table">
        <div :title="table.item.failedActivityId" class="text-truncate">{{ $store.state.activity.processActivities[table.item.failedActivityId] }}</div>
      </template>
      <template v-slot:cell(incidentType)="table">
        <div :title="table.item.incidentType" class="text-truncate">{{ table.item.incidentType }}</div>
      </template>
      <template v-slot:cell(annotation)="table">
        <div :title="table.item.annotation" class="text-truncate w-100" @mouseenter="focusedCell = table.item" @mouseleave="focusedCell = null">
          {{ table.item.annotation }}
          <span v-if="table.item && focusedCell === table.item" @click.stop="copyValueToClipboard(table.item.annotation)"
            class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
        </div>
      </template>
      <template v-slot:cell(actions)="table">
        <b-button :title="$t('process-instance.incidents.editAnnotation')"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-note-edit-outline"
          @click="showAnnotationModal(table.item)">
        </b-button>
        <b-button :title="$t('process-instance.incidents.retryJob')"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-reload"
          @click="$refs.incidentRetryModal.show(table.item)">
        </b-button>
      </template>
    </FlowTable>
    <div v-else>
      <p class="text-center p-4">{{ $t('process-instance.noIncidents') }}</p>
    </div>
  </div>

  <b-modal ref="stackTraceModal" :title="$t('process-instance.stacktrace')" size="xl" :ok-only="true">
    <div v-if="stackTraceMessage" class="container-fluid pt-3">
      <b-form-textarea v-model="stackTraceMessage" rows="20" readonly></b-form-textarea>
      <b-button variant="link" @click="copyValueToClipboard(stackTraceMessage)">{{ $t('process-instance.copyValueToClipboard') }}</b-button>
    </div>
  </b-modal>

  <b-modal ref="annotationModal" :title="$t('process-instance.incidents.editAnnotation')">
    <div v-if="selectedIncident">
      <b-form-group>
        <template #label>{{ $t('process-instance.incidents.annotation') }}</template>
        <b-form-textarea v-model="selectedIncident.annotation" :maxlength="annotationMaxLength" class="mb-1"></b-form-textarea>
        <div class="small float-end" :class="{ 'text-danger': invalidAnnotation }">{{ annotationLengthInfo }}</div>
      </b-form-group>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.annotationModal.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="saveAnnotation()" :disabled="invalidAnnotation" variant="primary">{{ $t('commons.save') }}</b-button>
    </template>
  </b-modal>

  <IncidentRetryModal ref="incidentRetryModal" @increment-number-retry="incrementNumberRetry"></IncidentRetryModal>
  <SuccessAlert ref="messageCopy" style="z-index: 9999">{{ $t('process.copySuccess') }}</SuccessAlert>
  <SuccessAlert ref="successRetryJob">{{ $t('process-instance.successRetryJob') }}</SuccessAlert>

</template>

<script>
import moment from 'moment'
import procesessVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { IncidentService } from '@/services.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import IncidentRetryModal from '@/components/process/modals/IncidentRetryModal.vue'

export default {
  name: 'IncidentsTable',
  components: { FlowTable, SuccessAlert, IncidentRetryModal },
  mixins: [procesessVariablesMixin, copyToClipboardMixin],
  data: function() {
    return {
      stackTraceMessage: '',
      localIncidents: [],
      selectedIncident: null,
      focusedCell: null,
      annotationMaxLength: 4000
    }
  },
  created: function() {
    this.localIncidents = [...this.incidents]
  },
  props: {
    incidents: Array,
    activityInstance: Object,
    activityInstanceHistory: Object
  },
  computed: {
    invalidAnnotation: function() {
      if (!this.selectedIncident) return true
      else if (!this.selectedIncident.annotation) return false
      return this.selectedIncident.annotation.length > this.annotationMaxLength
    },
    annotationLengthInfo: function() {
      const length = this.selectedIncident?.annotation?.length || 0
      return `${length} / ${this.annotationMaxLength}`
    }
  },
  methods: {
    showIncidentMessage: function(jobDefinitionId) {
      this.stackTraceMessage = ''
      IncidentService.fetchIncidentStacktraceByJobId(jobDefinitionId).then(res => {
        this.stackTraceMessage = res
        this.$refs.stackTraceModal.show()
      })
    },
    showPrettyTimestamp: function(orignalDate) {
      return moment(orignalDate).format('DD/MM/YYYY HH:mm:ss')
    },
    incrementNumberRetry: function({ id, params }) {
      IncidentService.retryJobById(id, params).then(() => {
        this.localIncidents.splice(this.localIncidents.findIndex(obj => obj.configuration === id), 1)
        this.$refs.incidentRetryModal.hide()
      })
    },
    showAnnotationModal: function(incident) {
      this.selectedIncident = { ...incident }
      this.$refs.annotationModal.show()
    },
    saveAnnotation: function() {
      if (this.invalidAnnotation) return
      var params = { annotation: this.selectedIncident.annotation }
      IncidentService.setIncidentAnnotation(this.selectedIncident.id, params).then(() => {
        const incident = this.localIncidents.find(i => i.id === this.selectedIncident.id)
        if (incident) {
          incident.annotation = this.selectedIncident.annotation
        }
        this.$refs.annotationModal.hide()
      })
    }
  }
}
</script>
