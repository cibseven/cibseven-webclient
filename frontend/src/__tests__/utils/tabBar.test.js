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
import { defineTabBar } from '@/utils/tabBar.js'
import { registerPlugin, resetPlugins } from '@/plugins/pluginsConfig.js'

const BUILTIN = [
  { id: 'variables', text: 'process.variables' },
  { id: 'jobs', text: 'process.jobs' }
]

const deepLinkConfig = section => ({
  deepLinks: { [section]: [{ id: 'audit', url: 'https://example', type: 'tab' }] }
})

describe('defineTabBar', () => {
  beforeEach(() => {
    resetPlugins()
  })

  it('hands back the bar own tabs when nothing else is configured', () => {
    const tabsFor = defineTabBar({ deepLinkSection: 'aSection', pluginSlot: 'a-slot', builtin: BUILTIN })

    expect(tabsFor({ config: {} })).toEqual(BUILTIN)
  })

  /** Built-in first, then what the configuration adds, then what is deployed. */
  it('puts the built-in tabs first, then deep links, then contributions', () => {
    const tabsFor = defineTabBar({ deepLinkSection: 'bSection', pluginSlot: 'b-slot', builtin: BUILTIN })
    registerPlugin('b-slot', { name: 'Demo' }, { id: 'demo', text: 'demo.title' })

    expect(tabsFor({ config: deepLinkConfig('bSection'), t: key => key }).map(tab => tab.id))
      .toEqual(['variables', 'jobs', 'audit', 'demo'])
  })

  it('reserves the built-in ids against plugins', () => {
    const tabsFor = defineTabBar({ deepLinkSection: 'cSection', pluginSlot: 'c-slot', builtin: BUILTIN })
    registerPlugin('c-slot', { name: 'FakeJobs' }, { id: 'jobs', text: 'fake.title' })

    expect(tabsFor({ config: {} }).map(tab => tab.id)).toEqual(['variables', 'jobs'])
  })

  /**
   * A bar whose tabs depend on the configuration renders a shorter list than it reserves, so a
   * plugin cannot take an id that appears only on some installations.
   */
  it('reserves ids the bar does not always render', () => {
    const tabsFor = defineTabBar({
      deepLinkSection: 'dSection',
      pluginSlot: 'd-slot',
      builtin: BUILTIN,
      reservedIds: [...BUILTIN.map(tab => tab.id), 'sometimes']
    })
    registerPlugin('d-slot', { name: 'Sometimes' }, { id: 'sometimes', text: 'x' })

    expect(tabsFor({ config: {} }).map(tab => tab.id)).toEqual(['variables', 'jobs'])
  })

  it('takes the tabs to render from the call, for a bar that decides them at runtime', () => {
    const tabsFor = defineTabBar({ deepLinkSection: 'eSection', pluginSlot: 'e-slot', builtin: BUILTIN })
    const withExtra = [...BUILTIN, { id: 'extra', text: 'process.extra' }]

    expect(tabsFor({ builtin: withExtra, config: {} }).map(tab => tab.id))
      .toEqual(['variables', 'jobs', 'extra'])
  })

  it('ignores contributions to other slots', () => {
    const tabsFor = defineTabBar({ deepLinkSection: 'fSection', pluginSlot: 'f-slot', builtin: BUILTIN })
    registerPlugin('another-slot', { name: 'Elsewhere' }, { id: 'elsewhere', text: 'x' })

    expect(tabsFor({ config: {} })).toEqual(BUILTIN)
  })

  it('ignores a contribution that declares no label', () => {
    const tabsFor = defineTabBar({ deepLinkSection: 'gSection', pluginSlot: 'g-slot', builtin: BUILTIN })
    registerPlugin('g-slot', { name: 'NoLabel' }, { id: 'no-label' })

    expect(tabsFor({ config: {} })).toEqual(BUILTIN)
  })

  /** The bars call this from a computed, so a missing config must not throw. */
  it('survives a call without a configuration', () => {
    const tabsFor = defineTabBar({ deepLinkSection: 'hSection', pluginSlot: 'h-slot', builtin: BUILTIN })

    expect(tabsFor()).toEqual(BUILTIN)
  })
})
