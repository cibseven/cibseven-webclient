<template>
    <FlowTable if="jobDefinitions.length" striped thead-class="sticky-header" @click="showJobDefinition($event)" 
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
        <div :title="activityMap[table.item.activityId]" class="text-truncate">
          {{ activityMap[table.item.activityId] }}
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

    <b-modal ref="changeJobStateModal" :title="stateActionKey(selectedJobDefinition)" size="lg">
      <div class="container-fluid pt-2">
        <p>{{ getStateMsg }}</p>
        <b-form-group>
          <b-form-checkbox v-model="includeExistingJob">
            {{ $t('process-instance.jobDefinitions.includeExisting') }}
          </b-form-checkbox>
          <label class="fw-medium mt-2 mb-1">{{ $t('process-instance.jobDefinitions.execute') }}</label>
          <div class="form-check" v-for="option in executionOptions" :key="option">
            <input class="form-check-input" type="radio" :id="option" :value="option" v-model="executionOption" />
            <label class="form-check-label" :for="option">{{ $t('process-instance.jobDefinitions.' + option) }}</label>
          </div>
          <div v-if="executionOption === 'delayed'" class="row">
            <label class="fw-medium mt-2 mb-1">{{ $t('process-instance.jobDefinitions.scheduleAt') }}</label>
            <div class="col-6">
              <b-form-datepicker v-model="scheduledAt.date" size="sm" :date-disabled-fn="isInThePast" input-class="text-start">
                  <template v-slot:prepend>
                      <span class="input-group-text" aria-hidden="true">
                          <i class="mdi mdi-calendar-outline"></i>
                      </span>
                  </template>
              </b-form-datepicker>
            </div>
            <div class="col-6">              
              <b-form-timepicker v-model="scheduledAt.time" size="sm" input-class="text-start" no-close-button :label-no-time-selected="$t('cib-timepicker.noDate')"
                  reset-button reset-value="23:59:00" class="flex-fill" :label-reset-button="$t('cib-timepicker.reset')" :locale="currentLanguage()">
                  <template v-slot:prepend>
                      <span class="input-group-text" aria-hidden="true"> 
                          <i class="mdi mdi-clock-outline"></i>
                      </span>
                  </template>
              </b-form-timepicker>
            </div>
          </div>
        </b-form-group>
      </div>
      <template v-slot:modal-footer>
        <b-button @click="$refs.changeJobStateModal.hide()" variant="link">{{ $t('confirm.close') }}</b-button>
        <b-button @click="changeJobDefinitionState()">{{ $t('confirm.ok') }}</b-button>
      </template>
    </b-modal>

    <b-modal ref="overrideJobPriorityModal" :title="$t('process-instance.jobDefinitions.changeJobPriority')" size="md">
      <div class="container-fluid pt-2">
        <p class="mb-3">{{ $t('process-instance.jobDefinitions.overridingMsg') }}</p>
        <div v-if="selectedJobDefinition && selectedJobDefinition.overridingJobPriority" class="mb-2">
          <label class="fw-medium mt-2 mb-1">{{ $t('process-instance.jobDefinitions.execute') }}</label>
          <div class="form-check" v-for="option in overridingOptions" :key="option">
            <input class="form-check-input" type="radio" :id="option" :value="option" v-model="overridingOption" />
            <label class="form-check-label" :for="option">{{ $t('process-instance.jobDefinitions.' + option) }}</label>
          </div>
        </div>
        <div v-if="overridingOption === 'set'">
          <b-form-group>
            <label class="fw-semibold">{{ $t('process-instance.jobDefinitions.jobPriority') }}</label>
            <b-form-input v-model="priority"></b-form-input>
          </b-form-group>
          <b-form-group>
            <b-form-checkbox v-model="includeExistingJob">
              {{ $t('process-instance.jobDefinitions.includeExisting') }}
            </b-form-checkbox>
          </b-form-group>
        </div>
      </div>
      <template v-slot:modal-footer>
        <b-button @click="$refs.overrideJobPriorityModal.hide()" variant="link">{{ $t('confirm.close') }}</b-button>
        <b-button @click="overrideJobPriority()">{{ $t('confirm.ok') }}</b-button>
      </template>
    </b-modal>
</template>

<script>
import moment from 'moment'
import FlowTable from '@/components/common-components/FlowTable.vue'
import { JobDefinitionService } from '@/services.js'

export default {
  name: 'JobsDefinitionsTable',
  components: { FlowTable },
  inject: ['currentLanguage'],
  props: {
    processId: String,
    activityMap: Object
  },
  emits: ['highlight-activity'],
  data: function() {
    const now = new Date()
    now.setMinutes(now.getMinutes() + 5)
    const hours = now.getHours().toString().padStart(2, '0')
    const minutes = now.getMinutes().toString().padStart(2, '0')

    return {
      selectedJobDefinition: null,
      includeExistingJob: true,
      executionOption: 'immediately',
      executionOptions: ['immediately','delayed'],
      scheduledAt: { date: new Date(), time: `${hours}:${minutes}` },
      priority: null,
      overridingOption: 'set',
      overridingOptions: ['clear','set']
    }
  },
  watch: {
    'processId': function() {
      this.getJobDefinitions()
    }
  },
  computed: {
    jobDefinitions: function() {
      return this.$store.getters['jobDefinition/getJobDefinitions']
    },
    getStateMsg: function() {
      if (!this.selectedJobDefinition) return null
      return this.selectedJobDefinition.suspended
        ? this.$t('process-instance.jobDefinitions.suspendMsg')
        : this.$t('process-instance.jobDefinitions.activateMsg')
    }
  },
  created: function() {
    this.getJobDefinitions()
  },
  methods: {
    getJobDefinitions: function() {
      this.$store.dispatch('jobDefinition/getJobDefinitions', {
        processDefinitionId: this.processId
      })
    },
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
      this.selectedJobDefinition = item
      this.$refs.changeJobStateModal.show()
    },
    changeJobDefinitionState: function() {
      var executionDate = null
      if (this.executionOption === 'delayed') {
        executionDate = this.formatScheduledAt()
      }
      var data = {
        suspended: !this.selectedJobDefinition.suspended,
        includeJobs: this.includeExistingJob,
        executionDate: executionDate
      }
      this.$store.dispatch('jobDefinition/suspendJobDefinition', {
        jobDefinitionId: this.selectedJobDefinition.id,
        params: data,
        fetchParams: { processDefinitionId: this.processId }
      })
      this.$refs.changeJobStateModal.hide()
    },
    openChangeJobPriorityModal: function(item) {
      this.selectedJobDefinition = item
      this.overridingOption = 'set'
      this.priority = item.overridingJobPriority
      this.$refs.overrideJobPriorityModal.show()
    },
    overrideJobPriority: function() {
      let data = {}
      if (this.overridingOption !== 'clear') {
        data = {
          includeJobs: this.includeExistingJob,
          priority: this.priority
        }
      }
      this.$store.dispatch('jobDefinition/overridePriority', {
        jobDefinitionId: this.selectedJobDefinition.id,
        params: data,
        fetchParams: { processDefinitionId: this.processId }
      })
      this.$refs.overrideJobPriorityModal.hide()
    },
    isInThePast: function(ymd, date) {
      return date < moment().startOf('day')
    },
    formatScheduledAt: function() {
      const date = moment(this.scheduledAt.date)
      const datePart = date.format('YYYY-MM-DD')
      const timePart = this.scheduledAt.time + ':00.000'
      return moment.utc(`${datePart}T${timePart}Z`).format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
    }
  }
}
</script>
