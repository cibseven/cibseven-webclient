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
  <ContentBlock :title="$t('batches.historicBatches')">
    <div class="overflow-auto p-0" style="max-height: 35vh" @scroll="showMore">
      <FlowTable v-if="historicBatches.length > 0" striped thead-class="sticky-header" :items="historicBatches" primary-key="id"
        :fields="[
          { label: 'batches.id', key: 'id', class: 'col-5', tdClass: 'p-0' },
          { label: 'batches.type', key: 'type', class: 'col-3', tdClass: 'p-1' },
          { label: 'batches.startTime', key: 'startTime', class: 'col-2', tdClass: 'p-1' },
          { label: 'batches.endTime', key: 'endTime', class: 'col-2', tdClass: 'p-1' },
        ]"
        sort-by="endTime" sort-desc @click="loadBatchDetails($event)">
        <template v-slot:cell(id)="table">
          <div
            class="p-0 m-0 h-100 w-100 d-flex align-items-center"
            :class="batchIsSelected(table.item.id) ? 'border-start border-4 border-primary' : ''">
            <div class="p-1 text-truncate">
              {{ table.item.id }}
            </div>
          </div>
        </template>
        <template v-slot:cell(startTime)="table">
          <div :title="formatDateForTooltips(table.item.startTime)">{{ formatDate(table.item.startTime) }}</div>
        </template>
        <template v-slot:cell(endTime)="table">
          <div :title="formatDateForTooltips(table.item.endTime)">{{ formatDate(table.item.endTime) }}</div>
        </template>
      </FlowTable>
      <div class="mb-3 text-center w-100" v-if="loading">
        <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('batches.loading') }}
      </div>
      <div class="mb-3 text-center w-100" v-if="!loading && historicBatches.length === 0">
        {{ $t('admin.noResults') }}
      </div>
    </div>
  </ContentBlock>
</template>

<script>
import { formatDate, formatDateForTooltips } from '@/utils/dates.js'
import { mapGetters, mapActions } from 'vuex'
import { debounce } from '@/utils/debounce.js'
import { BWaitingBox, FlowTable, ContentBlock } from '@cib/common-frontend'

export default {
  name: 'HistoricBatches',
  components: { FlowTable, BWaitingBox, ContentBlock },
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
  watch: {
    runtimeBatchesCount(newCount, oldCount) {
      // Only refresh if the count of runtime batches has decreased
      if (oldCount > 0 && newCount < oldCount) {
        this.refreshForNewBatches()
      }
    }
  },
  computed: {
    ...mapGetters(['historicBatches', 'runtimeBatches']),
    runtimeBatchesCount() {
      return this.runtimeBatches.length
    }
  },
  methods: {
    ...mapActions(['loadHistoricBatches', 'prependNewHistoricBatches']),
    formatDate,
    formatDateForTooltips,
    fetchHistoricBatches: function (showMore = false) {
      this.loading = true
      this.debouncedFetch(showMore)
    },
    debouncedFetch: debounce(500, function (showMore) {
      const params = {
        completed: true,
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
    refreshForNewBatches: function() {
      const params = {
        completed: true,
        sortBy: 'endTime',
        sortOrder: 'desc',
        firstResult: 0,
        maxResults: this.maxResults
      }

      this.prependNewHistoricBatches(params).then(newBatches => {
        if (newBatches.length > 0) {
          // Adjust firstResult to keep correct pagination
          this.firstResult += newBatches.length
        }
      })
    },
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
    batchIsSelected: function(id) {
      return this.$route.query.id === id && this.$route.query.type === 'history'
    }
  }
}
</script>
