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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import TenantStore from '../../store/TenantStore.js'
import { TenantService } from '@/services.js'
import { createStoreTestSuite } from './store-test-utils.js'

vi.mock('@/services.js', () => ({
  TenantService: {
    getTenants: vi.fn(),
    getTenantById: vi.fn(),
    createTenant: vi.fn(),
    updateTenant: vi.fn(),
    deleteTenant: vi.fn(),
    addUserToTenant: vi.fn(),
    removeUserFromTenant: vi.fn(),
    addGroupToTenant: vi.fn(),
    removeGroupFromTenant: vi.fn()
  }
}))

const tenant = { id: 't-1', name: 'Acme' }

beforeEach(() => {
  vi.clearAllMocks()
})

createStoreTestSuite('TenantStore', TenantStore, {
  initialState: (getState) => {
    it('should start with no tenants', () => {
      expect(getState().tenants).toEqual([])
    })
  },

  mutations: {
    setTenants: (mutation, getState) => {
      it('should replace the tenant list', () => {
        const state = getState()
        state.tenants = [{ id: 'old' }]

        mutation(state, [tenant])

        expect(state.tenants).toEqual([tenant])
      })
    }
  },

  getters: {
    tenants: (getter, getState) => {
      it('should expose the tenant list', () => {
        const state = getState()
        state.tenants = [tenant]
        expect(getter(state)).toEqual([tenant])
      })
    }
  },

  actions: {
    fetchTenants: (action, getContext) => {
      it('should fetch, commit and return the tenants', async () => {
        const context = getContext()
        TenantService.getTenants.mockResolvedValue([tenant])

        const result = await action(context)

        expect(TenantService.getTenants).toHaveBeenCalledWith()
        expect(context.commit).toHaveBeenCalledWith('setTenants', [tenant])
        expect(result).toEqual([tenant])
      })
    },

    deleteTenant: (action, getContext) => {
      // Deleting must refresh the list, otherwise the removed tenant lingers in the UI.
      it('should delete the tenant then re-fetch the list', async () => {
        const context = getContext()
        TenantService.deleteTenant.mockResolvedValue(undefined)

        await action(context, 't-1')

        expect(TenantService.deleteTenant).toHaveBeenCalledWith('t-1')
        expect(context.dispatch).toHaveBeenCalledWith('fetchTenants')
      })

      it('should not re-fetch when the deletion fails', async () => {
        const context = getContext()
        TenantService.deleteTenant.mockRejectedValue(new Error('Conflict'))

        await expect(action(context, 't-1')).rejects.toThrow('Conflict')
        expect(context.dispatch).not.toHaveBeenCalled()
      })
    },

    getTenantsByUser: (action, getContext) => {
      it('should query tenants filtered by user membership', async () => {
        TenantService.getTenants.mockResolvedValue([tenant])

        const result = await action(getContext(), 'demo')

        expect(TenantService.getTenants).toHaveBeenCalledWith({ userMember: 'demo' })
        expect(result).toEqual([tenant])
      })
    },

    getTenantsByGroup: (action, getContext) => {
      it('should query tenants filtered by group membership', async () => {
        TenantService.getTenants.mockResolvedValue([tenant])

        const result = await action(getContext(), 'sales')

        expect(TenantService.getTenants).toHaveBeenCalledWith({ groupMember: 'sales' })
        expect(result).toEqual([tenant])
      })
    }
  },

  additional: (storeModule) => {
    // The remaining actions are thin pass-throughs; a table keeps them honest without a
    // dozen near-identical blocks. Each row asserts the service call and the return value.
    describe('pass-through actions', () => {
      const cases = [
        ['getTenantById', 't-1', 'getTenantById', ['t-1'], tenant],
        ['updateTenant', tenant, 'updateTenant', [tenant], tenant],
        ['removeUserFromTenant', { tenantId: 't-1', userId: 'demo' }, 'removeUserFromTenant', ['t-1', 'demo'], undefined],
        ['addUserToTenant', { tenantId: 't-1', userId: 'demo' }, 'addUserToTenant', ['t-1', 'demo'], undefined],
        ['removeGroupFromTenant', { tenantId: 't-1', groupId: 'sales' }, 'removeGroupFromTenant', ['t-1', 'sales'], undefined],
        ['addGroupToTenant', { tenantId: 't-1', groupId: 'sales' }, 'addGroupToTenant', ['t-1', 'sales'], undefined]
      ]

      it.each(cases)('%s should call TenantService.%s and return its result', async (actionName, payload, serviceMethod, expectedArgs, resolved) => {
        TenantService[serviceMethod].mockResolvedValue(resolved)

        const result = await storeModule.actions[actionName]({ commit: vi.fn(), dispatch: vi.fn() }, payload)

        expect(TenantService[serviceMethod]).toHaveBeenCalledWith(...expectedArgs)
        expect(result).toEqual(resolved)
      })

      // createTenant deliberately swallows the service's return value.
      it('createTenant should call the service and resolve to undefined', async () => {
        TenantService.createTenant.mockResolvedValue(tenant)

        const result = await storeModule.actions.createTenant({ commit: vi.fn() }, tenant)

        expect(TenantService.createTenant).toHaveBeenCalledWith(tenant)
        expect(result).toBeUndefined()
      })
    })
  }
})
