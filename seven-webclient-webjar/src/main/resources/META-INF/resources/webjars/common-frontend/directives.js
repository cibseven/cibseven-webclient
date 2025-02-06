	
	const HoverStyle = {
	    inserted: function(el, binding) {
			var prevStyle = {}
	        el.addEventListener('mouseover', function() {
				if (binding.value.styles) {					
					prevStyle = JSON.parse(JSON.stringify(el.style))
					Object.keys(binding.value.styles).forEach(function(sk) {
						el.style[sk] = binding.value.styles[sk]
					})
				}
				if (binding.value.classes) {
					binding.value.classes.forEach(function(cls) { 
						el.classList.add(cls)
					})
				}
	        })
			el.addEventListener('mouseleave', function() {
				if (binding.value.styles) {						
					Object.keys(binding.value.styles).forEach(function(sk) {
						el.style[sk] = prevStyle[sk] ? prevStyle[sk] : null
					})
				}
				if (binding.value.classes) {
					binding.value.classes.forEach(function(cls) { 
						el.classList.remove(cls)
					})
				}
	        })
	    }
	}

	export { HoverStyle }