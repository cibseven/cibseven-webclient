/* globals window, permissionsMixin */

	import { permissionsMixin } from '../../permissions.js';
	
	const ProcessManagement = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/process/process-management.html').then(function(html) {
			return {
				template: html,
                inject: ['loadProcesses'],
                mixins: [permissionsMixin],
				data: function() {
					return {
						selected: null,
						filter: '',
						focused: null,
						onlyIncidents: false,
						loadingInstances: true,
						onlyActive: true
					}
				},
				computed: { 
					processesFiltered: function() {
						if (!this.$store.state.process.list) return []
						var processes = this.$store.state.process.list.filter(process => {
							return ((process.key.toUpperCase().includes(this.filter.toUpperCase()) ||
									((process.name) ? process.name.toUpperCase().includes(this.filter.toUpperCase()) : false)))
						})
						processes = processes.filter(process => {
							var incidents = this.onlyIncidents ? process.incidents > 0 : true
							var onlyActive = this.onlyActive ? process.suspended === 'false' : true
							return incidents && onlyActive
						})
						processes.sort((objA, objB) => {
							var nameA = objA.name ? objA.name.toUpperCase() : objA.name
							var nameB = objB.name ? objB.name.toUpperCase() : objB.name
							var comp = nameA < nameB ? -1 : nameA > nameB ? 1 : 0
							
							if (this.$root.config.subProcessFolder) {
								if (objA.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = 1
								else if (objB.resource.indexOf(this.$root.config.subProcessFolder) > -1) comp = -1
							}
							return comp
						})
						return processes
					},
					textEmptyProcessesList: function() {
						return this.filter === '' ? 'process.emptyProcessList' : 'process.emptyProcessListFiltered'
					},
					fields: function() {
						return [{ label: 'status', key: 'incidents', thClass:'py-0', tdClass:'py-0 ps-0 border-end border-top-0', 
							class: 'col-1 d-flex align-items-center justify-content-center' },
							{ label: 'runningInstances', key: 'runningInstances', class: 'col-1 justify-content-center', 
							tdClass: 'border-end py-1 border-top-0' },
							{ label: 'name', key: 'name', class: 'col-7', tdClass: 'border-end py-1 border-top-0' },
							{ label: 'actions', key: 'actions', sortable: false, class: 'col-3 d-flex justify-content-center', 
							tdClass: 'border-end py-0 border-top-0' },
						]
					}
				},
				created: function() {
					this.loadProcesses(true).then(() => {
						this.loadingInstances = false
					})
				},
				methods: {
					goToDeployment: function(process) {
						this.$router.push('/seven/auth/deployments/' + process.deploymentId)
					},
					goToCockpit: function(process) {
						window.open(this.$root.config.cockpitUrl + '#/process-definition/' + process.id, '_blank')
					},
					goToShowProcessHistory: function(process) {
						this.$router.push('/seven/auth/management/' + process.key)
					},
					openInModeler: function(process) {
						this.$router.push('/seven/auth/modeler/' + process.id)
					}
				}
			}
        })
	})
	
	export { ProcessManagement }