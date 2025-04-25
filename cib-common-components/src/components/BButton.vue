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
    :is="tag"
    ref="button"
    class="btn"
    :class="[sizeClass, classes]"
    :href="computedHref"
    :to="isRouterLink ? to : null"
    :role="tag === 'button' ? 'button' : null"
    :aria-disabled="disabled ? 'true' : null"
    :disabled="disabled && tag === 'button'">
      <slot></slot>
  </component>
</template>

<script>
export default {
  props: {
    variant: { type: String, default: 'primary' },
    block: { type: Boolean, default: false },
    href: { type: String, default: null },
    to: { type: [String, Object], default: null },
    size: { type: String, default: null },
    disabled: { type: Boolean, default: false }
  },
  computed: {
    tag: function () {
      return this.href ? 'a' : (this.to ? 'router-link' : 'button')
    },
    classes: function () {
      const res = []
      res.push('btn-' + this.variant)
      if (this.block) res.push('w-100')
      return res
    },
    sizeClass: function () {
      return this.size === 'sm' ? 'btn-sm' : this.size === 'lg' ? 'btn-lg' : ''
    },
    computedHref: function () {
      return this.href ? this.href : null
    },
    isRouterLink: function () {
      return !!this.to
    }
  },
  methods: {
    click: function (event) {
      if (this.disabled) {
        event.preventDefault()
        event.stopImmediatePropagation()
        return
      }
      if (!this.href && !this.to) {
        this.$emit('click', event)
      }
    },
    submit: function (event) {
      if (this.disabled) {
        event.preventDefault()
        event.stopImmediatePropagation()
        return
      }
      if (!this.href && !this.to) {
        this.$emit('submit', event)
      }
    },
    focus: function () {
      const element = this.$refs.button
      if (element && element.focus) {
        element.focus()
      }
    }
  }
}
</script>

<style scoped>
</style>
