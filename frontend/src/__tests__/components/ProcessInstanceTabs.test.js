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
import ProcessInstanceTabs, { RESERVED_TAB_IDS } from '@/components/process/ProcessInstanceTabs.vue'

describe('ProcessInstanceTabs', () => {
  describe('RESERVED_TAB_IDS', () => {
    it('lists every built-in tab id', () => {
      expect(RESERVED_TAB_IDS).toEqual([
        'variables', 'incidents', 'usertasks', 'jobs', 'calledProcessInstances', 'externalTasks'
      ])
    })
  })

  describe('tabs', () => {
    it('returns only the built-in tabs when no deep links are configured', () => {
      const tabs = ProcessInstanceTabs.computed.tabs.call({ $root: { config: {} } })
      expect(tabs).toEqual([
        { id: 'variables', text: 'process.variables' },
        { id: 'incidents', text: 'process.incidents' },
        { id: 'usertasks', text: 'process.usertasks' },
        { id: 'jobs', text: 'process.jobs' },
        { id: 'calledProcessInstances', text: 'process.calledProcessInstances' },
        { id: 'externalTasks', text: 'process.externalTasks' }
      ])
    })

    it('appends configured processInstance deep links after the built-in tabs', () => {
      const context = { $root: { config: { deepLinks: { processInstance: [
        { id: 'myExternalLinkId', url: 'https://external.example' }
      ] } } } }
      const tabs = ProcessInstanceTabs.computed.tabs.call(context)
      expect(tabs.at(-1)).toEqual({ id: 'myExternalLinkId', text: 'deepLinks.processInstance.myExternalLinkId.title', url: 'https://external.example' })
      expect(tabs).toHaveLength(7)
    })

    it('drops a deep link entry that collides with a built-in tab id', () => {
      const context = { $root: { config: { deepLinks: { processInstance: [
        { id: 'variables', url: 'https://external.example' }
      ] } } } }
      const tabs = ProcessInstanceTabs.computed.tabs.call(context)
      expect(tabs).toHaveLength(6)
    })
  })
})
