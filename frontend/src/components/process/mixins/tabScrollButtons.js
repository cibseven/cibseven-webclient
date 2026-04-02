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

export default {
  data() {
    return {
      showLeftButton: false,
      showRightButton: false,
      resizeObserver: null,
      onResize: null
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.checkScrollButtons()
      this.setupEventListeners()
    })
  },
  beforeUnmount() {
    this.cleanupEventListeners()
  },
  methods: {
    scrollLeft() {
      const el = this.$refs.tabsContainer
      if (!el) return

      const buttonWidth = 40 // Approximate width of scroll buttons
      const visibleWidth = el.clientWidth - buttonWidth * 2 // Account for both buttons
      const scrollAmount = Math.max(visibleWidth * 0.8, 150) // Scroll 80% of visible width or minimum 150px
      const newScrollLeft = Math.max(0, el.scrollLeft - scrollAmount)

      el.scrollTo({ left: newScrollLeft, behavior: 'smooth' })
    },
    scrollRight() {
      const el = this.$refs.tabsContainer
      if (!el) return

      const buttonWidth = 40 // Approximate width of scroll buttons
      const visibleWidth = el.clientWidth - buttonWidth * 2 // Account for both buttons
      const scrollAmount = Math.max(visibleWidth * 0.8, 150) // Scroll 80% of visible width or minimum 150px
      const maxScrollLeft = el.scrollWidth - el.clientWidth
      const newScrollLeft = Math.min(maxScrollLeft, el.scrollLeft + scrollAmount)

      el.scrollTo({ left: newScrollLeft, behavior: 'smooth' })
    },
    scrollToTab(tabElement) {
      const el = this.$refs.tabsContainer
      if (!el || !tabElement) return

      const buttonWidth = 40 // Width of scroll buttons
      const containerRect = el.getBoundingClientRect()
      const tabRect = tabElement.getBoundingClientRect()
      
      // Calculate relative position of tab within the container
      const tabLeft = tabRect.left - containerRect.left + el.scrollLeft
      const tabRight = tabLeft + tabRect.width
      
      // Calculate visible area (excluding button areas)
      const visibleLeft = el.scrollLeft + (this.showLeftButton ? buttonWidth : 0)
      const visibleRight = el.scrollLeft + el.clientWidth - (this.showRightButton ? buttonWidth : 0)
      
      let newScrollLeft = el.scrollLeft
      
      // If tab is cut off on the left
      if (tabLeft < visibleLeft) {
        newScrollLeft = tabLeft - buttonWidth // Add some padding
      }
      // If tab is cut off on the right
      else if (tabRight > visibleRight) {
        newScrollLeft = tabRight - el.clientWidth + buttonWidth // Add some padding
      }
      
      // Ensure we don't scroll beyond bounds
      newScrollLeft = Math.max(0, Math.min(newScrollLeft, el.scrollWidth - el.clientWidth))
      
      if (newScrollLeft !== el.scrollLeft) {
        el.scrollTo({ left: newScrollLeft, behavior: 'smooth' })
      }
    },
    checkScrollButtons() {
      const el = this.$refs.tabsContainer
      if (!el) return

      const tolerance = 2 // Small tolerance to avoid flickering due to rounding

      // Show left button if we're scrolled past the left edge
      this.showLeftButton = el.scrollLeft > tolerance

      // Show right button if there's more content to scroll to the right
      this.showRightButton = el.scrollLeft + el.clientWidth < el.scrollWidth - tolerance
    },
    scrollToActiveTab() {
      const el = this.$refs.tabsContainer
      if (!el) return

      // Find the active tab element
      const activeTab =
        el.querySelector('.nav-link.active') ||
        el.querySelector('[role="tab"][aria-selected="true"]') ||
        el.querySelector('.active')
      if (!activeTab) return

      // Scroll to the active tab
      this.scrollToTab(activeTab)
    },
    setupEventListeners() {
      this.onResize = () => {
        this.checkScrollButtons()
        this.$nextTick(() => this.scrollToActiveTab())
      }
      window.addEventListener('resize', this.onResize)
      const el = this.$refs.tabsContainer
      if (el && window.ResizeObserver) {
        this.resizeObserver = new ResizeObserver(() => {
          this.$nextTick(() => {
            this.checkScrollButtons()
            this.scrollToActiveTab()
          })
        })
        this.resizeObserver.observe(el)
      }
    },
    cleanupEventListeners() {
      if (this.onResize) {
        window.removeEventListener('resize', this.onResize)
        this.onResize = null
      }
      if (this.resizeObserver) {
        this.resizeObserver.disconnect()
        this.resizeObserver = null
      }
    }
  }
}