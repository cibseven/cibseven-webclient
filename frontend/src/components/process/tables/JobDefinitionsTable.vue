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
      { label: 'state', key: 'suspended', class: 'col-2', tdClass: 'py-1' },
      { label: 'activity', key: 'activityId', class: 'col-2', tdClass: 'py-1' },
      { label: 'type', key: 'jobType', class: 'col-2', tdClass: 'py-1' },
      { label: 'configuration', key: 'jobConfiguration', class: 'col-2', tdClass: 'py-1' },
      { label: 'overridingJobPriority', key: 'overridingJobPriority', class: 'col-2', tdClass: 'py-1' },
      { label: 'actions', key: 'actions', class: 'col-2', sortable: false, tdClass: 'py-0' }
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
        <CellActionButton v-if="hasUpdatePermission" :title="stateActionKey(table.item)"
          :icon="table.item.suspended ? 'mdi-play' : 'mdi-pause'"
          @click="openChangeStateJobModal(table.item)">
        </CellActionButton>
        <CellActionButton :title="$t('process-instance.jobDefinitions.changeJobPriority')"
          icon="mdi-cog"
          @click="openChangeJobPriorityModal(table.item)">
        </CellActionButton>
        <component :is="JobDefinitionsTableActionsPlugin" v-if="JobDefinitionsTableActionsPlugin" :table-item="table.item"></component>
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
import { FlowTable, CopyableActionButton, SuccessAlert,BWaitingBox } from '@cib/common-frontend'
import JobDefinitionStateModal from '@/components/process/modals/JobDefinitionStateModal.vue'
import JobDefinitionPriorityModal from '@/components/process/modals/JobDefinitionPriorityModal.vue'
import { mapActions, mapGetters } from 'vuex'
import { permissionsMixin } from '@/permissions.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import CellActionButton from '@/components/common-components/CellActionButton.vue'

export default {
  name: 'JobDefinitionsTable',
  components: { FlowTable, CopyableActionButton, JobDefinitionStateModal, JobDefinitionPriorityModal, SuccessAlert, BWaitingBox, CellActionButton },
  mixins: [copyToClipboardMixin, permissionsMixin],
  props: {
    process: Object,
  },
  computed: {
    ...mapGetters('job', ['jobDefinitions']),
    processId: function() {
      return this.process?.id
    },
    JobDefinitionsTableActionsPlugin: function() {
      return this.$options.components && this.$options.components.JobDefinitionsTableActionsPlugin
        ? this.$options.components.JobDefinitionsTableActionsPlugin
        : null
    },
    hasUpdatePermission: function() {
      return this.processByPermissions(this.$root.config.permissions.updateProcessDefinition, this.process) &&
        (this.processByPermissions(this.$root.config.permissions.updateInstanceProcessDefinition, this.process) ||
         this.processByPermissions(this.$root.config.permissions.updateProcessInstance, this.process) )
    }
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
        ? this.$t('process-instance.jobDefinitions.activateJob')
        : this.$t('process-instance.jobDefinitions.suspendJob')
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
