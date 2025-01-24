/* globals URLSearchParams */
	
	const Modeler = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/modeler/modeler.html').then(function(html) {
			return {
    			template: html,
    			props: { processId: String },
    			inject: ['currentLanguage'],
				mounted: function() {
					var params = new URLSearchParams()
					var theme = localStorage.getItem('theme') || this.$root.theme
					var themeContext = ''					
					var url = this.$root.config.modelerUrl
					params.append('locale', this.currentLanguage())
					if(this.processId) params.append("processId", encodeURIComponent(this.processId))
					if (theme && !['cib', 'generic'].includes(theme)) {
						themeContext = 'themes/' + theme + '/bootstrap_4.5.0.min.css'
						params.append('theme', themeContext)
						console.log(params)
					}
					params.append("token", encodeURIComponent(this.$root.user.authToken))
					var formFrame = this.$refs['modeler-frame']
					formFrame.src = url + "?" + params.toString()
				},
				methods: {
					init: function() {}
					
				}
    		}
        })
	})

	export { Modeler }