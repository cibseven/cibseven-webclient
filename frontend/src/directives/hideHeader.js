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

function restoreOriginal(el, original) {
  el.style.height = original.height
  el.style.overflow = original.overflow
  el.style.opacity = original.opacity
  el.style.pointerEvents = original.pointerEvents
  if (original.ariaHidden !== null) {
    el.setAttribute('aria-hidden', original.ariaHidden)
  } else {
    el.removeAttribute('aria-hidden')
  }
  if ('inert' in el) {
    el.inert = original.inert
  }
}

const hideHeader = {
  mounted(el, binding) {
    const original = {
      height: el.style.height,
      overflow: el.style.overflow,
      opacity: el.style.opacity,
      pointerEvents: el.style.pointerEvents,
      transition: el.style.transition,
      ariaHidden: el.getAttribute('aria-hidden'),
      inert: 'inert' in el ? el.inert : undefined
    }

    function applyHeaderVisibility(shouldHide) {
      if (!shouldHide) {
        restoreOriginal(el, original)
        return
      }
      el.style.overflow = 'hidden'
      el.style.height = '0px'
      el.style.opacity = '0'
      el.style.pointerEvents = 'none'
      el.setAttribute('aria-hidden', 'true')
      if ('inert' in el) {
        el.inert = true
      }
    }

    el.style.transition = 'height 0.3s ease, opacity 0.3s ease'

    function handleScroll(payload) {
      const { y, delta, direction } = payload

      if (el[STATE_KEY]?.suspended) {
        applyHeaderVisibility(false)
        return
      }

      if (Math.abs(delta) < 8) {
        return
      }

      const shouldHide = y < 40 ? false : direction === 'down'
      applyHeaderVisibility(shouldHide)
    }

    let lastWidth = window.innerWidth
    function handleResize() {
      if (window.innerWidth === lastWidth) return
      lastWidth = window.innerWidth
      applyHeaderVisibility(false)
    }

    function handleReset() {
      applyHeaderVisibility(false)
    }

    binding.instance.$eventBus.on('scrollOnMobile', handleScroll)
    binding.instance.$eventBus.on('scrollOnMobileReset', handleReset)
    window.addEventListener('resize', handleResize, { passive: true })
    el[STATE_KEY] = { handleScroll, handleResize, handleReset, original, applyHeaderVisibility, suspended: !!binding.value }
  },
  updated(el, binding) {
    const state = el[STATE_KEY]
    if (!state) return
    state.suspended = !!binding.value
    if (state.suspended) {
      state.applyHeaderVisibility(false)
    }
  },
  unmounted(el, binding) {
    const state = el[STATE_KEY]
    if (!state) return
    restoreOriginal(el, state.original)
    el.style.transition = state.original.transition
    binding.instance?.$eventBus?.off('scrollOnMobile', state.handleScroll)
    binding.instance?.$eventBus?.off('scrollOnMobileReset', state.handleReset)
    window.removeEventListener('resize', state.handleResize)
    delete el[STATE_KEY]
  }
}

export default hideHeader