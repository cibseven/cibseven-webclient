	const StatusProgressBar = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/status-progress-bar/status-progress-bar.html').then(function(html) {
			return {
				template: html,
				props: { statusData: Array, maxWidth: { type: Number, default: 300 } },
				computed: {
					style: function() {
						return {
							maxWidth: this.maxWidth + 'px'
						}
					},
					currentTask: function() {
						return this.statusData.find(st => st.status === 'PENDING')
					}
				},
    			methods: {
					getClasses: function(st) {
						var classes = []
						if (st === this.currentTask) classes.push('status-progress-bar-current')
						else if (st.status === 'DONE') classes.push('status-progress-bar-done')
						return classes
					}
    			}
			}
		})		
	})
		
	export { StatusProgressBar }