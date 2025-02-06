var PdfModuleJob = (function() {
  "use strict";

  // Imports

  var cibPdfJobCreate
  var cibPdfJobFree
  var cibPdfJobExecute
  var cibPdfJobSetPropertyWSimple
  var cibPdfJobGetPropertyW
  var cibPdfJobUseCallback
  var cibPdfJobLookupFonts

  // pdfModule library

  var jobHandlePtr = 0
  var module = {}
  var utils = {}

  function PdfModuleJob(module) {
    this.module = module;
    utils = new WasmUtils(module);

    cibPdfJobCreate = module.cwrap(
      'jsCibPdfJobCreate', 
      'number', 
      ['number'])
    cibPdfJobFree = module.cwrap(
      'jsCibPdfJobFree', 
      'number', 
      ['number'])
    cibPdfJobExecute = module.cwrap(
      'jsCibPdfJobExecute', 
      'number', 
      ['number'])
    cibPdfJobSetPropertyWSimple = module.cwrap(
      'jsCibPdfJobSetPropertyWSimple', 
      'number', 
      ['number', 'number', 'number'])
    cibPdfJobGetPropertyW = module.cwrap(
      'jsCibPdfJobGetPropertyW', 
      'number', 
      ['number', 'number', 'number', 'number'])
    cibPdfJobUseCallback = module.cwrap(
      'jsCibPdfJobUseCallback', 
      '', 
      ['number', 'number'])
    cibPdfJobLookupFonts = module.cwrap(
      'jsCibPdfJobLookupFonts',
      '', 
      ['number', 'number'])
    create();
 }

  var create = function() {
    jobHandlePtr = utils.allocateUint32(1)
    var err = cibPdfJobCreate(jobHandlePtr)
    if (err) throw new Error('Error ' + err + ' in pdfModuleJob.create')
  };

  PdfModuleJob.prototype.getHandle = function() {
    return jobHandlePtr
  }

  PdfModuleJob.prototype.isValid = function() {
    return utils.getIntFromMem(jobHandlePtr) != 0
  }

  PdfModuleJob.prototype.free = function() {
    var err = cibPdfJobFree(jobHandlePtr)
    if (err) throw new Error('Error ' + err + ' in pdfModuleJob.free')
    utils.freeMemory()
  };

  PdfModuleJob.prototype.enableReport = function(token) {
    cibPdfJobUseCallback(jobHandlePtr, utils.allocateStr32(token))
  }

  PdfModuleJob.prototype.execute = function() {
    var err = cibPdfJobExecute(jobHandlePtr)
    if (err) throw new Error('Error ' + err + ' in pdfModuleJob.execute')
  }

  PdfModuleJob.prototype.executeWithoutThrow = function() {
    return cibPdfJobExecute(jobHandlePtr)
  }

  PdfModuleJob.prototype.setProperty = function(property, value) {
    var err = cibPdfJobSetPropertyWSimple(
      jobHandlePtr, 
      utils.allocateStr32(property), 
      utils.allocateStr32(value))
    if (err) throw new Error('Error ' + err + ' in pdfModuleJob.setProperty(' + property + ', ' + value + ')')
  }

  PdfModuleJob.prototype.lookupFonts = function(lookupInfo) {
  	lookupInfo = lookupInfo || ''
    cibPdfJobLookupFonts(jobHandlePtr, utils.allocateStr32(lookupInfo))
  }

  PdfModuleJob.prototype.getProperty = function(property, reservedSize = 1024) {
    try {
      var size = utils.allocateUint32(reservedSize, false)
      var value = utils.allocateBytes(reservedSize << 2, false)
      var propStr = utils.allocateStr32(property, false)
      var err = cibPdfJobGetPropertyW(
        jobHandlePtr, 
        propStr,
        value,
        size)
      if (err) { 
        if (err === 47) return this.getProperty(property, this.module.HEAPU32[size >> 2])
        throw new Error('Error ' + err + ' in pdfModuleJob.getProperty(' + property + ')')
      }
      return utils.getWStrFromMem(value, this.module.HEAPU32[size >> 2])
    } finally {
      this.module._free(size)
      this.module._free(value)
      this.module._free(propStr)
    }
  }

  return PdfModuleJob
})()
