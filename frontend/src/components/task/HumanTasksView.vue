<template>
  <div class="row">
    <div class="overflow-auto bg-white col-md-6 col-12">
      <FlowTable striped resizable thead-class="sticky-header" :items="countsByType" 
        primary-key="id" prefix="human-tasks."
        sort-by="label" :fields="[
        { label: 'tasks', key: 'tasks', class: 'col-6', tdClass: 'py-1 border-end border-top-0' },
        { label: 'types', key: 'types', class: 'col-6', tdClass: 'py-1 border-top-0' }]">
      </FlowTable>
    </div>
    <div class="overflow-auto bg-white col-md-6 col-12">
      <FlowTable striped resizable thead-class="sticky-header" :items="taskCountByCandidateGroup" 
        primary-key="id" prefix="human-tasks."
        sort-by="label" :fields="[
        { label: 'taskCount', key: 'taskCount', class: 'col-6', tdClass: 'py-1 border-end border-top-0' },
        { label: 'groupName', key: 'groupName', class: 'col-6', tdClass: 'py-1 border-top-0' }]">
      </FlowTable>
    </div>
  </div>
</template>

<script>

import { TaskService } from '@/services.js'
import FlowTable from '@/components/common-components/FlowTable.vue'

export default {
  name: 'HumanTasksView',
  components: { FlowTable },
  data: function() {
    return {
      countsByType: [
        { tasks: 0, types: this.$t('human-tasks.user') },
        { tasks: 0, types: '' },
        { tasks: 0, types: '' },
        { tasks: 0, types: '' }
      ],
      taskCountByCandidateGroup: []
    }
  },
  mounted() {
    TaskService.findHistoryTaksCount({ unfinished: true, assigned: true }).then(data => {
      this.countsByType[0].tasks = data
    })
    TaskService.findHistoryTaksCount({ unfinished: true, 
      unassigned: true, withCandidateGroups: true }).then(data => {
      this.countsByType[1].tasks = data
    })
    TaskService.findHistoryTaksCount({ unfinished: true, 
      unassigned: true, withoutCandidateGroups: true }).then(data => {
      this.countsByType[2].tasks = data
    })
    TaskService.findHistoryTaksCount({ unfinished: true }).then(data => {
      this.countsByType[3].tasks = data
    })
    TaskService.getTaskCountByCandidateGroup().then(data => this.taskCountByCandidateGroup = data)
  }
}
</script>
