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

const STATE_KEY = '__scrollOnMobileDirectiveState__'

function buildPayload(currentY, delta) {
  return {
    y: currentY,
    delta,
    direction: delta > 0 ? 'down' : 'up'
  }
}

const scrollOnMobile = {
    mounted(el, binding) {
        let lastY = el.scrollTop ?? 0
    
        const onScroll = () => {
          if (window.innerWidth > 768) return
          const currentY = el.scrollTop ?? 0
          const delta = currentY - lastY
          const threshold = 8
          if (Math.abs(delta) < threshold) {
            return
          }
          const payload = buildPayload(currentY, delta)
          binding.instance?.$eventBus.emit('scrollOnMobile', payload)
          lastY = currentY

    }
    el.addEventListener('scroll', onScroll, { passive: true })

    el[STATE_KEY] = {
      onScroll,
      eventName: 'scrollOnMobile'
    }
},

  beforeUnmount(el) {
    const state = el[STATE_KEY]
    if (state?.onScroll) {
      el.removeEventListener('scroll', state.onScroll)
    }
    delete el[STATE_KEY]
  }
}
export default scrollOnMobile