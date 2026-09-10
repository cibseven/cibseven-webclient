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
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import DeepLinkButtons from '@/components/common-components/DeepLinkButtons.vue'

function createWrapper(props, { config = {}, t = key => key } = {}) {
  return mount(DeepLinkButtons, {
    props: { params: {}, ...props },
    global: {
      mocks: {
        $t: t,
        // DeepLinkButtons reads `this.$root.config`; mounted standalone (no parent),
        // $root resolves to this very instance, so mocking `config` directly is equivalent.
        config
      }
    }
  })
}

describe('DeepLinkButtons.vue', () => {
  let openSpy

  beforeEach(() => {
    openSpy = vi.spyOn(window, 'open').mockImplementation(() => {})
  })

  afterEach(() => {
    openSpy.mockRestore()
  })

  it('renders only button-type deep link entries for the given section', () => {
    const config = { deepLinks: { processInstance: [
      { id: 'tabLink', url: 'https://external.example/tab', type: 'tab' },
      { id: 'buttonLink', url: 'https://external.example/button', type: 'button' }
    ] } }
    const wrapper = createWrapper({ section: 'processInstance' }, { config })
    const buttons = wrapper.findAll('button')
    expect(buttons).toHaveLength(1)
    expect(buttons[0].text()).toBe('buttonLink')
  })

  it('renders no buttons when no deep links are configured', () => {
    const wrapper = createWrapper({ section: 'processInstance' }, { config: {} })
    expect(wrapper.findAll('button')).toHaveLength(0)
  })

  it('uses the translated label when a translation exists', () => {
    const config = { deepLinks: { processInstance: [
      { id: 'buttonLink', url: 'https://external.example/button', type: 'button' }
    ] } }
    const wrapper = createWrapper({ section: 'processInstance' }, { config, t: () => 'Button Link' })
    expect(wrapper.find('button').text()).toBe('Button Link')
  })

  it('opens the link url in a new window when clicked', async () => {
    const config = { deepLinks: { processInstance: [
      { id: 'buttonLink', url: 'https://external.example/button', type: 'button' }
    ] } }
    const wrapper = createWrapper({ section: 'processInstance' }, { config })
    await wrapper.find('button').trigger('click')
    expect(openSpy).toHaveBeenCalledWith('https://external.example/button', '_blank')
  })

  it('appends the given params to the url when opening it', async () => {
    const config = { deepLinks: { processInstance: [
      { id: 'buttonLink', url: 'https://external.example/button', type: 'button' }
    ] } }
    const wrapper = createWrapper(
      { section: 'processInstance', params: { processInstanceId: 'pi-1' } },
      { config }
    )
    await wrapper.find('button').trigger('click')
    expect(openSpy).toHaveBeenCalledWith('https://external.example/button?processInstanceId=pi-1', '_blank')
  })

  it('opens the url in the configured target window instead of _blank', async () => {
    const config = { deepLinks: { processInstance: [
      { id: 'buttonLink', url: 'https://external.example/button', type: 'button', target: 'myWindow' }
    ] } }
    const wrapper = createWrapper({ section: 'processInstance' }, { config })
    await wrapper.find('button').trigger('click')
    expect(openSpy).toHaveBeenCalledWith('https://external.example/button', 'myWindow')
  })

  it('sets a tooltip with the resolved label and url', () => {
    const config = { deepLinks: { processInstance: [
      { id: 'buttonLink', url: 'https://external.example/button', type: 'button' }
    ] } }
    const wrapper = createWrapper({ section: 'processInstance' }, { config, t: key => key })
    expect(wrapper.find('button').attributes('title')).toBe('deepLink.tooltip')
  })

  it('appends the given params to the url used to build the tooltip', () => {
    const config = { deepLinks: { processInstance: [
      { id: 'buttonLink', url: 'https://external.example/button', type: 'button' }
    ] } }
    let tooltipArgs
    const wrapper = createWrapper(
      { section: 'processInstance', params: { processInstanceId: 'pi-1' } },
      { config, t: (key, args) => { tooltipArgs = args; return key } }
    )
    wrapper.find('button').attributes('title')
    expect(tooltipArgs.url).toBe('https://external.example/button?processInstanceId=pi-1')
  })
})
