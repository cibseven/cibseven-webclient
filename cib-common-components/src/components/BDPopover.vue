<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
<script>
import * as Bootstrap from 'bootstrap'

export default {
  mounted(el, binding) {
    let content = binding.value || el.getAttribute('data-bs-content') || ''
    let title = el.getAttribute('title') || ''
    let placement = Object.keys(binding.modifiers)[1] || 'top'
    let event = Object.keys(binding.modifiers)[0] || 'hover'

    if (!content) return

    const popover = new Bootstrap.Popover(el, {
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
</script>

<style scoped>
</style>

