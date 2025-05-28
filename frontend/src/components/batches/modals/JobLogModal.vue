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
  <b-modal ref="jobLogModal" :title="$t('batches.jobLog.modalTitle')" size="xl">
    <div>
      <FlowTable v-if="jobLogs && !selectedJobLog && !loading" striped thead-class="sticky-header" :items="jobLogs" primary-key="id" prefix="batches.jobLog." 
        :fields="jobLogFields" @click="showJobLogDetails($event)">
        <template v-slot:cell(state)="table">{{ state(table.item) }}</template>
        <template v-slot:cell(jobExceptionMessage)="table">
          <div class="text-truncate" :title="table.item.jobExceptionMessage">{{ table.item.jobExceptionMessage }}</div>
        </template>
        <template v-slot:cell(timestamp)="table">
          <div class="text-truncate" :title="formatDate(table.item.timestamp)">{{ formatDate(table.item.timestamp) }}</div>
        </template>
        <template v-slot:cell(jobId)="table">
          <div class="text-truncate" :title="table.item.jobId">{{ table.item.jobId }}</div>
        </template>
        <template v-slot:cell(jobDefinitionType)="table">
          <div class="text-truncate" :title="table.item.jobDefinitionType">{{ table.item.jobDefinitionType }}</div>
        </template>
        <template v-slot:cell(jobDefinitionConfiguration)="table">
          <div class="text-truncate" :title="table.item.jobDefinitionConfiguration">{{ table.item.jobDefinitionConfiguration }}</div>
        </template>
        <template v-slot:cell(hostname)="table">
          <div class="text-truncate" :title="table.item.hostname">{{ table.item.hostname }}</div>
        </template>
      </FlowTable>
      <div v-else-if="selectedJobLog">
        <div class="row">
          <div v-for="field in filteredJobLogFields" :key="field.key" class="mb-2 col-4">
            <strong>{{ $t('batches.jobLog.' + field.label) }}:</strong>
            <div>
              {{ 
                field.key === 'timestamp'
                ? formatDate(selectedJobLog[field.key])
                : field.key === 'state'
                  ? state(selectedJobLog)
                  : selectedJobLog[field.key]  
              }}
            </div>
          </div>
        </div>
        <div v-if="selectedJobLog.stacktrace">
          <span class="d-block mb-2 fw-bold">{{ $t('batches.jobLog.message') }}</span>
          <pre class="bg-light rounded overflow-auto text-wrap text-break p-3" style="max-height: 40vh">
            {{ selectedJobLog.stacktrace }}
          </pre>
        </div>
      </div>
      <div class="mb-3 text-center w-100" v-else-if="loading">
        <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('batches.loading') }}
      </div>
    </div>
    <template v-slot:modal-footer>
      <b-button v-if="selectedJobLog" @click="selectedJobLog = null" variant="outline-secondary">{{ $t('process.back') }}</b-button>
      <b-button @click="$refs.jobLogModal.hide()" variant="primary">{{ $t('confirm.ok') }}</b-button>
    </template>
  </b-modal>
  <SuccessAlert ref="messageCopy" style="z-index: 9999">{{ $t('process.copySuccess') }}</SuccessAlert>
</template>

<script>  
  import { moment } from '@/globals.js'
  import { BWaitingBox } from 'cib-common-components'
  import { mapActions, mapGetters } from 'vuex'
  import FlowTable from '@/components/common-components/FlowTable.vue'
  import SuccessAlert from '@/components/common-components/SuccessAlert.vue'

  export default {
    name: 'JobLogModal',
    components: { FlowTable, BWaitingBox, SuccessAlert },
    props: { batch: Object },
    data: function() {
      return {
        loading: false,
        selectedJobLog: null
      }
    },
    computed: {
      ...mapGetters(['jobLogs']),
      jobLogFields: function() {
        return [
          { label: 'state', key: 'state', class: 'col-1', tdClass: 'p-1', sortable: false },
          { label: 'message', key: 'jobExceptionMessage', class: 'col-1', tdClass: 'p-1', sortable: false },
          { label: 'timestamp', key: 'timestamp', class: 'col-2', tdClass: 'p-1' },
          { label: 'jobId', key: 'jobId', class: 'col-2', tdClass: 'p-1' },
          { label: 'type', key: 'jobDefinitionType', class: 'col-1', tdClass: 'p-1', sortable: false },
          { label: 'configuration', key: 'jobDefinitionConfiguration', class: 'col-1', tdClass: 'p-1', sortable: false },
          { label: 'retries', key: 'jobRetries', class: 'col-1', tdClass: 'p-1 text-center' },
          { label: 'hostname', key: 'hostname', class: 'col-2', tdClass: 'p-1' },
          { label: 'priority', key: 'jobPriority', class: 'col-1', tdClass: 'p-1 text-center' }
        ]
      },
      filteredJobLogFields: function() {
        return this.jobLogFields.filter(field => field.key !== 'jobExceptionMessage')
      }
    },
    methods: {
      ...mapActions(['getHistoryJobLog', 'getHistoryJobLogStacktrace']),
      show: function() {
        this.selectedJobLog = null
        this.$refs.jobLogModal.show()
      },
      state: function(log) {
        if (log.creationLog) return this.$t('batches.jobLog.created')
        if (log.failureLog) return this.$t('batches.jobLog.failed')
        if (log.successLog) return this.$t('batches.jobLog.successful')
        return this.$t('batches.jobLog.unknown')
      },
      formatDate: function(date) {
        return moment(date).format('DD/MM/YYYY HH:mm:ss')
      },
      async showJobLogDetails(job) {
        this.selectedJobLog = null
        this.loading = true
        const stacktrace = await this.getHistoryJobLogStacktrace(job.id)
        this.selectedJobLog = job
        this.selectedJobLog.stacktrace = stacktrace
        this.loading = false
      }
    }
  }
</script>