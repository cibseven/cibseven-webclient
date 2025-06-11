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
  <div class="overflow-auto bg-white container-fluid g-0 h-100">
    <FlowTable v-if="!loading && userTasks.length > 0" resizable striped thead-class="sticky-header" :items="userTasks" primary-key="id" prefix="process-instance.usertasks."
      sort-by="label" :sort-desc="true" :fields="[
      { label: 'activity', key: 'name', class: 'col-2', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' },
      { label: 'assignee', key: 'assignee', class: 'col-1', thClass: 'border-end', tdClass: 'position-relative py-1 border-end border-top-0' },
      { label: 'owner', key: 'owner', class: 'col-1', thClass: 'border-end', tdClass: 'py-1 border-end border-top-0' },
      { label: 'startTime', key: 'created', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'due', key: 'due', class: 'col-2', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'followUp', key: 'followUp', class: 'col-1', thClass: 'border-end', tdClass: 'border-end py-1 border-top-0' },
      { label: 'taskID', key: 'id', class: 'col-2', thClass: 'border-end', tdClass: 'position-relative border-end py-1 border-top-0' },
      { label: 'actions', key: 'actions', class: 'col-1', sortable: false, tdClass: 'py-1 border-top-0' }]">
      <template v-slot:cell(assignee)="table">
        <div :title="table.item.assignee" class="text-truncate w-100" :class="focusedCell === table.item.assignee ? 'pe-4': ''" @mouseenter="focusedCell = table.item.assignee" @mouseleave="focusedCell = null">
          {{ table.item.assignee || '&nbsp;' }}
          <span v-if="focusedCell === table.item.assignee" @click.stop="$refs.taskAssignationModal.show(table.item.id, true)"
            class="mdi mdi-18px mdi-pencil-outline px-2 position-absolute end-0 text-secondary lh-sm"></span>
        </div>
      </template>
      <template v-slot:cell(created)="table">
        <span :title="formatDate(table.item.created)" class="text-truncate d-block">{{ formatDate(table.item.created) }}</span>
      </template>
      <template v-slot:cell(id)="table">
        <div :title="table.item.id" class="text-truncate w-100" :class="focusedCell === table.item.id ? 'pe-4': ''" @mouseenter="focusedCell = table.item.id" @mouseleave="focusedCell = null">
          {{ table.item.id }}
          <span v-if="table.item.id && focusedCell === table.item.id" @click.stop="copyValueToClipboard(table.item.id)"
            class="mdi mdi-18px mdi-content-copy px-2 position-absolute end-0 text-secondary lh-sm"></span>
        </div>
      </template>
      <template v-slot:cell(actions)="table">
        <b-button :title="$t('process-instance.assignModal.manageUsersGroups')" @click="$refs.taskAssignationModal.show(table.item.id, false)"
          size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-account"></b-button>
      </template>
    </FlowTable>
    <div v-else-if="loading">
      <p class="text-center p-4"><BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}</p>
    </div>
    <div v-else>
      <p class="text-center p-4">{{ $t('process-instance.noResults') }}</p>
    </div>

    <TaskAssignationModal ref="taskAssignationModal" @change-assignee="changeAssignee"></TaskAssignationModal>
    <SuccessAlert ref="messageCopy"> {{ $t('process.copySuccess') }} </SuccessAlert>
  </div>
</template>

<script>
import { TaskService } from '@/services.js'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'
import { formatDate } from '@/utils/dates.js'
import TaskAssignationModal from '@/components/process/modals/TaskAssignationModal.vue'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'UserTasksTable',
  components: { TaskAssignationModal, FlowTable, SuccessAlert, BWaitingBox },
  mixins: [copyToClipboardMixin],
  props: { selectedInstance: Object },
  data: function() {
    return {
      loading: true,
      userTasks: [],
      focusedCell: null
    }
  },
  mounted: function() {
    TaskService.findTasksPost({
      processInstanceId: this.selectedInstance.id,
      processDefinitionId: this.selectedInstance.processDefinitionId
    }).then(res => {
      this.loading = false
      this.userTasks = res
    })
  },
  methods: {
    formatDate,
    changeAssignee: function(event) {
      var userTask = this.userTasks.find(task => task.id === event.taskId)
      userTask.assignee = event.assignee
    }
  }
}
</script>
