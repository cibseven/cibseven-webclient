/* globals moment, setTimeout, localStorage, permissionsMixin */
	
	import { permissionsMixin } from '../../permissions.js';
	import { ProcessService } from '../../services.js';

	function sortDeployments(a, b, sorting, order) {
		if (!a[sorting] && b[sorting]) return -1
		else if (a[sorting] && !b[sorting]) return 1
		a = a[sorting].toLowerCase()
		b = b[sorting].toLowerCase()
		if (order === 'asc') return a < b ? -1 : a > b ? 1 : 0
		else return a < b ? 1 : a > b ? -1 : 0
	}

	const Deployments = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/deployment/deployments.html').then(function (html) {
			return {
    			template: html,
                inject: ['loadProcesses'],
                mixins: [permissionsMixin],
	        	data: function() { 
					return {
						rightOpen: false,
						deployments: [],
						deployment: null,
						loading: true,
						deleteLoader: false,
						filter: this.$route.query.filter || '',
						sorting: {}, 
						cascadeDelete: true,
						resources: [],
						deploymentsDelData: { total: 0, deleted: 0 }
					} 
				},
				watch: {
					sorting: {
						handler: function() {
							localStorage.setItem('deploymentSorting', JSON.stringify(this.sorting))
						},
						deep: true
					}
				},
				computed: {
					deploymentsFiltered: function() {
						return this.deployments.filter(d => {
							return (d.name ? d.name.toUpperCase().includes(this.filter.toUpperCase()) : false)
						}).sort((a, b) => sortDeployments(a, b, this.sorting.key, this.sorting.order))
					},
					sortingFields: function() {
						return [
							{ text: this.$t('sorting.deployments.deploymentTime'), value: 'originalTime' },
					 		{ text: this.$t('sorting.deployments.name'), value: 'name' }
						]
					},
					deploymentsSelected: function() {
						return this.deploymentsFiltered.filter(d => {
							return d.isSelected
						})
					}
				},
				created: function () {
					this.sorting = localStorage.getItem('deploymentSorting') ? JSON.parse(localStorage.getItem('deploymentSorting')) : 
						{ key: 'originalTime', order: 'asc' }
					ProcessService.findDeployments().then(deployments => {
						deployments.forEach(d => {
							d.isSelected = false
						})
						this.deployments = deployments
					})
				},
				methods: {
					deleteDeployments: function() {
						var vm = this
						this.deleteLoader = true
						this.deploymentsDelData.total = this.deploymentsSelected.length
						this.deploymentsDelData.deleted = 0
						var pool = this.deploymentsSelected.slice(0, this.deploymentsSelected.length)
						startTask()
						function startTask() {
							var deployment = pool.shift()
							if (deployment) {
								deleteDeployment(deployment)
							} else {
								vm.loadProcesses(false)
								vm.deleteLoader = false
								vm.$refs.deploymentsDeleted.show()
							}
						}
						function deleteDeployment(deployment) {
							ProcessService.deleteDeployment(deployment.id, true).then(() => {
								vm.deploymentsDelData.deleted++
								vm.deployments = vm.deployments.filter(df => {
									return deployment.id !== df.id
								})
								if (vm.deployment && deployment.id === vm.deployment.id) vm.deployment = null
								setTimeout(() => {
									startTask()
								}, 1000)
							})
						}
					},
					changeSelected: function(evt) {
						this.deploymentsFiltered.forEach(d => { d.isSelected = evt })
					},
					selectDeployment: function(d) {
						this.deployment = d
						this.rightOpen = true
						this.findDeploymentResources(d.id)
					},
					findDeploymentResources: function(deploymentId) {
						ProcessService.findDeploymentResources(deploymentId).then(resources => {
							this.resources = resources
						})
					},
					changeSortingOrder: function() {
						this.sorting.order = this.sorting.order === 'desc' ? 'asc' : 'desc'
					}
				}
    		}
        })
	})
	
	const ResourcesNavBar = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/deployment/resources-nav-bar.html').then(function (html) {
			return {
    			template: html,
				props: { resources: Array, deployment: Object },
				data: function() {
					return { resource: {} }
				},
				methods: {
					showResource: function(resource) {
						this.resource = resource
						ProcessService.findProcessesWithFilters('deploymentId=' + this.deployment.id + '&resourceName=' + resource.name)
						.then(processesDefinition => {
							if (processesDefinition.length > 0 ) {
								this.$refs.diagramModal.show()
								var processDefinition = processesDefinition[0]
								ProcessService.fetchDiagram(processDefinition.id).then(response => {
									setTimeout(() => {
										this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
									}, 500)
								})
							}
							else this.processesDefinition = null
						})
					}
				}
    		}
        })
	})

	const DeploymentList = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/deployment/deployment-list.html').then(function(html) {
			return {
				template: html,
				props: { deployments: Array, deployment: Object, sorting: Object },
				data: function() {
					return { deploymentsGrouped: {} }
				},
				watch: {
					'$route.params.deploymentId': {
						handler: function() {
							this.setDeploymentFromUrl(this.$route.params.deploymentId)
						},
						immediate: true
					},
					deployments: {
						handler: function() {
							if (this.sorting.key === 'originalTime') this.deploymentsGrouped = this.groupByDate(this.deployments)
							if (this.sorting.key === 'name') this.deploymentsGrouped = this.groupByName(this.deployments)
						},
						immediate: true
					}
				},
				methods: {
					setDeployment: function(d) {
						this.$router.push('/seven/auth/deployments/' + d.id)
					},
					setDeploymentFromUrl: function(deploymentId) {
						if (this.deployments.length > 0 && deploymentId) {
							var deployment = this.deployments.find(d => {
								return d.id === deploymentId
							})
							if (deployment) this.$emit('select-deployment', deployment)
						}
					},
					groupByName: function(deployments) {
						var deploymentsGrouped = {}
						deployments.forEach(d => {
							if (!deploymentsGrouped[d.name[0]]) {
								deploymentsGrouped[d.name[0]] = { visible: true, data: [] }
							}
							deploymentsGrouped[d.name[0]].data.push(d)
						})
						Object.keys(deploymentsGrouped).forEach(key => {
							deploymentsGrouped[key].data.sort((a, b) => sortDeployments(a, b, 'originalTime', this.sorting.key))
						})
						return deploymentsGrouped
					},
					groupByDate: function(deployments) {
						var deploymentsGrouped = {}
						deployments.forEach(d => {
							var date = moment(d.originalTime).format('YYYY-MM-DD')
							if (!deploymentsGrouped[date]) {
								deploymentsGrouped[date] = { visible: true, data: [] }
							}
							deploymentsGrouped[date].data.push(d)
						})
						return deploymentsGrouped
					},
					title: function(key) {
						if (this.sorting.key === 'originalTime') {
							return moment(key).format('LL')
						} else return key
					}
				}
    		}
        })
	})
	
	export { Deployments, ResourcesNavBar, DeploymentList }