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
import SmartSearch from '@/components/task/SmartSearch.vue'

function mountSmartSearch({ props = {}, criteriaKeys = [], modalEnabled = true, criterias = [] } = {}) {
  return mountWithDefaults(SmartSearch, {
    props,
    global: {
      mocks: {
        // Mounted standalone, $root is the component itself
        config: { taskFilter: { advancedSearch: { criteriaKeys, modalEnabled } } },
        $store: { state: { advancedSearch: { criterias } } }
      }
    }
  })
}

describe('SmartSearch', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
  })

  it('renders the task search as one SearchInput field', () => {
    wrapper = mountSmartSearch()
    const field = wrapper.find('.search-field')
    expect(field.exists()).toBe(true)
    expect(field.find('input').attributes('type')).toBe('search')
    expect(field.find('input').attributes('placeholder')).toBe('searches.searchByTaskName')
  })

  // The datalist used to share the input's id, so the label pointed at the datalist and the field had no name
  it('labels the search field and connects the suggestions through their own id', () => {
    wrapper = mountSmartSearch({ props: { options: ['Approve invoice', 'Review'] } })
    const input = wrapper.find('input')
    const datalist = wrapper.find('datalist')
    const id = input.attributes('id')

    expect(wrapper.find('label').attributes('for')).toBe(id)
    expect(wrapper.find('label').text()).toBe('searches.searchByTaskName')
    expect(datalist.attributes('id')).not.toBe(id)
    expect(input.attributes('list')).toBe(datalist.attributes('id'))
    expect(datalist.findAll('option').map(option => option.text())).toEqual(['Approve invoice', 'Review'])
  })

  it('shows the initial filter and emits the trimmed value on input', async () => {
    wrapper = mountSmartSearch({ props: { initialFilter: 'review' } })
    const input = wrapper.find('input')
    expect(input.element.value).toBe('review')

    await input.setValue(' approve ')
    expect(wrapper.emitted('search-filter')).toEqual([['approve']])
    expect(wrapper.vm.filter).toBe(' approve ')
  })

  it('focus() focuses the inner input', () => {
    wrapper = mountWithDefaults(SmartSearch, {
      attachTo: document.body,
      global: { mocks: { config: { taskFilter: { advancedSearch: { criteriaKeys: [], modalEnabled: false } } } } }
    })
    wrapper.vm.focus()
    expect(document.activeElement).toBe(wrapper.find('input').element)
  })

  it('hides the advanced search button without criteria keys', () => {
    wrapper = mountSmartSearch({ criteriaKeys: [] })
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('hides the advanced search button when the modal is disabled', () => {
    wrapper = mountSmartSearch({ criteriaKeys: ['name'], modalEnabled: false })
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('renders the advanced search button inside the field and emits on click', async () => {
    wrapper = mountSmartSearch({ criteriaKeys: ['name'] })
    const button = wrapper.find('.search-field button.search-field-button')
    expect(button.attributes('aria-label')).toBe('advanced-search.title')
    expect(button.find('.bg-danger').exists()).toBe(false)

    await button.trigger('click')
    expect(wrapper.emitted('open-advanced-search')).toHaveLength(1)
  })

  it('marks active advanced search criteria on the button', () => {
    wrapper = mountSmartSearch({ criteriaKeys: ['name'], criterias: [{ key: 'name' }] })
    expect(wrapper.find('button .bg-danger').exists()).toBe(true)
  })
})
