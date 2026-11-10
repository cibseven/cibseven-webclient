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
import { describe, it, expect, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import NavCluster from '@/components/common-components/navigation/NavCluster.vue'

const cockpit = {
  id: 'cockpit',
  label: 'Cockpit',
  icon: 'mdi-speedometer',
  to: '/seven/auth/processes',
  titleTo: { name: 'cockpit' },
  sections: [
    [{ key: 'dash', label: 'Dashboard', to: '/seven/auth/processes', active: true }, { key: 'proc', label: 'Processes', to: '/seven/auth/processes/list' }],
    [{ key: 'dec', label: 'Decisions', to: '/seven/auth/decisions/list' }]
  ]
}
const language = {
  id: 'language',
  label: 'Language',
  icon: 'mdi-web',
  sections: [[{ key: 'en', tag: 'EN', label: 'English', active: true, checkable: true, language: 'en' }, { key: 'de', tag: 'DE', label: 'Deutsch', checkable: true, language: 'de' }]]
}
const feedback = { id: 'feedback', label: 'Feedback', icon: 'mdi-message-alert-outline', modal: 'report' }

// Mounts like its parent does: the open state round-trips through v-model:open
const mountCluster = (cluster, props = {}) => {
  const wrapper = mount(NavCluster, {
    attachTo: document.body,
    props: {
      cluster,
      open: false,
      'onUpdate:open': value => wrapper.setProps({ open: value }),
      ...props
    },
    global: {
      mocks: { $t: (key, params) => params ? `${key}:${params.label}` : key },
      stubs: { 'router-link': { props: ['to'], template: '<a class="router-link" :data-to="JSON.stringify(to)" href="#"><slot></slot></a>' } }
    }
  })
  return wrapper
}

describe('NavCluster.vue', () => {
  let wrapper
  afterEach(() => {
    wrapper?.unmount()
    vi.useRealTimers()
  })

  describe('split control', () => {
    it('leads the icon to the section and opens the menu from the chevron', async () => {
      wrapper = mountCluster(cockpit)

      expect(wrapper.find('.cib-nav-btn-main').attributes('data-to')).toBe('"/seven/auth/processes"')
      expect(wrapper.find('.cib-nav-menu').exists()).toBe(false)

      await wrapper.find('.cib-nav-btn-chevron').trigger('click')

      expect(wrapper.emitted('update:open')[0]).toEqual([true])
      expect(wrapper.find('.cib-nav-menu').exists()).toBe(true)
      expect(wrapper.find('.cib-nav-btn-chevron').attributes('aria-expanded')).toBe('true')
    })

    it('names the chevron after its section', () => {
      wrapper = mountCluster(cockpit)
      expect(wrapper.find('.cib-nav-btn-chevron').attributes('aria-label')).toBe('cib-header.openMenu:Cockpit')
    })

    it('shows a linked title, the sections and the active item', async () => {
      wrapper = mountCluster(cockpit, { open: true })

      expect(wrapper.find('.cib-nav-menu-title-link').attributes('data-to')).toBe('{"name":"cockpit"}')
      expect(wrapper.findAll('.cib-nav-menu-section')).toHaveLength(2)
      const active = wrapper.find('.cib-nav-menu-item.active')
      expect(active.text()).toBe('Dashboard')
      expect(active.attributes('aria-current')).toBe('page')
    })

    it('marks the current section', () => {
      wrapper = mountCluster({ ...cockpit, active: true })
      expect(wrapper.classes()).toContain('cib-nav-cluster-current')
    })

    it('shows an avatar and a badge instead of the plain icon', () => {
      wrapper = mountCluster({ ...cockpit, avatar: 'AM', badge: 3 })
      expect(wrapper.find('.cib-nav-avatar').text()).toBe('AM')
      expect(wrapper.find('.cib-nav-badge').text()).toBe('3')
    })
  })

  describe('single control', () => {
    it('opens its menu from the one button and has a plain title', async () => {
      wrapper = mountCluster(language)
      expect(wrapper.find('.cib-nav-btn-main').exists()).toBe(false)

      await wrapper.find('.cib-nav-btn-single').trigger('click')

      expect(wrapper.find('.cib-nav-menu-title-link').exists()).toBe(false)
      expect(wrapper.find('.cib-nav-menu-title').text()).toBe('Language')
      expect(wrapper.findAll('.cib-nav-menu-tag').map(t => t.text())).toEqual(['EN', 'DE'])
      expect(wrapper.findAll('[aria-checked]').map(i => i.attributes('aria-checked'))).toEqual(['true', 'false'])
    })

    it('emits a picked item and closes', async () => {
      wrapper = mountCluster(language, { open: true })

      await wrapper.findAll('button.cib-nav-menu-item')[1].trigger('click')

      expect(wrapper.emitted('select')).toEqual([[language.sections[0][1]]])
      expect(wrapper.find('.cib-nav-menu').exists()).toBe(false)
    })

    it('emits itself when it is a plain action without a menu', async () => {
      wrapper = mountCluster(feedback)

      await wrapper.find('.cib-nav-btn-single').trigger('click')

      expect(wrapper.emitted('select')).toEqual([[feedback]])
      expect(wrapper.emitted('update:open')).toBeUndefined()
      expect(wrapper.find('.cib-nav-btn-single').attributes('aria-haspopup')).toBeUndefined()
    })
  })

  describe('hover', () => {
    it('opens on mouse hover over the chevron, but not for touch', async () => {
      wrapper = mountCluster(cockpit)
      const chevron = wrapper.find('.cib-nav-btn-chevron')

      await chevron.trigger('pointerenter', { pointerType: 'touch' })
      expect(wrapper.emitted('update:open')).toBeUndefined()

      await chevron.trigger('pointerenter', { pointerType: 'mouse' })
      expect(wrapper.emitted('update:open')).toEqual([[true]])
    })

    // Coming from the left the pointer reaches the icon before the chevron: the menu must open there already
    it('opens on mouse hover over the icon as well, but not for touch', async () => {
      wrapper = mountCluster(cockpit)
      const icon = wrapper.find('.cib-nav-btn-main')

      await icon.trigger('pointerenter', { pointerType: 'touch' })
      expect(wrapper.emitted('update:open')).toBeUndefined()

      await icon.trigger('pointerenter', { pointerType: 'mouse' })
      expect(wrapper.emitted('update:open')).toEqual([[true]])

      // Moving on to the chevron keeps it open rather than toggling it
      await wrapper.find('.cib-nav-btn-chevron').trigger('pointerenter', { pointerType: 'mouse' })
      expect(wrapper.emitted('update:open')).toEqual([[true]])
    })

    it('does not open anything when hovering the icon of a section without a menu', async () => {
      wrapper = mountCluster({ ...cockpit, sections: [] })
      await wrapper.find('.cib-nav-btn-main').trigger('pointerenter', { pointerType: 'mouse' })
      expect(wrapper.emitted('update:open')).toBeUndefined()
    })

    it('closes a hover-opened menu shortly after the pointer leaves, unless it comes back', async () => {
      vi.useFakeTimers()
      wrapper = mountCluster(cockpit)
      await wrapper.find('.cib-nav-btn-chevron').trigger('pointerenter', { pointerType: 'mouse' })

      await wrapper.trigger('mouseleave')
      await wrapper.trigger('mouseenter')
      vi.advanceTimersByTime(500)
      expect(wrapper.props('open')).toBe(true)

      await wrapper.trigger('mouseleave')
      vi.advanceTimersByTime(500)
      expect(wrapper.emitted('update:open')).toEqual([[true], [false]])
    })

    it('keeps a hover-opened menu open when the chevron is then clicked, and closes on the next click', async () => {
      wrapper = mountCluster(cockpit)
      const chevron = wrapper.find('.cib-nav-btn-chevron')

      await chevron.trigger('pointerenter', { pointerType: 'mouse' })
      await chevron.trigger('click')
      expect(wrapper.props('open')).toBe(true)

      await chevron.trigger('click')
      expect(wrapper.props('open')).toBe(false)
    })

    it('keeps a clicked-open menu when the pointer leaves', async () => {
      vi.useFakeTimers()
      wrapper = mountCluster(language)

      await wrapper.find('.cib-nav-btn-single').trigger('click')
      await wrapper.trigger('mouseleave')
      vi.advanceTimersByTime(500)

      expect(wrapper.props('open')).toBe(true)
    })

    it('keeps a hover-opened menu open once the keyboard moves into it', async () => {
      vi.useFakeTimers()
      wrapper = mountCluster(cockpit)
      const chevron = wrapper.find('.cib-nav-btn-chevron')

      await chevron.trigger('pointerenter', { pointerType: 'mouse' })
      await chevron.trigger('keydown', { key: 'ArrowDown' })
      await wrapper.trigger('mouseleave')
      vi.advanceTimersByTime(500)

      expect(wrapper.props('open')).toBe(true)
    })

    it('does not reopen through hover while already open', async () => {
      wrapper = mountCluster(cockpit, { open: true })
      await wrapper.find('.cib-nav-btn-chevron').trigger('pointerenter', { pointerType: 'mouse' })
      expect(wrapper.emitted('update:open')).toBeUndefined()
    })

    it('shows the tooltip while hovering the icon, not while the menu is open', async () => {
      wrapper = mountCluster(cockpit)

      await wrapper.find('.cib-nav-btn-main').trigger('mouseenter')
      expect(wrapper.find('[role="tooltip"]').text()).toBe('Cockpit')

      await wrapper.setProps({ open: true })
      expect(wrapper.find('[role="tooltip"]').exists()).toBe(false)
    })
  })

  describe('keyboard and outside clicks', () => {
    it('opens with arrow down and moves focus through the items', async () => {
      wrapper = mountCluster(cockpit)

      await wrapper.find('.cib-nav-btn-chevron').trigger('keydown', { key: 'ArrowDown' })
      await wrapper.vm.$nextTick()

      const items = wrapper.findAll('[role="menuitem"]')
      expect(document.activeElement).toBe(items[0].element)

      await wrapper.find('.cib-nav-menu').trigger('keydown', { key: 'ArrowDown' })
      expect(document.activeElement).toBe(items[1].element)
      await wrapper.find('.cib-nav-menu').trigger('keydown', { key: 'ArrowUp' })
      await wrapper.find('.cib-nav-menu').trigger('keydown', { key: 'ArrowUp' })
      expect(document.activeElement).toBe(items.at(-1).element)
    })

    it('closes with escape and gives focus back to the toggle', async () => {
      wrapper = mountCluster(cockpit, { open: true })

      await wrapper.find('.cib-nav-menu-item').trigger('keydown', { key: 'Escape' })

      expect(wrapper.find('.cib-nav-menu').exists()).toBe(false)
      expect(document.activeElement).toBe(wrapper.find('.cib-nav-btn-chevron').element)
    })

    it('closes on a click outside and when focus leaves it', async () => {
      wrapper = mountCluster(cockpit)
      await wrapper.setProps({ open: true })

      document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }))
      await wrapper.vm.$nextTick()
      expect(wrapper.props('open')).toBe(false)

      await wrapper.setProps({ open: true })
      await wrapper.trigger('focusout', { relatedTarget: document.body })
      expect(wrapper.props('open')).toBe(false)
    })
  })

  describe('accessibility', () => {
    it('marks pickable items as menuitemradio and plain ones as menuitem', () => {
      wrapper = mountCluster({ ...language, sections: [[...language.sections[0], { key: 'x', label: 'Plain' }]] }, { open: true })
      const roles = wrapper.findAll('button.cib-nav-menu-item').map(item => item.attributes('role'))
      expect(roles).toEqual(['menuitemradio', 'menuitemradio', 'menuitem'])
    })

    it('only points aria-describedby at the tooltip while it is shown', async () => {
      wrapper = mountCluster(cockpit)
      const main = wrapper.find('.cib-nav-btn-main')
      expect(main.attributes('aria-describedby')).toBeUndefined()

      await main.trigger('mouseenter')
      const id = main.attributes('aria-describedby')
      expect(wrapper.find(`#${id}`).attributes('role')).toBe('tooltip')
    })

    it('includes legacy slot entries in arrow-key navigation', async () => {
      wrapper = mount(NavCluster, {
        attachTo: document.body,
        props: { cluster: language, open: true },
        slots: { 'menu-prepend': '<ul class="cib-nav-legacy-items"><li><a href="#" class="legacy">Settings</a></li></ul>' },
        global: { mocks: { $t: key => key } }
      })
      const legacy = wrapper.find('a.legacy').element
      legacy.focus()

      await wrapper.find('.cib-nav-menu').trigger('keydown', { key: 'ArrowDown' })

      expect(document.activeElement).toBe(wrapper.findAll('[role="menuitemradio"]')[0].element)
    })
  })
})
