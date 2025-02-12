/* globals bootstrap, Popper, setTimeout, document, window, clearTimeout, HTMLElement */

	const BAlert = {
		template: '<transition name="fade">\
			<div ref="alert" v-if="show_" role="alert" class="alert alert-dismissible" :class="\'alert-\' + variant" aria-live="assertive" aria-atomic="true">\
				<slot></slot>\
				<b-button-close @click="dismiss"></b-button-close>\
			</div>\
		</transition>',
		props: { 
			variant: { type: String, default: 'primary' },
			show: { type: [Number, Boolean], default: false } 
		},
		data: function() { 
			return { 
				show_: false, 
				timeoutId: null 
			} 
		},
		watch: { 
			show: function(val) {
				this.show_ = val
				if (typeof val === 'number' && val > 0) {
					this.clearTimeout()
					this.timeoutId = setTimeout(() => {
						this.dismiss()
					}, val * 1000)
				}
			}
		},
		methods: {
			dismiss: function() {
				this.clearTimeout()
				this.show_ = false
				this.$emit('dismissed')
			},
			clearTimeout: function() {
				if (this.timeoutId) {
					clearTimeout(this.timeoutId)
					this.timeoutId = null
				}
			}
		},
		beforeDestroy: function() {
			this.clearTimeout()
		}
	}

	const BModal = Vue.defineAsyncComponent(() => {
	    return axios.get('webjars/common-frontend/bcomponents/b-modal.html').then(function(html) {
	        return {
	            template: html,
	            props: {
	                okOnly: Boolean,
	                title: String,
	                size: String,
	                scrollable: Boolean,
					fullscreen: Boolean,
	                hideFooter: Boolean,
	                bodyClass: String,
	                headerClass: String,
	                footerClass: [String, Object]
	            },
				data: function() {
			  		return {
			    		lastFocused: null
				  	}
				},
	            mounted: function() {
	                this.modal = new bootstrap.Modal(this.$refs.modal, {
	                    backdrop: 'static',
	                    keyboard: false
	                })
	
	                if (this.$refs.modal.parentNode !== document.body) {
	                    document.body.appendChild(this.$refs.modal)
	                }
	
					this.$refs.modal.addEventListener('show.bs.modal', () => { this.$emit('show') })
	                this.$refs.modal.addEventListener('shown.bs.modal', () => this.$emit('shown'))
	                this.$refs.modal.addEventListener('hide.bs.modal', () => { this.$emit('hide') })
	                this.$refs.modal.addEventListener('hidden.bs.modal', () => this.$emit('hidden'))
	                this.$refs.modal.addEventListener('hidePrevented.bs.modal', () => this.hide('close'))
	            },
	            computed: {
	                sizeClass: function() {
						return this.size === 'xl' ? 'modal-xl' : 
			           		this.size === 'lg' ? 'modal-lg' : 
			           		this.size === 'sm' ? 'modal-sm' : ''
	                },
					modalClasses() {
			      		return [
				        	this.sizeClass,
				        	{ 'modal-dialog-scrollable': this.scrollable, 'modal-fullscreen': this.fullscreen }
				      	]
				    }
	            },
	            methods: {
	                cancel: function() {
	                    this.hide('cancel')
	                },
	                ok: function() {
	                    this.hide('ok')
	                },
	                hide: function(trigger) {
	                    this.modal.hide()
	                    this.$emit('hide', trigger)
						if (this.lastFocused && typeof this.lastFocused.focus === 'function') {
					    	this.lastFocused.focus();
				     	}
	                },
	                show: function() {
						this.lastFocused = document.activeElement
	                    this.modal.show()
	                }
	            }
	        }
	    })
	})
	
	const BPopover = {
		template: `
	      <div v-show="isVisible" ref="popover" class="popover-content" :class="wrapperClass" :style="{ maxWidth: maxWidth, width: width || 'auto' }"
	        role="tooltip" :aria-hidden="!isVisible":id="popoverId">
	        <slot></slot>
	      </div>
	  `,
		props: {
			target: { type: [Function, String], required: false },
			placement: { type: String, default: 'bottom' }, // 'top', 'bottom', 'left', 'right'
			triggers: { type: String, default: 'click' }, // 'click', 'hover'
			show: { type: Boolean, default: false },
			wrapperClass: { type: String, default: 'border rounded shadow-sm bg-white p-3' },
			hideDelay: { type: Number, default: 200 },
			viewportPadding: { type: Number, default: 20 },
			transitionDuration: { type: Number, default: 250 },
			width: { type: String, default: null },
			maxWidth: { type: String, default: null }
		},
		data: function() {
			return {
				isVisible: false,
				popperInstance: null,
				hideTimeout: null,
				triggerType: null,
				popoverId: `popover-${Math.random().toString(36).substr(2, 9)}`
			}
		},
		watch: {
	    	show: function(newValue) {
	      		if (newValue) {
		       		this.showPopover()
		      	} else {
		        	this.hidePopover()
		      	}
		    }
	  	},
		mounted: function() {
			this.movePopoverToBody()
			this.setEventListeners()
			const popover = this.$refs.popover
	
			if (popover) {
				popover.addEventListener('mouseenter', this.cancelHidePopover)
				popover.addEventListener('mouseleave', this.scheduleHidePopover)
			}
	
			const targetElement = this.getTargetElement()
			if (targetElement) {
				targetElement.setAttribute('aria-describedby', this.popoverId)
			}
	
			document.addEventListener('click', this.handleDocumentClick)
			document.addEventListener('keydown', this.handleKeydown)
			window.addEventListener('resize', this.hidePopover)
			if (this.show) {
				setTimeout(() => {
	                this.showPopover()
	            }, 1000)
			}
		},
		beforeUnmount: function() {
			const popover = this.$refs.popover
	
			if (popover) {
				popover.removeEventListener('mouseenter', this.cancelHidePopover)
				popover.removeEventListener('mouseleave', this.scheduleHidePopover)
			}
	
			const targetElement = this.getTargetElement()
			if (targetElement) {
				targetElement.removeAttribute('aria-describedby')
			}
	
			this.destroyPopper()
			document.removeEventListener('click', this.handleDocumentClick)
			document.removeEventListener('keydown', this.handleKeydown)
			window.removeEventListener('resize', this.hidePopover)
	
			if (this.$refs.popover && this.$refs.popover.parentNode) {
				this.$refs.popover.parentNode.removeChild(this.$refs.popover)
			}
		},
		methods: {
			movePopoverToBody: function() {
				document.body.appendChild(this.$refs.popover)
			},
			getTargetElement: function() {
				if (typeof this.target === 'function') {
					const target = this.target()
					if (target instanceof HTMLElement) {
						return target
					} else if (target && target.$el instanceof HTMLElement) {
						return target.$el
					}
					//console.error('Target function did not return a valid HTMLElement or Vue component with $el.')
					return null
				} else if (typeof this.target === 'string') {
					return document.getElementById(this.target)
				}
				//console.error('Invalid target type. Expected a function or string.')
				return null
			},
			setEventListeners: function() {
				const targetElement = this.getTargetElement()
				if (!targetElement) {
					//console.error('Target element for popover not found.')
					return
				}
	
				const events = this.triggers.split(' ')
				events.forEach(event => {
					if (event === 'click') {
						targetElement.addEventListener('click', this.handleClickTrigger)
					} else if (event === 'hover') {
						targetElement.addEventListener('mouseenter', this.handleHoverTrigger)
						targetElement.addEventListener('mouseleave', this.scheduleHidePopover)
					}
				})
			},
			handleClickTrigger: function() {
				this.triggerType = 'click'
				this.togglePopover()
			},
			handleHoverTrigger: function() {
				this.triggerType = 'hover'
				this.showPopover()
			},
			togglePopover: function() {
				this.isVisible = !this.isVisible
				if (this.isVisible) {
					this.createPopperInstance()
				} else {
					this.destroyPopper()
				}
			},
			showPopover: function() {
				this.cancelHidePopover()
				this.isVisible = true
				this.createPopperInstance()
				this.$refs.popover.focus()
			},
			hidePopover: function() {
				if (this.triggerType === 'click') {
					return
				}
				this.isVisible = false
				this.destroyPopper()
			},
			scheduleHidePopover: function() {
				if (this.triggers === 'manual') return
				this.hideTimeout = setTimeout(() => {
					this.hidePopover()
				}, this.hideDelay)
			},
			cancelHidePopover: function() {
				if (this.hideTimeout) {
					clearTimeout(this.hideTimeout)
					this.hideTimeout = null
				}
			},
			createPopperInstance: function() {
				const targetElement = this.getTargetElement()
				if (!targetElement || !this.$refs.popover) return
	
				this.popperInstance = Popper.createPopper(targetElement, this.$refs.popover, {
					placement: this.placement,
					modifiers: [
						{
							name: 'preventOverflow',
							options: {
								boundary: 'viewport',
								padding: this.viewportPadding
							},
						},
						{
							name: 'offset',
							options: {
								offset: [0, 8]
							},
						},
					],
				})
	
				this.$refs.popover.style.zIndex = '1050'
			},
			destroyPopper: function() {
				if (this.popperInstance) {
					this.popperInstance.destroy()
					this.popperInstance = null
				}
			},
			handleDocumentClick: function(event) {
				if (this.triggerType === 'click' && this.isVisible) {
					const targetElement = this.getTargetElement()
					if (
						this.$refs.popover &&
						!this.$refs.popover.contains(event.target) &&
						targetElement &&
						!targetElement.contains(event.target)
					) {
						this.isVisible = false
						this.destroyPopper()
					}
				}
			},
			handleKeydown: function(event) {
				if (event.key === 'Escape' && this.isVisible) {
					this.hidePopover()
				}
			}
		}
	}	
	
	export { BAlert, BPopover, BModal }