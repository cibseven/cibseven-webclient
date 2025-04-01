<template>
  <div v-if="selectedInstance" class="h-100">
    <router-link
      class="btn btn-light rounded-0 border-bottom text-start w-100 align-middle d-flex align-items-center"
      :to="'/seven/auth/process/' + process.key + '/' + process.version">
      <span class="mdi mdi-18px mdi-arrow-left me-2 float-start"></span>
      <h5 class="m-0">
        {{ $t('process-instance.processInstanceId') }}: {{ selectedInstance.id }}
        <span v-if="selectedInstance.businessKey"> | {{ $t('process-instance.businessKey') }}: {{ selectedInstance.businessKey }}</span>
      </h5>
    </router-link>

    <div @mousedown="handleMouseDown" class="v-resizable position-absolute w-100" style="left: 0" :style="'height: ' + bpmnViewerHeight + 'px; ' + toggleTransition">
      <BpmnViewer @child-activity="filterByChildActivity($event)" @task-selected="selectTask($event)" :activityId="activityId" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory" :statistics="process.statistics"
        :process-definition-id="process.id" ref="diagram" class="h-100" :activities-history="process.activitiesHistory"></BpmnViewer>
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

      <VariablesTable v-if="activeTab === 'variables'" :selected-instance="selectedInstance" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory"></VariablesTable>
      <IncidentsTable v-else-if="activeTab === 'incidents'" :incidents="selectedInstance.incidents" :activity-instance="activityInstance" :activity-instance-history="activityInstanceHistory"></IncidentsTable>
      <UserTasksTable v-else-if="activeTab === 'usertasks'" :selected-instance="selectedInstance"></UserTasksTable>
      <JobsTable v-else-if="activeTab === 'jobs'" :jobs="selectedInstance.jobs"></JobsTable>

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

import BpmnViewer from '@/components/process/BpmnViewer.vue'

export default {
  name: 'ProcessInstanceView',
  components: { VariablesTable, IncidentsTable, UserTasksTable, BpmnViewer, JobsTable },
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
      tabs: [
        { id: 'variables', active: true },
        { id: 'incidents', active: false },
        { id: 'usertasks', active: false },
        { id: 'jobs', active: false }
      ],
      activeTab: 'variables'
    }
  },
  mounted: function() {
    ProcessService.fetchDiagram(this.process.id).then(response => {
      this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
    })
  },
  methods: {
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
