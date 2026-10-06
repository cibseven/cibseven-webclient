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
import variableUtils from '@/components/process/mixins/variableUtils.js'

const variable = (name, value) => ({ name, value, type: typeof value === 'number' ? 'Long' : 'String' })

describe('variableUtils.matchesValueCondition', () => {

  it('only matches variables of the condition name', () => {
    expect(variableUtils.matchesValueCondition(variable('a', '1'), { name: 'b', operator: 'eq', value: '1' })).toBe(false)
    expect(variableUtils.matchesValueCondition(variable('a', '1'), { name: 'a', operator: 'eq', value: '1' })).toBe(true)
  })

  it.each([
    ['eq', '5', '5', true],
    ['eq', '5', '6', false],
    ['neq', '5', '6', true],
    ['neq', '5', '5', false],
    ['gt', '6', '5', true],
    ['gt', '5', '5', false],
    ['gteq', '5', '5', true],
    ['lt', '4', '5', true],
    ['lteq', '5', '5', true],
    ['lteq', '6', '5', false]
  ])('compares numeric strings with operator %s (%s vs %s)', (operator, actual, expected, result) => {
    expect(variableUtils.matchesValueCondition(variable('n', actual), { name: 'n', operator, value: expected })).toBe(result)
  })

  it('compares numbers numerically, not lexicographically', () => {
    expect(variableUtils.matchesValueCondition(variable('n', 10), { name: 'n', operator: 'gt', value: '9' })).toBe(true)
    expect(variableUtils.matchesValueCondition(variable('n', '10'), { name: 'n', operator: 'lt', value: '9' })).toBe(false)
  })

  it('compares non numeric values as strings', () => {
    expect(variableUtils.matchesValueCondition(variable('s', 'abc'), { name: 's', operator: 'gt', value: 'abb' })).toBe(true)
    expect(variableUtils.matchesValueCondition(variable('s', 'abc'), { name: 's', operator: 'eq', value: 'abd' })).toBe(false)
  })

  it('supports SQL wildcards for the like operator', () => {
    expect(variableUtils.matchesValueCondition(variable('s', 'invoice-2026.pdf'), { name: 's', operator: 'like', value: 'invoice%' })).toBe(true)
    expect(variableUtils.matchesValueCondition(variable('s', 'invoice-2026.pdf'), { name: 's', operator: 'like', value: '%2026%' })).toBe(true)
    expect(variableUtils.matchesValueCondition(variable('s', 'invoice-2026.pdf'), { name: 's', operator: 'like', value: 'credit%' })).toBe(false)
  })

  it('treats a dot in the value as a literal, not as a regex wildcard', () => {
    expect(variableUtils.matchesValueCondition(variable('s', 'aXb'), { name: 's', operator: 'like', value: 'a.b' })).toBe(false)
    expect(variableUtils.matchesValueCondition(variable('s', 'a.b'), { name: 's', operator: 'like', value: 'a.b' })).toBe(true)
  })

  it('honours the ignore case flags', () => {
    const condition = { name: 'Status', operator: 'eq', value: 'DONE' }
    expect(variableUtils.matchesValueCondition(variable('status', 'done'), condition)).toBe(false)
    expect(variableUtils.matchesValueCondition(variable('status', 'done'), condition,
      { namesIgnoreCase: true, valuesIgnoreCase: true })).toBe(true)
  })

  it('does not filter anything out for unknown operators or empty conditions', () => {
    expect(variableUtils.matchesValueCondition(variable('a', '1'), { name: 'a', operator: 'weird', value: '2' })).toBe(true)
    expect(variableUtils.matchesValueCondition(variable('a', '1'), null)).toBe(true)
    expect(variableUtils.matchesValueCondition(variable('a', '1'), {})).toBe(true)
  })

  it('handles null variable values', () => {
    expect(variableUtils.matchesValueCondition({ name: 'a', value: null }, { name: 'a', operator: 'eq', value: '' })).toBe(true)
    expect(variableUtils.matchesValueCondition({ name: 'a', value: null }, { name: 'a', operator: 'eq', value: '1' })).toBe(false)
  })
})
