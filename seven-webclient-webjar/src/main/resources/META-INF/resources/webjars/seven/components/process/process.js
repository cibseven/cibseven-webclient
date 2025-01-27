/* globals window, moment, permissionsMixin, localStorage, setTimeout, document, 
	clearTimeout, FormData, Blob, Uint8Array, atob, FileReader, event, resizableTablesMixin, DOMParser */
	
	import { resizableTablesMixin } from '../../components/resizable-tables.js';
	import { permissionsMixin } from '../../permissions.js';
	import { TaskService, ProcessService, HistoryService, IncidentService } from '../../services.js';
	
	var fileObjects = ['de.cib.cibflow.api.files.FileValueDataFlowSource', 'de.cib.cibflow.api.files.FileValueDataSource']
	
	const serviceMap = {
	    ProcessService: ProcessService,
	    HistoryService: HistoryService
	}
	
	function debounce(delay, fn) {
		var timeoutID = null
		return function() {
			clearTimeout(timeoutID)
			var args = arguments
			var self = this
			timeoutID = setTimeout(function() { fn.apply(self, args) }, delay)
		}
	}
	
	var copyToClipboardMixin = {
		methods: {
			copyValueToClipboard: function(val) {
				if (val) {
					var textToCopy = (typeof val === 'object') ? JSON.stringify(val) : val					
					var tmpHtml = document.createElement('textarea')
					document.body.appendChild(tmpHtml)
					tmpHtml.value = textToCopy
				    tmpHtml.select()
				    document.execCommand('copy')
				    document.body.removeChild(tmpHtml)
					this.$refs.messageCopy.show()
				}
			}
		}
	}
	
	var procesessMixin = {
		props: { view: String, processName: String },
		data: function() { 
			return {
            	focused: null
            }
		},
		methods: {
			onImageLoadFailure: function(event) {
				event.target.src = 'webjars/seven/components/process/images/default.svg'     
		    },
			showDescription: function(key) {
				if (this.$te('process-descriptions.' + key)) return this.$t('process-descriptions.' + key) 
				return ''
			}
		}
	}
	
	var resizerMixin = {
		data: function() { 
			return {
				bpmnViewerOriginalHeight: 400,
            	bpmnViewerHeight: 400,
            	topBarHeight: 40,
            	dragSelectorHeight: 10,
            	filterHeight: 60,
            	mousePosition: null,
            	toggleIcon: 'mdi-chevron-down',
            	toggleButtonHeight: 40,
            	toggleTransition: '',
            	transitionTime: 0.4
            	
        	}
		},
		computed: {
			bottomContentPosition: function() {
				return this.bpmnViewerHeight + this.topBarHeight
			}
        },
		methods: {    
			handleMouseDown: function(e) {
	      		if (e.offsetY > this.bpmnViewerHeight - this.dragSelectorHeight) {
        			this.mousePosition = e.y
		        	document.addEventListener('mousemove', this.resize, false)
		        	document.addEventListener('mouseup', this.handleMouseUp, false)
		        	document.body.style.userSelect = 'none'
		      	}
		    },
		    resize: function(e) {
	      		var dy = e.y - this.mousePosition
	      		this.mousePosition = e.y
	      		this.bpmnViewerHeight += dy
		  		if (this.bpmnViewerHeight < (this.bpmnViewerHeight + this.$refs.rContent.offsetHeight)) this.toggleIcon = 'mdi-chevron-down'
				else this.toggleIcon = 'mdi-chevron-up'
		    },
		    handleMouseUp: function() {
		  		document.removeEventListener('mousemove', this.resize, false)
		  		document.removeEventListener('mouseup', this.handleMouseUp, false)
		  		document.body.style.userSelect = "text"
		    },
		    toggleContent: function() {
				this.toggleTransition = 'transition: top '+ this.transitionTime +'s ease, height '+ this.transitionTime +'s ease'
				if (this.bpmnViewerHeight < (this.bpmnViewerHeight + this.$refs.rContent.offsetHeight)) {
					this.bpmnViewerHeight += this.$refs.rContent.offsetHeight
					if (this.$refs.filterTable) this.bpmnViewerHeight += this.$refs.filterTable.offsetHeight
					this.toggleIcon = 'mdi-chevron-up'			
				}
				else {
					this.bpmnViewerHeight = this.bpmnViewerOriginalHeight
					this.toggleIcon = 'mdi-chevron-down'	
				}
				setTimeout(() => { this.toggleTransition = '' }, this.transitionTime * 1000)
			}
		}
	}
	
	var procesessVariablesMixin = {
        props: { process: Object, selectedInstance: Object, activityInstance: Object, activityInstanceHistory: Array },
		data: function() {
        	return { 
				variables: [],
				file: null,
				selectedVariable: null
			}
        },                
		watch: {
			'selectedInstance.id': {
				immediate: true,
				handler: function() {
					this.variables = []
					this.filteredVariables = []
					this.file = null
					this.selectedVariable = null
					this.loadSelectedInstanceVariables()
				}
			}
        },
		computed: {
			activityInstancesGrouped: function() {
				var res = []
				if (this.activityInstance) {
					res[this.activityInstance.id] = this.activityInstance.name
					this.activityInstance.childActivityInstances.forEach(ai => {
						res[ai.id] = ai.name
					})
				} else {
					res[this.selectedInstance.id] = this.selectedInstance.processDefinitionName
					this.activityInstanceHistory.forEach(ai => {
						res[ai.id] = ai.activityName
					})
				}
				return res
			}
		},			
        methods: {
			loadSelectedInstanceVariables: function() {
				if (this.selectedInstance !== null) {
					if (this.selectedInstance.state === 'ACTIVE') {
						this.fetchInstanceVariables('ProcessService', 'fetchProcessInstanceVariables')
					} else {
						if (this.$root.config.camundaHistoryLevel === 'full') {
							this.fetchInstanceVariables('HistoryService', 'fetchProcessInstanceVariablesHistory')
						}
					}
				}
			},
			fetchInstanceVariables: function(service, method) {
				var variablesToSerialize = []
				serviceMap[service][method](this.selectedInstance.id, false).then(variables => {
					variables.forEach(variable => {
						try {
							variable.value = variable.type === 'Object' ? JSON.parse(variable.value) : variable.value
						} catch (err) {
							variablesToSerialize.push(variable.id)
						}
						variable.modify = false
					})
					if (variablesToSerialize.length > 0) {
						serviceMap[service][method](this.selectedInstance.id, true).then(dVariables => {
							dVariables.forEach(dVariables => {
								var variableToSerialize = variables.find(variable => variable.id === dVariables.id)
								if (variableToSerialize) {
									variableToSerialize.value = dVariables.value
								}
							})
						})
					}
					variables.forEach(v => {
						v.scope = this.activityInstancesGrouped[v.activityInstanceId]
					})
					this.variables = variables.sort((a, b) => a.name.localeCompare(b.name))
					this.filteredVariables = variables.sort((a, b) => a.name.localeCompare(b.name))
				})
			},
			downloadFile: function(variable) {
				if (variable.type === 'Object') {
					if (variable.value.objectTypeName.includes('FileValueDataFlowSource')) {
						TaskService.downloadFile(variable.processInstanceId, variable.name).then(data => {
							this.$refs.importPopper.triggerDownload(data, variable.value.name)
						})
					} else {
						var blob = new Blob([Uint8Array.from(atob(variable.value.data), c => c.charCodeAt(0))], { type: variable.value.contentType })
						this.$refs.importPopper.triggerDownload(blob, variable.value.name)
					}
			    } else {				
					var download = this.selectedInstance.state === 'ACTIVE' ? 
					ProcessService.fetchVariableDataByExecutionId(variable.executionId, variable.name) : 
						HistoryService.fetchHistoryVariableDataById(variable.id)
					download.then(data => {							
						this.$refs.importPopper.triggerDownload(data, variable.valueInfo.filename)
					})
				}
			},
			uploadFile: function() {
				if (this.isFileValueDataSource(this.selectedVariable)) {
					var reader = new FileReader()
					reader.onload = event => {
						var fileData = {
					        contentType: this.file.type,
					        name: this.file.name,
					        encoding: 'UTF-8',
					        data: event.target.result.split(',')[1],
					        objectTypeName: this.selectedVariable.valueInfo.objectTypeName
				      	}
				      	var valueInfo = {
							objectTypeName: this.selectedVariable.valueInfo.objectTypeName,
							serializationDataFormat: 'application/json'
						}				      	
				      	var data = { fileObject: true, processDefinitionId: this.selectedVariable.processDefinitionId, modifications: {} }
						data.modifications[this.selectedVariable.name] = { value: JSON.stringify(fileData), 
							valueInfo: valueInfo, type: this.selectedVariable.type }
						ProcessService.modifyVariableByExecutionId(this.selectedVariable.executionId, data).then(() => {
							this.selectedVariable.value = fileData
						})
					}
					reader.onerror = () => {}
					reader.readAsDataURL(this.file)
				} else {
					var formData = new FormData()
					formData.append('file', this.file)
					var fileObj = { name: this.file.name, type: this.file.type }
					ProcessService.modifyVariableDataByExecutionId(this.selectedVariable.executionId, this.selectedVariable.name, formData)
					.then(() => {
						this.selectedVariable.valueInfo.filename = fileObj.name
						this.selectedVariable.valueInfo.mimeType = fileObj.type				
						this.file = null
					})
				}				
			},
			saveOrEditVariable: function(variable) {
				if (variable.modify) {
					var data = { modifications: {} }
					data.modifications[variable.name] = { value: variable.value }
					ProcessService.modifyVariableByExecutionId(variable.executionId, data).then(() => {
						variable.modify = false
					})
				} else variable.modify = true
			}
		}
	}
	
	const Process = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/process.html').then(function(html) {
        	return {
                template: html,
                inject: ['loadProcesses'],
                mixins: [permissionsMixin, resizerMixin, copyToClipboardMixin],
                props: { instances: Array, process: Object, firstResult: Number, maxResults: Number,
					activityInstance: Object, activityInstanceHistory: Array, activityId: String, loading: Boolean },
                data: function() { 
					return {
	                	selectedInstance: null,
	                	selectedTask: null,
	                	topBarHeight: 0,
	                	tabs: [
							{ id: 'instances', active: true },
						],
						activeTab: 'instances',
						events: {},
						usages: [],							
						sortByDefaultKey: 'startTimeOriginal',
						sorting: false,
					  	sortDesc: true
                	}
				},				
		        watch: {
					'process.id': function() {
						ProcessService.fetchDiagram(this.process.id).then(response => {
							this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
						})
					},
					activeTab: function() {
						/*
						if (this.activeTab === 'statistics') {
							this.MeteringService.fetchEvents(this.process.key).then(events => {
								events.forEach(event => {
									this.events[event] = []
									this.MeteringService.fetchInfoUsage(this.process.key, "2020-09-04T00:00:00+01:00", "2030-10-03T23:59:59+01:00", event)
									.then(usages => {
										this.events.event = JSON.parse(usages)
										this.usages = this.usages.concat(JSON.parse(usages))
									})
								})
							})
						}
						*/
					}
    			},
				mounted: function() {
	      			ProcessService.fetchDiagram(this.process.id).then(response => {
						setTimeout(() => {
							this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
						}, 100)
					})	
			    },
                computed: {
					processName: function() {
						return this.process.name !== null ? this.process.name : this.process.key
					}
                },
                methods: {
				    applySorting: function(sortedItems) {
						this.sorting = true
						this.sortDesc = null
    					this.sortByDefaultKey = ''
						this.$emit('update-items', sortedItems)
					  	this.$nextTick(() => {
					  		this.sorting = false
					  		this.sortDesc = true
	        			})
				    },
					changeTab: function(selectedTab) {
			      		this.tabs.forEach((tab) => {
		        			tab.active = tab.id === selectedTab.id
			      		})
			      		this.activeTab = selectedTab.id
				    },
                	selectInstance: function(event) {
                		if (!this.selectedInstance) this.$refs.diagram.setEvents()
                		this.selectedInstance = event.instance
                		this.$emit('instance-selected', { selectedInstance: event.instance, reload: event.reload })
                		this.$refs.diagram.cleanDiagramState()
                	},
                	selectTask: function(event) {
                		this.selectedTask = event
                		this.$emit('task-selected', event);
                	},
					viewProcess: function() {
						this.$refs.diagram.clearEvents()
                		this.$refs.diagram.cleanDiagramState()
						ProcessService.fetchDiagram(this.process.id).then(response => {
							this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
							this.$refs.diagramModal.show()
						})
					},
					viewDeployment: function() {
						this.$router.push('/seven/auth/deployments/' + this.process.deploymentId)
					},
					downloadBpmn: function() {
						var filename = this.process.resource.substr(this.process.resource.lastIndexOf('/') + 1, this.process.resource.lenght)
						window.location.href = 'flow-engine/process/' + this.process.id + '/data?filename=' + filename + 
							'&token=' + this.$root.user.authToken
					},
					openInModeler: function() {
						this.$router.push('/seven/auth/modeler/' + this.process.id)
					},			
                	clearState: function() {
            			this.selectedInstance = null
                		this.selectedTask = null
                		this.$emit('instance-selected', { selectedInstance: null })
                		this.$emit('task-selected', null)
                		this.$refs.diagram.clearEvents()
                		this.$refs.diagram.cleanDiagramState()
                	},		
					showConfirm: function(type) { this.$refs.confirm.show(type) },
					suspendProcess: function() {
						ProcessService.suspendProcess(this.process.id, true, true).then(() => {
							this.$store.dispatch('setSuspended', { process: this.process, suspended: 'true' })
							this.instances.forEach(instance => {
								if (instance.state === 'ACTIVE') instance.state = 'SUSPENDED'
							})
							this.$refs.success.show()
						})
					},
					activateProcess: function() {
						ProcessService.suspendProcess(this.process.id, false, true).then(() => {
							this.$store.dispatch('setSuspended', { process: this.process, suspended: 'false' })
							this.instances.forEach(instance => {
								if (instance.state === 'SUSPENDED') instance.state = 'ACTIVE'
							})
							this.$refs.success.show()
						})
					},
					handleScrollProcesses: function(el) {
						if (this.instances.length < this.firstResult) return
						if (Math.ceil(el.target.scrollTop + el.target.clientHeight) >= el.target.scrollHeight) {
							this.$emit('show-more')
						}
					},
					onInput: debounce(800, function(evt) { this.$emit('filter-instances', evt) })
                }
            }
        })
	})
	
	const InstancesTable = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/instances-table.html').then(function (html) {
            return {
                template: html,
                mixins: [resizableTablesMixin, copyToClipboardMixin, permissionsMixin],
                props: { instances: Array, sortDesc: Boolean, sortByDefaultKey: String },
                data: function() {
					return {
						focusedCell: null
					}
				},
                methods: {			
					selectInstance: function(instance, reload) {
						this.$emit('select-instance', { instance, reload })
					},
					viewProcess: function() {
						this.$emit('view-process')
					},		
					showConfirm: function(type) { this.$refs.confirm.show(type) },			
					deleteInstance: function(instance) {
						ProcessService.deleteInstance(instance.id).then(() => {
							this.$emit('instance-deleted')
							this.$refs.success.show()
						})
					},					
					deleteHistoryInstance: function(instance) {
						HistoryService.deleteProcessInstanceFromHistory(instance.id).then(() => {
							this.$emit('instance-deleted')
							this.$refs.success.show()
						})
					},					
					suspendInstance: function(instance) {
						ProcessService.suspendInstance(instance.id, true).then(() => {
							instance.state = 'SUSPENDED'
							this.$refs.success.show()
						})
					},
					activateInstance: function(instance) {
						ProcessService.suspendInstance(instance.id, false).then(() => {
							instance.state = 'ACTIVE'
							this.$refs.success.show()
						})
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
					getIconTitle: function(state) {
						switch(state) {
							case 'ACTIVE':
								return this.$t('process.instanceRunning')
							case 'SUSPENDED':
								return this.$t('process.instanceIncidents')
						}
						return this.$t('process.instanceFinished')
					},
				}
            }
        })
	})
	
	const ProcessVariablesSidebar = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/process-variables-sidebar.html').then(function(html) {
            return {
                template: html,
                mixins: [procesessVariablesMixin]
            }
        })
	})
	
	const ProcessVariablesTable = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/process-variables-table.html').then(function(html) {
            return {
                template: html,
                mixins: [procesessVariablesMixin, resizerMixin, copyToClipboardMixin],
                data: function() {
		        	return { 
						variableToModify: null,
						filterHeight: 0,
						filteredVariables: [],
						activityId: '',
						tabs: [
							{ id: 'variables', active: true },
							{ id: 'incidents', active: false },
							{ id: 'usertasks', active: false }
						],
						activeTab: 'variables',
						stackTraceMessage: ''
					}
		        },
				computed: {
					formattedJsonValue: {
						get: function() {
							if (this.variableToModify) {
								if (this.variableToModify.type === 'Json') {
									return JSON.stringify(JSON.parse(this.variableToModify.value), null, 2)
								} else return this.variableToModify.value
							}
							return ''
						},
						set: function(val) {
							this.variableToModify.value = val
						}
					}
				},
				mounted: function() {
	      			ProcessService.fetchDiagram(this.process.id).then(response => {
						this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
					})
			    },
			    methods: {					
					isFileValueDataSource: function(item) {
						if (item.type === 'Object') {
						    if (item.value && item.value.objectTypeName) {
					        	if (fileObjects.includes(item.value.objectTypeName)) return true
						    } 
					    }
					    return false
					},	
					displayObjectNameValue: function(item) {
						if (this.isFileValueDataSource(item)) {
						    return item.value.name
						}
						return item.value
					},
					isFile: function(item) {
						if (item.type === 'File') return true
						else return this.isFileValueDataSource(item)
					},	
					changeTab: function(selectedTab) {
			      		this.tabs.forEach((tab) => {
		        			tab.active = tab.id === selectedTab.id
			      		})
			      		this.activeTab = selectedTab.id
				    },		
                	selectTask: function(event) {
                		this.selectedTask = event
                		this.$emit('task-selected', event);
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
					modifyVariable: function(variable) {
						this.selectedVariable = variable
						this.variableToModify = JSON.parse(JSON.stringify(variable))
						this.$refs.modifyVariable.show()
					},
					deleteVariable: function(variable) {
						if (this.selectedInstance.state === 'ACTIVE') {
							ProcessService.deleteVariableByExecutionId(variable.executionId, variable.name).then(() => {
								this.loadSelectedInstanceVariables()
								this.$refs.success.show()
							})
						} else {
							HistoryService.deleteVariableHistoryInstance(variable.id).then(() => {
								this.loadSelectedInstanceVariables()
								this.$refs.success.show()
							})
						}
					},
					updateVariable: function() {
						var data = { modifications: {} }
						if (this.variableToModify.type === 'Json') {
							this.variableToModify.value = JSON.stringify(JSON.parse(this.variableToModify.value))
						}
						data.modifications[this.variableToModify.name] = { value: this.variableToModify.value, type: this.variableToModify.type }
						ProcessService.modifyVariableByExecutionId(this.variableToModify.executionId, data).then(() => {
							this.selectedVariable.value = this.variableToModify.value						
							this.$refs.modifyVariable.hide()
						})
					},									
					filterByChildActivity: function(event) {
						if (event) {
							this.activityId = event.activityId
							this.filteredVariables = this.variables.filter(obj => obj.activityInstanceId === event.id)
						} else {
							this.activityId = ''
							this.filteredVariables = this.variables
						}
					},
					getActivityName: function(activityId) {
					    var result = this.activityInstanceHistory.find(activity => {
					        return activity.activityId === activityId
					    })
						return result.activityName
					},
					getFailingActivity: function(activityId) {						
						return this.$refs.diagram.viewer.get('elementRegistry').get(activityId).businessObject.name
					},
					showIncidentMessage: function(jobDefinitionId) {
						this.stackTraceMessage = ''
						IncidentService.fetchIncidentStacktraceByJobId(jobDefinitionId).then(res => {
							this.stackTraceMessage = res
							this.$refs.stackTraceModal.show()
						})
					},
					showPrettyTimestamp: function(orignalDate) {
						return moment(orignalDate).format('DD/MM/YYYY HH:mm:ss')
					},
					retryJob: function(jobDefinitionId) {						
						IncidentService.retryJobById(jobDefinitionId).then(() => {
							this.selectedInstance.incidents.splice(this.selectedInstance.incidents.findIndex(obj => obj.configuration === jobDefinitionId), 1)
							this.$refs.successRetryJob.show()
						})
					}
				}
            }
        })
	})

	
	const Processes = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/processes.html').then(function(html) {
			return {
				template: html,
                inject: ['isMobile'],
                mixins: [permissionsMixin],
				data: function () {
					return {
						selected: null,
						filter: '',
						showProcesses: true,
						view: 'image-outline',
						isTable: false,
						selectedOption: localStorage.getItem('optionSelected') || 'all'
					}
				},
				watch: {
					'$route.query.key': function(key) {
						this.checkProcessInUrl(key)
					}
				},
				created: function() {
					this.view = this.isMobile() ? 'image-outline' : localStorage.getItem('viewMode') || 'image-outline'
					this.isTable = this.view === 'view-list'
				},
				computed: { 
					processesFiltered: function() {
						if (!this.$store.state.process.list) return []
						return this.$store.state.process.list.filter(process => {
							return ((process.key.toUpperCase().includes(this.filter.toUpperCase()) ||
									((process.name) ? process.name.toUpperCase().includes(this.filter.toUpperCase()) : false)) &&
									(!process.revoked))
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
					activeViewMode: function() { return 'mdi mdi-24px mdi-' + this.view },
					processesByOptions: function() { return this[this.selectedOption + 'Filter'](this.processesFiltered) },
					textEmptyProcessesList: function() {
						return this.selectedOption === 'all' && this.filter === ''  ? 'process.emptyProcessList' : 'process.emptyProcessListFiltered'
					}
				},
				mounted: function() {
					if (localStorage.getItem('favorites')) {
						this.$store.dispatch('setFavorites', { favorites: JSON.parse(localStorage.getItem('favorites')) })
					}
					if (this.$route.query.key) {
						setTimeout(() => { this.checkProcessInUrl(this.$route.query.key) }, 1000)
					}
				},
				methods: {
					checkProcessInUrl: function (processKey) {
						var index = this.processesByOptions.findIndex(process => { return process.key === processKey })
						if (index > -1) this.startProcess(this.processesByOptions[index])
						else {
							this.$root.$refs.error.show({ type: 'processNotFound', params: [processKey] })
						}
					},					
					processName: function(process) {
						if (process) return process.name ? process.name : process.key
					},
					favoritesFilter: function(processes) {
						return this.allFilter(processes).filter(process => {
							return process.favorite
						})
					},
					allFilter: function(processes) {
						return processes.filter(function(process) {
							return process.startableInTasklist && process.suspended !== 'true'
						})
					},
					favoriteHandler: function(process) {
						process.favorite = !process.favorite
						var favorites = this.favoritesFilter(this.$store.state.process.list).map(r => { return r.key })
						localStorage.setItem('favorites', JSON.stringify(favorites))
					},
					changeViewMode: function(mdi) {
						this.view = mdi
						this.isTable = this.view === 'view-list'
						localStorage.setItem('viewMode', this.view)
					},
					setOptionSelected: function(option) {
						return option === this.selectedOption ? 
							'border-bottom: 3px solid var(--primary); background: var(--light)' : 'background: var(--white)'
					},
					selectedOptionHandler: function(option) {
						this.selectedOption = option
						localStorage.setItem('optionSelected', option)
					},
					startProcess: function(process) { this.$refs.processStart.startProcess(process) },
					viewProcess: function(process) {
						this.selected = process
						ProcessService.fetchDiagram(process.id).then(response => {
							this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
							this.$refs.diagramModal.show()
						})
					}
				}
			}
        })
	})
	
	const ProcessDetailsSidebar = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/process-details-sidebar.html').then(function(html) {
            return {
                template: html,
                mixins: [copyToClipboardMixin, permissionsMixin],
                props: { process: Object, instances: Array, selectedInstance: Object, selectedTask: [Object, Boolean] },
                data: function() {
                	return { 
						selectedVersion: this.process.id,
						versions: [],
						processVisible: true,
						loading: false,
						lazyLoadHistory: this.$root.config.lazyLoadHistory,
						historyTimeToLive: ''
            		}
                },
				watch:  {
					selectedVersion: function() {
						this.findAndUpdateProcess()
					}
				},
                computed: {
					processName: function() {
						return this.process.name ? this.process.name : this.process.key
					},
					getCurrentTaskFromSelectedInstance: function() {
						if (this.selectedInstance && this.selectedInstance.currentTasks)
							return this.selectedInstance.currentTasks[this.selectedInstance.currentTasks.length - 1]
					}
                },
				mounted: function() {
					this.getVersions(this.lazyLoadHistory)
				},
                methods: {
					getVersions: function(lazyLoadHistory) {
						this.loading = true
						return ProcessService.findProcessVersionsByDefinitionKey(this.process.key, lazyLoadHistory)
							.then(versions => {
							if (lazyLoadHistory) {
								versions.forEach(v => {
									v.runningInstances = '-'
									v.allInstances = '-'
									v.completedInstances = '-'
								})
							}
							this.versions = versions
							this.loading = false
							if (lazyLoadHistory) this.findAndUpdateProcess()
							return versions
						})
					},
					findAndUpdateProcess: function() {
						ProcessService.findProcessById(this.selectedVersion, true).then(process => {
							for (let v of this.versions) {
								if (v.id === process.id) {
									Object.assign(v, process)
									break
								}
							}
						})
					},
                	getDate: function(type) {
                		if (this.instances.length === 0) return null
            			var date = type === 'min' ?
            					Math.min.apply(Math, this.instances.map(i => { return moment(i.startTimeOriginal) })) :
        						Math.max.apply(Math, this.instances.map(i => { return moment(i.startTimeOriginal) }))
    					return moment(date).format('LL HH:mm')
                	},
					editHistoryTimeToLive: function() {
						this.historyTimeToLive = this.process.historyTimeToLive
						this.$refs.historyTimeToLive.show()
					},
					updateHistoryTimeToLive: function() {
						var data = { historyTimeToLive: this.historyTimeToLive || null }
						ProcessService.updateHistoryTimeToLive(this.process.id, data).then(() => {
							this.process.historyTimeToLive = this.historyTimeToLive || null
							this.$refs.historyTimeToLive.hide()
							this.$refs.successOperation.show()
						})
					},
					showConfirm: function(data) { this.$refs.confirm.show(data) },
					deleteProcessDefinition: function(process) {
						ProcessService.deleteProcessDefinition(process.id, true).then(() => {
							this.getVersions(this.lazyLoadHistory).then(versions => {
								if (versions.length === 0) {
									this.$router.push('/seven/auth/process-management')
								} else {
									this.$emit('load-version-process', versions[0])
									this.selectedVersion = versions[0].id
								}
							})
							this.$refs.successOperation.show()
						})
					}
                }
            }
        })
	})
	
	const ProcessCard = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/process-card.html').then(function(html) {
            return {
                template: html,
                mixins: [permissionsMixin, procesessMixin],
                props: { process: Object },
				computed: {
					viewStyles: function() {
						return {
							'view-comfy': {
								cardSize: 'width: 280px; height: 250px',
								imgSize: 'width: 140px; height: 60px',
								imgBlock: 'height: 70px',
								textBlock: 'height: 150px'
							},
							'view-module': {
								cardSize: 'width: 400px; height: 250px',
								imgSize: 'width: 140px; height: 60px',
								imgBlock: 'height: 70px',
								textBlock: 'height: 150px'
							}
						}
					},
					textHtml: function() {
						return '<h5 :title="' + this.processName + '">' + this.processName + '</h5>' +
							'<div>' + this.showDescription(this.process.key) + '</div>'
					}
				}
            }
        })
	})
	
	const ProcessAdvanced = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/process-advanced.html').then(function(html) {
            return {
                template: html,
                mixins: [permissionsMixin, procesessMixin],
                props: { process: Object },
				computed: {
					viewStyles: function() {
						return {
							'image-outline': {
								cardSize: 'width: 290px; height: 160px',
								imgSize: 'height: 100px',
								imgBlock: 'height: 110px',
								textBlock: 'height: 45px'
							}
						}
					}
				},
				methods: {					
					shortProcessName: function(processName) {
						if (processName) {
						  	if (processName.length > 35) return processName.substring(0, 32).trim().concat('...')
						  	return processName
						}
					}
				}
            }
        })
	})
	
	const ProcessTable = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/process-table.html').then(function(html) {
            return {
                template: html,
                mixins: [permissionsMixin, procesessMixin],
                props: { processes: Array },
				computed: {
					fields: function() {
						return [{ label: 'favorite', key: 'favorite', sortable: false, thClass:'py-0', tdClass:'py-0 ps-0', 
							class: 'col-1 d-flex align-items-center justify-content-center'},
								{ label: 'name', key: 'name', class: 'col-3' },
								{ label: 'key', key: 'key', class: 'col-2' },
								{ label: 'description', key: 'description', sortable: false, class: 'col-4' },
								{ label: 'actions', key: 'actions', sortable: false, tdClass: 'py-0', class: 'col-2 d-flex justify-content-center' },
							]
					}
				}
            }
        })
	})
	
	const UserTasksTable = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/user-tasks-table.html').then(function(html) {
            return {
                template: html,
                mixins: [copyToClipboardMixin],
                props: { selectedInstance: Object },
                data: function() {
		        	return {
						userTasks: [],						
						focusedCell: null
					}
		        },
				mounted: function() {
	      			TaskService.findTasksPost({ 
						processInstanceId: this.selectedInstance.id, 
						processDefinitionId: this.selectedInstance.processDefinitionId 
					}).then(res => {
						this.userTasks = res
					})
			    },
			    methods: {
					changeAssignee: function(event) {
						var userTask = this.userTasks.find(task => task.id === event.taskId)
						userTask.assignee = event.assignee
					}
				}
            }
        })
	})
	
	const TaskAssignationModal = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/task-assignation-modal.html').then(function(html) {
            return {
                template: html,
                data: function() {
		        	return {
						identityLinks: [],
						identities: [
							{ value: 'userId', text: 'assignee', type: 'assignee' },
							{ value: 'userId', text: 'users', type: 'candidate' },
        					{ value: 'groupId', text: 'groups', type: 'candidate' }
						],
						selectedIdentity: {},
						identity: '',
						taskId: '',
						identityExists: false
					}
		        },
				computed: {
			  		filteredIdentities() {
				    	return this.identities.filter(identity => identity.type === this.selectedIdentity.type)
				  	}
				},
		        methods: {
					show: function(taskId, assignee) {
						this.selectedIdentity = this.identities[1]
						if (assignee) this.selectedIdentity = this.identities[0]
						this.taskId = taskId
						this.identityLinks = []
						this.identityExists = false
						TaskService.findIdentityLinks(taskId).then(res => {
							this.identityLinks = res || []
						})
						this.$refs.assignationModal.show()
					},					
					hasIndentityLinks: function(identity) {
				    	return this.identityLinks.some(obj => obj[identity.value] !== null && 
				    		obj[identity.value] !== undefined && 
				    		obj.type === identity.type)
				    },
				    addIdentity: function() {
						var newIdentityLink = { type: this.selectedIdentity.type }
						newIdentityLink[this.selectedIdentity.value] = this.identity
						
						this.identityExists = this.identityLinks.find(obj => {
						    return obj[this.selectedIdentity.value] === newIdentityLink[this.selectedIdentity.value] && 
						           obj.type === this.selectedIdentity.type
						})
						
						if (!this.identityExists) {						
							TaskService.addIdentityLink(this.taskId, newIdentityLink).then(() => {			
								this.identity = ''						
								if (this.selectedIdentity.type !== 'assignee') this.identityLinks.push(newIdentityLink)
								else {
									var assigneeIdentity = this.identityLinks.find(ilink => ilink.type === 'assignee')
									assigneeIdentity ? assigneeIdentity.userId = newIdentityLink.userId : this.identityLinks.push(newIdentityLink)
									this.$emit('change-assignee', { taskId: this.taskId, assignee: newIdentityLink.userId })
								}
							})
						}
					},
					removeIdentityLink: function(identityLink, idx) {
						TaskService.removeIdentityLink(this.taskId, identityLink).then(() => {
					  		this.identityLinks.splice(idx, 1)
					  		if (this.selectedIdentity.type === 'assignee') this.$emit('change-assignee', { taskId: this.taskId, assignee: null })
						})											
					}
				}
            }
        })
	})
	
	const AddVariableModal = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/add-variable-modal.html').then(function(html) {
            return {
                template: html,
				props: { selectedInstance: Object },
                data: function() {
		        	return {
						variable: { name: '', type: 'String', value: null, valueInfo: {} }
					}
		        },
				watch: {
					'variable.type': function(type) {
						if (type === 'Boolean') {
							this.variable.value = true
						} else this.variable.value = null
					}
				},
				computed: {
					types: function() {
						return [
							{ text: 'String', value: 'String' },
							{ text: 'Boolean', value: 'Boolean' },
							{ text: 'Short', value: 'Short' },
							{ text: 'Integer', value: 'Integer' },
							{ text: 'Long', value: 'Long' },
							{ text: 'Double', value: 'Double' },
							{ text: 'Date', value: 'Date' },
							{ text: 'Null', value: 'Null' },
							{ text: 'Object', value: 'Object' },
							{ text: 'Json', value: 'Json' },
							{ text: 'Xml', value: 'Xml' }
						]
					},
					isValid: function() {
						if (this.variable.type === 'Null') return true
						if (!this.variable.name || !this.variable.value) return false
						const value = this.variable.value
						if (this.variable.type === 'Object') {
							return this.variable.valueInfo.objectTypeName && this.variable.valueInfo.serializationDataFormat
						} else if (this.variable.type === 'Short') {
							return value >= -32768 && value <= 32767
						} else if (this.variable.type === 'Integer') {
							return value >= -2147483648 && value <= 2147483647
						} else if (this.variable.type === 'Long') {
							return value >= -Number.MAX_SAFE_INTEGER && value <= Number.MAX_SAFE_INTEGER
						} else if (this.variable.type === 'Double') {
							return !isNaN(value)
						} else if (this.variable.type === 'Json') {
							try {
								JSON.parse(value)
								return true
							} catch(e) {
								return false
							}
						} else if (this.variable.type === 'Xml') {
							const parser = new DOMParser()
							const xmlDoc = parser.parseFromString(value, 'text/xml')
							return !xmlDoc.getElementsByTagName('parsererror').length
						}
						return true
					}
				},
		        methods: {
					show: function() {
						this.$refs.addVariable.show()
					},
					addVariable: function() {
						var variable = JSON.parse(JSON.stringify(this.variable))
						if (variable.type === 'Date') variable.value = moment(variable.value).format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
						if (variable.type !== 'Object') delete variable.valueInfo
						ProcessService.putLocalExecutionVariable(this.selectedInstance.id, variable.name, variable).then(() => {
							this.$emit('variable-added')
							this.$refs.addVariable.hide()
						})
					},
					reset: function() {
						this.variable = { name: '', type: 'String', value: null, valueInfo: {} }
					}
				}
            }
        })
	})
	
	export { Process, InstancesTable, ProcessVariablesSidebar, ProcessVariablesTable, Processes, 
		ProcessDetailsSidebar, ProcessCard, ProcessAdvanced, ProcessTable, UserTasksTable, TaskAssignationModal, AddVariableModal }