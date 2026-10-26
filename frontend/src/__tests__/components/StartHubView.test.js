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
import { shallowMount } from '@vue/test-utils'
import StartHubView from '@/components/start/StartHubView.vue'

const mountHub = (props) => shallowMount(StartHubView, {
  props,
  global: {
    mocks: { $t: key => key },
    mixins: [{ data: () => ({ config: {} }) }]
  }
})

describe('StartHubView.vue', () => {
  it('gives items without an image the section image', () => {
    const wrapper = mountHub({
      fallbackSrc: 'section.svg',
      items: [{ to: '/a', title: 'A', src: 'a.svg' }, { to: { name: 'b' }, title: 'B' }]
    })

    const tiles = wrapper.findAllComponents({ name: 'StartViewItem' })
    expect(tiles.map(t => t.props('src'))).toEqual(['a.svg', 'section.svg'])
  })

  it('renders items that share a title as separate tiles', () => {
    const wrapper = mountHub({ items: [{ to: '/a', title: 'Same' }, { to: '/b', title: 'Same' }] })
    expect(wrapper.findAllComponents({ name: 'StartViewItem' }).map(t => t.props('to'))).toEqual(['/a', '/b'])
  })

  it('shows the empty state without items', () => {
    const wrapper = mountHub({ items: [] })
    expect(wrapper.findComponent({ name: 'StartViewItem' }).exists()).toBe(false)
    expect(wrapper.text()).toContain('start.emptyStart')
  })
})
