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
      sort-by="incidentType" native-layout :fields="[
      { label: 'message', key: 'incidentMessage', tdClass: 'border-end border-top-0' },
      { label: 'timestamp', key: 'incidentTimestamp', tdClass: 'border-end border-top-0' },
      { label: 'activity', key: 'activityId', tdClass: 'border-end border-top-0' },
      { label: 'failedActivity', key: 'failedActivityId', tdClass: 'border-end border-top-0' },
      { label: 'causeIncidentProcessInstanceId', key: 'causeIncidentProcessInstanceId', tdClass: 'border-end border-top-0' },
      { label: 'rootCauseIncidentProcessInstanceId', key: 'rootCauseIncidentProcessInstanceId', tdClass: 'border-end border-top-0' },
      { label: 'incidentType', key: 'incidentType', tdClass: 'border-end border-top-0' },
      { label: 'annotation', key: 'annotation', tdClass: 'border-end border-top-0' },
      { label: 'actions', key: 'actions', sortable: false, tdClass: 'py-0 border-top-0' }]">
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
      <template v-slot:cell(incidentTimestamp)="table">
        <div :title="table.item.incidentTimestamp" class="text-truncate">{{ showPrettyTimestamp(table.item.incidentTimestamp) }}</div>
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

    <AnnotationModal ref="annotationModal" @set-incident-annotation="setIncidentAnnotation"></AnnotationModal>
    <IncidentRetryModal ref="incidentRetryModal" @increment-number-retry="incrementNumberRetry"></IncidentRetryModal>
    <StackTraceModal ref="stackTraceModal"></StackTraceModal>
    <SuccessAlert ref="messageCopy">{{ $t('process.copySuccess') }}</SuccessAlert>
    <SuccessAlert ref="successRetryJob">{{ $t('process-instance.successRetryJob') }}</SuccessAlert>
  </div>
</template>

<script>
import { moment } from '@/globals.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { IncidentService, HistoryService } from '@/services.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import IncidentRetryModal from '@/components/process/modals/IncidentRetryModal.vue'
import AnnotationModal from '@/components/process/modals/AnnotationModal.vue'
import StackTraceModal from '@/components/process/modals/StackTraceModal.vue'
import { BWaitingBox } from 'cib-common-components'
import CopyableActionButton from '@/components/common-components/CopyableActionButton.vue'
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'IncidentsTable',
  components: { FlowTable, SuccessAlert, IncidentRetryModal, AnnotationModal, StackTraceModal, BWaitingBox, CopyableActionButton },
  mixins: [copyToClipboardMixin],
  props: {
    instance: Object,
    process: Object,
    activityInstance: Object
  },
  computed: {
    ...mapGetters('incidents', ['incidents'])
  },
  data: function() {
    return {
      loading: true
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
    ...mapActions('incidents', ['loadIncidents', 'removeIncident', 'updateIncidentAnnotation']),
    async loadIncidentsData(id, isInstance = true) {
      this.loading = true
      const params = {
        sortBy: 'incidentType',
        sortOrder: 'asc',
        ...(isInstance ? { processInstanceId: id } : { processDefinitionId: id })
      }
      try {
        await this.loadIncidents(params)
      } finally {
        this.loading = false
      }
    },
    showIncidentMessage: function(incident) {
      // Choose the appropriate method based on incident type
      let stackTracePromise
      const configuration = incident.rootCauseIncidentConfiguration || incident.configuration
      if (incident.incidentType === 'failedExternalTask') {
        // For external task incidents, use the external task error details endpoint
        stackTracePromise = IncidentService.fetchIncidentStacktraceByExternalTaskId(configuration)
      } else {
        // For other incident types, use job stack trace
        stackTracePromise = IncidentService.fetchIncidentStacktraceByJobId(configuration)
      }
      stackTracePromise
        .then(res => {
          this.$refs.stackTraceModal.show(res)
        })
        .catch(error => {
          // Handle stack trace loading errors with user feedback
          console.error('Failed to load incident stack trace:', error)
          if (this.$refs && this.$refs.error) {
            this.$refs.error.show()
          }
        })
    },
    showPrettyTimestamp: function(orignalDate) {
      return moment(orignalDate).format('DD/MM/YYYY HH:mm:ss')
    },
    incrementNumberRetry: function({ incident, params }) {
      // Choose the appropriate retry method based on incident type
      let retryPromise
      if (incident.incidentType === 'failedExternalTask') {
        // For external task incidents, use the external task retry endpoint
        retryPromise = IncidentService.retryExternalTaskById(incident.configuration, params)
      } else {
        // For other incident types, use job retry
        retryPromise = IncidentService.retryJobById(incident.configuration, params)
      }
      retryPromise.then(() => {
        this.removeIncident(incident.id)
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
