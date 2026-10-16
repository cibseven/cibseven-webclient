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
import { describe, it, expect, vi } from 'vitest'
import { createComponentContext } from '../support/callWithContext.js'
import EditHistoryTimeToLiveModal from '@/components/modals/EditHistoryTimeToLiveModal.vue'

// The modal only wraps a b-modal shell around plain data/validation logic, so it is
// exercised against a hand-built `this` rather than mounted.
function context(overrides = {}) {
  const vm = createComponentContext({
    ...EditHistoryTimeToLiveModal.data(),
    $refs: { modal: { show: vi.fn(), hide: vi.fn() } },
    ...overrides
  })
  for (const [name, method] of Object.entries(EditHistoryTimeToLiveModal.methods)) {
    if (typeof method === 'function' && !(name in vm)) vm[name] = method.bind(vm)
  }
  for (const [name, def] of Object.entries(EditHistoryTimeToLiveModal.computed)) {
    if (name in vm || typeof def !== 'function') continue
    Object.defineProperty(vm, name, { get: () => def.call(vm), configurable: true })
  }
  return vm
}

describe('EditHistoryTimeToLiveModal.vue', () => {
  describe('show', () => {
    it.each([
      ['null', { historyTimeToLive: null }, 'default', null],
      ['undefined', {}, 'default', null],
      ['zero', { historyTimeToLive: 0 }, 'zero', null],
      ['a positive number', { historyTimeToLive: 30 }, 'custom', 30]
    ])('preselects the %s choice from the item TTL', async (_, item, expectedChoice, expectedEditValue) => {
      const vm = context()

      vm.show(item)
      await vm.$nextTick()

      expect(vm.choice).toBe(expectedChoice)
      expect(vm.editTtlValue).toBe(expectedEditValue)
      expect(vm.$refs.modal.show).toHaveBeenCalled()
    })
  })

  describe('resetTtlDialog', () => {
    it('clears the dialog back to its defaults', () => {
      const vm = context({ selectedItem: { historyTimeToLive: 30 }, choice: 'custom', editTtlValue: 30, ttlValidationState: true })

      vm.resetTtlDialog()

      expect(vm).toMatchObject({ selectedItem: null, choice: 'default', editTtlValue: null, ttlValidationState: null })
    })
  })

  describe('defaultChoiceDisabled / defaultChoiceLabel', () => {
    it.each([
      [true, true],
      [false, false],
      [null, false],
      [undefined, false]
    ])('only a known enforceHistoryTimeToLive=true disables "default to system" (got %j)', (enforceHistoryTimeToLive, expectedDisabled) => {
      const vm = context({ $root: { config: { enforceHistoryTimeToLive } } })

      expect(vm.defaultChoiceDisabled).toBe(expectedDisabled)
    })

    it('shows the real system default when known', () => {
      const vm = context({ $root: { config: { historyTimeToLive: '30' } } })

      expect(vm.defaultChoiceLabel).toBe('historyTimeToLive.choiceDefault')
    })

    it('falls back to a generic label when the system default is unknown', () => {
      const vm = context({ $root: { config: {} } })

      expect(vm.defaultChoiceLabel).toBe('historyTimeToLive.choiceDefaultUnknown')
    })
  })

  describe('onChoiceChanged', () => {
    it('prefills the definition TTL (or a 30-day default) when switching to custom', async () => {
      const vm = context({ choice: 'custom', selectedItem: { historyTimeToLive: 45 } })

      vm.onChoiceChanged()
      await vm.$nextTick()

      expect(vm.editTtlValue).toBe(45)
      expect(vm.ttlValidationState).toBe(true)
    })

    it('falls back to 30 when the definition had no prior TTL', async () => {
      const vm = context({ choice: 'custom', selectedItem: { historyTimeToLive: null } })

      vm.onChoiceChanged()
      await vm.$nextTick()

      expect(vm.editTtlValue).toBe(30)
    })

    it('does not overwrite an already-entered custom value', async () => {
      const vm = context({ choice: 'custom', editTtlValue: 90, selectedItem: { historyTimeToLive: 45 } })

      vm.onChoiceChanged()
      await vm.$nextTick()

      expect(vm.editTtlValue).toBe(90)
    })

    it('clears validation for the default/zero choices', () => {
      const vm = context({ choice: 'zero' })

      vm.onChoiceChanged()

      expect(vm.ttlValidationState).toBe(true)
    })
  })

  describe('validateTtl', () => {
    it('is always valid outside the custom choice', () => {
      const vm = context({ choice: 'zero' })

      expect(vm.validateTtl()).toBe(true)
      expect(vm.ttlValidationState).toBe(true)
    })

    it.each([
      ['not a number', 'abc', false],
      ['negative', '-1', false],
      ['above the 9999 ceiling', '10000', false],
      ['zero', '0', true],
      ['within range', '30', true]
    ])('treats %s as %s', (_, rawValue, expectedValid) => {
      const vm = context({ choice: 'custom', editTtlValue: rawValue })

      expect(vm.validateTtl()).toBe(expectedValid)
      expect(vm.ttlValidationState).toBe(expectedValid)
    })

    it('reads the value from the input event over the stored editTtlValue', () => {
      const vm = context({ choice: 'custom', editTtlValue: 30 })

      expect(vm.validateTtl({ target: { value: '5000' } })).toBe(true)
    })
  })

  describe('saveTtlValue', () => {
    // Regression coverage for the 3-choice redesign: 'default' must emit ttlValue: null
    // (the engine treats null as unlimited retention) and 'zero' must emit ttlValue: 0
    // (the opposite - eligible for cleanup immediately) - the two must never collapse into
    // each other the way the old unlimited-checkbox implementation once did.
    it('emits ttlValue: null for the default choice', async () => {
      const vm = context({ choice: 'default', selectedItem: { id: 'def-1' }, $root: { config: {} } })

      await vm.saveTtlValue()
      await vm.$nextTick()

      expect(vm.$emit).toHaveBeenCalledWith('ttl-updated', {
        item: { id: 'def-1' },
        ttlValue: null,
        ttlDisplay: 'historyTimeToLive.choiceDefaultUnknown'
      })
      expect(vm.$refs.modal.hide).toHaveBeenCalled()
    })

    it('emits ttlValue: 0 for the zero choice', async () => {
      const vm = context({ choice: 'zero', selectedItem: { id: 'def-1' } })

      await vm.saveTtlValue()

      expect(vm.$emit).toHaveBeenCalledWith('ttl-updated', {
        item: { id: 'def-1' },
        ttlValue: 0,
        ttlDisplay: '0'
      })
    })

    it('emits the parsed day count for the custom choice', async () => {
      const vm = context({ choice: 'custom', editTtlValue: '45', selectedItem: { id: 'def-1' } })

      await vm.saveTtlValue()

      expect(vm.$emit).toHaveBeenCalledWith('ttl-updated', {
        item: { id: 'def-1' },
        ttlValue: 45,
        ttlDisplay: '45 historyTimeToLive.days'
      })
    })

    it('does not emit or close when the entered value fails validation', async () => {
      const vm = context({ choice: 'custom', editTtlValue: '-1' })

      await vm.saveTtlValue()

      expect(vm.$emit).not.toHaveBeenCalled()
      expect(vm.$refs.modal.hide).not.toHaveBeenCalled()
    })
  })
})
