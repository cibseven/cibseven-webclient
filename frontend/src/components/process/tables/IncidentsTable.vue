<template>
  <div class="overflow-auto bg-white position-absolute container-fluid g-0" style="top: 0; bottom: 0">
    <FlowTable v-if="!loading && selectedInstance.incidents.length > 0" striped thead-class="sticky-header" :items="selectedInstance.incidents" primary-key="id" prefix="process-instance.incidents."
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
        <div :title="table.item.activityId" class="text-truncate">{{ this.getActivityName(table.item.activityId) }}</div>
      </template>
      <template v-slot:cell(failedActivityId)="table">
        <div :title="table.item.failedActivityId" class="text-truncate">{{ getFailingActivity(table.item.failedActivityId) }}</div>
      </template>
      <template v-slot:cell(incidentType)="table">
        <div :title="table.item.incidentType" class="text-truncate">{{ table.item.incidentType }}</div>
      </template>
      <template v-slot:cell(actions)="table">
        <b-button :title="$t('process-instance.incidents.retryJob')"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-reload"
          @click="$refs.confirmRetryJob.show(table.item.configuration)">
        </b-button>
      </template>
    </FlowTable>
    <div v-else-if="loading">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
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

  <SuccessAlert ref="messageCopy" style="z-index: 9999">{{ $t('process.copySuccess') }}</SuccessAlert>

  <ConfirmDialog ref="confirmRetryJob" @ok="retryJob($event)">
    {{ $t('process-instance.confirmRetryJob') }}
  </ConfirmDialog>

  <SuccessAlert ref="successRetryJob">{{ $t('process-instance.successRetryJob') }}</SuccessAlert>

</template>

<script>
import moment from 'moment'
import procesessVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { IncidentService } from '@/services.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'IncidentsTable',
  components: { FlowTable, SuccessAlert, ConfirmDialog, BWaitingBox },
  mixins: [procesessVariablesMixin, copyToClipboardMixin],
  data: function() {
    return {
      stackTraceMessage: ''
    }
  },
  props: {
    getFailingActivity: Function
  },
  methods: {
    getActivityName: function(activityId) {
      var result = null
      if (this.activityInstance && this.activityInstance.childTransitionInstances) {
        result = this.activityInstance.childTransitionInstances.find(activity => {
          return activity.activityId === activityId
        })
        if (result !== undefined) {
          return result.activityName
        }
      }
      // original code
      result = this.activityInstanceHistory.find(activity => {
        return activity.activityId === activityId
      })
      if (result !== undefined) {
        return result.activityName
      }
      else {
        return 'N/A'
      }
    },
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
    retryJob: function(jobDefinitionId) {
      IncidentService.retryJobById(jobDefinitionId).then(() => {
        this.selectedInstance.incidents.splice(this.selectedInstance.incidents.findIndex(obj => obj.configuration === jobDefinitionId), 1)
        this.$refs.successRetryJob.show()
      })
    }
  }
}
</script>
