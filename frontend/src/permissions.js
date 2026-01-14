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
const permissionsMixin = {
	methods: {
		hasAdminManagementPermissions: function(permissions) {
			return (this.adminManagementPermissions(permissions.usersManagement, 'user') ||
			this.adminManagementPermissions(permissions.groupsManagement, 'group') ||
			this.adminManagementPermissions(permissions.authorizationsManagement, 'authorization') ||
			this.adminManagementPermissions(permissions.tenantsManagement, 'tenant') ||
			this.adminManagementPermissions(permissions.systemManagement, 'system'))
		},
		applicationPermissions: function(permissionsRequired, access) {
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			return this.$_permissionsMixin_checkPermissionsAllowed(access, null, permissionsCheck)
		},
		applicationPermissionsDenied: function (permissionsRequired, access) {
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			return this.$_permissionsMixin_checkPermissionsDenied(access, null, permissionsCheck)
		},
		tasksByPermissions: function(permissionsRequired, tasks) {
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			const tmpTasks = []
			tasks.forEach(function(t) {
				if (this.$_permissionsMixin_checkPermissionsAllowed(t, 'id', permissionsCheck)) tmpTasks.push(t)
			}.bind(this))
			return tmpTasks
		},
		processesByPermissions: function(permissionsRequired, processes) {
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			processes.forEach(function(p) {
				if (this.$_permissionsMixin_checkPermissionsAllowed(p, 'key', permissionsCheck)) p.revoked = false
				else p.revoked = true
			}.bind(this))
			return processes
		},
		processByPermissions: function(permissionsRequired, process) {
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			return this.$_permissionsMixin_checkPermissionsAllowed(process, 'key', permissionsCheck)
		},
		filtersByPermissions: function(permissionsRequired, filters) {
			const tmpFilters = []
			if (!filters || !Array.isArray(filters) || !filters.length) return tmpFilters // Return empty array if no filters are provided or filters is not an array
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)

			filters.forEach(function(f) {
				if (this.$_permissionsMixin_checkPermissionsAllowed(f, 'id', permissionsCheck)) tmpFilters.push(f)
			}.bind(this))
			return tmpFilters
		},
		filterByPermissions: function(permissionsRequired, filter, create) {
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			//Handle custom CREATE permissions case//
			if (create) {
				const createCheck = permissionsCheck.find(function(p) {
					return p.granted.includes('*') && !p.revoked.includes('*')
				})
				return !!createCheck
			}
			/////////////////////////////////////////
			return filter ? this.$_permissionsMixin_checkPermissionsAllowed(filter, 'id', permissionsCheck) : false
		},
		adminManagementPermissions: function(permissionsRequired, access) {
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			return this.$_permissionsMixin_checkPermissionsAllowed(access, null, permissionsCheck)
		},
		$_permissionsMixin_setAllPermissionsObject: function(permissionsRequired) {
			return Object.keys(permissionsRequired).map(function(key) {
				return this.$_permissionsMixin_getPermissionsProcessed(this.$root.user.permissions[key], permissionsRequired[key])
			}.bind(this))
		},
		$_permissionsMixin_getPermissionsProcessed: function(permissionsToHandle, permissionsToCheck) {
			const permissionsProcesses = {granted: [], revoked: []}
			const groups = ["groupId", "userId"]
			groups.forEach(function(group) {
				this.$_permissionsMixin_getPermissionsGrouped(permissionsToHandle, group).forEach(function(p) {
					const allPermsIncluded = permissionsToCheck.every(function(v) {
						return p.permissions.includes(v)
					})
					const somePermsIncluded = permissionsToCheck.some(function(v) {
						return p.permissions.includes(v)
					})
					if (p.type === 2) {
						if (!permissionsProcesses.revoked.includes(p.resourceId) &&
							(somePermsIncluded || p.permissions.includes('ALL')))
							permissionsProcesses.revoked.push(p.resourceId)
					} else if ((p.permissions.includes('ALL') || allPermsIncluded)) {
						if (!permissionsProcesses.granted.includes(p.resourceId))
							permissionsProcesses.granted.push(p.resourceId)
					}
				})
			}.bind(this))
			return permissionsProcesses
		},
		$_permissionsMixin_getPermissionsGrouped: function(permissions, field) {
			return permissions?.filter(function(p) {
				return p[field] !== null
			}) || []
		},
		$_permissionsMixin_checkPermissionsAllowed: function(object, key, permissionsCheck) {
			if (!this.$root.config.authorizationEnabled) return true;
			const val = key ? object[key] : object
			return (permissionsCheck.length > 0) && permissionsCheck.every(permission =>
				(permission.granted.includes(val) || permission.granted.includes('*')) &&
				!permission.revoked.includes(val) && !permission.revoked.includes('*')
			)
		},
		$_permissionsMixin_checkPermissionsDenied: function(object, key, permissionsCheck) {
			if (!this.$root.config.authorizationEnabled) return false;
			const val = key ? object[key] : object
			return permissionsCheck.some(permission =>
				permission.revoked.includes(val) || permission.revoked.includes('*')
			)
		}
	}
}

export { permissionsMixin }
