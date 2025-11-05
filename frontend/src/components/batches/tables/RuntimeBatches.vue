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
  <ContentBlock :title="$t('batches.inProgressBatches')">
    <div class="overflow-auto p-0" style="max-height: 35vh">
      <FlowTable v-if="batches && batches.length > 0 && !loading" striped thead-class="sticky-header" :items="batches" primary-key="id" prefix="batches."
        :fields="[
            { label: 'id', key: 'id', class: 'col-2', tdClass: 'p-0' },
            { label: 'type', key: 'type', class: 'col-2', tdClass: 'p-1' },
            { label: 'user', key: 'createUserId', class: 'col-2', tdClass: 'p-1' },
            { label: 'startTime', key: 'startTime', class: 'col-2', tdClass: 'p-1' },
            { label: 'failedJobs', key: 'failedJobs', class: 'col-2', tdClass: 'p-1' },
            { label: 'progress', key: 'progress', class: 'col-2', tdClass: 'p-1' }
        ]"
        @click="loadBatchDetails($event)" sort-by="startTime" sort-desc>
        <template v-slot:cell(id)="table">
          <div class="p-1 text-truncate" :class="batchIsSelected(table.item.id) ? 'border-start border-4 border-primary' : ''">
            {{ table.item.id }}
          </div>
        </template>
        <template v-slot:cell(startTime)="table">
          <div :title="formatDateForTooltips(table.item.startTime)">{{ formatDate(table.item.startTime) }}</div>
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
  </ContentBlock>
</template>

<script>
import { formatDate, formatDateForTooltips } from '@/utils/dates.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import { mapGetters, mapActions } from 'vuex'
import { BWaitingBox } from 'cib-common-components'
import ContentBlock from '@/components/common-components/ContentBlock.vue'

export default {
  name: 'RuntimeBatches',
  components: { FlowTable, BWaitingBox, ContentBlock },
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
  unmounted: function() {
    if (this.batchesInterval) {
      clearInterval(this.batchesInterval)
      this.batchesInterval = null
    }
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
    formatDate,
    formatDateForTooltips,
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
    batchIsSelected: function(id) {
      return this.$route.query.id === id && this.$route.query.type === 'runtime'
    }
  }
}
</script>
