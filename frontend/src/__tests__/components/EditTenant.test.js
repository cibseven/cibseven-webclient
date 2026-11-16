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
import { AdminService } from '@/services.js'
import EditTenant from '@/components/tenants/EditTenant.vue'
import { createComponentContext } from '../support/callWithContext.js'

vi.mock('@/services.js', () => ({
  AdminService: {
    findUsers: vi.fn(() => Promise.resolve([])),
    findGroups: vi.fn(() => Promise.resolve([]))
  }
}))

const settle = async () => { for (let i = 0; i < 5; i++) await Promise.resolve() }

/** A stand-in for the page on `route`, loading the tenant through `getTenantById`. */
function page(route, getTenantById) {
  const vm = createComponentContext({
    ...EditTenant.data.call({}),
    $route: { params: {}, query: {}, ...route },
    getTenantById
  })
  for (const [name, method] of Object.entries(EditTenant.methods)) {
    if (!(name in vm)) vm[name] = method.bind(vm)
  }
  return vm
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('EditTenant', () => {
  it('shows the tenant it loaded', async () => {
    const vm = page({ params: { tenantId: 'tenant-a' } }, vi.fn(() => Promise.resolve({ id: 'tenant-a', name: 'Tenant A' })))
    EditTenant.created.call(vm)
    await settle()
    expect(vm.getTenantById).toHaveBeenCalledWith('tenant-a')
    expect(vm.tenant).toEqual({ id: 'tenant-a', name: 'Tenant A' })
    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  it('says that an unknown tenant does not exist and returns to the tenant list (CIB7-2375)', async () => {
    const vm = page({ params: { tenantId: 'missing' } }, vi.fn(() => Promise.reject({ response: { status: 404 } })))
    EditTenant.created.call(vm)
    await settle()
    expect(vm.$root.$refs.error.show).toHaveBeenCalledWith({ type: 'tenantNotFound', params: ['missing'] })
    expect(vm.$router.push).toHaveBeenCalledWith({ name: 'adminTenants' })
  })

  it('leaves other errors to the application error handler', async () => {
    const vm = page({ params: { tenantId: 'tenant-a' } }, vi.fn(() => Promise.reject({ response: { status: 500 } })))
    EditTenant.created.call(vm)
    await settle()
    expect(vm.$root.$refs.error.show).not.toHaveBeenCalled()
    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  it('loads the members of the tenant when it opens on the users tab', async () => {
    const vm = page({ params: { tenantId: 'tenant-a' }, query: { tab: 'users' } }, vi.fn(() => Promise.resolve({ id: 'tenant-a' })))
    EditTenant.created.call(vm)
    await settle()
    expect(AdminService.findUsers).toHaveBeenCalledWith({ memberOfTenant: 'tenant-a' })
  })

  it('loads the groups of the tenant when it opens on the groups tab', async () => {
    const vm = page({ params: { tenantId: 'tenant-a' }, query: { tab: 'groups' } }, vi.fn(() => Promise.resolve({ id: 'tenant-a' })))
    EditTenant.created.call(vm)
    await settle()
    expect(AdminService.findGroups).toHaveBeenCalledWith({ memberOfTenant: 'tenant-a' })
  })
})
