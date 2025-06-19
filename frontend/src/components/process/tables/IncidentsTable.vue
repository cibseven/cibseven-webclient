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
    <FlowTable v-else-if="incidents.length > 0" striped thead-class="sticky-header" :items="incidents" primary-key="id" prefix="process-instance.incidents."
      sort-by="label" :sort-desc="true" :fields="[
      { label: 'message', key: 'incidentMessage', class: 'col-3', tdClass: 'py-1 border-end border-top-0' },
      { label: 'timestamp', key: 'incidentTimestamp', class: 'col-1', tdClass: 'py-1 border-end border-top-0' },
      { label: 'activity', key: 'activityId', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'failedActivity', key: 'failedActivityId', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'incidentType', key: 'incidentType', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'annotation', key: 'annotation', class: 'col-1', tdClass: 'py-1 border-end border-top-0' },
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
        <div :title="table.item.annotation" class="text-truncate w-100" @click="copyValueToClipboard(table.item.annotation)">
          {{ table.item.annotation }}
        </div>
      </template>
      <template v-slot:cell(actions)="table">
        <b-button :title="$t('process-instance.incidents.editAnnotation')"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-note-edit-outline"
          @click="$refs.annotationModal.show(table.item.id, table.item.annotation)">
        </b-button>
        <b-button :title="$t('process-instance.incidents.retryJob')"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-reload"
          @click="$refs.incidentRetryModal.show(table.item)">
        </b-button>
      </template>
    </FlowTable>
    <div v-else-if="!loading">
      <p class="text-center p-4">{{ $t('process-instance.noIncidents') }}</p>
    </div>

    <b-modal ref="stackTraceModal" :title="$t('process-instance.stacktrace')" size="xl" :ok-only="true">
      <div v-if="stackTraceMessage" class="container-fluid pt-3">
        <b-form-textarea v-model="stackTraceMessage" rows="20" readonly></b-form-textarea>
        <b-button variant="link" @click="copyValueToClipboard(stackTraceMessage)">{{ $t('process-instance.copyValueToClipboard') }}</b-button>
      </div>
    </b-modal>
  
    <AnnotationModal ref="annotationModal" @set-incident-annotation="setIncidentAnnotation"></AnnotationModal>
    <IncidentRetryModal ref="incidentRetryModal" @increment-number-retry="incrementNumberRetry"></IncidentRetryModal>
    <SuccessAlert ref="messageCopy">{{ $t('process.copySuccess') }}</SuccessAlert>
    <SuccessAlert ref="successRetryJob">{{ $t('process-instance.successRetryJob') }}</SuccessAlert>
  </div>
</template>

<script>
import { moment } from '@/globals.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { IncidentService } from '@/services.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import IncidentRetryModal from '@/components/process/modals/IncidentRetryModal.vue'
import AnnotationModal from '@/components/process/modals/AnnotationModal.vue'
import { BWaitingBox } from 'cib-common-components'
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'IncidentsTable',
  components: { FlowTable, SuccessAlert, IncidentRetryModal, AnnotationModal, BWaitingBox },
  mixins: [copyToClipboardMixin],
  props: {
    instance: Object,
    process: Object,
    activityInstance: Object,
    activityInstanceHistory: Object
  },
  computed: {
    ...mapGetters('incidents', ['incidents'])
  },
  data: function() {
    return {
      stackTraceMessage: '',
      loading: true
    }
  },
  watch: {
    'instance.id': {
      handler(id) {
        if (id) {
          this.loadIncidents(id, true)
        }
      },
      immediate: true
    },
    'process.id': {
      handler(id) {
        if (id && !this.instance) {
          this.loadIncidents(id, false)
        }
      },
      immediate: true
    }
  },
  methods: {
    ...mapActions('incidents', ['loadIncidentsByProcessInstance', 'loadIncidentsByProcessDefinition', 'removeIncident', 'updateIncidentAnnotation']),
    
    async loadIncidents(id, isInstance = true) {
      this.loading = true
      try {
        if (isInstance) {
          await this.loadIncidentsByProcessInstance(id)
        } else {
          await this.loadIncidentsByProcessDefinition(id)
        }
      } finally {
        this.loading = false
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
    incrementNumberRetry: function({ id, params }) {
      IncidentService.retryJobById(id, params).then(() => {
        this.removeIncident(id)
        this.$refs.incidentRetryModal.hide()
      })
    },
    setIncidentAnnotation: function({ id, params }) {
      IncidentService.setIncidentAnnotation(id, params).then(() => {
        this.updateIncidentAnnotation({ incidentId: id, annotation: params.annotation })
        this.$refs.annotationModal.hide()
      })
    }
  }
}
</script>
