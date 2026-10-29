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
import { describe, it, expect } from 'vitest'
import { formatHistoryTimeToLive } from '@/utils/historyTimeToLive.js'

const $t = (key) => key

describe('historyTimeToLive utils', () => {
  describe('formatHistoryTimeToLive', () => {
    // A definition without a TTL keeps its history forever: the engine default is only applied
    // to models at deployment time, and history cleanup skips definitions without a TTL. It must
    // never be shown as "default to system (n days)".
    it.each([null, undefined])('labels a definition without TTL (%j) as unlimited', (historyTimeToLive) => {
      expect(formatHistoryTimeToLive($t, historyTimeToLive)).toBe('historyTimeToLive.choiceUnlimited')
    })

    // Regression: 0 and null have opposite meanings for the engine (0 = removed by the next
    // cleanup run, null = never removed) and must never render identically.
    it('labels an explicit 0 with its own label', () => {
      expect(formatHistoryTimeToLive($t, 0)).toBe('historyTimeToLive.choiceZero')
    })

    it('labels a positive number as a day count', () => {
      expect(formatHistoryTimeToLive($t, 30)).toBe('30 historyTimeToLive.days')
    })
  })
})
