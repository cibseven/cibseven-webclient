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
import { formatEngineDefaultHistoryTimeToLive, formatHistoryTimeToLive } from '@/utils/historyTimeToLive.js'

// Renders "key" or "key:{params}", so the tests can assert both the label and its parameters.
const t = vi.fn((key, params) => params ? `${key}:${JSON.stringify(params)}` : key)

describe('historyTimeToLive utils', () => {
  describe('formatEngineDefaultHistoryTimeToLive', () => {
    it.each([
      [30, 'historyTimeToLive.choiceDefault:{"value":30}'],
      [0, 'historyTimeToLive.choiceDefaultZero'],
      [-1, 'historyTimeToLive.choiceDefaultUnlimited']
    ])('labels a reported engine default of %j', (historyTimeToLive, expected) => {
      expect(formatEngineDefaultHistoryTimeToLive(t, { historyTimeToLive })).toBe(expected)
    })

    // Engines before 3.0.0 do not report the default; /info passes that through as null.
    // "Unknown" must never be shown as unlimited (-1) or as 0, both of which are real values.
    it.each([
      ['null', { historyTimeToLive: null }],
      ['undefined', { historyTimeToLive: undefined }],
      ['missing', {}],
      ['a string', { historyTimeToLive: '30' }],
      ['NaN', { historyTimeToLive: Number.NaN }]
    ])('labels a %s engine default as unknown', (_, config) => {
      expect(formatEngineDefaultHistoryTimeToLive(t, config)).toBe('historyTimeToLive.choiceDefaultUnknown')
    })

    it.each([null, undefined])('labels a missing config (%j) as unknown', (config) => {
      expect(formatEngineDefaultHistoryTimeToLive(t, config)).toBe('historyTimeToLive.choiceDefaultUnknown')
    })

    it('does not depend on enforceHistoryTimeToLive', () => {
      for (const enforceHistoryTimeToLive of [true, false, null]) {
        expect(formatEngineDefaultHistoryTimeToLive(t, { enforceHistoryTimeToLive, historyTimeToLive: 30 }))
          .toBe('historyTimeToLive.choiceDefault:{"value":30}')
      }
    })
  })

  describe('formatHistoryTimeToLive', () => {
    // Regression: 0 and null have opposite meanings for the engine (0 = removed by the next
    // cleanup run, null = no TTL of its own) and must never render identically.
    it('labels an explicit 0 with its own label, ignoring the engine default', () => {
      expect(formatHistoryTimeToLive(t, 0, { historyTimeToLive: 999 })).toBe('historyTimeToLive.choiceZero')
    })

    it('labels a positive number as a day count, ignoring the engine default', () => {
      expect(formatHistoryTimeToLive(t, 30, { historyTimeToLive: 999 })).toBe('30 historyTimeToLive.days')
    })

    it.each([null, undefined])('falls back to the engine default label for a definition without TTL (%j)', (historyTimeToLive) => {
      expect(formatHistoryTimeToLive(t, historyTimeToLive, { historyTimeToLive: 30 })).toBe('historyTimeToLive.choiceDefault:{"value":30}')
      expect(formatHistoryTimeToLive(t, historyTimeToLive, { historyTimeToLive: 0 })).toBe('historyTimeToLive.choiceDefaultZero')
      expect(formatHistoryTimeToLive(t, historyTimeToLive, { historyTimeToLive: -1 })).toBe('historyTimeToLive.choiceDefaultUnlimited')
      expect(formatHistoryTimeToLive(t, historyTimeToLive, {})).toBe('historyTimeToLive.choiceDefaultUnknown')
    })
  })
})
