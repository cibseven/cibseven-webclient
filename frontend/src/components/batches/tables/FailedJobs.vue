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
  <ContentBlock :title="$t('batches.failedJobs')">
    <div class="overflow-auto p-0" style="max-height: 35vh">
      <FlowTable v-if="jobs && jobs.length > 0 && !loading" striped thead-class="sticky-header" :items="jobs" primary-key="id" prefix="batches."
        :fields="[
          { label: 'id', key: 'id', class: 'col-3', tdClass: 'p-1' },
          { label: 'exception', key: 'exceptionMessage', class: 'col-6', tdClass: 'p-1' },
          { label: 'actions', key: 'actions', sortable: false, class: 'col-3 d-flex justify-content-center', tdClass: 'py-0' }
        ]">
        <template v-slot:cell(id)="table">
          <div class="text-truncate" :title="table.item.id">
            {{ table.item.id }}
          </div>
        </template>
        <template v-slot:cell(exceptionMessage)="table">
          <div class="text-truncate" :title="table.item.exceptionMessage">
            {{ table.item.exceptionMessage }}
          </div>
        </template>
        <template v-slot:cell(actions)="table">
          <b-button :title="$t('batches.retryJob')" size="sm" variant="outline-secondary"
            class="border-0 mdi mdi-18px mdi-reload" @click="retryJob(table.item.id)">
          </b-button>
          <b-button :title="$t('batches.deleteJob')" size="sm" variant="outline-secondary"
            class="border-0 mdi mdi-18px mdi-delete-outline" @click="deleteJob(table.item.id)">
          </b-button>
          <b-button :title="$t('batches.seeFullLog')" size="sm" variant="outline-secondary"
            class="border-0 mdi mdi-18px mdi-text-long" @click="seeFullLog(table.item.id)">
          </b-button>
        </template>
      </FlowTable>
      <div class="mb-3 text-center w-100" v-if="loading">
        <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('batches.loading') }}
      </div>
      <div class="mb-3 text-center w-100" v-if="!loading && jobs.length === 0">
        {{ $t('admin.noResults') }}
      </div>
    </div>
  </ContentBlock>
  <JobLogModal ref="jobLogModal"></JobLogModal>
</template>

<script>
import { mapActions, mapGetters } from 'vuex'
import { ContentBlock, FlowTable } from '@cib/common-frontend'
import JobLogModal from '../modals/JobLogModal.vue'
import { BWaitingBox } from '@cib/common-frontend'

export default {
  name: 'FailedJobs',
  components: { ContentBlock, FlowTable, JobLogModal, BWaitingBox },
  props: { batch: Object },
  data: function() {
    return {
      loading: false,
    }
  },
  watch: {
    batch: {
      handler: 'loadFailedJobs',
      immediate: true
    }
  },
  computed: {
    ...mapGetters('job', ['jobs'])
  },
  methods: {
    ...mapActions('job', ['loadJobs', 'retryJobById', 'deleteJob']),
    async loadFailedJobs() {
      if (!this.batch?.batchJobDefinitionId) return
      this.loading = true
      const params = {
        jobDefinitionId: this.batch.batchJobDefinitionId,
        noRetriesLeft: true,
        sorting: [{ sortBy: 'jobId', sortOrder: 'asc' }],
        withException: true
      }
      try {
        await this.loadJobs(params)
      } finally {
        this.loading = false
      }
    },
    async retryJob(jobId) {
      await this.retryJobById({ id: jobId, retries: 1 })
      this.loadFailedJobs()
    },
    async seeFullLog(jobId) {
      this.$refs.jobLogModal.show(jobId)
    }
  }
}
</script>
