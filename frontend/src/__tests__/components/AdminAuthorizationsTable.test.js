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
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { AdminService } from '@/services.js'
import AdminAuthorizationsTable from '@/components/admin/AdminAuthorizationsTable.vue'

vi.mock('@/services.js', () => ({
  AdminService: {
    createAuthorization: vi.fn(() => Promise.resolve({ id: '42' })),
    updateAuthorization: vi.fn(() => Promise.resolve()),
    deleteAuthorization: vi.fn(() => Promise.resolve()),
    findAuthorizations: vi.fn(() => Promise.resolve([]))
  }
}))

const { isGlobal, isAllow, setType, applyGlobalIdentity, save, prepareEdit, reloadAfterRejectedSave,
  hasConflictingGlobal, persistAuthorization, cancelEdit } = AdminAuthorizationsTable.methods

const GLOBAL = 0
const ALLOW = 1
const DENY = 2

// Minimal component context: save()/setType() only touch these members.
function context(overrides = {}) {
  return {
    isUserToEdit: true,
    selected: ['READ'],
    authorizations: [],
    resourcesTypes: { '0': { permissions: ['READ', 'UPDATE'] } },
    $route: { params: { resourceTypeId: '0' } },
    cancelEdit: vi.fn(),
    reloadAfterRejectedSave: vi.fn(),
    loadAuthorizations: vi.fn(),
    $root: { $refs: { error: { show: vi.fn() } } },
    isGlobal,
    isAllow,
    applyGlobalIdentity,
    hasConflictingGlobal,
    persistAuthorization,
    ...overrides
  }
}

function authorization(overrides = {}) {
  return { id: '0', type: ALLOW, permissions: [], userId: null, groupId: null, resourceType: '0', resourceId: '*', ...overrides }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('authorization type helpers', () => {
  it.each([
    [GLOBAL, true],
    [ALLOW, false],
    [DENY, false],
    [null, false],
    [undefined, false]
  ])('isGlobal({ type: %j }) => %j', (type, expected) => {
    expect(isGlobal({ type })).toBe(expected)
  })

  it.each([
    [ALLOW, true],
    [GLOBAL, false],
    [DENY, false],
    [null, false],
    [undefined, false]
  ])('isAllow({ type: %j }) => %j', (type, expected) => {
    expect(isAllow({ type })).toBe(expected)
  })
})

describe('applyGlobalIdentity', () => {
  it.each([
    [{ userId: null, groupId: 'sales' }],
    [{ userId: 'demo', groupId: null }],
    [{ userId: null, groupId: null }]
  ])('forces the identity to the user "*" regardless of the starting values %j', (start) => {
    const ctx = context({ isUserToEdit: false })
    const auth = authorization(start)

    applyGlobalIdentity.call(ctx, auth)

    expect(auth.userId).toBe('*')
    expect(auth.groupId).toBeNull()
    expect(auth.userIdGroupId).toBe('*')
    expect(ctx.isUserToEdit).toBe(true)
  })
})

describe('setType', () => {
  it('locks a GLOBAL row to the user "*" without a group', () => {
    const ctx = context({ isUserToEdit: false })
    const auth = authorization({ groupId: 'sales' })

    setType.call(ctx, auth, GLOBAL)

    expect(auth.type).toBe(GLOBAL)
    expect(auth.userId).toBe('*')
    expect(auth.groupId).toBeNull()
    expect(auth.userIdGroupId).toBe('*')
    expect(ctx.isUserToEdit).toBe(true)
  })

  it.each([
    [ALLOW],
    [DENY]
  ])('clears the identity forced by GLOBAL when type %j is selected', (targetType) => {
    const ctx = context()
    const auth = authorization({ type: GLOBAL, userId: '*', userIdGroupId: '*' })

    setType.call(ctx, auth, targetType)

    expect(auth.type).toBe(targetType)
    expect(auth.userId).toBeNull()
    expect(auth.groupId).toBeNull()
    expect(auth.userIdGroupId).toBeNull()
  })

  it.each([
    [ALLOW, DENY],
    [DENY, ALLOW],
    [ALLOW, ALLOW]
  ])('keeps the entered identity when switching from %j to %j', (fromType, toType) => {
    const ctx = context()
    const auth = authorization({ type: fromType, userId: 'demo' })

    setType.call(ctx, auth, toType)

    expect(auth.type).toBe(toType)
    expect(auth.userId).toBe('demo')
    expect(auth.groupId).toBeNull()
  })

  it('leaves a group identity alone when switching between non-GLOBAL types', () => {
    const ctx = context({ isUserToEdit: false })
    const auth = authorization({ type: ALLOW, groupId: 'sales' })

    setType.call(ctx, auth, DENY)

    expect(auth.type).toBe(DENY)
    expect(auth.groupId).toBe('sales')
    expect(auth.userId).toBeNull()
  })
})

describe('prepareEdit', () => {
  it('normalizes a GLOBAL row that carries a group', () => {
    const auth = authorization({ id: '7', type: 0, userId: null, groupId: 'sales', permissions: ['READ'] })
    const ctx = context({ authorizations: [auth] })

    prepareEdit.call(ctx, auth)

    expect(auth.userId).toBe('*')
    expect(auth.groupId).toBeNull()
    expect(ctx.isUserToEdit).toBe(true)
  })

  it.each([
    [[], []],
    [['ALL'], ['READ', 'UPDATE']],
    [['NONE'], []],
    [['READ'], ['READ']],
    [['READ', 'UPDATE'], ['READ', 'UPDATE']]
  ])('expands stored permissions %j into the selected list %j', (stored, expectedSelected) => {
    const auth = authorization({ id: '7', permissions: stored })
    const ctx = context({ authorizations: [auth], selected: [] })

    prepareEdit.call(ctx, auth)

    expect(ctx.selected).toEqual(expectedSelected)
  })

  it.each([
    ['demo', null, true],
    [null, 'sales', false]
  ])('sets isUserToEdit from the stored identity (userId=%j, groupId=%j) => %j', (userId, groupId, expected) => {
    const auth = authorization({ id: '7', userId, groupId })
    const ctx = context({ authorizations: [auth], isUserToEdit: !expected })

    prepareEdit.call(ctx, auth)

    expect(ctx.isUserToEdit).toBe(expected)
  })

  it('drops an unfinished new row before editing another one', () => {
    const unfinished = authorization({ id: '0' })
    const auth = authorization({ id: '7' })
    const ctx = context({ authorizations: [unfinished, auth] })

    prepareEdit.call(ctx, auth)

    expect(ctx.authorizations).toEqual([auth])
  })

  it('keeps a backup of the stored row, including a copy of its permissions array', () => {
    const auth = authorization({ id: '7', permissions: ['READ'] })
    const ctx = context({ authorizations: [auth] })

    prepareEdit.call(ctx, auth)

    expect(ctx.editBackup.permissions).toEqual(['READ'])
    expect(ctx.editBackup.permissions).not.toBe(auth.permissions)
  })
})

describe('reloadAfterRejectedSave', () => {
  it('drops the edit state and reloads from the engine', () => {
    const ctx = context({ edit: '0', selected: ['READ'], authorizationSelected: {}, firstResult: 40 })

    reloadAfterRejectedSave.call(ctx)

    expect(ctx.edit).toBeNull()
    expect(ctx.selected).toEqual([])
    expect(ctx.authorizationSelected).toBeNull()
    expect(ctx.firstResult).toBe(0)
    expect(ctx.loadAuthorizations).toHaveBeenCalledWith('0')
  })
})

describe('save', () => {
  it('stores an ALLOW for the user "*" as GLOBAL', async () => {
    const ctx = context()
    const auth = authorization({ userId: '*' })

    await save.call(ctx, auth)

    expect(auth.type).toBe(GLOBAL)
    expect(auth.groupId).toBeNull()
    expect(AdminService.createAuthorization).toHaveBeenCalledWith(expect.objectContaining({ type: GLOBAL, userId: '*' }))
  })

  it('replaces an existing ALLOW row instead of updating it, because the engine keeps the type', async () => {
    const ctx = context()
    const auth = authorization({ id: '7', type: 1, userId: '*' })

    await save.call(ctx, auth)

    expect(auth.type).toBe(GLOBAL)
    expect(AdminService.updateAuthorization).not.toHaveBeenCalled()
    expect(AdminService.createAuthorization).toHaveBeenCalledWith(expect.objectContaining({ type: GLOBAL, userId: '*' }))
    expect(AdminService.deleteAuthorization).toHaveBeenCalledWith('7')
    expect(auth.id).toBe('42')
  })

  it('updates an existing row in place when nothing was converted', async () => {
    const ctx = context()
    const auth = authorization({ id: '7', type: 1, userId: 'demo' })

    await save.call(ctx, auth)

    expect(AdminService.updateAuthorization).toHaveBeenCalledWith('7', expect.objectContaining({ type: 1 }))
    expect(AdminService.deleteAuthorization).not.toHaveBeenCalled()
  })

  it('keeps the old row when the replacing create is rejected', async () => {
    AdminService.createAuthorization.mockRejectedValueOnce(new Error('duplicate'))
    const ctx = context()
    const auth = authorization({ id: '7', type: 1, userId: '*' })

    await save.call(ctx, auth)

    expect(AdminService.deleteAuthorization).not.toHaveBeenCalled()
    expect(ctx.reloadAfterRejectedSave).toHaveBeenCalled()
  })

  it('reloads the table when a create is rejected, so no unsaved row is left behind', async () => {
    AdminService.createAuthorization.mockRejectedValueOnce(new Error('duplicate'))
    const ctx = context()

    await save.call(ctx, authorization({ userId: '*' }))

    expect(ctx.reloadAfterRejectedSave).toHaveBeenCalled()
  })

  it('leaves an ALLOW for a concrete user untouched', () => {
    const ctx = context()
    const auth = authorization({ userId: 'demo' })

    save.call(ctx, auth)

    expect(auth.type).toBe(ALLOW)
    expect(AdminService.createAuthorization).toHaveBeenCalledWith(expect.objectContaining({ type: ALLOW, userId: 'demo' }))
  })

  it('leaves an ALLOW for a group named "*" untouched, only the user "*" is global', () => {
    const ctx = context({ isUserToEdit: false })
    const auth = authorization({ groupId: '*' })

    save.call(ctx, auth)

    expect(auth.type).toBe(ALLOW)
    expect(auth.groupId).toBe('*')
    expect(auth.userId).toBeNull()
  })

  it('leaves a DENY for the user "*" untouched', () => {
    const ctx = context()
    const auth = authorization({ type: DENY, userId: '*' })

    save.call(ctx, auth)

    expect(auth.type).toBe(DENY)
  })

  it.each([
    [[], ['NONE']],
    [['READ', 'UPDATE'], ['ALL']],
    [['READ'], ['READ']]
  ])('maps the selected permissions %j to the stored value %j', (selected, expectedPermissions) => {
    const ctx = context({ selected })
    const auth = authorization({ userId: 'demo' })

    save.call(ctx, auth)

    expect(auth.permissions).toEqual(expectedPermissions)
  })

  it.each([
    ['demo', null, true, 'userId', 'demo'],
    [null, 'sales', false, 'groupId', 'sales']
  ])('builds userIdGroupId from whichever identity is set (userId=%j, groupId=%j, isUserToEdit=%j)', (userId, groupId, isUserToEdit) => {
    const ctx = context({ isUserToEdit })
    const auth = authorization({ userId, groupId })

    save.call(ctx, auth)

    expect(auth.userIdGroupId).toBe(userId != null ? userId : groupId)
  })

  it('moves a typed group into userId when isUserToEdit is true but userId was left empty', () => {
    const ctx = context({ isUserToEdit: true })
    const auth = authorization({ userId: null, groupId: 'sales' })

    save.call(ctx, auth)

    expect(auth.userId).toBe('sales')
    expect(auth.groupId).toBeNull()
  })

  it('moves a typed user into groupId when isUserToEdit is false but groupId was left empty', () => {
    const ctx = context({ isUserToEdit: false })
    const auth = authorization({ userId: 'demo', groupId: null })

    save.call(ctx, auth)

    expect(auth.groupId).toBe('demo')
    expect(auth.userId).toBeNull()
  })
})

describe('a GLOBAL authorization for the resource already exists', () => {
  function storedGlobal(resourceId) {
    return { id: 'existing', type: 0, permissions: ['ALL'], userId: '*', groupId: null, resourceType: 0, resourceId }
  }

  it('reports the conflict instead of letting the engine reject the duplicate', async () => {
    const auth = authorization({ id: '7', type: 1, userId: '*', resourceId: 'reports' })
    const ctx = context({ authorizations: [storedGlobal('reports'), auth] })

    await save.call(ctx, auth)

    expect(ctx.$root.$refs.error.show).toHaveBeenCalledWith({ type: 'globalAuthorizationExists', params: ['reports'] })
    expect(AdminService.createAuthorization).not.toHaveBeenCalled()
    expect(AdminService.deleteAuthorization).not.toHaveBeenCalled()
    expect(AdminService.updateAuthorization).not.toHaveBeenCalled()
  })

  it('checks the loaded rows without asking the engine', async () => {
    const auth = authorization({ id: '7', type: 1, userId: '*', resourceId: 'reports' })
    const ctx = context({ authorizations: [storedGlobal('reports'), auth] })

    await save.call(ctx, auth)

    expect(AdminService.findAuthorizations).not.toHaveBeenCalled()
  })

  it('keeps the blocked row showing the ALLOW that was entered', async () => {
    const auth = authorization({ id: '7', type: 1, userId: '*', resourceId: 'reports' })
    const ctx = context({ authorizations: [storedGlobal('reports'), auth] })

    await save.call(ctx, auth)

    expect(auth.type).toBe(ALLOW)
    expect(auth.id).toBe('7')
  })

  it('blocks a new row entered as GLOBAL for that resource as well', async () => {
    const auth = authorization({ type: GLOBAL, userId: '*', resourceId: 'reports' })
    const ctx = context({ authorizations: [auth, storedGlobal('reports')] })

    await save.call(ctx, auth)

    expect(ctx.$root.$refs.error.show).toHaveBeenCalled()
    expect(AdminService.createAuthorization).not.toHaveBeenCalled()
    expect(auth.type).toBe(GLOBAL)
  })

  it('saves the GLOBAL row whose only match for the resource is itself', async () => {
    const auth = authorization({ id: 'existing', type: 0, userId: '*', resourceId: 'reports' })
    const ctx = context({ authorizations: [auth] })

    await save.call(ctx, auth)

    expect(ctx.$root.$refs.error.show).not.toHaveBeenCalled()
    expect(AdminService.updateAuthorization).toHaveBeenCalledWith('existing', expect.objectContaining({ type: 0 }))
  })

  it.each([
    ['invoices', 'reports'],
    ['reports ', 'reports'],
    ['REPORTS', 'reports']
  ])('does not treat resourceId %j as a match for the stored GLOBAL resourceId %j', async (resourceId, storedResourceId) => {
    const auth = authorization({ id: '7', type: 1, userId: '*', resourceId })
    const ctx = context({ authorizations: [storedGlobal(storedResourceId), auth] })

    await save.call(ctx, auth)

    expect(ctx.$root.$refs.error.show).not.toHaveBeenCalled()
    expect(AdminService.createAuthorization).toHaveBeenCalled()
  })

  it('leaves a conflict with a row that was never loaded to the engine', async () => {
    const auth = authorization({ id: '7', type: 1, userId: '*', resourceId: 'reports' })
    const ctx = context({ authorizations: [auth] })

    await save.call(ctx, auth)

    expect(ctx.$root.$refs.error.show).not.toHaveBeenCalled()
    expect(AdminService.createAuthorization).toHaveBeenCalled()
  })

  it('ignores a stored GLOBAL row for the same resource that is DENY, not GLOBAL type', async () => {
    const auth = authorization({ id: '7', type: 1, userId: '*', resourceId: 'reports' })
    const deny = { id: 'other', type: DENY, permissions: ['ALL'], userId: '*', groupId: null, resourceType: 0, resourceId: 'reports' }
    const ctx = context({ authorizations: [deny, auth] })

    await save.call(ctx, auth)

    expect(ctx.$root.$refs.error.show).not.toHaveBeenCalled()
    expect(AdminService.createAuthorization).toHaveBeenCalled()
  })
})

describe('hasConflictingGlobal', () => {
  it('returns false when the authorization has no resourceId yet', () => {
    const auth = authorization({ resourceId: null })
    const ctx = context({ authorizations: [auth] })

    expect(hasConflictingGlobal.call(ctx, auth)).toBe(false)
  })

  it('does not compare the row against itself', () => {
    const auth = authorization({ id: '7', type: GLOBAL, userId: '*', resourceId: 'reports' })
    const ctx = context({ authorizations: [auth] })

    expect(hasConflictingGlobal.call(ctx, auth)).toBe(false)
  })
})

describe('discarding an edit', () => {
  // prepareEdit()/cancelEdit() work on the row itself, so they need the real implementations
  function editing(auth, alsoLoaded = []) {
    const ctx = context({ authorizations: [auth, ...alsoLoaded], cancelEdit, editBackup: null })
    prepareEdit.call(ctx, auth)
    return ctx
  }

  it('puts the stored row back when the input was never persisted', () => {
    const auth = authorization({ id: '7', type: 1, userId: 'tester', permissions: ['READ'], resourceId: '*' })
    const ctx = editing(auth)

    // what the editor writes into the row while typing
    auth.userId = 'typo'
    auth.resourceId = 'oops'
    cancelEdit.call(ctx, auth)

    expect(auth.userId).toBe('tester')
    expect(auth.resourceId).toBe('*')
    expect(auth.permissions).toEqual(['READ'])
    expect(ctx.editBackup).toBeNull()
  })

  it('puts the stored ALLOW back after a save that was blocked by the GLOBAL conflict', async () => {
    const auth = authorization({ id: '7', type: 1, userId: 'tester', permissions: ['ALL'], resourceId: '*' })
    const ctx = editing(auth, [{ id: 'existing', type: 0, permissions: ['ALL'], userId: '*', groupId: null, resourceType: 0, resourceId: '*' }])

    auth.userId = '*'
    await save.call(ctx, auth)
    expect(ctx.$root.$refs.error.show).toHaveBeenCalled()

    cancelEdit.call(ctx, auth)

    expect(auth.userId).toBe('tester')
    expect(auth.type).toBe(1)
  })

  it('keeps the saved values when cancelEdit runs after a successful update', async () => {
    const auth = authorization({ id: '7', type: 1, userId: 'tester', permissions: ['ALL'], resourceId: '*' })
    const ctx = editing(auth)

    auth.userId = 'someone-else'
    await save.call(ctx, auth)

    expect(AdminService.updateAuthorization).toHaveBeenCalled()
    expect(auth.userId).toBe('someone-else')
  })

  it('still drops a new row that was never saved', () => {
    const auth = authorization({ id: '0', userId: 'tester' })
    const ctx = context({ authorizations: [auth], authorizationSelected: auth, cancelEdit, editBackup: null })

    cancelEdit.call(ctx, auth)

    expect(ctx.authorizations).toHaveLength(0)
  })

  it('resets the selected permissions and edit state regardless of the row being discarded', () => {
    const auth = authorization({ id: '7', permissions: ['READ'] })
    const ctx = editing(auth)
    ctx.edit = '7'
    ctx.selected = ['READ', 'UPDATE']

    cancelEdit.call(ctx, auth)

    expect(ctx.edit).toBeNull()
    expect(ctx.selected).toEqual([])
    expect(ctx.authorizationSelected).toBeNull()
  })
})

describe('the configured authorization types', () => {
  // admin.types is deployment configuration, its ids have been strings and are now numbers
  function typesFrom(configured) {
    return AdminAuthorizationsTable.data.call({
      $root: { config: { admin: { types: configured, resourcesTypes: {} } } }
    }).types
  }

  it('numbers ids that the configuration carries as strings', () => {
    const types = typesFrom({ '0': { id: '0', key: 'global' }, '1': { id: '1', key: 'allow' } })

    expect(types['0'].id).toBe(GLOBAL)
    expect(types['1'].id).toBe(ALLOW)
  })

  it('leaves ids that are already numbers alone', () => {
    const types = typesFrom({ '0': { id: 0, key: 'global' }, '2': { id: 2, key: 'deny' } })

    expect(types['0'].id).toBe(GLOBAL)
    expect(types['2'].id).toBe(DENY)
    expect(types['0'].key).toBe('global')
  })

  it.each([
    ['0', 'global', GLOBAL],
    ['1', 'allow', ALLOW],
    ['2', 'deny', DENY]
  ])('numbers the configured type %j (%j) to %j regardless of source shape', (key, typeKey, expectedId) => {
    const types = typesFrom({ [key]: { id: key, key: typeKey } })

    expect(types[key].id).toBe(expectedId)
    expect(types[key].key).toBe(typeKey)
  })
})
