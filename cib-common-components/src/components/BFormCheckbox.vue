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
  <div :class="['form-check', { 'form-switch': isSwitch }]">
    <label class="form-check-label" :for="id">
      <input type="checkbox" class="form-check-input"
              :id="id"
              :disabled="disabled"
              :required="required"
              :checked="computedChecked"
          @change="updateValue($event)">
      <slot></slot>
    </label>
  </div>
</template>

<script>
export default {
  props: {
    modelValue: {
      type: [Boolean, Array],
      default: false
    },
    value: {
      type: [String, Number, Object],
      default: null
    },
    required: Boolean,
    disabled: Boolean,
    switch: Boolean,
    id: {
      type: String,
      default: () => `checkbox-${Math.random().toString(36).substr(2, 9)}`
    }
  },
  computed: {
    isSwitch: function () {
      return this.switch
    },
    computedChecked: function () {
      if (Array.isArray(this.modelValue)) {
        return this.modelValue.includes(this.value)
      }
      return this.modelValue
    }
  },
  methods: {
    updateValue: function (event) {
      const checked = event.target.checked
      if (Array.isArray(this.modelValue)) {
        const newValue = [...this.modelValue]
        const index = newValue.findIndex(item => item === this.value)
        if (checked && index === -1) {
          newValue.push(this.value)
        } else if (!checked && index > -1) {
          newValue.splice(index, 1)
        }
        this.$emit('update:modelValue', newValue)
      } else {
        this.$emit('update:modelValue', checked)
      }
    }
  }
}
</script>

<style scoped>
</style>
