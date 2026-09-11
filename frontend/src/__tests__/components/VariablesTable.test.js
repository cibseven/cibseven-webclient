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
import VariablesTable from '@/components/process/tables/VariablesTable.vue'

describe('VariablesTable', () => {
  describe('hasDeepLinks', () => {
    it('returns true only when a button-type processInstance deep link is configured', () => {
      const tabOnly = { $root: { config: { deepLinks: { processInstance: [{ id: 'tabLink', url: 'https://external.example', type: 'tab' }] } } } }
      const buttonLink = { $root: { config: { deepLinks: { processInstance: [{ id: 'buttonLink', url: 'https://external.example', type: 'button' }] } } } }
      expect(VariablesTable.computed.hasDeepLinks.call(tabOnly)).toBe(false)
      expect(VariablesTable.computed.hasDeepLinks.call(buttonLink)).toBe(true)
    })
  })

  describe('matchedDeepLinkParams', () => {
    it('builds process instance and process definition context (with distinct tenant ids) and language params', () => {
      const context = {
        selectedInstance: { id: 'pi-1', tenantId: 'tenant-instance', businessKey: 'bk-1' },
        process: { id: 'def-1', key: 'myProcess', tenantId: 'tenant-def', version: '3', versionTag: 'v3' },
        currentLanguage: () => 'en'
      }
      expect(VariablesTable.computed.matchedDeepLinkParams.call(context)).toEqual({
        processInstanceId: 'pi-1',
        processInstanceTenantId: 'tenant-instance',
        businessKey: 'bk-1',
        processDefinitionId: 'def-1',
        processDefinitionKey: 'myProcess',
        processDefinitionVersion: '3',
        processDefinitionVersionTag: 'v3',
        processDefinitionTenantId: 'tenant-def',
        lang: 'en'
      })
    })

    it('fills fields with undefined when selectedInstance/process are missing', () => {
      const context = { selectedInstance: null, process: null, currentLanguage: () => 'en' }
      expect(VariablesTable.computed.matchedDeepLinkParams.call(context)).toEqual({
        processInstanceId: undefined,
        processInstanceTenantId: undefined,
        businessKey: undefined,
        processDefinitionId: undefined,
        processDefinitionKey: undefined,
        processDefinitionVersion: undefined,
        processDefinitionVersionTag: undefined,
        processDefinitionTenantId: undefined,
        lang: 'en'
      })
    })
  })
})
