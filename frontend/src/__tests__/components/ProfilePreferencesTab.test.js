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
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ProfilePreferencesTab from '@/components/admin/ProfilePreferencesTab.vue'
import { BFormGroup, BFormSelect, BFormCheckbox } from '@cib/common-frontend'

function createWrapper({ user = null, config = {} } = {}) {
  return mount(ProfilePreferencesTab, {
    global: {
      mocks: {
        $t: key => key,
        // ProfilePreferencesTab reads `this.$root.*`; mounted standalone (no parent),
        // $root resolves to this very instance, so mocking these directly is equivalent.
        user,
        config: {
          authorizationEnabled: false,
          notifications: { tasks: { enabled: false } },
          layout: { showPopoverHowToAssign: false },
          permissions: { tasklist: {}, cockpit: {} },
          ...config
        }
      },
      directives: {
        'b-popover': {}
      },
      stubs: {
        ContentBlock: { template: '<div><slot /></div>' },
        'b-form-group': BFormGroup,
        'b-form-select': BFormSelect,
        'b-form-checkbox': BFormCheckbox,
        RouterLink: true
      }
    }
  })
}

// Regression test for a11y bug: the 3 <select> elements on the settings page had no
// accessible name (axe select-name, WCAG 4.1.2) because their captions were plain <h6>
// text, never associated to the <select> via <label for>.
describe('ProfilePreferencesTab — settings selects have accessible names', () => {
  afterEach(() => localStorage.clear())

  it('associates each select with a <label for> pointing at its id', () => {
    const wrapper = createWrapper()
    const selects = wrapper.findAll('select')

    expect(selects.length).toBe(3)
    selects.forEach(select => {
      const id = select.attributes('id')
      expect(id).toBeTruthy()
      const label = wrapper.find(`label[for="${id}"]`)
      expect(label.exists()).toBe(true)
      expect(label.text().trim().length).toBeGreaterThan(0)
    })
  })
})

describe('ProfilePreferencesTab — persists preferences to localStorage', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => localStorage.clear())

  it('persists the start page selection', async () => {
    const wrapper = createWrapper({ user: { id: '1' } })
    await wrapper.find('#preferences-start-page').setValue('tasks')

    expect(localStorage.getItem('cibseven:preferences:startPage')).toBe('tasks')
  })

  it('persists the default and long date formats', async () => {
    const wrapper = createWrapper()
    await wrapper.find('#preferences-format-default').setValue('LL')
    await wrapper.find('#preferences-format-long').setValue('LLL')

    expect(localStorage.getItem('cibseven:preferences:formatDefault')).toBe('LL')
    expect(localStorage.getItem('cibseven:preferences:formatLong')).toBe('LLL')
  })

  it('persists the shorten badge numbers checkbox', async () => {
    const wrapper = createWrapper()
    const checkbox = wrapper.find('input[type="checkbox"]')
    await checkbox.setValue(false)

    expect(localStorage.getItem('cibseven:preferences:shortenBadgeNumbers')).toBe('false')
  })
})

describe('ProfilePreferencesTab — permission-gated start page options', () => {
  afterEach(() => localStorage.clear())

  it('offers the tasklist and cockpit groups when the user has permission', () => {
    const wrapper = createWrapper({
      user: { id: '1' },
      config: { authorizationEnabled: false }
    })

    // authorizationEnabled: false short-circuits applicationPermissions() to true,
    // so both groups should be present as soon as $root.user exists.
    const optionTexts = wrapper.findAll('option').map(o => o.text())
    expect(optionTexts).toContain('admin.preferences.general.startPage.options.tasks')
    expect(optionTexts).toContain('admin.preferences.general.startPage.options.decisionsList')
  })

  it('omits the tasklist and cockpit groups when there is no user', () => {
    const wrapper = createWrapper({ user: null })

    const optionTexts = wrapper.findAll('option').map(o => o.text())
    expect(optionTexts).not.toContain('admin.preferences.general.startPage.options.tasks')
    expect(optionTexts).not.toContain('admin.preferences.general.startPage.options.decisionsList')
  })
})
