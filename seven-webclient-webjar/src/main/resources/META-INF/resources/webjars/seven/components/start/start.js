/* globals permissionsMixin */

import { permissionsMixin } from './permissions.js';

const StartPage = function(resolve) {
	axios.get('webjars/seven/components/start/start.html').then(function(html) {
		resolve({
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
		})
	})		
}

export { StartPage }