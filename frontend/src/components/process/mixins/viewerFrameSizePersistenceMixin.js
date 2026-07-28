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

// Persists the ViewerFrame height/collapsed state (driven by resizerMixin's
// bpmnViewerHeight/toggleIcon) across page reloads, in localStorage. Shared
// under a single key across all host components that opt in (process
// definition and instance views), so resizing/collapsing in one is reflected
// in the other - resizerMixin itself (shared with the decision views) stays
// untouched since only components including this mixin persist anything.
const DEBOUNCE_MS = 300
const MIN_VIEWER_HEIGHT = 100
// Space to always keep visible below the viewer (tabs/table area), so a height
// saved on a taller window never pushes content off-screen on a shorter one.
const MIN_BOTTOM_VISIBLE = 120
const STORAGE_KEY = 'viewer-frame-size:process'

export default {
  data: function() {
    return {
      viewerFrameSaveTimer: null
    }
  },
  mounted: function() {
    this.restoreViewerFrameSize()
    document.addEventListener('mouseup', this.flushViewerFrameSave)
    window.addEventListener('resize', this.clampViewerFrameHeight)
  },
  watch: {
    bpmnViewerHeight: function() {
      this.scheduleViewerFrameSave()
    },
    toggleIcon: function() {
      this.scheduleViewerFrameSave()
    }
  },
  beforeUnmount: function() {
    clearTimeout(this.viewerFrameSaveTimer)
    document.removeEventListener('mouseup', this.flushViewerFrameSave)
    window.removeEventListener('resize', this.clampViewerFrameHeight)
  },
  methods: {
    maxViewerFrameHeight: function() {
      return window.innerHeight - (this.topBarHeight || 0) - MIN_BOTTOM_VISIBLE
    },
    clampViewerFrameHeight: function() {
      const max = this.maxViewerFrameHeight()
      if (this.bpmnViewerHeight > max) {
        this.bpmnViewerHeight = Math.max(MIN_VIEWER_HEIGHT, max)
      }
    },
    restoreViewerFrameSize: function() {
      try {
        const raw = localStorage.getItem(STORAGE_KEY)
        if (!raw) return
        const saved = JSON.parse(raw)
        if (typeof saved.height === 'number') {
          this.bpmnViewerHeight = Math.min(saved.height, Math.max(MIN_VIEWER_HEIGHT, this.maxViewerFrameHeight()))
        }
        if (saved.icon) this.toggleIcon = saved.icon
      } catch {
        // ignore malformed/unavailable storage
      }
    },
    saveViewerFrameSize: function() {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({
          height: this.bpmnViewerHeight,
          icon: this.toggleIcon
        }))
      } catch {
        // localStorage unavailable or quota exceeded - ignore
      }
    },
    scheduleViewerFrameSave: function() {
      clearTimeout(this.viewerFrameSaveTimer)
      this.viewerFrameSaveTimer = setTimeout(() => this.saveViewerFrameSize(), DEBOUNCE_MS)
    },
    flushViewerFrameSave: function() {
      clearTimeout(this.viewerFrameSaveTimer)
      this.saveViewerFrameSize()
    }
  }
}
