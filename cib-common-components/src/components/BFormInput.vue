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
  <div v-if="hasWrapper" :class="['form-group', wrapperClass]">
    <div :class="['d-flex align-items-center']">
      <input
        ref="input"
        v-bind="$attrs"
        :id="id"
        :type="type"
        :class="['form-control', inputClass, sizeClass, validationClass]"
        :placeholder="placeholder"
        :value="modelValue"
        :aria-describedby="ariaDescribedby"
        :aria-invalid="state === false ? 'true' : null"
        @input="$emit('update:modelValue', $event.target.value)"
        @blur="handleBlur"
        @focus="handleFocus"
      />
      <slot name="append"></slot>
    </div>
    <div v-if="state === false" class="invalid-feedback mt-0">
      <slot name="invalid-feedback">{{ invalidFeedback }}</slot>
    </div>
  </div>
  <input v-else
    ref="input"
    v-bind="$attrs"
    :id="id"
    :type="type"
    :class="['form-control', inputClass, sizeClass, validationClass]"
    :placeholder="placeholder"
    :value="modelValue"
    :aria-describedby="ariaDescribedby"
    :aria-invalid="state === false ? 'true' : null"
    @input="$emit('update:modelValue', $event.target.value)"
    @blur="handleBlur"
    @focus="handleFocus"
  />
</template>

<script>
export default {
  props: {
    modelValue: { type: [String, Number], default: '' },
    placeholder: { type: String, default: '' },
    id: { type: String, default: null },
    type: { type: String, default: 'text' },
    inputClass: { type: String, default: '' },
    wrapperClass: { type: String, default: '' },
    size: { type: String, default: null },
    state: { type: Boolean, default: null },
    invalidFeedback: { type: String, default: '' },
    ariaDescribedby: { type: String, default: null }
  },
  computed: {
    sizeClass: function () {
      return this.size === 'sm' ? 'form-control-sm' : this.size === 'lg' ? 'form-control-lg' : ''
    },
    validationClass: function () {
      if (this.state === true) return 'is-valid'
      if (this.state === false) return 'is-invalid'
      return ''
    },
    hasWrapper: function () {
      return !!this.invalidFeedback || !!this.$slots.append
    }
  },
  methods: {
    handleBlur: function (event) {
      this.$emit('blur', event)
    },
    handleFocus: function (event) {
      this.$emit('focus', event)
    },
    focus: function () {
      this.$refs.input.focus()
    }
  },
  inheritAttrs: false
}
</script>

<style scoped>
</style>
