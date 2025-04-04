<template>
    <b-modal ref="changeJobStateModal" :title="stateActionKey" size="lg">
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
        <b-button @click="changeJobDefinitionState()" variant="primary">{{ $t('confirm.ok') }}</b-button>
      </template>
    </b-modal>
</template>

<script>
    import moment from 'moment'

    export default {
        name: 'JobDefinitionStateModal',
        inject: ['currentLanguage'],
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
                scheduledAt: { date: new Date(), time: `${hours}:${minutes}` }
            }
        },
        computed: {
            getStateMsg: function() {
                if (!this.selectedJobDefinition) return null
                return this.selectedJobDefinition.suspended
                    ? this.$t('process-instance.jobDefinitions.suspendMsg')
                    : this.$t('process-instance.jobDefinitions.activateMsg')
            },
            stateActionKey: function() {
                if (!this.selectedJobDefinition) return null
                return this.selectedJobDefinition.suspended
                    ? this.$t('process-instance.jobDefinitions.suspendJob')
                    : this.$t('process-instance.jobDefinitions.activateJob')
            },
        },
        methods: {
            show: function(selectedJobDefinition) {
                this.selectedJobDefinition = selectedJobDefinition
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
                    fetchParams: { processDefinitionId: this.selectedJobDefinition.processDefinitionId }
                })
                this.$refs.changeJobStateModal.hide()
            },
            isInThePast: function(ymd, date) {
                return date < moment().startOf('day')
            },
            formatScheduledAt: function() {
                const date = moment(this.scheduledAt.date)
                const datePart = date.format('YYYY-MM-DD')
                const timePart = this.scheduledAt.time + ':00.000'
                return moment(`${datePart}T${timePart}`).format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
            }
        }
    }
</script>
