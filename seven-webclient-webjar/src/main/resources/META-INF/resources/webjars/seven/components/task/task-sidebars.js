/* globals moment, usersMixin, localStorage */
	
	import { TaskService, HistoryService, ProcessService } from '../../services.js';
	import { usersMixin } from '../users-mixin.js';

	const TaskDetailsSidebarAll = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/task/task-details-sidebar-all.html').then(function(html) {
            return {
                template: html,
				mixins: [usersMixin],
				inject: ['currentLanguage'],
                props: { processInstanceHistory: Object, task: Object },
				data: function() {
					return {						
						showInfo: localStorage.getItem('sidebarState') === 'info',
						showStatus: localStorage.getItem('sidebarState') === 'status',
						showChat: localStorage.getItem('sidebarState')  === 'chat',
						assignee: null, 
						loadingUsers: false, 
						time: { due: '' }
					}
				},
				watch: {
					showInfo: function(newVal) {
						if (newVal) localStorage.setItem('sidebarState', 'info')
					},
					showStatus: function(newVal) {
						if (newVal) localStorage.setItem('sidebarState', 'status')
					},
					showChat: function(newVal) {
						if (newVal) localStorage.setItem('sidebarState', 'chat')
					}
				}	
            }
        })
	})
	
	const TaskDetailsSidebar = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/task/task-details-sidebar.html').then(function(html) {
            return {
                template: html,
				mixins: [usersMixin],
				inject: ['currentLanguage'],
                props: { processInstanceHistory: Object, task: Object },
				data: function() {
					return { assignee: null, loadingUsers: false, time: { due: '' } }
				},
				watch: {
		        	assignee: function() {
		        		if (this.assignee != null) this.checkAssignee()
		        	},
					'task': function() {
						if (this.task && this.task.due) {
							var date = new Date(this.task.due)
							this.time.due = date.getHours() + ':' + date.getMinutes() + ':00'
						} else this.time.due = ''
					}
		        },
                computed: {
                	dueColor: function() {
						if (!this.task.due) return ''
						if (moment(this.task.due).isBefore(moment())) return 'text-danger'
						if (moment().add(this.$root.config.warnOnDueExpirationIn, 'hours').isAfter(moment(this.task.due))) return 'text-warning'
                	},
					dueDate: {
                		set: function(val) {
                			this.task.due = val ? moment(val).format('YYYY-MM-DDTHH:mm:ss.SSSZZ') : null
							this.setTime(this.time.due, 'due')
                		},
                		get: function() {
                			return this.task.due ? new Date(this.task.due) : null
                		}
                	},
                	reminderDate: {
                		set: function(val) {
                			this.task.followUp = val ? moment(val).startOf('day').format('YYYY-MM-DDTHH:mm:ss.SSSZZ') : null
                			TaskService.update(this.task)
                		},
                		get: function() {
                			return this.task.followUp ? new Date(this.task.followUp) : null
                		}
					}		
                },
				methods: {
					isInThePast: function(ymd, date) {
						return date < moment().startOf('day')
					},
					formatDate: function(date) {
						if (date) return moment(date).utc().format('LL HH:mm')
						return ''
					},
					update: function() {
						this.$refs.ariaLiveText.textContent = ''
						TaskService.setAssignee(this.task.id, this.assignee).then(() => {
							this.task.assignee = this.assignee
							this.assignee = null
							if (this.task.assignee != null)
								this.$refs.ariaLiveText.textContent = this.$t('task.userAssigned', [this.getCompleteName])
						})
					},
					checkAssignee: function() {
						TaskService.findTaskById(this.task.id).then(task => {
							if (task.assignee === null) this.update()
							else this.$refs.confirmTaskAssign.show()
						})
					},
					assignToMe: function() { this.assignee = this.$root.user.id },
					setTime: function(time, type) {
						if (this.task[type]) {
							var date = new Date(this.task[type])
							if (time) {
								var timeSplit = time.split(':')
								date.setHours(timeSplit[0])
								date.setMinutes(timeSplit[1])
							}
							this.task[type] = moment(date).format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
						}
						TaskService.update(this.task)
					}
				}
            }
        })
	})
	
	const TaskDetailsSidebarChat = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/task/task-details-sidebar-chat.html').then(function(html) {
	        return {
	            template: html,
				mixins: [usersMixin],
				inject: ['currentLanguage'],
	            props: { task: Object, sidebarShown: Boolean },
				data: function () {
					return { 
						chatComments: [],
						comment: { note: "", editor: "", editorDisplayName: "", date: "", taskName: "" },
						loadingComments: false
					}
				},
				watch: {
					'task': function (oldValue, newValue) {
						if (oldValue && newValue && oldValue.id === newValue.id) {
							this.fetchChatNotes()	
						} else {
							this.loadingComments = true
							this.chatComments = []
							this.fetchChatNotes()
						}
					},
					sidebarShown: function () {
						this.$nextTick(() => {
							this.scrollChatToBottom()
						})
					}
				},
				created: function() {
					this.fetchChatNotes()
				},
				methods: {
					fetchChatNotes: function () {
						if (this.task) {
							// Find parent of process instance to save the variable in there.
							HistoryService.findProcessInstance(this.task.processInstanceId).then((processInstance) => {							
								var id = this.task.processInstanceId
								if (processInstance.rootProcessInstanceId != null) {
									id = processInstance.rootProcessInstanceId
								}
								ProcessService.fetchChatComments(id, 
									this.task.processDefinitionId.split(":")[0], false).then((chatComments) => {
										if (chatComments !== undefined && chatComments.type !== 'Null' && chatComments.value !== undefined) {
											this.chatComments = JSON.parse(chatComments.value)
											this.$nextTick(() => {
												this.scrollChatToBottom()
											})
										}
										
									this.loadingComments = false
								}, () => {
									this.loadingComments = false
								})
							})
						}
					},
					getFormattedDate: function () {
						var date = new Date()
						return this.getTwoDigits(date.getDate()) + '.' +
						 this.getTwoDigits(date.getMonth() + 1) + '.' +
						 date.getFullYear() + ', ' + this.getTwoDigits(date.getHours()) + ':' + this.getTwoDigits(date.getMinutes())
					},
					getTwoDigits: function (number) {
						return (number < 10 ? '0' : '') + number
					},
					saveComments: function () {
						var comment = {
							name: 'chatComments',
							value: this.chatComments,
							type: 'json',
							valueInfo: {
				                "objectTypeName": "org.camunda.spin.plugin.variable.value.JsonValue",
				                "serializationDataFormat": "application/json"
							}
						}
						// Find parent of process instance to save the variable in there.
						HistoryService.findProcessInstance(this.task.processInstanceId).then((processInstance) => {
							var id = this.task.processInstanceId
							
							if (processInstance.rootProcessInstanceId != null) {
								id = processInstance.rootProcessInstanceId
							}
							
							ProcessService.submitVariables(id, [comment])
						})
						
					},
					setComment: function () {
						this.comment.editor = this.$root.user.userID
						this.comment.editorDisplayName = this.$root.user.displayName
						this.comment.date = this.getFormattedDate()
						this.comment.taskName = this.task.name
						this.chatComments.push(this.comment)
						this.saveComments()
						this.comment = {note: "", editor: "", editorDisplayName: "", date: "", taskName: ""}
						this.$nextTick(() => {
							this.scrollChatToBottom()
						})
					},
					scrollChatToBottom: function () {
						if (this.$refs.chat) {
							this.$refs.chat.scrollTop = this.$refs.chat.scrollHeight	
						}
					},
					handleEnterDown: function (event) {
						if (event.keyCode === 13 && !event.shiftKey) {
							 event.preventDefault()
							 this.setComment()
						} else {
							var cursorPosition = event.target.selectionStart

							this.comment.note = this.comment.note.substring(0, cursorPosition) + '\n' +
							 this.comment.note.substring(cursorPosition)
						}
					}
				}
	        }
		})
    })
	
	const TaskDetailsSidebarStatus = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/task/task-details-sidebar-status.html').then(function(html) {
	        return {
	            template: html,
				mixins: [usersMixin],
				inject: ['currentLanguage'],
	            props: { task: Object },
				data: function () {
					return {
						statusDataset: []
					}
				},
				watch: {
					'task': function (oldValue, newValue) {
						if (oldValue && newValue && oldValue.id === newValue.id) {
							this.fetchStatusProgress()	
						} else {
							this.statusDataset = []
							this.fetchStatusProgress()
						}
					}
				},
				mounted: function() {
					this.fetchStatusProgress()
				},
				methods: {
					fetchStatusProgress: function() {
						if (!this.task) return
						HistoryService.findProcessInstance(this.task.processInstanceId).then((processInstance) => {							
							var id = this.task.processInstanceId
						
							if (processInstance.rootProcessInstanceId != null) {
								id = processInstance.rootProcessInstanceId
							}
							
							ProcessService.fetchStatusDataset(id, this.task.processDefinitionId.split(":")[0], false)
							.then(statusDataset => {
								if (statusDataset && statusDataset.value) this.statusDataset = JSON.parse(statusDataset.value)
							})
						})
					}
				}
	        }
		})
    })

	export { TaskDetailsSidebarAll, TaskDetailsSidebar, TaskDetailsSidebarChat, TaskDetailsSidebarStatus }