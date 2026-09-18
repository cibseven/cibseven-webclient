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
import { describe, it, expect, beforeEach } from 'vitest'
import DecisionInstance from '@/components/decision/DecisionInstance.vue'
import { registerPlugin, resetPlugins } from '@/plugins/pluginsConfig.js'

const BUILTIN_TABS = ['inputs', 'outputs']

/** The component pulls in a viewer and the store, so the tab bar is read off the computed. */
function tabIds() {
  return DecisionInstance.computed.tabs.call({ $root: { config: {} } }).map(tab => tab.id)
}

describe('DecisionInstance tabs', () => {
  beforeEach(() => {
    resetPlugins()
  })

  it('shows only the built-in tabs without plugins', () => {
    expect(tabIds()).toEqual(BUILTIN_TABS)
  })

  it('appends a tab contributed by a plugin', () => {
    registerPlugin('decision-instance-tab', { name: 'DemoTrace' },
      { pluginId: 'demo-trace', id: 'demo-trace', text: 'plugins.demo-trace.title' })

    expect(tabIds()).toEqual([...BUILTIN_TABS, 'demo-trace'])
    expect(DecisionInstance.computed.tabs.call({ $root: { config: {} } }).at(-1))
      .toEqual({ id: 'demo-trace', text: 'plugins.demo-trace.title' })
  })

  it('keeps the built-in tabs first, whatever plugins are deployed', () => {
    registerPlugin('decision-instance-tab', { name: 'A' }, { id: 'a', text: 'a.title' })
    registerPlugin('decision-instance-tab', { name: 'B' }, { id: 'b', text: 'b.title' })

    expect(tabIds()).toEqual([...BUILTIN_TABS, 'a', 'b'])
  })

  /**
   * A tab id becomes ?tab=<id>, so a plugin taking one of the built-in ids would
   * add a second tab under it and make the selection ambiguous.
   */
  it('refuses a contribution taking the id of a built-in tab', () => {
    registerPlugin('decision-instance-tab', { name: 'FakeInputs' },
      { id: 'inputs', text: 'plugins.demo.title' })

    expect(tabIds()).toEqual(BUILTIN_TABS)
  })

  it('ignores a contribution that declares no tab label', () => {
    registerPlugin('decision-instance-tab', { name: 'NoLabel' }, { id: 'no-label' })

    expect(tabIds()).toEqual(BUILTIN_TABS)
  })

  /** The definition bar is a different slot, so its contributions must not appear here. */
  it('ignores contributions to the decision definition slot', () => {
    registerPlugin('decision-definition-tab', { name: 'Elsewhere' }, { id: 'elsewhere', text: 'x' })

    expect(tabIds()).toEqual(BUILTIN_TABS)
  })

  it('keeps the deep links a configuration adds before the contributed tabs', () => {
    registerPlugin('decision-instance-tab', { name: 'Late' }, { id: 'late', text: 'late.title' })
    const context = {
      $root: { config: { deepLinks: { decisionInstance: [{ id: 'audit', url: 'https://example', type: 'tab' }] } } },
      $t: key => key
    }

    expect(DecisionInstance.computed.tabs.call(context).map(tab => tab.id))
      .toEqual([...BUILTIN_TABS, 'audit', 'late'])
  })
})
