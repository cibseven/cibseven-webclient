<template>
  <div v-if="selectedInstance" class="h-100">
    <b-button variant="light" style="min-height: 40px; line-height: 20px;" :block="true" class="rounded-0 border-bottom text-start" @click="$emit('clear-state'); $emit('unselect-instance')">
      <span class="mdi mdi-18px mdi-arrow-left me-2 float-start"></span>
      <h5 class="m-0">
        {{ $t('process-instance.processInstanceId') }}: {{ selectedInstance.id }}
        <span v-if="selectedInstance.businessKey"> | {{ $t('process-instance.businessKey') }}: {{ selectedInstance.businessKey }}</span>
      </h5>
    </b-button>
    <div @mousedown="handleMouseDown" class="v-resizable position-absolute w-100" style="left: 0" :style="'height: ' + bpmnViewerHeight + 'px; ' + toggleTransition">
      <BpmnViewer @child-activity="filterByChildActivity($event)" @task-selected="selectTask($event)" :activityId="activityId" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory" :statistics="process.statistics"
      @open-subprocess="$emit('open-subprocess', $event)" :process-definition-id="process.id" ref="diagram" class="h-100" :activities-history="process.activitiesHistory"></BpmnViewer>
    </div>

    <ul class="nav nav-tabs position-absolute border-0 bg-light" style="left: -1px" :style="'top: ' + (bottomContentPosition - toggleButtonHeight) + 'px; ' + toggleTransition">
      <span role="button" size="sm" variant="light" class="border-bottom-0 bg-white rounded-top border py-1 px-2 me-1" @click="toggleContent">
        <span class="mdi mdi-18px" :class="toggleIcon"></span>
      </span>
      <li class="nav-item m-0" v-for="tab in tabs" :key="tab.id">
        <a role="button" @click="changeTab(tab)" class="nav-link py-2" :class="{ 'active': tab.active, 'bg-light border border-bottom-0': !tab.active }">
          {{ $t('process.' + tab.id) }}
        </a>
      </li>
    </ul>

    <div ref="rContent" class="position-absolute w-100" style="bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">
      <div v-if="activeTab === 'variables'">
        <div v-if="selectedInstance.state === 'ACTIVE'" class="bg-light d-flex position-absolute w-100">
          <div class="py-2 px-2">
            <b-button class="border" size="sm" variant="light" @click="$refs.addVariableModal.show()" :title="$t('process-instance.addVariable')">
              <span class="mdi mdi-plus"></span> {{ $t('process-instance.addVariable') }}
            </b-button>
          </div>
        </div>
        <div class="overflow-auto bg-white position-absolute container-fluid g-0" style="bottom: 0" :style="('top: ' + (selectedInstance.state === 'ACTIVE' ? '45px' : '0'))">
          <FlowTable v-if="filteredVariables" striped resizable thead-class="sticky-header" :items="filteredVariables" primary-key="id" prefix="process-instance.variables."
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
                class="border-0 mdi mdi-18px mdi-delete-outline" @click="$refs.deleteVariable.show(table.item)"></b-button>
            </template>
          </FlowTable>
        </div>
      </div>
      <div v-if="activeTab === 'incidents'">
        <div class="overflow-auto bg-white position-absolute container-fluid g-0" style="top: 0; bottom: 0">
          <FlowTable v-if="selectedInstance.incidents.length > 0" striped thead-class="sticky-header" :items="selectedInstance.incidents" primary-key="id" prefix="process-instance.incidents."
            sort-by="label" :sort-desc="true" :fields="[
            { label: 'message', key: 'incidentMessage', class: 'col-3', tdClass: 'py-1 border-end border-top-0' },
            { label: 'timestamp', key: 'incidentTimestamp', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
            { label: 'activity', key: 'activityId', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
            { label: 'failedActivity', key: 'failedActivityId', class: 'col-2', tdClass: 'py-1 border-end border-top-0' },
            { label: 'incidentType', key: 'incidentType', class: 'col-2', tdClass: 'py-1 border-top-0' },
            { label: 'actions', key: 'actions', class: 'col-1', sortable: false, tdClass: 'py-1 border-top-0' }]">
            <template v-slot:cell(incidentMessage)="table">
              <div :title="table.item.incidentMessage" class="text-truncate" @click="showIncidentMessage(table.item.configuration)">{{ table.item.incidentMessage }}</div>
            </template>
            <template v-slot:cell(incidentTimestamp)="table">
              <div :title="table.item.incidentTimestamp" class="text-truncate">{{ showPrettyTimestamp(table.item.incidentTimestamp) }}</div>
            </template>
            <template v-slot:cell(activityId)="table">
              <div :title="table.item.activityId" class="text-truncate">{{ getActivityName(table.item.activityId) }}</div>
            </template>
            <template v-slot:cell(failedActivityId)="table">
              <div :title="table.item.failedActivityId" class="text-truncate">{{ getFailingActivity(table.item.failedActivityId) }}</div>
            </template>
            <template v-slot:cell(incidentType)="table">
              <div :title="table.item.incidentType" class="text-truncate">{{ table.item.incidentType }}</div>
            </template>
            <template v-slot:cell(actions)="table">
              <b-button :title="$t('process-instance.incidents.retryJob')"
                size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-reload"
                @click="$refs.confirmRetryJob.show(table.item.configuration)">
              </b-button>
            </template>
          </FlowTable>
          <div v-else>
            <p class="text-center p-4">{{ $t('process-instance.noIncidents') }}</p>
          </div>
        </div>
      </div>
      <div v-if="activeTab === 'usertasks'">
        <UserTasksTable :selectedInstance="selectedInstance"></UserTasksTable>
      </div>
    </div>

    <TaskPopper ref="importPopper"></TaskPopper>
    <b-modal ref="uploadFile" :title="$t('process-instance.upload')">
      <div class="container-fluid">
        <b-form-file placeholder="" :browse-text="$t('process-instance.selectFile')" v-model="file"></b-form-file>
      </div>
      <template v-slot:modal-footer>
        <b-button @click="$refs.uploadFile.hide(); file = null">{{ $t('confirm.cancel') }}</b-button>
        <b-button :disabled="!file" variant="primary" @click="uploadFile(); $refs.uploadFile.hide()">{{ $t('process-instance.upload') }}</b-button>
      </template>
    </b-modal>
    <b-modal ref="modifyVariable" :title="$t('process-instance.edit')">
      <div v-if="variableToModify" class="container-fluid">
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
          <b-button @click="$refs.modifyVariable.hide()">{{ $t('confirm.cancel') }}</b-button>
          <b-button variant="primary" @click="updateVariable">{{ $t('process-instance.save') }}</b-button>
        </template>
      </template>
    </b-modal>
    <b-modal ref="stackTraceModal" :title="$t('process-instance.stacktrace')" size="xl">
      <div v-if="stackTraceMessage" class="container-fluid pt-3">
        <b-form-textarea v-model="stackTraceMessage" rows="20" readonly></b-form-textarea>
        <b-button variant="link" @click="copyValueToClipboard(stackTraceMessage)">{{ $t('process-instance.copyValueToClipboard') }}</b-button>
      </div>
      <template v-slot:modal-footer>
        <b-button @click="$refs.stackTraceModal.hide()">{{ $t('confirm.close') }}</b-button>
      </template>
    </b-modal>
    <SuccessAlert ref="messageCopy" style="z-index: 9999">{{ $t('process.copySuccess') }}</SuccessAlert>
    <ConfirmDialog ref="confirmRetryJob" @ok="retryJob($event)">
      {{ $t('process-instance.confirmRetryJob') }}
    </ConfirmDialog>
    <AddVariableModal ref="addVariableModal" :selected-instance="selectedInstance" @variable-added="loadSelectedInstanceVariables(); $refs.success.show()"></AddVariableModal>
    <SuccessAlert ref="successRetryJob">{{ $t('process-instance.successRetryJob') }}</SuccessAlert>
    <ConfirmDialog ref="deleteVariable" @ok="deleteVariable($event)">
      {{ $t('confirm.performOperation') }}
    </ConfirmDialog>
    <SuccessAlert top="0" style="z-index: 1031" ref="success">{{ $t('alert.successOperation') }}</SuccessAlert>
  </div>
</template>

<script>
import moment from 'moment'
import { ProcessService, HistoryService, IncidentService } from '@/services.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import procesessVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'
import AddVariableModal from '@/components/process/AddVariableModal.vue'
import UserTasksTable from '@/components/process/UserTasksTable.vue'
import BpmnViewer from '@/components/process/BpmnViewer.vue'
import FlowTable from '@/components/common-components/FlowTable.vue'
import TaskPopper from '@/components/common-components/TaskPopper.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'

export default {
  name: 'ProcessVariablesTable',
  components: { AddVariableModal, UserTasksTable, BpmnViewer, FlowTable, TaskPopper, SuccessAlert, ConfirmDialog },
  mixins: [procesessVariablesMixin, resizerMixin, copyToClipboardMixin],
  data: function() {
    return {
      variableToModify: null,
      filterHeight: 0,
      filteredVariables: [],
      activityId: '',
      tabs: [
        { id: 'variables', active: true },
        { id: 'incidents', active: false },
        { id: 'usertasks', active: false }
      ],
      activeTab: 'variables',
      stackTraceMessage: '',
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
  mounted: function() {
    ProcessService.fetchDiagram(this.process.id).then(response => {
      this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
    })
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
    changeTab: function(selectedTab) {
      this.tabs.forEach((tab) => {
        tab.active = tab.id === selectedTab.id
      })
      this.activeTab = selectedTab.id
    },
    selectTask: function(event) {
      this.selectedTask = event
      this.$emit('task-selected', event);
    },
    getIconState: function(state) {
      switch(state) {
        case 'ACTIVE':
          return 'mdi-chevron-triple-right text-success'
        case 'SUSPENDED':
          return 'mdi-close-circle-outline'
      }
      return 'mdi-flag-triangle'
    },
    modifyVariable: function(variable) {
      this.selectedVariable = variable
      this.variableToModify = JSON.parse(JSON.stringify(variable))
      this.$refs.modifyVariable.show()
    },
    deleteVariable: function(variable) {
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
    filterByChildActivity: function(event) {
      if (event) {
        this.activityId = event.activityId
        this.filteredVariables = this.variables.filter(obj => obj.activityInstanceId === event.id)
      } else {
        this.activityId = ''
        this.filteredVariables = this.variables
      }
    },
    getActivityName: function(activityId) {
      var result = this.activityInstanceHistory.find(activity => {
        return activity.activityId === activityId
      })
      return result.activityName
    },
    getFailingActivity: function(activityId) {
      return this.$refs.diagram.viewer.get('elementRegistry').get(activityId).businessObject.name
    },
    showIncidentMessage: function(jobDefinitionId) {
      this.stackTraceMessage = ''
      IncidentService.fetchIncidentStacktraceByJobId(jobDefinitionId).then(res => {
        this.stackTraceMessage = res
        this.$refs.stackTraceModal.show()
      })
    },
    showPrettyTimestamp: function(orignalDate) {
      return moment(orignalDate).format('DD/MM/YYYY HH:mm:ss')
    },
    retryJob: function(jobDefinitionId) {
      IncidentService.retryJobById(jobDefinitionId).then(() => {
        this.selectedInstance.incidents.splice(this.selectedInstance.incidents.findIndex(obj => obj.configuration === jobDefinitionId), 1)
        this.$refs.successRetryJob.show()
      })
    }
  }
}
</script>
