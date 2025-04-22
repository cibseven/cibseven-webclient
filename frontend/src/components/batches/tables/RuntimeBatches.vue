<template>
  <div class="bg-white shadow-sm border rounded p-3 mb-4">
    <h4>{{ $t('batches.inProgressBatches') }}</h4>
    <hr>
    <div class="overflow-auto" style="max-height: 35vh">
        <FlowTable v-if="batches && batches.length > 0 && !loading" striped thead-class="sticky-header" :items="batches" primary-key="id" prefix="batches."
          :fields="[
              { label: 'id', key: 'id', class: 'col-2', tdClass: 'border-end p-0' },
              { label: 'type', key: 'type', class: 'col-2', tdClass: 'border-end p-1' },
              { label: 'user', key: 'createUserId', class: 'col-2', tdClass: 'border-end p-1' },
              { label: 'startTime', key: 'startTime', class: 'col-2', tdClass: 'border-end p-1' },
              { label: 'failedJobs', key: 'failedJobs', class: 'col-2', tdClass: 'border-end p-1' },
              { label: 'progress', key: 'progress', class: 'col-2', tdClass: 'p-1' }
          ]"
          @click="loadBatchDetails($event)">
          <template v-slot:cell(id)="table">
            <div class="p-1 text-truncate" :class="batchIsSelected(table.item.id) ? 'border-start border-4 border-primary' : ''">
              {{ table.item.id }}
            </div>
          </template>
          <template v-slot:cell(startTime)="table">
            <div>{{ formatDate(table.item.startTime) }}</div>
          </template>
          <template v-slot:cell(progress)="table">
            <div class="w-100">
              <b-progress :value="table.item.completed" :max="table.item.totalJobs" :variant="getBatchVariant(table.item)" show-progress animated></b-progress>
            </div>
          </template>
        </FlowTable>
        <div class="mb-3 text-center w-100" v-if="loading">
          <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('batches.loading') }}
        </div>
        <div class="mb-3 text-center w-100" v-if="!loading && batches.length === 0">
          {{ $t('admin.noResults') }}
        </div>
    </div>
  </div>
</template>

<script>
  import moment from 'moment'
  import FlowTable from '@/components/common-components/FlowTable.vue'
  import { mapGetters, mapActions } from 'vuex'
  import { BWaitingBox } from 'cib-common-components'

  export default {
    name: 'RuntimeBatches',
    components: { FlowTable, BWaitingBox },
    inject: ['currentLanguage'],
    data: function () {
      return {
        loading: true,
        intervalMs: 5000,
        batchesInterval: null
      }
    },
    mounted: function() {
      this.loading = true
      this.loadBatches()
    },
    computed: {
      ...mapGetters(['runtimeBatches']),
      batches: function() {
        return this.runtimeBatches.map(batch => {
          const total = batch.totalJobs || 0
          const remaining = batch.remainingJobs || 0
          const completed = total - remaining
          return { ...batch, completed }
        })
      }
    },
    methods: {
      ...mapActions(['getRuntimeBatches']),
      loadBatches: function() {
        this.getRuntimeBatches().then(() => {
          if (this.runtimeBatches.length > 0 && !this.batchesInterval) {
            this.batchesInterval = setInterval(() => {
              this.loadBatches()
            }, this.intervalMs)
          }
          if (this.runtimeBatches.length === 0 && this.batchesInterval) {
            clearInterval(this.batchesInterval)
            this.batchesInterval = null
          }
          this.loading = false
        })
      },
      loadBatchDetails: function(batch) {
        this.$router.replace({
          query: {
            id: batch.id,
            type: 'runtime'
          }
        })
      },
      getBatchVariant: function(batch) {
        if (batch.suspended) return 'warning'
        if (batch.completed >= batch.totalJobs) return 'success'
        return ''
      },
      formatDate: function(date) {
        return moment(date).format('DD/MM/YYYY HH:mm:ss')
      },
      batchIsSelected: function(id) {
        return this.$route.query.id === id && this.$route.query.type === 'runtime'
      }
    }
  }
</script>
