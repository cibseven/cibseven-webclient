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
import { describe, it, expect, vi, afterEach } from 'vitest'
import tabUrlMixin from '@/components/process/mixins/tabUrlMixin.js'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'

describe('tabUrlMixin', () => {
  /** A component instance with the mixin's methods bound to it. */
  function instance({ query = {}, defaultTab = 'variables', name = 'process', params = {} } = {}) {
    const vm = {
      ...tabUrlMixin.data(),
      defaultTab,
      $route: { query, name, params },
      $router: { push: vi.fn() },
      $nextTick: (fn) => (fn ? Promise.resolve().then(fn) : Promise.resolve())
    }
    for (const [key, method] of Object.entries(tabUrlMixin.methods)) {
      vm[key] = method.bind(vm)
    }
    return vm
  }

  it('should start with no active tab', () => {
    expect(tabUrlMixin.data()).toEqual({ activeTab: '' })
  })

  describe('created', () => {
    // A deep link carrying ?tab=... must win, so a shared URL opens on the right tab.
    it('should adopt the tab from the URL', () => {
      const vm = instance({ query: { tab: 'incidents' } })

      tabUrlMixin.created.call(vm)

      expect(vm.activeTab).toBe('incidents')
    })

    it('should fall back to the component default when the URL has no tab', () => {
      const vm = instance({ query: {}, defaultTab: 'variables' })

      tabUrlMixin.created.call(vm)

      expect(vm.activeTab).toBe('variables')
    })
  })

  describe('$route.query.tab watcher', () => {
    const watcher = tabUrlMixin.watch['$route.query.tab'].handler

    it('should not fire immediately on registration', () => {
      expect(tabUrlMixin.watch['$route.query.tab'].immediate).toBe(false)
    })

    // Browser back/forward changes the URL without touching component state, so the tab
    // has to follow the URL.
    it('should follow the URL to the new tab', () => {
      const vm = instance()
      vm.activeTab = 'variables'

      watcher.call(vm, 'incidents')

      expect(vm.activeTab).toBe('incidents')
    })

    it('should fall back to the default tab when the parameter is removed', () => {
      const vm = instance({ defaultTab: 'variables' })
      vm.activeTab = 'incidents'

      watcher.call(vm, undefined)

      expect(vm.activeTab).toBe('variables')
    })

    // Guards against a watcher loop between the URL and the component.
    it('should not reassign when the tab already matches', () => {
      const vm = instance()
      vm.activeTab = 'incidents'

      watcher.call(vm, 'incidents')

      expect(vm.activeTab).toBe('incidents')
    })
  })

  describe('activeTab watcher', () => {
    it('should push the new tab into the URL', () => {
      const vm = instance()
      vm.updateUrlTab = vi.fn()

      tabUrlMixin.watch.activeTab.call(vm, 'incidents')

      expect(vm.updateUrlTab).toHaveBeenCalledWith('incidents')
    })
  })

  describe('updateUrlTab', () => {
    it('should push a route keeping the name, params and other query values', async () => {
      const vm = instance({ query: { foo: 'bar' }, name: 'process', params: { processKey: 'invoice' } })

      vm.updateUrlTab('incidents')
      await Promise.resolve()

      expect(vm.$router.push).toHaveBeenCalledWith({
        name: 'process',
        params: { processKey: 'invoice' },
        query: { foo: 'bar', tab: 'incidents' }
      })
    })

    // The push is deferred and re-checked so that a navigation already carrying the tab
    // does not produce a redundant history entry.
    it('should not push when the URL already carries that tab', async () => {
      const vm = instance({ query: { tab: 'incidents' } })

      vm.updateUrlTab('incidents')
      await Promise.resolve()

      expect(vm.$router.push).not.toHaveBeenCalled()
    })
  })

  describe('changeTab', () => {
    it('should set the active tab from the selected tab object', () => {
      const vm = instance()

      vm.changeTab({ id: 'incidents' })

      expect(vm.activeTab).toBe('incidents')
    })
  })
})

describe('resizerMixin', () => {
  /** A component instance with the mixin's methods bound and a stubbed bottom panel. */
  function instance({ contentHeight = 200 } = {}) {
    const vm = {
      ...resizerMixin.data(),
      $refs: { rContent: { offsetHeight: contentHeight } }
    }
    for (const [key, method] of Object.entries(resizerMixin.methods)) {
      vm[key] = method.bind(vm)
    }
    return vm
  }

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
    document.body.style.userSelect = ''
  })

  describe('initial data', () => {
    it('should expose the fixed chrome heights', () => {
      const data = resizerMixin.data()

      expect(data).toMatchObject({
        topBarHeight: 41,
        dragSelectorHeight: 10,
        filterHeight: 60,
        tabsAreaHeight: 39,
        mousePosition: null,
        toggleIcon: 'mdi-chevron-down',
        toggleTransition: '',
        transitionTime: 0.4
      })
    })

    // The viewer never takes more than half the remaining viewport, capped at 400px.
    it('should cap the viewer height at half the available viewport', () => {
      const data = resizerMixin.data()
      const expected = Math.min(400, Math.floor((window.innerHeight - 120) / 2))

      expect(data.bpmnViewerHeight).toBe(expected)
      expect(data.bpmnViewerOriginalHeight).toBe(expected)
    })
  })

  describe('bottomContentPosition', () => {
    it('should sit directly below the viewer and the top bar', () => {
      const vm = { bpmnViewerHeight: 300, topBarHeight: 41 }

      expect(resizerMixin.computed.bottomContentPosition.call(vm)).toBe(341)
    })
  })

  describe('handleMouseDown', () => {
    // Only a press inside the bottom 10px drag strip starts a resize.
    it('should begin a drag when pressed on the drag handle', () => {
      const addEventListener = vi.spyOn(document, 'addEventListener')
      const vm = instance()
      vm.bpmnViewerHeight = 300

      vm.handleMouseDown({ offsetY: 295, y: 500 })

      expect(vm.mousePosition).toBe(500)
      expect(addEventListener).toHaveBeenCalledWith('mousemove', vm.resize, false)
      expect(addEventListener).toHaveBeenCalledWith('mouseup', vm.handleMouseUp, false)
      expect(document.body.style.userSelect).toBe('none')
    })

    it('should ignore a press above the drag handle', () => {
      const addEventListener = vi.spyOn(document, 'addEventListener')
      const vm = instance()
      vm.bpmnViewerHeight = 300

      vm.handleMouseDown({ offsetY: 100, y: 500 })

      expect(vm.mousePosition).toBeNull()
      expect(addEventListener).not.toHaveBeenCalled()
    })
  })

  describe('resize', () => {
    it('should grow the viewer as the pointer moves down', () => {
      const vm = instance()
      vm.bpmnViewerHeight = 300
      vm.mousePosition = 500

      vm.resize({ y: 540 })

      expect(vm.bpmnViewerHeight).toBe(340)
      expect(vm.mousePosition).toBe(540)
    })

    it('should shrink the viewer as the pointer moves up', () => {
      const vm = instance()
      vm.bpmnViewerHeight = 300
      vm.mousePosition = 500

      vm.resize({ y: 460 })

      expect(vm.bpmnViewerHeight).toBe(260)
    })

    it('should point the toggle down while the bottom panel has height', () => {
      const vm = instance({ contentHeight: 200 })
      vm.mousePosition = 0
      vm.toggleIcon = 'mdi-chevron-up'

      vm.resize({ y: 10 })

      expect(vm.toggleIcon).toBe('mdi-chevron-down')
    })

    // A collapsed bottom panel has no height left to reveal, so the toggle flips to "up".
    it('should point the toggle up when the bottom panel is collapsed', () => {
      const vm = instance({ contentHeight: 0 })
      vm.mousePosition = 0

      vm.resize({ y: 10 })

      expect(vm.toggleIcon).toBe('mdi-chevron-up')
    })
  })

  describe('handleMouseUp', () => {
    it('should detach the drag listeners and restore text selection', () => {
      const removeEventListener = vi.spyOn(document, 'removeEventListener')
      const vm = instance()

      vm.handleMouseUp()

      expect(removeEventListener).toHaveBeenCalledWith('mousemove', vm.resize, false)
      expect(removeEventListener).toHaveBeenCalledWith('mouseup', vm.handleMouseUp, false)
      expect(document.body.style.userSelect).toBe('text')
    })
  })

  describe('toggleContent', () => {
    it('should expand the viewer over the bottom panel', () => {
      vi.useFakeTimers()
      const vm = instance({ contentHeight: 200 })
      vm.bpmnViewerHeight = 300

      vm.toggleContent()

      expect(vm.bpmnViewerHeight).toBe(500)
      expect(vm.toggleIcon).toBe('mdi-chevron-up')
      expect(vm.toggleTransition).toContain('transition: top 0.4s ease')
    })

    it('should collapse back to the original height when the panel is already hidden', () => {
      vi.useFakeTimers()
      const vm = instance({ contentHeight: 0 })
      vm.bpmnViewerOriginalHeight = 400
      vm.bpmnViewerHeight = 600

      vm.toggleContent()

      expect(vm.bpmnViewerHeight).toBe(400)
      expect(vm.toggleIcon).toBe('mdi-chevron-down')
    })

    // The inline transition is removed once it has played, so a later drag is not animated.
    it('should clear the transition once it has run', () => {
      vi.useFakeTimers()
      const vm = instance()

      vm.toggleContent()
      expect(vm.toggleTransition).not.toBe('')

      vi.advanceTimersByTime(400)

      expect(vm.toggleTransition).toBe('')
    })
  })
})
