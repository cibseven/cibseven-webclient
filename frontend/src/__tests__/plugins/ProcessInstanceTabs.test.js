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
import ProcessInstanceTabs from '@/components/process/ProcessInstanceTabs.vue'
import { registerPlugin, resetPlugins } from '@/plugins/pluginsConfig.js'

const BUILTIN_TABS = ['variables', 'incidents', 'usertasks', 'jobs',
  'calledProcessInstances', 'externalTasks']

function tabIds(wrapper) {
  return wrapper.findComponent({ name: 'GenericTabs' }).props('tabs').map(tab => tab.id)
}

describe('ProcessInstanceTabs', () => {
  beforeEach(() => {
    resetPlugins()
  })

  it('shows only the built-in tabs without plugins', () => {
    const wrapper = shallowMount(ProcessInstanceTabs, { props: { modelValue: 'variables' } })

    expect(tabIds(wrapper)).toEqual(BUILTIN_TABS)
  })

  it('appends a tab contributed by a plugin', () => {
    registerPlugin('process-instance-tab', { name: 'DemoStats' },
      { pluginId: 'demo-stats', id: 'demo-stats', text: 'plugins.demo-stats.title' })
    const wrapper = shallowMount(ProcessInstanceTabs, { props: { modelValue: 'variables' } })

    expect(tabIds(wrapper)).toEqual([...BUILTIN_TABS, 'demo-stats'])
    expect(wrapper.findComponent({ name: 'GenericTabs' }).props('tabs').at(-1))
      .toEqual({ id: 'demo-stats', text: 'plugins.demo-stats.title' })
  })

  it('keeps the built-in tabs first, whatever plugins are deployed', () => {
    registerPlugin('process-instance-tab', { name: 'A' }, { id: 'a', text: 'a.title' })
    registerPlugin('process-instance-tab', { name: 'B' }, { id: 'b', text: 'b.title' })
    const wrapper = shallowMount(ProcessInstanceTabs, { props: { modelValue: 'variables' } })

    expect(tabIds(wrapper)).toEqual([...BUILTIN_TABS, 'a', 'b'])
  })

  it('ignores a contribution that declares no tab label', () => {
    registerPlugin('process-instance-tab', { name: 'NoLabel' }, { id: 'no-label' })
    const wrapper = shallowMount(ProcessInstanceTabs, { props: { modelValue: 'variables' } })

    expect(tabIds(wrapper)).toEqual(BUILTIN_TABS)
  })

  it('ignores contributions to other slots', () => {
    registerPlugin('some-other-slot', { name: 'Elsewhere' }, { id: 'elsewhere', text: 'x' })
    const wrapper = shallowMount(ProcessInstanceTabs, { props: { modelValue: 'variables' } })

    expect(tabIds(wrapper)).toEqual(BUILTIN_TABS)
  })

  it('picks up a tab registered after the tab bar was rendered', async () => {
    const wrapper = shallowMount(ProcessInstanceTabs, { props: { modelValue: 'variables' } })
    expect(tabIds(wrapper)).toEqual(BUILTIN_TABS)

    registerPlugin('process-instance-tab', { name: 'Late' }, { id: 'late', text: 'late.title' })
    await flushPromises()

    expect(tabIds(wrapper)).toEqual([...BUILTIN_TABS, 'late'])
  })
})
