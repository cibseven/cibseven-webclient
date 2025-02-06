/* globals CibGetMasterController, console, window */
	
	const Doxiview = {
		template: '<iframe ref="doxiview"\
			:style="!fullscreen ? {} : { width: \'100%\', height: \'100%\', border: 0, top: \'0px\', left: \'0px\', position: \'fixed\' }">\
		</iframe>',
		inject: ['currentLanguage'],
		props: { url: String, startParameters: Object, fullscreen: Boolean },
		data: function() { return { master: null, loaded: null, last: null }},
		mounted: function() { /*jshint unused:false*/ 
			var self = this
			this.master = new CibGetMasterController().createMaster() 
			this.master.registerFunction('onPaymentSucceeded', function(params) { self.$emit('payment-succeeded', params) })
            this.master.registerFunction('toolbarButtonPressed', function(params) { self.$emit('toolbar-button-pressed', params) })
            this.master.registerFunction('onDocumentVersionUpdated', function(params) { self.$emit('document-version-updated', params) })
            this.master.registerFunction('onSignatureFieldSigned', function(params) { self.$emit('signature-field-signed', params) })
            this.master.registerFunction('terminate', function() { self.$emit('terminate') })            
            this.master.registerFunction('getStartParameters', function(params) {
            	self.last = JSON.stringify(self.startParameters)
            	return { 'doxiview': self.startParameters } 
            })
            this.loaded = new Promise(function(resolve) { self.master.registerFunction('applicationStarted', resolve) }) // DOXISAFES-857
			this.master.registerFunction('finishLoading', function() {}) // avoid iwc errors in the log	           
            this.master.openURLInFrame(this.$refs.doxiview, this.url + '/?iwc_mode=internal&locale=' + self.currentLanguage())
		},
		watch: { 
			startParameters: function() { this.last = null } 
		},
		methods: {
			openDocument: function() { // DOXISAFES-1073 this call can break doXiview if the same startParameters have been requested shortly before
				this.loaded.then(function() { 
					if (this.last === JSON.stringify(this.startParameters)) console && console.info('Skipping openDocument call')
					else this.master.callFunction('openDocument') 
				}.bind(this)) 
			}
		}
	}
	
	const Doxiview2 = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/doxiview2.html').then(function(html) {
			return { 
				template: html,
				inject: ['currentLanguage'],
				props: { url: String, startParameters: Object, fullscreen: Boolean, title: String, noHeader: Boolean },
				data: function() { return { 
					doxiview: false, 
					master: null
				} },	
				mounted: function() { this.open() },
				computed: {
					document: function() { return this.startParameters.httpBundle && this.startParameters.httpBundle.documents[0] },
					headers: function() {
						var res = {}
						var auth = this.startParameters.httpBundle && this.startParameters.httpBundle.httpParameters					
						if (auth) res[auth.split(':')[0]] = [this.startParameters.httpBundle.httpParameters.split(':')[1]]
						return res
					},
					downloadUrl: function() {
						if (this.headers.Authorization) 
							return this.document.url + (this.document.url.includes('?') ? '&token=' : '?token=') + this.headers.Authorization[0]
						else return this.document.url
					},
					prepareRequest: function() {
						return this.document && {
							type: 'de.cib.doxiviewnt.rest.WebDocumentRequest',
							url: this.document.url,
							headers: this.headers,
							contentType: this.document.mimeType,
							cacheId: this.document.cacheId
						}
					},
					options: function() { return ['ocr', 'form_creation', 'image_manipulation', 'pdf_manipulation', 'convert', 'pdf_compression', 'redact',
						'anonymization', 'pay_invoice', 'annotations', 'create_invoice', 'pdf_encryption'] 
					}
				},
				watch: { startParameters: function() { this.open() } },
				methods: {
					download: function() { window.open(this.downloadUrl, '_blank') },
					openDocument: function() {}, // just to stay compatible with doxiview component
					open: function() {
						this.doxiview = false						
						if (!this.startParameters.httpBundle) return
						else if (this.startParameters.searchOnOpen || this.startParameters.httpBundle.documents.length > 1) this.doxiview = true
						this.loadDoxiview()	// unsure about this, could speed up loading or cause problems due to concurrency					
					},
					openOptionsbar: function(content) {
						if (content) {
							this.startParameters.optionsbarVisible = true
							this.startParameters.optionsbarContent = content
						} else {
							this.startParameters.optionsbarVisible = false
							this.startParameters.optionsbarContent = null
						}
						this.loadDoxiview()
						this.doxiview = true
					},
					loadDoxiview: function() {
						var vm = this
						if (vm.master) vm.master.callFunction('openDocument')
						else {
							vm.master = new CibGetMasterController().createMaster()
							vm.master.registerFunction('onPaymentSucceeded', function(params) { vm.$emit('payment-succeeded', params) })
				            vm.master.registerFunction('toolbarButtonPressed', function(params) { vm.$emit('toolbar-button-pressed', params) })
				            vm.master.registerFunction('onDocumentVersionUpdated', function(params) { vm.$emit('document-version-updated', params) })
				            vm.master.registerFunction('terminate', function() { vm.$emit('terminate') })            
				            vm.master.registerFunction('getStartParameters', function() { return { doxiview: vm.startParameters } })
							vm.master.registerFunction('finishLoading', function() {}) // avoid iwc errors in the log	           
				           	vm.master.registerFunction('applicationStarted', function() {}) // avoid iwc errors in the log	           
				            vm.master.openURLInFrame(vm.$refs.doxiview, vm.url + '/?iwc_mode=internal&locale=' + vm.currentLanguage())
						}
					}
				}
			}
		})
	})
	
	const Webedit = {
		template: '<iframe ref="webedit" style="border: 0"\
			:style="!fullscreen ? {} : { width: \'100%\', height: \'100%\', top: \'0px\', left: \'0px\', position: \'fixed\' }">\
		</iframe>',
		props: { url: String, startParameters: Object, fullscreen: Boolean },
		data: function() {
			return { 
				master: null,
				content: '',
				isDirty: false
			}
		},
		mounted: function() {
			var self = this
			this.master = new CibGetMasterController().createMaster()
            this.master.registerFunction('getStartParameters', function() { return self.startParameters })
            this.master.registerFunction('fireDataDirty', function(params) { this.isDirty = params.isDirty }.bind(this))
            this.master.registerFunction('finishEditing', function(params) { this.$emit('terminate', params) }.bind(this))
            this.master.registerFunction('editorLoaded', function() {
            		this.$emit('ready')
            		this.master.callFunction('onShow')
            	}.bind(this))
        	this.master.registerFunction('exceptionThrown', function(error) { this.$emit('fail', error) }.bind(this))
        	this.master.registerFunction('loadElementContent', function() { return { rtfContent: this.content } }.bind(this))
        	this.master.registerFunction('saveElementContent', function(callbackId, params) { this.$emit('save-element-content', params) }.bind(this), true)
			this.master.openURLInFrame(this.$refs.webedit, this.url)
		},
		methods: {
			loadElement: function(containerId, elementName, content) {
				this.content = content
				this.master.callFunction('loadElement', { containerId: containerId, elementName: elementName }, {
					error: function(params) { this.$emit('fail', params) }.bind(this)
				})
			},
			setElementList: function(editSession) {
				return new Promise(function(resolve, reject) {
					this.master.callFunction('setElementList', editSession, { success: resolve, error: reject })
				}.bind(this))
			}
		}
	}

	export { Doxiview, Doxiview2, Webedit }