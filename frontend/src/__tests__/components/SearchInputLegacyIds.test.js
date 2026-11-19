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
import { shallowMount } from '@vue/test-utils'
import { mountWithDefaults } from '../support/mountWithDefaults.js'
import StartProcess from '@/components/start-process/StartProcess.vue'
import DeploymentsView from '@/components/deployment/DeploymentsView.vue'
import SearchInput from '@/components/common-components/SearchInput.vue'

vi.mock('@/services.js', async (importOriginal) => ({
  ...(await importOriginal()),
  ProcessService: {
    findDeployments: vi.fn(() => Promise.resolve([])),
    findDeploymentsCount: vi.fn(() => Promise.resolve(0))
  }
}))

// Globally registered in the app, not in a bare mount
const bootstrapStubs = Object.fromEntries(['b-list-group', 'b-list-group-item', 'b-input-group', 'b-input-group-prepend',
  'b-input-group-append', 'b-form-group', 'b-form-select', 'b-form-checkbox', 'b-button', 'b-spinner', 'b-waiting-box',
  'b-modal', 'b-popover'].map(name => [name, true]))

// Before SearchInput took an input id, these two search fields rendered id="searchInput" and
// pages outside this repository (E2E selectors) may still look them up by it (CIB7-1619)
describe('search fields that keep their former id', () => {
  let wrapper

  afterEach(() => wrapper?.unmount())

  it('start process dialog', () => {
    wrapper = mountWithDefaults(StartProcess, {
      global: {
        stubs: { 'b-list-group': true, 'b-list-group-item': true, GlobalEvents: true, RenderTemplate: true, StartProcessDefaultForm: true, BWaitingBox: true, HighlightedText: true },
        mocks: {
          $store: { state: { process: { list: [] } } },
          $eventBus: { on: vi.fn(), off: vi.fn() },
          config: { authorizationEnabled: false }
        },
        provide: { loadProcesses: vi.fn() }
      }
    })

    const input = wrapper.findComponent(SearchInput).find('input')
    expect(input.attributes('id')).toBe('searchInput')
    expect(wrapper.find('label[for="searchInput"]').exists()).toBe(true)
  })

  it('deployments view', () => {
    wrapper = shallowMount(DeploymentsView, {
      global: {
        stubs: bootstrapStubs,
        mocks: {
          $t: key => key,
          $route: { query: {} },
          $router: { push: vi.fn(), replace: vi.fn() },
          config: { authorizationEnabled: false }
        },
        provide: { loadProcesses: vi.fn() },
        directives: { 'b-popover': {}, 'b-tooltip': {} }
      }
    })

    expect(wrapper.findComponent(SearchInput).props('inputId')).toBe('searchInput')
  })
})
