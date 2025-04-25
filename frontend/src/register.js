/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import { registerComponents } from 'cib-common-components'
import { GlobalEvents } from 'vue-global-events'
import { HoverStyle } from '@/components/common-components/directives.js'
import ErrorDialog from '@/components/common-components/ErrorDialog.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'

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
}

export default registerOwnComponents
