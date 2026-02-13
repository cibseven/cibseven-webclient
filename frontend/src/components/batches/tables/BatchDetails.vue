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
  <ContentBlock :title="$t('batches.' + batchType + 'Details')" v-if="batchId && batchType">
    <template #actions>
      <div v-if="batchDetails && batchDetails.length > 0 && !loading">
        <b-button v-if="batchType === 'runtime'" class="me-1" size="sm" variant="light"
          @click="setBatchSuspension" :title="batch.suspended ? $t('batches.activate') : $t('batches.suspend')">
          <span class="mdi" :class="batch.suspended ? 'mdi-play' : 'mdi-pause'"></span>
        </b-button>
        <b-button v-if="batchType === 'runtime' && batch.failedJobs > 0" class="me-1" size="sm" variant="light"
          @click="retryJobs" :title="$t('batches.retryFailedJobs')">
          <span class="mdi mdi-reload"></span>
        </b-button>
        <b-button size="sm" variant="light" @click="$refs.confirmRemove.show()" :title="$t('batches.remove')">
          <span class="mdi mdi-delete-outline"></span>
        </b-button>
      </div>
    </template>
    <div v-if="batchDetails && batchDetails.length > 0 && !loading" class="p-0">
      <div class="overflow-auto">
        <FlowTable v-if="batchDetails" striped thead-class="sticky-header" :items="batchDetails" primary-key="id"
          :fields="[
            { label: 'batches.property', key: 'property', class: 'col-6', tdClass: 'p-1' },
            { label: 'batches.value', key: 'value', class: 'col-6', tdClass: 'p-1' },
          ]">
        </FlowTable>
      </div>
      <ConfirmDialog ref="confirmRemove" @ok="removeBatch" :ok-title="$t('batches.remove')">
          <p>{{ $t('batches.confirmRemove') }}</p>
      </ConfirmDialog>
    </div>
    <div class="mb-3 text-center w-100" v-else-if="loading">
      <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('batches.loading') }}
    </div>
    <div class="text-center w-100" v-else-if="!loading && batchDetails.length === 0">
      {{ $t('admin.noResults') }}
    </div>
  </ContentBlock>
  <FailedJobs v-if="batchType === 'runtime'" :batch="batch"></FailedJobs>
</template>

<script>
import { formatDate } from '@/utils/dates.js'
import FailedJobs from './FailedJobs.vue'
import { BWaitingBox, FlowTable, ContentBlock, ConfirmDialog } from '@cib/common-frontend'
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'BatchDetails',
  components: { FailedJobs, FlowTable, BWaitingBox, ConfirmDialog, ContentBlock },
  watch: {
    '$route.query': {
      handler() {
        this.loadBatchDetails()
      },
      immediate: true
    }
  },
  data: function() {
    return {
      batch: null,
      loading: true
    }
  },
  computed: {
    ...mapGetters(['selectedHistoricBatch']),
    batchId: function() {
      return this.$route.query.id
    },
    batchType: function() {
      return this.$route.query.type
    },
    batchDetails: function() {
      if (!this.batch) return []
      return Object.entries(this.batch)
        .filter(entry => {
          const value = entry[1]
          return value !== null && value !== undefined
        })
        .map(([key, value]) => {
          let formattedValue = value
          if (key.toLowerCase().includes('time') && typeof value === 'string') {
            formattedValue = this.formatDate(value)
          }
          return {
            property: key,
            value: formattedValue
          }
        })
    }
  },
  methods: {
    ...mapActions([
      'deleteHistoricBatch',
      'deleteRuntimeBatch',
      'getHistoricBatch',
      'getBatchStatistics',
      'setBatchSuspensionState'
    ]),
    ...mapActions('job', [
      'retryJobDefinitionById'
    ]),
    formatDate,
    async loadBatchDetails() {
      if (!this.batchId || !this.batchType) return
      this.batch = null
      this.loading = true
      try {
        if (this.batchType === 'history') {
          this.batch = await this.getHistoricBatch(this.batchId)
        } else if (this.batchType === 'runtime') {
          const res = await this.getBatchStatistics({ batchId: this.batchId })
          if (res && res.length > 0) {
            this.batch = res[0]
          } else {
            // If runtime is empty, update the URL to type=history so the watcher reloads automatically
            this.$router.replace({
              query: {
                ...this.$route.query,
                type: 'history'
              }
            })
          }
        }
      } finally {
        this.loading = false
      }
    },
    async setBatchSuspension() {
      const params = { suspended: !this.batch.suspended }
      await this.setBatchSuspensionState({ id: this.batchId, params })
      this.batch.suspended = !this.batch.suspended
    },
    removeBatch: function() {
      if (this.batchType === 'history') {
        this.deleteHistoricBatch(this.batchId).then(() => {
          this.$router.replace({ query: {} })
        })
      } else if (this.batchType === 'runtime') {
        this.deleteRuntimeBatch(this.batchId).then(() => {
          this.$router.replace({ query: {} })
        })
      }
    },
    async retryJobs() {
      const params = {
        id: this.batch.batchJobDefinitionId,
        retries: 1
      }
      await this.retryJobDefinitionById({ id: this.batch.batchJobDefinitionId, params })
      this.loadBatchDetails()
    }
  }
}
</script>
