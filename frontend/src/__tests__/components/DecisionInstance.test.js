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
import DecisionInstance from '@/components/decision/DecisionInstance.vue'

const { normalizeCell, isDmnStringLiteral } = DecisionInstance.methods

describe('DecisionInstance', () => {
  describe('normalizeCell', () => {
    it.each([
      [undefined,       ''],
      [null,            ''],
      ['',              ''],
      ['""',            ''],

      ['"hello"',       'hello'],
      ['"  hello  "',   'hello'],
      ['  "world"  ',   'world'],
      ['  " unclosed  ','unclosed'],
      ['  unclosed " ', 'unclosed'],
      ['hello',         'hello'],
      ['  hello  ',     'hello'],
      ['"budget"',      'budget'],
      ['"exceptional"', 'exceptional'],
      ['"a"',           'a'],
      ['"a", "b"',      'a", "b'], // ok - only removes surrounding quotes, not inner ones
    ])('normalizeCell(%s) → %s', (input, expected) => {
      expect(normalizeCell(input)).toBe(expected)
    })
  })

  describe('isDmnStringLiteral', () => {
    it.each([
      ['"hello"',       true],
      ['"budget"',      true],
      ['"exceptional"', true],
      ['""',            true],
      ['  "hello"  ',   true],
      ['"a", "b"',      true],

      [undefined,       false],
      [null,            false],
      ['',              false],
      ['  ',            false],

      ['hello',         false],
      ['123',           false],
      ['"unclosed',     false],
      ['unclosed"',     false],
    ])('isDmnStringLiteral(%s) → %s', (input, expected) => {
      expect(isDmnStringLiteral(input)).toBe(expected)
    })
  })
})
