/* globals sessionStorage, localStorage, window */
	
	var breakpoints = ['', 'sm-', 'md-', 'lg-', 'xl-']
	
	const CIBHeaderFlow = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/layout/cib-header.html').then(function(html) {
			return { 
				template: html,
				inject: ['currentLanguage'],
				props: { languages: Array, user: Object },
				methods: {
			        logout: function() {
			        	sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : localStorage.removeItem('token')
			        	this.$emit('logout')
			        }
				}
	       	}
		})
	})
	
	const Sidebars = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/layout/sidebars.html').then(function(html) {
			return { 
				template: html,
				props: { leftOpen: Boolean, rightOpen: Boolean, leftCaption: String, rightCaption: String,
					leftSize: { type: Array, default: function() { return [12, 6, 4, 3, 3] } },
					rightSize: { type: Array, default: function() { return [12, 6, 4, 3, 3] } }
				},
				computed: {
					middleClasses: function() {
						if (this.leftOpen) {
							var middleSize = this.leftSize.map(function(val, i) { return 12 - val - (this.rightOpen ? this.rightSize[i] : 0) }.bind(this))
							var offset = this.leftSize.map(function(size, i) { return 'offset-' + breakpoints[i] + size }).join(' ')
							return this.colClasses(middleSize) + ' ' + offset		
						} else if (this.rightOpen) return this.colClasses(this.rightSize.map(function(val) { return 12 - val }))
						else return 'col-12'
					}
				},
				methods: { // https://help.optimizely.com/Build_Campaigns_and_Experiments/Use_screen_measurements_to_design_for_responsive_breakpoints
			        showMain: function(keepRight) {
			        	if (window.innerWidth < 576) { // sm breakpoint
			        		this.$emit('update:leftOpen', false)
			        		this.$emit('update:rightOpen', false)
			        	} else if (window.innerWidth < 768) { // md breakpoint
				        	if (this.rightOpen && keepRight) this.$emit('update:leftOpen', false)
	    					else if (this.leftOpen && !keepRight) this.$emit('update:rightOpen', false)
			        	}
			        },
			        showRight: function() {
			        	this.$emit('update:rightOpen', true)
			        	if (window.innerWidth < 768) this.$emit('update:leftOpen', false)
			        },
			        colClasses: function(sizes) {
						return sizes.map(function(size, i) {
							if (!size) return 'd-none'
							else if (size && i > 0 && !sizes[i-1]) return 'd-' + breakpoints[i] + 'block col-' + breakpoints[i] + size  
							else return 'col-' + breakpoints[i] + size 
						}).join(' ')							
					}
				}
	       	}
		})
	})
	
	const Sidebar = { // https://getbootstrap.com/docs/5.2/examples/sidebars/#
		template: '<div class="d-flex flex-nowrap">\
			<div class="d-flex flex-column border border-top-0"> <slot name="left"></slot> </div>\
  			<div class="h-100" style="width: calc(100% - 48px)"> <slot></slot> </div>\
		</div>'
	}
	
	const SidebarItem = {
		template: '<b-list-group-item class="border-0 py-1 text-center" action :to="to" @click="$emit(\'click\', $event)"\
			style="padding-left: 12px; padding-right: 12px">\
			<div :class="iconClass" :title="tooltip" v-b-popover.hover.right></div>\
		</b-list-group-item>',
		props: { iconClass: String, tooltip: String, to: String }
	}
	
	const SidebarDropright = {
		template: '<b-dropdown dropright variant="light" toggle-class="border-0 shadow-none" no-caret>\
			<template #button-content> <span :class="iconClass"></span> </template>\
		    <slot></slot>\
		</b-dropdown>',
		props: { iconClass: String }
	}
		
	export { CIBHeaderFlow, Sidebars, Sidebar, SidebarItem, SidebarDropright }