/* globals FormData, Blob, console, screen, platform, window, setTimeout */
	
	/* jshint -W079 */
	const Error = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/dialogs/error.html').then(html => {
			return { 
				template: html,		
				data: function() { return { message: '' } },
				methods: {
					show: function(error) { 
						this.message = error.type ? this.$t('errors.' + error.type, error.params) : error				    	
						this.$refs.modal.show() 
					}
				}
	       	}	
		})
	})
	/* jshint +W079 */
	
	const Confirm = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/dialogs/confirm.html').then(html => {
			return { 
				template: html,	
				data: function() { return { param: null } },
				methods: {
					show: function(param) { 
						this.param = param
						this.$refs.modal.show() 
					}
				}
	       	}
		}).catch(e => console.log(e))
	})
	
	const Prompt = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/dialogs/prompt.html').then(html => {
			return { 
				template: html,		
				props: { secure: Boolean, title: String, pattern: String, feedback: String },
				data: function() { return { text: '', param: null, hide: this.secure, showFeedback: false } },
				methods: {
					show: function(param, text) {
						this.param = param
						this.text = text
						this.$refs.modal.show()
					},
					onHide: function(evt) {
						this.showFeedback = false
						if (evt.trigger === 'ok') this.$emit('ok', { 
							text: this.text, 
							param: this.param, 
							prevent: function() { 
								evt.preventDefault() 
								this.showFeedback = true
							}.bind(this) 
						}); else this.$emit('cancel', this.param)
					}
				}
	       	}
		})
	})
	
	const ProblemReport = {
		template: '<b-modal ref="modal" :title="$t(\'problem-report.title\')" @shown="$refs.textArea.focus()">\
				<cib-form ref="form" @submitted="report(); $refs.modal.hide(); problem = null">\
				<b-form-group :invalid-feedback="$t(\'errors.invalid\')">\
					<input v-model="email2" type="email" :placeholder="$t(\'problem-report.email\')" class="form-control">\
				</b-form-group>\
				<b-form-group>\
					<b-form-textarea ref="textArea" v-model="problem" :rows="10" :max-rows="10" required></b-form-textarea>\
				</b-form-group>\
				<b-form-group>\
					<clipboard tabindex="-1" @input="clip = $event"></clipboard>\
				</b-form-group>\
			</cib-form>\
			<template v-slot:modal-footer>\
				<button @click="$refs.form.onSubmit()" class="btn btn-primary">{{ $t(\'problem-report.ok\') }}</button>\
				<button type="button" class="btn btn-secondary" @click="$refs.modal.hide()">{{ $t(\'problem-report.cancel\') }}</button>\
			</template>\
		</b-modal>',		
		props: { url: String, email: String },
		data: function() { return { problem: '', email2: null, clip: null } },
		methods: {
			show: function() { 
				this.email2 = this.email
				this.$refs.modal.show() 
			},
			report: function() {
				var params = { 
					email : this.email2,
					platform: platform,
					screen: { 
						height: screen.height,
						width: screen.width
					}
				}
				this.$emit('report', params)
				console && console.warn('Reporting problem', params)
				var formData = new FormData()
				var text = window.location.origin + window.location.pathname + '\n' + this.problem
				formData.append('description', new Blob([text], { type : 'text/plain' }), 'description.txt')
				formData.append('logs', new Blob([JSON.stringify(params)], { type : 'application/json' }), 'params.json')	
				if (this.clip) formData.append('original', this.clip)
				axios.post(this.url, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
					.then(function(res) { this.$emit('sent', res) }.bind(this))
			}
		}	 
	}
	
	const Loader = {
		template: '<div>\
			<slot v-if="done"></slot>\
			<img v-else-if="show" src="webjars/common-frontend/dialogs/waitImage.svg" :style="styling">\
		</div>',
		props: { styling: String },		
		data: function() { return { done: false, show: true } },
		methods: {
			wait: function(prom, delay) { 
				this.done = false
				this.show = !delay
				if (delay) setTimeout(function() { this.show = true }.bind(this), delay)
				return prom.then(function(res) { 
					this.done = true
					return res
				}.bind(this), function() { this.done = true }.bind(this)) // any custom error handling needs to be attachend to prom before
			},
//			reset: function() { this.done = !(this.show = true) }
		}
	}
	
	const Success = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/dialogs/success.html').then(html => {
			return { 
				template: html,
				props: { top: { type: String, default: '60px' } },	
				data: function() { return { countdown: 0 } },
				methods: {
					show: function(time) { this.countdown = time || 5 }
				}
	       	}
		})
	})
	
	export { Error, Confirm, Prompt, ProblemReport, Loader, Success }