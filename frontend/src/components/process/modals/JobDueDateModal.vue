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
  <b-modal ref="changeDueDateModal" :title="$t('process-instance.jobs.changeDueDate')" size="lg">
    <div>
      <p class="mb-3">{{ $t('process-instance.jobs.changeDueDateMsg') }}</p>
      <div>
        <label class="fw-medium mt-2 mb-2">{{ $t('process-instance.jobs.recalculate') }}</label>
        <div class="form-check mb-2" v-for="option in dueDateOptions" :key="option.value">
          <input class="form-check-input" type="radio" :id="option.value" :value="option.value" v-model="selectedOption" />
          <label class="form-check-label" :for="option.value">{{ option.label }}</label>
        </div>
      </div>
      <div v-if="selectedOption === 'specific'" class="mt-3">
        <b-form-group>
          <label class="fw-semibold mb-1">{{ $t('process-instance.jobs.scheduleAt') }}</label>
          <div class="row">
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
          <span v-if="dateTimeError" class="text-danger">{{ $t('process-instance.jobs.invalidDateError') }}</span>
        </b-form-group>
        <b-form-group>
          <b-form-checkbox v-model="cascade">
            {{ $t('process-instance.jobs.cascade') }}
          </b-form-checkbox>
        </b-form-group>
      </div>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.changeDueDateModal.hide()" variant="light">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="changeDueDate()" variant="primary" :disabled="!canChange">
        {{ $t('process-instance.jobs.overrideAction') }}
      </b-button>
    </template>
  </b-modal>
</template>

<script>
import { moment } from '@/globals.js'
import { mapActions } from 'vuex'

export default {
  name: 'JobDueDateModal',
  inject: ['currentLanguage'],
  emits: ['job-due-date-changed'],
  data: function() {
    return {
      selectedJob: null,
      selectedOption: 'recalculate_creation',
      scheduledAt: null,
      cascade: false,
      dateTimeError: false
    }
  },
  computed: {
    dueDateOptions() {
      return [
        { value: 'recalculate_creation', label: this.$t('process-instance.jobs.recalculateFromCreation') },
        { value: 'recalculate_current', label: this.$t('process-instance.jobs.recalculateFromCurrent') },
        { value: 'specific', label: this.$t('process-instance.jobs.setSpecificDate') }
      ]
    },
    canChange: function() {
      if (this.selectedOption === 'specific') {
        return this.scheduledAt && this.scheduledAt.date && !this.dateTimeError
      }
      return true
    }
  },
  methods: {
    ...mapActions('job', ['recalculateJobDueDate', 'changeJobDueDate']),
    show: function(selectedJob) {
      const now = new Date()
      now.setMinutes(now.getMinutes() + 1)
      const hours = now.getHours().toString().padStart(2, '0')
      const minutes = now.getMinutes().toString().padStart(2, '0')
      
      this.selectedJob = selectedJob
      this.selectedOption = 'recalculate_creation'
      
      // Always initialize with current date/time
      this.scheduledAt = {
        date: new Date(),
        time: `${hours}:${minutes}`
      }
      
      this.cascade = false
      this.dateTimeError = false
      this.$refs.changeDueDateModal.show()
    },
    changeDueDate: function() {
      this.dateTimeError = false      
      let promise      
      if (this.selectedOption === 'specific') {
        if (!this.scheduledAt.date) {
          this.dateTimeError = true
          return
        }
        promise = this.changeJobDueDate({
          jobId: this.selectedJob.id,
          params: {
            duedate: this.formatScheduledAt(),
            cascade: this.cascade
          }
        })
      } else {
        promise = this.recalculateJobDueDate({
          jobId: this.selectedJob.id,
          params: { creationDateBased: this.selectedOption === 'recalculate_creation' }
        })
      }
      promise.then(() => {
        this.$emit('job-due-date-changed')
      })      
      this.$refs.changeDueDateModal.hide()
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
