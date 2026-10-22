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
import { createComponentContext } from '../support/callWithContext.js'
import EditHistoryTimeToLiveModal from '@/components/modals/EditHistoryTimeToLiveModal.vue'
import { HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY } from '@/constants'

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
  beforeEach(() => {
    localStorage.clear()
  })

  describe('element ids', () => {
    // The sidebars mount one modal per version; a shared id would make <label for> toggle the
    // radio of another instance and clear the selection in the visible one.
    it('gives every instance its own id prefix', () => {
      const first = EditHistoryTimeToLiveModal.data().idPrefix
      const second = EditHistoryTimeToLiveModal.data().idPrefix

      expect(first).toBeTruthy()
      expect(second).toBeTruthy()
      expect(first).not.toBe(second)
    })
  })

  describe('show', () => {
    it.each([
      ['null', { historyTimeToLive: null }, 'default'],
      ['undefined', {}, 'default'],
      ['zero', { historyTimeToLive: 0 }, 'zero']
    ])('preselects the %s choice from the item TTL and prefills the 30-day default', async (_, item, expectedChoice) => {
      const vm = context()

      vm.show(item)
      await vm.$nextTick()

      expect(vm.choice).toBe(expectedChoice)
      expect(vm.editTtlValue).toBe(30)
      expect(vm.$refs.modal.show).toHaveBeenCalled()
    })

    it('preselects the custom choice with the item TTL for a positive number', async () => {
      const vm = context()

      vm.show({ historyTimeToLive: 45 })
      await vm.$nextTick()

      expect(vm.choice).toBe('custom')
      expect(vm.editTtlValue).toBe(45)
      expect(vm.$refs.modal.show).toHaveBeenCalled()
    })

    it('prefills the last saved custom value for the default and zero choices', () => {
      localStorage.setItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY, '90')
      const vm = context()

      vm.show({ historyTimeToLive: null })
      expect(vm.editTtlValue).toBe(90)

      vm.show({ historyTimeToLive: 0 })
      expect(vm.editTtlValue).toBe(90)
    })
  })

  describe('resetTtlDialog', () => {
    it('clears the dialog back to its defaults', () => {
      const vm = context({ selectedItem: { historyTimeToLive: 45 }, choice: 'custom', editTtlValue: 45 })

      vm.resetTtlDialog()

      expect(vm).toMatchObject({ selectedItem: null, choice: 'default', editTtlValue: 30 })
    })

    it('restores the last saved custom value', () => {
      localStorage.setItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY, '90')
      const vm = context({ choice: 'custom', editTtlValue: 45 })

      vm.resetTtlDialog()

      expect(vm.editTtlValue).toBe(90)
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
      const vm = context({ $root: { config: { historyTimeToLive: 30 } } })

      expect(vm.defaultChoiceLabel).toBe('historyTimeToLive.choiceDefault')
    })

    it('shows the zero label when the engine cleans up immediately (0)', () => {
      const vm = context({ $root: { config: { historyTimeToLive: 0 } } })

      expect(vm.defaultChoiceLabel).toBe('historyTimeToLive.choiceDefaultZero')
    })

    it('shows the unlimited label when the engine keeps history indefinitely (-1)', () => {
      const vm = context({ $root: { config: { historyTimeToLive: -1 } } })

      expect(vm.defaultChoiceLabel).toBe('historyTimeToLive.choiceDefaultUnlimited')
    })

    it('falls back to a generic label when the system default is unknown', () => {
      const vm = context({ $root: { config: {} } })

      expect(vm.defaultChoiceLabel).toBe('historyTimeToLive.choiceDefaultUnknown')
    })
  })

  describe('errorInCustomChoice', () => {
    it.each(['default', 'zero'])('is never an error outside the custom choice (%s)', (choice) => {
      const vm = context({ choice, editTtlValue: 'abc' })

      expect(vm.errorInCustomChoice).toBe(false)
    })

    it.each([
      ['not a number', 'abc', true],
      ['empty', '', true],
      ['negative', '-1', true],
      ['above the 356000 ceiling', '356001', true],
      ['the 356000 ceiling', '356000', false],
      ['zero', '0', false],
      ['within range', '30', false],
      ['a number', 30, false]
    ])('treats %s (%j) as error=%s', (_, rawValue, expectedError) => {
      const vm = context({ choice: 'custom', editTtlValue: rawValue })

      expect(vm.errorInCustomChoice).toBe(expectedError)
    })

    it('exposes the input bounds', () => {
      const vm = context()

      expect(vm.customValueMin).toBe(0)
      expect(vm.customValueMax).toBe(356000)
    })
  })

  describe('captionValue / captionHint', () => {
    it.each([
      ['Default to system (30 days)', 'Default to system', '30 days'],
      ['Custom', 'Custom', ''],
      ['', '', ''],
      ['a', 'a', ''],
      ['a (b)', 'a', 'b'],
      ['a (b c)', 'a', 'b c'],
      ['a (b) (x)', 'a', 'b) (x'],
      [undefined, '', '']
    ])('splits %j into the value and the parenthesised hint', (caption, expectedValue, expectedHint) => {
      const vm = context()

      expect(vm.captionValue(caption)).toBe(expectedValue)
      expect(vm.captionHint(caption)).toBe(expectedHint)
    })
  })

  describe('last custom value storage', () => {
    it('falls back to 30 when nothing was saved', () => {
      const vm = context()

      expect(vm.loadLastCustomValue()).toBe(30)
    })

    it('round-trips a saved value', () => {
      const vm = context()

      vm.saveLastCustomValue(120)

      expect(localStorage.getItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY)).toBe('120')
      expect(vm.loadLastCustomValue()).toBe(120)
    })

    it.each(['abc', '-1', '356001'])('falls back to 30 when the stored value (%s) is not a valid number of days', (stored) => {
      localStorage.setItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY, stored)
      const vm = context()

      expect(vm.loadLastCustomValue()).toBe(30)
    })

    it.each(['0', '356000'])('accepts the boundary value %s', (stored) => {
      localStorage.setItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY, stored)
      const vm = context()

      expect(vm.loadLastCustomValue()).toBe(Number(stored))
    })

    it('does not save an empty value', () => {
      const vm = context()

      vm.saveLastCustomValue(null)
      vm.saveLastCustomValue('')

      expect(localStorage.getItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY)).toBeNull()
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

    it('remembers the custom value, but not the default or zero choices', async () => {
      const zero = context({ choice: 'zero', selectedItem: { id: 'def-1' } })
      await zero.saveTtlValue()
      expect(localStorage.getItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY)).toBeNull()

      const custom = context({ choice: 'custom', editTtlValue: '45', selectedItem: { id: 'def-1' } })
      await custom.saveTtlValue()
      expect(localStorage.getItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY)).toBe('45')
    })

    it('does not emit or close when the entered value fails validation', async () => {
      const vm = context({ choice: 'custom', editTtlValue: '-1' })

      await vm.saveTtlValue()

      expect(vm.$emit).not.toHaveBeenCalled()
      expect(vm.$refs.modal.hide).not.toHaveBeenCalled()
      expect(localStorage.getItem(HISTORY_TTL_LAST_CUSTOM_VALUE_STORAGE_KEY)).toBeNull()
    })
  })
})
