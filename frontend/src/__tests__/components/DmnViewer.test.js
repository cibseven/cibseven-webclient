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
import { describe, it, expect, vi } from 'vitest'
import DmnViewer from '@/components/decision/DmnViewer.vue'

describe('DmnViewer - collapsedSidebarGutterStyle', () => {
  const { collapsedSidebarGutterStyle } = DmnViewer.computed

  it('adds a left padding gutter when the sidebar is collapsed', () => {
    expect(collapsedSidebarGutterStyle.call({ sidebarLeftOpen: false })).toEqual({ paddingLeft: '40px' })
  })

  it('adds no padding when the sidebar is open', () => {
    expect(collapsedSidebarGutterStyle.call({ sidebarLeftOpen: true })).toEqual({})
  })

  it('defaults to no padding (sidebar open) when the prop is not provided', () => {
    expect(DmnViewer.props.sidebarLeftOpen.default).toBe(true)
  })
})

describe('DmnViewer - setViewbox', () => {
  it('delegates to the active viewer canvas', () => {
    const viewbox = vi.fn()
    const ctx = {
      viewer: { getActiveViewer: () => ({ get: (service) => (service === 'canvas' ? { viewbox } : null) }) }
    }
    const box = { x: 1, y: 2, width: 10, height: 10 }
    DmnViewer.methods.setViewbox.call(ctx, box)
    expect(viewbox).toHaveBeenCalledWith(box)
  })

  it('is a no-op when there is no active viewer', () => {
    expect(() => DmnViewer.methods.setViewbox.call({ viewer: { getActiveViewer: () => null } }, {})).not.toThrow()
  })

  it('is a no-op when the viewer has not been created yet', () => {
    expect(() => DmnViewer.methods.setViewbox.call({ viewer: null }, {})).not.toThrow()
  })
})

describe('DmnViewer - showDiagram', () => {
  it('returns a promise that resolves once the diagram is marked ready', async () => {
    vi.useFakeTimers()
    const ctx = {
      setDiagramReady: vi.fn(),
      viewer: {
        importXML: vi.fn(() => Promise.resolve()),
        getViews: vi.fn(() => []),
        getActiveViewer: vi.fn(() => null)
      }
    }
    const promise = DmnViewer.methods.showDiagram.call(ctx, '<xml/>')
    expect(promise).toBeInstanceOf(Promise)
    await vi.runAllTimersAsync()
    await promise
    expect(ctx.setDiagramReady).toHaveBeenCalledWith(true)
    vi.useRealTimers()
  })

  // The views dmn-js offers after importing a DRD with diagram information; it opens the first one
  describe('which view of a DRD it opens', () => {
    const drdView = { type: 'drd', element: { id: 'definitions' } }
    const dishView = { type: 'decisionTable', element: { id: 'dish' } }
    const beveragesView = { type: 'decisionTable', element: { id: 'beverages' } }
    const canvas = { zoom: vi.fn() }
    const drd = () => {
      const viewer = {
        activeView: drdView,
        importXML: vi.fn(() => Promise.resolve()),
        getViews: () => [drdView, dishView, beveragesView],
        getActiveView: () => viewer.activeView,
        open: vi.fn(view => { viewer.activeView = view; return Promise.resolve() }),
        // didi: get(name, false) answers null for a service the viewer does not have
        getActiveViewer: () => ({ get: (name, strict) => viewer.activeView.type === 'drd' ? canvas : (strict === false ? null : (() => { throw new Error(`No provider for "${name}"!`) })()) })
      }
      return { setDiagramReady: vi.fn(), viewer }
    }
    const show = async (ctx, ...args) => {
      vi.useFakeTimers()
      const shown = DmnViewer.methods.showDiagram.call(ctx, '<xml/>', ...args)
      await vi.runAllTimersAsync()
      await shown
      vi.useRealTimers()
      return ctx
    }

    it('opens the decision it is asked for, and the decision table has no canvas to fit', async () => {
      const ctx = await show(drd(), 'beverages')

      expect(ctx.viewer.open).toHaveBeenCalledWith(beveragesView)
      expect(ctx.setDiagramReady).toHaveBeenLastCalledWith(true)
    })

    it.each([[undefined], ['not-in-this-file']])('keeps the view dmn-js opened when asked for %s', async (decisionId) => {
      canvas.zoom.mockClear()
      const ctx = await show(drd(), decisionId)

      expect(ctx.viewer.open).not.toHaveBeenCalled()
      expect(canvas.zoom).toHaveBeenCalledWith('fit-viewport')
      expect(ctx.setDiagramReady).toHaveBeenLastCalledWith(true)
    })

    it('does not open the decision again when dmn-js already shows it (no diagram information)', async () => {
      const ctx = drd()
      ctx.viewer.activeView = beveragesView

      await show(ctx, 'beverages')

      expect(ctx.viewer.open).not.toHaveBeenCalled()
      expect(ctx.setDiagramReady).toHaveBeenLastCalledWith(true)
    })
  })

  it('marks the diagram not-ready and does not throw when import fails', async () => {
    const ctx = {
      setDiagramReady: vi.fn(),
      viewer: { importXML: vi.fn(() => Promise.reject(new Error('bad xml'))) }
    }
    await DmnViewer.methods.showDiagram.call(ctx, '<xml/>')
    expect(ctx.setDiagramReady).toHaveBeenLastCalledWith(false)
    expect(ctx.loader).toBe(false)
  })
})

describe('DmnViewer - viewbox-changed emission', () => {
  it('registers the eventBus listener once per active viewer and debounces the emit', () => {
    vi.useFakeTimers()
    let viewboxHandler
    const canvasViewbox = vi.fn(() => ({ x: 5 }))
    const activeViewer = {
      get: (service) => {
        if (service === 'eventBus') return { on: (evt, handler) => { if (evt === 'canvas.viewbox.changed') viewboxHandler = handler } }
        if (service === 'canvas') return { viewbox: canvasViewbox }
        return null
      }
    }
    const emit = vi.fn()
    const ctx = { viewer: { getActiveViewer: () => activeViewer }, viewboxListenerViewer: null, $emit: emit }

    DmnViewer.methods.attachViewboxListener.call(ctx)
    DmnViewer.methods.attachViewboxListener.call(ctx) // switching back to the same active viewer must not double-register
    expect(ctx.viewboxListenerViewer).toBe(activeViewer)

    viewboxHandler()
    viewboxHandler() // rapid pan/zoom events collapse into a single debounced emit
    vi.advanceTimersByTime(300)

    expect(emit).toHaveBeenCalledTimes(1)
    expect(emit).toHaveBeenCalledWith('viewbox-changed', { x: 5 })
    vi.useRealTimers()
  })

  it('does nothing when there is no active viewer yet', () => {
    expect(() => DmnViewer.methods.attachViewboxListener.call({ viewer: { getActiveViewer: () => null } })).not.toThrow()
  })
})

/**
 * What the dmn-viewer slot is handed. A contribution works on the table dmn-js rendered, and
 * dmn-js rebuilds it whenever the view changes, so the active view has to reach the slot.
 */
describe('DmnViewer - the view handed to plugins', () => {
  const context = () => ({
    activeView: null,
    isDrdView: true,
    attachViewboxListener: vi.fn(),
    $emit: vi.fn()
  })

  it('hands over the decision that was opened', () => {
    const ctx = context()
    const activeView = { type: 'decisionTable', element: { id: 'decision-1' } }

    DmnViewer.methods.onViewsChanged.call(ctx, { activeView })

    expect(ctx.activeView).toBe(activeView)
    expect(ctx.isDrdView).toBe(false)
  })

  it('hands over the drd view as well, and attaches the viewbox listener there', () => {
    const ctx = context()

    DmnViewer.methods.onViewsChanged.call(ctx, { activeView: { type: 'drd' } })

    expect(ctx.activeView).toEqual({ type: 'drd' })
    expect(ctx.isDrdView).toBe(true)
    expect(ctx.attachViewboxListener).toHaveBeenCalled()
  })

  it('clears the view when dmn-js reports none', () => {
    const ctx = context()
    ctx.activeView = { type: 'decisionTable' }

    DmnViewer.methods.onViewsChanged.call(ctx, {})

    expect(ctx.activeView).toBeNull()
  })

  it('still emits the view-changed event the views rely on', () => {
    const ctx = context()

    DmnViewer.methods.onViewsChanged.call(ctx, { activeView: { type: 'decisionTable' } })

    expect(ctx.$emit).toHaveBeenCalledWith('view-changed',
      { activeView: { type: 'decisionTable' }, isDrdView: false })
  })
})
