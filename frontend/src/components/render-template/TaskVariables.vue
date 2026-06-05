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
          <div class="w-100" :class="!row.item.existing || row.item.changed ? 'fw-semibold' : ''">
            <CopyableActionButton
              :displayValue="displayVariableValue(row.item)"
              :clickable="isFile(row.item) && row.item.existing"
              :title="displayValueTooltip(row.item)"
              @click="downloadFile(row.item)"
              @copy="copyValueToClipboard"
            />
          </div>
        </template>
        <template v-slot:cell(actions)="row">
          <CellActionButton v-if="isFile(row.item) && row.item.existing" :title="displayVariableValue(row.item)"
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
          :disabled="loadingVariables"
          :title="$t('task-variables.showExistingVariables.tooltip', { name: task.name })"
        >
          <BWaitingBox v-if="loadingVariables" class="d-inline me-2" styling="width: 16px"></BWaitingBox>
          <span v-else class="mdi mdi-eye me-1"></span>
          {{ $t('task-variables.showExistingVariables.title') }}
        </button>
      </div>

    </div>

    <!-- Add Variable Modal -->
    <AddVariableModalUI
      ref="addVariableModalUI"
      :edit-mode="editMode"
      :allow-edit-name="allowEditName"
      :allow-file-upload="true"
      :saving="saving"
      @add-variable="changeVariable"></AddVariableModalUI>    

    <SuccessAlert ref="messageCopy" style="z-index: 9999"> {{ $t('process.copySuccess') }} </SuccessAlert>
    <TaskPopper ref="importPopper"></TaskPopper>
</div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { FormsService, ProcessService } from '@/services.js'
import { FlowTable, CopyableActionButton, TaskPopper, BWaitingBox } from '@cib/common-frontend'
import CellActionButton from '@/components/common-components/CellActionButton.vue'
import AddVariableModalUI from '@/components/process/modals/AddVariableModalUI.vue'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { getFileContentBase64 } from '@/utils/fileUtils.js'
import variableUtils from '@/components/process/mixins/variableUtils.js'

export default {
  name: 'TaskVariables',
  props: {
    task: {
      type: Object,
      required: true
    }
  },
  mixins: [permissionsMixin, copyToClipboardMixin],
  components: { FlowTable, CopyableActionButton, TaskPopper, BWaitingBox, CellActionButton, AddVariableModalUI },
  emits: ['variables-updated'],
  data() {
    return {
      businessKey: null,
      loadingVariables: false,
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
      allowEditName: false,
      saving: false,
      editingVariable: null,
      editingVariableIndex: -1,
      existingVariablesShown: false,
    }
  },
  computed: {
    variablesToSubmit() {
      const ar = this.variables.filter(variable => !variable.existing || variable.changed)
      return ar.reduce((obj, variable) => {
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
  mounted() {
    ProcessService.findProcessInstance(this.task.processInstanceId).then(processInstance => {
      this.businessKey = processInstance.businessKey
    })
  },
  methods: {
    async showExistingVariables() {
      try {
        this.loadingVariables = true
        await FormsService.fetchVariables(this.task.id).then(async variablesObject => {
          const variablesArray = Object.entries(variablesObject).map(([name, variable]) => ({
            name,
            type: variable.type,
            value: variable.value,
            valueInfo: variable.valueInfo,
            existing: true,
            changed: false,
          }))

          for (const variable of variablesArray) {
            await this.appendVariable(this.variables, variable, true)
          }

          this.existingVariablesShown = true
        })
      } catch (error) {
        console.error('Error loading task variables:', error)
        this.existingVariablesShown = false
      } finally {
        this.loadingVariables = false
      }
    },
    showAddVariableModal() {
      this.editMode = false
      this.allowEditName = true
      this.saving = false
      this.editingVariable = null
      this.editingVariableIndex = -1
      this.$refs.addVariableModalUI.show()
    },
    showEditVariable(variable, index) {
      this.editMode = true
      this.allowEditName = !variable.existing
      this.saving = false
      this.editingVariable = JSON.parse(JSON.stringify(variable))
      this.editingVariableIndex = index
      this.$refs.addVariableModalUI.show(this.editingVariable)
    },
    async removeVariable(index) {
      this.variables.splice(index, 1)
    },
    async changeVariable(variable) {
      this.saving = true
      if (this.editMode) {
        variable.existing = this.editingVariable.existing
        variable.changed = true
      }
      else {
        variable.existing = false
        variable.changed = true
      }
      await this.appendVariable(this.variables, variable, false)
      this.$emit('variables-updated', this.variablesToSubmit)
      this.$refs.addVariableModalUI.hide()
      this.saving = false
    },
    async appendVariable(variables, variable, fromRemote) {

      // load file content
      if (!fromRemote && this.isFile(variable)) {
        variable.valueInfo = {
          filename: variable.file.name,
          mimeType: variable.file.type,
        }
        variable.value = await getFileContentBase64(variable.file)
      }

      // add or update variable in list
      const existingIndex = variables.findIndex(v => v.name === variable.name)
      if (existingIndex >= 0) {
        const existingVariable = variables[existingIndex]
        variable.existing = fromRemote ? true : existingVariable.existing
        variable.changed = fromRemote ? false : variable.changed
        variables.splice(existingIndex, 1, variable)
      } else {
        variable.existing = fromRemote
        variable.changed = fromRemote ? false : variable.changed
        variables.push(variable)
      }
    },

    displayVariableValue(variable) {
      return variableUtils.displayValue(variable)
    },
    displayValueTooltip(variable) {
      if (this.isFile(variable) && variable.existing) {
        return this.$t('process-instance.download') + ': ' + this.displayVariableValue(variable)
      }
      else {
        return this.displayVariableValue(variable)
      }
    },
    isFile(variable) {
      return variableUtils.isFile(variable)
    },
    isFileValueDataSource(variable) {
      return variableUtils.isFileValueDataSource(variable)
    },
    getFileVariableName(variable) {
      return variableUtils.getFileVariableName(variable)
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
