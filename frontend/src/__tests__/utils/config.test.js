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
import { applyConfigDefaults, isModelerEnabled } from '@/utils/config.js'

describe('config utility', () => {
  describe('applyConfigDefaults', () => {
    it('should return the default configuration when no config is given', () => {
      const config = applyConfigDefaults()
      expect(config.theme).toBe('cib')
      expect(config.supportedLanguages).toEqual(['en'])
      expect(config.permissions.tasklist).toEqual({ application: ['ACCESS'] })
      expect(config.deepLinks).toEqual({
        processDefinition: [],
        processInstance: [],
        decisionDefinition: [],
        decisionInstance: []
      })
    })

    it('should merge configured deepLinks entries over the empty defaults', () => {
      const config = applyConfigDefaults({
        deepLinks: { processInstance: [{ id: 'myExternalLinkId', url: 'https://external.example' }] }
      })
      expect(config.deepLinks.processInstance).toEqual([{ id: 'myExternalLinkId', url: 'https://external.example' }])
      expect(config.deepLinks.decisionInstance).toEqual([])
    })

    it('should return the default configuration for an empty object', () => {
      const config = applyConfigDefaults({})
      expect(config.theme).toBe('cib')
      expect(config.taskListTime).toBe('30000')
    })

    it('should override a top-level primitive value', () => {
      const config = applyConfigDefaults({ theme: 'dark' })
      expect(config.theme).toBe('dark')
      expect(config.taskListTime).toBe('30000')
    })

    it('should deep merge nested objects, preserving untouched sibling keys', () => {
      const config = applyConfigDefaults({
        permissions: {
          tasklist: { application: ['ACCESS', 'CUSTOM'] }
        }
      })
      expect(config.permissions.tasklist).toEqual({ application: ['ACCESS', 'CUSTOM'] })
      expect(config.permissions.cockpit).toEqual({ application: ['ACCESS'] })
    })

    it('should merge deeply nested objects several levels down', () => {
      const config = applyConfigDefaults({
        taskFilter: {
          tasksNumber: { interval: 5000 }
        }
      })
      expect(config.taskFilter.tasksNumber).toEqual({ enabled: true, interval: 5000 })
      expect(config.taskFilter.advancedSearch).toEqual({
        modalEnabled: false,
        filterEnabled: false,
        criteriaKeys: [],
        processVariables: []
      })
    })

    it('should replace array values instead of merging them', () => {
      const config = applyConfigDefaults({ supportedLanguages: ['de', 'fr'] })
      expect(config.supportedLanguages).toEqual(['de', 'fr'])
    })

    it('should overwrite a nested value with null', () => {
      const config = applyConfigDefaults({ permissions: { tasklist: null } })
      expect(config.permissions.tasklist).toBeNull()
    })

    it('should add keys that are not present in the default configuration', () => {
      const config = applyConfigDefaults({ customFeature: { enabled: true } })
      expect(config.customFeature).toEqual({ enabled: true })
      expect(config.theme).toBe('cib')
    })

    it('should not mutate the default configuration across calls', () => {
      applyConfigDefaults({ permissions: { tasklist: { application: ['CUSTOM'] } } })
      const config = applyConfigDefaults()
      expect(config.permissions.tasklist).toEqual({ application: ['ACCESS'] })
    })

    it('should not offer the deprecated optimize resource type', () => {
      const config = applyConfigDefaults({ admin: { resourcesTypes: {} } })
      const keys = Object.values(config.admin.resourcesTypes).map(resourceType => resourceType.key)
      expect(keys).not.toContain('optimize')
      // 15 is the engine's REPORT, which the defaults used to label as optimize
      expect(config.admin.resourcesTypes['15']).toBeUndefined()
    })

    it('should not offer the deprecated report and dashboard resource types', () => {
      const config = applyConfigDefaults({ admin: { resourcesTypes: {} } })
      const keys = Object.values(config.admin.resourcesTypes).map(resourceType => resourceType.key)
      expect(keys).not.toContain('report')
      expect(keys).not.toContain('dashboard')
      // 16 is the engine's DASHBOARD, which the defaults used to label as operationLogCategory
      expect(keys).not.toContain('operationLogCategory')
      expect(config.admin.resourcesTypes['16']).toBeUndefined()
    })

    it('should key every default resource type by its own id', () => {
      const { resourcesTypes } = applyConfigDefaults().admin
      Object.entries(resourcesTypes).forEach(([key, resourceType]) => {
        expect(resourceType.id).toBe(key)
      })
    })

    // ids and permissions as in the engine's Resources, TaskPermissions and ProcessInstancePermissions
    it.each([
      ['7', 'task', ['CREATE', 'READ', 'UPDATE', 'DELETE', 'TASK_ASSIGN', 'TASK_WORK', 'UPDATE_VARIABLE', 'READ_VARIABLE']],
      ['8', 'processInstance', ['CREATE', 'READ', 'UPDATE', 'DELETE', 'RETRY_JOB', 'SUSPEND', 'UPDATE_VARIABLE']]
    ])('should define resource type %j as %s with the engine permissions', (id, key, permissions) => {
      const resourceType = applyConfigDefaults().admin.resourcesTypes[id]
      expect(resourceType.key).toBe(key)
      expect(resourceType.permissions).toEqual(permissions)
    })
  })

  // The one check the modeler route, the navigation and the viewers share
  describe('isModelerEnabled', () => {
    it('is on only when the backend reports it on', () => {
      expect(isModelerEnabled({ modelerEnabled: true })).toBe(true)
    })

    // undefined: a backend from before the flag existed (e.g. 2.2); 'true' and 1 are not the boolean the backend sends
    it.each([[false], [undefined], ['true'], [1]])('is off for modelerEnabled %j', (modelerEnabled) => {
      expect(isModelerEnabled({ modelerEnabled })).toBe(false)
    })

    it('is off without a loaded configuration', () => {
      expect(isModelerEnabled(undefined)).toBe(false)
    })
  })
})
