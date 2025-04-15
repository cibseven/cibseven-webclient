<template>
  <div class="container-fluid bg-light pt-3 overflow-auto">
    <div class="row justify-content-around">
      <div class="col-12 col-md-5 p-3 my-3 bg-white border rounded shadow-sm">
        <h5>{{ $t('human-tasks.assignmentsByType') }}</h5>
        <hr>
        <FlowTable striped resizable thead-class="sticky-header" :items="countsByType"
          primary-key="id" prefix="human-tasks."
          sort-by="label" :fields="[
          { label: 'tasks', key: 'tasks', class: 'col-3', tdClass: 'py-1 border-top-0 fw-bold justify-content-center',
            thClass: 'd-flex justify-content-center', sortable: false },
          { label: 'types', key: 'types', class: 'col-9', tdClass: 'py-1 border-top-0', sortable: false }]">
          <template v-slot:cell(types)="table">
            <div :class="table.item.id === 3 ? 'fw-bold' : ''">{{ table.item.types }}</div>
          </template>
        </FlowTable>
      </div>
      <div class="col-12 col-md-5 p-3 my-3 bg-white border rounded shadow-sm">
        <h5>{{ $t('human-tasks.assignmentsByGroup') }} <span v-b-popover.hover.right="$t('human-tasks.assignmentsByGroupInfo')" class="mdi mdi-18px mdi-information-outline text-info"></span></h5>
        <hr>
        <FlowTable striped resizable thead-class="sticky-header" :items="taskCountByCandidateGroup"
          primary-key="id" prefix="human-tasks."
          sort-by="label" :fields="[
          { label: 'tasks', key: 'taskCount', class: 'col-3', tdClass: 'py-1 border-top-0 justify-content-center',
            thClass: 'd-flex justify-content-center', sortable: false },
          { label: 'groupName', key: 'groupName', class: 'col-9', tdClass: 'py-1 border-top-0', sortable: false }]">
          <template v-slot:cell(groupName)="table">
            <div>{{ table.item.groupName ? table.item.groupName : $t('human-tasks.noGroups') }}</div>
          </template>
        </FlowTable>
      </div>
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
        { tasks: 0, types: this.$t('human-tasks.user'), id: 0 },
        { tasks: 0, types: this.$t('human-tasks.group'), id: 1 },
        { tasks: 0, types: this.$t('human-tasks.unassigned'), id: 2 },
        { tasks: 0, types: this.$t('human-tasks.total'), id: 3 }
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
    TaskService.getTaskCountByCandidateGroup().then(data => {
      data.forEach((d, i) => d.id = i)
      this.taskCountByCandidateGroup = data
    })
  }
}
</script>
