/* globals localStorage, URLSearchParams, sessionStorage, window, history, URL */
	
	const EasyForm = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/easy-form/easy-form.html').then(function (html) {
			return {
    			template: html,
    			inject: ['currentLanguage'],
				mounted: function() {
					window.addEventListener('message', this.processMessage)
					var formFrame = this.$refs['easyform-frame']
					formFrame.src = this.buildUrlParams({ id: this.$route.query.id })
				},
				methods: {
					getToken: function() {
						return sessionStorage.getItem('token') || localStorage.getItem('token')
					},
					processMessage: function(e) {
						var formFrame = this.$refs['easyform-frame']
						if (e.source === formFrame.contentWindow && e.data.method) {
							if (e.data.method === 'setForm') {
								this.setForm(e.data)
							}
						}
					},
					buildUrlParams: function(params) {
						var theme = localStorage.getItem('theme') || this.$root.theme
						var themeContext = ''
						var hasQueryParams = this.$root.config.formbuilderUrl.includes('?')
						var separator = hasQueryParams ? '&' : '?'
						var urlParams = new URLSearchParams()
						urlParams.append('token', this.getToken())
						urlParams.append('locale', this.currentLanguage())
						if (theme && !['cib', 'generic'].includes(theme)) {
							themeContext = 'themes/' + theme + '/bootstrap_4.5.0.min.css'
							urlParams.append('theme', themeContext)
						}
						if (params.id) urlParams.append('id', params.id)
						return this.$root.config.formbuilderUrl + '/#/' + separator + urlParams.toString()
					},
					setForm: function(params) {
						let url = new URL(window.location.href.replace(/\?.+$/, ''))
						if (params.data && params.data.id) url.searchParams.set('id', params.data.id)
						history.pushState({}, '', url.pathname + url.hash + url.search)
					}
				},
				beforeDestroy: function() {
					window.removeEventListener('message', this.processMessage)
				}
    		}
        })
	})
	
	export { EasyForm }