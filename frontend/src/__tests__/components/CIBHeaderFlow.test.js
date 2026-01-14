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
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
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

const createWrapper = (props = {}) => {
  return mount(CIBHeaderFlow, {
    props: {
      languages: ['en', 'de'],
      user: { id: '1', displayName: 'Test User' },
      ...props
    },
    global: {
      provide: {
        currentLanguage: vi.fn((lang) => lang || 'en')
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
        'b-dropdown-item': { template: '<div class="dropdown-item"><slot></slot></div>' }
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

  describe('Hamburger Menu Toggle', () => {
    it('should toggle isCollapsed when clicking hamburger button', async () => {
      wrapper = createWrapper()
      expect(wrapper.vm.isCollapsed).toBe(false)
      
      const toggleButton = wrapper.find('.navbar-toggler')
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
      
      expect(wrapper.find('.navbar-toggler-icon').exists()).toBe(true)
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
      
      const toggleButton = wrapper.find('.navbar-toggler').element
      wrapper.vm.handleClickOutside({ target: toggleButton })
      expect(wrapper.vm.isCollapsed).toBe(true)
    })

    it('should not close menu when clicking span inside toggle button', async () => {
      wrapper.vm.isCollapsed = true
      await wrapper.vm.$nextTick()
      
      const toggleButton = wrapper.find('.navbar-toggler').element
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
})
