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
    <div v-if="isActiveInstance || ProcessVariablesSearchBoxPlugin || selectedActivityId || selectedScopeInstanceId" class="bg-white d-flex w-100 flex-wrap">
      <div v-if="ProcessVariablesSearchBoxPlugin" :class="isActiveInstance ? 'col-10 p-2' : 'col-12 p-2'">
        <component :is="ProcessVariablesSearchBoxPlugin"
          :query="filter"
          @change-query-object="changeFilter"
          :total-count="filteredVariables.length"
        ></component>
      </div>
      <div v-if="isActiveInstance" :class="ProcessVariablesSearchBoxPlugin ? 'col-2 p-3' : 'p-3'">
        <b-button class="border" size="sm" variant="light" @click="addNewVariable" :title="$t('process-instance.addVariable')">
          <span class="mdi mdi-plus"></span> {{ $t('process-instance.addVariable') }}
        </b-button>
      </div>
      <div v-if="!ProcessVariablesSearchBoxPlugin && (selectedActivityId || selectedScopeInstanceId)" class="p-3">
        <RemovableBadge
          @on-remove="clearActivityFilter"
          :tooltip-remove="$t('process-instance.variables.activityIdBadge.remove')"
          :label="$t('process-instance.variables.activityIdBadge.title', { activityId: selectedScopeName })"
          :tooltip="$t('process-instance.variables.activityIdBadge.tooltip', { activityId: selectedScopeName })"
        />
      </div>
    </div>
    <div class="overflow-y-scroll bg-white container-fluid g-0 flex-grow-1">
      <FlowTable v-if="!loading" striped resizable thead-class="sticky-header" :items="filteredVariables" primary-key="id"
        native-layout
        useCase="instance-variables"
        :columns="['name', 'type', 'value', 'scope', 'actions']"
        sort-by="name"
        :column-definitions="[
          { label: 'process-instance.variables.name', key: 'name', tdClass: 'pb-0' },
          { label: 'process-instance.variables.type', key: 'type', tdClass: 'pb-0' },
          { label: 'process-instance.variables.value', key: 'value', tdClass: 'pb-0' },
          { label: 'process-instance.variables.scope', key: 'scope', tdClass: 'pb-0', groupSeparator: true },
          { label: 'process-instance.variables.activityInstanceId', key: 'activityInstanceId', tdClass: 'pb-0' },
          { label: 'process-instance.variables.actions', key: 'actions', groupSeparator: true, disableToggle: true, sortable: false, tdClass: 'py-0' },
        ]">

        <template v-slot:cell(name)="table">
          <CopyableActionButton
            :displayValue="table.item.name"
            :clickable="false"
            :title="$t('process-instance.variables.name') + ':\n' + table.item.name"
            @copy="copyValueToClipboard"
          />          
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

        <template v-slot:cell(scope)="table">
          <CopyableActionButton
            :displayValue="table.item.scope"
            :clickable="true"
            :title="$t('process-instance.variables.scope') + ':\n' + table.item.scope + '\n\n' + $t('process-instance.variables.activityInstanceId') + ':\n' + table.item.activityInstanceId"
            @click="highlightScope(table.item)"
            @copy="copyValueToClipboard"
          />          
        </template>

        <template v-slot:cell(activityInstanceId)="table">
          <CopyableActionButton
            :displayValue="table.item.activityInstanceId"
            :clickable="false"
            :title="$t('process-instance.variables.activityInstanceId') + ':\n' + table.item.activityInstanceId"
            @copy="copyValueToClipboard"
          />
        </template>

        <template v-slot:cell(actions)="table">
          <div class="d-flex">
            <component :is="VariablesTableActionsPlugin" v-if="VariablesTableActionsPlugin" :table-item="table.item" :selected-instance="selectedInstance" :file-objects="fileObjects"></component>
            <CellActionButton v-if="isFile(table.item)" :title="displayValueTooltip(table.item)"
              icon="mdi-download-outline"
              @click="downloadFile(table.item)">
            </CellActionButton>
            <CellActionButton v-if="isFile(table.item) && table.item.isLive" :title="$t('process-instance.upload')"
              icon="mdi-upload-outline"
              @click="selectedVariable = table.item; $refs.uploadFile.show()">
            </CellActionButton>
            <CellActionButton v-if="'File' !== table.item.type && !isFileValueDataSource(table.item)"
              :title="$t(table.item.isLive ? 'process-instance.edit' : 'process-instance.variables.historicVariable.tooltip')"
              :icon="table.item.isLive ? 'mdi-square-edit-outline' : 'mdi-eye-outline'"
              @click="modifyVariable(table.item)">
            </CellActionButton>
            <CellActionButton v-if="hasDeletionPermissionFor(table.item)" :title="$t('confirm.delete')"
              icon="mdi-delete-outline" @click="deleteVariable(table.item)"></CellActionButton>
          </div>
        </template>
      </FlowTable>
      <div v-else>
        <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
      </div>
    </div>

    <AddVariableModal ref="addVariableModal" :selected-instance="selectedInstance" @variable-added="loadSelectedInstanceVariables(); $refs.success.show()"></AddVariableModal>
    <DeleteVariableModal ref="deleteVariableModal" @variable-deleted="onVariableDeleted"></DeleteVariableModal>
    <EditVariableModal ref="editVariableModal" :historic="!isActiveInstance" @variable-updated="loadSelectedInstanceVariables(); $refs.success.show()"></EditVariableModal>
    <SuccessAlert top="0" ref="success" style="z-index: 9999">{{ $t('alert.successOperation') }}</SuccessAlert>
    <SuccessAlert top="0" ref="runtimeVariableDeleted" style="z-index: 9999">{{ $t('process-instance.variables.deleteStatus.runtime') }}</SuccessAlert>
    <SuccessAlert top="0" ref="historicVariableDeleted" style="z-index: 9999">{{ $t('process-instance.variables.deleteStatus.historic') }}</SuccessAlert>
    <SuccessAlert ref="messageCopy" style="z-index: 9999"> {{ $t('process.copySuccess') }} </SuccessAlert>
    <TaskPopper ref="importPopper"></TaskPopper>

    <b-modal ref="uploadFile" :title="$t('process-instance.upload')">
      <div>
        <b-form-file placeholder="" :browse-text="$t('process-instance.selectFile')" v-model="file"></b-form-file>
      </div>
      <template v-slot:modal-footer>
        <b-button @click="$refs.uploadFile.hide(); file = null" variant="light">{{ $t('confirm.cancel') }}</b-button>
        <b-button :disabled="!file" @click="uploadFile(); $refs.uploadFile.hide()" variant="primary">{{ $t('process-instance.upload') }}</b-button>
      </template>
    </b-modal>
  </div>

</template>

<script>
import { BWaitingBox, FlowTable, TaskPopper, SuccessAlert, CopyableActionButton } from '@cib/common-frontend'
import RemovableBadge from '@/components/common-components/RemovableBadge.vue'
import DeleteVariableModal from '@/components/process/modals/DeleteVariableModal.vue'
import AddVariableModal from '@/components/process/modals/AddVariableModal.vue'
import EditVariableModal from '@/components/process/modals/EditVariableModal.vue'
import processesVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import CellActionButton from '@/components/common-components/CellActionButton.vue'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { permissionsMixin } from '@/permissions.js'
import { mapGetters, mapActions } from 'vuex'
import variableUtils from '@/components/process/mixins/variableUtils'

export default {
  name: 'VariablesTable',
  components: { FlowTable, TaskPopper, AddVariableModal, DeleteVariableModal, EditVariableModal, SuccessAlert, BWaitingBox, CopyableActionButton, CellActionButton, RemovableBadge },
  mixins: [ processesVariablesMixin, copyToClipboardMixin, permissionsMixin ],
  data: function() {
    return {
      filteredVariables: [],
      fileObjects: variableUtils.getFileObjects(),
      selectedScopeInstanceId: null,
    }
  },
  watch: {
    variables() { this.applyActivityFilter() },
    'filter.activityInstanceIdIn'(newVal) {
      if (this.ProcessVariablesSearchBoxPlugin && !newVal) {
        this.clearActivitySelection()
        this.setHighlightedElement('')
      }
    },
    selectedActivityId() {
      this.selectedScopeInstanceId = null
      if (this.ProcessVariablesSearchBoxPlugin) {
        const newFilter = { ...this.filter }
        delete newFilter.activityInstanceIdIn
        if (this.selectedActivityId) {
          const instanceIds = Object.keys(this.activityInstanceIdToActivityId)
            .filter(id => this.activityInstanceIdToActivityId[id] === this.selectedActivityId)
          if (instanceIds.length > 0) newFilter.activityInstanceIdIn = instanceIds
        }
        this.filter = { ...newFilter, deserializeValues: false }
      }
      this.applyActivityFilter()
    },
  },
  computed: {
    ...mapGetters('variableInstance', ['currentVariableInstance']),
    ...mapGetters('historicVariableInstance', ['currentHistoricVariableInstance']),
    ...mapGetters(['selectedActivityId']),
    selectedScopeName() {
      if (this.selectedScopeInstanceId) {
        return this.activityInstancesGrouped[this.selectedScopeInstanceId] || this.selectedScopeInstanceId
      }
      if (this.selectedActivityId) {
        const instanceId = Object.keys(this.activityInstanceIdToActivityId)
          .find(id => this.activityInstanceIdToActivityId[id] === this.selectedActivityId)
        return instanceId ? this.activityInstancesGrouped[instanceId] : this.selectedActivityId
      }
      return ''
    },
    ProcessVariablesSearchBoxPlugin: function() {
      return this.$options.components && this.$options.components.ProcessVariablesSearchBoxPlugin
        ? this.$options.components.ProcessVariablesSearchBoxPlugin
        : null
    },
    VariablesTableActionsPlugin: function() {
      return this.$options.components && this.$options.components.VariablesTableActionsPlugin
        ? this.$options.components.VariablesTableActionsPlugin
        : null
    },
  },
  methods: {
    ...mapActions(['setHighlightedElement', 'selectActivity', 'clearActivitySelection']),
    applyActivityFilter() {
      if (this.selectedScopeInstanceId) {
        this.filteredVariables = this.variables.filter(v => v.activityInstanceId === this.selectedScopeInstanceId)
      } else if (this.filter.activityInstanceIdIn?.length) {
        const ids = this.filter.activityInstanceIdIn
        this.filteredVariables = this.variables.filter(v => ids.includes(v.activityInstanceId))
      } else if (this.selectedActivityId) {
        this.filteredVariables = this.variables.filter(v => v.scopeActivityId === this.selectedActivityId)
      } else {
        this.filteredVariables = [...this.variables]
      }
    },
    clearActivityFilter() {
      this.selectedScopeInstanceId = null
      this.clearActivitySelection()
      this.setHighlightedElement('')
      this.applyActivityFilter()
    },
    highlightScope(variable) {
      if (this.ProcessVariablesSearchBoxPlugin) {
        if (variable.scopeActivityId) this.setHighlightedElement(variable.scopeActivityId)
        this.changeFilter({ ...this.filter, activityInstanceIdIn: [variable.activityInstanceId] })
      } else {
        this.selectedScopeInstanceId = variable.activityInstanceId
        if (variable.scopeActivityId) {
          this.selectActivity({ activityId: variable.scopeActivityId })
          this.setHighlightedElement(variable.scopeActivityId)
        }
        this.applyActivityFilter()
      }
    },
    async addNewVariable() {
      this.$refs.addVariableModal.show()
    },
    hasDeletionPermissionFor(variable) {
      if (variable.isLive) {
        return this.processByPermissions(this.$root.config.permissions.deleteProcessInstance, this.selectedInstance)
      }
      else {
        return this.processByPermissions(this.$root.config.permissions.deleteHistoricProcessInstance, this.selectedInstance)
      }
    },
    async modifyVariable(variable) {
      this.$refs.editVariableModal.show(variable.id, variable.name, !variable.isLive)
    },
    async deleteVariable(variable) {
      this.$refs.deleteVariableModal.show(variable.isLive === true, variable)
    },
    onVariableDeleted(variable) {
      this.loadSelectedInstanceVariables()
      if (variable?.isLive) {
        this.$refs.runtimeVariableDeleted.show()
      }
      else {
        this.$refs.historicVariableDeleted.show()
      }
    },
  },  
	mounted() {
		if (!this.$route.query.q) {
			this.loadSelectedInstanceVariables()
		}
	}
}
</script>
