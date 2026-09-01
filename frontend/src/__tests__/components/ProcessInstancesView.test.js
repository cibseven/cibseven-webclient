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
import ProcessInstancesView from '@/components/process/ProcessInstancesView.vue'

describe('ProcessInstancesView', () => {
  describe('matchedDeepLink', () => {
    it('returns the deep link entry matching the active tab', () => {
      const context = {
        activeTab: 'vhvOutput',
        $root: { config: { deepLinks: { processDefinition: [{ id: 'vhvOutput', url: 'https://external.example' }] } } }
      }
      expect(ProcessInstancesView.computed.matchedDeepLink.call(context)).toEqual({ id: 'vhvOutput', url: 'https://external.example', text: 'deepLinks.processDefinition.vhvOutput.title' })
    })

    it('returns undefined when the active tab is a built-in tab', () => {
      const context = { activeTab: 'instances', $root: { config: {} } }
      expect(ProcessInstancesView.computed.matchedDeepLink.call(context)).toBeUndefined()
    })
  })

  describe('resolvedDeepLinkUrl', () => {
    it('appends process definition context and language to the deep link url', () => {
      const context = {
        matchedDeepLink: { id: 'vhvOutput', url: 'https://external.example/app' },
        process: { id: 'def-1', key: 'myProcess' },
        tenantId: 'tenant-1',
        currentLanguage: () => 'en'
      }
      const url = new URL(ProcessInstancesView.computed.resolvedDeepLinkUrl.call(context))
      expect(url.searchParams.get('processDefinitionId')).toBe('def-1')
      expect(url.searchParams.get('processDefinitionKey')).toBe('myProcess')
      expect(url.searchParams.get('tenantId')).toBe('tenant-1')
      expect(url.searchParams.get('lang')).toBe('en')
    })
  })
})
