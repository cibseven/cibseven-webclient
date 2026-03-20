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

/**
 * Authorization types matching the CIBseven engine:
 *   0 = GLOBAL  – applies to every user, regardless of userId / groupId
 *   1 = GRANT   – allows the permission for a specific user or group
 *   2 = REVOKE  – denies the permission for a specific user or group
 */
/** @type {0} Authorization entry that applies globally to all users (userId and groupId are null) */
const AUTH_TYPE_GLOBAL = 0
/** @type {1} Authorization entry that grants a permission for a specific user or group */
const AUTH_TYPE_GRANT = 1
/** @type {2} Authorization entry that revokes a permission for a specific user or group */
const AUTH_TYPE_REVOKE = 2

/**
 * Determines whether an authorization entry is relevant for the current permission evaluation.
 * An entry is relevant when it is a global authorization (type 0) or is tied to a specific
 * user (userId set) or group (groupId set).
 *
 * @param {{ type: number, userId: string|null, groupId: string|null }} entry
 * @returns {boolean}
 */
function isAuthorizationEntryRelevant(entry) {
	return entry.type === AUTH_TYPE_GLOBAL || entry.userId !== null || entry.groupId !== null
}

/**
 * Process a list of raw authorization entries for a specific permission check.
 *
 * The engine tracks three authorization types:
 *   - GLOBAL (0): applies unconditionally to all users
 *   - GRANT  (1): allows access for a specific userId or groupId
 *   - REVOKE (2): denies access for a specific userId or groupId
 *
 * An entry is included when:
 *   - its type is GLOBAL (0), regardless of userId/groupId, OR
 *   - its userId is set (user-specific authorization), OR
 *   - its groupId is set (group-specific authorization)
 *
 * @param {Array}  permissionsToHandle  Raw authorization entries from user.permissions[resourceType]
 * @param {Array}  permissionsToCheck   Required permission names (e.g. ['READ', 'UPDATE'])
 * @returns {{ granted: string[], revoked: string[] }}
 */
function getPermissionsProcessed(permissionsToHandle, permissionsToCheck) {
	const result = { granted: [], revoked: [] }
	if (!permissionsToHandle || !permissionsToCheck) return result

	permissionsToHandle.forEach(function(p) {
		// Include global (type 0), user-specific, and group-specific entries.
		// Entries that are neither global nor tied to a user/group are skipped.
		if (!isAuthorizationEntryRelevant(p)) return

		const allPermsIncluded = permissionsToCheck.every(function(v) {
			return p.permissions.includes(v)
		})
		const somePermsIncluded = permissionsToCheck.some(function(v) {
			return p.permissions.includes(v)
		})

		if (p.type === AUTH_TYPE_REVOKE) {
			if (!result.revoked.includes(p.resourceId) &&
				(somePermsIncluded || p.permissions.includes('ALL')))
				result.revoked.push(p.resourceId)
		} else {
			// GLOBAL (0) or GRANT (1)
			if (p.permissions.includes('ALL') || allPermsIncluded) {
				if (!result.granted.includes(p.resourceId))
					result.granted.push(p.resourceId)
			}
		}
	})

	return result
}

/**
 * Build an array of permission-check result objects from a permissions-required
 * config entry and the current user's authorization data.
 *
 * Each key in `permissionsRequired` names a resource type (e.g. 'task', 'filter')
 * and maps to an array of required permission names.  The corresponding entry in
 * `userPermissions` holds the raw authorization entries for that resource type.
 *
 * @param {Object|null} permissionsRequired  e.g. { task: ['READ','UPDATE'] }
 * @param {Object}      userPermissions      e.g. this.$root.user.permissions
 * @returns {Array<{ granted: string[], revoked: string[] }>}
 */
function buildPermissionsChecks(permissionsRequired, userPermissions) {
	if (!permissionsRequired) return []
	return Object.keys(permissionsRequired).map(function(key) {
		return getPermissionsProcessed(userPermissions[key], permissionsRequired[key])
	})
}

/**
 * Check whether a given resource value is permitted by all supplied permission
 * checks (conjunctive – every check must pass).
 *
 * A check passes when the resource value (or the wildcard '*') is in the
 * granted list AND neither the resource value nor the wildcard is in the
 * revoked list.
 *
 * @param {*}       object           The entity to check (or a primitive value when key is null)
 * @param {string|null} key          Property of `object` to use as the resource ID, or null to use object directly
 * @param {Array}   permissionsCheck Result of buildPermissionsChecks()
 * @param {boolean} authorizationEnabled  When false, always returns true
 * @returns {boolean}
 */
function checkPermissionsAllowed(object, key, permissionsCheck, authorizationEnabled) {
	if (!authorizationEnabled) return true
	const val = key ? object[key] : object
	return (permissionsCheck.length > 0) && permissionsCheck.every(function(permission) {
		return (permission.granted.includes(val) || permission.granted.includes('*')) &&
			!permission.revoked.includes(val) && !permission.revoked.includes('*')
	})
}

/**
 * Check whether a given resource value is explicitly denied by any of the
 * supplied permission checks (disjunctive – any denying check is sufficient).
 *
 * @param {*}       object           The entity to check (or a primitive value when key is null)
 * @param {string|null} key          Property of `object` to use as the resource ID, or null to use object directly
 * @param {Array}   permissionsCheck Result of buildPermissionsChecks()
 * @param {boolean} authorizationEnabled  When false, always returns false
 * @returns {boolean}
 */
function checkPermissionsDenied(object, key, permissionsCheck, authorizationEnabled) {
	if (!authorizationEnabled) return false
	const val = key ? object[key] : object
	return permissionsCheck.some(function(permission) {
		return permission.revoked.includes(val) || permission.revoked.includes('*')
	})
}

/**
 * Check whether a given resource is permitted for a particular action, handling
 * both conjunctive (AND) and disjunctive (OR) action permission definitions.
 *
 * @param {Object|Array} actionDef
 *   The action's entry from ACTION_PERMISSIONS.
 *   • Plain object  { resourceType: ['PERM', …] } – conjunctive: ALL resource types must grant.
 *   • Array  [{ resourceType: ['PERM'] }, …]       – disjunctive: ANY alternative must grant.
 * @param {string}  resourceId         The resource identifier to check (e.g. process definition key).
 * @param {Object}  userPermissions    The current user's raw authorization entries, keyed by resource type.
 * @param {boolean} authorizationEnabled  When false every check returns true.
 * @returns {boolean}
 */
function checkActionAllowed(actionDef, resourceId, userPermissions, authorizationEnabled) {
	if (!authorizationEnabled) return true
	if (!actionDef) return false

	if (Array.isArray(actionDef)) {
		// Disjunctive: at least one alternative must grant access
		return actionDef.some(function(alternative) {
			const checks = buildPermissionsChecks(alternative, userPermissions)
			return checkPermissionsAllowed(resourceId, null, checks, true)
		})
	}

	// Conjunctive: every resource type must grant access
	const checks = buildPermissionsChecks(actionDef, userPermissions)
	return checkPermissionsAllowed(resourceId, null, checks, true)
}

export {
	AUTH_TYPE_GLOBAL,
	AUTH_TYPE_GRANT,
	AUTH_TYPE_REVOKE,
	isAuthorizationEntryRelevant,
	getPermissionsProcessed,
	buildPermissionsChecks,
	checkPermissionsAllowed,
	checkPermissionsDenied,
	checkActionAllowed,
}
