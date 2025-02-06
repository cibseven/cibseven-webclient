(function() { /* globals self, pdfModule, postMessage, Uint8Array, Blob, FileReader, console, WasmUtils, PdfModuleJob, URL, setTimeout */
	'use strict';

	function raiseError(errMessage, code) {
		code = code || -1
		setTimeout(function() { throw new Error(errMessage + ' (' + code + ')') })
	}

	self.onmessage = function() {
		console && console.log('module is not ready yet')
	}

	var pdfmSrc = 'lib/pdfmodule.js'
	// if (typeof SharedArrayBuffer !== 'undefined')
	// {
	// 	PDFMsrc = 'lib/pdfmodulet.js'
	// }

	self.importScripts(pdfmSrc, 'lib/pdfmodule-wasm.js', 'lib/pdfmodule-job.js')

	function processFile(module, utils, filedata) {
		var fileMapped = new Uint8Array(filedata)
		var fileMappedPtr = module._malloc(fileMapped.length)
		module.HEAPU8.set(fileMapped, fileMappedPtr)
		utils.addPtrToFree(fileMappedPtr)
		return { ptr: fileMappedPtr, length: fileMapped.length }
	}

	/*
	 * Rendering:
	 * Feature needs some enhacments, but it is usable for thumbnail rendering
	 * Mandatory fields:
	 * - token: worker's setting used as rendering cache key, use it in consecutive calls
	 * - page: page of document to render, pages start at 1
	 * Optional fields:
	 * - scale: <width><unit>;<height><unit>|<dpivalue>dpi (see more in documentation, ImageScaling)
	 * - free: if set, it will free pdfModule resources (normally set at the last page rendered)
	 * 
	 * On first call, the message returned will include the page count (pageCount), it is 
	 * expected that the user uses this information for consecutive calls
	 */
	var jobCache = {}
	
	function textExtractionUseCase(module, data) {
        data.set = {
            'TextExtraction': '1',
            'FillTextOutput': '1',
            'TextExtraction.FormattingOptions': '{"OutputFormats":["txt","hocr","json"]}',
            'Threads': '1'
        }

        data.get = {
            'TextOutput.Text': null,
            'TextOutput.Hocr': null,
			'TextOutput.Json': null
        }

        fileIOUseCase(module, data)
    }

	var fontsFallbacks = {
		'GoogleWebFonts': 'https://www.googleapis.com/webfonts/v1/webfonts?key=AIzaSyCk5UnMmdDTqM25_SGUgFhYnqvHCQeIZOc',
		'FallbackFonts': {
			'FreeSans4000': 'fonts/FreeSans-LrmZ.ttf',
			'FreeSans4001': 'fonts/FreeSansOblique-ol30.ttf',
			'FreeSans7000': 'fonts/FreeSansBold-Xgdd.ttf',
			'FreeSans7001': 'fonts/FreeSansBoldOblique-g9mR.ttf'
		}
	}

	/*
	 * Mandatory fields:
	 * - Page: page to be rendered
	 */
	function renderUseCase(module, data) {
		if (!data.hasOwnProperty('page')) raiseError('Setting \'page\' is missing')

		data.set = {
			'Render': '1',
			'RenderPageSelection': data.page,
			'RenderOutputType': 'Memory',
			'FontWorkSpace': '.',
			'ImageType': data.imageType ? data.imageType : 'BMP',
			'Threads': '1'
		}

		data.get = {
			'PageCount': null
		}

		// Indexes for PageInfo start at 0.
		data.get['PageInfo.' + (data.page - 1)] = null

		fileIOUseCase(module, data)
	}

	/*
	 * Mandatory fields:
	 * - TextOverlay: pdfModule's TextOverlay
	 */
	function pageNumbersUseCase(module, data) {
		if (!data.hasOwnProperty('textOverlay')) {
			raiseError('Setting \'textOverlay\' is missing')
		}

		data.set = {
			'TextOverlay': data.textOverlay,
			'Threads': '1'
		}

		fileIOUseCase(module, data)
	}

	/*
	 * Mandatory fields:
	 * - InputFilename: pdfModule's InputFilename
	 */
	function mergeUseCase(module, data) {
		if (!data.hasOwnProperty('inputFilename')) {
			raiseError('Setting \'inputFilename\' is missing')
		}

		data.set = {
			'InputFilename': data.inputFilename,
			'Threads': '1'
		}

		if (data.addFilesReferences) {
			data = addFilesReferences(data)
		}

		fileIOUseCase(module, data)
	}

	/*
	 * Mandatory fields:
	 * - pagesToSplit: A Array that each position is a array that contains the start and finish pages to split.
	 */
	function splitUseCase(module, data) {
		if (!data.hasOwnProperty('pagesToSplit')) {
			raiseError('Setting \'pagesToSplit\' is missing')
		}

		data.dontSendPostMessage = true

		var filesToReturn = []

		var file = jobModuleCache[data.token + '_files'][data.key]
		var inputFileNameSuffix = '#;' + file.pointer + '#' + file.length

		var job = jobModuleCache[data.token]
		var utils = new WasmUtils(module)

		if (!job) {
			job = createJob(module)
			job.setProperty('LicenseKey', data.licenseKey)
		}

		data.pagesToSplit.forEach(function(range) {
			var rangeOnePage = range[0] === range[1]
			var rangeToSplit = rangeOnePage ? range[0] : range[0] + '-' + range[1]
			var inputFilename = '{' + rangeToSplit + '};' + inputFileNameSuffix
			job.setProperty('InputFilename', inputFilename)
			job.setProperty('OutputInMemory', '1')
			var response = executeJob(module, job, data, utils)

			filesToReturn.push({
				content: response.blob,
				filenamePrefix: rangeToSplit
			})
		})

		var message = {
			blobs: filesToReturn,
			id: data.id
		}

		postMessage(message)

		job.free()
		jobModuleCache = {}
	}

	/*
	 * Mandatory fields:
	 * - PageRotation: pdfModule's PageRotation
	 */
	function rotateUseCase(module, data) {
		if (!data.hasOwnProperty('pageRotation')) raiseError('Setting \'pageRotation\' is missing')
		data.set = {
			'PageRotation': data.pageRotation,
			'MinPdfVersion': '2.0',
			'Threads': '1'
		}
		fileIOUseCase(module, data)
	}

	function imageToPdfUseCase(module, data) {
		data.set = {
			'MinPdfVersion': '2.0',
			'Threads': '1',
			'InputImageScaling': data.inputImageScaling,
			'InputFilename': data.inputFilename,
			'InputImageMargin': data.inputImageMargin
		}

		if (data.addFilesReferences) {
			data = addFilesReferences(data)
		}

		fileIOUseCase(module, data)
	}

	/*
	 * Mandatory fields:
	 * - compressQuality: pdfModule's CompressQuality
	 */
	function compressUseCase(module, data) {
		if (!data.hasOwnProperty('compressQuality')) raiseError('Setting \'compressQuality\' is missing')
		data.set = {
			'Compress': '1',
			'CompressQuality': data.compressQuality,
			'MinPdfVersion': '1.7',
			'Threads': '1',			
			'CopyOnlyWithSignature': '0',
			'FlattenSignatures': '1'
		}
		fileIOUseCase(module, data)
	}

	/*
	 * Optional fields:
	 * - ownerKey: pdfModule's OutputOwnerPassword
	 * - userKey: pdfModule's OutputUserPassword
	 * - printingPerm, clipboardPerm, formsPerm, assemblingPerm, notesPerm, modifyPerm, extractPerm: pdfModule's EncryptEnableXYZ flags
	 */
	function encryptionUseCase(module, data) {
		data.set = {
			'OutputOwnerPassword': data.ownerKey || '',
			'OutputUserPassword': data.userKey || '',
			'EncryptEnablePrinting': data.printingPerm || '1',
			'EncryptEnableClipboard': data.clipboardPerm || '1',
			'EncryptEnableForms': data.formsPerm || '1',
			'EncryptEnableAssembling': data.assemblingPerm || '1',
			'EncryptEnableNotes': data.notesPerm || '1',
			'EncryptEnableModifying': data.modifyPerm || '1',
			'EncryptEnableExtract': data.extractPerm || '1',
			'MinPdfVersion': '2.0',
			'Threads': '1'
		}
		fileIOUseCase(module, data)
	}

	/*
	 * Mandatory field: 
	 * - password: worker's optional field is required here
	 */
	function decryptionUseCase(module, data) {
		if (!data.hasOwnProperty('password')) raiseError('Setting \'password\' is missing')
		data.set = {
			'GrantPdfPermissions': '1',
			'RemoveEncryption': '1',
			'MinPdfVersion': '2.0',
			'Threads': '1'
		}
		fileIOUseCase(module, data)
	}

	function exportImagesUseCase(module, data) {
		data.set = {
			'ExtractImages': 'images',
			'ExtractImages.AllowBmp': '1',
			'ExtractImages.AllowJpeg': '1',
			'ExtractImages.CombinedMasks': '1',
			'Threads': '1'
		}

		fileIOUseCase(module, data)
	}

	function executeJob(module, job, data, utils) {

		if (data.set) {
			Object.keys(data.set).forEach(function(key) {
				job.setProperty(key, data.set[key])
			})
		}

		if (data.id || data.token) {
			data.id ? job.enableReport(data.id) : job.enableReport(data.token)
		}

		if (data.password) {
			job.setProperty('EncryptDocumentPassword', data.password)
		}

		if (data.log) {
			job.setProperty('TraceFilename', 'log.txt')
		}

		if (data.scale) {
			job.setProperty('ImageScaling', data.scale)
		}

		if (data.lookupFonts) {
			job.lookupFonts(JSON.stringify(fontsFallbacks))
		}

		if (data.images) {
			module.FS.mkdir('images')
		}

		if (data.grantPermissions) {
			job.setProperty('GrantPdfPermissions', '1')
		}

		job.execute()
		var output = job.getProperty('MemoryOutput')
		var memFile = output.split(';')
		var message = {}

		if (data.images) {

			var pathsToImages = module.FS.lookupPath('images').node.contents
			var messageBlobs = []

			Object.keys(pathsToImages).forEach(function(image) { // Getting each file from the pdf. There are saved in the virtual fs.
				var imageObject = pathsToImages[image]
				if (imageObject && imageObject.name && imageObject.name.includes('.')) {

					var content = new Blob([module.FS.readFile('images/' + imageObject.name)], { type: data.fileType })

					var pdfImageData = {
						content: content,
						// format name: 'data source of length 162137-page00001-00001.jpg' We get page00001-00001.jpg
						name: imageObject.name.split(/\s\d{1,}-/gm)[1],
						imageLink: URL.createObjectURL(content)
					}

					messageBlobs.push(pdfImageData)
					module.FS.unlink('images/' + imageObject.name) // Delete file from the virtual fs.
				}
			})

			module.FS.rmdir('images')

			message.blob = messageBlobs
		} else {
			var fileType = data.fileType ? data.fileType : 'application/pdf'
			message.blob = new Blob([new Uint8Array(utils.getChunkFromMem(parseInt(memFile[1]), parseInt(memFile[2])))], { type: fileType })
		}

		if (data.get) {
			message.get = {}
			Object.keys(data.get).forEach(function(key) {
				message.get[key] = job.getProperty(key)
			})
		}

		if (data.token) {
			message.token = data.token
		}

		if (data.log) {
			message.log = module.FS.readFile('log.txt', { encoding: 'utf8' })
		}

		if (data.dontSendPostMessage) {
			return message
		}

		if (data.saveJob && !data.free) {
			jobCache[data.token] = job

			if (data.fileReference) {
				jobModuleCache[data.token + '_files'][data.fileReference.key] = data.fileReference
			}

		} else {
			job.free()
			jobModuleCache = {}
		}

		message.id = data.id
		postMessage(message)
	}

	/*
	 * Optional fields:
	 * - set: dictionary of properties to be set
	 * - get: array of properties to retrieve
	 */
	function fileIOUseCase(module, data) {
		var job = jobModuleCache[data.token]
		var files = jobModuleCache[data.token + '_files']
		var utils = new WasmUtils(module)

		if (!job) {
			job = createJob(module)
			job.setProperty('LicenseKey', data.licenseKey)
		}

		job.setProperty('OutputInMemory', '1')

		try {
			if (files && Object.keys(files).length > 0) {
				if (data.fileKey) {
					var file = files[data.fileKey]

					if (file) {
						job = loadFileInJob(job, file)
					} else {
						readFileAndExecuteJob(module, job, data, utils)
						return
					}
				}

				if (data.loadAllFiles) {
					job = loadAllFilesInJob(job, files)
				}

				executeJob(module, job, data, utils)
			} else {
				readFileAndExecuteJob(module, job, data, utils)
			}
		} catch (err) {
			var message = {
				error: err.message ? err.message : err,
				id: data.id
			}

			if (data.log) {
				message.log = module.FS.readFile('log.txt', { encoding: 'utf8' })
			}

			postMessage(message)
		}
	}

	function readFileAndExecuteJob(module, job, data, utils) {
		if (!data.hasOwnProperty('file')) raiseError('Setting \'file\' is missing')
		var fr = new FileReader()
		fr.onload = function() {
			var file = processFile(module, utils, fr.result)
			job.setProperty('InputFilename', '#;' + file.ptr + '#' + file.length)

			if (data.password) {
				job.setProperty('EncryptDocumentPassword', data.password)
			}

			data.fileReference = {
				pointer: file.ptr,
				length: file.length,
				password: data.password ? data.password : '',
				key: data.file.lastModified + data.file.name
			}

			try {
				executeJob(module, job, data, utils)
			} catch (err) {
				var error = err.message ? err.message : err
				raiseError(error)
			}

		}

		fr.readAsArrayBuffer(data.file)
	}

	function loadAllFilesInJob(job, files) {
		var inputFilename = ''
		var inputPassword = ''

		files.forEach(function(file) {
			inputFilename += '#;' + file.pointer + '#' + file.length
			inputPassword += file.password ? file.password + ';' : ';'
		})

		job.setProperty('InputFilename', inputFilename)
		job.setProperty('EncryptDocumentPassword', inputPassword)

		return job
	}

	function loadFileInJob(job, file) {
		job.setProperty('InputFilename', '#;' + file.pointer + '#' + file.length)

		if (file.password) {
			job.setProperty('EncryptDocumentPassword', file.password)
		}

		return job
	}

	var base = {
		locateFile: function(file) { return 'lib/' + file },
		mainScriptUrlOrBlob: pdfmSrc
		//onRuntimeInitialized: function () { console.log('ready') },
	}

	function addFilesReferences(data) {
		var filesKeys = Object.keys(jobModuleCache[data.token + '_files'])
		var passwords = data.inputPasswords

		if (filesKeys.length > 0) {
			filesKeys.forEach(function(fileKey) {
				var file = jobModuleCache[data.token + '_files'][fileKey]
				data.set.InputFilename = data.set.InputFilename.replaceAll('Pointer' + file.key, file.pointer).replaceAll('Length' + file.key, file.length)
				passwords = passwords.replaceAll(file.key, file.password ? file.password : '')
			})
		}
		data.password = passwords

		return data
	}

	function getFiles(files) {
		return Promise.all(files.map(function(file) {
			return new Promise(function(resolve) {
				var fileReader = new FileReader()

				fileReader.onloadend = function() {
					resolve({ result: fileReader.result, key: file.lastModified + file.name, name: file.name })
				}

				fileReader.readAsArrayBuffer(file)
			})
		}))
	}
	
	function detectFilesAreSigned(module, data) {
		var job = jobModuleCache[data.token]
		var utils = new WasmUtils(module)

		if (!job) {
			job = createJob(module, data.token)
		}

		var filePromises = getFiles(data.files)

		filePromises.then(function(results) {
			var filesSigned = []

			job.setProperty('LicenseKey', data.licenseKey)

			results.forEach(function(result, idx) {
				var file = processFile(module, utils, result.result)
				job.setProperty('InputFilename', '#;' + file.ptr + '#' + file.length)

				var code = job.executeWithoutThrow()
				
				var isSigned = job.getProperty('IsSigned').substring(0, 1)
				isSigned === '1' && !code && filesSigned.push(idx)

				if (code) {
					if (code !== 20011) { // This happens when is not a expected code ( 20011 means encrypted file )
						raiseError('Error ' + code + ' in pdfModuleJob.execute', code)
						return
					}
				} else {
					var fileReference = {
						pointer: file.ptr,
						length: file.length,
						key: result.key
					}

					jobModuleCache[data.token + '_files'][result.key] = fileReference
				}
			})

			var response = {
				filesSigned: filesSigned,
				id: data.id
			}
			postMessage(response)
		}).catch(function(err) {			
			var error = err.message ? err.message : err
			raiseError(error)
		})
	}

	function detectFilesAreEncrypted(module, data) {
		var job = jobModuleCache[data.token]
		var utils = new WasmUtils(module)

		if (!job) {
			job = createJob(module, data.token)
		}

		var filePromises = getFiles(data.files)

		filePromises.then(function(results) {
			var filesEncrypted = []
			var filesProtected = []

			job.setProperty('LicenseKey', data.licenseKey)

			results.forEach(function(result, idx) {
				var file = processFile(module, utils, result.result)
				job.setProperty('InputFilename', '#;' + file.ptr + '#' + file.length)

				if (data.passwords[result.key]) {
					job.setProperty('EncryptDocumentPassword', data.passwords[result.key])
				}

				var code = job.executeWithoutThrow()

				try {
					var isProtected = job.getProperty('EncryptEnableAssembling').substring(0, 1)
					isProtected === '0' && !code && filesProtected.push(idx)
				} catch (_e) {
					// File is not protected
					// If this happen, means that the property doesn't exists in the job, this shouldn't happen in the normal behaviour
					// Expect it to be fixed in the release of pdfm.
				}

				if (code) {
					if (code !== 20011) { // This happens when is not a expected code ( 20011 means encrypted file )
						postMessage({ id: data.id, error: new Error('Error ' + code + ' in pdfModuleJob.execute') })
						return
					}
					filesEncrypted.push(idx)
				} else {
					var fileReference = {
						pointer: file.ptr,
						length: file.length,
						password: data.passwords[result.key],
						key: result.key
					}

					jobModuleCache[data.token + '_files'][result.key] = fileReference
				}
			})

			var response = {
				filesEncrypted: filesEncrypted,
				filesProtected: filesProtected,
				id: data.id
			}
			postMessage(response)
		}).catch(function(err) {
			var error = err.message ? err.message : err
			postMessage({ id: data.id, error: error })
		})
	}

	function freeJob(data) {
		var job = jobModuleCache[data.token]

		if (!job) {
			return
		}

		jobModuleCache = {}
		job.free()
	}

	var jobModuleCache = {}
	function createJob(module, token) {
		var job = new PdfModuleJob(module)
		jobModuleCache[token] = job
		jobModuleCache[token + '_files'] = []
		return job
	}

	/*
	 * Mandatory fields:
	 * - operation: encrypt/decrypt/compress/fileio/render
	 * - licenseKey: valid domain license
	 * - file: input file
	 * Optional fields
	 * - password: needed if file is encrypted
	 * - token: token that is returned on messages from this worker
	 * - log: whether you want a trace file returned
	 */
	pdfModule(base).then(function(factory) {
		factory.print = function(text) {
			console && console.debug('STDOUT: ' + text)
		}
		factory.printErr = function(text) {
			console && console.debug('STDERR: ' + text)
		}
		self.onmessage = function(msg) {
			var data = msg.data
			if (!data.hasOwnProperty('operation')) raiseError('Setting \'operation\' is missing')
			switch (data.operation.toLowerCase()) {
				case 'encrypt':
					encryptionUseCase(factory, data)
					break
				case 'decrypt':
					decryptionUseCase(factory, data)
					break
				case 'compress':
					compressUseCase(factory, data)
					break
				case 'fileio':
					fileIOUseCase(factory, data)
					break
				case 'render':
					renderUseCase(factory, data)
					break
				case 'detectencrypted':
					detectFilesAreEncrypted(factory, data)
					break
				case 'detectsigned':
					detectFilesAreSigned(factory, data)
					break
				case 'freejob':
					freeJob(data)
					break
				case 'rotate':
					rotateUseCase(factory, data)
					break
				case 'export-images':
					exportImagesUseCase(factory, data)
					break
				case 'image-to-pdf':
					imageToPdfUseCase(factory, data)
					break
				case 'merge':
					mergeUseCase(factory, data)
					break
				case 'split':
					splitUseCase(factory, data)
					break
				case 'page-numbers':
					pageNumbersUseCase(factory, data)
					break
				case 'text-extraction':
					textExtractionUseCase(factory, data)
					break
				default:
					raiseError('Setting \'operation\' is not supported (yet)')
			}
		}
	})
})()
