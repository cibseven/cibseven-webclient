/* globals window, bootstrap, document */

	const BButton = { 
	    template: `
	        <component :is="tag" ref="button" class="btn" :class="[sizeClass, classes]" :href="computedHref" :to="isRouterLink ? to : null" 
				:role="tag === 'button' ? 'button' : null" :aria-disabled="disabled ? 'true' : null" :disabled="disabled && tag === 'button'">
	            <slot></slot>
	        </component>
	    `,
	    props: {
	        variant: { type: String, default: 'primary' },
	        block: { type: Boolean, default: false },
	        href: { type: String, default: null },
	        to: { type: [String, Object], default: null },
			size: { type: String, default: null },
			disabled: { type: Boolean, default: false }
	    },
	    computed: {
	        tag: function() {
	            return this.href ? 'a' : (this.to ? 'router-link' : 'button')
	        },
	        classes: function() {
	            const res = []
	            res.push('btn-' + this.variant)
	            if (this.block) res.push('w-100')
	            return res
	        },
			sizeClass: function() {
	            return this.size === 'sm' ? 'btn-sm' : this.size === 'lg' ? 'btn-lg' : ''
	        },
	        computedHref: function() {
	            return this.href ? this.href : null
	        },
	        isRouterLink: function() {
	            return !!this.to
	        }
	    },
	    methods: {
	        click: function(event) {
	            if (this.disabled) {
	                event.preventDefault()
	                event.stopImmediatePropagation()
	                return
	            }
	            if (!this.href && !this.to) {
	                this.$emit('click', event)
	            }
	        },
			submit: function(event) {
				if (this.disabled) {
	                event.preventDefault()
	                event.stopImmediatePropagation()
	                return
	            }
	            if (!this.href && !this.to) {
	                this.$emit('submit', event)
	            }
			},
			focus: function() {
				const element = this.$refs.button
	            if (element && element.focus) {
	                element.focus()
	            }
			}
	    }
	}
		
	const BButtonClose = { 
		template: `<button type="button" class="btn-close float-end" :aria-label="$t('bcomponents.ariaLabelClose')" @click="$emit('click', $event)"> </button>`
	}

	const BBadge = {
	    template: `
	        <span 
	            :class="['badge', variantClass, pillClass]" 
	            :style="customStyle" 
	            role="status" 
	            :aria-label="ariaLabel">
	            <slot></slot>
	        </span>
	    `,
	    props: {
	        variant: { 
	            type: String, 
	            default: 'primary'
	        },
	        pill: { 
	            type: Boolean, 
	            default: false
	        },
	        customStyle: {
	            type: [String, Object], 
	            default: ''
	        },
	        ariaLabel: { 
	            type: String, 
	            default: ''
	        }
	    },
	    computed: {
	        variantClass: function() {
	            return `text-bg-${this.variant}`
	        },
	        pillClass: function() {
	            return this.pill ? 'rounded-pill' : ''
	        }
	    }
	}
	
	const BCol = {
		template: `
	        <div 
	            :class="colClasses" 
	            :role="role" 
	            :aria-label="ariaLabel">
	            <slot></slot>
	        </div>
	    `,
		props: {
			cols: {
				type: [Number, String],
				default: null
			},
			offset: {
				type: [Number, String],
				default: null
			},
			order: {
				type: [Number, String],
				default: null
			},
			alignSelf: {
				type: String,
				default: null
			},
			role: {
				type: String,
				default: null
			},
			ariaLabel: {
				type: String,
				default: null
			}
		},
		computed: {
			colClasses: function() {
				const classes = []

				if (this.cols) classes.push(`col-${this.cols}`)
				else classes.push('col')

				if (this.offset) classes.push(`offset-${this.offset}`)
				if (this.order) classes.push(`order-${this.order}`)
				if (this.alignSelf) classes.push(`align-self-${this.alignSelf}`)

				return classes.join(' ')
			}
		}
	}
	
	const BSpinner = {
		template: `
			<div :class="['d-inline-block', wrapperClass]" role="status" :aria-live="ariaLive" :aria-busy="ariaBusy">
				<span v-if="label" class="visually-hidden">{{ label }}</span>
				<div :class="[
					'spinner-' + type, 
					variant ? 'text-' + variant : '', 
					{ 
						'spinner-grow': isGrow, 
						'spinner-border': !isGrow,
						'spinner-border-sm': small && !isGrow,
						'spinner-grow-sm': small && isGrow
					}]" :style="computedStyle"></div>
			</div>
		`,
		props: {
			type: { type: String, default: 'border' },
			variant: { type: String, default: '' },
			label: { type: String, default: 'Loading...' },
			small: { type: Boolean, default: false },
			wrapperClass: { type: String, default: '' },
			ariaLive: { type: String, default: 'polite' },
			ariaBusy: { type: Boolean, default: true },
			style: { type: String, default: '' }
		},
		computed: {
			isGrow: function() { 
				return this.type === 'grow' 
			},
			computedStyle() {
				return this.small ? '' : this.style
			}
		}
	}
	
	const BOverlay = {
	    template: `
	        <div :class="['position-relative', { 'd-inline-block': noWrap }]" 
	             :style="{ position: noWrap ? 'relative' : 'static' }" 
	             :aria-busy="show ? 'true' : 'false'" 
	             :aria-live="show ? 'polite' : null">
	            <slot></slot>
	            <div v-if="show" class="overlay position-absolute w-100 h-100" 
					:class="{ 'd-flex': !noCenter, 'align-items-center': !noCenter, 'justify-content-center': !noCenter }" 
					style="top: 0; left: 0;">
	                <div class="w-100 h-100 position-absolute" :style="{ backgroundColor: overlayColor }"></div>
	                <div class="position-relative">
	                    <slot name="overlay"></slot>
	                </div>
	            </div>
	        </div>
	    `,
	    props: {
	        show: { type: Boolean, default: false },
	        opacity: { type: Number, default: 0.5 },
	        noCenter: { type: Boolean, default: false },
	        noWrap: { type: Boolean, default: false }
	    },
	    computed: {
	        overlayColor: function() {
	            return `rgba(255, 255, 255, ${this.opacity})`
	        }
	    }
	}
	
	const BCalendar = {
	    template: `
	        <div class="calendar-container" role="region" aria-labelledby="calendar-header">
	            <input type="text" class="form-control mb-3 text-center" v-model="formattedSelectedDate" 
					readonly :aria-label="$t('bcomponents.calendar.selectedDate')" :placeholder="$t('bcomponents.calendar.noDate')"/>
				<div class="calendar-header d-flex justify-content-between align-items-center mb-2">
				    <div class="d-flex">
				        <button class="btn btn-sm btn-outline-secondary me-2" @click="prevYear" :aria-label="$t('bcomponents.calendar.prevYear')">«</button>
				        <button class="btn btn-sm btn-outline-secondary" @click="prevMonth" :aria-label="$t('bcomponents.calendar.prevMonth')">‹</button>
				    </div>
				    
				    <span class="month-year" aria-live="polite">{{ formattedMonthYear }}</span>
				    
				    <div class="d-flex">
				        <button class="btn btn-sm btn-outline-secondary" @click="nextMonth" :aria-label="$t('bcomponents.calendar.nextMonth')">›</button>
				        <button class="btn btn-sm btn-outline-secondary ms-2" @click="nextYear" :aria-label="$t('bcomponents.calendar.nextYear')">»</button>
				    </div>
				</div>
	            <div class="calendar-grid border rounded">
					<div class="row fw-bold g-0">
	                	<div class="col text-center" v-for="day in daysOfWeek" :key="day" 
							role="columnheader">{{ day }}</div>
					</div>
					<div v-for="(week, weekIndex) in weeksInMonth" :key="weekIndex" class="row g-0" role="row">
			            <button class="border-0 col py-2" v-for="(day, dayIndex) in week"
			                :key="dayIndex" :disabled="isDisabled(day.date)"
							:class="[
			                    isSelected(day.date) ? 'bg-primary text-white fw-bold' : '',
			                    !isSelected(day.date) && day.isOtherMonth && !isDisabled(day.date) ? 'bg-light text-dark' : '',
			                    !isSelected(day.date) && !day.isOtherMonth && !isDisabled(day.date) && !isToday(day.date) ? 'bg-white text-dark' : '',
								!isSelected(day.date) && isToday(day.date) ? 'bg-white text-primary' : ''
			                ]"
							role="gridcell"
	                        :aria-selected="isSelected(day.date) ? 'true' : 'false'"
	                        :aria-label="formatDateForAccessibility(day.date)"
			                @click="!isDisabled(day.date) && selectDate(day.date)">
			                {{ day.day }}
			            </button>
			        </div>
	            </div>
	            <div class="calendar-footer mt-3 d-flex">
	                <b-button size="sm" variant="outline-primary" @click="selectToday">{{ $t('bcomponents.calendar.today') }}</b-button>
	                <b-button size="sm" variant="outline-danger" class="ms-auto" @click="clearSelection">
						{{ $t('bcomponents.calendar.clearSelection') }}
					</b-button>
	            </div>
	        </div>
	    `,
	    props: {
	        modelValue: {
	            type: [Date, String],
			    default: () => null
		    },
	        dateDisabledFn: { type: Function, default: null }
	    },
	    data: function() {
			return {
	            currentDate: this.parseToDate(this.modelValue || new Date()),
	            selectedDate: this.parseToDate(this.modelValue || null)
	        }
	    },
		watch: {
		    modelValue: function(newValue) {
		        this.updateDates(newValue)
		    }
		},
	    computed: {
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
	        daysOfWeek: function() {
	            return this.$t('bcomponents.calendar.daysOfWeek').split(',')				
	        },
	        formattedMonthYear: function() {
	            return this.currentDate.toLocaleString('default', { month: 'long', year: 'numeric' })
	        },
	        formattedSelectedDate: function() {
	            return this.selectedDate ? this.selectedDate.toLocaleDateString() : ''
	        },
	        daysInMonth: function() {
	            const year = this.currentDate.getFullYear()
	            const month = this.currentDate.getMonth()
	            const firstDay = new Date(year, month, 1)
	            const lastDay = new Date(year, month + 1, 0)

	            const days = []
				var date = ''

	            // Days from previous month
	            for (let i = firstDay.getDay() - 1; i > 0; i--) {
	                date = new Date(year, month, 1 - i)
	                days.push({ date, day: date.getDate(), isOtherMonth: true })
	            }

	            // Days of the current month
	            for (let i = 1; i <= lastDay.getDate(); i++) {
	                date = new Date(year, month, i)
	                days.push({ date, day: i, isOtherMonth: false })
	            }

	            // Fill the rest of the week with next month's days
	            for (let i = lastDay.getDay(); i < 6; i++) {
	                date = new Date(year, month + 1, i - lastDay.getDay() + 1)
	                days.push({ date, day: date.getDate(), isOtherMonth: true })
	            }

	            return days
	        }
	    },
	    methods: {
			updateDates: function(newValue) {
				if (newValue) {
		            const parsedDate = this.parseToDate(newValue)
		            this.currentDate = parsedDate
		            this.selectedDate = parsedDate
				}
	        },
			parseToDate: function(value) {
	            if (typeof value === 'string') {
	                return new Date(value)
	            }
	            return value
	        },
	        prevMonth: function() {
	            this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() - 1, 1)
	        },
	        nextMonth: function() {
	            this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1, 1)
	        },
			prevYear: function() {
		        this.currentDate = new Date(this.currentDate.getFullYear() - 1, this.currentDate.getMonth(), 1)
		    },
		    nextYear: function() {
		        this.currentDate = new Date(this.currentDate.getFullYear() + 1, this.currentDate.getMonth(), 1)
		    },
			isToday: function(date) {
		        const today = new Date()
		        return (
		            date.getDate() === today.getDate() &&
		            date.getMonth() === today.getMonth() &&
		            date.getFullYear() === today.getFullYear()
		        )
		    },
			selectDate: function(date) {
	            this.selectedDate = date
	            this.$emit('update:modelValue', date)
	        },
	        selectToday: function() {
	            this.selectDate(new Date())
	        },
	        clearSelection: function() {
	            this.selectedDate = null
	            this.$emit('update:modelValue', null)
	        },
	        isSelected: function(date) {
	            return this.selectedDate && this.selectedDate.toDateString() === date.toDateString()
	        },
	        isDisabled: function(date) {
	            return this.dateDisabledFn && this.dateDisabledFn(date.toISOString().split('T')[0], date)
	        },
			formatDateForAccessibility: function(date) {
	            return date.toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
	        }
	    }
	}
	
	const BCard = { 
		template: `
			<div class="card">
				<h5 v-if="title" class="card-title" :aria-level="headingLevel" role="heading">{{ title }}</h5>
				<slot></slot>
			</div>
		`,
		props: { 
			title: String,
			headingLevel: { type: Number, default: 2 }
		}
	}
	
	const BCardBody = {
	    template: `
	        <div class="card-body">
	            <slot></slot>
	        </div>
	    `
	}
	
	const BCardText = {
	    template: `
	        <div :class="['card-text', textClass]" role="article">
	            <slot></slot>
	        </div>
	    `,
	    props: {
	        textClass: { type: String, default: '' }
	    }
	}
	
	const BProgress = { //TODO max & without child
		template: '<div class="progress"> <slot></slot> </div>',
		props: {
			variant: String,
			animated: Boolean
		}
	}
	
	const BProgressBar = { 
		template: '<div role="progressbar" class="progress-bar" :class="classes" :style="{ width: value + \'%\'}">{{ label }}</div>',
		props: {
			label: String,
			value: Number			
		},
		computed: {
			classes: function() {
				var res = []
				res.push('bg-' + this.$parent.variant)
				if (this.$parent.animated) res.push('progress-bar-striped progress-bar-animated')				
				return res
			}
		}
	}
	
	const BAvatar = {
		template: `
			<div :class="avatarClasses" :style="avatarStyles" role="img" :aria-label="computedAriaLabel">
	      		<img v-if="src" :src="src" :alt="alt || computedAriaLabel" class="img-fluid" />
	      		<span v-else class="d-flex align-items-center justify-content-center">{{ initials }}</span>
		    </div>
	  	`,
		props: {
			src: { type: String, default: '' },
			alt: { type: String, default: '' },
			text: { type: String, default: '' },
			size: { type: String, default: 'md' }, // 'sm', 'md', 'lg'
			variant: { type: String, default: 'primary' }, // Bootstrap color variants like 'primary', 'secondary', etc.
			rounded: { type: Boolean, default: true } // If true, avatar will be rounded-circle
		},
		computed: {
			avatarClasses: function() {
				return [
					'd-inline-flex', 'align-items-center', 'justify-content-center',
					`bg-${this.variant}`,
					this.rounded ? 'rounded-circle' : 'rounded',
					'overflow-hidden'
				]
			},
			avatarStyles: function() {
				return {
					width: this.computedSize,
					height: this.computedSize,
					fontSize: this.computedFontSize,
					textAlign: 'center'
				}
			},
			computedSize: function() {
				switch (this.size) {
					case 'sm':
						return '32px'
					case 'lg':
						return '64px'
					default:
						return '48px'
				}
			},
			computedFontSize: function() {
				switch (this.size) {
					case 'sm':
						return '14px'
					case 'lg':
						return '28px'
					default:
						return '20px'
				}
			},
			computedAriaLabel: function() {
				return this.alt || (this.text ? this.$t('bcomponents.avatar.avatarFor', { name: this.text }) : this.$t('bcomponents.avatar.defaultAvatar'))
			},
			initials: function() {
				return this.text ? this.text.substring(0, 2).toUpperCase() : ''
			}
		}
	}
	
	const BDropdown = {
		template: `
	    	<div :class="dropright ? 'dropend' : 'dropdown'" ref="dropdownContainer">
	  			<button class="btn" :class="classes" type="button" ref="toggleButton" 
					aria-haspopup="true" :aria-expanded="isOpen.toString()" @click="toggle">
			    	<slot name="button-content">{{ title }}</slot>
		  		</button>
			  	<ul class="dropdown-menu" :class="{ 'dropdown-menu-end': right }" role="menu" @click.stop>
			    	<slot></slot>
			  	</ul>
	    	</div>
	  	`,
		props: {
			title: String,
			variant: String,
			toggleClass: String,
			right: Boolean,
			dropright: Boolean,
			noCaret: Boolean
		},
		computed: {
			classes: function() {
				let res = []
				res.push('btn-' + this.variant)
				if (this.toggleClass) res.push(this.toggleClass)
				if (!this.noCaret) res.push('dropdown-toggle')
				return res
			}
		},
		data: function() {
			return {
				dropdownInstance: null,
				isOpen: false
			}
		},
		mounted: function() {
			this.dropdownInstance = new bootstrap.Dropdown(this.$refs.toggleButton)
			document.addEventListener('click', this.handleDocumentClick)
		},
		beforeDestroy: function() {
			document.removeEventListener('click', this.handleDocumentClick)
		},
		methods: {
			show: function() {
				if (this.dropdownInstance && !this.isOpen) {
					this.dropdownInstance.show()
					this.isOpen = true
					this.$emit('shown')
				}
			},
			hide: function() {
				if (this.dropdownInstance && this.isOpen) {
					this.dropdownInstance.hide()
					this.isOpen = false
					this.$emit('hidden')
				}
			},
			toggle: function(event) {
				if (this.isOpen) {
					this.hide()
				} else {
					this.show()
				}
				if (event) {
					event.stopPropagation()
				}
			},
			handleDocumentClick: function(event) {
				if (this.$refs.dropdownContainer && this.$refs.dropdownContainer.contains(event.target)) {
					return
				}
				this.hide()
			}
		}
	}
	
	const BDropdownDivider = {
  		template: '<hr class="dropdown-divider" role="separator">'
	}
	
	const BDropdownItem = {
		template: '<li @click="$emit(\'click\', $event)">\
			<a class="dropdown-item" :class="{ active: active }" :href="url" role="menuitem" :aria-current="active ? \'page\' : null"> <slot></slot> </a>\
		</li>',
		props: { active: Boolean, href: String, to: String },
		computed: {
			url: function() { /*jshint scripturl:true */
				if (this.href) return this.href
				else if (this.to) return '#' + this.to
				else return 'javascript:void(0)'
			}
		}
	} // https://stackoverflow.com/a/46997175
	
	const BDropdownItemButton = {
		template: '<li @click="$emit(\'click\', $event)">\
			<button class="dropdown-item" type="button" role="menuitem"> <slot></slot> </button>\
		</li>'
	}
	
	const BDropdownGroup = {
		template: `
	    	<div :class="['dropdown-group', groupClass]" role="group" :aria-label="$t('bcomponents.ariaLabelDropdownGroup')">
	      		<slot></slot>
		    </div>
	  	`,
		props: {
			groupClass: {
				type: String,
				default: ''
			}
		}
	}
	
	const BDropdownForm = {
		template: `
		    <form :class="['b-dropdown-form', formClass]" @submit.prevent="onSubmit" :aria-label="ariaLabel">
		      <slot></slot>
		    </form>
	  	`,
		props: {
			formClass: {
				type: String,
				default: ''
			},
			ariaLabel: {
				type: String,
				default: ''
			}
		},
		methods: {
			onSubmit: function() {
				this.$emit('submit')
			}
		}
	}
	
	const BDdItemBtn = {
		template: `
	    <button 
	      :class="['dropdown-item', buttonClass]" 
	      type="button" 
	      :disabled="disabled" 
	      :aria-disabled="disabled">
	      <slot></slot>
	    </button>
	  `,
		props: {
			buttonClass: {
				type: [String, Array, Object],
				default: ''
			},
			disabled: {
				type: Boolean,
				default: false
			}
		}
	}
	
	const BListGroup = {
		template: '<ul class="list-group" role="list" @click="$emit(\'click\', $event)"> <slot></slot> </ul>'
	}
	
	const BListGroupItem = {
	    template: `
	        <component
	            :is="itemTag" :class="['list-group-item', itemClasses]"
	            :role="isButton ? 'button' : 'listitem'" :aria-current="active ? 'true' : null"
	            :aria-disabled="disabled ? 'true' : null" :tabindex="disabled ? -1 : 0"
	            @click="handleClick" @mousedown="$emit('mousedown', $event)" style="cursor: pointer"
				@keydown.space.prevent="handleClick" @keydown.enter.prevent="handleClick"
				@mouseenter="$emit('mouseenter', $event)" @mouseleave="$emit('mouseleave', $event)">
            	<slot></slot>
	        </component>
	    `,
	    props: {
	        to: { type: [String, Object], default: null },
	        href: { type: String, default: null },
	        button: { type: Boolean, default: false },
	        active: { type: Boolean, default: false },
	        disabled: { type: Boolean, default: false },
	        variant: { type: String, default: 'light' }
	    },
	    computed: {
	        itemClasses: function() {
				const classes = {
		            'list-group-item-action': this.isButton || this.isLink,
		            active: this.active,
		            disabled: this.disabled
		        }
		        if (this.variant) {
		            classes[`list-group-item-${this.variant}`] = true
		        }
		        return classes
	        },
	        isLink: function() {
	            return this.to || this.href
	        },
	        isButton: function() {
	            return this.button
	        },
	        itemTag: function() {
	            return this.isLink ? 'a' : this.isButton ? 'button' : 'li'
	        }
	    },
	    methods: {
	        handleClick: function(event) {
	            if (this.disabled) {
	                event.preventDefault()
	                return
	            }
	            if (this.isLink) {
	                if (this.to) {
	                    this.$router.push(this.to)
	                } else if (this.href) {
	                    window.location.href = this.href
	                }
	            } else {
	                this.$emit('click', event)
	            }
	        }
	    }
	}
		
	export { 
		BButton, BButtonClose, BAvatar, BBadge, BCalendar, 
		BCard, BCardBody, BCardText, BCol, BOverlay, 
		BProgress, BProgressBar, BSpinner,
		BDdItemBtn, BDropdown, BDropdownDivider, BDropdownForm, 
		BDropdownGroup, BDropdownItem, BDropdownItemButton,
		BListGroup, BListGroupItem
	 }