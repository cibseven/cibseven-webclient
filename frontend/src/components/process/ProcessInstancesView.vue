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
  <div v-if="process" class="h-100">
    <div @mousedown="handleMouseDown" class="v-resizable position-absolute w-100" style="left: 0" :style="'height: ' + bpmnViewerHeight + 'px; ' + toggleTransition">
      <component :is="BpmnViewerPlugin" v-if="BpmnViewerPlugin" ref="diagram" @task-selected="selectTask($event)" @activity-map-ready="activityMap = $event"
        :process-definition-id="process.id" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory" :statistics="process.statistics"
        :activities-history="process.activitiesHistory" :active-tab="activeTab" class="h-100">
      </component>
      <BpmnViewer v-else ref="diagram" @task-selected="selectTask($event)" @activity-map-ready="activityMap = $event"
        :process-definition-id="process.id" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory" :statistics="process.statistics"
        :activities-history="process.activitiesHistory" :active-tab="activeTab" class="h-100">
      </BpmnViewer>
      <span role="button" size="sm" variant="light" class="bg-white px-2 py-1 me-1 position-absolute border rounded" style="bottom: 15px; left: 15px;" @click="toggleContent">
        <span class="mdi mdi-18px" :class="toggleIcon"></span>
      </span>
    </div>

    <div class="position-absolute w-100 bg-light border-bottom" style="left: 0; z-index: 2" :style="'top: ' + (bottomContentPosition - tabsAreaHeight) + 'px; ' + toggleTransition">
      <div class="d-flex align-items-end">
        <div class="tabs-scroll-container flex-grow-1" style="white-space: nowrap;">
          <ul class="nav nav-tabs m-0 border-0 flex-nowrap" style="display: inline-flex; overflow-y: hidden">
            <component :is="ProcessInstancesTabsPlugin" v-if="ProcessInstancesTabsPlugin" v-model="activeTab" />
            <ProcessInstancesTabs v-else v-model="activeTab" />
          </ul>
        </div>
      </div>
    </div>

    <div class="position-absolute w-100 overflow-hidden" style="left: 0; bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">
      <div v-if="isInstancesView" ref="filterTable" class="bg-light d-flex position-absolute w-100">
        <div class="col-3 p-3">
          <b-input-group size="sm">
            <template #prepend>
              <b-button :title="$t('searches.search')" aria-hidden="true" size="sm" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </template>
            <b-form-input :title="$t('searches.search')" size="sm" :placeholder="$t('searches.search')" @input="(evt) => onInput(evt.target.value.trim())"></b-form-input>
            <b-button size="sm" variant="light" @click="$refs.sortModal.show()" class="ms-1 border"><span class="mdi mdi-sort" style="line-height: initial"></span></b-button>
          </b-input-group>
        </div>
        <div class="col-1 p-3">
          <span v-if="selectedActivityId" class="badge bg-info rounded-pill p-2 pe-3" style="font-weight: 500; font-size: 0.75rem">
            <span @click="clearActivitySelection" role="button" class="mdi mdi-close-thick py-2 px-1"></span> {{ selectedActivityId }}
          </span>
        </div>
        <div class="col-8 p-3 text-end">
          <div>
            <b-button v-if="process.suspended === 'false'" class="border" size="sm" variant="light" @click="confirmSuspend" :title="$t('process.suspendProcess')">
              <span class="mdi mdi-pause-circle-outline"></span> {{ $t('process.suspendProcess') }}
            </b-button>
            <b-button v-else class="border" size="sm" variant="light" @click="confirmActivate" :title="$t('process.activateProcess')">
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
      <div ref="rContent" class="overflow-auto bg-white position-absolute w-100" :style="isInstancesView ? 'top: 60px' : 'top: 0px'" style="left: 0; bottom: 0" @scroll="handleScroll">
        <InstancesTable v-if="isInstancesView" ref="instancesTable" 
          :process="process"
          :activity-instance="activityInstance"
          :sortByDefaultKey="sortByDefaultKey"
          :sortDesc="sortDesc"
          :sorting="sorting"
          :tenant-id="tenantId"
          :filter="filter"
          @instance-deleted="$emit('instance-deleted')"
          @filter-instances="$emit('filter-instances', $event)"
        ></InstancesTable>
        <IncidentsTable v-else-if="activeTab === 'incidents'"
          :process="process" :activity-instance="activityInstance"
          :activity-instance-history="process.activitiesHistory"/>
        <JobDefinitionsTable v-else-if="activeTab === 'jobDefinitions'"
          :process-id="process.id" />
        <CalledProcessDefinitionsTable v-else-if="activeTab === 'calledProcessDefinitions'" :process="process" :activities-history="process.activitiesHistory"/>
        <component :is="ProcessInstancesTabsContentPlugin" v-if="ProcessInstancesTabsContentPlugin" :process="process" :active-tab="activeTab"></component>
      </div>
    </div>

    <ConfirmDialog ref="confirmActivate" @ok="activateProcess" :ok-title="$t('process-instance.jobDefinitions.activate')">
      <p>{{ $t('process.confirm.activate') }}</p>
      <p>{{ $t('process.name') }}: <strong>{{ process?.name }}</strong><br>
        {{ $t('process-instance.details.version') }}: <strong>{{ process?.version }}</strong>
      </p>
    </ConfirmDialog>
    <ConfirmDialog ref="confirmSuspend" @ok="suspendProcess" :ok-title="$t('process-instance.jobDefinitions.suspend')">
      <p>{{ $t('process.confirm.suspend') }}</p>
      <p>{{ $t('process.name') }}: <strong>{{ process?.name }}</strong><br>
        {{ $t('process-instance.details.version') }}: <strong>{{ process?.version }}</strong>
      </p>
    </ConfirmDialog>

    <SuccessAlert ref="messageCopy"> {{ $t('process.copySuccess') }} </SuccessAlert>
    <SuccessAlert top="0" style="z-index: 1031" ref="success"> {{ $t('alert.successOperation') }}</SuccessAlert>
    <MultisortModal ref="sortModal" :sortKeys="['businessKey', 'startTime', 'endTime', 'instanceId']" :prefix="'process.'" @apply-sorting="applySorting"></MultisortModal>
  </div>
</template>

<script>
import { ProcessService, getServicesBasePath } from '@/services.js'
import { permissionsMixin } from '@/permissions.js'
import BpmnViewer from '@/components/process/BpmnViewer.vue'
import InstancesTable from '@/components/process/tables/InstancesTable.vue'
import JobDefinitionsTable from '@/components/process/tables/JobDefinitionsTable.vue'
import IncidentsTable from '@/components/process/tables/IncidentsTable.vue'
import MultisortModal from '@/components/process/modals/MultisortModal.vue'
import CalledProcessDefinitionsTable from '@/components/process/tables/CalledProcessDefinitionsTable.vue'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { debounce } from '@/utils/debounce.js'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import { BWaitingBox } from 'cib-common-components'
import ProcessInstancesTabs from '@/components/process/ProcessInstancesTabs.vue'
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'ProcessInstancesView',
  components: { InstancesTable, JobDefinitionsTable, BpmnViewer, MultisortModal,
     SuccessAlert, ConfirmDialog, BWaitingBox, IncidentsTable, CalledProcessDefinitionsTable, 
     ProcessInstancesTabs },
  inject: ['loadProcesses'],
  mixins: [permissionsMixin, resizerMixin, copyToClipboardMixin],
  props: { process: Object,
    activityInstance: Object, activityInstanceHistory: Array, loading: Boolean,
    processKey: String,
    versionIndex: { type: String, default: '' },
    tenantId: String,
    filter: String
  },
  data: function() {
    return {
      selectedInstance: null,
      selectedTask: null,
      topBarHeight: 0,
      activeTab: 'instances',
      events: {},
      usages: [],
      sortByDefaultKey: 'startTime',
      sorting: false,
      sortDesc: true
    }
  },
  watch: {
    'process.id': function() {
      //TODO: Refactor to fetch from store
      ProcessService.fetchDiagram(this.process.id).then(response => {
        this.$refs.diagram.showDiagram(response.bpmn20Xml)
        this.setDiagramXml(response.bpmn20Xml)
      }),
      this.clearActivitySelection()
      this.changeTab({ id: 'instances' })
    }
  },
  mounted: function() {
    ProcessService.fetchDiagram(this.process.id).then(response => {
      setTimeout(() => {
        this.$refs.diagram.showDiagram(response.bpmn20Xml)        
        this.setDiagramXml(response.bpmn20Xml)
      }, 100)
    })
  },
  computed: {
    ProcessActions: function() {
      return this.$options.components && this.$options.components.ProcessActions
        ? this.$options.components.ProcessActions
        : null
    },
    ProcessInstancesTabsContentPlugin: function() {
      return this.$options.components && this.$options.components.ProcessInstancesTabsContentPlugin
        ? this.$options.components.ProcessInstancesTabsContentPlugin
        : null
    },
    ProcessInstancesTabsPlugin: function() {
      return this.$options.components && this.$options.components.ProcessInstancesTabsPlugin
        ? this.$options.components.ProcessInstancesTabsPlugin
        : null
    },
    BpmnViewerPlugin: function() {
      return this.$options.components && this.$options.components.BpmnViewerPlugin
        ? this.$options.components.BpmnViewerPlugin
        : null
    },
    processName: function() {
      return this.process.name !== null ? this.process.name : this.process.key
    },
    isInstancesView: function() {
      return this.activeTab === 'instances'
    },
    ...mapGetters(['selectedActivityId']),
    ...mapGetters('instances', ['instances']),
  },
  methods: {    
    ...mapActions(['clearActivitySelection', 'setDiagramXml']),
    applySorting: function(sortingCriteria) {
      this.sorting = true
      this.sortDesc = null
      this.sortByDefaultKey = ''
      
      // Apply sorting via backend by reloading data with sorting criteria
      if (this.$refs.instancesTable) {
        this.$refs.instancesTable.applySorting(sortingCriteria)
      }
      
      this.$nextTick(() => {
        this.sorting = false
        this.sortDesc = true
      })
    },
    changeTab: function(selectedTab) {
      this.activeTab = selectedTab.id
    },
    selectTask: function(event) {
      this.selectedTask = event
      this.$emit('task-selected', event);
    },
    viewDeployment: function() {
      this.$router.push('/seven/auth/deployments/' + this.process.deploymentId)
    },
    downloadBpmn: function() {
      var filename = this.process.resource.substr(this.process.resource.lastIndexOf('/') + 1, this.process.resource.lenght)
      window.location.href = getServicesBasePath() + '/process/' + this.process.id + '/data?filename=' + filename +
        '&token=' + this.$root.user.authToken
    },
    refreshDiagram: function() {
      this.$refs.diagram.cleanDiagramState()
      this.$refs.diagram.drawDiagramState()
    },
    // "Suspend process definition" button
    confirmSuspend: function() {
      this.$refs.confirmSuspend.show()
    },
    suspendProcess: function() {
      ProcessService.suspendProcess(this.process.id, true, true).then(() => {
        this.$store.dispatch('setSuspended', { process: this.process, suspended: 'true' })
        // Refresh instances table to reflect the state change
        if (this.$refs.instancesTable) {
          this.$refs.instancesTable.loadInstances()
        }
        this.$refs.success.show()
      })
    },
    // "Activate process definition" button
    confirmActivate: function() {
      this.$refs.confirmActivate.show()
    },
    activateProcess: function() {
      ProcessService.suspendProcess(this.process.id, false, true).then(() => {
        this.$store.dispatch('setSuspended', { process: this.process, suspended: 'false' })
        // Refresh instances table to reflect the state change
        if (this.$refs.instancesTable) {
          this.$refs.instancesTable.loadInstances()
        }
        this.$refs.success.show()
      })
    },
    handleScroll: function(el) {
      if (this.isInstancesView) this.handleScrollProcesses(el)
    },
    handleScrollProcesses: function(el) {
      // Check if we're near the bottom and can load more data
      const scrollThreshold = 100 // Load more when within 100px of bottom
      const nearBottom = (el.target.scrollTop + el.target.clientHeight + scrollThreshold) >= el.target.scrollHeight
      
      if (nearBottom && this.$refs.instancesTable) {
        // Let InstancesTable handle the logic of whether to load more
        this.$refs.instancesTable.showMore()
      }
    },
    onInput: debounce(800, function(filter) { this.$emit('filter-instances', filter) })
  }
}
</script>
