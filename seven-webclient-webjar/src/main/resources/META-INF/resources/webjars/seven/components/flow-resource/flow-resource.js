/* globals localStorage, URLSearchParams, sessionStorage */
	
	const FlowResource = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/flow-resource/flow-resource.html').then(function (html) {
			return {
    			template: html,
				mounted: function() {
					var formFrame = this.$refs['flowresource-frame']
					var theme = localStorage.getItem('theme') || this.$root.theme
					var themeContext = ''
					var hasQueryParams = this.$root.config.flowResourceUrl.includes('?')
					var separator = hasQueryParams ? '&' : '?'
					var urlParams = new URLSearchParams()
					urlParams.append('token', this.getToken())
					if (theme && !['cib', 'generic'].includes(theme)) {
						themeContext = 'themes/' + theme + '/bootstrap_4.5.0.min.css'
						urlParams.append('theme', themeContext)
					}
					urlParams.append('header', 'false')
					formFrame.src = this.$root.config.flowResourceUrl + '/#/' + separator + urlParams.toString()
				},
				methods: {
					getToken: function() {
						return sessionStorage.getItem('token') || localStorage.getItem('token')
					}
				}
    		}
        })
	})
	
	export { FlowResource }