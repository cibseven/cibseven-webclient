/* globals document, File */

	const BForm = {
	    template: `
	        <form 
	            :class="formClasses" 
	            @submit="handleSubmit" 
	            :novalidate="novalidate" 
	            role="form" 
	            :aria-describedby="ariaDescribedby">
	            <slot></slot>
	        </form>
	    `,
	    props: {
	        novalidate: { type: Boolean, default: false },
	        formClass: { type: String, default: '' },
	        ariaDescribedby: { type: String, default: null }
	    },
	    computed: {
	        formClasses: function() {
	            return ['needs-validation', this.formClass]
	        }
	    },
	    methods: {
	        handleSubmit: function(event) {
	            if (!this.novalidate && !event.target.checkValidity()) {
	                event.preventDefault()
	                event.stopPropagation()
	                this.$emit('validation-failed', event)
	            } else {
	                this.$emit('submit', event)
	            }
	            event.target.classList.add('was-validated')
	        }
	    }
	}
	
	const BFormTag = {
	    template: `
	        <span 
	            :class="['badge', badgeVariant, 'me-1']" 
	            role="button" 
	            :aria-disabled="disabled.toString()" 
	            @click="handleClick">
	            <slot></slot>
	            <button 
	                v-if="!disabled" 
	                type="button" 
	                class="btn-close ms-2" 
	                :aria-label="$t('bcomponents.formTag.remove')" 
	                @click="handleRemove">
	            </button>
	        </span>
	    `,
	    props: {
	        variant: {
	            type: String,
	            default: 'primary'
	        },
	        disabled: {
	            type: Boolean,
	            default: false
	        }
	    },
	    computed: {
	        badgeVariant: function() {
	            return `bg-${this.variant}`
	        }
	    },
	    methods: {
	        handleRemove: function(event) {
	            event.stopPropagation()
	            this.$emit('remove')
	        },
	        handleClick: function(event) {
	            if (!this.disabled) {
	                this.$emit('click', event)
	            }
	        }
	    }
	}
	
	const BFormTimepicker = {
		template: `
		<div class="timepicker-container position-relative" @click.stop role="group" aria-labelledby="timepicker-label">
				<label id="timepicker-label" class="visually-hidden">{{ $t('bcomponents.timepicker.label') }}</label>
				<div class="input-group" :class="inputGroupClassComputed">
					<slot name="prepend"></slot>
					<input type="text" :value="formattedTime" @focus="openPicker" class="form-control" :class="inputClassComputed"
						readonly :aria-label="$t('bcomponents.timepicker.selectedTime')" :placeholder="placeholderText"/>
					<slot name="append"></slot>
				</div>
				<div v-if="showPicker" class="timepicker-dropdown bg-white border rounded p-2 position-absolute shadow-sm w-50 z-1" 
					role="dialog" aria-modal="true" aria-label="Time picker">
					<div class="d-flex justify-content-around mb-2">
						<div class="time-control">
							<button class="btn btn-light btn-sm w-100" @click="incrementHour" :aria-label="$t('bcomponents.timepicker.incrementHour')">
								▲
							</button>
							<input type="text" :value="displayHour" class="form-control text-center my-1" readonly 
								:aria-label="$t('bcomponents.timepicker.hour')">
							<button class="btn btn-light btn-sm w-100" @click="decrementHour" :aria-label="$t('bcomponents.timepicker.decrementHour')">
								▼
							</button>
						</div>
						<span class="mx-2 align-self-center" aria-hidden="true">:</span>
						<div class="time-control">
							<button class="btn btn-light btn-sm w-100" @click="incrementMinute" :aria-label="$t('bcomponents.timepicker.incrementMinute')">
								▲
							</button>
							<input type="text" :value="displayMinute" class="form-control text-center my-1" readonly 
								:aria-label="$t('bcomponents.timepicker.minute')">
							<button class="btn btn-light btn-sm w-100" @click="decrementMinute" :aria-label="$t('bcomponents.timepicker.decrementMinute')">
								▼
							</button>
						</div>
					</div>
					<button class="btn btn-outline-danger btn-sm w-100 mt-2" @click="clearSelection" :aria-label="$t('bcomponents.timepicker.clearSelection')">
						{{ $t('bcomponents.timepicker.clearSelection') }}
					</button>
				</div>
			</div>
		`,
		props: {
			modelValue: { type: String, default: null },
			value: { type: String, default: null },
			size: { type: String, default: null },
			inputClass: { type: String, default: null },
			inputGroupClass: { type: String, default: null },
		},
		data: function() {
			return {
				hour: null,
				minute: null,
				showPicker: false
			}
		},
		computed: {
			placeholderText: function() {
			    return this.modelValue === null ? this.$t('bcomponents.timepicker.noTimeSelected') : ''
			},
			formattedTime: function() {
				return this.modelValue === null ? '' : `${this.paddedHour}:${this.paddedMinute}`
			},
			paddedHour: function() {
				return this.hour !== null ? String(this.hour).padStart(2, '0') : '--'
			},
			paddedMinute: function() {
				return this.minute !== null ? String(this.minute).padStart(2, '0') : '--'
			},
			displayHour: function() {
			    return this.hour !== null ? this.paddedHour : '--'
			},
			displayMinute: function() {
			    return this.minute !== null ? this.paddedMinute : '--'
			},
			inputClassComputed: function() {
			    return [this.inputClass, this.size ? `form-control-${this.size}` : '']
			        .filter(Boolean)
			        .join(' ')
			},
			inputGroupClassComputed: function() {
			    return [this.inputGroupClass, this.size ? `input-group-${this.size}` : '']
			        .filter(Boolean)
			        .join(' ')
			}
		},
		watch: {
			modelValue: {
				immediate: true,
				handler(newValue) {
					if (newValue) {
		                const [hour, minute] = newValue.split(':').map(Number)
		                this.hour = hour || 0
		                this.minute = minute || 0
		            } else {
		                this.hour = null
		                this.minute = null
		            }
				}
			}
		},
		methods: {
			openPicker: function() {
				this.showPicker = true
				this.$eventBus.emit('timepicker-opened', this)
				document.addEventListener('click', this.handleDocumentClick)
				document.addEventListener('keydown', this.handleEscapeKey)
			},
			handleEscapeKey: function(event) {
				if (event.key === 'Escape') {
					this.closePicker()
				}
			},
			closePicker: function() {
				this.showPicker = false
				document.removeEventListener('click', this.handleDocumentClick)
				document.removeEventListener('keydown', this.handleEscapeKey)
			},
			handleDocumentClick: function(event) {
				if (this.showPicker && !this.$el.contains(event.target)) {
					this.closePicker()
				}
			},
			incrementHour: function() {
				if (this.hour === null) this.hour = 0
				this.hour = (this.hour + 1) % 24
				this.updateModel()
			},
			decrementHour: function() {
				if (this.hour === null) this.hour = 23
				this.hour = (this.hour - 1 + 24) % 24
				this.updateModel()
			},
			incrementMinute: function() {
				if (this.minute === null) this.minute = 0
				this.minute = (this.minute + 1) % 60
				this.updateModel()
			},
			decrementMinute: function() {
				if (this.minute === null) this.minute = 59
				this.minute = (this.minute - 1 + 60) % 60
				this.updateModel()
			},
			updateModel: function() {
				if (this.hour !== null && this.minute !== null) {
			        const time = `${String(this.hour).padStart(2, '0')}:${String(this.minute).padStart(2, '0')}`
			        this.$emit('update:modelValue', time)
			        this.$emit('input', time)
			    }
			},
			clearSelection: function() {
				if (this.modelValue === null) {
			        this.hour = null
			        this.minute = null
			    } else {
			        this.hour = 0
			        this.minute = 0
			        this.updateModel()
			    }
			}
		},
		created: function() {
			this.$eventBus.on('timepicker-opened', (openedPicker) => {
				if (openedPicker !== this) {
					this.closePicker()
				}
			})
		},
		beforeDestroy: function() {
			this.$eventBus.off('timepicker-opened')
			document.removeEventListener('click', this.handleDocumentClick)
		}
	}

	const BFormRow = {
		template: `
	        <div 
	            class="row" 
	            :class="customClass" 
	            role="group" 
	            :aria-labelledby="ariaLabelledby" 
	            :aria-describedby="ariaDescribedby">
	            <slot></slot>
	        </div>
	    `,
		props: {
			customClass: {
				type: String,
				default: ''
			},
			ariaLabelledby: {
				type: String,
				default: null
			},
			ariaDescribedby: {
				type: String,
				default: null
			}
		}
	}

	const BFormGroup = {
		template: `
		    <fieldset class="form-group" :id="id" role="group" :aria-labelledby="label ? labelId : null">
		  		<div class="row gx-1">
				    <legend v-if="label || $slots.label" tabindex="-1" :id="labelId" :class="[labelClass, labelColumnClasses, labelSizeClass]">
			      		<slot name="label">{{ label }}</slot>
				    </legend>
				    <div :class="inputColumnClasses">
			      		<slot></slot>
			      		<div v-if="state === false" class="invalid-feedback d-block" role="alert" aria-live="assertive">{{ invalidFeedback }}</div>
				    </div>
		  		</div>
		    </fieldset>
	  	`,
		props: {
			label: { type: String, default: '' },
			labelFor: { type: String, default: null },
			labelCols: { type: [Number, String], default: null },
			labelColsSm: { type: [Number, String], default: null },
			labelColsMd: { type: [Number, String], default: null },
			labelColsLg: { type: [Number, String], default: null },
			labelColsXl: { type: [Number, String], default: null },
			contentCols: { type: [Number, String], default: null },
			contentColsSm: { type: [Number, String], default: null },
			contentColsMd: { type: [Number, String], default: null },
			contentColsLg: { type: [Number, String], default: null },
			contentColsXl: { type: [Number, String], default: null },
			labelSize: { type: String, default: null },
			labelClass: { type: String, default: '' },
			state: { type: Boolean, default: null },
			invalidFeedback: { type: String, default: '' },
			id: { type: String, default: null }
		},
		computed: {
			labelColumnClasses: function() {
				const classes = []
				if (this.labelCols) classes.push(`col-${this.labelCols}`)
				if (this.labelColsSm) classes.push(`col-sm-${this.labelColsSm}`)
				if (this.labelColsMd) classes.push(`col-md-${this.labelColsMd}`)
				if (this.labelColsLg) classes.push(`col-lg-${this.labelColsLg}`)
				if (this.labelColsXl) classes.push(`col-xl-${this.labelColsXl}`)
				classes.push('col-form-label') // Bootstrap form label class
				return classes.join(' ')
			},
			inputColumnClasses: function() {
				const classes = []
				if (this.contentCols) classes.push(`col-${this.contentCols}`)
				if (this.contentColsSm) classes.push(`col-sm-${this.contentColsSm}`)
				if (this.contentColsMd) classes.push(`col-md-${this.contentColsMd}`)
				if (this.contentColsLg) classes.push(`col-lg-${this.contentColsLg}`)
				if (this.contentColsXl) classes.push(`col-xl-${this.contentColsXl}`)
				return classes.length ? classes.join(' ') : 'col' // Fallback to `col` if no content columns specified
			},
			labelSizeClass: function() {
				return this.labelSize ? `col-form-label-${this.labelSize}` : ''
			},
			labelId: function() {
				return this.labelFor || `${this.id || 'form-group'}__label`
			}
		}
	}
	
	const BFormCheckbox = {
		template: `
	    	<div :class="['form-check', { 'form-switch': isSwitch }]">
	      		<label class="form-check-label" :for="id">
	        		<input type="checkbox" class="form-check-input" :id="id"
						:disabled="disabled" :required="required" :checked="computedChecked"
		          		@change="updateValue($event)">
		        	<slot></slot>
	      		</label>
    		</div>
	  	`,
		props: {
			modelValue: {
				type: [Boolean, Array],
				default: false
			},
			value: {
				type: [String, Number, Object],
				default: null
			},
			required: Boolean,
			disabled: Boolean,
			switch: Boolean,
			id: {
				type: String,
				default: () => `checkbox-${Math.random().toString(36).substr(2, 9)}`
			}
		},
		computed: {
			isSwitch: function() {
				return this.switch
			},
			computedChecked: function() {
				if (Array.isArray(this.modelValue)) {
					return this.modelValue.includes(this.value)
				}
				return this.modelValue
			}
		},
		methods: {
			updateValue: function(event) {
				const checked = event.target.checked
				if (Array.isArray(this.modelValue)) {
					const newValue = [...this.modelValue]
					const index = newValue.findIndex(item => item === this.value)
					if (checked && index === -1) {
						newValue.push(this.value)
					} else if (!checked && index > -1) {
						newValue.splice(index, 1)
					}
					this.$emit('update:modelValue', newValue)
				} else {
					this.$emit('update:modelValue', checked)
				}
			}
		}
	}
	
	const BFormTextarea = {
		template: `
	    	<div :class="['form-group', wrapperClass]">
	      		<textarea ref="textarea" :id="id" :class="['form-control', textareaClass]"
		        	:placeholder="placeholder" :rows="parsedRows" :style="{ resize: resizeStyle }"
		        	:aria-describedby="ariaDescribedby" 
					:aria-label="ariaLabel" 
					:value="modelValue" 
					@input="$emit('update:modelValue', $event.target.value)" 
					@blur="handleBlur" 
					@focus="handleFocus">
				</textarea>
		    </div>
	  	`,
		props: {
			modelValue: { type: [String, Number], default: '' },
			placeholder: { type: String, default: '' },
			id: { type: String, default: null },
			rows: { type: [Number, String], default: 3 },
			resize: { type: String, default: 'vertical' },
			textareaClass: { type: String, default: '' },
			wrapperClass: { type: String, default: '' },
			ariaDescribedby: { type: String, default: null },
			ariaLabel: { type: String, default: null },
			noResize: { type: Boolean, default: false }
		},
		computed: {
			resizeStyle: function() {
				if (this.noResize === true) return 'none'
				return ['vertical', 'horizontal', 'both', 'none'].includes(this.resize) ? this.resize : 'vertical'
			},
			parsedRows: function() {
	            return typeof this.rows === 'string' ? parseInt(this.rows, 10) : this.rows
	        }
		},
		methods: {
			handleBlur: function(event) { this.$emit('blur', event) },
			handleFocus: function(event) { this.$emit('focus', event) },
			focus: function() { this.$refs.textarea.focus() }
		}
	}
	
	const BFormInput = {
	    template: `
	        <div v-if="hasWrapper" :class="['form-group', wrapperClass]">
	            <div :class="['d-flex align-items-center']">
	                <input 
	                    ref="input"
	                    v-bind="$attrs" 
	                    :id="id" 
	                    :type="type" 
	                    :class="['form-control', inputClass, sizeClass, validationClass]"
	                    :placeholder="placeholder" 
	                    :value="modelValue"
	                    :aria-describedby="ariaDescribedby"
	                    :aria-invalid="state === false ? 'true' : null"
	                    @input="$emit('update:modelValue', $event.target.value)" 
	                    @blur="handleBlur" 
	                    @focus="handleFocus" 
	                />
	                <slot name="append"></slot>
	            </div>
	            <div v-if="state === false" class="invalid-feedback mt-0">
	                <slot name="invalid-feedback">{{ invalidFeedback }}</slot>
	            </div>
	        </div>
	        <input v-else 
	            ref="input" 
	            v-bind="$attrs" 
	            :id="id" 
	            :type="type" 
	            :class="['form-control', inputClass, sizeClass, validationClass]" 
	            :placeholder="placeholder" 
	            :value="modelValue" 
	            :aria-describedby="ariaDescribedby"
	            :aria-invalid="state === false ? 'true' : null"
	            @input="$emit('update:modelValue', $event.target.value)" 
	            @blur="handleBlur" 
	            @focus="handleFocus" 
	        />
	    `,
	    props: {
	        modelValue: { type: [String, Number], default: '' },
	        placeholder: { type: String, default: '' },
	        id: { type: String, default: null },
	        type: { type: String, default: 'text' },
	        inputClass: { type: String, default: '' },
	        wrapperClass: { type: String, default: '' },
	        size: { type: String, default: null },
	        state: { type: Boolean, default: null },
	        invalidFeedback: { type: String, default: '' },
	        ariaDescribedby: { type: String, default: null }
	    },
	    computed: {
	        sizeClass: function() {
	            return this.size === 'sm' ? 'form-control-sm' : this.size === 'lg' ? 'form-control-lg' : ''
	        },
	        validationClass: function() {
	            if (this.state === true) return 'is-valid'
	            if (this.state === false) return 'is-invalid'
	            return ''
	        },
	        hasWrapper: function() {
	            return !!this.invalidFeedback || !!this.$slots.append
	        }
	    },
	    methods: {
	        handleBlur: function(event) {
	            this.$emit('blur', event)
	        },
	        handleFocus: function(event) {
	            this.$emit('focus', event)
	        },
	        focus: function() {
	            this.$refs.input.focus()
	        }
	    },
	    inheritAttrs: false
	}
	
	const BFormSelect = {
		template: `
	    	<div :class="['form-group', wrapperClass]">
		      <select 
		        :id="id" 
		        :class="['form-select', selectClass]" 
		        :style="selectStyle" 
		        :value="modelValue" 
		        :disabled="disabled"
		        :aria-describedby="ariaDescribedby"
		        @change="$emit('update:modelValue', $event.target.value)" 
		        @blur="handleBlur" 
		        @focus="handleFocus">
		        <slot name="first"></slot>	        
		        <template v-for="(group, groupIndex) in groupedOptions">
		          <optgroup v-if="group.label" :key="'group-' + groupIndex" :label="group.label">
		            <option v-for="(option, index) in group.options" 
		                :key="'group-' + groupIndex + '-option-' + index" 
		                :value="option.value" :selected="option.value === modelValue">
		              {{ option.text }}
		            </option>
		          </optgroup>
		          <option v-else v-for="(option, index) in group.options" 
				  	:key="'ungrouped-' + groupIndex + '-option-' + index" 
		          	:value="option.value" :selected="option.value === modelValue">
		            {{ option.text }}
		          </option>
		        </template>
		      </select>
		    </div>
  		`,
		props: {
		    modelValue: { type: [String, Number], default: null },
		    options: { 
	     		type: Array, 
		      	default: () => []
		    },
		    id: { type: String, default: null },
		    selectClass: { type: String, default: '' },
		    selectStyle: { type: Object, default: () => ({}) },
		    wrapperClass: { type: String, default: '' },
			disabled: { type: Boolean, default: false },
			ariaDescribedby: { type: String, default: null }
	  	},
	  	computed: {
	    	groupedOptions: function() {
				return this.options.map(option => {
		   			if (typeof option === 'object' && option.label && Array.isArray(option.options)) {
			     		return option
				   	}
		   			let normalizedOption = {}
				   	if (typeof option === 'object') normalizedOption = option
					else normalizedOption = { text: option, value: option }
			   		return { options: [ normalizedOption ] }
	 			})
		    }
	  	},
	  	methods: {
	    	handleBlur: function(event) {
	      		this.$emit('blur', event)
	    	},
	    	handleFocus: function(event) {
	      		this.$emit('focus', event)
	    	}
		}
	}
	
	const BFormSelectOption = {
		template: `
		    <option 
		        :value="modelValue" 
		        :disabled="disabled" 
		        :aria-disabled="disabled.toString()">
	      		<slot></slot>
		    </option>
		`,
		props: {
			modelValue: {
				type: [String, Number, Object, Boolean],
				default: null
			},
			disabled: {
				type: Boolean,
				default: false
			}
		}
	}
	
	const BFormFile = {
		template: `
	    	<div class="mb-3">
	      		<label v-if="$slots.label" :for="id" class="form-label">
		        	<slot name="label"></slot>
		      	</label>
		      	<label v-else :for="id" class="form-label">{{ label }}</label>
		      	<input :id="id" :class="classes" type="file" :accept="accept" :multiple="multiple" @change="handleChange" />
	    	</div>
	  	`,
		props: {
			id: { type: String },
			label: { type: String },
			accept: { type: String, default: '' },
			multiple: { type: Boolean, default: false },
			modelValue: { type: [File, Array], default: () => [] },
			variant: { type: String, default: 'primary' }
		},
		data: function() {
			return {
				files: []
			}
		},
		computed: {
			classes: function() {
				return ['form-control', `form-control-${this.variant}`]
			}
		},
		watch: {
			modelValue: {
				immediate: true,
				handler(newVal) {
					if (newVal && Array.isArray(newVal)) {
						this.files = newVal
					}
				}
			}
		},
		methods: {
			handleChange(event) {
				const selectedFiles = this.multiple ? Array.from(event.target.files) : event.target.files[0]
				this.files = selectedFiles
				this.$emit('update:modelValue', selectedFiles)
			}
		}
	}
	
	const BInputGroup = {
	    template: `
	        <div :class="['input-group', wrapperClass, sizeClass]" :style="customStyle">
	            <slot name="prepend"></slot>
	            <input v-if="!hasDefaultSlot" 
	                   :id="id" 
	                   :type="type" 
	                   :class="['form-control', inputClass]" 
	                   :value="modelValue" 
	                   :aria-labelledby="ariaLabelledby" 
	                   :aria-describedby="ariaDescribedby"
	                   @input="$emit('update:modelValue', $event.target.value)" />
	            <slot></slot>
	            <slot name="append"></slot>
	        </div>
	    `,
	    props: {
	        modelValue: { type: [String, Number], default: '' },
	        id: { type: String, default: null },
	        type: { type: String, default: 'text' },
	        inputClass: { type: String, default: '' },
	        wrapperClass: { type: String, default: '' },
	        customStyle: { type: [String, Object], default: '' },
	        size: { type: String, default: null },
	        ariaLabelledby: { type: String, default: null },
	        ariaDescribedby: { type: String, default: null }
	    },
	    computed: {
	        sizeClass: function() {
	            return this.size === 'sm' ? 'input-group-sm' : this.size === 'lg' ? 'input-group-lg' : ''
	        },
	        hasDefaultSlot: function() {
	            return !!this.$slots.default
	        }
	    }
	}
	
	const BInputGroupText = {
		template: `
			<span :class="['input-group-text', textClass]" role="presentation" :aria-hidden="ariaHidden">
				<slot>{{ text }}</slot>
			</span>
		`,
		props: {
			text: { type: String, default: '' },
			textClass: { type: String, default: '' },
			ariaHidden: { type: Boolean, default: true }
		}
	}
	
	const BInputGroupPrepend = {
		template: `
	    	<div class="input-group-prepend">
	      		<span v-if="text" class="input-group-text" :role="role" :aria-hidden="ariaHidden">{{ text }}</span>
	      		<slot v-else></slot>
	    	</div>
	  	`,
		props: {
			text: {
				type: String,
				default: null
			},
			role: {
				type: String,
				default: 'presentation'
			},
			ariaHidden: {
				type: Boolean,
				default: true
			}
		}
	}
	
	const BInputGroupAppend = {
		template: `
		    <div class="input-group-append">
		        <slot></slot>
		    </div>
		`
	}
	
	const BFormDatepicker = {
		template: `
	    	<div class="datepicker-container position-relative flex-fill" @click.stop role="group" aria-labelledby="datepicker-label">
				<label id="datepicker-label" class="visually-hidden">{{ $t('bcomponents.datepicker.label') }}</label>				
				<div class="input-group" :class="inputGroupClassComputed">
					<slot name="prepend"></slot>
					<input type="text" :value="formattedDate" class="form-control" :class="inputClassComputed" 
						@focus="openPicker" readonly :aria-label="$t('bcomponents.datepicker.selectedDate')" 
						:placeholder="formattedDate === '' ? $t('bcomponents.datepicker.noDate') : ''" />
					<slot name="append"></slot>
				</div>				
		      	<div v-if="showPicker" class="datepicker-dropdown bg-white border rounded p-2 position-absolute shadow-sm z-1 w-100"
					role="dialog" aria-modal="true" aria-label="Calendar">
		        	<div class="calendar-header d-flex justify-content-between align-items-center mb-2">
		          		<button class="btn btn-sm btn-outline-secondary" @click="prevMonth" :aria-label="$t('bcomponents.datepicker.prevMonth')">‹</button>
			          	<span class="month-year" aria-live="polite">{{ formattedMonthYear }}</span>
			          	<button class="btn btn-sm btn-outline-secondary" @click="nextMonth" :aria-label="$t('bcomponents.datepicker.nextMonth')">›</button>
			        </div>
		        	<div class="calendar-grid border rounded" role="grid">
		          		<div class="row g-0" role="row">
		            		<div class="col text-center" v-for="day in daysOfWeek" :key="day" role="columnheader">{{ day }}</div>
		          		</div>
		          		<div v-for="(week, weekIndex) in weeksInMonth" :key="weekIndex" class="row g-0" role="row">
			            	<button class="border-0 col py-2" v-for="(day, dayIndex) in week" 
			              		:key="dayIndex" :disabled="isDisabled(day.date)" :class="[
					                isSelected(day.date) ? 'bg-primary text-white fw-bold' : '',
					                !isSelected(day.date) && day.isOtherMonth && !isDisabled(day.date) ? 'bg-light text-dark' : '',
					                !isSelected(day.date) && !day.isOtherMonth && !isDisabled(day.date) ? 'bg-white text-dark' : ''
	              				]"
			              		@click="!isDisabled(day.date) && selectDate(day.date)"
								role="gridcell" 
								:aria-selected="isSelected(day.date) ? 'true' : 'false'" 
								:aria-label="formatDateForAccessibility(day.date)"
								>
			              			{{ day.day }}
			            	</button>
		          		</div>
		        	</div>
			        <div class="datepicker-footer mt-3 d-flex">
		          		<button class="btn btn-sm btn-outline-primary" @click="selectToday" :aria-label="$t('bcomponents.datepicker.selectToday')">
							{{ $t('bcomponents.datepicker.selectToday') }}
						</button>
		          		<button class="btn btn-sm btn-outline-danger ms-auto" @click="clearSelection" 
							:aria-label="$t('bcomponents.datepicker.clearSelection')">
							{{ $t('bcomponents.datepicker.clearSelection') }}
						</button>
			        </div>
	      		</div>
		    </div>
	  	`,
		props: {
			modelValue: { type: [String, Date], default: () => new Date() },
			dateDisabledFn: { type: Function, default: null },
			size: { type: String, default: null },
			inputClass: { type: String, default: null },
			inputGroupClass: { type: String, default: null }
		},
		watch: {
	  		modelValue: {
		    	handler(newVal) {
		      		this.selectedDate = this.parseToDate(newVal)
		    	},
		    	immediate: true
	  		}
		},
		data: function() {
			return {
				showPicker: false,
				currentDate: this.parseToDate(this.modelValue) || new Date(),
				selectedDate: this.parseToDate(this.modelValue)
			}
		},
		computed: {
			inputClassComputed: function() {
			    return [this.inputClass, this.size ? `form-control-${this.size}` : '']
			        .filter(Boolean)
			        .join(' ')
			},
			inputGroupClassComputed: function() {
			    return [this.inputGroupClass, this.size ? `input-group-${this.size}` : '']
			        .filter(Boolean)
			        .join(' ')
			},
			formattedDate: function() {
				return this.selectedDate ? this.selectedDate.toLocaleDateString() : ''
			},
			formattedMonthYear: function() {
				return this.currentDate.toLocaleString(this.$i18n.locale, { month: 'long', year: 'numeric' })
			},
			daysOfWeek: function() {
				return this.$t('bcomponents.datepicker.daysOfWeek').split(",")
			},
			weeksInMonth: function() {
				const weeks = []
				let week = []

				this.daysInMonth.forEach((day, index) => {
					week.push(day)
					if ((index + 1) % 7 === 0) {
						weeks.push(week)
						week = []
					}
				})

				if (week.length > 0) {
					const daysToFill = 7 - week.length
					const lastDayOfMonth = this.daysInMonth[this.daysInMonth.length - 1].date

					for (let i = 1; i <= daysToFill; i++) {
						const nextDate = new Date(
							lastDayOfMonth.getFullYear(),
							lastDayOfMonth.getMonth(),
							lastDayOfMonth.getDate() + i
						)
						week.push({ date: nextDate, day: nextDate.getDate(), isOtherMonth: true })
					}
					weeks.push(week)
				}

				return weeks
			},
			daysInMonth: function() {
				const year = this.currentDate.getFullYear()
				const month = this.currentDate.getMonth()
				const firstDayOfMonth = new Date(year, month, 1)
				const lastDayOfMonth = new Date(year, month + 1, 0)
				const startDay = firstDayOfMonth.getDay() === 0 ? 6 : firstDayOfMonth.getDay() - 1 // Adjust to start from monday (opcional)

				let days = []
				// Add days from the previous month to complete the initial week
				for (let i = 0; i < startDay; i++) {
					const prevDate = new Date(year, month, i - startDay + 1)
					days.push({ date: prevDate, day: prevDate.getDate(), isOtherMonth: true })
				}

				// Add days from the current month
				for (let day = 1; day <= lastDayOfMonth.getDate(); day++) {
					days.push({ date: new Date(year, month, day), day: day, isOtherMonth: false })
				}

				return days
			}
		},
		methods: {
			openPicker: function() {
				this.showPicker = true
				this.$eventBus.emit('datepicker-opened', this)
				document.addEventListener('click', this.handleDocumentClick)
				document.addEventListener('keydown', this.handleEscapeKey)
			},
			closePicker: function() {
				this.showPicker = false
				document.removeEventListener('click', this.handleDocumentClick)
				document.removeEventListener('keydown', this.handleEscapeKey)
			},
			handleEscapeKey: function(event) {
				if (event.key === 'Escape') {
					this.closePicker()
				}
			},
			handleDocumentClick: function(event) {
				if (this.showPicker && !this.$el.contains(event.target)) {
					this.closePicker()
				}
			},
			prevMonth: function() {
				this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() - 1, 1)
			},
			nextMonth: function() {
				this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1, 1)
			},
			selectDate: function(date) {
				this.selectedDate = date
				this.updateModel()
				this.closePicker()
			},
			selectToday: function() {
				this.selectDate(new Date())
			},
			clearSelection: function() {
				this.selectedDate = null
				this.updateModel()
				this.closePicker()
			},
			isSelected: function(date) {
				return this.selectedDate && this.selectedDate.toDateString() === date.toDateString()
			},
			isDisabled: function(date) {
				return this.dateDisabledFn && this.dateDisabledFn(date.toISOString().split('T')[0], date)
			},
			updateModel: function() {
				const value = this.selectedDate ? this.selectedDate.toISOString() : null
				this.$emit('update:modelValue', value)
			},
			parseToDate: function(value) {
				if (!value) return null
				return typeof value === 'string' ? new Date(value) : value instanceof Date ? value : null
			},
			formatDateForAccessibility: function(date) {
				return date.toLocaleDateString(this.$i18n.locale, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
			}
		},
		created: function() {
			this.$eventBus.on('datepicker-opened', (openedPicker) => {
				if (openedPicker !== this) {
					this.closePicker()
				}
			})
		},
		beforeDestroy: function() {
			this.$eventBus.off('datepicker-opened')
			document.removeEventListener('click', this.handleDocumentClick)
		}
	}
	
	
	export { BForm, BFormCheckbox, BFormDatepicker, BFormFile, BFormGroup, BFormInput, BFormRow,
		BFormSelect, BFormSelectOption, BFormTag, BFormTextarea, BFormTimepicker, BInputGroup,
		BInputGroupAppend, BInputGroupPrepend, BInputGroupText
	}