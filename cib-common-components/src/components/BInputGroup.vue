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
  <div :class="['input-group', wrapperClass, sizeClass]" :style="customStyle">
    <slot name="prepend"></slot>
    <input v-if="!hasDefaultSlot"
      :id="id"
      :type="type"
      :class="['form-control', inputClass]"
      :value="modelValue"
      :aria-labelledby="ariaLabelledby"
      :aria-describedby="ariaDescribedby"
      @input="$emit('update:modelValue', $event.target.value)" />
    <slot></slot>
    <slot name="append"></slot>
  </div>
</template>

<script>
export default {
  props: {
    modelValue: { type: [String, Number], default: '' },
    id: { type: String, default: null },
    type: { type: String, default: 'text' },
    inputClass: { type: String, default: '' },
    wrapperClass: { type: String, default: '' },
    customStyle: { type: [String, Object], default: '' },
    size: { type: String, default: null },
    ariaLabelledby: { type: String, default: null },
    ariaDescribedby: { type: String, default: null }
  },
  computed: {
    sizeClass: function () {
      return this.size === 'sm' ? 'input-group-sm' : this.size === 'lg' ? 'input-group-lg' : ''
    },
    hasDefaultSlot: function () {
      return !!this.$slots.default
    }
  }
}
</script>

<style scoped>
</style>
