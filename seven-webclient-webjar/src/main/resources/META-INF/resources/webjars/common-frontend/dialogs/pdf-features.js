/* globals File, console, Worker, WebAssembly */
	
	if (typeof WebAssembly === 'object' && typeof WebAssembly.instantiate === 'function') var pdfModuleWorker = new Worker('pdfmodule-worker.js')
	
	const PdfFeatures = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/common-frontend/dialogs/pdf-features.html').then(function(html) {
			return { 
				template: html,
				props: { licenseKey: String, onProgress: Function },
				data: function() { return { files: null, password: null, level: null, param: null, hide: true, processing: false, mode: 0, signed: [] } },
				methods: {
					show: function(fileList, param, mode) { // mode 0=default, 1=encrypt selected, 2=compress selected, 3=compress auto triggered
						if (fileList.length === 0) return
						this.files = []			
						for (var i=0; i < fileList.length; i++) {
							this.files.push(fileList[i])
							if (!fileList[i].name.toLowerCase().endsWith('.pdf')) {
								this.$emit('fail', fileList[i].name)
								return
							}
						}
						this.param = param
						this.mode = mode
						this.level = mode >= 2 ? 'higher' : null
						this.password = mode === 1 ? '' : null
						this.checkIfSigned(this.files).then(res => this.signed = res).finally(() => { 
							if (this.signed.length > 0 && mode === 3) 
								this.$emit('ok', { files: this.files.map(function(file) { return Promise.resolve(file) }), param: param })
							else this.$refs.modal.show()
						})						
					},
					onHide: function(evt) { if (evt.trigger !== 'ok') this.$emit('cancel', this.param) },
					process: function(password, level, files, param) {
						var vm = this
						var spaces = []
						if (level) {
							var proms = work({ licenseKey: vm.licenseKey, operation: 'compress', compressQuality: level }, files)
							if (password) {
								var inter = Promise.all(proms).then(function(res) { 
									return work({ licenseKey: vm.licenseKey, operation: 'encrypt', ownerKey: password, userKey: password }, res)
								})
								proms = proms.map(function(prom, i) { return inter.then(function(proms) { return proms[i] }) })								
							} 
							vm.$emit('ok', { files: proms, param: param, spaces: spaces })
							vm.$refs.modal.hide('ok')							
						} else if (password) {
							var proms2 = work({ licenseKey: vm.licenseKey, operation: 'encrypt', ownerKey: password, userKey: password }, files)
							vm.$emit('ok', { files: proms2, param: param })
							vm.$refs.modal.hide('ok')
						} else {
							vm.$emit('ok', { files: files.map(function(file) { return Promise.resolve(file) }), param: param })
							vm.$refs.modal.hide('ok')
						}
						
						function work(msg, files) { 
							var resolves = [], rejects = []
							return files.map(function(file, i) { return new Promise(function(resolve, reject) { 
								resolves.push(resolve)
								rejects.push(reject)
								if (i+1 === files.length) {
									vm.processing = true
									loop(0)
								}
							}) })
							
							function loop(i) {
								msg.file = files[i]	
								msg.token = files[i].name // enables progress too
								pdfModuleWorker.onmessage = function(res) { // https://developer.mozilla.org/en-US/docs/Web/API/File/File
									if (res.data.progress) {
										if (vm.onProgress) vm.onProgress(res.data.progress.value / files.length + i * 100 / files.length)
										else console.warn('onProgress not available', res.data.progress)
									} else {
										console && console.timeEnd('pdfModule processing ' + files[i].name);										
										(resolves[i])(new File([res.data.blob], files[i].name, { type: res.data.blob.type }))
										if (msg.operation === 'compress')
											spaces.push([files[i].size, res.data.blob.size, (files[i].size - res.data.blob.size) / files[i].size])
										if (i+1 < files.length) loop(i+1)
										else vm.processing = false
									}									
								}
								pdfModuleWorker.onerror = function(evt) { 
									vm.processing = false
									var err = evt.message
									if (evt.message.includes('20011')) err = { type: 'PdfEncryptedException' }
									else if (evt.message.includes('20044')) err = { type: 'PdfProtectedException' }
									rejects[i](err) 
								}
								console && console.time('pdfModule processing ' + files[i].name)								
								pdfModuleWorker.postMessage(msg)
							}	
						}
					},					
					checkIfSigned: function(files) {
						this.processing = true //TODO ? too fast to notice
						var vm = this
						return new Promise(function(resolve, reject) { 
							pdfModuleWorker.onmessage = function(res) {						
								resolve(res.data.filesSigned)
								vm.processing = false														
							}
							
							pdfModuleWorker.onerror = function(evt) { 
								vm.processing = false
								var err = evt.message // we receive "Uncaught Error: Error 99 in pdfModuleJob.getProperty(IsSigned) (-1)" when encrypted
							//	if (evt.message.includes('20011')) err = { type: 'PdfEncryptedException' }
							//	else if (evt.message.includes('20044')) err = { type: 'PdfProtectedException' }
								reject(err) // not doing nothing with that anyway 
							}
							pdfModuleWorker.postMessage({ licenseKey: vm.licenseKey, operation: 'detectsigned', files: files })
						})
					}
				}
			}
		})
	})
	
	const Processing = {
		template: '<b-alert dismissible fade variant="info" :show="countdown" @dismissed="countdown = 0"\
			class="d-flex align-items-center"\
			style="position: fixed; z-index: 1021; top: 60px; left: 50%; transform: translate(-50%, 0)">\
			<div class="mdi mdi-24px mdi-loading mdi-spin mr-2"></div>\
			<div>\
				<div>{{ $t(\'processing.text\') }}</div>\
				<b-progress v-if="progress" class="mt-1" variant="info" :value="progress"></b-progress>\
			</div>\
		</b-alert>',		
		data: function() { return { countdown: false, progress: null } },
		methods: {
			show: function() { this.countdown = true },
			hide: function() { 
				this.countdown = false
				this.progress = null 
			},
			setProgress: function(val) { this.progress = val }
		}		
	}
	
	const SpaceSaved = {
		template: '<success ref="suc" class="align-items-center">\
			<div>\
				<div v-for="space in spaces" v-html="$t(\'space-saved.text\',\
					[ $options.filters.size(space[0]), $options.filters.size(space[1]), Math.floor(space[2] * 100) ])">\
				</div>\
			</div>\
		</success>',		
		data: function() { return { spaces: [] } },
		methods: {
			show: function(spaces) { 
				this.spaces = spaces
				this.$refs.suc.show()
			}
		}		
	}
	
	export { PdfFeatures, Processing, SpaceSaved }