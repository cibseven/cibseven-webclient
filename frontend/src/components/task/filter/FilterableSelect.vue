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
  <div>
    <b-dd block @shown="$refs.filter.focus(); $emit('shown')"
      toggle-tag="div" @hidden="filter = ''" toggle-class="p-0 border-0"
      @show="function(evt) { disabled ? evt.preventDefault() : null }" @hide="$emit('hide')" no-caret>
      <template v-slot:button-content>
        <b-input-group>
          <b-form-input size="sm" type="text" ref="inputFilter" :value="value"
            style="cursor: auto" :placeholder="placeholder" @keydown.prevent ></b-form-input>
        </b-input-group>
      </template>
      <b-dd-form form-class="p-2"><b-form-input class="w-100" ref="filter" v-model="filter" @input="onInput"></b-form-input></b-dd-form>
      <div v-if="loading" class="w-100 text-center"><b-spinner></b-spinner></div>
      <b-dropdown-group v-if="elements && !loading" style="max-height:150px" class="overflow-auto">
        <b-dd-item-btn button-class="p-1 d-flex" v-for="(label, value, index) in filteredElements"
          :key="index" @click="$emit('update:modelValue', label.id)">
            <b-avatar class="me-2" :text="label.id.substring(0, 2)" variant="light"></b-avatar>
            <span class="me-auto">
              <div>{{ label.firstName + " " + label.lastName }}</div>
              <div class="small">{{ label.id }}</div>
            </span>
        </b-dd-item-btn>
      </b-dropdown-group>
    </b-dd>
  </div>
</template>

<script>
import { debounce } from '@/utils/debounce.js'

export default {
  name: 'FilterableSelect',
  props: {
    elements: [ Object, Array ],
    value: String,
    disabled: Boolean,
    required: Boolean,
    invalidFeedback: String,
    noInvalidValues: { type: Boolean, default: false },
    placeholder: String,
    loading: { type: Boolean, default: false }
  },
  data: function() {
    return { filter: '' }
  },
  watch: {
    filter: function(val) {
      if (val.length >= 3 && !this.loading) this.$emit('update:loading', true)
      else if (val.length < 3) {
        this.$emit('clean-elements', val)
        this.$emit('update:loading', false)
      }
    }
  },
  computed: {
    filteredElements: function() {
      var list = {}
      if (this.elements) {
        if (Array.isArray(this.elements)) {
          this.elements.forEach(element => {
            if((element.id.toLowerCase().indexOf(this.filter.toLowerCase()) !== -1) ||
              ((element.firstName + ' ' + element.lastName).toLowerCase().indexOf(this.filter.toLowerCase()) !== -1))
              list[element.id] = element
          })
        } else {
          Object.keys(this.elements).forEach(key => {
            if ((this.elements[key].id.toLowerCase().indexOf(this.filter.toLowerCase()) !== -1) ||
              ((this.elements[key].firstName + ' ' + this.elements[key].lastName).toLowerCase().indexOf(this.filter.toLowerCase()) !== -1))
              list[key] = this.elements[key]
          })
        }
      }
      return list
    },
    isValid: function() {
      var allElements = this.elements
      if (!Array.isArray(allElements)) allElements = Object.keys(allElements)
      return !this.noInvalidValues || !this.value ||
        allElements.includes(element => { if (this.value === element.id) return true; else return false })
    }
  },
  methods: {
    onInput: debounce(800, function() { if (this.filter.length >= 3) this.$emit('enter', this.filter) })
  }
}
</script>
