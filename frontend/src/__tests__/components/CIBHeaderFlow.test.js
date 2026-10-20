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
import { describe, it, expect, beforeAll, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { i18n } from '@/i18n'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import CIBHeaderFlow from '@/components/common-components/CIBHeaderFlow.vue'

// Mock EngineService
vi.mock('@/services.js', () => ({
  EngineService: {
    getEngines: vi.fn(() => Promise.resolve([{ name: 'default' }]))
  }
}))

// Mock engineTokens module
vi.mock('@/utils/engineTokens.js', () => ({
  storeTokenForEngine: vi.fn(),
  removeTokenForEngine: vi.fn(),
  hasTokenForEngine: vi.fn(() => false),
  restoreTokenForEngine: vi.fn()
}))

const createWrapper = (props = {}, slots = {}) => {
  return mount(CIBHeaderFlow, {
    slots,
    props: {
      languages: ['en', 'de'],
      user: { id: '1', displayName: 'Test User' },
      ...props
    },
    global: {
      mixins: [{
        data() {
          return {
            config: { productNamePageTitle: 'CIB seven' }
          }
        }
      }],
      provide: {
        currentLanguage: vi.fn((lang) => lang || 'en'),
        setCurrentLanguage: vi.fn((lang) => lang || 'en')
      },
      mocks: {
        $t: (msg) => msg,
        $te: () => true,
      },
      plugins: [i18n],
      stubs: {
        'b-navbar': { template: '<nav class="navbar"><slot></slot></nav>' },
        'b-collapse': { template: '<div><slot></slot></div>', props: ['modelValue'] },
        'b-navbar-nav': { template: '<div class="navbar-nav"><slot></slot></div>' },
        'b-nav-item-dropdown': { template: '<div class="nav-item-dropdown"><slot name="button-content"></slot><slot></slot></div>' },
        'b-dropdown-item-button': { template: '<div class="dropdown-item-button"><slot></slot></div>' },
        'b-dropdown-item': { template: '<div class="dropdown-item"><slot></slot></div>' },
        'b-dropdown-divider': { template: '<hr class="dropdown-divider" />' },
        'b-avatar': { template: '<div class="b-avatar">{{ text }}</div>', props: ['text', 'size', 'src', 'alt', 'variant', 'rounded'] },
        'router-link': { template: '<a class="router-link"><slot></slot></a>', props: ['to'] }
      }
    }
  })
}

describe('CIBHeaderFlow.vue', () => {

  beforeAll(() => {
    const translations = {
      ...JSON.parse(
        // eslint-disable-next-line no-undef
        readFileSync(resolve(__dirname, '../../assets/translations_en.json'), 'utf-8')
      ),
      bcomponents: {
        ariaLabelClose: 'Close',
      },
    }

    i18n.global.locale = 'en'
    i18n.global.setLocaleMessage('en', translations)
  })

  let wrapper

  describe('User avatar', () => {
    it('renders the user\'s initials as avatar', () => {
      wrapper = createWrapper({ user: { id: '1', displayName: 'Ada Morris' } })
      expect(wrapper.find('.cib-nav-avatar').text()).toBe('AM')
    })
  })

  describe('Hamburger Menu Toggle', () => {
    it('should toggle isCollapsed when clicking hamburger button', async () => {
      wrapper = createWrapper()
      expect(wrapper.vm.isCollapsed).toBe(false)
      
      const toggleButton = wrapper.find('.cib-nav-burger')
      await toggleButton.trigger('click')
      
      expect(wrapper.vm.isCollapsed).toBe(true)
    })

    it('should show close icon when menu is open', async () => {
      wrapper = createWrapper()
      wrapper.vm.isCollapsed = true
      await wrapper.vm.$nextTick()
      
      expect(wrapper.find('.mdi-close').exists()).toBe(true)
    })

    it('should show hamburger icon when menu is closed', async () => {
      wrapper = createWrapper()
      wrapper.vm.isCollapsed = false
      await wrapper.vm.$nextTick()
      
      expect(wrapper.find('.mdi-menu').exists()).toBe(true)
    })
  })

  describe('handleClickOutside method', () => {
    beforeEach(() => {
      wrapper = createWrapper()
    })

    it('should not close menu if menu is already closed', () => {
      wrapper.vm.isCollapsed = false
      wrapper.vm.handleClickOutside({ target: document.body })
      expect(wrapper.vm.isCollapsed).toBe(false)
    })

    it('should not close menu when clicking toggle button', async () => {
      wrapper.vm.isCollapsed = true
      await wrapper.vm.$nextTick()
      
      const toggleButton = wrapper.find('.cib-nav-burger').element
      wrapper.vm.handleClickOutside({ target: toggleButton })
      expect(wrapper.vm.isCollapsed).toBe(true)
    })

    it('should not close menu when clicking span inside toggle button', async () => {
      wrapper.vm.isCollapsed = true
      await wrapper.vm.$nextTick()
      
      const toggleButton = wrapper.find('.cib-nav-burger').element
      const spanElement = document.createElement('span')
      toggleButton.appendChild(spanElement)
      
      wrapper.vm.handleClickOutside({ target: spanElement })
      expect(wrapper.vm.isCollapsed).toBe(true)
    })

    it('should close menu when clicking outside navbar', async () => {
      wrapper.vm.isCollapsed = true
      await wrapper.vm.$nextTick()
      
      const outsideElement = document.createElement('div')
      document.body.appendChild(outsideElement)
      
      wrapper.vm.handleClickOutside({ target: outsideElement })
      
      expect(wrapper.vm.isCollapsed).toBe(false)
      document.body.removeChild(outsideElement)
    })
  })

  describe('closeMenu method', () => {
    it('should set isCollapsed to false', () => {
      wrapper = createWrapper()
      wrapper.vm.isCollapsed = true
      wrapper.vm.closeMenu()
      expect(wrapper.vm.isCollapsed).toBe(false)
    })
  })

  describe('Event listeners', () => {
    it('should add document click listener on mount', () => {
      const addEventListenerSpy = vi.spyOn(document, 'addEventListener')
      wrapper = createWrapper()
      expect(addEventListenerSpy).toHaveBeenCalledWith('click', wrapper.vm.handleClickOutside)
      addEventListenerSpy.mockRestore()
    })

    it('should remove document click listener on unmount', () => {
      const removeEventListenerSpy = vi.spyOn(document, 'removeEventListener')
      wrapper = createWrapper()
      const handleClickOutside = wrapper.vm.handleClickOutside
      wrapper.unmount()
      expect(removeEventListenerSpy).toHaveBeenCalledWith('click', handleClickOutside)
      removeEventListenerSpy.mockRestore()
    })
  })

  describe('Engine Token Management', () => {
    let engineTokensMock
    let originalLocation

    beforeEach(async () => {
      vi.clearAllMocks()
      localStorage.clear()
      sessionStorage.clear()
      // Capture the real window.location so afterEach can always restore it,
      // even if a test that mocks it fails mid-assertion.
      originalLocation = window.location
      // Get the mocked module
      engineTokensMock = await import('@/utils/engineTokens.js')
    })

    afterEach(() => {
      // Restore only if a test replaced it; the guard avoids a self-assign that
      // jsdom would treat as navigation for the tests that never mock location.
      if (window.location !== originalLocation) {
        window.location = originalLocation
      }
      localStorage.clear()
      sessionStorage.clear()
    })

    it('should store token when user is set', async () => {
      wrapper = createWrapper({ user: null })
      wrapper.vm.selectedEngine = 'engine1'
      
      await wrapper.setProps({ user: { id: '1', displayName: 'Test User' } })
      
      expect(engineTokensMock.storeTokenForEngine).toHaveBeenCalledWith('engine1')
    })

    it('should remove token for current engine when logout is called', async () => {
      wrapper = createWrapper()
      wrapper.vm.selectedEngine = 'engine1'
      localStorage.setItem('token', 'test-token')
      
      wrapper.vm.logout()
      
      expect(engineTokensMock.removeTokenForEngine).toHaveBeenCalledWith('engine1')
    })

    it('should store current token before switching engines', async () => {
      wrapper = createWrapper()
      wrapper.vm.selectedEngine = 'engine1'
      localStorage.setItem('token', 'old-token')
      
      // Mock hasTokenForEngine to return false (no cached token for new engine)
      engineTokensMock.hasTokenForEngine.mockReturnValue(false)
      
      wrapper.vm.selectEngine('engine2')
      
      expect(engineTokensMock.storeTokenForEngine).toHaveBeenCalledWith('engine1')
    })

    it('should restore cached token when switching to engine with cached token', async () => {
      wrapper = createWrapper()
      wrapper.vm.selectedEngine = 'engine1'
      localStorage.setItem('token', 'old-token')
      
      // Mock hasTokenForEngine to return true (cached token exists)
      engineTokensMock.hasTokenForEngine.mockReturnValue(true)
      engineTokensMock.restoreTokenForEngine.mockReturnValue('cached-token')
      
      // Mock window.location (restored by afterEach)
      delete window.location
      window.location = {
        hash: '',
        reload: vi.fn()
      }

      wrapper.vm.selectEngine('engine2')

      expect(engineTokensMock.restoreTokenForEngine).toHaveBeenCalledWith('engine2')
      expect(window.location.hash).toBe('#/seven/auth/start-configurable')
      expect(window.location.reload).toHaveBeenCalled()
    })

    it('should call logout when no cached token exists for new engine', async () => {
      wrapper = createWrapper()
      wrapper.vm.selectedEngine = 'engine1'
      localStorage.setItem('token', 'old-token')

      engineTokensMock.hasTokenForEngine.mockReturnValue(false)

      // Spy on logout method
      const logoutSpy = vi.spyOn(wrapper.vm, 'logout')

      wrapper.vm.selectEngine('engine2')

      expect(logoutSpy).toHaveBeenCalled()
    })
  })

  describe('Navigation clusters', () => {
    const tasks = { id: 'tasks', label: 'Tasks', icon: 'mdi-clipboard-text-outline', to: '/seven/auth/tasks', sections: [[{ key: 't', label: 'Task list', to: '/seven/auth/tasks' }]] }
    const help = { id: 'help', label: 'Help', icon: 'mdi-help-circle-outline', sections: [[{ key: 'about', label: 'About', modal: 'about' }]] }

    it('adds the engine selector to the main clusters only when there is more than one engine', async () => {
      wrapper = createWrapper({ navClusters: [tasks] })
      await flushPromises() // the mocked backend reports a single engine
      expect(wrapper.vm.mainClusters.map(c => c.id)).toEqual(['tasks'])

      wrapper.vm.engines = [{ id: 'dev', displayName: 'Dev' }, { id: 'test', displayName: 'Test' }]
      wrapper.vm.selectedEngine = 'test'
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.mainClusters.map(c => c.id)).toEqual(['tasks', 'engine'])
      const engines = wrapper.vm.engineCluster.sections[0]
      expect(engines.map(e => [e.label, e.active])).toEqual([['Dev', false], ['Test', true]])
    })

    it('places the parent\'s standard clusters before language and user', () => {
      wrapper = createWrapper({ utilityClusters: [help] })
      expect(wrapper.vm.utilityList.map(c => c.id)).toEqual(['help', 'language', 'user'])
    })

    it('marks the current language and tags each one with its code', () => {
      wrapper = createWrapper()
      const languages = wrapper.vm.languageCluster.sections[0]
      expect(languages.map(l => [l.tag, l.active])).toEqual([['EN', true], ['DE', false]])
    })

    it('leaves out language and user when there are none', () => {
      wrapper = createWrapper({ languages: [], user: null })
      expect(wrapper.vm.utilityList).toEqual([])
    })

    it('lets the avatar lead to the first user entry with a route and ends the menu with logout', () => {
      const profile = { key: 'profile', label: 'Profile', to: '/seven/auth/account/1', active: true }
      wrapper = createWrapper({ userItems: [profile] })

      expect(wrapper.vm.userCluster).toMatchObject({ to: '/seven/auth/account/1', active: true, avatar: 'TU' })
      expect(wrapper.vm.userCluster.sections[0].map(i => i.key)).toEqual(['profile', 'logout'])
    })

    it('shows the user menu as a single button when there is no user entry to lead to', () => {
      wrapper = createWrapper()
      expect(wrapper.vm.userCluster.to).toBeNull()
    })

    it('keeps one menu open at a time', () => {
      wrapper = createWrapper()
      wrapper.vm.setOpenCluster('tasks', true)
      wrapper.vm.setOpenCluster('help', true)
      expect(wrapper.vm.openCluster).toBe('help')

      // a menu that closes late does not close the one opened since
      wrapper.vm.setOpenCluster('tasks', false)
      expect(wrapper.vm.openCluster).toBe('help')
      wrapper.vm.setOpenCluster('help', false)
      expect(wrapper.vm.openCluster).toBeNull()
    })

    it('handles engine, language and logout itself and passes anything else on', () => {
      wrapper = createWrapper()
      const selectEngine = vi.spyOn(wrapper.vm, 'selectEngine').mockImplementation(() => {})
      const logout = vi.spyOn(wrapper.vm, 'logout').mockImplementation(() => {})
      wrapper.vm.isCollapsed = true

      wrapper.vm.onSelect({ engineId: 'dev' })
      wrapper.vm.onSelect({ language: 'de' })
      wrapper.vm.onSelect({ logout: true })
      wrapper.vm.onSelect({ modal: 'about' })

      expect(selectEngine).toHaveBeenCalledWith('dev')
      expect(wrapper.vm.setCurrentLanguage).toHaveBeenCalledWith('de')
      expect(logout).toHaveBeenCalled()
      expect(wrapper.emitted('select')).toEqual([[{ modal: 'about' }]])
      expect(wrapper.vm.isCollapsed).toBe(false)
    })

    it('renders the mobile menu only while it is open', async () => {
      wrapper = createWrapper({ navClusters: [tasks] })
      expect(wrapper.find('.cib-nav-mobile').exists()).toBe(false)

      await wrapper.find('.cib-nav-burger').trigger('click')

      expect(wrapper.find('.cib-nav-mobile').exists()).toBe(true)
    })

    // Parents written for the old header (CIB flow) still fill these slots
    describe('legacy slots', () => {
      const legacy = {
        userItems: '<li class="legacy-user-item">Settings</li>',
        customNavItems: '<li class="legacy-nav-item">Flow tools</li>'
      }

      it('shows the userItems slot at the top of the user menu', async () => {
        wrapper = createWrapper({}, legacy)
        wrapper.vm.setOpenCluster('user', true)
        await wrapper.vm.$nextTick()

        const menu = wrapper.find('.cib-nav-menu')
        expect(menu.find('.legacy-user-item').text()).toBe('Settings')
        expect(menu.text()).toContain('Logout')
      })

      it('shows customNavItems and userItems in the mobile menu', async () => {
        wrapper = createWrapper({}, legacy)
        await wrapper.find('.cib-nav-burger').trigger('click')

        expect(wrapper.find('.cib-nav-mobile .legacy-nav-item').exists()).toBe(true)
        const userRow = wrapper.findAll('.cib-nav-mobile-row').find(row => row.text().includes('Test User'))
        await userRow.trigger('click')
        expect(wrapper.find('.cib-nav-mobile .legacy-user-item').exists()).toBe(true)
      })

      it('closes the user menu when a legacy item is clicked', async () => {
        wrapper = createWrapper({}, legacy)
        wrapper.vm.setOpenCluster('user', true)
        await wrapper.vm.$nextTick()

        await wrapper.find('.cib-nav-menu .legacy-user-item').trigger('click')

        expect(wrapper.vm.openCluster).toBeNull()
        expect(wrapper.find('.cib-nav-menu').exists()).toBe(false)
      })

      it('closes the mobile menu when a legacy user item is tapped', async () => {
        wrapper = createWrapper({}, legacy)
        await wrapper.find('.cib-nav-burger').trigger('click')
        const userRow = wrapper.findAll('.cib-nav-mobile-row').find(row => row.text().includes('Test User'))
        await userRow.trigger('click')

        await wrapper.find('.cib-nav-mobile .legacy-user-item').trigger('click')

        expect(wrapper.find('.cib-nav-mobile').exists()).toBe(false)
      })

      it('adds nothing when the slots are not used', async () => {
        wrapper = createWrapper()
        wrapper.vm.setOpenCluster('user', true)
        await wrapper.vm.$nextTick()
        expect(wrapper.find('.cib-nav-legacy-items').exists()).toBe(false)
      })
    })

    it('renders the page actions only when given', () => {
      wrapper = createWrapper()
      expect(wrapper.find('.cib-nav-actions').exists()).toBe(false)

      wrapper = createWrapper({}, { actions: '<button class="cib-nav-action">Start</button>' })
      expect(wrapper.find('.cib-nav-actions .cib-nav-action').text()).toBe('Start')
    })
  })
})
