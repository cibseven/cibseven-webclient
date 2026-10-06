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
import { describe, it, expect, vi, afterEach } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import DecisionDefinitionVersion from '@/components/decision/DecisionDefinitionVersion.vue'
import { mountWithDefaults } from '../support/mountWithDefaults.js'

function mountView({ dispatch = vi.fn(() => Promise.resolve({ dmnXml: '' })), showDiagram = () => Promise.resolve() } = {}) {
  const decision = { id: 'dec-1', key: 'myDecision', version: 1 }
  return mountWithDefaults(DecisionDefinitionVersion, {
    props: { decisionKey: 'myDecision', versionIndex: '1' },
    i18nPlugin: false,
    global: {
      stubs: {
        DmnViewer: { template: '<div></div>', methods: { showDiagram } },
        // declared as props, otherwise the stub renders the component proxy as an attribute
        ViewerFrame: { template: '<div><slot></slot></div>', props: ['resizerMixin'] },
        PluginSlot: { template: '<div></div>', props: ['name', 'only', 'params'] },
        GenericTabs: true, ScrollableTabsContainer: true,
        DeepLinkFrame: true, DeepLinkButtons: true, DecisionInstancesTable: true, BWaitingBox: true
      },
      mocks: {
        config: { maxProcessesResults: 50, camundaHistoryLevel: 'none' },
        $store: {
          getters: { getSelectedDecisionVersion: () => decision },
          dispatch
        }
      },
      provide: { currentLanguage: () => 'en' }
    }
  })
}

describe('DecisionDefinitionVersion', () => {
  // CIB7-2118: resizerMixin measures this ref, so it must survive switching to a plugin or deep-link tab
  describe('bottom panel ref', () => {
    it('keeps the rContent ref on the bottom container while a plugin tab is active', async () => {
      const wrapper = mountView()
      await flushPromises()
      const panel = wrapper.vm.$refs.rContent
      expect(panel).toBeTruthy()

      await wrapper.setData({ activeTab: 'decision-insights' })

      expect(wrapper.vm.$refs.rContent).toBe(panel)
      expect(() => wrapper.vm.toggleContent()).not.toThrow()
      expect(() => wrapper.vm.resize({ y: 10 })).not.toThrow()
      wrapper.unmount()
    })
  })

  // The diagram is shown 100 ms after its XML arrives. Leaving the view in between must not
  // reach for a viewer that no longer exists; that surfaced as an uncaught error in CI.
  describe('leaving the view while the diagram loads', () => {
    const fakeTimeouts = () => vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    afterEach(() => { vi.useRealTimers() })

    it('shows the diagram once its XML has arrived', async () => {
      fakeTimeouts()
      const showDiagram = vi.fn(() => Promise.resolve())
      const wrapper = mountView({ dispatch: vi.fn(() => Promise.resolve({ dmnXml: '<dmn/>' })), showDiagram })
      await flushPromises()

      vi.advanceTimersByTime(100)

      expect(showDiagram).toHaveBeenCalledWith('<dmn/>')
      wrapper.unmount()
    })

    it('does nothing when the view is left while the diagram waits to be shown', async () => {
      fakeTimeouts()
      const showDiagram = vi.fn(() => Promise.resolve())
      const wrapper = mountView({ showDiagram })
      await flushPromises()

      wrapper.unmount()

      expect(() => vi.advanceTimersByTime(100)).not.toThrow()
      expect(showDiagram).not.toHaveBeenCalled()
    })

    it('does nothing when the view is left before the XML arrives', async () => {
      fakeTimeouts()
      let deliverXml
      const wrapper = mountView({ dispatch: vi.fn(type => type === 'getXmlById'
        ? new Promise(resolve => { deliverXml = resolve })
        : Promise.resolve({ dmnXml: '' })) })
      await flushPromises()

      // unmount cannot cancel a request that is still running, so the timer starts afterwards
      wrapper.unmount()
      deliverXml({ dmnXml: '<dmn/>' })
      await flushPromises()

      expect(() => vi.advanceTimersByTime(100)).not.toThrow()
    })
  })

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
        $root: { config: { deepLinks: { decisionDefinition: [{ id: 'myExternalLinkId', url: 'https://external.example', type: 'tab' }] } } },
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
        $root: { config: { deepLinks: { decisionDefinition: [{ id: 'myExternalLinkId', url: 'https://external.example', type: 'tab' }] } } },
        $t: () => 'My External Link'
      }
      const tabs = DecisionDefinitionVersion.computed.tabs.call(context)
      expect(tabs.at(-1)).toEqual({ id: 'myExternalLinkId', text: 'My External Link' })
    })

    it('ignores a configured deep link whose type is not "tab"', () => {
      const context = {
        $root: { config: { deepLinks: { decisionDefinition: [{ id: 'myButtonLinkId', url: 'https://external.example', type: 'button' }] } } },
        $t: key => key
      }
      const tabs = DecisionDefinitionVersion.computed.tabs.call(context)
      expect(tabs).toEqual([{ id: 'instances', text: 'decision.instances' }])
    })

    it('drops a deep link entry that collides with the built-in instances tab id', () => {
      const context = { $root: { config: { deepLinks: { decisionDefinition: [
        { id: 'instances', url: 'https://external.example', type: 'tab' }
      ] } } } }
      const tabs = DecisionDefinitionVersion.computed.tabs.call(context)
      expect(tabs).toEqual([{ id: 'instances', text: 'decision.instances' }])
    })
  })

  describe('matchedDeepLink', () => {
    it('returns the deep link entry matching the active tab', () => {
      const context = {
        activeTab: 'myExternalLinkId',
        $root: { config: { deepLinks: { decisionDefinition: [{ id: 'myExternalLinkId', url: 'https://external.example', type: 'tab' }] } } }
      }
      expect(DecisionDefinitionVersion.computed.matchedDeepLink.call(context)).toEqual({ id: 'myExternalLinkId', url: 'https://external.example', type: 'tab', text: 'deepLinks.decisionDefinition.myExternalLinkId.title' })
    })

    it('returns undefined when the active tab is the built-in instances tab', () => {
      const context = { activeTab: 'instances', $root: { config: {} } }
      expect(DecisionDefinitionVersion.computed.matchedDeepLink.call(context)).toBeUndefined()
    })

    it('returns undefined when the matching entry is a button-type link', () => {
      const context = {
        activeTab: 'myButtonLinkId',
        $root: { config: { deepLinks: { decisionDefinition: [{ id: 'myButtonLinkId', url: 'https://external.example', type: 'button' }] } } }
      }
      expect(DecisionDefinitionVersion.computed.matchedDeepLink.call(context)).toBeUndefined()
    })
  })

  describe('matchedDeepLinkParams', () => {
    it('builds decision definition context and language params', () => {
      const context = {
        decision: { id: 'dec-1', key: 'myDecision', tenantId: 'tenant-1', version: '2', versionTag: 'v2' },
        currentLanguage: () => 'en'
      }
      expect(DecisionDefinitionVersion.computed.matchedDeepLinkParams.call(context)).toEqual({
        decisionDefinitionId: 'dec-1',
        decisionDefinitionKey: 'myDecision',
        decisionDefinitionTenantId: 'tenant-1',
        decisionDefinitionVersion: '2',
        decisionDefinitionVersionTag: 'v2',
        lang: 'en'
      })
    })
  })
})
