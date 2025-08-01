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
  <div
    class="overflow-auto bg-white position-absolute container-fluid g-0"
    style="top: 0; bottom: 0"
  >
    <div v-if="loading">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
    <FlowTable
      v-else-if="externalTasks.length > 0"
      striped
      thead-class="sticky-header"
      :items="externalTasks"
      primary-key="id"
      prefix="process-instance.externalTasks."
      :fields="tableFields"
    >
      <template v-slot:cell(id)="table">
        <CopyableActionButton
          :display-value="table.item.id"
          :title="table.item.id"
          @copy="copyValueToClipboard"
          :clickable="false"
        />
      </template>
      <template v-slot:cell(activityId)="table">
        <CopyableActionButton
          :display-value="$store.state.activity.processActivities[table.item.activityId] || table.item.activityId"
          :copy-value="table.item.activityId"
          :title="table.item.activityId"
          @click="setHighlightedElement(table.item.activityId)"
          @copy="copyValueToClipboard"
        />
      </template>
      <template v-slot:cell(lockExpirationTime)="table">
        <div :title="formatDate(table.item.lockExpirationTime)" class="text-truncate">{{ formatDate(table.item.lockExpirationTime) }}</div>
      </template>
    </FlowTable>
    <div v-else-if="!loading">
      <p class="text-center p-4">
        {{ $t('process-instance.externalTasks.noExternalTasks') }}
      </p>
    </div>
    <SuccessAlert ref="messageCopy" style="z-index: 9999">
      {{ $t('process.copySuccess') }}
    </SuccessAlert>
  </div>
</template>

<script>
import { moment } from '@/globals.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import CopyableActionButton from '@/components/common-components/CopyableActionButton.vue'
import { BWaitingBox } from 'cib-common-components'
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'ExternalTasksTable',
  components: { FlowTable, SuccessAlert, CopyableActionButton, BWaitingBox },
  mixins: [copyToClipboardMixin],
  props: {
    instance: Object
  },
  data() {
    return {
      loading: true
    }
  },
  computed: {
    ...mapGetters('externalTasks', ['externalTasks']),
    tableFields() {
      return [
        {
          label: 'id',
          key: 'id',
          class: 'col-2',
          sortable: true,
          tdClass: 'position-relative border-end border-top-0',
        },
        {
          label: 'activityId',
          key: 'activityId',
          class: 'col-2',
          sortable: false,
          tdClass: 'position-relative border-end border-top-0',
        },
        {
          label: 'retries',
          key: 'retries',
          class: 'col-1',
          sortable: true,
          tdClass: 'border-end border-top-0',
        },
        {
          label: 'workerId',
          key: 'workerId',
          class: 'col-2',
          sortable: true,
          tdClass: 'border-end border-top-0',
        },
        {
          label: 'lockExpirationTime',
          key: 'lockExpirationTime',
          class: 'col-2',
          sortable: true,
          tdClass: 'border-end border-top-0'
        },
        {
          label: 'topicName',
          key: 'topicName',
          class: 'col-2',
          sortable: true,
          tdClass: 'border-end border-top-0',
        },
        {
          label: 'priority',
          key: 'priority',
          class: 'col-1',
          sortable: true,
          tdClass: 'border-top-0',
        },
      ]
    },
  },
  watch: {
    'instance.id': {
      handler(id) {
        if (id) {
          this.handleExternalTask(id)
        }
      },
      immediate: true
    }
  },
  methods: {
    ...mapActions(['setHighlightedElement']),
    ...mapActions('externalTasks', ['loadExternalTasks']),
    formatDate(date) {
      return date ? moment(date).format('DD/MM/YYYY HH:mm:ss') : '-'
    },
    async handleExternalTask(id) {
      this.loading = true
      try {
        const params = {
          processInstanceId: id,
          sortBy: 'taskPriority',
          sortOrder: 'desc'
        }
        await this.loadExternalTasks(params)
      } finally {
        this.loading = false
      }
    },
  },
}
</script>
