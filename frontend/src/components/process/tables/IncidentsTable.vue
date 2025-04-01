<template>
  <div class="overflow-auto bg-white position-absolute container-fluid g-0" style="top: 0; bottom: 0">
    <FlowTable v-if="incidents.length > 0" striped thead-class="sticky-header" :items="incidents" primary-key="id" prefix="process-instance.incidents."
      sort-by="label" :sort-desc="true" :fields="[
      { label: 'message', key: 'incidentMessage', class: 'col-3', tdClass: 'py-1 border-end border-top-0' },
      { label: 'timestamp', key: 'incidentTimestamp', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'activity', key: 'activityId', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'failedActivity', key: 'failedActivityId', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'incidentType', key: 'incidentType', class: 'col-2', tdClass: 'py-1 border-top-0' },
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
      <template v-slot:cell(actions)="table">
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

  <b-modal ref="stackTraceModal" :title="$t('process-instance.stacktrace')" size="xl">
    <div v-if="stackTraceMessage" class="container-fluid pt-3">
      <b-form-textarea v-model="stackTraceMessage" rows="20" readonly></b-form-textarea>
      <b-button variant="link" @click="copyValueToClipboard(stackTraceMessage)">{{ $t('process-instance.copyValueToClipboard') }}</b-button>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.stackTraceModal.hide()">{{ $t('confirm.close') }}</b-button>
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
      stackTraceMessage: ''
    }
  },
  props: {
    incidents: Array,
    activityInstance: Object,
    activityInstanceHistory: Object
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
          this.incidents.splice(this.incidents.findIndex(obj => obj.configuration === id), 1)
          this.$refs.incidentRetryModal.hide()
      })
    }
  }
}
</script>
