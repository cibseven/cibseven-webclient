var WasmUtils = (function() {
  'use strict';

  var _ptrFreeArr = []
  var _module = {}

  function WasmUtils(module) {
      _module = module;
  }
  
  WasmUtils.prototype._stringToUTF8 = function(str) {
    var charList = str.split(''), uintArray = []
    for (var i = 0; i < charList.length; i++) {
      uintArray.push(charList[i].charCodeAt(0))
    }
    return new Uint8Array(uintArray)
  }
  
  WasmUtils.prototype._stringToUTF32 = function(str) {
    var charList = str.split(''), uintArray = []
    for (var i = 0; i < charList.length; i++) {
      uintArray.push(charList[i].charCodeAt(0))
    }
    return new Uint32Array(uintArray)
  }
  
  WasmUtils.prototype.allocateStr8 = function(str) {
    var propArr = this._stringToUTF8(str + '\0')
    var propPtr = _module._malloc(propArr.length)
    _module.HEAPU8.set(propArr, propPtr)
    this.addPtrToFree(propPtr)
    return propPtr
  }
  
  WasmUtils.prototype.allocateStr32 = function(str, addToFree = true) {
    var propArr = this._stringToUTF32(str + '\0')
    var propPtr = _module._malloc(propArr.length << 2)
    _module.HEAPU32.set(propArr, propPtr >> 2)
    if (addToFree) this.addPtrToFree(propPtr)
    return propPtr
  }
  
  WasmUtils.prototype.allocateUint32 = function(n, addToFree = true) {
    var arr = new Uint32Array([n])
    var ptr = _module._malloc(arr.length)
    _module.HEAPU32.set(arr, ptr >> 2)
    if (addToFree) this.addPtrToFree(ptr)
    return ptr
  }
  
  WasmUtils.prototype.allocateBytes = function(n, addToFree = true) {
    var arr = new Uint8Array(n)
    var ptr = _module._malloc(arr.length)
    _module.HEAPU8.set(arr, ptr)
    if (addToFree) this.addPtrToFree(ptr)
    return ptr
  }
  
  WasmUtils.prototype.getWStrFromMem = function(ptr, size) {
    var str = ''
    ptr >>= 2
    var limit = ptr + size
    var chunkSize = 10000
    while (ptr < limit) {
      str += String.fromCharCode.apply(null, _module.HEAPU32.subarray(ptr, Math.min(ptr + chunkSize, limit)))
      ptr += chunkSize
    }
    return str
  }

  WasmUtils.prototype.getChunkFromMem = function(ptr, size) {
    return _module.HEAPU8.subarray(ptr, ptr + size)
  }

  WasmUtils.prototype.getIntFromMem = function(ptr)
  {
    return _module.HEAPU32[ptr >> 2];
  }
  
  WasmUtils.prototype.setIntInMem = function(ptr, value)
  {
    _module.HEAPU32[ptr >> 2] = value;
  }
  
  WasmUtils.prototype.addPtrToFree = function(ptr) {
    _ptrFreeArr.push(ptr)	
  }
  
  WasmUtils.prototype.freeMemory = function() {
    for (var i = 0; i < _ptrFreeArr.length; i++) {
      _module._free(_ptrFreeArr[i])
    }
    _ptrFreeArr = []
  }

  return WasmUtils
})()
