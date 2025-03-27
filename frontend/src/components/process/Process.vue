<template>
  <div v-if="process" class="h-100">
    <div @mousedown="handleMouseDown" class="v-resizable position-absolute w-100" style="left: 0" :style="'height: ' + bpmnViewerHeight + 'px; ' + toggleTransition">
      <BpmnViewer ref="diagram" @activity-id="$emit('activity-id', $event)" @task-selected="selectTask($event)" @open-subprocess="$emit('open-subprocess', $event)" @activity-map-ready="activityMap = $event"
        :process-definition-id="process.id" :activity-id="activityId" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory" :statistics="process.statistics"
        :activities-history="process.activitiesHistory" class="h-100">
      </BpmnViewer>
    </div>

    <ul class="nav nav-tabs position-absolute border-0 bg-light" style="left: -1px" :style="'top: ' + (bottomContentPosition - toggleButtonHeight) + 'px; ' + toggleTransition">
      <span role="button" size="sm" variant="light" class="border-bottom-0 bg-white rounded-top border py-1 px-2 me-1" @click="toggleContent">
        <span class="mdi mdi-18px" :class="toggleIcon"></span>
      </span>
      <li class="nav-item m-0" v-for="(tab, index) in tabs" :key="index">
        <a role="button" @click="changeTab(tab)" class="nav-link py-2" :class="{ 'active': tab.active, 'bg-light border border-bottom-0': !tab.active }">
          {{ $t('process.' + tab.id) }}
        </a>
      </li>
    </ul>

    <div class="position-absolute w-100" style="left: 0; bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">
      <div v-if="activeTab === 'instances'">
        <div ref="filterTable" class="bg-light d-flex position-absolute w-100">
          <div class="col-3 p-3">
            <b-input-group size="sm">
              <template #prepend>
                <b-button :title="$t('searches.search')" aria-hidden="true" size="sm" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
              </template>
              <b-form-input :title="$t('searches.search')" size="sm" :placeholder="$t('searches.search')" @input="onInput"></b-form-input>
              <b-button size="sm" variant="light" @click="$refs.sortModal.show()" class="ms-1 border"><span class="mdi mdi-sort" style="line-height: initial"></span></b-button>
            </b-input-group>
          </div>
          <div class="col-1 p-3">
            <span v-if="activityId" class="badge bg-info rounded-pill p-2 pe-3" style="font-weight: 500; font-size: 0.75rem">
              <span @click="$emit('activity-id', '')" role="button" class="mdi mdi-close-thick py-2 px-1"></span> {{ activityId }}
            </span>
          </div>
          <div class="col-8 p-3 text-end">
            <div>
              <b-button v-if="process.suspended === 'false'" class="border" size="sm" variant="light" @click="showConfirm({ ok: suspendProcess })" :title="$t('process.suspendProcess')">
                <span class="mdi mdi-pause-circle-outline"></span> {{ $t('process.suspendProcess') }}
              </b-button>
              <b-button v-else class="border" size="sm" variant="light" @click="showConfirm({ ok: activateProcess })" :title="$t('process.activateProcess')">
                <span class="mdi mdi-play-circle-outline"></span> {{ $t('process.activateProcess') }}
              </b-button>
              <b-button class="border" size="sm" variant="light" @click="downloadBpmn()" :title="$t('process.downloadBpmn')">
                <span class="mdi mdi-download"></span> {{ $t('process.downloadBpmn') }}
              </b-button>
              <b-button class="border" size="sm" variant="light" @click="viewDeployment()" :title="$t('process.showDeployment')">
                <span class="mdi mdi-file-eye-outline"></span> {{ $t('process.showDeployment') }}
              </b-button>
              <component :is="ProcessActions" v-if="ProcessActions" :process="process"></component>
            </div>
          </div>
        </div>
        <div ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 60px; left: 0; bottom: 0" @scroll="handleScrollProcesses">
          <InstancesTable ref="instancesTable" v-if="!loading && instances && !sorting" :instances="instances" :sortByDefaultKey="sortByDefaultKey" :sortDesc="sortDesc"
            @select-instance="selectInstance" @view-process="viewProcess" @instance-deleted="$emit('instance-deleted')"
          ></InstancesTable>
          <div v-else-if="loading" class="py-3 text-center w-100">
            <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
          </div>
        </div>
      </div>
      <div v-if="activeTab === 'incidents' || activeTab === 'jobDefinitions'" 
          ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 0px; left: 0; bottom: 0">
        <IncidentsTable v-if="activeTab === 'incidents' && !loading"
          :incidents="incidents" :activity-instance="activityInstance"
          :activity-instance-history="process.activitiesHistory" :get-failing-activity="getFailingActivity" />
        <JobDefinitionsTable v-else-if="activeTab === 'jobDefinitions'"
          :processId="process.id" :activityMap="activityMap" @highlight-activity="highlightActivity" />
      </div>
      <!--
      <div v-if="activeTab === 'statistics'">
        <div ref="rContent" class="overflow-auto container-fluid bg-white position-absolute" style="top: 60px; left: 0; bottom: 0" @scroll="handleScrollProcesses">
          <FlowTable v-if="usages.length" striped thead-class="sticky-header" :items="usages" primary-key="id" prefix="process."
            sort-by="startTimeOriginal" :sort-desc="true" :fields="[
            { label: 'event', key: 'event', class: 'col-4', tdClass: 'justify-content-center py-0 border-end border-top-0' },
            { label: 'date', key: 'date', class: 'col-2', tdClass: 'border-end py-1 border-top-0' },
            { label: 'productCode', key: 'productCode', class: 'col-4', tdClass: 'border-end py-1 border-top-0' },
            { label: 'usageCount', key: 'usageCount', class: 'col-2', tdClass: 'border-end py-1 border-top-0' }]"
            @click="selectInstance($event)">
          </FlowTable>
          <div class="py-3 text-center w-100" v-if="loading">
            <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
          </div>
        </div>
      </div>
      -->
    </div>
    <ConfirmDialog ref="confirm" @ok="$event.ok($event.instance)">
      {{ $t('confirm.performOperation') }}
    </ConfirmDialog>
    <SuccessAlert ref="messageCopy"> {{ $t('process.copySuccess') }} </SuccessAlert>
    <SuccessAlert top="0" style="z-index: 1031" ref="success"> {{ $t('alert.successOperation') }}</SuccessAlert>
    <MultisortModal ref="sortModal" :items="instances" :sortKeys="['state', 'businessKey', 'startTimeOriginal', 'endTimeOriginal', 'id', 'startUserId', 'incidents']" :prefix="'process.'" @apply-sorting="applySorting"></MultisortModal>
  </div>
</template>

<script>
import appConfig from '@/appConfig.js'
import { ProcessService } from '@/services.js'
import { permissionsMixin } from '@/permissions.js'
import BpmnViewer from '@/components/process/BpmnViewer.vue'
import InstancesTable from '@/components/process/InstancesTable.vue'
import JobDefinitionsTable from '@/components/process/tables/JobDefinitionsTable.vue'
import IncidentsTable from '@/components/process/tables/IncidentsTable.vue'
import MultisortModal from '@/components/process/MultisortModal.vue'
// import FlowTable from '@/components/common-components/FlowTable.vue'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { debounce } from '@/utils/debounce.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'Process',
  components: { InstancesTable, JobDefinitionsTable, BpmnViewer, MultisortModal,
     //FlowTable,
     SuccessAlert, ConfirmDialog, BWaitingBox, IncidentsTable },
  inject: ['loadProcesses'],
  mixins: [permissionsMixin, resizerMixin, copyToClipboardMixin],
  props: { instances: Array, process: Object, firstResult: Number, maxResults: Number, incidents: Array,
    activityInstance: Object, activityInstanceHistory: Array, activityId: String, loading: Boolean,
    processKey: String,
    versionIndex: { type: String, default: '' }
 },
  data: function() {
    return {
      selectedInstance: null,
      selectedTask: null,
      topBarHeight: 0,
      tabs: [
        { id: 'instances', active: true },
        { id: 'jobDefinitions', active: false },
        { id: 'incidents', active: false }
      ],
      activeTab: 'instances',
      events: {},
      usages: [],
      sortByDefaultKey: 'startTimeOriginal',
      sorting: false,
      sortDesc: true,
      activityMap: {}
    }
  },
  watch: {
    'process.id': function() {
      ProcessService.fetchDiagram(this.process.id).then(response => {
        this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
      }),      
      this.getJobDefinitions()
    }
  },
  mounted: function() {
    ProcessService.fetchDiagram(this.process.id).then(response => {
      setTimeout(() => {
        this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
      }, 100)
    }),
    this.getJobDefinitions()
  },
  computed: {
    ProcessActions: function() {
      return this.$options.components && this.$options.components.ProcessActions
        ? this.$options.components.ProcessActions
        : null
    },
    processName: function() {
      return this.process.name !== null ? this.process.name : this.process.key
    }
  },
  methods: {
    applySorting: function(sortedItems) {
      this.sorting = true
      this.sortDesc = null
      this.sortByDefaultKey = ''
      this.$emit('update-items', sortedItems)
      this.$nextTick(() => {
        this.sorting = false
        this.sortDesc = true
      })
    },
    changeTab: function(selectedTab) {
      this.tabs.forEach((tab) => {
        tab.active = tab.id === selectedTab.id
      })
      this.activeTab = selectedTab.id
    },
    selectInstance: function(event) {
      if (!this.selectedInstance) this.$refs.diagram.setEvents()
      this.selectedInstance = event.instance
      this.$emit('instance-selected', { selectedInstance: event.instance, reload: event.reload })
      this.$refs.diagram.cleanDiagramState()
    },
    selectTask: function(event) {
      this.selectedTask = event
      this.$emit('task-selected', event);
    },
    viewProcess: function() {
      this.$refs.diagram.clearEvents()
      this.$refs.diagram.cleanDiagramState()
      ProcessService.fetchDiagram(this.process.id).then(response => {
        this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
        this.$refs.diagramModal.show()
      })
    },
    viewDeployment: function() {
      this.$router.push('/seven/auth/deployments/' + this.process.deploymentId)
    },
    downloadBpmn: function() {
      var filename = this.process.resource.substr(this.process.resource.lastIndexOf('/') + 1, this.process.resource.lenght)
      window.location.href = appConfig.servicesBasePath + '/process/' + this.process.id + '/data?filename=' + filename +
        '&token=' + this.$root.user.authToken
    },
    clearState: function() {
      this.selectedInstance = null
      this.selectedTask = null
      this.$emit('instance-selected', { selectedInstance: null })
      this.$emit('task-selected', null)
      this.$refs.diagram.clearEvents()
      this.$refs.diagram.cleanDiagramState()
    },
    showConfirm: function(type) { this.$refs.confirm.show(type) },
    suspendProcess: function() {
      ProcessService.suspendProcess(this.process.id, true, true).then(() => {
        this.$store.dispatch('setSuspended', { process: this.process, suspended: 'true' })
        this.instances.forEach(instance => {
          if (instance.state === 'ACTIVE') instance.state = 'SUSPENDED'
        })
        this.$refs.success.show()
      })
    },
    activateProcess: function() {
      ProcessService.suspendProcess(this.process.id, false, true).then(() => {
        this.$store.dispatch('setSuspended', { process: this.process, suspended: 'false' })
        this.instances.forEach(instance => {
          if (instance.state === 'SUSPENDED') instance.state = 'ACTIVE'
        })
        this.$refs.success.show()
      })
    },
    highlightActivity: function(jobDefinition) {
      this.$refs.diagram.highlightElement(jobDefinition)
    },
    handleScrollProcesses: function(el) {
      if (this.instances.length < this.firstResult) return
      if (Math.ceil(el.target.scrollTop + el.target.clientHeight) >= el.target.scrollHeight) {
        this.$emit('show-more')
      }
    },
    onInput: debounce(800, function(evt) { this.$emit('filter-instances', evt) }),
    getFailingActivity: function(activityId) {
      let element = this.$refs.diagram.viewer.get('elementRegistry').get(activityId)
      if (element) return element.businessObject.name
      return ''
    },    
    getJobDefinitions: function() {
      this.$store.dispatch('jobDefinition/getJobDefinitions', {
        processDefinitionId: this.process.id
      })
    },
  }
}
</script>
