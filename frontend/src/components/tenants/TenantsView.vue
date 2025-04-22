<template>
  <div class="d-flex flex-column bg-light" :style="{ height: 'calc(100% - 55px)' }">
    <div class="container pt-4">
      <div class="row align-items-center pb-2">
        <div class="col-4">
          <b-input-group size="sm">
            <template #prepend>
              <b-button :title="$t('searches.search')" aria-hidden="true" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </template>
            <b-form-input :title="$t('searches.search')" :placeholder="$t('searches.search')" v-model="filter"></b-form-input>
          </b-input-group>
        </div>
        <div class="col-8 text-end">
          <b-button class="border me-1" size="sm" variant="light" v-if="$root.config.userProvider === 'org.cibseven.webapp.auth.SevenUserProvider'" @click="add()">
            <span class="mdi mdi-plus"> {{ $t('admin.tenants.add') }} </span>
          </b-button>
        </div>
      </div>
    </div>
    <div class="container overflow-auto bg-white shadow-sm border rounded g-0">
      <FlowTable striped :items="filteredTenants" :fields="tenantFields" primary-key="id" prefix="admin.tenants."
        @contextmenu="focused = $event" @mouseenter="focused = $event" @mouseleave="focused = null">
        <template v-slot:cell(actions)="row">
          <div>
            <b-button :disabled="focused !== row.item" style="opacity: 1" @click="edit(row.item)" class="px-2 border-0 shadow-none" :title="$t('admin.tenants.editTenant')" variant="link">
              <span class="mdi mdi-18px mdi-pencil-outline"></span>
            </b-button>
            <span class="border-start h-50" :class="focused === row.item ? 'border-secondary' : ''"></span>
            <b-button :disabled="focused !== row.item" style="opacity: 1" @click="prepareRemove(row.item)" class="px-2 border-0 shadow-none" :title="$t('admin.tenants.deleteTenant')" variant="link">
              <span class="mdi mdi-18px mdi-delete-outline"></span>
            </b-button>
          </div>
        </template>
      </FlowTable>
      <div class="mb-3 text-center w-100" v-if="loading">
        <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
      </div>
      <div class="mb-3 text-center w-100" v-if="!loading && tenants.length === 0">
        {{ $t('admin.noResults') }}
      </div>
    </div>
    <ConfirmDialog ref="deleteModal" @ok="remove(tenantSelected)" :ok-title="$t('confirm.delete')">
      <span v-if="tenantSelected">
        <p>{{ $t('admin.tenants.confirmDelete') }}</p>
        <p>
          <strong>{{ $t('admin.tenants.id') }}:</strong> {{ tenantSelected.id }} <br>
          <strong>{{ $t('admin.tenants.name') }}:</strong> {{ tenantSelected.name }}
        </p>
      </span>
    </ConfirmDialog>
  </div>
</template>

<script>
  import FlowTable from '@/components/common-components/FlowTable.vue'
  import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
  import { BWaitingBox } from 'cib-common-components'
  import { mapActions, mapGetters } from 'vuex'

  export default {
    name: 'TenantsView',
    components: { FlowTable, BWaitingBox, ConfirmDialog },
    data() {
      return {
        loading: false,
        filter: '',
        focused: null,
        tenantSelected: null
      }
    },
    computed: {
      ...mapGetters(['tenants']),
      tenantFields: function() {
        return [
          { label: 'id', key: 'id', class: 'col-5', tdClass: 'border-end py-1' },
          { label: 'name', key: 'name', class: 'col-5', tdClass: 'border-end py-1' },
          { label: 'actions', key: 'actions', class: 'col-2 text-center', sortable: false, thClass: 'justify-content-center', tdClass: 'justify-content-center py-0' }
        ]
      },
      filteredTenants: function() {
        if (!this.filter) return this.tenants
        const str = this.filter.toLowerCase()
        return this.tenants.filter(tenant =>
          tenant.id?.toLowerCase().includes(str) ||
          tenant.name?.toLowerCase().includes(str)
        )
      },
    },
    mounted() {
      this.loadTenants()
    },
    methods: {
      ...mapActions(['fetchTenants', 'deleteTenant']),
      loadTenants: function() {
        this.loading = true
        this.fetchTenants().finally(() => {
          this.loading = false
        })
      },
      add: function() {
        this.$router.push('/seven/auth/admin/create-tenant')
      },
      edit: function(tenant) {
        this.$router.push('/seven/auth/admin/tenant/' + tenant.id + '?tab=information')
      },
      prepareRemove: function(tenant) {
        this.tenantSelected = tenant
        this.$refs.deleteModal.show()
      },
      remove: function(tenant) {
        this.deleteTenant(tenant.id).then(() => {
          this.tenantSelected = null
        })
      }
    }
  }
</script>