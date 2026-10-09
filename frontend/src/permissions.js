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
			return (this.applicationPermissions(permissions.usersManagement, 'user') ||
			this.applicationPermissions(permissions.groupsManagement, 'group') ||
			this.applicationPermissions(permissions.authorizationsManagement, 'authorization') ||
			this.applicationPermissions(permissions.tenantsManagement, 'tenant') ||
			this.applicationPermissions(permissions.systemManagement, 'system'))
		},
		applicationPermissions: function(permissionsRequired, access) {
			if (!this.$root.config.authorizationEnabled) return true
			if (!permissionsRequired) return false
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(permissionsRequired)
			return this.$_permissionsMixin_checkPermissionsAllowed(access, null, permissionsCheck)
		},
		applicationPermissionsDenied: function (permissionsRequired, access) {
			if (!this.$root.config.authorizationEnabled) return false
			if (!permissionsRequired) return true
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
		/**
		 * Mirrors the engine check for deleting a running process instance
		 * (`AuthorizationCommandChecker.checkDeleteProcessInstance`): any one of the permissions is enough.
		 * @param {string} processInstanceId
		 * @param {string} processDefinitionKey
		 * @returns {boolean} `true` if the user has `DELETE` on the process instance
		 * or `DELETE_INSTANCE` on its process definition, `false` otherwise
		 */
		canDeleteRuntimeProcessInstance(processInstanceId, processDefinitionKey) {
			return this.$_permissionsMixin_checkAnyAllowed([
				['processInstance', 'DELETE', processInstanceId],
				['processDefinition', 'DELETE_INSTANCE', processDefinitionKey],
			])
		},
		/**
		 * @param {Object} processDefinition 
		 * @returns {boolean} `true` if the user has `DELETE_HISTORY` permission for the given processDefinition (delete historic process instances), `false` otherwise
		 */
		canDeleteHistoryProcessInstance(processDefinition) {
			const requiredPermissions = { 'processDefinition': ['DELETE_HISTORY'] }
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(requiredPermissions)
			return this.$_permissionsMixin_checkPermissionsAllowed(processDefinition, 'key', permissionsCheck)
		},
		/**
		 * Mirrors the engine check for adding, changing and removing variables of a running process instance
		 * (`AuthorizationCommandChecker.checkUpdateProcessInstanceVariables`): any one of the permissions is enough.
		 * @param {string} processInstanceId
		 * @param {string} processDefinitionKey
		 * @returns {boolean} `true` if the user has `UPDATE_VARIABLE` or `UPDATE` on the process instance,
		 * or `UPDATE_INSTANCE_VARIABLE` or `UPDATE_INSTANCE` on its process definition, `false` otherwise
		 */
		canUpdateProcessInstanceVariables(processInstanceId, processDefinitionKey) {
			return this.$_permissionsMixin_checkAnyAllowed([
				['processInstance', 'UPDATE_VARIABLE', processInstanceId],
				['processDefinition', 'UPDATE_INSTANCE_VARIABLE', processDefinitionKey],
				['processInstance', 'UPDATE', processInstanceId],
				['processDefinition', 'UPDATE_INSTANCE', processDefinitionKey],
			])
		},
		/**
		 * @param {string} processInstanceId
		 * @param {string} processDefinitionKey
		 * @param {boolean} isRuntime `true` for a variable of the running process instance, `false` for a historic one
		 * @returns {boolean} for a runtime variable, `true` if the user may delete the process instance
		 * (see `canDeleteRuntimeProcessInstance`) and may change its variables
		 * (for the engine, removing a variable is an update, see `canUpdateProcessInstanceVariables`);
		 * for a historic variable, `true` if the user has `DELETE_HISTORY` on the process definition, as the engine requires
		 */
		canDeleteProcessInstanceVariable(processInstanceId, processDefinitionKey, isRuntime) {
			if (!isRuntime) return this.canDeleteHistoryProcessInstance({ key: processDefinitionKey })
			return this.canDeleteRuntimeProcessInstance(processInstanceId, processDefinitionKey) &&
				this.canUpdateProcessInstanceVariables(processInstanceId, processDefinitionKey)
		},
		/**
		 * @param {string} deploymentId
		 * @returns {boolean} `true` if the user has `READ` permission for the given deploymentId, `false` otherwise
		 */
		canReadDeployment(deploymentId) {
			const requiredPermissions = { 'deployment': ['READ'] }
			const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject(requiredPermissions)
			return this.$_permissionsMixin_checkPermissionsAllowed({id: deploymentId}, 'id', permissionsCheck)
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
		$_permissionsMixin_setAllPermissionsObject: function(permissionsRequired) {
			if (!permissionsRequired) return []
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
		},
		/**
		 * @param {Array<[string, string, string]>} alternatives `[resource, permission, resourceId]` triples
		 * @returns {boolean} `true` if any one of them is allowed, like the engine's disjunctive permission checks
		 */
		$_permissionsMixin_checkAnyAllowed: function(alternatives) {
			return alternatives.some(([resource, permission, resourceId]) => {
				const permissionsCheck = this.$_permissionsMixin_setAllPermissionsObject({ [resource]: [permission] })
				return this.$_permissionsMixin_checkPermissionsAllowed({ id: resourceId }, 'id', permissionsCheck)
			})
		}
	}
}

export { permissionsMixin }
