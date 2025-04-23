<template>
  <div class="bg-white shadow p-3 mt-4">
    <div>
      <h4 class="d-inline">{{ $t('batches.failedJobs') }}</h4>
      <hr>
      <div class="overflow-auto">
        <FlowTable v-if="jobs && jobs.length > 0 && !loading" striped thead-class="sticky-header" :items="jobs" primary-key="id" prefix="batches." 
          :fields="[
            { label: 'id', key: 'id', class: 'col-3', tdClass: 'border-end p-1' },
            { label: 'exception', key: 'exceptionMessage', class: 'col-6', tdClass: 'border-end p-1' },
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
    </div>
  </div>
  <JobLogModal ref="jobLogModal"></JobLogModal>
</template>

<script>
import { mapActions, mapGetters } from 'vuex'
import FlowTable from '@/components/common-components/FlowTable.vue'
import JobLogModal from '../modals/JobLogModal.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'FailedJobs',
  components: { FlowTable, JobLogModal, BWaitingBox },
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
    ...mapGetters(['jobs'])
  },
  methods: {
    ...mapActions(['getJobs', 'retryJobById', 'getHistoryJobLog', 'getHistoryJobLogStacktrace', 'deleteJob']),
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
        await this.getJobs(params)
      } finally {
        this.loading = false
      }
    },
    async retryJob(jobId) {
      await this.retryJobById({ id: jobId, retries: 1 })
      this.loadFailedJobs()
    },
    async seeFullLog(jobId) {
      const params = {
        jobId,
        sortBy: 'timestamp',
        sortOrder: 'desc'
      }
      await this.getHistoryJobLog(params)
      this.$refs.jobLogModal.show()
    }
  }
}
</script>
