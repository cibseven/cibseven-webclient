<template>
  <div class="bg-white shadow p-3" v-if="batchId && type">
    <div v-if="batchDetails && batchDetails.length > 0 && !loading">
      <h4 class="d-inline">{{ $t('batches.' + type + 'Details') }}</h4>
      <b-button class="border float-end" size="sm" variant="light" @click="$refs.confirmRemove.show()" :title="$t('batches.remove')">
        <span class="mdi mdi-delete-outline me-1"></span>{{ $t('batches.remove') }}
      </b-button>
      <b-button v-if="type === 'runtime'" class="border float-end me-1" size="sm" variant="light" 
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

  export default {
    name: 'BatchDetails',
    emits: ['batch-deleted'],
    components: { FlowTable, BWaitingBox, ConfirmDialog },
    watch: {
      '$route.query': {
        handler() {
          this.loadBatchDetails()
        },
        immediate: true
      }
    },
    data: function () {
      return {
        batch: null,
        batchId: null,
        type: null,
        loading: false
      }
    },
    computed: {
      batchDetails: function() {
        if (!this.batch) return []
        return Object.entries(this.batch)
          .filter(([_, value]) => value !== null && value !== undefined)
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
      },
    },
    methods: {
      loadBatchDetails: function() {
        this.batchId = this.$route.query.id
        this.type = this.$route.query.type
        if (!this.batchId || !this.type) return
        this.batch = null  
        this.loading = true
        if (this.type === 'history') {
          BatchService.getHistoricBatchById(this.batchId).then(res => {
            this.batch = res
          }).finally(() => {
            this.loading = false
          })
        } else if (this.type === 'runtime') {
          BatchService.getBatchStatistics({ batchId: this.batchId }).then(res => {
            this.batch = res[0]
          }).finally(() => {
            this.loading = false
          })
        }
      },
      setBatchSuspensionState: function() {
        var params = { suspended: !this.batch.suspended }
        BatchService.setBatchSuspensionState(this.batchId, params).then(() => {
          this.batch.suspended = !this.batch.suspended
        })
      },
      removeBatch: function() {
        if (this.type === 'history') {
          BatchService.deleteHistoricBatch(this.batchId).then(() => {
            this.$emit('batch-deleted', { id: this.batchId, type: this.type })
            this.$router.replace({ query: {} })
          })
        } else if (this.type === 'runtime') {
          var params = { cascade: true }
          BatchService.deleteBatch(this.batchId, params).then(() => {
            this.$emit('batch-deleted', { id: this.batchId, type: this.type })
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