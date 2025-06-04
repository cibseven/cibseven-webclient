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
    <router-link
      class="btn btn-light rounded-0 border-bottom text-start w-100 align-middle d-flex align-items-center"
      :to="{
        path: `/seven/auth/process/${process.key}/${process.version}`,
        query: $route.query
      }">
      <span class="mdi mdi-18px mdi-arrow-left me-2 float-start"></span>
      <h5 class="m-0">
        {{ $t('process-instance.processInstanceId') }}: {{ selectedInstance.id }}
        <span v-if="selectedInstance.businessKey"> | {{ $t('process-instance.businessKey') }}: {{ selectedInstance.businessKey }}</span>
      </h5>
    </router-link>

    <div @mousedown="handleMouseDown" class="v-resizable position-absolute w-100" style="left: 0" :style="'height: ' + bpmnViewerHeight + 'px; ' + toggleTransition">
      <BpmnViewer @child-activity="filterByChildActivity($event)" @task-selected="selectTask($event)" :activityId="activityId" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory" :statistics="process.statistics"
        :process-definition-id="process.id" ref="diagram" class="h-100" :activities-history="process.activitiesHistory"></BpmnViewer>
      <span role="button" size="sm" variant="light" class="bg-white px-2 py-1 me-1 position-absolute border rounded" style="bottom: 15px; left: 15px;" @click="toggleContent">
        <span class="mdi mdi-18px" :class="toggleIcon"></span>
      </span>
    </div>

    <div class="position-absolute w-100 bg-light border-bottom" style="z-index: 1" :style="'top: ' + (bottomContentPosition - tabsAreaHeight) + 'px; ' + toggleTransition">
      <div class="d-flex align-items-end">
        <div class="tabs-scroll-container flex-grow-1" style="white-space: nowrap;">
          <ul class="nav nav-tabs m-0 border-0 flex-nowrap" style="display: inline-flex; overflow-y: hidden">
            <component :is="ProcessInstanceTabsPlugin" v-if="ProcessInstanceTabsPlugin" @change-tab="changeTab($event)"></component>
            <ProcessInstanceTabs v-else @change-tab="changeTab($event)"></ProcessInstanceTabs>
          </ul>
        </div>
      </div>
    </div>

    <div ref="rContent" class="position-absolute w-100 overflow-hidden" style="bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">

      <VariablesTable v-if="activeTab === 'variables'" :selected-instance="selectedInstance" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory"></VariablesTable>
      <IncidentsTable v-else-if="activeTab === 'incidents'" :incidents="selectedInstance.incidents" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory"></IncidentsTable>
      <UserTasksTable v-else-if="activeTab === 'usertasks'" :selected-instance="selectedInstance"></UserTasksTable>
      <JobsTable v-else-if="activeTab === 'jobs'" :jobs="selectedInstance.jobs"></JobsTable>
      <CalledProcessInstancesTable v-else-if="activeTab === 'calledProcessInstances'" :selectedInstance="selectedInstance" :activityInstanceHistory="activityInstanceHistory" :activity-instance="activityInstance"></CalledProcessInstancesTable>
      <component :is="ProcessInstanceTabsContentPlugin" v-if="ProcessInstanceTabsContentPlugin" :instance="selectedInstance" :active-tab="activeTab" :process="process"></component>
    </div>

  </div>
</template>

<script>
import { ProcessService } from '@/services.js'

import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import procesessVariablesMixin from '@/components/process/mixins/processesVariablesMixin.js'

import VariablesTable from '@/components/process/tables/VariablesTable.vue'
import IncidentsTable from '@/components/process/tables/IncidentsTable.vue'
import UserTasksTable from '@/components/process/tables/UserTasksTable.vue'
import JobsTable from '@/components/process/tables/JobsTable.vue'
import CalledProcessInstancesTable from '@/components/process/tables/CalledProcessInstancesTable.vue'
import ProcessInstanceTabs from '@/components/process/ProcessInstanceTabs.vue'

import BpmnViewer from '@/components/process/BpmnViewer.vue'

export default {
  name: 'ProcessInstanceView',
  components: { VariablesTable, IncidentsTable, UserTasksTable, BpmnViewer, 
    JobsTable, CalledProcessInstancesTable, ProcessInstanceTabs },
  mixins: [procesessVariablesMixin, resizerMixin],
  props: {
    selectedInstance: Object,
    activityInstance: Object,
    activityInstanceHistory: Object
  },
  data: function() {
    return {
      filterHeight: 0,
      activityId: '',
      activeTab: 'variables'
    }
  },
  watch: {
    'process.id': function() {
      ProcessService.fetchDiagram(this.process.id).then(response => {
        this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
      })
    }
  },
  computed: {
    ProcessInstanceTabsPlugin() {
      return this.$options.components && this.$options.components.ProcessInstanceTabsPlugin
        ? this.$options.components.ProcessInstanceTabsPlugin
        : null
    },
    ProcessInstanceTabsContentPlugin: function() {
      return this.$options.components && this.$options.components.ProcessInstanceTabsContentPlugin
        ? this.$options.components.ProcessInstanceTabsContentPlugin
        : null
    }
  },
  mounted: function() {
    ProcessService.fetchDiagram(this.process.id).then(response => {
      this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
    })
  },
  methods: {
    changeTab: function(selectedTab) {
      this.activeTab = selectedTab.id
    },
    selectTask: function(event) {
      this.selectedTask = event
      this.$emit('task-selected', event);
    },
    filterByChildActivity: function(event) {
      if (event) {
        this.activityId = event.activityId
        this.filteredVariables = this.variables.filter(obj => obj.activityInstanceId === event.id)
      } else {
        this.activityId = ''
        this.filteredVariables = this.variables
      }
    }
  }
}
</script>
