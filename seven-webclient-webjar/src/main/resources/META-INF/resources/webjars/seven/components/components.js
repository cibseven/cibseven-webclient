/* globals platform, localStorage, sessionStorage, location, permissionsMixin, 
clearTimeout, setTimeout, window, encodeURIComponent, document */
	
	import { permissionsMixin } from '../permissions.js';
	import { InfoService, AuthService } from '../services.js';

	function debounce(delay, fn) {
		var timeoutID = null
		return function() {
			clearTimeout(timeoutID)
			var args = arguments
			var self = this
			timeoutID = setTimeout(() => { fn.apply(self, args) }, delay)
		}
	}

	const Seven = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/seven.html').then(function(html) {
			return {
				template: html,
				mixins: [permissionsMixin],
				inject: ['isMobile'],
				data: function() { return { rememberNotShow: false, version: '' } },
				created: function() {
					InfoService.getVersion().then(version => {
						this.version = version
					})
				},
				watch: {
					pageTitle: function(title) {
						let defaultTitle = 'CIB Seven'
						if (this.$root.config.productNamePageTitle) defaultTitle = this.$root.config.productNamePageTitle
						if (!title) document.title = defaultTitle
						else document.title = defaultTitle +' | ' + title						
					}
				},
				computed: {
					startableProcesses: function() {
						return this.processesFiltered.find(p => { return p.startableInTasklist })
					},
					processesFiltered: function() {
						if (!this.$store.state.process.list) return []
						return this.$store.state.process.list.filter(process => {
							return ((!process.revoked))
						}).sort((objA, objB) => {
							var nameA = objA.name ? objA.name.toUpperCase() : objA.name
							var nameB = objB.name ? objB.name.toUpperCase() : objB.name
							var comp = nameA < nameB ? -1 : nameA > nameB ? 1 : 0
							
							if (this.$root.config.subProcessFolder) {
								if (objA.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = 1
								else if (objB.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = -1
							}
							return comp
						})
					},   					
					pageTitle: function() {
						switch (this.$route.name) {
							case 'tasklist': return this.$t('start.taskList')
							case 'processManagement': return this.$t('start.admin')
							case 'deployments': return this.$t('deployment.title')
							case 'processes': return this.$t('start.startProcesses')
							case 'management': return this.$t('start.admin')
							case 'modeler': return this.$t('start.modeler')
							case 'easyForm': return this.$t('start.easyForm')
							case 'flowResource': return this.$t('start.resources')
							case 'adminUsers':
							case 'createUser': 
								return this.$t('admin.users.title')
							case 'adminGroups':
							case 'createGroup': 
								return this.$t('admin.groups.title')
							case 'authorizations':
							case 'authorizationType':
								return this.$t('admin.authorizations.title')
							default: return ''
						}
					},
					expertMode: function() {
						var expertMode = localStorage.getItem('expertMode')
	    				if (expertMode) return true
	    				return false
					}
				},
    			mounted: function () {
    				if (platform.name === 'IE') {
	    				var isNotifiedUser = localStorage.getItem('ienotify')
	    				if (!isNotifiedUser) this.$refs.ieNotification.show() //must notify the user
    				}
					let defaultTitle = 'CIB Seven'
					if (this.$root.config.productNamePageTitle) defaultTitle = this.$root.config.productNamePageTitle
					
					if (!this.pageTitle) document.title = defaultTitle
					else document.title = defaultTitle +' | ' + this.pageTitle
    			},
    			methods: {
    				logout: function() {
						this.$router.push('/')
						location.reload() //refresh to empty vuex and axios defaults
						//Remove some storage variables when logout
						//https://helpdesk.cib.de/browse/BPM4CIB-3691
						localStorage.removeItem('accessToken')
						localStorage.removeItem('tokenModeler')
						sessionStorage.removeItem('accessToken')
						sessionStorage.removeItem('tokenModeler')									
					},
					openStartProcess: function() {
						this.$eventBus.emit('openStartProcess')
					},
					doNotShowIeNotification: function() { if (this.rememberNotShow) localStorage.setItem('ienotify', true) },
					openDoximaLogin: function () {
						window.location.href = this.$root.config.doximaLoginUrl + encodeURIComponent(window.location.href)
					}
    			}
			}
		})
	})
	
	const FilterableSelect = {
		template: '<div><b-dd block @shown="$refs.filter.focus(); $emit(\'shown\')"\
				toggle-tag="div" @hidden="filter = \'\'" toggle-class="p-0 border-0"\
				@show="function(evt) { disabled ? evt.preventDefault() : null }" @hide="$emit(\'hide\')" no-caret>\
				<template v-slot:button-content>\
					<b-input-group>\
						<b-form-input size="sm" type="text" ref="inputFilter" :value="value"\
						style="cursor: auto" :placeholder="placeholder" @keydown.prevent ></b-form-input>\
					</b-input-group>\
				</template>\
				<b-dd-form form-class="p-2"><b-form-input class="w-100" ref="filter" v-model="filter" @input="onInput"></b-form-input></b-dd-form>\
				<div v-if="loading" class="w-100 text-center"><b-spinner></b-spinner></div>\
				<b-dropdown-group v-if="elements && !loading" style="max-height:150px" class="overflow-auto">\
					<b-dd-item-btn button-class="p-1 d-flex" v-for="(label, value, index) in filteredElements"\
					:key="index" @click="$emit(\'input\', label.id)">\
							<b-avatar class="me-2" :text="label.id.substring(0, 2)" variant="light"></b-avatar>\
							<span class="me-auto">\
								<div>{{ label.firstName + " " + label.lastName }}</div>\
								<div class="small">{{ label.id }}</div>\
							</span>\
					</b-dd-item-btn>\
				</b-dropdown-group>\
			</b-dd></div>',
		props: { elements: Object | Array, value: String, disabled: Boolean, required: Boolean, invalidFeedback: String,
				noInvalidValues: { type: Boolean, default: false }, placeholder: String, loading: { type: Boolean, default: false } },
		data: function() {
			return { filter: '' }
		},
		watch: {
			filter: function(val) {
				if (val.length >= 3 && !this.loading) this.$emit('update:loading', true)
				else if (val.length < 3) {
					this.$emit('clean-elements', val)
					this.$emit('update:loading', false)
				}
			}
		},
		computed: {
			filteredElements: function() {
				var list = {}
				if (this.elements) {
					if (Array.isArray(this.elements)) {
						this.elements.forEach(element => {
							if((element.id.toLowerCase().indexOf(this.filter.toLowerCase()) !== -1) ||
								((element.firstName + ' ' + element.lastName).toLowerCase().indexOf(this.filter.toLowerCase()) !== -1))
								list[element.id] = element
						})
					} else {
						Object.keys(this.elements).forEach(key => {
							if ((this.elements[key].id.toLowerCase().indexOf(this.filter.toLowerCase()) !== -1) || 
								((this.elements[key].firstName + ' ' + this.elements[key].lastName).toLowerCase().indexOf(this.filter.toLowerCase()) !== -1))
								list[key] = this.elements[key]
						})
					}
				}
				return list
			},
			isValid: function() {
				var allElements = this.elements
				if (!Array.isArray(allElements)) allElements = Object.keys(allElements)
				return !this.noInvalidValues || !this.value ||
					allElements.includes(element => { if (this.value === element.id) return true; else return false })
			}
		},
		methods: {
			onInput: debounce(800, function() { if (this.filter.length >= 3) this.$emit('enter', this.filter) })
		}
	}
	
	const SidebarElementGroup = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/sidebar-element-group.html').then(function(html) {
			return {
				props: {
					cols: { type: Number, default: 4 }, 
					value: {}, 
					icon: String, 
					label: String, 
					textareaEval: Boolean, 
					disabled: { type: Boolean, default: true } },
				template: html
			}
		})
	})
		
	const SmartSearch = { 
		template: `<div>
			<datalist :id="id"> <option v-for="opt in options">{{ opt }}</option> </datalist>
			<b-input-group>
				<template v-slot:prepend>
					<b-input-group-text class="py-0 border-light"><span class="mdi mdi-18px mdi-magnify"
					style="line-height: initial"></span></b-input-group-text>
				</template>
				<b-form-input :title="$t('searches.searchByTaskName')" size="sm" ref="input" type="search" v-model="filter" :list="id"
				class="form-control border-start-0 ps-0 form-control border-light shadow-none"
					:placeholder="$t('searches.searchByTaskName')" :maxlength="maxlength" @input="$emit('input', filter)"/>				
				<template v-slot:append v-if="$root.config.taskFilter.advancedSearch.criteriaKeys.length > 0 && 
					$root.config.taskFilter.advancedSearch.modalEnabled">
					<b-button variant="light" class="py-0" @click="$emit('open-advanced-search')" :title="$t('advanced-search.title')"">
						<span class="mdi mdi-18px mdi-filter-variant" style="line-height: initial"></span>
						<span v-if="$store.state.advancedSearch.criterias.length > 0" class="bg-danger position-absolute rounded" 
							style="bottom: 5px; width: 7px; height: 7px; right: 5px;"></span>
					</b-button>
				</template>
			</b-input-group>
		</div>`,
		props: { options: Array, maxlength: Number },
		computed: { id: function() { return 'smart-search' + Date.now() } }, // https://dev.to/rahmanfadhil/how-to-generate-unique-id-in-javascript-1b13
		data: function() { return { filter: '' } },
		methods: {
			focus: function() { this.$refs.input.focus() }
		}
	}
		
	
	const SidebarsFlow = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/sidebars.html').then(function(html) {
			return { 
				template: html,
				props: { leftOpen: Boolean, rightOpen: Boolean, leftCaption: String, rightCaption: String,
					leftSize: { type: Array, default: function() { return [12, 6, 4, 3, 3] } },
					rightSize: { type: Array, default: function() { return [12, 6, 4, 2, 2] } },
					number: Number, headerCalc: { type: String, default: '40px' }
				},
				computed: {
					middleClasses: function() {
						if (this.leftOpen) {
							var middleSize = this.leftSize.map((val, i) => { return 12 - val - (this.rightOpen ? this.rightSize[i] : 0) })
							var offset = this.leftSize.map((size, i) => { return 'offset-' + this.breakpoints[i] + size }).join(' ')
							return this.colClasses(middleSize) + ' ' + offset		
						} else if (this.rightOpen) return this.colClasses(this.rightSize.map(val => { return 12 - val }))
						else return 'col-12'
					},
					breakpoints: function() {
						return ['', 'sm-', 'md-', 'lg-', 'xl-']
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
						return sizes.map((size, i) => {
							if (!size) return 'd-none'
							else if (size && i > 0 && !sizes[i-1]) return 'd-' + this.breakpoints[i] + 'block col-' + this.breakpoints[i] + size  
							else return 'col-' + this.breakpoints[i] + size 
						}).join(' ')							
					}
				}
	       	}
		})
	})
	
	const IconButton = {
		template: '<b-button :size="size" :variant="variant" :title="text" :disabled="disabled" @click="$emit(\'click\', $event)">\
			<span :class="setIconClasses" class="d-inline-block align-middle" style="line-height: 0;"></span>\
			<span class="d-none d-sm-inline">{{ text }}</span>\
			</b-button>',
		props: {
			variant: { type: String, default: 'outline-secondary' },
			icon: String,
			font: { type: Number, default: 18 },
			size: { type: String, default: 'sm' },
			disabled: { type: Boolean, default: false },
			text: String
		},
		computed: {
			setIconClasses: function() {
				return 'icon-cib-' + this.font + 'px icon-' + this.icon
			}
		}
	}
	
	const MultisortModal = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/multisort-modal.html').then(function(html) {
	        return {
	            template: html,
	            props: { prefix: String, sortKeys: Array, items: Array},
	            data: function() { 
					return {
						selectedIndex: null,
						sortingCriteria: [{ field: '', order: 1 }],
						orders: [
							{ text: this.$t('multisort.asc'), value: 1 },
							{ text: this.$t('multisort.desc'), value: -1 } 
						],
						moving: false
	            	}
				},
				computed: {
			  		sortKeysWithText() {
			    		return this.sortKeys.map(value => ({
				      		value: value,
				      		text: this.$t(this.prefix + value)
				    	}))
				  	}
				},
	            methods: {
					show: function() {
						this.$refs.sortModal.show()
					},
					addCriteria: function() {
			      		this.sortingCriteria.push({ field: '', order: 1 })
				    },
				    removeCriteria: function() {
			      		this.sortingCriteria.splice(this.selectedIndex, 1)
			      		this.selectedIndex = null
				    },
				    moveCriteria: function(direction) {
						this.moving = true
						var temp = null
				  		if (direction === 'up' && this.selectedIndex > 0) {
					    	temp = this.sortingCriteria[this.selectedIndex];
					    	this.sortingCriteria[this.selectedIndex] = this.sortingCriteria[this.selectedIndex - 1]
					    	this.sortingCriteria[this.selectedIndex - 1] = temp					    	
							this.selectedIndex = this.selectedIndex - 1
					  	} else if (direction === 'down' && this.selectedIndex < this.sortingCriteria.length - 1) {
					    	temp = this.sortingCriteria[this.selectedIndex]
					    	this.sortingCriteria[this.selectedIndex] = this.sortingCriteria[this.selectedIndex + 1]
					    	this.sortingCriteria[this.selectedIndex + 1] = temp
					    	this.selectedIndex = this.selectedIndex + 1
					  	}
					  	this.$nextTick(() => {
				          	this.moving = false
	        			})		
					},
				    applySorting: function() {
						var sortedItems = this.items.slice()
				  		sortedItems.sort((a, b) => {
				    		for (let i = 0; i < this.sortingCriteria.length; i++) {
					      		var criteria = this.sortingCriteria[i]
					      		var field = criteria.field
					      		if (a[field] < b[field]) return -1 * criteria.order
					      		if (a[field] > b[field]) return 1 * criteria.order
				    		}
						    return 0
					  	})
						this.$emit('apply-sorting', sortedItems)
					  	this.$refs.sortModal.hide()	  
				    }
	            }
	        }
	    })
	})
		
	const LoginFlow = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/login-flow.html').then(function(html) {
			return { 
				template: html,
				props: { credentials: Object, credentials2: Object, hideForgotten: Boolean,
					onRegister: Function, forgottenType: { type: String, default: 'email' } },
				data: function() {
					return { 
						rememberMe: true,
						show: false,
						email: null
					} 
				},
				methods: {
					onLogin: function() {
						var self = this
						AuthService.login(this.credentials, this.rememberMe).then(function(user) { self.$emit('success', user) }, function(error) {  
							var res = error.response.data
							if (res && res.type === 'LoginException' && res.params && res.params.length >= 1 && res.params[0] === 'StandardLogin') {
								self.credentials2.username = self.credentials.username
								self.credentials2.password = self.credentials.password
								self.$refs.otpDialog.show(res.params[1])
							} else if (error.response.status === 429) { // Too many requests
								res.params[1] = new Date(res.params[1]).toLocaleString('de-DE')
								self.$root.$refs.error.show(res)	
							} else self.$root.$refs.error.show(res)
						})
					} // https://vuejs.org/v2/guide/components-custom-events.html
				}
	       	}		
		})
	})
			
	export { Seven, FilterableSelect, SidebarElementGroup, SmartSearch, SidebarsFlow, IconButton, MultisortModal, LoginFlow }