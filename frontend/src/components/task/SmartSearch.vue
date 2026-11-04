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
    <datalist :id="id">
      <option v-for="(opt, index) in options" :key="index">{{ opt }}</option>
    </datalist>
    <SearchInput ref="input" :input-id="id" size="sm" type="search" :model-value="filter"
      :label="$t('searches.searchByTaskName')" @update:model-value="onInput">
      <template v-if="$root.config.taskFilter.advancedSearch.criteriaKeys.length > 0 &&
        $root.config.taskFilter.advancedSearch.modalEnabled" #append>
        <button type="button" class="search-field-button position-relative" @click="$emit('open-advanced-search')"
          :title="$t('advanced-search.title')" :aria-label="$t('advanced-search.title')">
          <span class="mdi mdi-18px mdi-filter-variant" aria-hidden="true"></span>
          <span v-if="$store.state.advancedSearch.criterias.length > 0" class="bg-danger position-absolute rounded"
            style="bottom: 0; width: 7px; height: 7px; right: 0;"></span>
        </button>
      </template>
    </SearchInput>
  </div>
</template>

<script>
import SearchInput from '@/components/common-components/SearchInput.vue'

export default {
  name: 'SmartSearch',
  components: { SearchInput },
  props: { 
    options: Array, 
    initialFilter: { type: String, default: '' }
  },
  emits: ['open-advanced-search', 'search-filter'],
  computed: { id: function() { return 'smart-search' + Date.now() } }, // https://dev.to/rahmanfadhil/how-to-generate-unique-id-in-javascript-1b13
  data: function() { return { filter: '' } },
  watch: {
    initialFilter: {
      immediate: true,
      handler: function(newVal) {
        this.filter = newVal
      }
    }
  },
  methods: {
    focus: function() { this.$refs.input.focus() },
    onInput: function(value) {
      this.filter = value
      this.$emit('search-filter', value.trim())
    }
  }
}
</script>
