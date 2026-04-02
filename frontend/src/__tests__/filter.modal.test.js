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
import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import { i18n } from '@/i18n'
import FilterModal from '@/components/task/filter/FilterModal.vue'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

function getWrapper() {
  const mockStore = {
    state: {
      filter: {
        selected: {
          id: 'mock-id',
          name: 'mock-name',
          properties: {
            priority: 'high'
          }
        },
        list: []
      },
      user: {
        searchUsers: []
      }
    },
  }

  const translations = JSON.parse(
    // eslint-disable-next-line no-undef
    readFileSync(resolve(__dirname, '../assets/translations_en.json'), 'utf-8')
  )

  i18n.global.locale = 'en'
  i18n.global.setLocaleMessage('en', translations)

  const wrapper = mount(FilterModal, {
    props: {
    },
    global: {
      stubs: {
        'router-link': true,
        'b-waiting-box': true,
        'apexchart': true,
        'b-form-input': true,
        'b-form-group': true,
        'b-form-select-option': true,
        'b-form-select': true,
        'b-button': true,
        'b-form-checkbox': true,
        'b-modal': true
      },
      plugins: [i18n],
      mocks: {
        $store: mockStore
      }
    },
  })
  return wrapper
}
describe('FilterModal', () => {
  // Tests for fixLike
  it.each([
    ['prop', 'val', 'val'],                     // non-LIKE key, return as-is
    ['prop', '%', '%'],                         // non-LIKE key, preserve wildcard

    ['like', 'val', 'val'],                     // lowercase "like", treated literally
    ['LIKE', 'val', 'val'],                     // uppercase "LIKE", no wrapping if not "Like"
    ['LIKE', '100%', '100%'],                   // already wildcarded
    ['LiKe', 'mixedCase', 'mixedCase'],         // mixed casing not matched exactly

    ['Like', '0', '%0%'],                       // LIKE key, numeric string
    ['Like', 'val', '%val%'],                   // LIKE key, wrap normally
    ['Like', '%50', '%50%'],
    ['Like', '%', '%'],                         // LIKE key, just wildcard — no double wrap
  ])('fixLike("%s", "%s") → "%s"', (key, value, expected) => {
    const wrapper = getWrapper()
    expect(wrapper.vm.fixLike(key, value)).toBe(expected)
  })

  // Tests for unfixLike
  it.each([
    ['prop', 'val', 'val'],                     // non-LIKE key, return as-is
    ['prop', '%', '%'],
    ['prop', '%val%', '%val%'],                 // wildcard pattern not removed

    ['like', 'val', 'val'],                     // non-matching key casing
    ['LIKE', 'val', 'val'],
    ['Like', 'val', 'val'],                     // LIKE key, no wildcards

    ['Like', '%val%', 'val'],                   // LIKE key, full wildcard match
    ['Like', 'abc%', 'abc'],                    // end only → unwrapped
    ['Like', '%abc', 'abc'],                    // start only → unwrapped

    ['Like', '%', ''],                          // special case: only wildcard
    ['Like', '%%', ''],                         // double wildcard
    ['Like', '%0%', '0'],                       // numeric value inside wildcards
    ['Like', '%value%', 'value'],               // typical case
    ['Like', '%   %', '   '],                   // space between wildcards
  ])('unfixLike("%s", "%s") → "%s"', (key, value, expected) => {
    const wrapper = getWrapper()
    expect(wrapper.vm.unfixLike(key, value)).toBe(expected)
  })

  // Round-trip tests: unfixLike(fixLike(...)) === original
  describe('Round-trip', () => {
    const keys = ['prop', 'like', 'LIKE', 'Like']
    const values = ['', '   ', 'val', '0', '100', 'your name']

    describe.each(keys)('Round-trip fixLike/unfixLike for key: "%s"', (key) => {
      it.each(values)('value: "%s"', (value) => {
        const wrapper = getWrapper()
        const fixed = wrapper.vm.fixLike(key, value)
        const unfixed = wrapper.vm.unfixLike(key, fixed)
        expect(unfixed).toBe(value)
      })
    })

    it('special cases', () => {
      const wrapper = getWrapper()
      const key = 'Like'
      expect(wrapper.vm.unfixLike(key, wrapper.vm.fixLike(key, '%'))).toBe('')
      expect(wrapper.vm.unfixLike(key, wrapper.vm.fixLike(key, '%val%'))).toBe('val')
    })
  })
})
