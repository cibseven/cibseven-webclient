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
import { moment } from '@/globals.js'
import { toEngineDate, isDateCriteriaKey, dateExamples } from '@/utils/filterDates.js'

const ENGINE_FORMAT = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}[+-]\d{4}$/

describe('filterDates', () => {
  describe('toEngineDate', () => {
    it.each([
      ['2026.03.03', '2026-03-03T00:00:00.000'],
      ['2026-03-03', '2026-03-03T00:00:00.000'],
      ['2026/03/03', '2026-03-03T00:00:00.000'],
      ['03.03.2026', '2026-03-03T00:00:00.000'],
      ['03.03.2026 14:30', '2026-03-03T14:30:00.000'],
      ['2026.03.03 14:30:15', '2026-03-03T14:30:15.000'],
      ['2026-03-03T14:30:15', '2026-03-03T14:30:15.000']
    ])('converts "%s" to the engine format', (input, expectedPrefix) => {
      const result = toEngineDate(input)
      expect(result).toMatch(ENGINE_FORMAT)
      expect(result.startsWith(expectedPrefix)).toBe(true)
    })

    it('keeps the instant of a value that already has an offset', () => {
      const input = '2013-01-23T14:42:45.000+0200'
      expect(new Date(toEngineDate(input)).getTime()).toBe(new Date('2013-01-23T14:42:45.000+02:00').getTime())
    })

    it.each([null, undefined, '', '   ', 'tomorrow', '2026.13.45', '31.02.2026'])('returns null for %j', (input) => {
      expect(toEngineDate(input)).toBeNull()
    })
  })

  describe('isDateCriteriaKey', () => {
    it.each(['dueAfter', 'dueBefore', 'followUpAfter', 'createdBefore', 'followUpBeforeOrNotExistent'])('%s is a date key', (key) => {
      expect(isDateCriteriaKey(key)).toBe(true)
    })

    it.each([
      'DUEAFTER', // uppercase variant of a correct 'dueAfter' date key
      'dueAfterExpression',
      'name',
      'assignee',
      'priority',
      undefined,
      null,
    ])('%s is not a date key', (key) => {
      expect(isDateCriteriaKey(key)).toBe(false)
    })
  })

  describe('dateExamples', () => {
    it('builds the examples from the given date', () => {
      expect(dateExamples(moment('2026-10-19T14:30:05'))).toEqual({
        iso: '2026-10-19',
        dotted: '2026.10.19',
        german: '19.10.2026',
        isoTime: '2026-10-19T14:30:05'
      })
    })

    it('defaults to today and every example is accepted by toEngineDate', () => {
      Object.values(dateExamples()).forEach(example => expect(toEngineDate(example)).not.toBeNull())
      expect(dateExamples().iso).toBe(moment().format('YYYY-MM-DD'))
    })
  })
})
