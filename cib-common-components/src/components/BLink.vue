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
    :href="computedHref"
    :to="isRouterLink ? to : null"
    :class="linkClasses"
    :target="target"
    :rel="rel"
    :disabled="disabled ? 'disabled' : null"
    @click="handleClick"
  >
    <slot></slot>
  </component>
</template>

<script>
export default {
  props: {
    href: { type: String, default: null },
    to: { type: [String, Object], default: null },
    target: { type: String, default: null },
    rel: { type: String, default: null },
    disabled: { type: Boolean, default: false },
    variant: { type: String, default: 'primary' },
    active: { type: Boolean, default: false },
    underline: { type: Boolean, default: true },
    buttonStyle: { type: Boolean, default: false }
  },
  computed: {
    isRouterLink() {
      return !!this.to
    },
    tag() {
      if (this.isRouterLink) {
        return 'router-link'
      }
      if (this.href) {
        return 'a'
      }
      return 'button'
    },
    computedHref() {
      return this.href && !this.isRouterLink ? this.href : null;
    },
    linkClasses() {
      return [
        this.buttonStyle ? 'btn' : 'nav-link',
        this.variant && this.buttonStyle ? `btn-${this.variant}` : null,
        this.active ? 'active' : null,
        this.disabled ? 'disabled' : null,
        !this.underline && !this.buttonStyle ? 'text-decoration-none' : null
      ]
    }
  },
  methods: {
    handleClick(event) {
      if (this.disabled) {
        event.preventDefault()
        event.stopPropagation()
      }
    }
  }
}
</script>

<style scoped>
</style>
