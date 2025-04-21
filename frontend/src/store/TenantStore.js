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
    async createTenant(_, tenant) {
      await TenantService.createTenant(tenant)
    }
  },
  getters: {
    tenants: state => state.tenants
  }
}

export default TenantStore