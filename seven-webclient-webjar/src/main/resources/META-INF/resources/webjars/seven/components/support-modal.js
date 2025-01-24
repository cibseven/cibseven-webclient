/* globals localStorage */
	
	const SupportModal = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/support-modal.html').then(function(html) {
			return {
				template: html,
				methods: {
					isTextProperty: function(property) {
						let language = localStorage.getItem('language')
						if (this.$root.config.supportDialog && this.$root.config.supportDialog[language] && 
								this.$root.config.supportDialog[language][property]) return true
						return false
					},
					getTextFromProperty: function(property) {
						let language = localStorage.getItem('language')
						if (this.$root.config.supportDialog && this.$root.config.supportDialog[language] && 
							this.$root.config.supportDialog[language][property])
								return this.$root.config.supportDialog[language][property]
						return false
					},
					show: function() {
						this.$refs.support.show()
					}
				}
			}
		})
	})
	
	export { SupportModal }