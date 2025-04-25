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

export default BToggle
</script>

<style scoped>
</style>
