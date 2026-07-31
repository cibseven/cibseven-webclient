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
    <p class="mb-2 mt-3">
      <span class="mdi mdi-18px mdi-information-outline text-info me-1"></span>
      {{ $t('start.defaultStartForm.introduction') }}
    </p>

    <b-form-group :label="$t('start.defaultStartForm.businessKey')">
      <b-form-input v-model.trim="businessKey" :placeholder="$t('start.defaultStartForm.businessKeyPlaceholder')"></b-form-input>
    </b-form-group>

    <div class="table-responsive border rounded">
      <FlowTable
        :items="variables"
        :fields="variableFields"
        striped
        hover
        class="mb-0"
        primary-key="name"
        :sort-by="'name'"
        :sort-desc="false"
      >
        <template v-slot:cell(actions)="row">
          <CellActionButton @click="showEditVariable(row.item)" icon="mdi-square-edit-outline" :title="$t('task-variables.editVariable.tooltip', { name: row.item.name })"></CellActionButton>
          <CellActionButton @click="removeVariable(row.item)" icon="mdi-delete-outline" :title="$t('task-variables.removeVariable.tooltip', { name: row.item.name })"></CellActionButton>
        </template>
      </FlowTable>

      <div v-if="variables.length === 0" class="text-muted text-center py-3">
        {{ $t('task-variables.noVariables') }}
      </div>

      <div class="my-3 ms-3">
        <button
          @click="showAddVariableModal"
          class="btn btn-sm btn-outline-secondary"
          type="button"
          :title="$t('task-variables.addVariable.tooltip')"
        >
          <span class="mdi mdi-plus"></span>
          {{ $t('task-variables.addVariable.title') }}
        </button>
      </div>
    </div>

    <AddVariableModalUI
      ref="addVariableModalUI"
      :edit-mode="editMode"
      :allow-edit-name="allowEditName"
      :allow-file-upload="false"
      :saving="saving"
      @add-variable="changeVariable"></AddVariableModalUI>
  </div>
</template>

<script>
import { FlowTable } from '@cib/common-frontend'
import CellActionButton from '@/components/common-components/CellActionButton.vue'
import AddVariableModalUI from '@/components/process/modals/AddVariableModalUI.vue'

export default {
  name: 'StartProcessDefaultForm',
  components: { FlowTable, CellActionButton, AddVariableModalUI },
  emits: ['start'],
  data: function() {
    return {
      businessKey: '',
      variables: [],
      variableFields: [
        {
          label: 'process-instance.variables.name',
          key: 'name',
          class: 'col-4'
        },
        {
          label: 'process-instance.variables.type',
          key: 'type',
          class: 'col-3'
        },
        {
          label: 'process-instance.variables.value',
          key: 'value',
          class: 'col-3'
        },
        {
          label: 'process-instance.variables.actions',
          key: 'actions',
          class: 'col-2',
          tdClass: 'py-0',
          sortable: false
        }
      ],
      editMode: false,
      allowEditName: true,
      saving: false,
      editingVariableName: null
    }
  },
  computed: {
    variablesToSubmit: function() {
      return this.variables.reduce((obj, variable) => {
        obj[variable.name] = {
          type: variable.type,
          value: variable.value,
        }
        if (variable.valueInfo !== undefined) {
          obj[variable.name].valueInfo = variable.valueInfo
        }
        return obj
      }, {})
    }
  },
  methods: {
    showAddVariableModal: function() {
      this.editMode = false
      this.allowEditName = true
      this.editingVariableName = null
      this.$refs.addVariableModalUI.show()
    },
    showEditVariable: function(variable) {
      this.editMode = true
      this.allowEditName = true
      this.editingVariableName = variable.name
      this.$refs.addVariableModalUI.show(JSON.parse(JSON.stringify(variable)))
    },
    removeVariable: function(variable) {
      const index = this.variables.findIndex(v => v.name === variable.name)
      if (index >= 0) this.variables.splice(index, 1)
    },
    changeVariable: function(variable) {
      if (this.editMode && this.editingVariableName !== null) {
        const oldIndex = this.variables.findIndex(v => v.name === this.editingVariableName)
        if (oldIndex >= 0) this.variables.splice(oldIndex, 1)
      }
      const existingIndex = this.variables.findIndex(v => v.name === variable.name)
      if (existingIndex >= 0) {
        this.variables.splice(existingIndex, 1, variable)
      } else {
        this.variables.push(variable)
      }
      this.$refs.addVariableModalUI.hide()
    },
    onStart: function() {
      this.$emit('start', { businessKey: this.businessKey || null, variables: this.variablesToSubmit })
    }
  }
}
</script>
