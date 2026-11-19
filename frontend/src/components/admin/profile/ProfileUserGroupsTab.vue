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
  <div>
    <ContentBlock :title="$t('admin.users.group.title', [user.firstName + ' ' + user.lastName])">

      <template v-if="editMode" v-slot:actions>
        <b-button size="sm" variant="secondary" v-if="$root.config.userEditable" @click="openAssignGroupModal" class="mdi mdi-plus">
          <span class="ms-2">{{ $t('admin.users.group.add') }}</span>
        </b-button>
      </template>

      <div v-if="groups" class="p-2">
        <FlowTable striped :items="groups" primary-key="id" :fields="groupFields">
          <template v-slot:cell(actions)="row">
            <CellActionButton @click="unassignGroup(row.item)" :title="$t('admin.groups.deleteGroup')" icon="mdi-delete-outline"></CellActionButton>
          </template>
        </FlowTable>
      </div>
    </ContentBlock>

    <b-modal v-if="editMode" ref="assignGroupsModal" :title="$t('admin.users.group.add')" size="lg">
      <div class="container g-0">
        <FlowTable :items="unAssignedGroups" primary-key="id" striped
          :fields="[
            { label: '', key: 'selected', class: 'col-sm-1', sortable: false, thClass: 'text-center', tdClass: 'text-center' },
            { label: 'admin.groups.id', key: 'id', class: 'col-sm-3' },
            { label: 'admin.groups.name', key: 'name', class: 'col-sm-5' },
            { label: 'admin.groups.type', key: 'type', class: 'col-sm-3' },
          ]">
          <template v-slot:cell(selected)="row">
            <b-form-checkbox v-model="row.item.selected"></b-form-checkbox>
          </template>
        </FlowTable>
      </div>
      <div v-if="!unAssignedGroupsLoading && !unAssignedGroups.length" class="text-center">
        {{ $t('admin.noResults') }}
      </div>
      <div v-else-if="unAssignedGroupsLoading" class="d-flex justify-content-center align-items-center">
        <b-waiting-box class="d-inline me-2" styling="width: 35px"></b-waiting-box> {{ $t('admin.loading') }}
      </div>
      <template v-slot:modal-footer>
        <b-button @click="$refs.assignGroupsModal.hide()" variant="light">{{ $t('confirm.cancel') }}</b-button>
        <b-button @click="assignGroups(); $refs.assignGroupsModal.hide()" variant="primary">{{ $t('confirm.ok') }}</b-button>
      </template>
    </b-modal>

    <SuccessAlert ref="unassignGroup" top="0" style="z-index: 1031">{{ $t('admin.users.unassignGroupMessage', [user.id]) }}</SuccessAlert>
  </div>
</template>

<script>
import { AdminService } from '@/services.js'
import { FlowTable, SuccessAlert, ContentBlock } from '@cib/common-frontend'
import CellActionButton from '@/components/common-components/CellActionButton.vue'

export default {
  name: 'ProfileUserGroupsTab',
  components: { FlowTable, SuccessAlert, ContentBlock, CellActionButton },
  props: {
    user: { type: Object, required: true },
    editMode: { type: Boolean, default: false }
  },
  data: function() {
    return {
      groups: null,
      unAssignedGroups: [],
      unAssignedGroupsLoading: false
    }
  },
  computed: {
    groupFields() {
      const isEditable = this.editMode && this.$root.config.userEditable
      const fields = [
        {
          label: 'admin.groups.id',
          key: 'id',
          class: 'col-md-4 col-sm-4',
          ...(isEditable && { tdClass: 'py-1' })
        },
        {
          label: 'admin.groups.name',
          key: 'name',
          class: 'col-md-4 col-sm-4',
          ...(isEditable && { tdClass: 'py-1' })
        },
        {
          label: 'admin.groups.type',
          key: 'type',
          class: isEditable ? 'col-md-2 col-sm-2' : 'col-md-4 col-sm-4',
          ...(isEditable && { tdClass: 'py-1' })
        }
      ]
      if (isEditable) {
        fields.push({
          label: 'admin.groups.actions',
          key: 'actions',
          class: 'col-md-2 col-sm-2',
          sortable: false,
          thClass: 'justify-content-center text-center',
          tdClass: 'justify-content-center py-0'
        })
      }
      return fields
    }
  },
  created: function() {
    this.loadGroups(this.user.id)
  },
  methods: {
    loadGroups: function(userId) {
      AdminService.findGroups({ member: userId }).then(response => {
        this.groups = response
      })
    },
    unassignGroup: function(group) {
      AdminService.deleteMember(group.id, this.user.id).then(() => {
        this.$refs.unassignGroup.show(2)
        this.loadGroups(this.user.id)
      })
    },
    loadUnassignedGroups: function() {
      this.unAssignedGroupsLoading = true
      const userGroups = JSON.parse(JSON.stringify(this.groups))
      this.unAssignedGroups = []
      AdminService.findGroups().then(allGroups => {
        allGroups.forEach(group => {
          const isAssigned = userGroups.some(userGroup => userGroup.id === group.id)
          if (!isAssigned){
            group.selected = false
            this.unAssignedGroups.push(group)
          }
        })
        this.unAssignedGroupsLoading = false
      })
    },
    openAssignGroupModal: function() {
      this.loadUnassignedGroups()
      this.$refs.assignGroupsModal.show()
    },
    assignGroups: function () {
      this.unAssignedGroups.forEach(unAssignedGroup => {
        if (unAssignedGroup.selected) {
          AdminService.addMember(unAssignedGroup.id, this.user.id).then(() => {
            this.groups.push(unAssignedGroup)
          })
        }
      })
    }
  }
}
</script>
