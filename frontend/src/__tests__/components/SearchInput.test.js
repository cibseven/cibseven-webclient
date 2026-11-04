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
import SearchInput from '@/components/common-components/SearchInput.vue'

function mountSearch(props = {}, options = {}) {
  return mountWithDefaults(SearchInput, { props, ...options })
}

describe('SearchInput', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
  })

  describe('one field container (CIB7-1619)', () => {
    it('renders the icon and the input inside one form-control container', () => {
      wrapper = mountSearch()
      const container = wrapper.find('.search-field')
      expect(container.classes()).toContain('form-control')
      expect(container.find('.mdi-magnify').exists()).toBe(true)
      expect(container.find('input').exists()).toBe(true)
      // the inner input carries no form-control border of its own, so only the container shows focus
      expect(container.find('input').classes()).not.toContain('form-control')
    })

    it('uses the small form-control variant for size sm only', () => {
      wrapper = mountSearch({ size: 'sm' })
      expect(wrapper.find('.search-field').classes()).toContain('form-control-sm')
      expect(wrapper.find('.mdi-magnify').classes()).toContain('mdi-16px')

      wrapper.unmount()
      wrapper = mountSearch({ size: 'md' })
      expect(wrapper.find('.search-field').classes()).not.toContain('form-control-sm')
      expect(wrapper.find('.mdi-magnify').classes()).toContain('mdi-18px')
    })

    it('marks the container as disabled together with the input', () => {
      wrapper = mountSearch({ disabled: true })
      expect(wrapper.find('.search-field').classes()).toContain('disabled')
      expect(wrapper.find('input').attributes('disabled')).toBeDefined()
    })

    it('renders append slot content inside the container', () => {
      wrapper = mountSearch({}, { slots: { append: '<span class="count">3 / 5</span>' } })
      expect(wrapper.find('.search-field .count').text()).toBe('3 / 5')
    })
  })

  describe('decorative icon (default)', () => {
    it('is hidden from assistive technology and not focusable', () => {
      wrapper = mountSearch()
      const icon = wrapper.find('.search-field-icon')
      expect(icon.element.tagName).toBe('SPAN')
      expect(icon.attributes('aria-hidden')).toBe('true')
      expect(wrapper.find('button').exists()).toBe(false)
    })

    it('does not emit search on Enter', async () => {
      wrapper = mountSearch()
      await wrapper.find('input').trigger('keydown', { key: 'Enter' })
      expect(wrapper.emitted('search')).toBeUndefined()
    })
  })

  describe('search button', () => {
    it('renders the magnifier as a labelled button', () => {
      wrapper = mountSearch({ searchButton: true, searchButtonLabel: 'Refresh and filter' })
      const button = wrapper.find('button.search-field-button')
      expect(button.attributes('type')).toBe('button')
      expect(button.attributes('aria-label')).toBe('Refresh and filter')
      expect(button.find('.mdi-magnify').attributes('aria-hidden')).toBe('true')
      expect(wrapper.find('.search-field-icon').exists()).toBe(false)
    })

    it('falls back to the generic search label', () => {
      wrapper = mountSearch({ searchButton: true })
      expect(wrapper.find('button').attributes('aria-label')).toBe('searches.search')
    })

    it('emits search with the current value on click and on Enter', async () => {
      wrapper = mountSearch({ searchButton: true })
      const input = wrapper.find('input')
      input.element.value = 'invoice'

      await wrapper.find('button').trigger('click')
      await input.trigger('keydown', { key: 'Enter' })

      expect(wrapper.emitted('search')).toEqual([['invoice'], ['invoice']])
    })

    it('trims the emitted search value with the trim modifier', async () => {
      wrapper = mountSearch({ searchButton: true, modelModifiers: { trim: true } })
      wrapper.find('input').element.value = '  invoice '
      await wrapper.find('button').trigger('click')
      expect(wrapper.emitted('search')).toEqual([['invoice']])
    })

    it('is disabled together with the input', () => {
      wrapper = mountSearch({ searchButton: true, disabled: true })
      expect(wrapper.find('button').attributes('disabled')).toBeDefined()
    })
  })

  describe('value', () => {
    it('shows modelValue and emits update:modelValue on input', async () => {
      wrapper = mountSearch({ modelValue: 'abc' })
      const input = wrapper.find('input')
      expect(input.element.value).toBe('abc')

      await input.setValue(' abcd ')
      expect(wrapper.emitted('update:modelValue')).toEqual([[' abcd ']])
    })

    it('trims the emitted value with the trim modifier', async () => {
      wrapper = mountSearch({ modelModifiers: { trim: true } })
      await wrapper.find('input').setValue(' abcd ')
      expect(wrapper.emitted('update:modelValue')).toEqual([['abcd']])
    })

    it('focus() focuses the inner input', () => {
      wrapper = mountSearch({}, { attachTo: document.body })
      wrapper.vm.focus()
      expect(document.activeElement).toBe(wrapper.find('input').element)
    })
  })

  describe('labelling', () => {
    it('links a visually hidden label to the input, without the trailing ellipsis', () => {
      wrapper = mountSearch({ label: 'Filter...', inputId: 'my-search' })
      const label = wrapper.find('label')
      expect(label.attributes('for')).toBe('my-search')
      expect(label.classes()).toContain('visually-hidden')
      expect(label.text()).toBe('Filter')
      expect(wrapper.find('input').attributes('id')).toBe('my-search')
      expect(wrapper.find('input').attributes('placeholder')).toBe('Filter...')
    })

    it('generates a unique input id per instance', () => {
      wrapper = mountSearch()
      const other = mountSearch()
      expect(wrapper.find('input').attributes('id')).not.toBe(other.find('input').attributes('id'))
      other.unmount()
    })

    it('omits its own label when the page provides one', () => {
      wrapper = mountSearch({ hideLabel: true, placeholder: 'Name or ID', describedBy: 'hint', type: 'search', autocomplete: 'off' })
      const input = wrapper.find('input')
      expect(wrapper.find('label').exists()).toBe(false)
      expect(input.attributes('placeholder')).toBe('Name or ID')
      expect(input.attributes('aria-describedby')).toBe('hint')
      expect(input.attributes('type')).toBe('search')
      expect(input.attributes('autocomplete')).toBe('off')
    })

    it('renders no aria-describedby by default', () => {
      wrapper = mountSearch()
      expect(wrapper.find('input').attributes('aria-describedby')).toBeUndefined()
    })
  })
})
