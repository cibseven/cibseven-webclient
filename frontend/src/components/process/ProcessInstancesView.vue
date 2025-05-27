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
      <BpmnViewer ref="diagram" @activity-id="$emit('activity-id', $event)" @task-selected="selectTask($event)" @activity-map-ready="activityMap = $event"
        :process-definition-id="process.id" :activity-id="activityId" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory" :statistics="process.statistics"
        :activities-history="process.activitiesHistory" class="h-100">
      </BpmnViewer>
    </div>

    <ul class="nav nav-tabs position-absolute border-0 bg-light" style="left: -1px" :style="'top: ' + (bottomContentPosition - toggleButtonHeight) + 'px; ' + toggleTransition">
      <span role="button" size="sm" variant="light" class="border-bottom-0 bg-white rounded-top border py-1 px-2 me-1" @click="toggleContent">
        <span class="mdi mdi-18px" :class="toggleIcon"></span>
      </span>
      <component :is="ProcessInstancesTabsPlugin" v-if="ProcessInstancesTabsPlugin" @change-tab="changeTab($event)"></component>
      <ProcessInstancesTabs v-else @change-tab="changeTab($event)"></ProcessInstancesTabs>
    </ul>

    <div class="position-absolute w-100" style="left: 0; bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">
      <div v-if="activeTab === 'instances'">
        <div ref="filterTable" class="bg-light d-flex position-absolute w-100">
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
            <span v-if="activityId" class="badge bg-info rounded-pill p-2 pe-3" style="font-weight: 500; font-size: 0.75rem">
              <span @click="$emit('activity-id', '')" role="button" class="mdi mdi-close-thick py-2 px-1"></span> {{ activityId }}
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
        <div ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 60px; left: 0; bottom: 0" @scroll="handleScrollProcesses">
          <InstancesTable ref="instancesTable" v-if="!loading && instances.length > 0 && !sorting"
            :instances="instances"
            :sortByDefaultKey="sortByDefaultKey"
            :sortDesc="sortDesc"
            @instance-deleted="$emit('instance-deleted')"
          ></InstancesTable>
          <div v-else-if="loading" class="py-3 text-center w-100">
            <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
          </div>
          <div v-else>
            <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
          </div>
        </div>
      </div>
      <div v-if="activeTab !== 'instances'" ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 0px; left: 0; bottom: 0">
        <IncidentsTable v-if="activeTab === 'incidents' && !loading"
          :incidents="incidents" :activity-instance="activityInstance"
          :activity-instance-history="process.activitiesHistory"/>
        <JobDefinitionsTable v-else-if="activeTab === 'jobDefinitions'"
          :process-id="process.id" @highlight-activity="highlightActivity" />
        <CalledProcessDefinitionsTable v-else-if="activeTab === 'calledProcessDefinitions' && !loading"
          :process="process" :instances="instances" :calledProcesses="calledProcesses" @changeTabToInstances="changeTab({ id: 'instances' })"/>
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
    <MultisortModal ref="sortModal" :items="instances" :sortKeys="['state', 'businessKey', 'startTimeOriginal', 'endTimeOriginal', 'id', 'startUserId', 'incidents']" :prefix="'process.'" @apply-sorting="applySorting"></MultisortModal>
  </div>
</template>

<script>
import appConfig from '@/appConfig.js'
import { ProcessService } from '@/services.js'
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

export default {
  name: 'ProcessInstancesView',
  components: { InstancesTable, JobDefinitionsTable, BpmnViewer, MultisortModal,
     SuccessAlert, ConfirmDialog, BWaitingBox, IncidentsTable, CalledProcessDefinitionsTable, ProcessInstancesTabs },
  inject: ['loadProcesses'],
  mixins: [permissionsMixin, resizerMixin, copyToClipboardMixin],
  props: { instances: Array, process: Object, firstResult: Number, maxResults: Number, incidents: Array,
    activityInstance: Object, activityInstanceHistory: Array, activityId: String, loading: Boolean,
    processKey: String, calledProcesses: Array,
    versionIndex: { type: String, default: '' }
  },
  data: function() {
    return {
      selectedInstance: null,
      selectedTask: null,
      topBarHeight: 0,
      activeTab: 'instances',
      events: {},
      usages: [],
      sortByDefaultKey: 'startTimeOriginal',
      sorting: false,
      sortDesc: true
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
      window.location.href = appConfig.servicesBasePath + '/process/' + this.process.id + '/data?filename=' + filename +
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
        this.instances.forEach(instance => {
          if (instance.state === 'ACTIVE') instance.state = 'SUSPENDED'
        })
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
    onInput: debounce(800, function(filter) { this.$emit('filter-instances', filter) }),
    getJobDefinitions: function() {
      this.$store.dispatch('jobDefinition/getJobDefinitions', {
        processDefinitionId: this.process.id
      })
    }
  }
}
</script>
