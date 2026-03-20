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
import {
	buildPermissionsChecks,
	checkPermissionsAllowed,
	checkPermissionsDenied,
} from './utils/permissionsUtils.js'

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
			if (!this.$root.config.authorizationEnabled) return true
			if (!permissionsRequired) return false
			const permissionsCheck = buildPermissionsChecks(permissionsRequired, this.$root.user.permissions)
			return checkPermissionsAllowed(access, null, permissionsCheck, this.$root.config.authorizationEnabled)
		},
		applicationPermissionsDenied: function(permissionsRequired, access) {
			if (!this.$root.config.authorizationEnabled) return false
			if (!permissionsRequired) return true
			const permissionsCheck = buildPermissionsChecks(permissionsRequired, this.$root.user.permissions)
			return checkPermissionsDenied(access, null, permissionsCheck, this.$root.config.authorizationEnabled)
		},
		tasksByPermissions: function(permissionsRequired, tasks) {
			const permissionsCheck = buildPermissionsChecks(permissionsRequired, this.$root.user.permissions)
			const tmpTasks = []
			tasks.forEach(function(t) {
				if (checkPermissionsAllowed(t, 'id', permissionsCheck, this.$root.config.authorizationEnabled)) tmpTasks.push(t)
			}.bind(this))
			return tmpTasks
		},
		processesByPermissions: function(permissionsRequired, processes) {
			const permissionsCheck = buildPermissionsChecks(permissionsRequired, this.$root.user.permissions)
			processes.forEach(function(p) {
				if (checkPermissionsAllowed(p, 'key', permissionsCheck, this.$root.config.authorizationEnabled)) p.revoked = false
				else p.revoked = true
			}.bind(this))
			return processes
		},
		processByPermissions: function(permissionsRequired, process) {
			const permissionsCheck = buildPermissionsChecks(permissionsRequired, this.$root.user.permissions)
			return checkPermissionsAllowed(process, 'key', permissionsCheck, this.$root.config.authorizationEnabled)
		},
		filtersByPermissions: function(permissionsRequired, filters) {
			const tmpFilters = []
			if (!filters || !Array.isArray(filters) || !filters.length) return tmpFilters // Return empty array if no filters are provided or filters is not an array
			const permissionsCheck = buildPermissionsChecks(permissionsRequired, this.$root.user.permissions)
			filters.forEach(function(f) {
				if (checkPermissionsAllowed(f, 'id', permissionsCheck, this.$root.config.authorizationEnabled)) tmpFilters.push(f)
			}.bind(this))
			return tmpFilters
		},
		filterByPermissions: function(permissionsRequired, filter, create) {
			const permissionsCheck = buildPermissionsChecks(permissionsRequired, this.$root.user.permissions)
			//Handle custom CREATE permissions case//
			if (create) {
				const createCheck = permissionsCheck.find(function(p) {
					return p.granted.includes('*') && !p.revoked.includes('*')
				})
				return !!createCheck
			}
			/////////////////////////////////////////
			return filter ? checkPermissionsAllowed(filter, 'id', permissionsCheck, this.$root.config.authorizationEnabled) : false
		},
		adminManagementPermissions: function(permissionsRequired, access) {
			const permissionsCheck = buildPermissionsChecks(permissionsRequired, this.$root.user.permissions)
			return checkPermissionsAllowed(access, null, permissionsCheck, this.$root.config.authorizationEnabled)
		},
	}
}

export { permissionsMixin }
