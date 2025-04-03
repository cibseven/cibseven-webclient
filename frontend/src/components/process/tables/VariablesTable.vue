<template>
  <div v-if="selectedInstance.state === 'ACTIVE'" class="bg-light d-flex position-absolute w-100">
    <div class="py-2 px-2">
      <b-button class="border" size="sm" variant="light" @click="$refs.addVariableModal.show()" :title="$t('process-instance.addVariable')">
        <span class="mdi mdi-plus"></span> {{ $t('process-instance.addVariable') }}
      </b-button>
    </div>
  </div>
  <div class="overflow-auto bg-white position-absolute container-fluid g-0" style="bottom: 0" :style="('top: ' + (selectedInstance.state === 'ACTIVE' ? '45px' : '0'))">
    <FlowTable v-if="!loading" striped resizable thead-class="sticky-header" :items="filteredVariables" primary-key="id" prefix="process-instance.variables."
      sort-by="label" :sort-desc="true" :fields="[
      { label: 'name', key: 'name', class: 'col-3', tdClass: 'py-1 border-end border-top-0' },
      { label: 'type', key: 'type', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'value', key: 'value', class: 'col-4', tdClass: 'py-1 border-end border-top-0' },
      { label: 'scope', key: 'scope', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
      { label: 'actions', key: 'actions', class: 'col-1', sortable: false, tdClass: 'py-1 border-top-0' }]">
      <template v-slot:cell(name)="table">
        <div :title="table.item.name" class="text-truncate">{{ table.item.name }}</div>
      </template>
      <template v-slot:cell(type)="table">
        <div :title="table.item.type" class="text-truncate">{{ table.item.type }}</div>
      </template>
      <template v-slot:cell(value)="table">
        <div v-if="table.item.type === 'File'" class="text-truncate">{{ table.item.valueInfo.filename }}</div>
        <div v-if="isFileValueDataSource(table.item)" class="text-truncate">
          {{ displayObjectNameValue(table.item) }}
        </div>
        <div v-else :title="table.item.value" class="text-truncate">{{ table.item.value }}</div>
      </template>
      <template v-slot:cell(actions)="table">
        <b-button v-if="isFile(table.item)" :title="$t('process-instance.download')"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-download-outline"
          @click="downloadFile(table.item)">
        </b-button>
        <b-button v-if="isFile(table.item)" :title="$t('process-instance.upload')"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-upload-outline"
          @click="selectedVariable = table.item; $refs.uploadFile.show()">
        </b-button>
        <b-button v-if="!['File', 'Null'].includes(table.item.type) && !isFileValueDataSource(table.item)"
          :title="$t('process-instance.edit')" size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-square-edit-outline"
          @click="modifyVariable(table.item)">
        </b-button>
        <b-button v-if="selectedInstance.state !== 'SUSPENDED'" :title="$t('process-instance.edit')" size="sm" variant="outline-secondary"
          class="border-0 mdi mdi-18px mdi-delete-outline" @click="deleteVariable(table.item)"></b-button>
      </template>
    </FlowTable>
    <div v-else>
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
  </div>

  <TaskPopper ref="importPopper"></TaskPopper>

  <b-modal ref="uploadFile" :title="$t('process-instance.upload')">
    <div>
      <b-form-file placeholder="" :browse-text="$t('process-instance.selectFile')" v-model="file"></b-form-file>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.uploadFile.hide(); file = null" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button :disabled="!file" @click="uploadFile(); $refs.uploadFile.hide()" variant="primary">{{ $t('process-instance.upload') }}</b-button>
    </template>
  </b-modal>

  <b-modal ref="modifyVariable" :title="$t('process-instance.edit')">
    <div v-if="variableToModify">
      <b-form-group :label="$t('process-instance.variables.name')">
        <b-form-input v-model="variableToModify.name" disabled></b-form-input>
      </b-form-group>
      <b-form-group :label="$t('process-instance.variables.type')">
        <b-form-input v-model="variableToModify.type" disabled></b-form-input>
      </b-form-group>
      <b-form-group :label="$t('process-instance.variables.value')">
        <b-form-textarea rows="5" placeholder="Enter value" v-model="formattedJsonValue" :disabled="selectedInstance.state === 'COMPLETED'"></b-form-textarea>
      </b-form-group>
    </div>
    <template v-slot:modal-footer>
      <b-button v-if="selectedInstance.state === 'COMPLETED'" @click="$refs.modifyVariable.hide()">{{ $t('confirm.close') }}</b-button>
      <template v-else>
        <b-button @click="$refs.modifyVariable.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
        <b-button @click="updateVariable" variant="primary">{{ $t('process-instance.save') }}</b-button>
      </template>
    </template>
  </b-modal>

  <AddVariableModal ref="addVariableModal" :selected-instance="selectedInstance" @variable-added="loadSelectedInstanceVariables(); $refs.success.show()"></AddVariableModal>

  <DeleteVariableModal ref="deleteVariableModal"></DeleteVariableModal>

  <SuccessAlert top="0" style="z-index: 1031" ref="success">{{ $t('alert.successOperation') }}</SuccessAlert>

</template>

<script>
import procesessVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import { ProcessService, HistoryService } from '@/services.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import TaskPopper from '@/components/common-components/TaskPopper.vue'
import AddVariableModal from '@/components/process/modals/AddVariableModal.vue'
import DeleteVariableModal from '@/components/process/modals/DeleteVariableModal.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'VariablesTable',
  components: { FlowTable, TaskPopper, AddVariableModal, DeleteVariableModal, SuccessAlert, BWaitingBox },
  mixins: [procesessVariablesMixin],
  data: function() {
    return {
      filteredVariables: [],
      variableToModify: null,
      fileObjects: ['de.cib.cibflow.api.files.FileValueDataFlowSource', 'de.cib.cibflow.api.files.FileValueDataSource']
    }
  },

  computed: {
    formattedJsonValue: {
      get: function() {
        if (this.variableToModify) {
          if (this.variableToModify.type === 'Json') {
            return JSON.stringify(JSON.parse(this.variableToModify.value), null, 2)
          } else return this.variableToModify.value
        }
        return ''
      },
      set: function(val) {
        this.variableToModify.value = val
      }
    }
  },

  methods: {

    isFileValueDataSource: function(item) {
      if (item.type === 'Object') {
        if (item.value && item.value.objectTypeName) {
          if (this.fileObjects.includes(item.value.objectTypeName)) return true
        }
      }
      return false
    },
    displayObjectNameValue: function(item) {
      if (this.isFileValueDataSource(item)) {
        return item.value.name
      }
      return item.value
    },
    isFile: function(item) {
      if (item.type === 'File') return true
      else return this.isFileValueDataSource(item)
    },

    modifyVariable: function(variable) {
      this.selectedVariable = variable
      this.variableToModify = JSON.parse(JSON.stringify(variable))
      this.$refs.modifyVariable.show()
    },
    deleteVariable: function(variable) {
      this.$refs.deleteVariableModal.show({
        ok: this.deleteVariableConfirmed,
        variable: variable
      })
    },
    deleteVariableConfirmed: function(variable) {
      if (this.selectedInstance.state === 'ACTIVE') {
        ProcessService.deleteVariableByExecutionId(variable.executionId, variable.name).then(() => {
          this.loadSelectedInstanceVariables()
          this.$refs.success.show()
        })
      } else {
        HistoryService.deleteVariableHistoryInstance(variable.id).then(() => {
          this.loadSelectedInstanceVariables()
          this.$refs.success.show()
        })
      }
    },
    updateVariable: function() {
      var data = { modifications: {} }
      if (this.variableToModify.type === 'Json') {
        this.variableToModify.value = JSON.stringify(JSON.parse(this.variableToModify.value))
      }
      data.modifications[this.variableToModify.name] = { value: this.variableToModify.value, type: this.variableToModify.type }
      ProcessService.modifyVariableByExecutionId(this.variableToModify.executionId, data).then(() => {
        this.selectedVariable.value = this.variableToModify.value
        this.$refs.modifyVariable.hide()
      })
    },
  }
}
</script>
