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
  <div :class="['position-relative', { 'd-inline-block': noWrap }]"
        :style="{ position: noWrap ? 'relative' : 'static' }"
        :aria-busy="show ? 'true' : 'false'"
        :aria-live="show ? 'polite' : null">
    <slot></slot>
    <div v-if="show" class="overlay position-absolute w-100 h-100"
      :class="{ 'd-flex': !noCenter, 'align-items-center': !noCenter, 'justify-content-center': !noCenter }"
      style="top: 0; left: 0;">
      <div class="w-100 h-100 position-absolute" :style="{ backgroundColor: overlayColor }"></div>
      <div class="position-relative">
        <slot name="overlay"></slot>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    show: { type: Boolean, default: false },
    opacity: { type: Number, default: 0.5 },
    noCenter: { type: Boolean, default: false },
    noWrap: { type: Boolean, default: false }
  },
  computed: {
    overlayColor: function () {
      return `rgba(255, 255, 255, ${this.opacity})`
    }
  }
}
</script>

<style scoped>
</style>
