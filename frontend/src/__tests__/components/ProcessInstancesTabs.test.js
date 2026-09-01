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
import ProcessInstancesTabs, { RESERVED_TAB_IDS } from '@/components/process/ProcessInstancesTabs.vue'

describe('ProcessInstancesTabs', () => {
  describe('RESERVED_TAB_IDS', () => {
    it('lists every built-in tab id', () => {
      expect(RESERVED_TAB_IDS).toEqual(['instances', 'jobDefinitions', 'incidents', 'calledProcessDefinitions'])
    })
  })

  describe('tabs', () => {
    it('returns only the built-in tabs when no deep links are configured', () => {
      const tabs = ProcessInstancesTabs.computed.tabs.call({ $root: { config: {} } })
      expect(tabs).toEqual([
        { id: 'instances', text: 'process.instances' },
        { id: 'jobDefinitions', text: 'process.jobDefinitions' },
        { id: 'incidents', text: 'process.incidents' },
        { id: 'calledProcessDefinitions', text: 'process.calledProcessDefinitions' }
      ])
    })

    it('appends configured processDefinition deep links after the built-in tabs', () => {
      const context = { $root: { config: { deepLinks: { processDefinition: [
        { id: 'vhvOutput', url: 'https://external.example' }
      ] } } } }
      const tabs = ProcessInstancesTabs.computed.tabs.call(context)
      expect(tabs.at(-1)).toEqual({ id: 'vhvOutput', text: 'deepLinks.processDefinition.vhvOutput.title', url: 'https://external.example' })
      expect(tabs).toHaveLength(5)
    })

    it('drops a deep link entry that collides with a built-in tab id', () => {
      const context = { $root: { config: { deepLinks: { processDefinition: [
        { id: 'incidents', url: 'https://external.example' }
      ] } } } }
      const tabs = ProcessInstancesTabs.computed.tabs.call(context)
      expect(tabs).toHaveLength(4)
    })
  })
})
