	
	const QuickNavBar = Vue.defineAsyncComponent(() => {
		return axios.get('webjars/seven/components/quick-nav-bar.html').then(function(html) {
			return { 
				template: html
	       	}
		})
	})
	
	const Sidebar = { // https://getbootstrap.com/docs/5.2/examples/sidebars/#
		template: '<div class="d-flex flex-nowrap">\
			<div class="d-flex flex-column border border-top-0"> <slot name="left"></slot> </div>\
  			<div class="h-100" style="width: calc(100% - 48px)"> <slot></slot> </div>\
		</div>'
	}
	
	const SidebarItem = {
		template: '<b-list-group-item class="border-0 py-1 text-center" action :to="to" @click="$emit(\'click\', $event)"\
			style="padding-left: 12px; padding-right: 12px">\
			<div :class="iconClass" :title="tooltip" v-b-popover.hover.right></div>\
		</b-list-group-item>',
		props: { iconClass: String, tooltip: String, to: String }
	}
	
	const SidebarDropright = {
		template: '<b-dropdown dropright variant="transparent" toggle-class="border-0 shadow-none" no-caret>\
			<template #button-content> <span :class="iconClass"></span> </template>\
		    <slot></slot>\
		</b-dropdown>',
		props: { iconClass: String }
	}
	
	export { QuickNavBar, Sidebar, SidebarItem, SidebarDropright }
	