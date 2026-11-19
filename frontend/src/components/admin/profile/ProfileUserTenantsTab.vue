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
    <ContentBlock :title="$t('admin.tenants.associationTitle', [user.firstName + ' ' + user.lastName])">

      <template v-if="editMode" v-slot:actions>
        <b-button size="sm" variant="secondary" v-if="$root.config.userEditable" @click="openAssignTenantModal" class="mdi mdi-plus">
          <span class="ms-2"> {{ $t('admin.tenants.addTo') }} </span>
        </b-button>
      </template>

      <div v-if="userTenants.length > 0" class="p-2">
        <FlowTable striped :items="userTenants" primary-key="id" :fields="tenantFields">
          <template v-slot:cell(actions)="row">
            <CellActionButton @click="unassignTenant(row.item)" :title="$t('admin.tenants.unassignTenant')" icon="mdi-delete-outline"></CellActionButton>
          </template>
        </FlowTable>
      </div>
      <span v-else class="d-block text-center p-4">
        {{ $t('admin.noResults') }}
      </span>
    </ContentBlock>

    <b-modal v-if="editMode" ref="assignTenantsModal" :title="$t('admin.tenants.addTo')" size="lg">
      <div v-if="unassignedTenants.length > 0" class="container g-0">
        <FlowTable :items="unassignedTenants" primary-key="id" striped
          :fields="[
            { label: '', key: 'selected', class: 'col-sm-1', sortable: false, tdClass: 'text-center' },
            { label: 'admin.tenants.fullId', key: 'id', class: 'col-6' },
            { label: 'admin.tenants.fullName', key: 'name', class: 'col-5' },
          ]">
          <template v-slot:cell(selected)="row">
            <b-form-checkbox v-model="row.item.selected"></b-form-checkbox>
          </template>
        </FlowTable>
      </div>
      <div v-else>
        {{ $t('admin.noResults') }}
      </div>
      <template v-slot:modal-footer>
        <b-button @click="$refs.assignTenantsModal.hide()" variant="light">{{ $t('confirm.cancel') }}</b-button>
        <b-button @click="assignTenants(); $refs.assignTenantsModal.hide()" variant="primary">{{ $t('confirm.ok') }}</b-button>
      </template>
    </b-modal>

    <SuccessAlert ref="unassignTenant" top="0" style="z-index: 1031">{{ $t('admin.tenants.unassignUserMessage', [user.id]) }}</SuccessAlert>
  </div>
</template>

<script>
import { FlowTable, SuccessAlert, ContentBlock } from '@cib/common-frontend'
import CellActionButton from '@/components/common-components/CellActionButton.vue'
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'ProfileUserTenantsTab',
  components: { FlowTable, SuccessAlert, ContentBlock, CellActionButton },
  props: {
    user: { type: Object, required: true },
    editMode: { type: Boolean, default: false }
  },
  data: function() {
    return {
      userTenants: [],
      unassignedTenants: []
    }
  },
  computed: {
    ...mapGetters(['tenants']),
    tenantFields() {
      const isEditable = this.editMode && this.$root.config.userEditable
      const fields = [
        {
          label: 'admin.tenants.fullId',
          key: 'id',
          class: isEditable ? 'col-md-4 col-sm-4' : 'col-md-6 col-sm-6',
          ...(isEditable && { tdClass: 'py-1' })
        },
        {
          label: 'admin.tenants.fullName',
          key: 'name',
          class: isEditable ? 'col-md-4 col-sm-4' : 'col-md-6 col-sm-6',
          ...(isEditable && { tdClass: 'py-1' })
        }
      ]
      if (isEditable) {
        fields.push({
          label: 'admin.tenants.actions',
          key: 'actions',
          class: 'col-md-4 col-sm-4',
          sortable: false,
          thClass: 'justify-content-center text-center',
          tdClass: 'justify-content-center py-0'
        })
      }
      return fields
    }
  },
  created: function() {
    this.loadTenants(this.user.id)
  },
  methods: {
    ...mapActions(['fetchTenants', 'getTenantsByUser', 'removeUserFromTenant', 'addUserToTenant']),
    async loadTenants(userId) {
      this.userTenants = await this.getTenantsByUser(userId)
    },
    openAssignTenantModal: function() {
      this.loadUnassignedTenants()
      this.$refs.assignTenantsModal.show()
    },
    async loadUnassignedTenants() {
      await this.fetchTenants()
      const userTenants = JSON.parse(JSON.stringify(this.userTenants))
      this.unassignedTenants = []
      this.tenants.forEach(tenant => {
        const isAssigned = userTenants.some(userTenant => userTenant.id === tenant.id)
        if (!isAssigned){
          tenant.selected = false
          this.unassignedTenants.push(tenant)
        }
      })
    },
    assignTenants() {
      this.unassignedTenants.forEach(tenant => {
        if (tenant.selected) {
          this.addUserToTenant({ tenantId: tenant.id, userId: this.user.id }).then(() => {
            this.userTenants.push(tenant)
          })
        }
      })
    },
    unassignTenant(tenant) {
      this.removeUserFromTenant({ tenantId: tenant.id, userId: this.user.id }).then(() => {
        this.$refs.unassignTenant.show(2)
        this.loadTenants(this.user.id)
      })
    }
  }
}
</script>
