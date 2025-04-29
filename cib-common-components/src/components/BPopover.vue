<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
<template>
  <div v-show="isVisible" ref="popover" class="popover-content" :class="wrapperClass" :style="{ maxWidth: maxWidth, width: width || 'auto' }"
    role="tooltip" :aria-hidden="!isVisible" :id="popoverId">
    <slot></slot>
  </div>
</template>

<script>
import { createPopper } from '@popperjs/core'

export default {
  props: {
    target: { type: [Function, String], required: false },
    placement: { type: String, default: 'bottom' }, // 'top', 'bottom', 'left', 'right'
    triggers: { type: String, default: 'click' }, // 'click', 'hover'
    show: { type: Boolean, default: false },
    wrapperClass: { type: String, default: 'border rounded shadow-sm bg-white p-3' },
    hideDelay: { type: Number, default: 200 },
    viewportPadding: { type: Number, default: 20 },
    transitionDuration: { type: Number, default: 250 },
    width: { type: String, default: null },
    maxWidth: { type: String, default: null }
  },
  data: function () {
    return {
      isVisible: false,
      popperInstance: null,
      hideTimeout: null,
      triggerType: null,
      popoverId: `popover-${Math.random().toString(36).substr(2, 9)}`
    }
  },
  watch: {
    show: function (newValue) {
      if (newValue) {
        this.showPopover()
      } else {
        this.hidePopover()
      }
    }
  },
  mounted: function () {
    this.movePopoverToBody()
    this.setEventListeners()
    const popover = this.$refs.popover

    if (popover) {
      popover.addEventListener('mouseenter', this.cancelHidePopover)
      popover.addEventListener('mouseleave', this.scheduleHidePopover)
    }

    const targetElement = this.getTargetElement()
    if (targetElement) {
      targetElement.setAttribute('aria-describedby', this.popoverId)
    }

    document.addEventListener('click', this.handleDocumentClick)
    document.addEventListener('keydown', this.handleKeydown)
    window.addEventListener('resize', this.hidePopover)
    if (this.show) {
      setTimeout(() => {
        this.showPopover()
      }, 1000)
    }
  },
  beforeUnmount: function () {
    const popover = this.$refs.popover

    if (popover) {
      popover.removeEventListener('mouseenter', this.cancelHidePopover)
      popover.removeEventListener('mouseleave', this.scheduleHidePopover)
    }

    const targetElement = this.getTargetElement()
    if (targetElement) {
      targetElement.removeAttribute('aria-describedby')
    }

    this.destroyPopper()
    document.removeEventListener('click', this.handleDocumentClick)
    document.removeEventListener('keydown', this.handleKeydown)
    window.removeEventListener('resize', this.hidePopover)

    if (this.$refs.popover && this.$refs.popover.parentNode) {
      this.$refs.popover.parentNode.removeChild(this.$refs.popover)
    }
  },
  methods: {
    movePopoverToBody: function () {
      document.body.appendChild(this.$refs.popover)
    },
    getTargetElement: function () {
      if (typeof this.target === 'function') {
        const target = this.target()
        if (target instanceof HTMLElement) {
          return target
        } else if (target && target.$el instanceof HTMLElement) {
          return target.$el
        }
        //console.error('Target function did not return a valid HTMLElement or Vue component with $el.')
        return null
      } else if (typeof this.target === 'string') {
        return document.getElementById(this.target)
      }
      //console.error('Invalid target type. Expected a function or string.')
      return null
    },
    setEventListeners: function () {
      const targetElement = this.getTargetElement()
      if (!targetElement) {
        //console.error('Target element for popover not found.')
        return
      }

      const events = this.triggers.split(' ')
      events.forEach(event => {
        if (event === 'click') {
          targetElement.addEventListener('click', this.handleClickTrigger)
        } else if (event === 'hover') {
          targetElement.addEventListener('mouseenter', this.handleHoverTrigger)
          targetElement.addEventListener('mouseleave', this.scheduleHidePopover)
        }
      })
    },
    handleClickTrigger: function () {
      this.triggerType = 'click'
      this.togglePopover()
    },
    handleHoverTrigger: function () {
      this.triggerType = 'hover'
      this.showPopover()
    },
    togglePopover: function () {
      this.isVisible = !this.isVisible
      if (this.isVisible) {
        this.createPopperInstance()
      } else {
        this.destroyPopper()
      }
    },
    showPopover: function () {
      this.cancelHidePopover()
      this.isVisible = true
      this.createPopperInstance()
      this.$refs.popover.focus()
    },
    hidePopover: function () {
      if (this.triggerType === 'click') {
        return
      }
      this.isVisible = false
      this.destroyPopper()
    },
    scheduleHidePopover: function () {
      if (this.triggers === 'manual') return
      this.hideTimeout = setTimeout(() => {
        this.hidePopover()
      }, this.hideDelay)
    },
    cancelHidePopover: function () {
      if (this.hideTimeout) {
        clearTimeout(this.hideTimeout)
        this.hideTimeout = null
      }
    },
    createPopperInstance: function () {
      const targetElement = this.getTargetElement()
      if (!targetElement || !this.$refs.popover) return

      this.popperInstance = createPopper(targetElement, this.$refs.popover, {
        placement: this.placement,
        modifiers: [
          {
            name: 'preventOverflow',
            options: {
              boundary: 'viewport',
              padding: this.viewportPadding
            },
          },
          {
            name: 'offset',
            options: {
              offset: [0, 8]
            },
          },
        ],
      })

      this.$refs.popover.style.zIndex = '1050'
    },
    destroyPopper: function () {
      if (this.popperInstance) {
        this.popperInstance.destroy()
        this.popperInstance = null
      }
    },
    handleDocumentClick: function (event) {
      if (this.triggerType === 'click' && this.isVisible) {
        const targetElement = this.getTargetElement()
        if (
          this.$refs.popover &&
          !this.$refs.popover.contains(event.target) &&
          targetElement &&
          !targetElement.contains(event.target)
        ) {
          this.isVisible = false
          this.destroyPopper()
        }
      }
    },
    handleKeydown: function (event) {
      if (event.key === 'Escape' && this.isVisible) {
        this.hidePopover()
      }
    }
  }
}
</script>

<style scoped>
</style>
