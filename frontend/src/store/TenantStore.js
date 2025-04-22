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
    },
    async getTenantsByUser(_, userId) {
      return await TenantService.getTenants({ userMember: userId })
    },
    async removeUserFromTenant(_, { tenantId, userId }) {
      return await TenantService.removeUserFromTenant(tenantId, userId)
    },
    async addUserToTenant(_, { tenantId, userId }) {
      return await TenantService.addUserToTenant(tenantId, userId)
    },
    async getTenantsByGroup(_, groupId) {
      return await TenantService.getTenants({ groupMember: groupId })
    },
    async removeGroupFromTenant(_, { tenantId, groupId }) {
      return await TenantService.removeGroupFromTenant(tenantId, groupId)
    },
    async addGroupToTenant(_, { tenantId, groupId }) {
      return await TenantService.addGroupToTenant(tenantId, groupId)
    }
  },
  getters: {
    tenants: state => state.tenants
  }
}

export default TenantStore