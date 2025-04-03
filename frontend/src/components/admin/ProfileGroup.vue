<template>
  <Sidebars ref="sidebars" header-margin="55px" v-model:left-open="leftOpen"
    :left-caption="$t('admin.groups.title') + ' - ' + group.id" :left-size="[12, 6, 4, 3, 2]">
    <template v-slot:left>
      <b-list-group>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'information'" exact :to="'?tab=information'">
          <span> {{ $t('admin.groups.information') }}</span>
        </b-list-group-item>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'users'" exact :to="'?tab=users'">
          <span> {{ $t('admin.groups.users') }}</span>
        </b-list-group-item>
      </b-list-group>
    </template>

    <transition name="slide-in" mode="out-in">
      <div v-if="group.id" class="d-flex flex-column h-100 bg-light">
        <b-button variant="light" style="min-height: 35px; line-height: 20px;" :block="true" class="rounded-0 border-bottom text-start" href="#/seven/auth/admin/groups">
          <span class="mdi mdi-arrow-left me-2"></span>
          <span class="fw-semibold">{{ $t('admin.groups.title') }}</span>
        </b-button>
        <div class="container-fluid overflow-auto">
          <div v-if="$route.query.tab === 'information'" class="row">
            <div class="col-sm-12 col-md-12 col-lg-8 col-xl-6 p-4">
              <b-card class="border-0 p-5 shadow" :title="$t('admin.groups.editMessage', [group.name])">
                <b-card-text class="border-top border-top pt-4 mt-3">
                  <CIBForm @submitted="update()">
                    <b-form-group :label="$t('admin.groups.name') + '*'" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4"
                      :invalid-feedback="$t('errors.invalid')">
                      <b-form-input v-model="group.name" @update:modelValue="dirty=true" :state="notEmpty(group.name)" required></b-form-input>
                    </b-form-group>
                    <b-form-group :label="$t('admin.groups.type')" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4">
                      <b-form-input v-model="group.type" @update:modelValue="dirty = true"></b-form-input>
                    </b-form-group>
                    <div class="d-flex justify-content-between" v-if="$root.config.userProvider === 'org.cibseven.webapp.auth.SevenUserProvider'">
                      <b-button variant="warning" @click="$refs.deleteModal.show()">{{ $t('admin.groups.deleteGroup') }}</b-button>
                      <b-button type="submit" variant="secondary" :disabled="!dirty" >{{ $t('admin.groups.update') }}</b-button>
                    </div>
                  </CIBForm>
                </b-card-text>
              </b-card>
            </div>
          </div>
          <div v-if="$route.query.tab === 'users'" class="row">
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
                <div v-if="users" class="container-fluid overflow-auto bg-white shadow g-0">
                  <FlowTable :items="users" primary-key="id" striped
                    prefix="admin.users." :fields="[{label: 'id', key: 'id', class: 'col-md-3 col-sm-3', tdClass: 'border-end py-2' },
                      {label: 'firstName', key: 'firstName', class: 'col-md-3 col-sm-3', tdClass: 'border-end py-2' },
                      {label: 'lastName', key: 'lastName', class: 'col-md-3 col-sm-3', tdClass: 'border-end py-2' },
                      {label: 'email', key: 'email', class: 'col-md-3 col-sm-3', tdClass: 'py-2' }]"
                    @contextmenu="focusedUser = $event" @mouseenter="focusedUser = $event" @mouseleave="focusedUser = null">
                  </FlowTable>
                </div>
              </div>
            </div>
          </div>
        </div>

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
  </Sidebars>
</template>

<script>
import { AdminService } from '@/services.js'
import { notEmpty } from '@/components/admin/utils.js'
import Sidebars from '@/components/common-components/Sidebars.vue'
import FlowTable from '@/components/common-components/FlowTable.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
import CIBForm from '@/components/common-components/CIBForm.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'

export default {
  name: 'ProfileGroup',
  components: { Sidebars, FlowTable, SuccessAlert, CIBForm, ConfirmDialog },
  data: function() {
    return {
      leftOpen: true,
      group: { id: null, name: null,  type: null },
      dirty: false,
      users: null,
      selectedUser: null,
      focusedUser: null,
      perPage: 15,
      page: 1
    }
  },
  watch: {
    '$route.params.groupId': function() {
      this.loadGroup(this.$route.params.groupId)
      this.clean()
    },
    '$route.query.tab': function() {
      if (this.$route.query.tab === 'users') this.loadUsers(this.$route.params.groupId)
    }
  },
  created: function() {
    if (this.$route.params.groupId) this.loadGroup(this.$route.params.groupId)
    if (this.$route.query.tab === 'users') this.loadUsers(this.$route.params.groupId)
  },
  methods: {
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
        this.$router.push('/seven/auth/admin/groups')
      })
    },
    clean: function () {
      this.dirty = false
      this.users = null
    }
  }
}
</script>
