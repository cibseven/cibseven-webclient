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
  <!-- One visual field: the focus ring is drawn on this container (:focus-within), not on the inner input -->
  <div class="search-field form-control" :class="{ 'form-control-sm': size === 'sm', 'disabled': disabled }">
    <button v-if="searchButton" type="button" class="search-field-button"
      :title="computedSearchButtonLabel" :aria-label="computedSearchButtonLabel" :disabled="disabled"
      @click.stop="onSearch">
      <span class="mdi mdi-magnify" :class="iconSizeClass" aria-hidden="true"></span>
    </button>
    <span v-else class="search-field-icon mdi mdi-magnify" :class="iconSizeClass" aria-hidden="true"></span>
    <label v-if="!hideLabel" :for="inputId" class="visually-hidden">{{ computedLabel.replace('...', '') }}</label>
    <input
      :id="inputId"
      ref="input"
      class="search-field-input"
      :type="type"
      :placeholder="placeholder || computedLabel"
      :title="computedLabel"
      :value="modelValue"
      :disabled="disabled"
      :autocomplete="autocomplete"
      :aria-describedby="describedBy || null"
      @input="onInput"
      @keydown.enter="onEnter"
    >
    <slot name="append"></slot>
  </div>
</template>

<script>
let counter = 0

export default {
  name: 'SearchInput',
  emits: ['update:modelValue', 'search'],
  props: {
    modelValue: { type: String, default: '' },
    modelModifiers: { type: Object, default: () => ({}) },
    disabled: { type: Boolean, default: false },
    label: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    // Set when the page renders its own <label :for="inputId">
    hideLabel: { type: Boolean, default: false },
    inputId: { type: String, default: () => `search-input-${++counter}` },
    type: { type: String, default: 'text' },
    autocomplete: { type: String, default: null },
    describedBy: { type: String, default: '' },
    // Renders the magnifier as a button that triggers the same 'search' action as pressing Enter.
    // Without it the magnifier is decorative only and not focusable.
    searchButton: { type: Boolean, default: false },
    searchButtonLabel: { type: String, default: '' },
    size: {
      type: String,
      default: 'md',
      validator: value => ['sm', 'md', 'lg'].includes(value)
    }
  },
  computed: {
    computedLabel() {
      return this.label || this.$t('searches.search')
    },
    computedSearchButtonLabel() {
      return this.searchButtonLabel || this.$t('searches.search')
    },
    iconSizeClass() {
      return this.size === 'sm' ? 'mdi-16px' : 'mdi-18px'
    }
  },
  methods: {
    focus() {
      this.$refs.input.focus()
    },
    normalize(value) {
      return this.modelModifiers.trim ? value.trim() : value
    },
    onInput(event) {
      this.$emit('update:modelValue', this.normalize(event.target.value))
    },
    onSearch() {
      this.$emit('search', this.normalize(this.$refs.input.value))
    },
    onEnter() {
      if (this.searchButton) this.onSearch()
    }
  }
}
</script>

<style lang="css" scoped>
.search-field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* Same ring the theme draws on a focused .form-control, moved to the whole field; an outline causes no layout shift */
.search-field:focus-within {
  border-color: var(--bs-gray-800) !important;
  outline: 2px solid var(--bs-gray-800);
  outline-offset: -1px;
}

.search-field.disabled {
  background-color: var(--bs-secondary-bg);
}

.search-field.disabled:hover {
  border-color: var(--bs-border-color) !important;
}

.search-field-icon {
  line-height: 1;
}

.search-field-input {
  flex: 1 1 auto;
  min-width: 0;
  padding: 0;
  border: 0;
  background-color: transparent;
  color: inherit;
}

.search-field-input:focus {
  outline: none !important;
  box-shadow: none !important;
}

/* "Simple icon" buttons inside the field: $secondary, $dark on hover, $dark plus the theme's outline on focus-visible */
.search-field-button,
.search-field :slotted(.search-field-button) {
  display: inline-flex;
  align-items: center;
  padding: 0;
  border: 0;
  border-radius: var(--bs-border-radius-sm);
  background-color: transparent;
  color: var(--bs-secondary);
  line-height: 1;
}

.search-field-button:not(:disabled):hover,
.search-field-button:focus-visible,
.search-field :slotted(.search-field-button:not(:disabled):hover),
.search-field :slotted(.search-field-button:focus-visible) {
  color: var(--bs-dark);
}

/* The field's :focus-within ring already marks the focus, so the magnifier gets no box of its own */
.search-field-button:focus-visible {
  outline: 0 !important;
}
</style>
