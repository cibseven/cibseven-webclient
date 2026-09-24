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
import { callWith } from '../support/callWithContext.js'
import { DecisionService } from '@/services.js'

vi.mock('@/services.js', () => ({
  DecisionService: { updateHistoryTTLById: vi.fn() }
}))

const DecisionDefinitionDetails = (await import('@/components/decision/DecisionDefinitionDetails.vue')).default

beforeEach(() => {
  vi.clearAllMocks()
})

describe('DecisionDefinitionDetails.vue', () => {
  describe('data', () => {
    it('defaults historyTimeToLive to null until mounted', () => {
      expect(DecisionDefinitionDetails.data()).toEqual({ historyTimeToLive: null })
    })
  })

  describe('ttlDescription', () => {
    it('describes the definition name and version', () => {
      const { result } = callWith(DecisionDefinitionDetails.computed.ttlDescription, {
        version: { name: 'Discount', version: 3 }
      })
      expect(result).toBe('decision.details.definitionName: Discount (decision.details.definitionVersion: 3)')
    })
  })

  describe('historyTimeToLiveDisplay', () => {
    // Regression: 0 and null are opposite engine semantics (0 = eligible for cleanup
    // today, null = never) and must never render identically.
    it.each([
      ['0 (eligible today)', 0, '0 decision.days'],
      ['a positive number', 7, '7 decision.days']
    ])('renders an explicit %s as-is, ignoring engine config', (_, historyTimeToLive, expected) => {
      const { result } = callWith(DecisionDefinitionDetails.computed.historyTimeToLiveDisplay, {
        historyTimeToLive,
        $root: { config: { enforceHistoryTimeToLive: true, historyTimeToLive: '999' } }
      })
      expect(result).toBe(expected)
    })

    describe('when the definition has no override (null or undefined)', () => {
      // Regression: enforceHistoryTimeToLive is a known boolean (true OR false) whenever
      // the engine actually reported its config - only null/undefined means "unknown".
      // Both known values must be treated identically for showing the engine's own default.
      it.each([
        ['null historyTimeToLive, enforce true', null, true],
        ['undefined historyTimeToLive, enforce true', undefined, true],
        ['null historyTimeToLive, enforce false', null, false],
        ['undefined historyTimeToLive, enforce false', undefined, false]
      ])('shows unlimited when the engine default itself is unlimited (%s)', (_, historyTimeToLive, enforceHistoryTimeToLive) => {
        const { result } = callWith(DecisionDefinitionDetails.computed.historyTimeToLiveDisplay, {
          historyTimeToLive,
          $root: { config: { enforceHistoryTimeToLive, historyTimeToLive: null } }
        })
        expect(result).toBe('∞')
      })

      it.each([true, false])('shows the engine default day count, with its value, whenever enforceHistoryTimeToLive is known (%s)', (enforceHistoryTimeToLive) => {
        const $t = vi.fn((key, params) => params ? `${key}:${JSON.stringify(params)}` : key)
        const { result, context } = callWith(DecisionDefinitionDetails.computed.historyTimeToLiveDisplay, {
          historyTimeToLive: null,
          $t,
          $root: { config: { enforceHistoryTimeToLive, historyTimeToLive: '7' } }
        })
        expect(result).toBe('historyTimeToLive.choiceDefault:{"value":"7"}')
        expect(context.$t).toHaveBeenCalledWith('historyTimeToLive.choiceDefault', { value: '7' })
      })

      it('shows a generic "unknown default" label when the engine reports nothing at all', () => {
        const { result } = callWith(DecisionDefinitionDetails.computed.historyTimeToLiveDisplay, {
          historyTimeToLive: null,
          $root: { config: {} }
        })
        expect(result).toBe('historyTimeToLive.choiceDefaultUnknown')
      })
    })
  })

  describe('editHistoryTimeToLive', () => {
    it('opens the shared modal with the current TTL', () => {
      const show = vi.fn()
      callWith(DecisionDefinitionDetails.methods.editHistoryTimeToLive, {
        historyTimeToLive: 7,
        $refs: { ttlModal: { show } }
      })

      expect(show).toHaveBeenCalledWith({ historyTimeToLive: 7 })
    })
  })

  describe('onTtlUpdated', () => {
    it('saves the new TTL, updates local state, and emits updated-history-ttl without mutating the version prop', async () => {
      DecisionService.updateHistoryTTLById.mockResolvedValue({})
      const version = { id: 'drd-1', historyTimeToLive: 7 }
      const { context } = callWith(DecisionDefinitionDetails.methods.onTtlUpdated,
        { version, historyTimeToLive: 7 },
        { ttlValue: null }
      )
      await Promise.resolve()
      await Promise.resolve()

      expect(DecisionService.updateHistoryTTLById).toHaveBeenCalledWith('drd-1', { historyTimeToLive: null })
      expect(context.historyTimeToLive).toBeNull()
      expect(context.$emit).toHaveBeenCalledWith('updated-history-ttl', null)
      // The version prop itself must be left alone - the component's own data is the
      // source of truth for what it displays, per the vue/no-mutating-props cleanup.
      expect(version.historyTimeToLive).toBe(7)
    })

    it('forwards a rejected save to the shared error banner', async () => {
      const error = { response: { data: { message: 'denied' } } }
      DecisionService.updateHistoryTTLById.mockRejectedValue(error)
      const { context } = callWith(DecisionDefinitionDetails.methods.onTtlUpdated,
        { version: { id: 'drd-1' }, historyTimeToLive: 7 },
        { ttlValue: 0 }
      )
      await Promise.resolve()
      await Promise.resolve()

      expect(context.$root.$refs.error.show).toHaveBeenCalledWith(error.response.data)
      expect(context.historyTimeToLive).toBe(7)
    })
  })
})
