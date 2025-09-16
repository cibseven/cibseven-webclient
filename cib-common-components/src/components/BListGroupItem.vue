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
    :is="itemTag" :class="['list-group-item', itemClasses]"
    :role="isButton ? 'button' : 'listitem'" :aria-current="active ? 'true' : null"
    :aria-disabled="disabled ? 'true' : null" :tabindex="disabled ? -1 : 0"
    @click="handleClick" @mousedown="$emit('mousedown', $event)" style="cursor: pointer"
    @keydown.space.prevent="handleClick" @keydown.enter.prevent="handleClick"
    @mouseenter="$emit('mouseenter', $event)" @mouseleave="$emit('mouseleave', $event)">
      <slot></slot>
  </component>
</template>

<script>
export default {
  props: {
    to: { type: [String, Object], default: null },
    href: { type: String, default: null },
    button: { type: Boolean, default: false },
    active: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false },
    variant: { type: String, default: 'light' }
  },
  computed: {
    itemClasses: function () {
      const classes = {
        'list-group-item-action': this.isButton || this.isLink,
        active: this.active,
        disabled: this.disabled
      }
      if (this.variant) {
        classes[`list-group-item-${this.variant}`] = true
      }
      return classes
    },
    isLink: function () {
      return this.to || this.href
    },
    isButton: function () {
      return this.button
    },
    itemTag: function () {
      return this.isLink ? 'a' : this.isButton ? 'button' : 'li'
    }
  },
  methods: {
    handleClick: function (event) {
      if (this.disabled) {
        event.preventDefault()
        return
      }
      if (this.isLink) {
        if (this.to) {
          this.$router.push(this.to)
        } else if (this.href) {
          window.location.href = this.href
        }
      }
    }
  }
}
</script>

<style scoped>
</style>
