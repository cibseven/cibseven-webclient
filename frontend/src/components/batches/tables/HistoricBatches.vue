<template>
  <div class="bg-white shadow-sm p-3 border rounded">
    <h5>{{ $t('batches.historicBatches') }}</h5>
    <hr>
    <div class="overflow-auto" style="max-height: 35vh" @scroll="showMore">
      <FlowTable striped thead-class="sticky-header" :items="historicBatches" primary-key="id" prefix="batches."
        :fields="[
          { label: 'id', key: 'id', class: 'col-5', tdClass: 'border-end p-0' },
          { label: 'type', key: 'type', class: 'col-3', tdClass: 'border-end p-1' },
          { label: 'startTime', key: 'startTime', class: 'col-2', tdClass: 'border-end p-1' },
          { label: 'endTime', key: 'endTime', class: 'col-2', tdClass: 'p-1' },
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
        <template v-slot:cell(endTime)="table">
          <div>{{ formatDate(table.item.endTime) }}</div>
        </template>
      </FlowTable>
      <div class="mb-3 text-center w-100" v-if="loading">
        <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('batches.loading') }}
      </div>
      <div class="mb-3 text-center w-100" v-if="!loading && historicBatches.length === 0">
        {{ $t('admin.noResults') }}
      </div>
    </div>
  </div>
</template>

<script>
  import moment from 'moment'
  import FlowTable from '@/components/common-components/FlowTable.vue'
  import { mapGetters, mapActions } from 'vuex'
  import { debounce } from '@/utils/debounce.js'
  import { BWaitingBox } from 'cib-common-components'

  export default {
    name: 'HistoricBatches',
    components: { FlowTable, BWaitingBox },
    inject: ['currentLanguage'],
    data() {
      return {
        loading: true,
        firstResult: 0,
        maxResults: 40,
        hasMore: true
      }
    },
    mounted: function() {
      this.fetchHistoricBatches()
    },
    computed: {
      ...mapGetters(['historicBatches'])
    },
    methods: {
      ...mapActions(['loadHistoricBatches']),
      fetchHistoricBatches: debounce(500, function (showMore = false) {
        this.loading = true
        const params = {
          finished: true,
          sortBy: 'endTime',
          sortOrder: 'desc',
          firstResult: this.firstResult,
          maxResults: this.maxResults
        }
        this.loadHistoricBatches({ query: params, append: showMore }).then(res => {
          this.firstResult += res.length
          this.hasMore = res.length === this.maxResults
          this.loading = false
        })
      }),
      showMore: function(el) {
        if (this.loading || !this.hasMore) return
        const nearBottom = el.target.scrollTop + el.target.offsetHeight >= el.target.scrollHeight - 1
        if (nearBottom) {
          this.fetchHistoricBatches(true)
        }
      },
      loadBatchDetails: function(batch) {
        this.$router.replace({
          query: { id: batch.id, type: 'history' }
        })
      },
      formatDate: function(date) {
        return moment(date).format('DD/MM/YYYY HH:mm:ss')
      },
      batchIsSelected: function(id) {
        return this.$route.query.id === id && this.$route.query.type === 'history'
      }
    }
  }
</script>
