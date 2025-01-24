/* globals decodeURI, window, setTimeout */
	
	import { AuthService } from '../../services.js';	

	const PasswordRecover = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/password/password.html').then(function(html) {
            return {
                template: html, 
				data: function() {
					return { 
						tokenValidated: false,
						showPassword: false,
						showPassRepeat: false,
						passwordRepeat: null,
						profile: { id: null },
						credentials: { password: null },				
						recoverToken: null,
						sendingEmail: false,
						passwordPolicyError: false
					}			
				},
				mounted: function() {
					var hashAux = window.location.hash
					
					if (hashAux.includes('userId=')) {
						var userId = ''
				
						var userIdStartPos = hashAux.indexOf('userId=') + 'userId='.length
						
						if(hashAux.indexOf('&', userIdStartPos) > -1) 
							userId = hashAux.substring(userIdStartPos, hashAux.indexOf('&', userIdStartPos))
						else userId = hashAux.substring(userIdStartPos)
					
						window.location.href = hashAux = hashAux.replace('&userId=' + userId, '')
						this.userId = userId
					}
					
					if (hashAux.includes('recoverToken=')) {
						var recoverToken = ''
				
						var recoverTokenStartPos = hashAux.indexOf('recoverToken=') + 'recoverToken='.length
						
						if(hashAux.indexOf('&', recoverTokenStartPos) > -1) 
							recoverToken = hashAux.substring(recoverTokenStartPos, hashAux.indexOf('&', recoverTokenStartPos))
						else recoverToken = hashAux.substring(recoverTokenStartPos)
						
						AuthService.passwordRecoverCheck(decodeURIComponent(recoverToken)).then((response) => {
							this.tokenValidated = response
							this.recoverToken = decodeURI(recoverToken)
							window.location.href = hashAux.replace('?recoverToken=' + recoverToken, '')
						})
					}
				},
				methods: {
					show: function() {
						this.$refs.passwordModal.show()
					},							
					isValidEmail: function(value) {
						if (value === null || value === '') return null
						if (/^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/.test(value)) return true
						return false
					},
					fieldType: function(showPass) {
						if (showPass) 
							return 'text'
						return 'password'
					},
					onReset: function() {
						this.$router.push('/flow/login')
					},	
					notEmpty: function(value) {
						if (value === null) return null
						if (value.length < 1) return false
						return value != null && value.length > 0
					},									
					same: function(value, value2) {
						if (value === null || value2 === null) return null
						if (value.length < 1 || value2.length < 1) return false
						if (value != null && value2 != null && value === value2) return true
						return false
					},
					onSendEmail: function() {
						this.sendingEmail = true
						AuthService.passwordRecover(this.profile).then(() => {
							this.sendingEmail = false
							this.$refs.passwordModal.hide()
							this.$refs.emailSent.show()
							this.$refs.emailInfo.show()							
						}, () => {
							this.sendingEmail = false
						})
					},
					resendEmail: function() {
						this.$refs.emailInfo.hide()
						this.$refs.passwordModal.show()
						this.onSendEmail()
					},
					onChangePassword: function() {
						if (this.same(this.credentials.password, this.passwordRepeat)) {
							AuthService.passwordRecoverUpdatePassword(this.userId, this.credentials.password, 
								'', this.recoverToken).then(() => {
								this.passwordPolicyError = false
								setTimeout(() => {
									this.$router.push('/flow/login')			
								}, 1000)
							}, error => {
								var data = error.response.data
								if (data && data.type === 'PasswordPolicyException') {
									this.passwordPolicyError = true
								}
							})
						}
					}								
				}
            }
        })
	})
	
	export  { PasswordRecover }