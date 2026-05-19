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
  <div class="mt-2">
    <p class="mb-2 fw-bold">{{ $t('task-variables.title', { name: task.name }) }}</p>
    <p class="mb-2">{{ $t('task-variables.introduction') }}</p>
    <p v-if="businessKey" class="mb-2">{{ $t('task-variables.businessKey', { businessKey: businessKey }) }}</p>

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
        <template v-slot:cell(name)="table">
          <CopyableActionButton
            :displayValue="table.item.name"
            :clickable="false"
            :title="$t('process-instance.variables.name') + ':\n' + table.item.name"
            @copy="copyValueToClipboard"
          />       
        </template>
        <template v-slot:cell(value)="row">
          <CopyableActionButton
            :displayValue="displayVariableValue(row.item)"
            :clickable="isFile(row.item)"
            :title="displayVariableValue(row.item)"
            @click="downloadFile(row.item)"
            @copy="copyValueToClipboard"
          />          
        </template>
        <template v-slot:cell(actions)="row">
          <CellActionButton v-if="isFile(row.item)" :title="displayVariableValue(row.item)"
            icon="mdi-download-outline"
            @click="downloadFile(row.item)">
          </CellActionButton>
          <CellActionButton v-if="!isFile(row.item)" @click="showEditVariable(row.item, row.index)" icon="mdi-square-edit-outline" :title="$t('task-variables.editVariable.tooltip', { name: row.item.name })"></CellActionButton>
          <CellActionButton v-if="!row.item.existing || row.item.changed" @click="removeVariable(row.index)" icon="mdi-delete-outline" :title="$t('task-variables.removeVariable.tooltip', { name: row.item.name })"></CellActionButton>
        </template>
      </FlowTable>

      <!-- Variables Table -->
      <div v-if="variables.length === 0" class="text-muted text-center py-3">
        <template v-if="existingVariablesShown">
          {{ $t('task-variables.noVariables') }}
        </template>
        <template v-else>
          {{ $t('task-variables.noVariablesLoaded') }}
        </template>
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

      <div class="my-3 ms-3" v-if="!existingVariablesShown">
        <button
          @click="showExistingVariables"
          class="btn btn-sm btn-outline-secondary"
          type="button"
          :title="$t('task-variables.showExistingVariables.tooltip', { name: task.name })"
        >
          <span class="mdi mdi-eye me-1"></span>
          {{ $t('task-variables.showExistingVariables.title') }}
        </button>
      </div>

    </div>

    <!-- Add Variable Modal -->
    <AddVariableModalUI
      ref="addVariableModalUI"
      :edit-mode="editMode"
      :allow-edit-name="true"
      :allow-file-upload="false"
      :saving="saving"
      @add-variable="changeVariable"></AddVariableModalUI>    

    <SuccessAlert ref="messageCopy" style="z-index: 9999"> {{ $t('process.copySuccess') }} </SuccessAlert>
    <TaskPopper ref="importPopper"></TaskPopper>
</div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { FormsService, ProcessService } from '@/services.js'
import { FlowTable, CopyableActionButton, TaskPopper } from '@cib/common-frontend'
import CellActionButton from '@/components/common-components/CellActionButton.vue'
import AddVariableModalUI from '@/components/process/modals/AddVariableModalUI.vue'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'

export default {
  name: 'TaskVariables',
  props: {
    task: {
      type: Object,
      required: true
    }
  },
  mixins: [permissionsMixin, copyToClipboardMixin],
  components: { FlowTable, CopyableActionButton, TaskPopper, CellActionButton, AddVariableModalUI },
  emits: ['variables-updated'],
  data() {
    return {
      businessKey: null,
      variables: [],
      variableFields: [
        {
          label: 'process-instance.variables.name',
          key: 'name',
          class: 'col-3'
        },
        {
          label: 'process-instance.variables.type',
          key: 'type',
          class: 'col-2'
        },
        {
          label: 'process-instance.variables.value',
          key: 'value',
          class: 'col-5'
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
      saving: false,
      editingVariable: null,
      editingVariableIndex: -1,
      existingVariablesShown: false,
    }
  },
  computed: {
    variablesToSubmit() {
      const ar = this.variables.filter(variable => !variable.existing || variable.changed).map(variable => ({
        name: variable.name,
        type: variable.type,
        value: variable.value
      }))

      return ar.reduce((obj, variable) => {
        obj[variable.name] = { type: variable.type, value: variable.value }
        return obj
      }, {})
    }
  },
  mounted() {
    ProcessService.findProcessInstance(this.task.processInstanceId).then(processInstance => {
      this.businessKey = processInstance.businessKey
    })
  },
  methods: {
    async showExistingVariables() {
      try {
        await FormsService.fetchVariables(this.task.id).then(variablesObject => {
          const variablesArray = Object.entries(variablesObject).map(([name, variable]) => ({
            name,
            type: variable.type,
            value: variable.value,
            valueInfo: variable.valueInfo,
            existing: true,
            changed: false,
          }))

          variablesArray.forEach(variable => {
            this.appendVariable(this.variables, variable, false)
          })

          this.existingVariablesShown = true
        })
      } catch (error) {
        console.error('Error loading task variables:', error)
        this.existingVariablesShown = false
      }
    },
    showAddVariableModal() {
      this.editMode = false
      this.saving = false
      this.editingVariable = null
      this.editingVariableIndex = -1
      this.$refs.addVariableModalUI.show()
    },
    showEditVariable(variable, index) {
      this.editMode = true
      this.saving = false
      this.editingVariable = JSON.parse(JSON.stringify(variable))
      this.editingVariableIndex = index
      this.$refs.addVariableModalUI.show(this.editingVariable)
    },
    async removeVariable(index) {
      this.variables.splice(index, 1)
    },
    changeVariable(variable) {
      this.saving = true
      if (this.editMode) {
        variable.existing = this.editingVariable.existing
        variable.changed = true
      }
      else {
        variable.existing = false
        variable.changed = true
      }
      this.appendVariable(this.variables, variable, true)
      this.$emit('variables-updated', this.variablesToSubmit)
      this.$refs.addVariableModalUI.hide()
      this.saving = false
    },
    displayVariableValue(variable) {
      if (this.isFileValueDataSource(variable)) {
        return this.getFileVariableName(variable)
      }
      if (variable.type === 'File') {
        return variable.valueInfo.filename
      }
      if (variable.value === null || variable.value === undefined) {
        return 'null'
      }
      if (variable.type === 'Boolean') {
        return variable.value ? 'true' : 'false'
      }
      if (typeof variable.value === 'object') {
        return JSON.stringify(variable.value)
      }
      return String(variable.value)
    },
    appendVariable(variables, variable, changed = false) {
      const existingIndex = variables.findIndex(v => v.name === variable.name)
      if (existingIndex >= 0) {
        const existingVariable = variables[existingIndex]
        variable.existing = existingVariable.existing
        variable.changed = changed
        variables.splice(existingIndex, 1, variable)
      } else {
        variable.changed = changed
        variables.push(variable)
      }
    },
    isFile(item) {
      return (item.type === 'File') || this.isFileValueDataSource(item)
    },
    isFileValueDataSource(item) {
      if (item.type === 'Object') {
        const objectTypeName =
          (item.value && item.value.objectTypeName) ||
          (item.valueInfo && item.valueInfo.objectTypeName)
        if (objectTypeName && this.fileObjects.includes(objectTypeName)) return true
      }
      return false
    },
    getFileVariableName(item) {
      // Prioritize valueDeserialized over value
      const targetValue = item.valueDeserialized || item.value
      if (targetValue && typeof targetValue === 'object' && targetValue.name) {
        return targetValue.name
      }
      if (targetValue && typeof targetValue === 'string') {
        try {
          const parsed = JSON.parse(targetValue)
          if (parsed && parsed.name) return parsed.name
        } catch { return '' }
      }
      return ''
    },
		downloadFile(variable) {
			if (variable.type === 'Object') {
				const blob = new Blob([Uint8Array.from(atob(variable.value.data), c => c.codePointAt(0))], { type: variable.value.contentType })
				this.$refs.importPopper.triggerDownload(blob, this.getFileVariableName(variable))
			} else {
				ProcessService.fetchVariableDataByExecutionId(this.task.executionId, variable.name).then(data => {
					this.$refs.importPopper.triggerDownload(data, variable.valueInfo.filename)
				})
			}
		},
  }
}
</script>
