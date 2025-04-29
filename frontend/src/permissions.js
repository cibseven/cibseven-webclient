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
var permissionsMixin = {
	methods: {
        hasAdminManagementPermissions: function(permissions) {
			return (this.adminManagementPermissions(permissions.usersManagement, 'user') ||
			this.adminManagementPermissions(permissions.groupsManagement, 'group') ||
			this.adminManagementPermissions(permissions.authorizationsManagement, 'authorization') ||
			this.adminManagementPermissions(permissions.tenantsManagement, 'tenant') ||
			this.adminManagementPermissions(permissions.systemManagement, 'system'))
		},
		applicationPermissions: function(permissionsRequired, access) {
			var permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			return this.$_permissionsMixin_checkPermissionsAllowed(access, null, permissionsCheck)
		},
		applicationPermissionsDenied: function (permissionsRequired, access) {
			var permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			return this.$_permissionsMixin_checkPermissionsDenied(access, null, permissionsCheck)
		},
		tasksByPermissions: function(permissionsRequired, tasks) {
			var permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			var tmpTasks = []
			tasks.forEach(function(t) {
				if (this.$_permissionsMixin_checkPermissionsAllowed(t, 'id', permissionsCheck)) tmpTasks.push(t)
			}.bind(this))
			return tmpTasks
		},
		processesByPermissions: function(permissionsRequired, processes) {
			var permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			processes.forEach(function(p) {
				if (this.$_permissionsMixin_checkPermissionsAllowed(p, 'key', permissionsCheck)) p.revoked = false
				else p.revoked = true
			}.bind(this))
			return processes
		},
		processByPermissions: function(permissionsRequired, process) {
			var permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			return this.$_permissionsMixin_checkPermissionsAllowed(process, 'key', permissionsCheck)
		},
		filtersByPermissions: function(permissionsRequired, filters) {
			var tmpFilters = []
			var permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)

			filters.forEach(function(f) {
				if (this.$_permissionsMixin_checkPermissionsAllowed(f, 'id', permissionsCheck)) tmpFilters.push(f)
			}.bind(this))
			return tmpFilters
		},
		filterByPermissions: function(permissionsRequired, filter, create) {
			var permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			//Handle custom CREATE permissions case//
			if (create) {
				var createCheck = permissionsCheck.find(function(p) {
					return p.granted.indexOf('*') !== -1 && p.revoked.indexOf('*') === -1
				})
				return !!createCheck
			}
			/////////////////////////////////////////
			return filter ? this.$_permissionsMixin_checkPermissionsAllowed(filter, 'id', permissionsCheck) : false
		},
		adminManagementPermissions: function(permissionsRequired, access) {
			var permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			return this.$_permissionsMixin_checkPermissionsAllowed(access, null, permissionsCheck)
		},
		$_permissionsMixin_setAllPermissionsObject: function(permissionsRequired) {
			return Object.keys(permissionsRequired).map(function(key) {
				return this.$_permissionsMixin_getPermissionsProcessed(this.$root.user.permissions[key], permissionsRequired[key])
			}.bind(this))
		},
		$_permissionsMixin_getPermissionsProcessed: function(permissionsToHandle, permissionsToCheck) {
			var permissionsProcesses = {granted: [], revoked: []}
			var groups = ["groupId", "userId"]
			groups.forEach(function(group) {
				this.$_permissionsMixin_getPermissionsGrouped(permissionsToHandle, group).forEach(function(p) {
					var allPermsIncluded = permissionsToCheck.every(function(v) {
						return p.permissions.indexOf(v) !== -1
					})
					var somePermsIncluded = permissionsToCheck.some(function(v) {
						return p.permissions.indexOf(v) !== -1
					})
					if (p.type === 2) {
						if (permissionsProcesses.revoked.indexOf(p.resourceId) === -1 &&
							(somePermsIncluded || p.permissions.indexOf('ALL') !== -1))
							permissionsProcesses.revoked.push(p.resourceId)
					} else if ((p.permissions.indexOf('ALL') !== -1 || allPermsIncluded)) {
						if (permissionsProcesses.granted.indexOf(p.resourceId) === -1)
							permissionsProcesses.granted.push(p.resourceId)
					}
				})
			}.bind(this))
			return permissionsProcesses
		},
		$_permissionsMixin_getPermissionsGrouped: function(permissions, field) {
			return permissions.filter(function(p) {
				return p[field] !== null
			})
		},
		$_permissionsMixin_checkPermissionsAllowed: function(object, key, permissionsCheck) {
			var check = false
			var val = key ? object[key] : object
			for (var i = 0; i < permissionsCheck.length; i++) {
				if ((permissionsCheck[i].granted.indexOf(val) !== -1 || permissionsCheck[i].granted.indexOf('*') !== -1) &&
				permissionsCheck[i].revoked.indexOf(val) === -1 && permissionsCheck[i].revoked.indexOf('*') === -1) check = true
				else return false
			}
			return check
		},
		$_permissionsMixin_checkPermissionsDenied: function(object, key, permissionsCheck) {
		    var val = key ? object[key] : object
		    for (var i = 0; i < permissionsCheck.length; i++) {
		        if (permissionsCheck[i].revoked.indexOf(val) !== -1 || permissionsCheck[i].revoked.indexOf('*') !== -1) {
		            return true
		        }
		    }
		    return false
		}
	}
}

export { permissionsMixin }
