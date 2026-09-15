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
        getDefinitions: vi.fn(() => ({ drgElement: [] })),
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
