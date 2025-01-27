/* globals Blob, moment */
	
	import { TaskService, ProcessService, HistoryService } from '../../services.js';
	
	function getStringObjByKeys(keys, obj) {
		var result = ''
		keys.forEach(key => {
			result += obj[key] + ';'
		})
		return result.slice(0, -1)
	}
	
	const Management = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/management/management.html').then(function(html) {
			return {
    			template: html,
                props: { processId: String },
	        	data: function() { 
					return {
	    				leftOpen: true,
						rightOpen: false,
						process: null,
						instances: null,
						processDefinitions: null,
						selectedInstance: null,
						task: null,
						activityInstance: null,
						activityInstanceHistory: null,
						firstResult: 0,
						maxResults: this.$root.config.maxProcessesResults,
						filter: '',
						activityId: '',
						loading: false
	    			} 
				},
    			computed: {
    				shortendLeftCaption: function() {
    					return this.$t('process.details.historyVersions')
    				},
    				shortendRightCaption: function() {
    					return this.$t('process.details.variable')
    				},
					processName: function() {
						if (!this.process) return this.$t('process.process')
    					return this.process.name ? this.process.name : this.process.key

					}
    			},
    			created: function() {
					if (this.$route.query.processId) this.loadProcess(this.processId)
					else {
						this.$store.dispatch('getProcessByDefinitionKey', { key: this.processId }).then(process => {
							this.process = process
							this.loadInstances()
							if (!this.process.statistics) this.loadStatistics()
							if (!this.process.activitiesHistory) this.loadProcessActivitiesHistory()
						})
					}
				},							
				methods: {
					updateItems: function(sortedItems) {
		      			this.instances = sortedItems
				    },
					loadProcess: function(id) {
						this.firstResult = 0
						this.$store.dispatch('getProcessById', { id: id }).then(process => {
							this.process = process
							this.loadInstances()
							if (!this.process.statistics) this.loadStatistics()
							if (!this.process.activitiesHistory) this.loadProcessActivitiesHistory()
						})
					},
					loadProcessActivitiesHistory: function() {
						HistoryService.findActivitiesProcessDefinitionHistory(this.process.id).then(activities => {
							this.process.activitiesHistory = activities
						})
					},
					loadProcessVersion: function(process) {
						if (this.process.id !== process.id) {
							this.firstResult = 0
							this.process = process
							this.loadInstances()
							if (!this.process.statistics) this.loadStatistics()
							if (!this.process.activitiesHistory) this.loadProcessActivitiesHistory()
						}
					},
					loadInstances: function(showMore) {
						if (this.$root.config.camundaHistoryLevel !== 'none') {								
							this.loading = true								
							HistoryService.findProcessesInstancesHistoryById(this.process.id, this.activityId,
								this.firstResult, this.maxResults, this.filter
							).then(instances => {
								this.loading = false
								if (!showMore) this.instances = instances
								else this.instances = !this.instances ? instances : this.instances.concat(instances)
							})
						}
						else {
							ProcessService.findProcessVersionsByDefinitionKey(this.process.key).then(processDefinitions => {
								this.processDefinitions = processDefinitions
								var promises = []			
								this.processDefinitions.forEach(() => {
									promises.push(HistoryService.findProcessesInstancesHistoryById(this.process.id, this.activityId, this.firstResult, 
										this.maxResults, this.filter))
								})
								Promise.all(promises).then(response => {
									if (!showMore) this.instances = []
									var i = 0
									response.forEach(instances => {
										instances.forEach(instance => {
											instance.processDefinitionId = processDefinitions[i].id
											instance.processDefinitionVersion = processDefinitions[i].version
										})
										this.instances = this.instances.concat(instances)
										i++
									})
								})
							})		
						}
					},
					loadStatistics: function() {
						ProcessService.findProcessStatistics(this.process.id).then(statistics => {
							this.$store.dispatch('setStatistics', { process: this.process, statistics: statistics })
						})
					},
					clearInstance: function() {
						this.setSelectedInstance({ selectedInstance: null })
						this.$refs.process.clearState()
						this.firstResult = 0
						this.loadInstances()
					},
					setSelectedInstance: function(evt) {
						var selectedInstance = evt.selectedInstance
						if (!selectedInstance) {
							this.rightOpen = false
							this.selectedInstance = null
						} else this.rightOpen = true
						this.task = null
						this.activityInstance = null
						this.activityInstanceHistory = selectedInstance ? this.activityInstanceHistory : null
						if (selectedInstance) {
							if (this.selectedInstance && this.selectedInstance.id === selectedInstance.id && !evt.reload) return
							this.selectedInstance = selectedInstance
							if (this.selectedInstance.state === 'ACTIVE') {
								//Management
								TaskService.findTasksByProcessInstance(selectedInstance.id).then(tasks => {
									this.selectedInstance.currentTasks = tasks
								})
								ProcessService.findActivityInstance(selectedInstance.id).then(activityInstance => {
									this.activityInstance = activityInstance
									HistoryService.findActivitiesInstancesHistory(selectedInstance.id).then(activityInstanceHistory => {
										this.activityInstanceHistory = activityInstanceHistory
									})
								})
							} else {
								//History
								if (this.$root.config.camundaHistoryLevel !== 'none') {
									HistoryService.findActivitiesInstancesHistory(selectedInstance.id).then(activityInstanceHistory => {
										this.activityInstanceHistory = activityInstanceHistory
									})
								}
							}
						}
					},
					setSelectedTask: function(selectedTask) {
						if (this.selectedInstance && selectedTask) {
							HistoryService.findTasksByDefinitionKeyHistory(selectedTask.id, this.selectedInstance.id).then(function(task) {
								if (task.length === 0) {
									this.task = false 
									return
								}
								this.task = task[0]
								var serviceCall = !this.task.endTime ? TaskService.fetchActivityVariables : 
									HistoryService.fetchActivityVariablesHistory
								serviceCall(this.task.activityInstanceId).then(variables => {
									variables.forEach(variable => {
										variable.value = variable.type === 'Object' ? JSON.stringify(variable.value) : variable.value
									})
									this.task.variables = variables
								})
							})
						}
					},
					filterByActivityId: function(event) {
						this.activityId = event
						this.instances = []
						this.firstResult = 0
						this.loadInstances()
					},
					showMore: function() {
						this.firstResult += this.$root.config.maxProcessesResults
						this.loadInstances(true)
					},
					filterInstances: function(filter) {
						this.filter = filter
						this.firstResult = 0
						this.loadInstances()
					},					
					getIconState: function(state) {
						switch(state) {
							case 'ACTIVE':
								return 'mdi-chevron-triple-right text-success'
							case 'SUSPENDED':
								return 'mdi-close-circle-outline'
						}
						return 'mdi-flag-triangle'
					},
					loadVersionProcess: function(event) {
						this.selectedInstance = null
						Vue.nextTick(function() {
							this.activityId = ''
							this.$refs.process.clearState()
							this.loadProcessVersion(event)
						}.bind(this))
					},
					openSubprocess: function(event) {
						this.activityId = ''
						this.setSelectedInstance({ selectedInstance: null })
						this.loadProcessVersion(event)
						Vue.nextTick(function() {
							this.$refs.navbar.getVersions()
						}.bind(this))
						this.$router.push('/flow/auth/management/' + event.key)
					},
					exportCSV: function() {
						var headers = [
							{ text: 'state', key: 'state' },
							{ text: 'businessKey', key: 'businessKey' },
							{ text: 'startTime', key: 'startTime' },
							{ text: 'endTime', key: 'endTime' },
							{ text: 'id', key: 'id' },
							{ text: 'startUserId', key: 'startUserId' },
							{ text: 'details.definitionName', key: 'processDefinitionName' },
							{ text: 'details.definitionVersion', key: 'processDefinitionVersion' }
						]
						headers.forEach(h => h.text = this.$t('process.' + h.text))
						var csvContent = headers.map(h => h.text).join(';') + '\n'
						var keys = headers.map(h => h.key)
						this.instances.forEach(v => {
							csvContent += getStringObjByKeys(keys, v) + '\n'
						})
						var csvBlob = new Blob([csvContent], { type: 'text/csv' })
						var filename = 'Management_Instances_' + moment().format('YYYYMMDD_HHmm') + '.csv'
						this.$refs.importPopper.triggerDownload(csvBlob, filename)
					}
				}
    		}
        })
	})
	
	export { Management }