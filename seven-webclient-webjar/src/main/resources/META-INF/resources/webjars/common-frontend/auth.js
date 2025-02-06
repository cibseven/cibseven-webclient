/* globals window, sessionStorage, localStorage, FileReader */
	

	var AuthService = {
		login: function(params, remember) {
			return axios.create().post('auth/login', params, { params: { source: 'WEBSITE' } }).then(function(user) {
				axios.defaults.headers.common.authorization = user.data.authToken
				;(remember ? localStorage : sessionStorage).setItem('token', user.data.authToken)
				return user.data
			})
        },           
        
       	update: function(params) {
			return axios.patch('auth/user', params, { params: { source: 'WEBSITE' } }).then(function(user) {
				axios.defaults.headers.common.authorization = user.authToken
				if (sessionStorage.getItem('token') || !localStorage.getItem('token')) sessionStorage.setItem('token', user.authToken)
				else localStorage.setItem('token', user.authToken)
				return user
			})
        },
        
        'delete': function(id) { return axios['delete']('auth/user/' + id + '?source=WEBSITE') },
        
        requestPasswordReset: function(params) {
        	params.source = 'WEBSITE' 
        	return axios.create().post('auth/reset', null, { params: params }) 
        },
        
        poll4otp: function(userId) { return axios.get('auth/otp/' + userId + '?source=WEBSITE') },
	}
	
	const Login = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/login.html').then(function(html) {
			return { 
				template: html,
				props: { 
					credentials: Object, 
					credentials2: Object, 
					hideForgotten: Boolean,	
					onRegister: Function, 
					forgottenType: { type: String, default: 'email' } 
				},
				data: function() {
					return { 
						rememberMe: true,
						show: false,
						email: null
					} 
				},
				methods: {					
					onLogin: function() {
						var self = this
						this.credentials.username = this.$refs.username.value // https://helpdesk.cib.de/browse/DOXISAFES-456
						this.credentials.password = this.$refs.password.$refs.input.value
						AuthService.login(this.credentials, this.rememberMe).then(function(user) { self.$emit('success', user) }, function(error) { 
							var res = error.response.data
							if (res && res.type === 'LoginException' && res.params && res.params.length >= 1 && res.params[0] === 'StandardLogin') {
								self.credentials2.username = self.credentials.username
								self.credentials2.password = self.credentials.password
								self.$refs.otpDialog.show(res.params[1])
							} else if (error.response.status === 429) { // Too many requests
								res.params[1] = new Date(res.params[1]).toLocaleString('de-DE')
								self.$root.$refs.error.show(res)	
							} else self.$root.$refs.error.show(res)
						})
					}, // https://vuejs.org/v2/guide/components-custom-events.html
										
					onForgotten: function() {		
						this.email = this.$refs.email.value		
						if (this.credentials2) this.credentials2.email = this.email
						AuthService.requestPasswordReset({ email: this.email }).then(function() { 
							this.$refs.emailDialog.hide()
							this.$refs.emailSent.show(true)	
						}.bind(this), 
						function(error) { 
							this.$refs.emailDialog.hide()
							var res = error.response.data
							if (res && res.type === 'LoginException' && res.params && res.params.length >= 1 && res.params[0] === 'StandardLogin')
								this.$refs.resetDialog.show(res.params[1])								
							else this.$root.$refs.error.show(res)
						}.bind(this))
					}					
				}
	       	}
		})
	})
	
	const OtpDialog = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/otp-dialog.html').then(function(html) {
			return { 
				template: html,
				props: { credentials2: Object, rememberMe: Boolean },
				data: function() { return { busy: false } },
				methods: {
					show: function(userId) { 
						this.userId = userId
						this.$refs.otpDialog.show()
						this.requestOtp() 
					},
					requestOtp: function() {
						this.busy = true
						AuthService.poll4otp(this.userId).then(function(otp) { 
							if (otp) { // not timeout
								this.credentials2.otp = otp
								this.onLogin2()
							} 
						}.bind(this)).finally(function() { this.busy = false }.bind(this))
					},
					onLogin2: function() {						
						AuthService.login(this.credentials2, this.rememberMe).then(function(user) { this.$emit('success', user) }.bind(this), function(error) { 
							var res = error.response.data
							if (res && res.type === 'LoginException') res.type = 'LoginExceptionTwoFactor'
							this.$root.$refs.error.show(res) 
						}.bind(this))
					}
				}
	       	}
		})
	})
	
	const ResetDialog = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/reset-dialog.html').then(function(html) {
			return { 
				template: html,
				props: { credentials2: Object },
				data: function() { return { busy: false } },
				methods: {
					show: function(userId) { 
						this.userId = userId
						this.$refs.resetDialog.show()
						this.requestOtp() 
					},
					requestOtp: function() {
						this.busy = true
						AuthService.poll4otp(this.userId).then(function(otp) { 
							if (otp) { // not timeout
								this.credentials2.otp = otp
								this.onForgotten2()
							}
						}.bind(this)).finally(function() { this.busy = false }.bind(this))
					},
					onForgotten2: function() {
						delete this.credentials2.username
						delete this.credentials2.password
						AuthService.requestPasswordReset(this.credentials2).then(function() { 
							this.$refs.resetDialog.hide() 
							this.$emit('success')
						}.bind(this), function(error) { 
							var res = error.response.data
							if (res && res.type === 'LoginException') res.type = 'LoginExceptionTwoFactor'
							this.$root.$refs.error.show(res) 
						}.bind(this))
					},
					onForgotten3: function() {
						delete this.credentials2.otp
						AuthService.requestPasswordReset(this.credentials2).then(function() { 
							this.$refs.resetDialog.hide()
							this.$emit('success')
						}.bind(this), function(error) { 
							var res = error.response.data
							if (res && res.type === 'LoginException') res.type = 'LoginExceptionTwoFactor'
							this.$root.$refs.error.show(res) 
						}.bind(this))
					}
				}
	       	}
		})
	})
	
	const AccountForm = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/account-form.html').then(function(html) {
			return {
				template: html,
				props: { registration: Object, userid: String, minPasswordStrength: Number, hideDelete: Boolean },
				data: function() {
					return { 
						show: false,
						score: 0,
						confirmPassword: ''
					} 
				},
				methods: {
					onDelete: function() {
						AuthService['delete'](this.userid).then(function() { 
							sessionStorage.getItem('token') ? sessionStorage.removeItem('token') : localStorage.removeItem('token')
						    var baseUrl = window.location.origin + window.location.pathname; // server + context 
							window.location.href = baseUrl // real page change so that cached service data is reset
						})
					}, 
					onUpdate: function() {
						var self = this
						AuthService.update(this.registration).then(function(user) { self.$emit('update', user) }) 
					} // https://vuejs.org/v2/guide/components-custom-events.html
				}
			}
		})
	})
	
	const Simg = {
		template: '<img :src="encoded">',
		props: { src: String, auth: String },
		data: function() { return { encoded: null } },
		mounted: function() { this.src && this.load(this.src) },
		watch: { 
			src: function(val) { val && this.load(val) }	
		},
		methods: { 
			load: function(src) {
				axios.create().get(src, { 
					headers: { authorization: this.auth },
					responseType: 'blob'
				}).then(function(res) { // https://pqina.nl/blog/convert-an-image-to-a-base64-string-with-javascript/#fetching-the-image-source
					var reader = new FileReader()
		            reader.onloadend = function() { this.encoded = reader.result }.bind(this)
		            reader.readAsDataURL(res.data);
				}.bind(this)) 		
			}
		}
	}
	
	export { Login, OtpDialog, ResetDialog, AccountForm, Simg }