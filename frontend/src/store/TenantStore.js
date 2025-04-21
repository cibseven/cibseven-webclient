import { TenantService } from '@/services.js'

const TenantStore = {
  state: {
    tenants: []
  },
  mutations: {
    setTenants(state, tenants) {
      state.tenants = tenants
    }
  },
  actions: {
    async fetchTenants({ commit }) {
      const tenants = await TenantService.getTenants()
      commit('setTenants', tenants)
      return tenants
    },
    async getTenantById(_, tenantId) {
      return await TenantService.getTenantById(tenantId)
    },
    async createTenant(_, tenant) {
      await TenantService.createTenant(tenant)
    },
    async updateTenant(_, tenant) {
      return await TenantService.updateTenant(tenant)
    },
    async deleteTenant({ dispatch }, tenantId) {
      await TenantService.deleteTenant(tenantId)
      await dispatch('fetchTenants')
    }
  },
  getters: {
    tenants: state => state.tenants
  }
}

export default TenantStore