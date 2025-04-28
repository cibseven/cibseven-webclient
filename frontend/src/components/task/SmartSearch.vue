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
    <b-input-group>
      <template v-slot:prepend>
        <b-input-group-text class="py-0 border-light"><span class="mdi mdi-18px mdi-magnify"
          style="line-height: initial"></span></b-input-group-text>
      </template>
      <b-form-input :title="$t('searches.searchByTaskName')" size="sm" ref="input" type="search" v-model="filter" :list="id"
        class="form-control border-start-0 ps-0 form-control border-light shadow-none"
        :placeholder="$t('searches.searchByTaskName')" :maxlength="maxlength" @input="$emit('search-filter', $event.target.value.trim())"/>
      <template v-slot:append v-if="$root.config.taskFilter.advancedSearch.criteriaKeys.length > 0 &&
        $root.config.taskFilter.advancedSearch.modalEnabled">
        <b-button variant="light" class="py-0" @click="$emit('open-advanced-search')" :title="$t('advanced-search.title')">
          <span class="mdi mdi-18px mdi-filter-variant" style="line-height: initial"></span>
          <span v-if="$store.state.advancedSearch.criterias.length > 0" class="bg-danger position-absolute rounded"
            style="bottom: 5px; width: 7px; height: 7px; right: 5px;"></span>
        </b-button>
      </template>
    </b-input-group>
  </div>
</template>

<script>
export default {
  name: 'SmartSearch',
  props: { options: Array, maxlength: Number },
  computed: { id: function() { return 'smart-search' + Date.now() } }, // https://dev.to/rahmanfadhil/how-to-generate-unique-id-in-javascript-1b13
  data: function() { return { filter: '' } },
  methods: {
    focus: function() { this.$refs.input.focus() }
  }
}
</script>
