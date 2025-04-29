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
  <div class="mb-3">
    <label v-if="$slots.label" :for="id" class="form-label">
      <slot name="label"></slot>
    </label>
    <label v-else :for="id" class="form-label">{{ label }}</label>
    <input :id="id" :class="classes" type="file" :accept="accept" :multiple="multiple" @change="handleChange" />
  </div>
</template>

<script>
export default {
  props: {
    id: { type: String },
    label: { type: String },
    accept: { type: String, default: '' },
    multiple: { type: Boolean, default: false },
    modelValue: { type: [File, Array], default: () => [] },
    variant: { type: String, default: 'primary' }
  },
  data: function () {
    return {
      files: []
    }
  },
  computed: {
    classes: function () {
      return ['form-control', `form-control-${this.variant}`]
    }
  },
  watch: {
    modelValue: {
      immediate: true,
      handler(newVal) {
        if (newVal && Array.isArray(newVal)) {
          this.files = newVal
        }
      }
    }
  },
  methods: {
    handleChange(event) {
      const selectedFiles = this.multiple ? Array.from(event.target.files) : event.target.files[0]
      this.files = selectedFiles
      this.$emit('update:modelValue', selectedFiles)
    }
  }
}
</script>

<style scoped>
</style>
