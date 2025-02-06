/* globals console, zxcvbn, moment */
	
	const CIBForm = { 
		template: '<form @submit.prevent="onSubmit" :class="{ \'was-validated\': showValidation }" ref="form" novalidate>\
			<slot :showValidation="showValidation"></slot> </form>',
		data: function() {
			return { showValidation: false } 
		},
		methods: { 
			onSubmit: function() {
				if (this.$refs.form.checkValidity()) {
					this.showValidation = false
					this.$emit('submitted') 
					return true
				} else {
					this.showValidation = true
					this.$emit('fail')
					return false
				}					
			}
		} // https://vuejs.org/v2/api/#ref 
	}
	
	const CIBDatepicker = { // https://github.com/weifeiyue/vue-datepicker-local		
		template: '<vue-datepicker-local ref="datePicker" :value="modelValue" :disabled="disabled" :disabledDate="disabledDate"\
			:local="local" :clearable="true" :format="format || \'DD.MM.YYYY\'" @clear="$refs.datePicker.show = false"\
			@input="$emit(\'update:modelValue\', $event === \'\' ? null : $event)">\
		</vue-datepicker-local>',
		props: { modelValue: [Date, String], disabled: Boolean, disabledDate: Function, format: String },
		computed: {
			local: function() {
				return {
					dow: 1, // Monday is the first day of the week
					hourTip: 'Stunde', // tip of select hour //TODO ? not used yet
					minuteTip: 'Minute', // tip of select minute
					secondTip: 'Sekunde', // tip of select second
					yearSuffix: '', // format of head
					monthsHead: this.$t('cib-datepicker.months').split('_'), // months of head							
					months: this.$t('cib-datepicker.months').split('_'), // months of panel
					weeks: this.$t('cib-datepicker.weeks').split('_'), // weeks
					cancelTip: '', // default text for cancel button 
					submitTip: '' // default text for submit button 
				}
			}
		}
	}
	
	const CIBDatepicker2 = { //no-flip -> https://github.com/bootstrap-vue/bootstrap-vue/issues/5326
		template: '<b-form-datepicker no-flip reset-button today-button value-as-date :value="modelValue" :disabled="disabled" :start-weekday="1"\
			:locale="currentLanguage()" label-help="" :label-no-date-selected="$t(labelNoDateSelected || \'cib-datepicker2.noDate\')"\
			:label-reset-button="$t(\'cib-datepicker2.reset\')" :label-today-button="$t(\'cib-datepicker2.today\')"\
			:date-format-options="{ year: \'numeric\', month: \'2-digit\', day: \'2-digit\' }"\
			:date-disabled-fn="disabledDate" @input="$emit(\'update:modelValue\', $event)" :button-only="buttonOnly" :size="size"></b-form-datepicker>',
		inject: ['currentLanguage'],
		props: { modelValue: Date, disabled: Boolean, disabledDate: Function, 
		labelNoDateSelected: String, buttonOnly: Boolean, size: String }
	}
	
	const SecureInput = {
		template: '<div class="input-group">\
			<input ref="input" :value="modelValue" :placeholder="placeholder" :type="show ? \'text\' : \'password\'" class="form-control rounded-right pr-5"\
				:required="required" :disabled="disabled" :autocomplete="autocomplete" @input="$emit(\'update:modelValue\', $event.target.value)">\
			<span @click="show = !show" class="mdi mdi-18px mdi-eye text-secondary"\
			style="position: absolute; right: 11px; top: 4px; z-index: 3; cursor: pointer"></span>\
			<div class="invalid-feedback" v-t="\'errors.invalid\'"></div>\
		</div>',
		props: { modelValue: String, placeholder: String, required: Boolean, disabled: Boolean, autocomplete: String },
		data: function() {
			return { show: false } 
		}
	}
	
	const SecureBar = { // https://wiki.cib.de/index.php/Password_strength
		template: '<b-popover :target="target" triggers="focus" :placement="placement || \'bottom\'" ref="pop" no-fade>\
			<div style="width: 250px">\
				<span v-t="\'security-bar.prefix\'" class="font-weight-bold"></span>\
				<span v-if="score > 0" v-t="\'security-bar.strength\' + score"></span> <br>\
				<span v-if="crackTime" v-t="\'security-bar.crackTime\'" class="font-weight-bold"></span>\
				<span v-if="crackTime">{{ crackTime }}</span>\
			</div>\
			<b-progress :value="score" :max="5" :variant="variant"></b-progress>\
		</b-popover>',
		props: { password: String, target: [Function, String], placement: String },
		computed: {
			result: function() { 
				if (this.password) {
					var res = zxcvbn(this.password)
					console && console.info('zxcvbn', res.crack_times_seconds, res.feedback)
					this.$emit('zxcvbn', res)
					return res
				}
			},
			score: function() { return !this.password ? 0 : this.result.score+1 },
			variant: function() { return ['danger', 'danger', 'warning', 'info', 'success'][this.score-1] },
			crackTime: function() {
				if (this.password && typeof moment === 'function') { // avoid weird overflow in moment						
					if (this.result.crack_times_seconds.online_no_throttling_10_per_second > 3600 * 24 * 356 * 1000) return moment().add(1000, 'y').fromNow()
					else return moment().add(this.result.crack_times_seconds.online_no_throttling_10_per_second, 's').fromNow()
				} 
			}
		}
	}
	
	const TextButton = {
		template: '<b-button :variant="variant" class="btn-md" @click="$emit(\'click\')">\
			<div class="d-flex flex-nowrap align-items-center">	<span :class="iconClasses"></span> <span class="ml-2" v-t="label"></span> </div>\
		</b-button>',
		props: { label: String, variant: String, iconClasses: String }		
	}
	
	export { CIBForm, CIBDatepicker, CIBDatepicker2, SecureInput, SecureBar, TextButton }