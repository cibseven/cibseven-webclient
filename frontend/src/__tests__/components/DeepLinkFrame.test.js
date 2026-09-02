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
import { mount } from '@vue/test-utils'
import DeepLinkFrame from '@/components/common-components/DeepLinkFrame.vue'

function createWrapper(props, { te = false, t = key => key } = {}) {
  return mount(DeepLinkFrame, {
    props,
    global: { mocks: { $t: t, $te: te instanceof Function ? te : () => te } }
  })
}

describe('DeepLinkFrame.vue', () => {
  it('resolves the url from the link and appends the given params', () => {
    const wrapper = createWrapper({
      link: { id: 'vhvOutput', url: 'https://external.example/app', text: 'deepLinks.processInstance.vhvOutput.title' },
      params: { processInstanceId: 'pi-1' }
    })
    const iframe = wrapper.find('iframe')
    expect(iframe.exists()).toBe(true)
    expect(iframe.attributes('src')).toBe('https://external.example/app?processInstanceId=pi-1')
  })

  it('defaults params to an empty object, leaving the url unchanged', () => {
    const wrapper = createWrapper({
      link: { id: 'vhvOutput', url: 'https://external.example/app', text: 'deepLinks.processInstance.vhvOutput.title' }
    })
    expect(wrapper.find('iframe').attributes('src')).toBe('https://external.example/app')
  })

  it('falls back to the link id as title when no translation exists for the text key', () => {
    const wrapper = createWrapper({
      link: { id: 'vhvOutput', url: 'https://external.example/app', text: 'deepLinks.processInstance.vhvOutput.title' }
    }, { te: false })
    expect(wrapper.find('iframe').attributes('title')).toBe('vhvOutput')
  })

  it('uses the translated text as title when a translation exists for the text key', () => {
    const wrapper = createWrapper({
      link: { id: 'vhvOutput', url: 'https://external.example/app', text: 'deepLinks.processInstance.vhvOutput.title' }
    }, { te: true, t: () => 'VHV Output' })
    expect(wrapper.find('iframe').attributes('title')).toBe('VHV Output')
  })
})
