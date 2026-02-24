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
  <div v-bind="$attrs" class="border-0-on-hover">
    <label for="searchInput" class="visually-hidden">{{ $t('searches.search').replace('...', '') }}</label>
    <span class="mdi mdi-magnify mdi-16px"
      :class="{
        'leading-icon-sm': size === 'sm',
        'leading-icon-md': size === 'md',
        'leading-icon-lg': size === 'lg'
      }"
      aria-hidden="true"
    ></span>
    <input
      id="searchInput"
      class="form-control border-light input-with-leading-icon"
      :class="{
        'form-control-sm': size === 'sm',
        'form-control-md': size === 'md',
        'form-control-lg': size === 'lg'
      }"
      type="text"
      :placeholder="$t('searches.search')"
      :aria-label="$t('searches.search').replace('...', '')"
      :value="modelValue"
      :disabled="disabled"
      @input="onInput"
    >
  </div>
</template>

<script>
export default {
  name: 'SearchInput',
  emits: ['update:modelValue'],
  props: { 
    modelValue: { type: String, default: '' },
    modelModifiers: { type: Object, default: () => ({}) },
    disabled: { type: Boolean, default: false },
    size: {
      type: String,
      default: 'md',
      validator: value => ['sm', 'md', 'lg'].includes(value)
    }
  },
  methods: {
    onInput: function(event) {
      let value = event.target.value
      if (this.modelModifiers.trim) value = value.trim()
      this.$emit('update:modelValue', value)
    }
  }
}
</script>

<style lang="css" scoped>
.border-0-on-hover:hover {
  border-color: transparent !important;
}

.leading-icon-sm,
.leading-icon-md,
.leading-icon-lg {
  position: absolute;
  margin-left: 8px;
  height: 25px;
  display: flex;
  align-items: center;
}

.leading-icon-sm {
  margin-top: 2px;
}
.leading-icon-md {
  margin-top: 5px;
}
.leading-icon-lg {
  margin-top: 6px;
}

.input-with-leading-icon {
  padding-left: 28px;
}
</style>
