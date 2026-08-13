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
import { describe, it, expect } from 'vitest'
import UsersManagement from '@/components/admin/UsersManagement.vue'

function evaluateUsersManagement(overrides = {}) {
  const vm = {
    permissionsUsers: false,
    permissionsUsersManagement: false,
    permissionsGroupsManagement: false,
    permissionsTenantsManagement: false,
    permissionsAuthorizationsManagement: false,
    permissionsSystemManagement: false,
    ...overrides
  }
  Object.defineProperty(vm, 'adminGroup', {
    get: () => UsersManagement.computed.adminGroup.call(vm)
  })
  Object.defineProperty(vm, 'showAccessManagement', {
    get: () => UsersManagement.computed.showAccessManagement.call(vm)
  })
  Object.defineProperty(vm, 'showSystem', {
    get: () => UsersManagement.computed.showSystem.call(vm)
  })
  Object.defineProperty(vm, 'hasTiles', {
    get: () => UsersManagement.computed.hasTiles.call(vm)
  })
  return vm
}

describe('UsersManagement.vue', () => {
  it('shows no tiles when no admin permission is granted', () => {
    const vm = evaluateUsersManagement()
    expect(vm.adminGroup).toBeUndefined()
    expect(vm.showAccessManagement).toBe(false)
    expect(vm.showSystem).toBe(false)
    expect(vm.hasTiles).toBe(false)
  })

  it('shows only the Access-management tile when a single identity permission is granted', () => {
    const vm = evaluateUsersManagement({ permissionsUsers: true, permissionsGroupsManagement: true })
    expect(vm.showAccessManagement).toBe(true)
    expect(vm.showSystem).toBe(false)
    expect(vm.hasTiles).toBe(true)
  })

  it('shows only the System tile when only systemManagement is granted', () => {
    const vm = evaluateUsersManagement({ permissionsUsers: true, permissionsSystemManagement: true })
    expect(vm.showAccessManagement).toBe(false)
    expect(vm.showSystem).toBe(true)
    expect(vm.hasTiles).toBe(true)
  })

  it('shows both tiles when identity and system permissions are granted', () => {
    const vm = evaluateUsersManagement({
      permissionsUsers: true,
      permissionsUsersManagement: true,
      permissionsSystemManagement: true
    })
    expect(vm.showAccessManagement).toBe(true)
    expect(vm.showSystem).toBe(true)
    expect(vm.hasTiles).toBe(true)
  })
})
