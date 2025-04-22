<template>
  <div class="bg-white shadow-sm p-3 border rounded" v-if="batchId && batchType">
    <div v-if="batchDetails && batchDetails.length > 0 && !loading">
      <h5 class="d-inline">{{ $t('batches.' + batchType + 'Details') }}</h5>
      <b-button class="border float-end" size="sm" variant="light" @click="$refs.confirmRemove.show()" :title="$t('batches.remove')">
        <span class="mdi mdi-delete-outline me-1"></span>{{ $t('batches.remove') }}
      </b-button>
      <b-button v-if="batchType === 'runtime'" class="border float-end me-1" size="sm" variant="light"
        @click="setBatchSuspensionState" :title="$t('batches.suspend')">
        <span class="mdi me-1" :class="batch.suspended ? 'mdi-play' : 'mdi-pause'"></span>
        {{ batch.suspended ? $t('batches.activate') : $t('batches.suspend') }}
      </b-button>
      <hr>
      <div class="overflow-auto">
        <FlowTable v-if="batchDetails" striped thead-class="sticky-header" :items="batchDetails" primary-key="id" prefix="batches."
          :fields="[
            { label: 'property', key: 'property', class: 'col-6', tdClass: 'border-end p-1' },
            { label: 'value', key: 'value', class: 'col-6', tdClass: 'p-1' },
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
  </div>
</template>

<script>
  import moment from 'moment'
  import FlowTable from '@/components/common-components/FlowTable.vue'
  import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
  import { BWaitingBox } from 'cib-common-components'
  import { BatchService } from '@/services.js'
  import { mapActions, mapGetters } from 'vuex'

  export default {
    name: 'BatchDetails',
    components: { FlowTable, BWaitingBox, ConfirmDialog },
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
      ...mapActions(['deleteHistoricBatch', 'deleteRuntimeBatch', 'getHistoricBatch']),
      async loadBatchDetails() {
        if (!this.batchId || !this.batchType) return
        this.batch = null
        this.loading = true

        try {
          if (this.batchType === 'history') {
            this.batch = await this.getHistoricBatch(this.batchId)
          } else if (this.batchType === 'runtime') {
            const res = await BatchService.getBatchStatistics({ batchId: this.batchId })
            this.batch = res[0]
          }
        } finally {
          this.loading = false
        }
      },
      async setBatchSuspensionState() {
        const params = { suspended: !this.batch.suspended }
        await BatchService.setBatchSuspensionState(this.batchId, params)
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
      formatDate: function(date) {
        return moment(date).format('DD/MM/YYYY HH:mm:ss')
      }
    }
  }
</script>
