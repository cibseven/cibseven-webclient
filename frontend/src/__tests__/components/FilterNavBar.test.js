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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createRouter, createMemoryHistory, RouterLink } from 'vue-router'
import { mountWithDefaults } from '../support/mountWithDefaults.js'
import FilterNavBar from '@/components/task/filter/FilterNavBar.vue'

const filters = [
  { id: 'f1', name: 'All tasks', properties: { priority: 1 }, tasksNumber: 3, tasksNumberLastUpdated: 1 },
  { id: 'f2', name: 'My tasks', properties: { priority: 2 }, tasksNumber: null, tasksNumberLastUpdated: 0 }
]

describe('FilterNavBar', () => {
  let router

  beforeEach(async () => {
    router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/seven/auth/tasks/:filterId?/:taskId?', component: { template: '<div />' } }]
    })
    await router.push('/seven/auth/tasks/f1/t1')
    await router.isReady()
  })

  function createWrapper() {
    return mountWithDefaults(FilterNavBar, {
      global: {
        plugins: [router],
        stubs: {
          FilterModal: true,
          ConfirmDialog: true,
          BWaitingBox: { template: '<div />', data: () => ({ done: false }) },
          'b-list-group': { template: '<ul class="list-group"><slot></slot></ul>' },
          // The real link, so the rendered href and aria-current are what the browser gets
          'router-link': RouterLink
        },
        mocks: {
          $store: {
            state: { filter: { list: filters, selected: { id: 'f1' }, settings: {} } },
            // Never settles: the test is about the list markup, not the fetch that follows it
            dispatch: vi.fn(() => new Promise(() => {})),
            commit: vi.fn()
          },
          // Mounted standalone, $root is this instance, so these stand in for $root.*
          user: { id: 'demo', permissions: {} },
          config: { authorizationEnabled: false, filtersSearch: false, permissions: {}, taskFilter: { tasksNumber: { enabled: false } } }
        }
      }
    })
  }

  it('renders each filter as a list item with a real link, not a link with the listitem role', () => {
    const items = createWrapper().findAll('ul > li')

    expect(items).toHaveLength(2)
    const link = items[1].find('a')
    expect(link.attributes('href')).toBe('/seven/auth/tasks/f2')
    expect(link.attributes('role')).toBeUndefined()
    expect(link.text()).toBe('My tasks')
  })

  // On a task route (/tasks/f1/t1) the filter link is not exact-active, so router-link
  // alone would not set aria-current; the selected filter must still be announced.
  it('marks only the selected filter as the current page', () => {
    const links = createWrapper().findAll('ul > li > a')

    expect(links[0].attributes('aria-current')).toBe('page')
    expect(links[1].attributes('aria-current')).toBeUndefined()
  })

  it('keeps the filter actions out of the link', () => {
    const items = createWrapper().findAll('ul > li')

    for (const item of items) {
      expect(item.findAll('a button')).toHaveLength(0)
      expect(item.findAll(':scope > div > button, :scope > button').map(b => b.attributes('title')))
        .toEqual(['nav-bar.filters.edit', 'nav-bar.filters.delete', 'nav-bar.filters.pin'])
    }
  })
})
