/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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