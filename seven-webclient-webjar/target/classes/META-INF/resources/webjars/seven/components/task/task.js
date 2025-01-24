/* globals window, setTimeout, localStorage, setInterval, 
	clearInterval, moment, permissionsMixin, clearTimeout, document, usersMixin */
	
	import { permissionsMixin } from '../../permissions.js';
	import { usersMixin } from '../users-mixin.js';
	import { TaskService, AdminService, ProcessService, HistoryService, AuthService } from '../../services.js';
	
	function debounce(delay, fn) {
		var timeoutID = null
		return function() {
			clearTimeout(timeoutID)
			var args = arguments
			var self = this
			timeoutID = setTimeout(() => { fn.apply(self, args) }, delay)
		}
	}
	
	const Tasks = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/task/tasks.html').then(function(html) {
			return {
    			template: html,
                inject: ['isMobile'],
				mixins: [permissionsMixin],
	        	data: function () {
					var leftOpenFilter = localStorage.getItem('leftOpenFilter') ? 
						localStorage.getItem('leftOpenFilter') === 'true' : true
					var externalMode = window.location.href.includes('externalMode') ? true : false
					if (externalMode) leftOpenFilter = false
					return {						
						leftOpenFilter: leftOpenFilter,    				
						leftOpenTask: !externalMode,
	    				rightOpenTask:  localStorage.getItem('rightOpenTask') === 'true' && this.canOpenRightTask(),
						tasks: [],
						task: null,
						processInstanceHistory: null,
						processesInstances: null, //Only needed to fetch the businessKey of every instance.
						filter: null,
						interval: null,
						filterMessage: '',
						filterName: '',
						nTasksShown: 0,
						nFiltersShown: 0,
						tasksNavbarSizes: [[12, 6, 4, 4, 3], [12, 6, 4, 5, 4], [12, 6, 4, 6, 5]],
						tasksNavbarSize: 0,
						taskResultsIndex: 0,
						search: ''
    				} 
				},
				computed: {
					rightCaptionTask: function() {
						if (this.canOpenRightTask())
							return this.$t('task.options')
						return null					
					},
					leftCaptionTask: function() {
						return this.$store.state.filter.selected.name
					},
					leftCaptionFilter: function() {
						return this.leftOpenTask ? this.$t('seven.filters') : ''
					},
					getTasksNavbarSize: function() { return this.tasksNavbarSizes[this.tasksNavbarSize] }
				},
				watch: {
					rightOpenTask: function(newVal) {
						localStorage.setItem('rightOpenTask', newVal)
					},
					'$route.params.taskId': function() { if (!this.$route.params.taskId) this.cleanSelectedTask() },
					'$route.params.filterId': function() { if (!this.$route.params.filterId) this.cleanSelectedFilter() },
					leftOpenTask: function(leftOpen) {
						if (leftOpen) {
							this.leftOpenFilter = !localStorage.getItem('leftOpenFilter') || localStorage.getItem('leftOpenFilter') === 'true'
						} else this.leftOpenFilter = false 
					},
					leftOpenFilter: function() {
						if (this.leftOpenTask) localStorage.setItem('leftOpenFilter', this.leftOpenFilter) 
					},
					search: debounce(800, function() { this.listTasksWithFilter() })
				},
    			mounted: function() {					
					var taskSorting = localStorage.getItem('taskSorting') ? JSON.parse(localStorage.getItem('taskSorting')) : {}
					if (!localStorage.getItem('taskSorting') || 
						(localStorage.getItem('taskSorting') && (taskSorting.sorting || taskSorting.order))) {							
						localStorage.setItem('taskSorting', JSON.stringify(this.$root.config.taskSorting.default))
					}
					this.taskResultsIndex += this.$root.config.maxTaskResults
    				this.setIntervalTaskList()
				},
				methods: {
					canOpenRightTask: function() {
						return (this.$root.config.layout.showTaskDetailsSidebar ||
								this.$root.config.layout.showChat ||
								this.$root.config.layout.showStatusBar)
					},
					setIntervalTaskList: function() {
						if (this.$root.config.taskListTime !== '0') {
	    					this.interval = setInterval(() => { 
	    						this.listTasksWithFilterAuto()
								if (this.task) this.checkActiveTask()
	    					}, this.$root.config.taskListTime)
	    				}
					},
					listTasksWithFilter: function() {
						this.tasks = []
						this.processesInstances = []
						this.nTasksShown = 0
						if (this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = false
						this.fetchTasks(0, this.taskResultsIndex)
					},
					listTasksWithFilterAuto: function(showMore) {
						if (this.$route.params.filterId) {
							if (showMore) this.$refs.navbar.$refs.taskLoader.done = false
							var firstResult = showMore ? this.taskResultsIndex : 0
							var maxResults = showMore ? this.$root.config.maxTaskResults : this.taskResultsIndex
							if (this.$store.state.filter.selected.id) {
								this.fetchTasks(firstResult, maxResults, showMore)
							}
						} else {
							if (this.$refs.navbar && this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = true
						}
					},
					fetchTasks: function(firstResult, maxResults, showMore) {
						var taskSorting = [JSON.parse(localStorage.getItem('taskSorting'))]
						//If necessary we add the created extra sorting so the data is well sorted
						if (taskSorting[0].sortBy !== 'created') taskSorting.push({ sortBy: 'created', sortOrder: 'desc' })
						var filters = { sorting: taskSorting }
						if (this.search) {
							filters.orQueries = [
								{ 
									nameLike: '%' + this.search + '%', 
									assigneeLike: '%' + this.search + '%', 
								 	processDefinitionId: this.search, 
									processInstanceBusinessKeyLike: '%' + this.search + '%'
								}
							]
						}
						if (this.$root.config.taskFilter.advancedSearch.filterEnabled) {
							this.$store.dispatch('loadAdvancedSearchData')
						}
						AuthService.fetchAuths().then(permissions => {
							this.$root.user.permissions = permissions
							var advCriterias = this.$store.state.advancedSearch.criterias
							if (advCriterias.length > 0) {
								if (this.$store.state.advancedSearch.matchAllCriteria === true) {									
									for (var key in this.$store.getters.formatedCriteriaData) {
									    if (this.$store.getters.formatedCriteriaData.hasOwnProperty(key)) { 
											filters[key] = this.$store.getters.formatedCriteriaData[key]
									    }
									}									
								}
								else {
									if (!filters.orQueries) filters.orQueries = []
									filters.orQueries.push(this.$store.getters.formatedCriteriaData)
								}
							}
							TaskService.findTasksByFilter(this.$store.state.filter.selected.id, filters, 
								{ firstResult: firstResult, maxResults: maxResults }).then(result => {
								var tasks = this.tasksByPermissions(this.$root.config.permissions.displayTasks, result)
								TaskService.findTasksCountByFilter(this.$store.state.filter.selected.id, filters).then(count => {
									this.nTasksShown = count
								})
								//Only needed to fetch the businessKey of every instance.
								this.updateProcessesInstances(tasks, showMore)
							}, () => {
								if (this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = true
							})
    					})	
					},				
					updateTask: function(updatedTask) {
						this.processInstanceHistory.tasksHistory[0].due = updatedTask.due
						var index = this.tasks.findIndex(task => {
							return task.id === updatedTask.id
						})
						//Because of the longpoll this.task is not necessarily the same object like in this.tasks
						this.tasks.splice(index, 1, updatedTask)
						this.listTasksWithFilterAuto() 
					},
					updateAssignee: function(assignee, target) {
						if (this.processInstanceHistory) this.processInstanceHistory.tasksHistory[0].assignee = assignee
						if (target === 'taskList') {
							var currentTaskIndex = this.tasks.findIndex(task => {
								return task.id === this.task.id
							})
							if (currentTaskIndex !== -1) this.tasks[currentTaskIndex].assignee = assignee
						}
						this.listTasksWithFilterAuto() 
					},
					completedTask: function(task) {
						this.tasks = this.tasks.filter(t => { return t.id !== task.id })
						this.$refs.completedTask.show(2)
						this.processInstanceHistory = null
						this.listTasksWithFilterAuto()
						this.checkAndOpenTask(JSON.parse(JSON.stringify(this.task)))
						this.task = null
					},
					checkAndOpenTask: function(task, started) {
						if (this.$root.config.automaticallyOpenTask) this.openTaskAutomatically(task, started)
					},
					openTaskAutomatically: function(task, started) {
						var counter = 0
						const intervalTime = 2500
						const maxExecutions = 3
						var method = started ? HistoryService.findProcessInstance(task.processInstanceId) : 
							HistoryService.findTasksByTaskIdHistory(task.id)
						method.then(response => {
							var data = Array.isArray(response) ? response[0] : response
							if (data) {
								var intervalId = setInterval(() => {
									counter++
									if (counter > maxExecutions) clearInterval(intervalId)
								    else this.openTask(data, intervalId)
								}, intervalTime)
								counter++
								this.openTask(data, intervalId)
							}
						})
					},
					openTask: function(resOrin, intervalId) {
						var createdAfter = resOrin.endTimeOriginal || resOrin.startTimeOriginal
						if (createdAfter) createdAfter = moment(createdAfter).subtract(5, 'seconds').format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
						TaskService.findTasksByProcessInstanceAsignee(null, createdAfter).then(tasks => {
							if (tasks.length > 0) {
								var taskRedirect = tasks.find(t => t.processInstanceId === resOrin.rootProcessInstanceId)
								if (taskRedirect) {
									clearInterval(intervalId)
									this.$router.push('/flow/auth/tasks/' + this.$store.state.filter.selected.id + '/' + taskRedirect.id)
								} else {
									this.openTaskByTaskInstance(tasks, tasks.shift(), resOrin, intervalId)
								}
							}
						})
					},
					openTaskByTaskInstance: function(tasks, task, resOrin, intervalId) {
						if (task) {
							HistoryService.findTasksByTaskIdHistory(task.id).then(taskH => {
								if (taskH[0]) {
									if (taskH[0].rootProcessInstanceId === resOrin.rootProcessInstanceId) {
										clearInterval(intervalId)
										this.$router.push('/flow/auth/tasks/' + this.$store.state.filter.selected.id + '/' + taskH[0].id)
									} else {
										this.openTaskByTaskInstance(tasks, tasks.shift(), resOrin, intervalId)
									}
								} else {
									this.openTaskByTaskInstance(tasks, tasks.shift(), resOrin, intervalId)
								}
							})
						}
					},
					selectedTask: function(task) {
						this.task = task
						let defaultTitle = 'CIB flow'
						if (this.$root.config.productNamePageTitle) defaultTitle = this.$root.config.productNamePageTitle
						document.title = defaultTitle +' | ' + this.$t('start.taskList') + ' | ' + task.name
						if (this.isMobile()) {
							this.leftOpenTask = false
						}
						// Only needed when the task side detail is load.
						if (this.$root.config.layout.showTaskDetailsSidebar) {
							ProcessService.findProcessInstance(this.task.processInstanceId).then(instance => {
								HistoryService.findTasksByProcessInstanceHistory(this.task.processInstanceId).then(tasksHistory => {
									this.processInstanceHistory = instance
									this.processInstanceHistory.tasksHistory = tasksHistory
								})													
							})
						}
					},
					selectedFilter: function() {
						this.task = null
						this.listTasksWithFilter() 
					},
					displayPopover: function(evt) {
						if (this.$refs.down) 
							this.$refs.down.$refs.task.displayPopover = localStorage.getItem('showPopoverHowToAssign') === 'false' ? false : evt
					},
					showFilterAlert: function(evt) {
						this.filterMessage = evt.message
						this.filterName = evt.filter
						this.$refs.filter.show(2)
					},
					updateProcessesInstances: function(tasks, showMore) {
						if (tasks && tasks.length > 0) {
							var processesInstancesIds = []
							tasks.forEach(task => {
								processesInstancesIds.push(task.processInstanceId)
							})
							// TODO -> processInstanceIds needs to be removed from here, this link the webclient with Camunda and this needs to be
							// transparent.
							ProcessService.findCurrentProcessesInstances({ processInstanceIds : processesInstancesIds }).then(instances => {
								this.processesInstances = instances
								tasks.forEach(task => {
									var instance = this.processesInstances.find(p => {
										return p.id === task.processInstanceId
									})
									if (instance) task.businessKey = instance.businessKey
								})
								this.tasks = showMore ? [...this.tasks, ...tasks] : tasks
								this.checkActiveTask()
								if (this.$refs.navbar && this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = true
							}, () => { 
								this.tasks = showMore ? [...this.tasks, ...tasks] : tasks
								this.checkActiveTask()
								if (this.$refs.navbar && this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = true
							})
						} else {
							this.tasks = showMore ? [...this.tasks, ...tasks] : tasks
							this.checkActiveTask()
							if (this.$refs.navbar && this.$refs.navbar.$refs.taskLoader) this.$refs.navbar.$refs.taskLoader.done = true
						} 
					},
					cleanSelectedTask: function() {
						this.processInstanceHistory = null
						this.task = null
						if (this.$route.params.filterId) {
							this.listTasksWithFilterAuto()
							var path = '/flow/auth/tasks/' + this.$route.params.filterId
							if (path !== this.$route.path) this.$router.push(path)	
						}
					},
					cleanSelectedFilter: function() {
						this.$store.state.filter.selected = { 
							id: null,
							resourceType: 'Task', 
							name: '',
							owner: null, 
							query: {}, 
					        properties: {
					            color: '#555555',
					            showUndefinedVariable: false,
					            description: '',
					            refresh: true,
					            priority: 50
					        }
						}
						this.tasks = []
						this.cleanSelectedTask()
					},
					manageNavbarSize: function() {
						if (this.tasksNavbarSize === 2) {
							this.leftOpenTask = false
							this.tasksNavbarSize = 0
						}
						else this.tasksNavbarSize++	
					},
					collapseNavbar: function () {
						this.leftOpenTask = false
						this.tasksNavbarSize = 0
					},
					showMore: function() {
						this.listTasksWithFilterAuto(true)
						this.taskResultsIndex += this.$root.config.maxTaskResults
					},
					navigateRegion: function(region) {
						if (['regionFilter', 'regionTasks'].includes(region)) {
							this.$refs[region].$refs.leftSidebar.focus()
						} else {
							if (this.$refs.down) this.$refs.down.$refs.task.$refs.titleTask.focus() 
						}
					},
					checkActiveTask: function() {
						if (this.task) {
							var task = this.tasks.find(t => { return t.id === this.task.id })
							if (!task) {
								clearInterval(this.interval)
								TaskService.checkActiveTask(this.task.id, this.$root.user.authToken).catch(() => {
									if (this.$route.params.taskId === this.task.id) {
										if (this.$router.currentRoute.path !== '/flow/auth/tasks/' + this.$route.params.filterId){
											this.$router.push('/flow/auth/tasks/' + this.$route.params.filterId)
										}
										//this.$router.push('/flow/auth/tasks/' + this.$route.params.filterId)
										this.setIntervalTaskList()
									}
//									this.$refs.confirmTaskClosed.show()
								})
							}
						}
					},
					handleKeydown: function(event) {
						if (event.altKey) {
					        const keyRegionMap = {
					            '1': 'regionFilter',
					            '2': 'regionTasks',
					            '3': 'regionTask',
					        }
					        const region = keyRegionMap[event.key]
					        if (region) {
					            this.navigateRegion(region)
					        }
					    }
			        }
				},
				beforeRouteLeave: function(to, from, next) {
					clearInterval(this.interval)
					next()
				}
    		}
        })
	})
	
	const TasksNavBar = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/task/tasks-nav-bar.html').then(function(html) {
			return { 
				template: html,
				props: { tasks: Array, taskResultsIndex: Number },
                inject: ['currentLanguage'],
				data: function () {
					return { 
						currentSorting: {},
						showTasks: true,
						selected: null,
						mode: 'create',
						focused: null,
						taskSorting: { sortBy: null, sortOrder: 'desc' },
						selectedDateT: {},
						selectedFilter: '',
						pauseRefreshButton: false,
						advancedFilter: [],
						advancedFilterAux: null
					}
				},
				watch: {
					tasks: function() { this.checkTaskIdInUrl(this.$route.params.taskId) },
					'$route.params.taskId': function(to) { this.checkTaskIdInUrl(to) },
					'advancedFilter': {
						deep: true,
						handler: function (newValues) {
							if (JSON.stringify(newValues) !== this.advancedFilterAux) {
								this.updateAdvancedFilters()
					        }
						}
					}
				},
				computed: {
					tasksFiltered: function() {
						var tasks = []
						if (this.tasks) {
							tasks = this.tasks.filter(task => {
								var reminder = task.followUp && moment(task.followUp)
								var dueDate = task.due && moment(task.due)
								if (!this.$store.state.filter.settings.reminder && !this.$store.state.filter.settings.dueDate) return true
								return this.dateFitsFilter(reminder, this.$store.state.filter.settings.reminder) ||
									this.dateFitsFilter(dueDate, this.$store.state.filter.settings.dueDate)
							})
						}
						return tasks
					},
					filteredFields() {
				    	return this.$root.config.taskSorting.fields.filter(item => this.showFields(item))
				  	}
				},
				created: function () {
					// Clean previous way of filtering TODO: Delete in a while 3 months.
					this.taskSorting = JSON.parse(localStorage.getItem('taskSorting'))
					if (this.$root.config.taskFilter.advancedSearch.filterEnabled) {
						this.loadAdvancedFilters()
					}
				},
				methods: {
					loadAdvancedFilters: function() {
						this.advancedFilter = []
						this.$root.config.taskFilter.advancedSearch.processVariables.forEach(pv => {
							var criteria = this.$store.state.advancedSearch.criterias
								.find(obj => obj.id === pv.key && (obj.operator === 'eq' || obj.operator === 'like'))
							var advancedFilterObj = {
								key: pv.key,
								variableName: pv.variableName, 
								displayName: pv.displayName,
								type: typeof pv.value === 'boolean' ? 'Boolean' : 'String',
								defaultValue: pv.value,
								operator: pv.operator
							}
							if (criteria) {
								advancedFilterObj.check = true
								advancedFilterObj.value = pv.type === 'Boolean' ? '' : criteria.value
								if (pv.operator === 'like') advancedFilterObj.value = advancedFilterObj.value.slice(1, -1)
							}
							else {
								advancedFilterObj.check = false 
								advancedFilterObj.value = pv.type === 'Boolean' ? '' : pv.value
							}
							this.advancedFilter.push(advancedFilterObj)
							this.advancedFilterAux = JSON.stringify(this.advancedFilter)
						})
					},
					updateAdvancedFilters: debounce(800, function() {
						var criterias = this.advancedFilter
							.filter(filterItem => filterItem.check && (filterItem.value || filterItem.type === 'Boolean'))
							.map(filterItem => {
								var value = filterItem.type === 'Boolean' ? filterItem.defaultValue : filterItem.value
								if (filterItem.operator === 'like') value = '%' + value + '%'
						        return {
									id: filterItem.key,
						            key: 'processVariables',
						            name: filterItem.variableName,
						            operator: filterItem.operator,
						            value: value
						        }
						    })

						this.$store.dispatch('updateAdvancedSearch', {
						    matchAllCriteria: true,
						    criterias: criterias
						})

						this.advancedFilterAux = JSON.stringify(this.advancedFilter)
						this.$emit('refresh-tasks')
					}),
					checkTaskIdInUrl: function(taskId) {
						if (this.tasks.length > 0 && taskId && this.$route.params.filterId !== '*') {
							var index = this.tasks.findIndex(task => {
								return task.id === taskId
							})
							if (index > -1) this.$emit('selected-task', this.tasks[index])
						}
					},
					getDateFormatted: function(date, format, emptyMsg) {
						if (!date) return this.$t('task.' + emptyMsg)
						if (format) return moment(date).format(format)
						else return moment(date).fromNow()
					},
					getDueClasses: function(task) {
						var classes = []
						if (!task.due) classes.push('text-muted')
						else if (moment(task.due).isBefore(moment())) classes.push('text-danger')
						else if (moment().add(this.$root.config.warnOnDueExpirationIn, 'hours').isAfter(moment(task.due))) classes.push('text-warning')
						if (task !== this.focused && task.id !== this.selectedDateT.id && !task.due) classes.push('invisible')
						return classes
					},
					getReminderClasses: function(task) {
						var classes = []
						if (!task.followUp) classes.push('text-muted')
						if (task !== this.focused && task.id !== this.selectedDateT.id && !task.followUp) classes.push('invisible')
						return classes
					},
					claim: function(task) {
						TaskService.setAssignee(task.id, this.$root.user.id).then(() => {
							task.assignee = this.$root.user.id
							this.$emit('update-assignee', task.assignee)
						})
					},
					checkAssignee: function(task) {
						TaskService.findTaskById(task.id).then(task => {
							if (task.assignee === null) this.claim(task)
							else this.$refs.confirmTaskAssign.show(task)
						})
					},
					selectedTask: function(task) {
						var selection = window.getSelection()
						var filterId = this.$store.state.filter.selected ? 
							this.$store.state.filter.selected.id : this.$route.params.filterId
						if (!selection.toString()) {
							var route = '/seven/auth/tasks/' + filterId + '/' + task.id
							if (this.$router.currentRoute.path !== route){
								this.$router.push(route)
							}							
						}
					},
					getProcessName: function(processDefinitionId) {
						if (processDefinitionId === null || !this.$root.config.layout.showProcessName) return ''
						var process = this.$store.state.process.list.find(item => {
							return (item.id.split(':')[0] === processDefinitionId.split(':')[0])
						})
						return process && process.name ? process.name : ''
					},
					getCompleteName: function(task) {
						if (this.$root.user.id.toLowerCase() === task.assignee.toLowerCase()) return this.$root.user.id // .displayName
						else {
							if (this.$store.state.user.listCandidates) {
								var user = this.$store.state.user.listCandidates.find(user => {
									return user.id.toLowerCase() === task.assignee.toLowerCase()
								})
								if ((user) && (user.displayName)) return user.displayName
							}
							return task.assignee
						}
					},					
					dateFitsFilter: function(date, filter) {
						return filter && date
					},
					setSorting: function(field) {
						if (field === 'order') {
							this.taskSorting.sortOrder = this.taskSorting.sortOrder === 'desc' ? 'asc' : 'desc'
						}
						localStorage.setItem('taskSorting', JSON.stringify(this.taskSorting))
						this.$emit('refresh-tasks')				
					},
					isInThePast: function(ymd, date) {
						return date < moment().startOf('day')
					},
					setDate: function(task, type) {
						task[type] = this.selectedDateT[type] ? moment(this.selectedDateT[type]).format('YYYY-MM-DDTHH:mm:ss.SSSZZ') : null
						TaskService.update(task)
						this.$refs[type + task.id][0].hide()
					},
					handleScrollTasks: function(el) {
						if (this.tasks.length < this.taskResultsIndex) return
						if (Math.ceil(el.target.scrollTop + el.target.clientHeight) >= el.target.scrollHeight) {
							this.$emit('show-more')
						}
					},
					setTime: function(time, type) {
						if (!this.selectedDateT[type]) return
						if (time) {							
							var timeSplit = time.split(':')
							this.selectedDateT[type].setHours(timeSplit[0])
							this.selectedDateT[type].setMinutes(timeSplit[1])
						} else {
							if (type === 'followUp') {
								this.selectedDateT.followUp.setHours(0,0,0,0)
							}
						}
					},
					copyTaskForDateManagement: function(task, type) {
						this.selectedDateT = JSON.parse(JSON.stringify(task))
						if (task[type]) {
							this.selectedDateT[type] = new Date(task[type])
							if (type === 'due') this.selectedDateT.dueTime = this.selectedDateT[type].getHours() + ':' + this.selectedDateT[type].getMinutes()
						}
					},
					pauseButton: function() {
						if (this.$root.config.pauseButtonTime) {
							this.pauseRefreshButton = true
							setTimeout(() => this.pauseRefreshButton = false, this.$root.config.pauseButtonTime)
						}
					},
					showFields: function (item) {
						if (item === 'followUpDate') {
							return this.$root.config.layout.showFilterReminderDate
						} else if (item === 'dueDate') {
							return this.$root.config.layout.showFilterDueDate
						} else {
							return true
						}
					}
				}
			}
		})
	})
		
	const Task = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/task/task.html').then(function(html) {
			return {
				template: html,
				mixins: [usersMixin],
                inject: ['isMobile'],
                props: { task: Object },
                data: function() { 
					return { 
						displayPopover: false,
						assignee: null,
						loadingUsers: false,
						candidateUsers: []
					} 
				},
                watch: {
                	'task.assignee': function(val) {
            			this.displayPopover = localStorage.getItem('showPopoverHowToAssign') === 'false' ? false : 
							val === null || this.task.assignee !== this.$root.user.id
					},
		        	assignee: function() {
		        		if (this.assignee != null) this.checkAssignee()
		        	},
					'task.id': function(taskId) {
						this.$store.commit('setCandidateUsers', [])
						this.$store.commit('setSearchUsers', [])
						this.loadIdentityLinks(taskId)
						this.$refs.titleTask.focus()
					}			
                },
				computed: {
					renderTemplateStyles: function() {
						if (this.task) {
							if ((this.task.assignee && this.task.assignee.toLowerCase() !== this.$root.user.id.toLowerCase()) || 
									(this.task.assignee == null && this.$root.config.notAssignedTaskLayer)) {
								return { filter: 'blur(1px)', '-webkit-filter': 'blur(1px)' }
							}
						}
						return {}
					}
				},
                mounted: function() {
					this.$store.commit('setCandidateUsers', [])
					this.$store.commit('setSearchUsers', [])
                	if (this.task.assignee === null || this.task.assignee !== this.$root.user.id) {
                		setTimeout(() => {
                			this.displayPopover = localStorage.getItem('showPopoverHowToAssign') === 'false' ? false : true
                		}, 1200)
                	}
					this.loadIdentityLinks(this.task.id)
				},
                methods: {
					loadIdentityLinks: function(taskId) {
						this.candidateUsers = []
						var promises = []
						TaskService.findIdentityLinks(taskId).then(identityLinks => {
							identityLinks.forEach(identityLink => {
								if (identityLink.type === 'candidate') {
									if (identityLink.groupId !== null) {
										var promise = AdminService.findUsers({ memberOfGroup: identityLink.groupId }).then(users => {
											this.$store.commit('setCandidateUsers', users)
											this.$store.commit('setSearchUsers', users)
										})
										promises.push(promise)
									}
									if (identityLink.userId !== null) {
										this.candidateUsers.push(identityLink.userId)
									}
								}
							})
							Promise.all(promises).then(() => {
								this.setAllUsersCandidates()
							})
						})
					},
					disablePopover: function() {
						localStorage.setItem('showPopoverHowToAssign', false)
						this.displayPopover = false
					},
					update: function() {
						this.$refs.ariaLiveText.textContent = ''
						TaskService.setAssignee(this.task.id, this.assignee).then(() => {
							this.task.assignee = this.assignee
							this.assignee = null
							this.$emit('update-assignee', this.task.assignee)
							if (this.task.assignee != null)
								this.$refs.ariaLiveText.textContent = this.$t('task.userAssigned', [this.getCompleteName])
							Vue.nextTick(() => {
								if (this.$refs.assignToMeButton) this.$refs.assignToMeButton.focus()
							})
						})
					},
					checkAssignee: function() {
						TaskService.findTaskById(this.task.id).then(task => {
							if (task.assignee === null) this.update()
							else this.$refs.confirmTaskAssign.show()
						})
					},
					setAllUsersCandidates: function() {
						if (!this.$root.config.layout.disableCandidateUsers) {
							this.$store.dispatch('findUsersByCandidates', { idIn: this.candidateUsers })
						}
					}
				}
			}
        })
	})
	
	const AdvancedSearchModal = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/task/advanced-search-modal.html').then(function(html) {
			return { 
				template: html,
				data: function () {
					return {		
						matchAllCriteria: true,
						criterias: [],
						selectedCriteriaKey: 'processVariables',
						selectedCriteriaValue: { name: null, operator: 'eq', value: '' },
				      	operators: [
							{ value: 'eq', text: '=' },
				        	{ value: 'neq', text: '!=' },
				        	{ value: 'gt', text: '>' },
				        	{ value: 'gteq', text: '>=' },
				        	{ value: 'lt', text: '<' },
				        	{ value: 'lteq', text: '<=' },
				        	{ value: 'like', text: 'like' },				        	
				        	{ value: 'notLike', text: 'not like' }
						],
						isValidForm: true		
					}
				},
				computed: {
					criteriaKeys: function() {
						var criteriaKeys = []
						this.$root.config.taskFilter.advancedSearch.criteriaKeys.forEach(item => {
							criteriaKeys.push({ value: item, text: this.$t('advanced-search.criteriaKeys.' + item) })
						})
						return criteriaKeys
					}
				},
				methods: {
					show: function() {
						if (this.$store.state.advancedSearch.criterias.length > 0) {
							this.matchAllCriteria = this.$store.state.advancedSearch.matchAllCriteria
							this.criterias = this.$store.state.advancedSearch.criterias.map(criteria => {
					        	return Object.assign({}, criteria)
					      	})
						}						
						this.$refs.advancedSearchModal.show()
					},
					addCriteria: function() {
						var value = this.selectedCriteriaValue.value
					    if (value === "true") value = true
					   	else if (value === "false") value = false
						this.criterias.push({
							key: this.selectedCriteriaKey,
				      		name: this.selectedCriteriaValue.name,
				      		operator: this.selectedCriteriaValue.operator,
				     		value: value
				    	})
						this.selectedCriteriaValue = { name: null, operator: 'eq', value: '' }
					},
					deleteCriteria: function(index) {
						this.criterias.splice(index, 1)
					},
					cleanAllCriteria: function() {
						this.criteria = []
					},
					handleSubmit: function() {
						this.$store.dispatch('updateAdvancedSearch', {
					        matchAllCriteria: this.matchAllCriteria,
					        criterias: this.criterias
				      	})
						this.$emit('refresh-tasks')
						this.$refs.advancedSearchModal.hide()
			    	},
			    	onModalHidden: function() {
						this.criterias = []
						this.matchAllCriteria = true
					}
				}
			}	
		})
	})
	
	export { Tasks, TasksNavBar, Task, AdvancedSearchModal }