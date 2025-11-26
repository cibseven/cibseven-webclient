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
  <div v-if="selectedInstance" class="h-100">
    <ol class="breadcrumb m-0 d-flex align-items-center w-100 ps-3" style="min-height: 40px; line-height: 20px;">
      <!-- Parent process link if exists -->
      <li v-if="parentProcess" class="breadcrumb-item">
        <span class="d-flex align-items-center">
          <router-link 
            :to="{
              path: `/seven/auth/process/${parentProcess.key}/${parentProcess.version}`,
              query: {
                ...Object.fromEntries(
                  Object.entries($route.query).filter(([key]) => key !== 'parentProcessDefinitionId')
                ),
                tab: 'instances'
              }
            }"
            class="text-decoration-none d-flex align-items-center fw-bold text-info">
            {{ parentProcess.name || parentProcess.key }}
          </router-link>
          <span v-if="superProcessInstance" class="pe-1">:</span>
          <router-link v-if="superProcessInstance"
            :to="{
              name: 'process-instance-id',
              params: {
                instanceId: superProcessInstance.id
              },
              query: { tab: 'variables' }
            }"
            class="text-decoration-none d-flex align-items-center fw-bold text-info">
            {{ superProcessInstance.id }}
          </router-link>
        </span>
      </li>
      <!-- Current process link -->
      <li class="breadcrumb-item d-flex align-items-center">
        <router-link 
          :to="{
            path: `/seven/auth/process/${process.key}/${process.version}`,
            query: { ...Object.fromEntries(
              Object.entries($route.query).filter(([key]) => !['tab'].includes(key))
            ), tab: 'instances' }
          }"
          class="text-decoration-none d-flex align-items-center fw-bold text-info">
          {{ process.name || process.key }}
        </router-link>
        <div class="ps-1 fw-bold">
          | {{ $t('process-instance.processInstanceId') }}: {{ selectedInstance.id }}
          <span v-if="selectedInstance.businessKey"> | {{ $t('process-instance.businessKey') }}: {{ selectedInstance.businessKey }}</span>
        </div>
      </li>
    </ol>

    <div @mousedown="handleMouseDown" class="v-resizable position-absolute w-100" style="left: 0" :style="'height: ' + bpmnViewerHeight + 'px; ' + toggleTransition">
      <component :is="BpmnViewerPlugin" v-if="BpmnViewerPlugin" ref="diagram" class="h-100"
        @child-activity="filterByChildActivity($event)" @task-selected="selectTask($event)" @activity-map-ready="activityMap = $event"
        :activityId="selectedActivityId" :activity-instance="activityInstance" :process-definition-id="process.id" :selected-instance="selectedInstance" :activity-instance-history="activityInstanceHistory" 
        :statistics="process.statistics" :active-tab="activeTab" >
      </component>
      <BpmnViewer v-else ref="diagram"
        :process-definition-id="process.id"
        :activity-instance="activityInstance"
        :activity-instance-history="activityInstanceHistory" 
        :statistics="process.statistics"
        :selected-instance="selectedInstance"
        :activityId="selectedActivityId" 
        @task-selected="selectTask($event)"
        @child-activity="filterByChildActivity($event)"
        class="h-100">
      </BpmnViewer>
      <span role="button" size="sm" variant="light" class="bg-white px-2 py-1 me-1 position-absolute border rounded" style="bottom: 90px; right: 11px;" @click="toggleContent">
        <span class="mdi mdi-18px" :class="toggleIcon"></span>
      </span>
    </div>

    <div class="position-absolute w-100 border-bottom" style="z-index: 2; left: 0;" :style="'height: '+ tabsAreaHeight +'px; top: ' + (bottomContentPosition - tabsAreaHeight) + 'px; ' + toggleTransition">
      <div class="d-flex align-items-end">
        <ScrollableTabsContainer :tabs-area-height="tabsAreaHeight" :active-tab="activeTab" :container-style="{ whiteSpace: 'nowrap' }">
          <component :is="ProcessInstanceTabsPlugin" v-if="ProcessInstanceTabsPlugin" v-model="activeTab"></component>
          <ProcessInstanceTabs v-else v-model="activeTab"></ProcessInstanceTabs>
        </ScrollableTabsContainer>
      </div>
    </div>

    <div ref="rContent" class="position-absolute w-100 overflow-hidden" style="left: 0; bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">

      <VariablesTable v-if="activeTab === 'variables'" :selected-instance="selectedInstance" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory"></VariablesTable>
      <IncidentsTable v-else-if="activeTab === 'incidents'" :scrollable-area="$refs.rContent" :instance="selectedInstance" :process="process" :activity-instance="activityInstance" 
        :is-instance-view="true" :activity-instance-history="activityInstanceHistory" :tenant-id="tenantId"></IncidentsTable>
      <UserTasksTable v-else-if="activeTab === 'usertasks'" :selected-instance="selectedInstance"></UserTasksTable>
      <JobsTable v-else-if="activeTab === 'jobs'" :instance="selectedInstance" :process="process"></JobsTable>
      <CalledProcessInstancesTable v-else-if="activeTab === 'calledProcessInstances'" :selected-instance="selectedInstance" :activity-instance-history="activityInstanceHistory"></CalledProcessInstancesTable>
      <ExternalTasksTable v-else-if="activeTab === 'externalTasks'" :instance="selectedInstance"></ExternalTasksTable>
      <component :is="ProcessInstanceTabsContentPlugin" v-if="ProcessInstanceTabsContentPlugin" :instance="selectedInstance" :active-tab="activeTab" :process="process"></component>
    </div>

  </div>
</template>

<script>
import { ProcessService, HistoryService } from '@/services.js'
import { mapActions } from 'vuex'

import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import tabUrlMixin from '@/components/process/mixins/tabUrlMixin.js'

import VariablesTable from '@/components/process/tables/VariablesTable.vue'
import IncidentsTable from '@/components/process/tables/IncidentsTable.vue'
import UserTasksTable from '@/components/process/tables/UserTasksTable.vue'
import JobsTable from '@/components/process/tables/JobsTable.vue'
import CalledProcessInstancesTable from '@/components/process/tables/CalledProcessInstancesTable.vue'
import ExternalTasksTable from '@/components/process/tables/ExternalTasksTable.vue'
import ProcessInstanceTabs from '@/components/process/ProcessInstanceTabs.vue'
import ScrollableTabsContainer from '@/components/common-components/ScrollableTabsContainer.vue'

import BpmnViewer from '@/components/process/BpmnViewer.vue'
import { mapGetters } from 'vuex'

export default {
  name: 'ProcessInstanceView',
  components: { VariablesTable, IncidentsTable, UserTasksTable, BpmnViewer, 
    JobsTable, CalledProcessInstancesTable, ExternalTasksTable, ProcessInstanceTabs, ScrollableTabsContainer },
  mixins: [resizerMixin, tabUrlMixin],
  props: {
    process: Object,
    tenantId: String,
    selectedInstance: Object,
    activityInstance: Object,
    activityInstanceHistory: Object
  },
  emits: ['task-selected'],
  data: function() {
    return {
      filterHeight: 0,
      defaultTab: 'variables',
      parentProcess: null,
      superProcessInstance: null
    }
  },
  watch: {
    'process.id': function() {
      ProcessService.fetchDiagram(this.process.id).then(response => {
        this.$refs.diagram.showDiagram(response.bpmn20Xml, this.selectedActivityId)
      })
    },
    'selectedInstance.superProcessInstanceId': function(newVal) {
      if (newVal) {
        this.loadSuperProcessInstance(newVal)
      } else {
        this.superProcessInstance = null
        this.parentProcess = null
      }
    }
  },
  computed: {
    ...mapGetters(['selectedActivityId']),
    ProcessInstanceTabsPlugin() {
      return this.$options.components && this.$options.components.ProcessInstanceTabsPlugin
        ? this.$options.components.ProcessInstanceTabsPlugin
        : null
    },
    ProcessInstanceTabsContentPlugin: function() {
      return this.$options.components && this.$options.components.ProcessInstanceTabsContentPlugin
        ? this.$options.components.ProcessInstanceTabsContentPlugin
        : null
    },
    BpmnViewerPlugin: function() {
      return this.$options.components && this.$options.components.BpmnViewerPlugin
        ? this.$options.components.BpmnViewerPlugin
        : null
    },
  },
  mounted: function() {
    ProcessService.fetchDiagram(this.process.id).then(response => {
      this.$refs.diagram.showDiagram(response.bpmn20Xml, this.selectedActivityId)
    })
    // Load super process instance if available
    if (this.selectedInstance?.superProcessInstanceId) {
      this.loadSuperProcessInstance(this.selectedInstance.superProcessInstanceId)
    }
    if (this.selectedInstance) {
      this.clearHistoricActivityStatistics()
      const params = { processInstanceIdIn: this.selectedInstance.id }
      this.loadHistoricActivityStatistics({ processDefinitionId: this.process.id, params })
    }
  },
  methods: {
    ...mapActions(['clearActivitySelection', 'setHighlightedElement', 'selectActivity', 'loadHistoricActivityStatistics', 'clearHistoricActivityStatistics']),
    selectTask: function(event) {
      this.$emit('task-selected', event);
    },
    filterByChildActivity: function(event) {
      const activityId = event?.id || ''
      if (activityId) {
        this.selectActivity({ activityId: activityId })
        this.setHighlightedElement(activityId)
      }
      else {
        this.clearActivitySelection()
        this.setHighlightedElement('')
      }
    },
    loadSuperProcessInstance: async function(superProcessInstanceId) {
      try {
        // Fetch the super process instance data
        this.superProcessInstance = await HistoryService.findProcessInstance(superProcessInstanceId)
        
        // Use the process definition data from the super process instance
        if (this.superProcessInstance.processDefinitionKey) {
          this.parentProcess = {
            key: this.superProcessInstance.processDefinitionKey,
            name: this.superProcessInstance.processDefinitionName || this.superProcessInstance.processDefinitionKey,
            version: this.superProcessInstance.processDefinitionVersion,
            id: this.superProcessInstance.processDefinitionId,
            tenantId: this.superProcessInstance.tenantId
          }
        }
      } catch (error) {
        console.error('Failed to load super process instance:', error)
        this.superProcessInstance = null
        this.parentProcess = null
      }
    }
  }
}
</script>
