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
  <b-modal ref="assignationModal" :title="modalTitle" :ok-only="true">
    <div>
      <ul class="nav nav-tabs mb-4">
        <li class="nav-item" v-for="(identity, idx) in filteredIdentities" :key="idx">
          <button
            class="nav-link py-1"
            :class="{ active: selectedIdentity === identity }"
            :id="'identityTab-' + idx"
            data-bs-toggle="tab"
            :data-bs-target="'#tabContent-' + idx"
            type="button"
            @click="selectedIdentity = identity">
            {{ $t('process-instance.assignModal.' + identity.text) }}
          </button>
        </li>
      </ul>
      <div class="tab-content mt-3">
        <div
          v-for="(identity, idx) in filteredIdentities"
          :key="'tabContent-' + idx"
          class="tab-pane fade"
          :class="{ 'show active': selectedIdentity === identity }"
          :id="'tabContent-' + idx">
          <div v-if="hasIndentityLinks(identity)">
            <table class="table table-striped">
              <thead>
                <tr>
                  <th class="col-10 border-top-0">{{ $t('process-instance.assignModal.' + identity.value) }}</th>
                  <th class="border-top-0">{{ $t('process-instance.assignModal.actions') }}</th>
                </tr>
              </thead>
              <tbody>
                <template v-for="(identityLink, idx) in identityLinks" :key="identityLink[identity.value]">
                  <tr v-if="identityLink[identity.value] && identityLink.type === identity.type">
                    <td class="align-middle py-1">{{ identityLink[identity.value] }}</td>
                    <td class="text-center py-1">
                      <button
                        @click="removeIdentityLink(identityLink, idx)"
                        class="btn btn-outline-secondary border-0 mdi mdi-18px mdi-delete-outline"
                        :title="$t('process-instance.assignModal.delete')">
                      </button>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
          <p v-else class="text-center mt-4">{{ $t('process-instance.noResults') }}</p>
        </div>
      </div>

      <b-input-group>
        <b-form-input v-model="identity" @keyup.enter="addIdentity" autocomplete="off" />
        <b-input-group-append>
          <b-button @click="addIdentity" variant="primary" :disabled="!identity" :title="$t('process-instance.assignModal.add')">
            {{ $t('process-instance.assignModal.add') }}
          </b-button>
        </b-input-group-append>
      </b-input-group>
      <p v-if="identityExists" class="mt-1 text-danger">{{ $t('process-instance.assignModal.exists') }}</p>
    </div>
  </b-modal>
</template>

<script>
import { TaskService } from '@/services.js'

export default {
  name: 'TaskAssignationModal',
  emits: ['change-assignee'],
  data: function() {
    return {
      identityLinks: [],
      identities: [
        { value: 'userId', text: 'assignee', type: 'assignee' },
        { value: 'userId', text: 'users', type: 'candidate' },
        { value: 'groupId', text: 'groups', type: 'candidate' }
      ],
      selectedIdentity: {},
      identity: '',
      taskId: '',
      identityExists: false,
      showOnlySelected: false,
      customTitle: null
    }
  },
  computed: {
    filteredIdentities() {
      if (this.showOnlySelected) {
        return this.identities.filter(identity => identity.text === this.selectedIdentity.text)
      }
      return this.identities.filter(identity => identity.type === this.selectedIdentity.type)
    },
    modalTitle() {
      if (this.customTitle) {
        return this.customTitle
      }
      return this.selectedIdentity.type === 'assignee' 
        ? this.$t('process-instance.assignModal.manageAssignee') 
        : this.$t('process-instance.assignModal.manageUsersGroups')
    }
  },
  methods: {
    show: function(taskId, assignee, specificTab = null) {
      
      if (specificTab) {
        const foundIdentity = this.identities.find(identity => identity.text === specificTab)
        if (foundIdentity) {
          this.selectedIdentity = foundIdentity
          this.showOnlySelected = true
          // Set custom title based on the specific tab
          if (specificTab === 'groups') {
            this.customTitle = this.$t('process-instance.assignModal.manageGroups')
          } else if (specificTab === 'users') {
            this.customTitle = this.$t('process-instance.assignModal.manageUsers')
          }
        }
      } else {
        this.selectedIdentity = assignee ? this.identities[0] : this.identities[1]
        this.showOnlySelected = false
        this.customTitle = null
      }
      
      this.taskId = taskId
      this.identityLinks = []
      this.identityExists = false
      TaskService.findIdentityLinks(taskId).then(res => {
        this.identityLinks = res || []
      })
      this.$refs.assignationModal.show()
    },
    hasIndentityLinks: function(identity) {
      return this.identityLinks.some(obj => obj[identity.value] !== null &&
        obj[identity.value] !== undefined &&
        obj.type === identity.type)
    },
    addIdentity: function() {
      const newIdentityLink = { type: this.selectedIdentity.type }
      newIdentityLink[this.selectedIdentity.value] = this.identity

      this.identityExists = this.identityLinks.find(obj => {
        return obj[this.selectedIdentity.value] === newIdentityLink[this.selectedIdentity.value] &&
            obj.type === this.selectedIdentity.type
      })

      if (!this.identityExists) {
        TaskService.addIdentityLink(this.taskId, newIdentityLink).then(() => {
          this.identity = ''
          if (this.selectedIdentity.type !== 'assignee') this.identityLinks.push(newIdentityLink)
          else {
            const assigneeIdentity = this.identityLinks.find(ilink => ilink.type === 'assignee')
            assigneeIdentity ? assigneeIdentity.userId = newIdentityLink.userId : this.identityLinks.push(newIdentityLink)
            this.$emit('change-assignee', { taskId: this.taskId, assignee: newIdentityLink.userId })
          }
        })
      }
    },
    removeIdentityLink: function(identityLink, idx) {
      TaskService.removeIdentityLink(this.taskId, identityLink).then(() => {
        this.identityLinks.splice(idx, 1)
        if (this.selectedIdentity.type === 'assignee') this.$emit('change-assignee', { taskId: this.taskId, assignee: null })
      })
    }
  }
}
</script>
