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
import { describe, it, expect, vi, afterEach } from 'vitest'
import {
  convertFormDataForFormJs,
  findDocumentPreviewComponents,
  getDocumentReferenceVariableName,
  determineValueTypeFromSchema,
  findFieldByKey
} from '@/components/forms/formJsUtils.js'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('convertFormDataForFormJs', () => {
  // The engine reports a variable's technical name separately; when it is null the map key
  // is already the field name.
  it('should key output by the variable name when one is given', () => {
    const result = convertFormDataForFormJs({
      userName: { name: 'user_name', type: 'String', value: 'John Doe', valueInfo: {} }
    })

    expect(result).toEqual({ user_name: 'John Doe' })
  })

  it('should fall back to the map key when the variable name is null', () => {
    const result = convertFormDataForFormJs({
      availableOptions: { name: null, type: 'String', value: 'x', valueInfo: {} }
    })

    expect(result).toEqual({ availableOptions: 'x' })
  })

  it('should parse Json variables into objects', () => {
    const result = convertFormDataForFormJs({
      availableOptions: { name: null, type: 'Json', value: '[{"label":"A","value":"a"}]' }
    })

    expect(result).toEqual({ availableOptions: [{ label: 'A', value: 'a' }] })
  })

  // Malformed JSON must not take the whole form down: the raw string is kept and the
  // failure is logged for diagnosis.
  it('should keep the raw string and log when Json fails to parse', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {})

    const result = convertFormDataForFormJs({
      broken: { name: null, type: 'Json', value: '{not json' }
    })

    expect(result).toEqual({ broken: '{not json' })
    expect(error).toHaveBeenCalledWith('Failed to parse JSON for broken:', '{not json', expect.any(Error))
  })

  it('should leave an already-parsed Json value untouched', () => {
    const value = [{ label: 'A' }]

    const result = convertFormDataForFormJs({ options: { name: null, type: 'Json', value } })

    expect(result.options).toBe(value)
  })

  it('should leave non-Json string values untouched', () => {
    const result = convertFormDataForFormJs({
      raw: { name: null, type: 'String', value: '{"looks":"like json"}' }
    })

    expect(result.raw).toBe('{"looks":"like json"}')
  })

  it('should convert several variables at once', () => {
    const result = convertFormDataForFormJs({
      a: { name: 'alpha', type: 'String', value: '1' },
      b: { name: null, type: 'Integer', value: 2 }
    })

    expect(result).toEqual({ alpha: '1', b: 2 })
  })

  it('should return an empty object for empty form data', () => {
    expect(convertFormDataForFormJs({})).toEqual({})
  })
})

describe('findDocumentPreviewComponents', () => {
  it('should return only the documentPreview components', () => {
    const preview = { type: 'documentPreview', key: 'doc' }

    const result = findDocumentPreviewComponents({
      components: [{ type: 'textfield', key: 'a' }, preview, { type: 'checkbox', key: 'b' }]
    })

    expect(result).toEqual([preview])
  })

  it('should return an empty array when there are none', () => {
    expect(findDocumentPreviewComponents({ components: [{ type: 'textfield' }] })).toEqual([])
  })

  it.each([[{}], [{ components: undefined }], [{ components: null }]])('should return an empty array for %j', (formContent) => {
    expect(findDocumentPreviewComponents(formContent)).toEqual([])
  })
})

describe('getDocumentReferenceVariableName', () => {
  // The postfix keeps a file picker and a document preview bound to the same variable from
  // overwriting each other in the form data.
  it('should append the document reference postfix', () => {
    expect(getDocumentReferenceVariableName('invoice')).toBe('invoice_documentReference')
  })

  it('should append the postfix even to an empty name', () => {
    expect(getDocumentReferenceVariableName('')).toBe('_documentReference')
  })
})

describe('findFieldByKey', () => {
  it('should find a top-level field', () => {
    const field = { key: 'a', type: 'textfield' }

    expect(findFieldByKey({ components: [field] }, 'a')).toBe(field)
  })

  it('should find a field nested inside a group', () => {
    const field = { key: 'deep', type: 'number' }
    const schema = { components: [{ key: 'group', components: [{ key: 'inner', components: [field] }] }] }

    expect(findFieldByKey(schema, 'deep')).toBe(field)
  })

  it('should return null when the key is not present', () => {
    expect(findFieldByKey({ components: [{ key: 'a' }] }, 'missing')).toBeNull()
  })

  it.each([[null], [undefined], [{}], [{ components: null }]])('should return null for schema %j', (schema) => {
    expect(findFieldByKey(schema, 'a')).toBeNull()
  })

  it('should not be confused by a nested group that lacks the key', () => {
    const schema = { components: [{ key: 'group', components: [{ key: 'other' }] }, { key: 'a' }] }

    expect(findFieldByKey(schema, 'a')).toEqual({ key: 'a' })
  })
})

describe('determineValueTypeFromSchema', () => {
  const schemaWith = (component) => ({ components: [component] })

  // A number field with decimal digits must become a Double, or the engine truncates it.
  it('should map a number field with decimal digits to Double', () => {
    expect(determineValueTypeFromSchema(schemaWith({ key: 'a', type: 'number', decimalDigits: 2 }), 'a')).toBe('Double')
  })

  it.each([[0], [undefined], [null]])('should map a number field with decimalDigits %j to Integer', (decimalDigits) => {
    expect(determineValueTypeFromSchema(schemaWith({ key: 'a', type: 'number', decimalDigits }), 'a')).toBe('Integer')
  })

  it('should map a checkbox to Boolean', () => {
    expect(determineValueTypeFromSchema(schemaWith({ key: 'a', type: 'checkbox' }), 'a')).toBe('Boolean')
  })

  it.each([['textfield'], ['textarea'], ['select'], ['unknown-widget']])('should map a %s to String', (type) => {
    expect(determineValueTypeFromSchema(schemaWith({ key: 'a', type }), 'a')).toBe('String')
  })

  it('should default to String when the field is not in the schema', () => {
    expect(determineValueTypeFromSchema(schemaWith({ key: 'other', type: 'number' }), 'a')).toBe('String')
  })

  it('should resolve the type of a nested field', () => {
    const schema = { components: [{ key: 'group', components: [{ key: 'a', type: 'checkbox' }] }] }

    expect(determineValueTypeFromSchema(schema, 'a')).toBe('Boolean')
  })
})
