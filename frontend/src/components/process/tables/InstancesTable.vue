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
  <div>
    <FlowTable v-if="instances.length > 0 && !sorting" striped resizable thead-class="sticky-header" :items="instances" primary-key="id" prefix="process."
      :sort-by="currentSortBy" :sort-desc="currentSortDesc" external-sort :fields="[
      { label: 'state', key: 'state', class: 'col-1', thClass: 'border-end text-center', sortable: false, tdClass: 'justify-content-center text-center py-0 border-end border-top-0' },
      { label: 'businessKey', key: 'businessKey', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'startTime', key: 'startTime', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'endTime', key: 'endTime', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'instanceId', key: 'id', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'startUserId', key: 'startUserId', class: 'col-1', sortable: false, thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'actions', key: 'actions', class: 'col-2', sortable: false, tdClass: 'py-0 border-top-0' }]"
      @click="selectInstance($event)" @external-sort="handleSortChanged">
      <template v-slot:cell(state)="table">
        <span :title="getIconTitle(table.item.state)" class="mdi mdi-18px" :class="getIconState(table.item.state)"></span>
        <span :title="$t('process.instanceIncidents')" v-if="table.item.incidents.length > 0" class="mdi mdi-18px mdi-alert-outline text-warning"></span>
      </template>
      <template v-slot:cell(id)="table">
        <CopyableActionButton
          :displayValue="table.item.id"
          :title="$t('process.showInstance') + ':\n' + table.item.id"
          :to="`/seven/auth/process/${table.item.processDefinitionKey}/${table.item.processDefinitionVersion}/${table.item.id}`"
          @copy="copyValueToClipboard"
        />
      </template>
      <template v-slot:cell(businessKey)="table">
        <CopyableActionButton
          :display-value="table.item.businessKey"
          :clickable="false"
          @copy="copyValueToClipboard"
        />
      </template>
      <template v-slot:cell(startTime)="table">
        <span :title="formatDate(table.item.startTime)" class="text-truncate d-block">{{ formatDate(table.item.startTime) }}</span>
      </template>
      <template v-slot:cell(endTime)="table">
        <span :title="formatDate(table.item.endTime)" class="text-truncate d-block">{{ formatDate(table.item.endTime) }}</span>
      </template>
      <template v-slot:cell(actions)="table">
        <b-button v-if="table.item.state === 'ACTIVE'" @click.stop="confirmSuspend(table.item)"
        size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-pause-circle-outline" :title="$t('process.suspendInstance')"></b-button>
        <b-button v-else-if="table.item.state === 'SUSPENDED'" @click.stop="confirmActivate(table.item)"
        size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-play-circle-outline" :title="$t('process.activateInstance')"></b-button>
        <b-button @click="selectInstance(table.item)" size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-eye-outline" :title="$t('process.showInstance')"></b-button>
        <b-button v-if="['ACTIVE', 'SUSPENDED'].includes(table.item.state) && processByPermissions($root.config.permissions.deleteProcessInstance, table.item)"
        @click.stop="confirmStopInstance(table.item)"
        size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-stop-circle-outline" :title="$t('process.stopInstance')"></b-button>
        <b-button v-else-if="['COMPLETED', 'EXTERNALLY_TERMINATED'].includes(table.item.state) && processByPermissions($root.config.permissions.deleteProcessInstance, table.item)"
        @click.stop="confirmDeleteHistoryInstance(table.item)"
        size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-delete-outline" :title="$t('process.deleteHistoryInstance')"></b-button>
      </template>
    </FlowTable>
    <div v-if="loading" class="py-3 text-center w-100">
      <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
    </div>
    <div v-else-if="instances.length === 0">
      <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
    </div>
    <ConfirmActionOnProcessInstanceModal ref="confirm"></ConfirmActionOnProcessInstanceModal>
    <SuccessAlert top="0"  ref="success"> {{ $t('alert.successOperation') }}</SuccessAlert>
    <SuccessAlert ref="messageCopy"> {{ $t('process.copySuccess') }} </SuccessAlert>
  </div>
</template>

<script>
import { ProcessService, HistoryService } from '@/services.js'
import { permissionsMixin } from '@/permissions.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import CopyableActionButton from '@/components/common-components/CopyableActionButton.vue'
import { formatDate } from '@/utils/dates.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import ConfirmActionOnProcessInstanceModal from '@/components/process/modals/ConfirmActionOnProcessInstanceModal.vue'
import { BWaitingBox } from 'cib-common-components'
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'InstancesTable',
  components: { FlowTable, SuccessAlert, ConfirmActionOnProcessInstanceModal, BWaitingBox, CopyableActionButton },
  emits: ['instance-deleted', 'filter-instances'],
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: {
    process: Object,
    activityInstance: Object,
    sortDesc: Boolean,
    sortByDefaultKey: String,
    sorting: Boolean,
    tenantId: String,
    filter: {}
  },
  computed: {
    ...mapGetters('instances', ['instances']),
    ...mapGetters(['selectedActivityId']),
    currentActivityId() {
      return this.selectedActivityId || this.activityInstance?.id
    },
    canLoadMore() {
      return this.hasMoreData && !this.loading
    }
  },
  data() {
    return {
      loading: false,
      firstResult: 0,
      maxResults: this.$root?.config?.maxProcessesResults || 50,
      hasMoreData: true,
      sortingCriteria: [], // Store the sorting criteria for backend calls
      currentSortBy: null, // Track current sort field for FlowTable display
      currentSortDesc: false // Track current sort direction for FlowTable display
    }
  },
  watch: {
    'process.id': {
      handler() {
        if (this.process?.id) {
          this.resetPagination()
          this.loadInstancesData()
        }
      },
      immediate: true
    },
    filter: {
      handler() {
        this.resetPagination()
        this.loadInstancesData()
      },
      deep: true
    },
    currentActivityId() {
      if (this.process?.id) {
        this.resetPagination()
        this.loadInstancesData()
      }
    },
  },
  mounted() {
    this.initializeSortState()
  },
  methods: {
    ...mapActions('instances', ['loadInstances', 'resetInstances']),
    formatDate,
    resetPagination() {
      this.firstResult = 0
      this.hasMoreData = true
      this.resetInstances()
    },
    async loadInstancesData(showMore = false) {
      if (!this.process) return
      if (!showMore) {
        this.resetInstances()
      }
      this.loading = true
      try {
        const instances = await this.loadInstances({
          processId: this.process.id,
          activityId: this.currentActivityId,
          filter: this.filter,
          tenantId: this.tenantId,
          camundaHistoryLevel: this.$root.config.camundaHistoryLevel,
          firstResult: this.firstResult,
          maxResults: this.maxResults,
          sortingCriteria: this.sortingCriteria, // Pass sorting criteria to backend
          fetchIncidents: true, // Enable incident fetching for table display
          showMore
        })
        // Check if we got fewer instances than requested (indicates end of data)
        this.hasMoreData = instances && instances.length === this.maxResults
      } finally {
        this.loading = false
      }
    },
    showMore: function() {
      // Only load more if we think there's more data available
      if (!this.canLoadMore) {
        return
      }
      this.firstResult += this.maxResults
      this.loadInstancesData(true)
    },
    filterInstances: function(filter) {
      this.$emit('filter-instances', filter)
      // Filter prop will be updated by parent, which will trigger a reload
    },
    applySorting: function(sortingCriteria) {
      // Switching to multi-sort mode - clear FlowTable sort state
      this.currentSortBy = null
      this.currentSortDesc = false

      // Store sorting criteria and reload data
      this.sortingCriteria = sortingCriteria
      this.resetPagination()
      this.loadInstancesData()
    },
    handleSortChanged: function(sortEvent) {
      // Switching to single-column FlowTable sorting
      // Convert FlowTable sort event to backend sorting format
      if (sortEvent && sortEvent.sortBy) {
        // Update local sort state for FlowTable display
        this.currentSortBy = sortEvent.sortBy
        this.currentSortDesc = sortEvent.sortDesc

        // Convert frontend column keys to backend field names
        let sortBy = sortEvent.sortBy
        if (sortEvent.sortBy === 'id') {
          sortBy = 'instanceId'
        }
        this.sortingCriteria = [{
          field: sortBy,
          order: sortEvent.sortDesc ? 'desc' : 'asc'
        }]
      } else {
        // No sorting - use default
        this.currentSortBy = null
        this.currentSortDesc = false
        this.sortingCriteria = []
      }
      this.resetPagination()
      this.loadInstancesData()
    },
    selectInstance: function(instance) {
      this.$router.push({
        name: 'process',
        params: {
          processKey: instance.processDefinitionKey,
          versionIndex: instance.processDefinitionVersion,
          instanceId: instance.id,
        },
        query: this.$route.query
      })
    },
    // "Stop Instance" button
    confirmStopInstance: function(instance) {
      this.$refs.confirm.show({
        ok: this.stopInstance,
        instance: instance,
        message: this.$t('process-instance.confirm.stopInstance')
      })
    },
    stopInstance: function(instance) {
      ProcessService.stopInstance(instance.id).then(() => {
        this.$emit('instance-deleted')
        this.resetPagination()
        this.loadInstancesData() // Refresh local data
        this.$refs.success.show()
      })
    },
    // "Delete History Instance" button
    confirmDeleteHistoryInstance: function(instance) {
      this.$refs.confirm.show({
        ok: this.deleteHistoryInstance,
        instance: instance,
        message: this.$t('process-instance.confirm.deleteHistoryInstance'),
        okTitle: this.$t('confirm.delete'),
      })
    },
    deleteHistoryInstance: function(instance) {
      HistoryService.deleteProcessInstanceFromHistory(instance.id).then(() => {
        this.$emit('instance-deleted')
        this.resetPagination()
        this.loadInstancesData() // Refresh local data
        this.$refs.success.show()
      })
    },
    // "Suspend" button
    confirmSuspend: function(instance) {
      this.$refs.confirm.show({
        ok: this.suspendInstance,
        instance: instance,
        message: this.$t('process-instance.confirm.suspend'),
        okTitle: this.$t('process-instance.jobDefinitions.suspend'),
      })
    },
    suspendInstance: function(instance) {
      ProcessService.suspendInstance(instance.id, true).then(() => {
        instance.state = 'SUSPENDED'
        this.$refs.success.show()
      })
    },
    // "Activate" button
    confirmActivate: function(instance) {
      this.$refs.confirm.show({
        ok: this.activateInstance,
        instance:instance,
        message: this.$t('process-instance.confirm.activate'),
        okTitle: this.$t('process-instance.jobDefinitions.activate'),
      })
    },
    activateInstance: function(instance) {
      ProcessService.suspendInstance(instance.id, false).then(() => {
        instance.state = 'ACTIVE'
        this.$refs.success.show()
      })
    },
    getIconState: function(state) {
      switch(state) {
        case 'ACTIVE':
          return 'mdi-chevron-triple-right text-success'
        case 'SUSPENDED':
          return 'mdi-close-circle-outline'
      }
      return 'mdi-flag-triangle'
    },
    getIconTitle: function(state) {
      switch(state) {
        case 'ACTIVE':
          return this.$t('process.instanceRunning')
        case 'SUSPENDED':
          return this.$t('process.instanceIncidents')
      }
      return this.$t('process.instanceFinished')
    },
    initializeSortState() {
      // Initialize sort state from props if provided
      if (this.sortByDefaultKey) {
        this.currentSortBy = this.sortByDefaultKey
        this.currentSortDesc = this.sortDesc || false
      }
    }
  }
}
</script>
