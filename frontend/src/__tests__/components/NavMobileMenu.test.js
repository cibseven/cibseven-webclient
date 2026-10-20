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
import NavMobileMenu from '@/components/common-components/navigation/NavMobileMenu.vue'

const tasks = { id: 'tasks', label: 'Tasks', icon: 'mdi-clipboard-text-outline', to: '/seven/auth/tasks', sections: [[{ key: 'list', label: 'Task list', to: '/seven/auth/tasks' }]] }
const cockpit = { id: 'cockpit', label: 'Cockpit', icon: 'mdi-speedometer', to: '/seven/auth/processes', active: true, sections: [[{ key: 'dash', label: 'Dashboard', to: '/seven/auth/processes', active: true }], [{ key: 'dec', label: 'Decisions', to: '/seven/auth/decisions/list' }]] }
const help = { id: 'help', label: 'Help', icon: 'mdi-help-circle-outline', sections: [[{ key: 'docs', label: 'Docs', href: 'https://docs', external: true }, { key: 'about', label: 'About', modal: 'about' }]] }
const user = { id: 'user', label: 'Ada Morris', avatar: 'AM', sections: [[{ key: 'logout', label: 'Logout', logout: true }]] }
const feedback = { id: 'feedback', label: 'Feedback', icon: 'mdi-message-alert-outline', modal: 'report' }

const mountMenu = (props) => mount(NavMobileMenu, {
  props,
  global: {
    mocks: { $t: key => key },
    stubs: { 'router-link': { props: ['to'], template: '<a class="router-link" href="#"><slot></slot></a>' } }
  }
})

describe('NavMobileMenu.vue', () => {
  it('lists main clusters, then the standard ones behind a separator', () => {
    const wrapper = mountMenu({ main: [tasks, cockpit], utility: [help, user] })

    expect(wrapper.findAll('.cib-nav-mobile-label').map(l => l.text())).toEqual(['Tasks', 'Cockpit', 'Help', 'Ada Morris'])
    expect(wrapper.findAll('.cib-nav-mobile-separator')).toHaveLength(1)
  })

  it('omits the separator when there are no standard clusters', () => {
    const wrapper = mountMenu({ main: [tasks] })
    expect(wrapper.find('.cib-nav-mobile-separator').exists()).toBe(false)
  })

  it('opens with the current section expanded and its active item marked', () => {
    const wrapper = mountMenu({ main: [tasks, cockpit] })

    const rows = wrapper.findAll('.cib-nav-mobile-row')
    expect(rows.map(r => r.attributes('aria-expanded'))).toEqual(['false', 'true'])
    expect(wrapper.findAll('.cib-nav-mobile-section')).toHaveLength(2)
    expect(wrapper.find('.cib-nav-mobile-item.active').text()).toBe('Dashboard')
  })

  it('expands a row instead of navigating, one row at a time', async () => {
    const wrapper = mountMenu({ main: [tasks, cockpit] })

    await wrapper.findAll('.cib-nav-mobile-row')[0].trigger('click')

    expect(wrapper.findAll('.cib-nav-mobile-row').map(r => r.attributes('aria-expanded'))).toEqual(['true', 'false'])
    expect(wrapper.emitted('navigate')).toBeUndefined()

    await wrapper.findAll('.cib-nav-mobile-row')[0].trigger('click')
    expect(wrapper.find('.cib-nav-mobile-section').exists()).toBe(false)
  })

  it('reports navigation and passes action items on', async () => {
    const wrapper = mountMenu({ main: [], utility: [help] })
    await wrapper.find('.cib-nav-mobile-row').trigger('click')

    const external = wrapper.find('a.cib-nav-mobile-item')
    expect(external.attributes('target')).toBe('_blank')
    await external.trigger('click')
    expect(wrapper.emitted('navigate')).toHaveLength(1)

    await wrapper.find('button.cib-nav-mobile-item').trigger('click')
    expect(wrapper.emitted('select')).toEqual([[help.sections[0][1]]])
  })

  it('shows a cluster without menu as a plain action row', async () => {
    const wrapper = mountMenu({ main: [], utility: [feedback] })

    const row = wrapper.find('.cib-nav-mobile-row')
    expect(row.attributes('aria-expanded')).toBeUndefined()
    await row.trigger('click')

    expect(wrapper.emitted('select')).toEqual([[feedback]])
  })

  it('shows a cluster without menu but with a route as a link', async () => {
    const wrapper = mountMenu({ main: [{ ...tasks, sections: [] }] })

    await wrapper.find('a.cib-nav-mobile-row').trigger('click')

    expect(wrapper.emitted('navigate')).toHaveLength(1)
  })

  it('shows the account icon for the user row', () => {
    const wrapper = mountMenu({ main: [], utility: [user] })
    expect(wrapper.find('.cib-nav-mobile-row .mdi-account').exists()).toBe(true)
  })
})
