/* globals console, VueI18n, localStorage, sessionStorage, VueRouter, moment, platform,
	 permissionsMixin, navigator, VueGlobalEvents, document, location, Vuex, window,
	 history, ProcessService, Worker, Notification, mitt */
	
 	import { BToggle, BButton, BButtonClose, BCollapse, BDropdown, BDropdownDivider, BDropdownItem, BDropdownItemButton, 
		BDropdownGroup, BDropdownForm, BProgress, BProgressBar, BNavbar, BNavbarToggle, BNavbarBrand, BNavbarNav, 
		BNavItemDropdown, BFormGroup, BFormCheckbox, BListGroup, BListGroupItem, BCard, BCardBody, BCardText, BAlert, 
		BPopover, BModal, BFormTextarea, BFormInput, BFormSelect, BInputGroup, BInputGroupText, 
		BInputGroupPrepend, BInputGroupAppend, BSpinner, BPagination, BOverlay, BForm, BDdItemBtn, 
		BBadge, BFormTag, BCalendar, BFormTimepicker, BFormRow, BCol, BFormDatepicker, BAvatar, BFormSelectOption, 
		BFormFile, BTab, BTabs, BLink, BDPopover } from './webjars/common-frontend/bcomponents/bcomponents.js'
	import { FlowTable } from './webjars/flow-common-frontend/flow-table.js'
	import { Error, Confirm, ProblemReport, Loader, Success } from './webjars/common-frontend/dialogs/dialogs.js'
	import { TaskPopper, Clipboard, TaskList } from './webjars/common-frontend/files/files.js'
	import { CIBHeaderFlow, Sidebars } from './webjars/common-frontend/layout/layout.js'
	import { ResetDialog, OtpDialog } from './webjars/common-frontend/auth.js'
	import { HoverStyle } from './webjars/common-frontend/directives.js'
	import { CIBForm, CIBDatepicker2, SecureInput } from './webjars/common-frontend/forms.js'
	import { permissionsMixin } from './permissions.js';
	import { QuickNavBar, Sidebar, SidebarItem, SidebarDropright } from './components/quick-nav-bar.js';
	import { PasswordRecover } from './components/password/password.js';
	import { Seven, FilterableSelect, SidebarElementGroup, SmartSearch, SidebarsFlow, IconButton, MultisortModal, LoginFlow } from './components/components.js';
	// COCKPIT COMPONENTS //
	import { BpmnViewer } from './components/process/bpmn-viewer.js';
	import { ProcessManagement } from './components/process/process-management.js';
	// Inside this process.js, there are components which belong, for example, to the "Start process" section
	// is this considered cockpit or tasklist?, depending on that, maybe we should split it.
	import { Process, InstancesTable, ProcessVariablesSidebar, ProcessVariablesTable, Processes, 
		ProcessDetailsSidebar, ProcessCard, ProcessAdvanced, ProcessTable, UserTasksTable, 
		TaskAssignationModal, AddVariableModal } from './components/process/process.js';
	import { Management } from './components/management/management.js';
	import { Deployments, ResourcesNavBar, DeploymentList } from './components/deployment/deployment.js';
	/////////////////////////////
	// TASKLIST COMPONENTS //
	import { Tasks, TasksNavBar, Task, AdvancedSearchModal } from './components/task/task.js';
	import { TaskDetailsSidebarAll, TaskDetailsSidebar, TaskDetailsSidebarChat, TaskDetailsSidebarStatus } from './components/task/task-sidebars.js';
	import { CamundaFilter, FilterModal, FilterNavBar, FilterNavCollapsed } from './components/filter/filter.js';
	import { RenderTemplate } from './components/render-template/render-template.js';
	import { StartProcess } from './components/process/start-process.js';
	/////////////////////////////
	// ADMIN COMPONENTS //
	import { AdminUsers, AdminGroups, AdminAuthorizations, AdminAuthorizationsTable,
		AuthorizationsNavBar, CreateUser, CreateGroup, ProfileUser, ProfileGroup } from './components/admin/admin.js';
	//////////////////////////
	import { Modeler } from './components/modeler/modeler.js';
	import { EasyForm } from './components/easy-form/easy-form.js';
	import { FlowResource } from './components/flow-resource/flow-resource.js';
	import { FlowProcessManagement } from './components/flow-process-management/flow-process-management.js';
	import { SupportModal } from './components/support-modal.js';
	import { StatusProgressBar } from './components/status-progress-bar/status-progress-bar.js';
	import { buildProcessStore, buildFilterStore, buildUserStore, buildAdvancedSearchStore } from  './store.js';
	import { FilterService, ProcessService, AdminService, InfoService, AuthService } from './services.js';
	 
	const eventBus = mitt()
	checkExternalReturn(window.location.href, window.location.hash)
	function checkExternalReturn(href, hash) {
		var hrefAux = href
		var hashAux = hash
		
		if (hrefAux.indexOf('doximatoken=') >= 0) {
			var doximaToken = ''
			var doXimatokenStartPos = hashAux.indexOf('doximatoken=') + 'doximatoken='.length
			if(hashAux.indexOf('&', doXimatokenStartPos) > -1) doximaToken = hashAux.substring(doXimatokenStartPos, hashAux.indexOf('&', doXimatokenStartPos))
			else doximaToken = hashAux.substring(doXimatokenStartPos)
			
			localStorage.setItem('doximaToken', doximaToken)
			
			window.location.href = hashAux = hashAux.replace('doximatoken=' + doximaToken, '')
		}
		if (hashAux.includes('token=')) {
			var token = ''
	
			var tokenStartPos = hashAux.indexOf('token=') + 'token='.length
			
			if(hashAux.indexOf('&', tokenStartPos) > -1) token = hashAux.substring(tokenStartPos, hashAux.indexOf('&', tokenStartPos))
			else token = hashAux.substring(tokenStartPos)
			localStorage.setItem('token', decodeURIComponent(token))
			
			window.location.href = hashAux.replace('&token=' + token, '')
		}
	}
	
	function parseParams(paramString) { 
		return paramString.split('&').reduce((params, param) => {
			params[param.split('=')[0].replace('?', '')] = decodeURIComponent(param.split('=')[1]) 
			return params 
		}, {}) 
	}
	
	var configRequest = Promise.all([axios.get('config.json'), InfoService.getProperties()]).then(responses => {
		Object.assign(responses[0].data, responses[1].data)
		var config = responses[0].data
		// Load personalized-css
		var theme = localStorage.getItem('theme') || config.theme 
		var logoPath = ''
		var loginImgPath = ''
		var resetPasswordImgPath = ''
		var params = parseParams(window.location.hash)
		var header = params.header || 'true'
		var pageTitle = ''

		function loadTheme() {
			if (!(theme && theme !== 'cib' && theme !== 'generic')) theme = 'generic'
			var css = document.createElement('Link')
			var favicon = document.createElement('Link')
			
			if (theme === 'generic') {
				//css.setAttribute('href', 'bootstrap/bootstrap_5.3.3.min.css?v=1.14.0')
				css.setAttribute('href', 'webjars/common-frontend/bootstrap/custom-bootstrap.css?v=1.14.0')
				document.title = 'CIB flow'
			} else {
				css.setAttribute('href', 'themes/' + theme + '/bootstrap_4.5.0.min.css')
				document.title = 'Aufgabenübersicht'
			}

			if (config.productNamePageTitle) document.title = config.productNamePageTitle
			pageTitle = document.title

			css.setAttribute('rel', 'stylesheet')
			css.setAttribute('type', 'text/css')
			favicon.setAttribute('rel', 'icon')
			favicon.setAttribute('type', 'image/x-icon')
			favicon.setAttribute('href', 'themes/' + theme + '/favicon.ico')
			document.head.appendChild(css)
			document.head.appendChild(favicon)
			logoPath = 'themes/' + theme + '/logo.svg'
			loginImgPath = 'themes/' + theme + '/login-image.svg'
			resetPasswordImgPath = 'components/password//reset-password.svg'
		}
		
		loadTheme()
		
		const i18n = VueI18n.createI18n({ locale: localStorage.getItem('language') || navigator.language.split('-')[0] || navigator.userLanguage || 'en' })
		
		i18n.global.locale = config.supportedLanguages.includes(i18n.global.locale) ? i18n.global.locale : config.supportedLanguages[0]
		
		axios.defaults.headers.common['Accept-Language'] = i18n.global.locale
		moment.locale(i18n.global.locale)
		
		var loadedLanguages = []
		function fetchTranslation(lang) { // http://kazupon.github.io/vue-i18n/guide/lazy-loading.html
			return loadedLanguages.includes(lang) ? Promise.resolve() : axios.create().get('translations_' + lang + '.json').then(res => {
				i18n.global.setLocaleMessage(lang, res.data)
				// Load custom translations files
				axios.create().get('themes/' + theme + '/cu_translations_' + lang + '.json').then(res => {
					i18n.global.mergeLocaleMessage(lang, res.data)
					loadedLanguages.push(lang)
					//if (res.data.login.productName) document.title = res.data.login.productName
					if (pageTitle) document.title = pageTitle
					else if (res.data.login.productName) document.title = res.data.login.productName						
				}).catch(() => {
					loadedLanguages.push(lang)
				})
			})
		}
		function setHtmlLang(lang) {
			var html = document.documentElement // returns the html tag
			html.setAttribute('lang', lang)
		}
		
		setHtmlLang(i18n.global.locale)
		fetchTranslation(i18n.global.locale)

		axios.interceptors.response.use(res => { return res.data }, handler)
		const app = Vue.createApp({ /*jshint nonew:false */
			el: '#app',
			mixins: [permissionsMixin],
			provide: function() {
				return {
		           currentLanguage(lang) {
		               if (!lang) return i18n.global.locale
		               return fetchTranslation(lang).then(() => {
		                   setHtmlLang(lang)
		                   i18n.global.locale = lang
		                   moment.locale(lang)
		                   axios.defaults.headers.common['Accept-Language'] = lang
		                   localStorage.setItem('language', lang)
		               })
		           },
		           loadProcesses(extraInfo) {
		               const method = extraInfo ? 'findProcessesWithInfo' : 'findProcesses'
		               return this.$store.dispatch(method).then((result) => {
		                   const processes = this.processesByPermissions(config.permissions.startProcess, result)
		                   processes.forEach((process) => {
		                       process.loading = false
		                   })
		                   this.$store.commit('setProcesses', { processes })
		               })
		           },
		           isMobile: isMobile
		       }
			},
			data: function() {
				return {
					user: null,
					config: config,
					consent: localStorage.getItem('consent'),
					logoPath: logoPath,
					loginImgPath: loginImgPath,
					resetPasswordImgPath: resetPasswordImgPath,
					theme: theme,
					header: header
				}
			},
			watch: {
				user: function(user) {
					if (user) this.handleTaskWorker()
				}
			},
			mounted: function() {
				this.$store.registerModule('process', buildProcessStore(ProcessService))
				this.$store.registerModule('filter', buildFilterStore(FilterService))
				this.$store.registerModule('user', buildUserStore(AdminService))
				this.$store.registerModule('advancedSearch', buildAdvancedSearchStore())
				if ('Notification' in window && this.config.notifications.tasks.enabled &&
					(Notification.permission !== 'granted' || Notification.permission !== 'denied')) {
					Notification.requestPermission()
				}
			},
			methods: {				
				remember: function() { localStorage.setItem('consent', true) },
				sendReport: function(data) { axios.post('report', data) },
				handleTaskWorker: function() {
					if (window.Worker && localStorage.getItem('tasksCheckNotificationsDisabled') !== 'true' &&
						this.$root.config.notifications.tasks.enabled && Notification.permission === 'granted') {
					    const taskWorker = new Worker('./task-worker.js')
						const authToken = sessionStorage.getItem('token') || localStorage.getItem('token')
					    taskWorker.postMessage({ type: 'setup', interval: this.$root.config.notifications.tasks.interval, 
							authToken: authToken, userId: this.$root.user.id })
						taskWorker.postMessage({ type: 'checkNewTasks' })
					    taskWorker.addEventListener('message', event => {
							if (event.data && event.data.type === 'sendNotification') {
								let theme = localStorage.getItem('theme') || this.$root.config.theme
								if (!(theme && theme !== 'cib' && theme !== 'generic')) theme = 'generic'
								var icon = 'themes/' + theme
								if (theme === 'generic') icon += '/notification-icon.svg'
								else icon += '/logo.svg'
								const options = {
									body: this.$t('notification.newTasks'),
									tag: 'cib-flow-check-new-tasks',
									icon: icon
								}
								const notification = new Notification(this.$t('notification.newTasksTitle'), options)
								notification.onclick = () => {
									notification.close()
									window.focus()
								}
							}
					    })
					}
				}		
			}
		})
		const store = Vuex.createStore({})
		var router = routing()
		registerOwnComponents(app)
		app.component('GlobalEvents', VueGlobalEvents.GlobalEvents)
		app.use(router)
	    app.use(store)
	    app.use(i18n)
		const root = app.mount('#app')
		app.config.globalProperties.$eventBus = eventBus
		
		function handler(error) {
			if (error.response) {
				var res = error.response
				if (res.status === 401) { // Unauthorized
					if (res.data && res.data.type === 'TokenExpiredException' && res.data.params && res.data.params.length > 0) {
						console && console.info('Prolonged token')
						if (sessionStorage.getItem('token')) sessionStorage.setItem('token', res.data.params[0])
						else if (localStorage.getItem('token')) localStorage.setItem('token', res.data.params[0])
						axios.defaults.headers.common.authorization = root.user.authToken = res.data.params[0]
						// Repeat last request
						error.config.headers.authorization = res.data.params[0]
						return axios.request(error.config)
					} else {
						console && console.warn('Not authenticated, redirecting ...')
						sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : localStorage.removeItem('token')   
						axios.defaults.headers.common.authorization = ''
						root.user = null
						if (router.currentRoute.path !== '/flow/login'){
							router.push('/flow/login/?nextUrl=' + router.currentRoute.path)	
						}
						root.$refs.error.show(res.data || res.statusText)
					}
					return Promise.reject(error)
				} else if (res.status === 500) {					
					var exceptions = ['NoObjectFoundException', 'InvalidAttributeValueException', 'SubmitDeniedException', 
					'UnsupportedTypeException', 'ExpressionEvaluationException', 'ExistingUserRequestException',
					'ExistingGroupRequestException', 'AccessDeniedException', 'SystemException', 'InvalidUserIdException', 'InvalidValueHistoryTimeToLive']
					if (res.data.type && exceptions.indexOf(res.data.type) !== -1)
						root.$refs.error.show(res.data)
					//root.$refs.report.show(res.data)
					return Promise.reject(error)
				} else {
					//root.$refs.error.show(res.data || res.statusText)
					return Promise.reject(error)
				} 
			} else { // for example "Network Error" - doesn't work with spaces in translations.json
				console && console.error('Strange AJAX error', error)
				var message = error.message.replace(' ', '_')
				if (message !== 'Request_aborted') root.$refs.error.show({ type: message }) 
				return Promise.reject(error)
			}
		}
		
		function routing() {
			var router = VueRouter.createRouter({
				history: VueRouter.createWebHashHistory(),
				linkActiveClass: 'active',
				routes: [
					{ path: '/', redirect: '/flow/auth/start' },
					{ path: '/start-process', name: 'start-process', component: () => {
						return axios.get('components/process/external-start-process.html').then(function(html) {
							return { 
								template: html, 
								data: function() {
									return { 
										startParamUrl: '',
										selectedProcess: {},
										started: false
									}			
								},
								mounted: function() {
									AuthService.createAnonUserToken().then(user => {
										this.$root.user = user
										axios.defaults.headers.common.authorization = user.authToken
										if (this.$route.query.processKey) {
						    				ProcessService.findProcessByDefinitionKey(this.$route.query.processKey).then(processLatest => {
												this.selectedProcess = processLatest
												ProcessService.startForm(processLatest.id).then(url => {
													if (url.key) {
														this.startParamUrl = this.$root.config.uiElementTemplateUrl + '/startform/' +
															url.key.split('?template=')[1] + '?processDefinitionId=' + processLatest.id + 
															'&processDefinitionKey=' + processLatest.key
													}
												})
											})
										}
									})
								},
								methods: {
									taskCompleted: function() {
										this.started = true
									},
									navigateBack: function() {
										history.back()
									}
								}
							}
						})
					}},				
					{ path: '/seven', component: { template: '<seven></seven>' }, children: [
						{ path: 'login', name: 'login', beforeEnter: function(to, from, next) {
								configRequest.then(function(config) {
									if (config.ssoActive) //If SSO go to other login
										location.href = 'sso-login.html?nextUrl=' + encodeURIComponent(to.query.nextUrl ? to.query.nextUrl : '')
									else next()
								})
							}, component: () => { 
							return axios.get('components/login.html').then(function(html) {
								return { 
									template: html, 
									data: function() {
										return { 
											credentials : { 
												username: null,
												password: null,
												type: 'de.cib.cibflow.rest.StandardLogin'
											}
										}			
									},
									methods: {
					    				onSuccess: function(user) {
					    					AuthService.fetchAuths().then(permissions => {
					    						user.permissions = permissions
												this.$root.user = user
												this.$route.query.nextUrl ? this.$router.push(this.$route.query.nextUrl) : 
													this.$router.push('/flow/auth/start') 
					    					})		    					
										},
					    				onForgotten: function() {
											this.$router.push('/flow/password-recover') 	    					
										}									
									}
								}
							})
						}},
						{ path: 'password-recover', beforeEnter: checkCamundaUserProvider(), component: {
			        			template: '<password-recover></password-recover>'
						} },					
						{ path: 'auth', beforeEnter: authGuard(true), component: { 
							template: '<loader ref="loader" class="d-flex justify-content-center" styling="width:20%">\
								<router-view ref="down" class="w-100 h-100"></router-view></loader>',
							mixins: [permissionsMixin],
							inject: ['loadProcesses'],
							mounted: function() {
								this.$refs.loader.done = true
								this.$refs.loader.wait(this.loadProcesses(false))
								// Preload the filters to have them in the admin view.
								this.$store.dispatch('findFilters').then(response => {
									this.$store.commit('setFilters', 
										{ filters: this.filtersByPermissions(this.$root.config.permissions.displayFilter, response) })
								})
							},
						}, children: [
							{ path: 'start', name: 'start', component: () => { 
								return axios.get('components/start/start.html').then(function(html) {
									return { 
										template: html, 
										mixins: [permissionsMixin],
										inject: ['loadProcesses'],
										data: function() { return { showAdminOptions: false, items: [] } },
										computed: {
											startableProcesses: function() {
												return this.processesFiltered.find(p => { return p.startableInTasklist })
											},
											countStartItems: function () {
								                return this.items.length;
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
											}	         
										},
										mounted() {
								            this.items = this.$refs.startContainer.children
								        },
										methods: {
											startProcess: function () {
												this.$refs.startProcess.show()
												this.loadProcesses(false)
											}
										}
									}
								})
							}},
					        { path: 'account/:userId', name: 'account', beforeEnter: (to, from, next) => {
									permissionsDeniedGuard('userProfile')(to, from, result => {
							    		if (result) next(result) 
										else {
								      		if (to.params.userId && to.params.userId === root.user.id && 
												root.config.layout.showUserSettings)next()
									      	else next('/flow/auth/start')
								    	}
								  	})
								}, component: { 
									template: '<profile-user></profile-user>'
							}},
					        { path: 'modeler/:processId?', name: 'modeler', beforeEnter: permissionsGuard('modeler'), component: {
			        			template: '<modeler :processId="$route.params.processId"></modeler>'
							}},
					        { path: 'easy-form', name: 'easyForm', beforeEnter: permissionsGuard('easyform'), component: {
			        			template: '<easy-form></easy-form>'
							}},
					        { path: 'flow-resource', name: 'flowResource', beforeEnter: permissionsGuard('flowresource'), component: {
			        			template: '<flow-resource></flow-resource>'
							}},	
							{ path: 'flow-process-management', name: 'flowProcessManagement', 
								beforeEnter: permissionsGuard('flowprocessmanagement'), component: {
			        			template: '<flow-process-management></flow-process-management>'
							}},		
							{ path: 'management/:processId', name: 'management', beforeEnter: permissionsGuard('cockpit'), component: { 
								template: '<management :processId="$route.params.processId"></management>'
							}},
					        { path: 'tasks', beforeEnter: permissionsGuard('tasklist'), component: { 
						        	template: '<tasks ref="tasks"></tasks>',
									watch: {
										'$route.query.filtername': function(filtername) {
											if (filtername) {
												if (this.$store.state.filter.list) {
													var taskId = this.$route.params.filterId
													var filter = this.$store.state.filter.list.find(filter => {
														return filtername.toUpperCase() === filter.name.toUpperCase() 
													})
													if (filter) {
														this.$router.push('/flow/auth/tasks/' + filter.id + '/' + taskId)
													}
												}
											}
										}
									}
				        		}, 
								children: [
									{ path: ':filterId/:taskId?', name: 'tasklist', component: {
					        			template: '<task ref="task" :task="task" @complete-task="$emit(\'complete-task\', $event)"\
					        				@update-assignee="$emit(\'update-assignee\', $event)" @update-task="$emit(\'update-task\', $event)"></task>',
					        			props: { task: Object }
					        		}}
				        		]
				        	},
					        { path: 'processes', name: 'processes', beforeEnter: permissionsGuard('cockpit'), component: {
			        			template: '<transition name="slide-in" mode="out-in">\
			        				<loader v-if="$store.state.process.list === null" class="h-100 d-flex justify-content-center" styling="width:20%"></loader>\
			        				<processes v-else></processes></transition>',
			        			inject: ['isMobile'],
							}},
					        { path: 'process-management', name: 'processManagement', beforeEnter: permissionsGuard('cockpit'), component: {
			        			template: '<transition name="slide-in" mode="out-in">\
			        				<loader v-if="$store.state.process.list === null" class="h-100 d-flex justify-content-center" styling="width:20%"></loader>\
			        				<process-management v-else></process-management></transition>'
							}},
							{ path: 'deployments/:deploymentId?', name: 'deployments', beforeEnter: permissionsGuard('cockpit'), component: { 
								template: '<deployments></deployments>'
							}},
				        	{ path: 'admin', component: { 
								template: '<router-view></router-view>'
							}, 
							children: [
								{ path: 'users-management', name: 'usersManagement', component: () => { 
									return axios.get('components/admin/users-management.html').then(function(html) {
										return { 
											template: html, 
											mixins: [permissionsMixin]
										}
									})
								}},
								{ path: 'users', name:'adminUsers', 
									beforeEnter: permissionsGuardUserAdmin('usersManagement', 'user'), component: {
				        			template: '<admin-users></admin-users>'
				        		}},
				        		{ path: 'user/:userId', 
				        			beforeEnter: permissionsGuardUserAdmin('usersManagement', 'user'), component: {
				        			template: '<profile-user :editMode=true></profile-user>'
				        		}},
				        		{ path: 'groups', name:'adminGroups', 
				        			beforeEnter: permissionsGuardUserAdmin('groupsManagement', 'group'), component: {
				        			template: '<admin-groups></admin-groups>'
				        		}},
				        		{ path: 'group/:groupId', 
				        			beforeEnter: permissionsGuardUserAdmin('groupsManagement', 'group'), component: {
				        			template: '<profile-group></profile-group>'
				        		}},
								{ path: 'authorizations', name:'authorizations', 
									beforeEnter: permissionsGuardUserAdmin('authorizationsManagement', 'authorization'), component: {
				        			template: '<admin-authorizations></admin-authorizations>'
				        		}, 
									children: [
						        		{ path: ':resourceTypeId/:resourceTypeKey', name:'authorizationType', component: {
											template: '<admin-authorizations-table></admin-authorizations-table>'
						        		} }
					        		]
					        	}
							] 
						},
		        		{ path: 'admin/create-user', name: 'createUser', component: {
		        			template: '<create-user></create-user>'
		        		}},
		        		{ path: 'admin/create-group', name: 'createGroup', beforeEnter: permissionsGuard('cockpit'), component: {
		        			template: '<create-group></create-group>'
		        		}}
						]}
					]}
				]
			})
			return router
			
			function checkCamundaUserProvider() {
				return function(to, from, next) {
					configRequest.then(function(config) {
						if (config.userProvider === 'de.cib.cibflow.auth.CamundaUserProvider') next()
					})				
				}
			}
			
			function authGuard(strict) {
				return function(to, from, next) {
					console && console.debug('navigation guard', from, to)
					if (root.user) next()
					else getSelfInfo()['catch'](error => {
						if (error.response) {
							var res = error.response
							var params = res.data.params && res.data.params.length > 0
							if (res.data && res.data.type === 'TokenExpiredException' && params) {
								console && console.info('Prolonged token')
								if (sessionStorage.getItem('token')) sessionStorage.setItem('token', res.data.params[0])  
								else if (localStorage.getItem('token')) localStorage.setItem('token', res.data.params[0])
								getSelfInfo()
							} else {
								console && console.warn('Not authenticated, redirecting ...')			
								sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : localStorage.removeItem('token') 
								next({ path: strict ? '/flow/login' : undefined, query: { nextUrl: to.fullPath } })
								if ((res.data.type !== 'AuthenticationException' && res.data.type !== 'TokenExpiredException') || params)
									root.$refs.error.show(res.data) // When reloading $refs.error is often undefined => init race condition ?
							}						
					    } else
					    	console && console.error('Strange AJAX error', error)
					})
					
					function getSelfInfo() {
						if (to.query.token) sessionStorage.setItem('token', to.query.token)
						var token = sessionStorage.getItem('token') || localStorage.getItem('token')
						var inst = axios.create() // bypass standard error handling
						return inst.get('auth', { headers: { authorization: token } }).then(res => {
							console && console.info('auth successful', res.data)
							axios.defaults.headers.common.authorization = res.data.authToken
	    					AuthService.fetchAuths().then(permissions => {
	    						res.data.permissions = permissions
								root.user = res.data
								next()
	    					})
						})
					}
				}
			}
			function permissionsGuard(permission) {
				return function(to, from, next) {
					//Custom access permission handler for 'Start Process' option
					var perm = to.fullPath === '/flow/auth/processes' ? 'tasklist' : permission
					if (root.applicationPermissions(root.config.permissions[perm], perm)) next()
					else next('/flow/auth/start')
				}
			}
			function permissionsDeniedGuard(permission) {
				return function(to, from, next) {
					//Custom access permission handler for 'Start Process' option
					var perm = to.fullPath === '/flow/auth/processes' ? 'tasklist' : permission
					if (!root.applicationPermissionsDenied(root.config.permissions[perm], perm)) next()
					else next('/flow/auth/start')
				}
			}
			function permissionsGuardUserAdmin(permission, condition) {
				return function(to, from, next) {
					if (root.adminManagementPermissions(root.config.permissions[permission], condition)) next()
					else next('/flow/auth/start')
				}
			}
		}
	
		return config
	})
		
	function isMobile() {
		if ((platform.os.family === 'Android') || (platform.os.family === 'iOS')) return true
		else return false
	}
	
	function registerOwnComponents(app) {
		app.component('quick-nav-bar', QuickNavBar)
		app.component('sidebar', Sidebar)
		app.component('sidebar-item', SidebarItem)
		app.component('sidebar-dropright', SidebarDropright)
		app.component('password-recover', PasswordRecover)
		app.component('bpmn-viewer', BpmnViewer)
		app.component('seven', Seven)
		app.component('filterable-select', FilterableSelect)
		app.component('sidebar-element-group', SidebarElementGroup)
		app.component('smart-search', SmartSearch)
		app.component('sidebars-flow', SidebarsFlow)
		app.component('icon-button', IconButton)
		app.component('multisort-modal', MultisortModal)
		app.component('login-flow', LoginFlow)
		app.component('tasks', Tasks)
		app.component('tasks-nav-bar', TasksNavBar)
		app.component('task', Task)
		app.component('advanced-search-modal', AdvancedSearchModal)
		app.component('task-details-sidebar-all', TaskDetailsSidebarAll)
		app.component('task-details-sidebar', TaskDetailsSidebar)
		app.component('task-details-sidebar-chat', TaskDetailsSidebarChat)
		app.component('task-details-sidebar-status', TaskDetailsSidebarStatus)
		app.component('camunda-filter', CamundaFilter)
		app.component('filter-modal', FilterModal)
		app.component('filter-nav-bar', FilterNavBar)
		app.component('filter-nav-collapsed', FilterNavCollapsed)
		app.component('process', Process)
		app.component('instances-table', InstancesTable)
		app.component('process-variables-sidebar', ProcessVariablesSidebar)
		app.component('process-variables-table', ProcessVariablesTable)
		app.component('process-details-sidebar', ProcessDetailsSidebar)
		app.component('processes', Processes)
		app.component('process-card', ProcessCard)
		app.component('process-advanced', ProcessAdvanced)
		app.component('process-table', ProcessTable)
		app.component('user-tasks-table', UserTasksTable)
		app.component('task-assignation-modal', TaskAssignationModal)
		app.component('process-management', ProcessManagement)
		app.component('management', Management)
		app.component('deployments', Deployments)
		app.component('resources-nav-bar', ResourcesNavBar)
		app.component('deployment-list', DeploymentList)
		app.component('render-template', RenderTemplate)
		app.component('start-process', StartProcess)
		app.component('admin-users', AdminUsers)
		app.component('admin-groups', AdminGroups)
		app.component('admin-authorizations', AdminAuthorizations)
		app.component('admin-authorizations-table', AdminAuthorizationsTable)
		app.component('authorizations-nav-bar', AuthorizationsNavBar)
		app.component('create-user', CreateUser)
		app.component('create-group', CreateGroup)
		app.component('profile-user', ProfileUser)
		app.component('profile-group', ProfileGroup)
		app.component('modeler', Modeler)
		app.component('easy-form', EasyForm)
		app.component('flow-resource', FlowResource)
		app.component('flow-process-management', FlowProcessManagement)
		app.component('support-modal', SupportModal)
		app.component('status-progress-bar', StatusProgressBar)
		app.component('add-variable-modal', AddVariableModal)
		
		app.directive('b-toggle', BToggle(eventBus))
		app.directive('b-popover', BDPopover)
		app.component('b-button', BButton)
		app.component('b-button-close', BButtonClose)
		app.component('b-collapse', BCollapse)
		app.component('b-dropdown', BDropdown)
		app.component('b-dropdown-divider', BDropdownDivider)
		app.component('b-dropdown-item', BDropdownItem)
		app.component('b-dropdown-item-button', BDropdownItemButton)
		app.component('b-dropdown-group', BDropdownGroup)
		app.component('b-dropdown-form', BDropdownForm)
		app.component('b-progress', BProgress)
		app.component('b-progress-bar', BProgressBar)
		app.component('b-navbar', BNavbar)
		app.component('b-navbar-toggle', BNavbarToggle)
		app.component('b-navbar-brand', BNavbarBrand)
		app.component('b-navbar-nav', BNavbarNav)
		app.component('b-nav-item-dropdown', BNavItemDropdown)
		app.component('b-form-group', BFormGroup)
		app.component('b-form-checkbox', BFormCheckbox)
		app.component('b-list-group', BListGroup)
		app.component('b-list-group-item', BListGroupItem)
		app.component('b-card', BCard)
		app.component('b-card-body', BCardBody)
		app.component('b-card-text', BCardText)
		app.component('b-alert', BAlert)
		app.component('b-popover', BPopover)
		app.component('b-modal', BModal)
		app.component('b-form-textarea', BFormTextarea)
		app.component('b-form-input', BFormInput)
		app.component('b-form-select', BFormSelect)
		app.component('b-input-group', BInputGroup)
		app.component('b-input-group-text', BInputGroupText)
		app.component('b-input-group-prepend', BInputGroupPrepend)
		app.component('b-input-group-append', BInputGroupAppend)
		app.component('b-spinner', BSpinner)
		app.component('b-pagination', BPagination)
		app.component('b-overlay', BOverlay)
		app.component('clipboard', Clipboard)
		app.component('task-list', TaskList)
		app.component('flow-table', FlowTable)
		app.component('b-form', BForm)
		app.component('b-dd-item-btn', BDdItemBtn)
		app.component('b-badge', BBadge)
		app.component('b-form-tag', BFormTag)
		app.component('b-calendar', BCalendar)
		app.component('b-form-timepicker', BFormTimepicker)
		app.component('b-form-row', BFormRow)
		app.component('b-col', BCol)
		app.component('b-form-datepicker', BFormDatepicker)
		app.component('b-avatar', BAvatar)
		app.component('b-form-select-option', BFormSelectOption)
		app.component('b-form-file', BFormFile)
		app.component('b-tab', BTab)
		app.component('b-tabs', BTabs)
		app.component('b-link', BLink)
		app.component('error', Error)
		app.component('confirm', Confirm)
		app.component('problem-report', ProblemReport)
		app.component('loader', Loader)
		app.component('success', Success)
		app.component('task-popper', TaskPopper)
		app.component('cib-header-flow', CIBHeaderFlow)
		app.component('sidebars', Sidebars)
		app.component('reset-dialog', ResetDialog)
		app.component('otp-dialog', OtpDialog)
		app.component('cib-form', CIBForm)
		app.component('cib-datepicker2', CIBDatepicker2)
		app.component('secure-input', SecureInput)
		
		// ALIASES
		app.component('b-dd', app.component('b-dropdown'))
		app.component('b-dd-form', app.component('b-dropdown-form'))
		
		app.directive('hover-style', HoverStyle)
		app.directive('block-truncate', {
		    inserted: function(el) {
				// Check if the block's height is smaller than the text content height. If so
		        // add an ellipsis replacing the last word
			    while (el.clientHeight < el.scrollHeight) {
					el.innerHTML = el.innerHTML.replace(/\W*\s(\S)*$/, '...')
			    }
		    },
			update: function(el, binding) {
				el.innerHTML = binding.value.text
				while (el.clientHeight < el.scrollHeight) {
					el.innerHTML = el.innerHTML.replace(/\W*\s(\S)*$/, '...')
			    }
			}
		})
	}
