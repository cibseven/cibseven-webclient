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
  <div :class="['d-inline-block', wrapperClass]" role="status" :aria-live="ariaLive" :aria-busy="ariaBusy">
    <span v-if="label" class="visually-hidden">{{ label }}</span>
    <div :class="[
      'spinner-' + type,
      variant ? 'text-' + variant : '',
      {
        'spinner-grow': isGrow,
        'spinner-border': !isGrow,
        'spinner-border-sm': small && !isGrow,
        'spinner-grow-sm': small && isGrow
      }]" :style="computedStyle"></div>
  </div>
</template>

<script>
export default {
  props: {
    type: { type: String, default: 'border' },
    variant: { type: String, default: '' },
    label: { type: String, default: 'Loading...' },
    small: { type: Boolean, default: false },
    wrapperClass: { type: String, default: '' },
    ariaLive: { type: String, default: 'polite' },
    ariaBusy: { type: Boolean, default: true },
    style: { type: String, default: '' }
  },
  computed: {
    isGrow: function () {
      return this.type === 'grow'
    },
    computedStyle() {
      return this.small ? '' : this.style
    }
  }
}
</script>

<style scoped>
</style>
