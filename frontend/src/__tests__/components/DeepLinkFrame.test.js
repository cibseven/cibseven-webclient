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

describe('DeepLinkFrame.vue', () => {
  it('renders an iframe pointing at the given url', () => {
    const wrapper = mount(DeepLinkFrame, { props: { url: 'https://external.example/app?processInstanceId=pi-1' } })
    const iframe = wrapper.find('iframe')
    expect(iframe.exists()).toBe(true)
    expect(iframe.attributes('src')).toBe('https://external.example/app?processInstanceId=pi-1')
  })

  it('defaults title to an empty string when not provided', () => {
    const wrapper = mount(DeepLinkFrame, { props: { url: 'https://external.example/app' } })
    expect(wrapper.find('iframe').attributes('title')).toBe('')
  })

  it('sets the iframe title when provided', () => {
    const wrapper = mount(DeepLinkFrame, { props: { url: 'https://external.example/app', title: 'VHV Output' } })
    expect(wrapper.find('iframe').attributes('title')).toBe('VHV Output')
  })
})
