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
  <SidebarsFlow ref="sidebars" header-margin="55px" v-model:left-open="leftOpen"
    :left-caption="$t('admin.groups.title') + ' - ' + group.id" :left-size="[12, 6, 4, 3, 2]">
    <template v-slot:left>
      <b-list-group>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'information'" exact :to="'?tab=information'">
          <span> {{ $t('admin.groups.information') }}</span>
        </b-list-group-item>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'users'" exact :to="'?tab=users'">
          <span> {{ $t('admin.groups.users') }}</span>
        </b-list-group-item>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'tenants'" exact :to="'?tab=tenants'">
          <span> {{ $t('admin.tenants.title') }}</span>
        </b-list-group-item>
      </b-list-group>
    </template>

    <transition name="slide-in" mode="out-in">
      <div v-if="group.id" class="d-flex flex-column h-100 bg-light">
        <b-button variant="light" style="min-height: 40px; line-height: 20px;" :block="true" class="rounded-0 border-bottom text-start" :to="{ name: 'adminGroups' }">
          <span class="mdi mdi-arrow-left me-2"></span>
          <span class="fw-semibold">{{ $t('admin.groups.title') }}</span>
        </b-button>
        <div class="container-fluid overflow-auto">
          <div v-if="$route.query.tab === 'information'" class="row">
            <div class="col-sm-12 col-md-12 col-lg-8 col-xl-6 p-4">
              <b-card class="p-5 shadow-sm border rounded" :title="$t('admin.groups.editMessage', [group.name])">
                <b-card-text class="border-top pt-4 mt-3">
                  <CIBForm @submitted="update()">
                    <b-form-group :label="$t('admin.groups.name') + '*'" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4"
                      :invalid-feedback="$t('errors.invalid')">
                      <b-form-input v-model="group.name" @update:modelValue="dirty=true" :state="notEmpty(group.name)" required></b-form-input>
                    </b-form-group>
                    <b-form-group :label="$t('admin.groups.type')" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4">
                      <b-form-input v-model="group.type" @update:modelValue="dirty = true"></b-form-input>
                    </b-form-group>
                    <div class="d-flex justify-content-between" v-if="$root.config.userProvider === 'org.cibseven.webapp.auth.SevenUserProvider'">
                      <b-button variant="light" @click="$refs.deleteModal.show()">{{ $t('admin.groups.deleteGroup') }}</b-button>
                      <b-button type="submit" variant="secondary" :disabled="!dirty" >{{ $t('admin.groups.update') }}</b-button>
                    </div>
                  </CIBForm>
                </b-card-text>
              </b-card>
            </div>
          </div>
          <div v-else-if="$route.query.tab === 'users'" class="row">
            <div class="col-12">
              <div class="p-3">
                <b-form-group labels-cols-lg="2" label-size="lg" label-class="fw-bold pt-0"
                  :label="$t('admin.groups.editMessage', [group.name])">
                </b-form-group>
                <div class="row py-3">
                  <div class="col-9">
                    <h5>{{ $t('admin.groups.user.title') }}</h5>
                  </div>
                </div>
                <div v-if="users" class="container-fluid overflow-auto bg-white shadow-sm border rounded g-0">
                  <FlowTable :items="users" primary-key="id" striped
                    prefix="admin.users." :fields="[{ label: 'id', key: 'id', class: 'col-md-3 col-sm-3', tdClass: 'py-2' },
                      { label: 'firstName', key: 'firstName', class: 'col-md-3 col-sm-3', tdClass: 'py-2' },
                      { label: 'lastName', key: 'lastName', class: 'col-md-3 col-sm-3', tdClass: 'py-2' },
                      { label: 'email', key: 'email', class: 'col-md-3 col-sm-3', tdClass: 'py-2' }]"
                    @contextmenu="focusedUser = $event" @mouseenter="focusedUser = $event" @mouseleave="focusedUser = null">
                  </FlowTable>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="$route.query.tab === 'tenants'" class="row">
            <div class="col-12">
              <div class="p-3">
                <b-form-group labels-cols-lg="2" label-size="lg" label-class="fw-bold pt-0"
                  :label="$t('admin.groups.editMessage', [group.name])">
                </b-form-group>
                <div class="row py-3">
                  <div class="col-9">
                    <h5>{{ $t('admin.tenants.associationTitle', [group.id]) }}</h5>
                  </div>
                  <div class="col-3 pb-3">
                    <div class="float-end">
                      <b-button size="sm" variant="secondary" v-if="$root.config.userProvider === 'org.cibseven.webapp.auth.SevenUserProvider'" @click="openAssignTenantModal">
                        <span class="mdi mdi-plus"> {{ $t('admin.tenants.addTo') }} </span>
                      </b-button>
                    </div>
                  </div>
                </div>
                <div v-if="groupTenants.length > 0" class="container-fluid overflow-auto bg-white shadow-sm border rounded g-0">
                  <FlowTable :items="groupTenants" primary-key="id" striped
                    prefix="admin.tenants." :fields="[{ label: 'fullId', key: 'id', class: 'col-5', tdClass: 'py-2' },
                      { label: 'fullName', key: 'name', class: 'col-5', tdClass: 'py-2' },
                      { label: 'actions', key: 'actions', class: 'col-2', tdClass: 'justify-content-center py-2', thClass: 'justify-content-center text-center', sortable: false }]"
                    @contextmenu="focusedTenant = $event" @mouseenter="focusedTenant = $event" @mouseleave="focusedTenant = null">
                    <template v-slot:cell(actions)="row">
                      <div>
                        <b-button :disabled="focusedTenant !== row.item" style="opacity: 1" @click="unassignTenant(row.item)" class="px-2 border-0 shadow-none" variant="link">
                          <span class="mdi mdi-18px mdi-delete-outline"></span>
                        </b-button>
                      </div>
                    </template>
                  </FlowTable>
                </div>
              </div>
            </div>
          </div>
        </div>

        <b-modal ref="assignTenantsModal" :title="$t('admin.tenants.addTo')" size="lg">
          <div v-if="unassignedTenants.length > 0" class="container g-0">
            <FlowTable :items="unassignedTenants" primary-key="id" prefix="admin.tenants." striped
              :fields="[{ label: '', key: 'selected', class: 'col-sm-1', sortable: false, thClass: 'text-center, border-top-0', tdClass: 'text-center' },
              { label: 'fullId', key: 'id', class: 'col-6', thClass: 'border-top-0' },
              { label: 'fullName', key: 'name', class: 'col-5', thClass: 'border-top-0' }]">
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

        <ConfirmDialog ref="deleteModal" @ok="deleteGroup()" :ok-title="$t('confirm.delete')">
          <div>
            <p>{{ $t('admin.groups.confirmDelete') }}</p>
            <p>
              <strong>{{ $t('admin.groups.id') }}:</strong> {{ group.id }} <br>
              <strong>{{ $t('admin.groups.name') }}:</strong> {{ group.name }}<br>
              <strong>{{ $t('admin.groups.type') }}:</strong> {{ group.type }}
            </p>
          </div>
        </ConfirmDialog>

      </div>
    </transition>
    <SuccessAlert ref="updateGroup" top="0" style="z-index: 1031">{{ $t('admin.groups.updateGroupMessage', [group.id]) }}</SuccessAlert>
    <SuccessAlert ref="deleteGroup" top="0" style="z-index: 1031">{{ $t('admin.groups.deleteGroupMessage', [group.id]) }}</SuccessAlert>
    <SuccessAlert ref="unassignTenant" top="0" style="z-index: 1031">{{ $t('admin.tenants.unassignGroupMessage', [group.id]) }}</SuccessAlert>
  </SidebarsFlow>
</template>

<script>
import { AdminService } from '@/services.js'
import { notEmpty } from '@/components/admin/utils.js'
import { SidebarsFlow, FlowTable, SuccessAlert, CIBForm, ConfirmDialog } from '@cib/common-frontend'
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'ProfileGroup',
  components: { SidebarsFlow, FlowTable, SuccessAlert, CIBForm, ConfirmDialog },
  data: function() {
    return {
      leftOpen: true,
      group: { id: null, name: null,  type: null },
      dirty: false,
      users: null,
      selectedUser: null,
      focusedUser: null,
      focusedTenant: null,
      perPage: 15,
      page: 1,
      groupTenants: [],
      unassignedTenants: []
    }
  },
  watch: {
    '$route.params.groupId': function() {
      this.loadGroup(this.$route.params.groupId)
      this.clean()
    },
    '$route.query.tab': function() {
      if (this.$route.query.tab === 'users') this.loadUsers(this.$route.params.groupId)
      else if (this.$route.query.tab === 'tenants') this.loadTenants(this.$route.params.groupId)
    }
  },
  computed: {
    ...mapGetters(['tenants'])
  },
  created: function() {
    if (this.$route.params.groupId) this.loadGroup(this.$route.params.groupId)
    if (this.$route.query.tab === 'users') this.loadUsers(this.$route.params.groupId)
    else if (this.$route.query.tab === 'tenants') this.loadTenants(this.$route.params.groupId)
  },
  methods: {
    ...mapActions(['fetchTenants', 'getTenantsByGroup', 'removeGroupFromTenant', 'addGroupToTenant']),
    loadGroup: function(groupId) {
      AdminService.findGroups({ id: groupId }).then(response => {
        this.group = response[0]
      })
    },
    loadUsers: function(groupId) {
      AdminService.findUsers({ memberOfGroup: groupId }).then(response => {
        this.users = response
      })
    },
    async loadTenants(groupId) {
      this.groupTenants = await this.getTenantsByGroup(groupId)
    },
    update: function() {
      AdminService.updateGroup(this.group.id, this.group).then(() => {
        this.$refs.updateGroup.show(2)
      })
    },
    notEmpty: function(value) {
      return notEmpty(value)
    },
    deleteGroup: function() {
      AdminService.deleteGroup(this.group.id).then(() => {
        this.$refs.deleteGroup.show(2)
        this.$router.push({ name: 'adminGroups' })
      })
    },
    clean: function () {
      this.dirty = false
      this.users = null
    },
    unassignTenant(tenant) {
      this.removeGroupFromTenant({ tenantId: tenant.id, groupId: this.group.id }).then(() => {
        this.$refs.unassignTenant.show(2)
        this.loadTenants(this.group.id)
      })
    },
    openAssignTenantModal: function() {
      this.loadUnassignedTenants()
      this.$refs.assignTenantsModal.show()
    },
    async loadUnassignedTenants() {
      await this.fetchTenants()
      const groupTenants = JSON.parse(JSON.stringify(this.groupTenants))
      this.unassignedTenants = []
      this.tenants.forEach(tenant => {
        let isAssigned = false
        groupTenants.forEach(groupTenant => {// TODO optimize with findIndex
          if (tenant.id === groupTenant.id) isAssigned = true
        })
        if (!isAssigned){
          tenant.selected = false
          this.unassignedTenants.push(tenant)
        }
      })
    },
    assignTenants() {
      this.unassignedTenants.forEach(tenant => {
        if (tenant.selected) {
          this.addGroupToTenant({ tenantId: tenant.id, groupId: this.group.id }).then(() => {
            this.groupTenants.push(tenant)
          })
        }
      })
    }
  }
}
</script>
