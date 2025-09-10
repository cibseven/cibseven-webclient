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
  <b-modal ref="advancedSearchModal" :title="$t('advanced-search.title')" @hidden="onModalHidden" size="lg">
    <form ref="formAdvancedSearch" @submit.stop.prevent="handleSubmit">
      <div class="row mb-3">
        <div class="col-5">
          <label>{{ $t('advanced-search.criteriaKey') }}</label>
          <b-form-select v-model="selectedCriteriaKey" :options="criteriaKeys">
            <template v-slot:first>
              <b-form-select-option :value="null" disabled>-- {{ $t('advanced-search.selectProperty') }} --</b-form-select-option>
            </template>
          </b-form-select>
        </div>
      </div>
      <div class="row mb-3">
        <div class="col-5">
          <b-form-input :placeholder="$t('advanced-search.property')" v-model="selectedCriteriaValue.name"></b-form-input>
        </div>
        <div class="col-2">
          <b-form-select v-model="selectedCriteriaValue.operator" :options="operators"></b-form-select>
        </div>
        <div class="col-5">
          <b-form-input :placeholder="$t('advanced-search.value')" v-model="selectedCriteriaValue.value"></b-form-input>
        </div>
      </div>
      <b-button @click="addCriteria" :title="$t('advanced-search.add')" variant="primary" class="mb-3">
        <span class="d-inline-block align-middle mdi mdi-16px mdi-plus" style="line-height: 0"></span> {{ $t('advanced-search.add') }}
      </b-button>

      <div class="container-fluid border">
        <FlowTable striped :selectable="false" :items="criterias" prefix="advanced-search.table."
          :fields="[{label: 'key', key: 'key', class: 'col-5'},
              {label: 'value', key: 'value', class: 'col-5'},
              {label: '', key: 'buttons', class: 'col-2', sortable: false, tdClass: 'py-0 text-center d-block'}]">
          <template v-slot:cell(key)="row">
            {{ $t('advanced-search.criteriaKeys.' + row.item.key) }}
          </template>
          <template v-slot:cell(value)="row">
            <div class="col-12" v-if="row.item.key === 'processVariables'">
              <div class="row">
                <div :title="row.item.name" class="col-5 p-0 text-truncate">{{ row.item.name }}</div>
                <div class="col-2 text-center">{{ $t('advanced-search.operators.' + row.item.operator) }}</div>
                <div :title="row.item.value" class="col-5 p-0 text-truncate text-end">{{ row.item.value }}</div>
              </div>
            </div>
            <span v-else> {{ row.item.value }} </span>
          </template>
          <template v-slot:cell(buttons)="row">
            <b-button class="p-0 px-2 border-0 mdi mdi-24px mdi-delete-outline shadow-none" variant="link" @click="deleteCriteria(row.index)"></b-button>
          </template>
        </FlowTable>

        <div v-if="criterias.length === 0">
          <img src="@/assets/images/task/no_tasks_pending.svg" class="d-block mx-auto mb-3" style="width: 200px">
          <div class="h5 text-secondary text-center">{{ $t('nav-bar.filters.noCriterias') }}</div>
        </div>
        <hr>
        <b-form-checkbox class="mb-3" v-model="matchAllCriteria" name="check-button" switch>
          <span>{{ $t('nav-bar.filters.matchAllCriteria') }}</span>
        </b-form-checkbox>
      </div>

    </form>
    <template #modal-footer>
      <b-button @click="$refs.advancedSearchModal.hide()" variant="link">{{ $t('advanced-search.cancel') }}</b-button>
      <b-button @click="handleSubmit" variant="primary">{{ $t('advanced-search.apply') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
import FlowTable from '@/components/common-components/FlowTable.vue'

export default {
  name: 'AdvancedSearchModal',
  components: { FlowTable },
  emits: ['refresh-tasks'],
  data: function () {
    return {
      matchAllCriteria: true,
      criterias: [],
      selectedCriteriaKey: 'processVariables',
      selectedCriteriaValue: { name: null, operator: 'eq', value: '' },
      operators: [
        { value: 'eq', text: '=' },
        { value: 'neq', text: '!=' },
        { value: 'gt', text: '>' },
        { value: 'gteq', text: '>=' },
        { value: 'lt', text: '<' },
        { value: 'lteq', text: '<=' },
        { value: 'like', text: 'like' },
        { value: 'notLike', text: 'not like' }
      ],
      isValidForm: true
    }
  },
  computed: {
    criteriaKeys: function() {
      var criteriaKeys = []
      this.$root.config.taskFilter.advancedSearch.criteriaKeys.forEach(item => {
        criteriaKeys.push({ value: item, text: this.$t('advanced-search.criteriaKeys.' + item) })
      })
      return criteriaKeys
    }
  },
  methods: {
    show: function() {
      if (this.$store.state.advancedSearch.criterias.length > 0) {
        this.matchAllCriteria = this.$store.state.advancedSearch.matchAllCriteria
        this.criterias = this.$store.state.advancedSearch.criterias.map(criteria => {
          return Object.assign({}, criteria)
        })
      }
      this.$refs.advancedSearchModal.show()
    },
    addCriteria: function() {
      var value = this.selectedCriteriaValue.value
      if (value === "true") value = true
      else if (value === "false") value = false
      this.criterias.push({
        key: this.selectedCriteriaKey,
        name: this.selectedCriteriaValue.name,
        operator: this.selectedCriteriaValue.operator,
        value: value
      })
      this.selectedCriteriaValue = { name: null, operator: 'eq', value: '' }
    },
    deleteCriteria: function(index) {
      this.criterias.splice(index, 1)
    },
    cleanAllCriteria: function() {
      this.criteria = []
    },
    handleSubmit: function() {
      this.$store.dispatch('updateAdvancedSearch', {
        matchAllCriteria: this.matchAllCriteria,
        criterias: this.criterias
      })
      this.$emit('refresh-tasks')
      this.$refs.advancedSearchModal.hide()
    },
    onModalHidden: function() {
      this.criterias = []
      this.matchAllCriteria = true
    }
  }
}
</script>
