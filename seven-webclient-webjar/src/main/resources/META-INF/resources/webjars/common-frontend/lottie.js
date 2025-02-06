(function() { /* globals lottie */
	"use strict"; // inspired by https://github.com/chenqingspring/vue-lottie and uses https://github.com/airbnb/lottie-web
	
	Vue.component('lottie', {
		template: '<div :style="style" ref="lavContainer"></div>',
		props: { 
			path: String,
			autoplay: { type: Boolean, default: true },
			loop: { type: Boolean, default: true },
			height: Number, width: Number
		},
		computed: {
			style: function() {
				return {
		        	width: this.width ? this.width + 'px' : '100%',
				    height: this.height ? this.height + 'px' : '100%',
				    overflow: 'hidden',
				    margin: '0 auto'
				}
			}
		},
		mounted: function() {
		    var anim = lottie.loadAnimation({
		          container: this.$refs.lavContainer,
		          renderer: 'svg',
		          path: this.path,		          
		          loop: this.loop !== false,
		          autoplay: this.autoplay !== false,	        
//		          animationData: this.animationData,
//		          rendererSettings: this.rendererSettings
		     })
		     this.$emit('anim-created', anim)
		}
	})
	
})()