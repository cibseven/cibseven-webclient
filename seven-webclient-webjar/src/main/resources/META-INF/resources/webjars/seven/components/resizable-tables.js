/* globals document, window */

var resizableTablesMixin = {
	mounted: function() {
		// Create a grip with eventListener inside every th to handle resize events
		var thElm = null
	    var startOffset
		var ths = document.querySelectorAll("thead.resizable th")
		
		ths.forEach((th, index) => {
			// Remove max-width property from ths to allow resize, convert into initial width property
			var computedWidth = window.getComputedStyle(th).getPropertyValue('max-width')
		  	th.style.width = computedWidth
		  	th.style.maxWidth = 'none'
		  			  	
			if (index < ths.length - 1) {
				var grip = document.createElement('div')
				Object.assign(grip.style, {
					top: 0, right: '-1px', bottom: 0, width: '10px',
					position: 'absolute', cursor: 'col-resize',
					borderRight: '1px solid var(--bs-light)'
				})
				
				grip.addEventListener('mousedown', function (e) {
					// Convert every th width from '%' to 'px' and prevent 'sorting column'
					var auxThs = document.querySelectorAll("thead.resizable th")
				    auxThs.forEach(th => {
				        var thWidthInPixels = th.offsetWidth
				        th.style.width = thWidthInPixels + 'px'
				    })
					thElm = th
					startOffset = th.offsetWidth - e.pageX
					thElm.style.pointerEvents = 'none'
				})				
				
				th.appendChild(grip)
			}
		})
					
	    document.addEventListener('mousemove', function (e) {
		    if (thElm) {			
			    var newWidth = startOffset + e.pageX
		        var widthChange = newWidth - thElm.offsetWidth	
		                
		        // Not allow resize less than 100px
		        if (thElm.offsetWidth + widthChange < 100) return	
		        	
		        // Apply changes to the current column
		        thElm.style.width = newWidth + 'px'		
		        
		        // Get the next column
		        var ths = Array.from(thElm.parentElement.children)
		        var index = ths.indexOf(thElm)
		        var adjacentTh = ths[index + 1] || ths[index - 1]
		        
		        if (adjacentTh) {
					// Not allow resize less than 100px
					if (adjacentTh.offsetWidth - widthChange < 100) return	
		            var adjacentThNewWidth = adjacentTh.offsetWidth - widthChange	
			        adjacentThNewWidth = Math.max(adjacentThNewWidth, 0)		
			        // Apply changes to the next column
			        adjacentTh.style.width = adjacentThNewWidth + 'px'
		        }
		    }
	    })
	    	
	    document.addEventListener('mouseup', function () {
			// Convert every th width from 'px' to '%' again
			var ths = document.querySelectorAll("thead.resizable th")
		    ths.forEach(th => {	
		        var parentWidth = th.parentElement.offsetWidth
		        var thWidthInPixels = (th.offsetWidth / parentWidth) * 100       	
		        th.style.width = thWidthInPixels + '%'
		    })
			if (thElm) thElm.style.pointerEvents = 'auto'
		    thElm = null
	    })   
	}
}

export { resizableTablesMixin }