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
  <div class="d-flex flex-column h-100">
    <div v-if="isActiveInstance || ProcessVariablesSearchBoxPlugin" class="bg-white d-flex w-100">
      <div v-if="ProcessVariablesSearchBoxPlugin" :class="isActiveInstance ? 'col-10 p-2' : 'col-12 p-2'">
        <component :is="ProcessVariablesSearchBoxPlugin"
          :query="filter"
          @change-query-object="changeFilter"
          :total-count="filteredVariables.length"
        ></component>
      </div>
      <div v-if="isActiveInstance" :class="ProcessVariablesSearchBoxPlugin ? 'col-2 p-3' : 'p-3'">
        <b-button class="border" size="sm" variant="light" @click="$refs.addVariableModal.show()" :title="$t('process-instance.addVariable')">
          <span class="mdi mdi-plus"></span> {{ $t('process-instance.addVariable') }}
        </b-button>
      </div>
    </div>
    <div class="overflow-y-scroll bg-white container-fluid g-0 flex-grow-1">
      <FlowTable v-if="!loading" striped resizable thead-class="sticky-header" :items="filteredVariables" primary-key="id" prefix="process-instance.variables."
        sort-by="name" :fields="[
        { label: 'name', key: 'name', class: 'col-3', tdClass: 'py-1' },
        { label: 'type', key: 'type', class: 'col-2', tdClass: 'py-1' },
        { label: 'value', key: 'value', class: 'col-3', tdClass: 'py-1' },
        { label: 'scope', key: 'scope', class: 'col-2', tdClass: 'py-1' },
        { label: 'actions', key: 'actions', class: 'col-2', sortable: false, tdClass: 'py-1' }]">
        <template v-slot:cell(name)="table">
          <div :title="table.item.name" class="text-truncate">{{ table.item.name }}</div>
        </template>
        <template v-slot:cell(type)="table">
          <div :title="table.item.type" class="text-truncate">{{ table.item.type }}</div>
        </template>
        <template v-slot:cell(value)="table">
          <CopyableActionButton
            :displayValue="displayValue(table.item)"
            :clickable="isFile(table.item)"
            :title="displayValueTooltip(table.item)"
            @click="downloadFile(table.item)"
            @copy="copyValueToClipboard"
          />
        </template>
        <template v-slot:cell(actions)="table">
          <b-button v-if="isFile(table.item)" :title="displayValueTooltip(table.item)"
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
          <b-button :title="$t('confirm.delete')" size="sm" variant="outline-secondary"
            class="border-0 mdi mdi-18px mdi-delete-outline" @click="deleteVariable(table.item)"></b-button>
        </template>
      </FlowTable>
      <div v-else>
        <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
      </div>
    </div>

    <AddVariableModal ref="addVariableModal" :selected-instance="selectedInstance" @variable-added="loadSelectedInstanceVariables(); $refs.success.show()"></AddVariableModal>
    <DeleteVariableModal ref="deleteVariableModal"></DeleteVariableModal>
    <EditVariableModal ref="editVariableModal" :disabled="!isActiveInstance" @variable-updated="loadSelectedInstanceVariables(); $refs.success.show()" @instance-status-updated="updateInstanceStatus"></EditVariableModal>
    <SuccessAlert top="0" ref="success" style="z-index: 9999">{{ $t('alert.successOperation') }}</SuccessAlert>
    <SuccessAlert ref="messageCopy" style="z-index: 9999"> {{ $t('process.copySuccess') }} </SuccessAlert>
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
  </div>

</template>

<script>
import { BWaitingBox } from 'cib-common-components'
import FlowTable from '@/components/common-components/FlowTable.vue'
import TaskPopper from '@/components/common-components/TaskPopper.vue'
import { ProcessService, HistoryService } from '@/services.js'
import DeleteVariableModal from '@/components/process/modals/DeleteVariableModal.vue'
import AddVariableModal from '@/components/process/modals/AddVariableModal.vue'
import EditVariableModal from '@/components/process/modals/EditVariableModal.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import processesVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import CopyableActionButton from '@/components/common-components/CopyableActionButton.vue'
import { mapGetters } from 'vuex'

export default {
  name: 'VariablesTable',
  components: { FlowTable, TaskPopper, AddVariableModal, DeleteVariableModal, EditVariableModal, SuccessAlert, BWaitingBox, CopyableActionButton },
  mixins: [processesVariablesMixin, copyToClipboardMixin],
  data: function() {
    return {
      filteredVariables: [],
      fileObjects: ['de.cib.cibflow.api.files.FileValueDataFlowSource', 'de.cib.cibflow.api.files.FileValueDataSource']
    }
  },
  computed: {
    ...mapGetters('variableInstance', ['currentVariableInstance']),
    ...mapGetters('historicVariableInstance', ['currentHistoricVariableInstance']),
    ProcessVariablesSearchBoxPlugin: function() {
      return this.$options.components && this.$options.components.ProcessVariablesSearchBoxPlugin
        ? this.$options.components.ProcessVariablesSearchBoxPlugin
        : null
    },
    isActiveInstance: function() {
      const activeStates = ['ACTIVE', 'SUSPENDED']
      return this.selectedInstance && activeStates.includes(this.selectedInstance.state)
    },
  },
  methods: {
    updateInstanceStatus() {
      this.selectedInstance.state = 'COMPLETED'
    },
    displayValue(item) {
      if (this.isFileValueDataSource(item)) {
        return this.getFileVariableName(item)
      }
      else if (item.type === 'File') {
        return item.valueInfo.filename
      }
      else if (item.type === 'Json') {

        if (typeof item.valueSerialized === 'string') {
          return item.valueSerialized
        }

        if (typeof item.value === 'object') {
          try {
            return JSON.stringify(item.value, null, 2)
          } catch {
            return '- Json Object -'
          }
        }
        return '- Json Object -'
      }
      else if (item.type === 'Object') {

        if (typeof item.valueDeserialized === 'object') {
          return JSON.stringify(item.valueDeserialized, null, 2)
        }

        if (typeof item.value === 'object') {
          try {
            return JSON.stringify(item.value, null, 2)
          } catch {
            return '- Object -'
          }
        }
        else if (typeof item.value === 'string') {
          return item.value
        }
        return '- Object -'
      }
      else if (item.type === 'Null') {
        return ''
      }
      else {
        return '' + item.value
      }
    },
    displayValueTooltip(item) {
      if (this.isFile(item)) {
        return this.$t('process-instance.download') + ': ' + this.displayValue(item)
      }
      else {
        return this.displayValue(item)
      }
    },
    isFile: function(item) {
      if (item.type === 'File') return true
      else return this.isFileValueDataSource(item)
    },
    async modifyVariable(variable) {
      this.$refs.editVariableModal.show(variable.id)
    },
    async deleteVariable(variable) {
      this.$refs.deleteVariableModal.show({
        ok: async () => {
          // Try active, fallback to historic if error
          if (this.selectedInstance.state === 'ACTIVE') {
            try {
              await ProcessService.deleteVariableByExecutionId(variable.executionId, variable.name)
              this.loadSelectedInstanceVariables()
              this.$refs.success.show()
            } catch {
              // Fallback to historic deletion
              await HistoryService.deleteVariableHistoryInstance(variable.id)
              this.selectedInstance.state = 'COMPLETED'
              this.loadSelectedInstanceVariables()
              this.$refs.success.show()
            }
          } else {
            await HistoryService.deleteVariableHistoryInstance(variable.id)
            this.loadSelectedInstanceVariables()
            this.$refs.success.show()
          }
        },
        variable: variable
      })
    }
  }
}
</script>
