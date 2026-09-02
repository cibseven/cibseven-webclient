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
import DecisionDefinitionVersion from '@/components/decision/DecisionDefinitionVersion.vue'

describe('DecisionDefinitionVersion', () => {
  describe('viewboxStorageKey', () => {
    it('scopes the persisted viewbox by the decision id', () => {
      const key = DecisionDefinitionVersion.methods.viewboxStorageKey.call({ decision: { id: 'dec-42' } })
      expect(key).toBe('cibseven:dmn-viewbox:dec-42')
    })
  })

  describe('viewerFrameStorageKey', () => {
    it('uses a decision-scoped storage key shared with DecisionInstance', () => {
      expect(DecisionDefinitionVersion.methods.viewerFrameStorageKey.call({})).toBe('cibseven:viewer-frame-size:decision')
    })
  })

  describe('tabs', () => {
    it('includes only the built-in instances tab when no deep links are configured', () => {
      const tabs = DecisionDefinitionVersion.computed.tabs.call({ $root: { config: {} } })
      expect(tabs).toEqual([{ id: 'instances', text: 'decision.instances' }])
    })

    it('appends configured decisionDefinition deep links, falling back to the id when untranslated', () => {
      const context = {
        $root: { config: { deepLinks: { decisionDefinition: [{ id: 'myExternalLinkId', url: 'https://external.example' }] } } },
        $t: key => key
      }
      const tabs = DecisionDefinitionVersion.computed.tabs.call(context)
      expect(tabs).toEqual([
        { id: 'instances', text: 'decision.instances' },
        { id: 'myExternalLinkId', text: 'myExternalLinkId' }
      ])
    })

    it('uses the translated label when a translation exists', () => {
      const context = {
        $root: { config: { deepLinks: { decisionDefinition: [{ id: 'myExternalLinkId', url: 'https://external.example' }] } } },
        $t: () => 'My External Link'
      }
      const tabs = DecisionDefinitionVersion.computed.tabs.call(context)
      expect(tabs.at(-1)).toEqual({ id: 'myExternalLinkId', text: 'My External Link' })
    })

    it('drops a deep link entry that collides with the built-in instances tab id', () => {
      const context = { $root: { config: { deepLinks: { decisionDefinition: [
        { id: 'instances', url: 'https://external.example' }
      ] } } } }
      const tabs = DecisionDefinitionVersion.computed.tabs.call(context)
      expect(tabs).toEqual([{ id: 'instances', text: 'decision.instances' }])
    })
  })

  describe('matchedDeepLink', () => {
    it('returns the deep link entry matching the active tab', () => {
      const context = {
        activeTab: 'myExternalLinkId',
        $root: { config: { deepLinks: { decisionDefinition: [{ id: 'myExternalLinkId', url: 'https://external.example' }] } } }
      }
      expect(DecisionDefinitionVersion.computed.matchedDeepLink.call(context)).toEqual({ id: 'myExternalLinkId', url: 'https://external.example', text: 'deepLinks.decisionDefinition.myExternalLinkId.title' })
    })

    it('returns undefined when the active tab is the built-in instances tab', () => {
      const context = { activeTab: 'instances', $root: { config: {} } }
      expect(DecisionDefinitionVersion.computed.matchedDeepLink.call(context)).toBeUndefined()
    })
  })

  describe('matchedDeepLinkParams', () => {
    it('builds decision definition context and language params', () => {
      const context = {
        decision: { id: 'dec-1', key: 'myDecision', tenantId: 'tenant-1' },
        currentLanguage: () => 'en'
      }
      expect(DecisionDefinitionVersion.computed.matchedDeepLinkParams.call(context)).toEqual({
        decisionDefinitionId: 'dec-1',
        decisionDefinitionKey: 'myDecision',
        tenantId: 'tenant-1',
        lang: 'en'
      })
    })
  })
})
