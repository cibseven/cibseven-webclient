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
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import tabScrollButtons from '@/components/process/mixins/tabScrollButtons.js'

/**
 * A stand-in for the tabs container element.
 *
 * jsdom performs no layout, so `clientWidth`/`scrollWidth`/`getBoundingClientRect` would
 * all be zero on a real element and none of the scroll arithmetic would be exercised. A
 * plain object with the handful of properties the mixin reads lets each geometry case be
 * set up exactly.
 */
function container(overrides = {}) {
  return {
    scrollLeft: 0,
    clientWidth: 500,
    scrollWidth: 1000,
    scrollTo: vi.fn(),
    querySelector: vi.fn(() => null),
    getBoundingClientRect: () => ({ left: 0, width: 500 }),
    ...overrides
  }
}

/** A component instance exposing the mixin's data, methods and a stubbed `$refs`. */
function instance(el, overrides = {}) {
  const vm = {
    ...tabScrollButtons.data(),
    $refs: el ? { tabsContainer: el } : {},
    $nextTick: (fn) => (fn ? Promise.resolve().then(fn) : Promise.resolve()),
    ...overrides
  }
  for (const [name, method] of Object.entries(tabScrollButtons.methods)) {
    vm[name] = method.bind(vm)
  }
  return vm
}

describe('tabScrollButtons mixin', () => {
  it('should start with both arrows hidden and no observers', () => {
    expect(tabScrollButtons.data()).toEqual({
      showLeftButton: false,
      showRightButton: false,
      resizeObserver: null,
      onResize: null
    })
  })

  describe('checkScrollButtons', () => {
    it('should hide the left arrow at the start of the strip', () => {
      const vm = instance(container({ scrollLeft: 0 }))
      vm.checkScrollButtons()
      expect(vm.showLeftButton).toBe(false)
    })

    it('should show the left arrow once scrolled away from the start', () => {
      const vm = instance(container({ scrollLeft: 100 }))
      vm.checkScrollButtons()
      expect(vm.showLeftButton).toBe(true)
    })

    // A 2px tolerance stops the arrows flickering on sub-pixel scroll positions.
    it('should keep the left arrow hidden within the rounding tolerance', () => {
      const vm = instance(container({ scrollLeft: 2 }))
      vm.checkScrollButtons()
      expect(vm.showLeftButton).toBe(false)
    })

    it('should show the right arrow while content remains to the right', () => {
      const vm = instance(container({ scrollLeft: 0, clientWidth: 500, scrollWidth: 1000 }))
      vm.checkScrollButtons()
      expect(vm.showRightButton).toBe(true)
    })

    it('should hide the right arrow at the end of the strip', () => {
      const vm = instance(container({ scrollLeft: 500, clientWidth: 500, scrollWidth: 1000 }))
      vm.checkScrollButtons()
      expect(vm.showRightButton).toBe(false)
    })

    it('should hide both arrows when everything fits', () => {
      const vm = instance(container({ scrollLeft: 0, clientWidth: 1000, scrollWidth: 1000 }))
      vm.checkScrollButtons()
      expect(vm.showLeftButton).toBe(false)
      expect(vm.showRightButton).toBe(false)
    })

    it('should do nothing when the container is not mounted', () => {
      const vm = instance(null)
      expect(() => vm.checkScrollButtons()).not.toThrow()
      expect(vm.showLeftButton).toBe(false)
    })
  })

  describe('scrollLeft', () => {
    // Scrolls 80% of the width left visible between the two 40px arrows.
    it('should scroll back by 80% of the visible width', () => {
      const el = container({ scrollLeft: 600, clientWidth: 500 })
      instance(el).scrollLeft()

      expect(el.scrollTo).toHaveBeenCalledWith({ left: 600 - 336, behavior: 'smooth' })
    })

    it('should never scroll past the start', () => {
      const el = container({ scrollLeft: 50, clientWidth: 500 })
      instance(el).scrollLeft()

      expect(el.scrollTo).toHaveBeenCalledWith({ left: 0, behavior: 'smooth' })
    })

    // On a narrow strip 80% of the visible width would be a uselessly small step, so a
    // 150px floor applies.
    it('should fall back to a 150px step on a narrow container', () => {
      const el = container({ scrollLeft: 400, clientWidth: 100 })
      instance(el).scrollLeft()

      expect(el.scrollTo).toHaveBeenCalledWith({ left: 250, behavior: 'smooth' })
    })

    it('should do nothing when the container is not mounted', () => {
      expect(() => instance(null).scrollLeft()).not.toThrow()
    })
  })

  describe('scrollRight', () => {
    it('should scroll forward by 80% of the visible width', () => {
      const el = container({ scrollLeft: 0, clientWidth: 500, scrollWidth: 2000 })
      instance(el).scrollRight()

      expect(el.scrollTo).toHaveBeenCalledWith({ left: 336, behavior: 'smooth' })
    })

    it('should never scroll past the end', () => {
      const el = container({ scrollLeft: 400, clientWidth: 500, scrollWidth: 1000 })
      instance(el).scrollRight()

      expect(el.scrollTo).toHaveBeenCalledWith({ left: 500, behavior: 'smooth' })
    })

    it('should fall back to a 150px step on a narrow container', () => {
      const el = container({ scrollLeft: 0, clientWidth: 100, scrollWidth: 2000 })
      instance(el).scrollRight()

      expect(el.scrollTo).toHaveBeenCalledWith({ left: 150, behavior: 'smooth' })
    })

    it('should do nothing when the container is not mounted', () => {
      expect(() => instance(null).scrollRight()).not.toThrow()
    })
  })

  describe('scrollToTab', () => {
    const tab = (left, width) => ({ getBoundingClientRect: () => ({ left, width }) })

    it('should scroll back when the tab is cut off on the left', () => {
      const el = container({ scrollLeft: 300, clientWidth: 500, scrollWidth: 2000 })
      const vm = instance(el, { showLeftButton: true })

      // Tab sits at 100px within the scrolled content, behind the left arrow.
      vm.scrollToTab(tab(-200, 80))

      expect(el.scrollTo).toHaveBeenCalledWith({ left: 60, behavior: 'smooth' })
    })

    it('should scroll forward when the tab is cut off on the right', () => {
      const el = container({ scrollLeft: 0, clientWidth: 500, scrollWidth: 2000 })
      const vm = instance(el, { showRightButton: true })

      vm.scrollToTab(tab(480, 100))

      expect(el.scrollTo).toHaveBeenCalledWith({ left: 120, behavior: 'smooth' })
    })

    it('should not scroll when the tab is already fully visible', () => {
      const el = container({ scrollLeft: 0, clientWidth: 500, scrollWidth: 2000 })

      instance(el).scrollToTab(tab(100, 80))

      expect(el.scrollTo).not.toHaveBeenCalled()
    })

    it('should clamp the target within the scrollable bounds', () => {
      const el = container({ scrollLeft: 0, clientWidth: 500, scrollWidth: 600 })
      const vm = instance(el)

      vm.scrollToTab(tab(2000, 100))

      expect(el.scrollTo).toHaveBeenCalledWith({ left: 100, behavior: 'smooth' })
    })

    it('should do nothing without a container', () => {
      expect(() => instance(null).scrollToTab(tab(0, 10))).not.toThrow()
    })

    it('should do nothing without a tab element', () => {
      const el = container()
      instance(el).scrollToTab(null)
      expect(el.scrollTo).not.toHaveBeenCalled()
    })
  })

  describe('scrollToActiveTab', () => {
    // Three selectors are tried in turn, because the active tab is marked differently by
    // Bootstrap navs and by ARIA tablists.
    it.each([
      ['.nav-link.active', 0],
      ['[role="tab"][aria-selected="true"]', 1],
      ['.active', 2]
    ])('should find the active tab via %s', (selector, missesBefore) => {
      const activeTab = { getBoundingClientRect: () => ({ left: 600, width: 100 }) }
      let call = 0
      const el = container({
        querySelector: vi.fn(() => (call++ < missesBefore ? null : activeTab)),
        scrollWidth: 2000
      })
      const vm = instance(el)

      vm.scrollToActiveTab()

      expect(el.querySelector).toHaveBeenNthCalledWith(missesBefore + 1, selector)
      expect(el.scrollTo).toHaveBeenCalled()
    })

    it('should do nothing when no tab is active', () => {
      const el = container({ querySelector: vi.fn(() => null) })

      instance(el).scrollToActiveTab()

      expect(el.scrollTo).not.toHaveBeenCalled()
    })

    it('should do nothing when the container is not mounted', () => {
      expect(() => instance(null).scrollToActiveTab()).not.toThrow()
    })
  })

  describe('event listeners', () => {
    let addSpy
    let removeSpy

    beforeEach(() => {
      addSpy = vi.spyOn(window, 'addEventListener')
      removeSpy = vi.spyOn(window, 'removeEventListener')
    })

    afterEach(() => {
      vi.restoreAllMocks()
    })

    it('should observe window resize and the container itself', () => {
      const el = container()
      const observe = vi.fn()
      const disconnect = vi.fn()
      vi.stubGlobal('ResizeObserver', class {
        constructor(cb) { this.cb = cb }
        observe = observe
        disconnect = disconnect
      })
      const vm = instance(el)

      vm.setupEventListeners()

      expect(addSpy).toHaveBeenCalledWith('resize', vm.onResize)
      expect(observe).toHaveBeenCalledWith(el)
      vi.unstubAllGlobals()
    })

    it('should re-check the arrows and re-centre the active tab on resize', async () => {
      const el = container({ scrollLeft: 100 })
      const vm = instance(el)

      vm.setupEventListeners()
      vm.onResize()
      await Promise.resolve()

      expect(vm.showLeftButton).toBe(true)
      expect(el.querySelector).toHaveBeenCalled()
    })

    // The observer callback runs on the next tick so that the DOM has settled first.
    it('should re-check the arrows when the container is resized', async () => {
      const el = container({ scrollLeft: 100 })
      let observerCallback
      vi.stubGlobal('ResizeObserver', class {
        constructor(cb) { observerCallback = cb }
        observe() {}
        disconnect() {}
      })
      const vm = instance(el)

      vm.setupEventListeners()
      observerCallback()
      await Promise.resolve()

      expect(vm.showLeftButton).toBe(true)
      vi.unstubAllGlobals()
    })

    it('should skip the resize observer when the container is not mounted', () => {
      const vm = instance(null)

      vm.setupEventListeners()

      expect(addSpy).toHaveBeenCalledWith('resize', vm.onResize)
      expect(vm.resizeObserver).toBeNull()
    })

    // Some environments have no ResizeObserver at all; the window listener still applies.
    it('should skip the resize observer when the browser has none', () => {
      const original = window.ResizeObserver
      delete window.ResizeObserver
      const vm = instance(container())

      vm.setupEventListeners()

      expect(vm.resizeObserver).toBeNull()
      window.ResizeObserver = original
    })

    it('should remove both the listener and the observer on cleanup', () => {
      const disconnect = vi.fn()
      vi.stubGlobal('ResizeObserver', class {
        observe() {}
        disconnect = disconnect
      })
      const vm = instance(container())
      vm.setupEventListeners()
      const handler = vm.onResize

      vm.cleanupEventListeners()

      expect(removeSpy).toHaveBeenCalledWith('resize', handler)
      expect(vm.onResize).toBeNull()
      expect(disconnect).toHaveBeenCalled()
      expect(vm.resizeObserver).toBeNull()
      vi.unstubAllGlobals()
    })

    it('should be safe to clean up when nothing was ever set up', () => {
      const vm = instance(container())

      expect(() => vm.cleanupEventListeners()).not.toThrow()
      expect(removeSpy).not.toHaveBeenCalled()
    })
  })

  describe('lifecycle hooks', () => {
    it('should check the arrows and attach listeners on mount', async () => {
      const el = container({ scrollLeft: 100 })
      const vm = instance(el)
      vm.setupEventListeners = vi.fn()

      await tabScrollButtons.mounted.call(vm)
      await Promise.resolve()

      expect(vm.showLeftButton).toBe(true)
      expect(vm.setupEventListeners).toHaveBeenCalled()
    })

    it('should clean up before unmount', () => {
      const vm = instance(container())
      vm.cleanupEventListeners = vi.fn()

      tabScrollButtons.beforeUnmount.call(vm)

      expect(vm.cleanupEventListeners).toHaveBeenCalled()
    })
  })
})
