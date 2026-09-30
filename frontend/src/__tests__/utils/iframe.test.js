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
import { findAndScroll, getIframeContext } from '@/utils/iframe.js'

/**
 * Plain-object stand-ins for DOM nodes.
 *
 * jsdom does no layout, so `scrollHeight`/`clientHeight` are always 0 on real elements and
 * none of the scrollability logic would run. Hand-built nodes let each case state exactly
 * which ancestor is scrollable.
 */
function element({ tagName = 'DIV', overflowY = 'visible', scrollHeight = 0, clientHeight = 0, scrollTop = 0, parentElement = null, ...rest } = {}) {
  return { tagName, overflowY, scrollHeight, clientHeight, scrollTop, parentElement, ...rest }
}

/** A document stub whose body may or may not be scrollable. */
function documentStub({ scrollHeight = 0, clientHeight = 0, scrollTop = 0, elementFromPoint = () => null } = {}) {
  const doc = {
    body: { scrollHeight, clientHeight, scrollTop },
    elementFromPoint: vi.fn(elementFromPoint)
  }
  doc.defaultView = { getComputedStyle: (el) => ({ overflowY: el.overflowY ?? 'visible' }) }
  return doc
}

function iframe({ contentDocument = null, contentWindow = {}, rect = { left: 0, top: 0 }, ...rest } = {}) {
  return element({
    tagName: 'IFRAME',
    contentDocument,
    contentWindow,
    getBoundingClientRect: () => rect,
    ...rest
  })
}

beforeEach(() => {
  // findAndScroll consults the *outer* window for non-iframe elements.
  vi.spyOn(window, 'getComputedStyle').mockImplementation((el) => ({ overflowY: el.overflowY ?? 'visible' }))
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('getIframeContext', () => {
  it('should translate the pointer position into iframe-local coordinates', () => {
    const doc = documentStub()
    const el = iframe({ contentDocument: doc, rect: { left: 30, top: 20 } })

    expect(getIframeContext(el, 100, 60)).toEqual({ iframe: el, doc, x: 70, y: 40 })
  })

  it('should fall back to contentWindow.document when contentDocument is absent', () => {
    const doc = documentStub()
    const el = iframe({ contentDocument: null, contentWindow: { document: doc } })

    expect(getIframeContext(el, 0, 0).doc).toBe(doc)
  })

  it.each([[null], [undefined]])('should return null for iframe %j', (el) => {
    expect(getIframeContext(el, 0, 0)).toBeNull()
  })

  // A cross-origin iframe exposes no contentWindow, so there is nothing to scroll.
  it('should return null when the iframe has no contentWindow', () => {
    expect(getIframeContext(iframe({ contentWindow: null }), 0, 0)).toBeNull()
  })

  it('should return null when neither document is reachable', () => {
    expect(getIframeContext(iframe({ contentDocument: null, contentWindow: {} }), 0, 0)).toBeNull()
  })
})

describe('findAndScroll', () => {
  const rootDoc = (overrides) => documentStub(overrides)

  it('should do nothing without a starting element', () => {
    const root = rootDoc({ scrollHeight: 100, clientHeight: 50 })

    findAndScroll(null, 10, 0, 0, root)

    expect(root.body.scrollTop).toBe(0)
  })

  it('should scroll the first scrollable element it finds', () => {
    const el = element({ overflowY: 'auto', scrollHeight: 200, clientHeight: 100 })

    findAndScroll(el, 25, 0, 0, rootDoc())

    expect(el.scrollTop).toBe(25)
  })

  it('should walk up to a scrollable ancestor', () => {
    const parent = element({ overflowY: 'scroll', scrollHeight: 200, clientHeight: 100 })
    const child = element({ parentElement: parent })

    findAndScroll(child, 10, 0, 0, rootDoc())

    expect(parent.scrollTop).toBe(10)
    expect(child.scrollTop).toBe(0)
  })

  it.each([['visible'], ['hidden']])('should skip an element whose overflowY is %s', (overflowY) => {
    const el = element({ overflowY, scrollHeight: 200, clientHeight: 100 })
    const root = rootDoc({ scrollHeight: 300, clientHeight: 100 })

    findAndScroll(el, 10, 0, 0, root)

    expect(el.scrollTop).toBe(0)
    expect(root.body.scrollTop).toBe(10)
  })

  // Scrollable styling alone is not enough — there has to be overflowing content.
  it('should skip a scrollable element with no overflowing content', () => {
    const el = element({ overflowY: 'auto', scrollHeight: 100, clientHeight: 100 })
    const root = rootDoc({ scrollHeight: 300, clientHeight: 100 })

    findAndScroll(el, 10, 0, 0, root)

    expect(el.scrollTop).toBe(0)
    expect(root.body.scrollTop).toBe(10)
  })

  it('should fall back to the root document body', () => {
    const root = rootDoc({ scrollHeight: 300, clientHeight: 100 })

    findAndScroll(element(), 15, 0, 0, root)

    expect(root.body.scrollTop).toBe(15)
  })

  it('should do nothing when nothing at all is scrollable', () => {
    const root = rootDoc({ scrollHeight: 100, clientHeight: 100 })

    expect(() => findAndScroll(element(), 15, 0, 0, root)).not.toThrow()
    expect(root.body.scrollTop).toBe(0)
  })

  it('should tolerate a root document without a body', () => {
    const root = documentStub()
    root.body = null

    expect(() => findAndScroll(element(), 15, 0, 0, root)).not.toThrow()
  })

  describe('crossing into an iframe', () => {
    it('should scroll a scrollable element under the pointer inside the iframe', () => {
      const inner = element({ overflowY: 'auto', scrollHeight: 200, clientHeight: 100 })
      const doc = documentStub({ elementFromPoint: () => inner })
      const el = iframe({ contentDocument: doc, rect: { left: 10, top: 20 } })

      findAndScroll(el, 30, 110, 70, doc)

      expect(doc.elementFromPoint).toHaveBeenCalledWith(100, 50)
      expect(inner.scrollTop).toBe(30)
    })

    it('should walk up the iframe DOM to a scrollable ancestor', () => {
      const innerParent = element({ overflowY: 'auto', scrollHeight: 200, clientHeight: 100 })
      const inner = element({ parentElement: innerParent })
      const doc = documentStub({ elementFromPoint: () => inner })

      findAndScroll(iframe({ contentDocument: doc }), 10, 0, 0, doc)

      expect(innerParent.scrollTop).toBe(10)
    })

    it('should fall back to the iframe body when no inner element is scrollable', () => {
      const doc = documentStub({ scrollHeight: 300, clientHeight: 100, elementFromPoint: () => element() })

      findAndScroll(iframe({ contentDocument: doc }), 12, 0, 0, doc)

      expect(doc.body.scrollTop).toBe(12)
    })

    it('should fall back to the iframe body when the pointer hits nothing', () => {
      const doc = documentStub({ scrollHeight: 300, clientHeight: 100, elementFromPoint: () => null })

      findAndScroll(iframe({ contentDocument: doc }), 12, 0, 0, doc)

      expect(doc.body.scrollTop).toBe(12)
    })

    // Having entered the iframe the walk stops there, even if nothing inside can scroll —
    // otherwise the outer page would jump while the pointer is over the iframe.
    it('should stop at the iframe rather than scrolling the outer document', () => {
      const doc = documentStub({ elementFromPoint: () => null })
      const root = rootDoc({ scrollHeight: 300, clientHeight: 100 })

      findAndScroll(iframe({ contentDocument: doc }), 12, 0, 0, root)

      expect(root.body.scrollTop).toBe(0)
    })

    it('should tolerate an iframe body that is missing', () => {
      const doc = documentStub({ elementFromPoint: () => null })
      doc.body = null

      expect(() => findAndScroll(iframe({ contentDocument: doc }), 12, 0, 0, rootDoc())).not.toThrow()
    })

    // A cross-origin iframe yields no context, so the walk continues outward as usual.
    it('should keep walking outward when the iframe is not accessible', () => {
      const parent = element({ overflowY: 'auto', scrollHeight: 200, clientHeight: 100 })
      const el = iframe({ contentWindow: null, parentElement: parent })

      findAndScroll(el, 10, 0, 0, rootDoc())

      expect(parent.scrollTop).toBe(10)
    })
  })
})
