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
import { applyConfigDefaults } from '@/utils/config.js'

describe('config utility', () => {
  describe('applyConfigDefaults', () => {
    it('should return the default configuration when no config is given', () => {
      const config = applyConfigDefaults()
      expect(config.theme).toBe('cib')
      expect(config.supportedLanguages).toEqual(['en'])
      expect(config.permissions.tasklist).toEqual({ application: ['ACCESS'] })
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
  })
})
