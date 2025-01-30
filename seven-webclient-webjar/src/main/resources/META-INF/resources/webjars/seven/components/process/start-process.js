	
	import { ProcessService } from '../../services.js';	

	const StartProcess = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/start-process.html').then(function(html) {
			return {
				template: html,
				inject: ['currentLanguage'],
				props: { hideProcessSelection: Boolean },
				data: function() {
					return {
						processesFilter: '',
						startParamUrl: '',
						selectedProcess: {}
					}
				},
				computed: {
					startableProcesses: function() {
    					return this.$store.state.process.list.filter(process => {
    						return process.startableInTasklist === true && !process.revoked && process.suspended !== 'true' &&
    							(process.name ? process.name.toLowerCase().indexOf(this.processesFilter.toLowerCase()) !== -1 :
								process.key.toLowerCase().indexOf(this.processesFilter.toLowerCase()) !== -1)
    					}).sort((objA, objB) => {
    						var nameA = objA.name ? objA.name.toUpperCase() : objA.key.toUpperCase()
								var nameB = objB.name ? objB.name.toUpperCase() : objB.key.toUpperCase()
								return nameA < nameB ? -1 : nameA > nameB ? 1 : 0
							})
    				},
					processName: function() {
						return this.selectedProcess.name !== null ? this.selectedProcess.name : this.selectedProcess.key
					}    				
				},
				mounted: function() {
					this.$eventBus.on('openStartProcess', this.show)
				},
				methods: {
					show: function() {
						this.$refs.startProcess.show()
					},
					startProcess: function(process) {
						process.loading = true
						ProcessService.findProcessByDefinitionKey(process.key).then(processLatest => {
							this.selectedProcess = processLatest
							ProcessService.startForm(processLatest.id).then(url => {
								if (!url.key && !url.camundaFormRef) {
									ProcessService.startProcess(processLatest.key, this.currentLanguage()).then(task => {
										this.$refs.startProcess.hide()
										task.processInstanceId = task.id
										this.$emit('process-started', task)
										process.loading = false
									})
								}
								else {
									if (url.camundaFormRef) {
										this.startParamUrl = this.$root.config.uiElementTemplateUrl + '/startform/camunda-form-template' +
											'?processDefinitionKey=' + processLatest.id
									} else {
										this.startParamUrl = this.$root.config.uiElementTemplateUrl + '/startform/' +
										url.key.split('?template=')[1] + '?processDefinitionId=' + processLatest.id + 
										'&processDefinitionKey=' + processLatest.key
									}
									if (this.hideProcessSelection) this.$refs.startProcess.show()
									process.loading = false
								}
							})
						})
					}
				},
				beforeUnmount: function() {
					this.$eventBus.off('openStartProcess', this.show)
				}
			}
		})
	})

	export { StartProcess }