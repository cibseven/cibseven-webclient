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
  <component
    v-if="valueToCopy"
    :is="componentType"
    :to="routerTo"
    :class="containerClasses"
    :title="title || displayValue"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
    @click="handleClick"
  >
    {{ displayValue }}
    <span
      v-if="isHovered"
      @click.stop.prevent="handleCopy"
      :title="$t('commons.copyValue') + ':\n' + valueToCopy"
      class="mdi mdi-18px mdi-content-copy position-absolute end-0 text-secondary lh-sm"
    ></span>
  </component>
</template>

<script>
export default {
  name: 'CopyableActionButton',
  props: {
    /**
     * The value to display in the button
     */
    displayValue: {
      type: String,
      default: '',
    },
    /**
     * The value to copy to clipboard (defaults to displayValue)
     */
    copyValue: {
      type: String,
      default: null,
    },
    /**
     * Custom title attribute (defaults to displayValue)
     */
    title: {
      type: String,
      default: null,
    },
    /**
     * Whether the component should be clickable (button) or just text with copy
     */
    clickable: {
      type: Boolean,
      default: true,
    },
    /**
     * Router link destination - can be string, object, or route location
     */
    to: {
      type: [String, Object],
      default: null,
    },
  },
  emits: ['click', 'copy'],
  data() {
    return {
      isHovered: false,
    }
  },
  computed: {
    componentType() {
      if (this.to) {
        return 'router-link'
      }
      return this.clickable ? 'button' : 'div'
    },
    routerTo() {
      return this.to || undefined
    },
    valueToCopy() {
      return this.copyValue || this.displayValue
    },
    containerClasses() {
      const baseClasses = {
        'text-truncate': true,
        'pe-4': this.isHovered,
        'position-relative': true,
        'w-100': true,
      }
      if (this.to) {
        // Router link styling
        return {
          ...baseClasses,
          'text-info': true,
          'd-block': true,
        }
      } else if (this.clickable) {
        // Button styling
        return {
          ...baseClasses,
          btn: true,
          'btn-link': true,
          'p-0': true,
          'text-info': true,
          'text-start': true,
          'd-block': true,
        }
      } else {
        // Non-clickable div styling
        return {
          ...baseClasses,
        }
      }
    },
  },
  methods: {
    handleClick() {
      // For router links, the navigation is handled automatically by Vue Router
      // Only emit click event for buttons or non-router links
      if (!this.to && this.clickable) {
        this.$emit('click')
      }
    },
    handleCopy() {
      this.$emit('copy', this.valueToCopy)
    },
  },
}
</script>
