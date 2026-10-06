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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import PluginSlot from '@/components/common/PluginSlot.vue'
import { registerPlugin, resetPlugins } from '@/plugins/pluginsConfig.js'

const Contribution = {
  name: 'Contribution',
  props: { instanceId: { type: String, default: '' } },
  template: '<span class="contribution">contribution {{ instanceId }}</span>'
}

describe('PluginSlot', () => {
  beforeEach(() => {
    resetPlugins()
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  it('renders nothing when no plugin contributed to the slot', () => {
    const wrapper = mount(PluginSlot, { props: { name: 'demo' } })

    expect(wrapper.text()).toBe('')
  })

  it('renders a registered contribution', () => {
    registerPlugin('demo', Contribution, { pluginId: 'demo-plugin' })
    const wrapper = mount(PluginSlot, { props: { name: 'demo' } })

    expect(wrapper.find('.contribution').exists()).toBe(true)
  })

  it('renders contributions of several plugins', () => {
    registerPlugin('demo', Contribution, { pluginId: 'a' })
    registerPlugin('demo', Contribution, { pluginId: 'b' })
    const wrapper = mount(PluginSlot, { props: { name: 'demo' } })

    expect(wrapper.findAll('.contribution')).toHaveLength(2)
  })

  // All of them used to render under the plugin's id: one duplicated key, which
  // makes Vue warn and reuse the wrong element as soon as the list changes.
  it('renders every contribution of one plugin, each under its own key', async () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    registerPlugin('demo', Contribution, { pluginId: 'one-plugin' })
    registerPlugin('demo', Contribution, { pluginId: 'one-plugin' })
    const wrapper = mount(PluginSlot, { props: { name: 'demo' } })

    registerPlugin('demo', Contribution, { pluginId: 'one-plugin' })
    await flushPromises()

    expect(wrapper.findAll('.contribution')).toHaveLength(3)
    const warnings = warn.mock.calls.map(call => String(call[0]))
    expect(warnings.filter(message => message.includes('Duplicate keys'))).toEqual([])
  })

  it('ignores contributions to other slots', () => {
    registerPlugin('other', Contribution, { pluginId: 'a' })
    const wrapper = mount(PluginSlot, { props: { name: 'demo' } })

    expect(wrapper.find('.contribution').exists()).toBe(false)
  })

  it('hands the slot params to every contribution', () => {
    registerPlugin('demo', Contribution, { pluginId: 'a' })
    const wrapper = mount(PluginSlot, {
      props: { name: 'demo', params: { instanceId: 'instance-1' } }
    })

    expect(wrapper.text()).toContain('instance-1')
  })

  it('picks up a contribution registered after mounting', async () => {
    const wrapper = mount(PluginSlot, { props: { name: 'demo' } })
    expect(wrapper.find('.contribution').exists()).toBe(false)

    registerPlugin('demo', Contribution, { pluginId: 'late' })
    await flushPromises()

    expect(wrapper.find('.contribution').exists()).toBe(true)
  })

  describe('only', () => {
    beforeEach(() => {
      registerPlugin('demo', Contribution, { pluginId: 'stats', id: 'demo-stats' })
      registerPlugin('demo', Contribution, { pluginId: 'other', id: 'other-tab' })
    })

    it('renders just the contribution matching the given id', () => {
      const wrapper = mount(PluginSlot, { props: { name: 'demo', only: 'demo-stats' } })

      expect(wrapper.findAll('.contribution')).toHaveLength(1)
    })

    it('renders nothing while another id is selected', () => {
      const wrapper = mount(PluginSlot, { props: { name: 'demo', only: 'variables' } })

      expect(wrapper.find('.contribution').exists()).toBe(false)
    })

    it('follows the selected id when it changes', async () => {
      const wrapper = mount(PluginSlot, { props: { name: 'demo', only: 'variables' } })
      expect(wrapper.find('.contribution').exists()).toBe(false)

      await wrapper.setProps({ only: 'other-tab' })

      expect(wrapper.findAll('.contribution')).toHaveLength(1)
    })

    it('skips contributions that declare no id', () => {
      registerPlugin('demo', Contribution, { pluginId: 'anonymous' })
      const wrapper = mount(PluginSlot, { props: { name: 'demo', only: 'demo-stats' } })

      expect(wrapper.findAll('.contribution')).toHaveLength(1)
    })
  })

  it('drops a failing contribution and keeps the working one', async () => {
    const Failing = {
      name: 'Failing',
      template: '<span class="failing">failing</span>',
      mounted() { throw new Error('plugin is broken') }
    }
    registerPlugin('demo', Failing, { pluginId: 'broken' })
    registerPlugin('demo', Contribution, { pluginId: 'working' })

    const wrapper = mount(PluginSlot, { props: { name: 'demo' } })
    await flushPromises()

    expect(wrapper.find('.failing').exists()).toBe(false)
    expect(wrapper.find('.contribution').exists()).toBe(true)
    expect(console.error).toHaveBeenCalledWith(
      expect.stringContaining('broken'), expect.any(Error))
  })
})
