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
import { shallowMount, flushPromises } from '@vue/test-utils'
import ProcessInstancesTabs from '@/components/process/ProcessInstancesTabs.vue'
import { registerPlugin, resetPlugins } from '@/plugins/pluginsConfig.js'

const BUILTIN_TABS = ['instances', 'jobDefinitions', 'incidents', 'calledProcessDefinitions']

const mountOptions = { props: { modelValue: 'instances' } }

function tabIds(wrapper) {
  return wrapper.findComponent({ name: 'GenericTabs' }).props('tabs').map(tab => tab.id)
}

describe('ProcessInstancesTabs', () => {
  beforeEach(() => {
    resetPlugins()
  })

  it('shows only the built-in tabs without plugins', () => {
    const wrapper = shallowMount(ProcessInstancesTabs, mountOptions)

    expect(tabIds(wrapper)).toEqual(BUILTIN_TABS)
  })

  it('appends a tab contributed by a plugin', () => {
    registerPlugin('process-definition-tab', { name: 'DemoStats' },
      { pluginId: 'demo-stats', id: 'demo-stats', text: 'plugins.demo-stats.title' })
    const wrapper = shallowMount(ProcessInstancesTabs, mountOptions)

    expect(tabIds(wrapper)).toEqual([...BUILTIN_TABS, 'demo-stats'])
    expect(wrapper.findComponent({ name: 'GenericTabs' }).props('tabs').at(-1))
      .toEqual({ id: 'demo-stats', text: 'plugins.demo-stats.title' })
  })

  it('keeps the built-in tabs first, whatever plugins are deployed', () => {
    registerPlugin('process-definition-tab', { name: 'A' }, { id: 'a', text: 'a.title' })
    registerPlugin('process-definition-tab', { name: 'B' }, { id: 'b', text: 'b.title' })
    const wrapper = shallowMount(ProcessInstancesTabs, mountOptions)

    expect(tabIds(wrapper)).toEqual([...BUILTIN_TABS, 'a', 'b'])
  })

  /**
   * A tab id becomes ?tab=<id>, so a plugin taking one of the built-in ids would
   * add a second tab under it and make the selection ambiguous.
   */
  it('refuses a contribution taking the id of a built-in tab', () => {
    registerPlugin('process-definition-tab', { name: 'FakeIncidents' },
      { id: 'incidents', text: 'plugins.demo.title' })
    const wrapper = shallowMount(ProcessInstancesTabs, mountOptions)

    expect(tabIds(wrapper)).toEqual(BUILTIN_TABS)
  })

  it('ignores a contribution that declares no tab label', () => {
    registerPlugin('process-definition-tab', { name: 'NoLabel' }, { id: 'no-label' })
    const wrapper = shallowMount(ProcessInstancesTabs, mountOptions)

    expect(tabIds(wrapper)).toEqual(BUILTIN_TABS)
  })

  /** The instance bar is a different slot, so its contributions must not appear here. */
  it('ignores contributions to the instance tab slot', () => {
    registerPlugin('process-instance-tab', { name: 'Elsewhere' }, { id: 'elsewhere', text: 'x' })
    const wrapper = shallowMount(ProcessInstancesTabs, mountOptions)

    expect(tabIds(wrapper)).toEqual(BUILTIN_TABS)
  })

  it('picks up a tab registered after the tab bar was rendered', async () => {
    const wrapper = shallowMount(ProcessInstancesTabs, mountOptions)
    expect(tabIds(wrapper)).toEqual(BUILTIN_TABS)

    registerPlugin('process-definition-tab', { name: 'Late' }, { id: 'late', text: 'late.title' })
    await flushPromises()

    expect(tabIds(wrapper)).toEqual([...BUILTIN_TABS, 'late'])
  })
})
