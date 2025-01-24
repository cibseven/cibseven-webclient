/* globals localStorage, URLSearchParams, sessionStorage */
	
	const FlowProcessManagement = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/flow-process-management/flow-process-management.html').then(function (html) {
			return {
    			template: html,
				mounted: function() {
					var formFrame = this.$refs['flowprocessmanagement-frame']
					var theme = localStorage.getItem('theme') || this.$root.theme
					var themeContext = ''
					var hasQueryParams = this.$root.config.flowProcessManagementUrl.includes('?')
					var separator = hasQueryParams ? '&' : '?'
					var urlParams = new URLSearchParams()
					urlParams.append('token', this.getToken())
					if (theme && !['cib', 'generic'].includes(theme)) {
						themeContext = 'themes/' + theme + '/bootstrap_4.5.0.min.css'
						urlParams.append('theme', themeContext)
					}
					urlParams.append('header', 'false')
					formFrame.src = this.$root.config.flowProcessManagementUrl + '/#/' + separator + urlParams.toString()
				},
				methods: {
					getToken: function() {
						return sessionStorage.getItem('token') || localStorage.getItem('token')
					}
				}
    		}
        })
	})
	
	export { FlowProcessManagement }