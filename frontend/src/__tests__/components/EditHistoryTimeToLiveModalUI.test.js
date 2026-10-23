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
import { mount } from '@vue/test-utils'
import { BFormGroup, BFormInput, BButton } from '@cib/common-frontend'
import EditHistoryTimeToLiveModal from '@/components/modals/EditHistoryTimeToLiveModal.vue'

// Accessibility of the rendered markup. The logic is covered in EditHistoryTimeToLiveModal.test.js
// against a hand-built `this`; these tests mount the template with the real b-form-group and
// b-form-input (from @cib/common-frontend), since the ARIA wiring partly lives in those.
//
// The component reads the engine configuration from this.$root.config. When mounted directly,
// $root is the component itself, so `config` is provided as a global property (mocks), which
// the component proxy falls back to.
function createWrapper(config = {}) {
  return mount(EditHistoryTimeToLiveModal, {
    global: {
      mocks: {
        $t: (key) => key,
        config
      },
      stubs: {
        'b-modal': {
          template: '<div><slot></slot><slot name="modal-footer"></slot></div>',
          methods: { show: vi.fn(), hide: vi.fn() }
        },
        'b-form-group': BFormGroup,
        'b-form-input': BFormInput,
        'b-button': BButton
      }
    }
  })
}

// Resolves an aria-labelledby / aria-describedby id list to the referenced elements' text,
// the way assistive technology composes the accessible name / description.
function resolveIdRefs(wrapper, idRefs) {
  return idRefs.split(' ').map(id => {
    const element = wrapper.find(`[id="${id}"]`)
    expect(element.exists(), `referenced element #${id} exists`).toBe(true)
    return element.text()
  }).join(' ')
}

describe('EditHistoryTimeToLiveModal.vue accessibility', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  describe('radio group', () => {
    it('names the radio group through the fieldset legend', () => {
      const wrapper = createWrapper()
      const fieldset = wrapper.find('fieldset')

      expect(fieldset.exists()).toBe(true)
      expect(resolveIdRefs(wrapper, fieldset.attributes('aria-labelledby'))).toBe('historyTimeToLive.ttl')
    })

    it('gives the group legend an id that is unique per modal instance', () => {
      // One modal is mounted per version in the sidebars; without an explicit id, every
      // instance would render the same "form-group__label" legend id.
      const first = createWrapper().find('fieldset legend').attributes('id')
      const second = createWrapper().find('fieldset legend').attributes('id')

      expect(first).not.toBe('form-group__label')
      expect(first).not.toBe(second)
    })
  })

  describe('custom number input', () => {
    it('has an accessible name composed of the custom radio label and the days unit', () => {
      const wrapper = createWrapper()
      const input = wrapper.find('input[type="number"]')

      expect(resolveIdRefs(wrapper, input.attributes('aria-labelledby'))).toBe('historyTimeToLive.choiceCustom historyTimeToLive.days')
    })

    it('links the validation message and marks the input invalid while the value is invalid', async () => {
      const wrapper = createWrapper()
      await wrapper.setData({ choice: 'custom', editTtlValue: '1.5' })
      const input = wrapper.find('input[type="number"]')

      expect(input.attributes('aria-invalid')).toBe('true')
      const describedBy = input.attributes('aria-describedby')
      expect(describedBy).toBeTruthy()
      const feedback = wrapper.find(`[id="${describedBy}"]`)
      expect(feedback.attributes('role')).toBe('alert')
      expect(feedback.text()).toBe('historyTimeToLive.validation')
    })

    it('has no description and is not marked invalid while the value is valid', async () => {
      const wrapper = createWrapper()
      await wrapper.setData({ choice: 'custom', editTtlValue: '30' })
      const input = wrapper.find('input[type="number"]')

      expect(input.attributes('aria-invalid')).toBeUndefined()
      expect(input.attributes('aria-describedby')).toBeUndefined()
      expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    })
  })

  describe('unlimited choice', () => {
    // null on a definition means its history is never cleaned up - the engine default is only
    // applied to models at deployment time - so the option must not be labelled as the default.
    it('is labelled as unlimited, not as the engine default, even when the engine has one', () => {
      const wrapper = createWrapper({ enforceHistoryTimeToLive: false, historyTimeToLive: 180 })
      const radio = wrapper.find('input[type="radio"][value="unlimited"]')
      const label = wrapper.find(`label[for="${radio.attributes('id')}"]`)

      expect(label.text()).toContain('historyTimeToLive.choiceUnlimited')
      expect(wrapper.text()).not.toContain('choiceDefault')
    })
  })

  describe('disabled unlimited choice', () => {
    it('shows the reason as visible text linked to the radio, not only as a title', () => {
      const wrapper = createWrapper({ enforceHistoryTimeToLive: true, historyTimeToLive: -1 })
      const radio = wrapper.find('input[type="radio"][value="unlimited"]')

      expect(radio.attributes('disabled')).toBeDefined()
      expect(resolveIdRefs(wrapper, radio.attributes('aria-describedby'))).toBe('historyTimeToLive.enforcedHint')
      expect(wrapper.find('label[title]').exists()).toBe(false)
    })

    it.each([false, null, undefined])('shows no reason while the choice is available (enforce %j)', (enforceHistoryTimeToLive) => {
      const wrapper = createWrapper({ enforceHistoryTimeToLive, historyTimeToLive: 180 })
      const radio = wrapper.find('input[type="radio"][value="unlimited"]')

      expect(radio.attributes('disabled')).toBeUndefined()
      expect(radio.attributes('aria-describedby')).toBeUndefined()
      expect(wrapper.text()).not.toContain('historyTimeToLive.enforcedHint')
    })
  })
})
