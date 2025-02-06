/* globals console, bootstrap */

	const BToggle = eventBus => {
		return {
			mounted(el, binding) {
				let targetId = Object.keys(binding.modifiers)[0] || binding.value
		
				if (!targetId) {
					console.warn('v-b-toggle requires a modifier or a value with the ID of the target collapse.')
					return
				}
		
				if (!eventBus) {
					console.warn('No event bus found to toggle collapse with ID ' + targetId)
					return
				}
		
				el.addEventListener('click', () => {
					eventBus.emit('bv::toggle::collapse', targetId)
				})
			}
		}
	}
	
	const BDPopover = {
		mounted(el, binding) {
			let content = binding.value || el.getAttribute('data-bs-content') || ''
			let title = el.getAttribute('title') || ''
			let placement = Object.keys(binding.modifiers)[1] || 'top'
			let event = Object.keys(binding.modifiers)[0] || 'hover'
	
			if (!content) return
	
			const popover = new bootstrap.Popover(el, {
				title,
				content,
				placement,
				trigger: event,
				html: true
			})
	
			el._bs_popover = popover
		},
		beforeUnmount(el) {
			if (el._bs_popover) {
				el._bs_popover.dispose()
				delete el._bs_popover
			}
		}
	}	
	
	export { BToggle, BDPopover }