import { registerComponents } from 'cib-common-components'
import { GlobalEvents } from 'vue-global-events'
import { HoverStyle } from '@/components/common-components/directives.js'
import ErrorDialog from '@/components/common-components/ErrorDialog.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import VueApexCharts from "vue3-apexcharts"

const registerOwnComponents = function(app) {

  registerComponents(app)

  app.component('error-dialog', ErrorDialog)
  app.component('confirm-dialog', ConfirmDialog)

  // ALIASES
  app.component('b-dd', app.component('b-dropdown'))
  app.component('b-dd-form', app.component('b-dropdown-form'))

  app.directive('hover-style', HoverStyle)
  app.directive('block-truncate', {
      inserted: function(el) {
      // Check if the block's height is smaller than the text content height. If so
          // add an ellipsis replacing the last word
        while (el.clientHeight < el.scrollHeight) {
        el.innerHTML = el.innerHTML.replace(/\W*\s(\S)*$/, '...')
        }
      },
    update: function(el, binding) {
      el.innerHTML = binding.value.text
      while (el.clientHeight < el.scrollHeight) {
        el.innerHTML = el.innerHTML.replace(/\W*\s(\S)*$/, '...')
        }
    }
  })

  app.component('GlobalEvents', GlobalEvents)

  app.component('apex-chart', VueApexCharts)
}

export default registerOwnComponents
