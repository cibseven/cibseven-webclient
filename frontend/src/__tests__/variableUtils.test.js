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
import variableUtils from '@/components/process/mixins/variableUtils'

const FILE_TYPE_FLOW = 'de.cib.cibflow.api.files.FileValueDataFlowSource'
const FILE_TYPE_SOURCE = 'de.cib.cibflow.api.files.FileValueDataSource'

// Helpers to build variable fixtures
const makeVar = (type, value, valueInfo = {}, extra = {}) => ({
  type,
  value,
  valueInfo,
  ...extra,
})

describe('getFileObjects', () => {
  it('returns the two known file object type names', () => {
    expect(variableUtils.getFileObjects()).toEqual([FILE_TYPE_FLOW, FILE_TYPE_SOURCE])
  })
})

describe('isFileValueDataSource', () => {
  it.each([
    [makeVar('Object', { objectTypeName: FILE_TYPE_FLOW }), true],
    [makeVar('Object', { objectTypeName: FILE_TYPE_SOURCE }), true],
    [makeVar('Object', null, { objectTypeName: FILE_TYPE_FLOW }), true],
    [makeVar('Object', { objectTypeName: 'some.Other.Type' }), false],
    [makeVar('Object', {}), false],
    [makeVar('File', null, { filename: 'doc.pdf' }), false],
    [makeVar('String', 'hello'), false],
  ])('variable %# → %s', (variable, expected) => {
    expect(variableUtils.isFileValueDataSource(variable)).toBe(expected)
  })
})

describe('isFile', () => {
  it.each([
    [makeVar('File', null, { filename: 'doc.pdf' }), true],
    [makeVar('Object', { objectTypeName: FILE_TYPE_SOURCE }), true],
    [makeVar('Object', { objectTypeName: 'other.Type' }), false],
    [makeVar('String', 'hello'), false],
    [makeVar('Null', null), false],
  ])('variable %# → %s', (variable, expected) => {
    expect(variableUtils.isFile(variable)).toBe(expected)
  })
})

describe('getFileVariableName', () => {
  it.each([
    // valueDeserialized object with name wins
    [makeVar('Object', null, {}, { valueDeserialized: { name: 'from-deserialized.txt' } }), 'from-deserialized.txt'],
    // falls back to value object with name
    [makeVar('Object', { name: 'from-value.txt' }), 'from-value.txt'],
    // value is JSON string with name
    [makeVar('Object', JSON.stringify({ name: 'from-json.txt' })), 'from-json.txt'],
    // value is JSON string without name
    [makeVar('Object', JSON.stringify({ other: 'field' })), ''],
    // value is invalid JSON string
    [makeVar('Object', 'not-json'), ''],
    // value is object without name
    [makeVar('Object', { size: 42 }), ''],
    // value is null
    [makeVar('Object', null), ''],
  ])('variable %# → "%s"', (variable, expected) => {
    expect(variableUtils.getFileVariableName(variable)).toBe(expected)
  })
})

describe('displayValue', () => {
  describe('File type', () => {
    it('returns valueInfo.filename', () => {
      expect(variableUtils.displayValue(makeVar('File', null, { filename: 'report.pdf' }))).toBe('report.pdf')
    })
  })

  describe('FileValueDataSource (Object with file objectTypeName)', () => {
    it('returns the file name from value object', () => {
      const variable = makeVar('Object', { objectTypeName: FILE_TYPE_SOURCE, name: 'upload.zip' })
      expect(variableUtils.displayValue(variable)).toBe('upload.zip')
    })
  })

  describe('Json type', () => {
    it.each([
      // valueSerialized string takes priority
      [makeVar('Json', null, {}, { valueSerialized: '{"a":1}' }), '{"a":1}'],
      // value object falls back to JSON.stringify
      [makeVar('Json', { key: 'val' }), JSON.stringify({ key: 'val' }, null, 2)],
      // value object that cannot be stringified, for example with BigInt
      [makeVar('Json', { y: BigInt(2) }), '- Json Object -'],
      [makeVar('Json', true), '- Json Object -'],
      // null value: typeof null === 'object', so JSON.stringify returns 'null'
      [makeVar('Json', null), 'null'],
      // non-object, non-string → placeholder
      [makeVar('Json', 42), '- Json Object -'],
    ])('variable %# → expected string', (variable, expected) => {
      expect(variableUtils.displayValue(variable)).toBe(expected)
    })
  })

  describe('Object type', () => {
    it.each([
      // valueDeserialized object
      [
        makeVar('Object', null, {}, { valueDeserialized: { x: 1 } }),
        JSON.stringify({ x: 1 }, null, 2),
      ],
      // value object (no valueDeserialized)
      [makeVar('Object', { y: 2 }), JSON.stringify({ y: 2 }, null, 2)],
      // value object that cannot be stringified, for example with BigInt
      [makeVar('Object', { y: BigInt(2) }), '- Object -'],
      [makeVar('Object', null, {}, { valueDeserialized: { "x" : BigInt(2) } }), '- Object -'],
      [makeVar('Object', true), '- Object -'],
      // value string
      [makeVar('Object', 'raw-string'), 'raw-string'],
      [makeVar('Object', '{"var": malformed JSON'), '{"var": malformed JSON'],
      // null value: typeof null === 'object', so JSON.stringify returns 'null'
      [makeVar('Object', null), 'null'],
    ])('variable %# → expected string', (variable, expected) => {
      expect(variableUtils.displayValue(variable)).toBe(expected)
    })
  })

  describe('Null type', () => {
    it('returns empty string', () => {
      expect(variableUtils.displayValue(makeVar('Null', null))).toBe('')
    })
  })

  describe('primitive types', () => {
    it.each([
      [makeVar('String', 'hello'), 'hello'],
      [makeVar('Integer', 42), '42'],
      [makeVar('Boolean', true), 'true'],
      [makeVar('Boolean', false), 'false'],
      [makeVar('Long', 0), '0'],
    ])('variable %# → "%s"', (variable, expected) => {
      expect(variableUtils.displayValue(variable)).toBe(expected)
    })
  })
})
