<template>
  <SidebarsFlow ref="sidebars" header-margin="55px" v-model:left-open="leftOpen"
    :left-caption="$t('admin.tenants.title') + ' - ' + tenant.id" :left-size="[12, 6, 4, 3, 2]">
    <template v-slot:left>
      <b-list-group>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'information'" exact :to="'?tab=information'">
          <span> {{ $t('admin.tenants.information') }}</span>
        </b-list-group-item>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'users'" exact :to="'?tab=users'">
          <span> {{ $t('admin.tenants.users') }}</span>
        </b-list-group-item>
        <b-list-group-item class="border-0 px-3 py-2" :active="$route.query.tab === 'groups'" exact :to="'?tab=groups'">
          <span> {{ $t('admin.tenants.groups') }}</span>
        </b-list-group-item>
      </b-list-group>
    </template>

    <transition name="slide-in" mode="out-in">
      <div v-if="tenant.id" class="d-flex flex-column h-100 bg-light">
        <b-button variant="light" style="min-height: 35px; line-height: 20px;" :block="true" class="rounded-0 border-bottom text-start" href="#/seven/auth/admin/tenants">
          <span class="mdi mdi-arrow-left me-2"></span>
          <span class="fw-semibold">{{ $t('admin.tenants.title') }}</span>
        </b-button>
        <div class="container-fluid overflow-auto">
          <div v-if="$route.query.tab === 'information'" class="row">
            <div class="col-sm-12 col-md-12 col-lg-8 col-xl-6 p-4">
              <b-card class="p-5 shadow-sm border rounded" :title="$t('admin.tenants.editMessage', [tenant.name])">
                <b-card-text class="border-top pt-4 mt-3">
                  <CIBForm @submitted="update()">
                    <b-form-group :label="$t('admin.tenants.name') + '*'" label-cols-sm="6" label-cols-md="6" label-cols-lg="4" label-align-sm="left" label-class="pb-4"
                      :invalid-feedback="$t('errors.invalid')">
                      <b-form-input v-model="tenant.name" @update:modelValue="dirty=true" :state="notEmpty(tenant.name)" required></b-form-input>
                    </b-form-group>
                    <div class="d-flex justify-content-between" v-if="$root.config.userProvider === 'org.cibseven.webapp.auth.SevenUserProvider'">
                      <b-button variant="warning" @click="$refs.deleteModal.show()">{{ $t('admin.tenants.deleteTenant') }}</b-button>
                      <b-button type="submit" variant="secondary" :disabled="!dirty" >{{ $t('admin.tenants.update') }}</b-button>
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
                  :label="$t('admin.tenants.editMessage', [tenant.name])">
                </b-form-group>
                <div class="row py-3">
                  <div class="col-9">
                    <h5>{{ $t('admin.tenants.tenantUsers') }}</h5>
                  </div>
                </div>
                <div v-if="users" class="container-fluid overflow-auto bg-white shadow-sm border rounded g-0">
                  <FlowTable :items="users" primary-key="id" striped
                    prefix="admin.users." :fields="[{label: 'id', key: 'id', class: 'col-md-3 col-sm-3', tdClass: 'border-end py-2' },
                      {label: 'firstName', key: 'firstName', class: 'col-md-3 col-sm-3', tdClass: 'border-end py-2' },
                      {label: 'lastName', key: 'lastName', class: 'col-md-3 col-sm-3', tdClass: 'border-end py-2' },
                      {label: 'email', key: 'email', class: 'col-md-3 col-sm-3', tdClass: 'py-2' }]">
                  </FlowTable>
                </div>
              </div>
            </div>
          </div>
          <div v-if="$route.query.tab === 'groups'" class="row">
            <div class="col-12">
              <div class="p-3">
                <b-form-group labels-cols-lg="2" label-size="lg" label-class="fw-bold pt-0"
                  :label="$t('admin.tenants.editMessage', [tenant.name])">
                </b-form-group>
                <div class="row py-3">
                  <div class="col-9">
                    <h5>{{ $t('admin.tenants.tenantGroups') }}</h5>
                  </div>
                </div>
                <div v-if="groups" class="container-fluid overflow-auto bg-white shadow-sm border rounded g-0">
                  <FlowTable :items="groups" primary-key="id" striped
                    prefix="admin.groups." :fields="[
                      { label: 'id', key: 'id', class: 'col-4', tdClass: 'border-end py-2' },
                      { label: 'name', key: 'name', class: 'col-4', tdClass: 'border-end py-2' },
                      { label: 'type', key: 'type', class: 'col-4', tdClass: 'py-2' }
                    ]">
                  </FlowTable>
                </div>
              </div>
            </div>
          </div>
        </div>
        <ConfirmDialog ref="deleteModal" @ok="remove()" :ok-title="$t('confirm.delete')">
          <div>
            <p>{{ $t('admin.tenants.confirmDelete') }}</p>
            <p>
              <strong>{{ $t('admin.tenants.id') }}:</strong> {{ tenant.id }} <br>
              <strong>{{ $t('admin.tenants.name') }}:</strong> {{ tenant.name }}
            </p>
          </div>
        </ConfirmDialog>
      </div>
    </transition>
    <SuccessAlert ref="updateTenant" top="0" style="z-index: 1031">{{ $t('admin.tenants.updateTenantMessage', [tenant.id]) }}</SuccessAlert>
    <SuccessAlert ref="deleteTenant" top="0" style="z-index: 1031">{{ $t('admin.tenants.deleteTenantMessage', [tenant.id]) }}</SuccessAlert>
  </SidebarsFlow>
</template>

<script>
  import { AdminService } from '@/services.js'
  import { notEmpty } from '@/components/admin/utils.js'
  import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'
  import FlowTable from '@/components/common-components/FlowTable.vue'
  import SuccessAlert from '@/components/common-components/SuccessAlert.vue'
  import CIBForm from '@/components/common-components/CIBForm.vue'
  import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
  import { mapActions } from 'vuex'

  export default {
    name: 'EditTenant',
    components: { SidebarsFlow, FlowTable, SuccessAlert, CIBForm, ConfirmDialog },
    data: function() {
      return {
        leftOpen: true,
        tenant: { id: null, name: null },
        dirty: false,
        users: null,
        groups: null
      }
    },
    watch: {
      '$route.params.tenantId': function() {
        this.loadTenant(this.$route.params.tenantId)
        this.clean()
      },
      '$route.query.tab': function() {
        if (this.$route.query.tab === 'users') this.loadUsers(this.$route.params.tenantId)
        else if (this.$route.query.tab === 'groups') this.loadGroups(this.$route.params.tenantId)
      }
    },
    created: function() {
      if (this.$route.params.tenantId) this.loadTenant(this.$route.params.tenantId)
      if (this.$route.query.tab === 'users') this.loadUsers(this.$route.params.groupId)
      else if (this.$route.query.tab === 'groups') this.loadGroups(this.$route.params.tenantId)
    },
    methods: {
      ...mapActions(['getTenantById', 'updateTenant', 'deleteTenant']),
      loadTenant: function(tenantId) {
        this.getTenantById(tenantId).then(res => {
          this.tenant = res
        })
      },
      loadUsers: function(tenantId) {
        AdminService.findUsers({ memberOfTenant: tenantId }).then(response => {
          this.users = response
        })
      },
      loadGroups: function(tenantId) {
        AdminService.findGroups({ memberOfTenant: tenantId }).then(response => {
          this.groups = response
        })
      },
      update: function() {
        this.updateTenant(this.tenant).then(() => {
          this.$refs.updateTenant.show(2)
        })
      },
      notEmpty: function(value) {
        return notEmpty(value)
      },
      remove: function() {
        this.deleteTenant(this.tenant.id).then(() => {
          this.$refs.deleteTenant.show(2)
          this.$router.push('/seven/auth/admin/tenants')
        })
      },
      clean: function () {
        this.dirty = false
        this.users = null
        this.groups = null
      }
    }
  }
</script>
