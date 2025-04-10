<template>
  <b-modal size="lg" scrollable ref="filterHandler" :title="$t('nav-bar.filters.addFilter')" @shown="$emit('display-popover', false)" @hidden="$emit('display-popover', true)">
    <div class="row">
      <div class="col-md-8">
        <b-form-group label-size="sm" :label-cols="4" :label="$t('nav-bar.filters.filterNameLabel')" :invalid-feedback="$t('nav-bar.filters.filterExists')">
          <b-form-input size="sm" v-model="selectedFilterName" :placeholder="$t('nav-bar.filters.filterNamePlaceholder')" :state="existFilter ? false : null"></b-form-input>
        </b-form-group>
      </div>
      <div class="col-md-4">
        <b-form-group label-size="sm" :label-cols="6" :label="$t('nav-bar.filters.filterPriorityLabel')">
          <b-form-input size="sm" v-model="selectedFilterPriority"></b-form-input>
        </b-form-group>
      </div>
    </div>
    <hr class="my-0">
    <b-form-group :label-cols="12" label-class="mb-3" label-size="sm" :label="$t('nav-bar.filters.selectedCriteria')">
      <div class="container">
        <div class="row text-center px-3">
          <b-form-select style="background-image: none" class="col-4" size="sm" @change="selectCriteria($event)" v-model="selectedCriteriaKey" :options="criteriasGrouped">
            <template v-slot:first>
              <b-form-select-option :value="null" disabled>-- {{ $t('nav-bar.filters.selectProperty') }} --</b-form-select-option>
            </template>
          </b-form-select>
          <div v-if="selectedCriteriaType !== 'variable'" class="col-5 pe-0">
            <FilterableSelect v-if="$store.state.user.searchUsers.length > 1 && selectedCriteriaType === 'filterable'" class="w-100" :placeholder="$t('nav-bar.filters.insertValue')"
            v-model="selectedCriteriaValue" :elements="$store.state.user.searchUsers" noInvalidValues/>
            <b-form-input v-else size="sm" :placeholder="$t('nav-bar.filters.insertValue')" v-model="selectedCriteriaValue"></b-form-input>
          </div>
          <div class="col-3 p-0">
            <b-button v-if="!isEditing" :disabled="!selectedCriteriaKey" @click="addCriteria" size="sm" class="mdi mdi-plus" variant="secondary">{{ $t('nav-bar.filters.addCriteria') }}</b-button>
            <div v-else>
              <b-button style="opacity: 1" class="px-2 border-0 shadow-none" variant="link">
              <span class="mdi mdi-18px mdi-content-save-outline" @click="updateCriteria()"></span></b-button>
              <span class="border-start h-50"></span>
              <b-button style="opacity: 1" class="px-2 border-0 shadow-none" variant="link" @click="cancelEditCriteria()"><span class="mdi mdi-18px mdi-block-helper"></span></b-button>
            </div>
          </div>
        </div>
        <div class="row mt-2"><small class="col-12" style="color: var(--gray)" v-html="$t('nav-bar.filters.legendExpression')"></small></div>
        <div class="row"><small class="col-12" style="color: var(--gray)">{{ $t('nav-bar.filters.legendMultiple') }}</small></div>
        <div v-if="selectedCriteriaType === 'variable'" class="mt-4">
          <div v-for="(criteria, index) of selectedCriteriaVariable" class="col-12 input-group px-0 pb-3" :key="index">
            <b-form-input class="rounded me-2" size="sm" :placeholder="$t('nav-bar.filters.insertVariableKey')" v-model="criteria.name"></b-form-input>
            <b-form-select class="rounded me-2" :options="variableOperators" size="sm" v-model="criteria.operator"></b-form-select>
            <b-form-input class="rounded me-2" size="sm" :placeholder="$t('nav-bar.filters.insertValue')" v-model="criteria.value"></b-form-input>
            <b-button :class="index > 0 ? '': 'invisible'" variant="outline-secondary" class="mdi mdi-18px mdi-delete-outline border-0 p-0" @click="deleteProcessVariable(index)"></b-button>
          </div>
          <b-button size="sm" variant="outline-secondary" class="mdi mdi-18px mdi-plus-circle-outline border-0" @click="addProcessVariable()"></b-button>
        </div>
      </div>
    </b-form-group>

    <div class="container-fluid border">
      <FlowTable :selectable="false" striped :items="criteriasToAdd" prefix="nav-bar.filters.criteria."
        :fields="[{label: 'key', key: 'name', class: 'col-5'},
            {label: 'value', key: 'value', class: 'col-5'},
            {label: '', key: 'buttons', class: 'col-2', sortable: false, tdClass: 'py-0'}]">
        <template v-slot:cell(value)="row">
          <div v-if="row.item.key === 'processVariables'">
            <div v-for="(item, index) of row.item.value" class="row g-0" :key="index">
              <div :title="item.name" class="col-5 p-0 text-truncate">{{ item.name }}</div>
              <div class="col-2 text-center">{{ $t('nav-bar.filters.operators.' + item.operator) }}</div>
              <div :title="item.value" class="col-5 p-0 text-truncate text-end">{{ item.value }}</div>
            </div>
          </div>
          <span v-else> {{ formatCriteria(row.item.value) }} </span>
        </template>
        <template v-slot:cell(buttons)="row">
          <b-button class="mdi mdi-18px mdi-pencil border-0" size="sm" variant="outline-secondary" @click="editCriteria(row.index)" :title="$t('commons.edit')"></b-button>
          <b-button class="mdi mdi-18px mdi-delete-outline border-0" size="sm" variant="outline-secondary" @click="deleteCriteria(row.index)" :title="$t('confirm.delete')"></b-button>
        </template>
      </FlowTable>
      <div v-if="criteriasToAdd.length < 1">
        <img src="/assets/images/task/no_tasks_pending.svg" class="d-block mx-auto mb-3" style="width: 200px">
        <div class="h5 text-secondary text-center">{{ $t('nav-bar.filters.noCriterias') }}</div>
      </div>
      <hr v-if="criteriasToAdd.length < 1">
      <b-form-checkbox v-if="existCandidateSelected" class="mb-3" v-model="includeAssigned" switch>
        <span>{{ $t('nav-bar.filters.includeAssigned') }}</span>
      </b-form-checkbox>
      <b-form-checkbox class="mb-3" v-model="matchAllCriteria" name="check-button" switch>
        <span>{{ $t('nav-bar.filters.matchAllCriteria') }}</span>
        <!-- <span v-if="matchAllCriteria">{{ $t('nav-bar.filters.matchAllCriteria') }}</span>
        <span v-else>{{ $t('nav-bar.filters.matchAnyCriteria') }}</span> -->
      </b-form-checkbox>
    </div>

    <template v-slot:modal-footer>
      <b-button @click="$refs.filterHandler.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="createFilter" :disabled="checkValidity" variant="primary">{{ mode === 'create' ? $t('nav-bar.filters.addFilter') : $t('nav-bar.filters.updateFilter') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import FilterableSelect from '@/components/task/filter/FilterableSelect.vue'
import FlowTable from '@/components/common-components/FlowTable.vue'

const candidateOptions = ['candidateGroup', 'candidateGroupExpression',
  'candidateGroups', 'candidateGroupsExpression', 'candidateUser', 'candidateUserExpression']

export default {
  name: 'FilterModal',
  components: { FilterableSelect, FlowTable },
  props: { tasks: Array, processes: Array, layout2: Boolean },
  mixins: [permissionsMixin],
  data: function () {
    return {
      mode: 'create',
      matchAllCriteria: true,
      filters: [],
      criterias: [],
      criteriasGrouped: [],
      selectedFilterId: null,
      selectedFilterName: '',
      selectedFilterPriority: 0,
      selectedCriteriaKey: null,
      selectedCriteriaValue: null,
      selectedCriteriaType: null,
      selectedCriteriaVariable: [{ name: '', operator: 'eq', value: '' }],
      criteriasToAdd: [],
      includeAssigned: false,
      isEditing: false,
      criteriaEdited: { key: null, rowIndex: null},
    }
  },
  watch: {
    '$store.state.filter.selected': function() {
      this.selectedFilterId = this.$store.state.filter.selected.id
      this.selectedFilterName = this.$store.state.filter.selected.name
      this.selectedFilterPriority = this.$store.state.filter.selected.properties.priority
    },
    selectedCriteriaKey: function(newValue) {
      this.selectCriteria(newValue)
    }
  },
  computed: {
    existFilter: function() {
      var checkNotSelected = this.mode === 'edit' ? this.$store.state.filter.selected.name !== this.selectedFilterName : true
      var checkExists = this.$store.state.filter.list.find(row => { return row.name === this.selectedFilterName })
      return checkExists && checkNotSelected
    },
    checkValidity: function() {
      var invalidFilter = this.criteriasToAdd.find(row => { return row.key === null })
      return !!this.existFilter || !!invalidFilter || this.selectedFilterName.length === 0
    },
    variableOperators: function() {
      return  [
        { value: 'eq', text: this.$t('nav-bar.filters.operators.txteq') },
        { value: 'neq', text: this.$t('nav-bar.filters.operators.txtneq') },
        { value: 'gt', text: this.$t('nav-bar.filters.operators.txtgt') },
        { value: 'gteq', text: this.$t('nav-bar.filters.operators.txtgteq') },
        { value: 'lt', text: this.$t('nav-bar.filters.operators.txtlt') },
        { value: 'lteq', text: this.$t('nav-bar.filters.operators.txtlteq') }
      ]
    },
    existCandidateSelected: function() {
      return this.criteriasToAdd.some(criteria => {
        return candidateOptions.includes(criteria.key)
      })
    }
  },
  created: function() {
    this.selectFilter(this.$store.state.filter.selected.id)
  },
  methods: { // TODO: Refactor, many methods and unnecessary structur,,
    selectFilter: function(value) {
      var selectedFilter = this.$store.state.filter.list.find(filter => {
        return filter.id === value
      })
      if (selectedFilter) {
        this.selectedFilterId = selectedFilter.id
        this.selectedFilterName = selectedFilter.name
        this.selectedFilterPriority = selectedFilter.properties.priority
      }
    },
    createFilter: function() {
      var query = {}
      if (this.matchAllCriteria) {
        this.criteriasToAdd.forEach(criteria => {
          query[criteria.key] = criteria.value
        })
        if (this.existCandidateSelected) query.includeAssignedTasks = this.includeAssigned
      } else {
        query.orQueries = []
        query.orQueries.push({})
        this.criteriasToAdd.forEach(criteria => {
          query.orQueries[0][criteria.key] = criteria.value
        })
        if (this.existCandidateSelected) query.orQueries[0].includeAssignedTasks = this.includeAssigned
      }
      if (this.mode === 'edit') {
        this.$store.state.filter.selected.name = this.selectedFilterName
        this.$store.state.filter.selected.properties.priority = this.selectedFilterPriority || 0
        this.$store.state.filter.selected.query = query
        this.$store.dispatch('updateFilter', { filter: this.$store.state.filter.selected }).then(() => {
          this.$emit('filter-alert', { message: 'msgFilterUpdated', filter: this.selectedFilterName })
          this.$refs.filterHandler.hide()
          this.$emit('set-filter', this.$store.state.filter.selected.id)
          this.selectedFilterId = this.$store.state.filter.selected.id
          this.$emit('filter-updated', this.selectedFilter)
        }, () => {
          this.$root.$refs.error.show({ type: 'filterSaveError' })
        })
      } else {
        var filterCreate = {
          id: null,
          resourceType: 'Task',
          name: this.selectedFilterName,
          owner: null,
          query: query,
          properties: {
            color: '#555555',
            showUndefinedVariable: false,
            description: '',
            refresh: true,
            priority: this.selectedFilterPriority || 0
          }
        }
        this.$store.dispatch('createFilter', { filter: filterCreate }).then(filter => {
          this.$emit('filter-alert', { message: 'msgFilterCreated', filter: this.selectedFilterName })
          this.$refs.filterHandler.hide()
          this.$emit('set-filter', filter.id)
          this.$emit('select-filter', filter)
          this.$store.state.filter.selected = filter
          this.selectedFilterId = filter.id
          localStorage.setItem('filter', JSON.stringify(this.$store.state.filter.selected))
        }, () => {
          this.$root.$refs.error.show({ type: 'filterSaveError' })
        })
      }
    },
    addProcessVariable: function() {
      this.selectedCriteriaVariable.push({ name: '', operator: 'eq', value: '' })
    },
    deleteProcessVariable: function(index) {
      if (this.selectedCriteriaVariable.length > index)
      this.selectedCriteriaVariable.splice(index, 1)
    },
    rowClass: function(item) {
      let stylesForRow = ['row']
      if (item.key === this.criteriaEdited.key ) stylesForRow.push('table-active')
      return stylesForRow
    },
    addCriteria: function() {
      var valueToAdd = []
      if (this.selectedCriteriaType === 'variable') {
        valueToAdd = this.selectedCriteriaVariable
      } else if (this.selectedCriteriaType === 'array') {
        this.selectedCriteriaValue.split(',').forEach(value => {
          valueToAdd.push(value.trim())
        })
      } else valueToAdd = this.selectedCriteriaValue
      this.criteriasToAdd.push({
        key: this.selectedCriteriaKey,
        name: this.$t('nav-bar.filters.keys.' + this.selectedCriteriaKey),
        value: valueToAdd, type: this.selectedCriteriaType
      })
      this.selectedCriteriaKey = null
      this.selectedCriteriaValue = null
      this.selectedCriteriaType = null
      this.selectedCriteriaVariable = [{ name: '', operator: 'eq', value: '' }]
    },
    updateCriteria: function() {
      var valueToAdd = []
      if (this.selectedCriteriaType === 'variable') {
        valueToAdd = this.selectedCriteriaVariable
      } else if (this.selectedCriteriaType === 'array') {
        this.selectedCriteriaValue.split(',').forEach(value => {
        valueToAdd.push(value.trim())
        })
      } else valueToAdd = this.selectedCriteriaValue

        this.criteriasToAdd.splice(this.criteriaEdited.rowIndex, 1, {
        key: this.selectedCriteriaKey,
        name: this.$t('nav-bar.filters.keys.' + this.selectedCriteriaKey),
        value: valueToAdd,
        type: this.selectedCriteriaType
        })
        this.selectedCriteriaKey = null
        this.selectedCriteriaValue = null
        this.selectedCriteriaType = null
        this.selectedCriteriaVariable = [{ name: '', operator: 'eq', value: '' }]

        this.cancelEditCriteria()
    },
    deleteCriteria: function(index) {
      this.criteriasToAdd.splice(index, 1)
    },
    editCriteria: function(index) {
      this.isEditing = true
      // so the row of the table doesnt change too when the inputs are modified
      let criteriaToEdit = JSON.parse(JSON.stringify(this.criteriasToAdd[index]))
      this.criteriaEdited = { key: criteriaToEdit.key, rowIndex: index }
      this.selectedCriteriaKey = criteriaToEdit.key
      this.selectCriteria(this.selectedCriteriaKey)
      if(this.selectedCriteriaType === 'array' && criteriaToEdit.value.length >0) {
        this.selectedCriteriaValue = criteriaToEdit.value[0]
      }
      else if ( this.selectedCriteriaType === 'variable' || Array.isArray(criteriaToEdit.value) && criteriaToEdit.value.length > 0) {
        this.selectedCriteriaVariable = criteriaToEdit.value
        this.selectedCriteriaValue = criteriaToEdit.value[0].value
      } else {
      this.selectedCriteriaValue = criteriaToEdit.value
      }
    },
    cancelEditCriteria: function() {
      this.isEditing = false
      this.selectedCriteriaKey = null
      this.selectedCriteriaValue = null
      this.selectedCriteriaType = null
      this.criteriaEdited = { key: null, rowIndex: null }
    },
    selectCriteria: function(evt) {
      var criteria = this.criterias.find(option => {
        return option.value === evt
      })
      if (criteria) this.selectedCriteriaType = criteria.type
    },
    showFilterDialog: function(mode) {
      this.mode = mode
      this.criterias = []
      this.criteriasGrouped = []
      this.criteriasToAdd = []
      this.selectedFilterName = ''
      this.selectedFilterPriority = 0
      this.selectedCriteriaKey = null
      this.selectedCriteriaValue = null
      this.selectedCriteriaType = null
      this.includeAssigned = false
      this.selectedCriteriaVariable = [{ name: '', operator: 'eq', value: '' }]
      this.isEditing = false,
      this.criteriaEdited = { key: null, rowIndex: null}

      // Prepared criterias
      var auxCriterias = {}
      this.$root.config.filters.forEach(filter => {
        if (filter.group) {
          if (auxCriterias[filter.group] === undefined) {
            auxCriterias[filter.group] = { "label": this.$t('nav-bar.filters.keys.' + filter.group), "options" : [] }
          }
          auxCriterias[filter.group].options.push(
            { value: filter.key, text: this.$t('nav-bar.filters.keys.' + filter.key), type: filter.type })
        } else {
          this.criteriasGrouped.push({ value: filter.key, text: this.$t('nav-bar.filters.keys.' + filter.key), type: filter.type })
        }
        this.criterias.push({ value: filter.key, text: this.$t('nav-bar.filters.keys.' + filter.key), type: filter.type })
      })

      Object.keys(auxCriterias).forEach(group => {
        this.criteriasGrouped.push(auxCriterias[group])
      })

      if(this.criteriasGrouped.length <= 0) this.criteriasGrouped = this.criterias

      if (this.mode === 'edit') {
        this.selectedFilterId = this.$store.state.filter.selected.id
        this.selectedFilterName = this.$store.state.filter.selected.name
        this.selectedFilterPriority = this.$store.state.filter.selected.properties.priority

        // Not matched all criterias.
        if (!this.$store.state.filter.selected.query) return
        if (this.$store.state.filter.selected.query.orQueries && this.$store.state.filter.selected.query.orQueries.length > 0) {
          this.matchAllCriteria = false
          Object.keys(this.$store.state.filter.selected.query.orQueries[0]).forEach(key => {
            var filterVal = this.$store.state.filter.selected.query.orQueries[0][key]
            if (key === 'includeAssignedTasks') this.includeAssigned = filterVal
            if (!filterVal || (filterVal && filterVal.length === 0)) return
            var index = this.criterias.findIndex(item => {
              return item.value === key
            })
            if (index > -1) {
              this.criteriasToAdd.push({
                key: key,
                name: this.$t('nav-bar.filters.keys.' + key),
                value: filterVal
              })
            }
          })
        } else {
          //Match all criterias.
          Object.keys(this.$store.state.filter.selected.query).forEach(key => {
            var filterVal = this.$store.state.filter.selected.query[key]
            if (key === 'includeAssignedTasks') this.includeAssigned = filterVal
            if (!filterVal || (filterVal && filterVal.length === 0)) return
            var index = this.criterias.findIndex(item => {
              return item.value === key
            })
            if (index > -1) {
              this.criteriasToAdd.push({
                key: key,
                name: this.$t('nav-bar.filters.keys.' + key),
                value: filterVal
              })
            }
          })
        }
      }
      this.$refs.filterHandler.show()
    },
    formatCriteria: function(value) {
      return Array.isArray(value) ? value.join(', ') : value
    }
  }
}
</script>
