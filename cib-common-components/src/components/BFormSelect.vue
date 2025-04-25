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
  <div :class="['form-group', wrapperClass]">
    <select
      :id="id"
      :class="['form-select', selectClass, sizeClass]"
      :style="selectStyle"
      v-model="value"
      :disabled="disabled"
      :aria-describedby="ariaDescribedby"
      @blur="handleBlur"
      @focus="handleFocus">
      <slot name="first"></slot>      
      <slot>
        <template v-for="(group, groupIndex) in groupedOptions" :key="groupIndex">
          <optgroup v-if="group.label" :label="group.label">
            <option v-for="(option, index) in group.options"
              :key="'group-' + groupIndex + '-option-' + index"
              :value="option.value">
              {{ option.text }}
            </option>
          </optgroup>
          <option v-else v-for="(option, index) in group.options"
            :key="'ungrouped-' + groupIndex + '-option-' + index"
            :value="option.value">
            {{ option.text }}
          </option>
        </template>
      </slot>
    </select>
  </div>
</template>

<script>
export default {
  props: {
    helloWorld: {},
    modelValue: { type: [String, Number], default: null },
    options: {
      type: Array,
      default: () => []
    },
    id: { type: String, default: null },
    selectClass: { type: String, default: '' },
    selectStyle: { type: Object, default: () => ({}) },
    wrapperClass: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
    ariaDescribedby: { type: String, default: null },
    size: { type: String, default: 'md' }
  },
  computed: {
    sizeClass() {
      if (this.size === 'sm') return 'form-select-sm'
      else if (this.size === 'lg') return 'form-select-lg'
    },
    groupedOptions: function () {
      return this.options.map(option => {
        if (typeof option === 'object' && option.label && Array.isArray(option.options)) {
          return option
        }
        let normalizedOption = {}
        if (typeof option === 'object') normalizedOption = option
        else normalizedOption = { text: option, value: option }
        return { options: [normalizedOption] }
      })
    },
    value: {
      set: function (val) {
        this.$emit('update:modelValue', val)
      },
      get: function () {
        return this.modelValue
      }
    }
  },
  methods: {
    handleBlur: function (event) {
      this.$emit('blur', event)
    },
    handleFocus: function (event) {
      this.$emit('focus', event)
    }
  }
}
</script>

<style scoped>
</style>
