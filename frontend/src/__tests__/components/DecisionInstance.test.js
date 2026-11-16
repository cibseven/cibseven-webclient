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
import DecisionInstance from '@/components/decision/DecisionInstance.vue'
import { mountWithDefaults } from '../support/mountWithDefaults.js'

vi.mock('@/services.js', async (importOriginal) => ({
  ...await importOriginal(),
  DecisionService: {
    getHistoricDecisionInstances: vi.fn(() => Promise.resolve([
      { id: 'inst-1', decisionDefinitionId: 'dec-1', decisionDefinitionKey: 'myDecision', inputs: [], outputs: [] }
    ]))
  }
}))

const { applyInstanceValues, applyInputHeaders, applyOutputRows, applyHighlightText, formatValue } = DecisionInstance.methods

/**
 * A decision table as the dmn-js viewer renders it: the rows carry no id, the rule id sits on the
 * index cell and on every input and output cell (data-row-id), the column id on the clause cells.
 */
function decisionTable({ inputs, outputs, rules }) {
  const cell = (className, ruleId, colId, text) =>
    `<td class="${className}" data-row-id="${ruleId}" data-col-id="${colId}">${text}</td>`
  const table = document.createElement('table')
  table.className = 'tjs-table'
  table.innerHTML = `
    <thead><tr><th class="index-column"></th>
      ${inputs.map(id => `<th class="input-cell" data-col-id="${id}"><div class="clause">${id}</div></th>`).join('')}
      ${outputs.map(id => `<th class="output-cell" data-col-id="${id}">${id}</th>`).join('')}
    </tr></thead>
    <tbody>${rules.map((rule, index) => `<tr>
      <td class="rule-index" data-element-id="${rule.id}" data-row-id="${rule.id}">${index + 1}</td>
      ${rule.inputs.map((text, i) => cell('input-cell', rule.id, inputs[i], text)).join('')}
      ${rule.outputs.map((text, i) => cell('output-cell', rule.id, outputs[i], text)).join('')}
    </tr>`).join('')}</tbody>`
  return table
}

/** The table of CIB7UNION-10 (hit policy First): rules 1, 3 and 4 share the output "true". */
function customerTable() {
  return decisionTable({
    inputs: ['isCtmOrder', 'count'],
    outputs: ['result'],
    rules: [
      { id: 'rule-1', inputs: ['true', ''], outputs: ['true'] },
      { id: 'rule-2', inputs: ['', '0'], outputs: ['false'] },
      { id: 'rule-3', inputs: ['false', '1'], outputs: ['true'] },
      { id: 'rule-4', inputs: ['', '&gt; 1'], outputs: ['true'] },
      { id: 'rule-5', inputs: ['', ''], outputs: ['false'] }
    ]
  })
}

function applyOutputs(table, outputs) {
  applyOutputRows.call({ instance: { outputs }, applyHighlightText, formatValue }, table)
}

const firedRules = table => Array.from(table.querySelectorAll('tr.fired-rule'))
  .map(row => row.querySelector('td.rule-index').getAttribute('data-row-id'))

const outputCell = (table, ruleId, colId) =>
  table.querySelector(`td.output-cell[data-row-id="${ruleId}"][data-col-id="${colId}"]`)

function mountView({ dispatch = vi.fn(() => Promise.resolve({ dmnXml: '' })), showDiagram = () => Promise.resolve() } = {}) {
  return mountWithDefaults(DecisionInstance, {
    props: { instanceId: 'inst-1', versionIndex: '1' },
    i18nPlugin: false,
    global: {
      stubs: {
        DmnViewer: { template: '<div></div>', methods: { showDiagram } },
        // declared as props, otherwise the stub renders the component proxy as an attribute
        ViewerFrame: { template: '<div><slot></slot></div>', props: ['resizerMixin'] },
        PluginSlot: { template: '<div></div>', props: ['name', 'only', 'params'] },
        FlowTable: { template: '<div></div>', inheritAttrs: false },
        GenericTabs: true, ScrollableTabsContainer: true,
        DeepLinkFrame: true, DeepLinkButtons: true
      },
      mocks: {
        config: {},
        $store: {
          // the namespaced mapGetters('diagram', ...) looks the module up here first
          _modulesNamespaceMap: { 'diagram/': {} },
          getters: { 'diagram/isDiagramReady': false, getSelectedDecisionVersion: () => ({ id: 'dec-1' }) },
          dispatch
        }
      },
      provide: { currentLanguage: () => 'en' }
    }
  })
}

describe('DecisionInstance', () => {
  // CIB7-2118: resizerMixin measures this ref, so it must survive switching away from inputs/outputs
  describe('bottom panel ref', () => {
    it.each(['outputs', 'decision-insights'])('keeps the rContent ref on the bottom container on the %s tab', async (tab) => {
      const wrapper = mountView()
      await flushPromises()
      const panel = wrapper.vm.$refs.rContent
      expect(panel).toBeTruthy()

      await wrapper.setData({ activeTab: tab })

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

  // CIB7-2716: the fired rules come from the outputs' ruleId, not from comparing cell texts
  describe('fired rules', () => {
    it('marks only the rule that fired, not the other rules with the same output (hit policy First)', () => {
      const table = customerTable()

      applyOutputs(table, [{ clauseId: 'result', ruleId: 'rule-3', value: true }])

      expect(firedRules(table)).toEqual(['rule-3'])
      expect(outputCell(table, 'rule-3', 'result').textContent).toBe('true = true')
      // rules 1 and 4 have the same output text but did not fire
      expect(outputCell(table, 'rule-1', 'result').textContent).toBe('true')
      expect(outputCell(table, 'rule-4', 'result').textContent).toBe('true')
    })

    it('marks every rule that fired when the hit policy collects several', () => {
      const table = customerTable()

      applyOutputs(table, [
        { clauseId: 'result', ruleId: 'rule-1', value: true },
        { clauseId: 'result', ruleId: 'rule-5', value: false }
      ])

      expect(firedRules(table)).toEqual(['rule-1', 'rule-5'])
      expect(outputCell(table, 'rule-1', 'result').textContent).toBe('true = true')
      expect(outputCell(table, 'rule-5', 'result').textContent).toBe('false = false')
      expect(outputCell(table, 'rule-3', 'result').textContent).toBe('true')
    })

    it('puts each output value into its own column of the fired rule', () => {
      const table = decisionTable({
        inputs: ['amount'],
        outputs: ['approver', 'limit'],
        rules: [
          { id: 'rule-a', inputs: ['&lt; 1000'], outputs: ['"clerk"', '1000'] },
          { id: 'rule-b', inputs: ['&gt;= 1000'], outputs: ['"manager"', 'amount * 2'] }
        ]
      })

      applyOutputs(table, [
        { clauseId: 'approver', ruleId: 'rule-b', value: 'manager' },
        { clauseId: 'limit', ruleId: 'rule-b', value: 3000 }
      ])

      expect(firedRules(table)).toEqual(['rule-b'])
      expect(outputCell(table, 'rule-b', 'approver').textContent).toBe('"manager" = manager')
      // the value the expression produced, not the expression again
      expect(outputCell(table, 'rule-b', 'limit').textContent).toBe('amount * 2 = 3000')
      expect(outputCell(table, 'rule-a', 'approver').textContent).toBe('"clerk"')
    })

    it('shows an object value as JSON', () => {
      const table = customerTable()

      applyOutputs(table, [{ clauseId: 'result', ruleId: 'rule-3', value: { approved: true } }])

      expect(outputCell(table, 'rule-3', 'result').textContent).toBe('true = {"approved":true}')
    })

    it('uses the only output cell of the rule when the output carries no column id', () => {
      const table = customerTable()

      applyOutputs(table, [{ clauseId: null, ruleId: 'rule-3', value: true }])

      expect(outputCell(table, 'rule-3', 'result').textContent).toBe('true = true')
    })

    it('writes no value when the output carries no column id and the rule has several output cells', () => {
      const table = decisionTable({
        inputs: ['amount'],
        outputs: ['approver', 'limit'],
        rules: [{ id: 'rule-a', inputs: ['&lt; 1000'], outputs: ['"clerk"', '1000'] }]
      })

      applyOutputs(table, [{ clauseId: null, ruleId: 'rule-a', value: 'clerk' }])

      expect(firedRules(table)).toEqual(['rule-a'])
      expect(table.querySelectorAll('[data-original-text]')).toHaveLength(0)
    })

    it('shows a null result, but no value for an output that was not fetched or failed', () => {
      const table = decisionTable({
        inputs: ['amount'],
        outputs: ['approver', 'document', 'limit'],
        rules: [{ id: 'rule-a', inputs: ['&lt; 1000'], outputs: ['null', 'doc', 'limitOf(amount)'] }]
      })

      applyOutputs(table, [
        { clauseId: 'approver', ruleId: 'rule-a', value: null },
        // binary values are not fetched (disableBinaryFetching), so the value is missing
        { clauseId: 'document', ruleId: 'rule-a' },
        { clauseId: 'limit', ruleId: 'rule-a', value: null, errorMessage: 'Cannot serialize value' }
      ])

      expect(firedRules(table)).toEqual(['rule-a'])
      expect(outputCell(table, 'rule-a', 'approver').textContent).toBe('null = null')
      expect(outputCell(table, 'rule-a', 'document').textContent).toBe('doc')
      expect(outputCell(table, 'rule-a', 'limit').textContent).toBe('limitOf(amount)')
    })

    it('marks nothing when the outputs name no rule', () => {
      const table = customerTable()

      applyOutputs(table, [{ clauseId: 'result', value: true }])
      applyOutputs(table, [])

      expect(firedRules(table)).toEqual([])
      expect(table.querySelectorAll('[data-original-text]')).toHaveLength(0)
    })

    it('does not repeat the value when the table is marked again after a view change', () => {
      const table = customerTable()
      const outputs = [{ clauseId: 'result', ruleId: 'rule-3', value: true }]

      applyOutputs(table, outputs)
      applyOutputs(table, outputs)

      expect(firedRules(table)).toEqual(['rule-3'])
      expect(outputCell(table, 'rule-3', 'result').textContent).toBe('true = true')
    })

    it('marks inputs and fired rules on the table inside the diagram viewer', () => {
      const table = customerTable()
      const container = document.createElement('div')
      container.appendChild(table)
      const instance = {
        inputs: [{ clauseId: 'isCtmOrder', value: false }, { clauseId: 'count', value: { pieces: 1 } }],
        outputs: [{ clauseId: 'result', ruleId: 'rule-3', value: true }]
      }

      applyInstanceValues.call({
        $refs: { diagram: { $refs: { diagram: container } } },
        instance, applyInputHeaders, applyOutputRows, applyHighlightText, formatValue
      })

      expect(firedRules(table)).toEqual(['rule-3'])
      expect(table.querySelector('th[data-col-id="isCtmOrder"] div.clause').textContent).toBe('isCtmOrder = false')
      // inputs and outputs format their values the same way
      expect(table.querySelector('th[data-col-id="count"] div.clause').textContent).toBe('count = {"pieces":1}')
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
        $root: { config: { deepLinks: { decisionInstance: [{ id: 'myExternalLinkId', url: 'https://external.example', type: 'tab' }] } } },
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
        $root: { config: { deepLinks: { decisionInstance: [{ id: 'myExternalLinkId', url: 'https://external.example', type: 'tab' }] } } },
        $t: () => 'My External Link'
      }
      const tabs = DecisionInstance.computed.tabs.call(context)
      expect(tabs.at(-1)).toEqual({ id: 'myExternalLinkId', text: 'My External Link' })
    })

    it('ignores a configured deep link whose type is not "tab"', () => {
      const context = {
        $root: { config: { deepLinks: { decisionInstance: [{ id: 'myButtonLinkId', url: 'https://external.example', type: 'button' }] } } },
        $t: key => key
      }
      const tabs = DecisionInstance.computed.tabs.call(context)
      expect(tabs).toEqual([
        { id: 'inputs', text: 'decision.inputs' },
        { id: 'outputs', text: 'decision.outputs' }
      ])
    })

    it('drops a deep link entry that collides with a built-in tab id', () => {
      const context = { $root: { config: { deepLinks: { decisionInstance: [
        { id: 'outputs', url: 'https://external.example', type: 'tab' }
      ] } } } }
      const tabs = DecisionInstance.computed.tabs.call(context)
      expect(tabs).toEqual([
        { id: 'inputs', text: 'decision.inputs' },
        { id: 'outputs', text: 'decision.outputs' }
      ])
    })
  })

  describe('hasDeepLinks', () => {
    it('returns true only when a button-type decisionInstance deep link is configured', () => {
      const tabOnly = { $root: { config: { deepLinks: { decisionInstance: [{ id: 'tabLink', url: 'https://external.example', type: 'tab' }] } } } }
      const buttonLink = { $root: { config: { deepLinks: { decisionInstance: [{ id: 'buttonLink', url: 'https://external.example', type: 'button' }] } } } }
      expect(DecisionInstance.computed.hasDeepLinks.call(tabOnly)).toBe(false)
      expect(DecisionInstance.computed.hasDeepLinks.call(buttonLink)).toBe(true)
    })
  })

  describe('matchedDeepLink', () => {
    it('returns the deep link entry matching the active tab', () => {
      const context = {
        activeTab: 'myExternalLinkId',
        $root: { config: { deepLinks: { decisionInstance: [{ id: 'myExternalLinkId', url: 'https://external.example', type: 'tab' }] } } }
      }
      expect(DecisionInstance.computed.matchedDeepLink.call(context)).toEqual({ id: 'myExternalLinkId', url: 'https://external.example', type: 'tab', text: 'deepLinks.decisionInstance.myExternalLinkId.title' })
    })

    it('returns undefined when the active tab is a built-in tab', () => {
      const context = { activeTab: 'inputs', $root: { config: {} } }
      expect(DecisionInstance.computed.matchedDeepLink.call(context)).toBeUndefined()
    })

    it('returns undefined when the matching entry is a button-type link', () => {
      const context = {
        activeTab: 'myButtonLinkId',
        $root: { config: { deepLinks: { decisionInstance: [{ id: 'myButtonLinkId', url: 'https://external.example', type: 'button' }] } } }
      }
      expect(DecisionInstance.computed.matchedDeepLink.call(context)).toBeUndefined()
    })
  })

  describe('matchedDeepLinkParams', () => {
    it('builds decision instance and decision definition context (with distinct tenant ids) and language params', () => {
      const context = {
        instance: {
          id: 'inst-1',
          tenantId: 'instance-tenant',
          processInstanceId: 'pi-1',
          decisionDefinitionId: 'dec-1',
          decisionDefinitionKey: 'myDecision'
        },
        decisionDefinition: { tenantId: 'definition-tenant' },
        currentLanguage: () => 'en'
      }
      expect(DecisionInstance.computed.matchedDeepLinkParams.call(context)).toEqual({
        decisionInstanceId: 'inst-1',
        decisionInstanceTenantId: 'instance-tenant',
        processInstanceId: 'pi-1',
        decisionDefinitionId: 'dec-1',
        decisionDefinitionKey: 'myDecision',
        decisionDefinitionTenantId: 'definition-tenant',
        lang: 'en'
      })
    })
  })
})
