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
import DecisionInstance from '@/components/decision/DecisionInstance.vue'

const { normalizeCell, isDmnStringLiteral } = DecisionInstance.methods

describe('DecisionInstance', () => {
  describe('viewboxStorageKey', () => {
    it('scopes the persisted viewbox by the decision definition id', () => {
      const key = DecisionInstance.methods.viewboxStorageKey.call({ instance: { decisionDefinitionId: 'dec-1' } })
      expect(key).toBe('cibseven:dmn-viewbox:dec-1')
    })
  })

  describe('viewerFrameStorageKey', () => {
    it('uses a decision-scoped storage key', () => {
      expect(DecisionInstance.methods.viewerFrameStorageKey.call({})).toBe('cibseven:viewer-frame-size:decision')
    })
  })

  describe('normalizeCell', () => {
    it.each([
      [undefined,       ''],
      [null,            ''],
      ['',              ''],
      ['""',            ''],

      ['"hello"',       'hello'],
      ['"  hello  "',   'hello'],
      ['  "world"  ',   'world'],
      ['  " unclosed  ','unclosed'],
      ['  unclosed " ', 'unclosed'],
      ['hello',         'hello'],
      ['  hello  ',     'hello'],
      ['"budget"',      'budget'],
      ['"exceptional"', 'exceptional'],
      ['"a"',           'a'],
      ['"a", "b"',      'a", "b'], // ok - only removes surrounding quotes, not inner ones
    ])('normalizeCell(%s) → %s', (input, expected) => {
      expect(normalizeCell(input)).toBe(expected)
    })
  })

  describe('tabs', () => {
    it('includes only the built-in inputs/outputs tabs when no deep links are configured', () => {
      const tabs = DecisionInstance.computed.tabs.call({ $root: { config: {} } })
      expect(tabs).toEqual([
        { id: 'inputs', text: 'decision.inputs' },
        { id: 'outputs', text: 'decision.outputs' }
      ])
    })

    it('appends configured decisionInstance deep links, falling back to the id when untranslated', () => {
      const context = {
        $root: { config: { deepLinks: { decisionInstance: [{ id: 'myExternalLinkId', url: 'https://external.example' }] } } },
        $t: key => key
      }
      const tabs = DecisionInstance.computed.tabs.call(context)
      expect(tabs).toEqual([
        { id: 'inputs', text: 'decision.inputs' },
        { id: 'outputs', text: 'decision.outputs' },
        { id: 'myExternalLinkId', text: 'myExternalLinkId' }
      ])
    })

    it('uses the translated label when a translation exists', () => {
      const context = {
        $root: { config: { deepLinks: { decisionInstance: [{ id: 'myExternalLinkId', url: 'https://external.example' }] } } },
        $t: () => 'My External Link'
      }
      const tabs = DecisionInstance.computed.tabs.call(context)
      expect(tabs.at(-1)).toEqual({ id: 'myExternalLinkId', text: 'My External Link' })
    })

    it('drops a deep link entry that collides with a built-in tab id', () => {
      const context = { $root: { config: { deepLinks: { decisionInstance: [
        { id: 'outputs', url: 'https://external.example' }
      ] } } } }
      const tabs = DecisionInstance.computed.tabs.call(context)
      expect(tabs).toEqual([
        { id: 'inputs', text: 'decision.inputs' },
        { id: 'outputs', text: 'decision.outputs' }
      ])
    })
  })

  describe('matchedDeepLink', () => {
    it('returns the deep link entry matching the active tab', () => {
      const context = {
        activeTab: 'myExternalLinkId',
        $root: { config: { deepLinks: { decisionInstance: [{ id: 'myExternalLinkId', url: 'https://external.example' }] } } }
      }
      expect(DecisionInstance.computed.matchedDeepLink.call(context)).toEqual({ id: 'myExternalLinkId', url: 'https://external.example', text: 'deepLinks.decisionInstance.myExternalLinkId.title' })
    })

    it('returns undefined when the active tab is a built-in tab', () => {
      const context = { activeTab: 'inputs', $root: { config: {} } }
      expect(DecisionInstance.computed.matchedDeepLink.call(context)).toBeUndefined()
    })
  })

  describe('matchedDeepLinkParams', () => {
    it('builds decision instance context and language params', () => {
      const context = {
        instance: { id: 'inst-1', decisionDefinitionId: 'dec-1', decisionDefinitionKey: 'myDecision' },
        currentLanguage: () => 'en'
      }
      expect(DecisionInstance.computed.matchedDeepLinkParams.call(context)).toEqual({
        decisionInstanceId: 'inst-1',
        decisionDefinitionId: 'dec-1',
        decisionDefinitionKey: 'myDecision',
        lang: 'en'
      })
    })
  })

  describe('isDmnStringLiteral', () => {
    it.each([
      ['"hello"',       true],
      ['"budget"',      true],
      ['"exceptional"', true],
      ['""',            true],
      ['  "hello"  ',   true],
      ['"a", "b"',      true],

      [undefined,       false],
      [null,            false],
      ['',              false],
      ['  ',            false],

      ['hello',         false],
      ['123',           false],
      ['"unclosed',     false],
      ['unclosed"',     false],
    ])('isDmnStringLiteral(%s) → %s', (input, expected) => {
      expect(isDmnStringLiteral(input)).toBe(expected)
    })
  })
})
