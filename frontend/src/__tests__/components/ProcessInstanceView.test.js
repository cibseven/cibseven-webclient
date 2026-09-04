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
import ProcessInstanceView from '@/components/process/ProcessInstanceView.vue'

describe('ProcessInstanceView', () => {
  describe('matchedDeepLink', () => {
    it('returns the deep link entry matching the active tab', () => {
      const context = {
        activeTab: 'myExternalLinkId',
        $root: { config: { deepLinks: { processInstance: [{ id: 'myExternalLinkId', url: 'https://external.example' }] } } }
      }
      expect(ProcessInstanceView.computed.matchedDeepLink.call(context)).toEqual({ id: 'myExternalLinkId', url: 'https://external.example', text: 'deepLinks.processInstance.myExternalLinkId.title' })
    })

    it('returns undefined when the active tab is a built-in tab', () => {
      const context = { activeTab: 'variables', $root: { config: {} } }
      expect(ProcessInstanceView.computed.matchedDeepLink.call(context)).toBeUndefined()
    })
  })

  describe('matchedDeepLinkParams', () => {
    it('builds process instance and process definition context (with distinct tenant ids) and language params', () => {
      const context = {
        selectedInstance: { id: 'inst-1', businessKey: 'bk-1', tenantId: 'instance-tenant' },
        process: { id: 'def-1', key: 'myProcess', version: '3', versionTag: 'v3', tenantId: 'definition-tenant' },
        currentLanguage: () => 'en'
      }
      expect(ProcessInstanceView.computed.matchedDeepLinkParams.call(context)).toEqual({
        processInstanceId: 'inst-1',
        processInstanceTenantId: 'instance-tenant',
        businessKey: 'bk-1',
        processDefinitionId: 'def-1',
        processDefinitionKey: 'myProcess',
        processDefinitionVersion: '3',
        processDefinitionVersionTag: 'v3',
        processDefinitionTenantId: 'definition-tenant',
        lang: 'en'
      })
    })
  })
})
