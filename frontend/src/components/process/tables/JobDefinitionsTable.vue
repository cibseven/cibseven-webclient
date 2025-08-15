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
    <FlowTable v-else-if="jobDefinitions.length > 0" striped thead-class="sticky-header" 
      :items="jobDefinitions" primary-key="id" prefix="process-instance.jobDefinitions." sort-by="suspended" :fields="[
      { label: 'state', key: 'suspended', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'activity', key: 'activityId', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'type', key: 'jobType', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'configuration', key: 'jobConfiguration', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'overridingJobPriority', key: 'overridingJobPriority', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'actions', key: 'actions', class: 'col-2', sortable: false, tdClass: 'py-1 border-top-0' }
      ]">
      <template v-slot:cell(suspended)="table">
        <div :title="getStateLabel(table.item)" class="text-truncate">
            {{ getStateLabel(table.item) }}
        </div>
      </template>
      <template v-slot:cell(activityId)="table">
        <CopyableActionButton
          :display-value="getActivityDisplayName(table.item.activityId)"
          :copy-value="table.item.activityId"
          :title="getActivityDisplayName(table.item.activityId)"
          @click="highlightElement(table.item.activityId)"
          @copy="copyValueToClipboard"
        />
      </template>
      <template v-slot:cell(actions)="table">
        <b-button :title="stateActionKey(table.item)"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px"
          :class="table.item.suspended ? 'mdi-play' : 'mdi-pause'"
          @click.stop="openChangeStateJobModal(table.item)">
        </b-button>
        <b-button :title="$t('process-instance.jobDefinitions.changeJobPriority')"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-cog"
          @click.stop="openChangeJobPriorityModal(table.item)">
        </b-button>
      </template>      
    </FlowTable>
    <div v-else-if="!loading">
      <p class="text-center p-4">{{ $t('process-instance.noJobDefinitions') }}</p>
    </div>

    <JobDefinitionStateModal ref="changeJobStateModal"></JobDefinitionStateModal>
    <JobDefinitionPriorityModal ref="overrideJobPriorityModal"></JobDefinitionPriorityModal>
    <SuccessAlert ref="messageCopy" style="z-index: 9999">{{ $t('process.copySuccess') }}</SuccessAlert>
  </div>
</template>

<script>
import FlowTable from '@/components/common-components/FlowTable.vue'
import CopyableActionButton from '@/components/common-components/CopyableActionButton.vue'
import JobDefinitionStateModal from '@/components/process/modals/JobDefinitionStateModal.vue'
import JobDefinitionPriorityModal from '@/components/process/modals/JobDefinitionPriorityModal.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import { BWaitingBox } from 'cib-common-components'
import { mapActions, mapGetters } from 'vuex'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'

export default {
  name: 'JobsDefinitionsTable',
  components: { FlowTable, CopyableActionButton, JobDefinitionStateModal, JobDefinitionPriorityModal, SuccessAlert, BWaitingBox },
  mixins: [copyToClipboardMixin],
  props: {
    processId: String
  },
  computed: {
    ...mapGetters('job', ['jobDefinitions'])
  },
  data() {
    return {
      loading: true
    }
  },
  watch: {
    processId: {
      handler(id) {
        if (id) {
          this.loadJobDefinitionsData(id)
        }
      },
      immediate: true
    }
  },
  methods: {
    ...mapActions(['setHighlightedElement']),
    ...mapActions('job', ['loadJobDefinitionsByProcessDefinition']),
    
    async loadJobDefinitionsData(processId) {
      this.loading = true
      try {
        await this.loadJobDefinitionsByProcessDefinition(processId)
      } finally {
        this.loading = false
      }
    },
    getActivityDisplayName(activityId) {
      if (!activityId) return ''
      // Try to get the activity name from the store, fallback to activityId if not found
      return this.$store.state.activity.processActivities[activityId] || activityId
    },
    highlightElement: function(activityId) {
      if (activityId) {
        this.setHighlightedElement(activityId)
      }
    },
    getStateLabel: function(item) {
      return item.suspended
        ? this.$t('process-instance.jobDefinitions.suspended')
        : this.$t('process-instance.jobDefinitions.active')
    },
    stateActionKey: function(item) {
      if (!item) return null
      return item.suspended
        ? this.$t('process-instance.jobDefinitions.suspendJob')
        : this.$t('process-instance.jobDefinitions.activateJob')
    },
    openChangeStateJobModal: function(item) {
      this.$refs.changeJobStateModal.show(item)
    },
    openChangeJobPriorityModal: function(item) {
      this.$refs.overrideJobPriorityModal.show(item)
    }
  }
}
</script>
