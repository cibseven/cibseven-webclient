<template>
  <b-modal ref="overrideJobPriorityModal" :title="$t('process-instance.jobDefinitions.changeJobPriority')" size="md">
    <div>
      <p class="mb-3">{{ $t('process-instance.jobDefinitions.overridingMsg') }}</p>
      <div v-if="selectedJobDefinition && selectedJobDefinition.overridingJobPriority !== null" class="mb-2">
        <label class="fw-medium mt-2 mb-1">{{ $t('process-instance.jobDefinitions.execute') }}</label>
        <div class="form-check" v-for="option in overridingOptions" :key="option">
          <input class="form-check-input" type="radio" :id="option" :value="option" v-model="overridingOption" />
          <label class="form-check-label" :for="option">{{ $t('process-instance.jobDefinitions.' + option) }}</label>
        </div>
      </div>
      <div v-if="overridingOption === 'set'">
        <b-form-group>
          <label class="fw-semibold">{{ $t('process-instance.jobDefinitions.jobPriority') }}</label>
          <b-form-input v-model="priority" type="number"></b-form-input>
          <span v-if="priorityError" class="text-danger">{{ $t('process-instance.jobDefinitions.invalidPriorityError') }}</span>
        </b-form-group>
        <b-form-group>
          <b-form-checkbox v-model="includeExistingJob">
            {{ $t('process-instance.jobDefinitions.includeExisting') }}
          </b-form-checkbox>
        </b-form-group>
      </div>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.overrideJobPriorityModal.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="overrideJobPriority()" variant="primary" :disabled="!canOverride">
        {{ overridingOption === 'set' ? $t('process-instance.jobDefinitions.overrideAction') : $t('process-instance.jobDefinitions.clearAction') }}
      </b-button>
    </template>
  </b-modal>
</template>

<script>
export default {
  name: 'JobDefinitionPriorityModal',
  data: function() {
    return {
      selectedJobDefinition: null,
      includeExistingJob: false,
      priority: null,
      overridingOption: 'set',
      overridingOptions: ['clear','set']
    }
  },
  computed: {
    getStateMsg: function() {
      if (!this.selectedJobDefinition) return null
      return this.selectedJobDefinition.suspended
        ? this.$t('process-instance.jobDefinitions.suspendMsg')
        : this.$t('process-instance.jobDefinitions.activateMsg')
    },
    canOverride: function() {
      if (this.overridingOption === 'clear') return true
      if (this.overridingOption === 'set') {
        return Number.isInteger(this.priority)
      }
      return false
    },
    priorityError: function() {
      if (this.overridingOption !== 'set') return false
      const parsed = Number(this.priority)
      return this.priority !== null && !Number.isInteger(parsed)
    }
  },
  methods: {
    show: function(selectedJobDefinition) {
      this.selectedJobDefinition = selectedJobDefinition
      this.overridingOption = 'set'
      this.priority = selectedJobDefinition.overridingJobPriority
      this.includeExistingJob = false
      this.$refs.overrideJobPriorityModal.show()
    },
    overrideJobPriority: function() {
      let data = {}
      if (this.overridingOption !== 'clear') {
        if (!this.canOverride) {
          return null
        }
        data = {
          includeJobs: this.includeExistingJob,
          priority: this.priority
        }
      }
      this.$store.dispatch('jobDefinition/overridePriority', {
        jobDefinitionId: this.selectedJobDefinition.id,
        params: data,
        fetchParams: { processDefinitionId: this.selectedJobDefinition.processDefinitionId }
      })
      this.$refs.overrideJobPriorityModal.hide()
    },
  }
}
</script>
