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

  describe('ttlDescriptionItems', () => {
    it('describes the definition name and version as title/value pairs', () => {
      const { result } = callWith(DecisionDefinitionDetails.computed.ttlDescriptionItems, {
        version: { name: 'Discount', version: 3 }
      })
      expect(result).toEqual([
        { title: 'decision.details.definitionName', value: 'Discount' },
        { title: 'decision.details.definitionVersion', value: 3 }
      ])
    })
  })

  describe('historyTimeToLiveDisplay', () => {
    // The labels themselves are covered in utils/historyTimeToLive.test.js; this only checks
    // that the definition's own TTL is passed to the shared helper.
    it.each([
      ['an explicit 0', 0, 'historyTimeToLive.choiceZero'],
      ['a positive number', 30, '30 historyTimeToLive.days'],
      ['no TTL (null)', null, 'historyTimeToLive.choiceUnlimited'],
      ['no TTL (undefined)', undefined, 'historyTimeToLive.choiceUnlimited']
    ])('renders %s through the shared helper', (_, historyTimeToLive, expected) => {
      const { result } = callWith(DecisionDefinitionDetails.computed.historyTimeToLiveDisplay, { historyTimeToLive })
      expect(result).toBe(expected)
    })

    // A definition without a TTL keeps its history forever, whatever the engine default is:
    // the default is only applied to models at deployment time, never to an existing definition.
    it.each([
      ['an engine default of 180 days', { historyTimeToLive: 180, enforceHistoryTimeToLive: false }],
      ['an engine default of 0', { historyTimeToLive: 0, enforceHistoryTimeToLive: true }],
      ['no engine default', { historyTimeToLive: -1, enforceHistoryTimeToLive: false }],
      ['an engine that does not report it', {}]
    ])('shows a definition without TTL as unlimited with %s', (_, config) => {
      const { result } = callWith(DecisionDefinitionDetails.computed.historyTimeToLiveDisplay, {
        historyTimeToLive: null,
        $root: { config }
      })
      expect(result).toBe('historyTimeToLive.choiceUnlimited')
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
