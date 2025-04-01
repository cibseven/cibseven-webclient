<template>
    <FlowTable v-if="jobDefinitions.length" striped thead-class="sticky-header" @click="showJobDefinition($event)" 
      :items="jobDefinitions" primary-key="id" prefix="process-instance.jobDefinitions." sort-by="label" 
      :sort-desc="true" :fields="[
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
        <div :title="table.item.activityId" class="text-truncate">
          {{ $store.state.activity.processActivities[table.item.activityId] }}
        </div>
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
    <div v-else>
      <p class="text-center p-4">{{ $t('process-instance.noJobDefinitions') }}</p>
    </div>

    <JobDefinitionStateModal ref="changeJobStateModal"></JobDefinitionStateModal>
    <JobDefinitionPriorityModal ref="overrideJobPriorityModal"></JobDefinitionPriorityModal>
    
</template>

<script>
import FlowTable from '@/components/common-components/FlowTable.vue'
import JobDefinitionStateModal from '@/components/process/JobDefinitionStateModal.vue'
import JobDefinitionPriorityModal from '@/components/process/JobDefinitionPriorityModal.vue'

export default {
  name: 'JobsDefinitionsTable',
  components: { FlowTable, JobDefinitionStateModal, JobDefinitionPriorityModal },
  props: {
    processId: String
  },
  emits: ['highlight-activity'],
  computed: {
    jobDefinitions: function() {
      return this.$store.getters['jobDefinition/getJobDefinitions']
    }
  },
  methods: {
    showJobDefinition: function(jobDefinition) {
      this.$emit('highlight-activity', jobDefinition)
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
