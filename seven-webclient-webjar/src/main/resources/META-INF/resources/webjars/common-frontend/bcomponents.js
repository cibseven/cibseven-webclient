(function() { /* globals bootstrap, Popper, setTimeout */
	"use strict";	
	
	Vue.component('b-button', { 
		template: '<button class="btn" :class="classes" @click="$emit(\'click\', $event)"> <slot></slot> </button>',	
		props: {
			variant: String,
			block: Boolean
		},
		computed: {
			classes: function() {
				var res = []
				res.push('btn-' + this.variant)
				if (this.block) res.push('w-100')				
				return res
			}
		}
	})
	
	Vue.component('b-button-close', { 
		template: '<button type="button" class="btn-close float-end" aria-label="Close" @click="$emit(\'click\', $event)"> </button>'
	})
	
	Vue.component('b-collapse', { 
		template: '<div class="collapse" :class="{ \'navbar-collapse\': isNav }"> <slot></slot> </div>',
		props: {
			isNav: Boolean
		},
	})	
	
	Vue.component('b-dropdown', {
		template: '<div :class="dropright ? \'dropend\' : \'dropdown\'">\
			<button class="btn" :class="classes" type="button" data-bs-toggle="dropdown" aria-expanded="false">\
		    	<slot name="button-content">{{ title }}</slot>\
		  	</button>\
		    <ul class="dropdown-menu" :class="{ \'dropdown-menu-end\': right }"> <slot></slot> </ul>\
	    </div>',
	    props: { title: String, variant: String, toggleClass: String, right: Boolean, dropright: Boolean, noCaret: Boolean },
	    computed: {
			classes: function() {
				var res = []
				res.push('btn-' + this.variant)
				if (this.toggleClass) res.push(' ' + this.toggleClass)
				if (!this.noCaret) res.push('dropdown-toggle')
				return res
			}
		}
	})
	
	Vue.component('b-dropdown-item', {
		template: '<li @click="$emit(\'click\', $event)">\
			<a class="dropdown-item" :class="{ active: active }" :href="url"> <slot></slot> </a>\
		</li>',
		props: { active: Boolean, href: String, to: String },
		computed: {
			url: function() { /*jshint scripturl:true */
				if (this.href) return this.href
				else if (this.to) return '#' + this.to
				else return 'javascript:void(0)'
			}
		}
	}) // https://stackoverflow.com/a/46997175
	
	Vue.component('b-dropdown-item-button', {
		template: '<li @click="$emit(\'click\', $event)">\
			<button class="dropdown-item"> <slot></slot> </button>\
		</li>'
	})
	
	Vue.component('b-progress', { //TODO max & without child
		template: '<div class="progress"> <slot></slot> </div>',
		props: {
			variant: String,
			animated: Boolean
		},
	})	
	
	Vue.component('b-progress-bar', { 
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
	})	
	
	
	
	Vue.component('b-navbar', { 
		template: '<nav class="navbar fixed-top navbar-expand-md">\
			<div class="container-fluid"> <slot> </slot> </div>\
		</nav>'
	})
	
	Vue.component('b-navbar-toggle', { 
		template: '<button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav_collapse"\
	    	aria-controls="nav_collapse" aria-expanded="false" aria-label="Toggle navigation">\
	    	<span class="navbar-toggler-icon"></span>\
	    </button>'
	})
	
	Vue.component('b-navbar-brand', { 
		template: '<a class="navbar-brand" :href="\'#\' + to"> <slot></slot> </a>',
		props: { to: String }
	})
	
	Vue.component('b-navbar-nav', { 
		template: '<ul class="navbar-nav"> <slot></slot> </ul>'
	})
	
	Vue.component('b-nav-item-dropdown', {
		template: '<li class="nav-item dropdown">\
			<a class="nav-link dropdown-toggle" href="javascript:void(0)" role="button" data-bs-toggle="dropdown" aria-expanded="false">\
				<slot name="button-content"></slot>\
			</a>\
		    <ul class="dropdown-menu dropdown-menu-end"> <slot></slot> </ul>\
	    </li>'
	})		
		
		
		
	Vue.component('b-form-group', {
		template: '<fieldset class="form-group">\
			<div class="row">\
				<legend v-if="label" tabindex="-1" class="col-form-label" :class="\'col-\' + labelCols">{{ label }}</legend>\
				<div class="col">\
					<slot></slot>\
					<div tabindex="-1" role="alert" aria-live="assertive" aria-atomic="true" class="invalid-feedback">{{ invalidFeedback }}</div>\
				</div>\
			</div>\
		</fieldset>',
		props: { labelCols: [Number, String], label: String, invalidFeedback: String }
	})
	
	Vue.component('b-form-checkbox', {
		template: '<div class="form-check">\
			<label class="form-check-label">\
				<input type="checkbox" class="form-check-input" :disabled="disabled" :required="required" :checked="value"\
					@change="$emit(\'input\', $event.target.checked)">\
				<slot></slot>\
			</label>\
		</div>',
		props: { value: Boolean, required: Boolean, disabled: Boolean }
	})
	
	
	
	Vue.component('b-list-group', {
		template: '<ul class="list-group" @click="$emit(\'click\', $event)"> <slot></slot> </ul>'
	})	
	
	Vue.component('b-list-group-item', {
		template: '<a v-if="to" :href="\'#\' + to" class="list-group-item" :class="{ \'list-group-item-action\': action }" @click="$emit(\'click\', $event)">\
			<slot></slot>\
		</a>\
		<li v-else class="list-group-item" :class="{ \'list-group-item-action\': action }" @click="$emit(\'click\', $event)"> <slot></slot>	</li>',
		props: { action: Boolean, to: String }
	})	
	
	Vue.component('b-card', { //TODO ?
		template: '<div class="card">\
			<h5 class="card-title">{{ title }}</h5>\
			<slot></slot>\
		</div>',
		props: { title: String }
	})	
	
	
	
	Vue.component('b-alert', {
		template: '<transition name="fade">\
			<div ref="alert" v-if="show" role="alert" class="alert alert-dismissible" :class="\'alert-\' + variant">\
				<slot></slot>\
				<b-button-close @click="show_ = false; $emit(\'dismissed\')"></b-button-close>\
			</div>\
		</transition>',
		props: { variant: String, show: [Number, Boolean] },
		data: function() { return { show_: false } },
		watch: { 
			show: function(val) {
				this.show_ = val
				if (typeof val === 'number' && val > 0) setTimeout(() => { //TODO ? cancel on close button
					this.show_ = 0
					this.$emit('dismissed') 
				}, val * 1000)
			}
		}
	})	
	
	
	
	Vue.component('b-popover', {
		template: '<transition name="fade">\
			<div v-show="show" ref="pop" role="tooltip" class="popover" :class="classes">\
				<div class="popover-arrow"></div>\
				<h3 class="popover-header"> <slot name="title"></slot> </h3>\
				<div class="popover-body"> <slot></slot> </div>\
			</div>\
		</transition>',
		props: { target: Function, placement: String },
		data: function() { return ({ show: false }) },		
		computed: {
			classes: function() {
				return {
					'top': 'bs-popover-top',
					'right': 'bs-popover-end',
					'bottom': 'bs-popover-bottom',
					'left': 'bs-popover-start',
					'bottomleft': 'bs-popover-bottom'
				}[this.placement]
			},
			popperPlacement: function() {
				return {
					'top': 'top',
					'right': 'right',
					'bottom': 'bottom',
					'left': 'left',
					'bottomleft': 'bottom-start'
				}[this.placement]
			}
		},		  	
		mounted: function() { 
	  		this.popper = Popper.createPopper(this.target(), this.$refs.pop, {
	  			placement: this.popperPlacement,
				modifiers: [{ name: 'flip' }, { //TODO ?
			    	name: 'preventOverflow',
			      	options: { mainAxis: true, altAxis: true, altBoundary: true }			      	
			    }, {
      				name: 'arrow',
      				options: { element: '.popover-arrow' }
    			}]
			})
	  		this.$on('open', () => { 
	  			this.show = true
	  			this.popper.update(); //TODO ? nextTick
	  		})
			this.$on('close', () => { //TODO ? https://popper.js.org/docs/v2/tutorial/#performance
				this.show = false
				this.$emit('hidden')
			})
	  	}
	})
	
	
	
	Vue.component('b-modal', function(resolve) { // https://dev.to/tefoh/use-bootstrap-5-in-vue-correctly-2k2g
		axios.get('b-modal.html').then(function(html) {
			resolve({ 
				template: html,
				props: { okOnly: Boolean, title: String },
				mounted: function() { 
			  		this.modal = new bootstrap.Modal(this.$refs.modal, { backdrop: 'static', keyboard: false })
			  		this.$refs.modal.addEventListener('hidePrevented.bs.modal', () => this.hide('close'))
			  		this.$refs.modal.addEventListener('shown.bs.modal', () => this.$emit('shown'))  
			  	},
				methods: {
					show: function() { this.modal.show() },
					hide: function(trigger) { //TODO preventDefault
						this.modal.hide()
						this.$emit('hide', trigger) 
					}
				}
	       	})		
		})
	})
	
})()
