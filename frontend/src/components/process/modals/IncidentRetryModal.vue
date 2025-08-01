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
	<b-modal ref="incidentRetryModal" :title="$t('process-instance.incidents.incrementNumberRetries')" size="lg">
		<div class="container-fluid pt-2">
			<p>{{ $t('process-instance.incidents.retryMsg') }}</p>
			<b-form-group>
				<label class="fw-medium mt-2 mb-1">{{ $t('process-instance.incidents.dueDateMsg') }}</label>
				<div class="form-check" v-for="option in executionOptions" :key="option">
					<input class="form-check-input" type="radio" :id="option" :value="option" v-model="executionOption" />
					<label class="form-check-label" :for="option">{{ $t('process-instance.incidents.' + option) }}</label>
				</div>
				<div v-if="executionOption === 'setDueDate'" class="row">
					<label class="fw-medium mt-2 mb-1">{{ $t('process-instance.incidents.scheduleAt') }}</label>
					<div class="col-6">
						<b-form-datepicker v-model="scheduledAt.date" size="sm" :date-disabled-fn="isInThePast" input-class="text-start">
							<template v-slot:prepend>
								<span class="input-group-text" aria-hidden="true">
									<i class="mdi mdi-calendar-outline"></i>
								</span>
							</template>
						</b-form-datepicker>
						<span v-if="invalidDate" class="text-danger">{{ $t('process-instance.incidents.invalidDateError') }}</span>
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
			<b-button @click="$refs.incidentRetryModal.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
			<b-button @click="incrementNumberRetries()" variant="primary">{{ $t('process-instance.incidents.retry') }}</b-button>
		</template>
	</b-modal>
</template>

<script>
	import { moment } from '@/globals.js'

	export default {
		name: 'IncidentRetryModal',
		inject: ['currentLanguage'],
		data: function() {
			return {
				selectedIncident: null,
				executionOption: 'keepDueDate',
				executionOptions: ['keepDueDate','setDueDate'],
				scheduledAt: null,
				invalidDate: false
			}
		},
		methods: {
			show: function(selectedIncident) {
				const now = new Date()
				now.setMinutes(now.getMinutes() + 1)
				const hours = now.getHours().toString().padStart(2, '0')
				const minutes = now.getMinutes().toString().padStart(2, '0')				
				this.invalidDate = false
				this.selectedIncident = selectedIncident
				this.executionOption = 'keepDueDate'
				this.scheduledAt = { date: new Date(), time: `${hours}:${minutes}` },
				this.$refs.incidentRetryModal.show()
			},
			hide: function() {
				this.$refs.incidentRetryModal.hide()
			},
			isInThePast: function(ymd, date) {
				return date < moment().startOf('day')
			},
			formatScheduledAt: function() {
				const date = moment(this.scheduledAt.date)
				const datePart = date.format('YYYY-MM-DD')
				const timePart = this.scheduledAt.time + ':00.000'
				return moment(`${datePart}T${timePart}`).format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
			},
			incrementNumberRetries: function() {
				this.invalidDate = false
				var params = { retries: 1 }
				if (this.executionOption === 'setDueDate') {
					if (!this.scheduledAt.date) {
						this.invalidDate = true
						return
					}
					params.dueDate = this.formatScheduledAt()
				}
				this.$emit('increment-number-retry', {
					incident: this.selectedIncident,
					params
				})
			}
		}
	}
</script>
