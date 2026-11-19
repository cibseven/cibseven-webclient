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
import { describe, it, expect, afterEach } from 'vitest'
import { mountWithDefaults } from '../support/mountWithDefaults.js'
import CellActionButton from '@/components/common-components/CellActionButton.vue'

// The "simple icon" look (CIB7-1619) lives in the scoped CSS, which jsdom does not apply;
// these tests pin the markup the CSS hangs on and the click behaviour.
describe('CellActionButton', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
  })

  it('renders a link-style button with the icon and title', () => {
    wrapper = mountWithDefaults(CellActionButton, { props: { title: 'Copy link', icon: 'mdi-link' } })
    const button = wrapper.find('button')
    expect(button.classes()).toEqual(expect.arrayContaining(['btn', 'btn-link', 'hovered-button']))
    expect(button.attributes('title')).toBe('Copy link')
    expect(button.find('.mdi.mdi-18px.mdi-link').exists()).toBe(true)
  })

  it('renders slot content without an icon', () => {
    wrapper = mountWithDefaults(CellActionButton, { props: { title: 'More' }, slots: { default: 'Load more' } })
    expect(wrapper.find('.mdi').exists()).toBe(false)
    expect(wrapper.find('button').text()).toBe('Load more')
  })

  it('emits click and stops propagation', async () => {
    let parentClicks = 0
    wrapper = mountWithDefaults({
      components: { CellActionButton },
      template: '<div @click="parentClick"><CellActionButton title="Clear" icon="mdi-close" @click="onClick"/></div>',
      data: () => ({ clicks: 0 }),
      methods: {
        parentClick() { parentClicks++ },
        onClick() { this.clicks++ }
      }
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.vm.clicks).toBe(1)
    expect(parentClicks).toBe(0)
  })

  it('passes attributes such as disabled and extra classes to the button', () => {
    wrapper = mountWithDefaults(CellActionButton, {
      props: { title: 'Search', icon: 'mdi-magnify' },
      attrs: { disabled: true, class: 'p-0' }
    })
    const button = wrapper.find('button')
    expect(button.attributes('disabled')).toBeDefined()
    expect(button.classes()).toContain('p-0')
  })

  it('renders a router link with the same hover class when "to" is set', () => {
    wrapper = mountWithDefaults(CellActionButton, { props: { title: 'Open', icon: 'mdi-open-in-new', to: '/seven/auth/processes' } })
    const link = wrapper.find('a')
    expect(link.classes()).toContain('hovered-link')
    expect(link.attributes('title')).toBe('Open')
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('does not emit click for a router link', async () => {
    wrapper = mountWithDefaults(CellActionButton, { props: { title: 'Open', to: '/seven/auth/processes' } })
    wrapper.vm.onClick(new Event('click'))
    expect(wrapper.emitted('click')).toBeUndefined()
  })
})
