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
    :left-caption="$t('admin.users.title') + ' - ' + user.id" :left-size="[12, 6, 4, 3, 2]">
    <template v-slot:left>
      <b-list-group>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'profile' || !$route.query.tab" exact :to="'?tab=profile'">
          <span> {{ $t('admin.users.profile') }}</span>
        </b-list-group-item>
        <b-list-group-item v-if="$root.config.userEditable" class="border-0 px-3 py-2" :active="$route.query.tab === 'account'" exact :to="'?tab=account'">
          <span> {{ $t('password.recover.changePassword') }}</span>
        </b-list-group-item>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'groups'" exact :to="'?tab=groups'">
          <span> {{ $t('admin.users.groups') }}</span>
        </b-list-group-item>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'tenants'" exact :to="'?tab=tenants'">
          <span> {{ $t('admin.tenants.title') }}</span>
        </b-list-group-item>
        <b-list-group-item v-if="$root.user && $root.user.id === user.id" class="border-0 px-3 py-2"
          :active="$route.query.tab === 'preferences'" exact :to="'?tab=preferences'">
          <span> {{ $t('admin.preferences.title') }}</span>
        </b-list-group-item>
      </b-list-group>
    </template>

    <transition name="slide-in" mode="out-in">
      <div class="d-flex flex-column bg-light w-100 h-100" v-if="user.id">
        <b-button v-if="editMode" variant="light" style="min-height: 40px; line-height: 20px;" :block="true" class="rounded-0 border-bottom text-start" href="#/seven/auth/admin/users">
          <span class="mdi mdi-arrow-left me-2"></span>
          <span class="fw-bold">{{ $t('admin.users.title') }}</span>
        </b-button>
        <div class="container-fluid overflow-auto">

          <!-- Profile Tab -->
          <div v-if="$route.query.tab === 'profile' || !$route.query.tab" class="row pt-3 ps-4 pe-4">
            <ContentBlock
              :title="$t('admin.users.editMessage', [user.firstName + ' ' + user.lastName])"
              class="col-lg-6 col-md-8 col-sm-12">
              <CIBForm @submitted="update()">
                <b-form-group :label="$t('admin.users.firstName') + '*'" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4"
                  :invalid-feedback="$t('errors.invalid')">
                  <b-form-input v-model="user.firstName"  @update:modelValue="dirty=true"
                    :state="notEmpty(user.firstName)" :readonly="!$root.config.userEditable || !editMode" required></b-form-input>
                </b-form-group>
                <b-form-group :label="$t('admin.users.lastName') + '*'" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4"
                  :invalid-feedback="$t('errors.invalid')">
                  <b-form-input v-model="user.lastName"  @update:modelValue="dirty=true"
                    :state="notEmpty(user.lastName)" :readonly="!$root.config.userEditable || !editMode" required></b-form-input>
                </b-form-group>
                <b-form-group :label="$t('admin.users.email')" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4"
                  :invalid-feedback="$t('errors.invalid')">
                  <b-form-input v-model="user.email" type="email" autocomplete="email"  @update:modelValue="dirty = true" :readonly="!$root.config.userEditable || !editMode"></b-form-input>
                </b-form-group>
                <div class="float-end" v-if="$root.config.userEditable">
                  <b-button type="submit" variant="secondary" :disabled="!dirty" >{{ $t('admin.users.update') }}</b-button>
                </div>
              </CIBForm>
            </ContentBlock>
          </div>

          <!-- Account Tab -->
          <div v-else-if="$route.query.tab === 'account' && $root.config.userEditable" class="row pt-3 ps-4 pe-4">
            <ContentBlock
              :title="$t('password.recover.changePassword')"
              class="col-lg-6 col-md-8 col-sm-12">
              <b-form-group labels-cols-lg="4" label-size="lg" label-class="fw-bold pt-0 pb-4" class="m-0">
                <b-form-group :label="$t('password.recover.currentUserPassword') + '*'" label-cols-sm="4"
                  label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                  <b-form-input v-model="credentials.authenticatedUserPassword"></b-form-input>
                </b-form-group>
                <b-form-group :label="$t('password.recover.newPassword') + '*'" label-cols-sm="4"
                  label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                  <b-form-input v-model="credentials.password"></b-form-input>
                </b-form-group>
                <b-form-group :label="$t('password.recover.newPasswordRepeat') + '*'" label-cols-sm="4"
                  label-align-sm="left" label-class="pb-4" :invalid-feedback="$t('errors.invalid')">
                  <b-form-input v-model="passwordRepeat"></b-form-input>
                </b-form-group>
                <div class="float-end d-flex align-items-center">
                  <b-button type="submit" variant="secondary" @click="changePassword($event)">{{$t('password.recover.changePassword')}}</b-button>
                </div>
              </b-form-group>
            </ContentBlock>
          </div>

          <!-- Groups Tab -->
          <div v-else-if="$route.query.tab === 'groups'" class="row pt-3 ps-4 pe-4">
            <ContentBlock
              :title="$t('admin.users.group.title', [user.firstName + ' ' + user.lastName])"
              class="">

              <template v-if="editMode" v-slot:actions>
                <b-button size="sm" variant="secondary" v-if="$root.config.userEditable" @click="openAssignGroupModal" class="mdi mdi-plus">
                  <span class="ms-2">{{ $t('admin.users.group.add') }}</span>
                </b-button>
              </template>

              <div v-if="groups" class="p-2">
                <FlowTable striped :items="groups" primary-key="id" prefix="admin.groups." :fields="groupFields"
                  @contextmenu="focusedGroup = $event" @mouseenter="focusedGroup = $event" @mouseleave="focusedGroup = null">
                  <template v-slot:cell(actions)="row">
                    <div>
                      <b-button :disabled="focusedGroup !== row.item" style="opacity: 1" @click="unassignGroup(row.item)" class="px-2 border-0 shadow-none" :title="$t('admin.groups.deleteGroup')" variant="link">
                        <span class="mdi mdi-18px mdi-delete-outline"></span>
                      </b-button>
                    </div>
                  </template>
                </FlowTable>
              </div>
            </ContentBlock>
          </div>

          <!-- Tenants Tab -->
          <div v-else-if="$route.query.tab === 'tenants'" class="row pt-3 ps-4 pe-4">
            <ContentBlock
              :title="$t('admin.tenants.associationTitle', [user.firstName + ' ' + user.lastName])"
              class="">

              <template v-if="editMode" v-slot:actions>
                <b-button size="sm" variant="secondary" v-if="$root.config.userEditable" @click="openAssignTenantModal" class="mdi mdi-plus">
                  <span class="ms-2"> {{ $t('admin.tenants.addTo') }} </span>
                </b-button>
              </template>

              <div v-if="userTenants.length > 0" class="p-2">
                <FlowTable striped :items="userTenants" primary-key="id" prefix="admin.tenants." :fields="tenantFields"
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
              <span v-else class="d-block text-center p-4">
                {{ $t('admin.noResults') }}
              </span>
            </ContentBlock>
          </div>

          <!-- Preferences Tab -->
          <div v-else-if="$route.query.tab === 'preferences'" class="row pt-3 ps-4 pe-4">
            <ProfilePreferencesTab></ProfilePreferencesTab>
          </div>

        </div>

        <!-- Assign Groups Modal -->
        <b-modal v-if="editMode" ref="assignGroupsModal" :title="$t('admin.users.group.add')" size="lg">
          <div class="container g-0">
            <FlowTable :items="unAssignedGroups" primary-key="id" prefix="admin.groups." striped
              :fields="[{ label: '', key: 'selected', class: 'col-sm-1', sortable: false, thClass: 'text-center', tdClass: 'text-center' },
              { label: 'id', key: 'id', class: 'col-sm-3' },
              { label: 'name', key: 'name', class: 'col-sm-5' },
              { label: 'type', key: 'type', class: 'col-sm-3' }]">
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

        <b-modal v-if="editMode" ref="assignTenantsModal" :title="$t('admin.tenants.addTo')" size="lg">
          <div v-if="unassignedTenants.length > 0" class="container g-0">
            <FlowTable :items="unassignedTenants" primary-key="id" prefix="admin.tenants." striped
              :fields="[{ label: '', key: 'selected', class: 'col-sm-1', sortable: false, tdClass: 'text-center' },
              { label: 'fullId', key: 'id', class: 'col-6' },
              { label: 'fullName', key: 'name', class: 'col-5' }]">
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

        <b-modal v-if="editMode" ref="deleteModal" :title="$t('confirm.title')">
          <div class="container-fluid">
            <div class="row align-items-center">
              <div class="col-2">
                <span class="mdi-36px mdi mdi-alert-outline text-warning me-3"></span>
              </div>
              <div class="col-10">
                <p>{{ $t('admin.users.confirmDelete') }}</p>
                <strong>{{ $t('admin.users.id') }}:</strong> {{ user.id }} <br>
                <strong>{{ $t('admin.users.firstName') }}:</strong> {{ user.firstName }}<br>
                <strong>{{ $t('admin.users.lastName') }}:</strong> {{ user.lastName }}<br>
                <strong>{{ $t('admin.users.email') }}:</strong> {{ user.email }}
              </div>
            </div>
          </div>
          <template v-slot:modal-footer>
            <b-button @click="$refs.deleteModal.hide()" variant="light">{{ $t('confirm.cancel') }}</b-button>
            <b-button @click="deleteUser(); $refs.deleteModal.hide()" variant="primary">{{ $t('confirm.ok') }}</b-button>
          </template>
        </b-modal>

      </div>
    </transition>
    <b-popover :target="() => $refs.passwordHelper" triggers="hover">
      <h6>{{ $t('password.policy.title') }}</h6>
      <div>{{ $t('password.policy.header') }}</div>
      <ul>
        <li v-for="(item, index) in $tm('password.policy.items')" :key="index">{{ item }}</li>
      </ul>
    </b-popover>
    <SuccessAlert ref="emailSent"> {{ $t('password.recover.emailSent') }} </SuccessAlert>
    <SuccessAlert ref="updateProfile" top="0" style="z-index: 1031">{{ $t('admin.users.updateProfileMessage', [user.id]) }}</SuccessAlert>
    <SuccessAlert ref="updatePassword" top="0" style="z-index: 1031">{{ $t('admin.users.updatePasswordMessage', [user.id]) }}</SuccessAlert>
    <SuccessAlert ref="deleteUser" top="0" style="z-index: 1031">{{ $t('admin.users.userDeletedMessage', [user.id]) }}</SuccessAlert>
    <SuccessAlert ref="unassignGroup" top="0" style="z-index: 1031">{{ $t('admin.users.unassignGroupMessage', [user.id]) }}</SuccessAlert>
    <SuccessAlert ref="unassignTenant" top="0" style="z-index: 1031">{{ $t('admin.tenants.unassignUserMessage', [user.id]) }}</SuccessAlert>
  </SidebarsFlow>
</template>

<script>
import { AdminService } from '@/services.js'
import { notEmpty, same } from '@/components/admin/utils.js'

import { SidebarsFlow, FlowTable, SuccessAlert, CIBForm, ContentBlock }  from '@cib/common-frontend'
import ProfilePreferencesTab from '@/components/admin/ProfilePreferencesTab.vue'

import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'ProfileUser',
  components: { SidebarsFlow, FlowTable, SuccessAlert, CIBForm, ProfilePreferencesTab, ContentBlock },
  inject: ['AuthService'],
  props: {
    editMode: {
      type: Boolean,
      default: false
    }
  },
  data: function () {
    return {
      leftOpen: true,
      user: { id: null, firstName: null,  lastName: null, email: null },
      dirty: false,
      credentials: { authenticatedUserPassword: null, password: null },
      passwordRepeat: null,
      groups: null,
      unAssignedGroups: [],
      unAssignedGroupsLoading: false,
      unassignedTenants: [],
      focusedGroup: null,
      focusedTenant: null,
      passwordPolicyError: false,
      passwordVisibility: { current: false, new: false, repeat: false },
      userTenants: []
    }
  },
  watch: {
    '$route.params.userId': function() {
      if (!this.editMode && this.$route.params.userId !== this.$router.app.user.id) {
        this.$router.push('/seven/auth/start')
      } else {
        this.loadUser(this.$route.params.userId)
        this.clean()
      }
    },
    '$route.query.tab': function() {
      if (this.$route.query.tab === 'groups') this.loadGroups(this.$route.params.userId)
      else if (this.$route.query.tab === 'tenants') this.loadTenants(this.$route.params.userId)
    }
  },
  computed: {
    ...mapGetters(['tenants']),
    groupFields() {
      const isEditable = this.editMode && this.$root.config.userEditable
      const fields = [
        {
          label: 'id',
          key: 'id',
          class: 'col-md-4 col-sm-4',
          ...(isEditable && { tdClass: 'py-1' })
        },
        {
          label: 'name',
          key: 'name',
          class: 'col-md-4 col-sm-4',
          ...(isEditable && { tdClass: 'py-1' })
        },
        {
          label: 'type',
          key: 'type',
          class: isEditable ? 'col-md-2 col-sm-2' : 'col-md-4 col-sm-4',
          ...(isEditable && { tdClass: 'py-1' })
        }
      ]
      if (isEditable) {
        fields.push({
          label: 'actions',
          key: 'actions',
          class: 'col-md-2 col-sm-2',
          sortable: false,
          thClass: 'justify-content-center text-center',
          tdClass: 'justify-content-center py-0'
        })
      }
      return fields
    },
    tenantFields() {
      const isEditable = this.editMode && this.$root.config.userEditable
      const fields = [
        {
          label: 'fullId',
          key: 'id',
          class: isEditable ? 'col-md-4 col-sm-4' : 'col-md-6 col-sm-6',
          ...(isEditable && { tdClass: 'py-1' })
        },
        {
          label: 'fullName',
          key: 'name',
          class: isEditable ? 'col-md-4 col-sm-4' : 'col-md-6 col-sm-6',
          ...(isEditable && { tdClass: 'py-1' })
        }
      ]
      if (isEditable) {
        fields.push({
          label: 'actions',
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
  created: function () {
    if (this.$route.params.userId) this.loadUser(this.$route.params.userId)
    if (this.$route.query.tab === 'groups') this.loadGroups(this.$route.params.userId)
    else if (this.$route.query.tab === 'tenants') this.loadTenants(this.$route.params.userId)
  },
  methods: {
    ...mapActions(['fetchTenants', 'getTenantsByUser', 'removeUserFromTenant', 'addUserToTenant']),
    loadUser: function(userId) {
      AdminService.findUsers({ id: userId }).then(response => {
        this.user = response[0]
      })
    },
    loadGroups: function(userId) {
      AdminService.findGroups({ member: userId }).then(response => {
        this.groups = response
      })
    },
    async loadTenants(userId) {
      this.userTenants = await this.getTenantsByUser(userId)
    },
    update: function() {
      AdminService.updateUserProfile(this.user.id, this.user).then(() => {
        this.$refs.updateProfile.show(2)
      })
    },
    notEmpty: function(value) {
      return notEmpty(value)
    },
    same: function(value, value2) {
      return same(value, value2)
    },
    changePassword: function (evt) {
      evt.preventDefault()
      if (same(this.credentials.password, this.passwordRepeat) && notEmpty(this.credentials.authenticatedUserPassword)) {
        AdminService.updateUserCredentials(this.user.id, this.credentials.password,
          this.credentials.authenticatedUserPassword).then(() => {
          this.passwordPolicyError = false
          this.$refs.updatePassword.show(2)
        }, error => {
          var data = error.response.data
          if (data && data.type === 'PasswordPolicyException') {
            this.passwordPolicyError = true
          }
        })
      }
    },
    deleteUser: function() {
      AdminService.deleteUser(this.user.id).then(() => {
        this.$refs.deleteUser.show(2)
        this.$router.push('/seven/auth/admin/users')
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
      var userGroups = JSON.parse(JSON.stringify(this.groups))
      this.unAssignedGroups = []
      AdminService.findGroups().then(allGroups => {
        allGroups.forEach(group => {
          var isAssigned = false
          userGroups.forEach(userGroup => {
            if (group.id === userGroup.id) isAssigned = true
          })
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
    },
    clean: function() {
      this.dirty = false
      this.credentials = { authenticatedUserPassword: null, password: null }
      this.passwordRepeat = null
      this.groups = null
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
        var isAssigned = false
        userTenants.forEach(userTenant => {
          if (tenant.id === userTenant.id) isAssigned = true
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
