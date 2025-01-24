/* globals sessionStorage, localStorage */
	
	const CIBHeaderFlow = function(resolve) {
		axios.get('webjars/seven/layout-flow/cib-header.html').then(function(html) {
			resolve({ 
				template: html,
				inject: ['currentLanguage'],
				props: { languages: Array, user: Object },
				methods: {
			        logout: function() {
			        	sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : localStorage.removeItem('token')
			        	this.$emit('logout')
			        }
				}
	       	})		
		})
	}

	export { CIBHeaderFlow }