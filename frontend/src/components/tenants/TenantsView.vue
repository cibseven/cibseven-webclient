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
      </div>
    </div>
    <div class="container overflow-auto bg-white shadow g-0">
      <FlowTable striped :items="filteredTenants" :fields="tenantFields" primary-key="id" table-class="table-striped" prefix="admin.tenants."></FlowTable>
      <div class="mb-3 text-center w-100" v-if="loading">
        <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
      </div>
      <div class="mb-3 text-center w-100" v-if="!loading && tenants.length === 0">
        {{ $t('admin.noResults') }}
      </div>
    </div>
  </div>
</template>

<script>
  import FlowTable from '@/components/common-components/FlowTable.vue'
  import { BWaitingBox } from 'cib-common-components'
  import { mapActions, mapGetters } from 'vuex'

  export default {
    name: 'TenantsView',
    components: { FlowTable, BWaitingBox },
    data() {
      return {
        loading: false,
        filter: ''
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
      ...mapActions(['fetchTenants']),
      loadTenants: function() {
        this.loading = true
        this.fetchTenants().finally(() => {
          this.loading = false
        })
      }
    }
  }
</script>