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
const STATE_KEY = '__hideHeaderDirectiveState__'

const hideHeader = {
  mounted(el, binding) {
    let ticking = false
    const originalHeight = el.style.height || `${el.getBoundingClientRect().height}px`

    function applyHeaderVisibility(shouldHide) {
      el.style.overflow = shouldHide ? 'hidden' : ''
      el.style.height = shouldHide ? '0px' : (originalHeight || '')
      el.style.opacity = shouldHide ? '0' : '1'
      el.style.pointerEvents = shouldHide ? 'none' : ''
      if (shouldHide) {
        el.setAttribute('aria-hidden', 'true')
      } else {
        el.removeAttribute('aria-hidden')
      }
      if ('inert' in el) {
        el.inert = shouldHide
      }
    }

    el.style.transition = 'height 0.3s ease, opacity 0.3s ease'

    function handleScroll(payload) {
      const { y, delta, direction } = payload

      if (binding.instance?.isCollapsed) {
        binding.instance.hideHeader = false
        applyHeaderVisibility(false)
        ticking = false
        return
      }

      if (ticking) return
      ticking = true

      if (Math.abs(delta) < 8) {
        ticking = false
        return
      }

      const shouldHide = y < 40 ? false : direction === 'down'
      binding.instance.hideHeader = shouldHide
      applyHeaderVisibility(shouldHide)
      ticking = false
    }

    binding.instance.$eventBus.on('scrollOnMobile', handleScroll)
    el[STATE_KEY] = { handleScroll, originalHeight }
  },
  unmounted(el, binding) {
    const state = el[STATE_KEY]
    if (!state) return
    el.style.height = state.originalHeight
    el.style.overflow = ''
    el.style.opacity = ''
    el.style.pointerEvents = ''
    el.style.transition = ''
    binding.instance?.$eventBus?.off('scrollOnMobile', state.handleScroll)
    delete el[STATE_KEY]
  }
}

export default hideHeader