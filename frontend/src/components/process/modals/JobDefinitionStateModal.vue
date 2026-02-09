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
  <b-modal ref="changeJobStateModal" :title="stateActionKey" size="lg">
    <div>
      <p>{{ getStateMsg }}</p>
      <b-form-group>
        <b-form-checkbox v-model="includeExistingJob" id="includeExisting">
          {{ $t('process-instance.jobDefinitions.includeExisting') }}
        </b-form-checkbox>
        <label for="includeExisting" class="fw-medium mt-2 mb-1">{{ $t('process-instance.jobDefinitions.execute') }}</label>
        <div class="form-check" v-for="option in executionOptions" :key="option">
          <input class="form-check-input" type="radio" name="executionOption" :id="option" :value="option" v-model="executionOption" />
          <label class="form-check-label" :for="option">{{ $t('process-instance.jobDefinitions.' + option) }}</label>
        </div>
        <div v-if="executionOption === 'delayed'" class="row">
          <p class="fw-medium mt-2 mb-1">{{ $t('process-instance.jobDefinitions.scheduleAt') }}</p>
          <div class="col-6">
            <b-form-datepicker v-model="scheduledAt.date" size="sm" :date-disabled-fn="isInThePast" input-class="text-start">
              <template v-slot:prepend>
                <span class="input-group-text" aria-hidden="true">
                  <i class="mdi mdi-calendar-outline"></i>
                </span>
              </template>
            </b-form-datepicker>
            <span v-if="invalidDate" class="text-danger">{{ $t('process-instance.jobDefinitions.invalidDateError') }}</span>
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
      <b-button @click="$refs.changeJobStateModal.hide()" variant="light">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="changeJobDefinitionState()" variant="primary">
        {{ selectedJobDefinition && selectedJobDefinition.suspended ? $t('process-instance.jobDefinitions.activate') : $t('process-instance.jobDefinitions.suspend') }}
      </b-button>
    </template>
  </b-modal>
</template>

<script>
import { moment } from '@/globals.js'

export default {
  name: 'JobDefinitionStateModal',
  inject: ['currentLanguage'],
  data: function() {
    return {
      selectedJobDefinition: null,
      includeExistingJob: true,
      executionOption: 'immediately',
      executionOptions: ['immediately','delayed'],
      scheduledAt: null,
      invalidDate: false
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
        ? this.$t('process-instance.jobDefinitions.activateJob')
        : this.$t('process-instance.jobDefinitions.suspendJob')
    },
},
methods: {
    show: function(selectedJobDefinition) {
      const now = new Date()
      now.setMinutes(now.getMinutes() + 5)
      const hours = now.getHours().toString().padStart(2, '0')
      const minutes = now.getMinutes().toString().padStart(2, '0')
      this.invalidDate = false
      this.selectedJobDefinition = selectedJobDefinition
      this.includeExistingJob = true
      this.executionOption = 'immediately'
      this.scheduledAt = { date: new Date(), time: `${hours}:${minutes}` }
      this.$refs.changeJobStateModal.show()
    },
    changeJobDefinitionState: function() {
      this.invalidDate = false
      let executionDate = null
      if (this.executionOption === 'delayed') {
        if (!this.scheduledAt.date) {
          this.invalidDate = true
          return
        }
        executionDate = this.formatScheduledAt()        
      }
      const data = {
        suspended: !this.selectedJobDefinition.suspended,
        includeJobs: this.includeExistingJob,
        executionDate: executionDate
      }
      this.$store.dispatch('job/suspendJobDefinition', {
        jobDefinitionId: this.selectedJobDefinition.id,
        params: data
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
