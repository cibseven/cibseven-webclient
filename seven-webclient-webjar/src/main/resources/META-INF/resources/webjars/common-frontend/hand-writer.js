(function() { /* globals SignaturePad, window, Uint32Array */
	"use strict";	
	Vue.component('hand-writer', function(resolve) {
		axios.get('webjars/common-frontend/hand-writer.html').then(function(html) {
			resolve({ 
				template: html,
				props: {
					penColor: String,
					hasUserWriten: Boolean
				},
				data: function() {
					return {
						signaturePad: null,
						canvas: null
					}
				},
				mounted: function() {
					this.canvas = this.$refs.canvasDraw
					this.signaturePad = new SignaturePad(this.canvas, {
						penColor: this.penColor,
						onEnd: function() {
							this.$emit('update:hasUserWriten', true)
						}.bind(this)
					})
					this.resizeCanvas()
					window.addEventListener('resize', this.resizeCanvas)
				},
				watch: {
					penColor: function(newColor) {
						this.signaturePad.penColor = newColor
						this.setCurrentColor(newColor)
					}
				},
				methods: {
					resizeCanvas: function() {
					    var ratio =  Math.max(window.devicePixelRatio || 1, 1)
					    this.canvas.width = this.canvas.offsetWidth * ratio
					    this.canvas.height = this.canvas.offsetHeight * ratio
					    this.canvas.getContext('2d').scale(ratio, ratio)
					    this.signaturePad.clear()
					},
					clearSignature: function() {
						this.signaturePad.clear()
					},
					prepareContent: function() {
						this.canvas.toBlob(function(blob) {
							this.$emit('content', blob)
						}.bind(this))
					},
					setCurrentColor: function(color) {
					   var context = this.canvas.getContext('2d')
					   context.save()
					   context.fillStyle = color
					   context.globalCompositeOperation = 'source-in'
					   context.fillRect(0, 0, this.canvas.width, this.canvas.height)
					   context.restore()
					},
					isBlankCanvas: function() {
						var context = this.canvas.getContext('2d')
					  	var pixelBuffer = new Uint32Array(
					    	context.getImageData(0, 0, this.canvas.width, this.canvas.height).data.buffer
					  	)
					  	return !pixelBuffer.some(function(color) { return color !== 0 })
					}
				},
				beforeDestroy: function() {
					window.removeEventListener('resize', this.resizeCanvas)
				}
	       	})		
		})
	})
})()