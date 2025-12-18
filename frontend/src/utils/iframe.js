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
export function findAndScroll(el, deltaY, iframeX, iframeY, rootDoc) {
  if (!el) return
  do {
    if (el.tagName === 'IFRAME') {
      const ctx = getIframeContext(el, iframeX, iframeY)
      if (ctx) {
        let nestedEl = ctx.doc.elementFromPoint(ctx.x, ctx.y)
        while (nestedEl) {
          const overflowY = ctx.doc.defaultView.getComputedStyle(nestedEl).overflowY
          const isScrollable = overflowY !== 'visible' && overflowY !== 'hidden'
          if ( isScrollable && nestedEl.scrollHeight > nestedEl.clientHeight) {
            nestedEl.scrollTop += deltaY
            return
          }
          nestedEl = nestedEl.parentElement
        } 
        if (ctx.doc.body && ctx.doc.body.scrollHeight > ctx.doc.body.clientHeight) {
          ctx.doc.body.scrollTop += deltaY
        }
        return
      }
    }
    const overflowY = window.getComputedStyle(el).overflowY
    const isScrollable = overflowY !== 'visible' && overflowY !== 'hidden'
    if (isScrollable && el.scrollHeight > el.clientHeight) {
      el.scrollTop += deltaY
      return
    }
    el = el.parentElement
  }  while (el)
  if (rootDoc.body && rootDoc.body.scrollHeight > rootDoc.body.clientHeight) {
    rootDoc.body.scrollTop += deltaY
  }
}
export function getIframeContext(iframeEl, x, y) {
  if (!iframeEl || !iframeEl.contentWindow) return null
  const doc = iframeEl.contentDocument || iframeEl.contentWindow.document
  if (!doc) return null
  const rect = iframeEl.getBoundingClientRect()
  return {
    iframe: iframeEl,
    doc,
    x: x - rect.left,
    y: y - rect.top
  }
}