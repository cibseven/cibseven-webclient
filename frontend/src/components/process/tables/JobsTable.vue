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
  <div class="overflow-auto bg-white container-fluid g-0 h-100">
    <div v-if="loading">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
    <FlowTable v-else-if="jobs && jobs.length > 0" resizable striped thead-class="sticky-header" :items="jobs" primary-key="id" prefix="process-instance.jobs."
      sort-by="label" :sort-desc="true" :fields="[
      { label: 'id', key: 'id', class: 'col-2', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' },
      { label: 'dueDate', key: 'dueDate', class: 'col-2', thClass: 'border-end', tdClass: 'position-relative py-1 border-end border-top-0' },
      { label: 'createTime', key: 'createTime', class: 'col-2', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' },
      { label: 'retries', key: 'retries', class: 'col-1', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'activity', key: 'activityId', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'failedActivity', key: 'failedActivityId', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'actions', key: 'actions', class: 'col-1', sortable: false, tdClass: 'py-1 border-top-0' }]">
      <template v-slot:cell(id)="table">
        <span :title="table.item.id" class="text-truncate">{{ table.item.id }}</span>
      </template>
      <template v-slot:cell(dueDate)="table">
        <span :title="formatDate(table.item.dueDate)" class="text-truncate">{{ formatDate(table.item.dueDate) }}</span>
      </template>
      <template v-slot:cell(createTime)="table">
        <span :title="formatDate(table.item.createTime)" class="text-truncate">{{ formatDate(table.item.createTime) }}</span>
      </template>
      <template v-slot:cell(activityId)="table">
        <span :title="table.item.activityId" class="text-truncate">{{ $store.state.activity.processActivities[table.item.activityId] }}</span>
      </template>
      <template v-slot:cell(failedActivityId)="table">
        <span :title="table.item.failedActivityId" class="text-truncate">{{ $store.state.activity.processActivities[table.item.failedActivityId] }}</span>
      </template>
      <template v-slot:cell(actions)="table">
        <b-button :title="suspendedStatusText(table.item)" @click="setSuspendedJob(table.item, !table.item.suspended)"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px" :class="table.item.suspended ? 'mdi-play' : 'mdi-pause'"></b-button>
      </template>
    </FlowTable>
    <div v-else-if="!loading">
      <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
    </div>
    <SuccessAlert ref="success"> {{ $t('alert.successOperation') }}</SuccessAlert>
    <SuccessAlert ref="messageCopy"> {{ $t('process.copySuccess') }} </SuccessAlert>
  </div>
</template>

<script>
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { formatDate } from '@/utils/dates.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import { BWaitingBox } from 'cib-common-components'
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'JobsTable',
  components: { FlowTable, SuccessAlert, BWaitingBox },
  mixins: [copyToClipboardMixin],
  props: { 
    instance: Object,
    process: Object
  },
  computed: {
    ...mapGetters('job', ['jobs'])
  },
  data() {
    return {
      loading: true
    }
  },
  watch: {
    'instance.id': {
      handler(id) {
        if (id) {
          this.loadJobsData(id, true)
        }
      },
      immediate: true
    },
    'process.id': {
      handler(id) {
        if (id && !this.instance) {
          this.loadJobsData(id, false)
        }
      },
      immediate: true
    }
  },
  methods: {
    ...mapActions('job', ['loadJobsByProcessInstance', 'loadJobsByProcessDefinition', 'setSuspended']),
    formatDate,
    async loadJobsData(id, isInstance = true) {
      this.loading = true
      try {
        if (isInstance) {
          await this.loadJobsByProcessInstance(id)
        } else {
          await this.loadJobsByProcessDefinition(id)
        }
      } finally {
        this.loading = false
      }
    },
    setSuspendedJob(job, suspended) {
      this.setSuspended({ jobId: job.id, suspended }).then(() => {
        this.$refs.success.show()
      })
    },
    suspendedStatusText(job) {
      return job.suspended ? this.$t('process-instance.jobs.resume') : this.$t('process-instance.jobs.suspend')
    }
  }
}
</script>
