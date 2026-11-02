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
import { AdminService, SetupService } from '@/services.js'
import ProfileUserInformationTab from '@/components/admin/profile/ProfileUserInformationTab.vue'
import ProfileUserPasswordTab from '@/components/admin/profile/ProfileUserPasswordTab.vue'
import ProfileUserGroupsTab from '@/components/admin/profile/ProfileUserGroupsTab.vue'
import ProfileUserTenantsTab from '@/components/admin/profile/ProfileUserTenantsTab.vue'
import ProfileGroupInformationTab from '@/components/admin/profile/ProfileGroupInformationTab.vue'
import ProfileGroupUsersTab from '@/components/admin/profile/ProfileGroupUsersTab.vue'
import ProfileGroupTenantsTab from '@/components/admin/profile/ProfileGroupTenantsTab.vue'
import { createComponentContext } from '../support/callWithContext.js'

vi.mock('@/services.js', () => ({
  AdminService: {
    updateUserProfile: vi.fn(() => Promise.resolve()),
    updateUserCredentials: vi.fn(() => Promise.resolve()),
    findGroups: vi.fn(() => Promise.resolve([])),
    findUsers: vi.fn(() => Promise.resolve([])),
    addMember: vi.fn(() => Promise.resolve()),
    deleteMember: vi.fn(() => Promise.resolve()),
    updateGroup: vi.fn(() => Promise.resolve()),
    deleteGroup: vi.fn(() => Promise.resolve())
  },
  SetupService: { validatePasswordPolicy: vi.fn() }
}))

const ADA = { id: 'ada', firstName: 'Ada', lastName: 'Lovelace' }
const SALES = { id: 'sales', name: 'Sales' }

const alert = () => ({ show: vi.fn() })
const settle = async () => { for (let i = 0; i < 5; i++) await Promise.resolve() }

/** A stand-in for the tab, given its props and the alerts and dialogs it reaches through $refs. */
function context(Component, overrides = {}) {
  const vm = createComponentContext({
    ...Component.data.call({}),
    ...overrides
  })
  vm.$root.config = { userEditable: true, ...overrides.config }
  for (const [name, method] of Object.entries(Component.methods ?? {})) {
    if (!(name in vm)) vm[name] = method.bind(vm)
  }
  for (const [name, def] of Object.entries(Component.computed ?? {})) {
    if (!(name in vm) && typeof def === 'function') Object.defineProperty(vm, name, { get: () => def.call(vm), configurable: true })
  }
  return vm
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('ProfileUserInformationTab', () => {
  it('saves the user and says so', async () => {
    const vm = context(ProfileUserInformationTab, { user: { ...ADA }, $refs: { updateProfile: alert() } })
    vm.update()
    await settle()
    expect(AdminService.updateUserProfile).toHaveBeenCalledWith('ada', vm.user)
    expect(vm.$refs.updateProfile.show).toHaveBeenCalledWith(2)
  })
})

describe('ProfileUserPasswordTab', () => {
  const tab = (overrides = {}) => context(ProfileUserPasswordTab, {
    user: { ...ADA },
    AuthService: { passwordRecover: vi.fn(() => Promise.resolve()) },
    $refs: { updatePassword: alert(), emailSent: alert() },
    ...overrides
  })
  const event = () => ({ preventDefault: vi.fn() })

  it('changes the password when the new one is repeated and the current one given', async () => {
    const vm = tab()
    vm.credentials = { authenticatedUserPassword: 'old', password: 'new' }
    vm.passwordRepeat = 'new'
    vm.changePassword(event())
    await settle()
    expect(AdminService.updateUserCredentials).toHaveBeenCalledWith('ada', 'new', 'old')
    expect(vm.$refs.updatePassword.show).toHaveBeenCalledWith(2)
  })

  it('does not change the password when the repetition differs', () => {
    const vm = tab()
    vm.credentials = { authenticatedUserPassword: 'old', password: 'new' }
    vm.passwordRepeat = 'other'
    vm.changePassword(event())
    expect(AdminService.updateUserCredentials).not.toHaveBeenCalled()
  })

  it('notes a password the policy refuses', async () => {
    AdminService.updateUserCredentials.mockRejectedValueOnce({ response: { data: { type: 'PasswordPolicyException' } } })
    const vm = tab()
    vm.credentials = { authenticatedUserPassword: 'old', password: 'new' }
    vm.passwordRepeat = 'new'
    vm.changePassword(event())
    await settle()
    expect(vm.passwordPolicyError).toBe(true)
  })

  it('checks the new password against the policy and lists the rules it breaks', async () => {
    SetupService.validatePasswordPolicy.mockResolvedValue({ data: { valid: false, rules: [{ placeholder: 'length', valid: false }, { placeholder: 'digit', valid: true }] } })
    const vm = tab()
    vm.credentials.password = 'short'
    vm.validatePassword()
    await settle()
    expect(SetupService.validatePasswordPolicy).toHaveBeenCalledWith('short', vm.user)
    expect(vm.passwordValid).toBe(false)
    expect(vm.invalidPasswordRules).toEqual([{ placeholder: 'length', valid: false }])

    vm.resetPasswordValidation()
    expect(vm.passwordValid).toBeNull()
    expect(vm.invalidPasswordRules).toEqual([])
  })

  it.each([
    ['sent', () => Promise.resolve(), true],
    ['failed', () => Promise.reject(new Error('down')), false]
  ])('sends a recovery email and ends waiting once it is %s', async (_, outcome, shown) => {
    const vm = tab({ AuthService: { passwordRecover: vi.fn(outcome) } })
    vm.onSendEmail()
    expect(vm.sendingEmail).toBe(true)
    await settle()
    expect(vm.AuthService.passwordRecover).toHaveBeenCalledWith({ id: 'ada' })
    expect(vm.sendingEmail).toBe(false)
    expect(vm.$refs.emailSent.show.mock.calls.length > 0).toBe(shown)
  })
})

describe('ProfileUserGroupsTab', () => {
  const tab = (overrides = {}) => context(ProfileUserGroupsTab, {
    user: { ...ADA }, editMode: true, $refs: { unassignGroup: alert(), assignGroupsModal: { show: vi.fn() } }, ...overrides
  })

  it('loads the groups of the user when it opens', async () => {
    AdminService.findGroups.mockResolvedValue([SALES])
    const vm = tab()
    ProfileUserGroupsTab.created.call(vm)
    await settle()
    expect(AdminService.findGroups).toHaveBeenCalledWith({ member: 'ada' })
    expect(vm.groups).toEqual([SALES])
  })

  it.each([
    [true, true, ['id', 'name', 'type', 'actions']],
    [false, true, ['id', 'name', 'type']],
    [true, false, ['id', 'name', 'type']]
  ])('in edit mode %s with editable users %s, shows the columns %j', (editMode, userEditable, keys) => {
    const vm = tab({ editMode, config: { userEditable } })
    expect(vm.groupFields.map(field => field.key)).toEqual(keys)
  })

  it('takes the user out of a group and reloads the groups', async () => {
    const vm = tab()
    vm.unassignGroup(SALES)
    await settle()
    expect(AdminService.deleteMember).toHaveBeenCalledWith('sales', 'ada')
    expect(vm.$refs.unassignGroup.show).toHaveBeenCalledWith(2)
    expect(AdminService.findGroups).toHaveBeenCalledWith({ member: 'ada' })
  })

  it('offers the groups the user is not in, and adds the user to the chosen ones', async () => {
    AdminService.findGroups.mockResolvedValue([{ id: 'sales' }, { id: 'hr' }])
    const vm = tab({ groups: [{ id: 'sales' }] })
    vm.openAssignGroupModal()
    expect(vm.$refs.assignGroupsModal.show).toHaveBeenCalled()
    await settle()
    expect(vm.unAssignedGroups).toEqual([{ id: 'hr', selected: false }])
    expect(vm.unAssignedGroupsLoading).toBe(false)

    vm.unAssignedGroups[0].selected = true
    vm.assignGroups()
    await settle()
    expect(AdminService.addMember).toHaveBeenCalledWith('hr', 'ada')
    expect(vm.groups.map(group => group.id)).toEqual(['sales', 'hr'])
  })
})

describe('ProfileUserTenantsTab', () => {
  const tab = (overrides = {}) => context(ProfileUserTenantsTab, {
    user: { ...ADA }, editMode: true,
    tenants: [{ id: 't1' }, { id: 't2' }],
    fetchTenants: vi.fn(() => Promise.resolve()),
    getTenantsByUser: vi.fn(() => Promise.resolve([{ id: 't1' }])),
    addUserToTenant: vi.fn(() => Promise.resolve()),
    removeUserFromTenant: vi.fn(() => Promise.resolve()),
    $refs: { unassignTenant: alert(), assignTenantsModal: { show: vi.fn() } },
    ...overrides
  })

  it('loads the tenants of the user when it opens', async () => {
    const vm = tab()
    ProfileUserTenantsTab.created.call(vm)
    await settle()
    expect(vm.getTenantsByUser).toHaveBeenCalledWith('ada')
    expect(vm.userTenants).toEqual([{ id: 't1' }])
  })

  it.each([
    [true, ['id', 'name', 'actions']],
    [false, ['id', 'name']]
  ])('in edit mode %s, shows the columns %j', (editMode, keys) => {
    expect(tab({ editMode }).tenantFields.map(field => field.key)).toEqual(keys)
  })

  it('takes the user out of a tenant and reloads the tenants', async () => {
    const vm = tab()
    vm.unassignTenant({ id: 't1' })
    await settle()
    expect(vm.removeUserFromTenant).toHaveBeenCalledWith({ tenantId: 't1', userId: 'ada' })
    expect(vm.$refs.unassignTenant.show).toHaveBeenCalledWith(2)
    expect(vm.getTenantsByUser).toHaveBeenCalledWith('ada')
  })

  it('offers the tenants the user is not in, and adds the user to the chosen ones', async () => {
    const vm = tab({ userTenants: [{ id: 't1' }] })
    vm.openAssignTenantModal()
    expect(vm.$refs.assignTenantsModal.show).toHaveBeenCalled()
    await settle()
    expect(vm.unassignedTenants).toEqual([{ id: 't2', selected: false }])

    vm.unassignedTenants[0].selected = true
    vm.assignTenants()
    await settle()
    expect(vm.addUserToTenant).toHaveBeenCalledWith({ tenantId: 't2', userId: 'ada' })
    expect(vm.userTenants.map(tenant => tenant.id)).toEqual(['t1', 't2'])
  })
})

describe('ProfileGroupInformationTab', () => {
  const tab = () => context(ProfileGroupInformationTab, { group: { ...SALES }, $refs: { updateGroup: alert(), deleteGroup: alert() } })

  it('saves the group and says so', async () => {
    const vm = tab()
    vm.update()
    await settle()
    expect(AdminService.updateGroup).toHaveBeenCalledWith('sales', vm.group)
    expect(vm.$refs.updateGroup.show).toHaveBeenCalledWith(2)
  })

  it('deletes the group and goes back to the list', async () => {
    const vm = tab()
    vm.deleteGroup()
    await settle()
    expect(AdminService.deleteGroup).toHaveBeenCalledWith('sales')
    expect(vm.$router.push).toHaveBeenCalledWith({ name: 'adminGroups' })
  })
})

describe('ProfileGroupUsersTab', () => {
  it('loads the members of the group when it opens', async () => {
    AdminService.findUsers.mockResolvedValue([ADA])
    const vm = context(ProfileGroupUsersTab, { group: { ...SALES } })
    ProfileGroupUsersTab.created.call(vm)
    await settle()
    expect(AdminService.findUsers).toHaveBeenCalledWith({ memberOfGroup: 'sales' })
    expect(vm.users).toEqual([ADA])
  })
})

describe('ProfileGroupTenantsTab', () => {
  const tab = (overrides = {}) => context(ProfileGroupTenantsTab, {
    group: { ...SALES },
    tenants: [{ id: 't1' }, { id: 't2' }],
    fetchTenants: vi.fn(() => Promise.resolve()),
    getTenantsByGroup: vi.fn(() => Promise.resolve([{ id: 't1' }])),
    addGroupToTenant: vi.fn(() => Promise.resolve()),
    removeGroupFromTenant: vi.fn(() => Promise.resolve()),
    $refs: { unassignTenant: alert(), assignTenantsModal: { show: vi.fn() } },
    ...overrides
  })

  it('loads the tenants of the group when it opens', async () => {
    const vm = tab()
    ProfileGroupTenantsTab.created.call(vm)
    await settle()
    expect(vm.getTenantsByGroup).toHaveBeenCalledWith('sales')
    expect(vm.groupTenants).toEqual([{ id: 't1' }])
  })

  it('takes the group out of a tenant and reloads the tenants', async () => {
    const vm = tab()
    vm.unassignTenant({ id: 't1' })
    await settle()
    expect(vm.removeGroupFromTenant).toHaveBeenCalledWith({ tenantId: 't1', groupId: 'sales' })
    expect(vm.$refs.unassignTenant.show).toHaveBeenCalledWith(2)
  })

  it('offers the tenants the group is not in, and adds the group to the chosen ones', async () => {
    const vm = tab({ groupTenants: [{ id: 't1' }] })
    vm.openAssignTenantModal()
    await settle()
    expect(vm.unassignedTenants).toEqual([{ id: 't2', selected: false }])

    vm.unassignedTenants[0].selected = true
    vm.assignTenants()
    await settle()
    expect(vm.addGroupToTenant).toHaveBeenCalledWith({ tenantId: 't2', groupId: 'sales' })
    expect(vm.groupTenants.map(tenant => tenant.id)).toEqual(['t1', 't2'])
  })
})
